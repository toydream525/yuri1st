package com.yurishelf.app.data

import com.yurishelf.app.data.ai.AiAnalysisResult
import com.yurishelf.app.data.local.AiAnalysisEntity
import com.yurishelf.app.domain.AiAnalysis
import com.yurishelf.app.domain.CatalogType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val analysisJson = Json { ignoreUnknownKeys = true }

fun AiAnalysisResult.toEntity(
    subjectId: Int,
    catalogType: CatalogType,
    analyzedAt: Long,
): AiAnalysisEntity = AiAnalysisEntity(
    subjectId = subjectId,
    catalogType = catalogType.name,
    category = category.name,
    confidence = confidence,
    reason = reason,
    riskPointsJson = analysisJson.encodeToString(riskPoints),
    sourcesJson = analysisJson.encodeToString(sources),
    analyzedAt = analyzedAt,
)

fun AiAnalysisEntity.toDomain(): AiAnalysis = AiAnalysis(
    subjectId = subjectId,
    catalogType = CatalogType.valueOf(catalogType),
    category = runCatching {
        com.yurishelf.app.domain.AiYuriCategory.valueOf(category)
    }.getOrDefault(com.yurishelf.app.domain.AiYuriCategory.UNKNOWN),
    confidence = confidence,
    reason = reason,
    riskPoints = decodeStringList(riskPointsJson),
    sources = decodeStringList(sourcesJson),
    analyzedAt = analyzedAt,
)

private fun decodeStringList(value: String): List<String> = runCatching {
    analysisJson.decodeFromString<List<String>>(value)
}.getOrDefault(emptyList())
