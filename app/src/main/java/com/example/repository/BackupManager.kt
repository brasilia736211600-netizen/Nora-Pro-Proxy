package com.example.repository

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.model.BrowserBookmark
import com.example.model.BrowserGeneralSettings
import com.example.model.BrowserHistoryItem
import com.example.model.FloatingWidgetSettings
import com.example.model.FullBrowserBackup
import com.example.model.NoraProfile
import com.example.model.ParsedBackupPreview
import com.example.model.ProfilesBackupPayload
import com.example.model.ProxyConfig
import com.example.model.ProxyProtocol
import com.example.model.TimeConfig
import com.example.model.TimeMode
import com.example.model.UAMode
import com.example.model.UserAgentConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.UUID

object BackupManager {

    const val SCHEMA_VERSION = 1
    const val APP_IDENTIFIER = "Nora Browser"
    const val TYPE_FULL_BROWSER = "FULL_BROWSER"
    const val TYPE_PROFILES_ONLY = "PROFILES_ONLY"

    fun serializeProfile(p: NoraProfile): JSONObject {
        val obj = JSONObject()
        obj.put("id", p.id)
        obj.put("name", p.name)
        obj.put("colorHex", p.colorHex)
        obj.put("isDefault", p.isDefault)
        obj.put("site", p.site)
        obj.put("createdAt", p.createdAt)
        obj.put("updatedAt", p.updatedAt)
        obj.put("lastUsedAt", p.lastUsedAt)
        obj.put("usageCount", p.usageCount)

        // Proxy
        val proxyObj = JSONObject()
        proxyObj.put("enabled", p.proxy.enabled)
        proxyObj.put("protocol", p.proxy.protocol.name)
        proxyObj.put("host", p.proxy.host)
        proxyObj.put("port", p.proxy.port)
        proxyObj.put("username", p.proxy.username)
        proxyObj.put("password", p.proxy.password)
        proxyObj.put("pacUrl", p.proxy.pacUrl)
        obj.put("proxy", proxyObj)

        // UA
        val uaObj = JSONObject()
        uaObj.put("mode", p.userAgent.mode.name)
        uaObj.put("selectedId", p.userAgent.selectedId)
        uaObj.put("browserFamily", p.userAgent.browserFamily)
        uaObj.put("osFamily", p.userAgent.osFamily)
        uaObj.put("customUA", p.userAgent.customUA)
        obj.put("userAgent", uaObj)

        // Time
        val timeObj = JSONObject()
        timeObj.put("mode", p.time.mode.name)
        timeObj.put("timezoneId", p.time.timezoneId)
        timeObj.put("offsetMinutes", p.time.offsetMinutes)
        timeObj.put("simulatedTimezone", p.time.simulatedTimezone)
        obj.put("time", timeObj)

        return obj
    }

    fun deserializeProfile(obj: JSONObject): NoraProfile {
        val proxyObj = obj.optJSONObject("proxy") ?: JSONObject()
        val uaObj = obj.optJSONObject("userAgent") ?: JSONObject()
        val timeObj = obj.optJSONObject("time") ?: JSONObject()

        val proxy = ProxyConfig(
            enabled = proxyObj.optBoolean("enabled", false),
            protocol = try {
                ProxyProtocol.valueOf(proxyObj.optString("protocol", ProxyProtocol.DIRECT.name))
            } catch (_: Exception) { ProxyProtocol.DIRECT },
            host = proxyObj.optString("host", ""),
            port = proxyObj.optInt("port", 8080),
            username = proxyObj.optString("username", ""),
            password = proxyObj.optString("password", ""),
            pacUrl = proxyObj.optString("pacUrl", "")
        )

        val ua = UserAgentConfig(
            mode = try {
                UAMode.valueOf(uaObj.optString("mode", UAMode.DEFAULT.name))
            } catch (_: Exception) { UAMode.DEFAULT },
            selectedId = uaObj.optString("selectedId", ""),
            browserFamily = uaObj.optString("browserFamily", "Chrome"),
            osFamily = uaObj.optString("osFamily", "Android"),
            customUA = uaObj.optString("customUA", "")
        )

        val time = TimeConfig(
            mode = try {
                TimeMode.valueOf(timeObj.optString("mode", TimeMode.DEVICE.name))
            } catch (_: Exception) { TimeMode.DEVICE },
            timezoneId = timeObj.optString("timezoneId", "UTC"),
            offsetMinutes = timeObj.optInt("offsetMinutes", 0),
            simulatedTimezone = timeObj.optString("simulatedTimezone", "UTC")
        )

        return NoraProfile(
            id = obj.optString("id", UUID.randomUUID().toString()),
            name = obj.optString("name", "Unnamed Profile"),
            colorHex = obj.optString("colorHex", "#3B82F6"),
            isDefault = obj.optBoolean("isDefault", false),
            site = obj.optString("site", ""),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
            lastUsedAt = obj.optLong("lastUsedAt", System.currentTimeMillis()),
            usageCount = obj.optInt("usageCount", 0),
            proxy = proxy,
            userAgent = ua,
            time = time
        )
    }

