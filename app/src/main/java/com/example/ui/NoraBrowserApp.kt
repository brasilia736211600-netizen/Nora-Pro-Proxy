package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FloatingWidgetSettings
import com.example.model.NoraProfile
import com.example.repository.ProfileRepository
import com.example.ui.components.FloatingProfileSwitcherWidget
import com.example.ui.components.IsolatedNoraWebViewHost
import com.example.ui.components.QuickProfileSwitcherSheet
import com.example.ui.screens.BackupRestoreScreen
import com.example.ui.screens.FullProfilesManagerScreen
import com.example.ui.screens.ProfileAdvancedSettingsScreen

enum class NoraScreen {
    BROWSER,
    PROFILES_MANAGER,
    PROFILE_SETTINGS,
    BACKUP_RESTORE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoraBrowserApp() {
    val context = LocalContext.current
    val repository = remember { ProfileRepository(context) }
    val profiles by repository.profiles.collectAsState()
    val activeProfileId by repository.activeProfileId.collectAsState()

    val activeProfile = profiles.find { it.id == activeProfileId } ?: profiles.firstOrNull()

    var currentScreen by remember { mutableStateOf(NoraScreen.BROWSER) }
    var profileBeingEdited by remember { mutableStateOf<NoraProfile?>(null) }

    // Browser navigation state
    var currentUrl by remember { mutableStateOf("https://duckduckgo.com") }
    var urlInput by remember { mutableStateOf("https://duckduckgo.com") }
    var currentTitle by remember { mutableStateOf("DuckDuckGo") }

    // Floating Switcher settings
    var widgetSettings by remember { mutableStateOf(FloatingWidgetSettings()) }
    var showQuickSwitcherSheet by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current

    BackHandler(enabled = currentScreen != NoraScreen.BROWSER) {
        when (currentScreen) {
            NoraScreen.BACKUP_RESTORE -> currentScreen = NoraScreen.PROFILES_MANAGER
            NoraScreen.PROFILE_SETTINGS -> currentScreen = NoraScreen.PROFILES_MANAGER
            NoraScreen.PROFILES_MANAGER -> currentScreen = NoraScreen.BROWSER
            NoraScreen.BROWSER -> {}
        }
    }

    when (currentScreen) {
        NoraScreen.PROFILES_MANAGER -> {
            FullProfilesManagerScreen(
                profiles = profiles,
                activeProfileId = activeProfileId,
                onSelectProfile = { p ->
                    repository.setActiveProfile(p.id)
                    currentScreen = NoraScreen.BROWSER
                },
                onEditProfile = { p ->
                    profileBeingEdited = p
                    currentScreen = NoraScreen.PROFILE_SETTINGS
                },
                onDuplicateProfile = { id ->
                    val copy = repository.duplicateProfile(id)
                    copy?.let {
                        profileBeingEdited = it
                        currentScreen = NoraScreen.PROFILE_SETTINGS
                    }
                },
                onDeleteProfile = { id ->
                    repository.deleteProfile(id)
                },
                onToggleProxy = { id ->
                    repository.toggleProxy(id)
                },
                onCreateProfile = { p ->
                    repository.saveProfile(p)
                },
                onOpenBackupRestore = {
                    currentScreen = NoraScreen.BACKUP_RESTORE
                },
                onBack = { currentScreen = NoraScreen.BROWSER }
            )
        }
        NoraScreen.BACKUP_RESTORE -> {
            BackupRestoreScreen(
                repository = repository,
                onBack = { currentScreen = NoraScreen.PROFILES_MANAGER }
            )
        }
        NoraScreen.PROFILE_SETTINGS -> {
            profileBeingEdited?.let { profile ->
                ProfileAdvancedSettingsScreen(
                    profile = profile,
                    onSave = { updated ->
                        repository.saveProfile(updated)
                        currentScreen = NoraScreen.PROFILES_MANAGER
                    },
                    onBack = { currentScreen = NoraScreen.PROFILES_MANAGER }
                )
            } ?: run {
                currentScreen = NoraScreen.PROFILES_MANAGER
            }
        }
        NoraScreen.BROWSER -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            OutlinedTextField(
                                value = urlInput,
                                onValueChange = { urlInput = it },
                                singleLine = true,
                                shape = RoundedCornerShape(24.dp),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                keyboardActions = KeyboardActions(
                                    onGo = {
                                        var formatted = urlInput.trim()
                                        if (!formatted.startsWith("http://") && !formatted.startsWith("https://")) {
                                            formatted = if (formatted.contains(".") && !formatted.contains(" ")) {
                                                "https://$formatted"
                                            } else {
                                                "https://duckduckgo.com/?q=${formatted.replace(" ", "+")}"
                                            }
                                        }
                                        currentUrl = formatted
                                        keyboardController?.hide()
                                    }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("url_input_bar")
                            )
                        },
                        actions = {
                            IconButton(
                                onClick = { currentScreen = NoraScreen.BACKUP_RESTORE },
                                modifier = Modifier.testTag("header_backup_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ImportExport,
                                    contentDescription = "Backup & Restore"
                                )
                            }
                            IconButton(
                                onClick = { currentScreen = NoraScreen.PROFILES_MANAGER },
                                modifier = Modifier.testTag("header_profiles_button")
                            ) {
                                activeProfile?.let { ap ->
                                    val color = try {
                                        Color(android.graphics.Color.parseColor(ap.colorHex))
                                    } catch (_: Exception) {
                                        MaterialTheme.colorScheme.primary
                                    }
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    ) {
                                        Text(
                                            text = ap.name.take(1).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                } ?: Icon(
                                    imageVector = Icons.Default.ManageAccounts,
                                    contentDescription = "Profiles"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    activeProfile?.let { profile ->
                        IsolatedNoraWebViewHost(
                            profile = profile,
                            url = currentUrl,
                            onUrlChanged = { newUrl ->
                                currentUrl = newUrl
                                urlInput = newUrl
                            },
                            onTitleChanged = { currentTitle = it }
                        )

                        // Draggable Floating Profile Switcher Widget
                        FloatingProfileSwitcherWidget(
                            activeProfile = profile,
                            settings = widgetSettings,
                            onClick = { showQuickSwitcherSheet = true },
                            onPositionChanged = { nx, ny ->
                                widgetSettings = widgetSettings.copy(offsetXPercent = nx, offsetYPercent = ny)
                            }
                        )
                    }

                    // Quick Switcher Bottom Sheet
                    if (showQuickSwitcherSheet && activeProfile != null) {
                        val siteDomain = try {
                            val host = java.net.URI(currentUrl).host
                            host?.removePrefix("www.") ?: ""
                        } catch (_: Exception) { "" }

                        QuickProfileSwitcherSheet(
                            currentProfileId = activeProfile.id,
                            currentSite = siteDomain,
                            profiles = profiles,
                            onSelectProfile = { p ->
                                repository.setActiveProfile(p.id)
                            },
                            onOpenManager = {
                                currentScreen = NoraScreen.PROFILES_MANAGER
                            },
                            onDismiss = { showQuickSwitcherSheet = false }
                        )
                    }
                }
            }
        }
    }
}
