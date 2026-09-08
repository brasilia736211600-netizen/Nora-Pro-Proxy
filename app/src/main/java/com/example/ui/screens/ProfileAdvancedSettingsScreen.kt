package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NoraProfile
import com.example.model.ProxyConfig
import com.example.model.ProxyProtocol
import com.example.model.TimeConfig
import com.example.model.TimeMode
import com.example.model.TimezoneHelper
import com.example.model.UAMode
import com.example.model.UserAgentConfig
import com.example.model.UserAgentLibrary
import com.example.network.ProxyTestResult
import com.example.network.ProxyTester
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileAdvancedSettingsScreen(
    profile: NoraProfile,
    onSave: (NoraProfile) -> Unit,
    onBack: () -> Unit
) {
    var editedProfile by remember { mutableStateOf(profile) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = General, 1 = Proxy (1a), 2 = User Agent (1b), 3 = Time / Clock (1c)
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = editedProfile.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Profile Configuration",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = { onSave(editedProfile) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("save_profile_settings_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SecondaryTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Profile") },
                    icon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Proxy") },
                    icon = { Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("User Agent") },
                    icon = { Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Time / Clock") },
                    icon = { Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    0 -> GeneralSettingsTab(
                        profile = editedProfile,
                        onUpdate = { editedProfile = it }
                    )
                    1 -> ProxySettingsTab(
                        proxy = editedProfile.proxy,
                        onUpdate = { editedProfile = editedProfile.copy(proxy = it) }
                    )
                    2 -> UserAgentSettingsTab(
                        ua = editedProfile.userAgent,
                        onUpdate = { editedProfile = editedProfile.copy(userAgent = it) }
                    )
                    3 -> TimeSettingsTab(
                        time = editedProfile.time,
                        proxy = editedProfile.proxy,
                        onUpdate = { editedProfile = editedProfile.copy(time = it) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GeneralSettingsTab(
    profile: NoraProfile,
    onUpdate: (NoraProfile) -> Unit
) {
    val colors = listOf("#3B82F6", "#0EA5E9", "#6366F1", "#8B5CF6", "#EC4899", "#F97316", "#10B981", "#64748B")

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "General Identity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = profile.name,
                onValueChange = { onUpdate(profile.copy(name = it)) },
                label = { Text("Profile Name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_profile_name")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = profile.site,
                onValueChange = { onUpdate(profile.copy(site = it)) },
                label = { Text("Bound Website / Domain (Optional)") },
                placeholder = { Text("e.g. twitter.com, reddit.com") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_profile_site")
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Profile Accent Color",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                colors.forEach { hex ->
                    val color = Color(android.graphics.Color.parseColor(hex))
                    val isSelected = profile.colorHex.equals(hex, ignoreCase = true)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { onUpdate(profile.copy(colorHex = hex)) }
                    ) {
                        if (isSelected) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProxySettingsTab(
    proxy: ProxyConfig,
    onUpdate: (ProxyConfig) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isChecking by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<ProxyTestResult?>(null) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Enable Profile Proxy",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Routes all WebView requests via dedicated proxy",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = proxy.enabled,
                    onCheckedChange = { onUpdate(proxy.copy(enabled = it)) },
                    modifier = Modifier.testTag("switch_proxy_enabled")
                )
            }

            AnimatedVisibility(visible = proxy.enabled) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        text = "Protocol",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(ProxyProtocol.HTTP, ProxyProtocol.HTTPS, ProxyProtocol.SOCKS5).forEach { proto ->
                            FilterChip(
                                selected = proxy.protocol == proto,
                                onClick = { onUpdate(proxy.copy(protocol = proto)) },
                                label = { Text(proto.scheme.uppercase()) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = proxy.host,
                            onValueChange = { onUpdate(proxy.copy(host = it)) },
                            label = { Text("Host / IP") },
                            placeholder = { Text("127.0.0.1") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(2f)
                                .testTag("input_proxy_host")
                        )

                        OutlinedTextField(
                            value = if (proxy.port == 0) "" else proxy.port.toString(),
                            onValueChange = {
                                val p = it.toIntOrNull() ?: 0
                                onUpdate(proxy.copy(port = p))
                            },
                            label = { Text("Port") },
                            placeholder = { Text("8080") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_proxy_port")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = proxy.username,
                        onValueChange = { onUpdate(proxy.copy(username = it)) },
                        label = { Text("Username (Optional)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_proxy_username")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = proxy.password,
                        onValueChange = { onUpdate(proxy.copy(password = it)) },
                        label = { Text("Password (Optional)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_proxy_password")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = proxy.pacUrl,
                        onValueChange = { onUpdate(proxy.copy(pacUrl = it)) },
                        label = { Text("PAC Script URL (Optional)") },
                        placeholder = { Text("https://example.com/proxy.pac") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_proxy_pac")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Proxy Tester Button
                    Button(
                        onClick = {
                            isChecking = true
                            testResult = null
                            coroutineScope.launch {
                                val res = ProxyTester.checkProxy(proxy)
                                testResult = res
                                isChecking = false
                            }
                        },
                        enabled = !isChecking && proxy.isValid(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_check_proxy")
                    ) {
                        if (isChecking) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(end = 8.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text("Testing Connection...")
                        } else {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Check Proxy Connection")
                        }
                    }

                    // Test Result Feedback
                    testResult?.let { res ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when (res) {
                                is ProxyTestResult.Success -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                is ProxyTestResult.Failure -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Icon(
                                    imageVector = when (res) {
                                        is ProxyTestResult.Success -> Icons.Default.CheckCircle
                                        is ProxyTestResult.Failure -> Icons.Default.Error
                                    },
                                    contentDescription = null,
                                    tint = when (res) {
                                        is ProxyTestResult.Success -> MaterialTheme.colorScheme.primary
                                        is ProxyTestResult.Failure -> MaterialTheme.colorScheme.error
                                    }
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = when (res) {
                                            is ProxyTestResult.Success -> "Connection Successful (${res.latencyMs} ms)"
                                            is ProxyTestResult.Failure -> "Connection Failed"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = when (res) {
                                            is ProxyTestResult.Success -> "HTTP ${res.responseCode} • Server Date: ${res.serverDate ?: "OK"}"
                                            is ProxyTestResult.Failure -> res.errorMessage
                                        },
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Zero-Leak Notice: If enabled, all web traffic fails closed if the proxy is unreachable. Direct fallback is strictly disabled.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserAgentSettingsTab(
    ua: UserAgentConfig,
    onUpdate: (UserAgentConfig) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "User Agent Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                UAMode.values().forEach { mode ->
                    FilterChip(
                        selected = ua.mode == mode,
                        onClick = { onUpdate(ua.copy(mode = mode)) },
                        label = { Text(mode.label) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (ua.mode) {
                UAMode.DEFAULT -> {
                    Text(
                        text = "Using system Android WebView default User-Agent string.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                UAMode.PRESET -> {
                    Text(
                        text = "Curated Library Picker",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // OS Picker
                    var osExpanded by remember { mutableStateOf(false) }
                    val osList = UserAgentLibrary.getOSList()
                    val currentOS = ua.osFamily.ifBlank { "Android" }

                    ExposedDropdownMenuBox(
                        expanded = osExpanded,
                        onExpandedChange = { osExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = currentOS,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Operating System") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = osExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = osExpanded,
                            onDismissRequest = { osExpanded = false }
                        ) {
                            osList.forEach { os ->
                                DropdownMenuItem(
                                    text = { Text(os) },
                                    onClick = {
                                        val firstBrowser = UserAgentLibrary.getBrowsersForOS(os).firstOrNull() ?: "Chrome"
                                        val firstVersion = UserAgentLibrary.getVersionsFor(os, firstBrowser).firstOrNull()
                                        onUpdate(ua.copy(osFamily = os, browserFamily = firstBrowser, selectedId = firstVersion?.id ?: ""))
                                        osExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Browser Picker
                    var browserExpanded by remember { mutableStateOf(false) }
                    val browserList = UserAgentLibrary.getBrowsersForOS(currentOS)
                    val currentBrowser = ua.browserFamily.ifBlank { browserList.firstOrNull() ?: "Chrome" }

                    ExposedDropdownMenuBox(
                        expanded = browserExpanded,
                        onExpandedChange = { browserExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = currentBrowser,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Browser Family") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = browserExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = browserExpanded,
                            onDismissRequest = { browserExpanded = false }
                        ) {
                            browserList.forEach { b ->
                                DropdownMenuItem(
                                    text = { Text(b) },
                                    onClick = {
                                        val firstVersion = UserAgentLibrary.getVersionsFor(currentOS, b).firstOrNull()
                                        onUpdate(ua.copy(browserFamily = b, selectedId = firstVersion?.id ?: ""))
                                        browserExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Version Picker
                    val versionList = UserAgentLibrary.getVersionsFor(currentOS, currentBrowser)
                    Text(
                        text = "Select Version / Preset",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    versionList.forEach { item ->
                        val isSelected = ua.selectedId == item.id
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onUpdate(ua.copy(selectedId = item.id)) }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = item.version,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = item.userAgentString,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                UAMode.CUSTOM -> {
                    OutlinedTextField(
                        value = ua.customUA,
                        onValueChange = { onUpdate(ua.copy(customUA = it)) },
                        label = { Text("Custom User-Agent String") },
                        placeholder = { Text("Mozilla/5.0 ...") },
                        minLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_custom_ua")
                    )
                }
                UAMode.RANDOM -> {
                    Text(
                        text = "Session Rotation Mode",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Every time a new WebView session starts under this profile, a distinct User-Agent will be selected from the catalog to reduce tracking fingerprinting.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeSettingsTab(
    time: TimeConfig,
    proxy: ProxyConfig,
    onUpdate: (TimeConfig) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isSyncingProxyTime by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Time / Clock Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TimeMode.values().forEach { mode ->
                    FilterChip(
                        selected = time.mode == mode,
                        onClick = { onUpdate(time.copy(mode = mode)) },
                        label = { Text(mode.label) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (time.mode) {
                TimeMode.DEVICE -> {
                    Text(
                        text = "WebView inherits device local time and system timezone directly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TimeMode.MANUAL -> {
                    Text(
                        text = "Manual Timezone & Offset",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    var tzExpanded by remember { mutableStateOf(false) }
                    val tzs = remember { TimezoneHelper.commonTimezones }
                    var searchTz by remember { mutableStateOf("") }
                    val filteredTzs = remember(searchTz) {
                        if (searchTz.isBlank()) tzs.take(40) else tzs.filter { it.contains(searchTz, ignoreCase = true) }
                    }

                    ExposedDropdownMenuBox(
                        expanded = tzExpanded,
                        onExpandedChange = { tzExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = TimezoneHelper.getDisplayName(time.timezoneId),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Selected IANA Timezone") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tzExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = tzExpanded,
                            onDismissRequest = { tzExpanded = false }
                        ) {
                            OutlinedTextField(
                                value = searchTz,
                                onValueChange = { searchTz = it },
                                placeholder = { Text("Search timezone...") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            )
                            filteredTzs.forEach { tz ->
                                DropdownMenuItem(
                                    text = { Text(TimezoneHelper.getDisplayName(tz)) },
                                    onClick = {
                                        onUpdate(time.copy(timezoneId = tz))
                                        tzExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = if (time.offsetMinutes == 0) "" else time.offsetMinutes.toString(),
                        onValueChange = {
                            val off = it.toIntOrNull() ?: 0
                            onUpdate(time.copy(offsetMinutes = off))
                        },
                        label = { Text("Manual Date/Time Offset (Minutes)") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_time_offset")
                    )
                }
                TimeMode.PROXY -> {
                    Text(
                        text = "Use Proxy Server Time",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Simulates clock inside WebView based on the proxy server's location and HTTP response headers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            isSyncingProxyTime = true
                            syncMessage = null
                            coroutineScope.launch {
                                val res = ProxyTester.checkProxy(proxy)
                                when (res) {
                                    is ProxyTestResult.Success -> {
                                        syncMessage = "Synchronized to proxy server date header: ${res.serverDate ?: "UTC"}"
                                        onUpdate(time.copy(simulatedTimezone = "UTC"))
                                    }
                                    is ProxyTestResult.Failure -> {
                                        syncMessage = "Failed to sync: ${res.errorMessage}"
                                    }
                                }
                                isSyncingProxyTime = false
                            }
                        },
                        enabled = !isSyncingProxyTime && proxy.enabled,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSyncingProxyTime) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(end = 8.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text("Syncing from Proxy...")
                        } else {
                            Icon(imageVector = Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sync Time from Proxy Exit Node")
                        }
                    }

                    syncMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
