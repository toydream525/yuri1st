package com.yurishelf.app.domain

enum class CatalogType(
    val apiValue: Int,
    val label: String,
    val requiredMetaTags: List<String> = emptyList(),
) {
    ANIME(apiValue = 2, label = "动画"),
    MANGA(apiValue = 1, label = "漫画", requiredMetaTags = listOf("漫画")),
    LIGHT_NOVEL(apiValue = 1, label = "轻小说", requiredMetaTags = listOf("小说")),
    GAME(apiValue = 4, label = "游戏"),
}

enum class YuriCategory(val label: String) {
    STRONG("真百"),
    LIGHT("轻百"),
    AMBIGUOUS("疑似"),
}

enum class AiYuriCategory(val label: String) {
    STRONG("真百"),
    LIGHT("轻百"),
    NON("非百"),
    UNKNOWN("未知"),
}

enum class BangumiCollectionType(val apiValue: Int, val label: String) {
    WISH(1, "想看"),
    COLLECT(2, "看过"),
    DOING(3, "在看"),
    ON_HOLD(4, "搁置"),
    DROPPED(5, "抛弃"),
    ;

    companion object {
        fun fromApiValue(value: Int?): BangumiCollectionType? =
            entries.firstOrNull { it.apiValue == value }
    }
}

enum class SortOption(val label: String) {
    SCORE("评分"),
    VOTES("评分人数"),
    RANK("排名"),
    DATE("日期"),
}

enum class ThemeMode(val label: String) {
    SYSTEM("跟随系统"),
    LIGHT("浅色"),
    DARK("夜间"),
}

enum class CatalogViewMode(val label: String) {
    LIST("列表"),
    GRID("封面"),
}

enum class SubjectFormat(val label: String) {
    TV("TV 动画"),
    OVA("OVA"),
    MOVIE("剧场版"),
    OTHER("其他"),
}

enum class WinLose(val label: String) {
    WIN("赢面"),
    LOSE("输面"),
}

data class SubjectKey(
    val id: Int,
    val catalogType: CatalogType,
) {
    val cacheKey: String get() = "${catalogType.name}:$id"
}

data class AiAnalysis(
    val subjectId: Int,
    val catalogType: CatalogType,
    val category: AiYuriCategory,
    val confidence: Double,
    val reason: String,
    val riskPoints: List<String>,
    val sources: List<String>,
    val analyzedAt: Long,
) {
    val cacheKey: String get() = "${catalogType.name}:$subjectId"
}

data class CatalogFilters(
    val type: CatalogType = CatalogType.ANIME,
    val query: String = "",
    val sort: SortOption = SortOption.SCORE,
    val minimumVotes: Int = 0,
    val year: Int? = null,
    val favoritesOnly: Boolean = false,
    val includeNsfw: Boolean = false,
    val nsfwOnly: Boolean = false,
    val aiCategory: AiYuriCategory? = null,
    val winLose: WinLose? = null,
    val format: SubjectFormat? = null,
)

data class SubjectPage(
    val items: List<Subject>,
    val currentPage: Int,
    val totalPages: Int,
    val totalItems: Int,
)

fun paginateSubjects(subjects: List<Subject>, requestedPage: Int, pageSize: Int = 20): SubjectPage {
    require(pageSize > 0) { "pageSize must be positive" }
    val totalPages = ((subjects.size + pageSize - 1) / pageSize).coerceAtLeast(1)
    val currentPage = requestedPage.coerceIn(1, totalPages)
    val fromIndex = (currentPage - 1) * pageSize
    return SubjectPage(
        items = subjects.drop(fromIndex).take(pageSize),
        currentPage = currentPage,
        totalPages = totalPages,
        totalItems = subjects.size,
    )
}

data class Subject(
    val id: Int,
    val type: CatalogType,
    val name: String,
    val nameCn: String,
    val summary: String,
    val date: String,
    val platform: String,
    val imageUrl: String,
    val ratingScore: Double,
    val ratingTotal: Int,
    val rank: Int,
    val ratingCounts: List<Int>,
    val wish: Int,
    val collect: Int,
    val doing: Int,
    val onHold: Int,
    val dropped: Int,
    val tags: List<String>,
    val metaTags: List<String>,
    val infoboxText: String,
    val episodeCount: Int,
    val isFavorite: Boolean,
    val nsfw: Boolean,
    val isBlocked: Boolean = false,
    val aiAnalysis: AiAnalysis? = null,
    val bangumiCollectionType: BangumiCollectionType? = null,
    val winLose: WinLose? = null,
) {
    val key: SubjectKey get() = SubjectKey(id, type)
    val displayName: String get() = nameCn.ifBlank { name }
    val secondaryName: String get() = if (nameCn.isNotBlank() && nameCn != name) name else ""
    val categoryLabel: String get() = classifySubject(type, metaTags, platform)
    val yuriCategory: YuriCategory get() = classifyYuri(tags, metaTags)
}

