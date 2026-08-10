package com.yurishelf.app.data

import android.content.SharedPreferences
import androidx.core.content.edit
import com.yurishelf.app.data.ai.AiAnalysisResult
import com.yurishelf.app.data.ai.AiSettings
import com.yurishelf.app.data.ai.AiSettingsStore
import com.yurishelf.app.data.ai.MissingAiApiKeyException
import com.yurishelf.app.data.ai.OpenAiClient
import com.yurishelf.app.data.ai.toAiContext
import com.yurishelf.app.data.local.SubjectDao
import com.yurishelf.app.data.local.AiAnalysisEntity
import com.yurishelf.app.data.remote.AccessTokenStore
import com.yurishelf.app.data.remote.BangumiApi
import com.yurishelf.app.data.remote.MissingAccessTokenException
import com.yurishelf.app.data.remote.ProxySettings
import com.yurishelf.app.data.remote.ProxySettingsStore
import com.yurishelf.app.data.remote.SearchFilter
import com.yurishelf.app.data.remote.SearchSubjectsRequest
import com.yurishelf.app.data.remote.UpdateCollectionRequest
import com.yurishelf.app.data.remote.authorizationForMode
import com.yurishelf.app.domain.AiAnalysis
import com.yurishelf.app.domain.BangumiCollectionType
import com.yurishelf.app.domain.CatalogType
import com.yurishelf.app.domain.Subject
import com.yurishelf.app.domain.SubjectKey
import com.yurishelf.app.domain.ThemeMode
import com.yurishelf.app.domain.WinLose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.HttpException

data class SyncSummary(
    val discovered: Int,
    val written: Int,
    val rejectedByRules: Int,
    val nsfwRequested: Boolean,
    val complete: Boolean,
    val mode: SyncMode,
    val shardedPartitions: Int = 0,
)

enum class SyncMode {
    ADD_MISSING,
    FORCE_UPDATE,
}

data class SyncProgress(
    val completedPages: Int,
    val totalPages: Int,
    val label: String,
) {
    val fraction: Float
        get() = if (totalPages <= 0) 0f else (completedPages.toFloat() / totalPages).coerceIn(0f, 1f)
}

data class OnlineSearchResult(
    val importedCount: Int,
    val message: String,
)

private data class QuerySyncResult(
    val acceptedIds: Set<Int>,
    val writtenIds: Set<Int>,
    val rejectedByRules: Int,
    val complete: Boolean,
    val truncated: Boolean = false,
)

private data class CatalogTypeSyncResult(
    val type: CatalogType,
    val acceptedIds: Set<Int>,
    val writtenIds: Set<Int>,
    val rejectedByRules: Int,
    val complete: Boolean,
    val sharded: Boolean = false,
)

