package com.example.util

import android.content.Context
import com.example.data.local.entity.ActionItemEntity
import com.example.data.local.entity.CommitmentEntity
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
import java.util.UUID

enum class DynamicListType(val displayName: String, val icon: String) {
    SHOPPING("Shopping List", "🛒"),
    TODO("To-Do List", "✅"),
    IDEAS("Ideas List", "💡"),
    READING("Reading List", "📚"),
    WATCH("Watch List", "🎬"),
    CUSTOM("Custom List", "📋")
}

data class DynamicListItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class DynamicList(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val type: DynamicListType = DynamicListType.TODO,
    val items: List<DynamicListItem> = emptyList(),
    val sourceNoteId: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val pendingCount: Int get() = items.count { !it.isCompleted }
    val completedCount: Int get() = items.count { it.isCompleted }
}

enum class CommitmentClassification(val label: String, val badgeIcon: String) {
    EXPLICIT_PROMISE("Explicit Promise", "🤝"),
    PERSONAL_INTENTION("Personal Intention", "🎯"),
    TASK("Actionable Task", "✅"),
    FOLLOW_UP("Follow-Up", "🔄"),
    CONDITIONAL("Conditional Commitment", "⏳"),
    UNDATED("Undated Commitment", "📅")
}

enum class TaskBoardColumn(val label: String, val icon: String) {
    TODAY("Today", "⚡"),
    THIS_WEEK("This Week", "📅"),
    QUEUE("Queue", "📥"),
    DONE("Completed", "✓")
}

enum class TaskPriority(val label: String, val badge: String) {
    HIGH("High", "🔴"),
    MEDIUM("Medium", "🟡"),
    LOW("Low", "🔵")
}

data class TaskExtendedMeta(
    val taskId: String,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val columnOverride: TaskBoardColumn? = null,
    val personId: String? = null,
    val projectId: String? = null
)

