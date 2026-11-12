package com.example.ui.people

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.core.content.ContextCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import com.example.data.local.converter.RoomConverters
import com.example.data.local.entity.ActionItemEntity
import com.example.data.local.entity.PersonEntity
import com.example.data.model.NoteWithDetails
import com.example.ui.RecallViewModel
import com.example.ui.components.ExportAndShareDialog
import com.example.ui.components.MeetingPrepBriefingDialog
import com.example.ui.components.ReminderSchedulerDialog
import com.example.ui.theme.RecallCyanSecondary
import com.example.ui.theme.RecallVioletPrimary
import com.example.ui.theme.NeonBrainIcon
import com.example.ui.theme.NeuralAppBackground
import com.example.ui.theme.PersonAvatarImage
import com.example.ui.theme.TaskbarDropdownButton
import com.example.ui.theme.UnifiedTaskbarInputBox
import com.example.ui.theme.VibrantTagBadge
import com.example.ui.theme.getVibrantTagColor
import com.example.ui.theme.savePersonAvatarFromBitmap
import com.example.ui.theme.savePersonAvatarFromUri
import com.example.ui.theme.seamlessInnerTextFieldColors
import com.example.ui.theme.translucentWhiteTextFieldColors
import com.example.util.AppIntegrationsHelper
import com.example.util.DateUtils
import com.example.util.ExportAndShareHelper