fun Subject.withAiAnalysis(analysis: AiAnalysis?): Subject = copy(aiAnalysis = analysis)

fun classifySubject(type: CatalogType, metaTags: List<String>, platform: String): String {
    if (type == CatalogType.GAME) return platform.ifBlank { "游戏平台未标注" }
    if (type == CatalogType.MANGA) {
        return when {
            metaTags.any { it.contains("连载") } -> "连载漫画"
            metaTags.any { it.contains("单行本") } -> "单行本"
            else -> "漫画"
        }
    }
    if (type == CatalogType.LIGHT_NOVEL) return "轻小说"

    val normalized = metaTags.map(String::uppercase)
    return when {
        "TV" in normalized -> "TV 动画"
        "WEB" in normalized -> "WEB 动画"
        "OVA" in normalized -> "OVA"
        "OAD" in normalized -> "OAD"
        metaTags.any { it.contains("剧场") } -> "剧场版"
        else -> "动画"
    }
}

fun classifyYuri(tags: List<String>, metaTags: List<String>): YuriCategory {
    val normalizedTags = tags.map { it.trim() }
    val normalizedMeta = metaTags.map { it.trim() }
    return when {
        normalizedTags.contains("百合") -> YuriCategory.STRONG
        normalizedTags.contains("轻百合") || normalizedTags.contains("輕百合") -> YuriCategory.LIGHT
        normalizedMeta.any { it.contains("百合") } -> YuriCategory.AMBIGUOUS
        else -> YuriCategory.AMBIGUOUS
    }
}

fun Subject.subjectFormat(): SubjectFormat {
    val normalized = metaTags.map(String::uppercase)
    return when {
        normalized.any { it == "TV" } -> SubjectFormat.TV
        normalized.any { it == "OVA" || it == "OAD" } -> SubjectFormat.OVA
        metaTags.any { it.contains("剧场") } -> SubjectFormat.MOVIE
        else -> SubjectFormat.OTHER
    }
}

fun filterAndSortSubjects(
    subjects: List<Subject>,
    filters: CatalogFilters,
    blockWords: List<String> = emptyList(),
): List<Subject> {
    val query = filters.query.trim()
    val activeBlockWords = blockWords.map { it.trim() }.filter { it.isNotEmpty() }
    val filtered = subjects.filter { subject ->
        !subject.isBlocked &&
            !matchesAnyBlockWord(subject, activeBlockWords) &&
            subject.ratingTotal >= filters.minimumVotes &&
            (filters.year == null || subject.date.take(4).toIntOrNull() == filters.year) &&
            (!filters.favoritesOnly || subject.isFavorite) &&
            (filters.includeNsfw || !subject.nsfw) &&
            (!filters.nsfwOnly || subject.nsfw) &&
            (filters.aiCategory == null || subject.aiAnalysis?.category == filters.aiCategory) &&
            (filters.winLose == null || subject.winLose == filters.winLose) &&
            (filters.format == null || subject.subjectFormat() == filters.format) &&
            (query.isEmpty() || sequenceOf(
                subject.name,
                subject.nameCn,
                subject.platform,
                subject.infoboxText,
            )
                .plus(subject.tags.asSequence())
                .plus(subject.metaTags.asSequence())
                .any { it.contains(query, ignoreCase = true) })
    }

    return when (filters.sort) {
        SortOption.SCORE -> filtered.sortedWith(
            compareByDescending<Subject> { it.ratingScore > 0 }
                .thenByDescending { it.ratingScore }
                .thenByDescending { it.ratingTotal },
        )
        SortOption.VOTES -> filtered.sortedByDescending { it.ratingTotal }
        SortOption.RANK -> filtered.sortedWith(
            compareBy<Subject> { if (it.rank > 0) it.rank else Int.MAX_VALUE }
                .thenByDescending { it.ratingTotal },
        )
        SortOption.DATE -> filtered.sortedByDescending { it.date }
    }
}

fun matchesAnyBlockWord(subject: Subject, blockWords: List<String>): Boolean {
    if (blockWords.isEmpty()) return false
    val haystack = sequenceOf(subject.name, subject.nameCn, subject.platform, subject.infoboxText)
        .plus(subject.tags.asSequence())
        .plus(subject.metaTags.asSequence())
    return blockWords.any { word ->
        haystack.any { it.contains(word, ignoreCase = true) }
    }
}

fun pickRandomSubjects(
    subjects: List<Subject>,
    minimumScore: Double,
    count: Int,
    random: kotlin.random.Random = kotlin.random.Random.Default,
    blockWords: List<String> = emptyList(),
): List<Subject> {
    require(count >= 0) { "count must be non-negative" }
    val activeBlockWords = blockWords.map { it.trim() }.filter { it.isNotEmpty() }
    return subjects.asSequence()
        .filter {
            !it.isBlocked &&
                !matchesAnyBlockWord(it, activeBlockWords) &&
                it.ratingScore >= minimumScore
        }
        .shuffled(random)
        .take(count)
        .toList()
}
