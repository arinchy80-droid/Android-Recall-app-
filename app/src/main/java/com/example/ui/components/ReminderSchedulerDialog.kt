package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.ReminderTone
import com.example.data.local.entity.ReminderVibratePattern
import com.example.ui.theme.Glossy3DActionButton
import com.example.ui.theme.Glossy3DOrbIcon
import com.example.ui.theme.OrbIconType
import com.example.ui.theme.RecallCyanSecondary
import com.example.ui.theme.RecallVioletPrimary
import com.example.ui.theme.translucentWhiteTextFieldColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun ReminderSchedulerDialog(
    initialTitle: String = "",
    initialDetails: String = "",
    noteId: String? = null,
    existingReminder: ReminderEntity? = null,
    onDismiss: () -> Unit,
    onSave: (title: String, details: String, timeMillis: Long, tone: ReminderTone, vibratePattern: ReminderVibratePattern) -> Unit,
    onDelete: (() -> Unit)? = null,
    onPreviewTone: (tone: ReminderTone, vibrate: ReminderVibratePattern) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var title by remember {
        mutableStateOf(
            existingReminder?.title ?: initialTitle.ifBlank { "Recall Memory Follow-Up" }
        )
    }
    var details by remember {
        mutableStateOf(existingReminder?.details ?: initialDetails)
    }

    val defaultTime = remember {
        existingReminder?.reminderTimeMillis
            ?: (System.currentTimeMillis() + (60 * 60 * 1000L)) // 1 hour ahead
    }
    var reminderTimeMillis by remember { mutableLongStateOf(defaultTime) }

    var selectedTone by remember {
        mutableStateOf(existingReminder?.getReminderTone() ?: ReminderTone.DIGITAL_CRYSTAL)
    }
    var selectedVibrate by remember {
        mutableStateOf(existingReminder?.getVibrationPattern() ?: ReminderVibratePattern.STRONG_PULSE)
    }

    val dateTimeFormatter = remember {
        SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault())
    }

    fun applyPresetMinutes(minutes: Int) {
        reminderTimeMillis = System.currentTimeMillis() + (minutes * 60 * 1000L)
    }

    fun applyTodayAt(hourOfDay: Int, minute: Int) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        reminderTimeMillis = cal.timeInMillis
    }

    fun openDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = reminderTimeMillis }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply {
                    timeInMillis = reminderTimeMillis
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                reminderTimeMillis = newCal.timeInMillis
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun openTimePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = reminderTimeMillis }
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val now = System.currentTimeMillis()
                val newCal = Calendar.getInstance().apply {
                    timeInMillis = reminderTimeMillis
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (timeInMillis < now - 60_000L) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    } else if (timeInMillis <= now) {
                        timeInMillis = now + 10_000L
                    }
                }
                reminderTimeMillis = newCal.timeInMillis
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            false
        ).show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF070C1A).copy(alpha = 0.96f),
            border = BorderStroke(
                1.5.dp,
                Brush.linearGradient(
                    listOf(
                        RecallCyanSecondary.copy(alpha = 0.85f),
                        Color(0xFF1E3A8A),
                        RecallVioletPrimary.copy(alpha = 0.85f)
                    )
                )
            ),
            shadowElevation = 24.dp,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .widthIn(max = 520.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(scrollState)
            ) {
                // Header Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Glossy3DOrbIcon(
                        orbType = OrbIconType.BELL,
                        size = 38.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (existingReminder != null) "Edit Reminder" else "Set Physical Tone Reminder",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Audible message tone & physical haptic feedback",
                            style = MaterialTheme.typography.labelSmall,
                            color = RecallCyanSecondary,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title Input
                Text(
                    text = "Reminder Note",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFCBD5E1)
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("What should we remind you about?") },
                    singleLine = true,
                    colors = translucentWhiteTextFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reminder_title_input")
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Date & Time Scheduling Section
                Text(
                    text = "Scheduled Alarm Time",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFCBD5E1)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E293B).copy(alpha = 0.90f),
                    border = BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = RecallCyanSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = dateTimeFormatter.format(reminderTimeMillis),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = RecallVioletPrimary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { openDatePicker() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CalendarMonth,
                                        contentDescription = "Pick Date",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Date", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF0284C7),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { openTimePicker() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.AccessTime,
                                        contentDescription = "Pick Time",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Time", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick time presets chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf(
                        "+1m" to { applyPresetMinutes(1) },
                        "+15m" to { applyPresetMinutes(15) },
                        "+30m" to { applyPresetMinutes(30) },
                        "+1 Hour" to { applyPresetMinutes(60) },
                        "Tonight 8 PM" to { applyTodayAt(20, 0) },
                        "Tomorrow 9 AM" to { applyTodayAt(9, 0) }
                    )
                    presets.forEach { (label, onClick) ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF1E293B).copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { onClick() }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE2E8F0),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Physical Message Tone Section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Physical Message Tone",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCBD5E1),
                        modifier = Modifier.weight(1f)
                    )
                    // Test Audio & Vibration Button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = RecallCyanSecondary.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, RecallCyanSecondary),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onPreviewTone(selectedTone, selectedVibrate)
                            }
                            .testTag("preview_tone_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Test Tone",
                                tint = RecallCyanSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Play & Feel Tone",
                                color = RecallCyanSecondary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tone selection list
                ReminderTone.entries.forEach { tone ->
                    val isSelected = selectedTone == tone
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) Color(0xFF162544).copy(alpha = 0.90f) else Color(0xFF0F172A).copy(alpha = 0.70f),
                        border = BorderStroke(
                            if (isSelected) 1.5.dp else 1.dp,
                            if (isSelected) RecallCyanSecondary else Color(0xFF1E293B)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                selectedTone = tone
                                onPreviewTone(tone, selectedVibrate)
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .border(
                                        1.5.dp,
                                        if (isSelected) RecallCyanSecondary else Color(0xFF64748B),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(RecallCyanSecondary)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tone.displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                                )
                                Text(
                                    text = tone.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp
                                )
                            }
                            IconButton(
                                onClick = { onPreviewTone(tone, selectedVibrate) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = "Test sound",
                                    tint = if (isSelected) RecallCyanSecondary else Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Physical Haptic Vibration Pattern Selection
                Text(
                    text = "Physical Tactile Vibration",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFCBD5E1)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ReminderVibratePattern.entries.forEach { pattern ->
                        val isSelected = selectedVibrate == pattern
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) RecallVioletPrimary.copy(alpha = 0.25f) else Color(0xFF0F172A).copy(alpha = 0.70f),
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) RecallVioletPrimary else Color(0xFF1E293B)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedVibrate = pattern
                                    onPreviewTone(selectedTone, pattern)
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Vibration,
                                    contentDescription = null,
                                    tint = if (isSelected) RecallVioletPrimary else Color(0xFF94A3B8),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = pattern.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Footer CTA Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (existingReminder != null && onDelete != null) {
                        OutlinedButton(
                            onClick = onDelete,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFFEF4444)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete", fontSize = 12.sp, maxLines = 1)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(2.dp))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text("Cancel", color = Color(0xFF94A3B8), fontSize = 12.sp, maxLines = 1)
                        }

                        Glossy3DActionButton(
                            text = if (existingReminder != null) "Update Reminder" else "Schedule Reminder",
                            orbType = OrbIconType.BELL,
                            onClick = {
                                onSave(title, details, reminderTimeMillis, selectedTone, selectedVibrate)
                                onDismiss()
                            },
                            modifier = Modifier.testTag("save_reminder_button")
                        )
                    }
                }
            }
        }
    }
}
