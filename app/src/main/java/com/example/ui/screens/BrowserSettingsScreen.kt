package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BrowserGeneralSettings
import com.example.model.FloatingWidgetSettings
import com.example.repository.ProfileRepository
import com.example.util.UrlHelper
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserSettingsScreen(
    repository: ProfileRepository,
    onOpenBackupRestore: () -> Unit,
    onBack: () -> Unit
) {
    val browserSettings by repository.browserSettings.collectAsState()
    val widgetSettings by repository.widgetSettings.collectAsState()

    var showClearDataDialog by remember { mutableStateOf(false) }
    var clearHistoryChecked by remember { mutableStateOf(true) }
    var clearCookiesChecked by remember { mutableStateOf(true) }
    var clearCacheChecked by remember { mutableStateOf(true) }

    var searchMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "إعدادات المتصفح الشاملة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Browser Global Settings",
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SECTION 1: SEARCH & HOMEPAGE
            SettingsSectionHeader(
                icon = Icons.Default.Search,
                titleArabic = "محرك البحث والصفحة الرئيسية",
                titleEnglish = "Search Engine & Home"
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Search Engine Picker
                    ExposedDropdownMenuBox(
                        expanded = searchMenuExpanded,
                        onExpandedChange = { searchMenuExpanded = !searchMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = browserSettings.searchEngine,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("محرك البحث الافتراضي / Default Engine") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = searchMenuExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = searchMenuExpanded,
                            onDismissRequest = { searchMenuExpanded = false }
                        ) {
                            UrlHelper.searchEngines.forEach { (name, _) ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        repository.updateBrowserSettings(browserSettings.copy(searchEngine = name))
                                        searchMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Homepage URL
                    var tempHome by remember(browserSettings.homeUrl) { mutableStateOf(browserSettings.homeUrl) }
                    OutlinedTextField(
                        value = tempHome,
                        onValueChange = {
                            tempHome = it
                            repository.updateBrowserSettings(browserSettings.copy(homeUrl = it))
                        },
                        label = { Text("عنوان صفحة البداية / Home Page URL") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // SECTION 2: PRIVACY & SHIELDS
            SettingsSectionHeader(
                icon = Icons.Default.Shield,
                titleArabic = "الخصوصية ودروع الحماية",
                titleEnglish = "Privacy & Shields"
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Tracker & Ad Blocker
                    SettingSwitchRow(
                        title = "حظر المتتبعات والإعلانات (Tracker Shield)",
                        description = "يمنع شفرات التتبع وحزم التحليلات والإعلانات المزعجة",
                        checked = browserSettings.adBlockEnabled,
                        onCheckedChange = { repository.updateBrowserSettings(browserSettings.copy(adBlockEnabled = it)) }
                    )

                    HorizontalDivider()

                    // Do Not Track
                    SettingSwitchRow(
                        title = "إرسال ترويسة عدم التتبع (Do Not Track)",
                        description = "مطالبة المواقع بعدم تتبع نشاط تصفحك عبر الويب",
                        checked = browserSettings.doNotTrack,
                        onCheckedChange = { repository.updateBrowserSettings(browserSettings.copy(doNotTrack = it)) }
                    )

                    HorizontalDivider()

                    // Clear on exit
                    SettingSwitchRow(
                        title = "مسح الكوكيز عند الإغلاق (Clear Cookies on Exit)",
                        description = "مسح الجلسات المؤقتة تلقائياً للحفاظ على الخصوصية القصوى",
                        checked = browserSettings.clearCookiesOnExit,
                        onCheckedChange = { repository.updateBrowserSettings(browserSettings.copy(clearCookiesOnExit = it)) }
                    )

                    HorizontalDivider()

                    // Clear Browsing Data Button
                    OutlinedButton(
                        onClick = { showClearDataDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("مسح بيانات التصفح الآن (Clear Browsing Data)")
                    }
                }
            }

            // SECTION 3: FLOATING WIDGET SETTINGS
            SettingsSectionHeader(
                icon = Icons.Default.Widgets,
                titleArabic = "الأداة العائمة للتبديل السريع",
                titleEnglish = "Floating Profile Switcher"
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SettingSwitchRow(
                        title = "إظهار الأداة العائمة على الشاشة",
                        description = "فقاعة عائمة ذكية للتبديل الفوري بين البروفايلات",
                        checked = widgetSettings.isVisible,
                        onCheckedChange = { repository.updateWidgetSettings(widgetSettings.copy(isVisible = it)) }
                    )

                    HorizontalDivider()

                    // Opacity Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "شفافية الأداة (Opacity)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${widgetSettings.opacityPercent}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = widgetSettings.opacityPercent.toFloat(),
                            onValueChange = {
                                repository.updateWidgetSettings(widgetSettings.copy(opacityPercent = it.roundToInt()))
                            },
                            valueRange = 30f..100f,
                            steps = 14
                        )
                    }

                    // Reset Position
                    OutlinedButton(
                        onClick = {
                            repository.updateWidgetSettings(
                                widgetSettings.copy(offsetXPercent = 0.8f, offsetYPercent = 0.75f)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إعادة ضبط موضع الأداة العائمة")
                    }
                }
            }

            // SECTION 4: BACKUP & DATA
            SettingsSectionHeader(
                icon = Icons.Default.Backup,
                titleArabic = "النسخ الاحتياطي ونقل البيانات",
                titleEnglish = "Backup & Portability"
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "استيراد وتصدير شامل للبيانات",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "تصدير جميع البروفايلات والعلامات والسجل بملف JSON آمن، أو استيراد بروفايلات محددة مع حل التعارضات.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onOpenBackupRestore,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("فتح مركز النسخ والاستيراد (Backup Hub)")
                    }
                }
            }

            // SECTION 5: ABOUT
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), shape = CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "متصفح نورا Nora Browser Pro",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "إصدار 2.5 • محرك عزل البروفايلات المضاد للكشف (Anti-Detect Zero-Leak Engine)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Clear Data Confirmation Dialog
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "مسح بيانات التصفح",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "حدد البيانات التي ترغب في مسحها نهائياً:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = clearHistoryChecked,
                            onCheckedChange = { clearHistoryChecked = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("سجل التصفح (History)", style = MaterialTheme.typography.bodyMedium)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = clearCookiesChecked,
                            onCheckedChange = { clearCookiesChecked = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ملفات تعريف الارتباط (Cookies)", style = MaterialTheme.typography.bodyMedium)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = clearCacheChecked,
                            onCheckedChange = { clearCacheChecked = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("الملفات المؤقتة والكاش (Cache)", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.clearBrowsingData(
                            clearHist = clearHistoryChecked,
                            clearMarks = false,
                            clearCookies = clearCookiesChecked,
                            clearCache = clearCacheChecked
                        )
                        showClearDataDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("مسح الآن")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    titleArabic: String,
    titleEnglish: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = titleArabic,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = titleEnglish,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
