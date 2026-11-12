package com.example.ui.stream

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import java.io.File
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.MediaEntity
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.MediaType
import com.example.data.local.entity.PersonEntity
import com.example.data.model.NoteWithDetails
import com.example.ui.RecallViewModel
import com.example.ui.components.AudioPlayerCard
import com.example.ui.theme.AppVisualPreferences
import com.example.ui.theme.CategoryIdeas
import com.example.ui.theme.CategoryMeeting
import com.example.ui.theme.CategoryPersonal
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.example.ui.theme.CategoryWork
import com.example.ui.theme.RecallCyanSecondary
import com.example.ui.theme.RecallEmeraldAccent
import com.example.ui.theme.RecallVioletPrimary
import com.example.ui.theme.NeuralAppBackground
import com.example.ui.theme.NeonBrainIcon
import com.example.ui.theme.GlassCircleIconButton
import com.example.ui.settings.HelpAndFaqScreen
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import com.example.ui.theme.TaskbarDropdownButton
import com.example.ui.theme.UnifiedTaskbarInputBox
import com.example.ui.theme.VibrantTagBadge
import com.example.ui.theme.getVibrantTagColor
import com.example.ui.theme.seamlessInnerTextFieldColors
import com.example.ui.theme.translucentWhiteTextFieldColors
import com.example.data.model.MemoryCategory
import com.example.util.AppIntegrationsHelper
import com.example.util.DateUtils
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.ReminderTone
import com.example.data.local.entity.ReminderVibratePattern
import com.example.ui.components.ReminderCardItem
import com.example.ui.components.ReminderSchedulerDialog
import com.example.ui.components.RemindersListSheet
import com.example.ui.theme.OrbIconType
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamScreen(
    viewModel: RecallViewModel,
    onNavigateToPerson: (PersonEntity) -> Unit,
    onNavigateToDebrief: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToReminders: (() -> Unit)? = null
) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()

    val isSmartSearchActive by viewModel.isSmartSearchActive.collectAsStateWithLifecycle()
    val isSmartSearchLoading by viewModel.isSmartSearchLoading.collectAsStateWithLifecycle()
    val isSmartSearchEnabled by viewModel.isSmartSearchEnabled.collectAsStateWithLifecycle()
    val smartSearchInterpretation by viewModel.smartSearchInterpretation.collectAsStateWithLifecycle()
    val smartSearchResults by viewModel.smartSearchResults.collectAsStateWithLifecycle()
    val filteredSmartSearchResults by viewModel.filteredSmartSearchResults.collectAsStateWithLifecycle()
    val smartSearchFilter by viewModel.smartSearchFilter.collectAsStateWithLifecycle()
    val smartSearchAiSummary by viewModel.smartSearchAiSummary.collectAsStateWithLifecycle()

    val selectedCalendarDate by viewModel.selectedCalendarDate.collectAsStateWithLifecycle()
    val isCalendarExpanded by viewModel.isCalendarExpanded.collectAsStateWithLifecycle()
    val calendarMonthOffset by viewModel.calendarMonthOffset.collectAsStateWithLifecycle()
    val memoryDates by viewModel.memoryDates.collectAsStateWithLifecycle()
    val reminderDates by viewModel.reminderDates.collectAsStateWithLifecycle()
    val remindersForSelectedDate by viewModel.remindersForSelectedDate.collectAsStateWithLifecycle()
    val notesCount by viewModel.notesCount.collectAsStateWithLifecycle()
    val peopleCount by viewModel.peopleCount.collectAsStateWithLifecycle()
    val activeReminders by viewModel.activeReminders.collectAsStateWithLifecycle()
    val allReminders by viewModel.allReminders.collectAsStateWithLifecycle()
    val allPeople by viewModel.people.collectAsStateWithLifecycle()
    val allActions by viewModel.actionItems.collectAsStateWithLifecycle()
    val allCommitments by viewModel.allCommitments.collectAsStateWithLifecycle()
    val allProjects by viewModel.allProjects.collectAsStateWithLifecycle()
    val allPlaces by viewModel.allPlaces.collectAsStateWithLifecycle()
    val allMemoryLinks by viewModel.allMemoryLinks.collectAsStateWithLifecycle()
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val overdueContacts by viewModel.overdueContacts.collectAsStateWithLifecycle()
    val unverifiedMemories by viewModel.unverifiedMemories.collectAsStateWithLifecycle()

    val anticipatoryBriefing = remember(notes, allPeople, allActions, allCommitments, allProjects) {
        com.example.util.MemoryIntelligenceHelper.buildAnticipatoryBriefing(
            allNotes = notes,
            allPeople = allPeople,
            allActions = allActions,
            allCommitments = allCommitments,
            allProjects = allProjects
        )
    }
    var isProactiveBriefingExpanded by remember { mutableStateOf(false) }
    var proactiveReminderDraft by remember { mutableStateOf<Triple<String, String, String?>?>(null) }

    var showRemindersSheet by remember { mutableStateOf(false) }
    var showNewReminderDialog by remember { mutableStateOf(false) }
    var reminderToEdit by remember { mutableStateOf<ReminderEntity?>(null) }
    var showIntelligenceHub by remember { mutableStateOf(false) }
    var intelligenceHubTab by remember { mutableStateOf(0) }
    var showHelpGuideOverlay by remember { mutableStateOf(false) }

    var selectedMediaForDialog by remember { mutableStateOf<MediaEntity?>(null) }
    val context = LocalContext.current

    val scope = rememberCoroutineScope()
    var isDictatingSearch by remember { mutableStateOf(false) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.audioRecorder.startRecording(scope)
            isDictatingSearch = true
        } else {
            viewModel.showFeedback("Microphone permission required for voice search.")
        }
    }

    fun toggleVoiceSearchSafely() {
        if (isDictatingSearch) {
            val (_, liveText) = viewModel.audioRecorder.stopRecording()
            isDictatingSearch = false
            if (liveText.isNotBlank()) {
                viewModel.onSearchQueryChanged(liveText)
            }
        } else {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                viewModel.audioRecorder.startRecording(scope)
                isDictatingSearch = true
            } else {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    var showFabMenu by remember { mutableStateOf(false) }
    var noteToDelete by remember { mutableStateOf<NoteEntity?>(null) }

    // Camera capture launcher for Quick Capture
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            viewModel.processCameraCapture(bitmap)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                cameraLauncher.launch()
            } catch (e: Throwable) {
                viewModel.showFeedback("Could not open camera: ${e.message}")
            }
        } else {
            viewModel.showFeedback("Camera permission is needed to take photos. You can also pick from gallery.")
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.processImageUriCapture(uri)
        }
    }

    fun launchCameraSafely() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            try {
                cameraLauncher.launch()
            } catch (e: Throwable) {
                viewModel.showFeedback("Could not open camera: ${e.message}")
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Filter categories matching the specification
    val categories = listOf(
        "All", "Favorites", "Meeting", "Work", "Projects", "Ideas", "Personal", "Finance",
        "Health", "Travel", "Family", "Learning", "Journal", "Visuals", "Clients",
        "Code & Tech", "Shopping", "Habits"
    )
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    NeuralAppBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
            ) {
                // 1. Sleek Top Bar (Logo + Title + Action Icons)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            NeonBrainIcon(size = 38.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Recall",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Personal Memory AI",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RecallCyanSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Intelligence Hub Button
                            GlassCircleIconButton(
                                onClick = {
                                    intelligenceHubTab = 0
                                    showIntelligenceHub = true
                                },
                                icon = Icons.Default.AutoAwesome,
                                contentDescription = "Intelligence Hub",
                                size = 36.dp
                            )

                            // Help & User Guide Button
                            GlassCircleIconButton(
                                onClick = {
                                    showHelpGuideOverlay = true
                                },
                                icon = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = "Help & User Guide",
                                size = 36.dp
                            )

                            // Profile / Settings Gear icon
                            GlassCircleIconButton(
                                onClick = onNavigateToSettings,
                                icon = Icons.Default.Settings,
                                contentDescription = "Settings",
                                size = 36.dp
                            )
                        }
                    }
                }

                // 1B. Quick Intelligence, Memory Graph & Geotag Places Bar
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFF0F172A).copy(alpha = 0.92f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.65f)),
                            modifier = Modifier
                                .clickable {
                                    intelligenceHubTab = 0
                                    showIntelligenceHub = true
                                }
                                .testTag("open_weekly_digest_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📊 Digest & Habits", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            color = Color(0xFF0F172A).copy(alpha = 0.92f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, RecallVioletPrimary.copy(alpha = 0.65f)),
                            modifier = Modifier
                                .clickable {
                                    intelligenceHubTab = 1
                                    showIntelligenceHub = true
                                }
                                .testTag("open_memory_graph_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🕸️ Memory Graph", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            color = Color(0xFF0F172A).copy(alpha = 0.92f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, RecallEmeraldAccent.copy(alpha = 0.65f)),
                            modifier = Modifier
                                .clickable {
                                    intelligenceHubTab = 2
                                    showIntelligenceHub = true
                                }
                                .testTag("open_places_map_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📍 Places & Geotags", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (overdueContacts.isNotEmpty()) {
                            Surface(
                                color = Color(0xFF0F172A).copy(alpha = 0.92f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.75f)),
                                modifier = Modifier.clickable {
                                    onNavigateToPerson(overdueContacts.first())
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🔔 Keep in Touch (${overdueContacts.size})",
                                        color = Color(0xFFF59E0B),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Unified Search & Filter Input Box with Internal Top Taskbar Dropdowns
                item {
                    var searchModeDropdownExpanded by remember { mutableStateOf(false) }
                    var calendarDropdownExpanded by remember { mutableStateOf(false) }
                    val activeCatColor = getVibrantTagColor(selectedCategory)

                    UnifiedTaskbarInputBox(
                        shape = RoundedCornerShape(20.dp),
                        borderColor = if (isSmartSearchEnabled) RecallVioletPrimary else Color(0xFF334155),
                        taskbarContent = {
                            // (A) Category / Tag Dropdown inside Search Taskbar
                            Box {
                                TaskbarDropdownButton(
                                    label = "Tag: $selectedCategory",
                                    dotColor = activeCatColor,
                                    accentColor = activeCatColor,
                                    onClick = { categoryDropdownExpanded = true },
                                    modifier = Modifier.testTag("category_dropdown_selector")
                                )
                                DropdownMenu(
                                    expanded = categoryDropdownExpanded,
                                    onDismissRequest = { categoryDropdownExpanded = false },
                                    properties = PopupProperties(clippingEnabled = true),
                                    modifier = Modifier
                                        .background(Color(0xFF0F172A))
                                        .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                                        .height(340.dp)
                                ) {
                                    categories.forEach { cat ->
                                        val isSelected = selectedCategory == cat
                                        val catColor = getVibrantTagColor(cat)
                                        val catIcon = when (cat) {
                                             "All" -> Icons.Default.ViewStream
                                             "Favorites" -> Icons.Default.Star
                                             else -> MemoryCategory.fromString(cat).icon
                                        }
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(catColor)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = cat,
                                                        color = if (isSelected) catColor else Color.White,
                                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                                                    )
                                                }
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = catIcon,
                                                    contentDescription = null,
                                                    tint = catColor,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            trailingIcon = {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = catColor,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            },
                                            onClick = {
                                                viewModel.selectedCategory.value = cat
                                                categoryDropdownExpanded = false
                                            },
                                            modifier = Modifier.testTag("filter_chip_$cat")
                                        )
                                    }
                                }
                            }

                            // (B) Search Mode & Voice Dropdown inside Search Taskbar
                            Box {
                                TaskbarDropdownButton(
                                    label = if (isSmartSearchEnabled) "AI NLU Search" else "Instant Search",
                                    icon = Icons.Default.GraphicEq,
                                    accentColor = if (isSmartSearchEnabled) RecallVioletPrimary else Color(0xFF0284C7),
                                    onClick = { searchModeDropdownExpanded = true }
                                )
                                DropdownMenu(
                                    expanded = searchModeDropdownExpanded,
                                    onDismissRequest = { searchModeDropdownExpanded = false },
                                    properties = PopupProperties(clippingEnabled = true),
                                    modifier = Modifier
                                        .background(Color(0xFF0F172A))
                                        .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "⚡ Instant Keyword Search",
                                                color = Color.White,
                                                fontWeight = if (!isSmartSearchEnabled) FontWeight.ExtraBold else FontWeight.Medium
                                            )
                                        },
                                        trailingIcon = {
                                            if (!isSmartSearchEnabled) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = RecallVioletPrimary, modifier = Modifier.size(16.dp))
                                            }
                                        },
                                        onClick = {
                                            if (isSmartSearchEnabled) viewModel.toggleSmartSearch()
                                            searchModeDropdownExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "🧠 Smart AI NLU Search",
                                                color = RecallVioletPrimary,
                                                fontWeight = if (isSmartSearchEnabled) FontWeight.ExtraBold else FontWeight.Medium
                                            )
                                        },
                                        trailingIcon = {
                                            if (isSmartSearchEnabled) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = RecallVioletPrimary, modifier = Modifier.size(16.dp))
                                            }
                                        },
                                        onClick = {
                                            if (!isSmartSearchEnabled) viewModel.toggleSmartSearch()
                                            searchModeDropdownExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("🎙️ Dictate Voice Search", color = Color.White, fontWeight = FontWeight.Medium) },
                                        leadingIcon = { Icon(Icons.Default.Mic, contentDescription = null, tint = RecallVioletPrimary) },
                                        onClick = {
                                            searchModeDropdownExpanded = false
                                            scope.launch {
                                                delay(60)
                                                toggleVoiceSearchSafely()
                                            }
                                        }
                                    )
                                    if (searchQuery.isNotEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("🧹 Clear Search Query", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold) },
                                            leadingIcon = { Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFDC2626)) },
                                            onClick = {
                                                searchModeDropdownExpanded = false
                                                viewModel.clearSmartSearch()
                                            }
                                        )
                                    }
                                }
                            }

                            // (C) Calendar / Date Filter Dropdown inside Search Taskbar
                            Box {
                                TaskbarDropdownButton(
                                    label = if (selectedCalendarDate != null) DateUtils.formatDayMonth(selectedCalendarDate!!) else "Calendar",
                                    icon = Icons.Default.CalendarToday,
                                    accentColor = if (isCalendarExpanded || selectedCalendarDate != null) RecallVioletPrimary else Color(0xFF0D9488),
                                    onClick = { calendarDropdownExpanded = true }
                                )
                                DropdownMenu(
                                    expanded = calendarDropdownExpanded,
                                    onDismissRequest = { calendarDropdownExpanded = false },
                                    properties = PopupProperties(clippingEnabled = true),
                                    modifier = Modifier
                                        .background(Color(0xFF0F172A))
                                        .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (isCalendarExpanded) "📅 Hide Calendar Grid" else "📅 Show Calendar Grid",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        },
                                        onClick = {
                                            calendarDropdownExpanded = false
                                            viewModel.toggleCalendarExpanded()
                                        }
                                    )
                                    if (selectedCalendarDate != null) {
                                        DropdownMenuItem(
                                            text = { Text("🧹 Clear Date Filter", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold) },
                                            onClick = {
                                                calendarDropdownExpanded = false
                                                viewModel.selectCalendarDate(null)
                                            }
                                        )
                                    }
                                }
                            }

                            // (D) Direct Reminders Access Button inside Search Taskbar
                            Box {
                                TaskbarDropdownButton(
                                    label = if (activeReminders.isNotEmpty()) "Alarms (${activeReminders.size})" else "Reminders",
                                    icon = if (activeReminders.isNotEmpty()) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                    accentColor = if (activeReminders.isNotEmpty()) Color(0xFFF59E0B) else Color(0xFF38BDF8),
                                    onClick = {
                                        onNavigateToReminders?.invoke() ?: run { showRemindersSheet = true }
                                    },
                                    modifier = Modifier.testTag("taskbar_reminders_btn")
                                )
                            }
                        },
                        bodyContent = {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.onSearchQueryChanged(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("memory_search_bar"),
                                placeholder = {
                                    Text(
                                        if (isSmartSearchEnabled) "Ask anything: “Meeting with John…”"
                                        else "Search memories, facts, meetings…",
                                        fontSize = 13.sp,
                                        color = Color(0xFF475569),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .padding(start = 4.dp)
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(RecallVioletPrimary.copy(alpha = 0.14f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Search",
                                            tint = RecallVioletPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                trailingIcon = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(end = 6.dp)
                                    ) {
                                        IconButton(
                                            onClick = { toggleVoiceSearchSafely() },
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(CircleShape)
                                                .background(RecallVioletPrimary.copy(alpha = 0.14f))
                                                .testTag("search_voice_btn")
                                        ) {
                                            Icon(
                                                imageVector = if (isDictatingSearch) Icons.Default.MicOff else Icons.Default.Mic,
                                                contentDescription = "Voice Search",
                                                tint = if (isDictatingSearch) Color(0xFFEF4444) else RecallVioletPrimary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))

                                        IconButton(
                                            onClick = { viewModel.toggleSmartSearch() },
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSmartSearchEnabled) RecallVioletPrimary.copy(alpha = 0.2f)
                                                    else Color(0xFFE2E8F0)
                                                )
                                                .testTag("search_smart_toggle")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.GraphicEq,
                                                contentDescription = "Smart NLU Search",
                                                tint = if (isSmartSearchEnabled) RecallVioletPrimary else Color(0xFF475569),
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }

                                        if (searchQuery.isNotEmpty()) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            IconButton(
                                                onClick = { viewModel.clearSmartSearch() },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Clear search", tint = Color(0xFF94A3B8), modifier = Modifier.size(15.dp))
                                            }
                                        }
                                    }
                                },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                ),
                                colors = seamlessInnerTextFieldColors()
                            )
                        }
                    )
                }

                if (searchQuery.isBlank()) {

                    // 4. Interactive Memory Calendar (displayed cleanly when toggled or date-filtered)
                    if (isCalendarExpanded || selectedCalendarDate != null) {
                        item {
                            InteractiveMemoryCalendar(
                                selectedDate = selectedCalendarDate,
                                isExpanded = isCalendarExpanded,
                                monthOffset = calendarMonthOffset,
                                memoryDates = memoryDates,
                                reminderDates = reminderDates,
                                entriesCountOnSelectedDate = notes.size,
                                remindersOnSelectedDate = remindersForSelectedDate,
                                onSelectDate = { viewModel.selectCalendarDate(it) },
                                onToggleExpand = { viewModel.toggleCalendarExpanded() },
                                onChangeMonth = { viewModel.changeCalendarMonth(it) },
                                onResetToday = { viewModel.resetCalendarToToday() },
                                onClearDate = { viewModel.clearCalendarFilter() },
                                onOpenReminders = { onNavigateToReminders?.invoke() ?: run { showRemindersSheet = true } },
                                onToggleReminderCompleted = { rem, completed ->
                                    viewModel.toggleReminderCompleted(rem, completed)
                                },
                                onAddNewReminderForDate = {
                                    showNewReminderDialog = true
                                }
                            )
                        }

                        if (remindersForSelectedDate.isNotEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF0F172A).copy(alpha = 0.88f),
                                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFF59E0B).copy(alpha = 0.65f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.NotificationsActive,
                                                contentDescription = null,
                                                tint = Color(0xFFF59E0B),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Scheduled Reminders for this Day (${remindersForSelectedDate.size})",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFFDE68A),
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "All Reminders →",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = RecallCyanSecondary,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.clickable {
                                                    onNavigateToReminders?.invoke() ?: run { showRemindersSheet = true }
                                                }
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        val dayTimeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
                                        remindersForSelectedDate.forEach { rem ->
                                            ReminderCardItem(
                                                reminder = rem,
                                                timeFormatter = dayTimeFormatter,
                                                onClick = { reminderToEdit = rem },
                                                onToggleCompleted = { viewModel.toggleReminderCompleted(rem, !rem.isCompleted) },
                                                onSnooze = { viewModel.snoozeReminder(rem.id, 10) },
                                                onDelete = { viewModel.deleteReminder(rem.id) },
                                                onPreviewTone = { tone: ReminderTone, vibrate: ReminderVibratePattern ->
                                                    viewModel.previewReminderTone(tone, vibrate)
                                                }
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5. Clean, Fresh Section Title Row (No Overlapping Elements)
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (selectedCalendarDate != null)
                                    "Memories: ${DateUtils.formatDayMonth(selectedCalendarDate!!)}"
                                else if (selectedCategory != "All") "$selectedCategory Memories"
                                else "Memory Stream",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF0B1120).copy(alpha = 0.72f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.45f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "$notesCount memories",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = RecallCyanSecondary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Interactive Reminders Access Chip
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (activeReminders.isNotEmpty()) Color(0xFF1E293B).copy(alpha = 0.95f) else Color(0xFF0B1120).copy(alpha = 0.72f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (activeReminders.isNotEmpty()) Color(0xFFF59E0B) else RecallCyanSecondary.copy(alpha = 0.45f)
                                ),
                                modifier = Modifier
                                    .padding(start = 6.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onNavigateToReminders?.invoke() ?: run { showRemindersSheet = true } }
                                    .testTag("stream_reminders_chip")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (activeReminders.isNotEmpty()) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                        contentDescription = "Reminders",
                                        tint = if (activeReminders.isNotEmpty()) Color(0xFFF59E0B) else RecallCyanSecondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (activeReminders.isNotEmpty()) "${activeReminders.size} alarms" else "Reminders",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (activeReminders.isNotEmpty()) Color(0xFFF59E0B) else RecallCyanSecondary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    // 5A-2. Phase 5 Proactive AI Assistant • Anticipatory Briefing Hub
                    if (anticipatoryBriefing.insights.isNotEmpty() && selectedCalendarDate == null && selectedCategory == "All") {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .testTag("proactive_assistant_briefing_card"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF0F172A).copy(alpha = 0.88f)
                                ),
                                border = BorderStroke(
                                    1.2.dp,
                                    Brush.horizontalGradient(
                                        listOf(
                                            RecallCyanSecondary.copy(alpha = 0.8f),
                                            RecallVioletPrimary.copy(alpha = 0.7f),
                                            Color(0xFF10B981).copy(alpha = 0.7f)
                                        )
                                    )
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "⚡ ${anticipatoryBriefing.greetingTitle}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = RecallCyanSecondary.copy(alpha = 0.18f),
                                            border = BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.6f)),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    com.example.util.NotificationHelper.showProactiveBriefingNotification(
                                                        context = context,
                                                        title = "⚡ ${anticipatoryBriefing.greetingTitle}",
                                                        summaryText = anticipatoryBriefing.executiveSummary
                                                    )
                                                    viewModel.showFeedback("🔔 Proactive Briefing sent to your lock screen!")
                                                }
                                                .testTag("proactive_briefing_notify_btn")
                                        ) {
                                            Text(
                                                text = "🔔 Alert",
                                                color = RecallCyanSecondary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                                            border = BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.6f)),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    intelligenceHubTab = 1
                                                    showIntelligenceHub = true
                                                }
                                                .testTag("proactive_briefing_patterns_btn")
                                        ) {
                                            Text(
                                                text = "🧬 Patterns",
                                                color = Color(0xFFFBBF24),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = RecallVioletPrimary.copy(alpha = 0.22f),
                                            border = BorderStroke(1.dp, RecallVioletPrimary.copy(alpha = 0.6f)),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { isProactiveBriefingExpanded = !isProactiveBriefingExpanded }
                                                .testTag("proactive_briefing_expand_btn")
                                        ) {
                                            Text(
                                                text = if (isProactiveBriefingExpanded) "Hide (${anticipatoryBriefing.insights.size})" else "View (${anticipatoryBriefing.insights.size})",
                                                color = Color(0xFFC4B5FD),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = anticipatoryBriefing.executiveSummary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 12.sp
                                    )

                                    // Compact metric pills row
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (anticipatoryBriefing.openCommitmentsCount > 0) {
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = Color(0xFF10B981).copy(alpha = 0.16f),
                                                border = BorderStroke(1.dp, Color(0xFF34D399).copy(alpha = 0.5f))
                                            ) {
                                                Text(
                                                    text = "🤝 ${anticipatoryBriefing.openCommitmentsCount} Promises",
                                                    color = Color(0xFF34D399),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                        if (anticipatoryBriefing.upcomingDatesCount > 0) {
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = Color(0xFFF59E0B).copy(alpha = 0.16f),
                                                border = BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.5f))
                                            ) {
                                                Text(
                                                    text = "🎂 ${anticipatoryBriefing.upcomingDatesCount} Dates",
                                                    color = Color(0xFFFBBF24),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                        if (anticipatoryBriefing.overduePeopleCount > 0) {
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = RecallCyanSecondary.copy(alpha = 0.16f),
                                                border = BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.5f))
                                            ) {
                                                Text(
                                                    text = "👋 ${anticipatoryBriefing.overduePeopleCount} Catch-Ups",
                                                    color = RecallCyanSecondary,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                        if (anticipatoryBriefing.activeProjectsCount > 0) {
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = RecallVioletPrimary.copy(alpha = 0.18f),
                                                border = BorderStroke(1.dp, RecallVioletPrimary.copy(alpha = 0.5f))
                                            ) {
                                                Text(
                                                    text = "🚀 ${anticipatoryBriefing.activeProjectsCount} Active Projects",
                                                    color = Color(0xFFC4B5FD),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }

                                    AnimatedVisibility(visible = isProactiveBriefingExpanded) {
                                        Column(modifier = Modifier.padding(top = 10.dp)) {
                                            anticipatoryBriefing.insights.take(6).forEach { insight ->
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = Color(0xFF1E293B).copy(alpha = 0.9f),
                                                    border = BorderStroke(1.dp, Color(0xFF334155)),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 3.dp)
                                                ) {
                                                    Column(modifier = Modifier.padding(10.dp)) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(
                                                                text = "${insight.badgeIcon} ${insight.title}",
                                                                style = MaterialTheme.typography.labelLarge,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White,
                                                                modifier = Modifier.weight(1f),
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = insight.subtitle,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = Color(0xFFCBD5E1),
                                                            fontSize = 11.sp,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Spacer(modifier = Modifier.height(6.dp))
                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            when (insight.type) {
                                                                com.example.util.ProactiveInsightType.OPEN_COMMITMENT -> {
                                                                    insight.commitment?.let { comm ->
                                                                        Surface(
                                                                            shape = RoundedCornerShape(6.dp),
                                                                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                                                                            border = BorderStroke(1.dp, Color(0xFF34D399)),
                                                                            modifier = Modifier
                                                                                .clip(RoundedCornerShape(6.dp))
                                                                                .clickable {
                                                                                    viewModel.toggleCommitmentCompleted(comm.id, true)
                                                                                }
                                                                        ) {
                                                                            Text(
                                                                                text = "✓ Done",
                                                                                color = Color(0xFF34D399),
                                                                                fontSize = 10.sp,
                                                                                fontWeight = FontWeight.Bold,
                                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                                            )
                                                                        }
                                                                        Surface(
                                                                            shape = RoundedCornerShape(6.dp),
                                                                            color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                                                                            border = BorderStroke(1.dp, Color(0xFFFBBF24)),
                                                                            modifier = Modifier
                                                                                .clip(RoundedCornerShape(6.dp))
                                                                                .clickable {
                                                                                    proactiveReminderDraft = Triple(
                                                                                        "Promise: ${comm.description.take(40)}",
                                                                                        comm.description,
                                                                                        comm.noteId
                                                                                    )
                                                                                }
                                                                        ) {
                                                                            Text(
                                                                                text = "🔔 Remind",
                                                                                color = Color(0xFFFBBF24),
                                                                                fontSize = 10.sp,
                                                                                fontWeight = FontWeight.Bold,
                                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                                com.example.util.ProactiveInsightType.UPCOMING_DATE -> {
                                                                    Surface(
                                                                        shape = RoundedCornerShape(6.dp),
                                                                        color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                                                                        border = BorderStroke(1.dp, Color(0xFFFBBF24)),
                                                                        modifier = Modifier
                                                                            .clip(RoundedCornerShape(6.dp))
                                                                            .clickable {
                                                                                proactiveReminderDraft = Triple(
                                                                                    insight.title,
                                                                                    insight.subtitle,
                                                                                    insight.noteId
                                                                                )
                                                                            }
                                                                    ) {
                                                                        Text(
                                                                            text = "🔔 Set Alarm",
                                                                            color = Color(0xFFFBBF24),
                                                                            fontSize = 10.sp,
                                                                            fontWeight = FontWeight.Bold,
                                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                                        )
                                                                    }
                                                                    insight.person?.let { p ->
                                                                        Surface(
                                                                            shape = RoundedCornerShape(6.dp),
                                                                            color = RecallCyanSecondary.copy(alpha = 0.18f),
                                                                            border = BorderStroke(1.dp, RecallCyanSecondary),
                                                                            modifier = Modifier
                                                                                .clip(RoundedCornerShape(6.dp))
                                                                                .clickable { onNavigateToPerson(p) }
                                                                        ) {
                                                                            Text(
                                                                                text = "👤 Dossier",
                                                                                color = RecallCyanSecondary,
                                                                                fontSize = 10.sp,
                                                                                fontWeight = FontWeight.Bold,
                                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                                com.example.util.ProactiveInsightType.DORMANT_RELATIONSHIP -> {
                                                                    insight.person?.let { p ->
                                                                        Surface(
                                                                            shape = RoundedCornerShape(6.dp),
                                                                            color = RecallCyanSecondary.copy(alpha = 0.2f),
                                                                            border = BorderStroke(1.dp, RecallCyanSecondary),
                                                                            modifier = Modifier
                                                                                .clip(RoundedCornerShape(6.dp))
                                                                                .clickable { viewModel.startCatchUpNoteForPerson(p) }
                                                                        ) {
                                                                            Text(
                                                                                text = "💬 Catch-Up Note",
                                                                                color = RecallCyanSecondary,
                                                                                fontSize = 10.sp,
                                                                                fontWeight = FontWeight.Bold,
                                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                                            )
                                                                        }
                                                                        Surface(
                                                                            shape = RoundedCornerShape(6.dp),
                                                                            color = RecallVioletPrimary.copy(alpha = 0.2f),
                                                                            border = BorderStroke(1.dp, RecallVioletPrimary),
                                                                            modifier = Modifier
                                                                                .clip(RoundedCornerShape(6.dp))
                                                                                .clickable { onNavigateToPerson(p) }
                                                                        ) {
                                                                            Text(
                                                                                text = "👤 Dossier",
                                                                                color = Color(0xFFC4B5FD),
                                                                                fontSize = 10.sp,
                                                                                fontWeight = FontWeight.Bold,
                                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                                com.example.util.ProactiveInsightType.PROJECT_MOMENTUM -> {
                                                                    insight.project?.let { proj ->
                                                                        Surface(
                                                                            shape = RoundedCornerShape(6.dp),
                                                                            color = RecallVioletPrimary.copy(alpha = 0.22f),
                                                                            border = BorderStroke(1.dp, RecallVioletPrimary),
                                                                            modifier = Modifier
                                                                                .clip(RoundedCornerShape(6.dp))
                                                                                .clickable { viewModel.startNoteForProject(proj) }
                                                                        ) {
                                                                            Text(
                                                                                text = "+ Project Note",
                                                                                color = Color(0xFFC4B5FD),
                                                                                fontSize = 10.sp,
                                                                                fontWeight = FontWeight.Bold,
                                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                                com.example.util.ProactiveInsightType.PENDING_TASK -> {
                                                                    insight.actionItem?.let { act ->
                                                                        Surface(
                                                                            shape = RoundedCornerShape(6.dp),
                                                                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                                                                            border = BorderStroke(1.dp, Color(0xFF34D399)),
                                                                            modifier = Modifier
                                                                                .clip(RoundedCornerShape(6.dp))
                                                                                .clickable { viewModel.toggleActionItem(act.id, true) }
                                                                        ) {
                                                                            Text(
                                                                                text = "✓ Complete",
                                                                                color = Color(0xFF34D399),
                                                                                fontSize = 10.sp,
                                                                                fontWeight = FontWeight.Bold,
                                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                                            )
                                                                        }
                                                                        Surface(
                                                                            shape = RoundedCornerShape(6.dp),
                                                                            color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                                                                            border = BorderStroke(1.dp, Color(0xFFFBBF24)),
                                                                            modifier = Modifier
                                                                                .clip(RoundedCornerShape(6.dp))
                                                                                .clickable {
                                                                                    proactiveReminderDraft = Triple(
                                                                                        "Task: ${act.task.take(40)}",
                                                                                        act.task,
                                                                                        act.noteId
                                                                                    )
                                                                                }
                                                                        ) {
                                                                            Text(
                                                                                text = "🔔 Remind",
                                                                                color = Color(0xFFFBBF24),
                                                                                fontSize = 10.sp,
                                                                                fontWeight = FontWeight.Bold,
                                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5B. AI Interpretations Awaiting Verification (Phase 2 Provenance & Fact Verification)
                    if (unverifiedMemories.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .testTag("unverified_ai_memories_banner"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF451A03).copy(alpha = 0.45f)
                                ),
                                border = BorderStroke(1.2.dp, Color(0xFFF59E0B).copy(alpha = 0.75f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "🤖 AI Interpretations Awaiting Review (${unverifiedMemories.size})",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFFBBF24),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Recall never turns uncertain AI inferences into facts without your approval.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFFDE68A)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    unverifiedMemories.take(3).forEach { unverified ->
                                        val confPct = (unverified.note.confidenceScore.coerceIn(0.1f, 1.0f) * 100).toInt()
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFF0F172A).copy(alpha = 0.85f),
                                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = unverified.note.title,
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        modifier = Modifier.weight(1f),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = Color(0xFFF59E0B).copy(alpha = 0.2f)
                                                    ) {
                                                        Text(
                                                            text = "$confPct% conf",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color(0xFFFBBF24),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Text(
                                                    text = unverified.note.content,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFFE2E8F0),
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = Color(0xFF10B981).copy(alpha = 0.22f),
                                                        border = BorderStroke(1.dp, Color(0xFF34D399)),
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .clickable { viewModel.verifyMemoryFact(unverified.note) }
                                                            .testTag("verify_fact_btn_${unverified.note.id}")
                                                    ) {
                                                        Text(
                                                            text = "✓ Confirm Fact",
                                                            color = Color(0xFF34D399),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                        )
                                                    }
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = RecallCyanSecondary.copy(alpha = 0.16f),
                                                        border = BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.6f)),
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .clickable { viewModel.openNoteEditor(unverified) }
                                                    ) {
                                                        Text(
                                                            text = "✎ Edit",
                                                            color = RecallCyanSecondary,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                        )
                                                    }
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = Color(0xFFEF4444).copy(alpha = 0.16f),
                                                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .clickable { noteToDelete = unverified.note }
                                                    ) {
                                                        Text(
                                                            text = "✕ Dismiss",
                                                            color = Color(0xFFF87171),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 6. Memory Stream Feed Cards
                    if (notes.isEmpty()) {
                        item {
                            if (selectedCalendarDate != null || selectedCategory != "All") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 36.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF0B1120).copy(alpha = 0.7f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.4f)),
                                            modifier = Modifier.size(64.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Search,
                                                    contentDescription = null,
                                                    tint = RecallCyanSecondary,
                                                    modifier = Modifier.size(30.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Text(
                                            text = if (selectedCalendarDate != null)
                                                "No memories on ${DateUtils.formatDayMonth(selectedCalendarDate!!)}"
                                            else "No memories in \"$selectedCategory\"",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (selectedCalendarDate != null)
                                                "Tap 'Clear filter' in Calendar to view all memories."
                                            else "Switch category back to 'All' or tap '+' to create one.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFCBD5E1)
                                        )
                                    }
                                }
                            } else {
                                // Beginner-Friendly Quick-Start & Living Notes Card
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .testTag("stream_beginner_welcome_card"),
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.92f)),
                                    border = BorderStroke(1.2.dp, RecallCyanSecondary.copy(alpha = 0.6f))
                                ) {
                                    Column(modifier = Modifier.padding(18.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            NeonBrainIcon(size = 38.dp)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Welcome to Recall",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = "Your self-updating personal memory & reminder assistant",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = RecallCyanSecondary
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { viewModel.openNoteEditor(null) },
                                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                                    containerColor = RecallVioletPrimary,
                                                    contentColor = Color.White
                                                ),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Write Note", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }

                                            Button(
                                                onClick = onNavigateToDebrief,
                                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF1E293B),
                                                    contentColor = Color.White
                                                ),
                                                border = BorderStroke(1.dp, RecallCyanSecondary),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Mic, contentDescription = null, tint = RecallCyanSecondary, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Voice Debrief", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    reminderToEdit = null
                                                    showNewReminderDialog = true
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Set Reminder", color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }

                                            TextButton(
                                                onClick = { showHelpGuideOverlay = true },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = RecallCyanSecondary, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("User Guide & FAQ", color = RecallCyanSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        items(notes, key = { it.note.id }) { item ->
                            SwipeableMemoryFeedCard(
                                item = item,
                                onClick = { viewModel.openNoteEditor(item) },
                                onPersonClick = onNavigateToPerson,
                                onTogglePin = { viewModel.togglePin(item.note) },
                                onDelete = { noteToDelete = item.note }
                            )
                        }
                    }
                } else {
                    // --- Smart Search Mode Active ---
                    // NLU Interpretation Card
                    smartSearchInterpretation?.let { nlu ->
                        item {
                            SmartSearchInterpretationCard(
                                interpretation = nlu,
                                isLoading = isSmartSearchLoading,
                                aiSummary = smartSearchAiSummary
                            )
                        }
                    }

                    // Filter Tabs (All, Notes, Photos, People, Tasks)
                    item {
                        SmartSearchFilterTabs(
                            selectedFilter = smartSearchFilter,
                            items = smartSearchResults,
                            onSelectFilter = { viewModel.setSmartSearchFilter(it) }
                        )
                    }

                    if (filteredSmartSearchResults.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    if (isSmartSearchLoading) {
                                        CircularProgressIndicator(
                                            color = RecallCyanSecondary,
                                            modifier = Modifier.size(36.dp),
                                            strokeWidth = 3.dp
                                        )
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Text(
                                            text = "Reasoning through personal memories…",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                    } else {
                                        Text(
                                            text = "No memories found matching “$searchQuery”",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Try searching by person, keywords, or time range.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFCBD5E1)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        items(filteredSmartSearchResults, key = { it.id }) { item ->
                            SmartSearchResultCard(
                                item = item,
                                onClickNote = { noteWithDetails ->
                                    viewModel.openNoteEditor(noteWithDetails)
                                },
                                onClickPhoto = { media ->
                                    selectedMediaForDialog = media
                                },
                                onClickPerson = { person ->
                                    onNavigateToPerson(person)
                                },
                                onToggleAction = { action ->
                                    viewModel.toggleActionItem(action.id, !action.isCompleted)
                                },
                                onToggleCommitment = { commitment ->
                                    viewModel.toggleCommitmentCompleted(commitment.id, !commitment.isCompleted)
                                }
                            )
                        }
                    }
                }
            }

        // Expandable Speed-Dial FAB Menu at Bottom Right
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AnimatedVisibility(visible = showFabMenu, enter = fadeIn(), exit = fadeOut()) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Action 1: Voice Debrief
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.95f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RecallVioletPrimary.copy(alpha = 0.6f)),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                showFabMenu = false
                                onNavigateToDebrief()
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "Voice Debrief",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(RecallVioletPrimary, Color(0xFF4338CA))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice Debrief",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Action 2: Camera Capture OCR
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.95f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.6f)),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                showFabMenu = false
                                launchCameraSafely()
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "Camera OCR",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(RecallCyanSecondary, Color(0xFF0891B2))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Camera OCR",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Action 3: Photo Gallery Pick
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.95f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f)),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                showFabMenu = false
                                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "Photo Gallery",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(Color(0xFFF59E0B), Color(0xFFB45309))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Collections,
                                    contentDescription = "Photo Gallery",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Action 4: New Written Note
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.95f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RecallVioletPrimary.copy(alpha = 0.6f)),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                showFabMenu = false
                                viewModel.openNoteEditor(null)
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "Text Note",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(RecallVioletPrimary, Color(0xFF312E81))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "New Note",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Action 5: Set Physical Tone Reminder
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.95f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.8f)),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                showFabMenu = false
                                showNewReminderDialog = true
                            }
                            .testTag("fab_set_reminder_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "Set Reminder",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(Color(0xFFF59E0B), Color(0xFFD97706))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "Set Reminder",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Primary High-Definition Cyber FAB
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(RecallVioletPrimary, Color(0xFF4338CA))
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.horizontalGradient(
                            listOf(RecallCyanSecondary, RecallVioletPrimary)
                        ),
                        shape = CircleShape
                    )
                    .clickable { showFabMenu = !showFabMenu }
                    .testTag("stream_expandable_fab"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (showFabMenu) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = "Expand quick capture actions",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Visual Memory Detail Dialog for Smart Search photo items
        selectedMediaForDialog?.let { media ->
            AlertDialog(
                onDismissRequest = { selectedMediaForDialog = null },
                containerColor = Color(0xFF0F172A),
                titleContentColor = Color.White,
                textContentColor = Color(0xFFCBD5E1),
                title = {
                    Text(
                        text = "Visual Memory Detail",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .background(
                                    Brush.linearGradient(
                                        listOf(RecallVioletPrimary.copy(alpha = 0.8f), RecallCyanSecondary.copy(alpha = 0.8f))
                                    ),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Captured Memory Photo", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Text(
                            text = "Captured: ${DateUtils.formatDate(media.capturedAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )

                        if (!media.aiDescription.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("AI Semantic Description", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = RecallCyanSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(media.aiDescription, style = MaterialTheme.typography.bodySmall, color = Color.White)
                                }
                            }
                        }

                        if (!media.ocrText.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.dp, RecallVioletPrimary.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Extracted OCR Text", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = RecallVioletPrimary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(media.ocrText, style = MaterialTheme.typography.bodySmall, color = Color.White)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    if (media.noteId != null) {
                        Button(onClick = {
                            val targetNoteId = media.noteId
                            selectedMediaForDialog = null
                            viewModel.openNoteById(targetNoteId)
                        }) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Linked Note", color = Color.White)
                        }
                    } else {
                        TextButton(onClick = { selectedMediaForDialog = null }) { Text("Done", color = RecallCyanSecondary) }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedMediaForDialog = null }) { Text("Close", color = Color(0xFFCBD5E1)) }
                }
            )
        }

        // Reminders List Sheet
        if (showRemindersSheet) {
            RemindersListSheet(
                reminders = allReminders,
                onDismiss = { showRemindersSheet = false },
                onAddNewReminder = {
                    showRemindersSheet = false
                    reminderToEdit = null
                    showNewReminderDialog = true
                },
                onEditReminder = { reminder ->
                    showRemindersSheet = false
                    reminderToEdit = reminder
                    showNewReminderDialog = true
                },
                onToggleCompleted = { reminder, completed ->
                    viewModel.toggleReminderCompleted(reminder, completed)
                },
                onSnooze = { reminderId ->
                    viewModel.snoozeReminder(reminderId, 10)
                },
                onDelete = { reminderId ->
                    viewModel.deleteReminder(reminderId)
                },
                onPreviewTone = { tone, vibrate ->
                    viewModel.previewReminderTone(tone, vibrate)
                }
            )
        }

        // Reminder Scheduler Dialog
        if (showNewReminderDialog || proactiveReminderDraft != null) {
            val editing = reminderToEdit
            val draft = proactiveReminderDraft
            ReminderSchedulerDialog(
                initialTitle = editing?.title ?: draft?.first ?: "",
                initialDetails = editing?.details ?: draft?.second ?: "",
                noteId = editing?.noteId ?: draft?.third,
                existingReminder = editing,
                onDismiss = {
                    showNewReminderDialog = false
                    reminderToEdit = null
                    proactiveReminderDraft = null
                },
                onSave = { rTitle, rDetails, rTime, rTone, rVibrate ->
                    if (editing != null) {
                        viewModel.updateReminder(
                            editing.copy(
                                title = rTitle,
                                details = rDetails,
                                reminderTimeMillis = rTime,
                                toneType = rTone.name,
                                vibratePattern = rVibrate.name,
                                isCompleted = false
                            )
                        )
                    } else {
                        viewModel.scheduleReminder(
                            title = rTitle,
                            details = rDetails,
                            noteId = draft?.third,
                            timeMillis = rTime,
                            tone = rTone,
                            vibratePattern = rVibrate
                        )
                    }
                    showNewReminderDialog = false
                    reminderToEdit = null
                    proactiveReminderDraft = null
                },
                onDelete = if (editing != null) {
                    {
                        viewModel.deleteReminder(editing.id)
                        showNewReminderDialog = false
                        reminderToEdit = null
                        proactiveReminderDraft = null
                    }
                } else null,
                onPreviewTone = { tone, vibrate ->
                    viewModel.previewReminderTone(tone, vibrate)
                }
            )
        }

        // Delete Confirmation Dialog for any memory in Stream
        noteToDelete?.let { targetNote ->
            AlertDialog(
                onDismissRequest = { noteToDelete = null },
                title = {
                    Text(
                        text = "Delete Memory?",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to delete \"${targetNote.title.ifBlank { "this memory" }}\"? This cannot be undone.",
                        color = Color(0xFFCBD5E1)
                    )
                },
                containerColor = Color(0xFF0F172A),
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteNote(targetNote.id)
                            noteToDelete = null
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Text("Delete", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { noteToDelete = null }) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                }
            )
        }

        if (showIntelligenceHub) {
            com.example.ui.components.IntelligenceHubDialog(
                initialTab = intelligenceHubTab,
                allNotes = notes,
                allPeople = allPeople,
                allActions = allActions,
                habits = habits,
                allPlaces = allPlaces,
                allProjects = allProjects,
                allCommitments = allCommitments,
                allMemoryLinks = allMemoryLinks,
                onToggleHabitDate = { habitId, dateKey ->
                    viewModel.toggleHabitDate(habitId, dateKey)
                },
                onAddHabit = { title, icon ->
                    viewModel.addCustomHabit(title, icon)
                },
                onDeleteHabit = { habitId ->
                    viewModel.deleteHabit(habitId)
                },
                onOpenNote = { noteId ->
                    viewModel.openNoteById(noteId)
                },
                onSelectPerson = { person ->
                    onNavigateToPerson(person)
                },
                onFilterByLocation = { locName ->
                    viewModel.onSearchQueryChanged("loc:$locName")
                },
                onFilterByTag = { tag ->
                    viewModel.onSearchQueryChanged(tag)
                },
                onShowFeedback = { msg ->
                    viewModel.showFeedback(msg)
                },
                onDismiss = {
                    showIntelligenceHub = false
                }
            )
        }

        if (showHelpGuideOverlay) {
            HelpAndFaqScreen(
                initialTab = 0,
                onClose = { showHelpGuideOverlay = false }
            )
        }
    }
}
}

/**
 * Wraps feed cards with SwipeToDismissBox gestures:
 * - Swipe Right: Pin note to top.
 * - Swipe Left: Delete note.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableMemoryFeedCard(
    item: NoteWithDetails,
    onClick: () -> Unit,
    onPersonClick: (PersonEntity) -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onTogglePin()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val bgColor = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> RecallVioletPrimary.copy(alpha = 0.35f)
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                else -> Color.Transparent
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor, RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (item.note.isPinned) Icons.Outlined.PushPin else Icons.Default.PushPin,
                            contentDescription = "Pin",
                            tint = RecallVioletPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (item.note.isPinned) "Unpin" else "Pin to Top",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = RecallVioletPrimary
                        )
                    }
                } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Delete",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    ) {
        val hasImage = item.media.any { it.mediaType == MediaType.IMAGE } ||
                item.note.category.equals("Visuals", ignoreCase = true)
        val hasAudio = !item.note.rawTranscription.isNullOrBlank() ||
                item.media.any { it.mediaType == MediaType.AUDIO } ||
                item.note.category.equals("Voice Memo", ignoreCase = true)

        when {
            hasImage -> PhotoMemoryCard(item, onClick, onPersonClick, onTogglePin, onDelete)
            hasAudio -> AudioMemoryCard(item, onClick, onPersonClick, onTogglePin, onDelete)
            else -> StandardNoteCard(item, onClick, onPersonClick, onTogglePin, onDelete)
        }
    }
}

/**
 * Feed Card 1: Standard Note Card
 * Title, timestamp, snippet, category chip, thumbnail of linked person.
 */
@Composable
fun StandardNoteCard(
    item: NoteWithDetails,
    onClick: () -> Unit,
    onPersonClick: (PersonEntity) -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    val note = item.note
    val context = LocalContext.current
    val isCompact by AppVisualPreferences.isCompactStream.collectAsStateWithLifecycle()
    var menuExpanded by remember { mutableStateOf(false) }

    val categoryColor = getCategoryColor(note.category)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        categoryColor.copy(alpha = 0.5f),
                        RecallCyanSecondary.copy(alpha = 0.35f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .testTag("standard_note_card_${note.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0B1120).copy(alpha = 0.65f)
        )
    ) {
        Column(modifier = Modifier.padding(if (isCompact) 11.dp else 16.dp)) {
            // Header Row: Category chip, relative time, pin status, dropdown options
            CardHeaderRow(
                category = note.category,
                categoryColor = categoryColor,
                timestamp = note.createdAt,
                isPinned = note.isPinned,
                isUpdated = (note.updatedAt - note.createdAt > 2000L) || note.content.contains("Update History:"),
                locationName = note.getLocationName(),
                sourceType = note.sourceType,
                verificationStatus = note.verificationStatus,
                menuExpanded = menuExpanded,
                onMenuToggle = { menuExpanded = it },
                onTogglePin = onTogglePin,
                onDelete = onDelete,
                onScheduleCalendar = {
                    AppIntegrationsHelper.scheduleInCalendar(
                        context = context,
                        title = note.title,
                        description = note.content,
                        dateHintOrIso = null
                    )
                }
            )

            Spacer(modifier = Modifier.height(if (isCompact) 6.dp else 10.dp))

            // Title - High contrast bright white against dark surface
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(if (isCompact) 3.dp else 6.dp))

            // Snippet body - Luminous readable silver
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFCBD5E1),
                maxLines = if (isCompact) 1 else 3,
                overflow = TextOverflow.Ellipsis
            )

            // Linked people chips
            if (item.people.isNotEmpty() && !isCompact) {
                Spacer(modifier = Modifier.height(12.dp))
                LinkedPeopleChipsRow(people = item.people, onPersonClick = onPersonClick)
            }
        }
    }
}

/**
 * Feed Card 2: Photo Card
 * Image prominently displayed with a one-sentence AI summary badge over the corner.
 */
@Composable
fun PhotoMemoryCard(
    item: NoteWithDetails,
    onClick: () -> Unit,
    onPersonClick: (PersonEntity) -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    val note = item.note
    val context = LocalContext.current
    val isCompact by AppVisualPreferences.isCompactStream.collectAsStateWithLifecycle()
    var menuExpanded by remember { mutableStateOf(false) }
    val categoryColor = getCategoryColor(note.category)

    val imageMedia = item.media.firstOrNull { it.mediaType == MediaType.IMAGE }
    val aiSummaryOneSentence = note.aiSummary
        ?: imageMedia?.aiDescription
        ?: note.title

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        RecallCyanSecondary.copy(alpha = 0.5f),
                        RecallVioletPrimary.copy(alpha = 0.4f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .testTag("photo_card_${note.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0B1120).copy(alpha = 0.65f)
        )
    ) {
        Column {
            // Prominent Visual Memory Banner with Real Photo and One-Sentence AI Summary Badge
            val localUri = imageMedia?.localUri
            val photoFile = remember(localUri) { if (!localUri.isNullOrBlank()) File(localUri) else null }
            val photoFileExists = photoFile != null && photoFile.exists()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isCompact) 115.dp else 165.dp)
                    .background(Color(0xFF0F172A))
            ) {
                if (photoFile != null && photoFile.exists()) {
                    AsyncImage(
                        model = photoFile,
                        contentDescription = note.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // High-contrast gradient scrim over the photo
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.3f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.82f)
                                    )
                                )
                            )
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        RecallVioletPrimary.copy(alpha = 0.8f),
                                        RecallCyanSecondary.copy(alpha = 0.8f)
                                    )
                                )
                            )
                    )
                }

                // One-sentence AI summary badge over the bottom corner
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.72f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .fillMaxWidth(0.92f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = RecallCyanSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = aiSummaryOneSentence,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Photo Badge on Top Left
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Photo,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Photo Memory", color = Color.White, fontSize = 10.sp)
                    }
                }
            }

            Column(modifier = Modifier.padding(if (isCompact) 11.dp else 16.dp)) {
                // Header Row
                CardHeaderRow(
                    category = note.category,
                    categoryColor = categoryColor,
                    timestamp = note.createdAt,
                    isPinned = note.isPinned,
                    isUpdated = (note.updatedAt - note.createdAt > 2000L) || note.content.contains("Update History:"),
                    sourceType = note.sourceType,
                    verificationStatus = note.verificationStatus,
                    menuExpanded = menuExpanded,
                    onMenuToggle = { menuExpanded = it },
                    onTogglePin = onTogglePin,
                    onDelete = onDelete,
                    onScheduleCalendar = {
                        AppIntegrationsHelper.scheduleInCalendar(
                            context = context,
                            title = note.title,
                            description = note.content,
                            dateHintOrIso = null
                        )
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // High-contrast bright title
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (note.content.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = note.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1),
                        maxLines = if (isCompact) 1 else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (item.people.isNotEmpty() && !isCompact) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LinkedPeopleChipsRow(people = item.people, onPersonClick = onPersonClick)
                }
            }
        }
    }
}

