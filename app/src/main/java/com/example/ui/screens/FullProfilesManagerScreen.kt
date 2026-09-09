package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NoraProfile
import com.example.model.ProfileFilterCriteria
import com.example.model.ProfileFilterEngine
import com.example.model.ProfileSortOption
import com.example.model.ProxyConfig
import com.example.model.ProxyProtocol
import com.example.model.TimeConfig
import com.example.model.TimeMode
import com.example.model.UAMode
import com.example.model.UserAgentConfig
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FullProfilesManagerScreen(
    profiles: List<NoraProfile>,
    activeProfileId: String,
    onSelectProfile: (NoraProfile) -> Unit,
    onEditProfile: (NoraProfile) -> Unit,
    onDuplicateProfile: (String) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onToggleProxy: (String) -> Unit,
    onCreateProfile: (NoraProfile) -> Unit,
    onOpenBackupRestore: () -> Unit = {},
    onOpenBrowserSettings: () -> Unit = {},
    onBack: () -> Unit
) {
    var criteria by remember { mutableStateOf(ProfileFilterCriteria()) }
    var showFilterBar by remember { mutableStateOf(false) }
    var profileToDelete by remember { mutableStateOf<NoraProfile?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    val filteredList = remember(profiles, criteria) {
        ProfileFilterEngine.filterAndSort(profiles, criteria)
    }

    val groupedProfiles = remember(filteredList, criteria.groupBySite) {
        if (criteria.groupBySite) ProfileFilterEngine.groupBySite(filteredList) else emptyMap()
    }

    val allSites = remember(profiles) {
        profiles.map { it.site }.filter { it.isNotBlank() }.distinct()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "إدارة البروفايلات (Profiles)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${profiles.size} ملفات تعريف • ${filteredList.size} معروض",
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
                    IconButton(
                        onClick = onOpenBrowserSettings,
                        modifier = Modifier.testTag("btn_open_browser_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "إعدادات المتصفح"
                        )
                    }

                    IconButton(
                        onClick = onOpenBackupRestore,
                        modifier = Modifier.testTag("btn_open_backup_restore")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ImportExport,
                            contentDescription = "استيراد وتصدير (Backup & Restore)"
                        )
                    }

                    IconButton(onClick = { showFilterBar = !showFilterBar }) {
                        Icon(imageVector = Icons.Default.FilterList, contentDescription = "Filters")
                    }

                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            ProfileSortOption.values().forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (criteria.sortOption == option) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Text(option.label)
                                        }
                                    },
                                    onClick = {
                                        val newAsc = if (criteria.sortOption == option) !criteria.sortAscending else false
                                        criteria = criteria.copy(sortOption = option, sortAscending = newAsc)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val newP = NoraProfile(
                        id = "profile_" + UUID.randomUUID().toString().take(8),
                        name = "New Profile",
                        colorHex = "#3B82F6",
                        proxy = ProxyConfig(enabled = false),
                        userAgent = UserAgentConfig(mode = UAMode.DEFAULT),
                        time = TimeConfig(mode = TimeMode.DEVICE)
                    )
                    onCreateProfile(newP)
                    onEditProfile(newP)
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_create_profile")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Create Profile")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = criteria.searchQuery,
                onValueChange = { criteria = criteria.copy(searchQuery = it) },
                placeholder = { Text("Search profile by name or site...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (criteria.searchQuery.isNotBlank()) {
                        IconButton(onClick = { criteria = criteria.copy(searchQuery = "") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("search_profiles_input")
            )

            // Collapsible Filter & Grouping Drawer
            AnimatedVisibility(visible = showFilterBar) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Filter Options",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Proxy Filter
                            FilterChip(
                                selected = criteria.proxyFilter == true,
                                onClick = {
                                    criteria = criteria.copy(
                                        proxyFilter = if (criteria.proxyFilter == true) null else true
                                    )
                                },
                                label = { Text("Proxy ON") }
                            )

                            FilterChip(
                                selected = criteria.proxyFilter == false,
                                onClick = {
                                    criteria = criteria.copy(
                                        proxyFilter = if (criteria.proxyFilter == false) null else false
                                    )
                                },
                                label = { Text("Direct Only") }
                            )

                            // Custom UA Filter
                            FilterChip(
                                selected = criteria.customUAOnly,
                                onClick = { criteria = criteria.copy(customUAOnly = !criteria.customUAOnly) },
                                label = { Text("Custom UA") }
                            )

                            // Group By Site
                            FilterChip(
                                selected = criteria.groupBySite,
                                onClick = { criteria = criteria.copy(groupBySite = !criteria.groupBySite) },
                                label = { Text("Group by Site") }
                            )
                        }

                        if (allSites.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Filter by Site:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                allSites.forEach { site ->
                                    FilterChip(
                                        selected = criteria.siteFilter == site,
                                        onClick = {
                                            criteria = criteria.copy(
                                                siteFilter = if (criteria.siteFilter == site) null else site
                                            )
                                        },
                                        label = { Text(site) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Profiles List
            if (filteredList.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No matching profiles found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (criteria.groupBySite) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    groupedProfiles.forEach { (siteName, siteGroup) ->
                        item {
                            Column {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Language,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = siteName,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = "${siteGroup.size} profiles",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    siteGroup.forEach { p ->
                                        ProfileCard(
                                            profile = p,
                                            isActive = p.id == activeProfileId,
                                            onSelect = { onSelectProfile(p) },
                                            onEdit = { onEditProfile(p) },
                                            onDuplicate = { onDuplicateProfile(p.id) },
                                            onDelete = { profileToDelete = p },
                                            onToggleProxy = { onToggleProxy(p.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("profiles_lazy_column")
                ) {
                    items(filteredList, key = { it.id }) { p ->
                        ProfileCard(
                            profile = p,
                            isActive = p.id == activeProfileId,
                            onSelect = { onSelectProfile(p) },
                            onEdit = { onEditProfile(p) },
                            onDuplicate = { onDuplicateProfile(p.id) },
                            onDelete = { profileToDelete = p },
                            onToggleProxy = { onToggleProxy(p.id) }
                        )
                    }
                }
            }
        }

        // Delete Confirmation Dialog
        profileToDelete?.let { p ->
            AlertDialog(
                onDismissRequest = { profileToDelete = null },
                title = { Text("Delete Profile") },
                text = { Text("Are you sure you want to delete profile \"${p.name}\"? All isolated cookies and partition state will be purged.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDeleteProfile(p.id)
                            profileToDelete = null
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { profileToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun ProfileCard(
    profile: NoraProfile,
    isActive: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onToggleProxy: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val color = try {
        Color(android.graphics.Color.parseColor(profile.colorHex))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = if (isActive) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onSelect() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            // Profile Color Avatar
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(color)
            ) {
                Text(
                    text = profile.name.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isActive) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "ACTIVE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = if (profile.site.isNotBlank()) profile.site else "Universal / All Sites",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Feature Indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Proxy indicator
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (profile.proxy.enabled) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VpnKey,
                                contentDescription = null,
                                tint = if (profile.proxy.enabled) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = if (profile.proxy.enabled) profile.proxy.protocol.scheme.uppercase() else "Direct",
                                fontSize = 9.sp,
                                color = if (profile.proxy.enabled) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // UA indicator
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = profile.userAgent.mode.name,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Time indicator
                    if (profile.time.mode != TimeMode.DEVICE) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = profile.time.mode.name,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Quick proxy toggle
            Switch(
                checked = profile.proxy.enabled,
                onCheckedChange = { onToggleProxy() },
                modifier = Modifier
                    .padding(end = 4.dp)
                    .testTag("quick_toggle_proxy_${profile.id}")
            )

            // Context Menu (Edit, Duplicate, Delete, Open)
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More Actions")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open / Activate")
                            }
                        },
                        onClick = {
                            onSelect()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Edit Settings")
                            }
                        },
                        onClick = {
                            onEdit()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Duplicate Profile")
                            }
                        },
                        onClick = {
                            onDuplicate()
                            showMenu = false
                        }
                    )
                    if (!profile.isDefault) {
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Delete Profile", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            onClick = {
                                onDelete()
                                showMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}
