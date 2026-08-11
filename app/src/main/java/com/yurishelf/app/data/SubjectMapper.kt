package com.yurishelf.app.data

import com.yurishelf.app.data.local.SubjectEntity
import com.yurishelf.app.data.remote.SubjectDto
import com.yurishelf.app.domain.BangumiCollectionType
import com.yurishelf.app.domain.CatalogType
import com.yurishelf.app.domain.Subject
import com.yurishelf.app.domain.WinLose
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

private val storageJson = Json { ignoreUnknownKeys = true }

fun SubjectDto.hasExactTag(tag: String): Boolean = tags.any { it.name == tag }

fun SubjectDto.hasExactYuriTag(): Boolean = hasExactTag("百合")

fun SubjectDto.isYuriEntry(): Boolean = hasExactTag("百合") || hasExactTag("轻百合")

/**
 * Chooses the catalog bucket for a subject fetched outside the regular
 * partition sync. Bangumi uses the same numeric type for manga and light
 * novels, so meta tags decide between [CatalogType.MANGA] and
 * [CatalogType.LIGHT_NOVEL]; the caller's current tab breaks ties.
 */
fun catalogTypeForSubject(dto: SubjectDto, preferred: CatalogType): CatalogType {
    val sameApiValue = CatalogType.entries.filter { it.apiValue == dto.type }
    if (sameApiValue.isEmpty()) return preferred
    val metaMatched = sameApiValue.filter { it.requiredMetaTags.all(dto.metaTags::contains) }
    return metaMatched.firstOrNull { it == preferred }
        ?: metaMatched.firstOrNull()
        ?: preferred.takeIf { it in sameApiValue }
        ?: sameApiValue.first()
}

fun SubjectDto.toEntity(
    existing: SubjectEntity?,
    now: Long,
    isDetail: Boolean,
    catalogType: CatalogType? = null,
    nsfwFallback: Boolean? = null,
    catalogGeneration: Long = existing?.catalogGeneration ?: 0,
): SubjectEntity {
    val remoteRating = rating
    val remoteCollection = collection
    val tagNames = tags.sortedByDescending { it.count }.map { it.name }
    val infoText = infobox.joinToString("\n") { item ->
        "${item.key}：${item.value.toDisplayText()}"
    }

    fun ratingCount(value: Int): Int = remoteRating?.count?.get(value.toString()) ?: 0

    return SubjectEntity(
        id = id,
        type = type,
        catalogType = (catalogType ?: existing?.catalogType?.let { CatalogType.valueOf(it) }
            ?: CatalogType.entries.first { it.apiValue == type }).name,
        name = name,
        nameCn = nameCn.orEmpty(),
        summary = summary.orEmpty().ifBlank { existing?.summary.orEmpty() },
        date = date.orEmpty().ifBlank { existing?.date.orEmpty() },
        platform = platform.orEmpty().ifBlank { existing?.platform.orEmpty() },
        imageUrl = images?.large
            ?: images?.common
            ?: images?.medium
            ?: existing?.imageUrl.orEmpty(),
        ratingScore = remoteRating?.score ?: existing?.ratingScore ?: 0.0,
        ratingTotal = remoteRating?.total ?: existing?.ratingTotal ?: 0,
        rank = remoteRating?.rank ?: existing?.rank ?: 0,
        rating1 = remoteRating?.let { ratingCount(1) } ?: existing?.rating1 ?: 0,
        rating2 = remoteRating?.let { ratingCount(2) } ?: existing?.rating2 ?: 0,
        rating3 = remoteRating?.let { ratingCount(3) } ?: existing?.rating3 ?: 0,
        rating4 = remoteRating?.let { ratingCount(4) } ?: existing?.rating4 ?: 0,
        rating5 = remoteRating?.let { ratingCount(5) } ?: existing?.rating5 ?: 0,
        rating6 = remoteRating?.let { ratingCount(6) } ?: existing?.rating6 ?: 0,
        rating7 = remoteRating?.let { ratingCount(7) } ?: existing?.rating7 ?: 0,
        rating8 = remoteRating?.let { ratingCount(8) } ?: existing?.rating8 ?: 0,
        rating9 = remoteRating?.let { ratingCount(9) } ?: existing?.rating9 ?: 0,
        rating10 = remoteRating?.let { ratingCount(10) } ?: existing?.rating10 ?: 0,
        wish = remoteCollection?.wish ?: existing?.wish ?: 0,
        collect = remoteCollection?.collect ?: existing?.collect ?: 0,
        doing = remoteCollection?.doing ?: existing?.doing ?: 0,
        onHold = remoteCollection?.onHold ?: existing?.onHold ?: 0,
        dropped = remoteCollection?.dropped ?: existing?.dropped ?: 0,
        tagsJson = if (tagNames.isNotEmpty()) storageJson.encodeToString(tagNames) else existing?.tagsJson ?: "[]",
        metaTagsJson = if (metaTags.isNotEmpty()) storageJson.encodeToString(metaTags) else existing?.metaTagsJson ?: "[]",
        infoboxText = infoText.ifBlank { existing?.infoboxText.orEmpty() },
        episodeCount = totalEpisodes ?: eps ?: existing?.episodeCount ?: 0,
        isFavorite = existing?.isFavorite ?: false,
        nsfw = nsfw ?: existing?.nsfw ?: nsfwFallback ?: true,
        syncedAt = now,
        detailSyncedAt = if (isDetail) now else existing?.detailSyncedAt ?: 0,
        isCatalogMember = if (isDetail) existing?.isCatalogMember ?: false else true,
        catalogGeneration = if (isDetail) existing?.catalogGeneration ?: 0 else catalogGeneration,
        isBlocked = existing?.isBlocked ?: false,
        bangumiCollectionType = existing?.bangumiCollectionType,
        bangumiCollectionSyncedAt = existing?.bangumiCollectionSyncedAt ?: 0,
        winLose = existing?.winLose,
        manualYuriCategory = existing?.manualYuriCategory,
    )
}

