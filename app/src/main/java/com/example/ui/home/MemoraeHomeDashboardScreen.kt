package com.example.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.ui.theme.MemoraeCircleIconButton
import com.example.ui.theme.MemoraeCompanionAvatar
import com.example.ui.theme.MemoraeGlassCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CommitmentEntity
import com.example.data.local.entity.PersonEntity
import com.example.data.model.NoteWithDetails
import com.example.ui.RecallViewModel
import com.example.ui.components.ReminderSchedulerDialog
import com.example.ui.navigation.Screen
import com.example.ui.settings.HelpAndFaqScreen
import com.example.ui.stream.StreamScreen
import com.example.ui.theme.GlassCircleIconButton
import com.example.ui.theme.NeonBrainIcon
import com.example.ui.theme.NeuralAppBackground
import com.example.ui.theme.RecallCyanSecondary
import com.example.ui.theme.RecallEmeraldAccent
import com.example.ui.theme.RecallVioletPrimary
import com.example.ui.theme.getVibrantTagColor
import com.example.ui.theme.seamlessInnerTextFieldColors
import com.example.util.DateUtils
import com.example.util.MemoryIntelligenceHelper
import com.example.util.ProactiveInsightType
import com.example.util.RecallLifeOrganiserManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun MemoraeHomeDashboardScreen(
    viewModel: RecallViewModel,
    onNavigateToScreen: (Screen, String?) -> Unit,
    onNavigateToPerson: (PersonEntity) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val allPeople by viewModel.people.collectAsStateWithLifecycle()
    val activeReminders by viewModel.activeReminders.collectAsStateWithLifecycle()
    val actionItems by viewModel.actionItems.collectAsStateWithLifecycle()
    val allCommitments by viewModel.allCommitments.collectAsStateWithLifecycle()
    val allProjects by viewModel.allProjects.collectAsStateWithLifecycle()
    val dynamicLists by viewModel.dynamicLists.collectAsStateWithLifecycle()
    val userDisplayName by viewModel.userDisplayName.collectAsStateWithLifecycle()
    val userFocusGoal by viewModel.userFocusGoal.collectAsStateWithLifecycle()
    val taskMetaMap by viewModel.taskMetaMap.collectAsStateWithLifecycle()

    var showFullStreamInline by remember { mutableStateOf(false) }
    var showHelpOverlay by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var reminderDraftTitle by remember { mutableStateOf("") }
    var reminderDraftDetails by remember { mutableStateOf("") }

    var askInputText by remember { mutableStateOf("") }
    var quickTaskInput by remember { mutableStateOf("") }
    var commitmentToConfirmComplete by remember { mutableStateOf<CommitmentEntity?>(null) }
    var commitmentToAssignDate by remember { mutableStateOf<CommitmentEntity?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            viewModel.processCameraCapture(bitmap)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            try {
                cameraLauncher.launch()
            } catch (e: Throwable) {
                viewModel.showFeedback("Could not open camera: ${e.message}")
            }
        } else {
            viewModel.showFeedback("Camera permission is needed to take photos.")
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.processImageUriCapture(uri)
        }
    }

    fun triggerQuickCamera() {
        val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            try {
                cameraLauncher.launch()
            } catch (e: Throwable) {
                viewModel.showFeedback("Could not open camera: ${e.message}")
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (showFullStreamInline) {
        StreamScreen(
            viewModel = viewModel,
            onNavigateToPerson = onNavigateToPerson,
            onNavigateToDebrief = { onNavigateToScreen(Screen.Capture, "Daily Debrief") },
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToReminders = { onNavigateToScreen(Screen.Organise, "Reminders") }
        )
        return
    }

    val briefing = remember(notes, allPeople, actionItems, allCommitments, allProjects) {
        MemoryIntelligenceHelper.buildAnticipatoryBriefing(
            allNotes = notes,
            allPeople = allPeople,
            allActions = actionItems,
            allCommitments = allCommitments,
            allProjects = allProjects
        )
    }

    val naturalDateSuggestion = remember(askInputText, allPeople) {
        RecallLifeOrganiserManager.parseNaturalDateTimeSuggestion(
            text = askInputText,
            knownPeopleNames = allPeople.map { it.name }
        )
    }

    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greetingPrefix = when (hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
    val greetingHeader = if (userDisplayName.isNotBlank()) "$greetingPrefix, $userDisplayName" else greetingPrefix
    val todayFormatted = remember {
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
    }

    val openCommitments = remember(allCommitments) {
        allCommitments.filter { !it.isCompleted }.sortedBy { it.dueDate ?: Long.MAX_VALUE }
    }
    val pendingTasks = remember(actionItems) {
        actionItems.filter { !it.isCompleted }
    }
    val peopleById = remember(allPeople) { allPeople.associateBy { it.id } }

    NeuralAppBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("memorae_home_dashboard"),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 104.dp)
        ) {
            // 1. Memorae Companion Avatar & Header Controls
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Bar: Settings circular button on left, Avatar in center, Concierge on right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MemoraeCircleIconButton(
                            icon = Icons.Default.Settings,
                            onClick = onNavigateToSettings,
                            contentDescription = "Settings",
                            modifier = Modifier.testTag("home_settings_btn")
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        MemoraeCompanionAvatar(
                            size = 68.dp,
                            onClick = { onNavigateToScreen(Screen.AiIntelligence, "Ask Recall") },
                            modifier = Modifier.testTag("home_companion_avatar")
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        MemoraeCircleIconButton(
                            icon = Icons.Default.AutoAwesome,
                            onClick = { onNavigateToScreen(Screen.AiIntelligence, "Ask Recall") },
                            contentDescription = "Ask Recall Concierge",
                            modifier = Modifier.testTag("home_companion_ai_btn")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Luminous Hero Greeting matching Screenshot 1
                    Text(
                        text = greetingHeader,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Talk to me, or pick a destination to begin.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.90f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            // Destination Glass Cards (matching Screenshot 1: "THE OFFICE | Get it done >", "THE PARK | Get inspired >")
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // THE OFFICE — Get it done
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.60f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onNavigateToScreen(Screen.Organise, "Tasks") }
                            .testTag("dest_card_office")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "THE OFFICE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.75f),
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    text = "Get it done",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // THE PARK — Get inspired
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.60f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onNavigateToScreen(Screen.Memories, "Ideas & Thoughts") }
                            .testTag("dest_card_park")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "THE PARK",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.75f),
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    text = "Get inspired",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // 2. Prominent "Ask Recall a Question" Bar (Memorae Conversational Command Bar)
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.88f),
                    border = BorderStroke(
                        1.2.dp,
                        Brush.horizontalGradient(
                            listOf(
                                RecallCyanSecondary.copy(alpha = 0.85f),
                                RecallVioletPrimary.copy(alpha = 0.85f)
                            )
                        )
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(RecallVioletPrimary.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Ask Recall",
                                    tint = RecallCyanSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = askInputText,
                                onValueChange = { askInputText = it },
                                placeholder = {
                                    Text(
                                        "Ask Recall anything or type a reminder/promise...",
                                        fontSize = 13.sp,
                                        color = Color(0xFF94A3B8),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                singleLine = true,
                                colors = seamlessInnerTextFieldColors(),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("home_ask_recall_input")
                            )
                            IconButton(
                                onClick = {
                                    val query = askInputText.trim()
                                    if (query.isNotBlank()) {
                                        viewModel.sendChatMessage(query)
                                        askInputText = ""
                                        onNavigateToScreen(Screen.AiIntelligence, "Ask Recall")
                                    } else {
                                        onNavigateToScreen(Screen.AiIntelligence, "Ask Recall")
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(RecallVioletPrimary)
                                    .testTag("home_ask_recall_send_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Ask Recall",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Phase 9: Natural-language date/time reminder detection chip
                        AnimatedVisibility(visible = naturalDateSuggestion != null) {
                            naturalDateSuggestion?.let { suggestion ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF1E293B).copy(alpha = 0.95f),
                                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.8f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsActive,
                                            contentDescription = null,
                                            tint = Color(0xFFF59E0B),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Smart Date Detected: ${suggestion.formattedDateLabel}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color(0xFFFDE68A),
                                                fontWeight = FontWeight.Bold
                                            )
                                            val personBadge = suggestion.mentionedPersonName?.let { " • 👤 $it" } ?: ""
                                            Text(
                                                text = "${suggestion.suggestedAction}$personBadge",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                viewModel.scheduleReminder(
                                                    title = suggestion.suggestedAction,
                                                    details = suggestion.originalText,
                                                    timeMillis = suggestion.suggestedTimeMillis,
                                                    recurrence = suggestion.recurrenceHint ?: "NONE"
                                                )
                                                askInputText = ""
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text("Set Reminder", color = Color(0xFF070B14), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
                                }
                            }
                        }

                        // Quick AI Prompt Pills
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuickPromptChip("⏪ Life Rewind") {
                                onNavigateToScreen(Screen.AiIntelligence, "Life Rewind")
                            }
                            QuickPromptChip("🤝 Forgotten Promises (${openCommitments.size})") {
                                onNavigateToScreen(Screen.Organise, "Commitments")
                            }
                            QuickPromptChip("📊 Weekly Intelligence") {
                                onNavigateToScreen(Screen.AiIntelligence, "Personal Insights")
                            }
                            QuickPromptChip("🕸️ Memory Graph") {
                                onNavigateToScreen(Screen.Memories, "Memory Graph")
                            }
                        }
                    }
                }
            }

            // 3. Zero-Friction Quick Capture Bar (Voice, Text, Camera, Gallery/Doc)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickCaptureActionTile(
                        icon = Icons.Default.Mic,
                        title = "Voice",
                        subtitle = "Record & unpack",
                        accentColor = RecallVioletPrimary,
                        onClick = { onNavigateToScreen(Screen.Capture, "Quick Capture") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_quick_voice_btn")
                    )
                    QuickCaptureActionTile(
                        icon = Icons.Default.Edit,
                        title = "Text Note",
                        subtitle = "Instant memory",
                        accentColor = RecallCyanSecondary,
                        onClick = { viewModel.openNoteEditor(null) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_quick_text_btn")
                    )
                    QuickCaptureActionTile(
                        icon = Icons.Default.CameraAlt,
                        title = "Camera",
                        subtitle = "Snap & OCR",
                        accentColor = RecallEmeraldAccent,
                        onClick = { triggerQuickCamera() },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_quick_camera_btn")
                    )
                    QuickCaptureActionTile(
                        icon = Icons.Default.PhotoLibrary,
                        title = "Upload",
                        subtitle = "Image or Doc",
                        accentColor = Color(0xFFF59E0B),
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_quick_upload_btn")
                    )
                }
            }

            // 4. Daily AI Briefing & Contextual Suggestions ("Your Day, at a Glance")
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0F172A).copy(alpha = 0.86f)
                    ),
                    border = BorderStroke(
                        1.dp,
                        Brush.horizontalGradient(
                            listOf(
                                RecallCyanSecondary.copy(alpha = 0.7f),
                                RecallVioletPrimary.copy(alpha = 0.7f)
                            )
                        )
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_daily_ai_briefing_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = briefing.greetingTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = RecallCyanSecondary.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.55f)),
                                modifier = Modifier.clickable {
                                    onNavigateToScreen(Screen.AiIntelligence, "Personal Insights")
                                }
                            ) {
                                Text(
                                    text = "Full Briefing →",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RecallCyanSecondary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = briefing.executiveSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Metric Summary Pills
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DashboardStatPill(
                                icon = "🧠",
                                label = "${notes.size} Memories",
                                color = RecallCyanSecondary,
                                onClick = { onNavigateToScreen(Screen.Memories, "All Memories") }
                            )
                            DashboardStatPill(
                                icon = "🤝",
                                label = "${openCommitments.size} Commitments",
                                color = Color(0xFFF59E0B),
                                onClick = { onNavigateToScreen(Screen.Organise, "Commitments") }
                            )
                            DashboardStatPill(
                                icon = "🔔",
                                label = "${activeReminders.size} Reminders",
                                color = Color(0xFF38BDF8),
                                onClick = { onNavigateToScreen(Screen.Organise, "Reminders") }
                            )
                            DashboardStatPill(
                                icon = "✅",
                                label = "${pendingTasks.size} Tasks",
                                color = RecallEmeraldAccent,
                                onClick = { onNavigateToScreen(Screen.Organise, "Tasks") }
                            )
                            DashboardStatPill(
                                icon = "📋",
                                label = "${dynamicLists.size} Lists",
                                color = RecallVioletPrimary,
                                onClick = { onNavigateToScreen(Screen.Organise, "Lists") }
                            )
                        }

                        if (briefing.insights.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            briefing.insights.take(3).forEach { insight ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF1E293B).copy(alpha = 0.78f),
                                    border = BorderStroke(1.dp, Color(0xFF334155)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = insight.icon, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = insight.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = insight.subtitle,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF94A3B8),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = RecallVioletPrimary.copy(alpha = 0.25f),
                                            border = BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.6f)),
                                            modifier = Modifier.clickable {
                                                when (insight.type) {
                                                    ProactiveInsightType.OPEN_COMMITMENT -> {
                                                        insight.commitment?.let { commitmentToConfirmComplete = it }
                                                    }
                                                    ProactiveInsightType.DORMANT_RELATIONSHIP -> {
                                                        insight.person?.let { onNavigateToPerson(it) }
                                                    }
                                                    ProactiveInsightType.PROJECT_MOMENTUM -> {
                                                        insight.project?.let { viewModel.startNoteForProject(it) }
                                                    }
                                                    ProactiveInsightType.UPCOMING_DATE,
                                                    ProactiveInsightType.PENDING_TASK -> {
                                                        reminderDraftTitle = insight.title
                                                        reminderDraftDetails = insight.subtitle
                                                        showReminderDialog = true
                                                    }
                                                }
                                            }
                                        ) {
                                            Text(
                                                text = insight.actionLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Upcoming Commitments & Important Reminders Section
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0F172A).copy(alpha = 0.86f)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🤝 Commitments & Reminders",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "+ Reminder",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFF59E0B),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable {
                                        reminderDraftTitle = ""
                                        reminderDraftDetails = ""
                                        showReminderDialog = true
                                    }
                                    .padding(end = 10.dp)
                            )
                            Text(
                                text = "Manage All →",
                                style = MaterialTheme.typography.labelSmall,
                                color = RecallCyanSecondary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    onNavigateToScreen(Screen.Organise, "Commitments")
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (openCommitments.isEmpty() && activeReminders.isEmpty()) {
                            Text(
                                text = "No open commitments or active alarms right now. Promises mentioned in voice notes or debriefs will appear here automatically.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        } else {
                            openCommitments.take(3).forEach { comm ->
                                val classification = RecallLifeOrganiserManager.classifyCommitment(comm)
                                val personName = comm.personId?.let { peopleById[it]?.name }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF1E293B).copy(alpha = 0.82f),
                                    border = BorderStroke(1.dp, Color(0xFF334155)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { commitmentToConfirmComplete = comm },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Confirm Complete",
                                                tint = Color(0xFFF59E0B),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = comm.description,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val metaText = buildList {
                                                add("${classification.badgeIcon} ${classification.label}")
                                                if (!personName.isNullOrBlank()) add("👤 $personName")
                                                if (comm.dueDate != null) {
                                                    add("📅 Due ${DateUtils.formatDayMonth(comm.dueDate)}")
                                                } else {
                                                    add("No due date")
                                                }
                                            }.joinToString(" • ")
                                            Text(
                                                text = metaText,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFFDE68A)
                                            )
                                        }
                                        if (comm.dueDate == null) {
                                            TextButton(
                                                onClick = { commitmentToAssignDate = comm }
                                            ) {
                                                Text("Set Date", color = RecallCyanSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            val fmt = remember { SimpleDateFormat("EEE h:mm a", Locale.getDefault()) }
                            activeReminders.take(2).forEach { rem ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF1E293B).copy(alpha = 0.82f),
                                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsActive,
                                            contentDescription = null,
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = rem.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "⏰ ${fmt.format(Date(rem.reminderTimeMillis))}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = RecallCyanSecondary
                                            )
                                        }
                                        TextButton(
                                            onClick = { viewModel.toggleReminderCompleted(rem, true) }
                                        ) {
                                            Text("Done", color = RecallEmeraldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. Outstanding Tasks & Memorae Quick Task Board Preview
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0F172A).copy(alpha = 0.86f)
                    ),
                    border = BorderStroke(1.dp, RecallEmeraldAccent.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "✅ Outstanding Tasks (${pendingTasks.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Task Board →",
                                style = MaterialTheme.typography.labelSmall,
                                color = RecallCyanSecondary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    onNavigateToScreen(Screen.Organise, "Tasks")
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick Add Task Bar
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = quickTaskInput,
                                onValueChange = { quickTaskInput = it },
                                placeholder = {
                                    Text("Add a task for Today...", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                },
                                singleLine = true,
                                colors = seamlessInnerTextFieldColors(),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1E293B).copy(alpha = 0.8f))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (quickTaskInput.isNotBlank()) {
                                        viewModel.createRichActionItem(quickTaskInput)
                                        quickTaskInput = ""
                                    }
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(RecallEmeraldAccent)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Task", tint = Color(0xFF050811))
                            }
                        }

                        if (pendingTasks.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            pendingTasks.take(4).forEach { taskItem ->
                                val meta = taskMetaMap[taskItem.id]
                                val col = RecallLifeOrganiserManager.resolveTaskBoardColumn(taskItem, meta)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = taskItem.isCompleted,
                                        onCheckedChange = { checked ->
                                            viewModel.toggleActionItem(taskItem.id, checked)
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = RecallEmeraldAccent,
                                            uncheckedColor = Color(0xFF94A3B8)
                                        ),
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = taskItem.task,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF1E293B),
                                        border = BorderStroke(1.dp, Color(0xFF475569))
                                    ) {
                                        Text(
                                            text = "${col.icon} ${col.label}",
                                            fontSize = 10.sp,
                                            color = RecallCyanSecondary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 7. Important People & Relationship Highlights
            if (allPeople.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "👥 People & Relationships (${allPeople.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "All People →",
                                style = MaterialTheme.typography.labelSmall,
                                color = RecallCyanSecondary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    onNavigateToScreen(Screen.Memories, "People")
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(allPeople.take(8), key = { it.id }) { person ->
                                val isOverdue = person.isOverdueForCatchUp()
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF0F172A).copy(alpha = 0.88f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isOverdue) Color(0xFFF59E0B) else RecallVioletPrimary.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier
                                        .width(150.dp)
                                        .clickable { onNavigateToPerson(person) }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape)
                                                    .background(RecallVioletPrimary.copy(alpha = 0.25f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = person.name.take(1).uppercase(),
                                                    color = Color.White,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 13.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = person.name,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = person.relationship ?: "Contact",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF94A3B8),
                                                    fontSize = 10.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = if (isOverdue) "🔔 Due for catch-up"
                                            else "Contacted ${person.daysSinceLastContact()}d ago",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isOverdue) Color(0xFFF59E0B) else RecallCyanSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 8. Recently Captured Memories Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🧠 Recently Captured Memories",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { showFullStreamInline = true }
                    ) {
                        Text("Filter / Calendar", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                    }
                    TextButton(
                        onClick = { onNavigateToScreen(Screen.Memories, "All Memories") }
                    ) {
                        Text("View All (${notes.size}) →", color = RecallCyanSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (notes.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Your Personal Memory Stream is Ready",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap Voice, Text Note, Camera, or Upload above to capture your first memory, conversation, or idea.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            } else {
                items(notes.take(6), key = { it.note.id }) { noteWithDetails ->
                    MemoraeCompactMemoryCard(
                        noteWithDetails = noteWithDetails,
                        onClick = { viewModel.openNoteEditor(noteWithDetails) },
                        onPersonClick = onNavigateToPerson
                    )
                }
            }
        }

        // Confirmation dialog before marking a commitment completed (Phase 2 rule)
        commitmentToConfirmComplete?.let { comm ->
            AlertDialog(
                onDismissRequest = { commitmentToConfirmComplete = null },
                containerColor = Color(0xFF0F172A),
                title = {
                    Text("Confirm Commitment Completed?", color = Color.White, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(
                        "Are you sure you want to mark \"${comm.description}\" as fulfilled?",
                        color = Color(0xFFCBD5E1)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.toggleCommitmentCompleted(comm.id, true)
                            commitmentToConfirmComplete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RecallEmeraldAccent)
                    ) {
                        Text("Confirm Fulfilled", color = Color(0xFF050811), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { commitmentToConfirmComplete = null }) {
                        Text("Not Yet", color = Color(0xFF94A3B8))
                    }
                }
            )
        }

        // Assign Date dialog for undated commitment (Phase 2 rule: ask user rather than inventing deadlines)
        commitmentToAssignDate?.let { comm ->
            val now = System.currentTimeMillis()
            val oneDay = 24L * 3600L * 1000L
            AlertDialog(
                onDismissRequest = { commitmentToAssignDate = null },
                containerColor = Color(0xFF0F172A),
                title = {
                    Text("Assign Due Date or Reminder", color = Color.White, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "\"${comm.description}\" has no deadline yet. Choose when you'd like to follow up:",
                            color = Color(0xFFCBD5E1),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(
                            onClick = {
                                viewModel.assignCommitmentDueDate(comm, now + oneDay)
                                viewModel.scheduleReminder(
                                    title = "Commitment: ${comm.description}",
                                    details = "Due tomorrow",
                                    noteId = comm.noteId,
                                    timeMillis = now + oneDay
                                )
                                commitmentToAssignDate = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = RecallVioletPrimary)
                        ) {
                            Text("Tomorrow (9:00 AM + Reminder)", color = Color.White)
                        }
                        Button(
                            onClick = {
                                viewModel.assignCommitmentDueDate(comm, now + 3 * oneDay)
                                commitmentToAssignDate = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Text("In 3 Days", color = Color.White)
                        }
                        Button(
                            onClick = {
                                viewModel.assignCommitmentDueDate(comm, now + 7 * oneDay)
                                commitmentToAssignDate = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Text("Next Week (+7 Days)", color = Color.White)
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { commitmentToAssignDate = null }) {
                        Text("Keep Undated", color = Color(0xFF94A3B8))
                    }
                }
            )
        }

        if (showReminderDialog) {
            ReminderSchedulerDialog(
                initialTitle = reminderDraftTitle,
                initialDetails = reminderDraftDetails,
                onDismiss = { showReminderDialog = false },
                onSave = { title, details, timeMillis, tone, vibrate ->
                    viewModel.scheduleReminder(
                        title = title,
                        details = details,
                        timeMillis = timeMillis,
                        tone = tone,
                        vibratePattern = vibrate
                    )
                    showReminderDialog = false
                },
                onPreviewTone = { tone, vibrate ->
                    viewModel.previewReminderTone(tone, vibrate)
                }
            )
        }

        if (showHelpOverlay) {
            HelpAndFaqScreen(onClose = { showHelpOverlay = false })
        }
    }
}

@Composable
private fun QuickPromptChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1E293B).copy(alpha = 0.9f),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFFE2E8F0),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun QuickCaptureActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.88f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.65f)),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8),
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DashboardStatPill(
    icon: String,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.55f)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MemoraeCompactMemoryCard(
    noteWithDetails: NoteWithDetails,
    onClick: () -> Unit,
    onPersonClick: (PersonEntity) -> Unit
) {
    val note = noteWithDetails.note
    val catColor = getVibrantTagColor(note.category)
    val isPendingInference = note.verificationStatus == "AI_INFERRED_PENDING"
    val locName = note.getLocationName()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F172A).copy(alpha = 0.88f)
        ),
        border = BorderStroke(
            1.dp,
            if (isPendingInference) Color(0xFFF59E0B).copy(alpha = 0.7f) else catColor.copy(alpha = 0.55f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = catColor.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, catColor.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = note.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = catColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                if (isPendingInference) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B))
                    ) {
                        Text(
                            text = "🤖 Inferred (${(note.confidenceScore * 100).toInt()}%)",
                            fontSize = 10.sp,
                            color = Color(0xFFFDE68A),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = DateUtils.formatRelativeTime(note.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
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
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (noteWithDetails.people.isNotEmpty() || !locName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    noteWithDetails.people.forEach { person ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = RecallVioletPrimary.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, RecallVioletPrimary.copy(alpha = 0.6f)),
                            modifier = Modifier.clickable { onPersonClick(person) }
                        ) {
                            Text(
                                text = "👤 ${person.name}",
                                fontSize = 10.sp,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                    if (!locName.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = RecallEmeraldAccent.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, RecallEmeraldAccent.copy(alpha = 0.55f))
                        ) {
                            Text(
                                text = "📍 $locName",
                                fontSize = 10.sp,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
