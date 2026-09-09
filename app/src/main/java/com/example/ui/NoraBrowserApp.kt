package com.example.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DesktopMac
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.model.BrowserTab
import com.example.model.NoraProfile
import com.example.repository.ProfileRepository
import com.example.ui.components.BookmarksHistorySheet
import com.example.ui.components.FloatingProfileSwitcherWidget
import com.example.ui.components.IsolatedNoraWebViewHost
import com.example.ui.components.MediaSnifferSheet
import com.example.ui.components.NoraWebViewController
import com.example.ui.components.PageToolsSheet
import com.example.ui.components.PrivacyShieldDialog
import com.example.ui.components.QuickProfileSwitcherSheet
import com.example.ui.components.SearchEnginePickerMenu
import com.example.ui.components.SslInfoDialog
import com.example.ui.components.TabsManagerSheet
import com.example.ui.components.ViewSourceDialog
import com.example.ui.screens.BackupRestoreScreen
import com.example.ui.screens.BrowserSettingsScreen
import com.example.ui.screens.FullProfilesManagerScreen
import com.example.ui.screens.ProfileAdvancedSettingsScreen
import com.example.util.AdBlockerEngine
import com.example.util.MediaSnifferHelper
import com.example.util.UrlHelper
import kotlinx.coroutines.launch
import java.util.UUID

enum class NoraScreen {
    BROWSER,
    PROFILES_MANAGER,
    PROFILE_SETTINGS,
    BROWSER_SETTINGS,
    BACKUP_RESTORE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoraBrowserApp() {
    val context = LocalContext.current
    val repository = remember { ProfileRepository(context) }
    val profiles by repository.profiles.collectAsState()
    val activeProfileId by repository.activeProfileId.collectAsState()
    val bookmarks by repository.bookmarks.collectAsState()
    val history by repository.history.collectAsState()
    val browserSettings by repository.browserSettings.collectAsState()
    val widgetSettings by repository.widgetSettings.collectAsState()

    val activeProfile = profiles.find { it.id == activeProfileId } ?: profiles.firstOrNull()

    var currentScreen by remember { mutableStateOf(NoraScreen.BROWSER) }
    var profileBeingEdited by remember { mutableStateOf<NoraProfile?>(null) }

    // Multi-tab Management
    var tabs by remember {
        mutableStateOf(
            listOf(
                BrowserTab(
                    id = UUID.randomUUID().toString(),
                    title = "DuckDuckGo",
                    url = "https://duckduckgo.com",
                    profileId = activeProfileId
                )
            )
        )
    }
    var activeTabId by remember { mutableStateOf(tabs.first().id) }

    val activeTab = tabs.find { it.id == activeTabId } ?: tabs.first()

    // Controller for current active WebView
    val webViewController = remember { NoraWebViewController() }

    // Address Bar State
    var urlInput by remember { mutableStateOf(activeTab.url) }

    // Dialogs and Sheets
    var showQuickSwitcherSheet by remember { mutableStateOf(false) }
    var showTabsSheet by remember { mutableStateOf(false) }
    var showBookmarksHistorySheet by remember { mutableStateOf(false) }
    var bookmarksHistoryInitialTab by remember { mutableIntStateOf(0) }
    var showShieldDialog by remember { mutableStateOf(false) }
    var showSslDialog by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showMediaSnifferSheet by remember { mutableStateOf(false) }
    var showPageToolsSheet by remember { mutableStateOf(false) }
    var showViewSourceDialog by remember { mutableStateOf(false) }
    var pageSourceContent by remember { mutableStateOf("") }
    var isSourceLoading by remember { mutableStateOf(false) }
    var showSearchEngineMenu by remember { mutableStateOf(false) }

    // Find in Page state
    var isFindInPageVisible by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }

    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Sync active tab url to urlInput when tab changes
    LaunchedEffect(activeTabId) {
        urlInput = activeTab.url
        webViewController.loadUrl(activeTab.url)
    }

