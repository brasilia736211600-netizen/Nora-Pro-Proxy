package com.example.model

import com.example.webview.IsolationManager
import org.junit.Assert.*
import org.junit.Test

class FilterAndIsolationTest {

    private val profiles = listOf(
        NoraProfile(
            id = "p1",
            name = "Twitter Dev",
            site = "twitter.com",
            createdAt = 1000L,
            lastUsedAt = 5000L,
            usageCount = 42,
            proxy = ProxyConfig(enabled = true, protocol = ProxyProtocol.SOCKS5, host = "127.0.0.1", port = 9050),
            userAgent = UserAgentConfig(mode = UAMode.CUSTOM, customUA = "MyCustomUA/1.0"),
            time = TimeConfig(mode = TimeMode.PROXY)
        ),
        NoraProfile(
            id = "p2",
            name = "Reddit Lurk",
            site = "reddit.com",
            createdAt = 2000L,
            lastUsedAt = 3000L,
            usageCount = 10,
            proxy = ProxyConfig(enabled = false),
            userAgent = UserAgentConfig(mode = UAMode.PRESET, selectedId = "android_chrome_128"),
            time = TimeConfig(mode = TimeMode.DEVICE)
        ),
        NoraProfile(
            id = "p3",
            name = "Twitter Personal",
            site = "twitter.com",
            createdAt = 3000L,
            lastUsedAt = 9000L,
            usageCount = 150,
            proxy = ProxyConfig(enabled = true, protocol = ProxyProtocol.HTTP, host = "proxy.us.net", port = 8080),
            userAgent = UserAgentConfig(mode = UAMode.RANDOM),
            time = TimeConfig(mode = TimeMode.MANUAL, timezoneId = "America/New_York")
        )
    )

    @Test
    fun testSearchFiltering() {
        val searchTwitter = ProfileFilterCriteria(searchQuery = "twitter")
        val results = ProfileFilterEngine.filterAndSort(profiles, searchTwitter)
        assertEquals(2, results.size)
        assertTrue(results.all { it.site == "twitter.com" })

        val searchLurk = ProfileFilterCriteria(searchQuery = "lurk")
        val lurkResults = ProfileFilterEngine.filterAndSort(profiles, searchLurk)
        assertEquals(1, lurkResults.size)
        assertEquals("Reddit Lurk", lurkResults.first().name)
    }

    @Test
    fun testProxyFiltering() {
        val proxyOnly = ProfileFilterCriteria(proxyFilter = true)
        val results = ProfileFilterEngine.filterAndSort(profiles, proxyOnly)
        assertEquals(2, results.size)
        assertTrue(results.all { it.proxy.enabled })

        val directOnly = ProfileFilterCriteria(proxyFilter = false)
        val directResults = ProfileFilterEngine.filterAndSort(profiles, directOnly)
        assertEquals(1, directResults.size)
        assertEquals("Reddit Lurk", directResults.first().name)
    }

    @Test
    fun testSorting() {
        val sortByUsageDesc = ProfileFilterCriteria(
            sortOption = ProfileSortOption.MOST_USED,
            sortAscending = false
        )
        val results = ProfileFilterEngine.filterAndSort(profiles, sortByUsageDesc)
        assertEquals("Twitter Personal", results[0].name) // 150
        assertEquals("Twitter Dev", results[1].name)      // 42
        assertEquals("Reddit Lurk", results[2].name)      // 10
    }

    @Test
    fun testGroupBySite() {
        val grouped = ProfileFilterEngine.groupBySite(profiles)
        assertEquals(2, grouped.keys.size)
        assertEquals(2, grouped["twitter.com"]?.size)
        assertEquals(1, grouped["reddit.com"]?.size)
    }

    @Test
    fun testUAShimGeneration() {
        val shim = IsolationManager.generateUAShimScript(
            effectiveUA = "Mozilla/5.0 (Windows NT 10.0) TestUA/1.0",
            os = "Windows",
            browser = "Edge"
        )
        assertTrue(shim.contains("TestUA/1.0"))
        assertTrue(shim.contains("Win32"))
        assertTrue(shim.contains("navigator.userAgent"))
    }

    @Test
    fun testTimeShimGeneration() {
        val manualProfile = profiles[2]
        val shim = IsolationManager.generateTimeShimScript(manualProfile)
        assertTrue(shim.contains("America/New_York"))
        assertTrue(shim.contains("MockDate"))
        assertTrue(shim.contains("Intl.DateTimeFormat"))

        val deviceProfile = profiles[1]
        val emptyShim = IsolationManager.generateTimeShimScript(deviceProfile)
        assertEquals("", emptyShim)
    }
}
