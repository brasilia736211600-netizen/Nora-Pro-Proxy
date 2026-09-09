package com.example.util

import java.net.URI
import java.net.URLEncoder

object UrlHelper {

    private val URL_REGEX = Regex(
        "^(https?://|ftp://)?[a-zA-Z0-9.-]+(\\.[a-zA-Z]{2,})(:[0-9]{1,5})?(/.*)?$",
        RegexOption.IGNORE_CASE
    )

    private val IP_REGEX = Regex(
        "^([0-9]{1,3}\\.){3}[0-9]{1,3}(:[0-9]{1,5})?(/.*)?$"
    )

    val searchEngines = listOf(
        "DuckDuckGo" to "https://duckduckgo.com/?q=",
        "Google" to "https://www.google.com/search?q=",
        "Brave Search" to "https://search.brave.com/search?q=",
        "Bing" to "https://www.bing.com/search?q=",
        "Startpage" to "https://www.startpage.com/sp/search?query=",
        "Yahoo" to "https://search.yahoo.com/search?p=",
        "Yandex" to "https://yandex.com/search/?text="
    )

    /**
     * Resolves user input into a valid web URL or a search query.
     * Prevents amateur behavior like prefixing arbitrary search queries with https://.
     */
    fun resolveInputToUrl(input: String, searchEngine: String = "DuckDuckGo"): String {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return "https://duckduckgo.com"

        // Already explicit protocol
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("about:", ignoreCase = true) ||
            trimmed.startsWith("file://", ignoreCase = true) ||
            trimmed.startsWith("chrome://", ignoreCase = true)
        ) {
            return trimmed
        }

        // Localhost or direct IP address
        if (trimmed.startsWith("localhost", ignoreCase = true) || IP_REGEX.matches(trimmed)) {
            return "http://$trimmed"
        }

        // Contains spaces or lacks dots -> definitely a search query
        if (trimmed.contains(" ") || !trimmed.contains(".")) {
            return buildSearchUrl(searchEngine, trimmed)
        }

        // Valid domain name format
        if (URL_REGEX.matches(trimmed)) {
            return "https://$trimmed"
        }

        // Default fallback to search engine
        return buildSearchUrl(searchEngine, trimmed)
    }

    fun buildSearchUrl(searchEngine: String, query: String): String {
        val encoded = try {
            URLEncoder.encode(query, "UTF-8")
        } catch (_: Exception) {
            query
        }
        val prefix = searchEngines.find { it.first.equals(searchEngine, ignoreCase = true) }?.second
            ?: "https://duckduckgo.com/?q="
        return prefix + encoded
    }

    fun extractDomain(url: String): String {
        return try {
            val uri = URI(url)
            uri.host ?: url
        } catch (_: Exception) {
            url.substringAfter("://").substringBefore("/")
        }
    }

    fun isHttps(url: String): Boolean {
        return url.startsWith("https://", ignoreCase = true)
    }
}
