package com.yurishelf.app.data.local

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "ai_analyses",
    primaryKeys = ["subjectId", "catalogType"],
    indices = [
        Index("catalogType"),
        Index("analyzedAt"),
    ],
)
data class AiAnalysisEntity(
    val subjectId: Int,
    val catalogType: String,
    val category: String,
    val confidence: Double,
    val reason: String,
    val riskPointsJson: String,
    val sourcesJson: String,
    val analyzedAt: Long,
)