data class LearnedPreferenceItem(
    val id: String = UUID.randomUUID().toString(),
    val category: String, // e.g., "Communication Style", "Interest", "Preferred Place", "Goal", "Routine"
    val statement: String,
    val evidenceSummary: String,
    val isUserVerified: Boolean = false,
    val isDismissed: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

data class NaturalDateSuggestion(
    val originalText: String,
    val suggestedAction: String,
    val mentionedPersonName: String?,
    val suggestedTimeMillis: Long,
    val formattedDateLabel: String,
    val recurrenceHint: String? = null // "DAILY", "WEEKLY", "MONTHLY" or null
)

data class LifeRewindResult(
    val query: String,
    val headlineSummary: String,
    val matchedNotes: List<NoteWithDetails>,
    val matchedCommitments: List<CommitmentEntity>,
    val matchedPlaces: List<PlaceEntity>,
    val matchedPeople: List<PersonEntity>,
    val timeframeDescription: String
)

object RecallLifeOrganiserManager {

    private const val PREFS_NAME = "recall_life_organiser_prefs"
    private const val KEY_DYNAMIC_LISTS = "dynamic_lists_v1"
    private const val KEY_TASK_META = "task_extended_meta_v1"
    private const val KEY_LEARNED_PREFS = "learned_preferences_v1"
    private const val KEY_REMINDER_RECURRENCE = "reminder_recurrence_v1"
    private const val KEY_USER_PROFILE_NAME = "user_profile_display_name"
    private const val KEY_USER_PROFILE_BIO = "user_profile_focus_goal"
    private const val KEY_WEEKLY_BRIEFING_ENABLED = "weekly_briefing_enabled"

    // --- User Profile Preferences ---
    fun getUserDisplayName(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USER_PROFILE_NAME, "") ?: ""
    }

    fun setUserDisplayName(context: Context, name: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_USER_PROFILE_NAME, name.trim())
            .apply()
    }

    fun getUserFocusGoal(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USER_PROFILE_BIO, "") ?: ""
    }

    fun setUserFocusGoal(context: Context, goal: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_USER_PROFILE_BIO, goal.trim())
            .apply()
    }

    fun isWeeklyBriefingEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_WEEKLY_BRIEFING_ENABLED, true)
    }

    fun setWeeklyBriefingEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_WEEKLY_BRIEFING_ENABLED, enabled)
            .apply()
    }

    // --- Reminder Recurrence Persistence ("NONE", "DAILY", "WEEKLY", "MONTHLY") ---
    fun loadReminderRecurrenceMap(context: Context): Map<String, String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_REMINDER_RECURRENCE, null) ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            val map = mutableMapOf<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = obj.optString(k, "NONE")
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun setReminderRecurrence(context: Context, reminderId: String, recurrence: String) {
        val current = loadReminderRecurrenceMap(context).toMutableMap()
        if (recurrence == "NONE" || recurrence.isBlank()) {
            current.remove(reminderId)
        } else {
            current[reminderId] = recurrence
        }
        val obj = JSONObject()
        current.forEach { (k, v) -> obj.put(k, v) }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_REMINDER_RECURRENCE, obj.toString())
            .apply()
    }

    // --- Dynamic Lists Persistence ---
    fun loadDynamicLists(context: Context): List<DynamicList> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_DYNAMIC_LISTS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val result = mutableListOf<DynamicList>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val itemsArr = obj.optJSONArray("items") ?: JSONArray()
                val items = mutableListOf<DynamicListItem>()
                for (j in 0 until itemsArr.length()) {
                    val itemObj = itemsArr.getJSONObject(j)
                    items.add(
                        DynamicListItem(
                            id = itemObj.optString("id", UUID.randomUUID().toString()),
                            text = itemObj.optString("text", ""),
                            isCompleted = itemObj.optBoolean("isCompleted", false),
                            createdAt = itemObj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
                val typeStr = obj.optString("type", DynamicListType.TODO.name)
                val listType = DynamicListType.entries.find { it.name == typeStr } ?: DynamicListType.TODO
                result.add(
                    DynamicList(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        type = listType,
                        items = items,
                        sourceNoteId = obj.optString("sourceNoteId", "").takeIf { it.isNotBlank() },
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
            result.sortedByDescending { it.updatedAt }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveDynamicLists(context: Context, lists: List<DynamicList>) {
        val arr = JSONArray()
        lists.forEach { list ->
            val obj = JSONObject()
            obj.put("id", list.id)
            obj.put("title", list.title)
            obj.put("type", list.type.name)
            obj.put("sourceNoteId", list.sourceNoteId ?: "")
            obj.put("updatedAt", list.updatedAt)
            val itemsArr = JSONArray()
            list.items.forEach { item ->
                val itemObj = JSONObject()
                itemObj.put("id", item.id)
                itemObj.put("text", item.text)
                itemObj.put("isCompleted", item.isCompleted)
                itemObj.put("createdAt", item.createdAt)
                itemsArr.put(itemObj)
            }
            obj.put("items", itemsArr)
            arr.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DYNAMIC_LISTS, arr.toString())
            .apply()
    }

    /**
     * Extracts a structured list from a note or OCR text if it contains shopping/todo/reading items.
     */
    fun extractListItemsFromText(rawText: String): List<String> {
        if (rawText.isBlank()) return emptyList()
        return rawText
            .split(Regex("[\\r\\n,;]+"))
            .map {
                it.trim()
                    .removePrefix("- [ ]")
                    .removePrefix("[ ]")
                    .removePrefix("-")
                    .removePrefix("*")
                    .removePrefix("•")
                    .replace(Regex("^\\d+[.)]\\s*"), "")
                    .trim()
            }
            .filter { it.length in 2..90 }
            .distinct()
    }

    // --- Commitment Classification (Phase 2) ---
    fun classifyCommitment(commitment: CommitmentEntity): CommitmentClassification {
        val lower = commitment.description.lowercase()
        return when {
            lower.contains("if ") || lower.contains("once ") || lower.contains("when ") || lower.contains("unless ") ->
                CommitmentClassification.CONDITIONAL
            lower.contains("follow up") || lower.contains("ask ") || lower.contains("check in") || lower.contains("remind me to ask") ->
                CommitmentClassification.FOLLOW_UP
            lower.contains("promised") || lower.contains("i'll ") || lower.contains("i will ") || lower.contains("owe ") ->
                CommitmentClassification.EXPLICIT_PROMISE
            lower.contains("should ") || lower.contains("want to ") || lower.contains("plan to ") || lower.contains("intend to ") ->
                CommitmentClassification.PERSONAL_INTENTION
            commitment.dueDate == null ->
                CommitmentClassification.UNDATED
            else ->
                CommitmentClassification.TASK
        }
    }

    // --- Task Kanban Column & Priority Classification (Phase 7) ---
    fun resolveTaskBoardColumn(
        action: ActionItemEntity,
        meta: TaskExtendedMeta?,
        nowMillis: Long = System.currentTimeMillis()
    ): TaskBoardColumn {
        if (action.isCompleted) return TaskBoardColumn.DONE
        meta?.columnOverride?.let { if (it != TaskBoardColumn.DONE) return it }
        val due = action.dueDate ?: return TaskBoardColumn.QUEUE
        val diffDays = (due - nowMillis) / (24L * 3600L * 1000L)
        return when {
            diffDays <= 0L -> TaskBoardColumn.TODAY
            diffDays <= 7L -> TaskBoardColumn.THIS_WEEK
            else -> TaskBoardColumn.QUEUE
        }
    }

    fun loadTaskMetaMap(context: Context): Map<String, TaskExtendedMeta> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_TASK_META, null) ?: return emptyMap()
        return try {
            val arr = JSONArray(raw)
            val map = mutableMapOf<String, TaskExtendedMeta>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val taskId = obj.getString("taskId")
                val prio = TaskPriority.entries.find { it.name == obj.optString("priority") } ?: TaskPriority.MEDIUM
                val col = TaskBoardColumn.entries.find { it.name == obj.optString("column") }
                map[taskId] = TaskExtendedMeta(
                    taskId = taskId,
                    priority = prio,
                    columnOverride = col,
                    personId = obj.optString("personId", "").takeIf { it.isNotBlank() },
                    projectId = obj.optString("projectId", "").takeIf { it.isNotBlank() }
                )
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun saveTaskMetaMap(context: Context, map: Map<String, TaskExtendedMeta>) {
        val arr = JSONArray()
        map.values.forEach { m ->
            val obj = JSONObject()
            obj.put("taskId", m.taskId)
            obj.put("priority", m.priority.name)
            obj.put("column", m.columnOverride?.name ?: "")
            obj.put("personId", m.personId ?: "")
            obj.put("projectId", m.projectId ?: "")
            arr.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TASK_META, arr.toString())
            .apply()
    }

    // --- Phase 8: Learned Personal Preferences & Behavioural Intelligence ---
    fun loadUserSavedPreferences(context: Context): List<LearnedPreferenceItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_LEARNED_PREFS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<LearnedPreferenceItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    LearnedPreferenceItem(
                        id = obj.getString("id"),
                        category = obj.getString("category"),
                        statement = obj.getString("statement"),
                        evidenceSummary = obj.optString("evidenceSummary", "User preference"),
                        isUserVerified = obj.optBoolean("isUserVerified", false),
                        isDismissed = obj.optBoolean("isDismissed", false),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveUserPreferences(context: Context, prefsList: List<LearnedPreferenceItem>) {
        val arr = JSONArray()
        prefsList.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("category", item.category)
            obj.put("statement", item.statement)
            obj.put("evidenceSummary", item.evidenceSummary)
            obj.put("isUserVerified", item.isUserVerified)
            obj.put("isDismissed", item.isDismissed)
            obj.put("updatedAt", item.updatedAt)
            arr.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LEARNED_PREFS, arr.toString())
            .apply()
    }

    /**
     * Combines user-saved preferences with observed behavioural patterns from actual memories,
     * clearly distinguishing observed patterns from user-confirmed preferences.
     */
    fun synthesizeLearnedPreferences(
        userSaved: List<LearnedPreferenceItem>,
        allNotes: List<NoteWithDetails>,
        allPlaces: List<PlaceEntity>,
        allProjects: List<ProjectEntity>
    ): List<LearnedPreferenceItem> {
        val dismissedIds = userSaved.filter { it.isDismissed }.map { it.id }.toSet()
        val activeMap = userSaved.filter { !it.isDismissed }.associateBy { it.id }.toMutableMap()

        // 1. Detect explicit "I prefer / I like / My goal is" statements in notes
        val prefRegex = Regex("\\b(?:i prefer|i always like|my favourite|my favorite|my goal is to|i usually)\\s+([^.!?\\n]{5,90})", RegexOption.IGNORE_CASE)
        for (noteWithDetails in allNotes) {
            val match = prefRegex.find(noteWithDetails.note.content)
            if (match != null) {
                val id = "obs_note_${noteWithDetails.note.id}"
                if (id !in dismissedIds && id !in activeMap) {
                    val snippet = match.value.trim().replaceFirstChar { it.uppercase() }
                    activeMap[id] = LearnedPreferenceItem(
                        id = id,
                        category = if (snippet.lowercase().contains("goal")) "Personal Goal" else "Observed Preference",
                        statement = snippet,
                        evidenceSummary = "Observed in memory \"${noteWithDetails.note.title}\" (${DateUtils.formatDate(noteWithDetails.note.createdAt)})",
                        isUserVerified = false,
                        updatedAt = noteWithDetails.note.createdAt
                    )
                }
            }
        }

        // 2. Detect preferred places from visit counts
        allPlaces.filter { it.visitCount >= 2 }.take(3).forEach { place ->
            val id = "obs_place_${place.id}"
            if (id !in dismissedIds && id !in activeMap) {
                activeMap[id] = LearnedPreferenceItem(
                    id = id,
                    category = "Preferred Place",
                    statement = "Frequently visits ${place.name} (${place.category})",
                    evidenceSummary = "Observed Pattern • ${place.visitCount} recorded visits",
                    isUserVerified = false,
                    updatedAt = place.lastVisitedAt
                )
            }
        }

        // 3. Detect ongoing project focus
        allProjects.filter { it.status.equals("ACTIVE", ignoreCase = true) }.take(3).forEach { proj ->
            val id = "obs_proj_${proj.id}"
            if (id !in dismissedIds && id !in activeMap) {
                activeMap[id] = LearnedPreferenceItem(
                    id = id,
                    category = "Ongoing Focus",
                    statement = "Active focus on ${proj.name}",
                    evidenceSummary = "Observed Pattern • Project status: ${proj.status}",
                    isUserVerified = false,
                    updatedAt = proj.updatedAt
                )
            }
        }

        return activeMap.values.sortedWith(
            compareByDescending<LearnedPreferenceItem> { it.isUserVerified }
                .thenByDescending { it.updatedAt }
        )
    }

    // --- Phase 9: Natural Language Date, Time & Person Reminder Detector ---
    /**
     * Recognises dates, times, and people mentioned naturally in conversation/notes.
     * Example: "I need to call Peter next Friday" -> Person: Peter, Action: Call Peter, Date: Next Friday 9:00 AM
     */
    fun parseNaturalDateTimeSuggestion(
        text: String,
        knownPeopleNames: List<String> = emptyList(),
        nowMillis: Long = System.currentTimeMillis()
    ): NaturalDateSuggestion? {
        if (text.isBlank()) return null
        val lower = text.lowercase()

        val hasTemporalCue = lower.contains("tomorrow") ||
            lower.contains("tonight") ||
            lower.contains("next week") ||
            lower.contains("next monday") ||
            lower.contains("next tuesday") ||
            lower.contains("next wednesday") ||
            lower.contains("next thursday") ||
            lower.contains("next friday") ||
            lower.contains("next saturday") ||
            lower.contains("next sunday") ||
            lower.contains("on monday") ||
            lower.contains("on tuesday") ||
            lower.contains("on wednesday") ||
            lower.contains("on thursday") ||
            lower.contains("on friday") ||
            lower.contains("every day") ||
            lower.contains("every week") ||
            lower.contains("every month")

        if (!hasTemporalCue) return null

        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        var recurrenceHint: String? = null

        when {
            lower.contains("every day") || lower.contains("daily") -> {
                recurrenceHint = "DAILY"
                cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 9)
                cal.set(Calendar.MINUTE, 0)
            }
            lower.contains("every week") || lower.contains("weekly") -> {
                recurrenceHint = "WEEKLY"
                cal.add(Calendar.DAY_OF_YEAR, 7)
                cal.set(Calendar.HOUR_OF_DAY, 9)
                cal.set(Calendar.MINUTE, 0)
            }
            lower.contains("every month") || lower.contains("monthly") -> {
                recurrenceHint = "MONTHLY"
                cal.add(Calendar.MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 9)
                cal.set(Calendar.MINUTE, 0)
            }
            lower.contains("tonight") -> {
                cal.set(Calendar.HOUR_OF_DAY, 20)
                cal.set(Calendar.MINUTE, 0)
                if (cal.timeInMillis <= nowMillis) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            lower.contains("tomorrow") -> {
                cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 9)
                cal.set(Calendar.MINUTE, 0)
            }
            lower.contains("next week") -> {
                cal.add(Calendar.DAY_OF_YEAR, 7)
                cal.set(Calendar.HOUR_OF_DAY, 9)
                cal.set(Calendar.MINUTE, 0)
            }
            else -> {
                val dayMap = mapOf(
                    "monday" to Calendar.MONDAY,
                    "tuesday" to Calendar.TUESDAY,
                    "wednesday" to Calendar.WEDNESDAY,
                    "thursday" to Calendar.THURSDAY,
                    "friday" to Calendar.FRIDAY,
                    "saturday" to Calendar.SATURDAY,
                    "sunday" to Calendar.SUNDAY
                )
                var targetDow: Int? = null
                for ((name, dow) in dayMap) {
                    if (lower.contains(name)) {
                        targetDow = dow
                        break
                    }
                }
                if (targetDow != null) {
                    var daysAhead = (targetDow - cal.get(Calendar.DAY_OF_WEEK) + 7) % 7
                    if (daysAhead == 0 || lower.contains("next ")) {
                        if (daysAhead == 0) daysAhead = 7
                    }
                    cal.add(Calendar.DAY_OF_YEAR, daysAhead)
                    cal.set(Calendar.HOUR_OF_DAY, 9)
                    cal.set(Calendar.MINUTE, 0)
                }
            }
        }

        // Check for explicit hour like "at 3pm" or "at 10am"
        val timeRegex = Regex("\\bat\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)\\b", RegexOption.IGNORE_CASE)
        val timeMatch = timeRegex.find(lower)
        if (timeMatch != null) {
            var hr = timeMatch.groupValues[1].toIntOrNull() ?: 9
            val min = timeMatch.groupValues[2].toIntOrNull() ?: 0
            val ampm = timeMatch.groupValues[3].lowercase()
            if (ampm == "pm" && hr < 12) hr += 12
            if (ampm == "am" && hr == 12) hr = 0
            cal.set(Calendar.HOUR_OF_DAY, hr)
            cal.set(Calendar.MINUTE, min)
        }
        cal.set(Calendar.SECOND, 0)

        val candidates = MemoryReconciliationEngine.extractCandidateNames(text, knownPeopleNames)
        val mentionedPerson = candidates.firstOrNull()

        val cleanAction = text.trim()
            .replace(Regex("^(?:i need to|i should|remind me to|i promised to|i will|i'll)\\s+", RegexOption.IGNORE_CASE), "")
            .replaceFirstChar { it.uppercase() }
            .take(80)

        val fmt = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault())
        return NaturalDateSuggestion(
            originalText = text.trim(),
            suggestedAction = cleanAction,
            mentionedPersonName = mentionedPerson,
            suggestedTimeMillis = cal.timeInMillis,
            formattedDateLabel = fmt.format(Date(cal.timeInMillis)),
            recurrenceHint = recurrenceHint
        )
    }

    // --- Phase 3: Life Rewind Chronological & Episodic Retrieval Engine ---
    fun executeLifeRewindQuery(
        query: String,
        allNotes: List<NoteWithDetails>,
        allPeople: List<PersonEntity>,
        allPlaces: List<PlaceEntity>,
        allCommitments: List<CommitmentEntity>,
        nowMillis: Long = System.currentTimeMillis()
    ): LifeRewindResult {
        val q = query.trim()
        val lower = q.lowercase()
        val oneDayMs = 24L * 3600L * 1000L

        // 1. Identify mentioned people or places
        val matchedPeople = allPeople.filter { p ->
            lower.contains(p.name.lowercase()) ||
                p.name.split(" ").any { part -> part.length >= 3 && lower.contains(part.lowercase()) }
        }
        val matchedPlaces = allPlaces.filter { pl ->
            lower.contains(pl.name.lowercase())
        }

        // 2. Identify time window if any
        var startMillis: Long? = null
        var endMillis: Long? = null
        var timeframeLabel = "All Recorded History"

        when {
            lower.contains("today") -> {
                startMillis = DateUtils.startOfDay(nowMillis)
                endMillis = nowMillis + oneDayMs
                timeframeLabel = "Today"
            }
            lower.contains("yesterday") -> {
                val yest = nowMillis - oneDayMs
                startMillis = DateUtils.startOfDay(yest)
                endMillis = startMillis + oneDayMs
                timeframeLabel = "Yesterday"
            }
            lower.contains("last week") || lower.contains("past week") || lower.contains("this week") -> {
                startMillis = nowMillis - 7L * oneDayMs
                endMillis = nowMillis + oneDayMs
                timeframeLabel = "Past 7 Days"
            }
            lower.contains("last month") || lower.contains("past month") || lower.contains("this month") -> {
                startMillis = nowMillis - 30L * oneDayMs
                endMillis = nowMillis + oneDayMs
                timeframeLabel = "Past 30 Days"
            }
            else -> {
                val dayNames = listOf("sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday")
                val mentionedDayIndex = dayNames.indexOfFirst { lower.contains(it) }
                if (mentionedDayIndex != -1) {
                    val targetCalDow = mentionedDayIndex + 1
                    val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
                    var daysBack = (cal.get(Calendar.DAY_OF_WEEK) - targetCalDow + 7) % 7
                    if (daysBack == 0) daysBack = 7
                    cal.add(Calendar.DAY_OF_YEAR, -daysBack)
                    startMillis = DateUtils.startOfDay(cal.timeInMillis)
                    endMillis = startMillis + oneDayMs
                    val fmt = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
                    timeframeLabel = fmt.format(Date(startMillis))
                }
            }
        }

        // 3. Filter notes by person, place, timeframe, or semantic keywords
        val keywords = q.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter {
                it.length >= 3 && it !in setOf(
                    "what", "when", "where", "who", "did", "was", "were", "doing", "last",
                    "speak", "spoke", "talk", "talked", "tell", "told", "about", "visit",
                    "visited", "happen", "happened", "during", "the", "with", "from", "make", "made"
                )
            }

        val candidateNotes = allNotes.filter { item ->
            val createdTs = item.note.createdAt
            val occurredTs = item.note.occurredAt ?: createdTs
            val inTimeframe = if (startMillis != null && endMillis != null) {
                (createdTs in startMillis..endMillis) || (occurredTs in startMillis..endMillis)
            } else true

            val matchesPerson = matchedPeople.isEmpty() || matchedPeople.any { mp ->
                item.people.any { it.id == mp.id } ||
                    item.note.title.contains(mp.name, ignoreCase = true) ||
                    item.note.content.contains(mp.name, ignoreCase = true)
            }

            val matchesPlace = matchedPlaces.isEmpty() || matchedPlaces.any { pl ->
                item.note.placeId == pl.id ||
                    item.note.getLocationName()?.equals(pl.name, ignoreCase = true) == true ||
                    item.note.content.contains(pl.name, ignoreCase = true)
            }

            val matchesKeyword = if (matchedPeople.isEmpty() && matchedPlaces.isEmpty() && startMillis == null && keywords.isNotEmpty()) {
                keywords.any { kw ->
                    item.note.title.lowercase().contains(kw) ||
                        item.note.content.lowercase().contains(kw) ||
                        item.note.category.lowercase().contains(kw) ||
                        item.note.tagsJson.lowercase().contains(kw)
                }
            } else true

            inTimeframe && matchesPerson && matchesPlace && matchesKeyword
        }.sortedByDescending { it.note.occurredAt ?: it.note.createdAt }

        val peopleIds = matchedPeople.map { it.id }.toSet()
        val matchedCommitments = allCommitments.filter { c ->
            val inTimeframe = if (startMillis != null && endMillis != null) c.createdAt in startMillis..endMillis else true
            val matchesP = peopleIds.isEmpty() || (c.personId != null && c.personId in peopleIds)
            val matchesKw = keywords.isEmpty() || keywords.any { c.description.lowercase().contains(it) }
            inTimeframe && (matchesP && (peopleIds.isNotEmpty() || matchesKw))
        }

        // 4. Build evidence-based summary without inventing facts
        val headline = when {
            candidateNotes.isEmpty() && matchedCommitments.isEmpty() ->
                "No recorded memories found matching \"$q\" in $timeframeLabel. Try broadening your date range or keywords."
            matchedPeople.isNotEmpty() && candidateNotes.isNotEmpty() -> {
                val latest = candidateNotes.first()
                val personNames = matchedPeople.joinToString(", ") { it.name }
                "Found ${candidateNotes.size} memory record(s) involving $personNames. Most recent: \"${latest.note.title}\" on ${DateUtils.formatDate(latest.note.occurredAt ?: latest.note.createdAt)}."
            }
            matchedPlaces.isNotEmpty() && candidateNotes.isNotEmpty() -> {
                val placeNames = matchedPlaces.joinToString(", ") { it.name }
                "Found ${candidateNotes.size} memory record(s) connected to $placeNames."
            }
            else -> {
                val latest = candidateNotes.firstOrNull()
                if (latest != null) {
                    "Retrieved ${candidateNotes.size} memory record(s) ($timeframeLabel). Latest: \"${latest.note.title}\" (${DateUtils.formatDate(latest.note.occurredAt ?: latest.note.createdAt)})."
                } else {
                    "Retrieved ${matchedCommitments.size} commitment record(s) ($timeframeLabel)."
                }
            }
        }

        return LifeRewindResult(
            query = q,
            headlineSummary = headline,
            matchedNotes = candidateNotes,
            matchedCommitments = matchedCommitments,
            matchedPlaces = matchedPlaces,
            matchedPeople = matchedPeople,
            timeframeDescription = timeframeLabel
        )
    }

    // --- Phase 10: Weekly AI Life Review & Synthesis ---
    fun buildWeeklyLifeBriefing(
        allNotes: List<NoteWithDetails>,
        allPeople: List<PersonEntity>,
        allCommitments: List<CommitmentEntity>,
        allActions: List<ActionItemEntity>,
        allProjects: List<ProjectEntity>,
        nowMillis: Long = System.currentTimeMillis()
    ): WeeklyLifeBriefingReport {
        val sevenDaysAgo = nowMillis - 7L * 24L * 3600L * 1000L
        val notesThisWeek = allNotes.filter { it.note.createdAt >= sevenDaysAgo }
        val completedCommitmentsThisWeek = allCommitments.filter { it.isCompleted && it.createdAt >= sevenDaysAgo }
        val openCommitments = allCommitments.filter { !it.isCompleted }
        val pendingTasksCount = allActions.count { !it.isCompleted }

        val personMentionCounts = mutableMapOf<String, Int>()
        notesThisWeek.forEach { n ->
            n.people.forEach { p ->
                personMentionCounts[p.name] = (personMentionCounts[p.name] ?: 0) + 1
            }
        }
        val topPeopleThisWeek = personMentionCounts.entries
            .sortedByDescending { it.value }
            .take(4)
            .map { "${it.key} (${it.value})" }

        val activeProjectNames = allProjects
            .filter { it.status.equals("ACTIVE", ignoreCase = true) }
            .map { it.name }

        val highlights = mutableListOf<String>()
        if (notesThisWeek.isNotEmpty()) {
            highlights.add("Captured ${notesThisWeek.size} memory record(s) over the past 7 days.")
        }
        if (completedCommitmentsThisWeek.isNotEmpty()) {
            highlights.add("Fulfilled ${completedCommitmentsThisWeek.size} commitment(s) this week.")
        }
        if (openCommitments.isNotEmpty()) {
            highlights.add("${openCommitments.size} open promise(s)/commitment(s) carry forward into the week ahead.")
        }
        if (topPeopleThisWeek.isNotEmpty()) {
            highlights.add("Key relationship interactions: ${topPeopleThisWeek.joinToString(", ")}.")
        }
        if (activeProjectNames.isNotEmpty()) {
            highlights.add("Active project momentum on: ${activeProjectNames.take(3).joinToString(", ")}.")
        }
        if (highlights.isEmpty()) {
            highlights.add("Capture daily debriefs, notes, or commitments this week to populate your automated 7-day life review.")
        }

        val markdownExport = buildString {
            appendLine("# Recall — Weekly AI Life Briefing")
            appendLine("Generated: ${DateUtils.formatDate(nowMillis)}")
            appendLine()
            appendLine("## 7-Day Snapshot")
            appendLine("- **Memories Captured (7d):** ${notesThisWeek.size}")
            appendLine("- **Commitments Fulfilled (7d):** ${completedCommitmentsThisWeek.size}")
            appendLine("- **Open Promises & Commitments:** ${openCommitments.size}")
            appendLine("- **Pending Action Tasks:** $pendingTasksCount")
            appendLine()
            appendLine("## Key Weekly Highlights")
            highlights.forEach { h -> appendLine("- $h") }
        }

        return WeeklyLifeBriefingReport(
            memoriesCapturedLast7Days = notesThisWeek.size,
            commitmentsFulfilledLast7Days = completedCommitmentsThisWeek.size,
            openCommitmentsCount = openCommitments.size,
            pendingTasksCount = pendingTasksCount,
            topPeopleInteracted = topPeopleThisWeek,
            activeProjects = activeProjectNames,
            executiveHighlights = highlights,
            markdownExport = markdownExport
        )
    }

    // --- Phase 11: Contextual Recommendations Engine ---
    fun buildContextualRecommendations(
        allNotes: List<NoteWithDetails>,
        allPeople: List<PersonEntity>,
        allCommitments: List<CommitmentEntity>,
        allActions: List<ActionItemEntity>,
        allProjects: List<ProjectEntity>,
        nowMillis: Long = System.currentTimeMillis()
    ): List<ContextualRecommendationItem> {
        val recs = mutableListOf<ContextualRecommendationItem>()
        val peopleById = allPeople.associateBy { it.id }

        // 1. Open Commitments needing attention or due date assignment
        allCommitments.filter { !it.isCompleted }.take(5).forEach { comm ->
            val classification = classifyCommitment(comm)
            val personName = comm.personId?.let { peopleById[it]?.name }
            val dueStr = comm.dueDate?.let { "Due ${DateUtils.formatDate(it)}" } ?: "No due date assigned yet"
            recs.add(
                ContextualRecommendationItem(
                    id = "rec_comm_${comm.id}",
                    categoryLabel = "${classification.badgeIcon} ${classification.label}",
                    title = comm.description,
                    reason = buildString {
                        append(dueStr)
                        if (!personName.isNullOrBlank()) append(" • Linked to $personName")
                    },
                    primaryActionLabel = "🔔 Remind Tomorrow",
                    targetCommitmentId = comm.id,
                    targetNoteId = comm.noteId,
                    targetPersonId = comm.personId
                )
            )
        }

        // 2. Overdue Relationship Follow-Ups
        allPeople.filter { it.isOverdueForCatchUp(nowMillis) }.take(4).forEach { person ->
            val daysSince = person.lastContactedAt?.let {
                ((nowMillis - it) / (24L * 3600L * 1000L)).coerceAtLeast(1L)
            } ?: 30L
            recs.add(
                ContextualRecommendationItem(
                    id = "rec_person_${person.id}",
                    categoryLabel = "👥 Relationship Re-Engagement",
                    title = "Check in with ${person.name} (${person.relationship ?: "Contact"})",
                    reason = "Last recorded interaction was $daysSince days ago (cadence: every ${person.getCatchUpCadenceDays()} days).",
                    primaryActionLabel = "📝 Draft Catch-Up",
                    targetPersonId = person.id
                )
            )
        }

        // 3. Active Projects with Open Commitments or Tasks
        allProjects.filter { it.status.equals("ACTIVE", ignoreCase = true) }.take(3).forEach { proj ->
            val openProjComm = allCommitments.count { it.projectId == proj.id && !it.isCompleted }
            val projNotesCount = allNotes.count { it.note.projectId == proj.id }
            recs.add(
                ContextualRecommendationItem(
                    id = "rec_proj_${proj.id}",
                    categoryLabel = "🚀 Project Next Step",
                    title = "Advance ${proj.name}",
                    reason = "$projNotesCount linked memory record(s) • $openProjComm open commitment(s).",
                    primaryActionLabel = "✅ Add Today Task",
                    targetProjectId = proj.id
                )
            )
        }

        // 4. Pinned or High-Confidence Ideas to Convert into Lists/Tasks
        allNotes.filter {
            it.note.isPinned || it.note.category.equals("Ideas", ignoreCase = true)
        }.take(3).forEach { item ->
            recs.add(
                ContextualRecommendationItem(
                    id = "rec_note_${item.note.id}",
                    categoryLabel = "💡 Memory Activation",
                    title = item.note.title.ifBlank { "Captured Idea" },
                    reason = "Captured ${DateUtils.formatRelativeTime(item.note.createdAt)} • Turn into an actionable task or list.",
                    primaryActionLabel = "Open Memory →",
                    targetNoteId = item.note.id
                )
            )
        }

        return recs
    }
}

data class WeeklyLifeBriefingReport(
    val memoriesCapturedLast7Days: Int,
    val commitmentsFulfilledLast7Days: Int,
    val openCommitmentsCount: Int,
    val pendingTasksCount: Int,
    val topPeopleInteracted: List<String>,
    val activeProjects: List<String>,
    val executiveHighlights: List<String>,
    val markdownExport: String
)

data class ContextualRecommendationItem(
    val id: String,
    val categoryLabel: String,
    val title: String,
    val reason: String,
    val primaryActionLabel: String,
    val targetCommitmentId: String? = null,
    val targetPersonId: String? = null,
    val targetProjectId: String? = null,
    val targetNoteId: String? = null
)