fun SubjectEntity.toDomain(): Subject = Subject(
    id = id,
    type = CatalogType.valueOf(catalogType),
    name = name,
    nameCn = nameCn,
    summary = summary,
    date = date,
    platform = platform,
    imageUrl = imageUrl,
    ratingScore = ratingScore,
    ratingTotal = ratingTotal,
    rank = rank,
    ratingCounts = listOf(
        rating1,
        rating2,
        rating3,
        rating4,
        rating5,
        rating6,
        rating7,
        rating8,
        rating9,
        rating10,
    ),
    wish = wish,
    collect = collect,
    doing = doing,
    onHold = onHold,
    dropped = dropped,
    tags = decodeStrings(tagsJson),
    metaTags = decodeStrings(metaTagsJson),
    infoboxText = infoboxText,
    episodeCount = episodeCount,
    isFavorite = isFavorite,
    nsfw = nsfw,
    isBlocked = isBlocked,
    bangumiCollectionType = BangumiCollectionType.fromApiValue(bangumiCollectionType),
    winLose = winLose?.let { runCatching { WinLose.valueOf(it) }.getOrNull() },
    manualYuriCategory = manualYuriCategory?.let {
        runCatching { com.yurishelf.app.domain.AiYuriCategory.valueOf(it) }.getOrNull()
    },
)

private fun decodeStrings(value: String): List<String> = runCatching {
    storageJson.decodeFromString<List<String>>(value)
}.getOrDefault(emptyList())

private fun JsonElement.toDisplayText(): String = when (this) {
    is JsonPrimitive -> contentOrNull.orEmpty()
    is JsonArray -> joinToString("、") { it.toDisplayText() }
    is JsonObject -> {
        val label = this["k"]?.toDisplayText().orEmpty()
        val value = this["v"]?.toDisplayText().orEmpty()
        if (label.isBlank()) value else "$label：$value"
    }
}
