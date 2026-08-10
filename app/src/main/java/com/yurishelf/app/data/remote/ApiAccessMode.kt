package com.yurishelf.app.data.remote

import java.io.IOException

class MissingAccessTokenException(
    message: String = "NSFW 模式需要 Bangumi Access Token",
) : IOException(message)

/**
 * Public mode never sends a token. NSFW mode always authenticates every API request.
 */
internal fun authorizationForMode(nsfwEnabled: Boolean, accessToken: String?): String? {
    if (!nsfwEnabled) return null
    val token = accessToken?.trim().takeUnless { it.isNullOrEmpty() }
        ?: throw MissingAccessTokenException()
    return "Bearer $token"
}
