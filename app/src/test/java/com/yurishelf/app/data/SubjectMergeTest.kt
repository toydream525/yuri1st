package com.yurishelf.app.data

import com.yurishelf.app.data.local.SubjectEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubjectMergeTest {
    @Test
    fun pageRefreshKeepsDetailEnrichedFieldsAfterDetailSync() {
        val existing = entity(
            id = 1,
            detailSyncedAt = 200L,
            syncedAt = 200L,
            summary = "detail summary",
            infoboxText = "detail infobox",
            episodeCount = 24,
            imageUrl = "https://example.com/large.jpg",
            platform = "TV",
            isFavorite = true,
            isBlocked = true,
        )
        val incoming = entity(
            id = 1,
            detailSyncedAt = 200L,
            syncedAt = 400L,
            summary = "page summary",
            infoboxText = "page infobox",
            episodeCount = 12,
            imageUrl = "https://example.com/small.jpg",
            platform = "",
            ratingScore = 8.8,
            isFavorite = false,
            isBlocked = false,
        )

        val merged = mergeCatalogPage(existing, incoming)

        assertEquals("detail summary", merged.summary)
        assertEquals("detail infobox", merged.infoboxText)
        assertEquals(24, merged.episodeCount)
        assertEquals("https://example.com/large.jpg", merged.imageUrl)
        assertEquals("TV", merged.platform)
        assertEquals(8.8, merged.ratingScore, 0.001)
        assertTrue(merged.isCatalogMember)
        assertTrue(merged.isFavorite)
        assertTrue(merged.isBlocked)
        assertEquals(400L, merged.syncedAt)
        assertEquals(77L, merged.catalogGeneration)
    }

    @Test
    fun pageRefreshUsesIncomingWhenNoDetailWasSynced() {
        val existing = entity(id = 2, detailSyncedAt = 0L, syncedAt = 100L, summary = "old", isFavorite = true, isBlocked = true)
        val incoming = entity(
            id = 2,
            detailSyncedAt = 0L,
            syncedAt = 300L,
            summary = "new",
            infoboxText = "page infobox",
            isFavorite = false,
            isBlocked = false,
        )

        val merged = mergeCatalogPage(existing, incoming)

        assertEquals("new", merged.summary)
        assertEquals("page infobox", merged.infoboxText)
        assertTrue(merged.isFavorite)
        assertTrue(merged.isBlocked)
    }

    @Test
    fun newRowsAreUsedAsIs() {
        val incoming = entity(id = 3, detailSyncedAt = 0L, syncedAt = 1L)

        assertEquals(incoming, mergeCatalogPage(existing = null, incoming))
    }

    @Test
    fun pageFillsBlankDetailFieldsWhenDetailWasSynced() {
        val existing = entity(
            id = 4,
            detailSyncedAt = 100L,
            syncedAt = 100L,
            summary = "",
            infoboxText = "",
            episodeCount = 0,
            imageUrl = "",
            platform = "",
        )
        val incoming = entity(
            id = 4,
            detailSyncedAt = 100L,
            syncedAt = 200L,
            summary = "page summary",
            infoboxText = "page infobox",
            episodeCount = 12,
            imageUrl = "page.jpg",
            platform = "TV",
        )

        val merged = mergeCatalogPage(existing, incoming)

        assertEquals("page summary", merged.summary)
        assertEquals("page infobox", merged.infoboxText)
        assertEquals(12, merged.episodeCount)
        assertEquals("page.jpg", merged.imageUrl)
        assertEquals("TV", merged.platform)
    }

    private fun entity(
        id: Int,
        detailSyncedAt: Long,
        syncedAt: Long,
        summary: String = "summary",
        infoboxText: String = "infobox",
        episodeCount: Int = 12,
        imageUrl: String = "image",
        platform: String = "platform",
        ratingScore: Double = 7.5,
        isFavorite: Boolean = false,
        isBlocked: Boolean = false,
    ) = SubjectEntity(
        id = id,
        type = 2,
        catalogType = "ANIME",
        name = "Name",
        nameCn = "名称",
        summary = summary,
        date = "2025-01-01",
        platform = platform,
        imageUrl = imageUrl,
        ratingScore = ratingScore,
        ratingTotal = 100,
        rank = 0,
        rating1 = 0,
        rating2 = 0,
        rating3 = 0,
        rating4 = 0,
        rating5 = 0,
        rating6 = 0,
        rating7 = 10,
        rating8 = 40,
        rating9 = 40,
        rating10 = 10,
        wish = 0,
        collect = 0,
        doing = 0,
        onHold = 0,
        dropped = 0,
        tagsJson = "[\"百合\"]",
        metaTagsJson = "[\"TV\"]",
        infoboxText = infoboxText,
        episodeCount = episodeCount,
        isFavorite = isFavorite,
        nsfw = false,
        syncedAt = syncedAt,
        detailSyncedAt = detailSyncedAt,
        isCatalogMember = true,
        catalogGeneration = if (syncedAt == 400L) 77L else 0L,
        isBlocked = isBlocked,
    )
}
