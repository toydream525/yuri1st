package com.yurishelf.app.data.remote

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class MissingUserAgentException : IOException("Bangumi User-Agent 未配置")

class SwitchableCallFactory(initialClient: OkHttpClient) : Call.Factory {
    private val current = AtomicReference(initialClient)

    override fun newCall(request: Request): Call = current.get().newCall(request)

    fun replaceWith(newClient: OkHttpClient) {
        val oldClient = current.getAndSet(newClient)
        oldClient.dispatcher.cancelAll()
        oldClient.connectionPool.evictAll()
    }
}

/**
 * Bangumi 的 JSON 中间件只接受精确的 `application/json`，Retrofit 的 body
 * 默认是 `application/json; charset=utf-8`，因此写请求需要改回精确值，
 * 否则得到 HTTP 415。该函数必须在网络拦截器中使用：BridgeInterceptor 会
 * 在应用拦截器之后用 body 的 content type 覆盖请求头，网络拦截器才看得到
 * 最终的 Content-Type。
 */
internal fun Request.normalizeBangumiJsonContentType(): Request {
    if (url.host != "api.bgm.tv") return this
    if (method != "POST" && method != "PATCH") return this
    val contentType = header("Content-Type")
    if (contentType == null || !contentType.startsWith("application/json", ignoreCase = true)) {
        return this
    }
    return newBuilder().header("Content-Type", "application/json").build()
}

object NetworkFactory {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    fun createHttpClient(
        userAgent: String,
        debug: Boolean,
        proxySettingsProvider: () -> ProxySettings,
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .proxySelector(ConfigurableProxySelector(proxySettingsProvider))
        .addInterceptor { chain ->
            if (userAgent.isBlank()) throw MissingUserAgentException()
            val request = chain.request().normalizeBangumiJsonContentType()
            val builder = request.newBuilder()
                .header("User-Agent", userAgent)
            if (request.url.host == "api.bgm.tv") {
                builder.header("Accept", "application/json")
            }
            chain.proceed(builder.build())
        }
        .apply {
            if (debug) {
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                    },
                )
            }
        }
        .addNetworkInterceptor { chain ->
            chain.proceed(chain.request().normalizeBangumiJsonContentType())
        }
        .build()

    fun createBangumiApi(callFactory: Call.Factory): BangumiApi = Retrofit.Builder()
        .baseUrl("https://api.bgm.tv/")
        .callFactory(callFactory)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(BangumiApi::class.java)
}
