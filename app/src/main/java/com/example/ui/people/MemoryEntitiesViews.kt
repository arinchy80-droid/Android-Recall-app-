package com.example.ui.people

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CommitmentEntity
import com.example.data.local.entity.PersonEntity
import com.example.data.local.entity.PlaceEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.model.NoteWithDetails
import com.example.ui.theme.NeuralAppBackground
import com.example.ui.theme.RecallCyanSecondary
import com.example.ui.theme.RecallVioletPrimary
import com.example.ui.theme.VibrantTagBadge
import com.example.ui.theme.getVibrantTagColor
import com.example.ui.theme.translucentWhiteTextFieldColors
import com.example.util.DateUtils
import java.util.UUID

enum class MemoryEntityHubTab(val label: String, val emoji: String) {
    PEOPLE("People", "👤"),
    PLACES("Places", "📍"),
    PROJECTS("Projects", "🚀"),
    COMMITMENTS("Commitments", "🤝")
}

@Composable
fun EntityHubTabRow(
    selectedTab: MemoryEntityHubTab,
    peopleCount: Int,
    placesCount: Int,
    projectsCount: Int,
    commitmentsCount: Int,
    onSelectTab: (MemoryEntityHubTab) -> Unit
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MemoryEntityHubTab.values().forEach { tab ->
            val isSelected = selectedTab == tab
            val count = when (tab) {
                MemoryEntityHubTab.PEOPLE -> peopleCount
                MemoryEntityHubTab.PLACES -> placesCount
                MemoryEntityHubTab.PROJECTS -> projectsCount
                MemoryEntityHubTab.COMMITMENTS -> commitmentsCount
            }
            val accentColor = when (tab) {
                MemoryEntityHubTab.PEOPLE -> RecallVioletPrimary
                MemoryEntityHubTab.PLACES -> Color(0xFF10B981)
                MemoryEntityHubTab.PROJECTS -> RecallCyanSecondary
                MemoryEntityHubTab.COMMITMENTS -> Color(0xFFF59E0B)
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) accentColor.copy(alpha = 0.28f) else Color(0xFF0F172A).copy(alpha = 0.85f),
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) accentColor else Color(0xFF334155)
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelectTab(tab) }
                    .testTag("entity_hub_tab_${tab.name.lowercase()}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${tab.emoji} ${tab.label}",
                        color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) accentColor else Color(0xFF1E293B)
                    ) {
                        Text(
                            text = count.toString(),
                            color = if (isSelected) Color(0xFF040711) else Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// 1. PLACES HUB & PLACE DOSSIER VIEW
// ============================================================================

@Composable
fun PlacesHubContent(
    places: List<PlaceEntity>,
    allNotes: List<NoteWithDetails>,
    onSelectPlace: (PlaceEntity) -> Unit,
    onAddPlaceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (places.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(top = 48.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = Color(0xFF10B981)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "No Places Recorded Yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Places tagged on notes or added here build geographic memory dossiers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCBD5E1)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onAddPlaceClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF040711), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Place", color = Color(0xFF040711), fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            items(places, key = { it.id }) { place ->
                val matchingNotes = remember(place, allNotes) {
                    allNotes.filter {
                        it.note.placeId == place.id ||
                            it.note.getLocationName().equals(place.name, ignoreCase = true)
                    }
                }
                val associatedPeople = remember(matchingNotes) {
                    matchingNotes.flatMap { it.people }.distinctBy { it.id }
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            Brush.horizontalGradient(
                                listOf(Color(0xFF10B981).copy(alpha = 0.55f), RecallCyanSecondary.copy(alpha = 0.45f))
                            ),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { onSelectPlace(place) }
                        .testTag("place_item_${place.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1120).copy(alpha = 0.72f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFF34D399)),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("📍", fontSize = 22.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = place.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = place.category,
                                        color = Color(0xFF34D399),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (!place.address.isNullOrBlank()) {
                                Text(
                                    text = place.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = RecallCyanSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${matchingNotes.size} memories • ${associatedPeople.size} people • ${place.visitCount} visit(s)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaceDossierView(
    place: PlaceEntity,
    linkedNotes: List<NoteWithDetails>,
    onBack: () -> Unit,
    onUpdatePlace: (PlaceEntity) -> Unit,
    onDeletePlace: (String) -> Unit,
    onStartNoteAtPlace: (PlaceEntity) -> Unit,
    onOpenNote: (NoteWithDetails) -> Unit,
    onOpenPerson: (PersonEntity) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    val associatedPeople = remember(linkedNotes) {
        linkedNotes.flatMap { it.people }.distinctBy { it.id }
    }

    NeuralAppBackground {
        Column(modifier = Modifier.fillMaxSize()) {
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
                    "Place Dossier",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.22f),
                    border = BorderStroke(1.dp, Color(0xFF34D399)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onStartNoteAtPlace(place) }
                ) {
                    Text(
                        text = "+ Memory Here",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF34D399),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                IconButton(onClick = { showEditDialog = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Place", tint = RecallCyanSecondary)
                }
                IconButton(onClick = { onDeletePlace(place.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Place", tint = Color(0xFFF87171))
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.6f), RoundedCornerShape(18.dp)),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.85f))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📍", fontSize = 28.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = place.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${place.category} • ${place.visitCount} visit(s)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF34D399)
                                    )
                                }
                            }
                            if (!place.address.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Address / Area: ${place.address}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = RecallCyanSecondary
                                )
                            }
                            if (place.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = place.notes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                        }
                    }
                }

                if (associatedPeople.isNotEmpty()) {
                    item {
                        Text(
                            text = "👤 People Associated with ${place.name} (${associatedPeople.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(associatedPeople, key = { it.id }) { person ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF1E293B),
                                    border = BorderStroke(1.dp, RecallVioletPrimary.copy(alpha = 0.6f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onOpenPerson(person) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("👤 ${person.name}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "📝 Memories at ${place.name} (${linkedNotes.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (linkedNotes.isEmpty()) {
                    item {
                        Text(
                            text = "No memories linked to this place yet. Tap '+ Memory Here' to record one.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                } else {
                    items(linkedNotes, key = { it.note.id }) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenNote(item) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.85f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = item.note.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = DateUtils.formatRelativeTime(item.note.createdAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
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
    }

    if (showEditDialog) {
        AddOrEditPlaceDialog(
            initialPlace = place,
            onDismiss = { showEditDialog = false },
            onSave = { updated ->
                onUpdatePlace(updated)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun AddOrEditPlaceDialog(
    initialPlace: PlaceEntity? = null,
    onDismiss: () -> Unit,
    onSave: (PlaceEntity) -> Unit
) {
    var name by remember { mutableStateOf(initialPlace?.name ?: "") }
    var category by remember { mutableStateOf(initialPlace?.category ?: "Office") }
    var address by remember { mutableStateOf(initialPlace?.address ?: "") }
    var notes by remember { mutableStateOf(initialPlace?.notes ?: "") }
    val categories = remember { listOf("Office", "Coffee Shop", "Home", "Client Site", "Restaurant", "Travel", "Other") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        title = {
            Text(
                if (initialPlace == null) "📍 Add Place to Memory" else "✏️ Edit Place Dossier",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Place Name (e.g. Perth HQ, Blue Bottle Cafe) *", color = Color(0xFF94A3B8)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                    colors = translucentWhiteTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        val isSelected = category.equals(cat, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFF10B981).copy(alpha = 0.28f) else Color(0xFF1E293B),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF34D399) else Color(0xFF334155)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { category = cat }
                        ) {
                            Text(
                                text = cat,
                                color = if (isSelected) Color(0xFF34D399) else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    placeholder = { Text("Address / Suburb / City (optional)", color = Color(0xFF94A3B8)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                    colors = translucentWhiteTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text("Notes about this place (Wi-Fi, parking, favorite table...)", color = Color(0xFF94A3B8)) },
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                    colors = translucentWhiteTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val entity = (initialPlace ?: PlaceEntity(id = UUID.randomUUID().toString(), name = name.trim())).copy(
                        name = name.trim(),
                        category = category.trim().ifBlank { "Location" },
                        address = address.trim().ifBlank { null },
                        notes = notes.trim(),
                        lastVisitedAt = System.currentTimeMillis()
                    )
                    onSave(entity)
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text("Save Place", color = Color(0xFF040711), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color(0xFF94A3B8)) }
        }
    )
}

// ============================================================================
// 2. PROJECTS HUB & PROJECT DOSSIER VIEW
// ============================================================================

@Composable
fun ProjectsHubContent(
    projects: List<ProjectEntity>,
    allNotes: List<NoteWithDetails>,
    allCommitments: List<CommitmentEntity>,
    onSelectProject: (ProjectEntity) -> Unit,
    onAddProjectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (projects.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(top = 48.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.FolderSpecial,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = RecallCyanSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "No Active Projects Yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Organize related conversations, ideas, commitments, and tasks under Projects.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCBD5E1)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onAddProjectClick,
                    colors = ButtonDefaults.buttonColors(containerColor = RecallCyanSecondary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF040711), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Project", color = Color(0xFF040711), fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            items(projects, key = { it.id }) { project ->
                val projNotes = remember(project, allNotes) {
                    allNotes.filter { it.note.projectId == project.id }
                }
                val projCommitments = remember(project, allCommitments) {
                    allCommitments.filter { it.projectId == project.id && !it.isCompleted }
                }
                val statusColor = when (project.status.uppercase()) {
                    "ACTIVE" -> Color(0xFF10B981)
                    "PAUSED" -> Color(0xFFF59E0B)
                    "COMPLETED" -> RecallCyanSecondary
                    else -> Color(0xFF94A3B8)
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            Brush.horizontalGradient(
                                listOf(RecallCyanSecondary.copy(alpha = 0.55f), RecallVioletPrimary.copy(alpha = 0.45f))
                            ),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { onSelectProject(project) }
                        .testTag("project_item_${project.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1120).copy(alpha = 0.72f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🚀", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = project.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = statusColor.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = project.status.uppercase(),
                                    color = statusColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (project.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = project.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${projNotes.size} linked memories • ${projCommitments.size} active commitment(s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = RecallCyanSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectDossierView(
    project: ProjectEntity,
    linkedNotes: List<NoteWithDetails>,
    projectCommitments: List<CommitmentEntity>,
    onBack: () -> Unit,
    onUpdateProject: (ProjectEntity) -> Unit,
    onDeleteProject: (String) -> Unit,
    onStartNoteForProject: (ProjectEntity) -> Unit,
    onOpenNote: (NoteWithDetails) -> Unit,
    onToggleCommitment: (String, Boolean) -> Unit,
    onAddCommitmentClick: () -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    val statuses = remember { listOf("ACTIVE", "PAUSED", "COMPLETED") }

    NeuralAppBackground {
        Column(modifier = Modifier.fillMaxSize()) {
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
                    "Project Workspace",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = RecallCyanSecondary.copy(alpha = 0.22f),
                    border = BorderStroke(1.dp, RecallCyanSecondary),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onStartNoteForProject(project) }
                ) {
                    Text(
                        text = "+ Project Note",
                        style = MaterialTheme.typography.labelSmall,
                        color = RecallCyanSecondary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                IconButton(onClick = { showEditDialog = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Project", tint = RecallCyanSecondary)
                }
                IconButton(onClick = { onDeleteProject(project.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Project", tint = Color(0xFFF87171))
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, RecallCyanSecondary.copy(alpha = 0.6f), RoundedCornerShape(18.dp)),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.85f))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "🚀 ${project.name}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            if (project.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = project.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                statuses.forEach { st ->
                                    val isSelected = project.status.equals(st, ignoreCase = true)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) RecallCyanSecondary.copy(alpha = 0.25f) else Color(0xFF1E293B),
                                        border = BorderStroke(1.dp, if (isSelected) RecallCyanSecondary else Color(0xFF334155)),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onUpdateProject(project.copy(status = st)) }
                                    ) {
                                        Text(
                                            text = st,
                                            color = if (isSelected) RecallCyanSecondary else Color.White,
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

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "🤝 Project Commitments (${projectCommitments.count { !it.isCompleted }} open)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFBBF24)
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onAddCommitmentClick() }
                        ) {
                            Text(
                                text = "+ Commitment",
                                color = Color(0xFFFBBF24),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                items(projectCommitments, key = { it.id }) { c ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E293B), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = c.isCompleted,
                            onCheckedChange = { onToggleCommitment(c.id, it) }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = c.description,
                            color = if (c.isCompleted) Color(0xFF64748B) else Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Text(
                        text = "📝 Project Memories (${linkedNotes.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (linkedNotes.isEmpty()) {
                    item {
                        Text(
                            text = "No memories linked to this project yet. Tap '+ Project Note' or select this project inside the Note Editor.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                } else {
                    items(linkedNotes, key = { it.note.id }) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenNote(item) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.85f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = item.note.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = DateUtils.formatRelativeTime(item.note.createdAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
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
    }

    if (showEditDialog) {
        AddOrEditProjectDialog(
            initialProject = project,
            onDismiss = { showEditDialog = false },
            onSave = { updated ->
                onUpdateProject(updated)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun AddOrEditProjectDialog(
    initialProject: ProjectEntity? = null,
    onDismiss: () -> Unit,
    onSave: (ProjectEntity) -> Unit
) {
    var name by remember { mutableStateOf(initialProject?.name ?: "") }
    var description by remember { mutableStateOf(initialProject?.description ?: "") }
    var status by remember { mutableStateOf(initialProject?.status ?: "ACTIVE") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        title = {
            Text(
                if (initialProject == null) "🚀 Create New Project" else "✏️ Edit Project",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Project Name (e.g. Q4 Product Launch, Home Renovation) *", color = Color(0xFF94A3B8)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                    colors = translucentWhiteTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Goal, scope, or key milestones...", color = Color(0xFF94A3B8)) },
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                    colors = translucentWhiteTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val entity = (initialProject ?: ProjectEntity(id = UUID.randomUUID().toString(), name = name.trim())).copy(
                        name = name.trim(),
                        description = description.trim(),
                        status = status,
                        updatedAt = System.currentTimeMillis()
                    )
                    onSave(entity)
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = RecallCyanSecondary)
            ) {
                Text("Save Project", color = Color(0xFF040711), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color(0xFF94A3B8)) }
        }
    )
}

// ============================================================================
// 3. COMMITMENTS TRACKER & PERSON COMMITMENTS SECTION
// ============================================================================

@Composable
fun CommitmentsTrackerContent(
    commitments: List<CommitmentEntity>,
    people: List<PersonEntity>,
    projects: List<ProjectEntity>,
    onToggleCompleted: (String, Boolean) -> Unit,
    onDeleteCommitment: (String) -> Unit,
    onAddCommitmentClick: () -> Unit,
    onScheduleReminderForCommitment: (CommitmentEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var filterMode by remember { mutableStateOf("OPEN") } // OPEN, BY_ME, TO_ME, COMPLETED
    val filtered = remember(commitments, filterMode) {
        when (filterMode) {
            "BY_ME" -> commitments.filter { !it.isCompleted && it.direction.equals("BY_ME", ignoreCase = true) }
            "TO_ME" -> commitments.filter { !it.isCompleted && it.direction.equals("TO_ME", ignoreCase = true) }
            "COMPLETED" -> commitments.filter { it.isCompleted }
            else -> commitments.filter { !it.isCompleted }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                "OPEN" to "Open (${commitments.count { !it.isCompleted }})",
                "BY_ME" to "Owed by Me",
                "TO_ME" to "Owed to Me",
                "COMPLETED" to "Fulfilled"
            ).forEach { (key, label) ->
                val isSelected = filterMode == key
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) Color(0xFFF59E0B).copy(alpha = 0.25f) else Color(0xFF0F172A),
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFFFBBF24) else Color(0xFF334155)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { filterMode = key }
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color(0xFFFBBF24) else Color(0xFFCBD5E1),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Handshake,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = Color(0xFFFBBF24)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No Commitments in View",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Promises, obligations, and debts extracted from debriefs or added manually appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onAddCommitmentClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF040711), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Commitment", color = Color(0xFF040711), fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                items(filtered, key = { it.id }) { item ->
                    val person = people.find { it.id == item.personId }
                    val project = projects.find { it.id == item.projectId }
                    val isByMe = item.direction.equals("BY_ME", ignoreCase = true)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (isByMe) Color(0xFFF59E0B).copy(alpha = 0.55f) else RecallCyanSecondary.copy(alpha = 0.55f),
                                RoundedCornerShape(14.dp)
                            )
                            .testTag("commitment_card_${item.id}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1120).copy(alpha = 0.78f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = item.isCompleted,
                                onCheckedChange = { onToggleCompleted(item.id, it) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isByMe) Color(0xFFF59E0B).copy(alpha = 0.22f) else RecallCyanSecondary.copy(alpha = 0.22f)
                                    ) {
                                        Text(
                                            text = if (isByMe) "🤝 OWED BY ME" else "🤝 OWED TO ME",
                                            color = if (isByMe) Color(0xFFFBBF24) else RecallCyanSecondary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    if (person != null) {
                                        Text(
                                            text = "👤 ${person.name}",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    if (project != null) {
                                        Text(
                                            text = "🚀 ${project.name}",
                                            color = RecallCyanSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (item.isCompleted) Color(0xFF64748B) else Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFBBF24).copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onScheduleReminderForCommitment(item) }
                            ) {
                                Text(
                                    text = "🔔 Remind",
                                    color = Color(0xFFFBBF24),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                            IconButton(
                                onClick = { onDeleteCommitment(item.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete commitment",
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
}

@Composable
fun PersonCommitmentsSection(
    person: PersonEntity,
    commitments: List<CommitmentEntity>,
    onToggleCompleted: (String, Boolean) -> Unit,
    onDeleteCommitment: (String) -> Unit,
    onAddCommitmentForPerson: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.85f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🤝 Commitments & Promises (${commitments.count { !it.isCompleted }} open)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFBBF24)
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onAddCommitmentForPerson() }
                ) {
                    Text(
                        text = "+ Add Promise",
                        color = Color(0xFFFBBF24),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (commitments.isEmpty()) {
                Text(
                    text = "No active promises or commitments recorded with ${person.name}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            } else {
                commitments.forEach { c ->
                    val isByMe = c.direction.equals("BY_ME", ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = c.isCompleted,
                            onCheckedChange = { onToggleCompleted(c.id, it) }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isByMe) "Owed by Me" else "Owed by ${person.name}",
                                color = if (isByMe) Color(0xFFFBBF24) else RecallCyanSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = c.description,
                                color = if (c.isCompleted) Color(0xFF64748B) else Color.White,
                                fontSize = 13.sp
                            )
                        }
                        IconButton(
                            onClick = { onDeleteCommitment(c.id) },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove commitment",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddCommitmentDialog(
    people: List<PersonEntity>,
    projects: List<ProjectEntity>,
    preselectedPersonId: String? = null,
    preselectedProjectId: String? = null,
    preselectedNoteId: String? = null,
    onDismiss: () -> Unit,
    onSave: (CommitmentEntity) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf("BY_ME") }
    var selectedPersonId by remember { mutableStateOf(preselectedPersonId) }
    var selectedProjectId by remember { mutableStateOf(preselectedProjectId) }
    var showPersonMenu by remember { mutableStateOf(false) }
    var showProjectMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        title = {
            Text("🤝 Record Commitment or Promise", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "BY_ME" to "🤝 Owed by Me",
                        "TO_ME" to "🤝 Owed to Me"
                    ).forEach { (dirKey, dirLabel) ->
                        val isSelected = direction == dirKey
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFFF59E0B).copy(alpha = 0.25f) else Color(0xFF1E293B),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFFFBBF24) else Color(0xFF334155)),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { direction = dirKey }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dirLabel,
                                    color = if (isSelected) Color(0xFFFBBF24) else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("What was promised or owed? (e.g. Send Q4 deck to Sarah by Friday)", color = Color(0xFF94A3B8)) },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                    colors = translucentWhiteTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        val selectedPersonName = people.find { it.id == selectedPersonId }?.name ?: "Link Person (Optional)"
                        OutlinedButton(
                            onClick = { showPersonMenu = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("👤 $selectedPersonName", color = Color.White, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        DropdownMenu(
                            expanded = showPersonMenu,
                            onDismissRequest = { showPersonMenu = false },
                            modifier = Modifier.background(Color(0xFF0F172A))
                        ) {
                            DropdownMenuItem(
                                text = { Text("None", color = Color(0xFF94A3B8)) },
                                onClick = {
                                    selectedPersonId = null
                                    showPersonMenu = false
                                }
                            )
                            people.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p.name, color = Color.White) },
                                    onClick = {
                                        selectedPersonId = p.id
                                        showPersonMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        val selectedProjectName = projects.find { it.id == selectedProjectId }?.name ?: "Link Project (Optional)"
                        OutlinedButton(
                            onClick = { showProjectMenu = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("🚀 $selectedProjectName", color = RecallCyanSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        DropdownMenu(
                            expanded = showProjectMenu,
                            onDismissRequest = { showProjectMenu = false },
                            modifier = Modifier.background(Color(0xFF0F172A))
                        ) {
                            DropdownMenuItem(
                                text = { Text("None", color = Color(0xFF94A3B8)) },
                                onClick = {
                                    selectedProjectId = null
                                    showProjectMenu = false
                                }
                            )
                            projects.forEach { proj ->
                                DropdownMenuItem(
                                    text = { Text(proj.name, color = Color.White) },
                                    onClick = {
                                        selectedProjectId = proj.id
                                        showProjectMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        CommitmentEntity(
                            id = UUID.randomUUID().toString(),
                            noteId = preselectedNoteId,
                            personId = selectedPersonId,
                            projectId = selectedProjectId,
                            description = description.trim(),
                            direction = direction,
                            isCompleted = false,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                },
                enabled = description.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
            ) {
                Text("Save Commitment", color = Color(0xFF040711), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color(0xFF94A3B8)) }
        }
    )
}
