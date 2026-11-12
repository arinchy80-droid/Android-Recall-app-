package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "media_attachments",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("noteId")]
)
data class MediaEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val noteId: String?, // Can be null if it's a standalone memory
    val mediaType: MediaType, // Enum: IMAGE, AUDIO
    val localUri: String,
    val ocrText: String? = null,
    val aiDescription: String? = null,
    val labelsJson: String = "[]", // Serialized List<String> from Cloud Vision
    val capturedAt: Long = System.currentTimeMillis()
)
