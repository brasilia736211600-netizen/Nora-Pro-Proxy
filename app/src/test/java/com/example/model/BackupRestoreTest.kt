package com.example.model

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.repository.BackupManager
import com.example.repository.ProfileRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BackupRestoreTest {

    private val sampleProfile = NoraProfile(
        id = "test_profile_1",
        name = "Social Account Alpha",
        colorHex = "#10B981",
        site = "x.com",
        proxy = ProxyConfig(
            enabled = true,
            protocol = ProxyProtocol.SOCKS5,
            host = "10.0.0.1",
            port = 1080,
            username = "user1"
        ),
        userAgent = UserAgentConfig(
            mode = UAMode.CUSTOM,
            customUA = "Mozilla/5.0 (CustomUA) Test"
        ),
        time = TimeConfig(
            mode = TimeMode.MANUAL,
            timezoneId = "Europe/London"
        )
    )

    @Test
    fun testProfileSerializationAndDeserialization() {
        val jsonObj = BackupManager.serializeProfile(sampleProfile)
        val deserialized = BackupManager.deserializeProfile(jsonObj)

        assertEquals(sampleProfile.id, deserialized.id)
        assertEquals(sampleProfile.name, deserialized.name)
        assertEquals(sampleProfile.colorHex, deserialized.colorHex)
        assertEquals(sampleProfile.site, deserialized.site)
        assertEquals(sampleProfile.proxy.enabled, deserialized.proxy.enabled)
        assertEquals(sampleProfile.proxy.protocol, deserialized.proxy.protocol)
        assertEquals(sampleProfile.proxy.host, deserialized.proxy.host)
        assertEquals(sampleProfile.proxy.port, deserialized.proxy.port)
        assertEquals(sampleProfile.proxy.username, deserialized.proxy.username)
        assertEquals(sampleProfile.userAgent.mode, deserialized.userAgent.mode)
        assertEquals(sampleProfile.userAgent.customUA, deserialized.userAgent.customUA)
        assertEquals(sampleProfile.time.mode, deserialized.time.mode)
        assertEquals(sampleProfile.time.timezoneId, deserialized.time.timezoneId)
    }

    @Test
    fun testExportProfilesJsonAndParse() {
        val profiles = listOf(
            sampleProfile,
            NoraProfile(id = "test_2", name = "Second Profile", site = "reddit.com")
        )

        val json = BackupManager.exportProfilesJson(profiles)
        assertTrue(json.contains("PROFILES_ONLY"))
        assertTrue(json.contains("Social Account Alpha"))
        assertTrue(json.contains("Second Profile"))

        val parseResult = BackupManager.parseBackup(json)
        assertTrue(parseResult.isSuccess)
        val preview = parseResult.getOrNull()
        assertNotNull(preview)
        assertFalse(preview!!.isFullBackup)
        assertEquals(2, preview.profiles.size)
        assertEquals("Social Account Alpha", preview.profiles[0].name)
    }

    @Test
    fun testExportFullBrowserJsonAndDeserialize() {
        val fullBackup = FullBrowserBackup(
            profiles = listOf(sampleProfile),
            activeProfileId = sampleProfile.id,
            widgetSettings = FloatingWidgetSettings(isVisible = true, sizeDp = 68),
            browserSettings = BrowserGeneralSettings(searchEngine = "Google", adBlockEnabled = true),
            bookmarks = listOf(
                BrowserBookmark(id = "bm_1", title = "Search", url = "https://duckduckgo.com")
            ),
            history = listOf(
                BrowserHistoryItem(id = "h_1", title = "Site", url = "https://x.com")
            )
        )

        val json = BackupManager.exportFullBrowserJson(fullBackup)
        assertTrue(json.contains("FULL_BROWSER"))
        assertTrue(json.contains("bookmarks"))
        assertTrue(json.contains("history"))

        val parseResult = BackupManager.deserializeFullBackup(json)
        assertTrue(parseResult.isSuccess)
        val restored = parseResult.getOrNull()
        assertNotNull(restored)
        assertEquals(1, restored!!.profiles.size)
        assertEquals(sampleProfile.id, restored.activeProfileId)
        assertEquals(68, restored.widgetSettings.sizeDp)
        assertEquals("Google", restored.browserSettings.searchEngine)
        assertEquals(1, restored.bookmarks.size)
        assertEquals("https://duckduckgo.com", restored.bookmarks[0].url)
        assertEquals(1, restored.history.size)
    }

    @Test
    fun testProfileConflictResolutions() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = ProfileRepository(context)

        val existingProfile = repo.profiles.value.first()
        val duplicateIncoming = existingProfile.copy(name = "Updated Remote Name")

        // 1. SKIP
        val skipResult = repo.importProfiles(listOf(duplicateIncoming), ProfileConflictResolution.SKIP)
        assertTrue(skipResult.success)
        assertEquals(1, skipResult.skippedCount)
        assertEquals(0, skipResult.overwrittenCount)

        // 2. OVERWRITE
        val overwriteResult = repo.importProfiles(listOf(duplicateIncoming), ProfileConflictResolution.OVERWRITE)
        assertTrue(overwriteResult.success)
        assertEquals(1, overwriteResult.overwrittenCount)
        val updated = repo.profiles.value.find { it.id == existingProfile.id }
        assertEquals("Updated Remote Name", updated?.name)

        // 3. KEEP_BOTH
        val keepBothResult = repo.importProfiles(listOf(duplicateIncoming), ProfileConflictResolution.KEEP_BOTH)
        assertTrue(keepBothResult.success)
        assertEquals(1, keepBothResult.renamedCount)
        assertTrue(repo.profiles.value.any { it.name.contains("(Imported)") })
    }

    @Test
    fun testSelectiveProfilesExport() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = ProfileRepository(context)

        val allProfiles = repo.profiles.value
        assertTrue(allProfiles.size >= 2)

        val selectedId = allProfiles.first().id
        val exportedJson = repo.exportSelectedProfilesJson(setOf(selectedId))

        val parseRes = BackupManager.parseBackup(exportedJson)
        assertTrue(parseRes.isSuccess)
        val preview = parseRes.getOrNull()!!
        assertEquals(1, preview.profiles.size)
        assertEquals(selectedId, preview.profiles[0].id)
    }

    @Test
    fun testFullBrowserRestoreAndMerge() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = ProfileRepository(context)

        val newBookmark = BrowserBookmark(
            id = UUID.randomUUID().toString(),
            title = "Special Bookmark",
            url = "https://special.example.com"
        )
        val backup = FullBrowserBackup(
            profiles = listOf(
                NoraProfile(id = "new_p_1", name = "Merged Profile", site = "example.com")
            ),
            bookmarks = listOf(newBookmark)
        )

        val mergeResult = repo.restoreFullBrowserBackup(backup, mergeMode = true)
        assertTrue(mergeResult.success)
        assertTrue(repo.profiles.value.any { it.id == "new_p_1" })
        assertTrue(repo.bookmarks.value.any { it.url == "https://special.example.com" })
    }
}
