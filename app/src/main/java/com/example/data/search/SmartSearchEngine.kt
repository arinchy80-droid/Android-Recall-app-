package com.example.data.search

import com.example.data.local.entity.ActionItemEntity
import com.example.data.local.entity.CommitmentEntity
import com.example.data.local.entity.MediaEntity
import com.example.data.local.entity.MediaType
import com.example.data.local.entity.MemoryLinkEntity
import com.example.data.local.entity.PersonEntity
import com.example.data.local.entity.PlaceEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.model.MemoryVerificationStatus
import com.example.data.model.NoteWithDetails
import com.example.data.model.SmartItemType
import com.example.data.model.SmartSearchItem
import com.example.data.model.SmartSearchNluInterpretation
import java.util.Locale
import kotlin.math.ln

object SmartSearchEngine {

    private val STOPWORDS = setOf(
        "show", "me", "all", "about", "my", "from", "the", "a", "an", "with", "regarding",
        "find", "of", "in", "to", "for", "and", "or", "on", "at", "by", "what", "who",
        "where", "how", "when", "is", "was", "are", "were", "be", "been", "last", "week",
        "summer", "month", "yesterday", "today", "notes", "note", "photo", "photos",
        "picture", "pictures", "image", "images", "tasks", "task", "did", "have", "has",
        "get", "got", "give", "please", "can", "could", "would", "tell", "do", "does"
    )