    fun exportFullBrowserJson(backup: FullBrowserBackup): String {
        val root = JSONObject()
        root.put("version", backup.version)
        root.put("app", backup.app)
        root.put("exportTimestamp", backup.exportTimestamp)
        root.put("backupType", TYPE_FULL_BROWSER)
        root.put("activeProfileId", backup.activeProfileId)

        // Profiles array
        val profilesArray = JSONArray()
        for (p in backup.profiles) {
            profilesArray.put(serializeProfile(p))
        }
        root.put("profiles", profilesArray)

        // Widget settings
        val widgetObj = JSONObject()
        widgetObj.put("isVisible", backup.widgetSettings.isVisible)
        widgetObj.put("sizeDp", backup.widgetSettings.sizeDp)
        widgetObj.put("opacityPercent", backup.widgetSettings.opacityPercent)
        widgetObj.put("offsetXPercent", backup.widgetSettings.offsetXPercent.toDouble())
        widgetObj.put("offsetYPercent", backup.widgetSettings.offsetYPercent.toDouble())
        widgetObj.put("showProxyBadge", backup.widgetSettings.showProxyBadge)
        widgetObj.put("showProfileName", backup.widgetSettings.showProfileName)
        root.put("widgetSettings", widgetObj)

        // Browser general settings
        val browserObj = JSONObject()
        browserObj.put("homeUrl", backup.browserSettings.homeUrl)
        browserObj.put("searchEngine", backup.browserSettings.searchEngine)
        browserObj.put("adBlockEnabled", backup.browserSettings.adBlockEnabled)
        browserObj.put("javaScriptEnabled", backup.browserSettings.javaScriptEnabled)
        browserObj.put("doNotTrack", backup.browserSettings.doNotTrack)
        browserObj.put("cookiesEnabled", backup.browserSettings.cookiesEnabled)
        browserObj.put("clearCookiesOnExit", backup.browserSettings.clearCookiesOnExit)
        root.put("browserSettings", browserObj)

        // Bookmarks
        val bookmarksArray = JSONArray()
        for (bm in backup.bookmarks) {
            val bmObj = JSONObject()
            bmObj.put("id", bm.id)
            bmObj.put("title", bm.title)
            bmObj.put("url", bm.url)
            bmObj.put("profileId", bm.profileId)
            bmObj.put("createdAt", bm.createdAt)
            bookmarksArray.put(bmObj)
        }
        root.put("bookmarks", bookmarksArray)

        // History
        val historyArray = JSONArray()
        for (hi in backup.history) {
            val hiObj = JSONObject()
            hiObj.put("id", hi.id)
            hiObj.put("title", hi.title)
            hiObj.put("url", hi.url)
            hiObj.put("profileId", hi.profileId)
            hiObj.put("visitedAt", hi.visitedAt)
            historyArray.put(hiObj)
        }
        root.put("history", historyArray)

        return root.toString(2)
    }

    fun exportProfilesJson(profiles: List<NoraProfile>): String {
        val root = JSONObject()
        root.put("version", SCHEMA_VERSION)
        root.put("app", APP_IDENTIFIER)
        root.put("exportTimestamp", System.currentTimeMillis())
        root.put("backupType", TYPE_PROFILES_ONLY)
        root.put("profileCount", profiles.size)

        val array = JSONArray()
        for (p in profiles) {
            array.put(serializeProfile(p))
        }
        root.put("profiles", array)

        return root.toString(2)
    }

