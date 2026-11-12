package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.ActionItemEntity
import com.example.data.local.entity.PersonEntity
import com.example.data.model.NoteWithDetails
import com.example.ui.theme.RecallCyanSecondary
import com.example.ui.theme.RecallEmeraldAccent
import com.example.ui.theme.RecallVioletPrimary
import com.example.ui.theme.translucentWhiteTextFieldColors
import com.example.util.DateUtils
import com.example.util.ExportAndShareHelper
import com.example.util.HabitItem
import com.example.util.MemoryIntelligenceHelper

@Composable
fun IntelligenceHubDialog(
    initialTab: Int = 0, // 0 = Digest & Habits, 1 = Patterns & Timeline, 2 = Semantic Graph, 3 = Places
    allNotes: List<NoteWithDetails>,
    allPeople: List<PersonEntity>,
    allActions: List<ActionItemEntity>,
    habits: List<HabitItem>,
    allPlaces: List<com.example.data.local.entity.PlaceEntity> = emptyList(),
    allProjects: List<com.example.data.local.entity.ProjectEntity> = emptyList(),
    allCommitments: List<com.example.data.local.entity.CommitmentEntity> = emptyList(),
    allMemoryLinks: List<com.example.data.local.entity.MemoryLinkEntity> = emptyList(),
    onToggleHabitDate: (habitId: String, dateKey: String) -> Unit,
    onAddHabit: (title: String, icon: String) -> Unit,
    onDeleteHabit: (habitId: String) -> Unit,
    onOpenNote: (String) -> Unit,
    onSelectPerson: (PersonEntity) -> Unit,
    onFilterByLocation: (String) -> Unit,
    onFilterByTag: (String) -> Unit,
    onShowFeedback: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var newHabitTitle by remember { mutableStateOf("") }
    var selectedTimelineFilter by remember { mutableStateOf("All") }

    val digest = remember(allNotes, allPeople, allActions) {
        MemoryIntelligenceHelper.buildWeeklyIntelligenceDigest(allNotes, allPeople, allActions)
    }
    val lifeReport = remember(allNotes, allPeople, allPlaces, allProjects, allCommitments, allMemoryLinks, allActions) {
        MemoryIntelligenceHelper.buildLifeIntelligenceReport(
            allNotes = allNotes,
            allPeople = allPeople,
            allPlaces = allPlaces,
            allProjects = allProjects,
            allCommitments = allCommitments,
            allMemoryLinks = allMemoryLinks,
            allActions = allActions
        )
    }
    val topicClusters = remember(allNotes) {
        MemoryIntelligenceHelper.buildTopicClusters(allNotes)
    }
    val locationClusters = remember(allNotes) {
        MemoryIntelligenceHelper.buildLocationClusters(allNotes)
    }
    val last7Days = remember { MemoryIntelligenceHelper.getLast7DayKeys() }
    val todayKey = remember { MemoryIntelligenceHelper.getTodayDateKey() }
    val notesById = remember(allNotes) { allNotes.associateBy { it.note.id } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF0A0F1D),
            border = BorderStroke(1.5.dp, Color(0xFF334155))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    RecallVioletPrimary.copy(alpha = 0.28f),
                                    RecallCyanSecondary.copy(alpha = 0.22f)
                                )
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🧠 Recall Intelligence Hub",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "Digest • Life Patterns • Timeline • Knowledge Graph • Places",
                                style = MaterialTheme.typography.labelSmall,
                                color = RecallCyanSecondary
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_intelligence_hub_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                // 4-Tab Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val tabs = listOf(
                        Triple(0, "📊 Digest", RecallCyanSecondary),
                        Triple(1, "🧬 Patterns", Color(0xFFF59E0B)),
                        Triple(2, "🕸️ Graph", RecallVioletPrimary),
                        Triple(3, "📍 Places (${locationClusters.size})", RecallEmeraldAccent)
                    )
                    tabs.forEach { (idx, label, accent) ->
                        val active = selectedTab == idx
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) accent.copy(alpha = 0.25f) else Color(0xFF1E293B),
                            border = BorderStroke(1.dp, if (active) accent else Color(0xFF334155)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTab = idx }
                                .testTag("intel_hub_tab_$idx")
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 9.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF334155))

                // Body Content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (selectedTab == 0) {
                        // TAB 0: WEEKLY DIGEST & HABITS
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                border = BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "✨ Weekly Executive Retrospective",
                                            color = RecallCyanSecondary,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp
                                        )
                                        TextButton(
                                            onClick = {
                                                val md = buildString {
                                                    appendLine("# 📊 Recall Weekly Intelligence Digest")
                                                    appendLine("- **Generated:** ${DateUtils.formatDateTime(System.currentTimeMillis())}")
                                                    appendLine("- **Momentum:** ${digest.energyAndSentimentLabel}")
                                                    appendLine()
                                                    appendLine("## Executive Takeaway")
                                                    appendLine(digest.executiveTakeaway)
                                                    appendLine()
                                                    appendLine("## 7-Day Metrics")
                                                    appendLine("- Memories Captured: ${digest.notesThisWeek}")
                                                    appendLine("- Voice Notes: ${digest.audioNotesThisWeek}")
                                                    appendLine("- Visual Captures: ${digest.visualNotesThisWeek}")
                                                    appendLine("- Completed Tasks: ${digest.completedTasksCount}")
                                                    appendLine("- Pending Tasks: ${digest.pendingTasksCount}")
                                                }
                                                ExportAndShareHelper.exportAndSharePdfDocument(
                                                    context = context,
                                                    fileNamePrefix = "weekly_digest",
                                                    documentTitle = "Recall Weekly Intelligence Digest",
                                                    subtitle = digest.energyAndSentimentLabel,
                                                    markdownContent = md,
                                                    onResult = onShowFeedback
                                                )
                                            }
                                        ) {
                                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = RecallCyanSecondary, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Export PDF", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = digest.executiveTakeaway,
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyMedium,
                                        lineHeight = 20.sp
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        color = Color(0xFF1E293B),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, Color(0xFF334155)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Insights, contentDescription = null, tint = RecallEmeraldAccent, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Pulse: ${digest.energyAndSentimentLabel}",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        DigestStatBox(
                                            label = "7d Memories",
                                            value = "${digest.notesThisWeek}",
                                            accent = RecallVioletPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        DigestStatBox(
                                            label = "Done Tasks",
                                            value = "${digest.completedTasksCount}",
                                            accent = RecallEmeraldAccent,
                                            modifier = Modifier.weight(1f)
                                        )
                                        DigestStatBox(
                                            label = "Open Tasks",
                                            value = "${digest.pendingTasksCount}",
                                            accent = Color(0xFFF59E0B),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        // HABIT & REFLECTION TRACKER
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                border = BorderStroke(1.dp, RecallVioletPrimary.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    val completedToday = habits.count { it.isCompletedToday(todayKey) }
                                    val totalHabits = habits.size.coerceAtLeast(1)
                                    val progress = completedToday.toFloat() / totalHabits.toFloat()

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "🔥 Daily Habit & Reflection Tracker",
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "$completedToday/${habits.size} Today",
                                            color = RecallEmeraldAccent,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(7.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = RecallEmeraldAccent,
                                        trackColor = Color(0xFF1E293B)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    habits.forEach { habit ->
                                        Surface(
                                            color = Color(0xFF1E293B).copy(alpha = 0.85f),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(
                                                1.dp,
                                                if (habit.isCompletedToday(todayKey)) RecallEmeraldAccent.copy(alpha = 0.7f) else Color(0xFF334155)
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clickable { onToggleHabitDate(habit.id, todayKey) }
                                                    ) {
                                                        Text(habit.icon, fontSize = 16.sp)
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Column {
                                                            Text(
                                                                text = habit.title,
                                                                color = Color.White,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 13.sp
                                                            )
                                                            Text(
                                                                text = "🔥 Streak: ${habit.currentStreak()} day(s)",
                                                                color = Color(0xFF94A3B8),
                                                                fontSize = 10.sp
                                                            )
                                                        }
                                                    }

                                                    IconButton(
                                                        onClick = { onDeleteHabit(habit.id) },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Close,
                                                            contentDescription = "Remove habit",
                                                            tint = Color(0xFF94A3B8),
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))
                                                // 7-day interactive streak pills
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    last7Days.forEach { (dateKey, dayLabel) ->
                                                        val done = habit.completedDateKeys.contains(dateKey)
                                                        Column(
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .clickable { onToggleHabitDate(habit.id, dateKey) }
                                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = dayLabel,
                                                                color = if (dateKey == todayKey) RecallCyanSecondary else Color(0xFF94A3B8),
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                            Spacer(modifier = Modifier.height(3.dp))
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(22.dp)
                                                                    .clip(CircleShape)
                                                                    .background(
                                                                        if (done) RecallEmeraldAccent else Color(0xFF0F172A)
                                                                    )
                                                                    .border(
                                                                        1.dp,
                                                                        if (done) RecallEmeraldAccent else Color(0xFF475569),
                                                                        CircleShape
                                                                    ),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                if (done) {
                                                                    Icon(
                                                                        Icons.Default.Check,
                                                                        contentDescription = "Done",
                                                                        tint = Color(0xFF0F172A),
                                                                        modifier = Modifier.size(13.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    // Add Custom Habit Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = newHabitTitle,
                                            onValueChange = { newHabitTitle = it },
                                            placeholder = { Text("Add custom daily habit...", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                                            colors = translucentWhiteTextFieldColors()
                                        )
                                        Button(
                                            onClick = {
                                                if (newHabitTitle.isNotBlank()) {
                                                    onAddHabit(newHabitTitle.trim(), "⚡")
                                                    newHabitTitle = ""
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = RecallVioletPrimary)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Add", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        // KEEP IN TOUCH NUDGES IN DIGEST
                        if (digest.overdueCatchUpPeople.isNotEmpty()) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.65f)),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = "👥 Keep in Touch Nudges (${digest.overdueCatchUpPeople.size})",
                                            color = Color(0xFFF59E0B),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        digest.overdueCatchUpPeople.take(5).forEach { person ->
                                            Surface(
                                                color = Color(0xFF1E293B),
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .clickable {
                                                        onDismiss()
                                                        onSelectPerson(person)
                                                    }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = person.name,
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp
                                                        )
                                                        Text(
                                                            text = "${person.relationship ?: "Contact"} • ${person.daysSinceLastContact()}d since last note (${person.getCadenceLabel()})",
                                                            color = Color(0xFF94A3B8),
                                                            fontSize = 11.sp
                                                        )
                                                    }
                                                    Text(
                                                        text = "Open Dossier →",
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
                    } else if (selectedTab == 1) {
                        // TAB 1: PHASE 6 PERSONAL LIFE INTELLIGENCE, PATTERN DISCOVERY & CHRONOLOGICAL TIMELINE
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.65f)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "🧬 Personal Life Intelligence & Fulfillment",
                                        color = Color(0xFFFBBF24),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Cross-category synthesis of your commitments, projects, verified memories, and neural links.",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        DigestStatBox(
                                            label = "Fulfillment",
                                            value = "${lifeReport.commitmentFulfillmentPercent}%",
                                            accent = RecallEmeraldAccent,
                                            modifier = Modifier.weight(1f)
                                        )
                                        DigestStatBox(
                                            label = "Active Projects",
                                            value = "${lifeReport.activeProjectsCount}",
                                            accent = RecallVioletPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        DigestStatBox(
                                            label = "Neural Links",
                                            value = "${lifeReport.memoryLinksCount}",
                                            accent = RecallCyanSecondary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        DigestStatBox(
                                            label = "Verified Facts",
                                            value = "${lifeReport.verifiedFactsCount}",
                                            accent = Color(0xFFF59E0B),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        // DISCOVERED LIFE PATTERNS
                        item {
                            Text(
                                text = "🔍 Discovered Life Patterns (${lifeReport.discoveredPatterns.size})",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                        }

                        if (lifeReport.discoveredPatterns.isEmpty()) {
                            item {
                                Surface(
                                    color = Color(0xFF0F172A),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFF334155)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Capture more memories, people, places, and commitments to uncover recurring life patterns.",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            }
                        } else {
                            items(lifeReport.discoveredPatterns, key = { it.id }) { pat ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                    border = BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.55f)),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            when {
                                                pat.relatedPerson != null -> {
                                                    onDismiss()
                                                    onSelectPerson(pat.relatedPerson)
                                                }
                                                pat.relatedNoteId != null -> {
                                                    onDismiss()
                                                    onOpenNote(pat.relatedNoteId)
                                                }
                                                !pat.filterQuery.isNullOrBlank() -> {
                                                    onDismiss()
                                                    onFilterByTag(pat.filterQuery)
                                                }
                                            }
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${pat.icon} ${pat.categoryLabel.uppercase()}",
                                                color = RecallCyanSecondary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = RecallEmeraldAccent.copy(alpha = 0.18f),
                                                border = BorderStroke(1.dp, RecallEmeraldAccent.copy(alpha = 0.6f))
                                            ) {
                                                Text(
                                                    text = "${pat.strengthPercent}% signal",
                                                    color = RecallEmeraldAccent,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = pat.title,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = pat.description,
                                            color = Color(0xFFCBD5E1),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }

                        // REDISCOVER PAST MEMORIES
                        if (lifeReport.rediscoveryMemories.isNotEmpty()) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                    border = BorderStroke(1.dp, RecallVioletPrimary.copy(alpha = 0.65f)),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = "✨ Rediscover Past Memories & Ideas",
                                            color = Color(0xFFC4B5FD),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "High-value ideas, experiences, and pinned insights surfaced so they are never overlooked.",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        lifeReport.rediscoveryMemories.forEach { mem ->
                                            Surface(
                                                color = Color(0xFF1E293B),
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 3.dp)
                                                    .clickable {
                                                        onDismiss()
                                                        onOpenNote(mem.note.id)
                                                    }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = mem.note.title,
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = "${mem.note.memoryType} • ${DateUtils.formatDate(mem.note.createdAt)}",
                                                            color = Color(0xFF94A3B8),
                                                            fontSize = 10.sp
                                                        )
                                                    }
                                                    Text(
                                                        text = "Revisit →",
                                                        color = RecallCyanSecondary,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // UNIFIED CHRONOLOGICAL LIFE TIMELINE
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "⏳ Chronological Life Timeline",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("All", "Memories", "Promises", "Places").forEach { filterOpt ->
                                    val active = selectedTimelineFilter == filterOpt
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (active) RecallCyanSecondary.copy(alpha = 0.24f) else Color(0xFF1E293B),
                                        border = BorderStroke(1.dp, if (active) RecallCyanSecondary else Color(0xFF334155)),
                                        modifier = Modifier.clickable { selectedTimelineFilter = filterOpt }
                                    ) {
                                        Text(
                                            text = filterOpt,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        lifeReport.timelineEpochs.forEach { epoch ->
                            val filteredEntries = epoch.entries.filter { entry ->
                                when (selectedTimelineFilter) {
                                    "Memories" -> entry.type == com.example.util.TimelineEventType.MEMORY
                                    "Promises" -> entry.type == com.example.util.TimelineEventType.COMMITMENT
                                    "Places" -> entry.type == com.example.util.TimelineEventType.PLACE_VISIT
                                    else -> true
                                }
                            }
                            if (filteredEntries.isNotEmpty()) {
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                        border = BorderStroke(1.dp, Color(0xFF334155)),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = "🗓️ ${epoch.epochTitle} (${filteredEntries.size})",
                                                color = Color(0xFFFBBF24),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 12.sp
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            filteredEntries.take(8).forEach { entry ->
                                                Surface(
                                                    color = Color(0xFF1E293B),
                                                    shape = RoundedCornerShape(8.dp),
                                                    border = BorderStroke(1.dp, Color(0xFF334155)),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 3.dp)
                                                        .clickable {
                                                            if (entry.noteId != null) {
                                                                onDismiss()
                                                                onOpenNote(entry.noteId)
                                                            } else if (entry.person != null) {
                                                                onDismiss()
                                                                onSelectPerson(entry.person)
                                                            }
                                                        }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(entry.icon, fontSize = 15.sp)
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = entry.title,
                                                                color = Color.White,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 12.sp,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Text(
                                                                text = "${entry.subtitle} • ${DateUtils.formatDate(entry.timestamp)}",
                                                                color = Color(0xFF94A3B8),
                                                                fontSize = 10.sp,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = RecallVioletPrimary.copy(alpha = 0.22f)
                                                        ) {
                                                            Text(
                                                                text = entry.badgeLabel,
                                                                color = Color(0xFFC4B5FD),
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
                    } else if (selectedTab == 2) {
                        // TAB 2: SEMANTIC MEMORY GRAPH, NEURAL LINKS & TOPIC CLUSTERS
                        if (allMemoryLinks.isNotEmpty()) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                    border = BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "🔗 Neural Memory Graph Edges (${allMemoryLinks.size})",
                                            color = RecallCyanSecondary,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "Explicit directional links connecting how your conversations, ideas, and projects evolve.",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        allMemoryLinks.take(6).forEach { link ->
                                            val srcTitle = notesById[link.sourceNoteId]?.note?.title ?: "Memory"
                                            val dstTitle = notesById[link.targetNoteId]?.note?.title ?: "Connected Memory"
                                            Surface(
                                                color = Color(0xFF1E293B),
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 3.dp)
                                                    .clickable {
                                                        onDismiss()
                                                        onOpenNote(link.targetNoteId)
                                                    }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "$srcTitle  ➔  $dstTitle",
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = "Relation: ${link.relationType} • ${(link.strengthScore * 100).toInt()}% link strength",
                                                            color = RecallCyanSecondary,
                                                            fontSize = 10.sp
                                                        )
                                                    }
                                                    Text("Open →", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (lifeReport.categoryDistribution.isNotEmpty()) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                    border = BorderStroke(1.dp, RecallVioletPrimary.copy(alpha = 0.55f)),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "🗂️ 12-Category Unified Memory Spectrum",
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        val totalNotes = lifeReport.totalMemoriesCount.coerceAtLeast(1)
                                        lifeReport.categoryDistribution.take(6).forEach { (mType, count) ->
                                            val ratio = count.toFloat() / totalNotes.toFloat()
                                            Column(modifier = Modifier.padding(vertical = 3.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(mType, color = Color(0xFFE2E8F0), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                    Text("$count (${(ratio * 100).toInt()}%)", color = RecallCyanSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                LinearProgressIndicator(
                                                    progress = { ratio.coerceIn(0.04f, 1f) },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(5.dp)
                                                        .clip(RoundedCornerShape(3.dp)),
                                                    color = RecallCyanSecondary,
                                                    trackColor = Color(0xFF1E293B)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                text = "🕸️ Topic & Tag Knowledge Clusters",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Automatically grouped by semantic tags, shared people, and recurring themes.",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }

                        if (topicClusters.isEmpty()) {
                            item {
                                Surface(
                                    color = Color(0xFF0F172A),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, Color(0xFF334155)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.Hub, contentDescription = null, tint = RecallVioletPrimary, modifier = Modifier.size(36.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("No topic clusters yet", color = Color.White, fontWeight = FontWeight.Bold)
                                        Text(
                                            "Capture notes or add tags to watch your semantic knowledge graph grow.",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            items(topicClusters) { cluster ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                    border = BorderStroke(1.dp, RecallVioletPrimary.copy(alpha = 0.55f)),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(cluster.badgeIcon, fontSize = 18.sp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = cluster.topicName,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 15.sp
                                                )
                                            }
                                            Surface(
                                                color = RecallVioletPrimary.copy(alpha = 0.25f),
                                                shape = RoundedCornerShape(50),
                                                border = BorderStroke(1.dp, RecallVioletPrimary),
                                                modifier = Modifier.clickable {
                                                    onDismiss()
                                                    onFilterByTag(cluster.topicName.removePrefix("#"))
                                                }
                                            ) {
                                                Text(
                                                    text = "${cluster.noteCount} memories • Filter",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
                                        }

                                        if (cluster.relatedPeople.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "👥 Connected People: ${cluster.relatedPeople.joinToString(", ")}",
                                                color = RecallCyanSecondary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        cluster.notes.take(3).forEach { n ->
                                            Surface(
                                                color = Color(0xFF1E293B),
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 3.dp)
                                                    .clickable {
                                                        onDismiss()
                                                        onOpenNote(n.note.id)
                                                    }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = n.note.title,
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = DateUtils.formatDate(n.note.createdAt),
                                                            color = Color(0xFF94A3B8),
                                                            fontSize = 10.sp
                                                        )
                                                    }
                                                    Text("Open →", color = RecallCyanSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // TAB 2: GEOTAGGING & PLACES RECALL
                        item {
                            Text(
                                text = "📍 Geotagged Places & Spatial Memory",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Browse memories by physical place (Office, Coffee Shop, Home, Client Site, or GPS).",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }

                        if (locationClusters.isEmpty()) {
                            item {
                                Surface(
                                    color = Color(0xFF0F172A),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, Color(0xFF334155)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = RecallEmeraldAccent, modifier = Modifier.size(36.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("No geotagged memories yet", color = Color.White, fontWeight = FontWeight.Bold)
                                        Text(
                                            "In the Note Editor, tap '📍 Place' to attach a location tag or GPS place to any memory.",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            items(locationClusters) { locCluster ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                    border = BorderStroke(1.dp, RecallEmeraldAccent.copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = RecallEmeraldAccent, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = locCluster.locationName,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 15.sp
                                                )
                                            }
                                            Surface(
                                                color = RecallEmeraldAccent.copy(alpha = 0.22f),
                                                shape = RoundedCornerShape(50),
                                                border = BorderStroke(1.dp, RecallEmeraldAccent),
                                                modifier = Modifier.clickable {
                                                    onDismiss()
                                                    onFilterByLocation(locCluster.locationName)
                                                }
                                            ) {
                                                Text(
                                                    text = "${locCluster.notes.size} notes • Filter",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        locCluster.notes.take(4).forEach { n ->
                                            Surface(
                                                color = Color(0xFF1E293B),
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 3.dp)
                                                    .clickable {
                                                        onDismiss()
                                                        onOpenNote(n.note.id)
                                                    }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = n.note.title,
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = DateUtils.formatDateTime(n.note.createdAt),
                                                            color = Color(0xFF94A3B8),
                                                            fontSize = 10.sp
                                                        )
                                                    }
                                                    Text("Open →", color = RecallEmeraldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

@Composable
private fun DigestStatBox(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )
            Text(
                text = label,
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * 5-Second Meeting Prep Briefing & Relationship Cadence Dialog for PeopleScreen
 */
@Composable
fun MeetingPrepBriefingDialog(
    person: PersonEntity,
    personNotes: List<NoteWithDetails>,
    allActions: List<ActionItemEntity>,
    allCommitments: List<com.example.data.local.entity.CommitmentEntity> = emptyList(),
    allProjects: List<com.example.data.local.entity.ProjectEntity> = emptyList(),
    onUpdateCadenceDays: (Int) -> Unit,
    onScheduleActionReminder: (ActionItemEntity) -> Unit,
    onOpenNote: (String) -> Unit,
    onShowFeedback: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val briefing = remember(person, personNotes, allActions, allCommitments, allProjects) {
        MemoryIntelligenceHelper.generateMeetingPrepBriefing(
            person = person,
            personNotes = personNotes,
            allActions = allActions,
            allCommitments = allCommitments,
            allProjects = allProjects
        )
    }
    var selectedCadence by remember(person) { mutableIntStateOf(person.getCatchUpCadenceDays()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⚡ 5-Sec Briefing: ${person.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
                Text(
                    text = "${briefing.relationship} • ${briefing.cadenceStatus}",
                    style = MaterialTheme.typography.labelSmall,
                    color = RecallCyanSecondary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Catch-up Cadence Selector
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "🔔 Keep-in-Touch Cadence",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(7 to "7d Weekly", 14 to "14d Bi-Weekly", 30 to "30d Monthly", 90 to "90d Quarterly").forEach { (days, label) ->
                                val active = selectedCadence == days
                                Surface(
                                    color = if (active) RecallVioletPrimary else Color(0xFF0F172A),
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, if (active) RecallCyanSecondary else Color(0xFF475569)),
                                    modifier = Modifier.clickable {
                                        selectedCadence = days
                                        onUpdateCadenceDays(days)
                                    }
                                ) {
                                    Text(
                                        text = label,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Suggested Talking Points
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "💬 Recommended Talking Points",
                            color = RecallCyanSecondary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        briefing.talkingPoints.forEach { tp ->
                            Text(
                                text = "• $tp",
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }

                // Open Commitments & Shared Projects (Phase 5)
                if (briefing.openCommitments.isNotEmpty() || briefing.sharedProjects.isNotEmpty()) {
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF34D399).copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "🤝 Open Promises & Projects (${briefing.openCommitments.size} open)",
                                color = Color(0xFF34D399),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                            if (briefing.sharedProjects.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "🚀 Shared Projects: ${briefing.sharedProjects.joinToString { it.name }}",
                                    color = Color(0xFFA78BFA),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            briefing.openCommitments.take(3).forEach { comm ->
                                val dirTag = if (comm.direction.equals("TO_ME", ignoreCase = true)) "Owed to you" else "Promised by you"
                                Text(
                                    text = "• [$dirTag] ${comm.description}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Open Action Items
                if (briefing.openActionItems.isNotEmpty()) {
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "✅ Open Action Items (${briefing.openActionItems.size})",
                                color = Color(0xFFF59E0B),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            briefing.openActionItems.take(4).forEach { act ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "• ${act.task}",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(
                                        onClick = { onScheduleActionReminder(act) },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = RecallCyanSecondary, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Remind", color = RecallCyanSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Export Dossier Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val md = remember(person, personNotes, briefing.openActionItems) {
                        ExportAndShareHelper.formatPersonDossierAsMarkdown(person, personNotes, briefing.openActionItems)
                    }
                    OutlinedButton(
                        onClick = {
                            ExportAndShareHelper.exportAndSharePdfDocument(
                                context = context,
                                fileNamePrefix = "dossier_${person.name}",
                                documentTitle = "Dossier: ${person.name}",
                                subtitle = "${briefing.relationship} • ${briefing.cadenceStatus}",
                                markdownContent = md,
                                onResult = onShowFeedback
                            )
                        },
                        border = BorderStroke(1.dp, RecallCyanSecondary),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = RecallCyanSecondary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = {
                            ExportAndShareHelper.exportAndShareMarkdownFile(
                                context = context,
                                fileNamePrefix = "dossier_${person.name}",
                                title = "Dossier: ${person.name}",
                                markdownContent = md,
                                onResult = onShowFeedback
                            )
                        },
                        border = BorderStroke(1.dp, RecallVioletPrimary),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = RecallVioletPrimary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Markdown", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = {
                            ExportAndShareHelper.shareTextSummary(context, "Dossier: ${person.name}", md)
                        },
                        border = BorderStroke(1.dp, RecallEmeraldAccent),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = RecallEmeraldAccent, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = RecallVioletPrimary)
            ) {
                Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}

/**
 * Unified Export & Share Dialog for Notes, Debriefs, or Dossiers
 */
@Composable
fun ExportAndShareDialog(
    documentTitle: String,
    subtitle: String,
    fileNamePrefix: String,
    markdownContent: String,
    onShowFeedback: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = {
            Column {
                Text(
                    text = "📤 Export & Share Memory",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = documentTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = RecallCyanSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Option 1: PDF
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, RecallCyanSecondary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            ExportAndShareHelper.exportAndSharePdfDocument(
                                context = context,
                                fileNamePrefix = fileNamePrefix,
                                documentTitle = documentTitle,
                                subtitle = subtitle,
                                markdownContent = markdownContent,
                                onResult = onShowFeedback
                            )
                            onDismiss()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = RecallCyanSecondary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Export as Formatted PDF (.pdf)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Generates a styled, multi-page PDF document ready to save or send.", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                    }
                }

                // Option 2: Markdown
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, RecallVioletPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            ExportAndShareHelper.exportAndShareMarkdownFile(
                                context = context,
                                fileNamePrefix = fileNamePrefix,
                                title = documentTitle,
                                markdownContent = markdownContent,
                                onResult = onShowFeedback
                            )
                            onDismiss()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = RecallVioletPrimary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Export as Markdown File (.md)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Structured headings, tags, checklists, and verbatim transcripts.", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                    }
                }

                // Option 3: Direct Share (Email, WhatsApp, Slack)
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, RecallEmeraldAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            ExportAndShareHelper.shareTextSummary(context, documentTitle, markdownContent)
                            onDismiss()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = RecallEmeraldAccent, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Direct Share (Email / Messaging)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Send formatted summary directly to Email, Slack, WhatsApp, or Messages.", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                    }
                }

                // Option 4: Copy to Clipboard
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF475569)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            ExportAndShareHelper.copyToClipboard(context, documentTitle, markdownContent)
                            onShowFeedback("Formatted Markdown copied to clipboard!")
                            onDismiss()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Copy Formatted Text", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Copy clean Markdown summary to your device clipboard.", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        }
    )
}
