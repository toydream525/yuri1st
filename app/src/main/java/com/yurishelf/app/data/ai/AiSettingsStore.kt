package com.yurishelf.app.data.ai

import android.content.SharedPreferences
import com.yurishelf.app.data.remote.KeystoreStringStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AiSettingsStore(
    private val preferences: SharedPreferences,
    private val apiKeyStore: KeystoreStringStore,
) {
    fun getSettings(): AiSettings = AiSettings(
        baseUrl = preferences.getString(KEY_BASE_URL, null)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: DEFAULT_AI_BASE_URL,
        model = preferences.getString(KEY_MODEL, null)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: DEFAULT_AI_MODEL,
        prompt = preferences.getString(KEY_PROMPT, null)
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_AI_PROMPT,
        webSearchEnabled = preferences.getBoolean(KEY_WEB_SEARCH, true),
        hasApiKey = apiKeyStore.get().isNullOrBlank().not(),
    )

    fun getApiKey(): String? = apiKeyStore.get()

    suspend fun saveFields(settings: AiSettings): Boolean = withContext(Dispatchers.IO) {
        preferences.edit()
            .putString(KEY_BASE_URL, settings.baseUrl.trim())
            .putString(KEY_MODEL, settings.model.trim())
            .putString(KEY_PROMPT, settings.prompt)
            .putBoolean(KEY_WEB_SEARCH, settings.webSearchEnabled)
            .commit()
    }

    suspend fun saveApiKey(apiKey: String?): Boolean = apiKeyStore.save(apiKey)

    companion object {
        const val KEY_BASE_URL = "ai_base_url"
        const val KEY_MODEL = "ai_model"
        const val KEY_PROMPT = "ai_prompt"
        const val KEY_WEB_SEARCH = "ai_web_search"
    }
}
