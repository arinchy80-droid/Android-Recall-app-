package com.example.ui.memories

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.PersonEntity
import com.example.data.model.NoteWithDetails
import com.example.ui.RecallViewModel
import com.example.ui.gallery.GalleryScreen
import com.example.ui.home.MemoraeCompactMemoryCard
import com.example.ui.navigation.Screen
import com.example.ui.people.AddOrEditPlaceDialog
import com.example.ui.people.PeopleScreen
import com.example.ui.people.PlaceDossierView
import com.example.ui.people.PlacesHubContent
import com.example.ui.stream.StreamScreen
import com.example.ui.theme.NeuralAppBackground
import com.example.ui.theme.RecallCyanSecondary
import com.example.ui.theme.RecallEmeraldAccent
import com.example.ui.theme.RecallVioletPrimary
import com.example.ui.theme.seamlessInnerTextFieldColors
import com.example.util.DateUtils
import com.example.util.MemoryIntelligenceHelper
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

private val MEMORIES_SECTIONS = listOf(
    "All Memories" to "🧠",
    "People" to "👤",
    "Places" to "📍",
    "Events" to "📅",
    "Conversations" to "🎙️",
    "Ideas & Thoughts" to "💡",
    "Timeline" to "⏳",
    "Memory Graph" to "🕸️",
    "Images & Docs" to "🖼️"
)

