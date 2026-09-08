package com.example.model

enum class ProfileConflictResolution(val titleArabic: String, val titleEnglish: String, val description: String) {
    KEEP_BOTH(
        titleArabic = "إنشاء نسخة جديدة (تجنب التعارض)",
        titleEnglish = "Keep Both (Create Copy)",
        description = "توليد معرف جديد للبروفايلات المستوردة لمنع استبدال بروفايلاتك الحالية"
    ),
    OVERWRITE(
        titleArabic = "استبدال البروفايل المطابق",
        titleEnglish = "Overwrite Existing",
        description = "تحديث واستبدال أي بروفايل حالي يحمل نفس المعرف"
    ),
    SKIP(
        titleArabic = "تخطي البروفايلات الموجودة",
        titleEnglish = "Skip Existing",
        description = "استيراد البروفايلات الجديدة فقط وتجاهل المتطابقة"
    )
}

data class BrowserBookmark(
    val id: String,
    val title: String,
    val url: String,
    val profileId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class BrowserHistoryItem(
    val id: String,
    val title: String,
    val url: String,
    val profileId: String = "",
    val visitedAt: Long = System.currentTimeMillis()
)

data class BrowserGeneralSettings(
    val homeUrl: String = "https://duckduckgo.com",
    val searchEngine: String = "DuckDuckGo",
    val adBlockEnabled: Boolean = true,
    val javaScriptEnabled: Boolean = true,
    val doNotTrack: Boolean = true,
    val cookiesEnabled: Boolean = true,
    val clearCookiesOnExit: Boolean = false
)

data class FullBrowserBackup(
    val version: Int = 1,
    val exportTimestamp: Long = System.currentTimeMillis(),
    val app: String = "Nora Browser",
    val backupType: String = "FULL_BROWSER",
    val profiles: List<NoraProfile> = emptyList(),
    val activeProfileId: String = "",
    val widgetSettings: FloatingWidgetSettings = FloatingWidgetSettings(),
    val browserSettings: BrowserGeneralSettings = BrowserGeneralSettings(),
    val bookmarks: List<BrowserBookmark> = emptyList(),
    val history: List<BrowserHistoryItem> = emptyList()
)

data class ProfilesBackupPayload(
    val version: Int = 1,
    val exportTimestamp: Long = System.currentTimeMillis(),
    val app: String = "Nora Browser",
    val backupType: String = "PROFILES_ONLY",
    val profileCount: Int = 0,
    val profiles: List<NoraProfile> = emptyList()
)

data class ParsedBackupPreview(
    val isFullBackup: Boolean,
    val version: Int,
    val timestamp: Long,
    val profiles: List<NoraProfile>,
    val bookmarksCount: Int = 0,
    val historyCount: Int = 0,
    val hasBrowserSettings: Boolean = false,
    val hasWidgetSettings: Boolean = false,
    val rawJson: String = ""
)

data class ImportResult(
    val success: Boolean,
    val message: String,
    val importedProfilesCount: Int = 0,
    val overwrittenCount: Int = 0,
    val skippedCount: Int = 0,
    val renamedCount: Int = 0,
    val bookmarksRestored: Int = 0,
    val historyRestored: Int = 0
)
