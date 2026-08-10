package com.yurishelf.app.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewModelScope
import com.yurishelf.app.data.CatalogRepository
import com.yurishelf.app.data.SeedInfo
import com.yurishelf.app.data.SyncMode
import com.yurishelf.app.data.SyncProgress
import com.yurishelf.app.data.UiSnapshot
import com.yurishelf.app.data.ai.AiRequestException
import com.yurishelf.app.data.ai.AiSettings
import com.yurishelf.app.data.ai.MissingAiApiKeyException
import com.yurishelf.app.data.remote.MissingUserAgentException
import com.yurishelf.app.data.remote.MissingAccessTokenException
import com.yurishelf.app.data.remote.ProxySettings
import com.yurishelf.app.domain.AiAnalysis
import com.yurishelf.app.domain.AiYuriCategory
import com.yurishelf.app.domain.BangumiCollectionType
import com.yurishelf.app.domain.CatalogFilters
import com.yurishelf.app.domain.CatalogType
import com.yurishelf.app.domain.CatalogViewMode
import com.yurishelf.app.domain.SortOption
import com.yurishelf.app.domain.Subject
import com.yurishelf.app.domain.SubjectFormat
import com.yurishelf.app.domain.SubjectKey
import com.yurishelf.app.domain.ThemeMode
import com.yurishelf.app.domain.WinLose
import com.yurishelf.app.domain.filterAndSortSubjects
import com.yurishelf.app.domain.matchesAnyBlockWord
import com.yurishelf.app.domain.paginateSubjects
import com.yurishelf.app.domain.pickRandomSubjects
import com.yurishelf.app.domain.withAiAnalysis
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import java.io.IOException

sealed interface SyncState {
    data object Idle : SyncState
    data class Loading(val progress: SyncProgress? = null) : SyncState
    data class Success(val message: String) : SyncState
    data class Failed(val message: String) : SyncState
}

