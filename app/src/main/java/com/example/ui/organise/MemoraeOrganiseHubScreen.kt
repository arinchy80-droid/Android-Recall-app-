package com.example.ui.organise

import android.content.Intent
import android.provider.CalendarContract
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.ActionItemEntity
import com.example.data.local.entity.CommitmentEntity
import com.example.data.local.entity.PersonEntity
import com.example.data.model.NoteWithDetails
import com.example.ui.RecallViewModel
import com.example.ui.people.AddCommitmentDialog
import com.example.ui.people.AddOrEditProjectDialog
import com.example.ui.people.ProjectDossierView
import com.example.ui.people.ProjectsHubContent
import com.example.ui.reminders.RemindersScreen
import com.example.ui.theme.NeuralAppBackground
import com.example.ui.theme.RecallCyanSecondary
import com.example.ui.theme.RecallEmeraldAccent
import com.example.ui.theme.RecallVioletPrimary
import com.example.ui.theme.seamlessInnerTextFieldColors
import com.example.util.CommitmentClassification
import com.example.util.DateUtils
import com.example.util.DynamicList
import com.example.util.DynamicListType
import com.example.util.RecallLifeOrganiserManager
import com.example.util.TaskBoardColumn
import com.example.util.TaskExtendedMeta
import com.example.util.TaskPriority
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ORGANISE_TABS = listOf(
    "Commitments" to "🤝",
    "Reminders" to "🔔",
    "Tasks" to "✅",
    "Lists" to "📋",
    "Projects" to "🚀",
    "Calendar" to "📅",
    "Upcoming Events" to "🎉",
    "Completed" to "🏆"
)

