package com.yurishelf.app.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ApiAccessModeTest {
    @Test
    fun publicModeNeverSendsToken() {
        assertNull(authorizationForMode(nsfwEnabled = false, accessToken = "secret"))
    }

    @Test
    fun nsfwModeSendsBearerTokenForEveryRequest() {
        assertEquals(
            "Bearer secret",
            authorizationForMode(nsfwEnabled = true, accessToken = "  secret  "),
        )
    }

    @Test
    fun nsfwModeFailsClosedWithoutToken() {
        assertThrows(MissingAccessTokenException::class.java) {
            authorizationForMode(nsfwEnabled = true, accessToken = "  ")
        }
    }
}
