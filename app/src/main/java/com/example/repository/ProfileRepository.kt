package com.example.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.model.BrowserBookmark
import com.example.model.BrowserGeneralSettings
import com.example.model.BrowserHistoryItem
import com.example.model.FloatingWidgetSettings
import com.example.model.FullBrowserBackup
import com.example.model.ImportResult
import com.example.model.NoraProfile
import com.example.model.ProfileConflictResolution
import com.example.model.ProxyConfig
import com.example.model.ProxyProtocol
import com.example.model.TimeConfig
import com.example.model.TimeMode
import com.example.model.UAMode
import com.example.model.UserAgentConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ProfileRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("nora_profiles_prefs", Context.MODE_PRIVATE)

    private val _profiles = MutableStateFlow<List<NoraProfile>>(emptyList())
    val profiles: StateFlow<List<NoraProfile>> = _profiles.asStateFlow()

    private val _activeProfileId = MutableStateFlow<String>("")
    val activeProfileId: StateFlow<String> = _activeProfileId.asStateFlow()

    private val _widgetSettings = MutableStateFlow(FloatingWidgetSettings())
    val widgetSettings: StateFlow<FloatingWidgetSettings> = _widgetSettings.asStateFlow()

    private val _browserSettings = MutableStateFlow(BrowserGeneralSettings())
    val browserSettings: StateFlow<BrowserGeneralSettings> = _browserSettings.asStateFlow()

    private val _bookmarks = MutableStateFlow<List<BrowserBookmark>>(emptyList())
    val bookmarks: StateFlow<List<BrowserBookmark>> = _bookmarks.asStateFlow()

    private val _history = MutableStateFlow<List<BrowserHistoryItem>>(emptyList())
    val history: StateFlow<List<BrowserHistoryItem>> = _history.asStateFlow()

    init {
        loadProfiles()
        loadWidgetSettings()
        loadBrowserSettings()
        loadBookmarks()
        loadHistory()
    }

    private fun loadProfiles() {
        val jsonStr = prefs.getString("saved_profiles_json", null)
        if (jsonStr.isNullOrBlank()) {
            val defaultList = listOf(
                NoraProfile(
                    id = "profile_default",
                    name = "Default (General)",
                    colorHex = "#3B82F6",
                    isDefault = true,
                    site = "",
                    proxy = ProxyConfig(enabled = false, protocol = ProxyProtocol.DIRECT),
                    userAgent = UserAgentConfig(mode = UAMode.DEFAULT),
                    time = TimeConfig(mode = TimeMode.DEVICE)
                ),
                NoraProfile(
                    id = "profile_x_main",
                    name = "X / Twitter Main",
                    colorHex = "#0EA5E9",
                    site = "x.com",
                    proxy = ProxyConfig(enabled = false, protocol = ProxyProtocol.DIRECT),
                    userAgent = UserAgentConfig(mode = UAMode.PRESET, selectedId = "android_chrome_128"),
                    time = TimeConfig(mode = TimeMode.DEVICE)
                ),
                NoraProfile(
                    id = "profile_x_alt",
                    name = "X / Twitter Alt (Proxy US)",
                    colorHex = "#6366F1",
                    site = "x.com",
                    proxy = ProxyConfig(
                        enabled = true,
                        protocol = ProxyProtocol.HTTP,
                        host = "us-proxy.example.com",
                        port = 8080
                    ),
                    userAgent = UserAgentConfig(mode = UAMode.PRESET, selectedId = "ios_safari_17"),
                    time = TimeConfig(mode = TimeMode.MANUAL, timezoneId = "America/New_York")
                ),
                NoraProfile(
                    id = "profile_reddit_lurk",
                    name = "Reddit Privacy",
                    colorHex = "#F97316",
                    site = "reddit.com",
                    proxy = ProxyConfig(
                        enabled = true,
                        protocol = ProxyProtocol.SOCKS5,
                        host = "127.0.0.1",
                        port = 9050
                    ),
                    userAgent = UserAgentConfig(mode = UAMode.RANDOM),
                    time = TimeConfig(mode = TimeMode.PROXY)
                ),
                NoraProfile(
                    id = "profile_threads",
                    name = "Threads / Meta",
                    colorHex = "#EC4899",
                    site = "threads.net",
                    proxy = ProxyConfig(enabled = false),
                    userAgent = UserAgentConfig(mode = UAMode.DEFAULT),
                    time = TimeConfig(mode = TimeMode.DEVICE)
                )
            )
            _profiles.value = defaultList
            _activeProfileId.value = defaultList.first().id
            persistProfiles()
        } else {
            try {
                val array = JSONArray(jsonStr)
                val list = mutableListOf<NoraProfile>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(BackupManager.deserializeProfile(obj))
                }
                _profiles.value = list
                val active = prefs.getString("active_profile_id", "") ?: ""
                _activeProfileId.value = if (active.isNotBlank() && list.any { it.id == active }) {
                    active
                } else {
                    list.firstOrNull()?.id ?: ""
                }
            } catch (_: Exception) {
                // fallback
            }
        }
    }

    private fun loadWidgetSettings() {
        val str = prefs.getString("widget_settings_json", null) ?: return
        try {
            val obj = JSONObject(str)
            _widgetSettings.value = FloatingWidgetSettings(
                isVisible = obj.optBoolean("isVisible", true),
                sizeDp = obj.optInt("sizeDp", 56),
                opacityPercent = obj.optInt("opacityPercent", 90),
                offsetXPercent = obj.optDouble("offsetXPercent", 0.85).toFloat(),
                offsetYPercent = obj.optDouble("offsetYPercent", 0.75).toFloat(),
                showProxyBadge = obj.optBoolean("showProxyBadge", true),
                showProfileName = obj.optBoolean("showProfileName", true)
            )
        } catch (_: Exception) {}
    }

    fun updateWidgetSettings(settings: FloatingWidgetSettings) {
        _widgetSettings.value = settings
        try {
            val obj = JSONObject()
            obj.put("isVisible", settings.isVisible)
            obj.put("sizeDp", settings.sizeDp)
            obj.put("opacityPercent", settings.opacityPercent)
            obj.put("offsetXPercent", settings.offsetXPercent.toDouble())
            obj.put("offsetYPercent", settings.offsetYPercent.toDouble())
            obj.put("showProxyBadge", settings.showProxyBadge)
            obj.put("showProfileName", settings.showProfileName)
            prefs.edit().putString("widget_settings_json", obj.toString()).apply()
        } catch (_: Exception) {}
    }

    private fun loadBrowserSettings() {
        val str = prefs.getString("browser_settings_json", null) ?: return
        try {
            val obj = JSONObject(str)
            _browserSettings.value = BrowserGeneralSettings(
                homeUrl = obj.optString("homeUrl", "https://duckduckgo.com"),
                searchEngine = obj.optString("searchEngine", "DuckDuckGo"),
                adBlockEnabled = obj.optBoolean("adBlockEnabled", true),
                javaScriptEnabled = obj.optBoolean("javaScriptEnabled", true),
                doNotTrack = obj.optBoolean("doNotTrack", true),
                cookiesEnabled = obj.optBoolean("cookiesEnabled", true),
                clearCookiesOnExit = obj.optBoolean("clearCookiesOnExit", false)
            )
        } catch (_: Exception) {}
    }

    fun updateBrowserSettings(settings: BrowserGeneralSettings) {
        _browserSettings.value = settings
        try {
            val obj = JSONObject()
            obj.put("homeUrl", settings.homeUrl)
            obj.put("searchEngine", settings.searchEngine)
            obj.put("adBlockEnabled", settings.adBlockEnabled)
            obj.put("javaScriptEnabled", settings.javaScriptEnabled)
            obj.put("doNotTrack", settings.doNotTrack)
            obj.put("cookiesEnabled", settings.cookiesEnabled)
            obj.put("clearCookiesOnExit", settings.clearCookiesOnExit)
            prefs.edit().putString("browser_settings_json", obj.toString()).apply()
        } catch (_: Exception) {}
    }

    private fun loadBookmarks() {
        val str = prefs.getString("bookmarks_json", null) ?: return
        try {
            val arr = JSONArray(str)
            val list = mutableListOf<BrowserBookmark>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    BrowserBookmark(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.optString("title", ""),
                        url = obj.optString("url", ""),
                        profileId = obj.optString("profileId", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            _bookmarks.value = list
        } catch (_: Exception) {}
    }

    fun addBookmark(title: String, url: String, profileId: String = "") {
        val item = BrowserBookmark(
            id = UUID.randomUUID().toString(),
            title = title.ifBlank { url },
            url = url,
            profileId = profileId
        )
        val updated = _bookmarks.value + item
        _bookmarks.value = updated
        persistBookmarks()
    }

    fun deleteBookmark(id: String) {
        _bookmarks.value = _bookmarks.value.filter { it.id != id }
        persistBookmarks()
    }

    private fun persistBookmarks() {
        try {
            val arr = JSONArray()
            for (b in _bookmarks.value) {
                val obj = JSONObject()
                obj.put("id", b.id)
                obj.put("title", b.title)
                obj.put("url", b.url)
                obj.put("profileId", b.profileId)
                obj.put("createdAt", b.createdAt)
                arr.put(obj)
            }
            prefs.edit().putString("bookmarks_json", arr.toString()).apply()
        } catch (_: Exception) {}
    }

    private fun loadHistory() {
        val str = prefs.getString("history_json", null) ?: return
        try {
            val arr = JSONArray(str)
            val list = mutableListOf<BrowserHistoryItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    BrowserHistoryItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.optString("title", ""),
                        url = obj.optString("url", ""),
                        profileId = obj.optString("profileId", ""),
                        visitedAt = obj.optLong("visitedAt", System.currentTimeMillis())
                    )
                )
            }
            _history.value = list
        } catch (_: Exception) {}
    }

    fun addHistory(title: String, url: String, profileId: String = "") {
        if (url.isBlank() || url.startsWith("about:")) return
        val item = BrowserHistoryItem(
            id = UUID.randomUUID().toString(),
            title = title.ifBlank { url },
            url = url,
            profileId = profileId,
            visitedAt = System.currentTimeMillis()
        )
        // Keep last 200 history items
        val updated = (listOf(item) + _history.value).take(200)
        _history.value = updated
        persistHistory()
    }

    fun clearHistory() {
        _history.value = emptyList()
        persistHistory()
    }

    private fun persistHistory() {
        try {
            val arr = JSONArray()
            for (h in _history.value) {
                val obj = JSONObject()
                obj.put("id", h.id)
                obj.put("title", h.title)
                obj.put("url", h.url)
                obj.put("profileId", h.profileId)
                obj.put("visitedAt", h.visitedAt)
                arr.put(obj)
            }
            prefs.edit().putString("history_json", arr.toString()).apply()
        } catch (_: Exception) {}
    }

    fun setActiveProfile(id: String) {
        if (_profiles.value.any { it.id == id }) {
            _activeProfileId.value = id
            prefs.edit().putString("active_profile_id", id).apply()
            val updated = _profiles.value.map {
                if (it.id == id) it.copy(lastUsedAt = System.currentTimeMillis(), usageCount = it.usageCount + 1)
                else it
            }
            _profiles.value = updated
            persistProfiles()
        }
    }

    fun getActiveProfile(): NoraProfile? {
        return _profiles.value.find { it.id == _activeProfileId.value } ?: _profiles.value.firstOrNull()
    }

    fun saveProfile(profile: NoraProfile) {
        val current = _profiles.value.toMutableList()
        val index = current.indexOfFirst { it.id == profile.id }
        if (index >= 0) {
            current[index] = profile.copy(updatedAt = System.currentTimeMillis())
        } else {
            current.add(profile.copy(createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()))
        }
        _profiles.value = current
        persistProfiles()
    }

    fun duplicateProfile(sourceId: String): NoraProfile? {
        val source = _profiles.value.find { it.id == sourceId } ?: return null
        val newProfile = source.copy(
            id = "profile_" + UUID.randomUUID().toString().take(8),
            name = "${source.name} (Copy)",
            isDefault = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            lastUsedAt = System.currentTimeMillis(),
            usageCount = 0
        )
        saveProfile(newProfile)
        return newProfile
    }

    fun deleteProfile(id: String): Boolean {
        val current = _profiles.value
        val toDelete = current.find { it.id == id } ?: return false
        if (toDelete.isDefault) return false
        val updated = current.filter { it.id != id }
        _profiles.value = updated
        if (_activeProfileId.value == id) {
            _activeProfileId.value = updated.firstOrNull()?.id ?: ""
            prefs.edit().putString("active_profile_id", _activeProfileId.value).apply()
        }
        persistProfiles()
        return true
    }

    fun toggleProxy(id: String): Boolean {
        val current = _profiles.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            val p = current[index]
            val newProxy = p.proxy.copy(enabled = !p.proxy.enabled)
            current[index] = p.copy(proxy = newProxy, updatedAt = System.currentTimeMillis())
            _profiles.value = current
            persistProfiles()
            return newProxy.enabled
        }
        return false
    }

    private fun persistProfiles() {
        val array = JSONArray()
        for (p in _profiles.value) {
            array.put(BackupManager.serializeProfile(p))
        }
        prefs.edit().putString("saved_profiles_json", array.toString()).apply()
    }

    // --- IMPORT & EXPORT LOGIC ---

    fun exportAllProfilesJson(): String {
        return BackupManager.exportProfilesJson(_profiles.value)
    }

    fun exportSelectedProfilesJson(selectedIds: Set<String>): String {
        val selected = _profiles.value.filter { it.id in selectedIds }
        return BackupManager.exportProfilesJson(selected)
    }

    fun importProfiles(
        profilesToImport: List<NoraProfile>,
        conflictResolution: ProfileConflictResolution
    ): ImportResult {
        if (profilesToImport.isEmpty()) {
            return ImportResult(success = false, message = "لا توجد بروفايلات محددة للاستيراد / No profiles selected")
        }

        val currentList = _profiles.value.toMutableList()
        val existingIds = currentList.map { it.id }.toSet()

        var importedCount = 0
        var overwrittenCount = 0
        var skippedCount = 0
        var renamedCount = 0

        for (profile in profilesToImport) {
            val isDuplicate = existingIds.contains(profile.id)

            if (isDuplicate) {
                when (conflictResolution) {
                    ProfileConflictResolution.OVERWRITE -> {
                        val index = currentList.indexOfFirst { it.id == profile.id }
                        if (index >= 0) {
                            currentList[index] = profile.copy(updatedAt = System.currentTimeMillis())
                            overwrittenCount++
                            importedCount++
                        }
                    }
                    ProfileConflictResolution.SKIP -> {
                        skippedCount++
                    }
                    ProfileConflictResolution.KEEP_BOTH -> {
                        val newId = "profile_" + UUID.randomUUID().toString().take(8)
                        val renamedProfile = profile.copy(
                            id = newId,
                            name = "${profile.name} (Imported)",
                            isDefault = false,
                            updatedAt = System.currentTimeMillis()
                        )
                        currentList.add(renamedProfile)
                        renamedCount++
                        importedCount++
                    }
                }
            } else {
                currentList.add(profile)
                importedCount++
            }
        }

        _profiles.value = currentList
        persistProfiles()

        return ImportResult(
            success = true,
            message = "تم استيراد $importedCount بروفايل بنجاح",
            importedProfilesCount = importedCount,
            overwrittenCount = overwrittenCount,
            skippedCount = skippedCount,
            renamedCount = renamedCount
        )
    }

    fun exportFullBrowserBackup(): FullBrowserBackup {
        return FullBrowserBackup(
            version = BackupManager.SCHEMA_VERSION,
            exportTimestamp = System.currentTimeMillis(),
            app = BackupManager.APP_IDENTIFIER,
            backupType = BackupManager.TYPE_FULL_BROWSER,
            profiles = _profiles.value,
            activeProfileId = _activeProfileId.value,
            widgetSettings = _widgetSettings.value,
            browserSettings = _browserSettings.value,
            bookmarks = _bookmarks.value,
            history = _history.value
        )
    }

    fun exportFullBrowserJson(): String {
        return BackupManager.exportFullBrowserJson(exportFullBrowserBackup())
    }

    fun restoreFullBrowserBackup(backup: FullBrowserBackup, mergeMode: Boolean): ImportResult {
        return try {
            if (mergeMode) {
                // Merge profiles
                val result = importProfiles(backup.profiles, ProfileConflictResolution.KEEP_BOTH)

                // Merge bookmarks
                val existingUrls = _bookmarks.value.map { it.url }.toSet()
                val newBookmarks = backup.bookmarks.filter { it.url !in existingUrls }
                _bookmarks.value = _bookmarks.value + newBookmarks
                persistBookmarks()

                // Merge history
                val existingHistoryIds = _history.value.map { it.id }.toSet()
                val newHistory = backup.history.filter { it.id !in existingHistoryIds }
                _history.value = (_history.value + newHistory).take(200)
                persistHistory()

                ImportResult(
                    success = true,
                    message = "تم دمج بيانات المتصفح بنجاح",
                    importedProfilesCount = result.importedProfilesCount,
                    renamedCount = result.renamedCount,
                    bookmarksRestored = newBookmarks.size,
                    historyRestored = newHistory.size
                )
            } else {
                // Full overwrite / restore
                _profiles.value = if (backup.profiles.isNotEmpty()) backup.profiles else _profiles.value
                persistProfiles()

                if (backup.activeProfileId.isNotBlank() && _profiles.value.any { it.id == backup.activeProfileId }) {
                    _activeProfileId.value = backup.activeProfileId
                    prefs.edit().putString("active_profile_id", backup.activeProfileId).apply()
                }

                updateWidgetSettings(backup.widgetSettings)
                updateBrowserSettings(backup.browserSettings)

                _bookmarks.value = backup.bookmarks
                persistBookmarks()

                _history.value = backup.history
                persistHistory()

                ImportResult(
                    success = true,
                    message = "تمت استعادة نسخة المتصفح بالكامل بنجاح",
                    importedProfilesCount = backup.profiles.size,
                    overwrittenCount = backup.profiles.size,
                    bookmarksRestored = backup.bookmarks.size,
                    historyRestored = backup.history.size
                )
            }
        } catch (e: Exception) {
            ImportResult(success = false, message = "فشلت الاستعادة: ${e.message}")
        }
    }
}
