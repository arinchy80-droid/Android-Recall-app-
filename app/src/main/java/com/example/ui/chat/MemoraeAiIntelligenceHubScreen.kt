package com.example.ui.chat

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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.PersonEntity
import com.example.data.model.NoteWithDetails
import com.example.data.model.SmartItemFilter
import com.example.ui.RecallViewModel
import com.example.ui.home.MemoraeCompactMemoryCard
import com.example.ui.navigation.Screen
import com.example.ui.theme.NeuralAppBackground
import com.example.ui.theme.RecallCyanSecondary
import com.example.ui.theme.RecallEmeraldAccent
import com.example.ui.theme.RecallVioletPrimary
import com.example.ui.theme.seamlessInnerTextFieldColors
import com.example.util.DateUtils
import com.example.util.LearnedPreferenceItem
import com.example.util.MemoryIntelligenceHelper
import com.example.util.RecallLifeOrganiserManager

private val AI_INTELLIGENCE_TABS = listOf(
    "Ask Recall" to "🤖",
    "Recommendations" to "✨",
    "Life Rewind" to "⏪",
    "Personal Insights" to "📊",
    "Relationships" to "👥",
    "Memory Search" to "🔍"
)

@Composable
fun MemoraeAiIntelligenceHubScreen(
    viewModel: RecallViewModel,
    onNavigateToNote: (String) -> Unit,
    onNavigateToPerson: (PersonEntity) -> Unit
) {
    val activeTab by viewModel.aiIntelligenceSubTab.collectAsStateWithLifecycle()
    val allNotes by viewModel.notes.collectAsStateWithLifecycle()
    val allPeople by viewModel.people.collectAsStateWithLifecycle()
    val allPlaces by viewModel.allPlaces.collectAsStateWithLifecycle()
    val allProjects by viewModel.allProjects.collectAsStateWithLifecycle()
    val allCommitments by viewModel.allCommitments.collectAsStateWithLifecycle()
    val allActions by viewModel.actionItems.collectAsStateWithLifecycle()
    val allMemoryLinks by viewModel.allMemoryLinks.collectAsStateWithLifecycle()
    val userSavedPreferences by viewModel.userSavedPreferences.collectAsStateWithLifecycle()

    NeuralAppBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top AI Intelligence Mode Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AI_INTELLIGENCE_TABS.forEach { (tabName, emoji) ->
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
                            .clickable { viewModel.aiIntelligenceSubTab.value = tabName }
                            .testTag("ai_tab_${tabName.lowercase().replace(" ", "_")}")
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
                    "Ask Recall" -> {
                        AskAiScreen(
                            viewModel = viewModel,
                            onNavigateToNote = onNavigateToNote
                        )
                    }

                    "Recommendations" -> {
                        ContextualRecommendationsSectionView(
                            viewModel = viewModel,
                            allNotes = allNotes,
                            allPeople = allPeople,
                            allProjects = allProjects,
                            allCommitments = allCommitments,
                            allActions = allActions,
                            onOpenNote = { noteId ->
                                allNotes.firstOrNull { it.note.id == noteId }?.let { viewModel.openNoteEditor(it) }
                            },
                            onOpenPerson = onNavigateToPerson
                        )
                    }

                    "Life Rewind" -> {
                        LifeRewindSectionView(
                            allNotes = allNotes,
                            allPeople = allPeople,
                            allPlaces = allPlaces,
                            allCommitments = allCommitments,
                            onOpenNote = { viewModel.openNoteEditor(it) },
                            onOpenPerson = onNavigateToPerson,
                            onAskAiDeepDive = { query ->
                                viewModel.sendChatMessage(query)
                                viewModel.aiIntelligenceSubTab.value = "Ask Recall"
                            }
                        )
                    }

                    "Personal Insights" -> {
                        PersonalInsightsAndPreferencesView(
                            viewModel = viewModel,
                            allNotes = allNotes,
                            allPeople = allPeople,
                            allPlaces = allPlaces,
                            allProjects = allProjects,
                            allCommitments = allCommitments,
                            allActions = allActions,
                            allMemoryLinks = allMemoryLinks,
                            userSavedPreferences = userSavedPreferences,
                            onOpenNote = { viewModel.openNoteEditor(it) }
                        )
                    }

                    "Relationships" -> {
                        RelationshipIntelligenceView(
                            viewModel = viewModel,
                            allPeople = allPeople,
                            allNotes = allNotes,
                            allCommitments = allCommitments,
                            allActions = allActions,
                            allProjects = allProjects,
                            onOpenPerson = onNavigateToPerson,
                            onStartCatchUp = { viewModel.startCatchUpNoteForPerson(it) }
                        )
                    }

                    "Memory Search" -> {
                        AdvancedHybridSearchSectionView(
                            viewModel = viewModel,
                            onOpenNote = { viewModel.openNoteEditor(it) },
                            onOpenPerson = onNavigateToPerson
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LifeRewindSectionView(
    allNotes: List<NoteWithDetails>,
    allPeople: List<PersonEntity>,
    allPlaces: List<com.example.data.local.entity.PlaceEntity>,
    allCommitments: List<com.example.data.local.entity.CommitmentEntity>,
    onOpenNote: (NoteWithDetails) -> Unit,
    onOpenPerson: (PersonEntity) -> Unit,
    onAskAiDeepDive: (String) -> Unit
) {
    var rewindQuery by remember { mutableStateOf("What was I doing this week?") }

    val rewindResult = remember(rewindQuery, allNotes, allPeople, allPlaces, allCommitments) {
        RecallLifeOrganiserManager.executeLifeRewindQuery(
            query = rewindQuery,
            allNotes = allNotes,
            allPeople = allPeople,
            allPlaces = allPlaces,
            allCommitments = allCommitments
        )
    }

    val presetQuestions = remember(allPeople, allPlaces) {
        buildList {
            add("What was I doing today?")
            add("What was I doing this week?")
            add("What decisions or ideas did I record this month?")
            allPeople.firstOrNull()?.let { p ->
                add("When did I last speak to ${p.name}?")
            }
            allPlaces.firstOrNull()?.let { pl ->
                add("What happened during my visit to ${pl.name}?")
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
                border = BorderStroke(
                    1.2.dp,
                    Brush.horizontalGradient(listOf(RecallCyanSecondary, RecallVioletPrimary))
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "⏪ Life Rewind — Chronological Memory Retrieval",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Revisit past conversations, days, people, and locations backed by original source records and dates.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = rewindQuery,
                        onValueChange = { rewindQuery = it },
                        placeholder = {
                            Text("e.g., What was I doing last Tuesday?", fontSize = 13.sp, color = Color(0xFF94A3B8))
                        },
                        leadingIcon = {
                            Icon(Icons.Default.History, contentDescription = null, tint = RecallCyanSecondary)
                        },
                        singleLine = true,
                        colors = seamlessInnerTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E293B))
                            .testTag("life_rewind_query_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetQuestions.forEach { preset ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (rewindQuery == preset) RecallVioletPrimary else Color(0xFF1E293B),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier.clickable { rewindQuery = preset }
                            ) {
                                Text(
                                    text = preset,
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Evidence-based Rewind Summary Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.90f),
                border = BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.65f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📌 Evidence Summary (${rewindResult.timeframeDescription})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = RecallCyanSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { onAskAiDeepDive(rewindQuery) }) {
                            Text("AI Synthesis →", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        text = rewindResult.headlineSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }
        }

        if (rewindResult.matchedNotes.isNotEmpty()) {
            item {
                Text(
                    text = "Original Source Memories (${rewindResult.matchedNotes.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
            items(rewindResult.matchedNotes, key = { "rewind_${it.note.id}" }) { item ->
                MemoraeCompactMemoryCard(
                    noteWithDetails = item,
                    onClick = { onOpenNote(item) },
                    onPersonClick = onOpenPerson
                )
            }
        }
    }
}

@Composable
private fun PersonalInsightsAndPreferencesView(
    viewModel: RecallViewModel,
    allNotes: List<NoteWithDetails>,
    allPeople: List<PersonEntity>,
    allPlaces: List<com.example.data.local.entity.PlaceEntity>,
    allProjects: List<com.example.data.local.entity.ProjectEntity>,
    allCommitments: List<com.example.data.local.entity.CommitmentEntity>,
    allActions: List<com.example.data.local.entity.ActionItemEntity>,
    allMemoryLinks: List<com.example.data.local.entity.MemoryLinkEntity>,
    userSavedPreferences: List<LearnedPreferenceItem>,
    onOpenNote: (NoteWithDetails) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val weeklyDigest = remember(allNotes, allPeople, allActions) {
        MemoryIntelligenceHelper.buildWeeklyIntelligenceDigest(
            allNotes = allNotes,
            allPeople = allPeople,
            allActions = allActions
        )
    }
    val weeklyBriefing = remember(allNotes, allPeople, allCommitments, allActions, allProjects) {
        RecallLifeOrganiserManager.buildWeeklyLifeBriefing(
            allNotes = allNotes,
            allPeople = allPeople,
            allCommitments = allCommitments,
            allActions = allActions,
            allProjects = allProjects
        )
    }
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
    val learnedPreferences = remember(userSavedPreferences, allNotes, allPlaces, allProjects) {
        RecallLifeOrganiserManager.synthesizeLearnedPreferences(
            userSaved = userSavedPreferences,
            allNotes = allNotes,
            allPlaces = allPlaces,
            allProjects = allProjects
        )
    }

    var showAddPreferenceDialog by remember { mutableStateOf(false) }
    var newPrefCategory by remember { mutableStateOf("Communication Style") }
    var newPrefStatement by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 104.dp)
    ) {
        // 1. Phase 5 & Phase 10: Weekly & Daily AI Intelligence Briefing
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.88f)),
                border = BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.65f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("weekly_intelligence_briefing_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "📊 Weekly AI Life Briefing",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = RecallCyanSecondary.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, RecallCyanSecondary),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    com.example.util.AppIntegrationsHelper.shareNoteAsMarkdown(
                                        context = context,
                                        title = "Recall Weekly AI Life Briefing",
                                        content = weeklyBriefing.markdownExport,
                                        category = "Weekly Briefing",
                                        tags = listOf("weekly-review", "intelligence"),
                                        peopleNames = weeklyBriefing.topPeopleInteracted
                                    )
                                }
                        ) {
                            Text(
                                text = "📤 Share Review",
                                color = RecallCyanSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = weeklyDigest.executiveTakeaway,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    weeklyBriefing.executiveHighlights.forEach { bullet ->
                        Text(
                            text = "• $bullet",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF93C5FD),
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        InsightMetricColumn("This Week", "${weeklyDigest.notesThisWeek} notes")
                        InsightMetricColumn("Promises Done", "${report.commitmentFulfillmentPercent}%")
                        InsightMetricColumn("Tasks Done", "${weeklyDigest.completedTasksCount}")
                        InsightMetricColumn("Active Projects", "${report.activeProjectsCount}")
                    }
                }
            }
        }

        // 2. Phase 8: Personal Preference & Behavioural Intelligence (Review, Confirm, Edit, Delete)
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
                                text = "🧬 Personal Preferences & Patterns",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "Observed patterns are clearly distinguished from confirmed preferences. You can verify or remove any entry.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { showAddPreferenceDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = RecallVioletPrimary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (learnedPreferences.isEmpty()) {
                        Text(
                            text = "No preferences or recurring patterns recorded yet. Add your preferred communication style, routines, or goals above.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1)
                        )
                    } else {
                        learnedPreferences.forEach { pref ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1E293B).copy(alpha = 0.85f),
                                border = BorderStroke(
                                    1.dp,
                                    if (pref.isUserVerified) RecallEmeraldAccent.copy(alpha = 0.6f)
                                    else Color(0xFFF59E0B).copy(alpha = 0.55f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = pref.category,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = RecallCyanSecondary,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (pref.isUserVerified) "✓ Confirmed Preference" else "👁️ Observed Pattern",
                                                fontSize = 10.sp,
                                                color = if (pref.isUserVerified) RecallEmeraldAccent else Color(0xFFFDE68A),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = pref.statement,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = pref.evidenceSummary,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                    if (!pref.isUserVerified) {
                                        IconButton(
                                            onClick = { viewModel.saveOrVerifyLearnedPreference(pref) },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Confirm Preference",
                                                tint = RecallEmeraldAccent,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteOrDismissLearnedPreference(pref) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Preference",
                                            tint = Color(0xFFEF4444),
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

        // 3. Discovered Behavioural Patterns
        if (report.discoveredPatterns.isNotEmpty()) {
            item {
                Text(
                    text = "🔍 Observed Life & Project Patterns",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
            items(report.discoveredPatterns, key = { it.id }) { pat ->
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
                        Text(text = pat.icon, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${pat.categoryLabel} • Observed Pattern",
                                style = MaterialTheme.typography.labelSmall,
                                color = RecallCyanSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = pat.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = pat.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddPreferenceDialog) {
        val categories = listOf("Communication Style", "Personal Goal", "Recurring Routine", "Interest", "Preferred Place")
        AlertDialog(
            onDismissRequest = { showAddPreferenceDialog = false },
            containerColor = Color(0xFF0F172A),
            title = {
                Text("Add Personal Preference or Goal", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (newPrefCategory == cat) RecallVioletPrimary else Color(0xFF1E293B),
                                modifier = Modifier.clickable { newPrefCategory = cat }
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = newPrefStatement,
                        onValueChange = { newPrefStatement = it },
                        placeholder = {
                            Text("e.g., Prefer concise morning briefings and bulleted action lists", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        },
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
                        if (newPrefStatement.isNotBlank()) {
                            viewModel.saveOrVerifyLearnedPreference(
                                LearnedPreferenceItem(
                                    category = newPrefCategory,
                                    statement = newPrefStatement.trim(),
                                    evidenceSummary = "Explicitly added by user",
                                    isUserVerified = true
                                )
                            )
                            newPrefStatement = ""
                            showAddPreferenceDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RecallEmeraldAccent)
                ) {
                    Text("Save Preference", color = Color(0xFF050811), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPreferenceDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}

@Composable
private fun InsightMetricColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = RecallCyanSecondary)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8), fontSize = 10.sp)
    }
}

@Composable
private fun ContextualRecommendationsSectionView(
    viewModel: RecallViewModel,
    allNotes: List<NoteWithDetails>,
    allPeople: List<PersonEntity>,
    allProjects: List<com.example.data.local.entity.ProjectEntity>,
    allCommitments: List<com.example.data.local.entity.CommitmentEntity>,
    allActions: List<com.example.data.local.entity.ActionItemEntity>,
    onOpenNote: (String) -> Unit,
    onOpenPerson: (PersonEntity) -> Unit
) {
    val recommendations = remember(allNotes, allPeople, allProjects, allCommitments, allActions) {
        RecallLifeOrganiserManager.buildContextualRecommendations(
            allNotes = allNotes,
            allPeople = allPeople,
            allProjects = allProjects,
            allCommitments = allCommitments,
            allActions = allActions
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
                border = BorderStroke(
                    1.2.dp,
                    Brush.horizontalGradient(listOf(RecallVioletPrimary, RecallCyanSecondary))
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "✨ Contextual Recommendations Engine",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Proactive, evidence-backed next actions synthesized across your open promises, relationships, active projects, and high-value ideas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }
        }

        if (recommendations.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "All caught up! As you record conversations, promises, and projects, Recall will surface contextual recommendations here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(recommendations, key = { it.id }) { rec ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.88f)),
                    border = BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("contextual_rec_${rec.id}")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = rec.categoryLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = RecallCyanSecondary,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.weight(1f)
                            )
                            rec.targetNoteId?.let { nId ->
                                Text(
                                    text = "Source Memory →",
                                    fontSize = 10.sp,
                                    color = Color(0xFF93C5FD),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { onOpenNote(nId) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = rec.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = rec.reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1)
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    when {
                                        rec.targetCommitmentId != null -> {
                                            val comm = allCommitments.firstOrNull { it.id == rec.targetCommitmentId }
                                            if (comm != null) {
                                                val targetTime = System.currentTimeMillis() + 24L * 3600L * 1000L
                                                viewModel.assignCommitmentDueDate(comm, targetTime)
                                                viewModel.scheduleReminder(
                                                    title = "Promise: ${comm.description}",
                                                    details = rec.reason,
                                                    noteId = comm.noteId,
                                                    timeMillis = targetTime
                                                )
                                            }
                                        }
                                        rec.targetPersonId != null -> {
                                            allPeople.firstOrNull { it.id == rec.targetPersonId }?.let { person ->
                                                viewModel.startCatchUpNoteForPerson(person)
                                            }
                                        }
                                        rec.targetProjectId != null -> {
                                            allProjects.firstOrNull { it.id == rec.targetProjectId }?.let { proj ->
                                                viewModel.createRichActionItem(
                                                    task = "Next milestone for ${proj.name}",
                                                    priority = com.example.util.TaskPriority.HIGH,
                                                    column = com.example.util.TaskBoardColumn.TODAY
                                                )
                                            }
                                        }
                                        rec.targetNoteId != null -> {
                                            onOpenNote(rec.targetNoteId)
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RecallEmeraldAccent),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text(
                                    text = rec.primaryActionLabel,
                                    color = Color(0xFF050811),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            rec.targetPersonId?.let { pId ->
                                allPeople.firstOrNull { it.id == pId }?.let { person ->
                                    TextButton(onClick = { onOpenPerson(person) }) {
                                        Text("Open Dossier →", color = RecallCyanSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
private fun RelationshipIntelligenceView(
    viewModel: RecallViewModel,
    allPeople: List<PersonEntity>,
    allNotes: List<NoteWithDetails>,
    allCommitments: List<com.example.data.local.entity.CommitmentEntity>,
    allActions: List<com.example.data.local.entity.ActionItemEntity>,
    allProjects: List<com.example.data.local.entity.ProjectEntity>,
    onOpenPerson: (PersonEntity) -> Unit,
    onStartCatchUp: (PersonEntity) -> Unit
) {
    val converters = remember { com.example.data.local.converter.RoomConverters() }

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
                    Text(
                        text = "👥 Relationship Intelligence & Follow-Up Cadence",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Tracks interaction frequency, shared commitments, custom follow-up cadence (7d / 14d / 30d / 90d), and 5-second talking points.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }
        }

        if (allPeople.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No contacts in your relationship graph yet. Mention people in notes or add them in Memories → People.",
                        color = Color(0xFF94A3B8),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(allPeople, key = { "rel_intel_${it.id}" }) { person ->
                val pNotes = remember(person, allNotes) {
                    allNotes.filter { n -> n.people.any { it.id == person.id } }
                }
                val prep = remember(person, pNotes, allActions, allCommitments, allProjects) {
                    MemoryIntelligenceHelper.generateMeetingPrepBriefing(
                        person = person,
                        personNotes = pNotes,
                        allActions = allActions,
                        allCommitments = allCommitments,
                        allProjects = allProjects
                    )
                }
                val isOverdue = person.isOverdueForCatchUp()
                val currentCadenceDays = person.getCatchUpCadenceDays()

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.88f)),
                    border = BorderStroke(
                        1.dp,
                        if (isOverdue) Color(0xFFF59E0B) else RecallCyanSecondary.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenPerson(person) }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${person.name} (${prep.relationship})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = prep.cadenceStatus,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isOverdue) Color(0xFFF59E0B) else RecallCyanSecondary
                                )
                            }
                            Button(
                                onClick = { onStartCatchUp(person) },
                                colors = ButtonDefaults.buttonColors(containerColor = RecallVioletPrimary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Log Catch-Up", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Interactive Follow-Up Cadence Selector + Quick Reminder
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Cadence:",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                            listOf(7 to "7d", 14 to "14d", 30 to "30d", 90 to "90d").forEach { (days, label) ->
                                val isSel = currentCadenceDays == days
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) RecallCyanSecondary else Color(0xFF1E293B),
                                    border = BorderStroke(1.dp, if (isSel) RecallCyanSecondary else Color(0xFF334155)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            val updatedMap = converters.toStringMap(person.keyDatesJson).toMutableMap()
                                            updatedMap["CatchUpCadenceDays"] = days.toString()
                                            viewModel.savePerson(
                                                person.copy(keyDatesJson = converters.fromStringMap(updatedMap))
                                            )
                                            viewModel.showFeedback("Updated ${person.name}'s follow-up cadence to every $days days.")
                                        }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        color = if (isSel) Color(0xFF050811) else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        val tomorrow = System.currentTimeMillis() + 24L * 3600L * 1000L
                                        viewModel.scheduleReminder(
                                            title = "Catch up with ${person.name}",
                                            details = prep.talkingPoints.firstOrNull() ?: "Scheduled from Relationship Intelligence",
                                            timeMillis = tomorrow
                                        )
                                    }
                            ) {
                                Text(
                                    text = "🔔 Remind",
                                    fontSize = 10.sp,
                                    color = Color(0xFFFDE68A),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        if (prep.talkingPoints.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            prep.talkingPoints.take(2).forEach { pt ->
                                Text(
                                    text = "• $pt",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCBD5E1),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
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
private fun AdvancedHybridSearchSectionView(
    viewModel: RecallViewModel,
    onOpenNote: (NoteWithDetails) -> Unit,
    onOpenPerson: (PersonEntity) -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val smartResults by viewModel.filteredSmartSearchResults.collectAsStateWithLifecycle()
    val smartFilter by viewModel.smartSearchFilter.collectAsStateWithLifecycle()
    val interpretation by viewModel.smartSearchInterpretation.collectAsStateWithLifecycle()
    val allNotes by viewModel.notes.collectAsStateWithLifecycle()

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
                        text = "🔍 Hybrid Semantic, Entity & Graph Search",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Search across keywords, people, places, dates, commitments, and 1-hop knowledge graph links.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        placeholder = {
                            Text("e.g., Find everything involving Sarah and Project Alpha...", fontSize = 13.sp, color = Color(0xFF94A3B8))
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = RecallCyanSecondary)
                        },
                        singleLine = true,
                        colors = seamlessInnerTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E293B))
                            .testTag("ai_hybrid_search_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SmartItemFilter.entries.forEach { filter ->
                            val isSel = smartFilter == filter
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) RecallVioletPrimary else Color(0xFF1E293B),
                                modifier = Modifier.clickable { viewModel.setSmartSearchFilter(filter) }
                            ) {
                                Text(
                                    text = filter.name,
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        interpretation?.let { nlu ->
            if (searchQuery.isNotBlank()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.90f),
                        border = BorderStroke(1.dp, RecallVioletPrimary.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "🧠 Query Intent: ${nlu.intent} • ${smartResults.size} Evidence Match(es)",
                                style = MaterialTheme.typography.labelMedium,
                                color = RecallCyanSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            if (!nlu.executiveSummary.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = nlu.executiveSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        if (searchQuery.isNotBlank() && smartResults.isNotEmpty()) {
            items(smartResults, key = { it.id }) { item ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            item.noteWithDetails?.let { onOpenNote(it) }
                                ?: item.personEntity?.let { onOpenPerson(it) }
                        }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "[${item.itemType.name}] ${item.title}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${item.matchScore.coerceIn(50, 99)}% match",
                                style = MaterialTheme.typography.labelSmall,
                                color = RecallEmeraldAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (item.subtitle.isNotBlank()) {
                            Text(text = item.subtitle, style = MaterialTheme.typography.labelSmall, color = RecallCyanSecondary)
                        }
                        if (item.snippet.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.snippet,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        } else {
            items(allNotes.take(10), key = { "search_default_${it.note.id}" }) { noteWithDetails ->
                MemoraeCompactMemoryCard(
                    noteWithDetails = noteWithDetails,
                    onClick = { onOpenNote(noteWithDetails) },
                    onPersonClick = onOpenPerson
                )
            }
        }
    }
}
