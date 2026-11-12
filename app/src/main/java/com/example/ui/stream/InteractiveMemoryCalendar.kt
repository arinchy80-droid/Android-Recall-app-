package com.example.ui.stream

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ReminderEntity
import com.example.ui.theme.NeonCalendarCheckIcon
import com.example.ui.theme.RecallCyanSecondary
import com.example.ui.theme.RecallVioletPrimary
import com.example.util.DateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Interactive Memory Calendar for the main screen.
 * Allows users to choose a day which expands to show entries and reminders for that day.
 */
@Composable
fun InteractiveMemoryCalendar(
    selectedDate: Long?,
    isExpanded: Boolean,
    monthOffset: Int,
    memoryDates: Set<Long>,
    reminderDates: Set<Long> = emptySet(),
    entriesCountOnSelectedDate: Int,
    remindersOnSelectedDate: List<ReminderEntity> = emptyList(),
    onSelectDate: (Long?) -> Unit,
    onToggleExpand: () -> Unit,
    onChangeMonth: (Int) -> Unit,
    onResetToday: () -> Unit,
    onClearDate: () -> Unit,
    onOpenReminders: (() -> Unit)? = null,
    onToggleReminderCompleted: ((ReminderEntity, Boolean) -> Unit)? = null,
    onAddNewReminderForDate: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val calendar = remember(monthOffset) {
        Calendar.getInstance().apply {
            add(Calendar.MONTH, monthOffset)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }

    val currentYear = calendar.get(Calendar.YEAR)
    val currentMonth = calendar.get(Calendar.MONTH)
    val monthTitle = remember(monthOffset) {
        DateUtils.formatMonthYear(calendar.timeInMillis)
    }

    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    // 1 = Sunday, 2 = Monday... convert to Monday-first (0 = Monday, 6 = Sunday)
    val firstDayOfWeek = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7

    val todayCalendar = remember { Calendar.getInstance() }
    val isTodayMonth = todayCalendar.get(Calendar.YEAR) == currentYear && todayCalendar.get(Calendar.MONTH) == currentMonth
    val todayDayNumber = if (isTodayMonth) todayCalendar.get(Calendar.DAY_OF_MONTH) else -1

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B1120).copy(alpha = 0.64f),
                        Color(0xFF090D18).copy(alpha = 0.72f)
                    )
                )
            )
            .border(
                width = 1.1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        RecallCyanSecondary.copy(alpha = 0.6f),
                        RecallVioletPrimary.copy(alpha = 0.55f),
                        RecallCyanSecondary.copy(alpha = 0.35f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(12.dp)
    ) {
        // 1. Calendar Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeonCalendarCheckIcon(size = 32.dp)

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = monthTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )

            // Month navigation controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = { onChangeMonth(-1) },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Month",
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(15.dp)
                    )
                }

                AssistChip(
                    onClick = onResetToday,
                    label = { Text("Today", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (monthOffset == 0) RecallVioletPrimary.copy(alpha = 0.35f) else Color(0xFF1E293B).copy(alpha = 0.6f),
                        labelColor = if (monthOffset == 0) RecallCyanSecondary else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.height(26.dp)
                )

                IconButton(
                    onClick = { onChangeMonth(1) },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Month",
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(15.dp)
                    )
                }

                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse Calendar" else "Expand Calendar",
                        tint = RecallCyanSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (selectedDate != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filtering memories for ${DateUtils.formatDayMonth(selectedDate)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = RecallCyanSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Clear filter",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    modifier = Modifier
                        .clickable { onClearDate() }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Weekday Column Headers (Mon, Tue, Wed, Thu, Fri, Sat, Sun)
        val weekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            weekdays.forEach { dayLabel ->
                Text(
                    text = dayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 2. Calendar Grid
        val totalCells = if (isExpanded) {
            val total = firstDayOfWeek + daysInMonth
            val rows = (total + 6) / 7
            rows * 7
        } else {
            7 // Compact single week
        }

        val rowsCount = totalCells / 7

        for (row in 0 until rowsCount) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col

                    if (isExpanded) {
                        val dayNumber = cellIndex - firstDayOfWeek + 1
                        if (dayNumber in 1..daysInMonth) {
                            val dayMillis = Calendar.getInstance().apply {
                                set(Calendar.YEAR, currentYear)
                                set(Calendar.MONTH, currentMonth)
                                set(Calendar.DAY_OF_MONTH, dayNumber)
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis

                            val isSelected = selectedDate != null && DateUtils.isSameDay(selectedDate, dayMillis)
                            val isToday = dayNumber == todayDayNumber
                            val hasMemories = memoryDates.any { DateUtils.isSameDay(it, dayMillis) }
                            val hasReminders = reminderDates.any { DateUtils.isSameDay(it, dayMillis) }

                            CalendarDayCell(
                                dayNumber = dayNumber,
                                isSelected = isSelected,
                                isToday = isToday,
                                hasMemories = hasMemories,
                                hasReminders = hasReminders,
                                onClick = { onSelectDate(dayMillis) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            // Blank cell outside month bounds
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    } else {
                        // Collapsed week mode: surrounding days
                        val baseDay = if (todayDayNumber > 0) todayDayNumber else 1
                        val dayNumber = (baseDay - 3 + col).coerceIn(1, daysInMonth)
                        val dayMillis = Calendar.getInstance().apply {
                            set(Calendar.YEAR, currentYear)
                            set(Calendar.MONTH, currentMonth)
                            set(Calendar.DAY_OF_MONTH, dayNumber)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis

                        val isSelected = selectedDate != null && DateUtils.isSameDay(selectedDate, dayMillis)
                        val isToday = dayNumber == todayDayNumber
                        val hasMemories = memoryDates.any { DateUtils.isSameDay(it, dayMillis) }
                        val hasReminders = reminderDates.any { DateUtils.isSameDay(it, dayMillis) }

                        CalendarDayCell(
                            dayNumber = dayNumber,
                            isSelected = isSelected,
                            isToday = isToday,
                            hasMemories = hasMemories,
                            hasReminders = hasReminders,
                            onClick = { onSelectDate(dayMillis) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 3. Selected Day Expansion Banner
        AnimatedVisibility(
            visible = selectedDate != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            if (selectedDate != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = RecallVioletPrimary.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = null,
                                tint = RecallCyanSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Entries for ${DateUtils.formatDayMonth(selectedDate)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                val memorySub = if (entriesCountOnSelectedDate > 0)
                                    "$entriesCountOnSelectedDate memory entry(s)"
                                else "No memories recorded"
                                val remSub = if (remindersOnSelectedDate.isNotEmpty())
                                    "${remindersOnSelectedDate.size} reminder(s) scheduled"
                                else null
                                Text(
                                    text = listOfNotNull(memorySub, remSub).joinToString(" • "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (entriesCountOnSelectedDate > 0 || remindersOnSelectedDate.isNotEmpty()) RecallCyanSecondary else Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }

                            // Clear filter button
                            IconButton(
                                onClick = onClearDate,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear date filter",
                                    tint = Color(0xFFE2E8F0),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Scheduled Reminders on this Day section
                        if (remindersOnSelectedDate.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Scheduled Reminders (${remindersOnSelectedDate.size})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFDE68A),
                                    fontSize = 11.sp,
                                    modifier = Modifier.weight(1f)
                                )

                                if (onOpenReminders != null) {
                                    Text(
                                        text = "View in Reminders →",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = RecallCyanSecondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier
                                            .clickable { onOpenReminders() }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            remindersOnSelectedDate.forEach { rem ->
                                val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
                                val formattedTime = timeFormat.format(Date(rem.reminderTimeMillis))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                        .background(Color(0xFF0F172A).copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (rem.isCompleted) Color(0xFF10B981) else Color.Transparent,
                                        border = BorderStroke(1.2.dp, if (rem.isCompleted) Color(0xFF10B981) else Color(0xFF94A3B8)),
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                onToggleReminderCompleted?.invoke(rem, !rem.isCompleted)
                                            }
                                    ) {
                                        if (rem.isCompleted) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = rem.title,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (rem.isCompleted) Color(0xFF64748B) else Color.White,
                                            textDecoration = if (rem.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "$formattedTime • ${rem.getReminderTone().displayName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFF59E0B),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        } else if (onAddNewReminderForDate != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { onAddNewReminderForDate(selectedDate) }
                                    .padding(vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = RecallCyanSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+ Set reminder for this day",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RecallCyanSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarDayCell(
    dayNumber: Int,
    isSelected: Boolean,
    isToday: Boolean,
    hasMemories: Boolean,
    hasReminders: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(2.dp)
            .height(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isSelected -> Brush.radialGradient(listOf(RecallCyanSecondary.copy(alpha = 0.35f), RecallVioletPrimary.copy(alpha = 0.45f)))
                    isToday -> Brush.linearGradient(listOf(RecallCyanSecondary.copy(alpha = 0.25f), Color(0xFF1E1B4B).copy(alpha = 0.65f)))
                    else -> Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                }
            )
            .then(
                if (isToday && !isSelected) {
                    Modifier.border(1.2.dp, RecallCyanSecondary, RoundedCornerShape(12.dp))
                } else if (isSelected) {
                    Modifier.border(1.5.dp, RecallCyanSecondary, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                }
            )
            .clickable { onClick() }
            .testTag("calendar_day_$dayNumber"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$dayNumber",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected || isToday) FontWeight.ExtraBold else FontWeight.Medium,
                color = when {
                    isSelected -> Color.White
                    isToday -> Color.White
                    hasMemories || hasReminders -> Color(0xFFF1F5F9)
                    else -> Color(0xFFCBD5E1)
                },
                fontSize = 12.sp
            )

            // Memory / Reminder indicator dot(s)
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(5.dp)
            ) {
                if (hasMemories) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color.White else RecallCyanSecondary)
                    )
                }
                if (hasMemories && hasReminders) {
                    Spacer(modifier = Modifier.width(3.dp))
                }
                if (hasReminders) {
                    Box(
                        modifier = Modifier
                            .size(4.5.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color(0xFFFDE68A) else Color(0xFFF59E0B))
                    )
                }
                if (!hasMemories && !hasReminders) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}
