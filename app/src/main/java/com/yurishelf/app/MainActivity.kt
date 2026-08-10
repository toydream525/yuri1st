package com.yurishelf.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.yurishelf.app.ui.CatalogScreen
import com.yurishelf.app.ui.CatalogViewModel
import com.yurishelf.app.ui.SubjectDetailScreen
import com.yurishelf.app.ui.theme.YuriShelfTheme

class MainActivity : ComponentActivity() {
    private val viewModel: CatalogViewModel by viewModels {
        CatalogViewModel.factory((application as YuriShelfApplication).container.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestStable60Hz()
        setContent {
            val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
            val detailState = viewModel.detailState.collectAsStateWithLifecycle().value
            val catalogListState = rememberLazyListState()
            val catalogGridState = rememberLazyGridState()
            var lastListPage by rememberSaveable { mutableStateOf(1) }

            YuriShelfTheme(themeMode = uiState.themeMode) {
                BackHandler(enabled = uiState.selectedSubjectKey != null) {
                    viewModel.closeSubject()
                }

                if (uiState.selectedSubjectKey == null) {
                    CatalogScreen(
                        state = uiState,
                        listState = catalogListState,
                        gridState = catalogGridState,
                        lastScrollPage = lastListPage,
                        onPageScrolled = { lastListPage = it },
                        onToggleViewMode = viewModel::toggleViewMode,
                        onSelectType = viewModel::selectType,
                        onSearch = viewModel::setSearchQuery,
                        onSort = viewModel::setSort,
                        onMinimumVotes = viewModel::setMinimumVotes,
                        onYear = viewModel::setYear,
                        onToggleFavoritesOnly = viewModel::toggleFavoritesOnly,
                        onSelectPage = viewModel::selectPage,
                        onNsfwEnabledChange = viewModel::setNsfwEnabled,
                        onSaveProxySettings = viewModel::saveProxySettings,
                        onSaveAccessToken = viewModel::saveAccessToken,
                        onDismissSyncMessage = viewModel::dismissSyncMessage,
                        onRefresh = viewModel::refresh,
                        onForceRefresh = viewModel::forceRefresh,
                        onOpenSubject = viewModel::openSubject,
                        onRollRandom = viewModel::rollRandom,
                        onClearRandom = viewModel::clearRandomPicks,
                        onToggleBlocked = viewModel::toggleBlocked,
                        onToggleFavorite = viewModel::toggleFavorite,
                        onDeleteSubject = viewModel::deleteSubject,
                        onAddBlockWord = viewModel::addBlockWord,
                        onRemoveBlockWord = viewModel::removeBlockWord,
                        onSearchOnline = viewModel::searchOnline,
                        onDismissSeedInfo = viewModel::dismissSeedInfo,
                        onThemeModeChange = viewModel::setThemeMode,
                        onToggleNsfwOnly = viewModel::toggleNsfwOnly,
                        onDismissNsfwNotice = viewModel::dismissNsfwNotice,
                        onOpenLink = { url ->
                            runCatching {
                                startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(url),
                                    ),
                                )
                            }.onFailure {
                                Toast.makeText(this, "无法打开浏览器", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onSaveAiSettings = viewModel::saveAiSettings,
                        onSetFormat = viewModel::setFormat,
                        onStartBatchAi = viewModel::startBatchAiAnalysis,
                        onCancelBatchAi = viewModel::cancelBatchAiAnalysis,
                        onDismissBatchAi = viewModel::dismissBatchAiResult,
                        onStartBatchPointGrid = viewModel::startBatchPointGrid,
                        onCancelBatchPointGrid = viewModel::cancelBatchPointGrid,
                        onDismissBatchPointGrid = viewModel::dismissBatchPointGridResult,
                        onSetAiCategory = viewModel::setAiCategory,
                        onSetWinLoseFilter = viewModel::setWinLoseFilter,
                        onToggleWinLose = viewModel::toggleWinLose,
                    )
                } else {
                    SubjectDetailScreen(
                        state = detailState,
                        onBack = viewModel::closeSubject,
                        onToggleFavorite = viewModel::toggleFavorite,
                        onToggleBlocked = viewModel::toggleBlocked,
                        onOpenOriginalPage = { subject ->
                            runCatching {
                                startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://bgm.tv/subject/${subject.id}"),
                                    ),
                                )
                            }.onFailure {
                                Toast.makeText(this, "无法打开浏览器", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onAnalyze = viewModel::analyzeSubject,
                        onUpdateBangumiCollection = viewModel::updateBangumiCollection,
                        onDismissDetailMessage = viewModel::dismissDetailMessage,
                        onToggleWinLose = viewModel::toggleWinLose,
                    )
                }
            }
        }
    }

    /**
     * 屏幕默认是 120Hz，列表滚动时帧长在 6~12ms 之间波动，
     * 会在 120Hz 下产生不均匀的帧节奏（每两帧漏一拍），看起来反而卡。
     * 锁定 60Hz 后每帧有完整的 16.6ms 预算，滚动节奏稳定。
     */
    private fun requestStable60Hz() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val mode60 = display?.supportedModes
                ?.minByOrNull { kotlin.math.abs(it.refreshRate - 60f) }
            if (mode60 != null) {
                window.attributes = window.attributes.apply {
                    preferredDisplayModeId = mode60.modeId
                }
            }
        } else {
            @Suppress("DEPRECATION")
            window.attributes = window.attributes.apply {
                preferredRefreshRate = 60f
            }
        }
    }

}
