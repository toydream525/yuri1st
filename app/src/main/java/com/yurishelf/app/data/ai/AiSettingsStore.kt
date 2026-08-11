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
        prompt = migratedPrompt(),
        webSearchEnabled = preferences.getBoolean(KEY_WEB_SEARCH, true),
        hasApiKey = apiKeyStore.get().isNullOrBlank().not(),
    )

    fun getApiKey(): String? = apiKeyStore.get()

    suspend fun saveFields(settings: AiSettings): Boolean = withContext(Dispatchers.IO) {
        preferences.edit()
            .putString(KEY_BASE_URL, settings.baseUrl.trim())
            .putString(KEY_MODEL, settings.model.trim())
            .putString(KEY_PROMPT, settings.prompt)
            .putInt(KEY_PROMPT_FORMAT_VERSION, PROMPT_FORMAT_VERSION)
            .putBoolean(KEY_WEB_SEARCH, settings.webSearchEnabled)
            .commit()
    }

    suspend fun saveApiKey(apiKey: String?): Boolean = apiKeyStore.save(apiKey)

    private fun migratedPrompt(): String {
        val stored = preferences.getString(KEY_PROMPT, null)?.takeIf { it.isNotBlank() }
            ?: return DEFAULT_AI_PROMPT
        if (preferences.getInt(KEY_PROMPT_FORMAT_VERSION, LEGACY_PROMPT_FORMAT_VERSION) >=
            PROMPT_FORMAT_VERSION
        ) return stored
        val migrated = migrateAiPrompt(stored)
        preferences.edit()
            .putString(KEY_PROMPT, migrated)
            .putInt(KEY_PROMPT_FORMAT_VERSION, PROMPT_FORMAT_VERSION)
            .apply()
        return migrated
    }

    companion object {
        const val KEY_BASE_URL = "ai_base_url"
        const val KEY_MODEL = "ai_model"
        const val KEY_PROMPT = "ai_prompt"
        const val KEY_PROMPT_FORMAT_VERSION = "ai_prompt_format_version"
        const val KEY_WEB_SEARCH = "ai_web_search"
        private const val LEGACY_PROMPT_FORMAT_VERSION = 1
        private const val PROMPT_FORMAT_VERSION = 2
    }
}

internal fun migrateAiPrompt(prompt: String): String {
    if (prompt.trim() == LEGACY_DEFAULT_AI_PROMPT.trim()) return DEFAULT_AI_PROMPT
    val lines = prompt.lines()
    val kept = mutableListOf<String>()
    var index = 0
    var insideJsonFence = false
    while (index < lines.size) {
        val line = lines[index]
        val trimmed = line.trim()
        if (trimmed.startsWith("```json", ignoreCase = true)) {
            insideJsonFence = true
            index += 1
            continue
        }
        if (insideJsonFence) {
            if (trimmed == "```") insideJsonFence = false
            index += 1
            continue
        }
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            val block = mutableListOf<String>()
            var depth = 0
            do {
                val candidate = lines[index]
                block += candidate
                depth += candidate.count { it == '{' } - candidate.count { it == '}' }
                index += 1
            } while (index < lines.size && depth > 0)
            val joined = block.joinToString("\n")
            if (joined.contains("\"category\"") &&
                (joined.contains("\"riskPoints\"") || joined.contains("\"confidence\""))
            ) continue
            kept += block
            continue
        }
        if (trimmed.matches(Regex("^只输出.*JSON.*$", RegexOption.IGNORE_CASE)) ||
            trimmed.matches(Regex("^JSON\\s*结构.*$", RegexOption.IGNORE_CASE))
        ) {
            index += 1
            continue
        }
        if (isInlineLegacyProtocol(trimmed)) {
            index += 1
            continue
        }
        kept += line
        index += 1
    }
    val readerFacing = kept.joinToString("\n").trim()
    return readerFacing.ifBlank { DEFAULT_AI_PROMPT }
}

private fun isInlineLegacyProtocol(line: String): Boolean {
    val normalized = line.lowercase()
    val hasCategory = normalized.contains("\"category\"")
    val hasCompanionField = normalized.contains("\"riskpoints\"") ||
        normalized.contains("\"confidence\"") ||
        normalized.contains("\"sources\"")
    return hasCategory && hasCompanionField
}
