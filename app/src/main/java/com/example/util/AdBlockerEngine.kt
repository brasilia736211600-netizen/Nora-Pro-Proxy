package com.example.util

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BlockedTrackerEntry(
    val url: String,
    val host: String,
    val timestamp: Long = System.currentTimeMillis()
)

object AdBlockerEngine {

    private val _blockedTrackersCount = MutableStateFlow(0)
    val blockedTrackersCount: StateFlow<Int> = _blockedTrackersCount.asStateFlow()

    private val _blockedLog = MutableStateFlow<List<BlockedTrackerEntry>>(emptyList())
    val blockedLog: StateFlow<List<BlockedTrackerEntry>> = _blockedLog.asStateFlow()

    private val customWhitelist = hashSetOf<String>()
    private val customBlacklist = hashSetOf<String>()

    private val adHosts = hashSetOf(
        "doubleclick.net",
        "google-analytics.com",
        "googlesyndication.com",
        "googletagservices.com",
        "adservice.google.com",
        "pagead2.googlesyndication.com",
        "ads.youtube.com",
        "facebook.net",
        "connect.facebook.net",
        "pixel.facebook.com",
        "adservice.google.",
        "adnxs.com",
        "criteo.com",
        "outbrain.com",
        "taboola.com",
        "scorecardresearch.com",
        "quantserve.com",
        "chartbeat.net",
        "hotjar.com",
        "clarity.ms",
        "branch.io",
        "adjust.com",
        "appsflyer.com",
        "flurry.com",
        "app-measurement.com",
        "firebaseinstallations.googleapis.com",
        "adroll.com",
        "rubiconproject.com",
        "pubmatic.com",
        "openx.net",
        "smartadserver.com",
        "casalemedia.com",
        "bidswitch.net",
        "moatads.com",
        "advertising.com",
        "adsystem",
        "advertising",
        "adserver",
        "telemetry",
        "trackers"
    )

    fun isAdOrTracker(url: String): Boolean {
        if (url.isBlank()) return false
        val lower = url.lowercase()
        val host = try {
            Uri.parse(url).host?.lowercase() ?: ""
        } catch (_: Exception) {
            ""
        }

        // Whitelist check
        if (host.isNotBlank()) {
            for (w in customWhitelist) {
                if (host == w || host.endsWith(".$w")) return false
            }
        }

        // Blacklist check
        if (host.isNotBlank()) {
            for (b in customBlacklist) {
                if (host == b || host.endsWith(".$b")) {
                    recordBlock(url, host)
                    return true
                }
            }
        }

        if (host.isNotBlank()) {
            for (ad in adHosts) {
                if (host == ad || host.endsWith(".$ad") || host.contains(ad)) {
                    recordBlock(url, host)
                    return true
                }
            }
        }

        if (lower.contains("/ads/") || lower.contains("/pagead/") || lower.contains("/doubleclick/")) {
            recordBlock(url, host.ifBlank { "ad-resource" })
            return true
        }

        return false
    }

    private fun recordBlock(url: String, host: String) {
        _blockedTrackersCount.value += 1
        val entry = BlockedTrackerEntry(url = url, host = host)
        val current = _blockedLog.value
        // Keep last 100 entries
        _blockedLog.value = (listOf(entry) + current).take(100)
    }

    fun addWhitelist(host: String) {
        if (host.isNotBlank()) customWhitelist.add(host.lowercase().trim())
    }

    fun removeWhitelist(host: String) {
        customWhitelist.remove(host.lowercase().trim())
    }

    fun isWhitelisted(host: String): Boolean {
        return customWhitelist.contains(host.lowercase().trim())
    }

    fun clearLog() {
        _blockedLog.value = emptyList()
        _blockedTrackersCount.value = 0
    }

    fun resetCount() {
        _blockedTrackersCount.value = 0
    }
}
