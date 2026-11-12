package com.example.ui.reminders

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import com.example.ui.theme.MemoraeCircleIconButton
import com.example.ui.theme.MemoraeGradientButton
import com.example.ui.theme.MemoraeSearchPill
import com.example.ui.theme.MemoraeStatusPill
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.ReminderTone
import com.example.data.local.entity.ReminderVibratePattern
import com.example.ui.RecallViewModel
import com.example.ui.components.ReminderCardItem
import com.example.ui.components.ReminderSchedulerDialog
import com.example.ui.theme.Glossy3DActionButton
import com.example.ui.theme.Glossy3DOrbIcon
import com.example.ui.theme.NeuralAppBackground
import com.example.ui.theme.OrbIconType
import com.example.ui.theme.RecallCyanSecondary
import com.example.ui.theme.RecallVioletPrimary
import com.example.ui.theme.seamlessInnerTextFieldColors
import com.example.util.DateUtils
import com.example.util.ReminderNotificationHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class ReminderFilter(val label: String) {
    ALL("All"),
    ACTIVE("Upcoming"),
    TODAY("Today"),
    COMPLETED("Completed")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    viewModel: RecallViewModel,
    onBack: (() -> Unit)? = null,
    onOpenNote: ((String) -> Unit)? = null
) {
    BackHandler(enabled = onBack != null) {
        onBack?.invoke()
    }

    val context = LocalContext.current
    val allReminders by viewModel.allReminders.collectAsStateWithLifecycle()
    val activeReminders by viewModel.activeReminders.collectAsStateWithLifecycle()
    val allCommitments by viewModel.allCommitments.collectAsStateWithLifecycle()
    val allPeople by viewModel.people.collectAsStateWithLifecycle()

    var hasNotifPerm by remember {
        mutableStateOf(ReminderNotificationHelper.hasNotificationPermission(context))
    }
    var canExactAlarm by remember {
        mutableStateOf(ReminderNotificationHelper.canScheduleExactAlarms(context))
    }

    val notifPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPerm = granted || ReminderNotificationHelper.hasNotificationPermission(context)
        canExactAlarm = ReminderNotificationHelper.canScheduleExactAlarms(context)
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(ReminderFilter.ACTIVE) }

    var showCreateDialog by remember { mutableStateOf(false) }
    var suggestedReminderDraft by remember { mutableStateOf<Triple<String, String, String?>?>(null) }
    var reminderToEdit by remember { mutableStateOf<ReminderEntity?>(null) }

    val unscheduledOpenCommitments = remember(allCommitments, activeReminders) {
        allCommitments.filter { comm ->
            !comm.isCompleted && activeReminders.none { rem ->
                (comm.noteId != null && rem.noteId == comm.noteId) ||
                    rem.title.contains(comm.description.take(20), ignoreCase = true)
            }
        }
    }

    val timeFormatter = remember { SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault()) }
    val now = System.currentTimeMillis()

    val todayReminders = remember(allReminders) {
        allReminders.filter { DateUtils.isSameDay(it.reminderTimeMillis, now) }
    }
    val completedReminders = remember(allReminders) {
        allReminders.filter { it.isCompleted }
    }

    val filteredReminders = remember(allReminders, selectedFilter, searchQuery) {
        val baseList = when (selectedFilter) {
            ReminderFilter.ALL -> allReminders
            ReminderFilter.ACTIVE -> allReminders.filter { !it.isCompleted }
            ReminderFilter.TODAY -> todayReminders
            ReminderFilter.COMPLETED -> completedReminders
        }

        if (searchQuery.isBlank()) {
            baseList.sortedBy { it.reminderTimeMillis }
        } else {
            val q = searchQuery.lowercase().trim()
            baseList.filter {
                it.title.lowercase().contains(q) ||
                        it.details.lowercase().contains(q) ||
                        it.toneType.lowercase().contains(q)
            }.sortedBy { it.reminderTimeMillis }
        }
    }

    val nextUpcoming = remember(activeReminders) {
        activeReminders.filter { it.reminderTimeMillis >= now }.minByOrNull { it.reminderTimeMillis }
    }

    NeuralAppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = RecallCyanSecondary,
                    contentColor = Color(0xFF070B14),
                    shape = CircleShape,
                    modifier = Modifier.testTag("reminders_screen_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Schedule Reminder",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 6.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Memorae Top Header Row
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MemoraeCircleIconButton(
                            icon = if (onBack != null) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Menu,
                            onClick = { onBack?.invoke() },
                            contentDescription = "Menu / Back"
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Text(
                            text = "Reminders",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        MemoraeCircleIconButton(
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            onClick = {
                                val next = activeReminders.minByOrNull { it.reminderTimeMillis }
                                if (next != null) {
                                    viewModel.previewReminderTone(next.getReminderTone(), next.getVibrationPattern())
                                    viewModel.showFeedback("Previewing: ${next.title} (${next.getReminderTone().displayName})")
                                } else {
                                    viewModel.showFeedback("No upcoming reminders to preview")
                                }
                            },
                            contentDescription = "Sound"
                        )
                    }
                }

                // Memorae Status Count Pills (Active: 7, Recurrent: 12, Completed: 64)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val recurrentCount = allReminders.count { it.details.contains("recur", ignoreCase = true) || it.details.contains("every", ignoreCase = true) }
                        MemoraeStatusPill(
                            label = "Active",
                            count = activeReminders.size,
                            isSelected = selectedFilter == ReminderFilter.ACTIVE,
                            onClick = { selectedFilter = ReminderFilter.ACTIVE },
                            modifier = Modifier.weight(1f)
                        )
                        MemoraeStatusPill(
                            label = "Recurrent",
                            count = recurrentCount,
                            isSelected = false,
                            onClick = { selectedFilter = ReminderFilter.ALL },
                            modifier = Modifier.weight(1f)
                        )
                        MemoraeStatusPill(
                            label = "Completed",
                            count = completedReminders.size,
                            isSelected = selectedFilter == ReminderFilter.COMPLETED,
                            onClick = { selectedFilter = ReminderFilter.COMPLETED },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Memorae Filter/Sort Controls
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Newest first ▾",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.90f)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = if (selectedFilter == ReminderFilter.TODAY) "Today ✓" else "Today ▾",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selectedFilter == ReminderFilter.TODAY) RecallCyanSecondary else Color.White.copy(alpha = 0.90f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedFilter = if (selectedFilter == ReminderFilter.TODAY) ReminderFilter.ACTIVE else ReminderFilter.TODAY
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Primary CTA Button: Vibrant Magenta-Violet Gradient
                item {
                    MemoraeGradientButton(
                        text = "Create new reminder",
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPerm) {
                                try {
                                    notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } catch (_: Throwable) {
                                }
                            }
                            showCreateDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reminders_create_gradient_btn")
                    )
                }

                // Frosted Search Pill matching Screenshot 3
                item {
                    MemoraeSearchPill(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholderText = "Search for a reminder",
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reminders_search_input")
                    )
                }

                // Section Header
                item {
                    Text(
                        text = if (selectedFilter == ReminderFilter.COMPLETED) "Completed reminders" else "Your active reminders",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }

                // Closed-App & Lock-Screen Alarm Status / Permission Banner
                item {
                    if (!hasNotifPerm || !canExactAlarm) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF451A03).copy(alpha = 0.9f),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Enable Lock-Screen & Closed-App Alerts",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Allow notifications & exact alarms so reminders ring when the app is shut.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFFDE68A),
                                        fontSize = 10.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF59E0B),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (!hasNotifPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                try {
                                                    notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                } catch (_: Throwable) {
                                                    ReminderNotificationHelper.openNotificationSettings(context)
                                                }
                                            } else if (!canExactAlarm) {
                                                ReminderNotificationHelper.openExactAlarmSettings(context)
                                            } else {
                                                ReminderNotificationHelper.openNotificationSettings(context)
                                            }
                                        }
                                ) {
                                    Text(
                                        text = "Enable",
                                        color = Color(0xFF070B14),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF0F172A).copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.55f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Lock-screen & closed-app exact alarms active",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFD1FAE5),
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = RecallCyanSecondary.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.6f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            viewModel.scheduleReminder(
                                                title = "Lock-Screen Alarm Test",
                                                details = "Triggered via exact AlarmClock while app is closed or screen is locked.",
                                                noteId = null,
                                                timeMillis = System.currentTimeMillis() + 5_000L,
                                                tone = ReminderTone.DIGITAL_CRYSTAL,
                                                vibratePattern = ReminderVibratePattern.STRONG_PULSE
                                            )
                                            viewModel.showFeedback("Test alarm set for 5s! Lock screen or close app now to test.")
                                        }
                                        .testTag("test_5s_lockscreen_alarm_btn")
                                ) {
                                    Text(
                                        text = "Test 5s Alert",
                                        color = RecallCyanSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Summary / Next upcoming highlight banner
                if (nextUpcoming != null && searchQuery.isBlank()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFF0B1426).copy(alpha = 0.85f),
                            border = BorderStroke(
                                1.2.dp,
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFFF59E0B).copy(alpha = 0.8f),
                                        RecallVioletPrimary.copy(alpha = 0.5f),
                                        RecallCyanSecondary.copy(alpha = 0.6f)
                                    )
                                )
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF59E0B).copy(alpha = 0.2f))
                                        .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = Color(0xFFFDE68A),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "NEXT UPCOMING",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFF59E0B),
                                            fontSize = 9.sp,
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "• ${nextUpcoming.getReminderTone().displayName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = RecallCyanSecondary,
                                            fontSize = 9.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = nextUpcoming.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = timeFormatter.format(Date(nextUpcoming.reminderTimeMillis)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 11.sp
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = RecallCyanSecondary.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { reminderToEdit = nextUpcoming }
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "Edit",
                                        color = RecallCyanSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Phase 5: Proactive Alarm Suggestions from Open Commitments
                if (unscheduledOpenCommitments.isNotEmpty() && searchQuery.isBlank()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF0F172A).copy(alpha = 0.88f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("proactive_alarm_suggestions_card")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "⚡ Proactive Alarm Suggestions (${unscheduledOpenCommitments.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF34D399)
                                )
                                Text(
                                    text = "Open commitments without a scheduled lock-screen alarm:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                unscheduledOpenCommitments.take(3).forEach { comm ->
                                    val linkedPerson = allPeople.find { it.id == comm.personId }?.name
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = comm.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White,
                                                maxLines = 1
                                            )
                                            if (!linkedPerson.isNullOrBlank()) {
                                                Text(
                                                    text = "With $linkedPerson",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = RecallCyanSecondary,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                                            border = BorderStroke(1.dp, Color(0xFF34D399)),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    suggestedReminderDraft = Triple(
                                                        "Commitment: ${comm.description.take(40)}",
                                                        comm.description,
                                                        comm.noteId
                                                    )
                                                }
                                        ) {
                                            Text(
                                                text = "🔔 Schedule",
                                                color = Color(0xFF34D399),
                                                fontSize = 11.sp,
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


                // Reminders list
                if (filteredReminders.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF0B1120).copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, Color(0xFF1E293B)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF0F172A),
                                    border = BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.4f)),
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = when (selectedFilter) {
                                                ReminderFilter.COMPLETED -> Icons.Default.CheckCircle
                                                ReminderFilter.TODAY -> Icons.Default.CalendarToday
                                                else -> Icons.Default.Notifications
                                            },
                                            contentDescription = null,
                                            tint = RecallCyanSecondary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = if (searchQuery.isNotBlank()) "No matching reminders found"
                                    else when (selectedFilter) {
                                        ReminderFilter.ACTIVE -> "No upcoming reminders scheduled"
                                        ReminderFilter.TODAY -> "No reminders scheduled for today"
                                        ReminderFilter.COMPLETED -> "No completed reminders"
                                        ReminderFilter.ALL -> "No reminders scheduled"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = if (searchQuery.isNotBlank()) "Try searching for a different keyword or clear search."
                                    else "Schedule reminders with custom physical acoustic tones (Digital Crystal, Ceramic Chime, Cosmic Ping, Gentle Rain) and vibration alarms.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                Glossy3DActionButton(
                                    text = "+ Schedule New Reminder",
                                    orbType = OrbIconType.BELL,
                                    onClick = { showCreateDialog = true }
                                )
                            }
                        }
                    }
                } else {
                    items(filteredReminders, key = { it.id }) { reminder ->
                        ReminderCardItem(
                            reminder = reminder,
                            timeFormatter = timeFormatter,
                            onClick = { reminderToEdit = reminder },
                            onToggleCompleted = {
                                viewModel.toggleReminderCompleted(reminder, !reminder.isCompleted)
                            },
                            onSnooze = {
                                viewModel.snoozeReminder(reminder.id, 10)
                            },
                            onDelete = {
                                viewModel.deleteReminder(reminder.id)
                            },
                            onPreviewTone = { tone, vibrate ->
                                viewModel.previewReminderTone(tone, vibrate)
                            },
                            onOpenNote = onOpenNote
                        )
                    }
                }
            }
        }
    }

    // Schedule / Edit Dialog
    if (showCreateDialog || suggestedReminderDraft != null) {
        val draft = suggestedReminderDraft
        ReminderSchedulerDialog(
            initialTitle = draft?.first ?: "Recall Memory Reminder",
            initialDetails = draft?.second ?: "",
            noteId = draft?.third,
            existingReminder = null,
            onDismiss = {
                showCreateDialog = false
                suggestedReminderDraft = null
            },
            onSave = { title, details, timeMillis, tone, vibrate ->
                viewModel.scheduleReminder(
                    title = title,
                    details = details,
                    noteId = draft?.third,
                    timeMillis = timeMillis,
                    tone = tone,
                    vibratePattern = vibrate
                )
                showCreateDialog = false
                suggestedReminderDraft = null
            },
            onPreviewTone = { tone, vibrate ->
                viewModel.previewReminderTone(tone, vibrate)
            }
        )
    }

    if (reminderToEdit != null) {
        val rem = reminderToEdit!!
        ReminderSchedulerDialog(
            initialTitle = rem.title,
            initialDetails = rem.details,
            noteId = rem.noteId,
            existingReminder = rem,
            onDismiss = { reminderToEdit = null },
            onSave = { title, details, timeMillis, tone, vibrate ->
                viewModel.updateReminder(
                    rem.copy(
                        title = title,
                        details = details,
                        reminderTimeMillis = timeMillis,
                        toneType = tone.name,
                        vibratePattern = vibrate.name
                    )
                )
                reminderToEdit = null
            },
            onDelete = {
                viewModel.deleteReminder(rem.id)
                reminderToEdit = null
            },
            onPreviewTone = { tone, vibrate ->
                viewModel.previewReminderTone(tone, vibrate)
            }
        )
    }
}
