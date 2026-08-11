package com.yurishelf.app.data.ai

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.yurishelf.app.data.remote.KeystoreStringStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AiSettingsStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preferences = context.getSharedPreferences("ai-settings-store-test", Context.MODE_PRIVATE)

    @After
    fun tearDown() {
        preferences.edit().clear().commit()
    }

    @Test
    fun legacyDefaultMigratesOnceToReaderFacingDefault() {
        preferences.edit().putString(AiSettingsStore.KEY_PROMPT, LEGACY_DEFAULT_AI_PROMPT).commit()

        val settings = store().getSettings()

        assertEquals(DEFAULT_AI_PROMPT, settings.prompt)
        assertEquals(2, preferences.getInt(AiSettingsStore.KEY_PROMPT_FORMAT_VERSION, 0))
    }

    @Test
    fun legacyCustomRemovesOnlyJsonProtocolAndKeepsNaturalLanguage() {
        preferences.edit().putString(
            AiSettingsStore.KEY_PROMPT,
            """
            保留这些词：字段、JSON 和 {花括号}。
            ```json
            {"category":"真百","confidence":0.9,"riskPoints":[]}
            ```
            不要把 BE 当雷点。
            """.trimIndent(),
        ).commit()

        val prompt = store().getSettings().prompt

        assertTrue(prompt.contains("字段、JSON 和 {花括号}"))
        assertTrue(prompt.contains("不要把 BE 当雷点"))
        assertFalse(prompt.contains("\"category\""))
    }

    @Test
    fun legacyCustomRemovesInlineProtocolButKeepsOrdinaryCategoryLanguage() {
        preferences.edit().putString(
            AiSettingsStore.KEY_PROMPT,
            """
            输出格式：{"category":"真百","confidence":0.9,"riskPoints":[]}
            我希望 category 的判断更偏向真百，不要太苛刻。
            """.trimIndent(),
        ).commit()

        val prompt = store().getSettings().prompt

        assertFalse(prompt.contains("输出格式"))
        assertTrue(prompt.contains("我希望 category 的判断更偏向真百"))
    }

    @Test
    fun savedCurrentPromptIsNotCleanedAgainAfterReload() = runBlocking {
        val desired = "我希望保留 JSON、字段和 {花括号} 这些自然语言。"
        store().saveFields(AiSettings(prompt = desired))

        assertEquals(desired, store().getSettings().prompt)
        assertEquals(desired, store().getSettings().prompt)
    }

    private fun store() = AiSettingsStore(
        preferences = preferences,
        apiKeyStore = KeystoreStringStore(preferences, "cipher", "iv", "ai-settings-store-test"),
    )
}
