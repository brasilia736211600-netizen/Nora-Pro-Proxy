package com.example.model

import java.util.TimeZone

object TimezoneHelper {
    val commonTimezones: List<String> by lazy {
        TimeZone.getAvailableIDs()
            .filter { it.contains("/") && !it.startsWith("Etc/") }
            .sorted()
    }

    fun getDisplayName(tzId: String): String {
        return try {
            val tz = TimeZone.getTimeZone(tzId)
            val offsetHours = tz.rawOffset / (1000 * 60 * 60)
            val offsetMinutes = Math.abs(tz.rawOffset / (1000 * 60) % 60)
            val sign = if (offsetHours >= 0) "+" else "-"
            val formattedOffset = "GMT$sign%02d:%02d".format(Math.abs(offsetHours), offsetMinutes)
            "$tzId ($formattedOffset)"
        } catch (_: Exception) {
            tzId
        }
    }
}
