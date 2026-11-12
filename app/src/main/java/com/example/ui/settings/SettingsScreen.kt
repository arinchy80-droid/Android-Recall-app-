package com.example.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Vibration
import com.example.data.local.entity.ReminderTone
import com.example.data.local.entity.ReminderVibratePattern
import com.example.ui.theme.Glossy3DOrbIcon
import com.example.ui.theme.OrbIconType
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.window.PopupProperties
import com.example.BuildConfig
import com.example.ui.RecallViewModel
import com.example.ui.theme.AppVisualPreferences
import com.example.ui.theme.NeuralAppBackground
import com.example.ui.theme.RecallCyanSecondary
import com.example.ui.theme.RecallVioletPrimary
import com.example.ui.theme.TaskbarDropdownButton
import com.example.ui.theme.UnifiedTaskbarInputBox
import com.example.ui.theme.WallpaperTheme
import com.example.ui.theme.getVibrantTagColor
import com.example.ui.theme.seamlessInnerTextFieldColors
import com.example.ui.theme.translucentWhiteTextFieldColors
import android.Manifest
import android.os.Build
import com.example.util.AppIntegrationsHelper
import com.example.util.NotificationHelper
import com.example.util.ReminderNotificationHelper
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: RecallViewModel,
    onBack: (() -> Unit)? = null
) {
    var showHelpAndFaq by remember { mutableStateOf(false) }
    var helpInitialTab by remember { mutableIntStateOf(0) }

    if (showHelpAndFaq) {
        HelpAndFaqScreen(
            initialTab = helpInitialTab,
            onClose = { showHelpAndFaq = false }
        )
        return
    }

    if (onBack != null) {
        BackHandler { onBack() }
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val actionItems by viewModel.actionItems.collectAsStateWithLifecycle()
    val notesCount by viewModel.notesCount.collectAsStateWithLifecycle()
    val peopleCount by viewModel.peopleCount.collectAsStateWithLifecycle()

    val selectedTheme by AppVisualPreferences.selectedTheme.collectAsStateWithLifecycle()
    val isCompactStream by AppVisualPreferences.isCompactStream.collectAsStateWithLifecycle()
    val isHapticsEnabled by AppVisualPreferences.isHapticsEnabled.collectAsStateWithLifecycle()
    val debriefReminderHour by AppVisualPreferences.debriefReminderHour.collectAsStateWithLifecycle()

    var showBackupDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonInput by remember { mutableStateOf("") }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var backupJsonContent by remember { mutableStateOf("") }

    val isAppLockEnabled by com.example.util.PrivacySecurityManager.isAppLockEnabled.collectAsStateWithLifecycle()
    val audioRetentionPolicy by com.example.util.PrivacySecurityManager.audioRetentionPolicy.collectAsStateWithLifecycle()
    val requireConsentPrompt by com.example.util.PrivacySecurityManager.requireRecordingConsentPrompt.collectAsStateWithLifecycle()
    val privacyAuditLogs by com.example.util.PrivacySecurityManager.auditLogs.collectAsStateWithLifecycle()
    var showChangePinDialog by remember { mutableStateOf(false) }
    var newPinDraft by remember { mutableStateOf("") }
    var showRetentionDropdown by remember { mutableStateOf(false) }
    var showAuditLogsExpanded by remember { mutableStateOf(false) }
    var audioStorageRefreshTick by remember { mutableStateOf(0) }
    val audioStorageStats = remember(audioRetentionPolicy, audioStorageRefreshTick) {
        com.example.util.PrivacySecurityManager.getAudioStorageStats(context)
    }

    var actionFilter by remember { mutableStateOf("Pending") }
    var quickTaskInput by remember { mutableStateOf("") }

    val customWallpaperPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val saved = AppVisualPreferences.saveCustomWallpaperUri(context, uri)
            if (saved) {
                viewModel.showFeedback("Custom photo wallpaper applied across all screens!")
            } else {
                viewModel.showFeedback("Could not load selected wallpaper photo.")
            }
        }
    }

    val notifPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val hasGeminiKey = try {
        val k = BuildConfig.GEMINI_API_KEY
        !k.isNullOrBlank() && k != "MY_GEMINI_API_KEY"
    } catch (e: Exception) {
        false
    }

    val filteredActions = remember(actionItems, actionFilter) {
        when (actionFilter) {
            "Pending" -> actionItems.filter { !it.isCompleted }
            "Completed" -> actionItems.filter { it.isCompleted }
            else -> actionItems
        }
    }

    val reminderHours = remember {
        listOf(
            -1 to "Off",
            19 to "7:00 PM",
            20 to "8:00 PM",
            21 to "9:00 PM",
            22 to "10:00 PM"
        )
    }

    NeuralAppBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("settings_list"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Profile, Intelligence & Settings",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "100% offline-first local vault • Personal Profile, AI & Connected Services",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                    IconButton(
                        onClick = {
                            helpInitialTab = 0
                            showHelpAndFaq = true
                        },
                        modifier = Modifier.testTag("header_help_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = "Open Help & FAQ",
                            tint = RecallCyanSecondary
                        )
                    }
                }
            }

            // Card -1: Personal Profile & Connected Services (Section 3.3 Profile & Settings)
            item {
                val currentDisplayName by viewModel.userDisplayName.collectAsStateWithLifecycle()
                val currentFocusGoal by viewModel.userFocusGoal.collectAsStateWithLifecycle()
                var nameInput by remember(currentDisplayName) { mutableStateOf(currentDisplayName) }
                var goalInput by remember(currentFocusGoal) { mutableStateOf(currentFocusGoal) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_profile_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.88f)),
                    border = BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.65f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Glossy3DOrbIcon(orbType = OrbIconType.PEOPLE, size = 32.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Personal Profile & Life Focus",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Personalises your Home Dashboard greeting and AI Life Intelligence focus",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Your Display Name (e.g., Aaron)", fontSize = 11.sp) },
                            singleLine = true,
                            colors = translucentWhiteTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_display_name_input")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = goalInput,
                            onValueChange = { goalInput = it },
                            label = { Text("Primary Life / Work Focus Goal", fontSize = 11.sp) },
                            singleLine = true,
                            colors = translucentWhiteTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_focus_goal_input")
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.updateUserProfile(nameInput, goalInput)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RecallVioletPrimary),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("save_profile_button")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save Profile", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    AppIntegrationsHelper.scheduleInCalendar(
                                        context = context,
                                        title = "Recall Weekly Life Review",
                                        description = "Review commitments, people, and memories in Recall",
                                        explicitTimeMillis = System.currentTimeMillis() + 86_400_000L
                                    )
                                },
                                border = BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.65f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = RecallCyanSecondary, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync Calendar", color = RecallCyanSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Sovereign Plan & Connected Services Status
                        var weeklyBriefingEnabled by remember {
                            mutableStateOf(com.example.util.RecallLifeOrganiserManager.isWeeklyBriefingEnabled(context))
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E293B).copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.55f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "⚡ Recall Sovereign Pro (Local-First Vault)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF10B981)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF10B981).copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "ACTIVE • UNLIMITED",
                                            color = Color(0xFF10B981),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Weekly AI Life Briefing Synthesis",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Automatically synthesize 7-day memory velocity, fulfilled promises & active projects",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                    Switch(
                                        checked = weeklyBriefingEnabled,
                                        onCheckedChange = {
                                            weeklyBriefingEnabled = it
                                            com.example.util.RecallLifeOrganiserManager.setWeeklyBriefingEnabled(context, it)
                                            viewModel.showFeedback(
                                                if (it) "Weekly AI Life Briefing enabled"
                                                else "Weekly AI Life Briefing paused"
                                            )
                                        },
                                        modifier = Modifier.testTag("toggle_weekly_briefing")
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.navigateToHubSection(
                                                com.example.ui.navigation.Screen.AiIntelligence,
                                                "Personal Insights"
                                            )
                                        },
                                        border = BorderStroke(1.dp, RecallVioletPrimary),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("🧬 AI Preferences", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.cleanupDuplicateNotesAndInvalidContacts()
                                        },
                                        border = BorderStroke(1.dp, RecallCyanSecondary),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("🧹 Deduplicate Vault", color = RecallCyanSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Card 0: Help, Comprehensive User Guide & FAQ
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_help_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.88f)),
                    border = BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.65f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = null,
                                tint = RecallCyanSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Help, User Guide & FAQ",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "9 step-by-step modules & 14 FAQs covering every feature and setting",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Learn how Living Notes automatically update older balances and facts (e.g., 'I owe Jason $600' -> 'I paid Jason $200'), plus Reminders, Physical Tones, Daily Debrief, People CRM, OCR Gallery, Ask AI, and Backups.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    helpInitialTab = 0
                                    showHelpAndFaq = true
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RecallVioletPrimary,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp)
                                    .testTag("open_help_guide_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("How-To Guide", fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = {
                                    helpInitialTab = 1
                                    showHelpAndFaq = true
                                },
                                border = BorderStroke(1.dp, RecallCyanSecondary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RecallCyanSecondary),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp)
                                    .testTag("open_faq_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QuestionAnswer,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("View FAQ", fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }
                }
            }

            // Card 0.5: Appearance, Themes & Personalization
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_appearance_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.88f)),
                    border = BorderStroke(1.dp, RecallVioletPrimary.copy(alpha = 0.65f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = RecallCyanSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Appearance, Themes & Reminders",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Active Theme: ${selectedTheme.displayName} (${selectedTheme.subtitle})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = RecallCyanSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Background Theme Selector:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(WallpaperTheme.entries) { theme ->
                                val isSelected = selectedTheme == theme
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) RecallVioletPrimary else Color(0xFF1E293B),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) RecallCyanSecondary else Color(0xFF475569)
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            AppIntegrationsHelper.triggerHapticClick(context, isHapticsEnabled)
                                            if (theme == WallpaperTheme.CUSTOM_PHOTO && AppVisualPreferences.customWallpaperPath.value == null) {
                                                customWallpaperPicker.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            } else {
                                                AppVisualPreferences.setTheme(context, theme)
                                            }
                                        }
                                        .testTag("theme_option_${theme.id}")
                                ) {
                                    Text(
                                        text = theme.displayName,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                customWallpaperPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            border = BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.7f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RecallCyanSecondary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
                                .testTag("pick_custom_wallpaper_button")
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Choose Custom Photo Wallpaper from Gallery", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Compact Stream Density Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Compact Stream Density", color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Show more memories on screen with streamlined cards", color = Color(0xFFCBD5E1), style = MaterialTheme.typography.labelSmall)
                            }
                            Switch(
                                checked = isCompactStream,
                                onCheckedChange = {
                                    AppVisualPreferences.setCompactStream(context, it)
                                    AppIntegrationsHelper.triggerHapticClick(context, isHapticsEnabled)
                                },
                                modifier = Modifier.testTag("toggle_compact_stream")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Haptic Feedback Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Tactile Haptic Feedback", color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Subtle vibration when starting recordings or completing tasks", color = Color(0xFFCBD5E1), style = MaterialTheme.typography.labelSmall)
                            }
                            Switch(
                                checked = isHapticsEnabled,
                                onCheckedChange = {
                                    AppVisualPreferences.setHapticsEnabled(context, it)
                                    if (it) AppIntegrationsHelper.triggerHapticClick(context, true)
                                },
                                modifier = Modifier.testTag("toggle_haptics")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Evening Debrief Reminder Time Selector
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = RecallCyanSecondary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Evening Debrief Reminder:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        !ReminderNotificationHelper.hasNotificationPermission(context)
                                    ) {
                                        try {
                                            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } catch (_: Throwable) {
                                        }
                                    }
                                    NotificationHelper.showDebriefNotification(context)
                                    viewModel.showFeedback("Evening Debrief reminder notification & chime sent!")
                                }
                            ) {
                                Text("Send Test", color = RecallCyanSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(reminderHours) { (hourVal, label) ->
                                val isSelected = debriefReminderHour == hourVal
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) RecallCyanSecondary else Color(0xFF1E293B),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            if (hourVal >= 0 &&
                                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                                !ReminderNotificationHelper.hasNotificationPermission(context)
                                            ) {
                                                try {
                                                    notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                } catch (_: Throwable) {
                                                }
                                            }
                                            AppVisualPreferences.setDebriefReminderHour(context, hourVal)
                                            viewModel.showFeedback(
                                                if (hourVal < 0) "Evening Debrief reminder turned off"
                                                else "Evening Debrief exact alarm set for $label (works when closed & locked)"
                                            )
                                        }
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFF040711) else Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Card 0.6: Physical Message Tones & Haptics Studio
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_tones_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.88f)),
                    border = BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.65f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Glossy3DOrbIcon(
                                orbType = OrbIconType.BELL,
                                size = 32.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Physical Message Tone Studio",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Audible notification chimes and tactile haptics",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = RecallCyanSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Tap to preview audible tone & feel physical vibration:",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        ReminderTone.entries.forEach { tone ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1E293B).copy(alpha = 0.85f),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.previewReminderTone(tone, ReminderVibratePattern.STRONG_PULSE)
                                        viewModel.showFeedback("Playing ${tone.displayName} tone + physical haptic pulse")
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = RecallCyanSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tone.displayName,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = tone.description,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF94A3B8),
                                            fontSize = 10.sp
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = RecallVioletPrimary.copy(alpha = 0.35f),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.PlayArrow,
                                                contentDescription = "Test",
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Play", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Card 1: Offline Local Database Status (Room SQLite + FTS4)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.85f)),
                    border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.65f))
                ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = RecallCyanSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Local Offline Database",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "OFFLINE-FIRST PERSISTENCE ACTIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "All notes, people profiles, and transcripts reside locally in Room SQLite with indexed FTS4.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "$notesCount Notes Indexed",
                            style = MaterialTheme.typography.labelMedium,
                            color = RecallVioletPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "$peopleCount People In CRM",
                            style = MaterialTheme.typography.labelMedium,
                            color = RecallCyanSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "${actionItems.count { !it.isCompleted }} Pending Actions",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Card 2: AI & Processing Engine (Gemini / On-Device Fallback)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.85f)),
                border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.65f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = RecallVioletPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "AI Intelligence & Speech Engine",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (hasGeminiKey) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = if (hasGeminiKey) "API KEY CONFIGURED" else "USING CLIENT & ON-DEVICE ENGINE",
                                color = if (hasGeminiKey) Color(0xFF10B981) else Color(0xFFF59E0B),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Audio: Continuous raw AAC recording + in-app variable-speed audio playback.\n• RAG Concierge: Local context grounding with exact markdown source citations.\n• Debrief Unpacker: Structured ingestion routing into discrete Room records.\n• Vision: On-device image capture and multimodal OCR parsing.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }
        }

        // Card 3: Action Items Manager + Calendar Sync
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.85f)),
                border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.65f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Extracted Action Items (${actionItems.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Filter Dropdown Selector: Pending / Completed / All (replaces single pill tabs)
                    var showActionFilterDropdown by remember { mutableStateOf(false) }
                    val activeFilterColor = getVibrantTagColor(actionFilter)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Filter Status:",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFCBD5E1),
                            fontWeight = FontWeight.SemiBold
                        )

                        Box {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.90f),
                                border = BorderStroke(1.5.dp, activeFilterColor),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { showActionFilterDropdown = true }
                                    .testTag("action_filter_dropdown")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = actionFilter,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = activeFilterColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select Action Filter",
                                        tint = Color(0xFF334155),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showActionFilterDropdown,
                                onDismissRequest = { showActionFilterDropdown = false },
                                properties = PopupProperties(clippingEnabled = true),
                                modifier = Modifier
                                    .background(Color(0xFF0F172A))
                                    .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                            ) {
                                listOf("Pending", "Completed", "All").forEach { filterTab ->
                                    val isSelected = actionFilter == filterTab
                                    val fColor = getVibrantTagColor(filterTab)
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = filterTab,
                                                color = if (isSelected) fColor else Color.White,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        },
                                        trailingIcon = {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = fColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        },
                                        onClick = {
                                            actionFilter = filterTab
                                            showActionFilterDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Unified Quick-Add Action Item Box with Internal Top Taskbar Dropdowns
                    var showPriorityPrefixMenu by remember { mutableStateOf(false) }

                    UnifiedTaskbarInputBox(
                        shape = RoundedCornerShape(16.dp),
                        borderColor = RecallVioletPrimary,
                        modifier = Modifier.fillMaxWidth(),
                        taskbarContent = {
                            // (A) Task Priority / Tag Prefix Dropdown
                            Box {
                                TaskbarDropdownButton(
                                    label = "Priority / Tag",
                                    accentColor = RecallVioletPrimary,
                                    dotColor = RecallVioletPrimary,
                                    onClick = { showPriorityPrefixMenu = true }
                                )
                                DropdownMenu(
                                    expanded = showPriorityPrefixMenu,
                                    onDismissRequest = { showPriorityPrefixMenu = false },
                                    properties = PopupProperties(clippingEnabled = true),
                                    modifier = Modifier
                                        .background(Color(0xFF0F172A))
                                        .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                                ) {
                                    listOf(
                                        "🔴 [Urgent] ",
                                        "📅 [Follow-up] ",
                                        "💼 [Work] ",
                                        "🏠 [Personal] "
                                    ).forEach { prefix ->
                                        DropdownMenuItem(
                                            text = { Text(prefix, color = Color.White, fontWeight = FontWeight.Bold) },
                                            onClick = {
                                                quickTaskInput = prefix + quickTaskInput
                                                showPriorityPrefixMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            // (B) Task Status Filter Dropdown inside Taskbar
                            Box {
                                TaskbarDropdownButton(
                                    label = "Show: $actionFilter",
                                    dotColor = activeFilterColor,
                                    accentColor = activeFilterColor,
                                    onClick = { showActionFilterDropdown = true }
                                )
                            }

                            if (quickTaskInput.isNotEmpty()) {
                                TaskbarDropdownButton(
                                    label = "Clear",
                                    accentColor = Color(0xFF94A3B8),
                                    onClick = { quickTaskInput = "" }
                                )
                            }
                        },
                        bodyContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 6.dp)
                            ) {
                                OutlinedTextField(
                                    value = quickTaskInput,
                                    onValueChange = { quickTaskInput = it },
                                    placeholder = { Text("Add a new task or reminder...", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    colors = seamlessInnerTextFieldColors(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("quick_add_task_input")
                                )
                                IconButton(
                                    onClick = {
                                        if (quickTaskInput.isNotBlank()) {
                                            viewModel.createQuickActionItem(quickTaskInput)
                                            quickTaskInput = ""
                                        }
                                    },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(RecallVioletPrimary, RoundedCornerShape(10.dp))
                                        .testTag("quick_add_task_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Task", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (filteredActions.isEmpty()) {
                        Text(
                            "No $actionFilter action items. Unpack your day in the Debrief tab or add one above.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1)
                        )
                    } else {
                        filteredActions.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = item.isCompleted,
                                    onCheckedChange = {
                                        AppIntegrationsHelper.triggerHapticClick(context, isHapticsEnabled)
                                        viewModel.toggleActionItem(item.id, it)
                                    }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.task,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        AppIntegrationsHelper.scheduleInCalendar(
                                            context = context,
                                            title = item.task,
                                            description = "Task from Recall Personal Memory AI",
                                            explicitTimeMillis = item.dueDate
                                        )
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CalendarToday,
                                        contentDescription = "Schedule in Calendar",
                                        tint = RecallCyanSecondary,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteActionItem(item.id) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Card 4: Privacy, Audio Retention Controls, Consent Workflows & Security Vault
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.85f)),
                border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.65f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Privacy, Audio Retention & Security Vault",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Protect your external brain with a persistent PIN Vault Lock, configurable raw audio retention policies, recording consent workflows, and portable JSON backups.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 1. Persistent PIN Vault Lock Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = RecallCyanSecondary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("PIN Security Vault Lock", color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Require 4-digit SHA-256 PIN when opening Recall", color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelSmall)
                        }
                        Switch(
                            checked = isAppLockEnabled,
                            onCheckedChange = { enabled ->
                                com.example.util.PrivacySecurityManager.setAppLockEnabled(context, enabled)
                                Toast.makeText(
                                    context,
                                    if (enabled) "PIN Security Vault Lock Enabled" else "PIN Security Vault Lock Disabled",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.testTag("settings_pin_lock_switch")
                        )
                    }

                    if (isAppLockEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    newPinDraft = ""
                                    showChangePinDialog = true
                                },
                                border = BorderStroke(1.dp, RecallCyanSecondary),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("change_pin_button")
                            ) {
                                Text("Change 4-Digit PIN", fontSize = 12.sp, color = RecallCyanSecondary, fontWeight = FontWeight.SemiBold)
                            }
                            OutlinedButton(
                                onClick = {
                                    com.example.util.PrivacySecurityManager.lockSessionNow()
                                },
                                border = BorderStroke(1.dp, RecallVioletPrimary),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("lock_vault_now_button")
                            ) {
                                Text("🔒 Lock Vault Now", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Explicit Recording Consent Prompt Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Pre-Recording Consent Prompt", color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Show explicit participant consent confirmation before voice capture", color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelSmall)
                        }
                        Switch(
                            checked = requireConsentPrompt,
                            onCheckedChange = { required ->
                                com.example.util.PrivacySecurityManager.setRequireRecordingConsentPrompt(context, required)
                            },
                            modifier = Modifier.testTag("settings_consent_prompt_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. Raw Audio Retention Policy Selector & Purge Control
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E293B).copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Raw Audio Retention Policy",
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        audioRetentionPolicy.subtitle,
                                        color = Color(0xFF94A3B8),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                Box {
                                    OutlinedButton(
                                        onClick = { showRetentionDropdown = true },
                                        border = BorderStroke(1.dp, RecallCyanSecondary),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("audio_retention_policy_button")
                                    ) {
                                        Text(audioRetentionPolicy.displayName, fontSize = 11.sp, color = RecallCyanSecondary, fontWeight = FontWeight.Bold)
                                    }
                                    DropdownMenu(
                                        expanded = showRetentionDropdown,
                                        onDismissRequest = { showRetentionDropdown = false },
                                        modifier = Modifier.background(Color(0xFF0F172A))
                                    ) {
                                        com.example.util.AudioRetentionPolicy.entries.forEach { policy ->
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(policy.displayName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                        Text(policy.subtitle, color = Color(0xFF94A3B8), fontSize = 11.sp)
                                                    }
                                                },
                                                onClick = {
                                                    showRetentionDropdown = false
                                                    com.example.util.PrivacySecurityManager.setAudioRetentionPolicy(context, policy)
                                                    audioStorageRefreshTick++
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "On-device raw audio: ${audioStorageStats.fileCount} file(s) • ${audioStorageStats.formattedSize()}",
                                    color = Color(0xFFCBD5E1),
                                    style = MaterialTheme.typography.labelSmall
                                )
                                TextButton(
                                    onClick = {
                                        val deleted = com.example.util.PrivacySecurityManager.purgeAllRawAudioFiles(context)
                                        audioStorageRefreshTick++
                                        viewModel.showFeedback("🧹 Purged $deleted raw audio file(s). Structured text memories preserved.")
                                    },
                                    modifier = Modifier.testTag("purge_raw_audio_button")
                                ) {
                                    Text("🧹 Purge Audio Now", color = Color(0xFFF59E0B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 4. Portable JSON Export / Restore
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                scope.launch {
                                    backupJsonContent = viewModel.repository.exportJsonBackup()
                                    com.example.util.PrivacySecurityManager.recordAuditEvent(
                                        context,
                                        "Exported JSON Backup",
                                        "Generated portable Unified Memory v5 JSON backup"
                                    )
                                    showBackupDialog = true
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RecallVioletPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export JSON", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = { showImportDialog = true },
                            border = BorderStroke(1.dp, RecallCyanSecondary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RecallCyanSecondary),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .testTag("import_json_backup_button")
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore JSON", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 5. Privacy & Security Audit Log Viewer
                    TextButton(
                        onClick = { showAuditLogsExpanded = !showAuditLogsExpanded },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("toggle_privacy_audit_log_button")
                    ) {
                        Text(
                            if (showAuditLogsExpanded) "▲ Hide Privacy & Security Audit Log (${privacyAuditLogs.size})"
                            else "🛡️ View Privacy & Security Audit Log (${privacyAuditLogs.size})",
                            color = RecallCyanSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (showAuditLogsExpanded) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF070B14),
                            border = BorderStroke(1.dp, Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (privacyAuditLogs.isEmpty()) {
                                    Text(
                                        "No security or retention events logged yet.",
                                        color = Color(0xFF64748B),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                } else {
                                    privacyAuditLogs.take(8).forEach { entry ->
                                        Column {
                                            Text(
                                                "• ${entry.action}",
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                "  ${entry.details}",
                                                color = Color(0xFF94A3B8),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    OutlinedButton(
                        onClick = { showClearConfirmDialog = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear All Local Data & Audio", fontSize = 13.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

    // Dialog: Change 4-Digit Security PIN
    if (showChangePinDialog) {
        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            containerColor = Color(0xFF0F172A),
            title = { Text("Update 4-Digit Vault PIN", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Enter a new 4-to-8 digit PIN. It is salted and hashed with SHA-256 on device.",
                        color = Color(0xFFCBD5E1),
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = newPinDraft,
                        onValueChange = { next ->
                            if (next.length <= 8 && next.all { it.isDigit() }) {
                                newPinDraft = next
                            }
                        },
                        label = { Text("New Numeric PIN", color = RecallCyanSecondary) },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (com.example.util.PrivacySecurityManager.updatePin(context, newPinDraft)) {
                            showChangePinDialog = false
                            viewModel.showFeedback("🔒 Vault PIN updated!")
                        } else {
                            Toast.makeText(context, "PIN must be at least 4 digits.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RecallVioletPrimary)
                ) {
                    Text("Save PIN", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // Dialog: JSON Backup Viewer & Copy
    if (showBackupDialog) {
        var showBackupMenu by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            containerColor = Color(0xFF0F172A),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text("Local Room Database Backup", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                UnifiedTaskbarInputBox(
                    borderColor = RecallVioletPrimary,
                    taskbarContent = {
                        Box {
                            TaskbarDropdownButton(
                                label = "Backup Actions",
                                accentColor = RecallVioletPrimary,
                                onClick = { showBackupMenu = true }
                            )
                            DropdownMenu(
                                expanded = showBackupMenu,
                                onDismissRequest = { showBackupMenu = false },
                                properties = PopupProperties(clippingEnabled = true),
                                modifier = Modifier
                                    .background(Color(0xFF0F172A))
                                    .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("📄 Copy JSON to Clipboard", color = Color.White, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        showBackupMenu = false
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Recall Backup", backupJsonContent))
                                        Toast.makeText(context, "Backup copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    },
                    bodyContent = {
                        OutlinedTextField(
                            value = backupJsonContent,
                            onValueChange = {},
                            readOnly = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                            colors = seamlessInnerTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        )
                    }
                )
            },
            confirmButton = {
                Button(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Recall Backup", backupJsonContent))
                    Toast.makeText(context, "Backup copied to clipboard!", Toast.LENGTH_SHORT).show()
                    showBackupDialog = false
                }) {
                    Text("Copy to Clipboard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupDialog = false }) { Text("Close") }
            }
        )
    }

    // Dialog: Import / Restore JSON Backup
    if (showImportDialog) {
        var showRestoreMenu by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            containerColor = Color(0xFF0F172A),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text("Restore from JSON", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                UnifiedTaskbarInputBox(
                    borderColor = RecallCyanSecondary,
                    taskbarContent = {
                        Box {
                            TaskbarDropdownButton(
                                label = "JSON Options",
                                accentColor = Color(0xFF0284C7),
                                onClick = { showRestoreMenu = true }
                            )
                            DropdownMenu(
                                expanded = showRestoreMenu,
                                onDismissRequest = { showRestoreMenu = false },
                                properties = PopupProperties(clippingEnabled = true),
                                modifier = Modifier
                                    .background(Color(0xFF0F172A))
                                    .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("📋 Paste from Clipboard", color = Color.White, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        showRestoreMenu = false
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                                        if (!clipText.isNullOrBlank()) {
                                            importJsonInput = clipText
                                        } else {
                                            Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🧹 Clear JSON Input", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        showRestoreMenu = false
                                        importJsonInput = ""
                                    }
                                )
                            }
                        }
                    },
                    bodyContent = {
                        OutlinedTextField(
                            value = importJsonInput,
                            onValueChange = { importJsonInput = it },
                            placeholder = { Text("Paste exported Recall JSON backup here...", color = Color(0xFF94A3B8)) },
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                            colors = seamlessInnerTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .testTag("restore_json_input")
                        )
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.importBackupJson(importJsonInput)
                        showImportDialog = false
                        importJsonInput = ""
                    },
                    enabled = importJsonInput.isNotBlank()
                ) {
                    Text("Restore Records")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Dialog: Confirm Clear All Data
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            containerColor = Color(0xFF0F172A),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFE2E8F0),
            title = { Text("Clear All Memories & Data?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently remove all stored notes, contacts, media, and action items so you can start with a completely fresh, empty database.", color = Color(0xFFE2E8F0)) },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirmDialog = false
                        viewModel.clearAllData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
