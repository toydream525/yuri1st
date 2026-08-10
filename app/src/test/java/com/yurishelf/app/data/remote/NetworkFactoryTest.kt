package com.yurishelf.app.data.remote

import okhttp3.Dns
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.net.InetAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI

class NetworkFactoryTest {

    @Test
    fun bangumiPost_rewritesCharsetContentTypeToExactJson() {
        val original = request(
            url = "https://api.bgm.tv/v0/users/-/collections/428735",
            method = "POST",
            contentType = "application/json; charset=utf-8",
        )

        val normalized = original.normalizeBangumiJsonContentType()

        assertEquals("application/json", normalized.header("Content-Type"))
    }

    @Test
    fun bangumiPatch_rewritesContentType() {
        val original = request(
            url = "https://api.bgm.tv/v0/users/-/collections/428735",
            method = "PATCH",
            contentType = "application/json; charset=utf-8",
        )

        assertEquals(
            "application/json",
            original.normalizeBangumiJsonContentType().header("Content-Type"),
        )
    }

    @Test
    fun nonBangumiHost_keepsOriginalRequest() {
        val original = request(
            url = "https://api.deepseek.com/chat/completions",
            method = "POST",
            contentType = "application/json; charset=utf-8",
        )

        assertEquals(original, original.normalizeBangumiJsonContentType())
    }

    @Test
    fun productionClient_sendsExactJsonContentTypeToBangumi() {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(204))
            val previousSelector = ProxySelector.getDefault()
            try {
                ProxySelector.setDefault(
                    object : ProxySelector() {
                        override fun select(uri: URI): List<Proxy> = listOf(Proxy.NO_PROXY)

                        override fun connectFailed(
                            uri: URI,
                            socketAddress: SocketAddress,
                            error: IOException,
                        ) = Unit
                    },
                )
                val client = NetworkFactory.createHttpClient(
                    userAgent = "yuri1st-test/1.0",
                    debug = false,
                    proxySettingsProvider = { ProxySettings() },
                ).newBuilder()
                    .dns(
                        object : Dns {
                            override fun lookup(hostname: String): List<InetAddress> =
                                if (hostname == "api.bgm.tv") {
                                    listOf(InetAddress.getByName("127.0.0.1"))
                                } else {
                                    Dns.SYSTEM.lookup(hostname)
                                }
                        },
                    )
                    .build()

                val request = Request.Builder()
                    .url("http://api.bgm.tv:${server.port}/v0/users/-/collections/428735")
                    .post("{\"type\":3}".toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    assertEquals(204, response.code)
                }
            } finally {
                ProxySelector.setDefault(previousSelector)
            }

            val recorded = server.takeRequest()
            assertEquals("/v0/users/-/collections/428735", recorded.path)
            assertEquals("application/json", recorded.getHeader("Content-Type"))
        } finally {
            server.shutdown()
        }
    }

    private fun request(
        url: String,
        method: String,
        contentType: String?,
    ): Request {
        val builder = Request.Builder().url(url)
        if (contentType != null) {
            builder.header("Content-Type", contentType)
        }
        val body = if (method == "GET") {
            null
        } else {
            "{}".toRequestBody(contentType?.toMediaType())
        }
        return builder.method(method, body).build()
    }
}
