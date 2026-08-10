package com.yurishelf.app.data.remote

import android.content.SharedPreferences

class AccessTokenStore(private val preferences: SharedPreferences) {
    private val store = KeystoreStringStore(
        preferences = preferences,
        ciphertextKey = KEY_CIPHERTEXT,
        ivKey = KEY_IV,
        alias = KEY_ALIAS,
    )

    fun setOnChanged(listener: () -> Unit) {
        store.setOnChanged(listener)
    }

    fun hasToken(): Boolean = store.get().isNullOrBlank().not()

    fun get(): String? = store.get()

    suspend fun save(token: String?): Boolean = store.save(token)

    private companion object {
        const val KEY_CIPHERTEXT = "bangumi_token_ciphertext"
        const val KEY_IV = "bangumi_token_iv"
        const val KEY_ALIAS = "yurishelf_bangumi_access_token"
    }
}
