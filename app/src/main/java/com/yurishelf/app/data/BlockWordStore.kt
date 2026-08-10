package com.yurishelf.app.data

import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BlockWordStore(private val preferences: SharedPreferences) {
    private val json = Json { ignoreUnknownKeys = true }

    fun get(): List<String> = runCatching {
        json.decodeFromString<List<String>>(preferences.getString(KEY, null) ?: "[]")
    }.getOrDefault(emptyList()).map { it.trim() }.filter { it.isNotEmpty() }.distinct()

    suspend fun save(words: List<String>): Boolean = withContext(Dispatchers.IO) {
        val normalized = words.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        preferences.edit().putString(KEY, json.encodeToString(normalized)).commit()
    }

    private companion object {
        const val KEY = "block_words_json"
    }
}