/**
 * Feed Card 3: Audio Note Card
 * Displays an audio waveform icon, duration, and generated transcript preview.
 */
@Composable
fun AudioMemoryCard(
    item: NoteWithDetails,
    onClick: () -> Unit,
    onPersonClick: (PersonEntity) -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    val note = item.note
    val context = LocalContext.current
    val isCompact by AppVisualPreferences.isCompactStream.collectAsStateWithLifecycle()
    var menuExpanded by remember { mutableStateOf(false) }
    val categoryColor = getCategoryColor(note.category)
    val audioMedia = item.media.firstOrNull { it.mediaType == MediaType.AUDIO && it.localUri.isNotBlank() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        RecallCyanSecondary.copy(alpha = 0.5f),
                        categoryColor.copy(alpha = 0.4f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .testTag("audio_note_card_${note.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0B1120).copy(alpha = 0.65f)
        )
    ) {
        Column(modifier = Modifier.padding(if (isCompact) 11.dp else 16.dp)) {
            // Header Row
            CardHeaderRow(
                category = if (note.category.isBlank() || note.category == "General") "Voice Memo" else note.category,
                categoryColor = categoryColor,
                timestamp = note.createdAt,
                isPinned = note.isPinned,
                isUpdated = (note.updatedAt - note.createdAt > 2000L) || note.content.contains("Update History:"),
                sourceType = note.sourceType,
                verificationStatus = note.verificationStatus,
                menuExpanded = menuExpanded,
                onMenuToggle = { menuExpanded = it },
                onTogglePin = onTogglePin,
                onDelete = onDelete,
                onScheduleCalendar = {
                    AppIntegrationsHelper.scheduleInCalendar(
                        context = context,
                        title = note.title,
                        description = note.content,
                        dateHintOrIso = null
                    )
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (audioMedia != null) {
                AudioPlayerCard(
                    audioPath = audioMedia.localUri,
                    title = audioMedia.aiDescription ?: "Recorded Voice Memory"
                )
            } else {
                // Audio Waveform Bar with duration
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Waveform",
                            tint = RecallCyanSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Voice Recording",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = RecallCyanSecondary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Audio Preview",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // High-contrast bright title
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Generated Transcript Preview
            val transcriptSnippet = note.rawTranscription ?: note.content
            if (transcriptSnippet.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Transcript: \"$transcriptSnippet\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCBD5E1),
                    maxLines = if (isCompact) 1 else 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (item.people.isNotEmpty() && !isCompact) {
                Spacer(modifier = Modifier.height(12.dp))
                LinkedPeopleChipsRow(people = item.people, onPersonClick = onPersonClick)
            }
        }
    }
}

@Composable
private fun CardHeaderRow(
    category: String,
    categoryColor: Color,
    timestamp: Long,
    isPinned: Boolean,
    isUpdated: Boolean = false,
    locationName: String? = null,
    sourceType: String = "USER_EXPLICIT",
    verificationStatus: String = "VERIFIED_FACT",
    menuExpanded: Boolean,
    onMenuToggle: (Boolean) -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    onScheduleCalendar: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VibrantTagBadge(
            text = category,
            color = categoryColor
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = DateUtils.formatRelativeTime(timestamp),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8)
        )

        if (verificationStatus == "AI_INFERRED_PENDING") {
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                color = Color(0xFFF59E0B).copy(alpha = 0.22f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.75f))
            ) {
                Text(
                    text = "🤖 AI Inferred",
                    color = Color(0xFFFBBF24),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }
        } else if (isUpdated) {
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                color = Color(0xFF10B981).copy(alpha = 0.22f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, Color(0xFF34D399).copy(alpha = 0.75f))
            ) {
                Text(
                    text = "🔄 Updated",
                    color = Color(0xFF6EE7B7),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }
        }

        if (!locationName.isNullOrBlank()) {
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                color = RecallEmeraldAccent.copy(alpha = 0.20f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, RecallEmeraldAccent.copy(alpha = 0.7f))
            ) {
                Text(
                    text = "📍 $locationName",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (isPinned) {
            Icon(
                imageVector = Icons.Default.PushPin,
                contentDescription = "Pinned",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }

        // Quick delete button on every memory card
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(26.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = "Delete Memory",
                tint = Color(0xFFF87171).copy(alpha = 0.85f),
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(2.dp))

        Box {
            IconButton(
                onClick = { onMenuToggle(true) },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { onMenuToggle(false) },
                properties = PopupProperties(clippingEnabled = true),
                modifier = Modifier
                    .background(Color(0xFF0F172A))
                    .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
            ) {
                DropdownMenuItem(
                    text = { Text(if (isPinned) "Unpin" else "Pin to top", color = Color.White) },
                    leadingIcon = {
                        Icon(
                            if (isPinned) Icons.Outlined.PushPin else Icons.Default.PushPin,
                            contentDescription = null,
                            tint = RecallVioletPrimary
                        )
                    },
                    onClick = {
                        onMenuToggle(false)
                        onTogglePin()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Schedule in Calendar", color = Color.White) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = RecallCyanSecondary
                        )
                    },
                    onClick = {
                        onMenuToggle(false)
                        coroutineScope.launch {
                            delay(60)
                            onScheduleCalendar()
                        }
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = {
                        onMenuToggle(false)
                        coroutineScope.launch {
                            delay(60)
                            onDelete()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun LinkedPeopleChipsRow(
    people: List<PersonEntity>,
    onPersonClick: (PersonEntity) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        people.take(3).forEach { person ->
            AssistChip(
                onClick = { onPersonClick(person) },
                label = {
                    Text(
                        person.name,
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = RecallCyanSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = Color(0xFF1E293B)
                ),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.height(28.dp)
            )
        }
        if (people.size > 3) {
            Text(
                text = "+${people.size - 3}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getCategoryColor(category: String): Color {
    return getVibrantTagColor(category)
}
