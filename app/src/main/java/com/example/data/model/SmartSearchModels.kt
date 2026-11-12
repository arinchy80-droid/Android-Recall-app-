package com.example.data.model

import com.example.data.local.entity.ActionItemEntity
import com.example.data.local.entity.CommitmentEntity
import com.example.data.local.entity.MediaEntity
import com.example.data.local.entity.MediaType
import com.example.data.local.entity.PersonEntity
import com.example.data.local.entity.PlaceEntity
import com.example.data.local.entity.ProjectEntity

enum class SmartItemType {
    NOTE,
    PHOTO,
    PERSON,
    ACTION,
    PLACE,
    PROJECT,
    COMMITMENT
}

enum class SmartItemFilter(val label: String) {
    ALL("All"),
    NOTES("Notes"),
    PHOTOS("Photos"),
    PEOPLE("People"),
    PLACES("Places"),
    PROJECTS("Projects"),
    COMMITMENTS("Commitments"),
    TASKS("Tasks")
}

data class SmartSearchNluInterpretation(
    val originalQuery: String,
    val intent: String = "ALL", // "NOTES", "PHOTOS", "PEOPLE", "PLACES", "PROJECTS", "COMMITMENTS", "TASKS", "ALL"
    val keywords: List<String> = emptyList(),
    val entities: List<String> = emptyList(),
    val places: List<String> = emptyList(),
    val projects: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
    val mediaType: MediaType? = null,
    val timeFilterType: String? = null, // "LAST_WEEK", "LAST_MONTH", "LAST_SUMMER", "YESTERDAY", "TODAY", null
    val timeRangeDescription: String? = null,
    val timeStartMillis: Long? = null,
    val timeEndMillis: Long? = null,
    val graphTraversalHops: Int = 0,
    val explanation: String = "",
    val executiveSummary: String? = null
)

data class SmartSearchItem(
    val id: String,
    val itemType: SmartItemType,
    val title: String,
    val subtitle: String,
    val snippet: String,
    val timestamp: Long,
    val matchScore: Int, // 0..100
    val highlights: List<String> = emptyList(),
    val graphPath: String? = null,
    val noteWithDetails: NoteWithDetails? = null,
    val mediaEntity: MediaEntity? = null,
    val personEntity: PersonEntity? = null,
    val actionEntity: ActionItemEntity? = null,
    val placeEntity: PlaceEntity? = null,
    val projectEntity: ProjectEntity? = null,
    val commitmentEntity: CommitmentEntity? = null
)

data class SmartSearchResult(
    val query: String,
    val interpretation: SmartSearchNluInterpretation,
    val items: List<SmartSearchItem>,
    val totalCount: Int,
    val isAiInterpreted: Boolean = false,
    val aiSummary: String? = null
)
