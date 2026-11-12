package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.ReminderTone
import com.example.data.local.entity.ReminderVibratePattern
import com.example.ui.theme.Glossy3DActionButton
import com.example.ui.theme.Glossy3DOrbIcon
import com.example.ui.theme.OrbIconType
import com.example.ui.theme.RecallCyanSecondary
import com.example.ui.theme.RecallVioletPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersListSheet(
    reminders: List<ReminderEntity>,
    onDismiss: () -> Unit,
    onAddNewReminder: () -> Unit,
    onEditReminder: (ReminderEntity) -> Unit,
    onToggleCompleted: (ReminderEntity, Boolean) -> Unit,
    onSnooze: (String) -> Unit,
    onDelete: (String) -> Unit,
    onPreviewTone: (ReminderTone, ReminderVibratePattern) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val timeFormatter = remember { SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault()) }

    val activeReminders = reminders.filter { !it.isCompleted }
    val completedReminders = reminders.filter { it.isCompleted }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF070C1A),
        scrimColor = Color.Black.copy(alpha = 0.65f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Glossy3DOrbIcon(
                    orbType = OrbIconType.BELL,
                    size = 36.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Physical Message Reminders",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "${activeReminders.size} active • Tone & vibration alarms",
                        style = MaterialTheme.typography.labelSmall,
                        color = RecallCyanSecondary
                    )
                }
                Glossy3DActionButton(
                    text = "+ New",
                    orbType = OrbIconType.PLUS,
                    onClick = onAddNewReminder,
                    height = 36.dp,
                    modifier = Modifier.testTag("sheet_add_reminder_btn")
                )
            }

            HorizontalDivider(
                color = Color(0xFF1E293B),
                thickness = 1.dp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (reminders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF0F172A),
                            border = BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.4f)),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = RecallCyanSecondary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No reminders scheduled",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Set reminders on any memory or note with custom physical message tones.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Glossy3DActionButton(
                            text = "Schedule First Reminder",
                            orbType = OrbIconType.BELL,
                            onClick = onAddNewReminder
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (activeReminders.isNotEmpty()) {
                        item {
                            Text(
                                text = "UPCOMING REMINDERS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = RecallCyanSecondary,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(activeReminders, key = { it.id }) { reminder ->
                            ReminderCardItem(
                                reminder = reminder,
                                timeFormatter = timeFormatter,
                                onClick = { onEditReminder(reminder) },
                                onToggleCompleted = { onToggleCompleted(reminder, true) },
                                onSnooze = { onSnooze(reminder.id) },
                                onDelete = { onDelete(reminder.id) },
                                onPreviewTone = onPreviewTone
                            )
                        }
                    }

                    if (completedReminders.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "COMPLETED",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF64748B),
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(completedReminders, key = { it.id }) { reminder ->
                            ReminderCardItem(
                                reminder = reminder,
                                timeFormatter = timeFormatter,
                                onClick = { onEditReminder(reminder) },
                                onToggleCompleted = { onToggleCompleted(reminder, false) },
                                onSnooze = { onSnooze(reminder.id) },
                                onDelete = { onDelete(reminder.id) },
                                onPreviewTone = onPreviewTone
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ReminderCardItem(
    reminder: ReminderEntity,
    timeFormatter: SimpleDateFormat,
    onClick: () -> Unit,
    onToggleCompleted: () -> Unit,
    onSnooze: () -> Unit,
    onDelete: () -> Unit,
    onPreviewTone: (ReminderTone, ReminderVibratePattern) -> Unit,
    onOpenNote: ((String) -> Unit)? = null
) {
    val tone = reminder.getReminderTone()
    val pattern = reminder.getVibrationPattern()
    val isPast = reminder.reminderTimeMillis < System.currentTimeMillis()

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (reminder.isCompleted) Color(0xFF0F172A).copy(alpha = 0.45f) else Color(0xFF0F172A).copy(alpha = 0.68f),
        border = BorderStroke(
            1.dp,
            if (reminder.isCompleted) Color(0xFF1E293B) else Color.White.copy(alpha = 0.22f)
        ),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Left frosted icon badge with Bell icon matching Screenshot 3
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onToggleCompleted() }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = if (reminder.isCompleted) Icons.Default.Check else Icons.Default.Notifications,
                            contentDescription = "Toggle Done",
                            tint = if (reminder.isCompleted) Color(0xFF10B981) else Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reminder.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (reminder.isCompleted) Color(0xFF94A3B8) else Color.White,
                        textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val recurrenceLabel = if (reminder.details.contains("every", ignoreCase = true))
                            " • " + reminder.details.take(24) else ""
                        Text(
                            text = "🕒 ${timeFormatter.format(Date(reminder.reminderTimeMillis))}$recurrenceLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (reminder.isCompleted) Color(0xFF64748B) else Color(0xFFE2E8F0),
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }

                // Right Action Icons: Edit pencil & Delete trash matching Screenshot 3
                IconButton(
                    onClick = onClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Reminder",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(17.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Reminder",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            if (reminder.details.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = reminder.details,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCBD5E1),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!reminder.noteId.isNullOrBlank() && onOpenNote != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = RecallVioletPrimary.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, RecallVioletPrimary.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenNote(reminder.noteId) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "View Attached Memory",
                            tint = RecallCyanSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Linked Memory • Tap to open",
                            style = MaterialTheme.typography.labelSmall,
                            color = RecallCyanSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tone & Vibration Badge with Instant Play button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0A1020), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = RecallVioletPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${tone.displayName} • ${pattern.displayName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = RecallVioletPrimary.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, RecallVioletPrimary.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onPreviewTone(tone, pattern) }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Preview Tone",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Test Tone", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