    /**
     * Parses the natural language query locally to extract people, places, projects,
     * time ranges, categories, intent, and semantic keywords with zero network latency.
     */
    fun parseQueryLocal(
        query: String,
        knownPeople: List<PersonEntity>,
        knownPlaces: List<PlaceEntity> = emptyList(),
        knownProjects: List<ProjectEntity> = emptyList()
    ): SmartSearchNluInterpretation {
        val q = query.lowercase().trim()
        val now = System.currentTimeMillis()

        // 1. Time range detection
        var timeFilterType: String? = null
        var timeRangeDesc: String? = null
        var timeStartMillis: Long? = null
        var timeEndMillis: Long? = null

        when {
            q.contains("last week") || q.contains("past week") || q.contains("this week") -> {
                timeFilterType = "LAST_WEEK"
                timeRangeDesc = if (q.contains("this week")) "This week" else "Last week"
                timeStartMillis = now - 14 * 86400000L
                timeEndMillis = now
            }
            q.contains("last summer") || q.contains("summer") -> {
                timeFilterType = "LAST_SUMMER"
                timeRangeDesc = "Last summer"
                timeStartMillis = now - 120 * 86400000L
                timeEndMillis = now - 40 * 86400000L
            }
            q.contains("last month") || q.contains("past month") || q.contains("this month") -> {
                timeFilterType = "LAST_MONTH"
                timeRangeDesc = if (q.contains("this month")) "This month" else "Last month"
                timeStartMillis = now - 35 * 86400000L
                timeEndMillis = now
            }
            q.contains("yesterday") -> {
                timeFilterType = "YESTERDAY"
                timeRangeDesc = "Yesterday"
                timeStartMillis = now - 2 * 86400000L
                timeEndMillis = now
            }
            q.contains("today") -> {
                timeFilterType = "TODAY"
                timeRangeDesc = "Today"
                timeStartMillis = now - 86400000L
                timeEndMillis = now
            }
        }

        // 2. Modality & Intent detection across all Unified Memory categories
        var intent = "ALL"
        var mediaType: MediaType? = null
        when {
            q.contains("photo") || q.contains("photos") || q.contains("picture") ||
                q.contains("pictures") || q.contains("image") || q.contains("images") ||
                q.contains("snapshot") || q.contains("gallery") -> {
                intent = "PHOTOS"
                mediaType = MediaType.IMAGE
            }
            q.contains("voice note") || q.contains("audio") || q.contains("recording") ||
                q.contains("transcript") -> {
                intent = "AUDIO"
                mediaType = MediaType.AUDIO
            }
            q.contains("commitment") || q.contains("commitments") || q.contains("promised") ||
                q.contains("promise") || q.contains("owe") || q.contains("owes") ||
                q.contains("debt") || q.contains("borrowed") || q.contains("lent") -> {
                intent = "COMMITMENTS"
            }
            q.contains("project") || q.contains("projects") || q.contains("roadmap") ||
                q.contains("milestone") || q.contains("initiative") -> {
                intent = "PROJECTS"
            }
            q.contains("where did") || q.contains("where was") || q.contains("places") ||
                q.contains("location") || q.contains("visited") -> {
                intent = "PLACES"
            }
            q.contains("who is") || q.contains("who was") || q.contains("contact") ||
                q.contains("dossier") || q.contains("people") -> {
                intent = "PEOPLE"
            }
            q.contains("task") || q.contains("tasks") || q.contains("action item") ||
                q.contains("todo") || q.contains("follow up") -> {
                intent = "TASKS"
            }
            q.contains("note") || q.contains("notes") || q.contains("meeting") ||
                q.contains("memo") -> {
                intent = "NOTES"
            }
        }

        // 3. Known Entities (People)
        val matchedEntities = mutableListOf<String>()
        for (person in knownPeople) {
            val pName = person.name.lowercase()
            val firstName = pName.split(" ").firstOrNull() ?: pName
            if (q.contains(pName) || (firstName.length >= 3 && Regex("\\b${Regex.escape(firstName)}\\b").containsMatchIn(q))) {
                if (!matchedEntities.contains(person.name)) {
                    matchedEntities.add(person.name)
                }
            }
        }

        // 4. Known Places
        val matchedPlaces = mutableListOf<String>()
        for (place in knownPlaces) {
            val plName = place.name.lowercase()
            if (plName.isNotBlank() && q.contains(plName)) {
                if (!matchedPlaces.contains(place.name)) {
                    matchedPlaces.add(place.name)
                }
            }
        }

        // 5. Known Projects
        val matchedProjects = mutableListOf<String>()
        for (project in knownProjects) {
            val prName = project.name.lowercase()
            if (prName.isNotBlank() && q.contains(prName)) {
                if (!matchedProjects.contains(project.name)) {
                    matchedProjects.add(project.name)
                }
            }
        }

        // Extract any entity names directly mentioned after prepositions like "with", "about", "to", "from"
        val prepPattern = Regex("\\b(?:with|about|to|from|for)\\s+([A-Z][a-zA-Z]{2,})\\b")
        prepPattern.findAll(query).forEach { matchResult ->
            val candidate = matchResult.groupValues[1]
            val lower = candidate.lowercase()
            if (!STOPWORDS.contains(lower) &&
                matchedEntities.none { it.contains(candidate, ignoreCase = true) } &&
                matchedPlaces.none { it.contains(candidate, ignoreCase = true) } &&
                matchedProjects.none { it.contains(candidate, ignoreCase = true) }
            ) {
                matchedEntities.add(candidate)
            }
        }

        // 6. Categories detection
        val categories = mutableListOf<String>()
        if (q.contains("meeting") || q.contains("sync") || q.contains("interview") || q.contains("discussion")) categories.add("Meeting")
        if (q.contains("project") || q.contains("work") || q.contains("task") || q.contains("job") || q.contains("office")) categories.add("Work")
        if (q.contains("idea") || q.contains("brainstorm") || q.contains("concept") || q.contains("research")) categories.add("Ideas")
        if (q.contains("personal") || q.contains("family") || q.contains("vacation") || q.contains("trip") || q.contains("health") || q.contains("fitness") || q.contains("finance")) categories.add("Personal")
        if (q.contains("photo") || q.contains("image") || q.contains("picture") || q.contains("screenshot") || q.contains("diagram") || q.contains("visual") || q.contains("camera")) categories.add("Visuals")

        // 7. Keyword extraction
        val words = query
            .replace(Regex("[^a-zA-Z0-9\\s]"), " ")
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() }

        val keywords = mutableListOf<String>()
        for (w in words) {
            val lower = w.lowercase()
            if (lower.length > 2 &&
                !STOPWORDS.contains(lower) &&
                !matchedEntities.any { it.contains(lower, ignoreCase = true) }
            ) {
                keywords.add(lower)
            }
        }

        // 8. Natural Language Explanation
        val explanationParts = mutableListOf<String>()
        when (intent) {
            "PHOTOS" -> explanationParts.add("Searching photos & visual memories")
            "COMMITMENTS" -> explanationParts.add("Searching promises, debts & commitments")
            "PROJECTS" -> explanationParts.add("Searching projects & linked deliverables")
            "PLACES" -> explanationParts.add("Searching places & geotagged memories")
            "NOTES" -> explanationParts.add("Searching notes & meetings")
            else -> explanationParts.add("Traversing personal memory graph")
        }

        if (matchedEntities.isNotEmpty()) {
            explanationParts.add("involving ${matchedEntities.joinToString(", ")}")
        }
        if (matchedPlaces.isNotEmpty()) {
            explanationParts.add("at ${matchedPlaces.joinToString(", ")}")
        }
        if (matchedProjects.isNotEmpty()) {
            explanationParts.add("under project ${matchedProjects.joinToString(", ")}")
        }
        if (keywords.isNotEmpty()) {
            val primaryKeywords = keywords.take(2).joinToString(" & ")
            explanationParts.add("regarding \"$primaryKeywords\"")
        }
        if (timeRangeDesc != null) {
            explanationParts.add("from $timeRangeDesc")
        }

