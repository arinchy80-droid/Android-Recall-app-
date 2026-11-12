package com.example.data.model

data class DailyDebriefResult(
    val dailySummary: String,
    val peopleUpdates: List<DebriefPersonUpdate>,
    val distinctNotes: List<DebriefParsedNote>,
    val actionItems: List<DebriefActionItem>,
    val noteUpdates: List<DebriefNoteUpdate> = emptyList(),
    val commitments: List<DebriefCommitmentItem> = emptyList(),
    val isLocalFallback: Boolean = false,
    val rawTranscription: String? = null,
    val audioFilePath: String? = null
)

data class DebriefNoteUpdate(
    val targetNoteId: String,
    val originalTitle: String,
    val updatedTitle: String,
    val originalContent: String,
    val updatedContent: String,
    val category: String = "General",
    val changeSummary: String,
    val peopleInvolved: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val confidenceScore: Float = 0.95f,
    val verificationStatus: String = "VERIFIED_FACT",
    var isIncluded: Boolean = true
)

data class DebriefPersonUpdate(
    val personName: String,
    val relationshipHint: String? = null,
    val newFacts: List<String> = emptyList(),
    val datesToRemember: List<DebriefDateItem> = emptyList(),
    val confidenceScore: Float = 0.95f,
    val isAiInferred: Boolean = false,
    var isIncluded: Boolean = true // For user toggle in Triage Confirmation UI
)

data class DebriefDateItem(
    val label: String,
    val dateIso: String
)

data class DebriefParsedNote(
    val title: String,
    val content: String,
    val category: String = "General",
    val tags: List<String> = emptyList(),
    val peopleInvolved: List<String> = emptyList(),
    val memoryType: String = "PERSONAL_OBSERVATIONS",
    val sourceType: String = "AUDIO_TRANSCRIPT",
    val confidenceScore: Float = 0.95f,
    val verificationStatus: String = "VERIFIED_FACT",
    var isIncluded: Boolean = true // For user toggle in Triage Confirmation UI
)

data class DebriefActionItem(
    val task: String,
    val dueDateHint: String? = null,
    var isIncluded: Boolean = true // For user toggle in Triage Confirmation UI
)

data class DebriefCommitmentItem(
    val description: String,
    val direction: String = "BY_ME", // BY_ME or TO_ME
    val personName: String? = null,
    val dueDateHint: String? = null,
    val confidenceScore: Float = 0.95f,
    var isIncluded: Boolean = true
)
