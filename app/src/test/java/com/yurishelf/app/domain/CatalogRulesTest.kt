package com.yurishelf.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CatalogRulesTest {
    @Test
    fun animeCategoryPrefersKnownMetaTag() {
        assertEquals("TV 动画", classifySubject(CatalogType.ANIME, listOf("日本", "TV"), ""))
        assertEquals("剧场版", classifySubject(CatalogType.ANIME, listOf("日本", "剧场版"), ""))
    }

    @Test
    fun gameCategoryUsesPlatform() {
        assertEquals("Nintendo Switch", classifySubject(CatalogType.GAME, emptyList(), "Nintendo Switch"))
    }

    @Test
    fun mangaHasASeparateCategory() {
        assertEquals("漫画", classifySubject(CatalogType.MANGA, listOf("漫画"), ""))
        assertEquals("连载漫画", classifySubject(CatalogType.MANGA, listOf("漫画", "连载中"), ""))
    }

    @Test
    fun lightNovelHasASeparateCategory() {
        assertEquals(
            "轻小说",
            classifySubject(CatalogType.LIGHT_NOVEL, listOf("轻小说"), ""),
        )
        assertEquals(listOf("小说"), CatalogType.LIGHT_NOVEL.requiredMetaTags)
    }

    @Test
    fun filtersByReleaseYear() {
        val old = subject(id = 10, score = 8.0, votes = 100).copy(date = "2024-01-01")
        val recent = subject(id = 11, score = 8.0, votes = 100).copy(date = "2025-06-01")

        assertEquals(
            listOf(11),
            filterAndSortSubjects(listOf(old, recent), CatalogFilters(year = 2025)).map { it.id },
        )
    }

    @Test
    fun paginatesTwentyItemsAndClampsLastPage() {
        val subjects = (1..45).map { subject(id = it, score = 8.0, votes = 100) }

        val second = paginateSubjects(subjects, requestedPage = 2)
        val beyondLast = paginateSubjects(subjects, requestedPage = 99)

        assertEquals(20, second.items.size)
        assertEquals(2, second.currentPage)
        assertEquals(3, second.totalPages)
        assertEquals(5, beyondLast.items.size)
        assertEquals(3, beyondLast.currentPage)
    }

    @Test
    fun filtersAndSortsByReliableScore() {
        val lowVotes = subject(id = 1, score = 9.5, votes = 2)
        val reliable = subject(id = 2, score = 8.1, votes = 300)

        val result = filterAndSortSubjects(
            subjects = listOf(lowVotes, reliable),
            filters = CatalogFilters(minimumVotes = 50, sort = SortOption.SCORE),
        )

        assertEquals(listOf(2), result.map { it.id })
    }

    @Test
    fun searchMatchesTags() {
        val entry = subject(id = 3, score = 7.8, votes = 100).copy(tags = listOf("百合", "校园"))
        val result = filterAndSortSubjects(listOf(entry), CatalogFilters(query = "校园"))
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun searchMatchesInfoboxAndMetaTags() {
        val entry = subject(id = 5, score = 7.8, votes = 100).copy(
            infoboxText = "原作：某某原作\n导演：某某导演",
            metaTags = listOf("TV", "WEB"),
        )

        assertTrue(filterAndSortSubjects(listOf(entry), CatalogFilters(query = "原作")).isNotEmpty())
        assertTrue(filterAndSortSubjects(listOf(entry), CatalogFilters(query = "某某导演")).isNotEmpty())
        assertTrue(filterAndSortSubjects(listOf(entry), CatalogFilters(query = "WEB")).isNotEmpty())
    }

    @Test
    fun blockedEntriesAreHidden() {
        val blocked = subject(id = 6, score = 7.5, votes = 80).copy(isBlocked = true)
        val visible = subject(id = 7, score = 7.5, votes = 80)

        val result = filterAndSortSubjects(listOf(blocked, visible), CatalogFilters())

        assertEquals(listOf(7), result.map { it.id })
    }

    @Test
    fun randomPicksRespectScoreAndExcludeBlocked() {
        val entries = (1..10).map {
            subject(id = it, score = 5.0 + it * 0.5, votes = 100)
        } + subject(id = 99, score = 9.5, votes = 100).copy(isBlocked = true)

        val picks = pickRandomSubjects(
            subjects = entries,
            minimumScore = 8.0,
            count = 5,
            random = Random(42),
        )

        assertEquals(5, picks.size)
        assertTrue(picks.all { it.ratingScore >= 8.0 })
        assertFalse(picks.any { it.id == 99 })
    }

    @Test
    fun classifiesYuriCategories() {
        assertEquals(YuriCategory.STRONG, classifyYuri(listOf("百合"), emptyList()))
        assertEquals(YuriCategory.LIGHT, classifyYuri(listOf("轻百合"), emptyList()))
        assertEquals(YuriCategory.AMBIGUOUS, classifyYuri(listOf("恋爱"), listOf("百合")))
    }

    @Test
    fun aiCategoryFilterSelectsOnlyCachedAnalysisLevel() {
        val strong = subject(id = 8, score = 8.0, votes = 100).copy(
            aiAnalysis = AiAnalysis(
                subjectId = 8,
                catalogType = CatalogType.ANIME,
                category = AiYuriCategory.STRONG,
                confidence = 0.9,
                reason = "",
                riskPoints = emptyList(),
                sources = emptyList(),
                analyzedAt = 0,
            ),
        )
        val light = subject(id = 9, score = 8.0, votes = 100).copy(
            aiAnalysis = AiAnalysis(
                subjectId = 9,
                catalogType = CatalogType.ANIME,
                category = AiYuriCategory.LIGHT,
                confidence = 0.8,
                reason = "",
                riskPoints = emptyList(),
                sources = emptyList(),
                analyzedAt = 0,
            ),
        )

        assertEquals(
            listOf(8),
            filterAndSortSubjects(
                listOf(strong, light),
                CatalogFilters(aiCategory = AiYuriCategory.STRONG),
            ).map { it.id },
        )
        assertEquals(
            listOf(9),
            filterAndSortSubjects(
                listOf(strong, light),
                CatalogFilters(aiCategory = AiYuriCategory.LIGHT),
            ).map { it.id },
        )
    }

    @Test
    fun aiCategoryFilterUsesManualOverrideWhenPresent() {
        val overridden = subject(id = 10, score = 8.0, votes = 100).copy(
            aiAnalysis = AiAnalysis(
                subjectId = 10,
                catalogType = CatalogType.ANIME,
                category = AiYuriCategory.LIGHT,
                confidence = 0.8,
                reason = "",
                riskPoints = emptyList(),
                sources = emptyList(),
                analyzedAt = 0,
            ),
            manualYuriCategory = AiYuriCategory.STRONG,
        )

        assertEquals(
            listOf(10),
            filterAndSortSubjects(
                listOf(overridden),
                CatalogFilters(aiCategory = AiYuriCategory.STRONG),
            ).map { it.id },
        )
    }

    @Test
    fun winLoseFilterSelectsOnlyCustomLabels() {
        val win = subject(id = 11, score = 8.0, votes = 100).copy(winLose = WinLose.WIN)
        val lose = subject(id = 12, score = 8.0, votes = 100).copy(winLose = WinLose.LOSE)
        val none = subject(id = 13, score = 8.0, votes = 100)

        assertEquals(
            listOf(11),
            filterAndSortSubjects(
                listOf(win, lose, none),
                CatalogFilters(winLose = WinLose.WIN),
            ).map { it.id },
        )
        assertEquals(
            listOf(12),
            filterAndSortSubjects(
                listOf(win, lose, none),
                CatalogFilters(winLose = WinLose.LOSE),
            ).map { it.id },
        )
    }

    @Test
    fun blockWordsHideMatchingNamesAndInfobox() {
        val entry = subject(id = 10, score = 8.0, votes = 100).copy(
            name = "魔法少女小圆",
            infoboxText = "原作：虚渊玄",
        )

        assertTrue(filterAndSortSubjects(listOf(entry), CatalogFilters(), listOf("小圆")).isEmpty())
        assertTrue(filterAndSortSubjects(listOf(entry), CatalogFilters(), listOf("虚渊玄")).isEmpty())
        assertFalse(filterAndSortSubjects(listOf(entry), CatalogFilters(), listOf("偶像")).isEmpty())
    }

    @Test
    fun randomPicksExcludeBlockWords() {
        val entry = subject(id = 11, score = 9.0, votes = 100).copy(name = "某作品")
        val picks = pickRandomSubjects(
            subjects = listOf(entry),
            minimumScore = 8.0,
            count = 5,
            random = Random(1),
            blockWords = listOf("作品"),
        )

        assertTrue(picks.isEmpty())
    }

    @Test
    fun nsfwEntriesRequireExplicitOptIn() {
        val entry = subject(id = 4, score = 7.5, votes = 80).copy(nsfw = true)

        assertTrue(filterAndSortSubjects(listOf(entry), CatalogFilters()).isEmpty())
        assertEquals(
            listOf(4),
            filterAndSortSubjects(listOf(entry), CatalogFilters(includeNsfw = true)).map { it.id },
        )
    }

    @Test
    fun nsfwOnlyFilterShowsOnlyNsfwEntries() {
        val safe = subject(id = 20, score = 8.0, votes = 100)
        val restricted = subject(id = 21, score = 8.0, votes = 100).copy(nsfw = true)

        val result = filterAndSortSubjects(
            listOf(safe, restricted),
            CatalogFilters(includeNsfw = true, nsfwOnly = true),
        )

        assertEquals(listOf(21), result.map { it.id })
    }

    @Test
    fun formatFilterClassifiesAndFiltersBySubjectFormat() {
        val tv = subject(id = 1, score = 8.0, votes = 100)
        val movie = subject(id = 2, score = 8.0, votes = 100).copy(metaTags = listOf("剧场版"))
        val ova = subject(id = 3, score = 8.0, votes = 100).copy(metaTags = listOf("OVA"))
        val web = subject(id = 4, score = 8.0, votes = 100).copy(metaTags = listOf("WEB"))

        assertEquals(SubjectFormat.TV, tv.subjectFormat())
        assertEquals(SubjectFormat.MOVIE, movie.subjectFormat())
        assertEquals(SubjectFormat.OVA, ova.subjectFormat())
        assertEquals(SubjectFormat.OTHER, web.subjectFormat())

        val result = filterAndSortSubjects(
            listOf(tv, movie, ova, web),
            CatalogFilters(format = SubjectFormat.TV),
        )
        assertEquals(listOf(1), result.map { it.id })
    }

    @Test
    fun advancedInversionReturnsTheComplementOfAllAdvancedConditions() {
        val matching = subject(id = 1, score = 8.0, votes = 100).copy(
            date = "2025-01-01",
            aiAnalysis = analysisFor(1, AiYuriCategory.STRONG),
        )
        val wrongCategory = subject(id = 2, score = 8.0, votes = 100).copy(
            date = "2025-01-01",
            aiAnalysis = analysisFor(2, AiYuriCategory.LIGHT),
        )
        val tooFewVotes = subject(id = 3, score = 8.0, votes = 10).copy(
            date = "2025-01-01",
            aiAnalysis = analysisFor(3, AiYuriCategory.STRONG),
        )
        val wrongYear = subject(id = 4, score = 8.0, votes = 100).copy(
            date = "2024-01-01",
            aiAnalysis = analysisFor(4, AiYuriCategory.STRONG),
        )
        val filters = CatalogFilters(
            minimumVotes = 50,
            year = 2025,
            aiCategory = AiYuriCategory.STRONG,
            format = SubjectFormat.TV,
            invertAdvanced = true,
        )

        assertEquals(
            listOf(2, 4, 3),
            filterAndSortSubjects(listOf(matching, wrongCategory, tooFewVotes, wrongYear), filters)
                .map { it.id },
        )
    }

    @Test
    fun advancedInversionDoesNotReverseBasicSearchVisibilityOrBlockingRules() {
        val matching = subject(id = 1, score = 8.0, votes = 100).copy(name = "目标")
        val queryMismatch = subject(id = 2, score = 8.0, votes = 10).copy(name = "其他")
        val hiddenNsfw = subject(id = 3, score = 8.0, votes = 10).copy(name = "目标", nsfw = true)
        val blocked = subject(id = 4, score = 8.0, votes = 10).copy(name = "目标", isBlocked = true)

        assertTrue(
            filterAndSortSubjects(
                listOf(matching, queryMismatch, hiddenNsfw, blocked),
                CatalogFilters(query = "目标", minimumVotes = 50, invertAdvanced = true),
            ).isEmpty(),
        )
    }

    @Test
    fun inversionWithoutAdvancedConditionsHasNoEffect() {
        val entry = subject(id = 1, score = 8.0, votes = 100)
        assertEquals(
            listOf(1),
            filterAndSortSubjects(listOf(entry), CatalogFilters(invertAdvanced = true)).map { it.id },
        )
    }

    private fun analysisFor(id: Int, category: AiYuriCategory) = AiAnalysis(
        subjectId = id,
        catalogType = CatalogType.ANIME,
        category = category,
        confidence = 0.8,
        reason = "",
        riskPoints = emptyList(),
        sources = emptyList(),
        analyzedAt = 0,
    )

    private fun subject(id: Int, score: Double, votes: Int) = Subject(
        id = id,
        type = CatalogType.ANIME,
        name = "Subject $id",
        nameCn = "条目 $id",
        summary = "",
        date = "2025-01-01",
        platform = "",
        imageUrl = "",
        ratingScore = score,
        ratingTotal = votes,
        rank = 0,
        ratingCounts = List(10) { 0 },
        wish = 0,
        collect = 0,
        doing = 0,
        onHold = 0,
        dropped = 0,
        tags = listOf("百合"),
        metaTags = listOf("TV"),
        infoboxText = "",
        episodeCount = 12,
        isFavorite = false,
        nsfw = false,
    )
}
