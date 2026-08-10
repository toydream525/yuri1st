package com.yurishelf.app.data

import com.yurishelf.app.data.remote.SubjectDto
import com.yurishelf.app.data.remote.TagDto
import com.yurishelf.app.domain.CatalogType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubjectMapperTest {
    @Test
    fun onlyAcceptsExactYuriTag() {
        val exact = SubjectDto(1, 2, "Exact", tags = listOf(TagDto("百合")))
        val relatedOnly = SubjectDto(2, 2, "Related", tags = listOf(TagDto("轻百合")))

        assertTrue(exact.hasExactYuriTag())
        assertFalse(relatedOnly.hasExactYuriTag())
        assertTrue(relatedOnly.hasExactTag("轻百合"))
    }

    @Test
    fun regressionSubject495291UrlParsesAndMatchesCatalogRule() {
        assertEquals(495291, parseSubjectReference("https://bgm.tv/subject/495291"))
        val yuri = SubjectDto(495291, 2, "Yuri entry", tags = listOf(TagDto("百合")))
        assertTrue(yuri.isYuriEntry())
        val unrelated = SubjectDto(495291, 2, "Unrelated", tags = listOf(TagDto("奇幻")))
        assertFalse(unrelated.isYuriEntry())
    }

    @Test
    fun missingNsfwInUnpartitionedDetailFailsClosed() {
        val entity = SubjectDto(1, 2, "Unknown NSFW").toEntity(
            existing = null,
            now = 1,
            isDetail = true,
            catalogType = CatalogType.ANIME,
        )

        assertTrue(entity.nsfw)
    }

    @Test
    fun missingNsfwInDetailPreservesKnownExistingValue() {
        val existing = SubjectDto(1, 2, "Known safe").toEntity(
            existing = null,
            now = 1,
            isDetail = false,
            catalogType = CatalogType.ANIME,
            nsfwFallback = false,
        )

        val refreshed = SubjectDto(1, 2, "Known safe").toEntity(
            existing = existing,
            now = 2,
            isDetail = true,
            catalogType = CatalogType.ANIME,
        )

        assertFalse(refreshed.nsfw)
    }

    @Test
    fun missingNsfwInSearchUsesExplicitPartition() {
        val safeEntity = SubjectDto(1, 2, "Safe partition").toEntity(
            existing = null,
            now = 1,
            isDetail = false,
            catalogType = CatalogType.ANIME,
            nsfwFallback = false,
        )
        val restrictedEntity = SubjectDto(2, 2, "Restricted partition").toEntity(
            existing = null,
            now = 1,
            isDetail = false,
            catalogType = CatalogType.ANIME,
            nsfwFallback = true,
        )

        assertFalse(safeEntity.nsfw)
        assertTrue(restrictedEntity.nsfw)
    }

    @Test
    fun catalogTypeForSubjectUsesMetaTagsForTypeOne() {
        val manga = SubjectDto(
            id = 1,
            type = 1,
            name = "Manga",
            metaTags = listOf("漫画"),
            tags = listOf(TagDto("百合")),
        )
        val novel = SubjectDto(
            id = 2,
            type = 1,
            name = "Novel",
            metaTags = listOf("小说"),
            tags = listOf(TagDto("百合")),
        )

        assertEquals(
            CatalogType.MANGA,
            catalogTypeForSubject(manga, preferred = CatalogType.LIGHT_NOVEL),
        )
        assertEquals(
            CatalogType.LIGHT_NOVEL,
            catalogTypeForSubject(novel, preferred = CatalogType.MANGA),
        )
    }

    @Test
    fun catalogTypeForSubjectFallsBackToPreferredOrFirstMatchingApiType() {
        val manga = SubjectDto(
            id = 3,
            type = 1,
            name = "Manga",
            metaTags = emptyList(),
            tags = listOf(TagDto("百合")),
        )

        assertEquals(
            CatalogType.MANGA,
            catalogTypeForSubject(manga, preferred = CatalogType.MANGA),
        )
        assertEquals(
            CatalogType.MANGA,
            catalogTypeForSubject(manga, preferred = CatalogType.ANIME),
        )
    }

    @Test
    fun catalogTypeForSubjectFallsBackToPreferredForUnknownApiType() {
        val real = SubjectDto(
            id = 4,
            type = 6,
            name = "Real",
            metaTags = emptyList(),
            tags = listOf(TagDto("百合")),
        )

        assertEquals(
            CatalogType.ANIME,
            catalogTypeForSubject(real, preferred = CatalogType.ANIME),
        )
    }
}
