package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val rawTranscription: String? = null,
    val category: String = "General",
    val tagsJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val aiSummary: String? = null,
    val memoryType: String = "PERSONAL_OBSERVATIONS",
    val occurredAt: Long = System.currentTimeMillis(),
    val sourceType: String = "USER_EXPLICIT",
    val confidenceScore: Float = 1.0f,
    val verificationStatus: String = "VERIFIED_FACT",
    val projectId: String? = null,
    val placeId: String? = null
) {
    fun getUnifiedMemoryType(): com.example.data.model.UnifiedMemoryType {
        return if (memoryType.isNotBlank() && memoryType != "PERSONAL_OBSERVATIONS") {
            com.example.data.model.UnifiedMemoryType.fromString(memoryType)
        } else {
            com.example.data.model.UnifiedMemoryType.inferFromLegacyCategory(
                category = category,
                hasAudio = !rawTranscription.isNullOrBlank(),
                hasLocation = !getLocationName().isNullOrBlank() || !placeId.isNullOrBlank(),
                contentHint = "$title $content"
            )
        }
    }

    fun getMemorySourceType(): com.example.data.model.MemorySourceType {
        return com.example.data.model.MemorySourceType.fromString(sourceType)
    }

    fun getMemoryVerificationStatus(): com.example.data.model.MemoryVerificationStatus {
        return com.example.data.model.MemoryVerificationStatus.fromString(verificationStatus)
    }

    fun getTagsList(): List<String> {
        return try {
            if (tagsJson.isBlank() || tagsJson == "[]") emptyList()
            else {
                val trimmed = tagsJson.trim().removeSurrounding("[", "]")
                if (trimmed.isBlank()) emptyList()
                else trimmed.split(",")
                    .map { it.trim().removeSurrounding("\"") }
                    .filter { it.isNotBlank() }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getLocationName(): String? {
        return getTagsList()
            .firstOrNull { it.startsWith("loc:", ignoreCase = true) }
            ?.substringAfter(":")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    fun getDisplayTagsList(): List<String> {
        return getTagsList().filterNot { it.startsWith("loc:", ignoreCase = true) }
    }

    companion object {
        fun formatTagsJson(tags: List<String>): String {
            return "[" + tags.filter { it.isNotBlank() }
                .map { "\"${it.trim().removePrefix("#")}\"" }
                .distinct()
                .joinToString(",") + "]"
        }

        fun mergeTagsWithLocation(tags: List<String>, locationName: String?): List<String> {
            val base = tags.filterNot { it.trim().removePrefix("#").startsWith("loc:", ignoreCase = true) }
            return if (!locationName.isNullOrBlank()) {
                base + "loc:${locationName.trim()}"
            } else {
                base
            }
        }
    }
}
