package com.example.model

object UserAgentLibrary {
    val items: List<UserAgentItem> = listOf(
        // Android
        UserAgentItem(
            id = "android_chrome_128",
            browser = "Chrome",
            os = "Android",
            version = "128.0 (Android 14)",
            userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.6613.127 Mobile Safari/537.36"
        ),
        UserAgentItem(
            id = "android_chrome_127",
            browser = "Chrome",
            os = "Android",
            version = "127.0 (Android 13)",
            userAgentString = "Mozilla/5.0 (Linux; Android 13; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.6533.103 Mobile Safari/537.36"
        ),
        UserAgentItem(
            id = "android_firefox_130",
            browser = "Firefox",
            os = "Android",
            version = "130.0 (Android 14)",
            userAgentString = "Mozilla/5.0 (Android 14; Mobile; rv:130.0) Gecko/130.0 Firefox/130.0"
        ),
        UserAgentItem(
            id = "android_samsung_25",
            browser = "Samsung Internet",
            os = "Android",
            version = "25.0 (Android 14)",
            userAgentString = "Mozilla/5.0 (Linux; Android 14; SAMSUNG SM-S928U) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/25.0 Chrome/121.0.6167.101 Mobile Safari/537.36"
        ),
        UserAgentItem(
            id = "android_opera_83",
            browser = "Opera",
            os = "Android",
            version = "83.0 (Android 14)",
            userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.6422.165 Mobile Safari/537.36 OPR/83.1.4331.81525"
        ),

        // iOS & iPadOS
        UserAgentItem(
            id = "ios_safari_17",
            browser = "Safari",
            os = "iOS",
            version = "17.6 (iPhone 15 Pro)",
            userAgentString = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.6 Mobile/15E148 Safari/604.1"
        ),
        UserAgentItem(
            id = "ios_safari_18",
            browser = "Safari",
            os = "iOS",
            version = "18.0 (iPhone 16 Pro)",
            userAgentString = "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1"
        ),
        UserAgentItem(
            id = "ios_chrome_128",
            browser = "Chrome",
            os = "iOS",
            version = "128.0 (iOS 17)",
            userAgentString = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/128.0.6613.98 Mobile/15E148 Safari/604.1"
        ),
        UserAgentItem(
            id = "ios_firefox_130",
            browser = "Firefox",
            os = "iOS",
            version = "130.0 (iOS 17)",
            userAgentString = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) FxiOS/130.0 Mobile/15E148 Safari/605.1.15"
        ),
        UserAgentItem(
            id = "ipados_safari_17",
            browser = "Safari",
            os = "iOS",
            version = "17.6 (iPad Pro)",
            userAgentString = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.6 Safari/605.1.15"
        ),

        // Windows
        UserAgentItem(
            id = "win_chrome_128",
            browser = "Chrome",
            os = "Windows",
            version = "128.0 (Windows 11)",
            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
        ),
        UserAgentItem(
            id = "win_edge_128",
            browser = "Edge",
            os = "Windows",
            version = "128.0 (Windows 11)",
            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36 Edg/128.0.0.0"
        ),
        UserAgentItem(
            id = "win_firefox_130",
            browser = "Firefox",
            os = "Windows",
            version = "130.0 (Windows 11)",
            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:130.0) Gecko/20100101 Firefox/130.0"
        ),
        UserAgentItem(
            id = "win_opera_113",
            browser = "Opera",
            os = "Windows",
            version = "113.0 (Windows 11)",
            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36 OPR/113.0.0.0"
        ),

        // macOS
        UserAgentItem(
            id = "mac_safari_17",
            browser = "Safari",
            os = "macOS",
            version = "17.6 (macOS Sonoma)",
            userAgentString = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.6 Safari/605.1.15"
        ),
        UserAgentItem(
            id = "mac_safari_18",
            browser = "Safari",
            os = "macOS",
            version = "18.0 (macOS Sequoia)",
            userAgentString = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15"
        ),
        UserAgentItem(
            id = "mac_chrome_128",
            browser = "Chrome",
            os = "macOS",
            version = "128.0 (macOS Sonoma)",
            userAgentString = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
        ),
        UserAgentItem(
            id = "mac_firefox_130",
            browser = "Firefox",
            os = "macOS",
            version = "130.0 (macOS Sonoma)",
            userAgentString = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:130.0) Gecko/20100101 Firefox/130.0"
        ),

        // Linux
        UserAgentItem(
            id = "linux_chrome_128",
            browser = "Chrome",
            os = "Linux",
            version = "128.0 (Ubuntu 24.04)",
            userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
        ),
        UserAgentItem(
            id = "linux_firefox_130",
            browser = "Firefox",
            os = "Linux",
            version = "130.0 (Fedora 40)",
            userAgentString = "Mozilla/5.0 (X11; Linux x86_64; rv:130.0) Gecko/20100101 Firefox/130.0"
        )
    )

    fun getOSList(): List<String> = items.map { it.os }.distinct().sorted()

    fun getBrowsersForOS(os: String): List<String> =
        items.filter { it.os == os }.map { it.browser }.distinct().sorted()

    fun getVersionsFor(os: String, browser: String): List<UserAgentItem> =
        items.filter { it.os == os && it.browser == browser }

    fun findById(id: String): UserAgentItem? = items.find { it.id == id }

    fun getRandomForFamily(os: String?, browser: String?): UserAgentItem {
        val pool = items.filter { item ->
            (os == null || item.os.equals(os, ignoreCase = true)) &&
            (browser == null || item.browser.equals(browser, ignoreCase = true))
        }
        return if (pool.isNotEmpty()) pool.random() else items.random()
    }
}
