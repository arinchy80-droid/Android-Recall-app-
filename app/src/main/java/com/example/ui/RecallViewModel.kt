package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.RecallDatabase
import com.example.data.local.entity.ActionItemEntity
import com.example.data.local.entity.MediaEntity
import com.example.data.local.entity.MediaType
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.PersonEntity
import com.example.data.local.entity.ReminderEntity
import com.example.data.model.ChatMessage
import com.example.data.model.Citation
import com.example.data.model.DailyDebriefResult
import com.example.data.model.MessageSender
import com.example.data.model.NoteWithDetails
import com.example.data.model.SmartItemFilter
import com.example.data.model.SmartItemType
import com.example.data.model.SmartSearchItem
import com.example.data.model.SmartSearchNluInterpretation
import com.example.data.search.SmartSearchEngine
import com.example.data.remote.GeminiService
import com.example.data.repository.RecallRepository
import com.example.util.AudioRecorderHelper
import com.example.util.DateUtils
import com.example.util.NotificationHelper
import com.example.util.PhotoStorageHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

private fun <T> Flow<T>.kotlinxSafeCatch(defaultValue: T): Flow<T> =
    this.catch { emit(defaultValue) }

class RecallViewModel(application: Application) : AndroidViewModel(application) {

    private val database = RecallDatabase.getInstance(application)
    val repository = RecallRepository(database)
    val geminiService = GeminiService(application)
    val audioRecorder = AudioRecorderHelper(application)

    // --- Search & Filters ---
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("All")

    // --- Advanced Smart Search State ---
    val isSmartSearchEnabled = MutableStateFlow(true)
    val isSmartSearchLoading = MutableStateFlow(false)
    val isSmartSearchActive = MutableStateFlow(false)
    val smartSearchFilter = MutableStateFlow(SmartItemFilter.ALL)
    val smartSearchInterpretation = MutableStateFlow<SmartSearchNluInterpretation?>(null)
    val smartSearchResults = MutableStateFlow<List<SmartSearchItem>>(emptyList())
    val smartSearchAiSummary = MutableStateFlow<String?>(null)

