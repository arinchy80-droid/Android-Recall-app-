package com.example.ui.stream

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import java.io.File
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ActionItemEntity
import com.example.data.local.entity.MediaEntity
import com.example.data.local.entity.PersonEntity
import com.example.data.model.NoteWithDetails
import com.example.data.model.SmartItemFilter
import com.example.data.model.SmartItemType
import com.example.data.model.SmartSearchItem
import com.example.data.model.SmartSearchNluInterpretation
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.ui.theme.CategoryMeeting
import com.example.ui.theme.CategoryPersonal
import com.example.ui.theme.CategoryWork
import com.example.ui.theme.NeonLightbulbIcon
import com.example.ui.theme.RecallCyanSecondary
import com.example.ui.theme.RecallVioletPrimary
import com.example.ui.theme.VibrantTagBadge
import com.example.ui.theme.getVibrantTagColor

@Composable
fun SmartSearchSuggestionsRow(
    onSelectSuggestion: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val suggestedQueries = listOf(
        "What commitments do I have open?",
        "Notes from my meetings last week",
        "Active projects and deliverables",
        "Photos and camera memories"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            NeonLightbulbIcon(size = 18.dp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Natural Language Search Suggestions",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
        ) {
            items(suggestedQueries) { query ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF0B1120).copy(alpha = 0.65f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Brush.horizontalGradient(
                            listOf(
                                RecallCyanSecondary.copy(alpha = 0.55f),
                                RecallVioletPrimary.copy(alpha = 0.55f)
                            )
                        )
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onSelectSuggestion(query) }
                        .testTag("suggestion_chip_${query.take(15)}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = RecallCyanSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = query,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFF1F5F9),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmartSearchInterpretationCard(
    interpretation: SmartSearchNluInterpretation,
    isLoading: Boolean,
    aiSummary: String?,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(RecallVioletPrimary.copy(alpha = 0.6f), RecallCyanSecondary.copy(alpha = 0.6f))
            )
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("smart_search_nlu_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Sparkle + "Smart Search NLU"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = RecallVioletPrimary.copy(alpha = 0.15f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = RecallVioletPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Natural Language Understanding",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                if (isLoading) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp),
                        color = RecallCyanSecondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Analyzing...",
                        style = MaterialTheme.typography.labelSmall,
                        color = RecallCyanSecondary
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = RecallCyanSecondary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Interpreted",
                            style = MaterialTheme.typography.labelSmall,
                            color = RecallCyanSecondary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Explanation
            if (interpretation.explanation.isNotBlank()) {
                Text(
                    text = interpretation.explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Extracted Entities, Time, Categories & Keywords Chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // People
                interpretation.entities.forEach { entity ->
                    NluTagChip(
                        icon = Icons.Default.Person,
                        label = entity,
                        tint = RecallVioletPrimary
                    )
                }
                // Places
                interpretation.places.forEach { placeName ->
                    NluTagChip(
                        icon = Icons.Default.Tag,
                        label = "📍 $placeName",
                        tint = Color(0xFF10B981)
                    )
                }
                // Projects
                interpretation.projects.forEach { projName ->
                    NluTagChip(
                        icon = Icons.Default.Tag,
                        label = "🚀 $projName",
                        tint = Color(0xFF60A5FA)
                    )
                }
                // Graph Traversal Hops
                if (interpretation.graphTraversalHops > 0) {
                    NluTagChip(
                        icon = Icons.Default.AutoAwesome,
                        label = "🕸️ ${interpretation.graphTraversalHops} Graph Hop(s)",
                        tint = Color(0xFFF43F5E)
                    )
                }
                // Time Range
                interpretation.timeRangeDescription?.let { timeDesc ->
                    NluTagChip(
                        icon = Icons.Default.CalendarToday,
                        label = timeDesc,
                        tint = RecallCyanSecondary
                    )
                }
                // Categories
                interpretation.categories.forEach { cat ->
                    NluTagChip(
                        icon = Icons.Default.Tag,
                        label = cat,
                        tint = Color(0xFFF59E0B)
                    )
                }
                // Keywords
                interpretation.keywords.take(3).forEach { kw ->
                    NluTagChip(
                        icon = Icons.Default.Search,
                        label = kw,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // AI Grounded Executive Answer Box
            if (!aiSummary.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = RecallVioletPrimary.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        RecallVioletPrimary.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("smart_search_ai_summary")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = RecallCyanSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Executive AI Answer",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = RecallCyanSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = aiSummary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NluTagChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = tint.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, tint.copy(alpha = 0.3f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = tint,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun SmartSearchFilterTabs(
    selectedFilter: SmartItemFilter,
    items: List<SmartSearchItem>,
    onSelectFilter: (SmartItemFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val counts = mapOf(
        SmartItemFilter.ALL to items.size,
        SmartItemFilter.NOTES to items.count { it.itemType == SmartItemType.NOTE },
        SmartItemFilter.PHOTOS to items.count { it.itemType == SmartItemType.PHOTO },
        SmartItemFilter.PEOPLE to items.count { it.itemType == SmartItemType.PERSON },
        SmartItemFilter.PLACES to items.count { it.itemType == SmartItemType.PLACE },
        SmartItemFilter.PROJECTS to items.count { it.itemType == SmartItemType.PROJECT },
        SmartItemFilter.COMMITMENTS to items.count { it.itemType == SmartItemType.COMMITMENT },
        SmartItemFilter.TASKS to items.count { it.itemType == SmartItemType.ACTION }
    )
    var dropdownExpanded by remember { mutableStateOf(false) }
    val activeCount = counts[selectedFilter] ?: 0

    Box(modifier = modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F172A).copy(alpha = 0.90f),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, RecallVioletPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { dropdownExpanded = true }
                .testTag("smart_search_filter_dropdown")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Tag,
                    contentDescription = null,
                    tint = RecallVioletPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Filter Results: ",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${selectedFilter.label} ($activeCount)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select Result Filter",
                    tint = Color.White
                )
            }
        }

        DropdownMenu(
            expanded = dropdownExpanded,
            onDismissRequest = { dropdownExpanded = false },
            modifier = Modifier
                .background(Color(0xFF0F172A))
                .border(androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
        ) {
            SmartItemFilter.values().forEach { filter ->
                val count = counts[filter] ?: 0
                val isSelected = selectedFilter == filter
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "${filter.label} ($count)",
                            color = if (isSelected) RecallVioletPrimary else Color.White,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                        )
                    },
                    leadingIcon = {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = RecallVioletPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    },
                    onClick = {
                        onSelectFilter(filter)
                        dropdownExpanded = false
                    },
                    modifier = Modifier.testTag("filter_tab_${filter.name}")
                )
            }
        }
    }
}

@Composable
fun SmartSearchResultCard(
    item: SmartSearchItem,
    onClickNote: (NoteWithDetails) -> Unit,
    onClickPhoto: (MediaEntity) -> Unit,
    onClickPerson: (PersonEntity) -> Unit,
    onToggleAction: (ActionItemEntity) -> Unit,
    onToggleCommitment: (com.example.data.local.entity.CommitmentEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    when (item.itemType) {
        SmartItemType.NOTE -> {
            item.noteWithDetails?.let { noteDetails ->
                SmartNoteResultCard(
                    item = item,
                    noteWithDetails = noteDetails,
                    onClick = { onClickNote(noteDetails) },
                    modifier = modifier
                )
            }
        }
        SmartItemType.PHOTO -> {
            item.mediaEntity?.let { media ->
                SmartPhotoResultCard(
                    item = item,
                    media = media,
                    onClick = { onClickPhoto(media) },
                    modifier = modifier
                )
            }
        }
        SmartItemType.PERSON -> {
            item.personEntity?.let { person ->
                SmartPersonResultCard(
                    item = item,
                    person = person,
                    onClick = { onClickPerson(person) },
                    modifier = modifier
                )
            }
        }
        SmartItemType.PLACE -> {
            item.placeEntity?.let { place ->
                SmartPlaceResultCard(
                    item = item,
                    place = place,
                    modifier = modifier
                )
            }
        }
        SmartItemType.PROJECT -> {
            item.projectEntity?.let { project ->
                SmartProjectResultCard(
                    item = item,
                    project = project,
                    modifier = modifier
                )
            }
        }
        SmartItemType.COMMITMENT -> {
            item.commitmentEntity?.let { commitment ->
                SmartCommitmentResultCard(
                    item = item,
                    commitment = commitment,
                    onToggle = { onToggleCommitment(commitment) },
                    modifier = modifier
                )
            }
        }
        SmartItemType.ACTION -> {
            item.actionEntity?.let { action ->
                SmartActionResultCard(
                    item = item,
                    action = action,
                    onToggle = { onToggleAction(action) },
                    modifier = modifier
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmartNoteResultCard(
    item: SmartSearchItem,
    noteWithDetails: NoteWithDetails,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val note = noteWithDetails.note
    val categoryColor = getVibrantTagColor(note.category)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.85f),
        tonalElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            categoryColor.copy(alpha = 0.45f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("smart_result_note_${note.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Category Pill + Match Score Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                VibrantTagBadge(
                    text = note.category,
                    color = categoryColor
                )

                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Score pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.15f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${item.matchScore}% Match",
                            color = Color(0xFF10B981),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title - High contrast bright white
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Snippet - Luminous silver
            Text(
                text = item.snippet,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFCBD5E1),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // Highlights
            if (item.highlights.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item.highlights.forEach { h ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "• $h",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmartPhotoResultCard(
    item: SmartSearchItem,
    media: MediaEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val photoFile = remember(media.localUri) { if (media.localUri.isNotBlank()) File(media.localUri) else null }
    val photoFileExists = photoFile != null && photoFile.exists()

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.85f),
        tonalElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            RecallCyanSecondary.copy(alpha = 0.4f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("smart_result_photo_${media.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top row: Photo badge + score
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = RecallCyanSecondary.copy(alpha = 0.15f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = RecallCyanSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Photo Memory",
                            color = RecallCyanSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.weight(1f))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${item.matchScore}% Match",
                        color = Color(0xFF10B981),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Visual Memory Banner Box (Actual Photo or Gradient)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center
            ) {
                if (photoFile != null && photoFile.exists()) {
                    AsyncImage(
                        model = photoFile,
                        contentDescription = "Photo Memory",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        RecallVioletPrimary.copy(alpha = 0.75f),
                                        RecallCyanSecondary.copy(alpha = 0.85f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Visual Memory • Captured Asset",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // AI Description - High contrast white
            Text(
                text = item.snippet,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )

            // Labels / highlights
            if (item.highlights.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item.highlights.forEach { h ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = RecallCyanSecondary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "• $h",
                                style = MaterialTheme.typography.labelSmall,
                                color = RecallCyanSecondary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SmartPersonResultCard(
    item: SmartSearchItem,
    person: PersonEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            RecallVioletPrimary.copy(alpha = 0.35f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("smart_result_person_${person.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = RecallVioletPrimary.copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val initials = person.name.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("")
                    Text(
                        text = initials,
                        color = RecallVioletPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = person.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = person.relationship ?: "Contact in CRM",
                    style = MaterialTheme.typography.bodySmall,
                    color = RecallVioletPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.snippet,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF10B981).copy(alpha = 0.15f)
            ) {
                Text(
                    text = "${item.matchScore}%",
                    color = Color(0xFF10B981),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun SmartActionResultCard(
    item: SmartSearchItem,
    action: ActionItemEntity,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("smart_result_action_${action.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (action.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Toggle completion",
                    tint = if (action.isCompleted) Color(0xFF10B981) else MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = action.task,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF10B981).copy(alpha = 0.15f)
            ) {
                Text(
                    text = "${item.matchScore}%",
                    color = Color(0xFF10B981),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun SmartPlaceResultCard(
    item: SmartSearchItem,
    place: com.example.data.local.entity.PlaceEntity,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.88f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.45f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("smart_result_place_${place.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "📍 ${place.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.18f)
                ) {
                    Text(
                        text = "${item.matchScore}% Match",
                        color = Color(0xFF10B981),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF34D399)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.snippet,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFCBD5E1)
            )
        }
    }
}

@Composable
fun SmartProjectResultCard(
    item: SmartSearchItem,
    project: com.example.data.local.entity.ProjectEntity,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.88f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF60A5FA).copy(alpha = 0.45f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("smart_result_project_${project.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🚀 ${project.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF60A5FA).copy(alpha = 0.18f)
                ) {
                    Text(
                        text = "${item.matchScore}% Match",
                        color = Color(0xFF60A5FA),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF93C5FD)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.snippet,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFCBD5E1)
            )
        }
    }
}

@Composable
fun SmartCommitmentResultCard(
    item: SmartSearchItem,
    commitment: com.example.data.local.entity.CommitmentEntity,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = if (commitment.direction == "TO_ME") RecallCyanSecondary else Color(0xFFF59E0B)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.88f),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("smart_result_commitment_${commitment.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (commitment.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Toggle commitment",
                    tint = if (commitment.isCompleted) Color(0xFF10B981) else accent
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = commitment.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF10B981).copy(alpha = 0.15f)
            ) {
                Text(
                    text = "${item.matchScore}%",
                    color = Color(0xFF10B981),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

