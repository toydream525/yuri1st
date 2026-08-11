package com.yurishelf.app.data.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Call
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.Closeable
import java.io.IOException
import java.net.URI

class AiRequestException(
    message: String,
    val statusCode: Int? = null,
) : IOException(message)

class MissingAiApiKeyException : IOException("尚未配置 AI API Key")

/**
 * Minimal OpenAI-compatible client:
 * - Official OpenAI endpoint: POST /responses with the `web_search` tool.
 * - Other compatible endpoints (DeepSeek, Moonshot, local OpenAI-compatible
 *   servers, ...): POST /chat/completions, retrying once without
 *   `response_format` when the provider rejects it.
 */
class OpenAiClient(
    private val callFactory: Call.Factory,
    private val forceResponses: Boolean = false,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    },
) {
    suspend fun analyze(
        baseUrl: String,
        apiKey: String,
        model: String,
        prompt: String,
        webSearchEnabled: Boolean,
        context: AiSubjectContext,
    ): AiAnalysisResult {
        val normalizedBase = baseUrl.trim().trimEnd('/')
        val useResponses = forceResponses || (webSearchEnabled && isOfficialOpenAi(normalizedBase))
        val userMessage = buildUserMessage(context)
        return if (useResponses) {
            callResponses(
                baseUrl = normalizedBase,
                apiKey = apiKey,
                model = model,
                prompt = systemPrompt(prompt),
                userMessage = userMessage,
            )
        } else {
            callChatCompletions(
                baseUrl = normalizedBase,
                apiKey = apiKey,
                model = model,
                prompt = systemPrompt(prompt),
                userMessage = userMessage,
            )
        }
    }

    private suspend fun callChatCompletions(
        baseUrl: String,
        apiKey: String,
        model: String,
        prompt: String,
        userMessage: String,
    ): AiAnalysisResult {
        val messages = listOf(
            ChatMessage(role = "system", content = prompt),
            ChatMessage(role = "user", content = userMessage),
        )
        val withFormat = ChatCompletionsRequest(
            model = model,
            messages = messages,
            temperature = 0.2,
            responseFormat = buildJsonObject { put("type", "json_object") },
        )
        var response = execute(
            baseUrl,
            "/chat/completions",
            apiKey,
            json.encodeToJsonElement(ChatCompletionsRequest.serializer(), withFormat),
        )
        if (response.statusCode == 400) {
            response.close()
            response = execute(
                baseUrl,
                "/chat/completions",
                apiKey,
                json.encodeToJsonElement(
                    ChatCompletionsRequest.serializer(),
                    withFormat.copy(responseFormat = JsonNull),
                ),
            )
        }
        response.use {
            if (it.statusCode !in 200..299) throw it.toException()
            val body = it.bodyText
            val text = runCatching {
                json.decodeFromString<ChatCompletionsResponse>(body)
                    .choices
                    .firstOrNull()
                    ?.message
                    ?.content
            }.getOrNull()
            return parseAiJson(text.orEmpty())
                ?: throw AiRequestException("AI 返回内容无法解析为 JSON", it.statusCode)
        }
    }

    private suspend fun callResponses(
        baseUrl: String,
        apiKey: String,
        model: String,
        prompt: String,
        userMessage: String,
    ): AiAnalysisResult {
        val request = ResponsesRequest(
            model = model,
            input = listOf(
                ResponsesInputMessage(
                    role = "system",
                    content = listOf(ResponsesContentPart(type = "input_text", text = prompt)),
                ),
                ResponsesInputMessage(
                    role = "user",
                    content = listOf(ResponsesContentPart(type = "input_text", text = userMessage)),
                ),
            ),
            tools = listOf(
                buildJsonObject { put("type", "web_search") },
            ),
            temperature = 0.2,
        )
        execute(
            baseUrl,
            "/responses",
            apiKey,
            json.encodeToJsonElement(ResponsesRequest.serializer(), request),
        ).use {
            if (it.statusCode !in 200..299) throw it.toException()
            val body = it.bodyText
            val text = runCatching {
                extractResponsesOutputText(json.parseToJsonElement(body))
            }.getOrNull()
            return parseAiJson(text.orEmpty())
                ?: throw AiRequestException("AI 返回内容无法解析为 JSON", it.statusCode)
        }
    }

    private suspend fun execute(
        baseUrl: String,
        path: String,
        apiKey: String,
        body: JsonElement,
    ): AiHttpResponse = withContext(Dispatchers.IO) {
        val requestBody = json.encodeToString(JsonElement.serializer(), body)
            .toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url("$baseUrl$path")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(requestBody)
            .build()
        try {
            callFactory.newCall(request).execute().let { response ->
                val bodyText = response.body?.string().orEmpty()
                AiHttpResponse(
                    statusCode = response.code,
                    bodyText = bodyText,
                    closeable = response,
                )
            }
        } catch (error: IOException) {
            throw AiRequestException("无法连接 AI 服务，请检查网络或接口地址")
        }
    }

    private fun AiHttpResponse.toException(): AiRequestException {
        val detail = runCatching {
            json.parseToJsonElement(bodyText)
                .jsonObject["error"]
                ?.jsonObject
                ?.get("message")
                ?.jsonPrimitive
                ?.contentOrNull
        }.getOrNull()
        val message = detail?.takeIf { it.isNotBlank() }
            ?: "AI 服务返回错误（HTTP $statusCode）"
        return AiRequestException(message, statusCode)
    }

    private fun extractResponsesOutputText(root: JsonElement): String? {
        val output = root.jsonObject["output"] as? JsonArray ?: return null
        for (item in output) {
            val itemObject = item as? JsonObject ?: continue
            if (itemObject["type"]?.jsonPrimitive?.contentOrNull != "message") continue
            val content = itemObject["content"] as? JsonArray ?: continue
            for (part in content) {
                val partObject = part as? JsonObject ?: continue
                if (partObject["type"]?.jsonPrimitive?.contentOrNull == "output_text") {
                    return partObject["text"]?.jsonPrimitive?.contentOrNull
                }
            }
        }
        return null
    }

    private fun buildUserMessage(context: AiSubjectContext): String = buildString {
        appendLine("请分析下面这部作品的百合程度和雷点。")
        appendLine("作品资料（来自 Bangumi）：")
        appendLine("- 标题（原名）：${context.title}")
        if (context.titleCn.isNotBlank()) appendLine("- 中文名：${context.titleCn}")
        appendLine("- 条目类型：${context.typeLabel}")
        if (context.date.isNotBlank()) appendLine("- 发行/播出日期：${context.date}")
        if (context.platform.isNotBlank()) appendLine("- 平台/媒介：${context.platform}")
        if (context.score.isNotBlank()) appendLine("- 当前评分：${context.score}")
        if (context.tags.isNotEmpty()) appendLine("- 标签：${context.tags.joinToString("、")}")
        if (context.metaTags.isNotEmpty()) {
            appendLine("- 元标签：${context.metaTags.joinToString("、")}")
        }
        if (context.infobox.isNotBlank()) appendLine("- 基本资料：${context.infobox}")
        if (context.summary.isNotBlank()) appendLine("- 简介：${context.summary.trim()}")
        appendLine()
        appendLine("请给出可靠、简洁的判断。")
    }

    private fun systemPrompt(userPreferences: String): String = """
        你负责分析百合作品。以下是用户的判断偏好，请严格遵循：
        $userPreferences

        回复必须仅包含一个 JSON 对象，不要使用 Markdown 或额外文字。
        JSON 字段固定为：
        {"category":"真百|轻百|非百","confidence":0到1之间的数字,"reason":"简洁中文理由","riskPoints":["明确雷点"],"sources":["公开来源"]}
        所有字段都必须存在；没有雷点或来源时使用空数组。category 只能为真百、轻百或非百。
    """.trimIndent()

    private fun isOfficialOpenAi(baseUrl: String): Boolean = runCatching {
        URI(baseUrl).host?.let { it == "api.openai.com" } == true
    }.getOrDefault(false)

    private data class AiHttpResponse(
        val statusCode: Int,
        val bodyText: String,
        val closeable: okhttp3.Response,
    ) : Closeable {
        override fun close() = closeable.close()
    }

    @Serializable
    private data class ChatMessage(
        val role: String,
        val content: String,
    )

    @Serializable
    private data class ChatCompletionsRequest(
        val model: String,
        val messages: List<ChatMessage>,
        val temperature: Double,
        @SerialName("response_format") val responseFormat: JsonElement = JsonNull,
    )

    @Serializable
    private data class ChatCompletionsResponse(
        val choices: List<ChatChoice> = emptyList(),
    )

    @Serializable
    private data class ChatChoice(
        val message: ChatResponseMessage = ChatResponseMessage(),
    )

    @Serializable
    private data class ChatResponseMessage(
        val content: String? = null,
    )

    @Serializable
    private data class ResponsesRequest(
        val model: String,
        val input: List<ResponsesInputMessage>,
        val tools: List<JsonElement>,
        val temperature: Double,
    )

    @Serializable
    private data class ResponsesInputMessage(
        val role: String,
        val content: List<ResponsesContentPart>,
    )

    @Serializable
    private data class ResponsesContentPart(
        val type: String,
        val text: String,
    )

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
