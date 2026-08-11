package com.yurishelf.app.data.ai

import com.yurishelf.app.data.remote.UpdateCollectionRequest
import com.yurishelf.app.data.remote.UserCollectionDto
import com.yurishelf.app.domain.AiYuriCategory
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiModelsTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun parseAiJson_readsCamelAndSnakeRiskPoints() {
        val payload = """
            ```json
            {
              "category": "真百",
              "confidence": 0.9,
              "reason": "主线明确描写两人交往。",
              "risk_points": ["开放式结局", "有男性主要角色"]
            }
            ```
        """.trimIndent()

        val result = parseAiJson(payload)

        assertEquals(AiYuriCategory.STRONG, result?.category)
        assertEquals(0.9, result?.confidence ?: 0.0, 0.001)
        assertEquals(listOf("开放式结局", "有男性主要角色"), result?.riskPoints)
        assertTrue(result?.reason?.contains("主线") == true)
    }

    @Test
    fun parseAiJson_clampsConfidenceAndMapsUnknownCategory() {
        val result = parseAiJson(
            """
            {"category":"未知","confidence":99,"reason":"资料不足。","riskPoints":[]}
            """.trimIndent(),
        )

        assertEquals(AiYuriCategory.UNKNOWN, result?.category)
        assertEquals(1.0, result?.confidence ?: 0.0, 0.001)
        assertEquals(emptyList<String>(), result?.riskPoints)
    }

    @Test
    fun extractJsonObject_ignoresProseAroundPayload() {
        val text = "好的，这是结果：\n{\"category\":\"轻百\",\"confidence\":0.6,\"reason\":\"互动多但主线暧昧。\",\"riskPoints\":[\"暧昧\"]}\n希望对你有帮助。"

        val extracted = extractJsonObject(text)

        assertTrue(extracted?.contains("\"category\"") == true)
        assertNull(extractJsonObject("没有 JSON"))
    }

    @Test
    fun categoryFromLabel_acceptsChineseAndEnglishNames() {
        assertEquals(AiYuriCategory.STRONG, categoryFromLabel("真百"))
        assertEquals(AiYuriCategory.LIGHT, categoryFromLabel("light"))
        assertEquals(AiYuriCategory.NON, categoryFromLabel("非百合"))
        assertEquals(AiYuriCategory.UNKNOWN, categoryFromLabel("不清楚"))
    }

    @Test
    fun defaultPrompt_isReaderFacingAndDoesNotExposeJsonProtocol() {
        assertTrue(DEFAULT_AI_PROMPT.contains("范围宜宽不宜窄"))
        assertTrue(DEFAULT_AI_PROMPT.contains("百合结局"))
        assertTrue(!DEFAULT_AI_PROMPT.contains("riskPoints"))
        assertTrue(!DEFAULT_AI_PROMPT.contains("JSON"))
    }

    @Test
    fun migrateAiPrompt_replacesLegacyDefaultAndStripsProtocolFromCustomText() {
        assertEquals(DEFAULT_AI_PROMPT, migrateAiPrompt(LEGACY_DEFAULT_AI_PROMPT))

        val migrated = migrateAiPrompt(
            """
            请更偏向真百。
            只输出 JSON。
            {"category":"真百|轻百|非百","riskPoints":[]}
            不要把 BE 当作雷点。
            """.trimIndent(),
        )
        assertTrue(migrated.contains("请更偏向真百"))
        assertTrue(migrated.contains("不要把 BE 当作雷点"))
        assertTrue(!migrated.contains("JSON"))
        assertTrue(!migrated.contains("riskPoints"))
    }

    @Test
    fun collectionDtos_roundTripSerialization() {
        val request = UpdateCollectionRequest(
            type = 3,
            comment = "在看",
            tags = listOf("百合"),
        )
        val encoded = json.encodeToString(UpdateCollectionRequest.serializer(), request)
        assertTrue(encoded.contains("\"type\":3"))
        assertTrue(encoded.contains("\"comment\":\"在看\""))
        assertTrue(encoded.contains("\"tags\":[\"百合\"]"))

        val userCollection = json.decodeFromString<UserCollectionDto>(
            """
            {
              "subject_id": 42,
              "subject_type": 2,
              "type": 4,
              "rate": 8,
              "comment": "搁置中",
              "tags": ["百合"],
              "ep_status": 3,
              "vol_status": 0,
              "updated_at": "2026-01-01T00:00:00+08:00",
              "private": false
            }
            """.trimIndent(),
        )
        assertEquals(42, userCollection.subjectId)
        assertEquals(4, userCollection.type)
        assertEquals(8, userCollection.rate)
        assertEquals(listOf("百合"), userCollection.tags)
    }
}
