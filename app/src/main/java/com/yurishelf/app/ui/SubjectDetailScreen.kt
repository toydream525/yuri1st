package com.yurishelf.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yurishelf.app.domain.Subject
import com.yurishelf.app.domain.BangumiCollectionType
import com.yurishelf.app.domain.AiYuriCategory
import com.yurishelf.app.domain.WinLose
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(
    state: DetailUiState,
    onBack: () -> Unit,
    onToggleFavorite: (Subject) -> Unit,
    onToggleBlocked: (Subject) -> Unit,
    onOpenOriginalPage: (Subject) -> Unit,
    onAnalyze: (Subject) -> Unit,
    onUpdateBangumiCollection: (Subject, BangumiCollectionType) -> Unit,
    onDismissDetailMessage: () -> Unit,
    onToggleWinLose: (Subject, WinLose) -> Unit,
    onSetAiCategoryOverride: (Subject, AiYuriCategory?) -> Unit,
) {
    var pendingBlock by remember { mutableStateOf<Subject?>(null) }

    pendingBlock?.let { subject ->
        AlertDialog(
            onDismissRequest = { pendingBlock = null },
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
                        pendingBlock = null
                        onToggleBlocked(subject)
                    },
                ) { Text("屏蔽") }
            },
            dismissButton = {
                TextButton(onClick = { pendingBlock = null }) { Text("取消") }
            },
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = state.subject?.displayName ?: "条目详情",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                actions = {
                    state.subject?.let { subject ->
                        IconButton(onClick = {
                            if (subject.isBlocked) {
                                onToggleBlocked(subject)
                            } else {
                                pendingBlock = subject
                            }
                        }) {
                            Icon(
                                Icons.Filled.Block,
                                contentDescription = if (subject.isBlocked) "取消屏蔽" else "屏蔽",
                                tint = if (subject.isBlocked) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                        IconButton(onClick = { onToggleFavorite(subject) }) {
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
                            )
                        }
                    }
                },
            )
        },
    ) { contentPadding ->
        if (state.subject == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentAlignment = Alignment.Center,
            ) {
                LinearProgressIndicator(modifier = Modifier.width(180.dp))
            }
        } else {
            SubjectDetailBody(
                subject = state.subject,
                loading = state.loading,
                onOpenOriginalPage = onOpenOriginalPage,
                onAnalyze = onAnalyze,
                onUpdateBangumiCollection = onUpdateBangumiCollection,
                onDismissDetailMessage = onDismissDetailMessage,
                detailMessage = state.detailMessage,
                analyzing = state.analyzing,
                collectionUpdating = state.collectionUpdating,
                analysis = state.analysis,
                onToggleWinLose = onToggleWinLose,
                onSetAiCategoryOverride = onSetAiCategoryOverride,
                modifier = Modifier.padding(contentPadding),
            )
        }
    }
}