@Composable
fun MemoraeMemoriesHubScreen(
    viewModel: RecallViewModel,
    onNavigateToScreen: (Screen, String?) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val activeSection by viewModel.memoriesSubTab.collectAsStateWithLifecycle()
    val allNotes by viewModel.notes.collectAsStateWithLifecycle()
    val allPeople by viewModel.people.collectAsStateWithLifecycle()
    val allPlaces by viewModel.allPlaces.collectAsStateWithLifecycle()
    val allProjects by viewModel.allProjects.collectAsStateWithLifecycle()
    val allCommitments by viewModel.allCommitments.collectAsStateWithLifecycle()
    val allMemoryLinks by viewModel.allMemoryLinks.collectAsStateWithLifecycle()
    val selectedPlace by viewModel.selectedPlace.collectAsStateWithLifecycle()

    var showAddPlaceDialog by remember { mutableStateOf(false) }
    var timelineSearchQuery by remember { mutableStateOf("") }
    var selectedTimelinePersonFilter by remember { mutableStateOf<String?>(null) }
    var selectedTimelineLocationFilter by remember { mutableStateOf<String?>(null) }

    NeuralAppBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            // Horizontal Memorae Section Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MEMORIES_SECTIONS.forEach { (sectionName, emoji) ->
                    val isSelected = activeSection == sectionName
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) RecallVioletPrimary else Color(0xFF0F172A).copy(alpha = 0.88f),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) RecallCyanSecondary else Color(0xFF334155)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { viewModel.memoriesSubTab.value = sectionName }
                            .testTag("memories_tab_${sectionName.lowercase().replace(" ", "_")}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = emoji, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = sectionName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (activeSection) {
                    "All Memories" -> {
                        StreamScreen(
                            viewModel = viewModel,
                            onNavigateToPerson = { person ->
                                viewModel.selectPerson(person)
                                viewModel.memoriesSubTab.value = "People"
                            },
                            onNavigateToDebrief = {
                                onNavigateToScreen(Screen.Capture, "Daily Debrief")
                            },
                            onNavigateToSettings = onNavigateToSettings,
                            onNavigateToReminders = {
                                onNavigateToScreen(Screen.Organise, "Reminders")
                            }
                        )
                    }

                    "People" -> {
                        PeopleScreen(viewModel = viewModel)
                    }

                    "Places" -> {
                        if (selectedPlace != null) {
                            val place = selectedPlace!!
                            val linkedNotes = remember(place, allNotes) {
                                allNotes.filter {
                                    it.note.placeId == place.id ||
                                        it.note.getLocationName()?.equals(place.name, ignoreCase = true) == true
                                }
                            }
                            PlaceDossierView(
                                place = place,
                                linkedNotes = linkedNotes,
                                onBack = { viewModel.clearSelectedPlace() },
                                onUpdatePlace = { viewModel.savePlace(it) },
                                onDeletePlace = { viewModel.deletePlace(it) },
                                onStartNoteAtPlace = { viewModel.startNoteAtPlace(it) },
                                onOpenNote = { viewModel.openNoteEditor(it) },
                                onOpenPerson = { p ->
                                    viewModel.clearSelectedPlace()
                                    viewModel.selectPerson(p)
                                    viewModel.memoriesSubTab.value = "People"
                                }
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                                PlacesHubContent(
                                    places = allPlaces,
                                    allNotes = allNotes,
                                    onSelectPlace = { viewModel.selectPlace(it) },
                                    onAddPlaceClick = { showAddPlaceDialog = true }
                                )
                            }
                        }
                    }

                    "Events" -> {
                        val eventNotes = remember(allNotes) {
                            allNotes.filter {
                                it.note.category.equals("Events", ignoreCase = true) ||
                                    it.note.memoryType == "IMPORTANT_DATES" ||
                                    it.note.memoryType == "EXPERIENCES" ||
                                    it.note.category.equals("Travel", ignoreCase = true)
                            }
                        }
                        FilteredMemoriesSectionView(
                            title = "📅 Events & Experiences (${eventNotes.size})",
                            subtitle = "Important dates, milestones, trips, and personal events connected to people and places.",
                            actionButtonLabel = "+ Log Event Memory",
                            notes = eventNotes,
                            onAddClick = {
                                val draft = NoteEntity(
                                    id = UUID.randomUUID().toString(),
                                    title = "",
                                    content = "",
                                    category = "Events",
                                    memoryType = "IMPORTANT_DATES"
                                )
                                viewModel.openNoteEditor(NoteWithDetails(note = draft))
                            },
                            onOpenNote = { viewModel.openNoteEditor(it) },
                            onPersonClick = { p ->
                                viewModel.selectPerson(p)
                                viewModel.memoriesSubTab.value = "People"
                            }
                        )
                    }

                    "Conversations" -> {
                        val convNotes = remember(allNotes) {
                            allNotes.filter {
                                it.note.memoryType == "CONVERSATIONS" ||
                                    it.note.category.equals("Meeting", ignoreCase = true) ||
                                    !it.note.rawTranscription.isNullOrBlank()
                            }
                        }
                        FilteredMemoriesSectionView(
                            title = "🎙️ Conversations & Meetings (${convNotes.size})",
                            subtitle = "Recorded voice conversations, meetings, and daily debrief transcripts.",
                            actionButtonLabel = "+ Record Conversation",
                            notes = convNotes,
                            onAddClick = {
                                onNavigateToScreen(Screen.Capture, "Daily Debrief")
                            },
                            onOpenNote = { viewModel.openNoteEditor(it) },
                            onPersonClick = { p ->
                                viewModel.selectPerson(p)
                                viewModel.memoriesSubTab.value = "People"
                            }
                        )
                    }

                    "Ideas & Thoughts" -> {
                        val ideaNotes = remember(allNotes) {
                            allNotes.filter {
                                it.note.memoryType == "IDEAS" ||
                                    it.note.category.equals("Ideas", ignoreCase = true) ||
                                    it.note.category.equals("General", ignoreCase = true) ||
                                    it.note.isPinned
                            }
                        }
                        FilteredMemoriesSectionView(
                            title = "💡 Ideas, Thoughts & Pinned Notes (${ideaNotes.size})",
                            subtitle = "Quick inspirations, creative concepts, and pinned personal knowledge.",
                            actionButtonLabel = "+ New Idea",
                            notes = ideaNotes,
                            onAddClick = {
                                val draft = NoteEntity(
                                    id = UUID.randomUUID().toString(),
                                    title = "",
                                    content = "",
                                    category = "Ideas",
                                    memoryType = "IDEAS"
                                )
                                viewModel.openNoteEditor(NoteWithDetails(note = draft))
                            },
                            onOpenNote = { viewModel.openNoteEditor(it) },
                            onPersonClick = { p ->
                                viewModel.selectPerson(p)
                                viewModel.memoriesSubTab.value = "People"
                            }
                        )
                    }

                    "Timeline" -> {
                        LifeTimelineSectionView(
                            allNotes = allNotes,
                            allPeople = allPeople,
                            allPlaces = allPlaces,
                            allProjects = allProjects,
                            allCommitments = allCommitments,
                            allMemoryLinks = allMemoryLinks,
                            searchQuery = timelineSearchQuery,
                            onSearchQueryChange = { timelineSearchQuery = it },
                            selectedPersonFilter = selectedTimelinePersonFilter,
                            onSelectPersonFilter = { selectedTimelinePersonFilter = it },
                            selectedLocationFilter = selectedTimelineLocationFilter,
                            onSelectLocationFilter = { selectedTimelineLocationFilter = it },
                            onOpenNoteById = { viewModel.openNoteById(it) },
                            onOpenPerson = { p ->
                                viewModel.selectPerson(p)
                                viewModel.memoriesSubTab.value = "People"
                            }
                        )
                    }

                    "Memory Graph" -> {
                        PersonalKnowledgeGraphSectionView(
                            viewModel = viewModel,
                            allNotes = allNotes,
                            allPeople = allPeople,
                            allPlaces = allPlaces,
                            allProjects = allProjects,
                            allCommitments = allCommitments,
                            allMemoryLinks = allMemoryLinks,
                            onOpenNote = { viewModel.openNoteEditor(it) },
                            onOpenPerson = { p ->
                                viewModel.selectPerson(p)
                                viewModel.memoriesSubTab.value = "People"
                            },
                            onOpenPlace = { pl ->
                                viewModel.selectPlace(pl)
                                viewModel.memoriesSubTab.value = "Places"
                            },
                            onOpenProject = { proj ->
                                viewModel.selectProject(proj)
                                onNavigateToScreen(Screen.Organise, "Projects")
                            }
                        )
                    }

                    "Images & Docs" -> {
                        GalleryScreen(
                            viewModel = viewModel,
                            onOpenNote = { noteId -> viewModel.openNoteById(noteId) }
                        )
                    }
                }
            }
        }

        if (showAddPlaceDialog) {
            AddOrEditPlaceDialog(
                initialPlace = null,
                onDismiss = { showAddPlaceDialog = false },
                onSave = { newPlace ->
                    viewModel.savePlace(newPlace)
                    showAddPlaceDialog = false
                }
            )
        }
    }
}