@Composable
fun MemoraeOrganiseHubScreen(
    viewModel: RecallViewModel,
    onOpenNote: (String) -> Unit
) {
    val activeTab by viewModel.organiseSubTab.collectAsStateWithLifecycle()
    val allCommitments by viewModel.allCommitments.collectAsStateWithLifecycle()
    val allPeople by viewModel.people.collectAsStateWithLifecycle()
    val allProjects by viewModel.allProjects.collectAsStateWithLifecycle()
    val allNotes by viewModel.notes.collectAsStateWithLifecycle()
    val actionItems by viewModel.actionItems.collectAsStateWithLifecycle()
    val taskMetaMap by viewModel.taskMetaMap.collectAsStateWithLifecycle()
    val dynamicLists by viewModel.dynamicLists.collectAsStateWithLifecycle()
    val activeReminders by viewModel.activeReminders.collectAsStateWithLifecycle()
    val selectedProject by viewModel.selectedProject.collectAsStateWithLifecycle()

    var showAddProjectDialog by remember { mutableStateOf(false) }
    var showAddCommitmentDialog by remember { mutableStateOf(false) }

    NeuralAppBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Organise Navigation Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ORGANISE_TABS.forEach { (tabName, emoji) ->
                    val isSelected = activeTab == tabName
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) RecallVioletPrimary else Color(0xFF0F172A).copy(alpha = 0.88f),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) RecallCyanSecondary else Color(0xFF334155)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { viewModel.organiseSubTab.value = tabName }
                            .testTag("organise_tab_${tabName.lowercase().replace(" ", "_")}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = emoji, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tabName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (activeTab) {
                    "Commitments" -> {
                        ForgottenPromisesAndCommitmentsView(
                            viewModel = viewModel,
                            allCommitments = allCommitments,
                            allPeople = allPeople,
                            allProjects = allProjects,
                            onAddCommitmentClick = { showAddCommitmentDialog = true },
                            onOpenSourceNote = onOpenNote
                        )
                    }

                    "Reminders" -> {
                        RemindersScreen(
                            viewModel = viewModel,
                            onBack = { viewModel.organiseSubTab.value = "Commitments" },
                            onOpenNote = onOpenNote
                        )
                    }

                    "Tasks" -> {
                        MemoraeKanbanTasksView(
                            viewModel = viewModel,
                            actionItems = actionItems,
                            taskMetaMap = taskMetaMap,
                            allPeople = allPeople,
                            allProjects = allProjects,
                            onOpenSourceNote = onOpenNote
                        )
                    }

                    "Lists" -> {
                        MemoraeDynamicListsView(
                            viewModel = viewModel,
                            dynamicLists = dynamicLists,
                            allNotes = allNotes
                        )
                    }

                    "Projects" -> {
                        if (selectedProject != null) {
                            val proj = selectedProject!!
                            val linkedNotes = remember(proj, allNotes) {
                                allNotes.filter { it.note.projectId == proj.id }
                            }
                            val projCommitments = remember(proj, allCommitments) {
                                allCommitments.filter { it.projectId == proj.id }
                            }
                            ProjectDossierView(
                                project = proj,
                                linkedNotes = linkedNotes,
                                projectCommitments = projCommitments,
                                onBack = { viewModel.clearSelectedProject() },
                                onUpdateProject = { viewModel.saveProject(it) },
                                onDeleteProject = { viewModel.deleteProject(it) },
                                onStartNoteForProject = { viewModel.startNoteForProject(it) },
                                onOpenNote = { viewModel.openNoteEditor(it) },
                                onToggleCommitment = { id, done -> viewModel.toggleCommitmentCompleted(id, done) },
                                onAddCommitmentClick = { showAddCommitmentDialog = true }
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                                ProjectsHubContent(
                                    projects = allProjects,
                                    allNotes = allNotes,
                                    allCommitments = allCommitments,
                                    onSelectProject = { viewModel.selectProject(it) },
                                    onAddProjectClick = { showAddProjectDialog = true }
                                )
                            }
                        }
                    }

                    "Calendar" -> {
                        CalendarAndUpcomingEventsView(
                            viewModel = viewModel,
                            activeReminders = activeReminders,
                            allCommitments = allCommitments,
                            allNotes = allNotes,
                            onOpenNote = onOpenNote
                        )
                    }

                    "Upcoming Events" -> {
                        UpcomingEventsOrganiseView(
                            viewModel = viewModel,
                            allNotes = allNotes,
                            allPeople = allPeople,
                            allCommitments = allCommitments,
                            activeReminders = activeReminders,
                            onOpenNote = onOpenNote
                        )
                    }

                    "Completed" -> {
                        CompletedItemsLedgerView(
                            viewModel = viewModel,
                            allCommitments = allCommitments,
                            actionItems = actionItems,
                            dynamicLists = dynamicLists,
                            onOpenSourceNote = onOpenNote
                        )
                    }
                }
            }
        }

        if (showAddProjectDialog) {
            AddOrEditProjectDialog(
                initialProject = null,
                onDismiss = { showAddProjectDialog = false },
                onSave = { newProj ->
                    viewModel.saveProject(newProj)
                    showAddProjectDialog = false
                }
            )
        }

        if (showAddCommitmentDialog) {
            AddCommitmentDialog(
                people = allPeople,
                projects = allProjects,
                preselectedPersonId = null,
                preselectedProjectId = selectedProject?.id,
                onDismiss = { showAddCommitmentDialog = false },
                onSave = { newComm ->
                    viewModel.saveCommitment(newComm)
                    showAddCommitmentDialog = false
                }
            )
        }
    }
}

