package com.example.util

import android.content.Context
import android.location.Geocoder
import android.location.LocationManager
import com.example.data.local.converter.RoomConverters
import com.example.data.local.entity.ActionItemEntity
import com.example.data.local.entity.CommitmentEntity
import com.example.data.local.entity.MemoryLinkEntity
import com.example.data.local.entity.PersonEntity
import com.example.data.local.entity.PlaceEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.model.NoteWithDetails
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class LinkedMemoryMatch(
    val noteWithDetails: NoteWithDetails,
    val similarityPercent: Int,
    val matchReason: String
)

data class TopicCluster(
    val topicName: String,
    val badgeIcon: String,
    val noteCount: Int,
    val relatedPeople: List<String>,
    val notes: List<NoteWithDetails>
)

data class LocationCluster(
    val locationName: String,
    val notes: List<NoteWithDetails>,
    val peopleCount: Int
)

data class HabitItem(
    val id: String,
    val title: String,
    val icon: String,
    val completedDateKeys: Set<String>
) {
    fun isCompletedToday(todayKey: String = MemoryIntelligenceHelper.getTodayDateKey()): Boolean {
        return completedDateKeys.contains(todayKey)
    }

    fun currentStreak(): Int {
        var streak = 0
        val cal = Calendar.getInstance()
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        for (i in 0..30) {
            val key = fmt.format(cal.time)
            if (completedDateKeys.contains(key)) {
                streak++
            } else if (i > 0) {
                break
            }
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }
}

data class WeeklyIntelligenceDigest(
    val notesThisWeek: Int,
    val audioNotesThisWeek: Int,
    val visualNotesThisWeek: Int,
    val completedTasksCount: Int,
    val pendingTasksCount: Int,
    val topTags: List<Pair<String, Int>>,
    val activePeople: List<String>,
    val overdueCatchUpPeople: List<PersonEntity>,
    val energyAndSentimentLabel: String,
    val executiveTakeaway: String
)

data class MeetingPrepBriefing(
    val person: PersonEntity,
    val relationship: String,
    val cadenceStatus: String,
    val keyFacts: List<String>,
    val keyDates: Map<String, String>,
    val recentSharedNotes: List<NoteWithDetails>,
    val openActionItems: List<ActionItemEntity>,
    val talkingPoints: List<String>,
    val openCommitments: List<CommitmentEntity> = emptyList(),
    val sharedProjects: List<ProjectEntity> = emptyList()
)

enum class ProactiveInsightType {
    OPEN_COMMITMENT,
    UPCOMING_DATE,
    DORMANT_RELATIONSHIP,
    PROJECT_MOMENTUM,
    PENDING_TASK
}

data class ProactiveInsightItem(
    val id: String,
    val type: ProactiveInsightType,
    val icon: String,
    val title: String,
    val subtitle: String,
    val actionLabel: String,
    val priorityScore: Int,
    val person: PersonEntity? = null,
    val commitment: CommitmentEntity? = null,
    val project: ProjectEntity? = null,
    val noteWithDetails: NoteWithDetails? = null,
    val actionItem: ActionItemEntity? = null,
    val dateLabel: String? = null,
    val dateValue: String? = null
) {
    val badgeIcon: String get() = icon
    val noteId: String? get() = noteWithDetails?.note?.id ?: commitment?.noteId ?: actionItem?.noteId
}

data class AnticipatoryDailyBriefing(
    val greetingTitle: String,
    val executiveSummary: String,
    val openCommitmentsByMeCount: Int,
    val openCommitmentsToMeCount: Int,
    val overdueRelationshipsCount: Int,
    val upcomingDatesCount: Int,
    val pendingTasksCount: Int,
    val activeProjectsCount: Int = 0,
    val insights: List<ProactiveInsightItem>
) {
    val openCommitmentsCount: Int get() = openCommitmentsByMeCount + openCommitmentsToMeCount
    val overduePeopleCount: Int get() = overdueRelationshipsCount
}

data class LifePatternInsight(
    val id: String,
    val icon: String,
    val categoryLabel: String,
    val title: String,
    val description: String,
    val strengthPercent: Int,
    val relatedPerson: PersonEntity? = null,
    val relatedNoteId: String? = null,
    val filterQuery: String? = null
)

enum class TimelineEventType {
    MEMORY,
    COMMITMENT,
    PLACE_VISIT
}

data class LifeTimelineEntry(
    val id: String,
    val timestamp: Long,
    val type: TimelineEventType,
    val icon: String,
    val title: String,
    val subtitle: String,
    val badgeLabel: String,
    val noteId: String? = null,
    val person: PersonEntity? = null
)

data class TimelineEpochGroup(
    val epochTitle: String,
    val entries: List<LifeTimelineEntry>
)

data class LifeIntelligenceReport(
    val totalMemoriesCount: Int,
    val verifiedFactsCount: Int,
    val pendingInferencesCount: Int,
    val commitmentFulfillmentPercent: Int,
    val completedCommitmentsCount: Int,
    val openCommitmentsCount: Int,
    val activeProjectsCount: Int,
    val completedProjectsCount: Int,
    val memoryLinksCount: Int,
    val categoryDistribution: List<Pair<String, Int>>,
    val discoveredPatterns: List<LifePatternInsight>,
    val rediscoveryMemories: List<NoteWithDetails>,
    val timelineEpochs: List<TimelineEpochGroup>
)

object MemoryIntelligenceHelper {

    private val STOP_WORDS = setOf(
        "the", "and", "for", "with", "that", "this", "from", "have", "was", "were",
        "are", "not", "but", "what", "all", "when", "can", "said", "there", "use",
        "each", "which", "she", "how", "their", "will", "other", "about", "out",
        "many", "then", "them", "these", "some", "her", "would", "make", "like",
        "him", "into", "time", "has", "look", "two", "more", "write", "see", "note",
        "memory", "untitled", "general", "today", "yesterday", "tomorrow"
    )

    fun findRelatedMemories(
        currentNoteId: String?,
        title: String,
        content: String,
        category: String,
        tags: List<String>,
        linkedPersonIds: Set<String>,
        locationName: String?,
        allNotes: List<NoteWithDetails>,
        maxResults: Int = 5
    ): List<LinkedMemoryMatch> {
        val currentKeywords = extractSignificantWords("$title $content")
        val normalizedTags = tags.map { it.lowercase().trim().removePrefix("#") }.toSet()

        val candidates = allNotes.filter { it.note.id != currentNoteId }
        val scored = mutableListOf<LinkedMemoryMatch>()

        for (candidate in candidates) {
            var score = 0
            val reasons = mutableListOf<String>()

            // 1. Shared People (High weight)
            val sharedPeople = candidate.people.filter { linkedPersonIds.contains(it.id) }
            if (sharedPeople.isNotEmpty()) {
                score += 42 + (sharedPeople.size - 1) * 12
                reasons.add("👥 ${sharedPeople.joinToString(", ") { it.name }}")
            }

            // 2. Shared Location
            val candLoc = candidate.note.getLocationName()
            if (!locationName.isNullOrBlank() && candLoc != null && candLoc.equals(locationName, ignoreCase = true)) {
                score += 28
                reasons.add("📍 $candLoc")
            }

            // 3. Shared Tags
            val candTags = candidate.note.getDisplayTagsList().map { it.lowercase() }.toSet()
            val commonTags = normalizedTags.intersect(candTags)
            if (commonTags.isNotEmpty()) {
                score += 25 + (commonTags.size - 1) * 10
                reasons.add("🏷️ #${commonTags.first()}")
            }

            // 4. Shared Keywords in Title/Content
            val candWords = extractSignificantWords("${candidate.note.title} ${candidate.note.content}")
            val sharedWords = currentKeywords.intersect(candWords)
            if (sharedWords.isNotEmpty()) {
                score += (sharedWords.size * 9).coerceAtMost(36)
                if (reasons.size < 2) {
                    reasons.add("🔗 Topic: ${sharedWords.take(2).joinToString(", ")}")
                }
            }

            // 5. Same Non-General Category
            if (category.isNotBlank() && category != "General" &&
                candidate.note.category.equals(category, ignoreCase = true)
            ) {
                score += 12
                if (reasons.isEmpty()) {
                    reasons.add("📂 $category")
                }
            }

            if (score >= 18) {
                val similarity = score.coerceIn(22, 98)
                scored.add(
                    LinkedMemoryMatch(
                        noteWithDetails = candidate,
                        similarityPercent = similarity,
                        matchReason = reasons.take(2).joinToString(" • ").ifBlank { "Semantic context match" }
                    )
                )
            }
        }

        return scored.sortedByDescending { it.similarityPercent }.take(maxResults)
    }

    private fun extractSignificantWords(text: String): Set<String> {
        return text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length >= 4 && !STOP_WORDS.contains(it) }
            .toSet()
    }

    /**
     * Extracts actionable tasks from raw note text or voice transcript.
     */
    fun extractActionItemsFromText(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        val results = mutableListOf<String>()
        val lines = text.split(Regex("[\\r\\n]+|(?<=[.!?])\\s+"))

        val actionVerbsRegex = Regex(
            "^\\s*(?:[-*•]|\\[\\s*]|todo:?|action:?|task:?|remind me to|need to|must|should|follow up with|call|email|send|schedule|prepare|review|buy|book|submit|finish|check)\\b.*",
            RegexOption.IGNORE_CASE
        )

        for (rawLine in lines) {
            val cleaned = rawLine.trim()
                .removePrefix("- [ ]")
                .removePrefix("[ ]")
                .removePrefix("-")
                .removePrefix("*")
                .removePrefix("•")
                .replace(Regex("^(?:TODO|Action|Task):\\s*", RegexOption.IGNORE_CASE), "")
                .trim()

            if (cleaned.length in 6..140 && actionVerbsRegex.matches(rawLine.trim())) {
                val formatted = cleaned.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                if (results.none { it.equals(formatted, ignoreCase = true) }) {
                    results.add(formatted)
                }
            }
        }

        // If no explicit bullet/verb matched but there are sentences with "by Friday", "tomorrow", etc.
        if (results.isEmpty()) {
            for (rawLine in lines) {
                val trimmed = rawLine.trim()
                if (trimmed.length in 10..120 &&
                    trimmed.contains(Regex("\\b(tomorrow|monday|tuesday|wednesday|thursday|friday|next week|deadline|urgent|asap)\\b", RegexOption.IGNORE_CASE))
                ) {
                    results.add(trimmed)
                }
            }
        }

        return results.take(6)
    }

    fun buildTopicClusters(allNotes: List<NoteWithDetails>): List<TopicCluster> {
        val clusterMap = mutableMapOf<String, MutableList<NoteWithDetails>>()

        for (item in allNotes) {
            val tags = item.note.getDisplayTagsList()
            if (tags.isNotEmpty()) {
                tags.forEach { tag ->
                    val key = "#${tag.lowercase()}"
                    clusterMap.getOrPut(key) { mutableListOf() }.add(item)
                }
            } else {
                val catKey = item.note.category.ifBlank { "General" }
                clusterMap.getOrPut(catKey) { mutableListOf() }.add(item)
            }
        }

        return clusterMap.entries
            .map { (name, list) ->
                val distinctNotes = list.distinctBy { it.note.id }
                val peopleNames = distinctNotes.flatMap { it.people }.map { it.name }.distinct()
                val icon = when {
                    name.startsWith("#") -> "🏷️"
                    name.equals("Meeting", true) || name.equals("Events", true) -> "🤝"
                    name.equals("Visuals", true) -> "📷"
                    name.equals("Ideas", true) -> "💡"
                    name.equals("Work", true) -> "💼"
                    else -> "🧠"
                }
                TopicCluster(
                    topicName = name,
                    badgeIcon = icon,
                    noteCount = distinctNotes.size,
                    relatedPeople = peopleNames,
                    notes = distinctNotes
                )
            }
            .sortedByDescending { it.noteCount }
    }

    fun buildLocationClusters(allNotes: List<NoteWithDetails>): List<LocationCluster> {
        val byLocation = allNotes.mapNotNull { item ->
            val loc = item.note.getLocationName()
            if (!loc.isNullOrBlank()) loc to item else null
        }.groupBy({ it.first }, { it.second })

        return byLocation.entries.map { (locName, items) ->
            val distinctPeople = items.flatMap { it.people }.map { it.id }.distinct().size
            LocationCluster(
                locationName = locName,
                notes = items.sortedByDescending { it.note.createdAt },
                peopleCount = distinctPeople
            )
        }.sortedByDescending { it.notes.size }
    }

    fun generateMeetingPrepBriefing(
        person: PersonEntity,
        personNotes: List<NoteWithDetails>,
        allActions: List<ActionItemEntity>,
        allCommitments: List<CommitmentEntity> = emptyList(),
        allProjects: List<ProjectEntity> = emptyList()
    ): MeetingPrepBriefing {
        val converters = RoomConverters()
        val facts = converters.toStringList(person.factsJson)
        val dates = converters.toStringMap(person.keyDatesJson)
            .filterKeys { it != "CatchUpCadenceDays" }

        val personNoteIds = personNotes.map { it.note.id }.toSet()
        val openActions = allActions.filter { action ->
            !action.isCompleted && (
                (action.noteId != null && personNoteIds.contains(action.noteId)) ||
                    action.task.contains(person.name, ignoreCase = true)
                )
        }

        val personCommitments = allCommitments.filter { c ->
            !c.isCompleted && (
                c.personId == person.id ||
                    c.description.contains(person.name, ignoreCase = true)
                )
        }

        val personProjectIds = (personNotes.mapNotNull { it.note.projectId } + personCommitments.mapNotNull { it.projectId }).toSet()
        val sharedProjects = allProjects.filter { it.id in personProjectIds }

        val talkingPoints = mutableListOf<String>()
        personCommitments.forEach { comm ->
            val prefix = if (comm.direction == "TO_ME") "Check on promise owed to you" else "Fulfill your promise"
            talkingPoints.add("🤝 $prefix: \"${comm.description}\"")
        }
        if (sharedProjects.isNotEmpty()) {
            talkingPoints.add("🚀 Review active project(s): ${sharedProjects.joinToString(", ") { it.name }}")
        }
        if (openActions.isNotEmpty()) {
            talkingPoints.add("Follow up on open task: \"${openActions.first().task}\"")
        }
        if (personNotes.isNotEmpty()) {
            val latest = personNotes.maxByOrNull { it.note.createdAt }!!
            talkingPoints.add("Reference last note from ${DateUtils.formatDate(latest.note.createdAt)}: \"${latest.note.title}\"")
        }
        if (facts.isNotEmpty()) {
            talkingPoints.add("Personal detail to remember: ${facts.first()}")
        }
        if (dates.isNotEmpty()) {
            val firstDate = dates.entries.first()
            talkingPoints.add("Check in regarding ${firstDate.key}: ${firstDate.value}")
        }
        if (talkingPoints.isEmpty()) {
            talkingPoints.add("Ask about their current priorities and upcoming milestones.")
            talkingPoints.add("Capture key facts during your conversation to enrich their dossier.")
        }

        val daysElapsed = person.daysSinceLastContact()
        val cadenceDays = person.getCatchUpCadenceDays()
        val cadenceStatus = if (daysElapsed >= cadenceDays) {
            "Overdue by ${daysElapsed - cadenceDays}d (${person.getCadenceLabel()})"
        } else {
            "On track • Contacted ${daysElapsed}d ago (${person.getCadenceLabel()})"
        }

        return MeetingPrepBriefing(
            person = person,
            relationship = person.relationship ?: "Contact",
            cadenceStatus = cadenceStatus,
            keyFacts = facts,
            keyDates = dates,
            recentSharedNotes = personNotes.sortedByDescending { it.note.createdAt }.take(5),
            openActionItems = openActions,
            talkingPoints = talkingPoints,
            openCommitments = personCommitments,
            sharedProjects = sharedProjects
        )
    }

    /**
     * Phase 5: Proactive AI Assistant & Anticipatory Briefing Engine.
     * Synthesizes open commitments, upcoming important dates, dormant relationships,
     * active project momentum, and pending tasks into a single actionable briefing.
     */
    fun buildAnticipatoryBriefing(
        allNotes: List<NoteWithDetails>,
        allPeople: List<PersonEntity>,
        allActions: List<ActionItemEntity>,
        allCommitments: List<CommitmentEntity> = emptyList(),
        allProjects: List<ProjectEntity> = emptyList()
    ): AnticipatoryDailyBriefing {
        val converters = RoomConverters()
        val peopleById = allPeople.associateBy { it.id }
        val projectsById = allProjects.associateBy { it.id }

        val openCommitments = allCommitments.filter { !it.isCompleted }
        val openByMe = openCommitments.filter { it.direction != "TO_ME" }
        val openToMe = openCommitments.filter { it.direction == "TO_ME" }
        val overduePeople = allPeople.filter { it.isOverdueForCatchUp() }
            .sortedByDescending { it.daysSinceLastContact() }
        val pendingTasks = allActions.filter { !it.isCompleted }

        val insights = mutableListOf<ProactiveInsightItem>()

        // 1. Open Commitments (Highest Proactive Priority)
        openCommitments.take(5).forEach { comm ->
            val linkedPerson = comm.personId?.let { peopleById[it] }
            val linkedProject = comm.projectId?.let { projectsById[it] }
            val dirPrefix = if (comm.direction == "TO_ME") "📥 Owed to You" else "📤 You Promised"
            val contextParts = mutableListOf(dirPrefix)
            linkedPerson?.let { contextParts.add("👤 ${it.name}") }
            linkedProject?.let { contextParts.add("🚀 ${it.name}") }
            insights.add(
                ProactiveInsightItem(
                    id = "proactive_comm_${comm.id}",
                    type = ProactiveInsightType.OPEN_COMMITMENT,
                    icon = "🤝",
                    title = comm.description,
                    subtitle = contextParts.joinToString(" • "),
                    actionLabel = "✓ Complete",
                    priorityScore = if (comm.direction == "BY_ME") 95 else 90,
                    person = linkedPerson,
                    commitment = comm,
                    project = linkedProject
                )
            )
        }

        // 2. Upcoming / Recorded Important Dates & Milestones from People Dossiers & IMPORTANT_DATES Notes
        var datesCount = 0
        for (person in allPeople) {
            val datesMap = converters.toStringMap(person.keyDatesJson)
                .filterKeys { it != "CatchUpCadenceDays" }
            for ((label, dateVal) in datesMap) {
                if (dateVal.isNotBlank()) {
                    datesCount++
                    if (insights.count { it.type == ProactiveInsightType.UPCOMING_DATE } < 3) {
                        insights.add(
                            ProactiveInsightItem(
                                id = "proactive_date_${person.id}_$label",
                                type = ProactiveInsightType.UPCOMING_DATE,
                                icon = "📅",
                                title = "${person.name}: $label ($dateVal)",
                                subtitle = "Important Date • ${person.relationship ?: "Contact"}",
                                actionLabel = "🔔 Set Alarm",
                                priorityScore = 88,
                                person = person,
                                dateLabel = label,
                                dateValue = dateVal
                            )
                        )
                    }
                }
            }
        }
        val dateNotes = allNotes.filter { it.note.memoryType == "IMPORTANT_DATES" }
        datesCount += dateNotes.size
        dateNotes.take(2).forEach { dNote ->
            if (insights.count { it.type == ProactiveInsightType.UPCOMING_DATE } < 4) {
                insights.add(
                    ProactiveInsightItem(
                        id = "proactive_datenote_${dNote.note.id}",
                        type = ProactiveInsightType.UPCOMING_DATE,
                        icon = "📅",
                        title = dNote.note.title,
                        subtitle = dNote.note.content.take(80),
                        actionLabel = "🔔 Set Alarm",
                        priorityScore = 85,
                        noteWithDetails = dNote
                    )
                )
            }
        }

        // 3. Dormant Relationships Overdue for Catch-Up
        overduePeople.take(3).forEach { person ->
            val days = person.daysSinceLastContact()
            insights.add(
                ProactiveInsightItem(
                    id = "proactive_dormant_${person.id}",
                    type = ProactiveInsightType.DORMANT_RELATIONSHIP,
                    icon = "👥",
                    title = "Reconnect with ${person.name}",
                    subtitle = "${person.relationship ?: "Contact"} • Last contacted ${days}d ago (${person.getCadenceLabel()})",
                    actionLabel = "💬 Catch Up",
                    priorityScore = 82,
                    person = person
                )
            )
        }

        // 4. Active Projects Needing Momentum
        val activeProjects = allProjects.filter { it.status.equals("ACTIVE", ignoreCase = true) }
        activeProjects.take(2).forEach { proj ->
            val projNotesCount = allNotes.count { it.note.projectId == proj.id }
            val projOpenComms = openCommitments.count { it.projectId == proj.id }
            insights.add(
                ProactiveInsightItem(
                    id = "proactive_proj_${proj.id}",
                    type = ProactiveInsightType.PROJECT_MOMENTUM,
                    icon = "🚀",
                    title = "Project: ${proj.name}",
                    subtitle = "$projOpenComms open commitment(s) • $projNotesCount linked note(s)",
                    actionLabel = "+ Project Note",
                    priorityScore = 76,
                    project = proj
                )
            )
        }

        // 5. Pending Action Items
        pendingTasks.take(3).forEach { task ->
            insights.add(
                ProactiveInsightItem(
                    id = "proactive_task_${task.id}",
                    type = ProactiveInsightType.PENDING_TASK,
                    icon = "✅",
                    title = task.task,
                    subtitle = "Open Action Item",
                    actionLabel = "🔔 Remind",
                    priorityScore = 72,
                    actionItem = task
                )
            )
        }

        val sortedInsights = insights.sortedByDescending { it.priorityScore }

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greetingTitle = when (hour) {
            in 5..11 -> "🌅 Morning Proactive Briefing"
            in 12..17 -> "⚡ Afternoon Intelligence Radar"
            else -> "🌙 Evening Memory Synthesis"
        }

        val summaryParts = mutableListOf<String>()
        if (openByMe.isNotEmpty()) summaryParts.add("${openByMe.size} promise(s) by you")
        if (openToMe.isNotEmpty()) summaryParts.add("${openToMe.size} owed to you")
        if (overduePeople.isNotEmpty()) summaryParts.add("${overduePeople.size} contact(s) due for catch-up")
        if (datesCount > 0) summaryParts.add("$datesCount important date(s)")
        if (pendingTasks.isNotEmpty()) summaryParts.add("${pendingTasks.size} open task(s)")

        val executiveSummary = if (summaryParts.isNotEmpty()) {
            "Active radar: ${summaryParts.joinToString(" • ")}."
        } else {
            "All commitments, relationships, and tasks are caught up."
        }

        return AnticipatoryDailyBriefing(
            greetingTitle = greetingTitle,
            executiveSummary = executiveSummary,
            openCommitmentsByMeCount = openByMe.size,
            openCommitmentsToMeCount = openToMe.size,
            overdueRelationshipsCount = overduePeople.size,
            upcomingDatesCount = datesCount,
            pendingTasksCount = pendingTasks.size,
            activeProjectsCount = activeProjects.size,
            insights = sortedInsights
        )
    }

    fun buildWeeklyIntelligenceDigest(
        allNotes: List<NoteWithDetails>,
        allPeople: List<PersonEntity>,
        allActions: List<ActionItemEntity>
    ): WeeklyIntelligenceDigest {
        val sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000L)
        val recentNotes = allNotes.filter { it.note.createdAt >= sevenDaysAgo }

        val audioCount = recentNotes.count { !it.note.rawTranscription.isNullOrBlank() }
        val visualCount = recentNotes.count { it.media.isNotEmpty() || it.note.category.equals("Visuals", true) }

        val completedCount = allActions.count { it.isCompleted }
        val pendingCount = allActions.count { !it.isCompleted }

        val tagFrequency = mutableMapOf<String, Int>()
        for (n in recentNotes.ifEmpty { allNotes }) {
            for (t in n.note.getDisplayTagsList()) {
                val clean = t.lowercase()
                tagFrequency[clean] = (tagFrequency[clean] ?: 0) + 1
            }
        }
        val topTags = tagFrequency.entries
            .sortedByDescending { it.value }
            .take(6)
            .map { it.key to it.value }

        val activePeople = recentNotes
            .flatMap { it.people }
            .groupingBy { it.name }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { "${it.key} (${it.value})" }

        val overduePeople = allPeople.filter { it.isOverdueForCatchUp() }
            .sortedByDescending { it.daysSinceLastContact() }

        val combinedText = recentNotes.joinToString(" ") { "${it.note.title} ${it.note.content}" }.lowercase()
        val positiveHits = listOf("great", "excited", "progress", "completed", "resolved", "won", "launch", "happy", "success")
            .count { combinedText.contains(it) }
        val intenseHits = listOf("urgent", "deadline", "blocker", "issue", "delayed", "stress", "critical")
            .count { combinedText.contains(it) }

        val sentiment = when {
            recentNotes.isEmpty() -> "Calm & Ready • Start capturing memories this week"
            positiveHits > intenseHits -> "High Momentum & Constructive (${positiveHits} positive signals)"
            intenseHits > positiveHits -> "High Focus / Deadline-Driven (${intenseHits} priority signals)"
            else -> "Balanced & Analytical Execution"
        }

        val takeaway = buildString {
            append("You captured ${recentNotes.size} memories in the last 7 days")
            if (completedCount + pendingCount > 0) {
                append(", with $completedCount completed and $pendingCount open action items. ")
            } else {
                append(". ")
            }
            if (overduePeople.isNotEmpty()) {
                append("Consider reaching out to ${overduePeople.take(2).joinToString(" & ") { it.name }} to stay on cadence.")
            } else if (topTags.isNotEmpty()) {
                append("Your primary focus areas centered around #${topTags.first().first}.")
            } else {
                append("Your relationship circles are all up to date!")
            }
        }

        return WeeklyIntelligenceDigest(
            notesThisWeek = recentNotes.size,
            audioNotesThisWeek = audioCount,
            visualNotesThisWeek = visualCount,
            completedTasksCount = completedCount,
            pendingTasksCount = pendingCount,
            topTags = topTags,
            activePeople = activePeople,
            overdueCatchUpPeople = overduePeople,
            energyAndSentimentLabel = sentiment,
            executiveTakeaway = takeaway
        )
    }

    /**
     * Phase 6: Personal Life Intelligence, Cross-Category Pattern Discovery & Chronological Timeline Engine.
     * Analyzes relationships between memories, people, places, projects, commitments, and graph links.
     */
    fun buildLifeIntelligenceReport(
        allNotes: List<NoteWithDetails>,
        allPeople: List<PersonEntity>,
        allPlaces: List<PlaceEntity> = emptyList(),
        allProjects: List<ProjectEntity> = emptyList(),
        allCommitments: List<CommitmentEntity> = emptyList(),
        allMemoryLinks: List<MemoryLinkEntity> = emptyList(),
        allActions: List<ActionItemEntity> = emptyList()
    ): LifeIntelligenceReport {
        val verifiedCount = allNotes.count { it.note.verificationStatus != "AI_INFERRED_PENDING" }
        val pendingCount = allNotes.count { it.note.verificationStatus == "AI_INFERRED_PENDING" }

        val completedComms = allCommitments.count { it.isCompleted }
        val openComms = allCommitments.count { !it.isCompleted }
        val totalComms = allCommitments.size
        val fulfillmentPct = if (totalComms > 0) {
            ((completedComms.toFloat() / totalComms.toFloat()) * 100f).toInt().coerceIn(0, 100)
        } else {
            100
        }

        val activeProjs = allProjects.count { it.status.equals("ACTIVE", ignoreCase = true) }
        val completedProjs = allProjects.count { it.status.equals("COMPLETED", ignoreCase = true) }

        // 1. 12-Category Unified Memory Type Distribution
        val typeCounts = mutableMapOf<String, Int>()
        for (item in allNotes) {
            val mType = item.note.memoryType.ifBlank { "OBSERVATIONS" }
            typeCounts[mType] = (typeCounts[mType] ?: 0) + 1
        }
        val categoryDistribution = typeCounts.entries
            .sortedByDescending { it.value }
            .map { it.key to it.value }

        // 2. Discover Cross-Category Life Patterns
        val patterns = mutableListOf<LifePatternInsight>()
        val peopleById = allPeople.associateBy { it.id }
        val projectsById = allProjects.associateBy { it.id }
        val placesById = allPlaces.associateBy { it.id }

        // Pattern A: Key Relationship & Collaboration Synergy
        val personNoteMap = mutableMapOf<String, MutableList<NoteWithDetails>>()
        for (n in allNotes) {
            for (p in n.people) {
                personNoteMap.getOrPut(p.id) { mutableListOf() }.add(n)
            }
        }
        val topCollaboratorEntry = personNoteMap.entries.maxByOrNull { it.value.size }
        if (topCollaboratorEntry != null && topCollaboratorEntry.value.isNotEmpty()) {
            val person = peopleById[topCollaboratorEntry.key]
            val pNotes = topCollaboratorEntry.value
            if (person != null) {
                val topSharedProject = pNotes.mapNotNull { it.note.projectId }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key?.let { projectsById[it] }
                val topSharedLocation = pNotes.mapNotNull { it.note.getLocationName() }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
                val contextSuffix = buildString {
                    if (topSharedProject != null) append(" around project '${topSharedProject.name}'")
                    if (!topSharedLocation.isNullOrBlank()) append(" and frequently at $topSharedLocation")
                }
                patterns.add(
                    LifePatternInsight(
                        id = "pat_collab_${person.id}",
                        icon = "🤝",
                        categoryLabel = "Relational Synergy",
                        title = "Primary Memory Anchor: ${person.name}",
                        description = "${person.name} is connected to ${pNotes.size} memory record(s)$contextSuffix.",
                        strengthPercent = (55 + pNotes.size * 10).coerceAtMost(98),
                        relatedPerson = person,
                        relatedNoteId = pNotes.first().note.id
                    )
                )
            }
        }

        // Pattern B: Project Focus & Velocity
        if (allProjects.isNotEmpty()) {
            val topProj = allProjects.maxByOrNull { proj ->
                allNotes.count { it.note.projectId == proj.id } + allCommitments.count { it.projectId == proj.id }
            }
            if (topProj != null) {
                val pNoteCount = allNotes.count { it.note.projectId == topProj.id }
                val pOpenComms = allCommitments.count { it.projectId == topProj.id && !it.isCompleted }
                val pDoneComms = allCommitments.count { it.projectId == topProj.id && it.isCompleted }
                patterns.add(
                    LifePatternInsight(
                        id = "pat_proj_${topProj.id}",
                        icon = "🚀",
                        categoryLabel = "Project Velocity",
                        title = "Active Focus: ${topProj.name} (${topProj.status})",
                        description = "Backed by $pNoteCount memory note(s), $pDoneComms fulfilled commitment(s), and $pOpenComms open deliverable(s).",
                        strengthPercent = (60 + pNoteCount * 8 + pOpenComms * 6).coerceAtMost(96),
                        filterQuery = topProj.name
                    )
                )
            }
        }

        // Pattern C: Spatial / Geotagged Memory Habit
        val topPlace = allPlaces.maxByOrNull { it.visitCount }
        if (topPlace != null) {
            val placeNotes = allNotes.filter {
                it.note.placeId == topPlace.id || it.note.getLocationName().equals(topPlace.name, ignoreCase = true)
            }
            val peopleAtPlace = placeNotes.flatMap { it.people }.map { it.name }.distinct()
            val peopleSuffix = if (peopleAtPlace.isNotEmpty()) " (often with ${peopleAtPlace.take(2).joinToString(", ")})" else ""
            patterns.add(
                LifePatternInsight(
                    id = "pat_place_${topPlace.id}",
                    icon = "📍",
                    categoryLabel = "Spatial Habit",
                    title = "Recurring Anchor Place: ${topPlace.name}",
                    description = "Logged ${topPlace.visitCount.coerceAtLeast(placeNotes.size)} visit(s) in category '${topPlace.category}'$peopleSuffix.",
                    strengthPercent = (58 + topPlace.visitCount * 7).coerceAtMost(95),
                    filterQuery = "loc:${topPlace.name}"
                )
            )
        }

        // Pattern D: Commitment & Promise Reliability
        if (allCommitments.isNotEmpty()) {
            val byMeOpen = allCommitments.count { !it.isCompleted && it.direction == "BY_ME" }
            val toMeOpen = allCommitments.count { !it.isCompleted && it.direction == "TO_ME" }
            patterns.add(
                LifePatternInsight(
                    id = "pat_commitments",
                    icon = "⚖️",
                    categoryLabel = "Commitment Balance",
                    title = "Promise Fulfillment: $fulfillmentPct% Complete",
                    description = "You currently have $byMeOpen open promise(s) owed by you and $toMeOpen deliverable(s) owed to you ($completedComms completed).",
                    strengthPercent = fulfillmentPct.coerceIn(50, 99)
                )
            )
        }

        // Pattern E: Neural Graph Connectivity
        if (allMemoryLinks.isNotEmpty()) {
            val relTypes = allMemoryLinks.groupingBy { it.relationType }.eachCount()
            val topRel = relTypes.maxByOrNull { it.value }?.key ?: "RELATED_TO"
            patterns.add(
                LifePatternInsight(
                    id = "pat_graph_links",
                    icon = "🕸️",
                    categoryLabel = "Knowledge Graph Density",
                    title = "${allMemoryLinks.size} Cross-Memory Neural Link(s)",
                    description = "Your memories are interconnected across ${allMemoryLinks.size} directional edges (primary relation: $topRel).",
                    strengthPercent = (65 + allMemoryLinks.size * 5).coerceAtMost(97)
                )
            )
        }

        // 3. Rediscover Past Memories ("Memory Rediscovery" — surface meaningful memories to revisit)
        val rediscoveryCandidates = allNotes
            .sortedWith(
                compareByDescending<NoteWithDetails> { it.note.isPinned }
                    .thenByDescending {
                        it.note.memoryType in setOf("IDEAS", "EXPERIENCES", "PERSONAL_OBSERVATIONS", "IMPORTANT_DATES", "PROJECTS")
                    }
                    .thenBy { it.note.createdAt } // surface older memories first within high-value tiers
            )
            .take(4)

        // 4. Unified Chronological Life Timeline grouped by Epoch
        val now = System.currentTimeMillis()
        val oneDayMs = 24L * 3600L * 1000L
        val timelineItems = mutableListOf<LifeTimelineEntry>()

        for (n in allNotes) {
            val ts = n.note.occurredAt ?: n.note.createdAt
            val pNames = n.people.joinToString(", ") { it.name }
            val loc = n.note.getLocationName()
            val subParts = mutableListOf<String>()
            if (pNames.isNotBlank()) subParts.add("👥 $pNames")
            if (!loc.isNullOrBlank()) subParts.add("📍 $loc")
            if (subParts.isEmpty()) subParts.add(n.note.content.take(70))

            timelineItems.add(
                LifeTimelineEntry(
                    id = "tl_note_${n.note.id}",
                    timestamp = ts,
                    type = TimelineEventType.MEMORY,
                    icon = if (n.note.isPinned) "📌" else "🧠",
                    title = n.note.title,
                    subtitle = subParts.joinToString(" • "),
                    badgeLabel = n.note.memoryType.ifBlank { n.note.category },
                    noteId = n.note.id,
                    person = n.people.firstOrNull()
                )
            )
        }

        for (c in allCommitments) {
            val linkedP = c.personId?.let { peopleById[it] }
            val statusStr = if (c.isCompleted) "Fulfilled" else if (c.direction == "TO_ME") "Owed to You" else "Promised by You"
            timelineItems.add(
                LifeTimelineEntry(
                    id = "tl_comm_${c.id}",
                    timestamp = c.createdAt,
                    type = TimelineEventType.COMMITMENT,
                    icon = if (c.isCompleted) "✅" else "🤝",
                    title = c.description,
                    subtitle = listOfNotNull(statusStr, linkedP?.let { "👤 ${it.name}" }).joinToString(" • "),
                    badgeLabel = "COMMITMENT",
                    noteId = c.noteId,
                    person = linkedP
                )
            )
        }

        for (pl in allPlaces) {
            timelineItems.add(
                LifeTimelineEntry(
                    id = "tl_place_${pl.id}",
                    timestamp = pl.lastVisitedAt,
                    type = TimelineEventType.PLACE_VISIT,
                    icon = "📍",
                    title = "Visited ${pl.name}",
                    subtitle = "${pl.category} • ${pl.visitCount} total visit(s)",
                    badgeLabel = "PLACE"
                )
            )
        }

        val sortedTimeline = timelineItems.sortedByDescending { it.timestamp }
        val todayEntries = sortedTimeline.filter { now - it.timestamp < oneDayMs }
        val weekEntries = sortedTimeline.filter { (now - it.timestamp) in oneDayMs until (7L * oneDayMs) }
        val monthEntries = sortedTimeline.filter { (now - it.timestamp) in (7L * oneDayMs) until (30L * oneDayMs) }
        val archiveEntries = sortedTimeline.filter { (now - it.timestamp) >= (30L * oneDayMs) }

        val epochs = mutableListOf<TimelineEpochGroup>()
        if (todayEntries.isNotEmpty()) epochs.add(TimelineEpochGroup("Today", todayEntries))
        if (weekEntries.isNotEmpty()) epochs.add(TimelineEpochGroup("Last 7 Days", weekEntries))
        if (monthEntries.isNotEmpty()) epochs.add(TimelineEpochGroup("Earlier This Month", monthEntries))
        if (archiveEntries.isNotEmpty()) epochs.add(TimelineEpochGroup("Personal History Archive", archiveEntries))

        return LifeIntelligenceReport(
            totalMemoriesCount = allNotes.size,
            verifiedFactsCount = verifiedCount,
            pendingInferencesCount = pendingCount,
            commitmentFulfillmentPercent = fulfillmentPct,
            completedCommitmentsCount = completedComms,
            openCommitmentsCount = openComms,
            activeProjectsCount = activeProjs,
            completedProjectsCount = completedProjs,
            memoryLinksCount = allMemoryLinks.size,
            categoryDistribution = categoryDistribution,
            discoveredPatterns = patterns,
            rediscoveryMemories = rediscoveryCandidates,
            timelineEpochs = epochs
        )
    }

    // --- Geolocation Reverse Lookup Helper ---
    @Suppress("MissingPermission")
    fun detectDevicePlaceName(context: Context): String? {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
            val providers = locationManager.getProviders(true)
            var bestLocation: android.location.Location? = null
            for (provider in providers) {
                val loc = locationManager.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                    bestLocation = loc
                }
            }
            if (bestLocation != null) {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(bestLocation.latitude, bestLocation.longitude, 1)
                val addr = addresses?.firstOrNull()
                if (addr != null) {
                    val feature = addr.subLocality ?: addr.locality ?: addr.adminArea
                    if (!feature.isNullOrBlank()) {
                        return feature
                    }
                }
                val latStr = String.format(Locale.US, "%.2f", bestLocation.latitude)
                val lonStr = String.format(Locale.US, "%.2f", bestLocation.longitude)
                "GPS ($latStr, $lonStr)"
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // --- Persistent Habit Tracker Storage ---
    private const val PREFS_NAME = "recall_habits_prefs"
    private const val KEY_HABITS_JSON = "habits_list_v1"

    fun getTodayDateKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun getLast7DayKeys(): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        val cal = Calendar.getInstance()
        val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayFmt = SimpleDateFormat("EEE", Locale.US)
        cal.add(Calendar.DAY_OF_YEAR, -6)
        for (i in 0..6) {
            list.add(keyFmt.format(cal.time) to dayFmt.format(cal.time).take(2))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return list
    }

    fun loadHabits(context: Context): List<HabitItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_HABITS_JSON, null)
        if (raw.isNullOrBlank()) {
            val defaults = listOf(
                HabitItem("h_debrief", "Evening Daily Debrief", "🎙️", emptySet()),
                HabitItem("h_reach_out", "Connect with 1 Contact", "👥", emptySet()),
                HabitItem("h_zero_actions", "Triage Open Action Items", "✅", emptySet()),
                HabitItem("h_capture_insight", "Log 1 Key Memory / Idea", "💡", emptySet())
            )
            saveHabits(context, defaults)
            return defaults
        }
        return try {
            val arr = JSONArray(raw)
            val result = mutableListOf<HabitItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val datesArr = obj.optJSONArray("dates") ?: JSONArray()
                val dates = mutableSetOf<String>()
                for (j in 0 until datesArr.length()) {
                    dates.add(datesArr.getString(j))
                }
                result.add(
                    HabitItem(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        icon = obj.optString("icon", "✨"),
                        completedDateKeys = dates
                    )
                )
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveHabits(context: Context, habits: List<HabitItem>) {
        val arr = JSONArray()
        habits.forEach { h ->
            val obj = JSONObject()
            obj.put("id", h.id)
            obj.put("title", h.title)
            obj.put("icon", h.icon)
            val dArr = JSONArray()
            h.completedDateKeys.forEach { dArr.put(it) }
            obj.put("dates", dArr)
            arr.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_HABITS_JSON, arr.toString())
            .apply()
    }
}