    // Handle back button hierarchically
    BackHandler(enabled = true) {
        when {
            currentScreen != NoraScreen.BROWSER -> {
                when (currentScreen) {
                    NoraScreen.BACKUP_RESTORE -> currentScreen = NoraScreen.PROFILES_MANAGER
                    NoraScreen.PROFILE_SETTINGS -> currentScreen = NoraScreen.PROFILES_MANAGER
                    NoraScreen.BROWSER_SETTINGS -> currentScreen = NoraScreen.BROWSER
                    NoraScreen.PROFILES_MANAGER -> currentScreen = NoraScreen.BROWSER
                    NoraScreen.BROWSER -> {}
                }
            }
            isFindInPageVisible -> {
                isFindInPageVisible = false
                webViewController.webView?.clearMatches()
            }
            webViewController.canGoBack -> {
                webViewController.goBack()
            }
            tabs.size > 1 -> {
                // Close current tab and switch to previous
                val remaining = tabs.filter { it.id != activeTabId }
                tabs = remaining
                activeTabId = remaining.last().id
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // LAYER 1: The Core Browser (Always alive in composition, preventing reload when switching views!)
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            leadingIcon = {
                                Box {
                                    IconButton(
                                        onClick = {
                                            if (webViewController.isSecureConnection) {
                                                showSslDialog = true
                                            } else {
                                                showSearchEngineMenu = true
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (webViewController.isSecureConnection) Icons.Default.Lock else Icons.Default.Search,
                                            contentDescription = "Security Status & Search Engine",
                                            tint = if (webViewController.isSecureConnection) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    SearchEnginePickerMenu(
                                        expanded = showSearchEngineMenu,
                                        currentEngine = browserSettings.searchEngine,
                                        onSelectEngine = { selected ->
                                            repository.updateBrowserSettings(browserSettings.copy(searchEngine = selected))
                                            showSearchEngineMenu = false
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("محرك البحث الافتراضي: $selected")
                                            }
                                        },
                                        onDismiss = { showSearchEngineMenu = false }
                                    )
                                }
                            },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val isCurrentBookmarked = bookmarks.any { it.url == activeTab.url }
                                    IconButton(
                                        onClick = {
                                            repository.toggleBookmark(
                                                title = activeTab.title.ifBlank { urlInput },
                                                url = activeTab.url
                                            )
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(
                                                    if (!isCurrentBookmarked) "تمت الإضافة إلى العلامات المرجعية" else "تمت إزالة العلامة المرجعية"
                                                )
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isCurrentBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                            contentDescription = "Bookmark",
                                            tint = if (isCurrentBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            if (webViewController.isLoading) {
                                                webViewController.stopLoading()
                                            } else {
                                                webViewController.reload()
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (webViewController.isLoading) Icons.Default.Close else Icons.Default.Refresh,
                                            contentDescription = "Reload or Stop",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    val resolvedUrl = UrlHelper.resolveInputToUrl(urlInput, browserSettings.searchEngine)
                                    urlInput = resolvedUrl
                                    tabs = tabs.map {
                                        if (it.id == activeTabId) it.copy(url = resolvedUrl) else it
                                    }
                                    webViewController.loadUrl(resolvedUrl)
                                    keyboardController?.hide()
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("url_input_bar")
                        )
                    },
                    actions = {
                        // Profile Avatar / Indicator Button
                        IconButton(
                            onClick = { showQuickSwitcherSheet = true },
                            modifier = Modifier.testTag("header_profile_button")
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
                            } ?: Icon(Icons.Default.ManageAccounts, contentDescription = null)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                // High-End Bottom Navigation Bar
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier.height(56.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back
                        IconButton(
                            onClick = { webViewController.goBack() },
                            enabled = webViewController.canGoBack
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = if (webViewController.canGoBack) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }

                        // Forward
                        IconButton(
                            onClick = { webViewController.goForward() },
                            enabled = webViewController.canGoForward
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Forward",
                                tint = if (webViewController.canGoForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }

                        // Media Sniffer (XBrowser-style video/audio sniffer)
                        val sniffedList = webViewController.sniffedMedia
                        IconButton(onClick = { showMediaSnifferSheet = true }) {
                            BadgedBox(
                                badge = {
                                    if (sniffedList.isNotEmpty()) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ) {
                                            Text("${sniffedList.size}", fontSize = 9.sp)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = "Sniffed Media",
                                    tint = if (sniffedList.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Page Tools & Scripts (Dark mode, Reader mode, Unlock copy, Zoom)
                        IconButton(onClick = { showPageToolsSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Page Tools",
                                tint = if (webViewController.isDarkModeInjected || webViewController.isReaderModeInjected)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Privacy Shield
                        val blockedCount by AdBlockerEngine.blockedTrackersCount.collectAsState()
                        IconButton(onClick = { showShieldDialog = true }) {
                            BadgedBox(
                                badge = {
                                    if (blockedCount > 0) {
                                        Badge(
                                            containerColor = Color(0xFF10B981),
                                            contentColor = Color.White
                                        ) {
                                            Text(if (blockedCount > 99) "99+" else "$blockedCount", fontSize = 9.sp)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Shield",
                                    tint = if (browserSettings.adBlockEnabled) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Tabs Manager Button with active count badge
                        IconButton(onClick = { showTabsSheet = true }) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (activeTab.isIncognito) Color(0xFF374151)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                            ) {
                                Text(
                                    text = "${tabs.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeTab.isIncognito) Color(0xFFF3F4F6) else MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // More Options Menu
                        Box {
                            IconButton(onClick = { showOptionsMenu = true }) {
                                Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More Options")
                            }

                            DropdownMenu(
                                expanded = showOptionsMenu,
                                onDismissRequest = { showOptionsMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("علامة تبويب جديدة (New Tab)") },
                                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                                    onClick = {
                                        val newTab = BrowserTab(
                                            id = UUID.randomUUID().toString(),
                                            title = "DuckDuckGo",
                                            url = browserSettings.homeUrl,
                                            profileId = activeProfileId,
                                            isIncognito = false
                                        )
                                        tabs = tabs + newTab
                                        activeTabId = newTab.id
                                        showOptionsMenu = false
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("علامة تبويب خفية (Incognito)") },
                                    leadingIcon = { Icon(Icons.Default.VisibilityOff, contentDescription = null) },
                                    onClick = {
                                        val incognitoTab = BrowserTab(
                                            id = UUID.randomUUID().toString(),
                                            title = "تصفح خفي",
                                            url = browserSettings.homeUrl,
                                            profileId = activeProfileId,
                                            isIncognito = true
                                        )
                                        tabs = tabs + incognitoTab
                                        activeTabId = incognitoTab.id
                                        showOptionsMenu = false
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("أدوات الصفحة (Page Tools)") },
                                    leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null) },
                                    onClick = {
                                        showOptionsMenu = false
                                        showPageToolsSheet = true
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("مقتنص الوسائط (${sniffedList.size})") },
                                    leadingIcon = { Icon(Icons.Default.Movie, contentDescription = null) },
                                    onClick = {
                                        showOptionsMenu = false
                                        showMediaSnifferSheet = true
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("كود المصدر (View Source)") },
                                    leadingIcon = { Icon(Icons.Default.Code, contentDescription = null) },
                                    onClick = {
                                        showOptionsMenu = false
                                        showViewSourceDialog = true
                                        isSourceLoading = true
                                        webViewController.getPageSource { src ->
                                            pageSourceContent = src
                                            isSourceLoading = false
                                        }
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("العلامات المرجعية (Bookmarks)") },
                                    leadingIcon = { Icon(Icons.Default.Bookmark, contentDescription = null) },
                                    onClick = {
                                        bookmarksHistoryInitialTab = 0
                                        showBookmarksHistorySheet = true
                                        showOptionsMenu = false
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("سجل التصفح (History)") },
                                    leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                                    onClick = {
                                        bookmarksHistoryInitialTab = 1
                                        showBookmarksHistorySheet = true
                                        showOptionsMenu = false
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("مشاركة الرابط (Share)") },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                    onClick = {
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, activeTab.url)
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "مشاركة الرابط عبر"))
                                        showOptionsMenu = false
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (webViewController.isDesktopMode) "وضع الهاتف (Mobile Site)" else "موقع الكمبيوتر (Desktop Site)"
                                        )
                                    },
                                    leadingIcon = { Icon(Icons.Default.DesktopMac, contentDescription = null) },
                                    onClick = {
                                        webViewController.isDesktopMode = !webViewController.isDesktopMode
                                        showOptionsMenu = false
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("محرك البحث: ${browserSettings.searchEngine}") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                    onClick = {
                                        showOptionsMenu = false
                                        showSearchEngineMenu = true
                                    }
                                )

                                HorizontalDivider()

                                DropdownMenuItem(
                                    text = { Text("إدارة البروفايلات (Profiles)") },
                                    leadingIcon = { Icon(Icons.Default.ManageAccounts, contentDescription = null) },
                                    onClick = {
                                        currentScreen = NoraScreen.PROFILES_MANAGER
                                        showOptionsMenu = false
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("إعدادات المتصفح (Settings)") },
                                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                    onClick = {
                                        currentScreen = NoraScreen.BROWSER_SETTINGS
                                        showOptionsMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
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
                        url = activeTab.url,
                        controller = webViewController,
                        adBlockEnabled = browserSettings.adBlockEnabled && profile.privacy.blockTrackers,
                        onUrlChanged = { newUrl ->
                            urlInput = newUrl
                            tabs = tabs.map {
                                if (it.id == activeTabId) it.copy(url = newUrl) else it
                            }
                            if (!activeTab.isIncognito) {
                                repository.addHistory(
                                    title = webViewController.currentTitle.ifBlank { UrlHelper.extractDomain(newUrl) },
                                    url = newUrl,
                                    profileId = profile.id
                                )
                            }
                        },
                        onTitleChanged = { newTitle ->
                            tabs = tabs.map {
                                if (it.id == activeTabId) it.copy(title = newTitle) else it
                            }
                        }
                    )

                    // Draggable Floating Profile Switcher Widget
                    FloatingProfileSwitcherWidget(
                        activeProfile = profile,
                        settings = widgetSettings,
                        onClick = { showQuickSwitcherSheet = true },
                        onPositionChanged = { nx, ny ->
                            repository.updateWidgetSettings(
                                widgetSettings.copy(offsetXPercent = nx, offsetYPercent = ny)
                            )
                        }
                    )
                }

                // Find in Page Floating Bar
                AnimatedVisibility(
                    visible = isFindInPageVisible,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    Surface(
                        tonalElevation = 8.dp,
                        shadowElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = findQuery,
                                onValueChange = {
                                    findQuery = it
                                    webViewController.webView?.findAllAsync(it)
                                },
                                placeholder = { Text("بحث في الصفحة...") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp)
                            )
                            IconButton(onClick = { webViewController.webView?.findNext(false) }) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous")
                            }
                            IconButton(onClick = { webViewController.webView?.findNext(true) }) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next")
                            }
                            IconButton(onClick = {
                                isFindInPageVisible = false
                                webViewController.webView?.clearMatches()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }
                    }
                }

                // Quick Profile Switcher Sheet
                if (showQuickSwitcherSheet && activeProfile != null) {
                    val siteDomain = UrlHelper.extractDomain(activeTab.url)
                    QuickProfileSwitcherSheet(
                        currentProfileId = activeProfile.id,
                        currentSite = siteDomain,
                        profiles = profiles,
                        onSelectProfile = { p ->
                            repository.setActiveProfile(p.id)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("تم التبديل إلى: ${p.name} 🔄 جاري التحديث بالعزل")
                            }
                        },
                        onOpenManager = {
                            currentScreen = NoraScreen.PROFILES_MANAGER
                        },
                        onDismiss = { showQuickSwitcherSheet = false }
                    )
                }

                // Tabs Manager Sheet
                if (showTabsSheet) {
                    TabsManagerSheet(
                        tabs = tabs,
                        activeTabId = activeTabId,
                        onSelectTab = { selectedTab ->
                            activeTabId = selectedTab.id
                        },
                        onCloseTab = { closeId ->
                            if (tabs.size > 1) {
                                val remaining = tabs.filter { it.id != closeId }
                                tabs = remaining
                                if (activeTabId == closeId) {
                                    activeTabId = remaining.last().id
                                }
                            }
                        },
                        onNewTab = {
                            val newTab = BrowserTab(
                                id = UUID.randomUUID().toString(),
                                title = "DuckDuckGo",
                                url = browserSettings.homeUrl,
                                profileId = activeProfileId,
                                isIncognito = false
                            )
                            tabs = tabs + newTab
                            activeTabId = newTab.id
                        },
                        onNewIncognitoTab = {
                            val incognitoTab = BrowserTab(
                                id = UUID.randomUUID().toString(),
                                title = "تصفح خفي",
                                url = browserSettings.homeUrl,
                                profileId = activeProfileId,
                                isIncognito = true
                            )
                            tabs = tabs + incognitoTab
                            activeTabId = incognitoTab.id
                        },
                        onDismiss = { showTabsSheet = false }
                    )
                }

                // Media Sniffer Sheet (XBrowser flagship audio/video grabber)
                if (showMediaSnifferSheet) {
                    MediaSnifferSheet(
                        mediaList = webViewController.sniffedMedia,
                        onDismiss = { showMediaSnifferSheet = false }
                    )
                }

                // Page Tools Sheet (Dark mode, Reader mode, Unlock copy, Zoom, Source)
                if (showPageToolsSheet) {
                    PageToolsSheet(
                        isDarkModeActive = webViewController.isDarkModeInjected,
                        isReaderModeActive = webViewController.isReaderModeInjected,
                        isDesktopMode = webViewController.isDesktopMode,
                        currentTextZoom = webViewController.textZoom,
                        onToggleDarkMode = { webViewController.toggleDarkMode() },
                        onToggleReaderMode = { webViewController.toggleReaderMode() },
                        onToggleDesktopMode = { webViewController.isDesktopMode = !webViewController.isDesktopMode },
                        onUnlockCopy = {
                            webViewController.unlockCopyRestrictions()
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("تم فك قيود التحديد والنسخ بنجاح")
                            }
                        },
                        onViewSource = {
                            showPageToolsSheet = false
                            showViewSourceDialog = true
                            isSourceLoading = true
                            webViewController.getPageSource { src ->
                                pageSourceContent = src
                                isSourceLoading = false
                            }
                        },
                        onFindInPage = {
                            showPageToolsSheet = false
                            isFindInPageVisible = true
                        },
                        onTextZoomChanged = { newZoom ->
                            webViewController.setTextZoomLevel(newZoom)
                        },
                        onDismiss = { showPageToolsSheet = false }
                    )
                }

                // View HTML Source Dialog
                if (showViewSourceDialog) {
                    ViewSourceDialog(
                        url = activeTab.url,
                        htmlContent = pageSourceContent,
                        isLoading = isSourceLoading,
                        onDismiss = { showViewSourceDialog = false }
                    )
                }

                // Bookmarks & History Sheet
                if (showBookmarksHistorySheet) {
                    BookmarksHistorySheet(
                        initialTab = bookmarksHistoryInitialTab,
                        bookmarks = bookmarks,
                        history = history,
                        onSelectUrl = { targetUrl ->
                            urlInput = targetUrl
                            tabs = tabs.map {
                                if (it.id == activeTabId) it.copy(url = targetUrl) else it
                            }
                            webViewController.loadUrl(targetUrl)
                        },
                        onDeleteBookmark = { bId -> repository.deleteBookmark(bId) },
                        onClearHistory = { repository.clearHistory() },
                        onDismiss = { showBookmarksHistorySheet = false }
                    )
                }

                // Privacy Shield Dialog
                if (showShieldDialog && activeProfile != null) {
                    PrivacyShieldDialog(
                        profile = activeProfile,
                        onClearProfileCookies = {
                            repository.clearProfileData(activeProfile.id)
                            webViewController.reload()
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("تم مسح كوكيز وجلسة البروفايل")
                            }
                        },
                        onDismiss = { showShieldDialog = false }
                    )
                }

                // SSL Info Dialog
                if (showSslDialog) {
                    SslInfoDialog(
                        url = activeTab.url,
                        onDismiss = { showSslDialog = false }
                    )
                }
            }
        }

        // LAYER 2: Overlay Screens (Rendered on top without destroying the WebView underneath!)
        AnimatedVisibility(
            visible = currentScreen == NoraScreen.PROFILES_MANAGER,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            FullProfilesManagerScreen(
                profiles = profiles,
                activeProfileId = activeProfileId,
                onSelectProfile = { p ->
                    val wasDifferent = activeProfileId != p.id
                    repository.setActiveProfile(p.id)
                    currentScreen = NoraScreen.BROWSER
                    if (wasDifferent) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("تم التبديل إلى: ${p.name} 🔄 جاري التحديث بالعزل")
                        }
                    }
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
                onDeleteProfile = { id -> repository.deleteProfile(id) },
                onToggleProxy = { id -> repository.toggleProxy(id) },
                onCreateProfile = { p -> repository.saveProfile(p) },
                onOpenBackupRestore = { currentScreen = NoraScreen.BACKUP_RESTORE },
                onOpenBrowserSettings = { currentScreen = NoraScreen.BROWSER_SETTINGS },
                onBack = { currentScreen = NoraScreen.BROWSER }
            )
        }

        AnimatedVisibility(
            visible = currentScreen == NoraScreen.PROFILE_SETTINGS,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            profileBeingEdited?.let { profile ->
                ProfileAdvancedSettingsScreen(
                    profile = profile,
                    onSave = { updated ->
                        repository.saveProfile(updated)
                        currentScreen = NoraScreen.PROFILES_MANAGER
                    },
                    onClearProfileData = { profileId ->
                        repository.clearProfileData(profileId)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("تم مسح بيانات وتخزين هذا البروفايل")
                        }
                    },
                    onBack = { currentScreen = NoraScreen.PROFILES_MANAGER }
                )
            }
        }

        AnimatedVisibility(
            visible = currentScreen == NoraScreen.BROWSER_SETTINGS,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            BrowserSettingsScreen(
                repository = repository,
                onOpenBackupRestore = { currentScreen = NoraScreen.BACKUP_RESTORE },
                onBack = { currentScreen = NoraScreen.BROWSER }
            )
        }

        AnimatedVisibility(
            visible = currentScreen == NoraScreen.BACKUP_RESTORE,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            BackupRestoreScreen(
                repository = repository,
                onBack = { currentScreen = NoraScreen.PROFILES_MANAGER }
            )
        }
    }
}
