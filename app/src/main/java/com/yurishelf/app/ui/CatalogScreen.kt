package com.yurishelf.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.yurishelf.app.BuildConfig
import com.yurishelf.app.data.SeedInfo
import com.yurishelf.app.data.ai.AiSettings
import com.yurishelf.app.data.parseSubjectReference
import com.yurishelf.app.data.remote.ProxySettings
import com.yurishelf.app.domain.CatalogType
import com.yurishelf.app.domain.CatalogViewMode
import com.yurishelf.app.domain.AiYuriCategory
import com.yurishelf.app.domain.SortOption
import com.yurishelf.app.domain.Subject
import com.yurishelf.app.domain.SubjectFormat
import com.yurishelf.app.domain.BangumiCollectionType
import com.yurishelf.app.domain.ThemeMode
import com.yurishelf.app.domain.WinLose
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun CatalogScreen(
    state: CatalogUiState,
    listState: LazyListState,
    gridState: LazyGridState,
    lastScrollPage: Int,
    onPageScrolled: (Int) -> Unit,
    onToggleViewMode: () -> Unit,
    onSelectType: (CatalogType) -> Unit,
    onSearch: (String) -> Unit,
    onSort: (SortOption) -> Unit,
    onMinimumVotes: (Int) -> Unit,
    onYear: (Int?) -> Unit,
    onToggleFavoritesOnly: () -> Unit,
    onSetAiCategory: (AiYuriCategory?) -> Unit,
    onSetWinLoseFilter: (WinLose?) -> Unit,
    onToggleWinLose: (Subject, WinLose) -> Unit,
    onSelectPage: (Int) -> Unit,
    onNsfwEnabledChange: (Boolean) -> Unit,
    onSaveProxySettings: (ProxySettings) -> Unit,
    onSaveAccessToken: (String?) -> Unit,
    onDismissSyncMessage: () -> Unit,
    onRefresh: () -> Unit,
    onForceRefresh: () -> Unit,
    onOpenSubject: (Subject) -> Unit,
    onRollRandom: (Double) -> Unit,
    onClearRandom: () -> Unit,
    onToggleBlocked: (Subject) -> Unit,
    onToggleFavorite: (Subject) -> Unit,
    onDeleteSubject: (Subject) -> Unit,
    onAddBlockWord: (String) -> Unit,
    onRemoveBlockWord: (String) -> Unit,
    onSearchOnline: () -> Unit,
    onDismissSeedInfo: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onToggleNsfwOnly: () -> Unit,
    onDismissNsfwNotice: () -> Unit,
    onOpenLink: (String) -> Unit,
    onSaveAiSettings: (AiSettings, String?, Boolean) -> Unit,
    onSetFormat: (SubjectFormat?) -> Unit,
    onStartBatchAi: (List<Subject>) -> Unit,
    onCancelBatchAi: () -> Unit,
    onDismissBatchAi: () -> Unit,
    onStartBatchPointGrid: (BangumiCollectionType, String, Boolean) -> Unit,
    onCancelBatchPointGrid: () -> Unit,
    onDismissBatchPointGrid: () -> Unit,
) {
    var showProxySettings by remember { mutableStateOf(false) }
    var showAccessTokenSettings by remember { mutableStateOf(false) }
    var showForceRefreshConfirmation by remember { mutableStateOf(false) }
    var showRefreshConfirmation by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showAdvancedFilters by rememberSaveable { mutableStateOf(false) }
    var showBlockedSubjects by remember { mutableStateOf(false) }
    var showBlockWords by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showAiSettings by remember { mutableStateOf(false) }
    var showBatchAiConfirm by remember { mutableStateOf(false) }
    var showBatchPointGrid by remember { mutableStateOf(false) }
    var pendingBlockSubject by remember { mutableStateOf<Subject?>(null) }

    val context = LocalContext.current
    val imageLoader = context.imageLoader
    LaunchedEffect(state.subjects) {
        state.subjects.forEach { subject ->
            if (subject.imageUrl.isNotBlank()) {
                imageLoader.enqueue(
                    ImageRequest.Builder(context)
                        .data(subject.imageUrl)
                        .size(PREFETCH_IMAGE_WIDTH, PREFETCH_IMAGE_HEIGHT)
                        .memoryCacheKey(subject.imageUrl)
                        .build(),
                )
            }
        }
    }

    if (showBatchAiConfirm) {
        BatchAiConfirmDialog(
            count = state.subjects.size,
            onDismiss = { showBatchAiConfirm = false },
            onConfirm = {
                showBatchAiConfirm = false
                onStartBatchAi(state.subjects)
            },
        )
    }

    if (state.batchAiState.running) {
        BatchProgressDialog(
            title = "正在批量 AI 分析",
            done = state.batchAiState.done,
            total = state.batchAiState.total,
            currentTitle = state.batchAiState.currentTitle,
            footnote = "每部之间自动限速；取消后已完成的结果仍会保留。",
            onCancel = onCancelBatchAi,
        )
    } else if (
        state.batchAiState.total > 0 &&
        state.batchAiState.done >= state.batchAiState.total
    ) {
        BatchResultDialog(
            title = "批量 AI 分析完成",
            summary = "成功 ${state.batchAiState.success} 部，失败 ${state.batchAiState.failed} 部。" +
                "结果已缓存，列表和封面会显示 AI 徽章。",
            lastError = state.batchAiState.lastError,
            onDismiss = onDismissBatchAi,
        )
    }

    if (showBatchPointGrid) {
        BatchPointGridDialog(
            hasAccessToken = state.hasAccessToken,
            currentPageCount = state.subjects.size,
            onDismiss = { showBatchPointGrid = false },
            onConfirm = { type, label, allEntries ->
                showBatchPointGrid = false
                onStartBatchPointGrid(type, label, allEntries)
            },
        )
    }

    if (state.batchPointGridState.running) {
        val grid = state.batchPointGridState
        BatchProgressDialog(
            title = "正在批量点格子（${grid.scopeLabel}）",
            done = grid.done,
            total = grid.total,
            currentTitle = grid.currentTitle,
            footnote = buildString {
                if (grid.skipped > 0) append("已跳过 ${grid.skipped} 条（已在目标列表中）。")
                append("正在限速写入，避免触发 Bangumi 频率限制；可随时取消。")
            },
            onCancel = onCancelBatchPointGrid,
        )
    } else if (
        (!state.batchPointGridState.running &&
            state.batchPointGridState.done >= state.batchPointGridState.total &&
            state.batchPointGridState.total > 0) ||
            (!state.batchPointGridState.running &&
                state.batchPointGridState.total == 0 &&
                state.batchPointGridState.skipped > 0)
    ) {
        val grid = state.batchPointGridState
        BatchResultDialog(
            title = "批量点格子完成",
            summary = "成功 ${grid.success} 条，失败 ${grid.failed} 条，" +
                "跳过 ${grid.skipped} 条（已在目标列表中）。",
            lastError = grid.lastError,
            onDismiss = onDismissBatchPointGrid,
        )
    }

    if (showAiSettings) {
        AiSettingsDialog(
            settings = state.aiSettings,
            onDismiss = { showAiSettings = false },
            onSave = { settings, apiKey, removeKey ->
                showAiSettings = false
                onSaveAiSettings(settings, apiKey, removeKey)
            },
        )
    }

    if (showAccessTokenSettings) {
        AccessTokenDialog(
            hasToken = state.hasAccessToken,
            onDismiss = { showAccessTokenSettings = false },
            onSave = {
                showAccessTokenSettings = false
                onSaveAccessToken(it)
            },
        )
    }

    if (showProxySettings) {
        SettingsDialog(
            settings = state.proxySettings,
            themeMode = state.themeMode,
            nsfwEnabled = state.filters.includeNsfw,
            hasAccessToken = state.hasAccessToken,
            onDismiss = { showProxySettings = false },
            onSave = {
                showProxySettings = false
                onSaveProxySettings(it)
            },
            onThemeModeChange = onThemeModeChange,
            onOpenAccessToken = {
                showProxySettings = false
                showAccessTokenSettings = true
            },
            onNsfwEnabledChange = onNsfwEnabledChange,
            onForceRefresh = {
                showProxySettings = false
                showForceRefreshConfirmation = true
            },
            onOpenBlocked = {
                showProxySettings = false
                showBlockedSubjects = true
            },
            onOpenBlockWords = {
                showProxySettings = false
                showBlockWords = true
            },
            onOpenAbout = {
                showProxySettings = false
                showAbout = true
            },
            onOpenAiSettings = {
                showProxySettings = false
                showAiSettings = true
            },
            aiSettings = state.aiSettings,
        )
    }

    if (showSearchDialog) {
        SearchDialog(
            query = state.filters.query,
            onQueryChange = onSearch,
            onSearchOnline = {
                showSearchDialog = false
                onSearchOnline()
            },
            onDismiss = { showSearchDialog = false },
        )
    }

    pendingBlockSubject?.let { subject ->
        AlertDialog(
            onDismissRequest = { pendingBlockSubject = null },
            title = { Text("屏蔽条目？") },
            text = {
                Text(
                    "屏蔽「${subject.displayName}」后，它将从列表和随机推荐中隐藏；" +
                        "可在“设置→屏蔽管理”中解除。",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingBlockSubject = null
                        onToggleBlocked(subject)
                    },
                ) { Text("屏蔽") }
            },
            dismissButton = {
                TextButton(onClick = { pendingBlockSubject = null }) { Text("取消") }
            },
        )
    }

    if (showBlockedSubjects) {
        BlockedSubjectsDialog(
            subjects = state.blockedSubjects,
            onDismiss = { showBlockedSubjects = false },
            onUnblock = onToggleBlocked,
            onDelete = onDeleteSubject,
            onOpenSubject = { subject ->
                showBlockedSubjects = false
                onOpenSubject(subject)
            },
        )
    }

    if (showBlockWords) {
        BlockWordsDialog(
            words = state.blockWords,
            onDismiss = { showBlockWords = false },
            onAdd = onAddBlockWord,
            onRemove = onRemoveBlockWord,
        )
    }

    if (showAbout) {
        AboutDialog(
            onDismiss = { showAbout = false },
            onOpenLink = onOpenLink,
        )
    }

    if (state.randomMinimumScore != null) {
        RandomPicksDialog(
            minimumScore = state.randomMinimumScore,
            subjects = state.randomPicks,
            onDismiss = onClearRandom,
            onRollAgain = onRollRandom,
            onOpenSubject = { subject ->
                onClearRandom()
                onOpenSubject(subject)
            },
        )
    }

    if (showForceRefreshConfirmation) {
        AlertDialog(
            onDismissRequest = { showForceRefreshConfirmation = false },
            title = { Text("强制重新拉取数据库？") },
            text = {
                Text("这会重新请求“百合/轻百合”目录并更新已有条目的远端资料；本地收藏会保留。普通刷新只累加新条目。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showForceRefreshConfirmation = false
                        onForceRefresh()
                    },
                ) { Text("开始重新拉取") }
            },
            dismissButton = {
                TextButton(onClick = { showForceRefreshConfirmation = false }) { Text("取消") }
            },
        )
    }

    if (showRefreshConfirmation) {
        AlertDialog(
            onDismissRequest = { showRefreshConfirmation = false },
            title = { Text("刷新目录？") },
            text = {
                Text("将检查 Bangumi 并累加新的“百合/轻百合”条目；本地收藏和屏蔽不受影响。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRefreshConfirmation = false
                        onRefresh()
                    },
                ) { Text("刷新") }
            },
            dismissButton = {
                TextButton(onClick = { showRefreshConfirmation = false }) { Text("取消") }
            },
        )
    }

    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(48.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { showSearchDialog = true }) {
                        val hasQuery = state.filters.query.isNotBlank()
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = if (hasQuery) "搜索（有内容）" else "搜索",
                            tint = if (hasQuery) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .then(
                                    if (hasQuery) {
                                        Modifier
                                            .background(
                                                MaterialTheme.colorScheme.primary,
                                                RoundedCornerShape(10.dp),
                                            )
                                            .padding(6.dp)
                                    } else {
                                        Modifier.padding(2.dp)
                                    },
                                ),
                        )
                    }
                    IconButton(onClick = onToggleViewMode) {
                        if (state.viewMode == CatalogViewMode.LIST) {
                            Icon(Icons.Filled.GridView, contentDescription = "切换到封面模式")
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.ViewList,
                                contentDescription = "切换到列表模式",
                            )
                        }
                    }
                    IconButton(
                        onClick = { showBatchPointGrid = true },
                        enabled = state.hasAccessToken &&
                            (state.subjects.isNotEmpty() || state.totalSubjects > 0),
                    ) {
                        Icon(
                            Icons.Filled.Checklist,
                            contentDescription = "批量点格子",
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = { onRollRandom(DEFAULT_RANDOM_MINIMUM_SCORE) },
                        enabled = state.subjects.isNotEmpty() || state.totalSubjects > 0,
                    ) {
                        Icon(
                            Icons.Filled.Casino,
                            contentDescription = "随机推荐",
                        )
                    }
                    IconButton(
                        onClick = { showRefreshConfirmation = true },
                        enabled = state.syncState !is SyncState.Loading,
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "刷新")
                    }
                    IconButton(onClick = { showProxySettings = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "设置")
                    }
                }
            }
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            val selectedTab = CatalogType.entries.indexOf(state.filters.type)
            TabRow(selectedTabIndex = selectedTab) {
                CatalogType.entries.forEach { type ->
                    Tab(
                        selected = state.filters.type == type,
                        onClick = { onSelectType(type) },
                        text = { Text(type.label) },
                    )
                }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(SortOption.entries) { option ->
                    FilterChip(
                        selected = state.filters.sort == option,
                        onClick = { onSort(option) },
                        label = { Text(option.label) },
                    )
                }
                item {
                    FilterChip(
                        selected = state.filters.favoritesOnly,
                        onClick = onToggleFavoritesOnly,
                        label = { Text("仅收藏") },
                    )
                }
                if (state.filters.includeNsfw) {
                    item {
                        FilterChip(
                            selected = state.filters.nsfwOnly,
                            onClick = onToggleNsfwOnly,
                            label = { Text("仅 NSFW") },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = { showAdvancedFilters = !showAdvancedFilters },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (showAdvancedFilters) "收起高级筛选" else "高级筛选")
                }
                TextButton(
                    onClick = { showBatchAiConfirm = true },
                    enabled = state.subjects.isNotEmpty() && !state.batchAiState.running,
                ) {
                    Text("AI 分析本页")
                }
            }

            AnimatedVisibility(visible = showAdvancedFilters, enter = fadeIn(), exit = fadeOut()) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("分类", style = MaterialTheme.typography.labelLarge)
                        FilterChip(
                            selected = state.filters.format == null,
                            onClick = { onSetFormat(null) },
                            label = { Text("不限") },
                        )
                        SubjectFormat.entries.forEach { format ->
                            FilterChip(
                                selected = state.filters.format == format,
                                onClick = { onSetFormat(format) },
                                label = { Text(format.label) },
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("AI/自定义", style = MaterialTheme.typography.labelLarge)
                        FilterChip(
                            selected = state.filters.aiCategory == null &&
                                state.filters.winLose == null,
                            onClick = {
                                onSetAiCategory(null)
                                onSetWinLoseFilter(null)
                            },
                            label = { Text("不限") },
                        )
                        AiYuriCategory.entries
                            .filter { it != AiYuriCategory.UNKNOWN }
                            .forEach { category ->
                                FilterChip(
                                    selected = state.filters.aiCategory == category,
                                    onClick = { onSetAiCategory(category) },
                                    label = { Text(category.label) },
                                )
                            }
                        WinLose.entries.forEach { winLose ->
                            FilterChip(
                                selected = state.filters.winLose == winLose,
                                onClick = { onSetWinLoseFilter(winLose) },
                                label = { Text(winLose.label) },
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("最低评分人数", style = MaterialTheme.typography.labelLarge)
                        listOf(0, 10, 50, 200).forEach { minimum ->
                            FilterChip(
                                selected = state.filters.minimumVotes == minimum,
                                onClick = { onMinimumVotes(minimum) },
                                label = { Text(if (minimum == 0) "不限" else "$minimum+") },
                            )
                        }
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        item {
                            Text("年份", style = MaterialTheme.typography.labelLarge)
                        }
                        item {
                            FilterChip(
                                selected = state.filters.year == null,
                                onClick = { onYear(null) },
                                label = { Text("不限") },
                            )
                        }
                        items(state.availableYears) { year ->
                            FilterChip(
                                selected = state.filters.year == year,
                                onClick = { onYear(year) },
                                label = { Text(year.toString()) },
                            )
                        }
                    }
                }
            }

            SyncBanner(
                syncState = state.syncState,
                onDismiss = onDismissSyncMessage,
            )
            SeedBanner(
                seedInfo = state.seedInfo,
                onDismiss = onDismissSeedInfo,
            )
            if (state.filters.includeNsfw && !state.nsfwNoticeDismissed) {
                NsfwBanner(onDismiss = onDismissNsfwNotice)
            }

            if (state.subjects.isEmpty() && state.syncState !is SyncState.Loading) {
                EmptyCatalog(modifier = Modifier.weight(1f))
            } else {
                LaunchedEffect(state.currentPage) {
                    if (state.currentPage != lastScrollPage) {
                        if (
                            state.viewMode == CatalogViewMode.LIST &&
                            listState.firstVisibleItemIndex > 0
                        ) {
                            listState.scrollToItem(0)
                        }
                        if (
                            state.viewMode == CatalogViewMode.GRID &&
                            gridState.firstVisibleItemIndex > 0
                        ) {
                            gridState.scrollToItem(0)
                        }
                    }
                    onPageScrolled(state.currentPage)
                }
                if (state.viewMode == CatalogViewMode.LIST) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(
                            items = state.subjects,
                            key = { "${it.type.name}:${it.id}" },
                            contentType = { "subject" },
                        ) { subject ->
                            SubjectCard(
                                subject = subject,
                                onClick = { onOpenSubject(subject) },
                                onToggleBlocked = { pendingBlockSubject = subject },
                                onToggleFavorite = { onToggleFavorite(subject) },
                                onToggleWinLose = onToggleWinLose,
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        state = gridState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.subjects, key = { "${it.type.name}:${it.id}" }) { subject ->
                            SubjectCoverCard(
                                subject = subject,
                                onClick = { onOpenSubject(subject) },
                            )
                        }
                    }
                }
                CatalogPager(
                    currentPage = state.currentPage,
                    totalPages = state.totalPages,
                    totalSubjects = state.totalSubjects,
                    onSelectPage = onSelectPage,
                )
            }
        }
    }
}

