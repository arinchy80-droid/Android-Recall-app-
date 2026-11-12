package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "commitments",
    indices = [
        Index(value = ["noteId"]),
        Index(value = ["personId"]),
        Index(value = ["projectId"])
    ]
)
data class CommitmentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val noteId: String? = null,
    val personId: String? = null,
    val projectId: String? = null,
    val description: String,
    val direction: String = "BY_ME", // BY_ME or TO_ME
    val dueDate: Long? = null,
    val isCompleted: Boolean = false,
    val sourceType: String = "USER_EXPLICIT",
    val confidenceScore: Float = 1.0f,
    val verificationStatus: String = "VERIFIED_FACT",
    val createdAt: Long = System.currentTimeMillis()
)