class CatalogRepository(
    private val api: BangumiApi,
    private val dao: SubjectDao,
    private val preferences: SharedPreferences,
    private val proxySettingsStore: ProxySettingsStore,
    private val accessTokenStore: AccessTokenStore,
    private val blockWordStore: BlockWordStore,
    private val seedCatalogImporter: SeedCatalogImporter,
    private val aiSettingsStore: AiSettingsStore,
    private val aiClient: OpenAiClient,
) {
    private val syncMutex = Mutex()

    fun observeSubjects(type: CatalogType): Flow<List<Subject>> =
        dao.observeByCatalogType(type.name).map { entities -> entities.map { it.toDomain() } }

    fun observeSubject(key: SubjectKey): Flow<Subject?> =
        dao.observeByKey(key.id, key.catalogType.name).map { it?.toDomain() }

    fun observeBlockedSubjects(): Flow<List<Subject>> =
        dao.observeBlocked().map { entities -> entities.map { it.toDomain() } }

    fun observeAiAnalyses(): Flow<List<AiAnalysis>> =
        dao.observeAllAnalyses().map { entities -> entities.map { it.toDomain() } }

    fun getBlockWords(): List<String> = blockWordStore.get()

    fun getSeedInfo(): SeedInfo? = seedCatalogImporter.readSeedInfo()

    suspend fun importSeedIfNeeded(): SeedInfo? = seedCatalogImporter.importIfNeeded()

    fun isSeedNoticeDismissed(): Boolean = preferences.getBoolean(KEY_SEED_NOTICE_DISMISSED, false)

    suspend fun dismissSeedNotice(): Boolean = withContext(Dispatchers.IO) {
        preferences.edit().putBoolean(KEY_SEED_NOTICE_DISMISSED, true).commit()
    }

    fun isNsfwNoticeDismissed(): Boolean = preferences.getBoolean(KEY_NSFW_NOTICE_DISMISSED, false)

    suspend fun dismissNsfwNotice(): Boolean = withContext(Dispatchers.IO) {
        preferences.edit().putBoolean(KEY_NSFW_NOTICE_DISMISSED, true).commit()
    }

    suspend fun saveBlockWords(words: List<String>): Boolean = blockWordStore.save(words)

    suspend fun deleteSubject(key: SubjectKey) {
        dao.deleteByKey(key.id, key.catalogType.name)
        dao.deleteAiAnalysis(key.id, key.catalogType.name)
        addTombstone(key.id, key.catalogType)
    }

    suspend fun searchOnline(
        query: String,
        includeNsfw: Boolean,
        type: CatalogType,
    ): OnlineSearchResult {
        val accessToken = withContext(Dispatchers.IO) { accessTokenStore.get() }
        val authorization = authorizationForMode(includeNsfw, accessToken)
        val referenceId = parseSubjectReference(query)
        val now = System.currentTimeMillis()
        return if (referenceId != null) {
            val dto = api.getSubject(referenceId, authorization)
            if (!dto.isYuriEntry()) {
                OnlineSearchResult(
                    importedCount = 0,
                    message = "该条目没有“百合/轻百合”标签，未导入",
                )
            } else {
                val catalogType = catalogTypeForSubject(dto, preferred = type)
                val existing = dao.getByKey(dto.id, catalogType.name)
                dao.upsertCatalogPagePreservingFavorites(
                    listOf(
                        dto.toEntity(
                            existing = existing,
                            now = now,
                            isDetail = false,
                            catalogType = catalogType,
                            nsfwFallback = includeNsfw,
                        ),
                    ),
                )
                OnlineSearchResult(
                    importedCount = 1,
                    message = "已导入：${dto.nameCn.orEmpty().ifBlank { dto.name }}",
                )
            }
        } else {
            val page = api.searchSubjects(
                limit = ONLINE_SEARCH_LIMIT,
                offset = 0,
                authorization = authorization,
                request = SearchSubjectsRequest(
                    keyword = query,
                    sort = "match",
                    filter = SearchFilter(
                        type = listOf(type.apiValue),
                        tag = emptyList(),
                        metaTags = type.requiredMetaTags,
                        nsfw = includeNsfw,
                    ),
                ),
            )
            val valid = page.data.filter {
                it.isYuriEntry() && (includeNsfw || it.nsfw != true)
            }
            val existingById = if (valid.isEmpty()) {
                emptyMap()
            } else {
                dao.getByCatalogTypeAndIds(type.name, valid.map { it.id }).associateBy { it.id }
            }
            dao.upsertCatalogPagePreservingFavorites(
                valid.map {
                    it.toEntity(
                        existing = existingById[it.id],
                        now = now,
                        isDetail = false,
                        catalogType = type,
                        nsfwFallback = includeNsfw,
                    )
                },
            )
            OnlineSearchResult(
                importedCount = valid.size,
                message = "在线搜索“$query”完成：命中 ${valid.size} 条并写入本地",
            )
        }
    }

    private suspend fun addTombstone(subjectId: Int, type: CatalogType) =
        withContext(Dispatchers.IO) {
            val current = preferences.getStringSet(KEY_TOMBSTONES, emptySet())?.toMutableSet()
                ?: mutableSetOf()
            current.add("${type.name}:$subjectId")
            preferences.edit().putStringSet(KEY_TOMBSTONES, current).commit()
        }

    private fun isTombstoned(subjectId: Int, type: CatalogType): Boolean =
        preferences.getStringSet(KEY_TOMBSTONES, emptySet())
            ?.contains("${type.name}:$subjectId") == true

    suspend fun shouldRefresh(now: Long = System.currentTimeMillis()): Boolean {
        val lastAttempt = preferences.getLong(
            KEY_LAST_ATTEMPT,
            preferences.getLong(KEY_LAST_SYNC, 0),
        )
        return lastAttempt == 0L || now - lastAttempt >= CACHE_MAX_AGE_MS
    }

    fun isNsfwEnabled(): Boolean = preferences.getBoolean(KEY_NSFW_ENABLED, false)

    fun getThemeMode(): ThemeMode = runCatching {
        ThemeMode.valueOf(
            preferences.getString(KEY_THEME_MODE, null) ?: ThemeMode.SYSTEM.name,
        )
    }.getOrDefault(ThemeMode.SYSTEM)

    suspend fun saveThemeMode(mode: ThemeMode): Boolean = withContext(Dispatchers.IO) {
        preferences.edit().putString(KEY_THEME_MODE, mode.name).commit()
    }

    fun getProxySettings(): ProxySettings = proxySettingsStore.get()

    suspend fun saveProxySettings(settings: ProxySettings): Boolean = proxySettingsStore.save(settings)

    suspend fun hasAccessToken(): Boolean = withContext(Dispatchers.IO) {
        accessTokenStore.hasToken()
    }

    suspend fun saveAccessToken(token: String?): Boolean = accessTokenStore.save(token)

    suspend fun setNsfwEnabled(enabled: Boolean): Boolean = withContext(Dispatchers.IO) {
        val previous = preferences.getBoolean(KEY_NSFW_ENABLED, false)
        val committed = preferences.edit().putBoolean(KEY_NSFW_ENABLED, enabled).commit()
        if (!committed) {
            // commit() mutates the in-memory map before disk I/O; explicitly restore fail-closed state.
            preferences.edit().putBoolean(KEY_NSFW_ENABLED, previous).apply()
        }
        committed
    }

    suspend fun refreshAll(
        includeNsfw: Boolean,
        mode: SyncMode = SyncMode.ADD_MISSING,
        onProgress: (SyncProgress) -> Unit = {},
    ): SyncSummary = syncMutex.withLock {
        val accessToken = withContext(Dispatchers.IO) { accessTokenStore.get() }
        val authorization = authorizationForMode(includeNsfw, accessToken)
        val nsfwModes = if (includeNsfw) listOf(false, true) else listOf(false)
        val tracker = SyncProgressTracker(
            estimatedQueries = CatalogType.entries.size * nsfwModes.size * SOURCE_TAGS.size,
            callback = onProgress,
        )
        val results = coroutineScope {
            CatalogType.entries.map { type ->
                async {
                    refreshCatalogType(
                        type = type,
                        nsfwModes = nsfwModes,
                        authorization = authorization,
                        mode = mode,
                        progress = tracker,
                    )
                }
            }.awaitAll()
        }
        val acceptedKeys = buildSet {
            results.forEach { result ->
                result.acceptedIds.forEach { add("${result.type.name}:$it") }
            }
        }
        val writtenKeys = buildSet {
            results.forEach { result ->
                result.writtenIds.forEach { add("${result.type.name}:$it") }
            }
        }
        val complete = results.all(CatalogTypeSyncResult::complete)
        val now = System.currentTimeMillis()
        preferences.edit {
            putLong(KEY_LAST_ATTEMPT, now)
            if (complete) {
                putLong(KEY_LAST_SYNC, now)
                putBoolean(KEY_INITIAL_SYNC_COMPLETE, true)
            }
        }
        SyncSummary(
            discovered = acceptedKeys.size,
            written = writtenKeys.size,
            rejectedByRules = results.sumOf(CatalogTypeSyncResult::rejectedByRules),
            nsfwRequested = includeNsfw,
            complete = complete,
            mode = mode,
            shardedPartitions = results.count { it.sharded },
        )
    }

    suspend fun refreshDetail(key: SubjectKey): String? {
        val existing = dao.getByKey(key.id, key.catalogType.name)
        val accessToken = withContext(Dispatchers.IO) { accessTokenStore.get() }
        val authorization = authorizationForMode(isNsfwEnabled(), accessToken)
        val dto = api.getSubject(key.id, authorization)
        dao.upsertDetailPreservingLocalState(
            dto.toEntity(
                existing = existing,
                now = System.currentTimeMillis(),
                isDetail = true,
                catalogType = key.catalogType,
            ),
        )
        return syncBangumiCollectionState(key)
    }

    suspend fun setFavorite(key: SubjectKey, favorite: Boolean) {
        dao.setFavorite(key.id, key.catalogType.name, favorite)
    }

    suspend fun setBlocked(key: SubjectKey, blocked: Boolean) {
        dao.setBlocked(key.id, key.catalogType.name, blocked)
    }

    suspend fun setWinLose(key: SubjectKey, winLose: WinLose?) {
        dao.setWinLose(key.id, key.catalogType.name, winLose?.name)
    }

    fun getAiSettings(): AiSettings = aiSettingsStore.getSettings()

    fun loadUiSnapshot(): UiSnapshot? = preferences.loadUiSnapshot()

    suspend fun saveUiSnapshot(snapshot: UiSnapshot): Boolean = preferences.saveUiSnapshot(snapshot)


    suspend fun saveAiSettings(
        settings: AiSettings,
        apiKey: String?,
        removeApiKey: Boolean,
    ): Boolean {
        val fieldsSaved = aiSettingsStore.saveFields(settings)
        val keySaved = when {
            removeApiKey -> aiSettingsStore.saveApiKey(null)
            !apiKey.isNullOrBlank() -> aiSettingsStore.saveApiKey(apiKey)
            else -> true
        }
        return fieldsSaved && keySaved
    }

    suspend fun analyzeSubject(subject: Subject): AiAnalysis {
        val accessToken = withContext(Dispatchers.IO) { accessTokenStore.get() }
        val authorization = authorizationForMode(isNsfwEnabled(), accessToken)
        val dto = api.getSubject(subject.id, authorization)
        val settings = aiSettingsStore.getSettings()
        val apiKey = aiSettingsStore.getApiKey()?.trim().takeUnless { it.isNullOrEmpty() }
            ?: throw MissingAiApiKeyException()
        val result = aiClient.analyze(
            baseUrl = settings.baseUrl,
            apiKey = apiKey,
            model = settings.model,
            prompt = settings.prompt,
            webSearchEnabled = settings.webSearchEnabled,
            context = dto.toAiContext(),
        )
        val now = System.currentTimeMillis()
        val entity = result.toEntity(subject.id, subject.type, now)
        dao.upsertAiAnalyses(listOf(entity))
        return entity.toDomain()
    }

    suspend fun updateBangumiCollection(key: SubjectKey, type: BangumiCollectionType) {
        val accessToken = withContext(Dispatchers.IO) { accessTokenStore.get() }
        val token = accessToken?.trim().takeUnless { it.isNullOrEmpty() }
            ?: throw MissingAccessTokenException("Bangumi 点格子需要先在设置中配置 Access Token")
        val authorization = "Bearer $token"
        val request = UpdateCollectionRequest(type = type.apiValue)
        try {
            api.addCollection(key.id, authorization, request)
        } catch (error: HttpException) {
            if (error.code() == 404 || error.code() == 405) {
                api.updateCollection(key.id, authorization, request)
            } else {
                throw error
            }
        }
        dao.setBangumiCollection(
            key.id,
            key.catalogType.name,
            type.apiValue,
            System.currentTimeMillis(),
        )
    }

    private suspend fun syncBangumiCollectionState(key: SubjectKey): String? {
        val token = withContext(Dispatchers.IO) { accessTokenStore.get() }
            ?.trim()
            ?.takeUnless { it.isNullOrEmpty() }
            ?: return null
        return try {
            val authorization = "Bearer $token"
            val username = cachedUsername() ?: run {
                val fetched = api.getMe(authorization).username
                if (fetched.isBlank()) return null
                cacheUsername(fetched)
                fetched
            }
            val type = try {
                api.getUserCollection(username, key.id, authorization).type
            } catch (error: HttpException) {
                if (error.code() == 404) null else throw error
            }
            dao.setBangumiCollection(
                key.id,
                key.catalogType.name,
                type,
                System.currentTimeMillis(),
            )
            null
        } catch (error: HttpException) {
            when (error.code()) {
                401 -> "Bangumi Access Token 无效或已过期，点格子状态可能不准确"
                403 -> "Bangumi Access Token 缺少读取/写入收藏权限，点格子状态可能不准确"
                else -> null
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun cachedUsername(): String? =
        preferences.getString(KEY_BANGUMI_USERNAME, null)?.takeIf { it.isNotBlank() }

    private fun cacheUsername(username: String) {
        preferences.edit().putString(KEY_BANGUMI_USERNAME, username).apply()
    }

    suspend fun getAllCatalogMembers(): List<Subject> =
        dao.getAllCatalogMembers().map { it.toDomain() }

    private suspend fun refreshCatalogType(
        type: CatalogType,
        nsfwModes: List<Boolean>,
        authorization: String?,
        mode: SyncMode,
        progress: SyncProgressTracker,
    ): CatalogTypeSyncResult {
        val acceptedIds = mutableSetOf<Int>()
        val writtenIds = mutableSetOf<Int>()
        var rejected = 0
        var complete = true
        var sharded = false

        nsfwModes.forEach { nsfwOnly ->
            val generation = System.currentTimeMillis()
            var partitionComplete = true
            SOURCE_TAGS.forEach { sourceTag ->
                val useSharded = shouldShard(type, sourceTag, nsfwOnly)
                val result = if (useSharded) {
                    refreshQuerySharded(
                        type = type,
                        sourceTag = sourceTag,
                        nsfwOnly = nsfwOnly,
                        authorization = authorization,
                        mode = mode,
                        generation = generation,
                        progress = progress,
                    )
                } else {
                    refreshQuery(
                        type = type,
                        sourceTag = sourceTag,
                        nsfwOnly = nsfwOnly,
                        authorization = authorization,
                        mode = mode,
                        generation = generation,
                        progress = progress,
                    )
                }
                acceptedIds += result.acceptedIds
                writtenIds += result.writtenIds
                rejected += result.rejectedByRules
                partitionComplete = partitionComplete && result.complete
                if (result.truncated) markShard(type, sourceTag, nsfwOnly)
                if (useSharded && result.complete) clearShard(type, sourceTag, nsfwOnly)
                sharded = sharded || useSharded
            }
            if (mode == SyncMode.FORCE_UPDATE && partitionComplete) {
                dao.deactivateMissingFromPartition(type.name, nsfwOnly, generation)
            }
            complete = complete && partitionComplete
        }
        return CatalogTypeSyncResult(type, acceptedIds, writtenIds, rejected, complete, sharded)
    }

    private suspend fun refreshQuerySharded(
        type: CatalogType,
        sourceTag: String,
        nsfwOnly: Boolean,
        authorization: String?,
        mode: SyncMode,
        generation: Long,
        progress: SyncProgressTracker,
    ): QuerySyncResult {
        val endYearExclusive = java.time.Year.now().value + 1
        val acceptedIds = mutableSetOf<Int>()
        val writtenIds = mutableSetOf<Int>()
        var rejected = 0
        var complete = true
        var truncated = false

        yearChunks(SHARD_START_YEAR, endYearExclusive, SHARD_SPAN_YEARS).forEach { range ->
            val result = refreshQuery(
                type = type,
                sourceTag = sourceTag,
                nsfwOnly = nsfwOnly,
                authorization = authorization,
                mode = mode,
                generation = generation,
                progress = progress,
                airDate = "${range.startYear}-01-01" to "${range.endYearExclusive}-01-01",
            )
            acceptedIds += result.acceptedIds
            writtenIds += result.writtenIds
            rejected += result.rejectedByRules
            complete = complete && result.complete
            truncated = truncated || result.truncated
        }

        val fallback = refreshQuery(
            type = type,
            sourceTag = sourceTag,
            nsfwOnly = nsfwOnly,
            authorization = authorization,
            mode = mode,
            generation = generation,
            progress = progress,
            airDate = null,
        )
        acceptedIds += fallback.acceptedIds
        writtenIds += fallback.writtenIds
        rejected += fallback.rejectedByRules
        complete = complete && fallback.complete
        truncated = truncated || fallback.truncated

        return QuerySyncResult(acceptedIds, writtenIds, rejected, complete, truncated)
    }

    private suspend fun refreshQuery(
        type: CatalogType,
        sourceTag: String,
        nsfwOnly: Boolean,
        authorization: String?,
        mode: SyncMode,
        generation: Long,
        progress: SyncProgressTracker,
        airDate: Pair<String, String>? = null,
    ): QuerySyncResult {
        var offset = 0
        var pageNumber = 0
        val acceptedIds = mutableSetOf<Int>()
        val writtenIds = mutableSetOf<Int>()
        var rejected = 0
        var hasMore: Boolean
        var remoteHasMore: Boolean
        var partitionTrustworthy = true
        var queryConfigured = false

        do {
            val requestedOffset = offset
            val page = api.searchSubjects(
                limit = PAGE_SIZE,
                offset = offset,
                authorization = authorization,
                request = SearchSubjectsRequest(
                    keyword = "",
                    sort = "rank",
                    filter = SearchFilter(
                        type = listOf(type.apiValue),
                        tag = listOf(sourceTag),
                        metaTags = type.requiredMetaTags,
                        airDate = airDate?.let { listOf(">=${it.first}", "<${it.second}") }.orEmpty(),
                        nsfw = nsfwOnly,
                    ),
                ),
            )
            val responseTotal = page.total
            val responseLimit = page.limit
            if (!queryConfigured) {
                progress.configureQuery(responseTotal, responseLimit)
                queryConfigured = true
            }
            val paginationTrustworthy = responseTotal != null &&
                responseLimit != null && responseLimit > 0 &&
                page.offset == requestedOffset &&
                page.data.size <= responseLimit &&
                responseTotal >= requestedOffset + page.data.size
            if (!paginationTrustworthy) partitionTrustworthy = false

            val valid = page.data.filter { dto ->
                val hasRequiredMetaTags = type.requiredMetaTags.all(dto.metaTags::contains)
                val matchesNsfwPartition = (dto.nsfw ?: nsfwOnly) == nsfwOnly
                if (!matchesNsfwPartition) partitionTrustworthy = false
                val acceptedByRule = dto.type == type.apiValue &&
                    dto.hasExactTag(sourceTag) &&
                    hasRequiredMetaTags &&
                    matchesNsfwPartition
                if (!acceptedByRule) rejected += 1
                acceptedByRule && !isTombstoned(dto.id, type)
            }
            val existingById = if (valid.isEmpty()) {
                emptyMap()
            } else {
                dao.getByCatalogTypeAndIds(type.name, valid.map { it.id }).associateBy { it.id }
            }
            val candidates = when (mode) {
                SyncMode.ADD_MISSING -> valid.filter { existingById[it.id] == null }
                SyncMode.FORCE_UPDATE -> valid
            }
            val inactiveIds = if (mode == SyncMode.ADD_MISSING) {
                valid.mapNotNull { dto ->
                    dto.id.takeIf { existingById[dto.id]?.isCatalogMember == false }
                }
            } else {
                emptyList()
            }
            if (inactiveIds.isNotEmpty()) {
                dao.activateCatalogMembers(type.name, inactiveIds, nsfwOnly)
                writtenIds += inactiveIds
            }
            val now = System.currentTimeMillis()
            dao.upsertCatalogPagePreservingFavorites(
                candidates.map {
                    it.toEntity(
                        existing = existingById[it.id],
                        now = now,
                        isDetail = false,
                        catalogType = type,
                        nsfwFallback = nsfwOnly,
                        catalogGeneration = generation,
                    )
                },
            )

            acceptedIds += valid.map { it.id }
            writtenIds += candidates.map { it.id }
            offset += page.data.size
            pageNumber += 1
            remoteHasMore = responseTotal?.let { paginationTrustworthy && offset < it } ?: false
            hasMore = page.data.isNotEmpty() && remoteHasMore && pageNumber < MAX_PAGES
            val yearLabel = airDate?.let { " · ${it.first.take(4)}-${it.second.take(4)}" }.orEmpty()
            progress.pageCompleted(
                "${type.label} · $sourceTag$yearLabel${if (nsfwOnly) " · NSFW" else ""}",
            )

            if (hasMore) delay(REQUEST_GAP_MS)
        } while (hasMore)

        val truncatedByClientLimit = remoteHasMore && pageNumber >= MAX_PAGES
        val complete = !remoteHasMore && partitionTrustworthy && !truncatedByClientLimit
        return QuerySyncResult(acceptedIds, writtenIds, rejected, complete, truncatedByClientLimit)
    }

    private fun shouldShard(type: CatalogType, sourceTag: String, nsfwOnly: Boolean): Boolean =
        preferences.getBoolean(shardKey(type, sourceTag, nsfwOnly), false)

    private fun markShard(type: CatalogType, sourceTag: String, nsfwOnly: Boolean) {
        preferences.edit().putBoolean(shardKey(type, sourceTag, nsfwOnly), true).apply()
    }

    private fun clearShard(type: CatalogType, sourceTag: String, nsfwOnly: Boolean) {
        preferences.edit().putBoolean(shardKey(type, sourceTag, nsfwOnly), false).apply()
    }

    private fun shardKey(type: CatalogType, sourceTag: String, nsfwOnly: Boolean): String =
        "needs_shard:${type.name}:$sourceTag:$nsfwOnly"

    private class SyncProgressTracker(
        estimatedQueries: Int,
        private val callback: (SyncProgress) -> Unit,
    ) {
        private var completedPages = 0
        private var totalPages = estimatedQueries * MAX_PAGES

        @Synchronized
        fun configureQuery(remoteTotal: Int?, remoteLimit: Int?) {
            val limit = remoteLimit?.takeIf { it > 0 } ?: PAGE_SIZE
            val actualPages = remoteTotal
                ?.let { total -> ((total + limit - 1) / limit).coerceIn(1, MAX_PAGES) }
                ?: 1
            totalPages -= MAX_PAGES - actualPages
        }

        @Synchronized
        fun pageCompleted(label: String) {
            completedPages += 1
            callback(
                SyncProgress(
                    completedPages = completedPages,
                    totalPages = totalPages.coerceAtLeast(completedPages),
                    label = label,
                ),
            )
        }
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val MAX_PAGES = 50
        const val REQUEST_GAP_MS = 350L
        const val CACHE_MAX_AGE_MS = 12 * 60 * 60 * 1000L
        const val KEY_LAST_SYNC = "last_catalog_sync"
        const val KEY_LAST_ATTEMPT = "last_catalog_attempt"
        const val KEY_INITIAL_SYNC_COMPLETE = "initial_sync_complete"
        const val KEY_NSFW_ENABLED = "include_nsfw"
        const val KEY_SEED_NOTICE_DISMISSED = "seed_notice_dismissed"
        const val KEY_NSFW_NOTICE_DISMISSED = "nsfw_notice_dismissed"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_TOMBSTONES = "deleted_subject_keys"
        const val KEY_BANGUMI_USERNAME = "bangumi_username"
        const val ONLINE_SEARCH_LIMIT = 20
        const val SHARD_START_YEAR = 1980
        const val SHARD_SPAN_YEARS = 5
        val SOURCE_TAGS = listOf("百合", "轻百合")
    }
}
