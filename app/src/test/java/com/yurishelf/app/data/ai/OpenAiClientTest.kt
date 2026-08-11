package com.yurishelf.app.data.ai

import com.yurishelf.app.domain.AiYuriCategory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class OpenAiClientTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun chatCompletions_parsesResultAndSendsResponseFormat() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "choices": [
                        {
                          "message": {
                            "content": "{\"category\":\"轻百\",\"confidence\":0.7,\"reason\":\"互动多。\",\"riskPoints\":[\"暧昧\"]}"
                          }
                        }
                      ]
                    }
                    """.trimIndent(),
                ),
        )

        val result = client().analyze(
            baseUrl = server.url("/v1").toString().trimEnd('/'),
            apiKey = "test-key",
            model = "test-model",
            prompt = "输出 JSON",
            webSearchEnabled = true,
            context = context(),
        )

        val recorded = server.takeRequest()
        assertEquals("/v1/chat/completions", recorded.path)
        assertEquals("Bearer test-key", recorded.getHeader("Authorization"))
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("\"response_format\":{\"type\":\"json_object\"}"))
        assertTrue(body.contains("\"model\":\"test-model\""))
        val systemPrompt = Json.parseToJsonElement(body).jsonObject["messages"]
            ?.jsonArray
            ?.first()
            ?.jsonObject
            ?.get("content")
            ?.jsonPrimitive
            ?.content
            .orEmpty()
        assertTrue(systemPrompt.contains("输出 JSON"))
        assertTrue(systemPrompt.contains("\"riskPoints\""))
        assertEquals(AiYuriCategory.LIGHT, result.category)
        assertEquals(0.7, result.confidence, 0.001)
        assertEquals(listOf("暧昧"), result.riskPoints)
    }

    @Test
    fun chatCompletions_retriesWithoutResponseFormatOn400() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"error":{"message":"unsupported"}}"""),
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {"choices":[{"message":{"content":"{\"category\":\"真百\",\"confidence\":0.9,\"reason\":\"明确恋爱。\",\"riskPoints\":[]}"}}]}
                    """.trimIndent(),
                ),
        )

        val result = client().analyze(
            baseUrl = server.url("/v1").toString().trimEnd('/'),
            apiKey = "k",
            model = "m",
            prompt = "输出 JSON",
            webSearchEnabled = false,
            context = context(),
        )

        assertEquals(2, server.requestCount)
        val first = server.takeRequest().body.readUtf8()
        val second = server.takeRequest().body.readUtf8()
        assertTrue(first.contains("response_format"))
        assertTrue(!second.contains("response_format"))
        assertEquals(AiYuriCategory.STRONG, result.category)
    }

    @Test
    fun responsesPath_usesWebSearchToolAndParsesOutputText() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "output": [
                        {
                          "type": "message",
                          "content": [
                            {
                              "type": "output_text",
                              "text": "{\"category\":\"非百\",\"confidence\":0.6,\"reason\":\"标签误导。\",\"riskPoints\":[\"后宫元素\"],\"sources\":[\"Bangumi 条目页\"]}"
                            }
                          ]
                        }
                      ]
                    }
                    """.trimIndent(),
                ),
        )

        val result = OpenAiClient(OkHttpClient(), forceResponses = true).analyze(
            baseUrl = server.url("/v1").toString().trimEnd('/'),
            apiKey = "k",
            model = "m",
            prompt = "输出 JSON",
            webSearchEnabled = true,
            context = context(),
        )

        val recorded = server.takeRequest()
        assertEquals("/v1/responses", recorded.path)
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("\"type\":\"web_search\""))
        assertTrue(body.contains("\"input\""))
        assertEquals(AiYuriCategory.NON, result.category)
        assertEquals(listOf("Bangumi 条目页"), result.sources)
    }

    private fun client() = OpenAiClient(OkHttpClient())

    private fun context() = AiSubjectContext(
        title = "测试作品",
        titleCn = "",
        typeLabel = "动画",
        date = "2026-01-01",
        platform = "TV",
        score = "8.0（100 人评分）",
        summary = "简介",
        tags = listOf("百合"),
        metaTags = listOf("TV"),
        infobox = "原作：某人",
    )
}
