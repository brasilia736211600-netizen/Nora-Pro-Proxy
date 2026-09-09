package com.example.model

enum class ProxyProtocol(val label: String, val scheme: String) {
    DIRECT("Direct (No Proxy)", "direct"),
    HTTP("HTTP Proxy", "http"),
    HTTPS("HTTPS Proxy", "https"),
    SOCKS5("SOCKS5 Proxy", "socks5")
}

enum class UAMode(val label: String) {
    DEFAULT("Device Default"),
    PRESET("Curated Library"),
    CUSTOM("Custom String"),
    RANDOM("Randomize per Session")
}

enum class TimeMode(val label: String) {
    DEVICE("Device Time (System)"),
    MANUAL("Manual Timezone / Offset"),
    PROXY("Use Proxy Server Time")
}

data class ProxyConfig(
    val enabled: Boolean = false,
    val protocol: ProxyProtocol = ProxyProtocol.DIRECT,
    val host: String = "",
    val port: Int = 8080,
    val username: String = "",
    val password: String = "",
    val pacUrl: String = ""
) {
    fun toProxyUrl(): String? {
        if (!enabled || protocol == ProxyProtocol.DIRECT || host.isBlank()) return null
        val proto = when (protocol) {
            ProxyProtocol.HTTP -> "http"
            ProxyProtocol.HTTPS -> "https"
            ProxyProtocol.SOCKS5 -> "socks5"
            ProxyProtocol.DIRECT -> return null
        }
        return "$proto://$host:$port"
    }

    fun isValid(): Boolean {
        if (!enabled || protocol == ProxyProtocol.DIRECT) return true
        if (pacUrl.isNotBlank()) return pacUrl.startsWith("http://") || pacUrl.startsWith("https://")
        return host.isNotBlank() && port in 1..65535
    }
}

data class UserAgentItem(
    val id: String,
    val browser: String,
    val os: String,
    val version: String,
    val userAgentString: String
)

data class UserAgentConfig(
    val mode: UAMode = UAMode.DEFAULT,
    val selectedId: String = "",
    val browserFamily: String = "Chrome",
    val osFamily: String = "Android",
    val customUA: String = "",
    val activeUA: String = ""
)

data class TimeConfig(
    val mode: TimeMode = TimeMode.DEVICE,
    val timezoneId: String = "UTC",
    val offsetMinutes: Int = 0,
    val simulatedTimezone: String = "UTC"
)

data class ProfilePrivacyConfig(
    val blockTrackers: Boolean = true,
    val blockThirdPartyCookies: Boolean = true,
    val doNotTrack: Boolean = true,
    val forceDesktopMode: Boolean = false,
    val webrtcProtection: Boolean = true,
    val startupUrl: String = "",
    val notes: String = ""
)

data class NoraProfile(
    val id: String,
    val name: String,
    val colorHex: String = "#3B82F6",
    val isDefault: Boolean = false,
    val site: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = System.currentTimeMillis(),
    val usageCount: Int = 0,
    val proxy: ProxyConfig = ProxyConfig(),
    val userAgent: UserAgentConfig = UserAgentConfig(),
    val time: TimeConfig = TimeConfig(),
    val privacy: ProfilePrivacyConfig = ProfilePrivacyConfig()
)
