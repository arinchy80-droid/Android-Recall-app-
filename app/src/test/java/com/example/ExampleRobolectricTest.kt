package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.RecallDatabase
import com.example.data.local.converter.RoomConverters
import com.example.data.local.entity.ActionItemEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.PersonEntity
import com.example.data.repository.RecallRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: RecallDatabase
    private lateinit var repository: RecallRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, RecallDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RecallRepository(db)
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Recall", appName)
    }

    @Test
    fun testRoomDatabaseTransactionsAndCrossRef() = runBlocking {
        // Insert person
        val converters = RoomConverters()
        val person = PersonEntity(
            id = "person_sarah",
            name = "Sarah Chen",
            relationship = "Tech Lead",
            factsJson = converters.fromStringList(listOf("Prefers Earl Grey", "Daughter Maya"))
        )
        repository.savePerson(person)

        // Insert note with person binding
        val note = NoteEntity(
            id = "note_1",
            title = "Lunch with Sarah",
            content = "Discussed Q4 roadmap and vector store latency.",
            category = "Meeting"
        )
        repository.saveNote(note, listOf(person.id))

        // Verify retrieval
        val notes = repository.allNotes.first()
        assertEquals(1, notes.size)
        assertEquals("Lunch with Sarah", notes[0].note.title)
        assertEquals(1, notes[0].people.size)
        assertEquals("Sarah Chen", notes[0].people[0].name)

        // Verify person notes query
        val notesForSarah = repository.getNotesForPerson("person_sarah").first()
        assertEquals(1, notesForSarah.size)

        // Test clearAllData
        repository.clearAllData()
        val emptyNotes = repository.allNotes.first()
        assertEquals(0, emptyNotes.size)
        val emptyPeople = repository.allPeople.first()
        assertEquals(0, emptyPeople.size)
    }

    @Test
    fun testLocalFallbackCleanUpAndSmartTags() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val geminiService = com.example.data.remote.GeminiService(context)

        // 1. Verify text cleanup fixes speech filler words and casing
        val raw = "um so basically we met with alice to discuss q4 revenue and budgets"
        val cleaned = geminiService.localFallbackCleanUp(raw)
        assertTrue(cleaned.contains("Alice", ignoreCase = true))
        assertTrue(!cleaned.startsWith("um"))

        // 2. Verify smart tagging extracts title, category, and tags
        val smartResult = geminiService.localFallbackSmartTag(
            text = "Met with Alice regarding Q4 revenue goals and budgeting roadmap",
            existingPeople = listOf("Alice")
        )
        assertNotNull(smartResult.title)
        assertTrue(smartResult.title.isNotBlank())
        assertTrue(com.example.data.model.MemoryCategory.allDisplayNames.contains(smartResult.category))
        assertTrue(smartResult.mentionedPeople.contains("Alice"))
        assertTrue(smartResult.tags.isNotEmpty())
    }

    @Test
    fun testMainActivityStartupDoesNotCrash() {
        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        assertNotNull(activity)
    }

    @Test
    fun testMemoryReconciliationEngine_JasonDebtDeductionScenario() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val geminiService = com.example.data.remote.GeminiService(context)

        // Day 1: User records "I owe Jason 600 dollars"
        val day1Debrief = geminiService.processDailyDebrief(
            transcribedText = "I owe Jason 600 dollars",
            existingNotes = emptyList(),
            existingPeople = emptyList()
        ).getOrThrow()

        repository.commitDailyDebrief(day1Debrief)

        val notesAfterDay1 = repository.allNotes.first()
        assertEquals(1, notesAfterDay1.size)
        val originalNoteId = notesAfterDay1.first().note.id
        assertTrue(notesAfterDay1.first().note.content.contains("600"))

        val peopleAfterDay1 = repository.allPeople.first()
        assertTrue(peopleAfterDay1.any { it.name.equals("Jason", ignoreCase = true) })

        // Day 2: User says "I paid Jason $200"
        val day2Debrief = geminiService.processDailyDebrief(
            transcribedText = "I paid Jason $200",
            existingNotes = notesAfterDay1,
            existingPeople = peopleAfterDay1
        ).getOrThrow()

        // Verify reconciliation engine detected the in-place update on the existing note
        assertEquals(1, day2Debrief.noteUpdates.size)
        val noteUpdate = day2Debrief.noteUpdates.first()
        assertEquals(originalNoteId, noteUpdate.targetNoteId)
        assertTrue("Updated title should reflect 400: ${noteUpdate.updatedTitle}", noteUpdate.updatedTitle.contains("400"))
        assertTrue("Updated content should reflect 400: ${noteUpdate.updatedContent}", noteUpdate.updatedContent.contains("400"))

        // Commit Day 2 debrief
        repository.commitDailyDebrief(day2Debrief)

        // Verify the existing note in Room was updated in-place (not duplicated) and reflects $400
        val notesAfterDay2 = repository.allNotes.first()
        assertEquals(1, notesAfterDay2.size)
        val updatedNote = notesAfterDay2.first().note
        assertEquals(originalNoteId, updatedNote.id)
        assertTrue("Saved note title should contain 400: ${updatedNote.title}", updatedNote.title.contains("400"))
        assertTrue("Saved note content should contain $400 and update history: ${updatedNote.content}", updatedNote.content.contains("400") && updatedNote.content.contains("Update History"))

        // Verify Jason's profile facts were also updated to reflect $400
        val jasonAfterDay2 = repository.allPeople.first().first { it.name.equals("Jason", ignoreCase = true) }
        assertTrue("Jason's facts should reflect 400: ${jasonAfterDay2.factsJson}", jasonAfterDay2.factsJson.contains("400"))
    }

    @Test
    fun testMemoryReconciliationEngine_FactualStatusUpdateScenario() = runBlocking {
        val jason = PersonEntity(
            id = "p_jason",
            name = "Jason",
            relationship = "Contractor"
        )
        repository.savePerson(jason)

        val initialNote = NoteEntity(
            id = "n_project",
            title = "Kitchen Renovation with Jason",
            content = "Meeting with Jason on Tuesday at 2pm to finalize kitchen cabinets.",
            category = "Projects"
        )
        repository.saveNote(initialNote, listOf(jason.id))

        val existingNotes = repository.allNotes.first()
        val updates = com.example.util.MemoryReconciliationEngine.findNoteUpdatesForInput(
            newInputText = "Update: moved meeting with Jason for kitchen cabinets to Friday at 4pm",
            existingNotes = existingNotes,
            knownPeopleNames = listOf("Jason")
        )

        assertEquals(1, updates.size)
        repository.applyNoteUpdates(updates)

        val updatedNote = repository.getNoteByIdOnce("n_project")!!.note
        assertTrue(updatedNote.content.contains("Friday at 4pm"))
        assertTrue(updatedNote.content.contains("Update History"))
    }

    @Test
    fun testPhase1To4_UnifiedMemoryGraphRevisionAndBackupCUJ() = runBlocking {
        val converters = RoomConverters()

        // 1. Create Person & Project
        val person = PersonEntity(
            id = "p_marcus",
            name = "Marcus Vance",
            relationship = "Co-Founder",
            factsJson = converters.fromStringList(listOf("Focuses on edge AI"))
        )
        repository.savePerson(person)

        val project = com.example.data.local.entity.ProjectEntity(
            id = "proj_hyperion",
            name = "Project Hyperion",
            description = "Next-gen neural memory architecture",
            status = "ACTIVE"
        )
        repository.saveProject(project)

        // 2. Save two notes sharing Project Hyperion and location tag `loc:Perth Innovation Hub`
        val note1 = NoteEntity(
            id = "note_hyp_1",
            title = "Hyperion Kickoff",
            content = "Discussed vector indexing with Marcus at Perth Innovation Hub.",
            category = "Projects",
            memoryType = com.example.data.model.UnifiedMemoryType.PROJECTS.name,
            projectId = project.id,
            tagsJson = converters.fromStringList(listOf("loc:Perth Innovation Hub", "architecture"))
        )
        repository.saveNote(note1, listOf(person.id))

        val note2 = NoteEntity(
            id = "note_hyp_2",
            title = "Hyperion Latency Benchmarks",
            content = "Reviewed sub-millisecond query results at Perth Innovation Hub.",
            category = "Projects",
            memoryType = com.example.data.model.UnifiedMemoryType.PROJECTS.name,
            projectId = project.id,
            tagsJson = converters.fromStringList(listOf("loc:Perth Innovation Hub", "benchmarks"))
        )
        repository.saveNote(note2, listOf(person.id))

        // Verify automatic PlaceEntity creation & visitCount increment
        val places = repository.allPlaces.first()
        val hubPlace = places.find { it.name.equals("Perth Innovation Hub", ignoreCase = true) }
        assertNotNull("PlaceEntity should be auto-synced from loc: tag", hubPlace)
        assertTrue("Place visitCount should be at least 2", (hubPlace?.visitCount ?: 0) >= 2)

        // Verify automatic MemoryLinkEntity graph edge discovery between note1 and note2
        val links = repository.allMemoryLinks.first()
        assertTrue(
            "Auto-discovered MemoryLinkEntity should connect note_hyp_1 and note_hyp_2",
            links.any {
                (it.sourceNoteId == "note_hyp_1" && it.targetNoteId == "note_hyp_2") ||
                    (it.sourceNoteId == "note_hyp_2" && it.targetNoteId == "note_hyp_1")
            }
        )

        // 3. Edit note1 and verify non-destructive NoteRevisionEntity snapshot + restoration
        val editedNote1 = repository.getNoteByIdOnce("note_hyp_1")!!.note.copy(
            title = "Hyperion Kickoff (Updated)",
            content = "Updated architecture notes with quantization targets."
        )
        repository.saveNote(editedNote1, listOf(person.id))

        val revisions = repository.getRevisionsForNote("note_hyp_1").first()
        assertTrue("Editing note should snapshot a NoteRevisionEntity", revisions.isNotEmpty())
        assertEquals("Hyperion Kickoff", revisions.first().previousTitle)

        // Restore snapshot and verify non-destructive rollback
        val restored = repository.restoreNoteRevision(editedNote1, revisions.first())
        assertEquals("Hyperion Kickoff", restored.title)
        assertTrue(restored.content.contains("Discussed vector indexing"))

        // 4. Verify Portable v5 JSON Backup Export & Restore
        val exportedJson = repository.exportJsonBackup()
        assertTrue(exportedJson.contains("Project Hyperion"))
        assertTrue(exportedJson.contains("Perth Innovation Hub"))

        repository.clearAllData()
        assertEquals(0, repository.allNotes.first().size)

        val importedCount = repository.importJsonBackup(exportedJson).getOrThrow()
        assertTrue("JSON restore should import saved entities", importedCount > 0)
        assertEquals(2, repository.allNotes.first().size)
        assertEquals(1, repository.allProjects.first().size)
    }

    @Test
    fun testPhase2_ProvenanceGateSeparatesVerifiedFactsFromAiInterpretationsCUJ() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val geminiService = com.example.data.remote.GeminiService(context)

        // Process debrief containing both a concrete commitment and a speculative observation
        val debrief = geminiService.processDailyDebrief(
            transcribedText = "I promised Chloe I would send the Q3 contract by Friday. Maybe Chloe is thinking about relocating to Sydney.",
            existingNotes = emptyList(),
            existingPeople = emptyList()
        ).getOrThrow()

        repository.commitDailyDebrief(debrief)

        val savedNotes = repository.allNotes.first()
        assertTrue(savedNotes.isNotEmpty())

        // Insert an explicit AI_INFERRED_PENDING note and verify confirmation workflow
        val speculativeNote = NoteEntity(
            id = "note_speculative_1",
            title = "Chloe Relocation Possibility",
            content = "Maybe Chloe is thinking about relocating to Sydney.",
            category = "People",
            memoryType = com.example.data.model.UnifiedMemoryType.PEOPLE.name,
            sourceType = com.example.data.model.MemorySourceType.AI_INTERPRETATION.name,
            confidenceScore = 0.62f,
            verificationStatus = com.example.data.model.MemoryVerificationStatus.AI_INFERRED_PENDING.name
        )
        repository.saveNote(speculativeNote, emptyList())

        val beforeConfirm = repository.getNoteByIdOnce("note_speculative_1")!!.note
        assertEquals(
            com.example.data.model.MemoryVerificationStatus.AI_INFERRED_PENDING.name,
            beforeConfirm.verificationStatus
        )

        // Confirm as Verified Fact
        repository.verifyMemoryFact(beforeConfirm)
        val afterConfirm = repository.getNoteByIdOnce("note_speculative_1")!!.note
        assertEquals(
            com.example.data.model.MemoryVerificationStatus.VERIFIED_FACT.name,
            afterConfirm.verificationStatus
        )
        assertEquals(1.0f, afterConfirm.confidenceScore, 0.01f)
    }

    @Test
    fun testPhase5And6_ProactiveBriefingLifeIntelligenceAndNotificationsCUJ() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val now = System.currentTimeMillis()

        val person = PersonEntity(
            id = "p_liam",
            name = "Liam O'Connor",
            relationship = "Partner",
            lastContactedAt = now - (40L * 24L * 3600L * 1000L) // Dormant > 30 days
        )
        repository.savePerson(person)

        val commitment = com.example.data.local.entity.CommitmentEntity(
            id = "comm_liam_1",
            personId = person.id,
            description = "Deliver signed patent filing to Liam",
            direction = "BY_ME",
            isCompleted = false,
            createdAt = now - 3_600_000L
        )
        repository.saveCommitment(commitment)

        val allNotes = repository.allNotes.first()
        val allPeople = repository.allPeople.first()
        val allCommitments = repository.allCommitments.first()

        val briefing = com.example.util.MemoryIntelligenceHelper.buildAnticipatoryBriefing(
            allNotes = allNotes,
            allPeople = allPeople,
            allActions = emptyList(),
            allCommitments = allCommitments,
            allProjects = emptyList()
        )
        assertEquals(1, briefing.openCommitmentsCount)
        assertTrue(briefing.overduePeopleCount >= 1)

        // Verify notification helper executes cleanly under Robolectric
        com.example.util.NotificationHelper.showProactiveBriefingNotification(
            context = context,
            title = briefing.greetingTitle,
            summaryText = briefing.executiveSummary,
            playToneAsync = false
        )
    }

    @Test
    fun testPhase7_PrivacySecurityManagerContextPersistenceAndAudioPurgeCUJ() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        com.example.util.PrivacySecurityManager.ensureInitialized(context)

        // Enable PIN lock with custom PIN 4321
        com.example.util.PrivacySecurityManager.setAppLockEnabled(context, true, "4321")
        assertTrue(com.example.util.PrivacySecurityManager.isAppLockEnabled.value)
        assertTrue(com.example.util.PrivacySecurityManager.verifyPin("4321"))

        // Lock and unlock with Context
        com.example.util.PrivacySecurityManager.lockSessionNow()
        assertTrue(!com.example.util.PrivacySecurityManager.isSessionUnlocked.value)
        assertTrue(com.example.util.PrivacySecurityManager.unlockSession(context, "4321"))
        assertTrue(com.example.util.PrivacySecurityManager.isSessionUnlocked.value)

        // Create a dummy raw audio file in cacheDir and verify purgeAllRawAudioFiles(context)
        val dummyVoice = java.io.File(context.cacheDir, "debrief_cuj_test.m4a").apply {
            writeText("test_audio_content")
        }
        val statsBefore = com.example.util.PrivacySecurityManager.getAudioStorageStats(context)
        assertTrue(statsBefore.fileCount >= 1)

        val deletedCount = com.example.util.PrivacySecurityManager.purgeAllRawAudioFiles(context)
        assertTrue(deletedCount >= 1)
        assertTrue(!dummyVoice.exists())
        assertTrue(com.example.util.PrivacySecurityManager.auditLogs.value.isNotEmpty())

        // Reset app lock state for clean test isolation
        com.example.util.PrivacySecurityManager.setAppLockEnabled(context, false, "1234")
    }

    @Test
    fun testNoBogusVerbContactsAndNoDuplicateNotesWithDifferentHeadingsCUJ() = runBlocking {
        // 1. Verify verbs and common nouns ("have", "saw", "been", "meeting", "notes") are NEVER accepted or extracted as people
        assertTrue(!com.example.util.MemoryReconciliationEngine.isValidPersonName("Have"))
        assertTrue(!com.example.util.MemoryReconciliationEngine.isValidPersonName("Saw"))
        assertTrue(!com.example.util.MemoryReconciliationEngine.isValidPersonName("Been"))
        assertTrue(!com.example.util.MemoryReconciliationEngine.isValidPersonName("Meeting"))
        assertTrue(!com.example.util.MemoryReconciliationEngine.isValidPersonName("Notes"))
        assertTrue(com.example.util.MemoryReconciliationEngine.isValidPersonName("Sarah Chen"))
        assertTrue(com.example.util.MemoryReconciliationEngine.isValidPersonName("Jason"))

        val extracted = com.example.util.MemoryReconciliationEngine.extractCandidateNames(
            "I need to have a meeting today. I have been working on notes and saw a great movie with Sarah.",
            emptyList()
        )
        assertTrue("Should not extract Have, Been, or Saw: $extracted", extracted.none {
            it.equals("Have", ignoreCase = true) ||
                it.equals("Been", ignoreCase = true) ||
                it.equals("Saw", ignoreCase = true)
        })
        assertTrue("Should extract Sarah: $extracted", extracted.any { it.equals("Sarah", ignoreCase = true) })

        // 2. Verify saving two notes with different headings and the exact same main content deduplicates into 1 note
        val sharedBody = "Review the quarterly hardware architecture roadmap and finalize procurement."
        val draftNote = NoteEntity(
            id = "note_draft_1",
            title = "Untitled Note",
            content = sharedBody,
            category = "General"
        )
        repository.saveNote(draftNote, emptyList())

        val smartTaggedNote = NoteEntity(
            id = "note_tagged_2",
            title = "Quarterly Hardware Roadmap",
            content = sharedBody,
            category = "Projects"
        )
        repository.saveNote(smartTaggedNote, emptyList())

        val allSavedNotes = repository.allNotes.first()
        assertEquals("Duplicate notes with identical main content must be merged into 1 note", 1, allSavedNotes.size)
        assertEquals("Should retain the more descriptive heading", "Quarterly Hardware Roadmap", allSavedNotes.first().note.title)

        // 3. Verify cleanupBogusPeopleAndDuplicateNotes purges bogus contacts ("Have", "Saw", "Been") while preserving valid contacts
        repository.savePerson(PersonEntity(id = "p_have", name = "Have", relationship = "Contact"))
        repository.savePerson(PersonEntity(id = "p_saw", name = "Saw", relationship = "Contact"))
        repository.savePerson(PersonEntity(id = "p_been", name = "Been", relationship = "Contact"))
        repository.savePerson(PersonEntity(id = "p_valid", name = "Elena Rostova", relationship = "Researcher"))

        val (purgedPeople, _) = repository.cleanupBogusPeopleAndDuplicateNotes()
        assertEquals(3, purgedPeople)

        val remainingPeople = repository.allPeople.first()
        assertEquals(1, remainingPeople.size)
        assertEquals("Elena Rostova", remainingPeople.first().name)

        // 4. Verify deleting an entire person profile removes it cleanly
        repository.deletePerson("p_valid")
        assertTrue(repository.allPeople.first().isEmpty())
    }

    @Test
    fun testPhase8To12_MemoraeOrganiserRecommendationsPreferencesAndGraphLinksCUJ() = runBlocking {
        val now = System.currentTimeMillis()
        val sarah = PersonEntity(
            id = "p_sarah_12",
            name = "Sarah Jenkins",
            relationship = "Partner",
            keyDatesJson = "{\"CatchUpCadenceDays\":\"14\",\"Birthday\":\"Oct 14\"}",
            lastContactedAt = now - (20L * 24L * 3600L * 1000L)
        )
        repository.savePerson(sarah)

        val note1 = NoteEntity(
            id = "n_12_1",
            title = "Q4 Strategy Session",
            content = "I prefer morning strategy reviews before 10am. Promised Sarah I will share the updated forecast.",
            category = "Projects"
        )
        val note2 = NoteEntity(
            id = "n_12_2",
            title = "Forecast Financial Model",
            content = "Completed revenue projection scenarios for Q4.",
            category = "Finance"
        )
        repository.saveNote(note1, listOf(sarah.id))
        repository.saveNote(note2, listOf(sarah.id))

        // 1. Test explicit manual neural edge linking and deletion
        repository.linkMemories("n_12_1", "n_12_2", "EXPANDS", 0.95f)
        val linksAfterCreate = repository.allMemoryLinks.first()
        val manualLink = linksAfterCreate.firstOrNull { it.relationType == "EXPANDS" }
        assertNotNull(manualLink)
        repository.deleteMemoryLink(manualLink!!.id)
        val linksAfterDelete = repository.allMemoryLinks.first()
        assertTrue(linksAfterDelete.none { it.id == manualLink.id })

        // 2. Test Commitment saving & Contextual Recommendations Engine
        val comm = com.example.data.local.entity.CommitmentEntity(
            id = "comm_12_1",
            personId = sarah.id,
            noteId = note1.id,
            description = "I promised Sarah I will share the updated forecast",
            direction = "BY_ME",
            dueDate = null,
            isCompleted = false
        )
        repository.saveCommitment(comm)

        val allNotes = repository.allNotes.first()
        val allPeople = repository.allPeople.first()
        val allCommitments = repository.allCommitments.first()

        val recs = com.example.util.RecallLifeOrganiserManager.buildContextualRecommendations(
            allNotes = allNotes,
            allPeople = allPeople,
            allCommitments = allCommitments,
            allActions = emptyList(),
            allProjects = emptyList(),
            nowMillis = now
        )
        assertTrue(recs.any { it.targetCommitmentId == "comm_12_1" })
        assertTrue(recs.any { it.targetPersonId == sarah.id })
    }
}