data class CatalogUiState(
    val filters: CatalogFilters = CatalogFilters(),
    val subjects: List<Subject> = emptyList(),
    val blockedSubjects: List<Subject> = emptyList(),
    val availableYears: List<Int> = emptyList(),
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val totalSubjects: Int = 0,
    val syncState: SyncState = SyncState.Idle,
    val selectedSubjectKey: SubjectKey? = null,
    val proxySettings: ProxySettings = ProxySettings(),
    val hasAccessToken: Boolean = false,
    val blockWords: List<String> = emptyList(),
    val seedInfo: SeedInfo? = null,
    val randomMinimumScore: Double? = null,
    val randomPicks: List<Subject> = emptyList(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val nsfwNoticeDismissed: Boolean = false,
    val viewMode: CatalogViewMode = CatalogViewMode.LIST,
    val aiSettings: AiSettings = AiSettings(),
    val batchAiState: BatchAiState = BatchAiState(),
    val batchPointGridState: BatchPointGridState = BatchPointGridState(),
)

private data class NetworkSettingsState(
    val proxy: ProxySettings,
    val hasAccessToken: Boolean,
)

private data class CatalogContent(
    val subjects: List<Subject> = emptyList(),
    val availableYears: List<Int> = emptyList(),
)

private data class PagedCatalogContent(
    val filters: CatalogFilters,
    val subjects: List<Subject>,
    val availableYears: List<Int>,
    val currentPage: Int,
    val totalPages: Int,
    val totalSubjects: Int,
)

private data class RandomPicks(
    val minimumScore: Double,
    val subjects: List<Subject>,
)

private data class SelectedSubjectState(
    val subject: Subject?,
    val loading: Boolean,
)

data class DetailUiState(
    val subject: Subject? = null,
    val loading: Boolean = false,
    val analysis: AiAnalysis? = null,
    val analyzing: Boolean = false,
    val collectionUpdating: Boolean = false,
    val detailMessage: String? = null,
)

data class BatchAiState(
    val running: Boolean = false,
    val done: Int = 0,
    val total: Int = 0,
    val success: Int = 0,
    val failed: Int = 0,
    val currentTitle: String = "",
)

data class BatchPointGridState(
    val running: Boolean = false,
    val done: Int = 0,
    val total: Int = 0,
    val success: Int = 0,
    val failed: Int = 0,
    val skipped: Int = 0,
    val currentTitle: String = "",
    val scopeLabel: String = "",
)

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModel(
    private val repository: CatalogRepository,
) : ViewModel() {
    private val storedNsfwEnabled = repository.isNsfwEnabled()
    private val filters = MutableStateFlow(
        CatalogFilters(includeNsfw = false),
    )
    private val syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    private val requestedPage = MutableStateFlow(1)
    private val selectedSubjectKey = MutableStateFlow<SubjectKey?>(null)
    private val detailLoading = MutableStateFlow(false)
    private val randomPicks = MutableStateFlow<RandomPicks?>(null)
    private val blockWords = MutableStateFlow(repository.getBlockWords())
    private val seedInfo = MutableStateFlow(
        if (repository.isSeedNoticeDismissed()) null else repository.getSeedInfo(),
    )
    private val themeMode = MutableStateFlow(repository.getThemeMode())
    private val nsfwNoticeDismissed = MutableStateFlow(repository.isNsfwNoticeDismissed())
    private val viewMode = MutableStateFlow(CatalogViewMode.LIST)
    private val aiSettings = MutableStateFlow(repository.getAiSettings())
    private val aiAnalyses = repository.observeAiAnalyses().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )
    private val analyzingSubjectKey = MutableStateFlow<SubjectKey?>(null)
    private val collectionUpdatingKey = MutableStateFlow<SubjectKey?>(null)
    private val detailMessage = MutableStateFlow<String?>(null)
    private val batchAiState = MutableStateFlow(BatchAiState())
    private val batchPointGridState = MutableStateFlow(BatchPointGridState())
    private val blockedSubjects = repository.observeBlockedSubjects().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )
    private val networkSettings = MutableStateFlow(
        NetworkSettingsState(repository.getProxySettings(), hasAccessToken = false),
    )
    private var detailRefreshJob: Job? = null
    private var catalogRefreshJob: Job? = null
    private var nsfwPreferenceJob: Job? = null
    private var requestedNsfwEnabled = filters.value.includeNsfw
    private var proxySaveJob: Job? = null
    private var accessTokenSaveJob: Job? = null
    private var searchJob: Job? = null
    private var batchAiJob: Job? = null
    private var batchPointGridJob: Job? = null
    private var queuedRefreshMode: SyncMode? = null
    private var credentialChangesInProgress = 0
    private val credentialSettingsMutex = Mutex()

    private val catalogContent: Flow<CatalogContent> = combine(filters, blockWords) { active, words ->
        active to words
    }.flatMapLatest { (activeFilters, words) ->
        combine(
            repository.observeSubjects(activeFilters.type).conflate(),
            aiAnalyses,
        ) { subjects, analyses ->
            val analysesByKey = analyses.associateBy { it.cacheKey }
            val withAnalysis = subjects.map { it.withAiAnalysis(analysesByKey[it.key.cacheKey]) }
            CatalogContent(
                subjects = filterAndSortSubjects(withAnalysis, activeFilters, words),
                availableYears = subjects.asSequence()
                    .filter { !it.isBlocked && !matchesAnyBlockWord(it, words) }
                    .filter { activeFilters.includeNsfw || !it.nsfw }
                    .mapNotNull { it.date.take(4).toIntOrNull() }
                    .distinct()
                    .sortedDescending()
                    .toList(),
            )
        }
            .flowOn(Dispatchers.Default)
    }

    private val pagedCatalogContent = combine(
        filters,
        catalogContent,
        requestedPage,
    ) { activeFilters, content, page ->
        val subjectPage = paginateSubjects(content.subjects, page, PAGE_SIZE)
        PagedCatalogContent(
            filters = activeFilters,
            subjects = subjectPage.items,
            availableYears = content.availableYears,
            currentPage = subjectPage.currentPage,
            totalPages = subjectPage.totalPages,
            totalSubjects = subjectPage.totalItems,
        )
    }

    private val baseUiState: Flow<CatalogUiState> = combine(
        pagedCatalogContent,
        syncState,
        selectedSubjectKey,
        networkSettings,
        aiSettings,
    ) { content, sync, selectedKey, network, ai ->
        CatalogUiState(
            filters = content.filters,
            subjects = content.subjects,
            availableYears = content.availableYears,
            currentPage = content.currentPage,
            totalPages = content.totalPages,
            totalSubjects = content.totalSubjects,
            syncState = sync,
            selectedSubjectKey = selectedKey,
            proxySettings = network.proxy,
            hasAccessToken = network.hasAccessToken,
            aiSettings = ai,
        )
    }

    val uiState: StateFlow<CatalogUiState> = combine(
        baseUiState,
        blockedSubjects,
        randomPicks,
        blockWords,
        seedInfo,
    ) { base, blocked, picks, words, seed ->
        base.copy(
            blockedSubjects = blocked,
            blockWords = words,
            seedInfo = seed,
            randomMinimumScore = picks?.minimumScore,
            randomPicks = picks?.subjects.orEmpty(),
        )
    }.combine(themeMode) { state, theme ->
        state.copy(themeMode = theme)
    }.combine(nsfwNoticeDismissed) { state, dismissed ->
        state.copy(nsfwNoticeDismissed = dismissed)
    }.combine(viewMode) { state, mode ->
        state.copy(viewMode = mode)
    }.combine(batchAiState) { state, ai ->
        state.copy(batchAiState = ai)
    }.combine(batchPointGridState) { state, grid ->
        state.copy(batchPointGridState = grid)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CatalogUiState(),
    )

    private val selectedSubject: Flow<Subject?> = selectedSubjectKey.flatMapLatest { key ->
        if (key == null) flowOf(null) else repository.observeSubject(key)
    }

    private val selectedSubjectState: Flow<SelectedSubjectState> = combine(
        selectedSubject,
        detailLoading,
        filters,
    ) { subject, loading, activeFilters ->
        SelectedSubjectState(
            subject = subject?.takeIf { activeFilters.includeNsfw || !it.nsfw },
            loading = loading,
        )
    }

    val detailState: StateFlow<DetailUiState> = combine(
        selectedSubjectState,
        aiAnalyses,
        analyzingSubjectKey,
        collectionUpdatingKey,
        detailMessage,
    ) { state, analyses, analyzingKey, updatingKey, message ->
        val visibleSubject = state.subject
        val analysis = visibleSubject?.let {
            analyses.firstOrNull { item ->
                item.subjectId == it.id && item.catalogType == it.type
            }
        }
        DetailUiState(
            subject = visibleSubject,
            loading = state.loading,
            analysis = analysis,
            analyzing = visibleSubject != null && analyzingKey == visibleSubject.key,
            collectionUpdating = visibleSubject != null && updatingKey == visibleSubject.key,
            detailMessage = message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DetailUiState(),
    )

    init {
        Log.d(TAG_RESTORE, "viewmodel init, snapshot=" + repository.loadUiSnapshot())
        restoreStoredUiState()
        viewModelScope.launch {
            val hasAccessToken = withContext(Dispatchers.IO) {
                repository.hasAccessToken()
            }
            networkSettings.update { it.copy(hasAccessToken = hasAccessToken) }
            if (storedNsfwEnabled && hasAccessToken) {
                filters.update { it.copy(includeNsfw = true) }
                requestedNsfwEnabled = true
            }
            val importedSeed = withContext(Dispatchers.IO) {
                runCatching { repository.importSeedIfNeeded() }.getOrNull()
            }
            seedInfo.value = if (repository.isSeedNoticeDismissed()) {
                null
            } else {
                importedSeed ?: repository.getSeedInfo()
            }
            val startupSettingsReady = if (storedNsfwEnabled && !hasAccessToken) {
                val repaired = credentialSettingsMutex.withLock {
                    repository.setNsfwEnabled(false)
                }
                if (!repaired) {
                    syncState.value = SyncState.Failed("无法修复 NSFW 设置，请检查设备存储")
                }
                repaired
            } else {
                true
            }
            if (startupSettingsReady && repository.shouldRefresh()) refresh()
        }
        viewModelScope.launch {
            combine(selectedSubject, filters) { subject, activeFilters ->
                subject to activeFilters.includeNsfw
            }.collect { (subject, includeNsfw) ->
                val selectedKey = selectedSubjectKey.value
                if (subject == null && selectedKey != null) {
                    Log.d(TAG_RESTORE, "clearing key=$selectedKey reason=subjectNull")
                    repository.debugMarker("clear_reason", "subjectNull:$selectedKey")
                    repository.debugLog("clear: subjectNull:$selectedKey")
                    detailRefreshJob?.cancel()
                    detailLoading.value = false
                    selectedSubjectKey.value = null
                    syncState.value = SyncState.Failed("该条目已不在当前目录中")
                } else if (subject?.nsfw == true && !includeNsfw) {
                    Log.d(TAG_RESTORE, "clearing key=$selectedKey reason=nsfwHidden")
                    repository.debugMarker("clear_reason", "nsfwHidden:$selectedKey")
                    repository.debugLog("clear: nsfwHidden:$selectedKey")
                    detailRefreshJob?.cancel()
                    detailLoading.value = false
                    selectedSubjectKey.value = null
                    syncState.value = SyncState.Failed("该条目被标记为 NSFW，已按当前设置隐藏")
                }
            }
        }
        viewModelScope.launch {
            uiState.collect { state ->
                repository.debugLog("save: detail=${state.selectedSubjectKey?.cacheKey ?: "none"}")
                repository.saveUiSnapshot(
                    UiSnapshot(
                        typeName = state.filters.type.name,
                        query = state.filters.query,
                        sortName = state.filters.sort.name,
                        minimumVotes = state.filters.minimumVotes,
                        yearOrZero = state.filters.year ?: 0,
                        favoritesOnly = state.filters.favoritesOnly,
                        nsfwOnly = state.filters.nsfwOnly,
                        formatName = state.filters.format?.name,
                        aiCategoryName = state.filters.aiCategory?.name,
                        winLoseName = state.filters.winLose?.name,
                        page = state.currentPage,
                        viewModeName = state.viewMode.name,
                        detailKey = state.selectedSubjectKey?.cacheKey,
                    ),
                )
            }
        }
    }

    private fun restoreStoredUiState() {
        val snapshot = repository.loadUiSnapshot() ?: return
        Log.d(TAG_RESTORE, "restoring snapshot=$snapshot")
        repository.debugMarker("restore_applied", snapshot.detailKey ?: "no-detail")
        repository.debugLog("restore: detail=${snapshot.detailKey ?: "none"}")
        val type = snapshot.typeName?.let { name ->
            runCatching { CatalogType.valueOf(name) }.getOrNull()
        } ?: CatalogType.ANIME
        val sort = snapshot.sortName?.let { name ->
            runCatching { SortOption.valueOf(name) }.getOrNull()
        } ?: SortOption.SCORE
        val restoredViewMode = snapshot.viewModeName?.let { name ->
            runCatching { CatalogViewMode.valueOf(name) }.getOrNull()
        } ?: CatalogViewMode.LIST
        val includeNsfw = filters.value.includeNsfw
        filters.value = CatalogFilters(
            type = type,
            query = snapshot.query,
            sort = sort,
            minimumVotes = snapshot.minimumVotes,
            year = snapshot.yearOrZero.takeIf { it > 0 },
            favoritesOnly = snapshot.favoritesOnly,
            includeNsfw = includeNsfw,
            nsfwOnly = snapshot.nsfwOnly && includeNsfw,
            format = snapshot.formatName?.let { name ->
                runCatching { SubjectFormat.valueOf(name) }.getOrNull()
            },
            aiCategory = snapshot.aiCategoryName?.let { name ->
                runCatching { AiYuriCategory.valueOf(name) }.getOrNull()
            },
            winLose = snapshot.winLoseName?.let { name ->
                runCatching { WinLose.valueOf(name) }.getOrNull()
            },
        )
        requestedPage.value = snapshot.page.coerceAtLeast(1)
        viewMode.value = restoredViewMode
        snapshot.detailKey?.let { key ->
            val parts = key.split(":", limit = 2)
            val detailType = parts.getOrNull(0)?.let { name ->
                runCatching { CatalogType.valueOf(name) }.getOrNull()
            }
            val detailId = parts.getOrNull(1)?.toIntOrNull()
            if (detailType != null && detailId != null) {
                openSubjectKey(SubjectKey(detailId, detailType))
            }
        }
    }

    fun selectType(type: CatalogType) {
        requestedPage.value = 1
        filters.update { it.copy(type = type) }
    }

    fun setSearchQuery(query: String) {
        requestedPage.value = 1
        filters.update { it.copy(query = query) }
    }

    fun searchOnline() {
        val query = filters.value.query.trim()
        if (query.isEmpty()) return
        if (searchJob?.isActive == true) return
        searchJob = viewModelScope.launch {
            syncState.value = SyncState.Loading()
            try {
                val result = repository.searchOnline(
                    query = query,
                    includeNsfw = filters.value.includeNsfw,
                    type = filters.value.type,
                )
                syncState.value = SyncState.Success(result.message)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                syncState.value = SyncState.Failed(error.toUserMessage())
            }
        }
    }

    fun setSort(sort: SortOption) {
        requestedPage.value = 1
        filters.update { it.copy(sort = sort) }
    }

    fun setMinimumVotes(minimumVotes: Int) {
        requestedPage.value = 1
        filters.update { it.copy(minimumVotes = minimumVotes) }
    }

    fun setYear(year: Int?) {
        requestedPage.value = 1
        filters.update { it.copy(year = year) }
    }

    fun toggleNsfwOnly() {
        if (!filters.value.includeNsfw) return
        requestedPage.value = 1
        filters.update { it.copy(nsfwOnly = !it.nsfwOnly) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            if (repository.saveThemeMode(mode)) {
                themeMode.value = mode
            } else {
                syncState.value = SyncState.Failed("无法保存主题设置，请检查设备存储")
            }
        }
    }

    fun saveAiSettings(settings: AiSettings, apiKey: String?, removeApiKey: Boolean) {
        viewModelScope.launch {
            if (repository.saveAiSettings(settings, apiKey, removeApiKey)) {
                aiSettings.value = repository.getAiSettings()
                syncState.value = SyncState.Success("AI 分析设置已保存")
            } else {
                syncState.value = SyncState.Failed("无法保存 AI 设置，请检查设备存储")
            }
        }
    }

    fun toggleViewMode() {
        viewMode.value = if (viewMode.value == CatalogViewMode.LIST) {
            CatalogViewMode.GRID
        } else {
            CatalogViewMode.LIST
        }
    }

    fun toggleFavoritesOnly() {
        requestedPage.value = 1
        filters.update { it.copy(favoritesOnly = !it.favoritesOnly) }
    }

    fun setAiCategory(category: AiYuriCategory?) {
        requestedPage.value = 1
        filters.update { it.copy(aiCategory = category, winLose = null) }
    }

    fun setWinLoseFilter(winLose: WinLose?) {
        requestedPage.value = 1
        filters.update { it.copy(winLose = winLose, aiCategory = null) }
    }

    fun setFormat(format: SubjectFormat?) {
        requestedPage.value = 1
        filters.update { it.copy(format = format) }
    }

    fun selectPage(page: Int) {
        requestedPage.value = page.coerceAtLeast(1)
    }

    fun setNsfwEnabled(enabled: Boolean) {
        if (enabled && !networkSettings.value.hasAccessToken) {
            syncState.value = SyncState.Failed("查询 NSFW 内容需要先在“设置”中配置 Access Token")
            return
        }
        requestedNsfwEnabled = enabled
        if (nsfwPreferenceJob?.isActive == true) return
        if (filters.value.includeNsfw == enabled) return

        beginCredentialChange()
        nsfwPreferenceJob = viewModelScope.launch {
            try {
                while (filters.value.includeNsfw != requestedNsfwEnabled) {
                    val target = requestedNsfwEnabled
                    if (!applyNsfwSetting(target)) {
                        requestedNsfwEnabled = filters.value.includeNsfw
                        break
                    }
                }
            } finally {
                endCredentialChange()
            }
        }
    }

    private suspend fun applyNsfwSetting(enabled: Boolean): Boolean =
        credentialSettingsMutex.withLock {
            if (enabled && !repository.hasAccessToken()) {
                syncState.value = SyncState.Failed("查询 NSFW 内容需要有效的 Access Token")
                return@withLock false
            }
            if (!repository.setNsfwEnabled(enabled)) {
                syncState.value = SyncState.Failed("无法保存 NSFW 设置，请检查设备存储")
                return@withLock false
            }
            filters.update {
                it.copy(
                    includeNsfw = enabled,
                    nsfwOnly = enabled && it.nsfwOnly,
                )
            }
            requestedPage.value = 1
            syncState.value = SyncState.Success(
                if (enabled) {
                    "NSFW 已开启，所有请求将使用你的 Access Token"
                } else {
                    "NSFW 已关闭，已切换到 Bangumi 官方公开 API"
                },
            )
            true
        }

    private fun beginCredentialChange() {
        credentialChangesInProgress += 1
        catalogRefreshJob?.cancel()
    }

    private fun endCredentialChange() {
        credentialChangesInProgress = (credentialChangesInProgress - 1).coerceAtLeast(0)
        if (credentialChangesInProgress > 0) return
        val queued = queuedRefreshMode
        queuedRefreshMode = null
        if (queued != null) refresh(queued)
    }

    private fun queueRefresh(mode: SyncMode) {
        if (mode == SyncMode.FORCE_UPDATE || queuedRefreshMode == null) {
            queuedRefreshMode = mode
        }
    }

    fun saveProxySettings(settings: ProxySettings) {
        if (proxySaveJob?.isActive == true) return
        if (!settings.isValid) {
            syncState.value = SyncState.Failed("代理地址或端口无效")
            return
        }
        proxySaveJob = viewModelScope.launch {
            if (!repository.saveProxySettings(settings)) {
                syncState.value = SyncState.Failed("无法保存代理设置，请检查设备存储")
                return@launch
            }
            networkSettings.update { it.copy(proxy = repository.getProxySettings()) }
            syncState.value = SyncState.Success("代理已切换为：${networkSettings.value.proxy.summary}")
        }
    }

    fun saveAccessToken(token: String?) {
        if (accessTokenSaveJob?.isActive == true) return
        beginCredentialChange()
        accessTokenSaveJob = viewModelScope.launch {
            try {
                credentialSettingsMutex.withLock {
                    val removingToken = token.isNullOrBlank()
                    if (removingToken && filters.value.includeNsfw) {
                        if (!repository.setNsfwEnabled(false)) {
                            syncState.value = SyncState.Failed(
                                "无法关闭 NSFW，Access Token 未移除",
                            )
                            return@withLock
                        }
                        filters.update { it.copy(includeNsfw = false, nsfwOnly = false) }
                        requestedNsfwEnabled = false
                        requestedPage.value = 1
                    }
                    if (!repository.saveAccessToken(token)) {
                        syncState.value = SyncState.Failed("无法保存 Access Token，请检查设备安全存储")
                        return@withLock
                    }
                    val hasToken = repository.hasAccessToken()
                    networkSettings.update { it.copy(hasAccessToken = hasToken) }
                    syncState.value = SyncState.Success(
                        if (hasToken) "Access Token 已加密保存" else "Access Token 已移除",
                    )
                }
            } finally {
                endCredentialChange()
            }
        }
    }

    fun dismissSyncMessage() {
        if (syncState.value is SyncState.Success || syncState.value is SyncState.Failed) {
            syncState.value = SyncState.Idle
        }
    }

    fun refresh() = refresh(SyncMode.ADD_MISSING)

    fun forceRefresh() = refresh(SyncMode.FORCE_UPDATE)

    private fun refresh(mode: SyncMode) {
        if (credentialChangesInProgress > 0) {
            queueRefresh(mode)
            return
        }
        if (catalogRefreshJob?.isActive == true) {
            queueRefresh(mode)
            return
        }
        syncState.value = SyncState.Loading()
        val job = viewModelScope.launch {
            try {
                val includeNsfw = credentialSettingsMutex.withLock {
                    filters.value.includeNsfw
                }
                val summary = repository.refreshAll(
                    includeNsfw = includeNsfw,
                    mode = mode,
                    onProgress = { progress -> syncState.value = SyncState.Loading(progress) },
                )
                val action = when (summary.mode) {
                    SyncMode.ADD_MISSING -> "新增 ${summary.written} 个"
                    SyncMode.FORCE_UPDATE -> "重新写入 ${summary.written} 个"
                }
                val permissionNote = if (summary.nsfwRequested) {
                    "；已使用个人 Token 查询 NSFW"
                } else {
                    ""
                }
                val completenessNote = if (summary.complete) {
                    ""
                } else {
                    "；官方搜索部分结果达到 1000 条上限，已保留现有目录"
                }
                val shardNote = if (summary.shardedPartitions > 0) {
                    "；已对 ${summary.shardedPartitions} 个分区使用年份分片同步"
                } else {
                    ""
                }
                syncState.value = SyncState.Success(
                    "$action，扫描到 ${summary.discovered} 个“百合/轻百合”条目" +
                        permissionNote + completenessNote + shardNote,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                syncState.value = SyncState.Failed(error.toUserMessage())
            }
        }
        catalogRefreshJob = job
        job.invokeOnCompletion {
            viewModelScope.launch {
                if (catalogRefreshJob === job) catalogRefreshJob = null
                if (credentialChangesInProgress == 0) queuedRefreshMode?.let { queuedMode ->
                    queuedRefreshMode = null
                    refresh(queuedMode)
                }
            }
        }
    }

    fun openSubject(subject: Subject) {
        openSubjectKey(subject.key)
    }

    fun openSubjectKey(key: SubjectKey) {
        Log.d(TAG_RESTORE, "open key=$key")
        repository.debugMarker("last_open", key.cacheKey)
        repository.debugLog("open: $key")
        selectedSubjectKey.value = key
        detailRefreshJob?.cancel()
        detailRefreshJob = viewModelScope.launch {
            detailLoading.value = true
            try {
                val warning = credentialSettingsMutex.withLock {
                    repository.refreshDetail(key)
                }
                if (warning != null) detailMessage.value = warning
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                syncState.value = SyncState.Failed(error.toUserMessage())
            } finally {
                if (selectedSubjectKey.value == key) detailLoading.value = false
            }
        }
    }

    fun closeSubject() {
        repository.debugLog("close: ${selectedSubjectKey.value?.cacheKey ?: "none"}")
        detailRefreshJob?.cancel()
        detailLoading.value = false
        selectedSubjectKey.value = null
    }

    fun toggleFavorite(subject: Subject) {
        viewModelScope.launch {
            repository.setFavorite(subject.key, !subject.isFavorite)
        }
    }

    fun toggleBlocked(subject: Subject) {
        viewModelScope.launch {
            repository.setBlocked(subject.key, !subject.isBlocked)
        }
    }

    fun toggleWinLose(subject: Subject, winLose: WinLose) {
        viewModelScope.launch {
            repository.setWinLose(
                subject.key,
                if (subject.winLose == winLose) null else winLose,
            )
        }
    }

    fun analyzeSubject(subject: Subject) {
        if (analyzingSubjectKey.value != null) return
        detailMessage.value = null
        analyzingSubjectKey.value = subject.key
        viewModelScope.launch {
            try {
                repository.analyzeSubject(subject)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                detailMessage.value = error.toUserMessage()
            } finally {
                analyzingSubjectKey.value = null
            }
        }
    }

    fun startBatchAiAnalysis(subjects: List<Subject>) {
        if (batchAiState.value.running || subjects.isEmpty()) return
        batchAiJob?.cancel()
        batchAiJob = viewModelScope.launch {
            val total = subjects.size
            var success = 0
            var failed = 0
            batchAiState.value = BatchAiState(running = true, total = total)
            subjects.forEachIndexed { index, subject ->
                batchAiState.value = batchAiState.value.copy(
                    done = index,
                    currentTitle = subject.displayName,
                )
                try {
                    repository.analyzeSubject(subject)
                    success += 1
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Throwable) {
                    failed += 1
                }
                if (index < total - 1) delay(BATCH_AI_GAP_MS)
            }
            batchAiState.value = BatchAiState(
                running = false,
                done = total,
                total = total,
                success = success,
                failed = failed,
            )
        }
    }

    fun cancelBatchAiAnalysis() {
        batchAiJob?.cancel()
        batchAiState.value = BatchAiState()
    }

    fun dismissBatchAiResult() {
        if (!batchAiState.value.running) batchAiState.value = BatchAiState()
    }

    fun updateBangumiCollection(subject: Subject, type: BangumiCollectionType) {
        if (collectionUpdatingKey.value != null) return
        detailMessage.value = null
        collectionUpdatingKey.value = subject.key
        viewModelScope.launch {
            try {
                repository.updateBangumiCollection(subject.key, type)
                detailMessage.value = "已在 Bangumi 点格子更新为“${type.label}”"
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                detailMessage.value = error.toUserMessage()
            } finally {
                collectionUpdatingKey.value = null
            }
        }
    }

    fun startBatchPointGrid(
        type: BangumiCollectionType,
        scopeLabel: String,
        allEntries: Boolean,
    ) {
        if (batchPointGridState.value.running) return
        batchPointGridJob?.cancel()
        batchPointGridJob = viewModelScope.launch {
            val includeNsfw = filters.value.includeNsfw
            val currentType = filters.value.type
            val subjects = if (allEntries) {
                withContext(Dispatchers.Default) {
                    repository.getAllCatalogMembers()
                        .filter { it.type == currentType && (includeNsfw || !it.nsfw) }
                }
            } else {
                uiState.value.subjects
            }
            val targets = subjects.filter { it.bangumiCollectionType != type }
            val skipped = subjects.size - targets.size
            val total = targets.size
            if (total == 0) {
                batchPointGridState.value = BatchPointGridState(
                    running = false,
                    total = 0,
                    skipped = skipped,
                    scopeLabel = scopeLabel,
                )
                return@launch
            }
            var success = 0
            var failed = 0
            batchPointGridState.value = BatchPointGridState(
                running = true,
                total = total,
                skipped = skipped,
                scopeLabel = scopeLabel,
            )
            targets.forEachIndexed { index, subject ->
                batchPointGridState.value = batchPointGridState.value.copy(
                    done = index,
                    currentTitle = subject.displayName,
                )
                try {
                    repository.updateBangumiCollection(subject.key, type)
                    success += 1
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Throwable) {
                    failed += 1
                }
                if (index < total - 1) delay(BATCH_POINT_GRID_GAP_MS)
            }
            batchPointGridState.value = batchPointGridState.value.copy(
                running = false,
                done = total,
                success = success,
                failed = failed,
            )
        }
    }

    fun cancelBatchPointGrid() {
        batchPointGridJob?.cancel()
        batchPointGridState.value = BatchPointGridState()
    }

    fun dismissBatchPointGridResult() {
        if (!batchPointGridState.value.running) {
            batchPointGridState.value = BatchPointGridState()
        }
    }

    fun dismissDetailMessage() {
        detailMessage.value = null
    }

    fun deleteSubject(subject: Subject) {
        viewModelScope.launch {
            repository.deleteSubject(subject.key)
            if (selectedSubjectKey.value == subject.key) closeSubject()
        }
    }

    fun addBlockWord(word: String) {
        val trimmed = word.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val updated = (blockWords.value + trimmed).distinct()
            if (repository.saveBlockWords(updated)) {
                blockWords.value = updated
            } else {
                syncState.value = SyncState.Failed("无法保存屏蔽词，请检查设备存储")
            }
        }
    }

    fun removeBlockWord(word: String) {
        viewModelScope.launch {
            val updated = blockWords.value - word
            if (repository.saveBlockWords(updated)) {
                blockWords.value = updated
            } else {
                syncState.value = SyncState.Failed("无法保存屏蔽词，请检查设备存储")
            }
        }
    }

    fun rollRandom(minimumScore: Double) {
        viewModelScope.launch {
            val includeNsfw = filters.value.includeNsfw
            val pool = withContext(Dispatchers.Default) {
                repository.getAllCatalogMembers().filter { includeNsfw || !it.nsfw }
            }
            randomPicks.value = RandomPicks(
                minimumScore = minimumScore,
                subjects = pickRandomSubjects(
                    subjects = pool,
                    minimumScore = minimumScore,
                    count = RANDOM_PICK_COUNT,
                    blockWords = blockWords.value,
                ),
            )
        }
    }

    fun clearRandomPicks() {
        randomPicks.value = null
    }

    fun dismissSeedInfo() {
        viewModelScope.launch {
            if (repository.dismissSeedNotice()) {
                seedInfo.value = null
            }
        }
    }

    fun dismissNsfwNotice() {
        viewModelScope.launch {
            if (repository.dismissNsfwNotice()) {
                nsfwNoticeDismissed.value = true
            }
        }
    }

    private fun Throwable.toUserMessage(): String = when (this) {
        is MissingUserAgentException -> "尚未配置 Bangumi User-Agent，请按 README 设置后重新构建"
        is MissingAccessTokenException -> message ?: "NSFW 模式需要有效的 Bangumi Access Token"
        is MissingAiApiKeyException -> "尚未配置 AI API Key，请在“设置 → AI 雷点分析”中配置"
        is AiRequestException -> when (statusCode) {
            400 -> "AI 请求被拒绝，请检查接口地址、模型名或提示词"
            401, 403 -> "AI API Key 无效或没有权限"
            404 -> "AI 接口地址或模型不存在，请检查配置"
            429 -> "AI 请求过于频繁，请稍后再试"
            else -> message ?: "AI 服务返回错误"
        }
        is IOException -> "无法连接 Bangumi，请检查网络后重试"
        is HttpException -> when (code()) {
            400, 422 -> "Bangumi 暂不接受仅标签查询，需要调整搜索兼容策略"
            401 -> "Bangumi Access Token 无效或已过期"
            403 -> "Bangumi Access Token 缺少写入收藏的权限，请在官网重新生成 Token 并勾选“写入收藏”"
            404 -> "Bangumi 未找到该条目或你的收藏状态"
            405 -> "Bangumi 接口不支持该操作，请更新应用"
            415 -> "Bangumi 不接受该请求格式，请更新应用"
            429 -> "请求过于频繁，请稍后再试"
            else -> "Bangumi 接口返回错误（HTTP ${code()}）"
        }
        else -> message ?: "同步失败"
    }

    companion object {
        private const val PAGE_SIZE = 20
        private const val RANDOM_PICK_COUNT = 5
        private const val BATCH_AI_GAP_MS = 800L
        private const val BATCH_POINT_GRID_GAP_MS = 1_200L
        private const val TAG_RESTORE = "YuriShelfRestore"

        fun factory(repository: CatalogRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { CatalogViewModel(repository) }
        }
    }
}