@Composable
fun PeopleScreen(viewModel: RecallViewModel) {
    val people by viewModel.people.collectAsStateWithLifecycle()
    val allNotes by viewModel.notes.collectAsStateWithLifecycle()
    val allPlaces by viewModel.allPlaces.collectAsStateWithLifecycle()
    val allProjects by viewModel.allProjects.collectAsStateWithLifecycle()
    val allCommitments by viewModel.allCommitments.collectAsStateWithLifecycle()
    val searchQuery by viewModel.peopleSearchQuery.collectAsStateWithLifecycle()
    val selectedCircleFilter by viewModel.selectedRelationshipFilter.collectAsStateWithLifecycle()
    val selectedPerson by viewModel.selectedPerson.collectAsStateWithLifecycle()
    val selectedPersonNotes by viewModel.selectedPersonNotes.collectAsStateWithLifecycle()
    val selectedPlace by viewModel.selectedPlace.collectAsStateWithLifecycle()
    val selectedProject by viewModel.selectedProject.collectAsStateWithLifecycle()
    val isSynthesizing by viewModel.isSynthesizingProfile.collectAsStateWithLifecycle()
    val overdueContacts by viewModel.overdueContacts.collectAsStateWithLifecycle()
    val allActions by viewModel.actionItems.collectAsStateWithLifecycle()

    var selectedEntityTab by remember { mutableStateOf(MemoryEntityHubTab.PEOPLE) }
    var showAddPersonDialog by remember { mutableStateOf(false) }
    var showAddPlaceDialog by remember { mutableStateOf(false) }
    var showAddProjectDialog by remember { mutableStateOf(false) }
    var showAddCommitmentDialog by remember { mutableStateOf(false) }
    var preselectedCommitmentPersonId by remember { mutableStateOf<String?>(null) }
    var preselectedCommitmentProjectId by remember { mutableStateOf<String?>(null) }
    var commitmentForReminder by remember { mutableStateOf<com.example.data.local.entity.CommitmentEntity?>(null) }
    var isPulseExpanded by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val converters = remember { RoomConverters() }

    val circleFilters = remember {
        listOf("All", "Needs Catch-Up", "Work", "Family", "Friends", "Clients")
    }

    // Contacts needing catch-up based on custom cadence (7d, 14d, 30d, 90d)
    val needsCatchUpList = remember(people, overdueContacts) {
        if (overdueContacts.isNotEmpty()) overdueContacts else people.filter { it.isOverdueForCatchUp() }
    }

    // Contacts with recorded key dates / milestones (excluding internal CatchUpCadenceDays)
    val upcomingMilestones = remember(people) {
        people.flatMap { p ->
            val map = converters.toStringMap(p.keyDatesJson).filterKeys { it != "CatchUpCadenceDays" }
            map.entries.map { (label, dateStr) -> Triple(p, label, dateStr) }
        }
    }

    var personPendingDelete by remember { mutableStateOf<PersonEntity?>(null) }

    if (selectedPerson != null) {
        BackHandler { viewModel.clearSelectedPerson() }
        val personCommitments = remember(selectedPerson, allCommitments) {
            allCommitments.filter { it.personId == selectedPerson!!.id }
        }
        PersonDossierView(
            person = selectedPerson!!,
            linkedNotes = selectedPersonNotes,
            allActions = allActions,
            personCommitments = personCommitments,
            allProjects = allProjects,
            isSynthesizing = isSynthesizing,
            onBack = { viewModel.clearSelectedPerson() },
            onUpdatePerson = { updated -> viewModel.savePerson(updated) },
            onDeletePerson = {
                val targetId = selectedPerson!!.id
                viewModel.deletePerson(targetId)
            },
            onUpdateCadence = { days -> viewModel.updatePersonCatchUpCadence(selectedPerson!!, days) },
            onToggleCommitment = { id, done -> viewModel.toggleCommitmentCompleted(id, done) },
            onDeleteCommitment = { id -> viewModel.deleteCommitment(id) },
            onAddCommitmentForPerson = {
                preselectedCommitmentPersonId = selectedPerson!!.id
                preselectedCommitmentProjectId = null
                showAddCommitmentDialog = true
            },
            onScheduleActionReminder = { rTitle, rDetails, rTime, rTone, rVibrate, noteId ->
                viewModel.scheduleReminder(
                    title = rTitle,
                    details = rDetails,
                    noteId = noteId,
                    timeMillis = rTime,
                    tone = rTone,
                    vibratePattern = rVibrate
                )
            },
            onPreviewTone = { tone, vibrate -> viewModel.previewReminderTone(tone, vibrate) },
            onSynthesize = { viewModel.synthesizePersonDossier(selectedPerson!!) },
            onStartCatchUp = { viewModel.startCatchUpNoteForPerson(selectedPerson!!) },
            onOpenNote = { noteItem -> viewModel.openNoteEditor(noteItem) }
        )
    } else if (selectedPlace != null) {
        BackHandler { viewModel.clearSelectedPlace() }
        val placeNotes = remember(selectedPlace, allNotes) {
            allNotes.filter {
                it.note.placeId == selectedPlace!!.id ||
                    it.note.getLocationName().equals(selectedPlace!!.name, ignoreCase = true)
            }
        }
        PlaceDossierView(
            place = selectedPlace!!,
            linkedNotes = placeNotes,
            onBack = { viewModel.clearSelectedPlace() },
            onUpdatePlace = { viewModel.savePlace(it) },
            onDeletePlace = { viewModel.deletePlace(it) },
            onStartNoteAtPlace = { viewModel.startNoteAtPlace(it) },
            onOpenNote = { viewModel.openNoteEditor(it) },
            onOpenPerson = { person ->
                viewModel.clearSelectedPlace()
                viewModel.selectPerson(person)
            }
        )
    } else if (selectedProject != null) {
        BackHandler { viewModel.clearSelectedProject() }
        val projNotes = remember(selectedProject, allNotes) {
            allNotes.filter { it.note.projectId == selectedProject!!.id }
        }
        val projCommitments = remember(selectedProject, allCommitments) {
            allCommitments.filter { it.projectId == selectedProject!!.id }
        }
        ProjectDossierView(
            project = selectedProject!!,
            linkedNotes = projNotes,
            projectCommitments = projCommitments,
            onBack = { viewModel.clearSelectedProject() },
            onUpdateProject = { viewModel.saveProject(it) },
            onDeleteProject = { viewModel.deleteProject(it) },
            onStartNoteForProject = { viewModel.startNoteForProject(it) },
            onOpenNote = { viewModel.openNoteEditor(it) },
            onToggleCommitment = { id, done -> viewModel.toggleCommitmentCompleted(id, done) },
            onAddCommitmentClick = {
                preselectedCommitmentProjectId = selectedProject!!.id
                preselectedCommitmentPersonId = null
                showAddCommitmentDialog = true
            }
        )
    } else {
        NeuralAppBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(10.dp))

                    // Sleek Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NeonBrainIcon(size = 36.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "People, Places & Projects",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${people.size} people • ${allPlaces.size} places • ${allProjects.size} projects",
                                style = MaterialTheme.typography.labelSmall,
                                color = RecallCyanSecondary,
                                fontSize = 11.sp
                            )
                        }

                        // Quick Add Action Pill based on active tab
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = RecallVioletPrimary.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, RecallVioletPrimary.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    when (selectedEntityTab) {
                                        MemoryEntityHubTab.PEOPLE -> showAddPersonDialog = true
                                        MemoryEntityHubTab.PLACES -> showAddPlaceDialog = true
                                        MemoryEntityHubTab.PROJECTS -> showAddProjectDialog = true
                                        MemoryEntityHubTab.COMMITMENTS -> {
                                            preselectedCommitmentPersonId = null
                                            preselectedCommitmentProjectId = null
                                            showAddCommitmentDialog = true
                                        }
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when (selectedEntityTab) {
                                        MemoryEntityHubTab.PEOPLE -> "New Contact"
                                        MemoryEntityHubTab.PLACES -> "New Place"
                                        MemoryEntityHubTab.PROJECTS -> "New Project"
                                        MemoryEntityHubTab.COMMITMENTS -> "New Promise"
                                    },
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Phase 3 Unified Entity Hub Tab Switcher (People | Places | Projects | Commitments)
                    EntityHubTabRow(
                        selectedTab = selectedEntityTab,
                        peopleCount = people.size,
                        placesCount = allPlaces.size,
                        projectsCount = allProjects.size,
                        commitmentsCount = allCommitments.count { !it.isCompleted },
                        onSelectTab = { selectedEntityTab = it }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    when (selectedEntityTab) {
                        MemoryEntityHubTab.PLACES -> {
                            PlacesHubContent(
                                places = allPlaces,
                                allNotes = allNotes,
                                onSelectPlace = { viewModel.selectPlace(it) },
                                onAddPlaceClick = { showAddPlaceDialog = true },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        MemoryEntityHubTab.PROJECTS -> {
                            ProjectsHubContent(
                                projects = allProjects,
                                allNotes = allNotes,
                                allCommitments = allCommitments,
                                onSelectProject = { viewModel.selectProject(it) },
                                onAddProjectClick = { showAddProjectDialog = true },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        MemoryEntityHubTab.COMMITMENTS -> {
                            CommitmentsTrackerContent(
                                commitments = allCommitments,
                                people = people,
                                projects = allProjects,
                                onToggleCompleted = { id, done -> viewModel.toggleCommitmentCompleted(id, done) },
                                onDeleteCommitment = { id -> viewModel.deleteCommitment(id) },
                                onAddCommitmentClick = {
                                    preselectedCommitmentPersonId = null
                                    preselectedCommitmentProjectId = null
                                    showAddCommitmentDialog = true
                                },
                                onScheduleReminderForCommitment = { commitmentForReminder = it },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        MemoryEntityHubTab.PEOPLE -> {

                    // Unified People Search & Circle Filter Box with Internal Top Taskbar Dropdowns
                    var showCircleDropdown by remember { mutableStateOf(false) }
                    var showSearchActionsDropdown by remember { mutableStateOf(false) }
                    val activeCircleColor = getVibrantTagColor(selectedCircleFilter)

                    UnifiedTaskbarInputBox(
                        shape = RoundedCornerShape(20.dp),
                        borderColor = activeCircleColor,
                        modifier = Modifier.fillMaxWidth(),
                        taskbarContent = {
                            // (A) Relationship Circle / Tag Filter Dropdown
                            Box {
                                TaskbarDropdownButton(
                                    label = "Circle: $selectedCircleFilter",
                                    dotColor = activeCircleColor,
                                    accentColor = activeCircleColor,
                                    onClick = { showCircleDropdown = true },
                                    modifier = Modifier.testTag("people_circle_dropdown")
                                )
                                DropdownMenu(
                                    expanded = showCircleDropdown,
                                    onDismissRequest = { showCircleDropdown = false },
                                    properties = PopupProperties(clippingEnabled = true),
                                    modifier = Modifier
                                        .background(Color(0xFF0F172A))
                                        .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                                ) {
                                    circleFilters.forEach { circle ->
                                        val isSelected = selectedCircleFilter == circle
                                        val cColor = getVibrantTagColor(circle)
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(cColor)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = circle,
                                                        color = if (isSelected) cColor else Color.White,
                                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                                                    )
                                                }
                                            },
                                            trailingIcon = {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = cColor,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            },
                                            onClick = {
                                                viewModel.selectedRelationshipFilter.value = circle
                                                showCircleDropdown = false
                                            }
                                        )
                                    }
                                }
                            }

                            // (B) CRM Actions Dropdown
                            Box {
                                TaskbarDropdownButton(
                                    label = "CRM Options",
                                    icon = Icons.Default.FilterList,
                                    accentColor = RecallVioletPrimary,
                                    onClick = { showSearchActionsDropdown = true }
                                )
                                DropdownMenu(
                                    expanded = showSearchActionsDropdown,
                                    onDismissRequest = { showSearchActionsDropdown = false },
                                    properties = PopupProperties(clippingEnabled = true),
                                    modifier = Modifier
                                        .background(Color(0xFF0F172A))
                                        .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("+ Add New Contact", color = RecallVioletPrimary, fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            showSearchActionsDropdown = false
                                            scope.launch {
                                                delay(60)
                                                showAddPersonDialog = true
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("✨ Clean Up Invalid Contacts & Duplicate Notes", color = RecallCyanSecondary, fontWeight = FontWeight.SemiBold) },
                                        onClick = {
                                            showSearchActionsDropdown = false
                                            viewModel.cleanupDuplicateNotesAndInvalidContacts()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("🧹 Reset Search & Filters", color = Color.White, fontWeight = FontWeight.Medium) },
                                        onClick = {
                                            showSearchActionsDropdown = false
                                            viewModel.peopleSearchQuery.value = ""
                                            viewModel.selectedRelationshipFilter.value = "All"
                                        }
                                    )
                                }
                            }
                        },
                        bodyContent = {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.peopleSearchQuery.value = it },
                                placeholder = { Text("Search people, relationships, facts...", color = Color(0xFF94A3B8)) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = RecallVioletPrimary) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.peopleSearchQuery.value = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF94A3B8))
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                ),
                                colors = seamlessInnerTextFieldColors()
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Relationship Pulse & Stay-in-Touch Intelligence Banner
                    if (needsCatchUpList.isNotEmpty() || upcomingMilestones.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                                .testTag("relationship_pulse_banner"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.88f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.55f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isPulseExpanded = !isPulseExpanded },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = RecallCyanSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Relationship Pulse & Stay in Touch",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "${needsCatchUpList.size} catch-up nudges • ${upcomingMilestones.size} key dates",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = RecallCyanSecondary
                                        )
                                    }
                                    Icon(
                                        imageVector = if (isPulseExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = "Toggle Pulse",
                                        tint = Color.White
                                    )
                                }

                                if (isPulseExpanded) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    needsCatchUpList.take(3).forEach { person ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Catch up with ${person.name}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = "Last contact: ${if (person.lastContactedAt != null) DateUtils.formatRelativeTime(person.lastContactedAt) else "Over 30 days ago"}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFFCBD5E1)
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = RecallVioletPrimary.copy(alpha = 0.85f),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable { viewModel.startCatchUpNoteForPerson(person) }
                                            ) {
                                                Text(
                                                    text = "Catch Up",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                )
                                            }
                                        }
                                    }

                                    upcomingMilestones.take(2).forEach { (person, label, dateIso) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Cake,
                                                contentDescription = null,
                                                tint = Color(0xFFFBBF24),
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "${person.name} • $label ($dateIso)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFFFDE68A),
                                                modifier = Modifier.weight(1f)
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = RecallCyanSecondary.copy(alpha = 0.2f),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, RecallCyanSecondary),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        AppIntegrationsHelper.scheduleInCalendar(
                                                            context = context,
                                                            title = "${person.name}: $label",
                                                            description = "Milestone from Recall CRM for ${person.name}",
                                                            dateHintOrIso = dateIso
                                                        )
                                                    }
                                            ) {
                                                Text(
                                                    text = "Calendar",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = RecallCyanSecondary,
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

                    if (people.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 60.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "No contacts matching filter",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "People mentioned in notes or debriefs will appear here.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 88.dp)
                        ) {
                            items(people, key = { it.id }) { person ->
                                PersonListItem(
                                    person = person,
                                    onClick = { viewModel.selectPerson(person) },
                                    onDeleteClick = { personPendingDelete = person }
                                )
                            }
                        }
                    }
                        }
                    }
            }

            FloatingActionButton(
                onClick = {
                    when (selectedEntityTab) {
                        MemoryEntityHubTab.PEOPLE -> showAddPersonDialog = true
                        MemoryEntityHubTab.PLACES -> showAddPlaceDialog = true
                        MemoryEntityHubTab.PROJECTS -> showAddProjectDialog = true
                        MemoryEntityHubTab.COMMITMENTS -> {
                            preselectedCommitmentPersonId = null
                            preselectedCommitmentProjectId = null
                            showAddCommitmentDialog = true
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("add_person_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Entity")
            }
        }
    }
    }

    if (showAddPersonDialog) {
        AddPersonDialog(
            onDismiss = { showAddPersonDialog = false },
            onAdd = { name, relationship, facts, avatarUri ->
                val newPerson = PersonEntity(
                    name = name,
                    relationship = relationship,
                    avatarUri = avatarUri,
                    factsJson = converters.fromStringList(facts),
                    lastContactedAt = System.currentTimeMillis()
                )
                viewModel.savePerson(newPerson)
                showAddPersonDialog = false
            }
        )
    }

    if (showAddPlaceDialog) {
        AddOrEditPlaceDialog(
            initialPlace = null,
            onDismiss = { showAddPlaceDialog = false },
            onSave = { place ->
                viewModel.savePlace(place)
                showAddPlaceDialog = false
            }
        )
    }

    if (showAddProjectDialog) {
        AddOrEditProjectDialog(
            initialProject = null,
            onDismiss = { showAddProjectDialog = false },
            onSave = { project ->
                viewModel.saveProject(project)
                showAddProjectDialog = false
            }
        )
    }

    if (showAddCommitmentDialog) {
        AddCommitmentDialog(
            people = people,
            projects = allProjects,
            preselectedPersonId = preselectedCommitmentPersonId,
            preselectedProjectId = preselectedCommitmentProjectId,
            onDismiss = { showAddCommitmentDialog = false },
            onSave = { commitment ->
                viewModel.saveCommitment(commitment)
                showAddCommitmentDialog = false
            }
        )
    }

    commitmentForReminder?.let { comm ->
        ReminderSchedulerDialog(
            initialTitle = "Commitment: ${comm.description.take(40)}",
            initialDetails = comm.description,
            noteId = comm.noteId,
            existingReminder = null,
            onDismiss = { commitmentForReminder = null },
            onSave = { rTitle, rDetails, rTime, rTone, rVibrate ->
                viewModel.scheduleReminder(
                    title = rTitle,
                    details = rDetails,
                    noteId = comm.noteId,
                    timeMillis = rTime,
                    tone = rTone,
                    vibratePattern = rVibrate
                )
                commitmentForReminder = null
            },
            onDelete = null,
            onPreviewTone = { tone, vibrate -> viewModel.previewReminderTone(tone, vibrate) }
        )
    }
    personPendingDelete?.let { targetPerson ->
        AlertDialog(
            onDismissRequest = { personPendingDelete = null },
            containerColor = Color(0xFF0F172A),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFCBD5E1),
            title = {
                Text("Delete Profile: ${targetPerson.name}?", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Text("Are you sure you want to permanently delete ${targetPerson.name}'s profile and contact information? Linked notes will remain safe in your memory stream.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePerson(targetPerson.id)
                        personPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_delete_person_button")
                ) {
                    Text("Delete Profile", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { personPendingDelete = null }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}

@Composable
fun PersonListItem(
    person: PersonEntity,
    onClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null
) {
    val converters = remember { RoomConverters() }
    val facts = remember(person.factsJson) { converters.toStringList(person.factsJson) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(RecallCyanSecondary.copy(alpha = 0.45f), RecallVioletPrimary.copy(alpha = 0.45f))
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .testTag("person_item_${person.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1120).copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with photo support
            PersonAvatarImage(
                name = person.name,
                avatarUri = person.avatarUri,
                size = 50.dp,
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = person.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (person.isOverdueForCatchUp()) Color(0xFFFBBF24).copy(alpha = 0.2f) else RecallVioletPrimary.copy(alpha = 0.2f),
                        border = BorderStroke(
                            1.dp,
                            if (person.isOverdueForCatchUp()) Color(0xFFFBBF24).copy(alpha = 0.5f) else RecallVioletPrimary.copy(alpha = 0.4f)
                        )
                    ) {
                        Text(
                            text = if (person.isOverdueForCatchUp()) "🔔 Overdue (${person.getCadenceLabel()})" else "⏱️ ${person.getCadenceLabel()}",
                            color = if (person.isOverdueForCatchUp()) Color(0xFFFBBF24) else Color(0xFFA78BFA),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (!person.relationship.isNullOrBlank()) {
                    Text(
                        text = person.relationship,
                        style = MaterialTheme.typography.bodySmall,
                        color = RecallCyanSecondary
                    )
                }
                if (facts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${facts.size} facts remembered • e.g. \"${facts.first().take(40)}...\"",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (person.lastContactedAt != null) {
                Text(
                    text = DateUtils.formatRelativeTime(person.lastContactedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            if (onDeleteClick != null) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("delete_person_card_button_${person.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete ${person.name}",
                        tint = Color(0xFFF87171),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PersonDossierView(
    person: PersonEntity,
    linkedNotes: List<NoteWithDetails>,
    allActions: List<ActionItemEntity> = emptyList(),
    personCommitments: List<com.example.data.local.entity.CommitmentEntity> = emptyList(),
    allProjects: List<com.example.data.local.entity.ProjectEntity> = emptyList(),
    isSynthesizing: Boolean,
    onBack: () -> Unit,
    onUpdatePerson: (PersonEntity) -> Unit,
    onDeletePerson: () -> Unit = {},
    onUpdateCadence: (Int) -> Unit = {},
    onToggleCommitment: (String, Boolean) -> Unit = { _, _ -> },
    onDeleteCommitment: (String) -> Unit = {},
    onAddCommitmentForPerson: () -> Unit = {},
    onScheduleActionReminder: (
        title: String,
        details: String,
        timeMillis: Long,
        tone: com.example.data.local.entity.ReminderTone,
        vibrate: com.example.data.local.entity.ReminderVibratePattern,
        noteId: String?
    ) -> Unit = { _, _, _, _, _, _ -> },
    onPreviewTone: (com.example.data.local.entity.ReminderTone, com.example.data.local.entity.ReminderVibratePattern) -> Unit = { _, _ -> },
    onSynthesize: () -> Unit,
    onStartCatchUp: () -> Unit = {},
    onOpenNote: (NoteWithDetails) -> Unit = {}
) {
    val context = LocalContext.current
    val converters = remember { RoomConverters() }
    val facts = remember(person.factsJson) { converters.toStringList(person.factsJson) }
    val dates = remember(person.keyDatesJson) {
        converters.toStringMap(person.keyDatesJson).filterKeys { it != "CatchUpCadenceDays" }
    }

    var showAddFactDialog by remember { mutableStateOf(false) }
    var editingFactIndex by remember { mutableStateOf<Int?>(null) }
    var showAddDateDialog by remember { mutableStateOf(false) }
    var showEditContactInfoDialog by remember { mutableStateOf(false) }
    var showDeleteProfileConfirmDialog by remember { mutableStateOf(false) }
    var showBriefingDialog by remember { mutableStateOf(false) }
    var showExportDossierDialog by remember { mutableStateOf(false) }
    var actionForReminder by remember { mutableStateOf<ActionItemEntity?>(null) }

    // Camera capture launcher for person profile picture
    val dossierCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val savedPath = savePersonAvatarFromBitmap(context, bitmap)
            if (savedPath != null) {
                onUpdatePerson(person.copy(avatarUri = savedPath))
                Toast.makeText(context, "Profile picture updated!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val dossierCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                dossierCameraLauncher.launch()
            } catch (e: Throwable) {
                Toast.makeText(context, "Could not launch camera", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Camera permission is required to take a photo", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchDossierCameraSafely() {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            try {
                dossierCameraLauncher.launch()
            } catch (e: Throwable) {
                Toast.makeText(context, "Could not launch camera", Toast.LENGTH_SHORT).show()
            }
        } else {
            dossierCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Phone memory photo picker launcher for person profile picture
    val dossierPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = savePersonAvatarFromUri(context, uri)
            if (savedPath != null) {
                onUpdatePerson(person.copy(avatarUri = savedPath))
                Toast.makeText(context, "Profile picture uploaded!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    NeuralAppBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Back Row + Share Markdown + Catch Up CTA
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    "Person Dossier",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = RecallCyanSecondary.copy(alpha = 0.18f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RecallCyanSecondary),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onStartCatchUp() }
                ) {
                    Text(
                        text = "+ Catch-Up Note",
                        style = MaterialTheme.typography.labelSmall,
                        color = RecallCyanSecondary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = { showEditContactInfoDialog = true },
                    modifier = Modifier.testTag("edit_person_info_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Contact Info",
                        tint = RecallCyanSecondary
                    )
                }
                IconButton(
                    onClick = {
                        AppIntegrationsHelper.sharePersonDossierAsMarkdown(
                            context = context,
                            person = person,
                            linkedNotes = linkedNotes
                        )
                    },
                    modifier = Modifier.testTag("share_person_markdown_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Dossier as Markdown",
                        tint = RecallCyanSecondary
                    )
                }
                IconButton(
                    onClick = { showDeleteProfileConfirmDialog = true },
                    modifier = Modifier.testTag("delete_person_dossier_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Contact Profile",
                        tint = Color(0xFFF87171)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Header: Hero avatar with Camera / Gallery upload, relationship tag, and last interaction
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                brush = Brush.horizontalGradient(
                                    listOf(RecallCyanSecondary.copy(alpha = 0.55f), RecallVioletPrimary.copy(alpha = 0.55f))
                                ),
                                shape = RoundedCornerShape(20.dp)
                            ),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1120).copy(alpha = 0.68f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                contentAlignment = Alignment.BottomEnd,
                                modifier = Modifier
                                    .clickable {
                                        dossierPhotoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                            ) {
                                PersonAvatarImage(
                                    name = person.name,
                                    avatarUri = person.avatarUri,
                                    size = 84.dp,
                                    fontSize = 34.sp
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = RecallCyanSecondary,
                                    border = BorderStroke(1.5.dp, Color(0xFF0B1120)),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = "Change Profile Picture",
                                            tint = Color(0xFF040711),
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Camera & Upload Photo Action Buttons Row
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { launchDossierCameraSafely() },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.8f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color(0xFF0F172A).copy(alpha = 0.85f),
                                        contentColor = RecallCyanSecondary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .heightIn(min = 36.dp)
                                        .testTag("person_take_photo_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text("Take Photo", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                }

                                OutlinedButton(
                                    onClick = {
                                        dossierPhotoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, RecallVioletPrimary.copy(alpha = 0.85f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color(0xFF0F172A).copy(alpha = 0.85f),
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .heightIn(min = 36.dp)
                                        .testTag("person_upload_photo_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = null,
                                        tint = RecallCyanSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text("Upload Photo", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                                if (!person.avatarUri.isNullOrBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            onUpdatePerson(person.copy(avatarUri = null))
                                            Toast.makeText(context, "Profile photo removed", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, Color(0xFFF87171).copy(alpha = 0.8f)),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = Color(0xFF0F172A).copy(alpha = 0.85f),
                                            contentColor = Color(0xFFF87171)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .heightIn(min = 36.dp)
                                            .testTag("person_remove_photo_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove Photo",
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Remove Photo", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    person.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { showEditContactInfoDialog = true },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Name or Relationship",
                                        tint = RecallCyanSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            if (!person.relationship.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    VibrantTagBadge(
                                        text = person.relationship,
                                        color = getVibrantTagColor(person.relationship)
                                    )
                                    IconButton(
                                        onClick = {
                                            onUpdatePerson(person.copy(relationship = null))
                                            Toast.makeText(context, "Relationship tag removed", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .size(24.dp)
                                            .testTag("delete_relationship_tag_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Delete Relationship Info",
                                            tint = Color(0xFFF87171),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                            if (person.lastContactedAt != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Last interaction: ${DateUtils.formatRelativeTime(person.lastContactedAt)} • Cadence: ${person.getCadenceLabel()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // ⚡ 5-Sec Briefing & Cadence + 📤 Export PDF / MD Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showBriefingDialog = true },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = RecallCyanSecondary,
                                        contentColor = Color(0xFF0F172A)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 44.dp)
                                ) {
                                    Text(
                                        "⚡ 5-Sec Briefing",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1
                                    )
                                }

                                OutlinedButton(
                                    onClick = { showExportDossierDialog = true },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, RecallCyanSecondary),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color(0xFF0F172A),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 44.dp)
                                ) {
                                    Text(
                                        "📤 Export PDF / MD",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // ✨ Synthesize Profile Action Button
                            Button(
                                onClick = onSynthesize,
                                enabled = !isSynthesizing,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 46.dp)
                                    .testTag("synthesize_profile_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RecallVioletPrimary,
                                    contentColor = Color.White
                                )
                            ) {
                                if (isSynthesizing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Synthesizing from notes...", fontSize = 13.sp, maxLines = 1)
                                } else {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Synthesize Profile with AI", fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                            }
                        }
                    }
                }

            // Key Facts Card: Surface-elevated card showing bulleted facts extracted by AI over time
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(
                                listOf(RecallVioletPrimary.copy(alpha = 0.5f), RecallCyanSecondary.copy(alpha = 0.4f))
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1120).copy(alpha = 0.65f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Key Facts (${facts.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            if (facts.isNotEmpty()) {
                                TextButton(
                                    onClick = {
                                        onUpdatePerson(person.copy(factsJson = converters.fromStringList(emptyList())))
                                        Toast.makeText(context, "All facts cleared", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.testTag("clear_all_facts_button")
                                ) {
                                    Text("Clear All", color = Color(0xFFF87171), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            IconButton(
                                onClick = { showAddFactDialog = true },
                                modifier = Modifier.testTag("add_fact_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Fact", tint = RecallCyanSecondary)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        if (facts.isEmpty()) {
                            Text(
                                "No facts extracted yet. Tap '+' to add a fact or mention them in voice debriefs.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                facts.forEachIndexed { index, fact ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF1E293B).copy(alpha = 0.65f),
                                        border = BorderStroke(1.dp, Color(0xFF334155)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { editingFactIndex = index }
                                            .testTag("person_fact_item_$index")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("•", color = RecallCyanSecondary, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = fact,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color(0xFFF1F5F9),
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = { editingFactIndex = index },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .testTag("edit_fact_button_$index")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit Fact",
                                                    tint = RecallCyanSecondary,
                                                    modifier = Modifier.size(17.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    val currentFacts = converters.toStringList(person.factsJson).toMutableList()
                                                    if (index in currentFacts.indices) {
                                                        currentFacts.removeAt(index)
                                                        onUpdatePerson(person.copy(factsJson = converters.fromStringList(currentFacts)))
                                                        Toast.makeText(context, "Fact deleted", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .testTag("delete_fact_button_$index")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Fact",
                                                    tint = Color(0xFFF87171),
                                                    modifier = Modifier.size(17.dp)
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

            // Important Dates Card (with Add & Delete support for every date)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(
                                listOf(RecallCyanSecondary.copy(alpha = 0.5f), RecallVioletPrimary.copy(alpha = 0.4f))
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1120).copy(alpha = 0.65f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Important Dates & Milestones (${dates.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { showAddDateDialog = true },
                                modifier = Modifier.testTag("add_date_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Important Date", tint = RecallCyanSecondary)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        if (dates.isEmpty()) {
                            Text(
                                "No important dates saved yet. Tap '+' to add a birthday, anniversary, or milestone.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1)
                            )
                        } else {
                            dates.forEach { (label, dateIso) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = RecallCyanSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(dateIso, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = {
                                            AppIntegrationsHelper.scheduleInCalendar(
                                                context = context,
                                                title = "${person.name}: $label",
                                                description = "Important milestone for ${person.name} from Recall CRM",
                                                dateHintOrIso = dateIso
                                            )
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = "Schedule in Calendar",
                                            tint = RecallCyanSecondary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            val fullMap = converters.toStringMap(person.keyDatesJson).toMutableMap()
                                            fullMap.remove(label)
                                            onUpdatePerson(person.copy(keyDatesJson = converters.fromStringMap(fullMap)))
                                            Toast.makeText(context, "Deleted date: $label", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("delete_date_button_$label")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Date $label",
                                            tint = Color(0xFFF87171),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Commitments & Promises Section for this Person (Phase 3)
            item {
                PersonCommitmentsSection(
                    person = person,
                    commitments = personCommitments,
                    onToggleCompleted = onToggleCommitment,
                    onDeleteCommitment = onDeleteCommitment,
                    onAddCommitmentForPerson = onAddCommitmentForPerson
                )
            }

            // Timeline View: Notes linked to this person
            item {
                Text(
                    "Interaction Timeline (${linkedNotes.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (linkedNotes.isEmpty()) {
                item {
                    Text(
                        "No notes linked to ${person.name} yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                }
            } else {
                items(linkedNotes, key = { it.note.id }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = RecallCyanSecondary.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onOpenNote(item) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1120).copy(alpha = 0.62f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    item.note.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    DateUtils.formatRelativeTime(item.note.createdAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RecallCyanSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                item.note.content,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE2E8F0),
                                maxLines = 3
                            )
                        }
                    }
                }
            }
        }
        }
    }

    if (showAddFactDialog) {
        var newFactText by remember { mutableStateOf("") }
        var showFactPrefixMenu by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showAddFactDialog = false },
            containerColor = Color(0xFF0F172A),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text("Add Fact for ${person.name}", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                UnifiedTaskbarInputBox(
                    borderColor = RecallVioletPrimary,
                    taskbarContent = {
                        Box {
                            TaskbarDropdownButton(
                                label = "Fact Category",
                                accentColor = RecallVioletPrimary,
                                dotColor = RecallVioletPrimary,
                                onClick = { showFactPrefixMenu = true }
                            )
                            DropdownMenu(
                                expanded = showFactPrefixMenu,
                                onDismissRequest = { showFactPrefixMenu = false },
                                properties = PopupProperties(clippingEnabled = true),
                                modifier = Modifier
                                    .background(Color(0xFF0F172A))
                                    .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                            ) {
                                listOf(
                                    "💼 Work / Role: ",
                                    "❤️ Preference: ",
                                    "🎂 Birthday / Date: ",
                                    "🏠 Family / Home: ",
                                    "☕ Coffee / Food: "
                                ).forEach { prefix ->
                                    DropdownMenuItem(
                                        text = { Text(prefix, color = Color.White, fontWeight = FontWeight.SemiBold) },
                                        onClick = {
                                            newFactText = prefix + newFactText.removePrefix(prefix)
                                            showFactPrefixMenu = false
                                        }
                                    )
                                }
                            }
                        }
                        TaskbarDropdownButton(
                            label = "Clear",
                            icon = Icons.Default.Close,
                            accentColor = Color(0xFF94A3B8),
                            onClick = { newFactText = "" }
                        )
                    },
                    bodyContent = {
                        OutlinedTextField(
                            value = newFactText,
                            onValueChange = { newFactText = it },
                            placeholder = { Text("e.g. Prefers morning meetings, has two dogs", color = Color(0xFF94A3B8)) },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                            colors = seamlessInnerTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val currentFacts = converters.toStringList(person.factsJson).toMutableList()
                        currentFacts.add(newFactText)
                        onUpdatePerson(person.copy(factsJson = converters.fromStringList(currentFacts)))
                        showAddFactDialog = false
                    },
                    enabled = newFactText.isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFactDialog = false }) { Text("Cancel") }
            }
        )
    }

    editingFactIndex?.let { index ->
        val currentFacts = converters.toStringList(person.factsJson)
        val initialFactText = currentFacts.getOrNull(index) ?: ""
        var editedFactText by remember(index, initialFactText) { mutableStateOf(initialFactText) }
        var showEditFactPrefixMenu by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { editingFactIndex = null },
            containerColor = Color(0xFF0F172A),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text("Edit Fact for ${person.name}", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                UnifiedTaskbarInputBox(
                    borderColor = RecallCyanSecondary,
                    taskbarContent = {
                        Box {
                            TaskbarDropdownButton(
                                label = "Fact Category",
                                accentColor = RecallCyanSecondary,
                                dotColor = RecallCyanSecondary,
                                onClick = { showEditFactPrefixMenu = true }
                            )
                            DropdownMenu(
                                expanded = showEditFactPrefixMenu,
                                onDismissRequest = { showEditFactPrefixMenu = false },
                                properties = PopupProperties(clippingEnabled = true),
                                modifier = Modifier
                                    .background(Color(0xFF0F172A))
                                    .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                            ) {
                                listOf(
                                    "💼 Work / Role: ",
                                    "❤️ Preference: ",
                                    "🎂 Birthday / Date: ",
                                    "🏠 Family / Home: ",
                                    "☕ Coffee / Food: "
                                ).forEach { prefix ->
                                    DropdownMenuItem(
                                        text = { Text(prefix, color = Color.White, fontWeight = FontWeight.SemiBold) },
                                        onClick = {
                                            editedFactText = prefix + editedFactText.removePrefix(prefix)
                                            showEditFactPrefixMenu = false
                                        }
                                    )
                                }
                            }
                        }
                        TaskbarDropdownButton(
                            label = "Delete Fact",
                            icon = Icons.Default.Delete,
                            accentColor = Color(0xFFF87171),
                            onClick = {
                                val updatedFacts = currentFacts.toMutableList()
                                if (index in updatedFacts.indices) {
                                    updatedFacts.removeAt(index)
                                    onUpdatePerson(person.copy(factsJson = converters.fromStringList(updatedFacts)))
                                    Toast.makeText(context, "Fact deleted", Toast.LENGTH_SHORT).show()
                                }
                                editingFactIndex = null
                            }
                        )
                    },
                    bodyContent = {
                        OutlinedTextField(
                            value = editedFactText,
                            onValueChange = { editedFactText = it },
                            placeholder = { Text("Edit fact details...", color = Color(0xFF94A3B8)) },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                            colors = seamlessInnerTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_fact_text_field")
                        )
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updatedFacts = currentFacts.toMutableList()
                        if (index in updatedFacts.indices) {
                            updatedFacts[index] = editedFactText.trim()
                            onUpdatePerson(person.copy(factsJson = converters.fromStringList(updatedFacts)))
                            Toast.makeText(context, "Fact updated", Toast.LENGTH_SHORT).show()
                        }
                        editingFactIndex = null
                    },
                    enabled = editedFactText.isNotBlank(),
                    modifier = Modifier.testTag("save_edited_fact_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingFactIndex = null }) { Text("Cancel") }
            }
        )
    }

    if (showAddDateDialog) {
        var dateLabel by remember { mutableStateOf("") }
        var dateValue by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDateDialog = false },
            containerColor = Color(0xFF0F172A),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text("Add Important Date for ${person.name}", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = dateLabel,
                        onValueChange = { dateLabel = it },
                        label = { Text("Label (e.g. Birthday, Anniversary)", color = Color(0xFF94A3B8)) },
                        singleLine = true,
                        colors = translucentWhiteTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dateValue,
                        onValueChange = { dateValue = it },
                        label = { Text("Date (e.g. 1992-05-14 or May 14)", color = Color(0xFF94A3B8)) },
                        singleLine = true,
                        colors = translucentWhiteTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val fullMap = converters.toStringMap(person.keyDatesJson).toMutableMap()
                        fullMap[dateLabel.trim()] = dateValue.trim()
                        onUpdatePerson(person.copy(keyDatesJson = converters.fromStringMap(fullMap)))
                        showAddDateDialog = false
                    },
                    enabled = dateLabel.isNotBlank() && dateValue.isNotBlank()
                ) {
                    Text("Save Date")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDateDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showEditContactInfoDialog) {
        var editedName by remember(person.name) { mutableStateOf(person.name) }
        var editedRelationship by remember(person.relationship) { mutableStateOf(person.relationship ?: "") }
        AlertDialog(
            onDismissRequest = { showEditContactInfoDialog = false },
            containerColor = Color(0xFF0F172A),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text("Edit Contact Information", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        label = { Text("Contact Name", color = Color(0xFF94A3B8)) },
                        singleLine = true,
                        colors = translucentWhiteTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editedRelationship,
                        onValueChange = { editedRelationship = it },
                        label = { Text("Relationship / Role (leave blank to remove)", color = Color(0xFF94A3B8)) },
                        singleLine = true,
                        trailingIcon = {
                            if (editedRelationship.isNotEmpty()) {
                                IconButton(onClick = { editedRelationship = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear Relationship", tint = Color(0xFF94A3B8))
                                }
                            }
                        },
                        colors = translucentWhiteTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = {
                            showEditContactInfoDialog = false
                            showDeleteProfileConfirmDialog = true
                        },
                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF87171)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Entire Contact Profile", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdatePerson(
                            person.copy(
                                name = editedName.trim(),
                                relationship = editedRelationship.trim().ifBlank { null }
                            )
                        )
                        showEditContactInfoDialog = false
                    },
                    enabled = editedName.isNotBlank()
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditContactInfoDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteProfileConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteProfileConfirmDialog = false },
            containerColor = Color(0xFF0F172A),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFCBD5E1),
            title = {
                Text("Delete Profile: ${person.name}?", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Text("Are you sure you want to permanently delete ${person.name}'s entire profile and contact information? Linked notes will remain safe in your memory stream.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteProfileConfirmDialog = false
                        onDeletePerson()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_delete_dossier_button")
                ) {
                    Text("Delete Profile", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteProfileConfirmDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    if (showBriefingDialog) {
        MeetingPrepBriefingDialog(
            person = person,
            personNotes = linkedNotes,
            allActions = allActions,
            allCommitments = personCommitments,
            allProjects = allProjects,
            onUpdateCadenceDays = { days ->
                onUpdateCadence(days)
            },
            onScheduleActionReminder = { act ->
                actionForReminder = act
            },
            onOpenNote = { noteId ->
                linkedNotes.find { it.note.id == noteId }?.let { onOpenNote(it) }
            },
            onShowFeedback = { msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showBriefingDialog = false }
        )
    }

    if (showExportDossierDialog) {
        ExportAndShareDialog(
            documentTitle = "CRM Dossier - ${person.name}",
            subtitle = "${person.relationship ?: "Contact"} • ${linkedNotes.size} linked memories",
            markdownContent = ExportAndShareHelper.formatPersonDossierAsMarkdown(
                person = person,
                linkedNotes = linkedNotes,
                openActions = allActions.filter { !it.isCompleted }
            ),
            fileNamePrefix = "dossier_${person.name}",
            onShowFeedback = { msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showExportDossierDialog = false }
        )
    }

    actionForReminder?.let { act ->
        ReminderSchedulerDialog(
            initialTitle = "Action (${person.name}): ${act.task}",
            initialDetails = "Follow-up task for ${person.name}",
            noteId = act.noteId,
            existingReminder = null,
            onDismiss = { actionForReminder = null },
            onSave = { rTitle, rDetails, rTime, rTone, rVibrate ->
                onScheduleActionReminder(rTitle, rDetails, rTime, rTone, rVibrate, act.noteId)
                actionForReminder = null
                Toast.makeText(context, "🔔 Scheduled alarm for ${person.name}!", Toast.LENGTH_SHORT).show()
            },
            onDelete = null,
            onPreviewTone = { tone, vibrate ->
                onPreviewTone(tone, vibrate)
            }
        )
    }
}

@Composable
fun AddPersonDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, relationship: String, facts: List<String>, avatarUri: String?) -> Unit
) {
    val context = LocalContext.current
    val dialogScope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("Work") }
    var firstFact by remember { mutableStateOf("") }
    var avatarUri by remember { mutableStateOf<String?>(null) }
    var showRelDropdown by remember { mutableStateOf(false) }
    val relOptions = remember { listOf("Work", "Family", "Friends", "Clients", "Mentor", "Other") }

    val dialogCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val savedPath = savePersonAvatarFromBitmap(context, bitmap)
            if (savedPath != null) {
                avatarUri = savedPath
            }
        }
    }

    val dialogCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                dialogCameraLauncher.launch()
            } catch (_: Throwable) {
            }
        } else {
            Toast.makeText(context, "Camera permission is required to take a photo", Toast.LENGTH_SHORT).show()
        }
    }

    val dialogPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = savePersonAvatarFromUri(context, uri)
            if (savedPath != null) {
                avatarUri = savedPath
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = { Text("Add Person to CRM", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Picture Preview + Take Photo / Upload Photo controls
                PersonAvatarImage(
                    name = name.ifBlank { "?" },
                    avatarUri = avatarUri,
                    size = 64.dp,
                    fontSize = 26.sp
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val hasPerm = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPerm) {
                                try {
                                    dialogCameraLauncher.launch()
                                } catch (_: Throwable) {
                                }
                            } else {
                                dialogCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, RecallCyanSecondary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.heightIn(min = 36.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = RecallCyanSecondary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Camera", fontSize = 11.sp, color = RecallCyanSecondary, fontWeight = FontWeight.Bold, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = {
                            dialogPhotoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, RecallVioletPrimary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.heightIn(min = 36.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = RecallVioletPrimary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Phone Memory", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }

                var showPhotoDropdown by remember { mutableStateOf(false) }
                var showStarterFactDropdown by remember { mutableStateOf(false) }
                val relColor = getVibrantTagColor(relationship)

                // Unified Translucent White Input Box with Integrated Top Taskbar Dropdowns
                UnifiedTaskbarInputBox(
                    borderColor = relColor,
                    modifier = Modifier.fillMaxWidth(),
                    taskbarContent = {
                        // (A) Relationship Circle Dropdown
                        Box {
                            TaskbarDropdownButton(
                                label = relationship,
                                dotColor = relColor,
                                accentColor = relColor,
                                onClick = { showRelDropdown = true }
                            )
                            DropdownMenu(
                                expanded = showRelDropdown,
                                onDismissRequest = { showRelDropdown = false },
                                properties = PopupProperties(clippingEnabled = true),
                                modifier = Modifier
                                    .background(Color(0xFF0F172A))
                                    .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                            ) {
                                relOptions.forEach { opt ->
                                    val c = getVibrantTagColor(opt)
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = opt,
                                                color = c,
                                                fontWeight = FontWeight.Bold
                                            )
                                        },
                                        onClick = {
                                            relationship = opt
                                            showRelDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        // (B) Profile Photo Source Dropdown
                        Box {
                            TaskbarDropdownButton(
                                label = "Photo",
                                icon = Icons.Default.CameraAlt,
                                accentColor = Color(0xFF0284C7),
                                onClick = { showPhotoDropdown = true }
                            )
                            DropdownMenu(
                                expanded = showPhotoDropdown,
                                onDismissRequest = { showPhotoDropdown = false },
                                properties = PopupProperties(clippingEnabled = true),
                                modifier = Modifier
                                    .background(Color(0xFF0F172A))
                                    .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("📷 Take Photo (Camera)", color = Color.White, fontWeight = FontWeight.SemiBold) },
                                    onClick = {
                                        showPhotoDropdown = false
                                        dialogScope.launch {
                                            delay(60)
                                            val hasPerm = ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.CAMERA
                                            ) == PackageManager.PERMISSION_GRANTED
                                            if (hasPerm) {
                                                try {
                                                    dialogCameraLauncher.launch()
                                                } catch (_: Throwable) {
                                                }
                                            } else {
                                                dialogCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                            }
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🖼️ Phone Memory (Gallery)", color = Color.White, fontWeight = FontWeight.SemiBold) },
                                    onClick = {
                                        showPhotoDropdown = false
                                        dialogScope.launch {
                                            delay(60)
                                            dialogPhotoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                    }
                                )
                            }
                        }

                        // (C) Fact Template Dropdown
                        Box {
                            TaskbarDropdownButton(
                                label = "Fact Type",
                                accentColor = RecallVioletPrimary,
                                onClick = { showStarterFactDropdown = true }
                            )
                            DropdownMenu(
                                expanded = showStarterFactDropdown,
                                onDismissRequest = { showStarterFactDropdown = false },
                                properties = PopupProperties(clippingEnabled = true),
                                modifier = Modifier
                                    .background(Color(0xFF0F172A))
                                    .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                            ) {
                                listOf("Role / Company: ", "Met at: ", "Prefers: ", "Birthday: ").forEach { tpl ->
                                    DropdownMenuItem(
                                        text = { Text(tpl, color = Color.White, fontWeight = FontWeight.Medium) },
                                        onClick = {
                                            firstFact = tpl + firstFact
                                            showStarterFactDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    },
                    bodyContent = {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = { Text("Full Name *", color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            colors = seamlessInnerTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp)
                                .height(1.dp)
                                .background(Color(0xFF334155))
                        )
                        OutlinedTextField(
                            value = firstFact,
                            onValueChange = { firstFact = it },
                            placeholder = { Text("Initial Fact / Preference (e.g. Drinks matcha, lives in NYC)", color = Color(0xFF94A3B8)) },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                            colors = seamlessInnerTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val factsList = if (firstFact.isNotBlank()) listOf(firstFact.trim()) else emptyList()
                    onAdd(name.trim(), relationship.trim(), factsList, avatarUri)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = RecallVioletPrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                enabled = name.isNotBlank()
            ) {
                Text("Save Person", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
