package com.example.data.repository

import com.example.data.local.RecallDatabase
import com.example.data.local.converter.RoomConverters
import com.example.data.local.entity.ActionItemEntity
import com.example.data.local.entity.CommitmentEntity
import com.example.data.local.entity.MediaEntity
import com.example.data.local.entity.MediaType
import com.example.data.local.entity.MemoryLinkEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.NotePersonCrossRef
import com.example.data.local.entity.NoteRevisionEntity
import com.example.data.local.entity.PersonEntity
import com.example.data.local.entity.PlaceEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.model.DailyDebriefResult
import com.example.data.model.DebriefNoteUpdate
import com.example.data.model.MemorySourceType
import com.example.data.model.MemoryVerificationStatus
import com.example.data.model.NoteWithDetails
import com.example.data.model.UnifiedMemoryType
import com.example.util.MemoryReconciliationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class RecallRepository(
    private val database: RecallDatabase
) {
    private val noteDao = database.noteDao()
    private val personDao = database.personDao()
    private val mediaDao = database.mediaDao()
    private val actionItemDao = database.actionItemDao()
    private val reminderDao = database.reminderDao()
    private val placeDao = database.placeDao()
    private val projectDao = database.projectDao()
    private val commitmentDao = database.commitmentDao()
    private val memoryLinkDao = database.memoryLinkDao()
    private val noteRevisionDao = database.noteRevisionDao()
    private val converters = RoomConverters()

    // --- Reminders Observables ---
    val allReminders: Flow<List<com.example.data.local.entity.ReminderEntity>> = reminderDao.getAllReminders()
    val activeReminders: Flow<List<com.example.data.local.entity.ReminderEntity>> = reminderDao.getActiveReminders()

    fun getRemindersForNote(noteId: String): Flow<List<com.example.data.local.entity.ReminderEntity>> {
        return reminderDao.getRemindersForNote(noteId)
    }

    fun getActiveReminderForNote(noteId: String): Flow<com.example.data.local.entity.ReminderEntity?> {
        return reminderDao.getActiveReminderForNote(noteId)
    }

    // --- Notes & Unified Memory Observables ---
    val allNotes: Flow<List<NoteWithDetails>> = noteDao.getAllNotesWithDetails()
    val notesCount: Flow<Int> = noteDao.getNotesCount()

    fun getNotesByCategory(category: String): Flow<List<NoteWithDetails>> {
        return if (category == "All") allNotes else noteDao.getNotesByCategory(category)
    }

    fun getNotesByMemoryType(memoryType: UnifiedMemoryType): Flow<List<NoteWithDetails>> {
        return noteDao.getNotesByMemoryType(memoryType.name)
    }

    fun getNotesForProject(projectId: String): Flow<List<NoteWithDetails>> {
        return noteDao.getNotesForProject(projectId)
    }

    fun getNotesForPlace(placeId: String): Flow<List<NoteWithDetails>> {
        return noteDao.getNotesForPlace(placeId)
    }

    fun getNoteById(noteId: String): Flow<NoteWithDetails?> {
        return noteDao.getNoteWithDetailsById(noteId)
    }

    suspend fun getNoteByIdOnce(noteId: String): NoteWithDetails? {
        return noteDao.getNoteWithDetailsByIdOnce(noteId)
    }

    fun getNotesForPerson(personId: String): Flow<List<NoteWithDetails>> {
        return noteDao.getNotesForPerson(personId)
    }

    fun searchNotes(query: String): Flow<List<NoteWithDetails>> {
        return noteDao.searchNotesWithDetails(query)
    }

    // --- Places, Projects, Commitments, Links & Revisions Observables ---
    val allPlaces: Flow<List<PlaceEntity>> = placeDao.getAllPlaces()
    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()
    val allCommitments: Flow<List<CommitmentEntity>> = commitmentDao.getAllCommitments()
    val activeCommitments: Flow<List<CommitmentEntity>> = commitmentDao.getActiveCommitments()
    val allMemoryLinks: Flow<List<MemoryLinkEntity>> = memoryLinkDao.getAllMemoryLinks()
    val unverifiedMemories: Flow<List<NoteWithDetails>> = noteDao.getNotesByVerificationStatus(MemoryVerificationStatus.AI_INFERRED_PENDING.name)

    fun getRevisionsForNote(noteId: String): Flow<List<NoteRevisionEntity>> {
        return noteRevisionDao.getRevisionsForNote(noteId)
    }

    fun getLinksForNote(noteId: String): Flow<List<MemoryLinkEntity>> {
        return memoryLinkDao.getLinksForNote(noteId)
    }

    fun getCommitmentsForPerson(personId: String): Flow<List<CommitmentEntity>> {
        return commitmentDao.getCommitmentsForPerson(personId)
    }

    fun getCommitmentsForProject(projectId: String): Flow<List<CommitmentEntity>> {
        return commitmentDao.getCommitmentsForProject(projectId)
    }

    // --- People Observables ---
    val allPeople: Flow<List<PersonEntity>> = personDao.getAllPeople()
    val peopleCount: Flow<Int> = personDao.getPeopleCount()

    fun getPersonById(personId: String): Flow<PersonEntity?> {
        return personDao.getPersonById(personId)
    }

    suspend fun getPersonByIdOnce(personId: String): PersonEntity? {
        return personDao.getPersonByIdOnce(personId)
    }

    fun searchPeople(query: String): Flow<List<PersonEntity>> {
        return personDao.searchPeople(query)
    }

    // --- Action Items Observables ---
    val allActionItems: Flow<List<ActionItemEntity>> = actionItemDao.getAllActionItems()
    val pendingActionItems: Flow<List<ActionItemEntity>> = actionItemDao.getPendingActionItems()
    val pendingActionItemsCount: Flow<Int> = actionItemDao.getPendingCount()

    // --- Media Observables ---
    val allMedia: Flow<List<MediaEntity>> = mediaDao.getAllMedia()

    /**
     * Ensures a first-class PlaceEntity exists whenever a location name is attached to a memory.
     */
    private suspend fun resolveOrSyncPlaceId(locationName: String?, existingPlaceId: String?): String? {
        val cleanName = locationName?.trim()?.takeIf { it.isNotBlank() } ?: return existingPlaceId
        val existingPlace = placeDao.findPlaceByName(cleanName)
        return if (existingPlace != null) {
            val updated = existingPlace.copy(
                visitCount = existingPlace.visitCount + 1,
                lastVisitedAt = System.currentTimeMillis()
            )
            placeDao.updatePlace(updated)
            updated.id
        } else {
            val newPlace = PlaceEntity(
                id = UUID.randomUUID().toString(),
                name = cleanName,
                category = "Location",
                visitCount = 1,
                lastVisitedAt = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            )
            placeDao.insertPlace(newPlace)
            newPlace.id
        }
    }

    /**
     * Chooses the best descriptive title when merging two notes that have the exact same main content
     * but different headings (e.g. "Untitled Note" vs a Smart Tagged heading or Debrief title).
     */
    private fun chooseBestTitle(titleA: String, titleB: String, fallbackContent: String): String {
        val cleanA = titleA.trim()
        val cleanB = titleB.trim()
        val isGenericA = cleanA.isBlank() || cleanA.equals("Untitled Note", ignoreCase = true) || cleanA.startsWith("Daily Debrief:", ignoreCase = true)
        val isGenericB = cleanB.isBlank() || cleanB.equals("Untitled Note", ignoreCase = true) || cleanB.startsWith("Daily Debrief:", ignoreCase = true)

        return when {
            !isGenericB && isGenericA -> cleanB
            !isGenericA && isGenericB -> cleanA
            cleanB.isNotBlank() && cleanB != "Untitled Note" -> cleanB
            cleanA.isNotBlank() && cleanA != "Untitled Note" -> cleanA
            else -> fallbackContent.lines().firstOrNull { it.isNotBlank() }?.take(45) ?: "Untitled Note"
        }
    }

    // --- Write Operations ---
    suspend fun saveNote(
        note: NoteEntity,
        linkedPersonIds: List<String>,
        mediaAttachments: List<MediaEntity> = emptyList(),
        actionItems: List<ActionItemEntity> = emptyList()
    ) = withContext(Dispatchers.IO) {
        // 0. Check if another note with a different ID already has the exact same normalized main content
        val allExistingWithDetails = noteDao.getAllNotesWithDetails().first()
        val duplicateContentNote = if (note.content.isNotBlank()) {
            allExistingWithDetails.firstOrNull { other ->
                other.note.id != note.id &&
                    MemoryReconciliationEngine.areNoteContentsEquivalent(other.note.content, note.content)
            }
        } else null

        val targetNoteId = duplicateContentNote?.note?.id ?: note.id
        val prior = duplicateContentNote?.note ?: noteDao.getNoteWithDetailsByIdOnce(note.id)?.note

        // If we are collapsing a temporary draft note.id into an existing duplicateContentNote, remove the draft row
        if (duplicateContentNote != null && note.id != duplicateContentNote.note.id) {
            memoryLinkDao.deleteLinksForNote(note.id)
            noteRevisionDao.deleteRevisionsForNote(note.id)
            noteDao.deleteCrossRefsForNote(note.id)
            noteDao.deleteNoteById(note.id)
        }

        // 1. Preserve non-destructive revision snapshot if updating an existing note with changed content
        if (prior != null && !MemoryReconciliationEngine.areNoteContentsEquivalent(prior.content, note.content)) {
            noteRevisionDao.insertRevision(
                NoteRevisionEntity(
                    id = UUID.randomUUID().toString(),
                    noteId = targetNoteId,
                    previousTitle = prior.title,
                    previousContent = prior.content,
                    changeSummary = "Updated memory content",
                    sourceType = note.sourceType,
                    createdAt = System.currentTimeMillis()
                )
            )
        }

        val effectiveLocation = note.getLocationName() ?: prior?.getLocationName()
        // 2. Sync PlaceEntity if note contains a location tag
        val resolvedPlaceId = resolveOrSyncPlaceId(effectiveLocation, note.placeId ?: prior?.placeId)

        val resolvedTitle = if (prior != null) {
            chooseBestTitle(prior.title, note.title, note.content)
        } else {
            note.title.ifBlank {
                note.content.lines().firstOrNull { it.isNotBlank() }?.take(45) ?: "Untitled Note"
            }
        }

        val mergedDisplayTags = if (prior != null) {
            (prior.getDisplayTagsList() + note.getDisplayTagsList()).distinct()
        } else {
            note.getDisplayTagsList()
        }
        val mergedTagsJson = NoteEntity.formatTagsJson(
            NoteEntity.mergeTagsWithLocation(mergedDisplayTags, effectiveLocation)
        )

        // 3. Ensure canonical memoryType and sourceType are populated
        val resolvedMemoryType = if (note.memoryType.isBlank() || note.memoryType == "PERSONAL_OBSERVATIONS") {
            UnifiedMemoryType.inferFromLegacyCategory(
                category = note.category,
                hasAudio = !note.rawTranscription.isNullOrBlank() || mediaAttachments.any { it.mediaType == MediaType.AUDIO },
                hasLocation = resolvedPlaceId != null,
                contentHint = "$resolvedTitle ${note.content}"
            ).name
        } else {
            note.memoryType
        }

        val resolvedSourceType = if (note.sourceType == "USER_EXPLICIT" && !note.rawTranscription.isNullOrBlank()) {
            MemorySourceType.AUDIO_TRANSCRIPT.name
        } else {
            note.sourceType
        }

        val mergedPersonIds = if (duplicateContentNote != null) {
            (duplicateContentNote.people.map { it.id } + linkedPersonIds).distinct()
        } else {
            linkedPersonIds.distinct()
        }

        val enrichedNote = note.copy(
            id = targetNoteId,
            title = resolvedTitle,
            tagsJson = mergedTagsJson,
            isPinned = note.isPinned || (prior?.isPinned == true),
            createdAt = prior?.createdAt ?: note.createdAt,
            updatedAt = System.currentTimeMillis(),
            memoryType = resolvedMemoryType,
            sourceType = resolvedSourceType,
            placeId = resolvedPlaceId,
            projectId = note.projectId ?: prior?.projectId,
            occurredAt = if (note.occurredAt <= 0L) (prior?.occurredAt ?: note.createdAt) else note.occurredAt
        )

        noteDao.insertNoteWithPeople(enrichedNote, mergedPersonIds)
        if (mediaAttachments.isNotEmpty()) {
            val updatedMedia = mediaAttachments.map { it.copy(noteId = enrichedNote.id) }
            mediaDao.insertMediaList(updatedMedia)
        }
        if (actionItems.isNotEmpty()) {
            val updatedItems = actionItems.map { it.copy(noteId = enrichedNote.id) }
            actionItemDao.insertActionItems(updatedItems)
        }

        // 4. Automatically discover and persist relational MemoryLinkEntity graph edges
        autoDiscoverMemoryLinks(enrichedNote, mergedPersonIds)
    }

    /**
     * Automatically connects a newly saved or updated memory to existing memories that share
     * a project, a place + person, or overlapping people + topic tags.
     */
    private suspend fun autoDiscoverMemoryLinks(note: NoteEntity, linkedPersonIds: List<String>) {
        try {
            val existingNotes = noteDao.getAllNotesWithDetails().first()
            val existingLinks = memoryLinkDao.getAllMemoryLinksRaw()
            val alreadyLinkedIds = existingLinks
                .filter { it.sourceNoteId == note.id || it.targetNoteId == note.id }
                .flatMap { listOf(it.sourceNoteId, it.targetNoteId) }
                .toSet()

            val noteTags = note.getDisplayTagsList().map { it.lowercase() }.toSet()
            val personIdSet = linkedPersonIds.toSet()

            val candidateLinks = mutableListOf<MemoryLinkEntity>()
            for (other in existingNotes) {
                val otherNote = other.note
                if (otherNote.id == note.id || otherNote.id in alreadyLinkedIds) continue

                val sharedProject = note.projectId != null && note.projectId == otherNote.projectId
                val sharedPlace = note.placeId != null && note.placeId == otherNote.placeId
                val otherPersonIds = other.people.map { it.id }.toSet()
                val sharedPeopleCount = personIdSet.intersect(otherPersonIds).size
                val otherTags = otherNote.getDisplayTagsList().map { it.lowercase() }.toSet()
                val sharedTagsCount = noteTags.intersect(otherTags).size

                when {
                    sharedProject -> {
                        candidateLinks.add(
                            MemoryLinkEntity(
                                id = UUID.randomUUID().toString(),
                                sourceNoteId = note.id,
                                targetNoteId = otherNote.id,
                                relationType = "CONTINUES",
                                strengthScore = 0.90f,
                                createdAt = System.currentTimeMillis()
                            )
                        )
                    }
                    sharedPeopleCount > 0 && sharedTagsCount > 0 -> {
                        candidateLinks.add(
                            MemoryLinkEntity(
                                id = UUID.randomUUID().toString(),
                                sourceNoteId = note.id,
                                targetNoteId = otherNote.id,
                                relationType = "EXPANDS",
                                strengthScore = 0.85f,
                                createdAt = System.currentTimeMillis()
                            )
                        )
                    }
                    sharedPlace && sharedPeopleCount > 0 -> {
                        candidateLinks.add(
                            MemoryLinkEntity(
                                id = UUID.randomUUID().toString(),
                                sourceNoteId = note.id,
                                targetNoteId = otherNote.id,
                                relationType = "RELATED_TO",
                                strengthScore = 0.80f,
                                createdAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
                if (candidateLinks.size >= 3) break
            }

            if (candidateLinks.isNotEmpty()) {
                memoryLinkDao.insertMemoryLinks(candidateLinks)
            }
        } catch (_: Exception) {
            // Non-blocking graph link discovery
        }
    }

    suspend fun togglePinNote(note: NoteEntity) = withContext(Dispatchers.IO) {
        noteDao.updateNote(note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteNote(noteId: String) = withContext(Dispatchers.IO) {
        memoryLinkDao.deleteLinksForNote(noteId)
        noteRevisionDao.deleteRevisionsForNote(noteId)
        noteDao.deleteCrossRefsForNote(noteId)
        noteDao.deleteNoteById(noteId)
    }

    // --- Unified Memory Graph Entity Operations ---
    suspend fun savePlace(place: PlaceEntity) = withContext(Dispatchers.IO) {
        placeDao.insertPlace(place)
    }

    suspend fun deletePlace(placeId: String) = withContext(Dispatchers.IO) {
        placeDao.deletePlaceById(placeId)
    }

    suspend fun saveProject(project: ProjectEntity) = withContext(Dispatchers.IO) {
        projectDao.insertProject(project.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteProject(projectId: String) = withContext(Dispatchers.IO) {
        projectDao.deleteProjectById(projectId)
    }

    suspend fun saveCommitment(commitment: CommitmentEntity) = withContext(Dispatchers.IO) {
        commitmentDao.insertCommitment(commitment)
    }

    suspend fun toggleCommitmentCompleted(id: String, isCompleted: Boolean) = withContext(Dispatchers.IO) {
        commitmentDao.setCommitmentCompleted(id, isCompleted)
    }

    suspend fun deleteCommitment(id: String) = withContext(Dispatchers.IO) {
        commitmentDao.deleteCommitmentById(id)
    }

    suspend fun linkMemories(
        sourceNoteId: String,
        targetNoteId: String,
        relationType: String = "RELATED_TO",
        strengthScore: Float = 1.0f
    ) = withContext(Dispatchers.IO) {
        if (sourceNoteId == targetNoteId) return@withContext
        memoryLinkDao.insertMemoryLink(
            MemoryLinkEntity(
                id = UUID.randomUUID().toString(),
                sourceNoteId = sourceNoteId,
                targetNoteId = targetNoteId,
                relationType = relationType,
                strengthScore = strengthScore,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteMemoryLink(linkId: String) = withContext(Dispatchers.IO) {
        memoryLinkDao.deleteMemoryLinkById(linkId)
    }

    /**
     * Explicitly confirms an AI-inferred or audio-extracted memory as a verified user fact.
     */
    suspend fun verifyMemoryFact(note: NoteEntity) = withContext(Dispatchers.IO) {
        noteDao.updateNote(
            note.copy(
                verificationStatus = MemoryVerificationStatus.VERIFIED_FACT.name,
                confidenceScore = 1.0f,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * Marks a memory as an unverified AI interpretation requiring user review.
     */
    suspend fun markMemoryAsAiInferred(note: NoteEntity) = withContext(Dispatchers.IO) {
        noteDao.updateNote(
            note.copy(
                verificationStatus = MemoryVerificationStatus.AI_INFERRED_PENDING.name,
                sourceType = MemorySourceType.AI_INTERPRETATION.name,
                confidenceScore = 0.72f,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * Restores a historical NoteRevisionEntity snapshot while preserving the current state in revision history.
     */
    suspend fun restoreNoteRevision(note: NoteEntity, revision: NoteRevisionEntity): NoteEntity = withContext(Dispatchers.IO) {
        noteRevisionDao.insertRevision(
            NoteRevisionEntity(
                id = UUID.randomUUID().toString(),
                noteId = note.id,
                previousTitle = note.title,
                previousContent = note.content,
                changeSummary = "Snapshot prior to restoring version from ${java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.getDefault()).format(java.util.Date(revision.createdAt))}",
                sourceType = MemorySourceType.USER_EXPLICIT.name,
                createdAt = System.currentTimeMillis()
            )
        )
        val restored = note.copy(
            title = revision.previousTitle,
            content = revision.previousContent,
            updatedAt = System.currentTimeMillis()
        )
        noteDao.updateNote(restored)
        restored
    }

    suspend fun savePerson(person: PersonEntity): PersonEntity = withContext(Dispatchers.IO) {
        val rawFacts = converters.toStringList(person.factsJson)
        val cleanedFacts = rawFacts
            .map { cleanFactText(it) }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
        val rawDates = converters.toStringMap(person.keyDatesJson)
        val dedupedDates = mergeAndDeduplicateDates(emptyMap(), rawDates)
        val savedPerson = person.copy(
            factsJson = converters.fromStringList(cleanedFacts),
            keyDatesJson = converters.fromStringMap(dedupedDates)
        )
        personDao.insertPerson(savedPerson)
        savedPerson
    }

    private fun cleanFactText(raw: String): String {
        return raw.trim()
            .replace(Regex("^[•\\-*\\d.)]+\\s*"), "")
            .trim()
    }

    private fun extractCoreFactTokens(fact: String, personName: String): Set<String> {
        val nameTokens = personName.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .toSet()

        val stopWords = setOf(
            "is", "are", "was", "were", "be", "been", "being",
            "a", "an", "the", "to", "in", "on", "at", "of", "for", "with", "and", "or", "by", "from", "as", "into", "about",
            "has", "have", "had", "he", "she", "they", "his", "her", "their", "him", "them",
            "likes", "like", "enjoys", "enjoy", "prefers", "prefer", "loves", "love", "favorite", "favourite",
            "works", "work", "working", "worked", "currently", "mentioned", "noted", "that", "who", "very", "really", "also"
        )

        return fact.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .map { it.trim() }
            .filter { it.length >= 2 && it !in stopWords && it !in nameTokens }
            .map { token ->
                when {
                    token.length > 5 && token.endsWith("ing") -> token.removeSuffix("ing")
                    token.length > 4 && token.endsWith("ed") -> token.removeSuffix("ed")
                    token.length > 4 && token.endsWith("es") -> token.removeSuffix("es")
                    token.length > 3 && token.endsWith("s") -> token.removeSuffix("s")
                    else -> token
                }
            }
            .toSet()
    }

    private fun areFactsDuplicate(factA: String, factB: String, personName: String): Boolean {
        val cleanA = cleanFactText(factA)
        val cleanB = cleanFactText(factB)
        if (cleanA.equals(cleanB, ignoreCase = true)) return true

        val normA = cleanA.lowercase().replace(Regex("[^a-z0-9\\s]"), "").replace(Regex("\\s+"), " ").trim()
        val normB = cleanB.lowercase().replace(Regex("[^a-z0-9\\s]"), "").replace(Regex("\\s+"), " ").trim()
        if (normA == normB) return true
        if (normA.length >= 8 && normB.length >= 8 && (normA.contains(normB) || normB.contains(normA))) {
            return true
        }

        val tokensA = extractCoreFactTokens(cleanA, personName)
        val tokensB = extractCoreFactTokens(cleanB, personName)
        if (tokensA.isEmpty() || tokensB.isEmpty()) return false
        if (tokensA == tokensB) return true

        val intersection = tokensA.intersect(tokensB).size
        val minSize = minOf(tokensA.size, tokensB.size)
        val unionSize = tokensA.union(tokensB).size

        if (minSize >= 2 && intersection.toDouble() / minSize >= 0.70) return true
        if (unionSize >= 2 && intersection.toDouble() / unionSize >= 0.55) return true

        return false
    }

    private fun mergeAndDeduplicateFacts(
        existingFacts: List<String>,
        newFacts: List<String>,
        personName: String
    ): List<String> {
        val result = mutableListOf<String>()
        val allCandidates = existingFacts + newFacts
        for (rawCandidate in allCandidates) {
            val cleaned = cleanFactText(rawCandidate)
            if (cleaned.isBlank()) continue
            val lower = cleaned.lowercase()
            if (lower == "synthesized from local notes" || lower.startsWith("mentioned in notes with ")) continue

            val duplicateIndex = result.indexOfFirst { existing ->
                areFactsDuplicate(existing, cleaned, personName)
            }
            if (duplicateIndex == -1) {
                result.add(cleaned)
            }
        }
        return result
    }

    private fun mergeAndDeduplicateDates(
        existingDates: Map<String, String>,
        newDates: Map<String, String>
    ): Map<String, String> {
        val invalidValues = setOf("", "...", "unknown", "n/a", "none", "null", "yyyy-mm-dd", "yyyy-mm-dd or hint", "tbd")
        val merged = linkedMapOf<String, String>()

        fun putDeduplicated(rawKey: String, rawVal: String) {
            val k = rawKey.trim()
            val v = rawVal.trim()
            if (k.isBlank() || v.isBlank() || v.lowercase() in invalidValues) return
            val existingKey = merged.keys.firstOrNull {
                it.equals(k, ignoreCase = true) ||
                    (it.lowercase().replace(" ", "") == k.lowercase().replace(" ", ""))
            }
            if (existingKey != null) {
                merged[existingKey] = v
            } else {
                merged[k] = v
            }
        }

        existingDates.forEach { (k, v) -> putDeduplicated(k, v) }
        newDates.forEach { (k, v) -> putDeduplicated(k, v) }
        return merged
    }

    suspend fun updatePersonFactsAndDates(
        personId: String,
        newFacts: List<String>,
        newDates: Map<String, String>
    ) = withContext(Dispatchers.IO) {
        val existing = personDao.getPersonByIdOnce(personId) ?: return@withContext
        val currentFacts = converters.toStringList(existing.factsJson)
        val deduplicatedFacts = mergeAndDeduplicateFacts(currentFacts, newFacts, existing.name)

        val currentDates = converters.toStringMap(existing.keyDatesJson)
        val deduplicatedDates = mergeAndDeduplicateDates(currentDates, newDates)

        val updated = existing.copy(
            factsJson = converters.fromStringList(deduplicatedFacts),
            keyDatesJson = converters.fromStringMap(deduplicatedDates),
            lastContactedAt = System.currentTimeMillis()
        )
        personDao.updatePerson(updated)
    }

    suspend fun deletePerson(personId: String) = withContext(Dispatchers.IO) {
        noteDao.deleteCrossRefsForPerson(personId)
        personDao.deletePersonById(personId)
    }

    suspend fun toggleActionItem(id: String, isCompleted: Boolean) = withContext(Dispatchers.IO) {
        actionItemDao.setCompleted(id, isCompleted)
    }

    suspend fun deleteActionItem(id: String) = withContext(Dispatchers.IO) {
        actionItemDao.deleteActionItemById(id)
    }

    suspend fun addMedia(media: MediaEntity) = withContext(Dispatchers.IO) {
        mediaDao.insertMedia(media)
    }

    /**
     * Applies a list of `DebriefNoteUpdate` items in-place to existing notes and their linked people's facts.
     */
    suspend fun applyNoteUpdates(
        updates: List<DebriefNoteUpdate>,
        audioFilePath: String? = null,
        rawTranscription: String? = null
    ): Int = withContext(Dispatchers.IO) {
        var updatedCount = 0
        for (update in updates.filter { it.isIncluded }) {
            val existingWithDetails = noteDao.getNoteWithDetailsByIdOnce(update.targetNoteId) ?: continue
            val existingNote = existingWithDetails.note

            val linkedPersonIds = mutableListOf<String>()
            existingWithDetails.people.forEach { linkedPersonIds.add(it.id) }

            for (personName in update.peopleInvolved) {
                if (!MemoryReconciliationEngine.isValidPersonName(personName)) continue
                val found = personDao.findPersonByName(personName)
                val personEntity = if (found != null) {
                    val currentFacts = converters.toStringList(found.factsJson)
                    val reconciledFacts = MemoryReconciliationEngine.reconcilePersonFacts(
                        existingFacts = currentFacts,
                        incomingFacts = listOf("${update.updatedTitle} (${update.changeSummary})"),
                        personName = found.name
                    )
                    val deduped = mergeAndDeduplicateFacts(emptyList(), reconciledFacts, found.name)
                    val updatedPerson = found.copy(
                        factsJson = converters.fromStringList(deduped),
                        lastContactedAt = System.currentTimeMillis()
                    )
                    personDao.updatePerson(updatedPerson)
                    updatedPerson
                } else {
                    val newPerson = PersonEntity(
                        id = UUID.randomUUID().toString(),
                        name = personName.trim(),
                        relationship = "Contact",
                        factsJson = converters.fromStringList(listOf("${update.updatedTitle} (${update.changeSummary})")),
                        lastContactedAt = System.currentTimeMillis()
                    )
                    personDao.insertPerson(newPerson)
                    newPerson
                }
                if (!linkedPersonIds.contains(personEntity.id)) {
                    linkedPersonIds.add(personEntity.id)
                }
            }

            val existingLocation = existingNote.getLocationName()
            val mergedTags = NoteEntity.mergeTagsWithLocation(
                (existingNote.getDisplayTagsList() + update.tags).distinct(),
                existingLocation
            )

            // Record non-destructive revision snapshot before applying in-place reconciliation update
            noteRevisionDao.insertRevision(
                NoteRevisionEntity(
                    id = UUID.randomUUID().toString(),
                    noteId = existingNote.id,
                    previousTitle = existingNote.title,
                    previousContent = existingNote.content,
                    changeSummary = update.changeSummary.ifBlank { "Living Notes Reconciliation Update" },
                    sourceType = if (!rawTranscription.isNullOrBlank()) MemorySourceType.AUDIO_TRANSCRIPT.name else existingNote.sourceType,
                    createdAt = System.currentTimeMillis()
                )
            )

            val resolvedCategory = update.category.ifBlank { existingNote.category }
            val resolvedPlaceId = resolveOrSyncPlaceId(existingLocation, existingNote.placeId)
            val resolvedMemoryType = UnifiedMemoryType.inferFromLegacyCategory(
                category = resolvedCategory,
                hasAudio = !rawTranscription.isNullOrBlank() || !existingNote.rawTranscription.isNullOrBlank(),
                hasLocation = resolvedPlaceId != null,
                contentHint = "${update.updatedTitle} ${update.updatedContent}"
            ).name

            val updatedNoteEntity = existingNote.copy(
                title = update.updatedTitle.ifBlank { existingNote.title },
                content = update.updatedContent.ifBlank { existingNote.content },
                category = resolvedCategory,
                tagsJson = NoteEntity.formatTagsJson(mergedTags),
                aiSummary = update.changeSummary,
                rawTranscription = rawTranscription ?: existingNote.rawTranscription,
                memoryType = resolvedMemoryType,
                placeId = resolvedPlaceId,
                updatedAt = maxOf(System.currentTimeMillis(), existingNote.createdAt + 10_000L)
            )

            noteDao.insertNoteWithPeople(updatedNoteEntity, linkedPersonIds.distinct())

            if (!audioFilePath.isNullOrBlank()) {
                val audioMedia = MediaEntity(
                    id = UUID.randomUUID().toString(),
                    noteId = updatedNoteEntity.id,
                    mediaType = MediaType.AUDIO,
                    localUri = audioFilePath,
                    ocrText = rawTranscription ?: update.changeSummary,
                    aiDescription = "Debrief Update Recording: ${update.changeSummary}",
                    capturedAt = System.currentTimeMillis()
                )
                mediaDao.insertMedia(audioMedia)
            }

            updatedCount++
        }
        updatedCount
    }

    /**
     * Atomically commits a Daily Debrief triage result into the Room database,
     * including in-place reconciliation updates to existing notes (`noteUpdates`).
     */
    suspend fun commitDailyDebrief(result: DailyDebriefResult): Int = withContext(Dispatchers.IO) {
        var itemsCommitted = 0

        // 1. Process People Updates (with stateful fact reconciliation)
        val personNameToIdMap = mutableMapOf<String, String>()

        for (update in result.peopleUpdates.filter { it.isIncluded }) {
            val cleanName = update.personName.trim()
            if (!MemoryReconciliationEngine.isValidPersonName(cleanName)) continue
            val existing = personDao.findPersonByName(cleanName)
            if (existing != null) {
                val currentFacts = converters.toStringList(existing.factsJson)
                val reconciledFacts = MemoryReconciliationEngine.reconcilePersonFacts(
                    existingFacts = currentFacts,
                    incomingFacts = update.newFacts,
                    personName = existing.name
                )
                val mergedFacts = mergeAndDeduplicateFacts(emptyList(), reconciledFacts, existing.name)

                val currentDates = converters.toStringMap(existing.keyDatesJson)
                val incomingDates = update.datesToRemember.associate { it.label to it.dateIso }
                val mergedDates = mergeAndDeduplicateDates(currentDates, incomingDates)

                val updatedPerson = existing.copy(
                    relationship = update.relationshipHint ?: existing.relationship,
                    factsJson = converters.fromStringList(mergedFacts),
                    keyDatesJson = converters.fromStringMap(mergedDates),
                    lastContactedAt = System.currentTimeMillis()
                )
                personDao.updatePerson(updatedPerson)
                personNameToIdMap[cleanName.lowercase()] = existing.id
            } else {
                val deduplicatedFacts = mergeAndDeduplicateFacts(emptyList(), update.newFacts, cleanName)
                val datesMap = mergeAndDeduplicateDates(
                    emptyMap(),
                    update.datesToRemember.associate { it.label to it.dateIso }
                )
                val newPerson = PersonEntity(
                    id = UUID.randomUUID().toString(),
                    name = cleanName,
                    relationship = update.relationshipHint ?: "Contact",
                    factsJson = converters.fromStringList(deduplicatedFacts),
                    keyDatesJson = converters.fromStringMap(datesMap),
                    lastContactedAt = System.currentTimeMillis()
                )
                personDao.insertPerson(newPerson)
                personNameToIdMap[cleanName.lowercase()] = newPerson.id
            }
            itemsCommitted++
        }

        // 2. Process In-Place Updates to Existing Notes (`noteUpdates`)
        if (result.noteUpdates.isNotEmpty()) {
            val updatedNotesCount = applyNoteUpdates(
                updates = result.noteUpdates,
                audioFilePath = result.audioFilePath,
                rawTranscription = result.rawTranscription ?: result.dailySummary
            )
            itemsCommitted += updatedNotesCount
        }

        // 3. Process Distinct New Notes (with content-based deduplication so notes with different headings & same body never duplicate)
        var primaryCreatedNoteId: String? = null
        val uniqueIncludedNotes = result.distinctNotes
            .filter { it.isIncluded }
            .distinctBy {
                MemoryReconciliationEngine.normalizeNoteContentForDedup(it.content).ifBlank { it.title.lowercase().trim() }
            }

        for (parsedNote in uniqueIncludedNotes) {
            // Resolve involved people (strictly validated)
            val linkedPersonIds = mutableListOf<String>()
            for (rawName in parsedNote.peopleInvolved) {
                val name = rawName.trim()
                if (!MemoryReconciliationEngine.isValidPersonName(name)) continue
                val cachedId = personNameToIdMap[name.lowercase()]
                if (cachedId != null) {
                    linkedPersonIds.add(cachedId)
                } else {
                    val found = personDao.findPersonByName(name)
                    if (found != null) {
                        linkedPersonIds.add(found.id)
                    } else {
                        val newPerson = PersonEntity(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            relationship = "Acquaintance",
                            lastContactedAt = System.currentTimeMillis()
                        )
                        personDao.insertPerson(newPerson)
                        personNameToIdMap[name.lowercase()] = newPerson.id
                        linkedPersonIds.add(newPerson.id)
                    }
                }
            }

            // Check if a note with the exact same normalized content already exists in DB
            val existingDuplicate = noteDao.getAllNotesWithDetails().first().firstOrNull { existing ->
                MemoryReconciliationEngine.areNoteContentsEquivalent(existing.note.content, parsedNote.content)
            }

            if (existingDuplicate != null) {
                if (primaryCreatedNoteId == null) primaryCreatedNoteId = existingDuplicate.note.id
                val bestTitle = chooseBestTitle(existingDuplicate.note.title, parsedNote.title, parsedNote.content)
                val mergedTags = NoteEntity.mergeTagsWithLocation(
                    (existingDuplicate.note.getDisplayTagsList() + parsedNote.tags).distinct(),
                    existingDuplicate.note.getLocationName()
                )
                val mergedPeople = (existingDuplicate.people.map { it.id } + linkedPersonIds).distinct()
                val updatedNote = existingDuplicate.note.copy(
                    title = bestTitle,
                    tagsJson = NoteEntity.formatTagsJson(mergedTags),
                    updatedAt = System.currentTimeMillis()
                )
                noteDao.insertNoteWithPeople(updatedNote, mergedPeople)
                itemsCommitted++
                continue
            }

            val noteId = UUID.randomUUID().toString()
            if (primaryCreatedNoteId == null) primaryCreatedNoteId = noteId
            val now = System.currentTimeMillis()
            val inferredType = if (parsedNote.memoryType.isNotBlank() && parsedNote.memoryType != "PERSONAL_OBSERVATIONS") {
                parsedNote.memoryType
            } else {
                UnifiedMemoryType.inferFromLegacyCategory(
                    category = parsedNote.category,
                    hasAudio = !result.audioFilePath.isNullOrBlank() || !result.rawTranscription.isNullOrBlank(),
                    hasLocation = parsedNote.tags.any { it.startsWith("loc:", ignoreCase = true) },
                    contentHint = "${parsedNote.title} ${parsedNote.content}"
                ).name
            }
            val note = NoteEntity(
                id = noteId,
                title = parsedNote.title,
                content = parsedNote.content,
                category = parsedNote.category,
                tagsJson = NoteEntity.formatTagsJson(parsedNote.tags),
                rawTranscription = result.rawTranscription ?: result.dailySummary,
                aiSummary = result.dailySummary.take(200),
                createdAt = now,
                updatedAt = now,
                memoryType = inferredType,
                occurredAt = now,
                sourceType = parsedNote.sourceType.ifBlank {
                    if (!result.rawTranscription.isNullOrBlank()) MemorySourceType.AUDIO_TRANSCRIPT.name else MemorySourceType.USER_EXPLICIT.name
                },
                confidenceScore = parsedNote.confidenceScore,
                verificationStatus = parsedNote.verificationStatus.ifBlank { MemoryVerificationStatus.VERIFIED_FACT.name }
            )

            val resolvedPlaceId = resolveOrSyncPlaceId(note.getLocationName(), note.placeId)
            noteDao.insertNoteWithPeople(note.copy(placeId = resolvedPlaceId), linkedPersonIds.distinct())
            if (!result.audioFilePath.isNullOrBlank()) {
                val audioMedia = MediaEntity(
                    id = UUID.randomUUID().toString(),
                    noteId = note.id,
                    mediaType = MediaType.AUDIO,
                    localUri = result.audioFilePath,
                    ocrText = result.rawTranscription ?: result.dailySummary,
                    aiDescription = "Daily Debrief Audio Recording",
                    capturedAt = System.currentTimeMillis()
                )
                mediaDao.insertMedia(audioMedia)
            }
            itemsCommitted++
        }

        // 4. Process Action Items
        for (item in result.actionItems.filter { it.isIncluded }) {
            val actionItem = ActionItemEntity(
                id = UUID.randomUUID().toString(),
                noteId = primaryCreatedNoteId,
                task = item.task,
                isCompleted = false,
                createdAt = System.currentTimeMillis()
            )
            actionItemDao.insertActionItem(actionItem)
            itemsCommitted++
        }

        // 5. Process Extracted Commitments
        for (commitment in result.commitments.filter { it.isIncluded }) {
            val resolvedPersonId = commitment.personName?.let { pName ->
                personNameToIdMap[pName.lowercase()] ?: personDao.findPersonByName(pName)?.id
            }
            val verStatus = if (commitment.confidenceScore < 0.80f) {
                MemoryVerificationStatus.AI_INFERRED_PENDING.name
            } else {
                MemoryVerificationStatus.VERIFIED_FACT.name
            }
            commitmentDao.insertCommitment(
                CommitmentEntity(
                    id = UUID.randomUUID().toString(),
                    noteId = primaryCreatedNoteId,
                    personId = resolvedPersonId,
                    description = commitment.description,
                    direction = commitment.direction,
                    isCompleted = false,
                    sourceType = if (!result.rawTranscription.isNullOrBlank()) MemorySourceType.AUDIO_TRANSCRIPT.name else MemorySourceType.USER_EXPLICIT.name,
                    confidenceScore = commitment.confidenceScore,
                    verificationStatus = verStatus,
                    createdAt = System.currentTimeMillis()
                )
            )
            itemsCommitted++
        }

        itemsCommitted
    }

    suspend fun addActionItem(task: String, dueDate: Long? = null) = withContext(Dispatchers.IO) {
        if (task.isBlank()) return@withContext
        actionItemDao.insertActionItem(
            ActionItemEntity(
                id = UUID.randomUUID().toString(),
                noteId = null,
                task = task.trim(),
                dueDate = dueDate,
                isCompleted = false,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * Purges any legacy mock/sample seed records so the app starts completely fresh,
     * and non-destructively backfills Unified Memory metadata (places & memoryType) for existing notes.
     */
    suspend fun purgeMockDataAndEnsureFresh() = withContext(Dispatchers.IO) {
        val mockNoteIds = listOf(
            "note_sarah_vector",
            "note_neurosync_pilot",
            "note_elena_deep_work",
            "note_family_offsite",
            "note_visual_whiteboard",
            "note_john_project_proposal",
            "note_dog_summer_vacation"
        )
        val mockPersonIds = listOf(
            "person_sarah_chen",
            "person_marcus_vance",
            "person_elena_rostova",
            "person_john_reynolds"
        )
        val mockActionIds = listOf(
            "action_1",
            "action_2",
            "action_3",
            "action_4",
            "action_john_proposal"
        )
        val mockMediaIds = listOf(
            "media_visual_whiteboard_1",
            "media_dog_summer_park",
            "media_dog_summer_lake"
        )

        for (id in mockNoteIds) {
            noteDao.deleteNoteById(id)
            noteDao.deleteCrossRefsForNote(id)
        }
        for (id in mockPersonIds) {
            personDao.deletePersonById(id)
        }
        for (id in mockActionIds) {
            actionItemDao.deleteActionItemById(id)
        }
        for (id in mockMediaIds) {
            mediaDao.deleteMediaById(id)
        }

        // Backfill PlaceEntity records and canonical memoryType for existing user notes
        try {
            val existingNotes = noteDao.getAllNotesRaw()
            for (note in existingNotes) {
                val locName = note.getLocationName()
                val placeId = if (!locName.isNullOrBlank() && note.placeId.isNullOrBlank()) {
                    resolveOrSyncPlaceId(locName, null)
                } else {
                    note.placeId
                }
                val inferredType = if (note.memoryType == "PERSONAL_OBSERVATIONS") {
                    UnifiedMemoryType.inferFromLegacyCategory(
                        category = note.category,
                        hasAudio = !note.rawTranscription.isNullOrBlank(),
                        hasLocation = placeId != null,
                        contentHint = "${note.title} ${note.content}"
                    ).name
                } else {
                    note.memoryType
                }
                val occurred = if (note.occurredAt <= 0L) note.createdAt else note.occurredAt
                if (placeId != note.placeId || inferredType != note.memoryType || occurred != note.occurredAt) {
                    noteDao.updateNote(
                        note.copy(
                            placeId = placeId,
                            memoryType = inferredType,
                            occurredAt = occurred
                        )
                    )
                }
            }
        } catch (_: Exception) {
        }

        // Automatically purge any bogus AI-created verb/noun contacts (like "Have", "Saw", "Been")
        // and merge any duplicate notes that share the exact same main content
        cleanupBogusPeopleAndDuplicateNotes()
    }

    /**
     * Purges any invalid contacts whose names are verbs, pronouns, prepositions, or common nouns
     * (e.g. "Have", "Saw", "Been", "Notes", "Meeting") and merges duplicate notes that have
     * different headings/titles but the exact same normalized main content.
     *
     * @return Pair(purgedPeopleCount, mergedDuplicateNotesCount)
     */
    suspend fun cleanupBogusPeopleAndDuplicateNotes(): Pair<Int, Int> = withContext(Dispatchers.IO) {
        var purgedPeopleCount = 0
        var mergedNotesCount = 0

        try {
            val allPeopleList = personDao.getAllPeopleRaw()
            for (person in allPeopleList) {
                if (!MemoryReconciliationEngine.isValidPersonName(person.name)) {
                    noteDao.deleteCrossRefsForPerson(person.id)
                    personDao.deletePersonById(person.id)
                    purgedPeopleCount++
                }
            }
        } catch (_: Exception) {
        }

        try {
            val allNotesDetails = noteDao.getAllNotesWithDetails().first()
            val groupedByContent = allNotesDetails
                .filter { MemoryReconciliationEngine.normalizeNoteContentForDedup(it.note.content).isNotBlank() }
                .groupBy { MemoryReconciliationEngine.normalizeNoteContentForDedup(it.note.content) }

            for ((_, duplicates) in groupedByContent) {
                if (duplicates.size <= 1) continue

                // Sort so the best note is first: prefer pinned, then non-"Untitled Note" title, then most recently updated
                val sorted = duplicates.sortedWith(
                    compareByDescending<NoteWithDetails> { it.note.isPinned }
                        .thenByDescending {
                            val t = it.note.title.trim()
                            t.isNotBlank() && !t.equals("Untitled Note", ignoreCase = true) && !t.startsWith("Daily Debrief:", ignoreCase = true)
                        }
                        .thenByDescending { it.note.updatedAt }
                )

                val keeper = sorted.first()
                val redundantList = sorted.drop(1)

                var bestTitle = keeper.note.title
                val mergedTags = keeper.note.getDisplayTagsList().toMutableList()
                val mergedPersonIds = keeper.people.map { it.id }.toMutableList()
                var isPinned = keeper.note.isPinned
                var latestUpdatedAt = keeper.note.updatedAt

                for (dup in redundantList) {
                    bestTitle = chooseBestTitle(bestTitle, dup.note.title, keeper.note.content)
                    for (t in dup.note.getDisplayTagsList()) {
                        if (!mergedTags.contains(t)) mergedTags.add(t)
                    }
                    for (p in dup.people) {
                        if (MemoryReconciliationEngine.isValidPersonName(p.name) && !mergedPersonIds.contains(p.id)) {
                            mergedPersonIds.add(p.id)
                        }
                    }
                    if (dup.note.isPinned) isPinned = true
                    if (dup.note.updatedAt > latestUpdatedAt) latestUpdatedAt = dup.note.updatedAt

                    // Re-associate media and action items to keeper before deleting duplicate note
                    if (dup.media.isNotEmpty()) {
                        mediaDao.insertMediaList(dup.media.map { it.copy(noteId = keeper.note.id) })
                    }
                    if (dup.actionItems.isNotEmpty()) {
                        actionItemDao.insertActionItems(dup.actionItems.map { it.copy(noteId = keeper.note.id) })
                    }

                    deleteNote(dup.note.id)
                    mergedNotesCount++
                }

                val updatedKeeper = keeper.note.copy(
                    title = bestTitle,
                    tagsJson = NoteEntity.formatTagsJson(
                        NoteEntity.mergeTagsWithLocation(mergedTags.distinct(), keeper.note.getLocationName())
                    ),
                    isPinned = isPinned,
                    updatedAt = latestUpdatedAt
                )
                noteDao.insertNoteWithPeople(updatedKeeper, mergedPersonIds.distinct())
            }
        } catch (_: Exception) {
        }

        Pair(purgedPeopleCount, mergedNotesCount)
    }

    /**
     * Completely wipes all tables for a 100% clean, fresh slate safely without throwing FTS external content exception.
     */
    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        try {
            reminderDao.deleteAllReminders()
            actionItemDao.deleteAllActionItems()
            commitmentDao.deleteAllCommitments()
            memoryLinkDao.deleteAllMemoryLinks()
            noteRevisionDao.deleteAllRevisions()
            placeDao.deleteAllPlaces()
            projectDao.deleteAllProjects()
            mediaDao.deleteAllMedia()
            noteDao.deleteAllCrossRefs()
            noteDao.deleteAllNotes()
            personDao.deleteAllPeople()
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    suspend fun exportJsonBackup(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        val notesArray = JSONArray()
        val peopleArray = JSONArray()
        val actionsArray = JSONArray()
        val placesArray = JSONArray()
        val projectsArray = JSONArray()
        val commitmentsArray = JSONArray()
        val linksArray = JSONArray()
        val revisionsArray = JSONArray()

        val notes = noteDao.getAllNotesRaw()
        for (n in notes) {
            val obj = JSONObject()
            obj.put("id", n.id)
            obj.put("title", n.title)
            obj.put("content", n.content)
            obj.put("category", n.category)
            obj.put("tagsJson", n.tagsJson)
            obj.put("rawTranscription", n.rawTranscription ?: "")
            obj.put("isPinned", n.isPinned)
            obj.put("createdAt", n.createdAt)
            obj.put("updatedAt", n.updatedAt)
            obj.put("aiSummary", n.aiSummary ?: "")
            obj.put("memoryType", n.memoryType)
            obj.put("occurredAt", n.occurredAt)
            obj.put("sourceType", n.sourceType)
            obj.put("confidenceScore", n.confidenceScore.toDouble())
            obj.put("verificationStatus", n.verificationStatus)
            if (n.projectId != null) obj.put("projectId", n.projectId)
            if (n.placeId != null) obj.put("placeId", n.placeId)
            notesArray.put(obj)
        }

        val people = personDao.getAllPeopleRaw()
        for (p in people) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            obj.put("relationship", p.relationship ?: "Contact")
            obj.put("factsJson", p.factsJson)
            obj.put("keyDatesJson", p.keyDatesJson)
            obj.put("lastContactedAt", p.lastContactedAt)
            peopleArray.put(obj)
        }

        val actions = actionItemDao.getAllActionItems().first()
        for (a in actions) {
            val obj = JSONObject()
            obj.put("id", a.id)
            obj.put("task", a.task)
            obj.put("isCompleted", a.isCompleted)
            if (a.dueDate != null) obj.put("dueDate", a.dueDate)
            actionsArray.put(obj)
        }

        val places = placeDao.getAllPlacesRaw()
        for (pl in places) {
            val obj = JSONObject()
            obj.put("id", pl.id)
            obj.put("name", pl.name)
            obj.put("category", pl.category)
            if (pl.address != null) obj.put("address", pl.address)
            if (pl.latitude != null) obj.put("latitude", pl.latitude)
            if (pl.longitude != null) obj.put("longitude", pl.longitude)
            obj.put("notes", pl.notes)
            obj.put("visitCount", pl.visitCount)
            obj.put("lastVisitedAt", pl.lastVisitedAt)
            obj.put("createdAt", pl.createdAt)
            placesArray.put(obj)
        }

        val projects = projectDao.getAllProjectsRaw()
        for (pr in projects) {
            val obj = JSONObject()
            obj.put("id", pr.id)
            obj.put("name", pr.name)
            obj.put("description", pr.description)
            obj.put("status", pr.status)
            obj.put("tagsJson", pr.tagsJson)
            obj.put("createdAt", pr.createdAt)
            obj.put("updatedAt", pr.updatedAt)
            projectsArray.put(obj)
        }

        val commitments = commitmentDao.getAllCommitmentsRaw()
        for (cm in commitments) {
            val obj = JSONObject()
            obj.put("id", cm.id)
            if (cm.noteId != null) obj.put("noteId", cm.noteId)
            if (cm.personId != null) obj.put("personId", cm.personId)
            if (cm.projectId != null) obj.put("projectId", cm.projectId)
            obj.put("description", cm.description)
            obj.put("direction", cm.direction)
            if (cm.dueDate != null) obj.put("dueDate", cm.dueDate)
            obj.put("isCompleted", cm.isCompleted)
            obj.put("sourceType", cm.sourceType)
            obj.put("confidenceScore", cm.confidenceScore.toDouble())
            obj.put("verificationStatus", cm.verificationStatus)
            obj.put("createdAt", cm.createdAt)
            commitmentsArray.put(obj)
        }

        val links = memoryLinkDao.getAllMemoryLinksRaw()
        for (lk in links) {
            val obj = JSONObject()
            obj.put("id", lk.id)
            obj.put("sourceNoteId", lk.sourceNoteId)
            obj.put("targetNoteId", lk.targetNoteId)
            obj.put("relationType", lk.relationType)
            obj.put("strengthScore", lk.strengthScore.toDouble())
            obj.put("createdAt", lk.createdAt)
            linksArray.put(obj)
        }

        val revisions = noteRevisionDao.getAllRevisionsRaw()
        for (rv in revisions) {
            val obj = JSONObject()
            obj.put("id", rv.id)
            obj.put("noteId", rv.noteId)
            obj.put("previousTitle", rv.previousTitle)
            obj.put("previousContent", rv.previousContent)
            obj.put("changeSummary", rv.changeSummary)
            obj.put("sourceType", rv.sourceType)
            obj.put("createdAt", rv.createdAt)
            revisionsArray.put(obj)
        }

        root.put("version", 5)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("notes", notesArray)
        root.put("people", peopleArray)
        root.put("actionItems", actionsArray)
        root.put("places", placesArray)
        root.put("projects", projectsArray)
        root.put("commitments", commitmentsArray)
        root.put("memoryLinks", linksArray)
        root.put("noteRevisions", revisionsArray)
        root.toString(2)
    }

    /**
     * Restores and merges a portable JSON backup into the local Room SQLite database.
     * Supports both legacy v2/v4 backups and v5 Unified Memory System backups.
     */
    suspend fun importJsonBackup(jsonString: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString.trim())
            var importedCount = 0

            val peopleArray = root.optJSONArray("people")
            if (peopleArray != null) {
                for (i in 0 until peopleArray.length()) {
                    val obj = peopleArray.optJSONObject(i) ?: continue
                    val name = obj.optString("name", "").trim()
                    if (name.isBlank()) continue
                    val person = PersonEntity(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = name,
                        relationship = obj.optString("relationship", "Contact").ifBlank { "Contact" },
                        factsJson = obj.optString("factsJson", "[]"),
                        keyDatesJson = obj.optString("keyDatesJson", "{}"),
                        lastContactedAt = obj.optLong("lastContactedAt", System.currentTimeMillis())
                    )
                    savePerson(person)
                    importedCount++
                }
            }

            val placesArray = root.optJSONArray("places")
            if (placesArray != null) {
                for (i in 0 until placesArray.length()) {
                    val obj = placesArray.optJSONObject(i) ?: continue
                    val name = obj.optString("name", "").trim()
                    if (name.isBlank()) continue
                    val place = PlaceEntity(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = name,
                        category = obj.optString("category", "Location"),
                        address = obj.optString("address", "").ifBlank { null },
                        latitude = if (obj.has("latitude") && !obj.isNull("latitude")) obj.optDouble("latitude") else null,
                        longitude = if (obj.has("longitude") && !obj.isNull("longitude")) obj.optDouble("longitude") else null,
                        notes = obj.optString("notes", ""),
                        visitCount = obj.optInt("visitCount", 1),
                        lastVisitedAt = obj.optLong("lastVisitedAt", System.currentTimeMillis()),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                    placeDao.insertPlace(place)
                    importedCount++
                }
            }

            val projectsArray = root.optJSONArray("projects")
            if (projectsArray != null) {
                for (i in 0 until projectsArray.length()) {
                    val obj = projectsArray.optJSONObject(i) ?: continue
                    val name = obj.optString("name", "").trim()
                    if (name.isBlank()) continue
                    val project = ProjectEntity(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = name,
                        description = obj.optString("description", ""),
                        status = obj.optString("status", "ACTIVE"),
                        tagsJson = obj.optString("tagsJson", "[]"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                    projectDao.insertProject(project)
                    importedCount++
                }
            }

            val notesArray = root.optJSONArray("notes")
            if (notesArray != null) {
                for (i in 0 until notesArray.length()) {
                    val obj = notesArray.optJSONObject(i) ?: continue
                    val title = obj.optString("title", "Restored Note")
                    val content = obj.optString("content", "")
                    if (title.isBlank() && content.isBlank()) continue
                    val createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    val category = obj.optString("category", "General")
                    val rawTranscription = obj.optString("rawTranscription", "").ifBlank { null }
                    val defaultMemoryType = UnifiedMemoryType.inferFromLegacyCategory(
                        category = category,
                        hasAudio = !rawTranscription.isNullOrBlank(),
                        hasLocation = false,
                        contentHint = "$title $content"
                    ).name
                    val note = NoteEntity(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = title,
                        content = content,
                        category = category,
                        tagsJson = obj.optString("tagsJson", "[]"),
                        rawTranscription = rawTranscription,
                        aiSummary = obj.optString("aiSummary", "").ifBlank { null },
                        isPinned = obj.optBoolean("isPinned", false),
                        createdAt = createdAt,
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                        memoryType = obj.optString("memoryType", defaultMemoryType).ifBlank { defaultMemoryType },
                        occurredAt = obj.optLong("occurredAt", createdAt),
                        sourceType = obj.optString("sourceType", "USER_EXPLICIT").ifBlank { "USER_EXPLICIT" },
                        confidenceScore = obj.optDouble("confidenceScore", 1.0).toFloat(),
                        verificationStatus = obj.optString("verificationStatus", "VERIFIED_FACT").ifBlank { "VERIFIED_FACT" },
                        projectId = obj.optString("projectId", "").ifBlank { null },
                        placeId = obj.optString("placeId", "").ifBlank { null }
                    )
                    val resolvedPlaceId = resolveOrSyncPlaceId(note.getLocationName(), note.placeId)
                    noteDao.insertNote(note.copy(placeId = resolvedPlaceId))
                    importedCount++
                }
            }

            val actionsArray = root.optJSONArray("actionItems")
            if (actionsArray != null) {
                for (i in 0 until actionsArray.length()) {
                    val obj = actionsArray.optJSONObject(i) ?: continue
                    val task = obj.optString("task", "").trim()
                    if (task.isBlank()) continue
                    val dueDate = if (obj.has("dueDate") && !obj.isNull("dueDate")) obj.optLong("dueDate") else null
                    val action = ActionItemEntity(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        noteId = null,
                        task = task,
                        dueDate = dueDate,
                        isCompleted = obj.optBoolean("isCompleted", false),
                        createdAt = System.currentTimeMillis()
                    )
                    actionItemDao.insertActionItem(action)
                    importedCount++
                }
            }

            val commitmentsArray = root.optJSONArray("commitments")
            if (commitmentsArray != null) {
                for (i in 0 until commitmentsArray.length()) {
                    val obj = commitmentsArray.optJSONObject(i) ?: continue
                    val desc = obj.optString("description", "").trim()
                    if (desc.isBlank()) continue
                    val commitment = CommitmentEntity(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        noteId = obj.optString("noteId", "").ifBlank { null },
                        personId = obj.optString("personId", "").ifBlank { null },
                        projectId = obj.optString("projectId", "").ifBlank { null },
                        description = desc,
                        direction = obj.optString("direction", "BY_ME"),
                        dueDate = if (obj.has("dueDate") && !obj.isNull("dueDate")) obj.optLong("dueDate") else null,
                        isCompleted = obj.optBoolean("isCompleted", false),
                        sourceType = obj.optString("sourceType", "USER_EXPLICIT"),
                        confidenceScore = obj.optDouble("confidenceScore", 1.0).toFloat(),
                        verificationStatus = obj.optString("verificationStatus", "VERIFIED_FACT"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                    commitmentDao.insertCommitment(commitment)
                    importedCount++
                }
            }

            val linksArray = root.optJSONArray("memoryLinks")
            if (linksArray != null) {
                for (i in 0 until linksArray.length()) {
                    val obj = linksArray.optJSONObject(i) ?: continue
                    val sourceId = obj.optString("sourceNoteId", "").trim()
                    val targetId = obj.optString("targetNoteId", "").trim()
                    if (sourceId.isBlank() || targetId.isBlank()) continue
                    val link = MemoryLinkEntity(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        sourceNoteId = sourceId,
                        targetNoteId = targetId,
                        relationType = obj.optString("relationType", "RELATED_TO"),
                        strengthScore = obj.optDouble("strengthScore", 1.0).toFloat(),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                    memoryLinkDao.insertMemoryLink(link)
                    importedCount++
                }
            }

            val revisionsArray = root.optJSONArray("noteRevisions")
            if (revisionsArray != null) {
                for (i in 0 until revisionsArray.length()) {
                    val obj = revisionsArray.optJSONObject(i) ?: continue
                    val noteId = obj.optString("noteId", "").trim()
                    if (noteId.isBlank()) continue
                    val rev = NoteRevisionEntity(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        noteId = noteId,
                        previousTitle = obj.optString("previousTitle", ""),
                        previousContent = obj.optString("previousContent", ""),
                        changeSummary = obj.optString("changeSummary", ""),
                        sourceType = obj.optString("sourceType", "USER_EXPLICIT"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                    noteRevisionDao.insertRevision(rev)
                    importedCount++
                }
            }

            Result.success(importedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Reminder Operations ---
    suspend fun insertReminder(reminder: com.example.data.local.entity.ReminderEntity) = withContext(Dispatchers.IO) {
        reminderDao.insertReminder(reminder)
    }

    suspend fun updateReminder(reminder: com.example.data.local.entity.ReminderEntity) = withContext(Dispatchers.IO) {
        reminderDao.updateReminder(reminder)
    }

    suspend fun deleteReminder(reminder: com.example.data.local.entity.ReminderEntity) = withContext(Dispatchers.IO) {
        reminderDao.deleteReminder(reminder)
    }

    suspend fun deleteReminderById(id: String) = withContext(Dispatchers.IO) {
        reminderDao.deleteReminderById(id)
    }

    suspend fun deleteRemindersForNote(noteId: String) = withContext(Dispatchers.IO) {
        reminderDao.deleteRemindersForNote(noteId)
    }

    suspend fun setReminderCompleted(id: String, isCompleted: Boolean) = withContext(Dispatchers.IO) {
        reminderDao.setCompleted(id, isCompleted)
    }

    suspend fun snoozeReminder(id: String, newTimeMillis: Long) = withContext(Dispatchers.IO) {
        reminderDao.snoozeReminder(id, newTimeMillis)
    }

    suspend fun getReminderById(id: String): com.example.data.local.entity.ReminderEntity? = withContext(Dispatchers.IO) {
        reminderDao.getReminderById(id)
    }
}