@Composable
private fun SubjectDetailBody(
    subject: Subject,
    loading: Boolean,
    onOpenOriginalPage: (Subject) -> Unit,
    onAnalyze: (Subject) -> Unit,
    onUpdateBangumiCollection: (Subject, BangumiCollectionType) -> Unit,
    onDismissDetailMessage: () -> Unit,
    detailMessage: String?,
    analyzing: Boolean,
    collectionUpdating: Boolean,
    analysis: com.yurishelf.app.domain.AiAnalysis?,
    onToggleWinLose: (Subject, WinLose) -> Unit,
    onSetAiCategoryOverride: (Subject, AiYuriCategory?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val coverWidth = with(density) { 132.dp.roundToPx() }
    val coverHeight = with(density) { 188.dp.roundToPx() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp),
    ) {
        if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

        Row(modifier = Modifier.padding(16.dp)) {
            if (subject.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(subject.imageUrl)
                        .size(coverWidth, coverHeight)
                        .build(),
                    contentDescription = subject.displayName,
                    modifier = Modifier
                        .size(width = 132.dp, height = 188.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(width = 132.dp, height = 188.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("暂无封面")
                }
            }

            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(subject.displayName, style = MaterialTheme.typography.headlineSmall)
                if (subject.secondaryName.isNotBlank()) {
                    Text(
                        subject.secondaryName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(subject.categoryLabel, style = MaterialTheme.typography.labelLarge)
                if (subject.nsfw) {
                    Text(
                        "NSFW",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (subject.date.isNotBlank()) Text("日期：${subject.date}")
                if (subject.episodeCount > 0) Text("章节：${subject.episodeCount}")
                Spacer(Modifier.height(14.dp))
                Text(
                    text = if (subject.ratingScore > 0) {
                        String.format(Locale.US, "%.1f", subject.ratingScore)
                    } else {
                        "暂无评分"
                    },
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                if (subject.ratingTotal > 0) Text("${subject.ratingTotal} 人评分")
                if (subject.rank > 0) Text("Bangumi 排名 #${subject.rank}")
            }
        }

        TextButton(
            onClick = { onOpenOriginalPage(subject) },
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            Text("打开 Bangumi 原页面")
        }

        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("自定义", style = MaterialTheme.typography.labelLarge)
            WinLose.entries.forEach { winLose ->
                val selected = subject.winLose == winLose
                SuggestionChip(
                    onClick = { onToggleWinLose(subject, winLose) },
                    label = { Text(winLose.label) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = when {
                            selected && winLose == WinLose.WIN ->
                                MaterialTheme.colorScheme.primaryContainer
                            selected && winLose == WinLose.LOSE ->
                                MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                    ),
                )
            }
        }

        if (detailMessage != null) {
            DetailMessageBanner(
                message = detailMessage,
                onDismiss = onDismissDetailMessage,
            )
        }

        SectionTitle("Bangumi 点格子")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (collectionUpdating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
            }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(BangumiCollectionType.entries) { type ->
                    FilterChip(
                        selected = subject.bangumiCollectionType == type,
                        onClick = { onUpdateBangumiCollection(subject, type) },
                        enabled = !collectionUpdating,
                        label = { Text(type.label) },
                    )
                }
            }
            Text(
                text = if (subject.bangumiCollectionType == null) {
                    "尚未点格子；点击上方状态即可写入你的 Bangumi 账号。"
                } else {
                    "当前状态：${subject.bangumiCollectionType?.label}（Bangumi 官方 v0 接口不提供删除收藏）"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionTitle("AI 百合倾向分析")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "由 AI 根据条目资料与联网信息生成，可能不准确，仅供参考。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (analysis != null || subject.manualYuriCategory != null) {
                val category = subject.effectiveAiCategory ?: AiYuriCategory.UNKNOWN
                AiCategoryBadge(
                    category = category,
                    riskCount = if (category == AiYuriCategory.NON) analysis?.riskPoints?.size ?: 0 else 0,
                    source = subject.aiCategorySource,
                )
                if (subject.manualYuriCategory != null) {
                    Text(
                        "当前分类已由你手动设为“${category.label}”。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (category == AiYuriCategory.UNKNOWN) {
                    Text("AI 未能可靠判断，置信度较低。")
                }
                analysis?.let { currentAnalysis ->
                    Text(
                        "置信度：${String.format(Locale.US, "%.0f%%", currentAnalysis.confidence * 100)}",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(currentAnalysis.reason, style = MaterialTheme.typography.bodyMedium)
                }
                if (analysis?.riskPoints?.isNotEmpty() == true) {
                    Text("雷点：", fontWeight = FontWeight.SemiBold)
                    analysis.riskPoints.forEach { risk ->
                        Text("• $risk", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (analysis?.sources?.isNotEmpty() == true) {
                    Text(
                        "参考来源：${analysis.sources.joinToString("；")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                analysis?.let { currentAnalysis ->
                    Text(
                        "分析时间：${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(currentAnalysis.analyzedAt))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else if (analyzing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("AI 正在读取条目并联网分析…")
                }
            }
            Text("分类", fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf(AiYuriCategory.STRONG, AiYuriCategory.LIGHT, AiYuriCategory.NON)) { category ->
                    FilterChip(
                        selected = subject.manualYuriCategory == category,
                        onClick = { onSetAiCategoryOverride(subject, category) },
                        label = { Text(category.label) },
                    )
                }
                if (subject.manualYuriCategory != null) {
                    item {
                        TextButton(onClick = { onSetAiCategoryOverride(subject, null) }) {
                            Text("跟随 AI")
                        }
                    }
                }
            }
            TextButton(
                onClick = { onAnalyze(subject) },
                enabled = !analyzing,
                modifier = Modifier.align(Alignment.Start),
            ) {
                Text(if (analysis == null) "开始 AI 分析" else "重新分析")
            }
        }

        if (subject.tags.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            ) {
                items(subject.tags) { tag ->
                    AssistChip(onClick = {}, label = { Text(tag) })
                }
            }
        }

        SectionTitle("评分分布")
        RatingDistribution(subject.ratingCounts)

        SectionTitle("收藏统计")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CollectionStat("想看", subject.wish)
            CollectionStat("看过", subject.collect)
            CollectionStat("在看", subject.doing)
            CollectionStat("搁置", subject.onHold)
            CollectionStat("抛弃", subject.dropped)
        }

        if (subject.summary.isNotBlank()) {
            SectionTitle("简介")
            Text(
                text = subject.summary,
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        if (subject.infoboxText.isNotBlank()) {
            SectionTitle("基本资料")
            Text(
                text = subject.infoboxText,
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Text(
            text = "条目、标签和评分数据来自 Bangumi；本页不读取评论区。",
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun DetailMessageBanner(
    message: String,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            style = MaterialTheme.typography.bodySmall,
        )
        TextButton(onClick = onDismiss) {
            Text("关闭", color = MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 10.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun RatingDistribution(counts: List<Int>) {
    val maximum = counts.maxOrNull()?.coerceAtLeast(1) ?: 1
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        (10 downTo 1).forEach { score ->
            val count = counts.getOrElse(score - 1) { 0 }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$score", modifier = Modifier.width(24.dp), style = MaterialTheme.typography.labelSmall)
                LinearProgressIndicator(
                    progress = { count.toFloat() / maximum },
                    modifier = Modifier
                        .weight(1f)
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp)),
                )
                Text(
                    "$count",
                    modifier = Modifier.width(50.dp),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun CollectionStat(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
