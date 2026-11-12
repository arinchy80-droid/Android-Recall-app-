package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "memory_links",
    indices = [
        Index(value = ["sourceNoteId"]),
        Index(value = ["targetNoteId"])
    ]
)
data class MemoryLinkEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sourceNoteId: String,
    val targetNoteId: String,
    val relationType: String = "RELATED_TO", // CONTINUES, CONTRADICTS, EXPANDS, CAUSED_BY, RELATED_TO
    val strengthScore: Float = 1.0f,
    val createdAt: Long = System.currentTimeMillis()
)
