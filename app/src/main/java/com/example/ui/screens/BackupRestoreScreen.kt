package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FullBrowserBackup
import com.example.model.NoraProfile
import com.example.model.ParsedBackupPreview
import com.example.model.ProfileConflictResolution
import com.example.repository.BackupManager
import com.example.repository.ProfileRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BackupRestoreScreen(
    repository: ProfileRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val profiles by repository.profiles.collectAsState()
    val bookmarks by repository.bookmarks.collectAsState()
    val history by repository.history.collectAsState()
    val widgetSettings by repository.widgetSettings.collectAsState()
    val browserSettings by repository.browserSettings.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("البروفايلات (Profiles)", "المتصفح بالكامل (Full Browser)")

    // Profile Export State
    var profileExportMode by remember { mutableStateOf(0) } // 0 = All, 1 = Selected
    var selectedProfileIdsForExport by remember { mutableStateOf<Set<String>>(emptySet()) }
    var profileExportSearch by remember { mutableStateOf("") }

    // Content to save for SAF CreateDocument
    var pendingJsonToSave by remember { mutableStateOf<String?>(null) }
    var pendingSaveDescription by remember { mutableStateOf("") }

    // Profile Import State
    var parsedProfilesPreview by remember { mutableStateOf<ParsedBackupPreview?>(null) }
    var selectedProfilesToImportIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var conflictResolution by remember { mutableStateOf(ProfileConflictResolution.KEEP_BOTH) }

    // Full Browser Import State
    var parsedFullBackupPreview by remember { mutableStateOf<FullBrowserBackup?>(null) }
    var fullRestoreMergeMode by remember { mutableStateOf(false) }
    var showFullRestoreConfirmation by remember { mutableStateOf(false) }

    // SAF Launchers
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null && pendingJsonToSave != null) {
            val success = BackupManager.writeTextToUri(context, uri, pendingJsonToSave!!)
            if (success) {
                scope.launch {
                    snackbarHostState.showSnackbar("تم حفظ الملف بنجاح / File saved successfully")
                }
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar("تعذر كتابة الملف / Failed to write file")
                }
            }
            pendingJsonToSave = null
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val jsonContent = BackupManager.readTextFromUri(context, uri)
            if (!jsonContent.isNullOrBlank()) {
                if (selectedTabIndex == 0) {
                    // Profiles Import
                    val result = BackupManager.parseBackup(jsonContent)
                    result.onSuccess { preview ->
                        parsedProfilesPreview = preview
                        selectedProfilesToImportIds = preview.profiles.map { it.id }.toSet()
                        scope.launch {
                            snackbarHostState.showSnackbar("تم تحميل ${preview.profiles.size} بروفايل من الملف")
                        }
                    }.onFailure { err ->
                        scope.launch {
                            snackbarHostState.showSnackbar("فشل قراءة الملف: ${err.message}")
                        }
                    }
                } else {
                    // Full Browser Import
                    val result = BackupManager.deserializeFullBackup(jsonContent)
                    result.onSuccess { full ->
                        parsedFullBackupPreview = full
                        scope.launch {
                            snackbarHostState.showSnackbar("تم تحميل نسخة المتصفح الاحتياطية بنجاح")
                        }
                    }.onFailure { err ->
                        // Fallback check if it's profile only
                        val previewRes = BackupManager.parseBackup(jsonContent)
                        if (previewRes.isSuccess) {
                            val preview = previewRes.getOrNull()!!
                            parsedFullBackupPreview = FullBrowserBackup(
                                profiles = preview.profiles,
                                exportTimestamp = preview.timestamp
                            )
                            scope.launch {
                                snackbarHostState.showSnackbar("تم قراءة البروفايلات (${preview.profiles.size}) من الملف")
                            }
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("ملف غير صالح: ${err.message}")
                            }
                        }
                    }
                }
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar("تعذر قراءة محتوى الملف / Unable to read file")
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "النسخ الاحتياطي والاستيراد",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Backup, Export & Restore",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("backup_restore_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
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
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        modifier = Modifier.testTag("backup_tab_$index")
                    )
                }
            }

            if (selectedTabIndex == 0) {
                // ==========================================
                // TAB 1: PROFILES IMPORT & EXPORT
                // ==========================================
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        // SECTION: EXPORT PROFILES
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("export_profiles_card")
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.FileUpload,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "تصدير البروفايلات (Export Profiles)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                                Text(
                                    text = "تصدير جميع البروفايلات أو بروفايلات محددة كملف JSON لمشاركتها أو حفظها",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                                )

                                // Export Option: All vs Selected
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { profileExportMode = 0 }
                                        .padding(vertical = 4.dp)
                                ) {
                                    RadioButton(
                                        selected = profileExportMode == 0,
                                        onClick = { profileExportMode = 0 },
                                        modifier = Modifier.testTag("export_all_radio")
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Column {
                                        Text(
                                            text = "جميع البروفايلات (${profiles.size} بروفايل)",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "تصدير كامل قائمة البروفايلات بالإعدادات والبروكسي",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { profileExportMode = 1 }
                                        .padding(vertical = 4.dp)
                                ) {
                                    RadioButton(
                                        selected = profileExportMode == 1,
                                        onClick = { profileExportMode = 1 },
                                        modifier = Modifier.testTag("export_selected_radio")
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Column {
                                        Text(
                                            text = "تحديد بروفايلات معينة فقط (${selectedProfileIdsForExport.size}/${profiles.size})",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "اختر يدوياً البروفايلات التي ترغب في تصديرها",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Interactive Profile Checklist when Custom is selected
                                AnimatedVisibility(visible = profileExportMode == 1) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "قائمة الاختيار:",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Row {
                                                TextButton(
                                                    onClick = {
                                                        selectedProfileIdsForExport = profiles.map { it.id }.toSet()
                                                    }
                                                ) {
                                                    Text("تحديد الكل", fontSize = 12.sp)
                                                }
                                                TextButton(
                                                    onClick = {
                                                        selectedProfileIdsForExport = emptySet()
                                                    }
                                                ) {
                                                    Text("إلغاء التحديد", fontSize = 12.sp)
                                                }
                                            }
                                        }

                                        // Search
                                        OutlinedTextField(
                                            value = profileExportSearch,
                                            onValueChange = { profileExportSearch = it },
                                            placeholder = { Text("بحث في البروفايلات...", fontSize = 12.sp) },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Search,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            singleLine = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        val filteredForExport = profiles.filter {
                                            profileExportSearch.isBlank() ||
                                                    it.name.contains(profileExportSearch, ignoreCase = true) ||
                                                    it.site.contains(profileExportSearch, ignoreCase = true)
                                        }

                                        filteredForExport.forEach { profile ->
                                            val isChecked = selectedProfileIdsForExport.contains(profile.id)
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        selectedProfileIdsForExport = if (isChecked) {
                                                            selectedProfileIdsForExport - profile.id
                                                        } else {
                                                            selectedProfileIdsForExport + profile.id
                                                        }
                                                    }
                                                    .padding(vertical = 4.dp)
                                            ) {
                                                Checkbox(
                                                    checked = isChecked,
                                                    onCheckedChange = { check ->
                                                        selectedProfileIdsForExport = if (check) {
                                                            selectedProfileIdsForExport + profile.id
                                                        } else {
                                                            selectedProfileIdsForExport - profile.id
                                                        }
                                                    }
                                                )
                                                val color = try {
                                                    Color(android.graphics.Color.parseColor(profile.colorHex))
                                                } catch (_: Exception) {
                                                    MaterialTheme.colorScheme.primary
                                                }
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                        .background(color)
                                                ) {
                                                    Text(
                                                        text = profile.name.take(1).uppercase(),
                                                        color = Color.White,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = profile.name,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        maxLines = 1
                                                    )
                                                    if (profile.site.isNotBlank()) {
                                                        Text(
                                                            text = profile.site,
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                                if (profile.proxy.enabled) {
                                                    Text(
                                                        text = profile.proxy.protocol.name,
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.secondary,
                                                        modifier = Modifier
                                                            .background(
                                                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                                                RoundedCornerShape(4.dp)
                                                            )
                                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Export Buttons
                                val profilesToExport = if (profileExportMode == 0) {
                                    profiles
                                } else {
                                    profiles.filter { it.id in selectedProfileIdsForExport }
                                }

                                val canExport = profilesToExport.isNotEmpty()

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            if (!canExport) {
                                                Toast.makeText(context, "الرجاء اختيار بروفايل واحد على الأقل", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            val json = BackupManager.exportProfilesJson(profilesToExport)
                                            val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                                            pendingJsonToSave = json
                                            pendingSaveDescription = "Nora Profiles Backup"
                                            createDocumentLauncher.launch("nora_profiles_$dateStr.json")
                                        },
                                        enabled = canExport,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("save_profiles_file_btn")
                                    ) {
                                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("حفظ ملف (${profilesToExport.size})", fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            if (!canExport) return@OutlinedButton
                                            val json = BackupManager.exportProfilesJson(profilesToExport)
                                            BackupManager.shareText(
                                                context = context,
                                                text = json,
                                                title = "Nora Profiles Backup (${profilesToExport.size} profiles)"
                                            )
                                        },
                                        enabled = canExport,
                                        modifier = Modifier.testTag("share_profiles_btn")
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            if (!canExport) return@OutlinedButton
                                            val json = BackupManager.exportProfilesJson(profilesToExport)
                                            BackupManager.copyToClipboard(context, json, "Nora Profiles JSON")
                                            scope.launch {
                                                snackbarHostState.showSnackbar("تم نسخ كود JSON للحافظة بنجاح")
                                            }
                                        },
                                        enabled = canExport,
                                        modifier = Modifier.testTag("copy_profiles_btn")
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    // SECTION: IMPORT PROFILES
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("import_profiles_card")
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.FileDownload,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "استيراد البروفايلات (Import Profiles)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                                Text(
                                    text = "استيراد بروفايلات من ملف JSON أو من الحافظة مع إمكانية فحصها وتحديد ما تريد استيراده",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                                )

                                // Source Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            openDocumentLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("pick_profile_file_btn")
                                    ) {
                                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("اختيار ملف JSON", fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val text = BackupManager.getFromClipboard(context)
                                            if (!text.isNullOrBlank()) {
                                                val parseRes = BackupManager.parseBackup(text)
                                                parseRes.onSuccess { preview ->
                                                    parsedProfilesPreview = preview
                                                    selectedProfilesToImportIds = preview.profiles.map { it.id }.toSet()
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("تم قراءة ${preview.profiles.size} بروفايل من الحافظة")
                                                    }
                                                }.onFailure { err ->
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("نص غير صالح في الحافظة: ${err.message}")
                                                    }
                                                }
                                            } else {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("الحافظة فارغة أو لا تحتوي على نص")
                                                }
                                            }
                                        },
                                        modifier = Modifier.testTag("paste_profile_json_btn")
                                    ) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("لصق من الحافظة", fontSize = 12.sp)
                                    }
                                }

                                // If profiles were parsed from file or clipboard:
                                parsedProfilesPreview?.let { preview ->
                                    Spacer(modifier = Modifier.height(14.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Preview Summary Header
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "تم العثور على ${preview.profiles.size} بروفايل جاهز للاستيراد",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Conflict strategy selection
                                    Text(
                                        text = "طريقة التعامل مع البروفايلات المتطابقة (Conflict Resolution):",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    ProfileConflictResolution.values().forEach { strategy ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { conflictResolution = strategy }
                                                .padding(vertical = 4.dp)
                                        ) {
                                            RadioButton(
                                                selected = conflictResolution == strategy,
                                                onClick = { conflictResolution = strategy }
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Column {
                                                Text(
                                                    text = strategy.titleArabic,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = strategy.description,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Selective import list
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "حدد البروفايلات المراد استيرادها (${selectedProfilesToImportIds.size}/${preview.profiles.size}):",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Row {
                                            TextButton(onClick = {
                                                selectedProfilesToImportIds = preview.profiles.map { it.id }.toSet()
                                            }) {
                                                Text("الكل", fontSize = 11.sp)
                                            }
                                            TextButton(onClick = {
                                                selectedProfilesToImportIds = emptySet()
                                            }) {
                                                Text("لا شيء", fontSize = 11.sp)
                                            }
                                        }
                                    }

                                    preview.profiles.forEach { p ->
                                        val isChecked = selectedProfilesToImportIds.contains(p.id)
                                        val isExisting = profiles.any { it.id == p.id }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedProfilesToImportIds = if (isChecked) {
                                                        selectedProfilesToImportIds - p.id
                                                    } else {
                                                        selectedProfilesToImportIds + p.id
                                                    }
                                                }
                                                .padding(vertical = 4.dp)
                                        ) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = { c ->
                                                    selectedProfilesToImportIds = if (c) {
                                                        selectedProfilesToImportIds + p.id
                                                    } else {
                                                        selectedProfilesToImportIds - p.id
                                                    }
                                                }
                                            )
                                            val color = try {
                                                Color(android.graphics.Color.parseColor(p.colorHex))
                                            } catch (_: Exception) {
                                                MaterialTheme.colorScheme.primary
                                            }
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                            ) {
                                                Text(
                                                    text = p.name.take(1).uppercase(),
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = p.name,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        maxLines = 1
                                                    )
                                                    if (isExisting) {
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "(موجود مسبقاً)",
                                                            fontSize = 10.sp,
                                                            color = MaterialTheme.colorScheme.error
                                                        )
                                                    }
                                                }
                                                if (p.site.isNotBlank()) {
                                                    Text(
                                                        text = p.site,
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = {
                                            val toImport = preview.profiles.filter { it.id in selectedProfilesToImportIds }
                                            if (toImport.isEmpty()) {
                                                Toast.makeText(context, "الرجاء تحديد بروفايل واحد على الأقل", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            val result = repository.importProfiles(toImport, conflictResolution)
                                            scope.launch {
                                                snackbarHostState.showSnackbar(result.message)
                                            }
                                            parsedProfilesPreview = null
                                        },
                                        enabled = selectedProfilesToImportIds.isNotEmpty(),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("confirm_import_profiles_btn")
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("استيراد البروفايلات المحددة (${selectedProfilesToImportIds.size})")
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            } else {
                // ==========================================
                // TAB 2: FULL BROWSER IMPORT & EXPORT
                // ==========================================
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        // FULL BROWSER EXPORT CARD
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("export_full_browser_card")
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.FileUpload,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "تصدير المتصفح بالكامل (Full Browser Backup)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                                Text(
                                    text = "حفظ نسخة احتياطية شاملة تشمل جميع البروفايلات، البروفايل النشط، إعدادات المتصفح، الأداة العائمة، العلامات المرجعية، والسجل.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                                )

                                // Overview of what will be exported
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "محتويات النسخة الاحتياطية الحالية:",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("• عدد البروفايلات:", fontSize = 12.sp)
                                            Text("${profiles.size} بروفايل", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("• العلامات المرجعية (Bookmarks):", fontSize = 12.sp)
                                            Text("${bookmarks.size}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("• سجل التصفح (History):", fontSize = 12.sp)
                                            Text("${history.size} صفحة", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("• إعدادات الأداة العائمة:", fontSize = 12.sp)
                                            Text(if (widgetSettings.isVisible) "مفعلة" else "معطلة", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("• محرك البحث الافتراضي:", fontSize = 12.sp)
                                            Text(browserSettings.searchEngine, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val json = repository.exportFullBrowserJson()
                                            val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                                            pendingJsonToSave = json
                                            pendingSaveDescription = "Nora Full Browser Backup"
                                            createDocumentLauncher.launch("nora_full_backup_$dateStr.json")
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("save_full_backup_file_btn")
                                    ) {
                                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("حفظ النسخة بالكامل", fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val json = repository.exportFullBrowserJson()
                                            BackupManager.shareText(
                                                context = context,
                                                text = json,
                                                title = "Nora Full Browser Backup"
                                            )
                                        },
                                        modifier = Modifier.testTag("share_full_backup_btn")
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val json = repository.exportFullBrowserJson()
                                            BackupManager.copyToClipboard(context, json, "Nora Full Browser Backup JSON")
                                            scope.launch {
                                                snackbarHostState.showSnackbar("تم نسخ كود النسخة الاحتياطية بالكامل للحافظة")
                                            }
                                        },
                                        modifier = Modifier.testTag("copy_full_backup_btn")
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    // FULL BROWSER RESTORE CARD
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("restore_full_browser_card")
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.FileDownload,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "استعادة المتصفح بالكامل (Restore Full Browser)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                                Text(
                                    text = "استعادة جميع بيانات المتصفح من ملف نسخة احتياطية كاملة سابقة",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            openDocumentLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("pick_full_backup_file_btn")
                                    ) {
                                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("اختيار ملف النسخة", fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val text = BackupManager.getFromClipboard(context)
                                            if (!text.isNullOrBlank()) {
                                                val res = BackupManager.deserializeFullBackup(text)
                                                res.onSuccess { full ->
                                                    parsedFullBackupPreview = full
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("تم قراءة النسخة الاحتياطية من الحافظة")
                                                    }
                                                }.onFailure { err ->
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("تعذر قراءة النسخة: ${err.message}")
                                                    }
                                                }
                                            } else {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("الحافظة فارغة")
                                                }
                                            }
                                        },
                                        modifier = Modifier.testTag("paste_full_backup_btn")
                                    ) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("لصق من الحافظة", fontSize = 12.sp)
                                    }
                                }

                                // Loaded Full Backup Details
                                parsedFullBackupPreview?.let { full ->
                                    Spacer(modifier = Modifier.height(14.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "بيانات النسخة الاحتياطية المحملة:",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                            }

                                            val dateFormatted = try {
                                                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(full.exportTimestamp))
                                            } catch (_: Exception) { "غير محدد" }

                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("• تاريخ الإنشاء: $dateFormatted", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("• عدد البروفايلات: ${full.profiles.size}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("• العلامات المرجعية: ${full.bookmarks.size}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("• سجل التصفح: ${full.history.size}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Mode: Overwrite vs Merge
                                    Text(
                                        text = "وضع الاستعادة:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { fullRestoreMergeMode = false }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        RadioButton(
                                            selected = !fullRestoreMergeMode,
                                            onClick = { fullRestoreMergeMode = false }
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Column {
                                            Text("استبدال كامل (Full Overwrite)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            Text("استبدال كل البيانات الحالية بالبيانات الموجودة في النسخة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { fullRestoreMergeMode = true }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        RadioButton(
                                            selected = fullRestoreMergeMode,
                                            onClick = { fullRestoreMergeMode = true }
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Column {
                                            Text("دمج مع البيانات الحالية (Merge Mode)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            Text("إضافة البروفايلات والعلامات الجديدة دون حذف أو مسح الحالية", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Button(
                                        onClick = {
                                            if (!fullRestoreMergeMode) {
                                                showFullRestoreConfirmation = true
                                            } else {
                                                val res = repository.restoreFullBrowserBackup(full, mergeMode = true)
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(res.message)
                                                }
                                                parsedFullBackupPreview = null
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (!fullRestoreMergeMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("start_full_restore_btn")
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (!fullRestoreMergeMode) "استبدال واستعادة المتصفح بالكامل" else "دمج بيانات المتصفح الآن")
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    // Full Restore Confirmation Dialog
    if (showFullRestoreConfirmation && parsedFullBackupPreview != null) {
        AlertDialog(
            onDismissRequest = { showFullRestoreConfirmation = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("تأكيد استعادة المتصفح بالكامل") },
            text = {
                Text(
                    "تحذير: سيتم استبدال جميع بيانات المتصفح الحالية والبروفايلات بالبيانات الموجودة في ملف النسخة الاحتياطية. هل ترغب في المتابعة؟",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val full = parsedFullBackupPreview!!
                        val res = repository.restoreFullBrowserBackup(full, mergeMode = false)
                        showFullRestoreConfirmation = false
                        parsedFullBackupPreview = null
                        scope.launch {
                            snackbarHostState.showSnackbar(res.message)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("نعم، استبدل واستعد")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFullRestoreConfirmation = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
