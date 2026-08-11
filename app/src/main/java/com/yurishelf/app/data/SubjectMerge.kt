package com.yurishelf.app.data

import com.yurishelf.app.data.local.SubjectEntity

/**
 * Merges an incoming catalog-page row with an existing stored row.
 *
 * Catalog pages are summaries; detail refreshes carry richer fields. Once a
 * detail sync has happened for a row, keep the detail-enriched fields
 * (summary, infobox, episode count, image, platform) while still refreshing
 * the volatile numeric/status fields (rating, rank, collection counts, tags,
 * nsfw partition) from the page. Favorites and blocked state are always
 * preserved.
 */
fun mergeCatalogPage(
    existing: SubjectEntity?,
    incoming: SubjectEntity,
): SubjectEntity {
    if (existing == null) return incoming

    val merged = if (existing.detailSyncedAt > 0) {
        existing.copy(
            isCatalogMember = true,
            catalogGeneration = incoming.catalogGeneration,
            syncedAt = maxOf(existing.syncedAt, incoming.syncedAt),
            name = incoming.name,
            nameCn = incoming.nameCn,
            date = incoming.date,
            summary = existing.summary.ifBlank { incoming.summary },
            platform = existing.platform.ifBlank { incoming.platform },
            imageUrl = existing.imageUrl.ifBlank { incoming.imageUrl },
            ratingScore = incoming.ratingScore,
            ratingTotal = incoming.ratingTotal,
            rank = incoming.rank,
            rating1 = incoming.rating1,
            rating2 = incoming.rating2,
            rating3 = incoming.rating3,
            rating4 = incoming.rating4,
            rating5 = incoming.rating5,
            rating6 = incoming.rating6,
            rating7 = incoming.rating7,
            rating8 = incoming.rating8,
            rating9 = incoming.rating9,
            rating10 = incoming.rating10,
            wish = incoming.wish,
            collect = incoming.collect,
            doing = incoming.doing,
            onHold = incoming.onHold,
            dropped = incoming.dropped,
            tagsJson = incoming.tagsJson,
            metaTagsJson = incoming.metaTagsJson,
            infoboxText = existing.infoboxText.ifBlank { incoming.infoboxText },
            episodeCount = if (existing.episodeCount > 0) {
                existing.episodeCount
            } else {
                incoming.episodeCount
            },
            nsfw = incoming.nsfw,
        )
    } else {
        incoming
    }

    return merged.copy(
        isFavorite = existing.isFavorite,
        isBlocked = existing.isBlocked,
        bangumiCollectionType = existing.bangumiCollectionType,
        bangumiCollectionSyncedAt = existing.bangumiCollectionSyncedAt,
        winLose = existing.winLose,
        manualYuriCategory = existing.manualYuriCategory,
    )
}