    val filteredSmartSearchResults: StateFlow<List<SmartSearchItem>> = combine(
        smartSearchResults,
        smartSearchFilter
    ) { items, filter ->
        when (filter) {
            SmartItemFilter.ALL -> items
            SmartItemFilter.NOTES -> items.filter { it.itemType == SmartItemType.NOTE }
            SmartItemFilter.PHOTOS -> items.filter { it.itemType == SmartItemType.PHOTO }
            SmartItemFilter.PEOPLE -> items.filter { it.itemType == SmartItemType.PERSON }
            SmartItemFilter.PLACES -> items.filter { it.itemType == SmartItemType.PLACE }
            SmartItemFilter.PROJECTS -> items.filter { it.itemType == SmartItemType.PROJECT }
            SmartItemFilter.COMMITMENTS -> items.filter { it.itemType == SmartItemType.COMMITMENT }
            SmartItemFilter.TASKS -> items.filter { it.itemType == SmartItemType.ACTION }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var smartSearchJob: Job? = null

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
        smartSearchJob?.cancel()

        if (query.isBlank()) {
            clearSmartSearch()
            return
        }

        if (isSmartSearchEnabled.value) {
            smartSearchJob = viewModelScope.launch {
                executeSmartSearch(query)
            }
        }
    }

    suspend fun executeSmartSearch(query: String) {
        if (query.isBlank()) {
            clearSmartSearch()
            return
        }
        isSmartSearchActive.value = true
        isSmartSearchLoading.value = true

        try {
            // Immediate local NLU parsing across People, Places, and Projects
            val allPeopleList = repository.allPeople.first()
            val allPlacesList = repository.allPlaces.first()
            val allProjectsList = repository.allProjects.first()
            val allCommitmentsList = repository.allCommitments.first()
            val allMemoryLinksList = repository.allMemoryLinks.first()

            val localNlu = SmartSearchEngine.parseQueryLocal(
                query = query,
                knownPeople = allPeopleList,
                knownPlaces = allPlacesList,
                knownProjects = allProjectsList
            )
            smartSearchInterpretation.value = localNlu

            val allNotesList = repository.allNotes.first()
            val allMediaList = repository.allMedia.first()
            val allActionsList = repository.allActionItems.first()

            val ranked = SmartSearchEngine.filterAndRankMemories(
                allNotes = allNotesList,
                allMedia = allMediaList,
                allPeople = allPeopleList,
                allActions = allActionsList,
                interpretation = localNlu,
                allPlaces = allPlacesList,
                allProjects = allProjectsList,
                allCommitments = allCommitmentsList,
                allMemoryLinks = allMemoryLinksList
            )
            val graphHops = ranked.count { !it.graphPath.isNullOrBlank() || it.highlights.any { h -> h.contains("🕸️") } }
            smartSearchInterpretation.value = localNlu.copy(graphTraversalHops = graphHops)
            smartSearchResults.value = ranked

            // Grounded AI synthesis with Gemini
            if (ranked.isNotEmpty()) {
                val candidateSummaries = ranked.take(6).joinToString("\n---\n") { item ->
                    "Item: [${item.itemType.name}] Title: ${item.title} | Subtitle: ${item.subtitle} | Snippet: ${item.snippet} | Highlights: ${item.highlights.joinToString(", ")}"
                }
                val geminiRes = geminiService.interpretAndSummarizeSmartSearch(query, candidateSummaries)
                geminiRes.onSuccess { aiNlu ->
                    smartSearchInterpretation.value = smartSearchInterpretation.value?.copy(
                        intent = if (aiNlu.intent != "ALL") aiNlu.intent else localNlu.intent,
                        explanation = aiNlu.explanation.ifBlank { localNlu.explanation },
                        executiveSummary = aiNlu.executiveSummary,
                        graphTraversalHops = graphHops
                    ) ?: aiNlu.copy(graphTraversalHops = graphHops)
                    smartSearchAiSummary.value = aiNlu.executiveSummary
                }
            }
        } catch (e: Exception) {
            // keep graceful
        } finally {
            isSmartSearchLoading.value = false
        }
    }

    fun clearSmartSearch() {
        searchQuery.value = ""
        isSmartSearchActive.value = false
        isSmartSearchLoading.value = false
        smartSearchInterpretation.value = null
        smartSearchResults.value = emptyList()
        smartSearchAiSummary.value = null
        smartSearchFilter.value = SmartItemFilter.ALL
    }

    fun setSmartSearchFilter(filter: SmartItemFilter) {
        smartSearchFilter.value = filter
    }

    fun toggleSmartSearch() {
        isSmartSearchEnabled.value = !isSmartSearchEnabled.value
        if (searchQuery.value.isNotBlank()) {
            onSearchQueryChanged(searchQuery.value)
        }
    }

    // --- Interactive Calendar State ---
    val selectedCalendarDate = MutableStateFlow<Long?>(null)
    val isCalendarExpanded = MutableStateFlow<Boolean>(false)
    val calendarMonthOffset = MutableStateFlow<Int>(0)

    val memoryDates: StateFlow<Set<Long>> = repository.allNotes
        .kotlinxSafeCatch(emptyList())
        .map { notesList ->
            notesList.map { DateUtils.startOfDay(it.note.createdAt) }.toSet()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun selectCalendarDate(dateMillis: Long?) {
        if (selectedCalendarDate.value == dateMillis) {
            selectedCalendarDate.value = null
        } else {
            selectedCalendarDate.value = dateMillis
        }
    }

    fun toggleCalendarExpanded() {
        isCalendarExpanded.value = !isCalendarExpanded.value
    }

    fun clearCalendarFilter() {
        selectedCalendarDate.value = null
    }

    fun changeCalendarMonth(delta: Int) {
        calendarMonthOffset.value = calendarMonthOffset.value + delta
    }

    fun resetCalendarToToday() {
        calendarMonthOffset.value = 0
        selectedCalendarDate.value = DateUtils.startOfDay(System.currentTimeMillis())
    }

    // Filtered / Searched Notes (with Category, Search, Calendar Date, and Tags)
    val notes: StateFlow<List<NoteWithDetails>> = combine(
        repository.allNotes.kotlinxSafeCatch(emptyList()),
        searchQuery,
        selectedCategory,
        selectedCalendarDate,
        repository.allReminders.kotlinxSafeCatch(emptyList())
    ) { allNotes, query, category, calDate, allRem ->
        var list = allNotes
        if (calDate != null) {
            val matchingReminderNoteIds = allRem
                .filter { DateUtils.isSameDay(it.reminderTimeMillis, calDate) && !it.noteId.isNullOrBlank() }
                .mapNotNull { it.noteId }
                .toSet()
            list = list.filter { DateUtils.isSameDay(it.note.createdAt, calDate) || matchingReminderNoteIds.contains(it.note.id) }
        }
        if (category != "All") {
            list = when {
                category.equals("Favorites", ignoreCase = true) -> list.filter { it.note.isPinned }
                category.equals("People", ignoreCase = true) -> list.filter { it.people.isNotEmpty() }
                category.equals("Events", ignoreCase = true) -> list.filter {
                    it.note.category.equals("Events", ignoreCase = true) || it.note.category.equals("Meeting", ignoreCase = true)
                }
                category.equals("Visuals", ignoreCase = true) -> list.filter {
                    it.note.category.equals("Visuals", ignoreCase = true) || it.media.any { m -> m.mediaType == MediaType.IMAGE }
                }
                else -> list.filter { it.note.category.equals(category, ignoreCase = true) }
            }
        }
        if (query.isNotBlank()) {
            val q = query.lowercase().trim()
            list = list.filter {
                it.note.title.lowercase().contains(q) ||
                        it.note.content.lowercase().contains(q) ||
                        it.note.tagsJson.lowercase().contains(q) ||
                        it.note.rawTranscription?.lowercase()?.contains(q) == true ||
                        it.people.any { p -> p.name.lowercase().contains(q) }
            }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All People + Relationship Circle Filter
    val peopleSearchQuery = MutableStateFlow("")
    val selectedRelationshipFilter = MutableStateFlow("All")
    val people: StateFlow<List<PersonEntity>> = combine(
        repository.allPeople.kotlinxSafeCatch(emptyList()),
        peopleSearchQuery,
        selectedRelationshipFilter
    ) { allPeople, q, circleFilter ->
        var list = allPeople
        if (circleFilter != "All") {
            list = when (circleFilter) {
                "Needs Catch-Up" -> list.filter { p ->
                    p.isOverdueForCatchUp()
                }
                "Work" -> list.filter { p ->
                    val r = p.relationship?.lowercase() ?: ""
                    r.contains("work") || r.contains("colleague") || r.contains("coworker") ||
                            r.contains("manager") || r.contains("team") || r.contains("partner") || r.contains("mentor")
                }
                "Family" -> list.filter { p ->
                    val r = p.relationship?.lowercase() ?: ""
                    r.contains("family") || r.contains("sister") || r.contains("brother") ||
                            r.contains("mom") || r.contains("dad") || r.contains("mother") ||
                            r.contains("father") || r.contains("wife") || r.contains("husband") ||
                            r.contains("spouse") || r.contains("cousin") || r.contains("parent")
                }
                "Friends" -> list.filter { p ->
                    val r = p.relationship?.lowercase() ?: ""
                    r.contains("friend") || r.contains("buddy") || r.contains("neighbor") || r.contains("social")
                }
                "Clients" -> list.filter { p ->
                    val r = p.relationship?.lowercase() ?: ""
                    r.contains("client") || r.contains("customer") || r.contains("lead") || r.contains("vendor")
                }
                else -> list.filter { p -> p.relationship?.contains(circleFilter, ignoreCase = true) == true }
            }
        }
        if (q.isNotBlank()) {
            list = list.filter {
                it.name.contains(q, ignoreCase = true) ||
                        it.relationship?.contains(q, ignoreCase = true) == true ||
                        it.factsJson.contains(q, ignoreCase = true)
            }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val overdueContacts: StateFlow<List<PersonEntity>> = repository.allPeople
        .kotlinxSafeCatch(emptyList())
        .map { list ->
            list.filter { it.isOverdueForCatchUp() }
                .sortedByDescending { it.daysSinceLastContact() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All Action Items
    val actionItems: StateFlow<List<ActionItemEntity>> = repository.allActionItems
        .kotlinxSafeCatch(emptyList())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reminders State
    val allReminders: StateFlow<List<com.example.data.local.entity.ReminderEntity>> = repository.allReminders
        .kotlinxSafeCatch(emptyList())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeReminders: StateFlow<List<com.example.data.local.entity.ReminderEntity>> = repository.activeReminders
        .kotlinxSafeCatch(emptyList())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Unverified AI-Inferred Memories & Active Commitments (Phase 2 Provenance)
    val unverifiedMemories: StateFlow<List<NoteWithDetails>> = repository.unverifiedMemories
        .kotlinxSafeCatch(emptyList())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeCommitments: StateFlow<List<com.example.data.local.entity.CommitmentEntity>> = repository.activeCommitments
        .kotlinxSafeCatch(emptyList())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCommitments: StateFlow<List<com.example.data.local.entity.CommitmentEntity>> = repository.allCommitments
        .kotlinxSafeCatch(emptyList())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlaces: StateFlow<List<com.example.data.local.entity.PlaceEntity>> = repository.allPlaces
        .kotlinxSafeCatch(emptyList())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProjects: StateFlow<List<com.example.data.local.entity.ProjectEntity>> = repository.allProjects
        .kotlinxSafeCatch(emptyList())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMemoryLinks: StateFlow<List<com.example.data.local.entity.MemoryLinkEntity>> = repository.allMemoryLinks
        .kotlinxSafeCatch(emptyList())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminderDates: StateFlow<Set<Long>> = repository.allReminders
        .kotlinxSafeCatch(emptyList())
        .map { remindersList ->
            remindersList
                .filter { !it.isCompleted }
                .map { DateUtils.startOfDay(it.reminderTimeMillis) }
                .toSet()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val remindersForSelectedDate: StateFlow<List<com.example.data.local.entity.ReminderEntity>> = combine(
        repository.allReminders.kotlinxSafeCatch(emptyList()),
        selectedCalendarDate
    ) { allRem, dateMillis ->
        if (dateMillis == null) {
            emptyList()
        } else {
            allRem.filter { DateUtils.isSameDay(it.reminderTimeMillis, dateMillis) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Counts
    val notesCount = repository.notesCount
        .kotlinxSafeCatch(0)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val peopleCount = repository.peopleCount
        .kotlinxSafeCatch(0)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val pendingActionCount = repository.pendingActionItemsCount
        .kotlinxSafeCatch(0)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // --- Selected Person, Place & Project Dossiers ---
    private val _selectedPerson = MutableStateFlow<PersonEntity?>(null)
    val selectedPerson: StateFlow<PersonEntity?> = _selectedPerson.asStateFlow()

    private val _selectedPersonNotes = MutableStateFlow<List<NoteWithDetails>>(emptyList())
    val selectedPersonNotes: StateFlow<List<NoteWithDetails>> = _selectedPersonNotes.asStateFlow()

    private val _selectedPlace = MutableStateFlow<com.example.data.local.entity.PlaceEntity?>(null)
    val selectedPlace: StateFlow<com.example.data.local.entity.PlaceEntity?> = _selectedPlace.asStateFlow()

    private val _selectedProject = MutableStateFlow<com.example.data.local.entity.ProjectEntity?>(null)
    val selectedProject: StateFlow<com.example.data.local.entity.ProjectEntity?> = _selectedProject.asStateFlow()

    // --- Daily Debrief State ---
    val isRecording = audioRecorder.isRecording
    val recordingDuration = audioRecorder.durationSeconds
    val recordingAmplitude = audioRecorder.normalizedAmplitude
    val liveTranscript = audioRecorder.liveTranscript
    val lastRecordedAudioFile = audioRecorder.lastRecordedFile
    val debriefMode = MutableStateFlow("Freestyle")

    private val _isProcessingDebrief = MutableStateFlow(false)
    val isProcessingDebrief: StateFlow<Boolean> = _isProcessingDebrief.asStateFlow()

    private val _debriefResult = MutableStateFlow<DailyDebriefResult?>(null)
    val debriefResult: StateFlow<DailyDebriefResult?> = _debriefResult.asStateFlow()

    private val _showTriageSheet = MutableStateFlow(false)
    val showTriageSheet: StateFlow<Boolean> = _showTriageSheet.asStateFlow()

    // --- Chat Concierge State ---
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = MessageSender.CONCIERGE,
                text = "Hello! I am Recall Concierge. I have indexed your personal notes, meetings, and contacts. Ask me anything about your past conversations, deadlines, or people details.",
                timestamp = System.currentTimeMillis()
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isQueryingAi = MutableStateFlow(false)
    val isQueryingAi: StateFlow<Boolean> = _isQueryingAi.asStateFlow()

    val enableHighThinking = MutableStateFlow(false)
    val chatbotTone = MutableStateFlow("Conversational")

    // --- Note Detail / Editor State ---
    val noteBeingEdited = MutableStateFlow<NoteWithDetails?>(null)
    val isNoteEditorOpen = MutableStateFlow(false)

    // --- Dossier Profile Synthesis State ---
    val isSynthesizingProfile = MutableStateFlow(false)

    // --- Standalone Gallery State ---
    val allMedia = repository.allMedia
        .kotlinxSafeCatch(emptyList())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _habits = MutableStateFlow<List<com.example.util.HabitItem>>(emptyList())
    val habits: StateFlow<List<com.example.util.HabitItem>> = _habits.asStateFlow()

    // --- Memorae Redesign: Hub Sub-Tab State & Dynamic Organiser State ---
    val captureSubTab = MutableStateFlow("Quick Capture")
    val memoriesSubTab = MutableStateFlow("All Memories")
    val aiIntelligenceSubTab = MutableStateFlow("Ask Recall")
    val organiseSubTab = MutableStateFlow("Commitments")

    private val _dynamicLists = MutableStateFlow<List<com.example.util.DynamicList>>(emptyList())
    val dynamicLists: StateFlow<List<com.example.util.DynamicList>> = _dynamicLists.asStateFlow()

    private val _taskMetaMap = MutableStateFlow<Map<String, com.example.util.TaskExtendedMeta>>(emptyMap())
    val taskMetaMap: StateFlow<Map<String, com.example.util.TaskExtendedMeta>> = _taskMetaMap.asStateFlow()

    private val _userSavedPreferences = MutableStateFlow<List<com.example.util.LearnedPreferenceItem>>(emptyList())
    val userSavedPreferences: StateFlow<List<com.example.util.LearnedPreferenceItem>> = _userSavedPreferences.asStateFlow()

    private val _reminderRecurrenceMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val reminderRecurrenceMap: StateFlow<Map<String, String>> = _reminderRecurrenceMap.asStateFlow()

    private val _userDisplayName = MutableStateFlow("")
    val userDisplayName: StateFlow<String> = _userDisplayName.asStateFlow()

    private val _userFocusGoal = MutableStateFlow("")
    val userFocusGoal: StateFlow<String> = _userFocusGoal.asStateFlow()

    private val _userFeedbackMessage = MutableStateFlow<String?>(null)
    val userFeedbackMessage: StateFlow<String?> = _userFeedbackMessage.asStateFlow()

    private val _pendingScreenNavigation = MutableStateFlow<com.example.ui.navigation.Screen?>(null)
    val pendingScreenNavigation: StateFlow<com.example.ui.navigation.Screen?> = _pendingScreenNavigation.asStateFlow()

    fun requestScreenNavigation(screen: com.example.ui.navigation.Screen) {
        _pendingScreenNavigation.value = screen
    }

    fun navigateToHubSection(screen: com.example.ui.navigation.Screen, subTab: String? = null) {
        if (subTab != null) {
            when (screen) {
                com.example.ui.navigation.Screen.Capture,
                com.example.ui.navigation.Screen.Debrief -> captureSubTab.value = subTab
                com.example.ui.navigation.Screen.Memories,
                com.example.ui.navigation.Screen.Stream,
                com.example.ui.navigation.Screen.People,
                com.example.ui.navigation.Screen.Gallery -> memoriesSubTab.value = subTab
                com.example.ui.navigation.Screen.AiIntelligence,
                com.example.ui.navigation.Screen.AskAi -> aiIntelligenceSubTab.value = subTab
                com.example.ui.navigation.Screen.Organise,
                com.example.ui.navigation.Screen.Reminders -> organiseSubTab.value = subTab
                else -> Unit
            }
        }
        _pendingScreenNavigation.value = screen
    }

    fun consumeScreenNavigation() {
        _pendingScreenNavigation.value = null
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val prefs = application.getSharedPreferences("recall_app_prefs", android.content.Context.MODE_PRIVATE)
                val isCleanSlateEnsured = prefs.getBoolean("fresh_launch_clean_slate_v5", false)
                if (!isCleanSlateEnsured) {
                    // Ensure complete clean slate for fresh launch: wipe all lingering mock, test, or sample data
                    repository.clearAllData()
                    prefs.edit().putBoolean("fresh_launch_clean_slate_v5", true).apply()
                } else {
                    repository.purgeMockDataAndEnsureFresh()
                }
                _habits.value = com.example.util.MemoryIntelligenceHelper.loadHabits(application)
                _dynamicLists.value = com.example.util.RecallLifeOrganiserManager.loadDynamicLists(application)
                _taskMetaMap.value = com.example.util.RecallLifeOrganiserManager.loadTaskMetaMap(application)
                _userSavedPreferences.value = com.example.util.RecallLifeOrganiserManager.loadUserSavedPreferences(application)
                _reminderRecurrenceMap.value = com.example.util.RecallLifeOrganiserManager.loadReminderRecurrenceMap(application)
                _userDisplayName.value = com.example.util.RecallLifeOrganiserManager.getUserDisplayName(application)
                _userFocusGoal.value = com.example.util.RecallLifeOrganiserManager.getUserFocusGoal(application)
                NotificationHelper.createNotificationChannel(application)
                com.example.util.ReminderNotificationHelper.createReminderChannel(application)
                com.example.util.ReminderNotificationHelper.rescheduleAllActiveReminders(application)
                NotificationHelper.rescheduleSavedDailyDebriefAlarm(application)
            } catch (t: Throwable) {
                Log.e("RecallViewModel", "Failed to complete background startup tasks safely", t)
            }
        }
    }

    fun toggleHabitDate(habitId: String, dateKey: String) {
        val current = _habits.value.toMutableList()
        val index = current.indexOfFirst { it.id == habitId }
        if (index != -1) {
            val h = current[index]
            val updatedDates = h.completedDateKeys.toMutableSet()
            if (updatedDates.contains(dateKey)) {
                updatedDates.remove(dateKey)
            } else {
                updatedDates.add(dateKey)
            }
            current[index] = h.copy(completedDateKeys = updatedDates)
            _habits.value = current
            com.example.util.MemoryIntelligenceHelper.saveHabits(getApplication(), current)
        }
    }

    fun addCustomHabit(title: String, icon: String = "⚡") {
        val clean = title.trim()
        if (clean.isBlank()) return
        val newHabit = com.example.util.HabitItem(
            id = "h_${System.currentTimeMillis()}",
            title = clean,
            icon = icon,
            completedDateKeys = emptySet()
        )
        val updated = _habits.value + newHabit
        _habits.value = updated
        com.example.util.MemoryIntelligenceHelper.saveHabits(getApplication(), updated)
        _userFeedbackMessage.value = "Habit added: $clean"
    }

    fun deleteHabit(habitId: String) {
        val updated = _habits.value.filterNot { it.id == habitId }
        _habits.value = updated
        com.example.util.MemoryIntelligenceHelper.saveHabits(getApplication(), updated)
        _userFeedbackMessage.value = "Habit removed."
    }

    fun updatePersonCatchUpCadence(person: PersonEntity, cadenceDays: Int) {
        viewModelScope.launch {
            val converters = com.example.data.local.converter.RoomConverters()
            val map = converters.toStringMap(person.keyDatesJson).toMutableMap()
            map["CatchUpCadenceDays"] = cadenceDays.toString()
            val updatedPerson = person.copy(
                keyDatesJson = converters.fromStringMap(map)
            )
            repository.savePerson(updatedPerson)
            if (_selectedPerson.value?.id == person.id) {
                _selectedPerson.value = updatedPerson
            }
            _userFeedbackMessage.value = "Catch-up cadence for ${person.name} set to every ${cadenceDays}d."
        }
    }

    fun extractActionItemsFromNoteText(
        text: String,
        onExtracted: (List<String>) -> Unit
    ) {
        val extracted = com.example.util.MemoryIntelligenceHelper.extractActionItemsFromText(text)
        if (extracted.isNotEmpty()) {
            onExtracted(extracted)
            _userFeedbackMessage.value = "Extracted ${extracted.size} action item(s) from text!"
        } else {
            _userFeedbackMessage.value = "No new action commitments detected in text."
        }
    }

    fun dismissFeedback() {
        _userFeedbackMessage.value = null
    }

    // --- Person Dossier Selection ---
    fun selectPerson(person: PersonEntity) {
        _selectedPerson.value = person
        viewModelScope.launch {
            repository.getNotesForPerson(person.id).collect {
                _selectedPersonNotes.value = it
            }
        }
    }

    fun clearSelectedPerson() {
        _selectedPerson.value = null
        _selectedPersonNotes.value = emptyList()
    }

    // --- Place, Project & Commitment Operations (Phase 3) ---
    fun selectPlace(place: com.example.data.local.entity.PlaceEntity) {
        _selectedPlace.value = place
    }

    fun clearSelectedPlace() {
        _selectedPlace.value = null
    }

    fun savePlace(place: com.example.data.local.entity.PlaceEntity) {
        viewModelScope.launch {
            repository.savePlace(place)
            if (_selectedPlace.value?.id == place.id) {
                _selectedPlace.value = place
            }
            _userFeedbackMessage.value = "📍 Saved place: ${place.name}"
        }
    }

    fun deletePlace(placeId: String) {
        viewModelScope.launch {
            repository.deletePlace(placeId)
            if (_selectedPlace.value?.id == placeId) {
                _selectedPlace.value = null
            }
            _userFeedbackMessage.value = "Place removed."
        }
    }

    fun startNoteAtPlace(place: com.example.data.local.entity.PlaceEntity) {
        val draftNote = NoteEntity(
            id = UUID.randomUUID().toString(),
            title = "Note at ${place.name}",
            content = "",
            category = "Travel",
            tagsJson = NoteEntity.formatTagsJson(NoteEntity.mergeTagsWithLocation(emptyList(), place.name)),
            placeId = place.id,
            memoryType = com.example.data.model.UnifiedMemoryType.PLACES.name,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        openNoteEditor(NoteWithDetails(note = draftNote))
    }

    fun selectProject(project: com.example.data.local.entity.ProjectEntity) {
        _selectedProject.value = project
    }

    fun clearSelectedProject() {
        _selectedProject.value = null
    }

    fun saveProject(project: com.example.data.local.entity.ProjectEntity) {
        viewModelScope.launch {
            repository.saveProject(project)
            if (_selectedProject.value?.id == project.id) {
                _selectedProject.value = project
            }
            _userFeedbackMessage.value = "🚀 Saved project: ${project.name}"
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
            if (_selectedProject.value?.id == projectId) {
                _selectedProject.value = null
            }
            _userFeedbackMessage.value = "Project removed."
        }
    }

    fun startNoteForProject(project: com.example.data.local.entity.ProjectEntity) {
        val draftNote = NoteEntity(
            id = UUID.randomUUID().toString(),
            title = "${project.name}: Update",
            content = "",
            category = "Projects",
            projectId = project.id,
            memoryType = com.example.data.model.UnifiedMemoryType.PROJECTS.name,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        openNoteEditor(NoteWithDetails(note = draftNote))
    }

    fun saveCommitment(commitment: com.example.data.local.entity.CommitmentEntity) {
        viewModelScope.launch {
            repository.saveCommitment(commitment)
            _userFeedbackMessage.value = "🤝 Saved commitment!"
        }
    }

    fun toggleCommitmentCompleted(id: String, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.toggleCommitmentCompleted(id, isCompleted)
            _userFeedbackMessage.value = if (isCompleted) "✓ Marked commitment fulfilled!" else "Reopened commitment."
        }
    }

    fun deleteCommitment(id: String) {
        viewModelScope.launch {
            repository.deleteCommitment(id)
            _userFeedbackMessage.value = "Commitment deleted."
        }
    }

    fun getLinksForNote(noteId: String): Flow<List<com.example.data.local.entity.MemoryLinkEntity>> {
        return repository.getLinksForNote(noteId)
    }

    fun linkMemories(sourceNoteId: String, targetNoteId: String, relationType: String = "RELATED_TO") {
        viewModelScope.launch {
            repository.linkMemories(sourceNoteId, targetNoteId, relationType, 0.95f)
            _userFeedbackMessage.value = "🔗 Linked memory ($relationType)!"
        }
    }

    fun deleteMemoryLink(linkId: String) {
        viewModelScope.launch {
            repository.deleteMemoryLink(linkId)
            _userFeedbackMessage.value = "Neural edge removed."
        }
    }

    fun startCatchUpNoteForPerson(person: PersonEntity) {
        viewModelScope.launch {
            val updatedPerson = person.copy(lastContactedAt = System.currentTimeMillis())
            repository.savePerson(updatedPerson)
            val draftNote = NoteEntity(
                id = UUID.randomUUID().toString(),
                title = "Catch-Up with ${person.name}",
                content = "",
                category = if (person.relationship?.contains("Work", ignoreCase = true) == true ||
                    person.relationship?.contains("Client", ignoreCase = true) == true
                ) "Meeting" else "Personal",
                tagsJson = NoteEntity.formatTagsJson(listOf("catch-up", person.name.lowercase().replace(" ", "-"))),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            openNoteEditor(
                NoteWithDetails(
                    note = draftNote,
                    people = listOf(updatedPerson),
                    media = emptyList(),
                    actionItems = emptyList()
                )
            )
            _userFeedbackMessage.value = "Started Catch-Up memory for ${person.name}"
        }
    }

    // --- Daily Debrief Controls ---
    fun startDebriefRecording() {
        audioRecorder.startRecording(viewModelScope)
    }

    fun finishAndProcessDebrief(manualTextOverride: String? = null) {
        val (audioFile, liveText) = audioRecorder.stopRecording()
        val hasManualText = !manualTextOverride.isNullOrBlank()
        val hasAudio = audioFile != null && audioFile.exists() && audioFile.length() > 400

        if (!hasManualText && !hasAudio && liveText.isBlank()) {
            _userFeedbackMessage.value = "Please speak or write a summary of your day to unpack."
            return
        }

        viewModelScope.launch {
            _isProcessingDebrief.value = true
            val existingPeople = repository.allPeople.first()
            val existingNotes = repository.allNotes.first()
            val allPeopleNames = existingPeople.map { it.name }
            val modePrefix = if (debriefMode.value != "Freestyle") "[Debrief Mode: ${debriefMode.value}] " else ""

            val result = if (hasManualText) {
                geminiService.processDailyDebrief(
                    transcribedText = modePrefix + manualTextOverride!!.trim(),
                    existingNotes = existingNotes,
                    existingPeople = existingPeople
                )
            } else if (hasAudio) {
                _userFeedbackMessage.value = "Sending full audio to Gemini for transcription & allocation..."
                geminiService.processDailyDebriefAudio(
                    audioFile = audioFile!!,
                    knownPeopleNames = allPeopleNames,
                    existingNotes = existingNotes,
                    existingPeople = existingPeople
                )
            } else {
                geminiService.processDailyDebrief(
                    transcribedText = modePrefix + liveText.trim(),
                    existingNotes = existingNotes,
                    existingPeople = existingPeople
                )
            }

            _isProcessingDebrief.value = false

            result.onSuccess { debrief ->
                val enriched = debrief.copy(
                    audioFilePath = if (hasAudio) audioFile?.absolutePath else debrief.audioFilePath
                )
                if (!enriched.rawTranscription.isNullOrBlank()) {
                    audioRecorder.setManualTranscript(enriched.rawTranscription)
                }
                _debriefResult.value = enriched
                _showTriageSheet.value = true
                if (enriched.noteUpdates.isNotEmpty()) {
                    _userFeedbackMessage.value = "Reconciled ${enriched.noteUpdates.size} existing note(s) & unpacked debrief!"
                } else if (enriched.isLocalFallback) {
                    _userFeedbackMessage.value = "Debrief unpacked safely on-device!"
                } else {
                    _userFeedbackMessage.value = "Full audio transcribed and unpacked successfully!"
                }
            }.onFailure { err ->
                _userFeedbackMessage.value = "Failed to parse debrief: ${err.message}"
            }
        }
    }

    fun dismissTriageSheet() {
        _showTriageSheet.value = false
    }

    fun toggleDebriefPersonIncluded(index: Int) {
        _debriefResult.value?.let { current ->
            if (index in current.peopleUpdates.indices) {
                current.peopleUpdates[index].isIncluded = !current.peopleUpdates[index].isIncluded
                _debriefResult.value = current.copy()
            }
        }
    }

    fun toggleDebriefNoteIncluded(index: Int) {
        _debriefResult.value?.let { current ->
            if (index in current.distinctNotes.indices) {
                current.distinctNotes[index].isIncluded = !current.distinctNotes[index].isIncluded
                _debriefResult.value = current.copy()
            }
        }
    }

    fun toggleDebriefNoteUpdateIncluded(index: Int) {
        _debriefResult.value?.let { current ->
            if (index in current.noteUpdates.indices) {
                current.noteUpdates[index].isIncluded = !current.noteUpdates[index].isIncluded
                _debriefResult.value = current.copy()
            }
        }
    }

    fun toggleDebriefActionIncluded(index: Int) {
        _debriefResult.value?.let { current ->
            if (index in current.actionItems.indices) {
                current.actionItems[index].isIncluded = !current.actionItems[index].isIncluded
                _debriefResult.value = current.copy()
            }
        }
    }

    fun toggleDebriefCommitmentIncluded(index: Int) {
        _debriefResult.value?.let { current ->
            if (index in current.commitments.indices) {
                current.commitments[index].isIncluded = !current.commitments[index].isIncluded
                _debriefResult.value = current.copy()
            }
        }
    }

    // --- Provenance Verification & Non-Destructive Revision History (Phase 2) ---
    fun verifyMemoryFact(note: NoteEntity) {
        viewModelScope.launch {
            repository.verifyMemoryFact(note)
            if (noteBeingEdited.value?.note?.id == note.id) {
                noteBeingEdited.value = repository.getNoteByIdOnce(note.id)
            }
            _userFeedbackMessage.value = "✓ Confirmed '${note.title}' as a verified fact!"
        }
    }

    fun markMemoryAsAiInferred(note: NoteEntity) {
        viewModelScope.launch {
            repository.markMemoryAsAiInferred(note)
            if (noteBeingEdited.value?.note?.id == note.id) {
                noteBeingEdited.value = repository.getNoteByIdOnce(note.id)
            }
            _userFeedbackMessage.value = "🤖 Flagged '${note.title}' as AI-inferred (awaiting review)."
        }
    }

    fun restoreNoteRevision(
        note: NoteEntity,
        revision: com.example.data.local.entity.NoteRevisionEntity,
        onRestored: (String, String) -> Unit
    ) {
        viewModelScope.launch {
            val restored = repository.restoreNoteRevision(note, revision)
            if (noteBeingEdited.value?.note?.id == note.id) {
                noteBeingEdited.value = repository.getNoteByIdOnce(note.id)
            }
            onRestored(restored.title, restored.content)
            _userFeedbackMessage.value = "↺ Restored revision from ${DateUtils.formatRelativeTime(revision.createdAt)}"
        }
    }

    fun commitDebriefToMemory() {
        val result = _debriefResult.value ?: return
        viewModelScope.launch {
            val count = repository.commitDailyDebrief(result)
            if (com.example.util.PrivacySecurityManager.audioRetentionPolicy.value == com.example.util.AudioRetentionPolicy.DELETE_AFTER_TRANSCRIPTION) {
                com.example.util.PrivacySecurityManager.enforceAudioRetentionPolicy(getApplication<android.app.Application>())
            }
            _showTriageSheet.value = false
            _debriefResult.value = null
            _userFeedbackMessage.value = "Committed $count items to memory successfully!"
        }
    }

    // --- Ask AI Concierge ---
    fun sendChatMessage(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank() || _isQueryingAi.value) return

        val userMsg = ChatMessage(
            sender = MessageSender.USER,
            text = trimmed,
            timestamp = System.currentTimeMillis()
        )
        _chatMessages.value = _chatMessages.value + userMsg

        viewModelScope.launch {
            _isQueryingAi.value = true
            val initialNotesList = repository.allNotes.first()
            val initialPeopleList = repository.allPeople.first()

            // Reconcile if the user is stating an update to an existing note (e.g. "I paid Jason $200")
            val isQuestion = trimmed.endsWith("?") ||
                trimmed.lowercase().startsWith("what ") ||
                trimmed.lowercase().startsWith("who ") ||
                trimmed.lowercase().startsWith("when ") ||
                trimmed.lowercase().startsWith("where ") ||
                trimmed.lowercase().startsWith("how ") ||
                trimmed.lowercase().startsWith("do i ") ||
                trimmed.lowercase().startsWith("did i ")

            val autoUpdates = if (!isQuestion) {
                com.example.util.MemoryReconciliationEngine.findNoteUpdatesForInput(
                    newInputText = trimmed,
                    existingNotes = initialNotesList,
                    knownPeopleNames = initialPeopleList.map { it.name }
                )
            } else {
                emptyList()
            }

            if (autoUpdates.isNotEmpty()) {
                repository.applyNoteUpdates(autoUpdates, rawTranscription = trimmed)
            }

            val allNotesList = if (autoUpdates.isNotEmpty()) repository.allNotes.first() else initialNotesList
            val allPeopleList = if (autoUpdates.isNotEmpty()) repository.allPeople.first() else initialPeopleList
            val allPlacesList = repository.allPlaces.first()
            val allProjectsList = repository.allProjects.first()
            val allCommitmentsList = repository.allCommitments.first()
            val allMemoryLinksList = repository.allMemoryLinks.first()

            val responseResult = geminiService.queryMemoryConcierge(
                userQuery = trimmed,
                notes = allNotesList,
                people = allPeopleList,
                enableHighThinking = enableHighThinking.value,
                places = allPlacesList,
                projects = allProjectsList,
                commitments = allCommitmentsList,
                memoryLinks = allMemoryLinksList
            )
            _isQueryingAi.value = false

            responseResult.onSuccess { (answer, citations) ->
                val reconciliationBanner = if (autoUpdates.isNotEmpty()) {
                    val summaryLines = autoUpdates.joinToString("\n") { u ->
                        "✅ **Updated Note '${u.updatedTitle}'**: ${u.changeSummary}"
                    }
                    "$summaryLines\n\n"
                } else ""
                val extraCitations = autoUpdates.map { u ->
                    Citation(
                        noteId = u.targetNoteId,
                        noteTitle = u.updatedTitle,
                        dateString = "Updated Today"
                    )
                }
                val aiMsg = ChatMessage(
                    sender = MessageSender.CONCIERGE,
                    text = reconciliationBanner + answer,
                    citations = (extraCitations + citations).distinctBy { it.noteId ?: it.noteTitle },
                    isThinking = enableHighThinking.value,
                    timestamp = System.currentTimeMillis()
                )
                _chatMessages.value = _chatMessages.value + aiMsg
            }.onFailure { err ->
                val errorMsg = ChatMessage(
                    sender = MessageSender.CONCIERGE,
                    text = "Could not retrieve response: ${err.message}. Check your API Key in Settings.",
                    timestamp = System.currentTimeMillis()
                )
                _chatMessages.value = _chatMessages.value + errorMsg
            }
        }
    }

    // --- Camera OCR & Visual Memory Ingestion ---
    fun processCameraCapture(bitmap: Bitmap, linkedPersonIds: List<String> = emptyList()) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val savedPath = PhotoStorageHelper.saveBitmap(context, bitmap)
            _userFeedbackMessage.value = "Synthesizing visual memory with Gemini..."
            val result = geminiService.analyzeImageMemory(bitmap)
            result.onSuccess { (ocr, desc, labels) ->
                val noteId = UUID.randomUUID().toString()
                val note = NoteEntity(
                    id = noteId,
                    title = if (labels.isNotEmpty()) "Visual Note: ${labels.first()}" else "Visual Memory",
                    content = desc,
                    category = "Visuals",
                    aiSummary = desc,
                    rawTranscription = ocr,
                    createdAt = System.currentTimeMillis(),
                    memoryType = com.example.data.model.UnifiedMemoryType.EXPERIENCES.name,
                    sourceType = com.example.data.model.MemorySourceType.IMAGE_OCR.name,
                    confidenceScore = 0.90f,
                    verificationStatus = com.example.data.model.MemoryVerificationStatus.VERIFIED_FACT.name
                )
                val media = MediaEntity(
                    id = UUID.randomUUID().toString(),
                    noteId = noteId,
                    mediaType = MediaType.IMAGE,
                    localUri = savedPath,
                    ocrText = ocr,
                    aiDescription = desc,
                    labelsJson = labels.joinToString { "\"$it\"" }
                )
                repository.saveNote(
                    note = note,
                    linkedPersonIds = linkedPersonIds,
                    mediaAttachments = listOf(media)
                )
                _userFeedbackMessage.value = "Saved photo memory with AI vision synthesis!"
            }.onFailure { err ->
                // Always save note with original photo even if AI analysis is offline
                val noteId = UUID.randomUUID().toString()
                val note = NoteEntity(
                    id = noteId,
                    title = "Photo Memory",
                    content = "Photo captured without AI description.",
                    category = "Visuals",
                    createdAt = System.currentTimeMillis(),
                    memoryType = com.example.data.model.UnifiedMemoryType.EXPERIENCES.name,
                    sourceType = com.example.data.model.MemorySourceType.IMAGE_OCR.name,
                    confidenceScore = 1.0f,
                    verificationStatus = com.example.data.model.MemoryVerificationStatus.VERIFIED_FACT.name
                )
                val media = MediaEntity(
                    id = UUID.randomUUID().toString(),
                    noteId = noteId,
                    mediaType = MediaType.IMAGE,
                    localUri = savedPath,
                    aiDescription = "Captured camera photo"
                )
                repository.saveNote(
                    note = note,
                    linkedPersonIds = linkedPersonIds,
                    mediaAttachments = listOf(media)
                )
                _userFeedbackMessage.value = "Saved photo! (Vision analysis skipped: ${err.message})"
            }
        }
    }

    fun processImageUriCapture(uri: Uri, linkedPersonIds: List<String> = emptyList()) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val saved = PhotoStorageHelper.saveUri(context, uri)
                if (saved != null) {
                    val (savedPath, bitmap) = saved
                    _userFeedbackMessage.value = "Synthesizing visual memory with Gemini..."
                    val result = geminiService.analyzeImageMemory(bitmap)
                    result.onSuccess { (ocr, desc, labels) ->
                        val noteId = UUID.randomUUID().toString()
                        val note = NoteEntity(
                            id = noteId,
                            title = if (labels.isNotEmpty()) "Visual Note: ${labels.first()}" else "Visual Memory",
                            content = desc,
                            category = "Visuals",
                            aiSummary = desc,
                            rawTranscription = ocr,
                            createdAt = System.currentTimeMillis(),
                            memoryType = com.example.data.model.UnifiedMemoryType.EXPERIENCES.name,
                            sourceType = com.example.data.model.MemorySourceType.IMAGE_OCR.name,
                            confidenceScore = 0.90f,
                            verificationStatus = com.example.data.model.MemoryVerificationStatus.VERIFIED_FACT.name
                        )
                        val media = MediaEntity(
                            id = UUID.randomUUID().toString(),
                            noteId = noteId,
                            mediaType = MediaType.IMAGE,
                            localUri = savedPath,
                            ocrText = ocr,
                            aiDescription = desc,
                            labelsJson = labels.joinToString { "\"$it\"" }
                        )
                        repository.saveNote(
                            note = note,
                            linkedPersonIds = linkedPersonIds,
                            mediaAttachments = listOf(media)
                        )
                        _userFeedbackMessage.value = "Saved photo memory with AI vision synthesis!"
                    }.onFailure { err ->
                        val noteId = UUID.randomUUID().toString()
                        val note = NoteEntity(
                            id = noteId,
                            title = "Photo Memory",
                            content = "Photo attached from gallery.",
                            category = "Visuals",
                            createdAt = System.currentTimeMillis(),
                            memoryType = com.example.data.model.UnifiedMemoryType.EXPERIENCES.name,
                            sourceType = com.example.data.model.MemorySourceType.IMAGE_OCR.name,
                            confidenceScore = 1.0f,
                            verificationStatus = com.example.data.model.MemoryVerificationStatus.VERIFIED_FACT.name
                        )
                        val media = MediaEntity(
                            id = UUID.randomUUID().toString(),
                            noteId = noteId,
                            mediaType = MediaType.IMAGE,
                            localUri = savedPath,
                            aiDescription = "Imported gallery photo"
                        )
                        repository.saveNote(
                            note = note,
                            linkedPersonIds = linkedPersonIds,
                            mediaAttachments = listOf(media)
                        )
                        _userFeedbackMessage.value = "Photo saved! (Vision analysis skipped: ${err.message})"
                    }
                } else {
                    _userFeedbackMessage.value = "Failed to load selected photo."
                }
            } catch (e: Exception) {
                _userFeedbackMessage.value = "Failed to load selected photo: ${e.message}"
            }
        }
    }

    fun analyzeAndAttachPhotoToCurrentNote(
        bitmap: Bitmap,
        savedPath: String,
        noteId: String,
        onSynthesized: (ocr: String?, desc: String?, labels: List<String>) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _userFeedbackMessage.value = "Analyzing photo with Gemini AI..."
            val result = geminiService.analyzeImageMemory(bitmap)
            result.onSuccess { (ocr, desc, labels) ->
                withContext(Dispatchers.Main) {
                    onSynthesized(ocr, desc, labels)
                    _userFeedbackMessage.value = "Photo analyzed! Description saved in note."
                }
            }.onFailure { err ->
                withContext(Dispatchers.Main) {
                    onSynthesized(null, null, emptyList())
                    _userFeedbackMessage.value = "Photo attached (Analysis offline: ${err.message})"
                }
            }
        }
    }

    // --- Note Detail / Editor Flow ---
    fun openNoteEditor(note: NoteWithDetails? = null) {
        noteBeingEdited.value = note
        isNoteEditorOpen.value = true
    }

    fun openNoteById(noteId: String) {
        viewModelScope.launch {
            val note = repository.getNoteByIdOnce(noteId)
            if (note != null) {
                openNoteEditor(note)
            } else {
                _userFeedbackMessage.value = "Note not found."
            }
        }
    }

    private var autoSaveJob: kotlinx.coroutines.Job? = null

    fun closeNoteEditor() {
        autoSaveJob?.cancel()
        isNoteEditorOpen.value = false
        noteBeingEdited.value = null
    }

    fun saveFullNote(
        id: String?,
        title: String,
        content: String,
        category: String,
        linkedPersonIds: List<String>,
        rawTranscription: String? = null,
        mediaAttachments: List<MediaEntity> = emptyList(),
        actionItems: List<ActionItemEntity> = emptyList(),
        tags: List<String> = emptyList(),
        projectId: String? = null
    ) {
        autoSaveJob?.cancel()
        val wasOpenedAsNewNote = noteBeingEdited.value == null
        viewModelScope.launch {
            val noteId = id ?: UUID.randomUUID().toString()
            val combinedInput = listOf(title.trim(), content.trim()).filter { it.isNotBlank() }.joinToString("\n")

            if (wasOpenedAsNewNote && combinedInput.isNotBlank() && mediaAttachments.isEmpty()) {
                val otherNotes = repository.allNotes.first().filter { it.note.id != noteId }
                val allPeopleNames = repository.allPeople.first()
                    .map { it.name }
                    .filter { com.example.util.MemoryReconciliationEngine.isValidPersonName(it) }
                val noteUpdates = com.example.util.MemoryReconciliationEngine.findNoteUpdatesForInput(
                    newInputText = combinedInput,
                    existingNotes = otherNotes,
                    knownPeopleNames = allPeopleNames
                )
                if (noteUpdates.isNotEmpty()) {
                    repository.applyNoteUpdates(noteUpdates, rawTranscription = rawTranscription)
                    // Clean up any auto-saved temporary draft for this noteId
                    repository.deleteNote(noteId)
                    closeNoteEditor()
                    val firstUpdate = noteUpdates.first()
                    _userFeedbackMessage.value = "Updated existing note: '${firstUpdate.originalTitle}' → '${firstUpdate.updatedTitle}'"
                    return@launch
                }
            }

            val existing = if (id != null) repository.getNoteByIdOnce(id) else null

            // Auto-link any mentioned known people or strictly validated person names
            val resolvedPersonIds = linkedPersonIds.toMutableList()
            if (combinedInput.isNotBlank()) {
                val existingPeople = repository.allPeople.first()
                val validKnownNames = existingPeople.map { it.name }
                    .filter { com.example.util.MemoryReconciliationEngine.isValidPersonName(it) }
                val mentionedNames = com.example.util.MemoryReconciliationEngine.extractCandidateNames(
                    combinedInput,
                    validKnownNames
                )
                for (name in mentionedNames) {
                    if (!com.example.util.MemoryReconciliationEngine.isValidPersonName(name, validKnownNames)) continue
                    val matchedPerson = existingPeople.firstOrNull { it.name.equals(name, ignoreCase = true) }
                    if (matchedPerson != null) {
                        if (!resolvedPersonIds.contains(matchedPerson.id)) {
                            resolvedPersonIds.add(matchedPerson.id)
                        }
                    } else {
                        val created = PersonEntity(
                            id = UUID.randomUUID().toString(),
                            name = name.trim(),
                            relationship = "Contact",
                            factsJson = com.example.data.local.converter.RoomConverters().fromStringList(listOf(combinedInput.take(140))),
                            lastContactedAt = System.currentTimeMillis()
                        )
                        repository.savePerson(created)
                        resolvedPersonIds.add(created.id)
                    }
                }
            }

            val note = NoteEntity(
                id = noteId,
                title = title.ifBlank {
                    content.lines().firstOrNull { it.isNotBlank() }?.take(45) ?: "Untitled Note"
                },
                content = content,
                rawTranscription = rawTranscription ?: existing?.note?.rawTranscription,
                category = category,
                tagsJson = NoteEntity.formatTagsJson(tags),
                isPinned = existing?.note?.isPinned ?: false,
                aiSummary = existing?.note?.aiSummary ?: if (content.length > 100) content.take(120) + "..." else null,
                createdAt = existing?.note?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                memoryType = existing?.note?.memoryType ?: com.example.data.model.UnifiedMemoryType.inferFromLegacyCategory(
                    category = category,
                    hasLocation = tags.any { it.startsWith("loc:") },
                    hasAudio = rawTranscription != null,
                    hasImage = mediaAttachments.isNotEmpty(),
                    contentHint = "$title $content"
                ).name,
                occurredAt = existing?.note?.occurredAt ?: System.currentTimeMillis(),
                sourceType = existing?.note?.sourceType ?: if (!rawTranscription.isNullOrBlank()) {
                    com.example.data.model.MemorySourceType.AUDIO_TRANSCRIPT.name
                } else {
                    com.example.data.model.MemorySourceType.USER_EXPLICIT.name
                },
                confidenceScore = existing?.note?.confidenceScore ?: 1.0f,
                verificationStatus = existing?.note?.verificationStatus
                    ?: com.example.data.model.MemoryVerificationStatus.VERIFIED_FACT.name,
                projectId = projectId ?: existing?.note?.projectId,
                placeId = existing?.note?.placeId
            )
            repository.saveNote(
                note = note,
                linkedPersonIds = resolvedPersonIds.distinct(),
                mediaAttachments = mediaAttachments,
                actionItems = actionItems
            )
            closeNoteEditor()
            _userFeedbackMessage.value = "Memory saved with ${tags.size} tag(s)."
        }
    }

    fun autoSaveNote(
        id: String,
        title: String,
        content: String,
        category: String,
        linkedPersonIds: List<String>,
        rawTranscription: String? = null,
        mediaAttachments: List<MediaEntity> = emptyList(),
        actionItems: List<ActionItemEntity> = emptyList(),
        tags: List<String> = emptyList(),
        projectId: String? = null
    ) {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            val existing = repository.getNoteByIdOnce(id)
            val fallbackTitle = content.lines().firstOrNull { it.isNotBlank() }?.take(45) ?: "Untitled Note"

            val note = NoteEntity(
                id = id,
                title = title.ifBlank { existing?.note?.title?.takeIf { it != "Untitled Note" } ?: fallbackTitle },
                content = content,
                rawTranscription = rawTranscription ?: existing?.note?.rawTranscription,
                category = category,
                tagsJson = NoteEntity.formatTagsJson(tags),
                isPinned = existing?.note?.isPinned ?: false,
                aiSummary = existing?.note?.aiSummary ?: if (content.length > 100) content.take(120) + "..." else null,
                createdAt = existing?.note?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                memoryType = existing?.note?.memoryType ?: com.example.data.model.UnifiedMemoryType.inferFromLegacyCategory(
                    category = category,
                    hasLocation = tags.any { it.startsWith("loc:") },
                    hasAudio = rawTranscription != null,
                    hasImage = mediaAttachments.isNotEmpty(),
                    contentHint = "$title $content"
                ).name,
                occurredAt = existing?.note?.occurredAt ?: System.currentTimeMillis(),
                sourceType = existing?.note?.sourceType ?: if (!rawTranscription.isNullOrBlank()) {
                    com.example.data.model.MemorySourceType.AUDIO_TRANSCRIPT.name
                } else {
                    com.example.data.model.MemorySourceType.USER_EXPLICIT.name
                },
                confidenceScore = existing?.note?.confidenceScore ?: 1.0f,
                verificationStatus = existing?.note?.verificationStatus
                    ?: com.example.data.model.MemoryVerificationStatus.VERIFIED_FACT.name,
                projectId = projectId ?: existing?.note?.projectId,
                placeId = existing?.note?.placeId
            )
            repository.saveNote(
                note = note,
                linkedPersonIds = linkedPersonIds,
                mediaAttachments = mediaAttachments,
                actionItems = actionItems
            )
        }
    }

    val isCleaningUpNote = MutableStateFlow(false)

    fun aiCleanUpNote(rawText: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            isCleaningUpNote.value = true
            _userFeedbackMessage.value = "Cleaning up with AI..."
            try {
                val res = geminiService.cleanUpNoteText(rawText)
                val cleaned = res.getOrElse { geminiService.localFallbackCleanUp(rawText) }
                onResult(cleaned)
                _userFeedbackMessage.value = "Cleaned into a single precise output."
            } catch (e: Throwable) {
                val fallback = geminiService.localFallbackCleanUp(rawText)
                onResult(fallback)
                _userFeedbackMessage.value = "Cleaned into a single precise output."
            } finally {
                isCleaningUpNote.value = false
            }
        }
    }

    fun smartTagNote(
        text: String,
        onResult: (title: String, category: String, people: List<String>, tags: List<String>) -> Unit
    ) {
        viewModelScope.launch {
            _userFeedbackMessage.value = "Analyzing note & generating smart tags..."
            try {
                val allPeopleList = repository.allPeople.first()
                    .map { it.name }
                    .filter { com.example.util.MemoryReconciliationEngine.isValidPersonName(it) }
                val res = geminiService.smartTagAndExtract(text, allPeopleList)
                val extraction = res.getOrElse {
                    geminiService.localFallbackSmartTag(text, allPeopleList)
                }
                onResult(extraction.title, extraction.category, extraction.mentionedPeople, extraction.tags)
                val tagPreview = if (extraction.tags.isNotEmpty()) {
                    "#" + extraction.tags.take(3).joinToString(" #")
                } else extraction.category
                _userFeedbackMessage.value = "Smart tags applied: $tagPreview"
            } catch (e: Throwable) {
                val fallback = geminiService.localFallbackSmartTag(text, emptyList())
                onResult(fallback.title, fallback.category, fallback.mentionedPeople, fallback.tags)
                _userFeedbackMessage.value = "Smart tags applied: ${fallback.category}"
            }
        }
    }

    fun synthesizePersonDossier(person: PersonEntity) {
        viewModelScope.launch {
            isSynthesizingProfile.value = true
            _userFeedbackMessage.value = "Synthesizing profile for ${person.name}..."
            val notesForPerson = repository.getNotesForPerson(person.id).first()
            val combinedNotes = notesForPerson.joinToString("\n---\n") {
                "Note: ${it.note.title} (${it.note.category}):\n${it.note.content}"
            }

            if (combinedNotes.isBlank()) {
                isSynthesizingProfile.value = false
                _userFeedbackMessage.value = "No notes recorded yet for ${person.name} to synthesize."
                return@launch
            }

            val converters = com.example.data.local.converter.RoomConverters()
            val existingFacts = converters.toStringList(person.factsJson)
            val existingDates = converters.toStringMap(person.keyDatesJson)

            val res = geminiService.synthesizePersonProfile(
                personName = person.name,
                notesText = combinedNotes,
                existingFacts = existingFacts,
                existingDates = existingDates
            )
            isSynthesizingProfile.value = false

            res.onSuccess { (facts, dates) ->
                val beforeCount = existingFacts.size
                repository.updatePersonFactsAndDates(person.id, facts, dates)
                val updatedPerson = repository.getPersonByIdOnce(person.id)
                if (updatedPerson != null) {
                    _selectedPerson.value = updatedPerson
                    val afterCount = converters.toStringList(updatedPerson.factsJson).size
                    val netNew = (afterCount - beforeCount).coerceAtLeast(0)
                    _userFeedbackMessage.value = if (netNew > 0) {
                        "Dossier synthesized! $netNew new unique fact(s) added ($afterCount total)."
                    } else {
                        "Dossier up to date & deduplicated ($afterCount unique facts)."
                    }
                } else {
                    _userFeedbackMessage.value = "Dossier synthesized!"
                }
            }.onFailure { err ->
                _userFeedbackMessage.value = "Profile synthesis failed: ${err.message}"
            }
        }
    }

    fun clearChatHistory() {
        _chatMessages.value = listOf(
            ChatMessage(
                sender = MessageSender.CONCIERGE,
                text = "Chat history cleared. How can I help you explore your personal memories?",
                timestamp = System.currentTimeMillis()
            )
        )
        _userFeedbackMessage.value = "Chat reset."
    }

    // --- Note CRUD ---
    fun saveNewNote(
        title: String,
        content: String,
        category: String,
        linkedPersonIds: List<String>
    ) {
        viewModelScope.launch {
            val combinedInput = listOf(title.trim(), content.trim()).filter { it.isNotBlank() }.joinToString("\n")
            val existingNotes = repository.allNotes.first()
            val existingPeople = repository.allPeople.first()
            val allPeopleNames = existingPeople.map { it.name }
                .filter { com.example.util.MemoryReconciliationEngine.isValidPersonName(it) }

            if (combinedInput.isNotBlank()) {
                val noteUpdates = com.example.util.MemoryReconciliationEngine.findNoteUpdatesForInput(
                    newInputText = combinedInput,
                    existingNotes = existingNotes,
                    knownPeopleNames = allPeopleNames
                )
                if (noteUpdates.isNotEmpty()) {
                    repository.applyNoteUpdates(noteUpdates, rawTranscription = combinedInput)
                    val firstUpdate = noteUpdates.first()
                    _userFeedbackMessage.value = "Updated existing note: '${firstUpdate.originalTitle}' → '${firstUpdate.updatedTitle}'"
                    return@launch
                }
            }

            val resolvedPersonIds = linkedPersonIds.toMutableList()
            if (combinedInput.isNotBlank()) {
                val mentionedNames = com.example.util.MemoryReconciliationEngine.extractCandidateNames(
                    combinedInput,
                    allPeopleNames
                )
                for (name in mentionedNames) {
                    if (!com.example.util.MemoryReconciliationEngine.isValidPersonName(name, allPeopleNames)) continue
                    val matchedPerson = existingPeople.firstOrNull { it.name.equals(name, ignoreCase = true) }
                    if (matchedPerson != null) {
                        if (!resolvedPersonIds.contains(matchedPerson.id)) {
                            resolvedPersonIds.add(matchedPerson.id)
                        }
                    } else {
                        val created = PersonEntity(
                            id = UUID.randomUUID().toString(),
                            name = name.trim(),
                            relationship = "Contact",
                            factsJson = com.example.data.local.converter.RoomConverters().fromStringList(listOf(combinedInput.take(140))),
                            lastContactedAt = System.currentTimeMillis()
                        )
                        repository.savePerson(created)
                        resolvedPersonIds.add(created.id)
                    }
                }
            }

            val note = NoteEntity(
                title = title.ifBlank {
                    content.lines().firstOrNull { it.isNotBlank() }?.take(45) ?: "Untitled Note"
                },
                content = content,
                category = category,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repository.saveNote(note, resolvedPersonIds.distinct())
            _userFeedbackMessage.value = "Note saved!"
        }
    }

    fun togglePin(note: NoteEntity) {
        viewModelScope.launch {
            repository.togglePinNote(note)
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            repository.deleteNote(noteId)
            if (noteBeingEdited.value?.note?.id == noteId) {
                noteBeingEdited.value = null
                isNoteEditorOpen.value = false
            }
            _userFeedbackMessage.value = "Memory deleted."
        }
    }

    fun savePerson(person: PersonEntity) {
        viewModelScope.launch {
            val saved = repository.savePerson(person)
            if (_selectedPerson.value?.id == person.id) {
                _selectedPerson.value = saved
            }
        }
    }

    fun deletePerson(personId: String) {
        viewModelScope.launch {
            repository.deletePerson(personId)
            if (_selectedPerson.value?.id == personId) {
                _selectedPerson.value = null
            }
            _userFeedbackMessage.value = "Contact profile deleted."
        }
    }

    fun cleanupDuplicateNotesAndInvalidContacts() {
        viewModelScope.launch {
            val (purgedPeople, mergedNotes) = repository.cleanupBogusPeopleAndDuplicateNotes()
            if (_selectedPerson.value != null) {
                val stillExists = repository.getPersonByIdOnce(_selectedPerson.value!!.id)
                if (stillExists == null) {
                    _selectedPerson.value = null
                }
            }
            _userFeedbackMessage.value = when {
                purgedPeople > 0 && mergedNotes > 0 ->
                    "Cleaned up $purgedPeople invalid contact(s) & merged $mergedNotes duplicate note(s)."
                purgedPeople > 0 ->
                    "Removed $purgedPeople invalid contact(s)."
                mergedNotes > 0 ->
                    "Merged $mergedNotes duplicate note(s)."
                else ->
                    "All contacts and notes are already clean & deduplicated."
            }
        }
    }

    fun toggleActionItem(id: String, completed: Boolean) {
        viewModelScope.launch {
            repository.toggleActionItem(id, completed)
        }
    }

    fun createQuickActionItem(task: String) {
        val clean = task.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            repository.addActionItem(clean)
            _userFeedbackMessage.value = "Task added: $clean"
        }
    }

    fun createRichActionItem(
        task: String,
        priority: com.example.util.TaskPriority = com.example.util.TaskPriority.MEDIUM,
        column: com.example.util.TaskBoardColumn = com.example.util.TaskBoardColumn.TODAY,
        dueDate: Long? = null,
        personId: String? = null,
        projectId: String? = null,
        noteId: String? = null
    ) {
        val clean = task.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            val id = UUID.randomUUID().toString()
            val effectiveDueDate = dueDate ?: when (column) {
                com.example.util.TaskBoardColumn.TODAY -> System.currentTimeMillis()
                com.example.util.TaskBoardColumn.THIS_WEEK -> System.currentTimeMillis() + 3L * 24L * 3600L * 1000L
                else -> null
            }
            val entity = ActionItemEntity(
                id = id,
                noteId = noteId,
                task = clean,
                dueDate = effectiveDueDate,
                isCompleted = column == com.example.util.TaskBoardColumn.DONE
            )
            database.actionItemDao().insertActionItems(listOf(entity))
            val updatedMeta = _taskMetaMap.value.toMutableMap()
            updatedMeta[id] = com.example.util.TaskExtendedMeta(
                taskId = id,
                priority = priority,
                columnOverride = column,
                personId = personId,
                projectId = projectId
            )
            _taskMetaMap.value = updatedMeta
            com.example.util.RecallLifeOrganiserManager.saveTaskMetaMap(getApplication(), updatedMeta)
            _userFeedbackMessage.value = "Task added to ${column.label}: $clean"
        }
    }

    fun updateTaskMeta(
        taskId: String,
        priority: com.example.util.TaskPriority? = null,
        column: com.example.util.TaskBoardColumn? = null,
        personId: String? = null,
        projectId: String? = null
    ) {
        val currentMap = _taskMetaMap.value.toMutableMap()
        val existing = currentMap[taskId] ?: com.example.util.TaskExtendedMeta(taskId = taskId)
        val updated = existing.copy(
            priority = priority ?: existing.priority,
            columnOverride = column ?: existing.columnOverride,
            personId = personId ?: existing.personId,
            projectId = projectId ?: existing.projectId
        )
        currentMap[taskId] = updated
        _taskMetaMap.value = currentMap
        com.example.util.RecallLifeOrganiserManager.saveTaskMetaMap(getApplication(), currentMap)
        if (column == com.example.util.TaskBoardColumn.DONE) {
            toggleActionItem(taskId, true)
        } else if (column != null) {
            toggleActionItem(taskId, false)
        }
    }

    fun deleteActionItem(id: String) {
        viewModelScope.launch {
            repository.deleteActionItem(id)
            val currentMap = _taskMetaMap.value.toMutableMap()
            if (currentMap.remove(id) != null) {
                _taskMetaMap.value = currentMap
                com.example.util.RecallLifeOrganiserManager.saveTaskMetaMap(getApplication(), currentMap)
            }
        }
    }

    // --- Dynamic Lists Operations (Phase 7) ---
    fun createDynamicList(
        title: String,
        type: com.example.util.DynamicListType,
        initialItems: List<String> = emptyList(),
        sourceNoteId: String? = null
    ) {
        val cleanTitle = title.trim().ifBlank { type.displayName }
        val newList = com.example.util.DynamicList(
            id = UUID.randomUUID().toString(),
            title = cleanTitle,
            type = type,
            items = initialItems.mapNotNull { it.trim().takeIf { s -> s.isNotBlank() } }.map {
                com.example.util.DynamicListItem(text = it)
            },
            sourceNoteId = sourceNoteId,
            updatedAt = System.currentTimeMillis()
        )
        val updated = listOf(newList) + _dynamicLists.value
        _dynamicLists.value = updated
        com.example.util.RecallLifeOrganiserManager.saveDynamicLists(getApplication(), updated)
        _userFeedbackMessage.value = "${type.icon} Created list: $cleanTitle"
    }

    fun addDynamicListItem(listId: String, itemText: String) {
        val clean = itemText.trim()
        if (clean.isBlank()) return
        // Support comma or newline separated multi-item quick entry
        val parts = if (clean.contains("\n") || clean.contains(",")) {
            com.example.util.RecallLifeOrganiserManager.extractListItemsFromText(clean)
        } else {
            listOf(clean)
        }
        val updated = _dynamicLists.value.map { list ->
            if (list.id == listId) {
                val newItems = parts.map { com.example.util.DynamicListItem(text = it) }
                list.copy(items = list.items + newItems, updatedAt = System.currentTimeMillis())
            } else list
        }
        _dynamicLists.value = updated
        com.example.util.RecallLifeOrganiserManager.saveDynamicLists(getApplication(), updated)
    }

    fun toggleDynamicListItem(listId: String, itemId: String) {
        val updated = _dynamicLists.value.map { list ->
            if (list.id == listId) {
                val newItems = list.items.map { item ->
                    if (item.id == itemId) item.copy(isCompleted = !item.isCompleted) else item
                }
                list.copy(items = newItems, updatedAt = System.currentTimeMillis())
            } else list
        }
        _dynamicLists.value = updated
        com.example.util.RecallLifeOrganiserManager.saveDynamicLists(getApplication(), updated)
    }

    fun clearCompletedInDynamicList(listId: String) {
        val updated = _dynamicLists.value.map { list ->
            if (list.id == listId) {
                list.copy(items = list.items.filterNot { it.isCompleted }, updatedAt = System.currentTimeMillis())
            } else list
        }
        _dynamicLists.value = updated
        com.example.util.RecallLifeOrganiserManager.saveDynamicLists(getApplication(), updated)
        _userFeedbackMessage.value = "Cleared completed list items."
    }

    fun deleteDynamicList(listId: String) {
        val updated = _dynamicLists.value.filterNot { it.id == listId }
        _dynamicLists.value = updated
        com.example.util.RecallLifeOrganiserManager.saveDynamicLists(getApplication(), updated)
        _userFeedbackMessage.value = "List deleted."
    }

    // --- Phase 8: Learned Personal Preferences Operations ---
    fun saveOrVerifyLearnedPreference(item: com.example.util.LearnedPreferenceItem) {
        val current = _userSavedPreferences.value.toMutableList()
        val idx = current.indexOfFirst { it.id == item.id }
        val verifiedItem = item.copy(isUserVerified = true, isDismissed = false, updatedAt = System.currentTimeMillis())
        if (idx >= 0) {
            current[idx] = verifiedItem
        } else {
            current.add(0, verifiedItem)
        }
        _userSavedPreferences.value = current
        com.example.util.RecallLifeOrganiserManager.saveUserPreferences(getApplication(), current)
        _userFeedbackMessage.value = "✓ Preference saved & confirmed."
    }

    fun deleteOrDismissLearnedPreference(item: com.example.util.LearnedPreferenceItem) {
        val current = _userSavedPreferences.value.toMutableList()
        val idx = current.indexOfFirst { it.id == item.id }
        if (idx >= 0) {
            if (item.id.startsWith("obs_")) {
                current[idx] = item.copy(isDismissed = true, updatedAt = System.currentTimeMillis())
            } else {
                current.removeAt(idx)
            }
        } else {
            current.add(item.copy(isDismissed = true, updatedAt = System.currentTimeMillis()))
        }
        _userSavedPreferences.value = current
        com.example.util.RecallLifeOrganiserManager.saveUserPreferences(getApplication(), current)
        _userFeedbackMessage.value = "Preference removed."
    }

    fun updateUserProfile(displayName: String, focusGoal: String) {
        val app = getApplication<Application>()
        com.example.util.RecallLifeOrganiserManager.setUserDisplayName(app, displayName)
        com.example.util.RecallLifeOrganiserManager.setUserFocusGoal(app, focusGoal)
        _userDisplayName.value = displayName.trim()
        _userFocusGoal.value = focusGoal.trim()
        _userFeedbackMessage.value = "Personal profile updated."
    }

    fun assignCommitmentDueDate(commitment: com.example.data.local.entity.CommitmentEntity, dueDateMillis: Long) {
        viewModelScope.launch {
            val updated = commitment.copy(dueDate = dueDateMillis)
            repository.saveCommitment(updated)
            _userFeedbackMessage.value = "📅 Assigned due date to commitment!"
        }
    }

    fun importBackupJson(json: String) {
        viewModelScope.launch {
            val res = repository.importJsonBackup(json)
            res.onSuccess { count ->
                _userFeedbackMessage.value = "Restored $count records from JSON backup!"
            }.onFailure { err ->
                _userFeedbackMessage.value = "Failed to import JSON: ${err.message}"
            }
        }
    }

    fun showFeedback(message: String) {
        _userFeedbackMessage.value = message
    }

    // --- Reminders Operations & Tone Previews ---

    fun getActiveReminderForNote(noteId: String): Flow<com.example.data.local.entity.ReminderEntity?> {
        return repository.getActiveReminderForNote(noteId)
    }

    fun scheduleReminder(
        title: String,
        details: String = "",
        noteId: String? = null,
        timeMillis: Long,
        tone: com.example.data.local.entity.ReminderTone = com.example.data.local.entity.ReminderTone.DIGITAL_CRYSTAL,
        vibratePattern: com.example.data.local.entity.ReminderVibratePattern = com.example.data.local.entity.ReminderVibratePattern.STRONG_PULSE,
        recurrence: String = "NONE",
        onCompleted: ((com.example.data.local.entity.ReminderEntity) -> Unit)? = null
    ) {
        val reminder = com.example.data.local.entity.ReminderEntity(
            title = title.ifBlank { "Recall Memory Reminder" },
            details = details,
            noteId = noteId,
            reminderTimeMillis = timeMillis,
            toneType = tone.name,
            vibratePattern = vibratePattern.name
        )
        viewModelScope.launch {
            repository.insertReminder(reminder)
            if (recurrence != "NONE") {
                com.example.util.RecallLifeOrganiserManager.setReminderRecurrence(getApplication(), reminder.id, recurrence)
                _reminderRecurrenceMap.value = com.example.util.RecallLifeOrganiserManager.loadReminderRecurrenceMap(getApplication())
            }
            com.example.util.ReminderNotificationHelper.scheduleReminderAlarm(getApplication(), reminder)
            val recLabel = if (recurrence != "NONE") " ($recurrence)" else ""
            _userFeedbackMessage.value = "Reminder scheduled$recLabel with ${tone.displayName} tone!"
            onCompleted?.invoke(reminder)
        }
    }

    fun updateReminder(reminder: com.example.data.local.entity.ReminderEntity) {
        viewModelScope.launch {
            repository.updateReminder(reminder)
            if (!reminder.isCompleted && reminder.reminderTimeMillis >= System.currentTimeMillis() - 60_000L) {
                com.example.util.ReminderNotificationHelper.scheduleReminderAlarm(getApplication(), reminder)
            } else {
                com.example.util.ReminderNotificationHelper.cancelReminderAlarm(getApplication(), reminder.id)
            }
            _userFeedbackMessage.value = "Reminder updated."
        }
    }

    fun toggleReminderCompleted(reminder: com.example.data.local.entity.ReminderEntity, completed: Boolean) {
        viewModelScope.launch {
            val recurrence = _reminderRecurrenceMap.value[reminder.id] ?: "NONE"
            if (completed && recurrence != "NONE") {
                // Phase 9: Recurring reminder automatically advances to next cycle when completed
                val cal = java.util.Calendar.getInstance().apply {
                    timeInMillis = maxOf(reminder.reminderTimeMillis, System.currentTimeMillis())
                }
                when (recurrence) {
                    "DAILY" -> cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                    "WEEKLY" -> cal.add(java.util.Calendar.DAY_OF_YEAR, 7)
                    "MONTHLY" -> cal.add(java.util.Calendar.MONTH, 1)
                }
                val advanced = reminder.copy(reminderTimeMillis = cal.timeInMillis, isCompleted = false)
                repository.updateReminder(advanced)
                com.example.util.ReminderNotificationHelper.scheduleReminderAlarm(getApplication(), advanced)
                _userFeedbackMessage.value = "↻ Recurring ($recurrence) reminder advanced to ${DateUtils.formatDayMonth(cal.timeInMillis)}!"
                return@launch
            }

            repository.setReminderCompleted(reminder.id, completed)
            if (completed) {
                com.example.util.ReminderNotificationHelper.cancelReminderAlarm(getApplication(), reminder.id)
                _userFeedbackMessage.value = "Reminder completed."
            } else {
                if (reminder.reminderTimeMillis >= System.currentTimeMillis() - 60_000L) {
                    com.example.util.ReminderNotificationHelper.scheduleReminderAlarm(getApplication(), reminder.copy(isCompleted = false))
                }
                _userFeedbackMessage.value = "Reminder restored."
            }
        }
    }

    fun snoozeReminder(reminderId: String, snoozeMinutes: Int = 10) {
        val newTime = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)
        viewModelScope.launch {
            repository.snoozeReminder(reminderId, newTime)
            val updated = repository.getReminderById(reminderId)
            if (updated != null) {
                com.example.util.ReminderNotificationHelper.scheduleReminderAlarm(getApplication(), updated)
                _userFeedbackMessage.value = "Reminder snoozed for $snoozeMinutes min."
            }
        }
    }

    fun deleteReminder(reminderId: String) {
        viewModelScope.launch {
            com.example.util.ReminderNotificationHelper.cancelReminderAlarm(getApplication(), reminderId)
            repository.deleteReminderById(reminderId)
            com.example.util.RecallLifeOrganiserManager.setReminderRecurrence(getApplication(), reminderId, "NONE")
            _reminderRecurrenceMap.value = com.example.util.RecallLifeOrganiserManager.loadReminderRecurrenceMap(getApplication())
            _userFeedbackMessage.value = "Reminder deleted."
        }
    }

    fun previewReminderTone(
        tone: com.example.data.local.entity.ReminderTone,
        vibratePattern: com.example.data.local.entity.ReminderVibratePattern
    ) {
        com.example.util.ReminderToneManager.previewPhysicalTone(getApplication(), tone, vibratePattern)
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            val app = getApplication<android.app.Application>()
            com.example.util.PrivacySecurityManager.purgeAllRawAudioFiles(app)
            com.example.util.PrivacySecurityManager.recordAuditEvent(
                app,
                "Cleared All Data",
                "User wiped all local memories and raw audio files"
            )
            clearSelectedPerson()
            clearCalendarFilter()
            clearSmartSearch()
            _dynamicLists.value = emptyList()
            com.example.util.RecallLifeOrganiserManager.saveDynamicLists(app, emptyList())
            _userFeedbackMessage.value = "All data and raw audio cleared. Recall is fresh and ready."
        }
    }
}
