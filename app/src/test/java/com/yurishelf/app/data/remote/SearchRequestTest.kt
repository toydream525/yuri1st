package com.yurishelf.app.data.remote

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchRequestTest {
    @Test
    fun emitsTagOnlyAnimeQueryExpectedByBangumiV0() {
        val request = SearchSubjectsRequest(
            keyword = "",
            sort = "rank",
            filter = SearchFilter(type = listOf(2), tag = listOf("百合"), nsfw = false),
        )

        val root = Json.parseToJsonElement(Json.encodeToString(request)).jsonObject
        val filter = root.getValue("filter").jsonObject

        assertEquals("", root.getValue("keyword").jsonPrimitive.content)
        assertEquals(2, filter.getValue("type").jsonArray.single().jsonPrimitive.content.toInt())
        assertEquals("百合", filter.getValue("tag").jsonArray.single().jsonPrimitive.content)
        assertEquals("false", filter.getValue("nsfw").jsonPrimitive.content)
    }

    @Test
    fun mangaQueryAddsMetaTagAndExplicitNsfwFlag() {
        val request = SearchSubjectsRequest(
            keyword = "",
            filter = SearchFilter(
                type = listOf(1),
                tag = listOf("百合"),
                metaTags = listOf("漫画"),
                nsfw = true,
            ),
        )

        val root = Json.parseToJsonElement(Json.encodeToString(request)).jsonObject
        val filter = root.getValue("filter").jsonObject

        assertEquals("漫画", filter.getValue("meta_tags").jsonArray.single().jsonPrimitive.content)
        assertEquals("true", filter.getValue("nsfw").jsonPrimitive.content)
    }

    @Test
    fun lightNovelQueryUsesItsOwnMetaTag() {
        val request = SearchSubjectsRequest(
            keyword = "",
            filter = SearchFilter(
                type = listOf(1),
                tag = listOf("百合"),
                metaTags = listOf("小说"),
                nsfw = false,
            ),
        )

        val root = Json.parseToJsonElement(Json.encodeToString(request)).jsonObject
        val filter = root.getValue("filter").jsonObject

        assertEquals("小说", filter.getValue("meta_tags").jsonArray.single().jsonPrimitive.content)
    }

    @Test
    fun shardedQueryAddsAirDateRange() {
        val request = SearchSubjectsRequest(
            keyword = "",
            filter = SearchFilter(
                type = listOf(2),
                tag = listOf("百合"),
                airDate = listOf(">=2020-01-01", "<2025-01-01"),
                nsfw = false,
            ),
        )

        val root = Json.parseToJsonElement(Json.encodeToString(request)).jsonObject
        val filter = root.getValue("filter").jsonObject
        val airDate = filter.getValue("air_date").jsonArray

        assertEquals(2, airDate.size)
        assertEquals(">=2020-01-01", airDate[0].jsonPrimitive.content)
        assertEquals("<2025-01-01", airDate[1].jsonPrimitive.content)
    }
}
