package com.example.model

import org.junit.Assert.*
import org.junit.Test

class ProfileModelsTest {

    @Test
    fun testProxyConfigUrlGeneration() {
        val httpProxy = ProxyConfig(
            enabled = true,
            protocol = ProxyProtocol.HTTP,
            host = "proxy.example.com",
            port = 8080
        )
        assertEquals("http://proxy.example.com:8080", httpProxy.toProxyUrl())

        val socksProxy = ProxyConfig(
            enabled = true,
            protocol = ProxyProtocol.SOCKS5,
            host = "127.0.0.1",
            port = 1080
        )
        assertEquals("socks5://127.0.0.1:1080", socksProxy.toProxyUrl())

        val disabledProxy = httpProxy.copy(enabled = false)
        assertNull(disabledProxy.toProxyUrl())

        val directProxy = httpProxy.copy(protocol = ProxyProtocol.DIRECT)
        assertNull(directProxy.toProxyUrl())
    }

    @Test
    fun testProxyConfigValidation() {
        val validProxy = ProxyConfig(
            enabled = true,
            protocol = ProxyProtocol.HTTP,
            host = "proxy.org",
            port = 3128
        )
        assertTrue(validProxy.isValid())

        val invalidPort = validProxy.copy(port = 70000)
        assertFalse(invalidPort.isValid())

        val blankHost = validProxy.copy(host = "")
        assertFalse(blankHost.isValid())

        val validPac = ProxyConfig(
            enabled = true,
            protocol = ProxyProtocol.HTTP,
            pacUrl = "https://pac.example.com/proxy.pac"
        )
        assertTrue(validPac.isValid())
    }

    @Test
    fun testUserAgentLibrary() {
        val allItems = UserAgentLibrary.items
        assertTrue(allItems.size >= 15)

        val osList = UserAgentLibrary.getOSList()
        assertTrue(osList.contains("Android"))
        assertTrue(osList.contains("iOS"))
        assertTrue(osList.contains("Windows"))
        assertTrue(osList.contains("macOS"))
        assertTrue(osList.contains("Linux"))

        val androidBrowsers = UserAgentLibrary.getBrowsersForOS("Android")
        assertTrue(androidBrowsers.contains("Chrome"))
        assertTrue(androidBrowsers.contains("Firefox"))

        val randomItem = UserAgentLibrary.getRandomForFamily("iOS", "Safari")
        assertEquals("iOS", randomItem.os)
        assertEquals("Safari", randomItem.browser)
        assertTrue(randomItem.userAgentString.contains("Safari"))
    }

    @Test
    fun testTimezoneHelper() {
        val tzs = TimezoneHelper.commonTimezones
        assertTrue(tzs.contains("America/New_York"))
        assertTrue(tzs.contains("Europe/London"))
        assertTrue(tzs.contains("Asia/Tokyo"))

        val display = TimezoneHelper.getDisplayName("Asia/Tokyo")
        assertTrue(display.contains("Asia/Tokyo"))
        assertTrue(display.contains("GMT+09:00"))
    }
}
