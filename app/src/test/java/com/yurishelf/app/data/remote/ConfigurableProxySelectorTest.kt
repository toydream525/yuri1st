package com.yurishelf.app.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI

class ConfigurableProxySelectorTest {
    private val target = URI("https://api.bgm.tv/v0/search/subjects")

    @Test
    fun systemModeDelegatesToSystemSelector() {
        val expected = Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved("system.proxy", 8080))
        val selector = ConfigurableProxySelector(
            settingsProvider = { ProxySettings() },
            systemDefault = FixedProxySelector(expected),
        )

        assertEquals(expected, selector.select(target).single())
    }

    @Test
    fun httpModeUsesConfiguredUnresolvedAddress() {
        val selector = ConfigurableProxySelector(
            settingsProvider = { ProxySettings(ProxyMode.HTTP, "127.0.0.1", 7890) },
            systemDefault = FixedProxySelector(Proxy.NO_PROXY),
        )

        val proxy = selector.select(target).single()
        val address = proxy.address() as InetSocketAddress
        assertEquals(Proxy.Type.HTTP, proxy.type())
        assertEquals("127.0.0.1", address.hostString)
        assertEquals(7890, address.port)
        assertTrue(address.isUnresolved)
    }

    @Test
    fun socksModeUsesSocksWithoutDisablingTls() {
        val selector = ConfigurableProxySelector(
            settingsProvider = { ProxySettings(ProxyMode.SOCKS, "localhost", 7891) },
        )

        assertEquals(Proxy.Type.SOCKS, selector.select(target).single().type())
    }

    @Test
    fun invalidCustomSettingsFallBackToSystem() {
        val selector = ConfigurableProxySelector(
            settingsProvider = { ProxySettings(ProxyMode.HTTP, "", 0) },
            systemDefault = FixedProxySelector(Proxy.NO_PROXY),
        )

        assertEquals(Proxy.NO_PROXY, selector.select(target).single())
        assertFalse(ProxySettings(ProxyMode.SOCKS, "", 70000).isValid)
    }

    private class FixedProxySelector(private val proxy: Proxy) : ProxySelector() {
        override fun select(uri: URI): List<Proxy> = listOf(proxy)

        override fun connectFailed(uri: URI, socketAddress: SocketAddress, error: IOException) = Unit
    }
}
