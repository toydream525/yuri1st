package com.yurishelf.app.data.remote

import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI

enum class ProxyMode(val label: String) {
    SYSTEM("跟随系统"),
    HTTP("HTTP"),
    SOCKS("SOCKS"),
}

data class ProxySettings(
    val mode: ProxyMode = ProxyMode.SYSTEM,
    val host: String = "127.0.0.1",
    val port: Int = 7890,
) {
    val isValid: Boolean
        get() = mode == ProxyMode.SYSTEM || (host.isNotBlank() && port in 1..65535)

    val summary: String
        get() = if (mode == ProxyMode.SYSTEM) mode.label else "${mode.label} ${host.trim()}:$port"
}

class ProxySettingsStore(private val preferences: SharedPreferences) {
    @Volatile
    private var onChanged: () -> Unit = {}

    fun get(): ProxySettings {
        val mode = runCatching {
            ProxyMode.valueOf(preferences.getString(KEY_MODE, null) ?: ProxyMode.SYSTEM.name)
        }.getOrDefault(ProxyMode.SYSTEM)
        val settings = ProxySettings(
            mode = mode,
            host = preferences.getString(KEY_HOST, "127.0.0.1").orEmpty().trim(),
            port = preferences.getInt(KEY_PORT, 7890),
        )
        return settings.takeIf(ProxySettings::isValid) ?: ProxySettings()
    }

    fun setOnChanged(listener: () -> Unit) {
        onChanged = listener
    }

    suspend fun save(settings: ProxySettings): Boolean = withContext(Dispatchers.IO) {
        if (!settings.isValid) return@withContext false
        val previous = get()
        val normalized = settings.copy(host = settings.host.trim())
        val committed = preferences.edit()
            .putString(KEY_MODE, normalized.mode.name)
            .putString(KEY_HOST, normalized.host)
            .putInt(KEY_PORT, normalized.port)
            .commit()
        if (committed) {
            runCatching { onChanged() }
        } else {
            preferences.edit()
                .putString(KEY_MODE, previous.mode.name)
                .putString(KEY_HOST, previous.host)
                .putInt(KEY_PORT, previous.port)
                .apply()
        }
        committed
    }

    private companion object {
        const val KEY_MODE = "proxy_mode"
        const val KEY_HOST = "proxy_host"
        const val KEY_PORT = "proxy_port"
    }
}

class ConfigurableProxySelector(
    private val settingsProvider: () -> ProxySettings,
    private val systemDefault: ProxySelector? = ProxySelector.getDefault(),
) : ProxySelector() {
    override fun select(uri: URI): List<Proxy> {
        val settings = settingsProvider()
        if (settings.mode == ProxyMode.SYSTEM || !settings.isValid) {
            return runCatching { systemDefault?.select(uri).orEmpty() }
                .getOrDefault(emptyList())
                .ifEmpty { listOf(Proxy.NO_PROXY) }
        }

        val type = if (settings.mode == ProxyMode.SOCKS) Proxy.Type.SOCKS else Proxy.Type.HTTP
        val address = InetSocketAddress.createUnresolved(settings.host, settings.port)
        return listOf(Proxy(type, address))
    }

    override fun connectFailed(uri: URI, socketAddress: SocketAddress, error: IOException) {
        if (settingsProvider().mode == ProxyMode.SYSTEM) {
            runCatching { systemDefault?.connectFailed(uri, socketAddress, error) }
        }
    }
}