@Composable
private fun ForgottenPromisesAndCommitmentsView(
    viewModel: RecallViewModel,
    allCommitments: List<CommitmentEntity>,
    allPeople: List<PersonEntity>,
    allProjects: List<com.example.data.local.entity.ProjectEntity>,
    onAddCommitmentClick: () -> Unit,
    onOpenSourceNote: (String) -> Unit
) {
    val filters = listOf("All Open", "Explicit Promises", "Follow-Ups", "Undated", "Overdue", "Completed")
    var selectedFilter by remember { mutableStateOf("All Open") }
    var commitmentToConfirm by remember { mutableStateOf<CommitmentEntity?>(null) }
    var commitmentToDate by remember { mutableStateOf<CommitmentEntity?>(null) }

    val peopleById = remember(allPeople) { allPeople.associateBy { it.id } }
    val now = System.currentTimeMillis()

    val filteredCommitments = remember(allCommitments, selectedFilter) {
        allCommitments.filter { comm ->
            val cls = RecallLifeOrganiserManager.classifyCommitment(comm)
            when (selectedFilter) {
                "All Open" -> !comm.isCompleted
                "Explicit Promises" -> !comm.isCompleted && cls == CommitmentClassification.EXPLICIT_PROMISE
                "Follow-Ups" -> !comm.isCompleted && cls == CommitmentClassification.FOLLOW_UP
                "Undated" -> !comm.isCompleted && comm.dueDate == null
                "Overdue" -> !comm.isCompleted && comm.dueDate != null && comm.dueDate < now
                "Completed" -> comm.isCompleted
                else -> true
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 104.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.88f)),
                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.65f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🤝 Forgotten Promises & Commitments",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "Tracks promises, intentions, follow-ups, and undated commitments from your conversations.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onAddCommitmentClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("+ Promise", color = Color(0xFF050811), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        filters.forEach { f ->
                            val isSel = selectedFilter == f
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) Color(0xFFF59E0B) else Color(0xFF1E293B),
                                modifier = Modifier.clickable { selectedFilter = f }
                            ) {
                                Text(
                                    text = f,
                                    fontSize = 11.sp,
                                    color = if (isSel) Color(0xFF050811) else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (filteredCommitments.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No commitments in '$selectedFilter'. Tap '+ Promise' or mention commitments during a Daily Debrief.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(filteredCommitments, key = { it.id }) { comm ->
                val cls = RecallLifeOrganiserManager.classifyCommitment(comm)
                val person = comm.personId?.let { peopleById[it] }
                val isOverdue = !comm.isCompleted && comm.dueDate != null && comm.dueDate < now

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.88f)),
                    border = BorderStroke(
                        1.dp,
                        when {
                            comm.isCompleted -> RecallEmeraldAccent.copy(alpha = 0.55f)
                            isOverdue -> Color(0xFFEF4444)
                            else -> Color(0xFFF59E0B).copy(alpha = 0.6f)
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.dp, Color(0xFF475569))
                            ) {
                                Text(
                                    text = "${cls.badgeIcon} ${cls.label}",
                                    fontSize = 10.sp,
                                    color = Color(0xFFFDE68A),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            person?.let { p ->
                                Text(
                                    text = "👤 ${p.name}",
                                    fontSize = 11.sp,
                                    color = RecallCyanSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            if (!comm.noteId.isNullOrBlank()) {
                                Text(
                                    text = "Source Memory →",
                                    fontSize = 10.sp,
                                    color = RecallCyanSecondary,
                                    modifier = Modifier.clickable { onOpenSourceNote(comm.noteId) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = comm.description,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            textDecoration = if (comm.isCompleted) TextDecoration.LineThrough else null
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (comm.dueDate != null) {
                                Text(
                                    text = if (isOverdue) "⚠️ Overdue (${DateUtils.formatDayMonth(comm.dueDate)})"
                                    else "📅 Due ${DateUtils.formatDayMonth(comm.dueDate)}",
                                    fontSize = 11.sp,
                                    color = if (isOverdue) Color(0xFFEF4444) else Color(0xFFCBD5E1),
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Text(
                                    text = "Undated — Assign a date or reminder?",
                                    fontSize = 11.sp,
                                    color = Color(0xFFFDE68A),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedButton(
                                    onClick = { commitmentToDate = comm },
                                    border = BorderStroke(1.dp, RecallCyanSecondary),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Assign Date", color = RecallCyanSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (!comm.isCompleted) {
                                Button(
                                    onClick = { commitmentToConfirm = comm },
                                    colors = ButtonDefaults.buttonColors(containerColor = RecallEmeraldAccent),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("✓ Complete", color = Color(0xFF050811), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            } else {
                                TextButton(onClick = { viewModel.toggleCommitmentCompleted(comm.id, false) }) {
                                    Text("Reopen", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    commitmentToConfirm?.let { comm ->
        AlertDialog(
            onDismissRequest = { commitmentToConfirm = null },
            containerColor = Color(0xFF0F172A),
            title = { Text("Confirm Commitment Completed?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Mark \"${comm.description}\" as fulfilled?", color = Color(0xFFCBD5E1)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.toggleCommitmentCompleted(comm.id, true)
                        commitmentToConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RecallEmeraldAccent)
                ) {
                    Text("Confirm Completed", color = Color(0xFF050811), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { commitmentToConfirm = null }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    commitmentToDate?.let { comm ->
        val oneDay = 24L * 3600L * 1000L
        AlertDialog(
            onDismissRequest = { commitmentToDate = null },
            containerColor = Color(0xFF0F172A),
            title = { Text("Assign Due Date & Reminder", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val target = System.currentTimeMillis() + oneDay
                            viewModel.assignCommitmentDueDate(comm, target)
                            viewModel.scheduleReminder("Promise: ${comm.description}", "Scheduled from Organise", comm.noteId, target)
                            commitmentToDate = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = RecallVioletPrimary)
                    ) {
                        Text("Tomorrow + Set Alarm", color = Color.White)
                    }
                    Button(
                        onClick = {
                            val target = System.currentTimeMillis() + 3 * oneDay
                            viewModel.assignCommitmentDueDate(comm, target)
                            commitmentToDate = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Text("In 3 Days", color = Color.White)
                    }
                    Button(
                        onClick = {
                            val target = System.currentTimeMillis() + 7 * oneDay
                            viewModel.assignCommitmentDueDate(comm, target)
                            commitmentToDate = null
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
                TextButton(onClick = { commitmentToDate = null }) {
                    Text("Close", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}

@Composable
private fun MemoraeKanbanTasksView(
    viewModel: RecallViewModel,
    actionItems: List<ActionItemEntity>,
    taskMetaMap: Map<String, TaskExtendedMeta>,
    allPeople: List<PersonEntity>,
    allProjects: List<com.example.data.local.entity.ProjectEntity>,
    onOpenSourceNote: (String) -> Unit
) {
    var selectedColumn by remember { mutableStateOf(TaskBoardColumn.TODAY) }
    var newTaskTitle by remember { mutableStateOf("") }
    var newTaskPriority by remember { mutableStateOf(TaskPriority.MEDIUM) }

    val filteredTasks = remember(actionItems, taskMetaMap, selectedColumn) {
        actionItems.filter { item ->
            RecallLifeOrganiserManager.resolveTaskBoardColumn(item, taskMetaMap[item.id]) == selectedColumn
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 104.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.88f)),
                border = BorderStroke(1.dp, RecallEmeraldAccent.copy(alpha = 0.65f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "✅ Memorae Task Board",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Organise tasks into Today, This Week, Queue, and Completed without losing source memory links.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    // Kanban Column Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TaskBoardColumn.entries.forEach { col ->
                            val isSel = selectedColumn == col
                            val count = actionItems.count {
                                RecallLifeOrganiserManager.resolveTaskBoardColumn(it, taskMetaMap[it.id]) == col
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) RecallEmeraldAccent else Color(0xFF1E293B),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedColumn = col }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${col.icon} ${col.label}",
                                        fontSize = 11.sp,
                                        color = if (isSel) Color(0xFF050811) else Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "$count",
                                        fontSize = 10.sp,
                                        color = if (isSel) Color(0xFF050811) else RecallCyanSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    // Priority Selector + Add Task Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TaskPriority.entries.forEach { prio ->
                            val isSel = newTaskPriority == prio
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) RecallVioletPrimary else Color(0xFF1E293B),
                                modifier = Modifier.clickable { newTaskPriority = prio }
                            ) {
                                Text(
                                    text = "${prio.badge} ${prio.label}",
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newTaskTitle,
                            onValueChange = { newTaskTitle = it },
                            placeholder = {
                                Text("Add task to ${selectedColumn.label}...", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            },
                            singleLine = true,
                            colors = seamlessInnerTextFieldColors(),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E293B))
                                .testTag("organise_add_task_input")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newTaskTitle.isNotBlank()) {
                                    viewModel.createRichActionItem(
                                        task = newTaskTitle,
                                        priority = newTaskPriority,
                                        column = selectedColumn
                                    )
                                    newTaskTitle = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RecallEmeraldAccent)
                        ) {
                            Text("Add", color = Color(0xFF050811), fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }

        if (filteredTasks.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No tasks in ${selectedColumn.label}. Add one above or move tasks across columns.",
                        color = Color(0xFF94A3B8),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(filteredTasks, key = { it.id }) { task ->
                val meta = taskMetaMap[task.id]
                val prio = meta?.priority ?: TaskPriority.MEDIUM
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = task.isCompleted,
                                onCheckedChange = { checked ->
                                    viewModel.toggleActionItem(task.id, checked)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = RecallEmeraldAccent)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.task,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                                )
                                Text(
                                    text = "${prio.badge} ${prio.label} Priority",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RecallCyanSecondary
                                )
                            }
                            if (!task.noteId.isNullOrBlank()) {
                                TextButton(onClick = { onOpenSourceNote(task.noteId) }) {
                                    Text("Memory →", color = RecallCyanSecondary, fontSize = 11.sp)
                                }
                            }
                            IconButton(
                                onClick = { viewModel.deleteActionItem(task.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Task", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                            }
                        }

                        // Quick Column Move Pills
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TaskBoardColumn.entries.forEach { targetCol ->
                                if (targetCol != selectedColumn) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF1E293B),
                                        border = BorderStroke(1.dp, Color(0xFF475569)),
                                        modifier = Modifier.clickable {
                                            viewModel.updateTaskMeta(task.id, column = targetCol)
                                        }
                                    ) {
                                        Text(
                                            text = "→ ${targetCol.label}",
                                            fontSize = 10.sp,
                                            color = Color(0xFFCBD5E1),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
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

@Composable
private fun MemoraeDynamicListsView(
    viewModel: RecallViewModel,
    dynamicLists: List<DynamicList>,
    allNotes: List<NoteWithDetails>
) {
    val context = LocalContext.current
    var showCreateListDialog by remember { mutableStateOf(false) }
    var newListTitle by remember { mutableStateOf("") }
    var newListType by remember { mutableStateOf(DynamicListType.SHOPPING) }
    var newListInitialItems by remember { mutableStateOf("") }
    var showPendingOnly by remember { mutableStateOf(false) }
    val itemDraftMap = remember { androidx.compose.runtime.mutableStateMapOf<String, String>() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 104.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.88f)),
                border = BorderStroke(1.dp, RecallVioletPrimary.copy(alpha = 0.65f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "📋 Dynamic Lists (Shopping, To-Do, Ideas, Reading, Watch)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "Add items on the go, filter pending items during errands, clear finished items in one tap, or share lists.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { showCreateListDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = RecallVioletPrimary),
                            modifier = Modifier.testTag("create_dynamic_list_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New List", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (showPendingOnly) RecallCyanSecondary else Color(0xFF1E293B),
                            modifier = Modifier.clickable { showPendingOnly = !showPendingOnly }
                        ) {
                            Text(
                                text = if (showPendingOnly) "✓ Showing Pending Only" else "Filter: Show Pending Only",
                                fontSize = 11.sp,
                                color = if (showPendingOnly) Color(0xFF050811) else Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                        DynamicListType.entries.forEach { t ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier.clickable {
                                    newListType = t
                                    newListTitle = t.displayName
                                    showCreateListDialog = true
                                }
                            ) {
                                Text(
                                    text = "+ ${t.icon} ${t.displayName}",
                                    fontSize = 11.sp,
                                    color = Color(0xFFCBD5E1),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (dynamicLists.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No dynamic lists yet. Tap '+ Shopping List', '+ Reading List', or 'New List' above to create one.",
                        color = Color(0xFF94A3B8),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(dynamicLists, key = { it.id }) { list ->
                val visibleItems = if (showPendingOnly) list.items.filter { !it.isCompleted } else list.items
                val draftText = itemDraftMap[list.id] ?: ""

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.88f)),
                    border = BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${list.type.icon} ${list.title}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${list.pendingCount} pending",
                                style = MaterialTheme.typography.labelSmall,
                                color = RecallCyanSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = {
                                    val shareBody = buildString {
                                        appendLine("${list.type.icon} ${list.title}")
                                        list.items.forEach { item ->
                                            appendLine("${if (item.isCompleted) "[x]" else "[ ]"} ${item.text}")
                                        }
                                    }
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, list.title)
                                        putExtra(Intent.EXTRA_TEXT, shareBody)
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share ${list.title}"))
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share List", tint = RecallCyanSecondary, modifier = Modifier.size(16.dp))
                            }
                            if (list.completedCount > 0) {
                                TextButton(onClick = { viewModel.clearCompletedInDynamicList(list.id) }) {
                                    Text("Clear Done (${list.completedCount})", color = Color(0xFFF59E0B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            IconButton(
                                onClick = { viewModel.deleteDynamicList(list.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete List", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        // Quick Add Item Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = draftText,
                                onValueChange = { itemDraftMap[list.id] = it },
                                placeholder = {
                                    Text("Add item(s) (comma-separated supported)...", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                },
                                singleLine = true,
                                colors = seamlessInnerTextFieldColors(),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF1E293B))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = {
                                    if (draftText.isNotBlank()) {
                                        viewModel.addDynamicListItem(list.id, draftText)
                                        itemDraftMap[list.id] = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RecallCyanSecondary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("Add", color = Color(0xFF050811), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        visibleItems.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleDynamicListItem(list.id, item.id) }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = item.isCompleted,
                                    onCheckedChange = { viewModel.toggleDynamicListItem(list.id, item.id) },
                                    colors = CheckboxDefaults.colors(checkedColor = RecallEmeraldAccent),
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (item.isCompleted) Color(0xFF64748B) else Color.White,
                                    textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateListDialog) {
        AlertDialog(
            onDismissRequest = { showCreateListDialog = false },
            containerColor = Color(0xFF0F172A),
            title = { Text("Create Dynamic List", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DynamicListType.entries.forEach { t ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (newListType == t) RecallVioletPrimary else Color(0xFF1E293B),
                                modifier = Modifier.clickable { newListType = t }
                            ) {
                                Text(
                                    text = "${t.icon} ${t.displayName}",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = newListTitle,
                        onValueChange = { newListTitle = it },
                        placeholder = { Text(newListType.displayName, fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                        singleLine = true,
                        colors = seamlessInnerTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E293B))
                    )
                    OutlinedTextField(
                        value = newListInitialItems,
                        onValueChange = { newListInitialItems = it },
                        placeholder = { Text("Optional items (comma or newline separated)", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                        minLines = 2,
                        colors = seamlessInnerTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E293B))
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val items = RecallLifeOrganiserManager.extractListItemsFromText(newListInitialItems)
                        viewModel.createDynamicList(
                            title = newListTitle.ifBlank { newListType.displayName },
                            type = newListType,
                            initialItems = items
                        )
                        newListTitle = ""
                        newListInitialItems = ""
                        showCreateListDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RecallEmeraldAccent)
                ) {
                    Text("Create List", color = Color(0xFF050811), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateListDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}

@Composable
private fun CalendarAndUpcomingEventsView(
    viewModel: RecallViewModel,
    activeReminders: List<com.example.data.local.entity.ReminderEntity>,
    allCommitments: List<CommitmentEntity>,
    allNotes: List<NoteWithDetails>,
    onOpenNote: (String) -> Unit
) {
    val context = LocalContext.current
    val recurrenceMap by viewModel.reminderRecurrenceMap.collectAsStateWithLifecycle()
    var quickNaturalInput by remember { mutableStateOf("") }
    var selectedRecurrence by remember { mutableStateOf("NONE") }

    val suggestion = remember(quickNaturalInput) {
        RecallLifeOrganiserManager.parseNaturalDateTimeSuggestion(quickNaturalInput)
    }
    val fmt = remember { SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 104.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.88f)),
                border = BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.65f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "📅 Calendar, Recurring Reminders & Google Calendar Sync",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Type a natural date (e.g. 'Call Peter next Friday at 2pm') to schedule a one-time or recurring reminder, or export to Google Calendar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = quickNaturalInput,
                        onValueChange = { quickNaturalInput = it },
                        placeholder = {
                            Text("e.g., Call Peter next Friday at 10am...", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        },
                        singleLine = true,
                        colors = seamlessInnerTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E293B))
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("NONE" to "One-Time", "DAILY" to "↻ Daily", "WEEKLY" to "↻ Weekly", "MONTHLY" to "↻ Monthly").forEach { (key, label) ->
                            val isSel = selectedRecurrence == key
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) RecallVioletPrimary else Color(0xFF1E293B),
                                modifier = Modifier.clickable { selectedRecurrence = key }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    suggestion?.let { sug ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.scheduleReminder(
                                        title = sug.suggestedAction,
                                        details = sug.originalText,
                                        timeMillis = sug.suggestedTimeMillis,
                                        recurrence = if (selectedRecurrence != "NONE") selectedRecurrence else (sug.recurrenceHint ?: "NONE")
                                    )
                                    quickNaturalInput = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RecallEmeraldAccent),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Schedule (${sug.formattedDateLabel})", color = Color(0xFF050811), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val calIntent = Intent(Intent.ACTION_INSERT).apply {
                                            data = CalendarContract.Events.CONTENT_URI
                                            putExtra(CalendarContract.Events.TITLE, sug.suggestedAction)
                                            putExtra(CalendarContract.Events.DESCRIPTION, sug.originalText)
                                            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, sug.suggestedTimeMillis)
                                            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, sug.suggestedTimeMillis + 3_600_000L)
                                        }
                                        context.startActivity(calIntent)
                                    } catch (e: Throwable) {
                                        viewModel.showFeedback("Could not launch Google Calendar: ${e.message}")
                                    }
                                },
                                border = BorderStroke(1.dp, RecallCyanSecondary)
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = RecallCyanSecondary, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Google Cal", color = RecallCyanSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        if (activeReminders.isNotEmpty()) {
            item {
                Text(
                    text = "🔔 Upcoming Scheduled Alarms (${activeReminders.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
            items(activeReminders, key = { "cal_rem_${it.id}" }) { rem ->
                val rec = recurrenceMap[rem.id] ?: "NONE"
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = rem.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            val recBadge = if (rec != "NONE") " • ↻ $rec" else ""
                            Text(
                                text = "${fmt.format(Date(rem.reminderTimeMillis))}$recBadge",
                                style = MaterialTheme.typography.labelSmall,
                                color = RecallCyanSecondary
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                try {
                                    val calIntent = Intent(Intent.ACTION_INSERT).apply {
                                        data = CalendarContract.Events.CONTENT_URI
                                        putExtra(CalendarContract.Events.TITLE, rem.title)
                                        putExtra(CalendarContract.Events.DESCRIPTION, rem.details)
                                        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, rem.reminderTimeMillis)
                                        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, rem.reminderTimeMillis + 3_600_000L)
                                    }
                                    context.startActivity(calIntent)
                                } catch (e: Throwable) {
                                    viewModel.showFeedback("Calendar app not found.")
                                }
                            },
                            border = BorderStroke(1.dp, Color(0xFF475569)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("+ Google Cal", color = Color.White, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpcomingEventsOrganiseView(
    viewModel: RecallViewModel,
    allNotes: List<NoteWithDetails>,
    allPeople: List<PersonEntity>,
    allCommitments: List<CommitmentEntity>,
    activeReminders: List<com.example.data.local.entity.ReminderEntity>,
    onOpenNote: (String) -> Unit
) {
    val context = LocalContext.current
    val converters = remember { com.example.data.local.converter.RoomConverters() }

    val eventNotes = remember(allNotes) {
        allNotes.filter {
            it.note.memoryType == "EVENTS" ||
                it.note.memoryType == "IMPORTANT_DATES" ||
                it.note.category.equals("Event", ignoreCase = true) ||
                it.note.category.equals("Events", ignoreCase = true)
        }
    }

    val peopleKeyDates = remember(allPeople) {
        allPeople.flatMap { person ->
            converters.toStringMap(person.keyDatesJson)
                .filterKeys { it != "CatchUpCadenceDays" }
                .map { (label, dateVal) -> Triple(person, label, dateVal) }
        }
    }

    val datedCommitments = remember(allCommitments) {
        allCommitments.filter { !it.isCompleted && it.dueDate != null }.sortedBy { it.dueDate }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 104.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.88f)),
                border = BorderStroke(1.dp, RecallVioletPrimary.copy(alpha = 0.65f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🎉 Upcoming Events, Milestones & Important Dates",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Unified view of birthdays, anniversaries, scheduled events, and dated commitments.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.openNoteEditor(null) },
                        colors = ButtonDefaults.buttonColors(containerColor = RecallVioletPrimary)
                    ) {
                        Text("+ Event", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (peopleKeyDates.isNotEmpty()) {
            item {
                Text(
                    text = "🎂 People Key Dates & Milestones (${peopleKeyDates.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
            items(peopleKeyDates, key = { "${it.first.id}_${it.second}" }) { (person, label, dateVal) ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${person.name} — $label",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "📅 $dateVal",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFDE68A)
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                val tomorrow = System.currentTimeMillis() + 24L * 3600L * 1000L
                                viewModel.scheduleReminder(
                                    title = "${person.name}: $label ($dateVal)",
                                    details = "Upcoming milestone reminder",
                                    timeMillis = tomorrow
                                )
                            },
                            border = BorderStroke(1.dp, RecallCyanSecondary),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("🔔 Remind", color = RecallCyanSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (datedCommitments.isNotEmpty()) {
            item {
                Text(
                    text = "🤝 Dated Commitments (${datedCommitments.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
            items(datedCommitments, key = { "event_comm_${it.id}" }) { comm ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = comm.description,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            comm.dueDate?.let { due ->
                                Text(
                                    text = "Due ${DateUtils.formatDayMonth(due)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RecallCyanSecondary
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                comm.dueDate?.let { due ->
                                    com.example.util.AppIntegrationsHelper.scheduleInCalendar(
                                        context = context,
                                        title = comm.description,
                                        description = "Recall Commitment",
                                        explicitTimeMillis = due
                                    )
                                }
                            },
                            border = BorderStroke(1.dp, Color(0xFF475569)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("+ Google Cal", color = Color.White, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        if (eventNotes.isNotEmpty()) {
            item {
                Text(
                    text = "🗓️ Logged Event Memories (${eventNotes.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
            items(eventNotes, key = { "ev_note_${it.note.id}" }) { item ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenNote(item.note.id) }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = item.note.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = item.note.content,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompletedItemsLedgerView(
    viewModel: RecallViewModel,
    allCommitments: List<CommitmentEntity>,
    actionItems: List<ActionItemEntity>,
    dynamicLists: List<DynamicList>,
    onOpenSourceNote: (String) -> Unit
) {
    val context = LocalContext.current
    val completedCommitments = remember(allCommitments) { allCommitments.filter { it.isCompleted } }
    val completedTasks = remember(actionItems) { actionItems.filter { it.isCompleted } }
    val completedListItems = remember(dynamicLists) {
        dynamicLists.flatMap { list ->
            list.items.filter { it.isCompleted }.map { item -> list to item }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 104.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.88f)),
                border = BorderStroke(1.dp, RecallEmeraldAccent.copy(alpha = 0.65f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🏆 Completed Items & Accomplishment Ledger",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "${completedCommitments.size} Promises Fulfilled • ${completedTasks.size} Tasks Done • ${completedListItems.size} List Items Checked",
                            style = MaterialTheme.typography.bodySmall,
                            color = RecallEmeraldAccent
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = {
                            val summary = buildString {
                                appendLine("# Recall Accomplishment Ledger")
                                appendLine("- Fulfilled Promises: ${completedCommitments.size}")
                                completedCommitments.forEach { appendLine("  * [x] ${it.description}") }
                                appendLine("- Completed Tasks: ${completedTasks.size}")
                                completedTasks.forEach { appendLine("  * [x] ${it.task}") }
                            }
                            com.example.util.AppIntegrationsHelper.shareNoteAsMarkdown(
                                context = context,
                                title = "Recall Accomplishment Ledger",
                                content = summary,
                                category = "Completed",
                                tags = listOf("accomplishments"),
                                peopleNames = emptyList()
                            )
                        },
                        border = BorderStroke(1.dp, RecallEmeraldAccent)
                    ) {
                        Text("📤 Share", color = RecallEmeraldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (completedCommitments.isEmpty() && completedTasks.isEmpty() && completedListItems.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No completed commitments or tasks yet. Mark items complete in Commitments, Tasks, or Lists to build your accomplishment ledger.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        }

        if (completedCommitments.isNotEmpty()) {
            item {
                Text(
                    text = "🤝 Fulfilled Commitments (${completedCommitments.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
            items(completedCommitments, key = { "done_comm_${it.id}" }) { comm ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, RecallEmeraldAccent.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RecallEmeraldAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = comm.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            textDecoration = TextDecoration.LineThrough,
                            modifier = Modifier.weight(1f)
                        )
                        if (!comm.noteId.isNullOrBlank()) {
                            TextButton(onClick = { onOpenSourceNote(comm.noteId) }) {
                                Text("Memory →", color = RecallCyanSecondary, fontSize = 11.sp)
                            }
                        }
                        TextButton(onClick = { viewModel.toggleCommitmentCompleted(comm.id, false) }) {
                            Text("↺ Reopen", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        if (completedTasks.isNotEmpty()) {
            item {
                Text(
                    text = "✅ Completed Tasks (${completedTasks.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
            items(completedTasks, key = { "done_task_${it.id}" }) { task ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RecallEmeraldAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = task.task,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            textDecoration = TextDecoration.LineThrough,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.toggleActionItem(task.id, false) }) {
                            Text("↺ Reopen", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