@Composable
private fun FilteredMemoriesSectionView(
    title: String,
    subtitle: String,
    actionButtonLabel: String,
    notes: List<NoteWithDetails>,
    onAddClick: () -> Unit,
    onOpenNote: (NoteWithDetails) -> Unit,
    onPersonClick: (PersonEntity) -> Unit
) {
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
                border = BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onAddClick,
                            colors = ButtonDefaults.buttonColors(containerColor = RecallVioletPrimary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(actionButtonLabel, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (notes.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No memories in this section yet", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Tap \"$actionButtonLabel\" above to add one.",
                            color = Color(0xFF94A3B8),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        } else {
            items(notes, key = { it.note.id }) { item ->
                MemoraeCompactMemoryCard(
                    noteWithDetails = item,
                    onClick = { onOpenNote(item) },
                    onPersonClick = onPersonClick
                )
            }
        }
    }
}

@Composable
private fun LifeTimelineSectionView(
    allNotes: List<NoteWithDetails>,
    allPeople: List<PersonEntity>,
    allPlaces: List<com.example.data.local.entity.PlaceEntity>,
    allProjects: List<com.example.data.local.entity.ProjectEntity>,
    allCommitments: List<com.example.data.local.entity.CommitmentEntity>,
    allMemoryLinks: List<com.example.data.local.entity.MemoryLinkEntity>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedPersonFilter: String?,
    onSelectPersonFilter: (String?) -> Unit,
    selectedLocationFilter: String?,
    onSelectLocationFilter: (String?) -> Unit,
    onOpenNoteById: (String) -> Unit,
    onOpenPerson: (PersonEntity) -> Unit
) {
    val report = remember(allNotes, allPeople, allPlaces, allProjects, allCommitments, allMemoryLinks) {
        MemoryIntelligenceHelper.buildLifeIntelligenceReport(
            allNotes = allNotes,
            allPeople = allPeople,
            allPlaces = allPlaces,
            allProjects = allProjects,
            allCommitments = allCommitments,
            allMemoryLinks = allMemoryLinks
        )
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
                border = BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "⏳ Chronological Life Timeline",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Filter your chronological history by keyword, person, or location.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = {
                            Text("Filter timeline events, decisions, places...", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = RecallCyanSecondary, modifier = Modifier.size(16.dp))
                        },
                        singleLine = true,
                        colors = seamlessInnerTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E293B))
                    )

                    if (allPeople.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selectedPersonFilter == null) RecallVioletPrimary else Color(0xFF1E293B),
                                modifier = Modifier.clickable { onSelectPersonFilter(null) }
                            ) {
                                Text(
                                    text = "All People",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            allPeople.take(8).forEach { person ->
                                val isSel = selectedPersonFilter == person.name
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSel) RecallVioletPrimary else Color(0xFF1E293B),
                                    modifier = Modifier.clickable {
                                        onSelectPersonFilter(if (isSel) null else person.name)
                                    }
                                ) {
                                    Text(
                                        text = "👤 ${person.name}",
                                        fontSize = 11.sp,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (allPlaces.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selectedLocationFilter == null) RecallEmeraldAccent.copy(alpha = 0.4f) else Color(0xFF1E293B),
                                modifier = Modifier.clickable { onSelectLocationFilter(null) }
                            ) {
                                Text(
                                    text = "All Places",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            allPlaces.take(6).forEach { pl ->
                                val isSel = selectedLocationFilter == pl.name
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSel) RecallEmeraldAccent.copy(alpha = 0.55f) else Color(0xFF1E293B),
                                    modifier = Modifier.clickable {
                                        onSelectLocationFilter(if (isSel) null else pl.name)
                                    }
                                ) {
                                    Text(
                                        text = "📍 ${pl.name}",
                                        fontSize = 11.sp,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        report.timelineEpochs.forEach { epoch ->
            val filteredEntries = epoch.entries.filter { entry ->
                val matchesQuery = searchQuery.isBlank() ||
                    entry.title.contains(searchQuery, ignoreCase = true) ||
                    entry.subtitle.contains(searchQuery, ignoreCase = true)
                val matchesPerson = selectedPersonFilter == null ||
                    entry.person?.name.equals(selectedPersonFilter, ignoreCase = true) ||
                    entry.subtitle.contains(selectedPersonFilter, ignoreCase = true)
                val matchesPlace = selectedLocationFilter == null ||
                    entry.subtitle.contains(selectedLocationFilter, ignoreCase = true) ||
                    entry.title.contains(selectedLocationFilter, ignoreCase = true)
                matchesQuery && matchesPerson && matchesPlace
            }

            if (filteredEntries.isNotEmpty()) {
                item {
                    Text(
                        text = "${epoch.epochTitle} (${filteredEntries.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = RecallCyanSecondary,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                items(filteredEntries, key = { it.id }) { entry ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.88f),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                entry.noteId?.let { onOpenNoteById(it) }
                                    ?: entry.person?.let { onOpenPerson(it) }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = entry.icon, fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = entry.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = DateUtils.formatDayMonth(entry.timestamp),
                                style = MaterialTheme.typography.labelSmall,
                                color = RecallCyanSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonalKnowledgeGraphSectionView(
    viewModel: RecallViewModel,
    allNotes: List<NoteWithDetails>,
    allPeople: List<PersonEntity>,
    allPlaces: List<com.example.data.local.entity.PlaceEntity>,
    allProjects: List<com.example.data.local.entity.ProjectEntity>,
    allCommitments: List<com.example.data.local.entity.CommitmentEntity>,
    allMemoryLinks: List<com.example.data.local.entity.MemoryLinkEntity>,
    onOpenNote: (NoteWithDetails) -> Unit,
    onOpenPerson: (PersonEntity) -> Unit,
    onOpenPlace: (com.example.data.local.entity.PlaceEntity) -> Unit,
    onOpenProject: (com.example.data.local.entity.ProjectEntity) -> Unit
) {
    val notesById = remember(allNotes) { allNotes.associateBy { it.note.id } }
    var graphFilter by remember { mutableStateOf("All Nodes") }
    var showCreateLinkDialog by remember { mutableStateOf(false) }
    var sourceNoteIdDraft by remember(allNotes) { mutableStateOf(allNotes.firstOrNull()?.note?.id ?: "") }
    var targetNoteIdDraft by remember(allNotes) { mutableStateOf(allNotes.drop(1).firstOrNull()?.note?.id ?: "") }
    var relationTypeDraft by remember { mutableStateOf("RELATED_TO") }

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
                border = BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(listOf(RecallVioletPrimary, RecallCyanSecondary))
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🕸️ Personal Knowledge Graph",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "${allNotes.size} memories • ${allPeople.size} people • ${allPlaces.size} places • ${allProjects.size} projects • ${allMemoryLinks.size} neural edges",
                                style = MaterialTheme.typography.labelSmall,
                                color = RecallCyanSecondary
                            )
                        }
                        if (allNotes.size >= 2) {
                            Button(
                                onClick = { showCreateLinkDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = RecallVioletPrimary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("graph_create_link_btn")
                            ) {
                                Text("+ Link Memories", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Interactive Node Filter Pills
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "All Nodes",
                            "People (${allPeople.size})",
                            "Places (${allPlaces.size})",
                            "Projects (${allProjects.size})",
                            "Edges (${allMemoryLinks.size})"
                        ).forEach { filterLabel ->
                            val key = filterLabel.substringBefore(" (")
                            val isSel = graphFilter == key
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) RecallCyanSecondary else Color(0xFF1E293B),
                                border = BorderStroke(1.dp, if (isSel) RecallCyanSecondary else Color(0xFF334155)),
                                modifier = Modifier.clickable { graphFilter = key }
                            ) {
                                Text(
                                    text = filterLabel,
                                    fontSize = 11.sp,
                                    color = if (isSel) Color(0xFF050811) else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Interactive Visual Knowledge Graph Constellation Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF070B14).copy(alpha = 0.85f))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val radius = (minOf(size.width, size.height) * 0.36f).coerceAtLeast(24f)

                            val nodeCount = (allPeople.size + allPlaces.size + allProjects.size + allNotes.take(6).size).coerceIn(4, 14)
                            val positions = (0 until nodeCount).map { idx ->
                                val angle = (2.0 * Math.PI * idx) / nodeCount
                                Offset(
                                    x = cx + (radius * cos(angle)).toFloat(),
                                    y = cy + (radius * sin(angle)).toFloat()
                                )
                            }

                            // Draw spokes from central neural hub & cross links
                            positions.forEachIndexed { idx, pos ->
                                drawLine(
                                    color = RecallCyanSecondary.copy(alpha = 0.35f),
                                    start = Offset(cx, cy),
                                    end = pos,
                                    strokeWidth = 2f
                                )
                                val nextPos = positions[(idx + 1) % positions.size]
                                drawLine(
                                    color = RecallVioletPrimary.copy(alpha = 0.28f),
                                    start = pos,
                                    end = nextPos,
                                    strokeWidth = 1.5f
                                )
                            }

                            // Central Neural Hub Node
                            drawCircle(
                                color = RecallVioletPrimary.copy(alpha = 0.35f),
                                radius = 22f,
                                center = Offset(cx, cy)
                            )
                            drawCircle(
                                color = RecallCyanSecondary,
                                radius = 9f,
                                center = Offset(cx, cy)
                            )

                            // Satellite Entity Nodes
                            positions.forEachIndexed { idx, pos ->
                                val nodeColor = when (idx % 3) {
                                    0 -> RecallVioletPrimary
                                    1 -> RecallEmeraldAccent
                                    else -> RecallCyanSecondary
                                }
                                drawCircle(
                                    color = nodeColor.copy(alpha = 0.3f),
                                    radius = 13f,
                                    center = pos
                                )
                                drawCircle(
                                    color = nodeColor,
                                    radius = 6f,
                                    center = pos
                                )
                            }
                        }
                    }
                }
            }
        }

        // Connected Entity Hubs: People
        if ((graphFilter == "All Nodes" || graphFilter == "People") && allPeople.isNotEmpty()) {
            item {
                Text(
                    text = "👥 People Connections (${allPeople.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
            items(allPeople.take(10), key = { "graph_p_${it.id}" }) { person ->
                val pNotes = allNotes.filter { n -> n.people.any { it.id == person.id } }
                val pComms = allCommitments.filter { it.personId == person.id && !it.isCompleted }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, RecallVioletPrimary.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenPerson(person) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("👤", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = person.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${pNotes.size} connected memory(s) • ${pComms.size} open promise(s)",
                                style = MaterialTheme.typography.labelSmall,
                                color = RecallCyanSecondary
                            )
                        }
                        Text("Open Dossier →", fontSize = 11.sp, color = Color(0xFFCBD5E1), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Connected Entity Hubs: Places
        if ((graphFilter == "All Nodes" || graphFilter == "Places") && allPlaces.isNotEmpty()) {
            item {
                Text(
                    text = "📍 Places & Spatial Nodes (${allPlaces.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
            items(allPlaces.take(10), key = { "graph_pl_${it.id}" }) { place ->
                val plNotes = allNotes.filter {
                    it.note.placeId == place.id ||
                        it.note.getLocationName()?.equals(place.name, ignoreCase = true) == true
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, RecallEmeraldAccent.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenPlace(place) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📍", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = place.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${place.category} • ${plNotes.size} linked memory(s) • ${place.visitCount} visit(s)",
                                style = MaterialTheme.typography.labelSmall,
                                color = RecallEmeraldAccent
                            )
                        }
                        Text("Open Place →", fontSize = 11.sp, color = Color(0xFFCBD5E1), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Connected Entity Hubs: Projects
        if ((graphFilter == "All Nodes" || graphFilter == "Projects") && allProjects.isNotEmpty()) {
            item {
                Text(
                    text = "🚀 Project Nodes (${allProjects.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
            items(allProjects.take(10), key = { "graph_proj_${it.id}" }) { project ->
                val projNotes = allNotes.filter { it.note.projectId == project.id }
                val projComms = allCommitments.filter { it.projectId == project.id && !it.isCompleted }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenProject(project) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🚀", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = project.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${projNotes.size} linked memory(s) • ${projComms.size} open commitment(s)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFDE68A)
                            )
                        }
                        Text("Open Project →", fontSize = 11.sp, color = Color(0xFFCBD5E1), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Cross-Memory Links
        if ((graphFilter == "All Nodes" || graphFilter == "Edges") && allMemoryLinks.isNotEmpty()) {
            item {
                Text(
                    text = "🔗 Cross-Memory Neural Edges (${allMemoryLinks.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
            items(allMemoryLinks, key = { it.id }) { link ->
                val src = notesById[link.sourceNoteId]
                val dst = notesById[link.targetNoteId]
                if (src != null && dst != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.88f),
                        border = BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenNote(src) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, tint = RecallCyanSecondary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${src.note.title}  ⟷  ${dst.note.title}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Relation: ${link.relationType} (${(link.strengthScore * 100).toInt()}% confidence)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            androidx.compose.material3.TextButton(
                                onClick = { viewModel.deleteMemoryLink(link.id) }
                            ) {
                                Text("Unlink", color = Color(0xFFEF4444), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateLinkDialog && allNotes.size >= 2) {
        val relationTypes = listOf("RELATED_TO", "CONTINUES", "EXPANDS", "CAUSES", "FOLLOW_UP")
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCreateLinkDialog = false },
            containerColor = Color(0xFF0F172A),
            title = {
                Text("🔗 Connect Two Memories in Knowledge Graph", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Source Memory:", color = RecallCyanSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allNotes.take(8).forEach { n ->
                            val isSel = sourceNoteIdDraft == n.note.id
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) RecallVioletPrimary else Color(0xFF1E293B),
                                modifier = Modifier.clickable { sourceNoteIdDraft = n.note.id }
                            ) {
                                Text(
                                    text = n.note.title.take(24),
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Text("Relationship Type:", color = RecallCyanSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        relationTypes.forEach { rel ->
                            val isSel = relationTypeDraft == rel
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) RecallCyanSecondary else Color(0xFF1E293B),
                                modifier = Modifier.clickable { relationTypeDraft = rel }
                            ) {
                                Text(
                                    text = rel,
                                    fontSize = 10.sp,
                                    color = if (isSel) Color(0xFF050811) else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Text("Target Memory:", color = RecallCyanSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allNotes.take(8).forEach { n ->
                            val isSel = targetNoteIdDraft == n.note.id
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) RecallEmeraldAccent else Color(0xFF1E293B),
                                modifier = Modifier.clickable { targetNoteIdDraft = n.note.id }
                            ) {
                                Text(
                                    text = n.note.title.take(24),
                                    fontSize = 11.sp,
                                    color = if (isSel) Color(0xFF050811) else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (sourceNoteIdDraft.isNotBlank() && targetNoteIdDraft.isNotBlank() && sourceNoteIdDraft != targetNoteIdDraft) {
                            viewModel.linkMemories(sourceNoteIdDraft, targetNoteIdDraft, relationTypeDraft)
                            showCreateLinkDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RecallEmeraldAccent)
                ) {
                    Text("Create Neural Edge", color = Color(0xFF050811), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showCreateLinkDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}
