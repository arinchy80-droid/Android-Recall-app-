package com.example

import com.example.data.local.entity.NoteEntity
import com.example.data.model.MemorySourceType
import com.example.data.model.MemoryVerificationStatus
import com.example.data.model.UnifiedMemoryType
import com.example.util.MemoryReconciliationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun unifiedMemoryType_allTwelveCanonicalCategoriesExist() {
        val types = UnifiedMemoryType.values()
        assertEquals(12, types.size)
        assertEquals(
            UnifiedMemoryType.COMMITMENTS,
            UnifiedMemoryType.inferFromLegacyCategory(
                category = "Finance",
                contentHint = "I owe Jason $600 for the car repair"
            )
        )
        assertEquals(
            UnifiedMemoryType.IMPORTANT_DATES,
            UnifiedMemoryType.inferFromLegacyCategory(
                category = "Family",
                contentHint = "Sarah's birthday is on October 14"
            )
        )
        assertEquals(
            UnifiedMemoryType.CONVERSATIONS,
            UnifiedMemoryType.inferFromLegacyCategory(
                category = "Meeting",
                hasAudio = true,
                contentHint = "1-on-1 discussion with Alex"
            )
        )
        assertEquals(
            UnifiedMemoryType.PLACES,
            UnifiedMemoryType.inferFromLegacyCategory(
                category = "Travel",
                hasLocation = true,
                contentHint = "Coffee shop in Perth"
            )
        )
    }

    @Test
    fun noteEntity_unifiedMemoryMetadataAndLocationHelpersWork() {
        val tags = NoteEntity.mergeTagsWithLocation(listOf("work", "roadmap"), "Perth Office")
        val note = NoteEntity(
            id = "test_note_1",
            title = "Q4 Roadmap Sync",
            content = "Discussed product milestones with the team.",
            category = "Projects",
            tagsJson = NoteEntity.formatTagsJson(tags),
            sourceType = MemorySourceType.AUDIO_TRANSCRIPT.name,
            confidenceScore = 0.92f,
            verificationStatus = MemoryVerificationStatus.VERIFIED_FACT.name
        )

        assertEquals("Perth Office", note.getLocationName())
        assertEquals(listOf("work", "roadmap"), note.getDisplayTagsList())
        assertEquals(UnifiedMemoryType.PROJECTS, note.getUnifiedMemoryType())
        assertEquals(MemorySourceType.AUDIO_TRANSCRIPT, note.getMemorySourceType())
        assertEquals(MemoryVerificationStatus.VERIFIED_FACT, note.getMemoryVerificationStatus())
    }

    @Test
    fun livingNotesReconciliation_subtractsPartialPaymentAccurately() {
        val existingNote = com.example.data.model.NoteWithDetails(
            note = NoteEntity(
                id = "note_jason_debt",
                title = "Owe Jason $600",
                content = "I owe Jason $600 for the equipment.",
                category = "Finance"
            )
        )

        val updates = MemoryReconciliationEngine.findNoteUpdatesForInput(
            newInputText = "I paid Jason $200 today",
            existingNotes = listOf(existingNote)
        )

        assertTrue(updates.isNotEmpty())
        val update = updates.first()
        assertEquals("note_jason_debt", update.targetNoteId)
        assertTrue(update.updatedTitle.contains("400") || update.updatedContent.contains("400"))
    }

    @Test
    fun phase2ProvenanceAndCommitmentExtraction_distinguishesExplicitFactsFromSpeculation() {
        val geminiService = com.example.data.remote.GeminiService()

        // 1. Explicit factual statement with commitment
        val factualDebrief = geminiService.fallbackLocalDebriefExtractor(
            transcript = "I met Sarah today at the Perth office. I promised Sarah I would send the Q4 contract by Friday.",
            existingPeople = listOf("Sarah")
        )
        assertTrue(factualDebrief.distinctNotes.isNotEmpty())
        val explicitNote = factualDebrief.distinctNotes.first()
        assertEquals(MemoryVerificationStatus.VERIFIED_FACT.name, explicitNote.verificationStatus)
        assertTrue(explicitNote.confidenceScore >= 0.85f)
        assertTrue(factualDebrief.commitments.isNotEmpty())
        assertEquals("BY_ME", factualDebrief.commitments.first().direction)

        // 2. Speculative / uncertain statement -> must be flagged AI_INFERRED_PENDING, never silently verified
        val speculativeDebrief = geminiService.fallbackLocalDebriefExtractor(
            transcript = "I think David might possibly relocate to Melbourne next month, not sure yet.",
            existingPeople = listOf("David")
        )
        assertTrue(speculativeDebrief.distinctNotes.isNotEmpty())
        val inferredNote = speculativeDebrief.distinctNotes.first()
        assertEquals(MemoryVerificationStatus.AI_INFERRED_PENDING.name, inferredNote.verificationStatus)
        assertEquals(MemorySourceType.AI_INTERPRETATION.name, inferredNote.sourceType)
        assertTrue(inferredNote.confidenceScore < 0.80f)
    }

    @Test
    fun phase3PlacesProjectsCommitmentsEngine_modelsAndLinksWork() {
        val place = com.example.data.local.entity.PlaceEntity(
            id = "place_perth_hq",
            name = "Perth HQ",
            category = "Office",
            address = "St Georges Terrace, Perth",
            visitCount = 4
        )
        val project = com.example.data.local.entity.ProjectEntity(
            id = "proj_recall_v5",
            name = "Recall Platform Rollout",
            description = "Unified memory engine & proactive assistant",
            status = "ACTIVE"
        )
        val commitment = com.example.data.local.entity.CommitmentEntity(
            id = "commit_1",
            description = "Send architecture blueprint to Sarah",
            direction = com.example.data.model.CommitmentDirection.BY_ME.name,
            personId = "person_sarah",
            projectId = project.id,
            isCompleted = false
        )
        val linkedNote = NoteEntity(
            id = "note_proj_place",
            title = "Architecture Review at Perth HQ",
            content = "Reviewed Phase 3 deliverables.",
            category = "Projects",
            projectId = project.id,
            placeId = place.id,
            tagsJson = NoteEntity.formatTagsJson(NoteEntity.mergeTagsWithLocation(listOf("architecture"), place.name))
        )

        assertEquals("Perth HQ", linkedNote.getLocationName())
        assertEquals(project.id, linkedNote.projectId)
        assertEquals(place.id, linkedNote.placeId)
        assertEquals("BY_ME", commitment.direction)
        assertEquals(project.id, commitment.projectId)
    }

    @Test
    fun phase4HybridSearchAndGraphTraversal_retrievesConnectedMemoriesAndCommitments() {
        val sarah = com.example.data.local.entity.PersonEntity(
            id = "p_sarah",
            name = "Sarah Jenkins",
            relationship = "Partner",
            factsJson = "[\"Leads enterprise partnerships\"]"
        )
        val project = com.example.data.local.entity.ProjectEntity(
            id = "proj_alpha",
            name = "Project Alpha",
            description = "Enterprise rollout in Perth",
            status = "ACTIVE"
        )
        val place = com.example.data.local.entity.PlaceEntity(
            id = "place_perth",
            name = "Perth Innovation Hub",
            category = "Office",
            visitCount = 3
        )
        val seedNote = com.example.data.model.NoteWithDetails(
            note = NoteEntity(
                id = "note_seed",
                title = "Kickoff with Sarah",
                content = "Discussed initial timeline for Project Alpha.",
                category = "Meeting",
                projectId = project.id,
                placeId = place.id
            ),
            people = listOf(sarah)
        )
        // Connected note via MemoryLinkEntity (doesn't mention Sarah directly by name, but linked via CONTINUES)
        val connectedNote = com.example.data.model.NoteWithDetails(
            note = NoteEntity(
                id = "note_connected",
                title = "Infrastructure Architecture Specs",
                content = "Finalized server latency targets and database sharding.",
                category = "Projects",
                projectId = project.id
            ),
            people = emptyList()
        )
        val link = com.example.data.local.entity.MemoryLinkEntity(
            id = "link_1",
            sourceNoteId = "note_seed",
            targetNoteId = "note_connected",
            relationType = "CONTINUES",
            strengthScore = 0.95f
        )
        val commitment = com.example.data.local.entity.CommitmentEntity(
            id = "comm_sarah",
            personId = sarah.id,
            projectId = project.id,
            description = "Send Sarah the signed SLA document",
            direction = "BY_ME",
            isCompleted = false
        )

        val nlu = com.example.data.search.SmartSearchEngine.parseQueryLocal(
            query = "What commitments and notes do I have with Sarah?",
            knownPeople = listOf(sarah),
            knownPlaces = listOf(place),
            knownProjects = listOf(project)
        )
        assertEquals("COMMITMENTS", nlu.intent)
        assertTrue(nlu.entities.contains("Sarah Jenkins"))

        val results = com.example.data.search.SmartSearchEngine.filterAndRankMemories(
            allNotes = listOf(seedNote, connectedNote),
            allMedia = emptyList(),
            allPeople = listOf(sarah),
            allActions = emptyList(),
            interpretation = nlu,
            allPlaces = listOf(place),
            allProjects = listOf(project),
            allCommitments = listOf(commitment),
            allMemoryLinks = listOf(link)
        )

        // Should surface the commitment, the seed note, AND the 1-hop graph-linked note
        assertTrue(results.any { it.commitmentEntity?.id == "comm_sarah" })
        assertTrue(results.any { it.noteWithDetails?.note?.id == "note_seed" })
        val hoppedNote = results.firstOrNull { it.noteWithDetails?.note?.id == "note_connected" }
        assertNotNull(hoppedNote)
        assertNotNull(hoppedNote?.graphPath)
    }

    @Test
    fun phase5ProactiveAssistantAndAnticipatoryBriefing_synthesizesInsightsAndMeetingPrep() {
        val now = System.currentTimeMillis()
        val fortyDaysAgo = now - (40L * 24L * 3600L * 1000L)

        val alex = com.example.data.local.entity.PersonEntity(
            id = "p_alex",
            name = "Alex Mercer",
            relationship = "Client",
            factsJson = "[\"Prefers morning briefings\",\"CEO at Mercer Labs\"]",
            keyDatesJson = "{\"CatchUpCadenceDays\":\"14\",\"Product Launch\":\"Nov 15\"}",
            lastContactedAt = fortyDaysAgo
        )
        val project = com.example.data.local.entity.ProjectEntity(
            id = "proj_mercer",
            name = "Mercer AI Rollout",
            description = "Phase 1 enterprise deployment",
            status = "ACTIVE"
        )
        val commitment = com.example.data.local.entity.CommitmentEntity(
            id = "comm_alex_1",
            personId = alex.id,
            projectId = project.id,
            description = "Deliver security architecture deck to Alex",
            direction = "BY_ME",
            isCompleted = false
        )
        val noteWithAlex = com.example.data.model.NoteWithDetails(
            note = NoteEntity(
                id = "note_alex_1",
                title = "Security Sync with Alex",
                content = "Reviewed encryption standards for Mercer AI Rollout.",
                category = "Meeting",
                memoryType = "CONVERSATIONS",
                projectId = project.id
            ),
            people = listOf(alex)
        )
        val actionItem = com.example.data.local.entity.ActionItemEntity(
            id = "act_alex_1",
            noteId = "note_alex_1",
            task = "Email SOC2 compliance summary to Alex",
            isCompleted = false
        )

        // 1. Test Anticipatory Daily Briefing synthesis
        val briefing = com.example.util.MemoryIntelligenceHelper.buildAnticipatoryBriefing(
            allNotes = listOf(noteWithAlex),
            allPeople = listOf(alex),
            allActions = listOf(actionItem),
            allCommitments = listOf(commitment),
            allProjects = listOf(project)
        )

        assertEquals(1, briefing.openCommitmentsCount)
        assertTrue(briefing.upcomingDatesCount >= 1)
        assertEquals(1, briefing.overduePeopleCount)
        assertEquals(1, briefing.activeProjectsCount)
        assertTrue(briefing.insights.any { it.type == com.example.util.ProactiveInsightType.OPEN_COMMITMENT })
        assertTrue(briefing.insights.any { it.type == com.example.util.ProactiveInsightType.UPCOMING_DATE })
        assertTrue(briefing.insights.any { it.type == com.example.util.ProactiveInsightType.DORMANT_RELATIONSHIP })
        assertTrue(briefing.insights.any { it.type == com.example.util.ProactiveInsightType.PROJECT_MOMENTUM })

        // 2. Test upgraded 5-Second Meeting Prep Briefing with commitments & projects
        val meetingPrep = com.example.util.MemoryIntelligenceHelper.generateMeetingPrepBriefing(
            person = alex,
            personNotes = listOf(noteWithAlex),
            allActions = listOf(actionItem),
            allCommitments = listOf(commitment),
            allProjects = listOf(project)
        )

        assertEquals(1, meetingPrep.openCommitments.size)
        assertEquals(1, meetingPrep.sharedProjects.size)
        assertEquals("Mercer AI Rollout", meetingPrep.sharedProjects.first().name)
        assertTrue(meetingPrep.talkingPoints.any { it.contains("Deliver security architecture deck") })
        assertTrue(meetingPrep.talkingPoints.any { it.contains("Mercer AI Rollout") })
    }

    @Test
    fun phase6PersonalLifeIntelligenceAndTimeline_discoversPatternsAndGroupsEpochs() {
        val now = System.currentTimeMillis()
        val tenDaysAgo = now - (10L * 24L * 3600L * 1000L)

        val elena = com.example.data.local.entity.PersonEntity(
            id = "p_elena",
            name = "Elena Rostova",
            relationship = "Co-founder"
        )
        val project = com.example.data.local.entity.ProjectEntity(
            id = "proj_nexus",
            name = "Project Nexus",
            description = "Unified memory graph architecture",
            status = "ACTIVE"
        )
        val place = com.example.data.local.entity.PlaceEntity(
            id = "place_kings_park",
            name = "Kings Park Cafe",
            category = "Coffee Shop",
            visitCount = 4,
            lastVisitedAt = now - 3_600_000L
        )
        val note1 = com.example.data.model.NoteWithDetails(
            note = NoteEntity(
                id = "n_idea_1",
                title = "Graph Traversal Caching Idea",
                content = "Use 1-hop adjacency tables to speed up local RAG.",
                category = "Ideas",
                memoryType = "IDEAS",
                isPinned = true,
                projectId = project.id,
                placeId = place.id,
                createdAt = tenDaysAgo
            ),
            people = listOf(elena)
        )
        val note2 = com.example.data.model.NoteWithDetails(
            note = NoteEntity(
                id = "n_conv_2",
                title = "Sprint Review with Elena",
                content = "Reviewed Project Nexus milestones at Kings Park Cafe.",
                category = "Meeting",
                memoryType = "CONVERSATIONS",
                projectId = project.id,
                placeId = place.id,
                createdAt = now - 7_200_000L
            ),
            people = listOf(elena)
        )
        val comm1 = com.example.data.local.entity.CommitmentEntity(
            id = "c_done",
            personId = elena.id,
            projectId = project.id,
            description = "Finalize v5 Room migration script",
            direction = "BY_ME",
            isCompleted = true,
            createdAt = tenDaysAgo
        )
        val comm2 = com.example.data.local.entity.CommitmentEntity(
            id = "c_open",
            personId = elena.id,
            projectId = project.id,
            description = "Publish benchmark results",
            direction = "BY_ME",
            isCompleted = false,
            createdAt = now - 3_600_000L
        )
        val link = com.example.data.local.entity.MemoryLinkEntity(
            id = "link_nexus",
            sourceNoteId = "n_idea_1",
            targetNoteId = "n_conv_2",
            relationType = "EXPANDS",
            strengthScore = 0.92f
        )

        val report = com.example.util.MemoryIntelligenceHelper.buildLifeIntelligenceReport(
            allNotes = listOf(note1, note2),
            allPeople = listOf(elena),
            allPlaces = listOf(place),
            allProjects = listOf(project),
            allCommitments = listOf(comm1, comm2),
            allMemoryLinks = listOf(link)
        )

        assertEquals(2, report.totalMemoriesCount)
        assertEquals(50, report.commitmentFulfillmentPercent)
        assertEquals(1, report.completedCommitmentsCount)
        assertEquals(1, report.openCommitmentsCount)
        assertEquals(1, report.activeProjectsCount)
        assertEquals(1, report.memoryLinksCount)
        assertTrue(report.discoveredPatterns.any { it.categoryLabel == "Relational Synergy" && it.title.contains("Elena Rostova") })
        assertTrue(report.discoveredPatterns.any { it.categoryLabel == "Project Velocity" && it.title.contains("Project Nexus") })
        assertTrue(report.discoveredPatterns.any { it.categoryLabel == "Spatial Habit" && it.title.contains("Kings Park Cafe") })
        assertTrue(report.rediscoveryMemories.any { it.note.id == "n_idea_1" })
        assertTrue(report.timelineEpochs.any { it.epochTitle == "Today" })
        assertTrue(report.timelineEpochs.any { it.epochTitle == "Earlier This Month" })
    }

    @Test
    fun phase7PrivacySecurityAndAudioRetention_enforcesPinHashAndAudioPurge() {
        val now = System.currentTimeMillis()
        // 1. Verify salted SHA-256 PIN hashing & verification
        com.example.util.PrivacySecurityManager.setAppLockEnabled(null, true, "9876")
        assertTrue(com.example.util.PrivacySecurityManager.isAppLockEnabled.value)
        assertTrue(com.example.util.PrivacySecurityManager.verifyPin("9876"))
        assertTrue(!com.example.util.PrivacySecurityManager.verifyPin("1234"))

        // Lock session and test unlockSession
        com.example.util.PrivacySecurityManager.lockSessionNow()
        assertTrue(!com.example.util.PrivacySecurityManager.isSessionUnlocked.value)
        assertTrue(!com.example.util.PrivacySecurityManager.unlockSession(null, "0000"))
        assertTrue(com.example.util.PrivacySecurityManager.unlockSession(null, "9876"))
        assertTrue(com.example.util.PrivacySecurityManager.isSessionUnlocked.value)

        // 2. Verify raw audio file scanning & retention policy enforcement in a temp directory
        val tempDir = java.nio.file.Files.createTempDirectory("recall_audio_test").toFile()
        try {
            val oldAudio = java.io.File(tempDir, "debrief_old.m4a").apply {
                writeText("mock_audio_bytes_old")
                setLastModified(now - 10L * 24L * 3600L * 1000L) // 10 days old
            }
            val recentAudio = java.io.File(tempDir, "debrief_recent.m4a").apply {
                writeText("mock_audio_bytes_recent")
                setLastModified(now - 1L * 24L * 3600L * 1000L) // 1 day old
            }
            val nonAudioNote = java.io.File(tempDir, "notes_backup.json").apply {
                writeText("{}")
            }

            val scannedInitial = com.example.util.PrivacySecurityManager.scanRawAudioFiles(listOf(tempDir))
            assertEquals(2, scannedInitial.size)

            // KEEP_7_DAYS should delete the 10-day-old audio file and keep the 1-day-old audio file
            val deleted7Days = com.example.util.PrivacySecurityManager.enforceAudioRetentionPolicy(
                directories = listOf(tempDir),
                policy = com.example.util.AudioRetentionPolicy.KEEP_7_DAYS,
                nowMillis = now
            )
            assertEquals(1, deleted7Days)
            assertTrue(!oldAudio.exists())
            assertTrue(recentAudio.exists())
            assertTrue(nonAudioNote.exists())

            // DELETE_AFTER_TRANSCRIPTION should delete remaining raw audio immediately
            val deletedImmediate = com.example.util.PrivacySecurityManager.enforceAudioRetentionPolicy(
                directories = listOf(tempDir),
                policy = com.example.util.AudioRetentionPolicy.DELETE_AFTER_TRANSCRIPTION,
                nowMillis = now
            )
            assertEquals(1, deletedImmediate)
            assertTrue(!recentAudio.exists())
            assertTrue(nonAudioNote.exists())
        } finally {
            tempDir.deleteRecursively()
            com.example.util.PrivacySecurityManager.setAppLockEnabled(null, false, "1234")
        }
    }

    @Test
    fun noBogusContactsAndNoDuplicateNotes_rejectsVerbsAndDeduplicatesContent() {
        // 1. Verbs and nouns reported by user ("have", "saw", "been", "notes", "reminders") must never be accepted as person names
        val bogusCandidates = listOf("have", "Have", "saw", "Saw", "been", "Been", "went", "got", "notes", "reminders", "meeting", "work", "home")
        for (word in bogusCandidates) {
            assertTrue("Expected '$word' to be rejected as a person name", !MemoryReconciliationEngine.isValidPersonName(word))
        }
        assertTrue(MemoryReconciliationEngine.isValidPersonName("Jason"))
        assertTrue(MemoryReconciliationEngine.isValidPersonName("Sarah Chen"))

        // 2. Sentence containing "to have", "have been", "saw" must not extract bogus people
        val extracted = MemoryReconciliationEngine.extractCandidateNames(
            text = "I need to have a reminder for tomorrow. I have been busy and saw that the notes were updated. Met with Sarah Chen.",
            knownPeopleNames = listOf("Have", "Saw", "Been", "Sarah Chen")
        )
        assertEquals(listOf("Sarah Chen"), extracted)

        // 3. Content equivalence across different headings
        assertTrue(
            MemoryReconciliationEngine.areNoteContentsEquivalent(
                "Call the supplier at 3pm regarding the shipment.",
                "call the supplier at 3pm regarding the shipment!"
            )
        )

        // 4. DailyDebriefResult with duplicate distinctNotes having different headings but identical content is deduplicated
        val rawDebrief = com.example.data.model.DailyDebriefResult(
            dailySummary = "Productive afternoon.",
            peopleUpdates = listOf(
                com.example.data.model.DebriefPersonUpdate(personName = "Have", relationshipHint = "Contact", newFacts = listOf("bogus")),
                com.example.data.model.DebriefPersonUpdate(personName = "Saw", relationshipHint = "Contact", newFacts = listOf("bogus")),
                com.example.data.model.DebriefPersonUpdate(personName = "Been", relationshipHint = "Contact", newFacts = listOf("bogus")),
                com.example.data.model.DebriefPersonUpdate(personName = "Sarah Chen", relationshipHint = "Colleague", newFacts = listOf("Reviewed Q4 deck"))
            ),
            distinctNotes = listOf(
                com.example.data.model.DebriefParsedNote(
                    title = "Untitled Note",
                    content = "Reviewed Q4 deck with Sarah Chen and finalized budget targets.",
                    category = "Work",
                    tags = listOf("q4"),
                    peopleInvolved = listOf("Sarah Chen", "Have", "Saw", "Been")
                ),
                com.example.data.model.DebriefParsedNote(
                    title = "Q4 Budget Review",
                    content = "Reviewed Q4 deck with Sarah Chen and finalized budget targets.",
                    category = "Work",
                    tags = listOf("budget"),
                    peopleInvolved = listOf("Sarah Chen")
                )
            ),
            actionItems = emptyList()
        )

        val reconciled = MemoryReconciliationEngine.reconcileDebriefWithExistingNotes(
            rawTranscript = "Reviewed Q4 deck with Sarah Chen and finalized budget targets.",
            debriefResult = rawDebrief,
            existingNotes = emptyList(),
            knownPeopleNames = listOf("Sarah Chen")
        )

        assertEquals(1, reconciled.peopleUpdates.size)
        assertEquals("Sarah Chen", reconciled.peopleUpdates.first().personName)
        assertEquals(1, reconciled.distinctNotes.size)
        assertEquals(listOf("Sarah Chen"), reconciled.distinctNotes.first().peopleInvolved)
    }

    @Test
    fun memoraeLifeOrganiserAndIntelligence_classifiesCommitmentsParsesDatesAndRewindsLife() {
        val now = System.currentTimeMillis()
        // 1. Commitment Classification (Phase 6: Explicit Promise vs Personal Intention vs Follow-Up vs Conditional vs Undated)
        val explicitPromise = com.example.data.local.entity.CommitmentEntity(
            id = "c1",
            description = "I promised Sarah I will send the signed contract",
            direction = "BY_ME",
            dueDate = now + 86_400_000L
        )
        val conditionalPromise = com.example.data.local.entity.CommitmentEntity(
            id = "c2",
            description = "Update the staging server if the client approves the budget",
            direction = "BY_ME",
            dueDate = now + 86_400_000L
        )
        val undatedCommitment = com.example.data.local.entity.CommitmentEntity(
            id = "c3",
            description = "Review the new office lease terms",
            direction = "BY_ME",
            dueDate = null
        )

        assertEquals(
            com.example.util.CommitmentClassification.EXPLICIT_PROMISE,
            com.example.util.RecallLifeOrganiserManager.classifyCommitment(explicitPromise)
        )
        assertEquals(
            com.example.util.CommitmentClassification.CONDITIONAL,
            com.example.util.RecallLifeOrganiserManager.classifyCommitment(conditionalPromise)
        )
        assertEquals(
            com.example.util.CommitmentClassification.UNDATED,
            com.example.util.RecallLifeOrganiserManager.classifyCommitment(undatedCommitment)
        )

        // 2. Natural-language date/time & recurring reminder detection (Phase 9)
        val parsedSuggestion = com.example.util.RecallLifeOrganiserManager.parseNaturalDateTimeSuggestion(
            text = "I need to call Peter next Friday at 2pm",
            knownPeopleNames = listOf("Peter"),
            nowMillis = now
        )
        assertNotNull(parsedSuggestion)
        assertEquals("Peter", parsedSuggestion?.mentionedPersonName)
        assertTrue(parsedSuggestion!!.suggestedTimeMillis > now)

        val recurringSuggestion = com.example.util.RecallLifeOrganiserManager.parseNaturalDateTimeSuggestion(
            text = "Remind me to review project metrics every week at 9am",
            nowMillis = now
        )
        assertNotNull(recurringSuggestion)
        assertEquals("WEEKLY", recurringSuggestion?.recurrenceHint)

        // 3. Dynamic Lists extraction (Phase 4)
        val extractedListItems = com.example.util.RecallLifeOrganiserManager.extractListItemsFromText(
            "Buy oat milk, espresso beans, and sparkling water"
        )
        assertEquals(3, extractedListItems.size)
        assertTrue(extractedListItems.any { it.contains("oat milk", ignoreCase = true) })

        // 4. Life Rewind query (Phase 3)
        val peter = com.example.data.local.entity.PersonEntity(
            id = "p_peter",
            name = "Peter",
            relationship = "Partner"
        )
        val noteLastWeek = com.example.data.model.NoteWithDetails(
            note = NoteEntity(
                id = "n_rewind_1",
                title = "Coffee with Peter",
                content = "Discussed the Q4 expansion roadmap with Peter.",
                category = "Conversations",
                createdAt = now - 2 * 86_400_000L
            ),
            people = listOf(peter)
        )
        val rewindResult = com.example.util.RecallLifeOrganiserManager.executeLifeRewindQuery(
            query = "What did I discuss with Peter this week?",
            allNotes = listOf(noteLastWeek),
            allPeople = listOf(peter),
            allPlaces = emptyList(),
            allCommitments = listOf(explicitPromise),
            nowMillis = now
        )
        assertEquals(1, rewindResult.matchedNotes.size)
        assertEquals("Coffee with Peter", rewindResult.matchedNotes.first().note.title)

        // 5. Phase 8 Learned Preferences (Observed vs Confirmed)
        val prefs = com.example.util.RecallLifeOrganiserManager.synthesizeLearnedPreferences(
            userSaved = emptyList(),
            allNotes = listOf(
                com.example.data.model.NoteWithDetails(
                    note = NoteEntity(
                        id = "n_pref",
                        title = "Morning Routine",
                        content = "I prefer morning meetings before 10am and flat white coffee.",
                        category = "Personal"
                    )
                )
            ),
            allPlaces = emptyList(),
            allProjects = emptyList()
        )
        assertTrue(prefs.isNotEmpty())
        assertTrue(prefs.any { it.statement.contains("prefer morning meetings", ignoreCase = true) })

        // 6. Phase 10 Weekly AI Life Briefing synthesis
        val weeklyBriefing = com.example.util.RecallLifeOrganiserManager.buildWeeklyLifeBriefing(
            allNotes = listOf(noteLastWeek),
            allPeople = listOf(peter),
            allCommitments = listOf(explicitPromise),
            allActions = emptyList(),
            allProjects = emptyList(),
            nowMillis = now
        )
        assertEquals(1, weeklyBriefing.memoriesCapturedLast7Days)
        assertEquals(1, weeklyBriefing.openCommitmentsCount)
        assertTrue(weeklyBriefing.topPeopleInteracted.any { it.contains("Peter") })
        assertTrue(weeklyBriefing.markdownExport.contains("Weekly AI Life Briefing"))

        // 7. Phase 11 Contextual Recommendations Engine
        val recommendations = com.example.util.RecallLifeOrganiserManager.buildContextualRecommendations(
            allNotes = listOf(noteLastWeek),
            allPeople = listOf(peter),
            allProjects = emptyList(),
            allCommitments = listOf(explicitPromise, undatedCommitment),
            allActions = emptyList(),
            nowMillis = now
        )
        assertTrue(recommendations.isNotEmpty())
        assertTrue(recommendations.any { it.targetCommitmentId == "c1" || it.targetCommitmentId == "c3" })
    }
}
