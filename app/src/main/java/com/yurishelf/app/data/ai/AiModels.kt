package com.yurishelf.app.data.ai

import com.yurishelf.app.domain.AiYuriCategory
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AiJsonResponse(
    val category: String = "",
    val confidence: Double = 0.0,
    val reason: String = "",
    @SerialName("riskPoints") val riskPoints: List<String> = emptyList(),
    @SerialName("risk_points") val riskPointsSnake: List<String> = emptyList(),
    val sources: List<String> = emptyList(),
)

data class AiAnalysisResult(
    val category: AiYuriCategory,
    val confidence: Double,
    val reason: String,
    val riskPoints: List<String>,
    val sources: List<String>,
)

data class AiSubjectContext(
    val title: String,
    val titleCn: String,
    val typeLabel: String,
    val date: String,
    val platform: String,
    val score: String,
    val summary: String,
    val tags: List<String>,
    val metaTags: List<String>,
    val infobox: String,
)

/**
 * Extracts the first balanced JSON object from a model reply, tolerating
 * Markdown code fences and extra prose around the payload.
 */
internal fun extractJsonObject(text: String): String? {
    val cleaned = text.trim()
        .removePrefix("```json")
        .removePrefix("```JSON")
        .removePrefix("```")
        .trim()
    val start = cleaned.indexOf('{')
    if (start < 0) return null
    var depth = 0
    var inString = false
    var escaped = false
    for (index in start until cleaned.length) {
        val char = cleaned[index]
        if (inString) {
            when {
                escaped -> escaped = false
                char == '\\' -> escaped = true
                char == '"' -> inString = false
            }
            continue
        }
        when (char) {
            '"' -> inString = true
            '{' -> depth += 1
            '}' -> {
                depth -= 1
                if (depth == 0) {
                    return cleaned.substring(start, index + 1)
                }
            }
        }
    }
    return null
}

internal fun categoryFromLabel(value: String): AiYuriCategory {
    val normalized = value.trim().lowercase()
    return when {
        normalized in setOf(
            "真百",
            "strong",
            "true",
            "真百合",
            "真百合作",
        ) -> AiYuriCategory.STRONG
        normalized in setOf(
            "轻百",
            "light",
            "轻百合",
            "轻百合作",
            "擦边",
        ) -> AiYuriCategory.LIGHT
        normalized in setOf(
            "非百",
            "non",
            "none",
            "非百合",
            "no",
            "false",
        ) -> AiYuriCategory.NON
        else -> AiYuriCategory.UNKNOWN
    }
}

internal fun parseAiJson(text: String): AiAnalysisResult? {
    val jsonText = extractJsonObject(text) ?: return null
    val payload = runCatching {
        analysisJson.decodeFromString<AiJsonResponse>(jsonText)
    }.getOrNull() ?: return null
    val category = categoryFromLabel(payload.category)
    val riskPoints = payload.riskPoints.ifEmpty { payload.riskPointsSnake }
    return AiAnalysisResult(
        category = category,
        confidence = payload.confidence.coerceIn(0.0, 1.0),
        reason = payload.reason.trim(),
        riskPoints = riskPoints.map { it.trim() }.filter { it.isNotEmpty() },
        sources = payload.sources.map { it.trim() }.filter { it.isNotEmpty() },
    )
}

private val analysisJson = kotlinx.serialization.json.Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

data class AiSettings(
    val baseUrl: String = DEFAULT_AI_BASE_URL,
    val model: String = DEFAULT_AI_MODEL,
    val prompt: String = DEFAULT_AI_PROMPT,
    val webSearchEnabled: Boolean = true,
    val hasApiKey: Boolean = false,
)

const val DEFAULT_AI_BASE_URL = "https://api.openai.com/v1"
const val DEFAULT_AI_MODEL = "gpt-4o-mini"

const val DEFAULT_AI_PROMPT = """你是一位熟悉 Bangumi、百合作品社区与雷点讨论的资深编辑。请根据下面提供的作品资料（标题、别名、类型、发行信息、简介、标签、基本资料）进行判断；如果开启联网搜索，也可以参考你检索到的公开资料、百科、官网或社区共识，但要区分资料与传闻。

只输出一个 JSON 对象，不要输出 Markdown、解释或其他文字。JSON 结构必须为：
{"category":"真百|轻百|非百","confidence":0到1之间的数字,"reason":"简洁的中文判断依据","riskPoints":["雷点1","雷点2"],"sources":["公开来源1","公开来源2"]}

判断规则：
1. category 为“真百”时，表示作品以女性角色之间的恋爱/感情关系为主线，或官方明确/强烈暗示；
2. category 为“轻百”时，表示以女性角色互动为主，但恋爱并非主线、关系停留在暧昧或擦边；
3. category 为“非百”时，表示与百合作关系不大，或百合只是标签误导、路人角色；
4. riskPoints 列举报点，例如：存在主要男性角色或后宫/党争元素、开放式结局、关系暧昧不明、作者或官方否认百合、剧情高开低走、有虐心或胃疼情节、真人化/跨媒体改编偏差等；没有确凿雷点时输出空数组 [];
5. confidence 表示你对判断的把握程度，信息不足、争议较大或资料相互矛盾时给 0.3～0.6 的低分，禁止虚高；
6. 不要编造资料中不存在的剧情或雷点；资料不足时在 reason 中明确写“资料不足”；
7. sources 列出判断参考的公开来源（如 Bangumi 条目页、维基、官网、访谈），不确定时可以留空数组。"""
