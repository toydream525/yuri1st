package com.yurishelf.app.data.remote

import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores a single short secret (Access Token, AI API Key) in
 * SharedPreferences, encrypted with an Android Keystore AES-GCM key.
 */
class KeystoreStringStore(
    private val preferences: SharedPreferences,
    private val ciphertextKey: String,
    private val ivKey: String,
    private val alias: String,
) {
    @Volatile
    private var cached: String? = null

    @Volatile
    private var loaded = false

    @Volatile
    private var onChanged: () -> Unit = {}

    fun setOnChanged(listener: () -> Unit) {
        onChanged = listener
    }

    fun get(): String? {
        if (loaded) return cached
        return synchronized(this) {
            if (!loaded) {
                cached = decryptStoredValue()
                loaded = true
            }
            cached
        }
    }

    suspend fun save(value: String?): Boolean = withContext(Dispatchers.IO) {
        val normalized = value?.trim().orEmpty()
        val previousCiphertext = preferences.getString(ciphertextKey, null)
        val previousIv = preferences.getString(ivKey, null)
        val editor = preferences.edit()
        if (normalized.isEmpty()) {
            editor.remove(ciphertextKey).remove(ivKey)
        } else {
            val encrypted = runCatching { encrypt(normalized) }.getOrElse {
                return@withContext false
            }
            editor.putString(ciphertextKey, encrypted.ciphertext)
                .putString(ivKey, encrypted.iv)
        }

        val committed = editor.commit()
        if (committed) {
            cached = normalized.ifEmpty { null }
            loaded = true
            runCatching { onChanged() }
        } else {
            preferences.edit()
                .putString(ciphertextKey, previousCiphertext)
                .putString(ivKey, previousIv)
                .apply()
        }
        committed
    }

    private fun decryptStoredValue(): String? = runCatching {
        val ciphertext = preferences.getString(ciphertextKey, null) ?: return null
        val iv = preferences.getString(ivKey, null) ?: return null
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)),
        )
        String(cipher.doFinal(Base64.decode(ciphertext, Base64.NO_WRAP)), Charsets.UTF_8)
    }.getOrNull()

    private fun encrypt(value: String): EncryptedValue {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        return EncryptedValue(
            ciphertext = Base64.encodeToString(
                cipher.doFinal(value.toByteArray(Charsets.UTF_8)),
                Base64.NO_WRAP,
            ),
            iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP),
        )
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER).run {
            init(
                KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
            generateKey()
        }
    }

    private data class EncryptedValue(val ciphertext: String, val iv: String)

    private companion object {
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
