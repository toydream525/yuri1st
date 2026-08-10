package com.yurishelf.app.data

import android.content.Context
import android.content.SharedPreferences
import com.yurishelf.app.data.local.SubjectDao
import com.yurishelf.app.data.remote.SubjectDto
import com.yurishelf.app.domain.CatalogType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
data class SeedCatalogDto(
    val generatedAt: String,
    val subjects: List<SubjectDto>,
)

data class SeedInfo(
    val count: Int,
    val generatedAt: String,
)

class SeedCatalogImporter(
    private val context: Context,
    private val dao: SubjectDao,
    private val preferences: SharedPreferences,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    suspend fun importIfNeeded(): SeedInfo? {
        if (preferences.getBoolean(KEY_IMPORTED, false)) return readSeedInfo()
        return withContext(Dispatchers.IO) {
            val raw = runCatching {
                context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
            }.getOrNull()
            if (raw.isNullOrBlank()) return@withContext null

            val seed = runCatching { json.decodeFromString<SeedCatalogDto>(raw) }.getOrNull()
                ?: return@withContext null
            val now = System.currentTimeMillis()
            val entities = seed.subjects.mapNotNull { dto ->
                val type = CatalogType.entries.firstOrNull { it.apiValue == dto.type }
                    ?: return@mapNotNull null
                dto.toEntity(
                    existing = null,
                    now = now,
                    isDetail = false,
                    catalogType = type,
                    nsfwFallback = false,
                )
            }
            if (entities.isEmpty()) return@withContext null

            dao.upsertCatalogPagePreservingFavorites(entities)
            val info = SeedInfo(entities.size, seed.generatedAt)
            val committed = preferences.edit()
                .putBoolean(KEY_IMPORTED, true)
                .putInt(KEY_COUNT, entities.size)
                .putString(KEY_GENERATED_AT, seed.generatedAt)
                .commit()
            if (committed) info else null
        }
    }

    fun readSeedInfo(): SeedInfo? {
        if (!preferences.getBoolean(KEY_IMPORTED, false)) return null
        return SeedInfo(
            count = preferences.getInt(KEY_COUNT, 0),
            generatedAt = preferences.getString(KEY_GENERATED_AT, "").orEmpty(),
        )
    }

    private companion object {
        const val ASSET_NAME = "seed_catalog.json"
        const val KEY_IMPORTED = "seed_catalog_imported"
        const val KEY_COUNT = "seed_catalog_count"
        const val KEY_GENERATED_AT = "seed_catalog_generated_at"
    }
}