@Composable
private fun SubjectCoverCard(
    subject: Subject,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column {
            if (subject.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(subject.imageUrl)
                        .size(480, 640)
                        .build(),
                    contentDescription = subject.displayName,
                    placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                    error = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("暂无封面", style = MaterialTheme.typography.labelSmall)
                }
            }
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                subject.aiAnalysis?.let { analysis ->
                    AiCategoryBadge(
                        category = analysis.category,
                        riskCount = analysis.riskPoints.size,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                Text(
                    text = subject.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (subject.ratingScore > 0) {
                        String.format(Locale.US, "%.1f", subject.ratingScore)
                    } else {
                        "暂无评分"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                subject.bangumiCollectionType?.let { type ->
                    BangumiCollectionBadge(type)
                }
                subject.winLose?.let { winLose ->
                    Text(
                        text = winLose.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (winLose == WinLose.WIN) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SeedBanner(seedInfo: SeedInfo?, onDismiss: () -> Unit) {
    if (seedInfo == null) return
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(SEED_NOTICE_DURATION_MILLIS)
        visible = false
        onDismiss()
    }
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        DismissibleBanner(
            message = "已内置 ${seedInfo.count} 条基础条目（数据更新于 ${seedInfo.generatedAt}），可直接浏览；点击右上角“刷新”可获取最新目录。",
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            onDismiss = {
                visible = false
                onDismiss()
            },
        )
    }
}

@Composable
private fun NsfwBanner(onDismiss: () -> Unit) {
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(NSFW_NOTICE_DURATION_MILLIS)
        visible = false
        onDismiss()
    }
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        DismissibleBanner(
            message = "NSFW 已开启。Bangumi 官网默认隐藏 NSFW 条目，请先在官网账户设置中允许显示；可在本应用设置中关闭。",
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            onDismiss = {
                visible = false
                onDismiss()
            },
        )
    }
}

@Composable
private fun SyncBanner(syncState: SyncState, onDismiss: () -> Unit) {
    LaunchedEffect(syncState) {
        val duration = when (syncState) {
            is SyncState.Success -> SUCCESS_NOTICE_DURATION_MILLIS
            is SyncState.Failed -> ERROR_NOTICE_DURATION_MILLIS
            else -> null
        }
        duration?.let {
            delay(it)
            onDismiss()
        }
    }
    when (syncState) {
        SyncState.Idle -> Unit
        is SyncState.Loading -> Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(vertical = 8.dp),
        ) {
            val progress = syncState.progress
            if (progress == null) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    "正在准备同步…",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                LinearProgressIndicator(
                    progress = { progress.fraction },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "${progress.label} · ${progress.completedPages}/${progress.totalPages} 页",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        is SyncState.Success -> DismissibleBanner(
            message = syncState.message,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            onDismiss = onDismiss,
        )
        is SyncState.Failed -> DismissibleBanner(
            message = syncState.message,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun DismissibleBanner(
    message: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor)
            .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            color = contentColor,
            style = MaterialTheme.typography.bodySmall,
        )
        TextButton(onClick = onDismiss) {
            Text("关闭", color = contentColor)
        }
    }
}

private const val SUCCESS_NOTICE_DURATION_MILLIS = 1_000L
private const val ERROR_NOTICE_DURATION_MILLIS = 1_000L
private const val NSFW_NOTICE_DURATION_MILLIS = 1_000L
private const val SEED_NOTICE_DURATION_MILLIS = 3_000L
private const val DEFAULT_RANDOM_MINIMUM_SCORE = 7.0
private const val PREFETCH_IMAGE_WIDTH = 480
private const val PREFETCH_IMAGE_HEIGHT = 640

@Composable
private fun RandomPicksDialog(
    minimumScore: Double,
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onRollAgain: (Double) -> Unit,
    onOpenSubject: (Subject) -> Unit,
) {
    var selectedScore by rememberSaveable { mutableStateOf(minimumScore) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("随机推荐") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0.0, 7.0, 7.5, 8.0, 8.5).forEach { score ->
                        FilterChip(
                            selected = selectedScore == score,
                            onClick = { selectedScore = score },
                            label = { Text(if (score == 0.0) "不限" else "${score}+") },
                        )
                    }
                }
                if (subjects.isEmpty()) {
                    Text("该评分以上没有可随机的内容，试试降低评分或先刷新目录。")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                        items(subjects, key = { "${it.type.name}:${it.id}" }) { subject ->
                            TextButton(onClick = { onOpenSubject(subject) }) {
                                Text(
                                    text = "${subject.displayName}（${String.format(Locale.US, "%.1f", subject.ratingScore)}）",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onRollAgain(selectedScore) }) { Text("再来一组") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}

@Composable
private fun BlockedSubjectsDialog(
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onUnblock: (Subject) -> Unit,
    onDelete: (Subject) -> Unit,
    onOpenSubject: (Subject) -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<Subject?>(null) }

    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除条目？") },
            text = { Text("将彻底删除「${pendingDelete?.displayName}」的本地数据，之后同步也不会重新收录。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete?.let(onDelete)
                        pendingDelete = null
                    },
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("屏蔽管理") },
        text = {
            if (subjects.isEmpty()) {
                Text("还没有屏蔽任何条目。")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(subjects, key = { "${it.type.name}:${it.id}" }) { subject ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = { onOpenSubject(subject) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = subject.displayName,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            TextButton(onClick = { onUnblock(subject) }) { Text("解除") }
                            TextButton(onClick = { pendingDelete = subject }) { Text("删除") }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}

@Composable
private fun BlockWordsDialog(
    words: List<String>,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    var newWord by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("屏蔽词") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "匹配条目名称、别名、标签和基本资料中的文字，命中后不再显示。",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = newWord,
                    onValueChange = { newWord = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("添加屏蔽词") },
                )
                if (words.isEmpty()) {
                    Text("还没有屏蔽词。")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                        items(words) { word ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = word,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                TextButton(onClick = { onRemove(word) }) { Text("移除") }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = newWord.isNotBlank(),
                onClick = {
                    onAdd(newWord)
                    newWord = ""
                },
            ) { Text("添加") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}

@Composable
private fun AboutDialog(
    onDismiss: () -> Unit,
    onOpenLink: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("关于本应用") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("版本 ${BuildConfig.VERSION_NAME}（${BuildConfig.VERSION_CODE}）")
                Text("设计：贝恩德塔")
                Text("条目、标签和评分数据来自 Bangumi（bgm.tv），本应用不是 Bangumi 官方应用。")
                Text("百合相关友链：")
                TextButton(onClick = { onOpenLink("https://stage1st.com/2b/thread-2281194-1-1.html") }) {
                    Text("S1 百综楼 12.0（专楼）")
                }
                TextButton(onClick = { onOpenLink("https://stage1st.com/2b/forum-6-1.html") }) {
                    Text("S1 动漫论坛（入口）")
                }
                TextButton(onClick = { onOpenLink("https://bbs.yamibo.com/") }) {
                    Text("百合会")
                }
                Text(
                    "S1 专楼需要登录后浏览；友链属于第三方站点，与本应用无隶属或背书关系。",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "项目开源：采用 MIT 许可证，源码仓库及第三方组件说明见 README。",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}

@Composable
private fun SearchDialog(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchOnline: () -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(query) }
    val isReference = parseSubjectReference(text) != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("搜索") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        onQueryChange(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("名称、别名、原作、Staff 或标签") },
                    trailingIcon = {
                        if (text.isNotEmpty()) {
                            IconButton(onClick = {
                                text = ""
                                onQueryChange("")
                            }) {
                                Icon(Icons.Filled.Close, contentDescription = "清除")
                            }
                        }
                    },
                )
                Text(
                    text = if (isReference) {
                        "识别为 Bangumi 条目链接或 ID，可直接导入。"
                    } else {
                        "也支持粘贴 bgm.tv / bangumi.tv 条目链接或纯 ID 在线导入。"
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            if (text.isNotBlank()) {
                Button(
                    onClick = onSearchOnline,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (isReference) "导入" else "在线搜索",
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else {
                OutlinedButton(onClick = onSearchOnline, enabled = false) {
                    Text(if (isReference) "导入" else "在线搜索")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun CatalogPager(
    currentPage: Int,
    totalPages: Int,
    totalSubjects: Int,
    onSelectPage: (Int) -> Unit,
) {
    var pageInput by rememberSaveable(currentPage) { mutableStateOf(currentPage.toString()) }
    var showPageJump by rememberSaveable { mutableStateOf(false) }
    val submitPage = {
        val requested = pageInput.toIntOrNull()?.coerceIn(1, totalPages)
        if (requested != null && requested != currentPage) onSelectPage(requested)
    }

    if (showPageJump) {
        AlertDialog(
            onDismissRequest = { showPageJump = false },
            title = { Text("跳转到页数") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pageInput,
                        onValueChange = { pageInput = it.filter(Char::isDigit).take(4) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("页码（1～$totalPages）") },
                    )
                    Text(
                        "共 $totalPages 页 · $totalSubjects 条",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = pageInput.toIntOrNull() != null,
                    onClick = {
                        submitPage()
                        showPageJump = false
                    },
                ) { Text("跳转") }
            },
            dismissButton = {
                TextButton(onClick = { showPageJump = false }) { Text("取消") }
            },
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = { onSelectPage(currentPage - 1) },
            enabled = currentPage > 1,
        ) { Text("上一页") }
        SuggestionChip(
            onClick = {
                pageInput = currentPage.toString()
                showPageJump = true
            },
            label = { Text("第 $currentPage / $totalPages 页 · 共 $totalSubjects 条") },
        )
        TextButton(
            onClick = { onSelectPage(currentPage + 1) },
            enabled = currentPage < totalPages,
        ) { Text("下一页") }
    }
}

@Composable
private fun BatchAiConfirmDialog(
    count: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("一键 AI 分析本页？") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("将对本页 $count 部作品逐个进行 AI 雷点分析。")
                Text(
                    "每部之间会自动限速，预计需要较长时间，并会消耗 AI 接口额度；" +
                        "已分析过的条目也会重新分析。结果仅供参考。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("开始分析") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun BatchProgressDialog(
    title: String,
    done: Int,
    total: Int,
    currentTitle: String,
    footnote: String,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LinearProgressIndicator(
                    progress = {
                        if (total > 0) done.toFloat() / total else 0f
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "$done/$total · 当前：$currentTitle",
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    footnote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onCancel) { Text("取消") }
        },
    )
}

@Composable
private fun BatchResultDialog(
    title: String,
    summary: String,
    lastError: String?,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(summary)
                lastError?.let { error ->
                    Text(
                        "示例错误：$error",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}

@Composable
private fun BatchPointGridDialog(
    hasAccessToken: Boolean,
    currentPageCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (BangumiCollectionType, String, Boolean) -> Unit,
) {
    var allEntries by rememberSaveable { mutableStateOf(false) }
    var selectedType by rememberSaveable {
        mutableStateOf(BangumiCollectionType.WISH)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("批量点格子") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!hasAccessToken) {
                    Text(
                        "需要先在“设置”中配置 Bangumi Access Token。",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text("范围", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !allEntries,
                        onClick = { allEntries = false },
                        label = { Text("当前页（$currentPageCount 条）") },
                    )
                    FilterChip(
                        selected = allEntries,
                        onClick = { allEntries = true },
                        label = { Text("全部条目（当前分类）") },
                    )
                }
                Text("加入列表", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BangumiCollectionType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.label) },
                        )
                    }
                }
                Text(
                    "Bangumi 对写请求有频率限制，将自动限速（约 1.2 秒/条）。" +
                        "“全部条目”数量可能很多、耗时很长；已在该列表中的条目会自动跳过，可随时取消。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = hasAccessToken,
                onClick = {
                    onConfirm(
                        selectedType,
                        if (allEntries) "全部条目" else "当前页",
                        allEntries,
                    )
                },
            ) { Text("开始") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun EmptyCatalog(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("本地还没有条目", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text("点击刷新，从 Bangumi 累加“百合/轻百合”标签条目")
        }
    }
}

@Composable
private fun SubjectCard(
    subject: Subject,
    onClick: () -> Unit,
    onToggleBlocked: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleWinLose: (Subject, WinLose) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val coverWidth = with(density) { 86.dp.roundToPx() }
    val coverHeight = with(density) { 122.dp.roundToPx() }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (subject.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(subject.imageUrl)
                        .size(coverWidth, coverHeight)
                        .build(),
                    contentDescription = subject.displayName,
                    placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                    error = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .size(width = 86.dp, height = 122.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(width = 86.dp, height = 122.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("暂无封面", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subject.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subject.aiAnalysis != null || subject.bangumiCollectionType != null) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        subject.aiAnalysis?.let { analysis ->
                            AiCategoryBadge(
                                category = analysis.category,
                                riskCount = analysis.riskPoints.size,
                            )
                        }
                        subject.bangumiCollectionType?.let { type ->
                            BangumiCollectionBadge(type)
                        }
                    }
                }
                if (subject.nsfw) {
                    Text(
                        "NSFW",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (subject.secondaryName.isNotBlank()) {
                    Text(
                        text = subject.secondaryName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = buildString {
                        append(subject.categoryLabel)
                        if (subject.date.isNotBlank()) append(" · ${subject.date}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = if (subject.ratingScore > 0) {
                            String.format(Locale.US, "%.1f", subject.ratingScore)
                        } else {
                            "暂无评分"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    if (subject.ratingTotal > 0 || subject.rank > 0) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = buildString {
                                if (subject.ratingTotal > 0) append("${subject.ratingTotal} 人评分")
                                if (subject.rank > 0) {
                                    if (subject.ratingTotal > 0) append(" · ")
                                    append("排名 #${subject.rank}")
                                }
                            },
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
                if (subject.tags.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        subject.tags.take(4).joinToString(" · "),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                val favoriteInteraction = remember { MutableInteractionSource() }
                Icon(
                    if (subject.isFavorite) {
                        Icons.Filled.Favorite
                    } else {
                        Icons.Filled.FavoriteBorder
                    },
                    contentDescription = if (subject.isFavorite) "取消收藏" else "收藏",
                    tint = if (subject.isFavorite) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .clickable(
                            interactionSource = favoriteInteraction,
                            indication = null,
                            onClick = onToggleFavorite,
                        )
                        .padding(6.dp),
                )
                WinLoseButton(
                    winLose = WinLose.WIN,
                    selected = subject.winLose == WinLose.WIN,
                    onClick = { onToggleWinLose(subject, WinLose.WIN) },
                )
                WinLoseButton(
                    winLose = WinLose.LOSE,
                    selected = subject.winLose == WinLose.LOSE,
                    onClick = { onToggleWinLose(subject, WinLose.LOSE) },
                )
                val blockInteraction = remember { MutableInteractionSource() }
                Icon(
                    Icons.Filled.Block,
                    contentDescription = "屏蔽",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable(
                            interactionSource = blockInteraction,
                            indication = null,
                            onClick = onToggleBlocked,
                        )
                        .padding(6.dp),
                )
            }
        }
    }
}

@Composable
private fun WinLoseButton(
    winLose: WinLose,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val containerColor = when {
        selected && winLose == WinLose.WIN -> MaterialTheme.colorScheme.primaryContainer
        selected && winLose == WinLose.LOSE -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when {
        selected && winLose == WinLose.WIN -> MaterialTheme.colorScheme.onPrimaryContainer
        selected && winLose == WinLose.LOSE -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val borderColor = when {
        selected && winLose == WinLose.WIN -> MaterialTheme.colorScheme.primary
        selected && winLose == WinLose.LOSE -> MaterialTheme.colorScheme.error
        else -> androidx.compose.ui.graphics.Color.Transparent
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .border(
                BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor),
                RoundedCornerShape(8.dp),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (winLose == WinLose.WIN) {
                    Icons.AutoMirrored.Filled.TrendingUp
                } else {
                    Icons.AutoMirrored.Filled.TrendingDown
                },
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = winLose.label,
                color = contentColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}
