package com.example.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.data.model.Citation
import com.example.data.model.DailyDebriefResult
import com.example.data.model.DebriefActionItem
import com.example.data.model.DebriefCommitmentItem
import com.example.data.model.DebriefDateItem
import com.example.data.model.DebriefNoteUpdate
import com.example.data.model.DebriefParsedNote
import com.example.data.model.DebriefPersonUpdate
import com.example.data.model.MemorySourceType
import com.example.data.model.MemoryVerificationStatus
import com.example.data.model.NoteWithDetails
import com.example.data.model.SmartSearchNluInterpretation
import com.example.data.model.UnifiedMemoryType
import com.example.data.local.entity.CommitmentEntity
import com.example.data.local.entity.MemoryLinkEntity
import com.example.data.local.entity.PersonEntity
import com.example.data.local.entity.PlaceEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.search.SmartSearchEngine
import com.example.util.MemoryReconciliationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import com.example.data.model.SmartTagExtractionResult
import com.example.data.model.MemoryCategory
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class GeminiService(private val context: Context? = null) {

    companion object {
        private const val MODEL_FLASH = "gemini-3.5-flash"
        private const val MODEL_FLASH_FALLBACK = "gemini-flash-latest"
        private const val MODEL_FLASH_LITE = "gemini-3.1-flash-lite-preview"
        private const val MODEL_PRO = "gemini-3.1-pro-preview"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun executeGenerateContent(
        apiKey: String,
        requestBody: JSONObject,
        preferredModel: String = MODEL_FLASH
    ): String? {
        val modelsToTry = listOf(preferredModel, MODEL_FLASH_FALLBACK, MODEL_FLASH_LITE, MODEL_PRO).distinct()
        for (model in modelsToTry) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody.toString().toRequestBody(jsonMediaType))
                    .build()
                val response = client.newCall(request).execute()
                val responseText = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val jsonResponse = JSONObject(responseText)
                    val candidate = jsonResponse.optJSONArray("candidates")?.optJSONObject(0)
                    val text = extractTextFromCandidate(candidate)
                    if (text.isNotBlank()) {
                        return text
                    }
                } else if (response.code == 503 || response.code == 429 || response.code in 500..599) {
                    // Service overloaded or rate limited; backoff before trying fallback model
                    Thread.sleep(350)
                }
            } catch (t: Throwable) {
                // Try next model fallback
            }
        }
        return null
    }

    private fun getApiKey(): String {
        return try {
            val key = BuildConfig.GEMINI_API_KEY
            if (key.isNullOrBlank() || key == "MY_GEMINI_API_KEY") "" else key
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Extracts structured daily debrief using gemini-3.5-flash with automatic multi-model failover
     * and robust on-device smart extraction fallback on 503 (service overloaded) or network failures.
     */
    suspend fun processDailyDebrief(
        transcribedText: String,
        existingNotes: List<NoteWithDetails> = emptyList(),
        existingPeople: List<PersonEntity> = emptyList()
    ): Result<DailyDebriefResult> = withContext(Dispatchers.IO) {
        val knownPeopleNames = existingPeople.map { it.name }
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            val localRaw = fallbackLocalDebriefExtractor(transcribedText, knownPeopleNames)
                .copy(isLocalFallback = true, rawTranscription = transcribedText)
            val reconciled = MemoryReconciliationEngine.reconcileDebriefWithExistingNotes(
                rawTranscript = transcribedText,
                debriefResult = localRaw,
                existingNotes = existingNotes,
                knownPeopleNames = knownPeopleNames
            )
            return@withContext Result.success(reconciled)
        }

        val existingNotesContext = if (existingNotes.isNotEmpty()) {
            buildString {
                append("\n=== EXISTING NOTES IN MEMORY (Check if this debrief updates any of these!) ===\n")
                existingNotes.take(40).forEach { n ->
                    val peopleList = n.people.joinToString(", ") { it.name }
                    append("- [ID: ${n.note.id}] [Title: ${n.note.title}] [Category: ${n.note.category}] [People: $peopleList]\n")
                    append("  Content: ${n.note.content.replace("\n", " ").take(240)}\n")
                }
            }
        } else ""

        val prompt = """
            Unstructured daily debrief transcript:
            \"\"\"
            $transcribedText
            \"\"\"
            $existingNotesContext
            
            Extract:
            1. daily_summary: ONE single, precise, clean, polished summary of the debrief (do NOT provide multiple options or variations).
            2. people_updates: list of mentioned people, relationship_hint, distinct non-duplicate new_facts (preferences, discussion points, life events, updated balances), dates_to_remember (label, date_iso).
            3. note_updates: CRITICAL — If the debrief modifies, partially pays off, settles, or updates any EXISTING NOTE listed above (for example, if an existing note says "I owe Jason $600" and the debrief says "I paid Jason $200"), emit a `note_updates` entry with the `target_note_id`, `original_title`, `updated_title` (reflecting the new $400 balance), `updated_content` (rewriting the balance to $400 and appending a dated update log), `change_summary` (e.g., "Deducted $200 payment from $600 balance → $400 remaining"), `category`, `tags`, and `people_involved`. Do NOT create a duplicate note in `distinct_notes` for information that updates an existing note!
            4. distinct_notes: Create ONE single, precise, comprehensive, well-polished note capturing any brand-new debrief content that does NOT belong to an existing note in `note_updates` (can be empty `[]` if the entire debrief was an update to an existing note).
            5. action_items: extracted concrete tasks with task and due_date_hint.
        """.trimIndent()

        val systemInstruction = """
            You are the automated ingestion and stateful memory reconciliation engine for Recall, an elite personal memory assistant and CRM.
            The user has provided an unstructured audio or text debrief summarizing their day.
            
            YOUR OBJECTIVE:
            1. Produce ONE SINGLE, PRECISE, POLISHED output. NEVER generate multiple options, alternative phrasings, or redundant notes for the same debrief.
            2. STATEFUL MEMORY RECONCILIATION (CRITICAL): Compare the user's input against `EXISTING NOTES IN MEMORY`. Whenever new information updates an older note (such as paying down a debt/loan, adding to a tab, changing a project deadline, or updating a status related to a person or topic), you MUST output an item in `note_updates` that edits the older note in-place with the newly calculated amount or status, rather than creating a separate conflicting note.
            3. PROVENANCE & CONFIDENCE SEPARATION: Distinguish between facts explicitly stated by the user (`is_ai_interpretation: false`, `confidence_score: 0.90..1.0`) and tentative AI interpretations or uncertain statements (`is_ai_interpretation: true`, `confidence_score: 0.45..0.78`). Never silently turn an uncertain interpretation into an established fact.
            4. Identify every actual human person mentioned (colleagues, friends, family, acquaintances). CRITICAL: ONLY include real human proper names in `people_updates` and `people_involved`. NEVER include verbs (such as "have", "saw", "been", "went", "got"), pronouns, prepositions, or common nouns as people.
            5. Consolidate any genuinely new topic into `distinct_notes` with a clear title, polished content, best-matching category, canonical `memory_type` (PEOPLE|PLACES|EVENTS|CONVERSATIONS|IDEAS|TASKS|COMMITMENTS|PROJECTS|EXPERIENCES|PREFERENCES|IMPORTANT_DATES|PERSONAL_OBSERVATIONS), relevant hashtags, `people_involved`, `confidence_score`, and `is_ai_interpretation`. CRITICAL: Never output multiple notes with different titles/headings that share the same main note content.
            6. Extract concrete action items (`action_items`) and any promises, debts, or obligations (`commitments` with `direction`: "BY_ME" or "TO_ME").
            
            OUTPUT SCHEMA (Valid JSON ONLY):
            {
              "daily_summary": "string",
              "people_updates": [
                {
                  "person_name": "string",
                  "relationship_hint": "string",
                  "new_facts": ["string"],
                  "dates_to_remember": [{"label": "string", "date_iso": "string"}],
                  "confidence_score": 0.95,
                  "is_ai_interpretation": false
                }
              ],
              "note_updates": [
                {
                  "target_note_id": "string (exact ID from EXISTING NOTES IN MEMORY)",
                  "original_title": "string",
                  "updated_title": "string",
                  "updated_content": "string",
                  "category": "string",
                  "change_summary": "string",
                  "tags": ["string"],
                  "people_involved": ["string"],
                  "confidence_score": 0.95
                }
              ],
              "distinct_notes": [
                {
                  "title": "string",
                  "content": "string",
                  "category": "Meeting|Work|Projects|Ideas|Personal|Finance|Health|Travel|Family|Learning|Journal|Clients|Code & Tech|Shopping|Habits|Home & Living|General",
                  "memory_type": "PEOPLE|PLACES|EVENTS|CONVERSATIONS|IDEAS|TASKS|COMMITMENTS|PROJECTS|EXPERIENCES|PREFERENCES|IMPORTANT_DATES|PERSONAL_OBSERVATIONS",
                  "tags": ["string"],
                  "people_involved": ["string"],
                  "confidence_score": 0.95,
                  "is_ai_interpretation": false
                }
              ],
              "action_items": [
                {
                  "task": "string",
                  "due_date_hint": "string"
                }
              ],
              "commitments": [
                {
                  "description": "string",
                  "direction": "BY_ME|TO_ME",
                  "person_name": "string",
                  "due_date_hint": "string",
                  "confidence_score": 0.95
                }
              ]
            }
        """.trimIndent()

        val requestBody = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemInstruction) })
                })
            })
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.2)
            })
        }

        // Try primary model followed by fallback models on 503/429/overload
        val modelsToTry = listOf(MODEL_FLASH, MODEL_FLASH_FALLBACK, MODEL_FLASH_LITE, MODEL_PRO)
        var lastError: Exception? = null

        for (model in modelsToTry) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody.toString().toRequestBody(jsonMediaType))
                    .build()

                val response = client.newCall(request).execute()
                val responseText = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val jsonResponse = JSONObject(responseText)
                    val candidates = jsonResponse.optJSONArray("candidates")
                    val firstCandidate = candidates?.optJSONObject(0)
                    val parts = firstCandidate?.optJSONObject("content")?.optJSONArray("parts")
                    val rawText = parts?.optJSONObject(0)?.optString("text") ?: ""

                    val cleanJson = cleanJsonString(rawText)
                    if (cleanJson.isNotBlank()) {
                        val parsed = parseDebriefJson(cleanJson, existingNotes)
                        val reconciled = MemoryReconciliationEngine.reconcileDebriefWithExistingNotes(
                            rawTranscript = transcribedText,
                            debriefResult = parsed,
                            existingNotes = existingNotes,
                            knownPeopleNames = knownPeopleNames
                        )
                        if (reconciled.distinctNotes.isNotEmpty() || reconciled.noteUpdates.isNotEmpty() || reconciled.peopleUpdates.isNotEmpty() || reconciled.actionItems.isNotEmpty() || reconciled.dailySummary.isNotBlank()) {
                            return@withContext Result.success(reconciled.copy(isLocalFallback = false))
                        }
                    }
                } else if (response.code == 503 || response.code == 429 || response.code in 500..599) {
                    lastError = Exception("Gemini error (${response.code}): $responseText")
                    Thread.sleep(350)
                    continue
                } else {
                    lastError = Exception("Gemini error (${response.code}): $responseText")
                }
            } catch (e: Exception) {
                lastError = e
            }
        }

        // When remote models are overloaded (503) or unavailable, gracefully fall back to the smart on-device extractor
        val fallbackRaw = fallbackLocalDebriefExtractor(transcribedText, knownPeopleNames)
            .copy(isLocalFallback = true, rawTranscription = transcribedText)
        val reconciledFallback = MemoryReconciliationEngine.reconcileDebriefWithExistingNotes(
            rawTranscript = transcribedText,
            debriefResult = fallbackRaw,
            existingNotes = existingNotes,
            knownPeopleNames = knownPeopleNames
        )
        Result.success(reconciledFallback)
    }

    /**
     * Multimodal audio processing for Daily Debrief.
     * Transcribes the raw audio file from start to finish and extracts modular notes,
     * note updates, people updates, and action items with Gemini.
     */
    suspend fun processDailyDebriefAudio(
        audioFile: java.io.File,
        knownPeopleNames: List<String> = emptyList(),
        existingNotes: List<NoteWithDetails> = emptyList(),
        existingPeople: List<PersonEntity> = emptyList()
    ): Result<DailyDebriefResult> = withContext(Dispatchers.IO) {
        val peopleNames = (knownPeopleNames + existingPeople.map { it.name }).distinct()
        val apiKey = getApiKey()
        if (apiKey.isBlank() || !audioFile.exists() || audioFile.length() < 100) {
            val fallbackText = "Daily debrief recorded via audio (${audioFile.name}). Add Gemini API key in Settings to enable multimodal audio transcription."
            val rawFallback = fallbackLocalDebriefExtractor(fallbackText, peopleNames)
                .copy(isLocalFallback = true, rawTranscription = fallbackText)
            val reconciled = MemoryReconciliationEngine.reconcileDebriefWithExistingNotes(
                rawTranscript = fallbackText,
                debriefResult = rawFallback,
                existingNotes = existingNotes,
                knownPeopleNames = peopleNames
            )
            return@withContext Result.success(reconciled)
        }

        val audioBytes = try {
            audioFile.readBytes()
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }

        val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)

        val peopleContext = if (peopleNames.isNotEmpty()) {
            "Existing known contacts in user CRM: ${peopleNames.joinToString(", ")}. Match any mentioned names to these contacts where relevant."
        } else ""

        val existingNotesContext = if (existingNotes.isNotEmpty()) {
            buildString {
                append("\n=== EXISTING NOTES IN MEMORY (Check if this audio debrief updates any of these!) ===\n")
                existingNotes.take(40).forEach { n ->
                    val pList = n.people.joinToString(", ") { it.name }
                    append("- [ID: ${n.note.id}] [Title: ${n.note.title}] [Category: ${n.note.category}] [People: $pList]\n")
                    append("  Content: ${n.note.content.replace("\n", " ").take(240)}\n")
                }
            }
        } else ""

        val systemInstruction = """
            You are the automated audio intelligence and stateful memory reconciliation engine for Recall, an elite personal memory assistant and CRM.
            The user has provided a full-length raw audio recording of their daily debrief.
            $peopleContext
            $existingNotesContext
            
            YOUR OBJECTIVE:
            1. Transcribe the FULL speech completely, accurately, and verbatim into `raw_transcription` without truncation or skipping sections.
            2. Produce ONE clean, cohesive summary of the day in `daily_summary`.
            3. Identify all mentioned human people (colleagues, friends, family). CRITICAL: ONLY include real human proper names in `people_updates` and `people_involved`. NEVER include verbs (like "have", "saw", "been"), pronouns, or common nouns as people. Extract distinct non-duplicate facts and key dates into `people_updates`.
            4. STATEFUL MEMORY RECONCILIATION (CRITICAL): If the user's speech modifies, pays down, settles, or updates any EXISTING NOTE in `EXISTING NOTES IN MEMORY` (for example, paying $200 toward a $600 debt to Jason), output an entry in `note_updates` with the `target_note_id`, `original_title`, `updated_title`, `updated_content` (with the new calculated balance and update log), and `change_summary`.
            5. PROVENANCE & CONFIDENCE SEPARATION: Distinguish between facts explicitly stated by the user (`is_ai_interpretation: false`, `confidence_score: 0.90..1.0`) and tentative AI interpretations or uncertain inferences (`is_ai_interpretation: true`, `confidence_score: 0.45..0.78`).
            6. Consolidate any brand-new debrief content into `distinct_notes` with canonical `memory_type`, `confidence_score`, and `is_ai_interpretation`. Never create duplicate notes with different headings and the same main content.
            7. Extract concrete action items into `action_items` and promises/debts into `commitments` (`BY_ME` or `TO_ME`).
            
            OUTPUT SCHEMA (Valid JSON ONLY):
            {
              "raw_transcription": "string (full verbatim transcript of the entire audio recording)",
              "daily_summary": "string",
              "people_updates": [
                {
                  "person_name": "string",
                  "relationship_hint": "string",
                  "new_facts": ["string"],
                  "dates_to_remember": [{"label": "string", "date_iso": "string"}],
                  "confidence_score": 0.95,
                  "is_ai_interpretation": false
                }
              ],
              "note_updates": [
                {
                  "target_note_id": "string",
                  "original_title": "string",
                  "updated_title": "string",
                  "updated_content": "string",
                  "category": "string",
                  "change_summary": "string",
                  "tags": ["string"],
                  "people_involved": ["string"],
                  "confidence_score": 0.95
                }
              ],
              "distinct_notes": [
                {
                  "title": "string",
                  "content": "string",
                  "category": "Meeting|Work|Projects|Ideas|Personal|Finance|Health|Travel|Family|Learning|Journal|Clients|Code & Tech|Shopping|Habits|Home & Living|General",
                  "memory_type": "PEOPLE|PLACES|EVENTS|CONVERSATIONS|IDEAS|TASKS|COMMITMENTS|PROJECTS|EXPERIENCES|PREFERENCES|IMPORTANT_DATES|PERSONAL_OBSERVATIONS",
                  "tags": ["string"],
                  "people_involved": ["string"],
                  "confidence_score": 0.95,
                  "is_ai_interpretation": false
                }
              ],
              "action_items": [
                {
                  "task": "string",
                  "due_date_hint": "string"
                }
              ],
              "commitments": [
                {
                  "description": "string",
                  "direction": "BY_ME|TO_ME",
                  "person_name": "string",
                  "due_date_hint": "string",
                  "confidence_score": 0.95
                }
              ]
            }
        """.trimIndent()

        val requestBody = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemInstruction) })
                })
            })
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "audio/mp4")
                                put("data", base64Audio)
                            })
                        })
                        put(JSONObject().apply {
                            put("text", "Listen to this entire raw daily debrief audio. Transcribe the full speech completely into `raw_transcription` and unpack all details and existing note updates into structured JSON.")
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.2)
            })
        }

        val modelsToTry = listOf(MODEL_FLASH, MODEL_FLASH_FALLBACK, MODEL_FLASH_LITE, MODEL_PRO)
        var lastError: Exception? = null

        for (model in modelsToTry) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody.toString().toRequestBody(jsonMediaType))
                    .build()

                val response = client.newCall(request).execute()
                val responseText = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val jsonResponse = JSONObject(responseText)
                    val candidates = jsonResponse.optJSONArray("candidates")
                    val firstCandidate = candidates?.optJSONObject(0)
                    val parts = firstCandidate?.optJSONObject("content")?.optJSONArray("parts")
                    val rawText = parts?.optJSONObject(0)?.optString("text") ?: ""

                    val cleanJson = cleanJsonString(rawText)
                    if (cleanJson.isNotBlank()) {
                        val parsed = parseDebriefJson(cleanJson, existingNotes)
                        val reconciled = MemoryReconciliationEngine.reconcileDebriefWithExistingNotes(
                            rawTranscript = parsed.rawTranscription ?: parsed.dailySummary,
                            debriefResult = parsed,
                            existingNotes = existingNotes,
                            knownPeopleNames = peopleNames
                        )
                        if (reconciled.distinctNotes.isNotEmpty() || reconciled.noteUpdates.isNotEmpty() || reconciled.peopleUpdates.isNotEmpty() || reconciled.actionItems.isNotEmpty() || reconciled.dailySummary.isNotBlank()) {
                            return@withContext Result.success(reconciled.copy(isLocalFallback = false))
                        }
                    }
                } else if (response.code == 503 || response.code == 429 || response.code in 500..599) {
                    lastError = Exception("Gemini error (${response.code}): $responseText")
                    Thread.sleep(350)
                    continue
                } else {
                    lastError = Exception("Gemini error (${response.code}): $responseText")
                }
            } catch (e: Exception) {
                lastError = e
            }
        }

        // Graceful on-device fallback if models unavailable
        val fallback = fallbackLocalDebriefExtractor("Audio recording from daily debrief (${audioFile.name})", peopleNames)
            .copy(isLocalFallback = true, rawTranscription = "Full audio recorded (${audioFile.name})")
        Result.success(fallback)
    }

    /**
     * RAG Conversational Memory Concierge with citations.
     */
    suspend fun queryMemoryConcierge(
        userQuery: String,
        notes: List<NoteWithDetails>,
        people: List<PersonEntity>,
        enableHighThinking: Boolean = false,
        places: List<PlaceEntity> = emptyList(),
        projects: List<ProjectEntity> = emptyList(),
        commitments: List<CommitmentEntity> = emptyList(),
        memoryLinks: List<MemoryLinkEntity> = emptyList()
    ): Result<Pair<String, List<Citation>>> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.success(
                fallbackLocalRAG(
                    query = userQuery,
                    notes = notes,
                    people = people,
                    places = places,
                    projects = projects,
                    commitments = commitments,
                    memoryLinks = memoryLinks
                )
            )
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val peopleById = people.associateBy { it.id }
        val projectsById = projects.associateBy { it.id }
        val placesById = places.associateBy { it.id }
        val notesById = notes.associateBy { it.note.id }

        // Build Unified Memory Graph context block
        val contextBuilder = StringBuilder()
        contextBuilder.append("=== PEOPLE PROFILES ===\n")
        people.forEach { p ->
            contextBuilder.append("- Name: ${p.name} (Relationship: ${p.relationship ?: "Unknown"})\n")
            contextBuilder.append("  Facts: ${p.factsJson}\n")
            contextBuilder.append("  Key Dates: ${p.keyDatesJson}\n")
        }

        if (places.isNotEmpty()) {
            contextBuilder.append("\n=== PLACES & LOCATIONS ===\n")
            places.forEach { pl ->
                contextBuilder.append("- Place: ${pl.name} [Category: ${pl.category}, Visits: ${pl.visitCount}]")
                if (!pl.address.isNullOrBlank()) contextBuilder.append(" Address: ${pl.address}")
                if (pl.notes.isNotBlank()) contextBuilder.append(" Notes: ${pl.notes}")
                contextBuilder.append("\n")
            }
        }

        if (projects.isNotEmpty()) {
            contextBuilder.append("\n=== PROJECTS ===\n")
            projects.forEach { pr ->
                contextBuilder.append("- Project: ${pr.name} [Status: ${pr.status}] Description: ${pr.description}\n")
            }
        }

        if (commitments.isNotEmpty()) {
            contextBuilder.append("\n=== COMMITMENTS & PROMISES ===\n")
            commitments.forEach { c ->
                val dirLabel = if (c.direction == "TO_ME") "Owed TO User" else "Promised BY User"
                val statusLabel = if (c.isCompleted) "COMPLETED" else "OPEN"
                val personName = c.personId?.let { peopleById[it]?.name } ?: "General"
                val projName = c.projectId?.let { projectsById[it]?.name }
                contextBuilder.append("- [$statusLabel | $dirLabel] ${c.description} (Person: $personName${if (projName != null) ", Project: $projName" else ""}, Verification: ${c.verificationStatus})\n")
            }
        }

        contextBuilder.append("\n=== NOTES & INTERCONNECTED MEMORIES ===\n")
        notes.take(50).forEach { item ->
            val dateStr = dateFormat.format(Date(item.note.createdAt))
            val placeName = item.note.placeId?.let { placesById[it]?.name } ?: item.note.getLocationName()
            val projName = item.note.projectId?.let { projectsById[it]?.name }
            contextBuilder.append("- [Title: ${item.note.title}] [Date: $dateStr] [Category: ${item.note.category}] [MemoryType: ${item.note.memoryType}] [Verification: ${item.note.verificationStatus}]\n")
            contextBuilder.append("  Content: ${item.note.content}\n")
            if (!item.note.aiSummary.isNullOrBlank()) {
                contextBuilder.append("  Summary: ${item.note.aiSummary}\n")
            }
            if (item.people.isNotEmpty()) {
                contextBuilder.append("  People: ${item.people.joinToString { it.name }}\n")
            }
            if (!placeName.isNullOrBlank()) {
                contextBuilder.append("  Place: $placeName\n")
            }
            if (!projName.isNullOrBlank()) {
                contextBuilder.append("  Project: $projName\n")
            }
            val links = memoryLinks.filter { it.sourceNoteId == item.note.id || it.targetNoteId == item.note.id }
            if (links.isNotEmpty()) {
                val linkDesc = links.mapNotNull { l ->
                    val otherId = if (l.sourceNoteId == item.note.id) l.targetNoteId else l.sourceNoteId
                    notesById[otherId]?.note?.title?.let { "${l.relationType} -> '$it'" }
                }
                if (linkDesc.isNotEmpty()) {
                    contextBuilder.append("  Graph Links: ${linkDesc.joinToString("; ")}\n")
                }
            }
        }

        val systemInstruction = """
            You are "Recall Concierge", an AI Personal Memory & Life Intelligence companion. You have access to the user's Unified Memory Graph: people profiles, places, projects, commitments/promises, and interconnected notes.
            
            CONTEXT PROVIDED:
            ---
            $contextBuilder
            ---
            
            PROTOCOL:
            1. Ground your answers ONLY on the provided context. If the answer is absent, state clearly: "I don't have any notes or memories recorded about that."
            2. Never fabricate personal facts, names, dates, commitments, or preferences.
            3. Respect provenance: if a note or fact is marked `AI_INFERRED_PENDING` or `[AI Inferred - Unverified]`, mention that it is an unverified AI interpretation rather than an established fact.
            4. Provide precise citations in your response using markdown badges: `[Source: {Note Title} - {Date}]`.
            5. Keep your tone professional, concise, empathetic, and personal.
        """.trimIndent()

        val requestBody = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemInstruction) })
                })
            })
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", userQuery) })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.3)
                if (enableHighThinking) {
                    put("thinkingConfig", JSONObject().apply {
                        put("thinkingBudget", 2048)
                    })
                }
            })
        }

        val modelsToTry = if (enableHighThinking) {
            listOf(MODEL_PRO, MODEL_FLASH, MODEL_FLASH_FALLBACK)
        } else {
            listOf(MODEL_FLASH, MODEL_FLASH_FALLBACK, MODEL_FLASH_LITE, MODEL_PRO)
        }

        for (model in modelsToTry) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody.toString().toRequestBody(jsonMediaType))
                    .build()

                val response = client.newCall(request).execute()
                val responseText = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val jsonResponse = JSONObject(responseText)
                    val candidates = jsonResponse.optJSONArray("candidates")
                    val firstCandidate = candidates?.optJSONObject(0)
                    val parts = firstCandidate?.optJSONObject("content")?.optJSONArray("parts")
                    val answerText = parts?.optJSONObject(0)?.optString("text") ?: ""
                    if (answerText.isNotBlank()) {
                        val citations = extractCitations(answerText, notes)
                        return@withContext Result.success(Pair(answerText, citations))
                    }
                } else if (response.code == 503 || response.code == 429 || response.code in 500..599) {
                    Thread.sleep(300)
                }
            } catch (e: Exception) {
                // Try fallback model
            }
        }

        // Graceful fallback to local RAG when models are overloaded
        Result.success(
            fallbackLocalRAG(
                query = userQuery,
                notes = notes,
                people = people,
                places = places,
                projects = projects,
                commitments = commitments,
                memoryLinks = memoryLinks
            )
        )
    }

    /**
     * Visual Memory Synthesis and OCR using Gemini Multimodal.
     */
    suspend fun analyzeImageMemory(bitmap: Bitmap): Result<Triple<String, String, List<String>>> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.success(
                Triple(
                    "Visual Memory Snapshot",
                    "Image captured. Add your Gemini API Key in Settings to enable automated neural OCR and semantic entity extraction.",
                    listOf("Camera Capture", "Memory", "Photo")
                )
            )
        }

        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

        val prompt = """
            Analyze this image for a personal memory assistant.
            1. Transcribe all readable text (OCR: documents, whiteboards, business cards, signs).
            2. Synthesize what this image depicts, key objects or people involved, and notable takeaway points.
            3. Return a JSON with:
            {
              "ocr_text": "string with all extracted text",
              "ai_description": "2-3 sentence semantic synthesis of what this memory is about",
              "labels": ["string", "string", "string"]
            }
        """.trimIndent()

        val requestBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.2)
            })
        }

        val modelsToTry = listOf(MODEL_FLASH, MODEL_FLASH_FALLBACK, MODEL_FLASH_LITE)
        for (model in modelsToTry) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody.toString().toRequestBody(jsonMediaType))
                    .build()

                val response = client.newCall(request).execute()
                val responseText = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val jsonResponse = JSONObject(responseText)
                    val parts = jsonResponse.optJSONArray("candidates")?.optJSONObject(0)
                        ?.optJSONObject("content")?.optJSONArray("parts")
                    val raw = parts?.optJSONObject(0)?.optString("text") ?: "{}"
                    val parsed = JSONObject(cleanJsonString(raw))

                    val ocr = parsed.optString("ocr_text", "No text detected")
                    val desc = parsed.optString("ai_description", "Memory photo captured")
                    val labelsArray = parsed.optJSONArray("labels")
                    val labels = mutableListOf<String>()
                    if (labelsArray != null) {
                        for (i in 0 until labelsArray.length()) {
                            labels.add(labelsArray.getString(i))
                        }
                    }

                    return@withContext Result.success(Triple(ocr, desc, labels))
                } else if (response.code == 503 || response.code == 429 || response.code in 500..599) {
                    Thread.sleep(300)
                }
            } catch (e: Exception) {
                // Try fallback model
            }
        }

        // Graceful fallback when models are overloaded
        val dateFormatted = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())
        Result.success(Triple("Visual Memory Snapshot", "Captured memory photo on $dateFormatted", listOf("Photo", "Memory", "Snapshot")))
    }

    private fun cleanJsonString(raw: String): String {
        var clean = raw.trim()
        val start = clean.indexOf('{')
        val end = clean.lastIndexOf('}')
        if (start != -1 && end != -1 && end > start) {
            return clean.substring(start, end + 1).trim()
        }
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json")
        } else if (clean.startsWith("```")) {
            clean = clean.removePrefix("```")
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```")
        }
        return clean.trim()
    }

    private fun parseDebriefJson(
        jsonString: String,
        existingNotes: List<NoteWithDetails> = emptyList()
    ): DailyDebriefResult {
        val root = JSONObject(jsonString)
        val dailySummary = root.optString("daily_summary", "Daily debrief recorded.")
        val rawTranscription = if (root.has("raw_transcription") && !root.isNull("raw_transcription")) {
            root.optString("raw_transcription").ifBlank { null }
        } else null

        val knownValidPeople = existingNotes.flatMap { it.people.map { p -> p.name } }
            .filter { MemoryReconciliationEngine.isValidPersonName(it) }
            .distinct()

        val peopleUpdates = mutableListOf<DebriefPersonUpdate>()
        val peopleArray = root.optJSONArray("people_updates")
        if (peopleArray != null) {
            for (i in 0 until peopleArray.length()) {
                val p = peopleArray.getJSONObject(i)
                val name = p.optString("person_name", "").trim()
                if (!MemoryReconciliationEngine.isValidPersonName(name, knownValidPeople)) continue
                val rel = if (p.has("relationship_hint") && !p.isNull("relationship_hint")) p.getString("relationship_hint") else null
                val factsArray = p.optJSONArray("new_facts")
                val facts = mutableListOf<String>()
                if (factsArray != null) {
                    for (j in 0 until factsArray.length()) {
                        facts.add(factsArray.getString(j))
                    }
                }
                val datesArray = p.optJSONArray("dates_to_remember")
                val dates = mutableListOf<DebriefDateItem>()
                if (datesArray != null) {
                    for (k in 0 until datesArray.length()) {
                        val d = datesArray.getJSONObject(k)
                        dates.add(
                            DebriefDateItem(
                                label = d.optString("label", "Date"),
                                dateIso = d.optString("date_iso", "")
                            )
                        )
                    }
                }
                val conf = p.optDouble("confidence_score", 0.95).toFloat().coerceIn(0.1f, 1.0f)
                val isInterp = p.optBoolean("is_ai_interpretation", conf < 0.80f)
                peopleUpdates.add(
                    DebriefPersonUpdate(
                        personName = name,
                        relationshipHint = rel,
                        newFacts = facts,
                        datesToRemember = dates,
                        confidenceScore = conf,
                        isAiInferred = isInterp
                    )
                )
            }
        }

        val noteUpdates = mutableListOf<DebriefNoteUpdate>()
        val updatesArray = root.optJSONArray("note_updates")
        if (updatesArray != null) {
            for (i in 0 until updatesArray.length()) {
                val u = updatesArray.getJSONObject(i)
                val targetId = u.optString("target_note_id", "")
                val matchedExisting = existingNotes.firstOrNull { it.note.id == targetId }
                if (targetId.isNotBlank() && matchedExisting != null) {
                    val involved = mutableListOf<String>()
                    val involvedArray = u.optJSONArray("people_involved")
                    if (involvedArray != null) {
                        for (j in 0 until involvedArray.length()) {
                            val candidate = involvedArray.getString(j).trim()
                            if (MemoryReconciliationEngine.isValidPersonName(candidate, knownValidPeople)) {
                                involved.add(candidate)
                            }
                        }
                    }
                    val tags = mutableListOf<String>()
                    val tagsArray = u.optJSONArray("tags")
                    if (tagsArray != null) {
                        for (t in 0 until tagsArray.length()) {
                            tags.add(tagsArray.getString(t))
                        }
                    }
                    val conf = u.optDouble("confidence_score", 0.95).toFloat().coerceIn(0.1f, 1.0f)
                    val verStatus = if (conf < 0.80f) MemoryVerificationStatus.AI_INFERRED_PENDING.name else MemoryVerificationStatus.VERIFIED_FACT.name
                    noteUpdates.add(
                        DebriefNoteUpdate(
                            targetNoteId = targetId,
                            originalTitle = u.optString("original_title", matchedExisting.note.title),
                            updatedTitle = u.optString("updated_title", matchedExisting.note.title),
                            originalContent = matchedExisting.note.content,
                            updatedContent = u.optString("updated_content", matchedExisting.note.content),
                            category = u.optString("category", matchedExisting.note.category),
                            changeSummary = u.optString("change_summary", "Updated note with new information"),
                            peopleInvolved = involved.ifEmpty {
                                matchedExisting.people.map { it.name }.filter { MemoryReconciliationEngine.isValidPersonName(it, knownValidPeople) }
                            },
                            tags = tags.ifEmpty { matchedExisting.note.getDisplayTagsList() },
                            confidenceScore = conf,
                            verificationStatus = verStatus
                        )
                    )
                }
            }
        }

        val distinctNotes = mutableListOf<DebriefParsedNote>()
        val notesArray = root.optJSONArray("distinct_notes")
        if (notesArray != null) {
            for (i in 0 until notesArray.length()) {
                val n = notesArray.getJSONObject(i)
                val title = n.optString("title", "Untitled Note")
                val content = n.optString("content", "")
                val category = n.optString("category", "General")
                val involvedArray = n.optJSONArray("people_involved")
                val involved = mutableListOf<String>()
                if (involvedArray != null) {
                    for (j in 0 until involvedArray.length()) {
                        val candidate = involvedArray.getString(j).trim()
                        if (MemoryReconciliationEngine.isValidPersonName(candidate, knownValidPeople)) {
                            involved.add(candidate)
                        }
                    }
                }
                val tagsArray = n.optJSONArray("tags")
                val tags = mutableListOf<String>()
                if (tagsArray != null) {
                    for (t in 0 until tagsArray.length()) {
                        tags.add(tagsArray.getString(t))
                    }
                }
                val rawMemType = n.optString("memory_type", "")
                val resolvedMemType = if (rawMemType.isNotBlank()) {
                    UnifiedMemoryType.fromString(rawMemType).name
                } else {
                    UnifiedMemoryType.inferFromLegacyCategory(
                        category = category,
                        hasAudio = !rawTranscription.isNullOrBlank(),
                        hasLocation = tags.any { it.startsWith("loc:", ignoreCase = true) },
                        contentHint = "$title $content"
                    ).name
                }
                val conf = n.optDouble("confidence_score", 0.95).toFloat().coerceIn(0.1f, 1.0f)
                val isAiInterp = n.optBoolean("is_ai_interpretation", false) || conf < 0.80f ||
                    hasSpeculativePhrasing("$title $content")
                val sourceType = if (isAiInterp) {
                    MemorySourceType.AI_INTERPRETATION.name
                } else {
                    MemorySourceType.AUDIO_TRANSCRIPT.name
                }
                val verificationStatus = if (isAiInterp) {
                    MemoryVerificationStatus.AI_INFERRED_PENDING.name
                } else {
                    MemoryVerificationStatus.VERIFIED_FACT.name
                }
                distinctNotes.add(
                    DebriefParsedNote(
                        title = title,
                        content = content,
                        category = category,
                        tags = tags,
                        peopleInvolved = involved,
                        memoryType = resolvedMemType,
                        sourceType = sourceType,
                        confidenceScore = if (isAiInterp && conf >= 0.80f) 0.72f else conf,
                        verificationStatus = verificationStatus
                    )
                )
            }
        }

        // First deduplicate any distinctNotes that have the exact same normalized main content
        val deduplicatedDistinctNotes = distinctNotes.distinctBy {
            MemoryReconciliationEngine.normalizeNoteContentForDedup(it.content).ifBlank { it.title.lowercase().trim() }
        }

        val consolidatedNotes = if (deduplicatedDistinctNotes.size > 1) {
            val first = deduplicatedDistinctNotes.first()
            val primaryTitle = first.title
            val mergedContent = deduplicatedDistinctNotes.joinToString("\n\n") { note ->
                if (note.title.isNotBlank() && note.title != primaryTitle) {
                    "${note.title}: ${note.content}"
                } else {
                    note.content
                }
            }.trim()
            val mergedTags = deduplicatedDistinctNotes.flatMap { it.tags }.distinct()
            val mergedPeople = deduplicatedDistinctNotes.flatMap { it.peopleInvolved }.distinct()
            val minConfidence = deduplicatedDistinctNotes.minOf { it.confidenceScore }
            val anyPending = deduplicatedDistinctNotes.any { it.verificationStatus == MemoryVerificationStatus.AI_INFERRED_PENDING.name }
            listOf(
                DebriefParsedNote(
                    title = primaryTitle,
                    content = mergedContent,
                    category = first.category,
                    tags = mergedTags,
                    peopleInvolved = mergedPeople,
                    memoryType = first.memoryType,
                    sourceType = if (anyPending) MemorySourceType.AI_INTERPRETATION.name else first.sourceType,
                    confidenceScore = minConfidence,
                    verificationStatus = if (anyPending) MemoryVerificationStatus.AI_INFERRED_PENDING.name else MemoryVerificationStatus.VERIFIED_FACT.name
                )
            )
        } else {
            deduplicatedDistinctNotes
        }

        val actionItems = mutableListOf<DebriefActionItem>()
        val actionsArray = root.optJSONArray("action_items")
        if (actionsArray != null) {
            for (i in 0 until actionsArray.length()) {
                val a = actionsArray.getJSONObject(i)
                val task = a.optString("task", "")
                val hint = if (a.has("due_date_hint") && !a.isNull("due_date_hint")) a.getString("due_date_hint") else null
                if (task.isNotBlank()) {
                    actionItems.add(DebriefActionItem(task = task, dueDateHint = hint))
                }
            }
        }

        val commitments = mutableListOf<DebriefCommitmentItem>()
        val commitmentsArray = root.optJSONArray("commitments")
        if (commitmentsArray != null) {
            for (i in 0 until commitmentsArray.length()) {
                val c = commitmentsArray.getJSONObject(i)
                val desc = c.optString("description", "").trim()
                if (desc.isNotBlank()) {
                    commitments.add(
                        DebriefCommitmentItem(
                            description = desc,
                            direction = c.optString("direction", "BY_ME").uppercase(),
                            personName = c.optString("person_name", "").ifBlank { null },
                            dueDateHint = c.optString("due_date_hint", "").ifBlank { null },
                            confidenceScore = c.optDouble("confidence_score", 0.92).toFloat().coerceIn(0.1f, 1.0f)
                        )
                    )
                }
            }
        }

        return DailyDebriefResult(
            dailySummary = dailySummary,
            peopleUpdates = peopleUpdates,
            distinctNotes = consolidatedNotes,
            actionItems = actionItems,
            noteUpdates = noteUpdates,
            commitments = commitments,
            rawTranscription = rawTranscription
        )
    }

    private fun hasSpeculativePhrasing(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("maybe ") ||
            lower.contains("might ") ||
            lower.contains("probably ") ||
            lower.contains("i think ") ||
            lower.contains("not sure") ||
            lower.contains("possibly ") ||
            lower.contains("unsure ") ||
            lower.contains("i guess ")
    }

    private fun extractCitations(answer: String, notes: List<NoteWithDetails>): List<Citation> {
        val citations = mutableListOf<Citation>()
        val pattern = Pattern.compile("\\[Source:\\s*([^\\-]+?)\\s*-\\s*([^\\]]+)\\]")
        val matcher = pattern.matcher(answer)

        while (matcher.find()) {
            val title = matcher.group(1)?.trim() ?: ""
            val dateStr = matcher.group(2)?.trim() ?: ""
            val matchedNote = notes.firstOrNull { it.note.title.contains(title, ignoreCase = true) }
            citations.add(
                Citation(
                    noteId = matchedNote?.note?.id,
                    noteTitle = title,
                    dateString = dateStr
                )
            )
        }
        return citations.distinctBy { it.noteTitle }
    }

    internal fun fallbackLocalDebriefExtractor(
        transcript: String,
        existingPeople: List<String> = emptyList()
    ): DailyDebriefResult {
        val cleanRaw = transcript.replace(Regex("^\\[Debrief Mode:[^\\]]+\\]\\s*"), "").trim()
        val polishedTranscript = localFallbackCleanUp(cleanRaw)
        val sentences = polishedTranscript.split(Regex("[.!?]\\s+")).map { it.trim() }.filter { it.isNotBlank() }
        val dateFormatted = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())
        val summary = if (sentences.isNotEmpty()) sentences.take(2).joinToString(". ").let { if (it.endsWith(".")) it else "$it." } else polishedTranscript

        val peopleUpdates = mutableListOf<DebriefPersonUpdate>()
        val actionItems = mutableListOf<DebriefActionItem>()
        val commitments = mutableListOf<DebriefCommitmentItem>()

        // 1. Detect mentioned people (only valid proper human names, never verbs/nouns like "have", "saw", "been")
        val validExistingPeople = existingPeople.filter { MemoryReconciliationEngine.isValidPersonName(it) }
        val personMentionRegex = Regex("(?:met with|talked with|talked to|spoke with|called|discussed with|synced with)\\s+([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)?)", RegexOption.IGNORE_CASE)
        val regexNames = personMentionRegex.findAll(polishedTranscript)
            .map { it.groupValues[1].trim() }
            .filter { MemoryReconciliationEngine.isValidPersonName(it, validExistingPeople) }
            .toList()
        val candidateNames = MemoryReconciliationEngine.extractCandidateNames(polishedTranscript, validExistingPeople)
        val detectedNames = (regexNames + candidateNames)
            .filter { MemoryReconciliationEngine.isValidPersonName(it, validExistingPeople) }
            .distinctBy { it.lowercase() }

        for (name in detectedNames) {
            val relevantSentences = sentences.filter { it.contains(name, ignoreCase = true) }.distinct()
            val facts = if (relevantSentences.isNotEmpty()) relevantSentences.take(3) else listOf(polishedTranscript)
            val speculative = facts.any { hasSpeculativePhrasing(it) }
            peopleUpdates.add(
                DebriefPersonUpdate(
                    personName = name,
                    relationshipHint = "Contact",
                    newFacts = facts,
                    datesToRemember = emptyList(),
                    confidenceScore = if (speculative) 0.70f else 0.92f,
                    isAiInferred = speculative
                )
            )
        }

        // 2. Action items & Commitments extraction
        sentences.forEach { s ->
            val lower = s.lowercase()
            if (lower.contains("need to") || lower.contains("remember to") || lower.contains("follow up") || lower.contains("send ") || lower.contains("call ") || lower.contains("prepare ") || lower.contains("owe ") || lower.contains("pay ")) {
                val taskText = s.trim()
                if (actionItems.none { it.task.equals(taskText, ignoreCase = true) }) {
                    actionItems.add(DebriefActionItem(task = taskText))
                }
            }
            if (lower.contains("owe ") || lower.contains("owes ") || lower.contains("promised") || lower.contains("lent ") || lower.contains("borrowed")) {
                val direction = if (lower.contains("owes me") || lower.contains("lent ") || lower.contains("promised me")) "TO_ME" else "BY_ME"
                val matchedPerson = detectedNames.firstOrNull { s.contains(it, ignoreCase = true) }
                commitments.add(
                    DebriefCommitmentItem(
                        description = s.trim(),
                        direction = direction,
                        personName = matchedPerson,
                        confidenceScore = if (hasSpeculativePhrasing(s)) 0.72f else 0.95f
                    )
                )
            }
        }

        // 3. Single precise consolidated note with provenance & confidence
        val smartMetadata = localFallbackSmartTag(polishedTranscript, detectedNames)
        val isSpeculative = hasSpeculativePhrasing(polishedTranscript)
        val inferredType = UnifiedMemoryType.inferFromLegacyCategory(
            category = smartMetadata.category,
            hasAudio = true,
            hasLocation = false,
            contentHint = polishedTranscript
        ).name
        val singleNote = DebriefParsedNote(
            title = if (smartMetadata.title.isNotBlank() && smartMetadata.title != "Untitled Note") smartMetadata.title else "Daily Debrief: $dateFormatted",
            content = polishedTranscript,
            category = smartMetadata.category,
            tags = smartMetadata.tags.map { if (it.startsWith("#")) it else "#$it" },
            peopleInvolved = detectedNames,
            memoryType = inferredType,
            sourceType = if (isSpeculative) MemorySourceType.AI_INTERPRETATION.name else MemorySourceType.AUDIO_TRANSCRIPT.name,
            confidenceScore = if (isSpeculative) 0.70f else 0.94f,
            verificationStatus = if (isSpeculative) MemoryVerificationStatus.AI_INFERRED_PENDING.name else MemoryVerificationStatus.VERIFIED_FACT.name
        )

        return DailyDebriefResult(
            dailySummary = summary,
            peopleUpdates = peopleUpdates,
            distinctNotes = listOf(singleNote),
            actionItems = actionItems,
            commitments = commitments,
            isLocalFallback = true
        )
    }

    fun fallbackLocalRAG(
        query: String,
        notes: List<NoteWithDetails>,
        people: List<PersonEntity>,
        places: List<PlaceEntity> = emptyList(),
        projects: List<ProjectEntity> = emptyList(),
        commitments: List<CommitmentEntity> = emptyList(),
        memoryLinks: List<MemoryLinkEntity> = emptyList()
    ): Pair<String, List<Citation>> {
        val nlu = SmartSearchEngine.parseQueryLocal(
            query = query,
            knownPeople = people,
            knownPlaces = places,
            knownProjects = projects
        )
        val rankedItems = SmartSearchEngine.filterAndRankMemories(
            allNotes = notes,
            allMedia = emptyList(),
            allPeople = people,
            allActions = notes.flatMap { it.actionItems },
            interpretation = nlu,
            allPlaces = places,
            allProjects = projects,
            allCommitments = commitments,
            allMemoryLinks = memoryLinks
        )

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val citations = mutableListOf<Citation>()

        val matchedCommitments = rankedItems.mapNotNull { it.commitmentEntity }.ifEmpty {
            if (nlu.intent == "COMMITMENTS") commitments.filter { !it.isCompleted } else emptyList()
        }
        val matchedProjects = rankedItems.mapNotNull { it.projectEntity }.ifEmpty {
            if (nlu.intent == "PROJECTS") projects else emptyList()
        }
        val matchedPlaces = rankedItems.mapNotNull { it.placeEntity }.ifEmpty {
            if (nlu.intent == "PLACES") places else emptyList()
        }
        val matchedPeople = rankedItems.mapNotNull { it.personEntity }
        val matchedNotes = rankedItems.filter { it.noteWithDetails != null }

        val answer = buildString {
            if (matchedCommitments.isNotEmpty()) {
                append("🤝 **Commitments & Promises:**\n")
                matchedCommitments.take(4).forEach { c ->
                    val dir = if (c.direction == "TO_ME") "📥 Owed to you" else "📤 You promised"
                    val state = if (c.isCompleted) "✓ Completed" else "⏳ Open"
                    append("• **$dir ($state)**: ${c.description}\n")
                }
                append("\n")
            }
            if (matchedProjects.isNotEmpty()) {
                append("🚀 **Projects:**\n")
                matchedProjects.take(3).forEach { pr ->
                    append("• **${pr.name}** [${pr.status}]: ${pr.description.ifBlank { "Active project" }}\n")
                }
                append("\n")
            }
            if (matchedPlaces.isNotEmpty()) {
                append("📍 **Places:**\n")
                matchedPlaces.take(3).forEach { pl ->
                    append("• **${pl.name}** (${pl.category}, ${pl.visitCount} visit(s))\n")
                }
                append("\n")
            }
            if (matchedPeople.isNotEmpty()) {
                append("👥 **People Profiles:**\n")
                matchedPeople.take(3).forEach { p ->
                    append("• **${p.name}** (${p.relationship ?: "Contact"}): Facts: ${p.factsJson}\n")
                }
                append("\n")
            }
            if (matchedNotes.isNotEmpty()) {
                append("📝 **Interconnected Memories:**\n")
                matchedNotes.take(4).forEach { item ->
                    val n = item.noteWithDetails!!
                    val dStr = dateFormat.format(Date(n.note.createdAt))
                    val graphHint = item.graphPath?.let { " *($it)*" } ?: ""
                    val unverifiedHint = if (n.note.verificationStatus == MemoryVerificationStatus.AI_INFERRED_PENDING.name) {
                        " *(🤖 Unverified AI Interpretation)*"
                    } else ""
                    append("• **${n.note.title}**$graphHint$unverifiedHint: ${n.note.content.take(160)}\n")
                    append("  [Source: ${n.note.title} - $dStr]\n")
                    citations.add(Citation(noteId = n.note.id, noteTitle = n.note.title, dateString = dStr))
                }
            }
            if (isEmpty()) {
                append("I don't have any notes or memories recorded about that.")
            }
        }

        return Pair(answer.trim(), citations)
    }

    private fun extractTextFromCandidate(candidate: JSONObject?): String {
        if (candidate == null) return ""
        val content = candidate.optJSONObject("content") ?: return ""
        val parts = content.optJSONArray("parts") ?: return ""
        val sb = StringBuilder()
        for (i in 0 until parts.length()) {
            val part = parts.optJSONObject(i) ?: continue
            val isThought = part.optBoolean("thought", false)
            if (!isThought) {
                val text = part.optString("text", "")
                if (text.isNotBlank()) {
                    sb.append(text)
                }
            }
        }
        if (sb.isEmpty()) {
            for (i in 0 until parts.length()) {
                val text = parts.optJSONObject(i)?.optString("text", "") ?: ""
                if (text.isNotBlank()) return text
            }
        }
        return sb.toString().trim()
    }

    fun localFallbackCleanUp(rawText: String): String {
        if (rawText.isBlank()) return rawText

        // 1. Remove filler words (speech-to-text artifacts)
        var cleaned = rawText
            .replace(Regex("(?i)\\b(um|uh|erm|you know|sort of|kind of|i mean|like basically|basically)\\b,?\\s*"), "")
            .replace(Regex("(?i)\\bso anyway\\b,?\\s*"), "")
            .replace(Regex("[ \t]+"), " ")
            .trim()

        // 2. Fix casing of personal pronoun 'i' and common contractions
        cleaned = cleaned.replace(Regex("\\bi\\b"), "I")
            .replace(Regex("(?i)\\bi'm\\b"), "I'm")
            .replace(Regex("(?i)\\bi'll\\b"), "I'll")
            .replace(Regex("(?i)\\bi've\\b"), "I've")
            .replace(Regex("(?i)\\bi'd\\b"), "I'd")

        // 3. Clean up multiple punctuation and normalize line breaks
        cleaned = cleaned.replace(Regex("[,;]{2,}"), ",")
            .replace(Regex("[.!?]{2,}"), ".")
            .replace(Regex("\n{3,}"), "\n\n")

        // 4. Format lines and sentences with proper capitalization
        val lines = cleaned.split("\n").map { line ->
            val trimmedLine = line.trim()
            if (trimmedLine.startsWith("-") || trimmedLine.startsWith("*") || trimmedLine.startsWith("•")) {
                val bulletContent = trimmedLine.drop(1).trim()
                if (bulletContent.isNotEmpty()) {
                    "• " + bulletContent.substring(0, 1).uppercase(Locale.getDefault()) + bulletContent.substring(1)
                } else trimmedLine
            } else if (trimmedLine.isNotEmpty()) {
                val sentences = trimmedLine.split(Regex("(?<=[.!?])\\s+"))
                sentences.map { s ->
                    val trimmed = s.trim()
                    if (trimmed.isNotEmpty()) {
                        trimmed.substring(0, 1).uppercase(Locale.getDefault()) + trimmed.substring(1)
                    } else ""
                }.filter { it.isNotEmpty() }.joinToString(" ")
            } else ""
        }

        val result = lines.joinToString("\n").trim()
        return if (result.isNotBlank()) result else rawText.trim()
    }

    fun localFallbackSmartTag(text: String, existingPeople: List<String>): SmartTagExtractionResult {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        val firstLine = lines.firstOrNull() ?: "New Memory"
        val words = firstLine.split(Regex("\\s+")).take(6).joinToString(" ")
        val title = if (words.length > 40) words.take(40) + "..." else words

        val lower = text.lowercase()
        val category = when {
            lower.contains("meeting") || lower.contains("sync") || lower.contains("call") || lower.contains("1:1") || lower.contains("zoom") || lower.contains("discuss") || lower.contains("met with") -> "Meeting"
            lower.contains("idea") || lower.contains("brainstorm") || lower.contains("concept") || lower.contains("feature") -> "Ideas"
            lower.contains("project") || lower.contains("sprint") || lower.contains("roadmap") || lower.contains("milestone") -> "Projects"
            lower.contains("work") || lower.contains("task") || lower.contains("deadline") || lower.contains("client") -> "Work"
            lower.contains("finance") || lower.contains("budget") || lower.contains("cost") || lower.contains("payment") || lower.contains("dollar") || lower.contains("revenue") || lower.contains("invoice") -> "Finance"
            lower.contains("health") || lower.contains("doctor") || lower.contains("workout") || lower.contains("gym") || lower.contains("diet") || lower.contains("sleep") -> "Health"
            lower.contains("family") || lower.contains("mom") || lower.contains("dad") || lower.contains("kids") || lower.contains("home") -> "Family"
            lower.contains("travel") || lower.contains("flight") || lower.contains("hotel") || lower.contains("trip") || lower.contains("vacation") -> "Travel"
            lower.contains("learn") || lower.contains("study") || lower.contains("read") || lower.contains("book") || lower.contains("course") -> "Learning"
            else -> "General"
        }

        val detected = mutableListOf<String>()
        for (person in existingPeople) {
            if (person.isNotBlank() &&
                MemoryReconciliationEngine.isValidPersonName(person) &&
                text.contains(person, ignoreCase = true) &&
                !detected.contains(person)
            ) {
                detected.add(person)
            }
        }

        // Generate intelligent keyword tags
        val generatedTags = mutableListOf<String>()
        val stopWords = setOf(
            "the", "and", "for", "with", "that", "this", "from", "have", "were", "been", "they",
            "what", "when", "where", "about", "your", "will", "would", "there", "their", "here",
            "note", "just", "some", "need", "into", "then", "more", "also", "very", "much"
        )
        // Add category tag
        generatedTags.add(category.lowercase().replace(" ", "-"))

        // Add domain tags based on keywords
        val keywordCandidates = listOf(
            "q1", "q2", "q3", "q4", "roadmap", "launch", "planning", "strategy", "review",
            "urgent", "bug", "feature", "design", "budget", "pricing", "contract", "interview",
            "doctor", "workout", "dinner", "flight", "hotel", "packing", "summary", "action",
            "feedback", "presentation", "slides", "release", "milestone", "revenue", "goals"
        )
        for (kw in keywordCandidates) {
            if (lower.contains(kw) && !generatedTags.contains(kw)) {
                generatedTags.add(kw)
            }
        }

        // Extract key words from text
        val wordsList = text.split(Regex("[^a-zA-Z0-9_-]+"))
        for (w in wordsList) {
            val clean = w.lowercase().trim()
            if (clean.length in 4..16 && !stopWords.contains(clean) && !generatedTags.contains(clean)) {
                generatedTags.add(clean)
                if (generatedTags.size >= 5) break
            }
        }

        return SmartTagExtractionResult(
            title = title,
            category = category,
            mentionedPeople = detected,
            tags = generatedTags.take(5)
        )
    }

    /**
     * AI Clean Up: Fixes grammar, removes filler words, and formats notes directly.
     */
    private fun extractSinglePreciseCleanedText(rawOutput: String, fallbackInput: String): String {
        var text = rawOutput.trim()
        // If JSON object was returned, extract "cleaned_text"
        if (text.contains("\"cleaned_text\"")) {
            try {
                val parsed = JSONObject(cleanJsonString(text))
                val extracted = parsed.optString("cleaned_text", "").trim()
                if (extracted.isNotBlank()) {
                    text = extracted
                }
            } catch (_: Exception) {
            }
        }

        if (text.startsWith("```") && text.endsWith("```")) {
            text = text.removePrefix("```").removeSuffix("```").trim()
            if (text.startsWith("markdown\n")) text = text.removePrefix("markdown\n").trim()
            if (text.startsWith("text\n")) text = text.removePrefix("text\n").trim()
        }

        // Strip conversational introductory lines or multiple options ("Option 1:", "Version 1:", etc.)
        val lines = text.lines().map { it.trim() }
        val filteredLines = mutableListOf<String>()
        for (line in lines) {
            val lower = line.lowercase()
            if (lower.startsWith("here is ") ||
                lower.startsWith("here are ") ||
                lower.startsWith("sure, here") ||
                lower.startsWith("certainly!") ||
                lower.matches(Regex("^(?:\\*\\*)?(?:option|version|choice|alternative)\\s*1\\b.*", RegexOption.IGNORE_CASE))
            ) {
                // If line has content after colon in "Option 1: actual text", keep the part after colon
                val colonIdx = line.indexOf(':')
                if (lower.contains("option 1") && colonIdx != -1 && colonIdx + 1 < line.length) {
                    val afterColon = line.substring(colonIdx + 1).trim().removeSurrounding("\"").trim()
                    if (afterColon.isNotBlank() && !afterColon.endsWith(")")) {
                        filteredLines.add(afterColon)
                    }
                }
                continue
            }
            // Stop immediately if a second option/variation starts
            if (lower.matches(Regex("^(?:\\*\\*)?(?:option|version|choice|alternative)\\s*[2-9]\\b.*", RegexOption.IGNORE_CASE)) ||
                lower.startsWith("or, if you prefer") ||
                lower.startsWith("alternatively:")
            ) {
                break
            }
            filteredLines.add(line)
        }

        val finalCleaned = filteredLines.joinToString("\n").trim().removeSurrounding("\"").trim()
        return if (finalCleaned.isNotBlank()) finalCleaned else localFallbackCleanUp(fallbackInput)
    }

    /**
     * AI Clean Up: Fixes grammar, removes filler words, and formats notes directly into ONE single precise output.
     */
    suspend fun cleanUpNoteText(rawText: String): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isNotBlank()) {
            val systemInstruction = """
                You are a precision note and daily debrief editor.
                Your ONLY task is to rewrite and polish the user's raw text or speech transcript into ONE SINGLE, precise, clean, grammatically correct output.
                CRITICAL RULES:
                1. NEVER provide multiple options, variations, or alternatives (e.g., never write 'Option 1', 'Option 2', 'Professional version', 'Casual version').
                2. NEVER include conversational introductions, explanations, or commentary.
                3. Preserve all names, dates, numbers, and factual meaning while removing speech fillers ('um', 'uh', 'like', 'you know') and fixing grammar/punctuation.
                4. Output valid JSON matching: {"cleaned_text": "your single precise polished text here"}
            """.trimIndent()

            val prompt = """
                Polish this text into ONE single precise output:
                \"\"\"
                $rawText
                \"\"\"
            """.trimIndent()

            val requestBody = JSONObject().apply {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstruction) })
                    })
                })
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.1)
                })
            }

            val remoteResult = executeGenerateContent(apiKey, requestBody)
            if (!remoteResult.isNullOrBlank()) {
                val singleOutput = extractSinglePreciseCleanedText(remoteResult, rawText)
                return@withContext Result.success(singleOutput)
            }
        }

        Result.success(localFallbackCleanUp(rawText))
    }

    /**
     * Smart Tag: Analyzes note, auto-categorizes, extracts people and relevant keyword tags.
     */
    suspend fun smartTagAndExtract(
        text: String,
        existingPeople: List<String>
    ): Result<SmartTagExtractionResult> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isNotBlank()) {
            val prompt = """
                Analyze this note:
                \"\"\"
                $text
                \"\"\"
                
                Known contacts: ${existingPeople.joinToString()}
                
                Return a valid JSON object matching this schema:
                {
                  "suggested_title": "concise 3-6 word title summarizing the note",
                  "suggested_category": "Meeting|Work|Projects|Ideas|Personal|Finance|Health|Travel|Family|Learning|General",
                  "mentioned_people": ["list of full names mentioned in the note"],
                  "suggested_tags": ["list of 3-6 relevant lowercase keyword tags without hash symbol, e.g. 'planning', 'q4', 'revenue'"]
                }
            """.trimIndent()

            val requestBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.1)
                })
            }

            val remoteResult = executeGenerateContent(apiKey, requestBody)
            if (!remoteResult.isNullOrBlank()) {
                try {
                    val parsed = JSONObject(cleanJsonString(remoteResult))
                    val fallback = localFallbackSmartTag(text, existingPeople)
                    val title = parsed.optString("suggested_title").takeIf { it.isNotBlank() } ?: fallback.title
                    val rawCategory = parsed.optString("suggested_category").takeIf { it.isNotBlank() } ?: fallback.category
                    val category = MemoryCategory.fromString(rawCategory).displayName

                    val people = mutableListOf<String>()
                    val peopleArray = parsed.optJSONArray("mentioned_people")
                    if (peopleArray != null) {
                        for (i in 0 until peopleArray.length()) {
                            val p = peopleArray.optString(i, "").trim()
                            if (p.isNotBlank() && MemoryReconciliationEngine.isValidPersonName(p, existingPeople)) {
                                people.add(p)
                            }
                        }
                    }
                    for (p in existingPeople) {
                        if (p.isNotBlank() &&
                            MemoryReconciliationEngine.isValidPersonName(p) &&
                            text.contains(p, ignoreCase = true) &&
                            !people.contains(p)
                        ) {
                            people.add(p)
                        }
                    }

                    val tags = mutableListOf<String>()
                    val tagsArray = parsed.optJSONArray("suggested_tags")
                    if (tagsArray != null) {
                        for (i in 0 until tagsArray.length()) {
                            val t = tagsArray.optString(i, "").trim().removePrefix("#").lowercase()
                            if (t.isNotBlank() && !tags.contains(t)) {
                                tags.add(t)
                            }
                        }
                    }
                    if (tags.isEmpty()) {
                        tags.addAll(fallback.tags)
                    }

                    return@withContext Result.success(
                        SmartTagExtractionResult(
                            title = title,
                            category = category,
                            mentionedPeople = people,
                            tags = tags
                        )
                    )
                } catch (e: Throwable) {
                    // Fallback to local smart tagger
                }
            }
        }

        Result.success(localFallbackSmartTag(text, existingPeople))
    }

    /**
     * Synthesize Profile: Reads all notes referencing a person and updates their facts and important dates
     * without duplicating existing facts or generating redundant entries.
     */
    suspend fun synthesizePersonProfile(
        personName: String,
        notesText: String,
        existingFacts: List<String> = emptyList(),
        existingDates: Map<String, String> = emptyMap()
    ): Result<Pair<List<String>, Map<String, String>>> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            val fallbackFacts = notesText.lines()
                .map { it.trim().removePrefix("-").trim() }
                .filter { it.isNotBlank() && it.contains(personName, ignoreCase = true) }
                .distinctBy { it.lowercase() }
                .take(5)
            return@withContext Result.success(Pair(fallbackFacts, emptyMap()))
        }

        val existingFactsBlock = if (existingFacts.isEmpty()) "None" else existingFacts.joinToString("\n- ", prefix = "- ")
        val existingDatesBlock = if (existingDates.isEmpty()) "None" else existingDates.entries.joinToString("\n- ", prefix = "- ") { "${it.key}: ${it.value}" }

        val prompt = """
            Synthesize a clean, deduplicated personal CRM profile for '$personName' based strictly on these notes and existing profile data:
            
            EXISTING PROFILE FACTS (DO NOT DUPLICATE OR REPHRASE THESE):
            $existingFactsBlock
            
            EXISTING KEY DATES:
            $existingDatesBlock
            
            NOTES MENTIONING '$personName':
            \"\"\"
            $notesText
            \"\"\"
            
            CRITICAL INSTRUCTIONS:
            1. Extract ONLY genuinely NEW facts (preferences, likes, dislikes, family members, pets, career details, commitments) from the notes that are NOT already stated or implied in EXISTING PROFILE FACTS.
            2. NEVER output duplicate facts or rephrase the same fact in multiple ways. Each fact must be unique, concise, and distinct.
            3. Only include key dates in "dates" if an actual concrete date or milestone was mentioned in the notes (never output placeholder strings like "..." or "Unknown").
            
            Output ONLY valid JSON:
            {
              "facts": ["unique new fact 1", "unique new fact 2"],
              "dates": {}
            }
        """.trimIndent()

        val requestBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.1)
            })
        }

        val invalidDateValues = setOf("", "...", "unknown", "n/a", "none", "null", "yyyy-mm-dd", "yyyy-mm-dd or hint")
        val modelsToTry = listOf(MODEL_FLASH, MODEL_FLASH_FALLBACK, MODEL_FLASH_LITE)
        for (model in modelsToTry) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody.toString().toRequestBody(jsonMediaType))
                    .build()

                val response = client.newCall(request).execute()
                val responseText = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val jsonResponse = JSONObject(responseText)
                    val parts = jsonResponse.optJSONArray("candidates")?.optJSONObject(0)
                        ?.optJSONObject("content")?.optJSONArray("parts")
                    val raw = parts?.optJSONObject(0)?.optString("text") ?: "{}"
                    val parsed = JSONObject(cleanJsonString(raw))

                    val factsArray = parsed.optJSONArray("facts")
                    val facts = mutableListOf<String>()
                    if (factsArray != null) {
                        for (i in 0 until factsArray.length()) {
                            val f = factsArray.optString(i, "").trim()
                            if (f.isNotBlank()) facts.add(f)
                        }
                    }

                    val datesObj = parsed.optJSONObject("dates")
                    val dates = mutableMapOf<String, String>()
                    if (datesObj != null) {
                        val keys = datesObj.keys()
                        while (keys.hasNext()) {
                            val k = keys.next().trim()
                            val v = datesObj.optString(k, "").trim()
                            if (k.isNotBlank() && v.isNotBlank() && v.lowercase() !in invalidDateValues) {
                                dates[k] = v
                            }
                        }
                    }

                    return@withContext Result.success(Pair(facts, dates))
                } else if (response.code == 503 || response.code == 429 || response.code in 500..599) {
                    Thread.sleep(300)
                }
            } catch (e: Exception) {
                // Try fallback model
            }
        }

        // Heuristic fallback
        val fallbackFacts = notesText.lines()
            .map { it.trim().removePrefix("-").trim() }
            .filter { it.isNotBlank() && it.contains(personName, ignoreCase = true) }
            .distinctBy { it.lowercase() }
            .take(5)
        Result.success(Pair(fallbackFacts, emptyMap()))
    }

    /**
     * Advanced Smart Search NLU: interprets complex natural language queries
     * and synthesizes an executive summary answer grounded in the retrieved memories.
     */
    suspend fun interpretAndSummarizeSmartSearch(
        query: String,
        retrievedMemoriesSummary: String
    ): Result<SmartSearchNluInterpretation> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception("Gemini API key is not configured"))
        }

        val prompt = """
            User Natural Language Query:
            \"\"\"
            $query
            \"\"\"

            Retrieved Memory Context:
            \"\"\"
            $retrievedMemoriesSummary
            \"\"\"

            Task:
            1. Analyze user intent: "NOTES" | "PHOTOS" | "PEOPLE" | "TASKS" | "ALL"
            2. Extract mentioned entities/people: e.g. ["John"]
            3. Extract core keywords/topics: e.g. ["meeting", "project proposal"]
            4. Extract temporal constraint: e.g. "Last week", "Last summer", "Yesterday"
            5. Synthesize an "executiveSummary": a concise, factual 1-2 sentence answer directly answering the query based on the retrieved memory context. If relevant memories exist, highlight key points (who, when, what was agreed, or photo details).
            6. Produce a user-facing "explanation": e.g. "Found notes regarding your meeting with John about the project proposal from last week."

            Respond ONLY in valid JSON matching this schema:
            {
              "intent": "NOTES|PHOTOS|PEOPLE|TASKS|ALL",
              "entities": ["string"],
              "keywords": ["string"],
              "timeDescription": "string",
              "explanation": "string",
              "executiveSummary": "string"
            }
        """.trimIndent()

        val systemInstruction = """
            You are the NLU Smart Search Engine for Recall, an episodic personal memory assistant.
            You interpret natural language search queries across notes, photos, people CRM, and action items.
            Provide accurate entity extraction, temporal resolution, and a concise executive summary grounded strictly in the retrieved memory context.
        """.trimIndent()

        val requestBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                    })
                })
            })
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", systemInstruction))
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.2)
            })
        }

        val modelsToTry = listOf(MODEL_FLASH, MODEL_FLASH_FALLBACK, MODEL_FLASH_LITE)
        for (model in modelsToTry) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody.toString().toRequestBody(jsonMediaType))
                    .build()

                val response = client.newCall(request).execute()
                val responseText = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val jsonResponse = JSONObject(responseText)
                    val parts = jsonResponse.optJSONArray("candidates")?.optJSONObject(0)
                        ?.optJSONObject("content")?.optJSONArray("parts")
                    val raw = parts?.optJSONObject(0)?.optString("text") ?: "{}"
                    val parsed = JSONObject(cleanJsonString(raw))

                    val intent = parsed.optString("intent", "ALL")
                    val explanation = parsed.optString("explanation", "Searching memories matching query")
                    val executiveSummary = parsed.optString("executiveSummary").takeIf { it.isNotBlank() }
                    val timeDescription = parsed.optString("timeDescription").takeIf { it.isNotBlank() }

                    val entitiesArray = parsed.optJSONArray("entities")
                    val entities = mutableListOf<String>()
                    if (entitiesArray != null) {
                        for (i in 0 until entitiesArray.length()) {
                            entities.add(entitiesArray.getString(i))
                        }
                    }

                    val keywordsArray = parsed.optJSONArray("keywords")
                    val keywords = mutableListOf<String>()
                    if (keywordsArray != null) {
                        for (i in 0 until keywordsArray.length()) {
                            keywords.add(keywordsArray.getString(i))
                        }
                    }

                    return@withContext Result.success(
                        SmartSearchNluInterpretation(
                            originalQuery = query,
                            intent = intent,
                            keywords = keywords,
                            entities = entities,
                            timeRangeDescription = timeDescription,
                            explanation = explanation,
                            executiveSummary = executiveSummary
                        )
                    )
                } else if (response.code == 503 || response.code == 429 || response.code in 500..599) {
                    Thread.sleep(300)
                }
            } catch (e: Exception) {
                // Try fallback model
            }
        }

        // Graceful heuristic fallback when remote models are overloaded
        val words = query.split("\\s+".toRegex()).filter { it.length > 2 }
        Result.success(
            SmartSearchNluInterpretation(
                originalQuery = query,
                intent = "ALL",
                keywords = words,
                entities = emptyList(),
                timeRangeDescription = null,
                explanation = "Showing matching notes and people for '$query'",
                executiveSummary = null
            )
        )
    }
}
