package com.example.model

import com.example.webview.IsolationManager
import org.junit.Assert.*
import org.junit.Test

class ProfileIsolationLeakTest {

    private val profileAlice = NoraProfile(
        id = "profile_alice",
        name = "Alice (Work)",
        site = "internal.corp",
        proxy = ProxyConfig(enabled = true, protocol = ProxyProtocol.SOCKS5, host = "socks.corp.com", port = 1080),
        userAgent = UserAgentConfig(mode = UAMode.CUSTOM, customUA = "Mozilla/5.0 (CorpOS) AliceBrowser/1.0"),
        time = TimeConfig(mode = TimeMode.MANUAL, timezoneId = "America/Los_Angeles", offsetMinutes = 120)
    )

    private val profileBob = NoraProfile(
        id = "profile_bob",
        name = "Bob (Personal)",
        site = "social.net",
        proxy = ProxyConfig(enabled = false, protocol = ProxyProtocol.DIRECT),
        userAgent = UserAgentConfig(mode = UAMode.PRESET, selectedId = "android_firefox_130"),
        time = TimeConfig(mode = TimeMode.DEVICE)
    )

    @Test
    fun testZeroLeakSwitchingGuarantees() {
        // Step 1: In Profile Alice context
        val effectiveUaAlice = IsolationManager.resolveUserAgent(profileAlice, "DefaultUA")
        assertEquals("Mozilla/5.0 (CorpOS) AliceBrowser/1.0", effectiveUaAlice)

        val uaSpoofAlice = IsolationManager.generateUAShimScript(effectiveUaAlice, "Linux", "AliceBrowser")
        assertTrue(uaSpoofAlice.contains("AliceBrowser/1.0"))
        assertFalse(uaSpoofAlice.contains("DefaultUA"))

        val timeShimAlice = IsolationManager.generateTimeShimScript(profileAlice)
        assertTrue(timeShimAlice.contains("America/Los_Angeles"))
        assertTrue(timeShimAlice.contains("MockDate"))

        val proxyUrlAlice = profileAlice.proxy.toProxyUrl()
        assertEquals("socks5://socks.corp.com:1080", proxyUrlAlice)

        // Step 2: Switch boundary to Profile Bob - simulate hard partition
        val effectiveUaBob = IsolationManager.resolveUserAgent(profileBob, "DefaultUA")
        assertNotEquals(effectiveUaAlice, effectiveUaBob)
        assertTrue(effectiveUaBob.contains("Firefox") || effectiveUaBob.contains("Mozilla"))

        val uaSpoofBob = IsolationManager.generateUAShimScript(effectiveUaBob, "Android", "Firefox")
        assertFalse(uaSpoofBob.contains("AliceBrowser"))

        val timeShimBob = IsolationManager.generateTimeShimScript(profileBob)
        // In DEVICE time mode, zero time shim is injected to avoid polluting JavaScript Date object
        assertEquals("", timeShimBob)
        assertFalse(timeShimBob.contains("America/Los_Angeles"))

        val proxyUrlBob = profileBob.proxy.toProxyUrl()
        assertNull(proxyUrlBob)
    }

    @Test
    fun testFailClosedProxyBehavior() {
        // When proxy is enabled, direct fallback MUST be absent
        val secureProxy = ProxyConfig(
            enabled = true,
            protocol = ProxyProtocol.HTTP,
            host = "private.proxy",
            port = 3128
        )
        assertTrue(secureProxy.isValid())
        assertNotNull(secureProxy.toProxyUrl())

        val directProfile = profileBob.copy(proxy = ProxyConfig(enabled = false))
        assertNull(directProfile.proxy.toProxyUrl())
    }
}