    fun parseBackup(jsonString: String): Result<ParsedBackupPreview> {
        return try {
            val trimmed = jsonString.trim()
            if (trimmed.isEmpty()) {
                return Result.failure(IllegalArgumentException("JSON string is empty"))
            }

            // Case 1: Plain JSON Array of profiles
            if (trimmed.startsWith("[")) {
                val array = JSONArray(trimmed)
                val list = mutableListOf<NoraProfile>()
                for (i in 0 until array.length()) {
                    list.add(deserializeProfile(array.getJSONObject(i)))
                }
                return Result.success(
                    ParsedBackupPreview(
                        isFullBackup = false,
                        version = 1,
                        timestamp = System.currentTimeMillis(),
                        profiles = list,
                        rawJson = jsonString
                    )
                )
            }

            // Case 2: JSON Object (Full Browser or Profiles Payload)
            val root = JSONObject(trimmed)
            val backupType = root.optString("backupType", "")
            val isFull = backupType == TYPE_FULL_BROWSER || root.has("browserSettings") || root.has("bookmarks")
            val version = root.optInt("version", 1)
            val timestamp = root.optLong("exportTimestamp", System.currentTimeMillis())

            val profiles = mutableListOf<NoraProfile>()
            val profilesArray = root.optJSONArray("profiles")
            if (profilesArray != null) {
                for (i in 0 until profilesArray.length()) {
                    profiles.add(deserializeProfile(profilesArray.getJSONObject(i)))
                }
            }

            val bookmarksCount = root.optJSONArray("bookmarks")?.length() ?: 0
            val historyCount = root.optJSONArray("history")?.length() ?: 0
            val hasBrowserSettings = root.has("browserSettings")
            val hasWidgetSettings = root.has("widgetSettings")

            Result.success(
                ParsedBackupPreview(
                    isFullBackup = isFull,
                    version = version,
                    timestamp = timestamp,
                    profiles = profiles,
                    bookmarksCount = bookmarksCount,
                    historyCount = historyCount,
                    hasBrowserSettings = hasBrowserSettings,
                    hasWidgetSettings = hasWidgetSettings,
                    rawJson = jsonString
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun deserializeFullBackup(jsonString: String): Result<FullBrowserBackup> {
        return try {
            val root = JSONObject(jsonString)
            val version = root.optInt("version", 1)
            val timestamp = root.optLong("exportTimestamp", System.currentTimeMillis())
            val app = root.optString("app", APP_IDENTIFIER)
            val activeId = root.optString("activeProfileId", "")

            val profiles = mutableListOf<NoraProfile>()
            val profilesArray = root.optJSONArray("profiles")
            if (profilesArray != null) {
                for (i in 0 until profilesArray.length()) {
                    profiles.add(deserializeProfile(profilesArray.getJSONObject(i)))
                }
            }

            // Widget
            var widgetSettings = FloatingWidgetSettings()
            root.optJSONObject("widgetSettings")?.let { w ->
                widgetSettings = FloatingWidgetSettings(
                    isVisible = w.optBoolean("isVisible", true),
                    sizeDp = w.optInt("sizeDp", 56),
                    opacityPercent = w.optInt("opacityPercent", 90),
                    offsetXPercent = w.optDouble("offsetXPercent", 0.85).toFloat(),
                    offsetYPercent = w.optDouble("offsetYPercent", 0.75).toFloat(),
                    showProxyBadge = w.optBoolean("showProxyBadge", true),
                    showProfileName = w.optBoolean("showProfileName", true)
                )
            }

            // Browser settings
            var browserSettings = BrowserGeneralSettings()
            root.optJSONObject("browserSettings")?.let { b ->
                browserSettings = BrowserGeneralSettings(
                    homeUrl = b.optString("homeUrl", "https://duckduckgo.com"),
                    searchEngine = b.optString("searchEngine", "DuckDuckGo"),
                    adBlockEnabled = b.optBoolean("adBlockEnabled", true),
                    javaScriptEnabled = b.optBoolean("javaScriptEnabled", true),
                    doNotTrack = b.optBoolean("doNotTrack", true),
                    cookiesEnabled = b.optBoolean("cookiesEnabled", true),
                    clearCookiesOnExit = b.optBoolean("clearCookiesOnExit", false)
                )
            }

            // Bookmarks
            val bookmarks = mutableListOf<BrowserBookmark>()
            root.optJSONArray("bookmarks")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val b = arr.getJSONObject(i)
                    bookmarks.add(
                        BrowserBookmark(
                            id = b.optString("id", UUID.randomUUID().toString()),
                            title = b.optString("title", ""),
                            url = b.optString("url", ""),
                            profileId = b.optString("profileId", ""),
                            createdAt = b.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // History
            val history = mutableListOf<BrowserHistoryItem>()
            root.optJSONArray("history")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val h = arr.getJSONObject(i)
                    history.add(
                        BrowserHistoryItem(
                            id = h.optString("id", UUID.randomUUID().toString()),
                            title = h.optString("title", ""),
                            url = h.optString("url", ""),
                            profileId = h.optString("profileId", ""),
                            visitedAt = h.optLong("visitedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            Result.success(
                FullBrowserBackup(
                    version = version,
                    exportTimestamp = timestamp,
                    app = app,
                    backupType = TYPE_FULL_BROWSER,
                    profiles = profiles,
                    activeProfileId = activeId,
                    widgetSettings = widgetSettings,
                    browserSettings = browserSettings,
                    bookmarks = bookmarks,
                    history = history
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun writeTextToUri(context: Context, uri: Uri, content: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                OutputStreamWriter(stream, Charsets.UTF_8).use { writer ->
                    writer.write(content)
                    writer.flush()
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun readTextFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun copyToClipboard(context: Context, text: String, label: String = "Nora Backup JSON") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }

    fun getFromClipboard(context: Context): String? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (clipboard.hasPrimaryClip()) {
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                return clip.getItemAt(0).text?.toString()
            }
        }
        return null
    }

    fun shareText(context: Context, text: String, title: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