        val explanation = explanationParts.joinToString(" ")

        return SmartSearchNluInterpretation(
            originalQuery = query,
            intent = intent,
            keywords = keywords.distinct(),
            entities = matchedEntities.distinct(),
            places = matchedPlaces.distinct(),
            projects = matchedProjects.distinct(),
            categories = categories.distinct(),
            mediaType = mediaType,
            timeFilterType = timeFilterType,
            timeRangeDescription = timeRangeDesc,
            timeStartMillis = timeStartMillis,
            timeEndMillis = timeEndMillis,
            explanation = explanation
        )
    }

    /**
     * Scores, matches, and ranks all memory items (notes, media, people, places,
     * projects, commitments, action items) using hybrid BM25/TF-IDF lexical scoring
     * combined with multi-hop relational graph traversal across MemoryLinkEntity,
     * PlaceEntity, ProjectEntity, and PersonEntity connections.
     */
    fun filterAndRankMemories(
        allNotes: List<NoteWithDetails>,
        allMedia: List<MediaEntity>,
        allPeople: List<PersonEntity>,
        allActions: List<ActionItemEntity>,
        interpretation: SmartSearchNluInterpretation,
        allPlaces: List<PlaceEntity> = emptyList(),
        allProjects: List<ProjectEntity> = emptyList(),
        allCommitments: List<CommitmentEntity> = emptyList(),
        allMemoryLinks: List<MemoryLinkEntity> = emptyList()
    ): List<SmartSearchItem> {
        val results = mutableListOf<SmartSearchItem>()

        val qLower = interpretation.originalQuery.lowercase().trim()
        val entities = interpretation.entities.map { it.lowercase() }
        val placeNames = interpretation.places.map { it.lowercase() }
        val projectNames = interpretation.projects.map { it.lowercase() }
        val keywords = interpretation.keywords.map { it.lowercase() }
        val categories = interpretation.categories.map { it.lowercase() }
        val startMillis = interpretation.timeStartMillis
        val endMillis = interpretation.timeEndMillis

        // Lookup maps for relational graph resolution
        val peopleById = allPeople.associateBy { it.id }
        val placesById = allPlaces.associateBy { it.id }
        val projectsById = allProjects.associateBy { it.id }
        val notesById = allNotes.associateBy { it.note.id }

        // Matched entity IDs for structured graph filtering
        val matchedPersonIds = allPeople
            .filter { p -> entities.any { p.name.lowercase().contains(it) } }
            .map { it.id }
            .toSet()
        val matchedPlaceIds = allPlaces
            .filter { pl ->
                placeNames.any { pl.name.lowercase().contains(it) } ||
                    keywords.any { pl.name.lowercase().contains(it) || (pl.address?.lowercase()?.contains(it) == true) }
            }
            .map { it.id }
            .toSet()
        val matchedProjectIds = allProjects
            .filter { pr ->
                projectNames.any { pr.name.lowercase().contains(it) } ||
                    keywords.any { pr.name.lowercase().contains(it) }
            }
            .map { it.id }
            .toSet()

        // Compute inverse document frequency (IDF) across notes for BM25-style keyword weighting
        val totalDocs = allNotes.size.coerceAtLeast(1)
        val keywordIdfBoost = keywords.associateWith { kw ->
            val docFreq = allNotes.count { item ->
                item.note.title.contains(kw, ignoreCase = true) ||
                    item.note.content.contains(kw, ignoreCase = true) ||
                    item.note.tagsJson.contains(kw, ignoreCase = true)
            }
            // Rare terms in the user's memory corpus get up to +12 bonus points per hit
            val idf = ln(1.0 + (totalDocs - docFreq + 0.5) / (docFreq + 0.5))
            (idf * 6.0).toInt().coerceIn(2, 12)
        }

        // Track direct note scores so we can execute 1-hop graph expansion via MemoryLinkEntity & Project/Place edges
        val directNoteScores = mutableMapOf<String, Int>()
        val noteHighlightsMap = mutableMapOf<String, MutableList<String>>()
        val noteGraphPaths = mutableMapOf<String, String>()

        // -------------------------------------------------------------
        // 1. NOTES SCORING (Lexical BM25 + Direct Entity/Place/Project Links)
        // -------------------------------------------------------------
        for (item in allNotes) {
            val note = item.note
            val titleLower = note.title.lowercase()
            val contentLower = note.content.lowercase()
            val rawLower = note.rawTranscription?.lowercase() ?: ""
            val summaryLower = note.aiSummary?.lowercase() ?: ""
            val categoryLower = note.category.lowercase()
            val locationLower = note.getLocationName()?.lowercase() ?: ""
            val peopleInNote = item.people.map { it.name.lowercase() }
            val linkedPlace = note.placeId?.let { placesById[it] }
            val linkedProject = note.projectId?.let { projectsById[it] }

            var score = 0
            val highlights = mutableListOf<String>()

            // A. Person Entity matching (linked or mentioned)
            for (ent in entities) {
                val matchesLinked = peopleInNote.any { it.contains(ent) }
                val matchesText = titleLower.contains(ent) || contentLower.contains(ent) || rawLower.contains(ent)
                if (matchesLinked || matchesText) {
                    score += if (matchesLinked) 45 else 35
                    highlights.add("👤 ${ent.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }}")
                }
            }

            // B. Structured Place matching
            if (note.placeId != null && note.placeId in matchedPlaceIds) {
                score += 40
                highlights.add("📍 ${linkedPlace?.name ?: note.getLocationName() ?: "Place"}")
            } else if (locationLower.isNotBlank() && (placeNames.any { locationLower.contains(it) } || keywords.any { locationLower.contains(it) })) {
                score += 35
                highlights.add("📍 ${note.getLocationName()}")
            }

            // C. Structured Project matching
            if (note.projectId != null && note.projectId in matchedProjectIds) {
                score += 42
                highlights.add("🚀 ${linkedProject?.name ?: "Project"}")
            }

            // D. Time range matching (check both occurredAt and createdAt)
            if (startMillis != null && endMillis != null) {
                val effectiveTime = if (note.occurredAt > 0L) note.occurredAt else note.createdAt
                if (effectiveTime in startMillis..endMillis || note.createdAt in startMillis..endMillis) {
                    score += 30
                    interpretation.timeRangeDescription?.let { highlights.add("🗓️ $it") }
                }
            }

            // E. Category & UnifiedMemoryType matching
            if (categories.any { it.equals(categoryLower, ignoreCase = true) }) {
                score += 20
                highlights.add("Category: ${note.category}")
            }
            if (interpretation.intent == "COMMITMENTS" && note.memoryType == "COMMITMENTS") {
                score += 25
                highlights.add("🤝 Commitment Memory")
            }
            if (interpretation.intent == "PROJECTS" && (note.memoryType == "PROJECTS" || note.projectId != null)) {
                score += 25
                highlights.add("🚀 Project Memory")
            }
            if (interpretation.intent == "PLACES" && (note.memoryType == "PLACES" || note.placeId != null || locationLower.isNotBlank())) {
                score += 25
                highlights.add("📍 Location Memory")
            }

            // F. Keyword & Phrase matching with IDF weighting
            var keywordHits = 0
            for (kw in keywords) {
                val idfBonus = keywordIdfBoost[kw] ?: 4
                var hit = false
                if (titleLower.contains(kw)) {
                    score += 22 + idfBonus
                    hit = true
                }
                if (contentLower.contains(kw) || summaryLower.contains(kw)) {
                    score += 14 + idfBonus
                    hit = true
                }
                if (rawLower.contains(kw)) {
                    score += 10
                    hit = true
                }
                if (linkedProject?.name?.lowercase()?.contains(kw) == true) {
                    score += 20
                    highlights.add("🚀 ${linkedProject.name}")
                    hit = true
                }
                if (linkedPlace?.name?.lowercase()?.contains(kw) == true) {
                    score += 20
                    highlights.add("📍 ${linkedPlace.name}")
                    hit = true
                }
                if (hit) keywordHits++
            }
            if (keywordHits > 0) {
                highlights.add("$keywordHits keyword match${if (keywordHits > 1) "es" else ""}")
            }

            // G. Direct full query substring match
            if (qLower.length >= 3 && (titleLower.contains(qLower) || contentLower.contains(qLower))) {
                score += 45
                highlights.add("Direct match")
            }

            // H. Intent & Provenance adjustments
            if (interpretation.intent == "NOTES" && score > 0) {
                score += 15
            }
            if (score > 0) {
                if (note.verificationStatus == MemoryVerificationStatus.AI_INFERRED_PENDING.name) {
                    // Keep unverified AI inferences ranked slightly lower than verified facts and clearly labeled
                    score = (score - 8).coerceAtLeast(10)
                    highlights.add("🤖 AI Inferred (Unverified)")
                } else if (note.confidenceScore >= 0.90f) {
                    score += 4
                }
                directNoteScores[note.id] = score
                noteHighlightsMap[note.id] = highlights
            }
        }

        // -------------------------------------------------------------
        // 1B. RELATIONAL GRAPH TRAVERSAL (1-Hop Expansion across MemoryLinkEntity, Projects & Places)
        // -------------------------------------------------------------
        val seedNoteIds = directNoteScores.filterValues { it >= 40 }.keys.toSet()

        // Traverse explicit MemoryLinkEntity edges from high-confidence seed notes
        for (link in allMemoryLinks) {
            val fromSeed = link.sourceNoteId in seedNoteIds
            val toSeed = link.targetNoteId in seedNoteIds
            if (fromSeed || toSeed) {
                val neighborId = if (fromSeed) link.targetNoteId else link.sourceNoteId
                val seedId = if (fromSeed) link.sourceNoteId else link.targetNoteId
                val seedTitle = notesById[seedId]?.note?.title?.take(28) ?: "Related Note"
                val hopBoost = (24 * link.strengthScore).toInt().coerceIn(12, 30)
                val current = directNoteScores[neighborId] ?: 0
                directNoteScores[neighborId] = if (current == 0) 52 + hopBoost / 2 else current + hopBoost
                val hList = noteHighlightsMap.getOrPut(neighborId) { mutableListOf() }
                val pathLabel = "🕸️ Graph Link (${link.relationType}): $seedTitle"
                hList.add(pathLabel)
                if (!noteGraphPaths.containsKey(neighborId)) {
                    noteGraphPaths[neighborId] = pathLabel
                }
            }
        }

        // Traverse indirect Project & Place hops (e.g., searching for a Person surfaces notes in the same Project/Place as that Person's notes)
        val personLinkedProjectIds = allNotes
            .filter { n -> n.people.any { it.id in matchedPersonIds } && n.note.projectId != null }
            .mapNotNull { it.note.projectId }
            .toSet()
        val seedProjectIds = matchedProjectIds + personLinkedProjectIds + seedNoteIds.mapNotNull { notesById[it]?.note?.projectId }

        if (seedProjectIds.isNotEmpty()) {
            for (item in allNotes) {
                val projId = item.note.projectId ?: continue
                if (projId in seedProjectIds && (directNoteScores[item.note.id] ?: 0) == 0) {
                    val projName = projectsById[projId]?.name ?: "Linked Project"
                    directNoteScores[item.note.id] = 56
                    val hList = noteHighlightsMap.getOrPut(item.note.id) { mutableListOf() }
                    val pathLabel = "🕸️ Graph Hop via Project: $projName"
                    hList.add(pathLabel)
                    noteGraphPaths[item.note.id] = pathLabel
                }
            }
        }

        // Materialize Note search items
        for ((noteId, rawScore) in directNoteScores) {
            val item = notesById[noteId] ?: continue
            val note = item.note
            val snippet = extractSnippet(note.content, keywords)
            val locBadge = note.getLocationName()?.let { " • 📍 $it" } ?: ""
            val projBadge = note.projectId?.let { projectsById[it]?.name }?.let { " • 🚀 $it" } ?: ""
            results.add(
                SmartSearchItem(
                    id = "note_${note.id}",
                    itemType = SmartItemType.NOTE,
                    title = note.title,
                    subtitle = "${note.category}$locBadge$projBadge • ${formatDateAgo(note.createdAt)}",
                    snippet = snippet,
                    timestamp = note.createdAt,
                    matchScore = rawScore.coerceIn(50, 99),
                    highlights = noteHighlightsMap[noteId]?.distinct() ?: emptyList(),
                    graphPath = noteGraphPaths[noteId],
                    noteWithDetails = item
                )
            )
        }

        // -------------------------------------------------------------
        // 2. COMMITMENTS SCORING (Promises & Obligations Owed By/To User)
        // -------------------------------------------------------------
        for (commitment in allCommitments) {
            val descLower = commitment.description.lowercase()
            val linkedPerson = commitment.personId?.let { peopleById[it] }
            val linkedProject = commitment.projectId?.let { projectsById[it] }
            val personNameLower = linkedPerson?.name?.lowercase() ?: ""
            val projectNameLower = linkedProject?.name?.lowercase() ?: ""

            var score = 0
            val highlights = mutableListOf<String>()

            if (interpretation.intent == "COMMITMENTS" || interpretation.intent == "TASKS") {
                score += 35
                highlights.add(if (commitment.direction == "TO_ME") "📥 Owed to Me" else "📤 Promised by Me")
            }

            if (commitment.personId != null && commitment.personId in matchedPersonIds) {
                score += 45
                highlights.add("👤 ${linkedPerson?.name ?: "Linked Person"}")
            } else if (entities.any { descLower.contains(it) || personNameLower.contains(it) }) {
                score += 40
            }

            if (commitment.projectId != null && commitment.projectId in seedProjectIds) {
                score += 38
                highlights.add("🚀 ${linkedProject?.name ?: "Project"}")
            }

            for (kw in keywords) {
                if (descLower.contains(kw) || personNameLower.contains(kw) || projectNameLower.contains(kw)) {
                    score += 22
                    highlights.add("Match: $kw")
                }
            }

            if (score > 0) {
                val dirBadge = if (commitment.direction == "TO_ME") "📥 Owed to You" else "📤 You Promised"
                val statusLabel = if (commitment.isCompleted) "Completed" else "Open Commitment"
                val personPart = linkedPerson?.let { " • 👤 ${it.name}" } ?: ""
                val projectPart = linkedProject?.let { " • 🚀 ${it.name}" } ?: ""
                results.add(
                    SmartSearchItem(
                        id = "commitment_${commitment.id}",
                        itemType = SmartItemType.COMMITMENT,
                        title = commitment.description,
                        subtitle = "$dirBadge • $statusLabel$personPart$projectPart",
                        snippet = if (commitment.verificationStatus == MemoryVerificationStatus.AI_INFERRED_PENDING.name) {
                            "🤖 AI-Inferred Commitment (Awaiting confirmation)"
                        } else {
                            "Structured commitment tracked in your Personal Memory Graph."
                        },
                        timestamp = commitment.createdAt,
                        matchScore = score.coerceIn(55, 99),
                        highlights = highlights.distinct(),
                        commitmentEntity = commitment
                    )
                )
            }
        }

        // -------------------------------------------------------------
        // 3. PLACES SCORING (First-Class Place Dossiers)
        // -------------------------------------------------------------
        for (place in allPlaces) {
            val nameLower = place.name.lowercase()
            val catLower = place.category.lowercase()
            val addrLower = place.address?.lowercase() ?: ""
            val notesLower = place.notes.lowercase()

            var score = 0
            val highlights = mutableListOf<String>()

            if (place.id in matchedPlaceIds || placeNames.any { nameLower.contains(it) }) {
                score += 50
                highlights.add("📍 Place Match")
            }

            for (kw in keywords) {
                if (nameLower.contains(kw) || catLower.contains(kw) || addrLower.contains(kw) || notesLower.contains(kw)) {
                    score += 25
                    highlights.add("Location: $kw")
                }
            }

            // Graph hop: if user searches for a Person and that Person has notes at this Place
            val notesAtPlace = allNotes.filter {
                it.note.placeId == place.id || it.note.getLocationName().equals(place.name, ignoreCase = true)
            }
            if (matchedPersonIds.isNotEmpty() && notesAtPlace.any { n -> n.people.any { it.id in matchedPersonIds } }) {
                score += 35
                highlights.add("🕸️ Visited with matched person")
            }

            if (interpretation.intent == "PLACES" && score > 0) {
                score += 20
            } else if (interpretation.intent == "PLACES" && keywords.isEmpty() && entities.isEmpty()) {
                score += 40
            }

            if (score > 0) {
                val associatedPeople = notesAtPlace.flatMap { it.people }.map { it.name }.distinct()
                val peopleSummary = if (associatedPeople.isNotEmpty()) {
                    "People here: ${associatedPeople.take(4).joinToString(", ")}"
                } else {
                    place.notes.ifBlank { place.address ?: "Recorded location in your memory graph" }
                }
                results.add(
                    SmartSearchItem(
                        id = "place_${place.id}",
                        itemType = SmartItemType.PLACE,
                        title = place.name,
                        subtitle = "📍 ${place.category} • ${place.visitCount} visit(s) • ${notesAtPlace.size} note(s)",
                        snippet = peopleSummary,
                        timestamp = place.lastVisitedAt,
                        matchScore = score.coerceIn(52, 99),
                        highlights = highlights.distinct(),
                        placeEntity = place
                    )
                )
            }
        }

        // -------------------------------------------------------------
        // 4. PROJECTS SCORING (First-Class Project Dossiers)
        // -------------------------------------------------------------
        for (project in allProjects) {
            val nameLower = project.name.lowercase()
            val descLower = project.description.lowercase()

            var score = 0
            val highlights = mutableListOf<String>()

            if (project.id in matchedProjectIds || projectNames.any { nameLower.contains(it) }) {
                score += 55
                highlights.add("🚀 Project Match")
            }

            for (kw in keywords) {
                if (nameLower.contains(kw) || descLower.contains(kw)) {
                    score += 25
                    highlights.add("Topic: $kw")
                }
            }

            if (project.id in personLinkedProjectIds) {
                score += 35
                highlights.add("🕸️ Collaborator match")
            }

            if (interpretation.intent == "PROJECTS" && (score > 0 || keywords.isEmpty())) {
                score += 30
            }

            if (score > 0) {
                val projNotesCount = allNotes.count { it.note.projectId == project.id }
                val projOpenCommitments = allCommitments.count { it.projectId == project.id && !it.isCompleted }
                results.add(
                    SmartSearchItem(
                        id = "project_${project.id}",
                        itemType = SmartItemType.PROJECT,
                        title = project.name,
                        subtitle = "🚀 ${project.status} • $projNotesCount note(s) • $projOpenCommitments open commitment(s)",
                        snippet = project.description.ifBlank { "Active project dossier in your Recall memory system." },
                        timestamp = project.updatedAt,
                        matchScore = score.coerceIn(55, 99),
                        highlights = highlights.distinct(),
                        projectEntity = project
                    )
                )
            }
        }

        // -------------------------------------------------------------
        // 5. MEDIA (PHOTOS & AUDIO) SCORING
        // -------------------------------------------------------------
        for (media in allMedia) {
            val descLower = media.aiDescription?.lowercase() ?: ""
            val ocrLower = media.ocrText?.lowercase() ?: ""
            val labelsLower = media.labelsJson.lowercase()

            var score = 0
            val highlights = mutableListOf<String>()

            if (interpretation.mediaType == media.mediaType || interpretation.intent == "PHOTOS") {
                score += 35
                highlights.add(if (media.mediaType == MediaType.IMAGE) "Photo Memory" else "Audio Memory")
            }

            if (startMillis != null && endMillis != null) {
                if (media.capturedAt in startMillis..endMillis ||
                    (interpretation.timeFilterType == "LAST_SUMMER" && labelsLower.contains("summer"))
                ) {
                    score += 35
                    interpretation.timeRangeDescription?.let { highlights.add(it) }
                }
            }

            for (kw in keywords) {
                var hit = false
                if (labelsLower.contains(kw)) {
                    score += 30
                    highlights.add("Label: $kw")
                    hit = true
                }
                if (descLower.contains(kw)) {
                    score += 25
                    hit = true
                }
                if (ocrLower.contains(kw)) {
                    score += 20
                    highlights.add("OCR text match")
                    hit = true
                }
                if (hit && !highlights.contains("Match: $kw")) {
                    highlights.add("Match: $kw")
                }
            }

            if (qLower.contains("dog") && (labelsLower.contains("dog") || descLower.contains("dog"))) {
                score += 50
                highlights.add("Pet / Dog memory")
            }

            if (score > 30) {
                val snippet = media.aiDescription ?: media.ocrText ?: "Visual capture with labels: ${media.labelsJson}"
                results.add(
                    SmartSearchItem(
                        id = "media_${media.id}",
                        itemType = SmartItemType.PHOTO,
                        title = media.aiDescription?.take(40) ?: "Visual Memory (${media.mediaType.name})",
                        subtitle = "Captured ${formatDateAgo(media.capturedAt)}",
                        snippet = snippet,
                        timestamp = media.capturedAt,
                        matchScore = score.coerceIn(55, 99),
                        highlights = highlights.distinct(),
                        mediaEntity = media
                    )
                )
            }
        }

        // -------------------------------------------------------------
        // 6. PEOPLE CRM SCORING (Plus Graph Hop from Place/Project)
        // -------------------------------------------------------------
        for (person in allPeople) {
            val nameLower = person.name.lowercase()
            val relLower = person.relationship?.lowercase() ?: ""
            val factsLower = person.factsJson.lowercase()

            var score = 0
            val highlights = mutableListOf<String>()

            if (entities.any { nameLower.contains(it) }) {
                score += 55
                highlights.add("👤 Entity match")
            }

            for (kw in keywords) {
                if (relLower.contains(kw) || factsLower.contains(kw)) {
                    score += 24
                    highlights.add("Fact match: $kw")
                }
            }

            // Graph hop: if query matched a Place or Project, surface People linked to notes at that Place/Project
            if (matchedPlaceIds.isNotEmpty()) {
                val metAtPlace = allNotes.any { n ->
                    n.note.placeId in matchedPlaceIds && n.people.any { it.id == person.id }
                }
                if (metAtPlace) {
                    score += 38
                    highlights.add("🕸️ Met at matched place")
                }
            }
            if (matchedProjectIds.isNotEmpty()) {
                val inProject = allNotes.any { n ->
                    n.note.projectId in matchedProjectIds && n.people.any { it.id == person.id }
                } || allCommitments.any { c ->
                    c.projectId in matchedProjectIds && c.personId == person.id
                }
                if (inProject) {
                    score += 38
                    highlights.add("🕸️ Involved in matched project")
                }
            }

            if (interpretation.intent == "PEOPLE") {
                score += 25
            }

            if (score > 0) {
                val openPersonCommitments = allCommitments.count { it.personId == person.id && !it.isCompleted }
                val commitSuffix = if (openPersonCommitments > 0) " • $openPersonCommitments open commitment(s)" else ""
                results.add(
                    SmartSearchItem(
                        id = "person_${person.id}",
                        itemType = SmartItemType.PERSON,
                        title = person.name,
                        subtitle = (person.relationship ?: "Contact") + commitSuffix,
                        snippet = "Key facts: ${person.factsJson.replace("[\"", "").replace("\"]", "").replace("\",\"", " • ")}",
                        timestamp = person.createdAt,
                        matchScore = score.coerceIn(50, 99),
                        highlights = highlights.distinct(),
                        personEntity = person
                    )
                )
            }
        }

        // -------------------------------------------------------------
        // 7. ACTION ITEMS SCORING
        // -------------------------------------------------------------
        for (act in allActions) {
            val taskLower = act.task.lowercase()
            var score = 0
            val highlights = mutableListOf<String>()

            for (ent in entities) {
                if (taskLower.contains(ent)) {
                    score += 35
                    highlights.add("Related to $ent")
                }
            }
            for (kw in keywords) {
                if (taskLower.contains(kw)) {
                    score += 20
                    highlights.add("Task keyword: $kw")
                }
            }
            if (interpretation.intent == "TASKS") {
                score += 25
            }

            if (score > 0) {
                results.add(
                    SmartSearchItem(
                        id = "action_${act.id}",
                        itemType = SmartItemType.ACTION,
                        title = act.task,
                        subtitle = if (act.isCompleted) "Completed" else "Pending Task",
                        snippet = "Action item extracted from memory stream.",
                        timestamp = act.createdAt,
                        matchScore = score.coerceIn(50, 99),
                        highlights = highlights.distinct(),
                        actionEntity = act
                    )
                )
            }
        }

        // -------------------------------------------------------------
        // 8. SORTING & INTENT PRIORITIZATION
        // -------------------------------------------------------------
        return results.sortedWith(
            compareByDescending<SmartSearchItem> { item ->
                when (interpretation.intent) {
                    "PHOTOS" -> if (item.itemType == SmartItemType.PHOTO) 200 else 0
                    "NOTES" -> if (item.itemType == SmartItemType.NOTE) 200 else 0
                    "PEOPLE" -> if (item.itemType == SmartItemType.PERSON) 200 else 0
                    "PLACES" -> if (item.itemType == SmartItemType.PLACE) 200 else 0
                    "PROJECTS" -> if (item.itemType == SmartItemType.PROJECT) 200 else 0
                    "COMMITMENTS" -> if (item.itemType == SmartItemType.COMMITMENT) 200 else 0
                    "TASKS" -> if (item.itemType == SmartItemType.ACTION || item.itemType == SmartItemType.COMMITMENT) 200 else 0
                    else -> 0
                } + item.matchScore
            }.thenByDescending { it.timestamp }
        )
    }

    private fun extractSnippet(content: String, keywords: List<String>): String {
        if (content.length <= 120) return content
        for (kw in keywords) {
            val idx = content.indexOf(kw, ignoreCase = true)
            if (idx != -1) {
                val start = (idx - 30).coerceAtLeast(0)
                val end = (idx + 90).coerceAtMost(content.length)
                val prefix = if (start > 0) "..." else ""
                val suffix = if (end < content.length) "..." else ""
                return "$prefix${content.substring(start, end)}$suffix"
            }
        }
        return content.take(120) + "..."
    }

    private fun formatDateAgo(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val days = diff / 86400000L
        return when {
            days <= 0 -> "Today"
            days == 1L -> "Yesterday"
            days < 7 -> "$days days ago"
            days < 14 -> "Last week"
            days < 30 -> "${days / 7} weeks ago"
            days < 60 -> "Last month"
            days < 120 -> "Last summer"
            else -> "${days / 30} months ago"
        }
    }
}
