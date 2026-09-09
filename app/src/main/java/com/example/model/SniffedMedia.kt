package com.example.model

import java.net.URI
import java.util.UUID

enum class MediaType {
    VIDEO,
    AUDIO,
    STREAM_HLS,
    STREAM_DASH,
    UNKNOWN
}

data class SniffedMedia(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val title: String = "",
    val mediaType: MediaType = MediaType.VIDEO,
    val mimeType: String = "video/mp4",
    val timestamp: Long = System.currentTimeMillis()
) {
    val domain: String
        get() = try {
            URI(url).host ?: url
        } catch (_: Exception) {
            url
        }

    val typeLabel: String
        get() = when (mediaType) {
            MediaType.STREAM_HLS -> "M3U8 / HLS Live"
            MediaType.STREAM_DASH -> "MPD / DASH"
            MediaType.VIDEO -> "MP4 / WebM Video"
            MediaType.AUDIO -> "MP3 / Audio"
            MediaType.UNKNOWN -> "Media Stream"
        }
}
