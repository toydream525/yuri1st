package com.yurishelf.app.data

import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class UiSnapshot(
    val typeName: String? = null,
    val query: String = "",
    val sortName: String? = null,
    val minimumVotes: Int = 0,
    val yearOrZero: Int = 0,
    val favoritesOnly: Boolean = false,
    val nsfwOnly: Boolean = false,
    val formatName: String? = null,
    val aiCategoryName: String? = null,
    val winLoseName: String? = null,
    val page: Int = 1,
    val viewModeName: String? = null,
    val detailKey: String? = null,
)

private val snapshotJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

fun SharedPreferences.loadUiSnapshot(): UiSnapshot? =
    getString(KEY_UI_SNAPSHOT, null)
        ?.let { raw ->
            runCatching { snapshotJson.decodeFromString<UiSnapshot>(raw) }.getOrNull()
        }

suspend fun SharedPreferences.saveUiSnapshot(snapshot: UiSnapshot): Boolean =
    withContext(Dispatchers.IO) {
        edit()
            .putString(KEY_UI_SNAPSHOT, snapshotJson.encodeToString(snapshot))
            .commit()
    }

private const val KEY_UI_SNAPSHOT = "ui_snapshot_json"
