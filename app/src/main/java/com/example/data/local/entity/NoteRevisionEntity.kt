package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Preserves non-destructive historical snapshots whenever an existing note is updated or reconciled.
 */
@Entity(
    tableName = "note_revisions",
    indices = [Index(value = ["noteId"])]
)
data class NoteRevisionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val noteId: String,
    val previousTitle: String,
    val previousContent: String,
    val changeSummary: String,
    val sourceType: String = "USER_EXPLICIT",
    val createdAt: Long = System.currentTimeMillis()
)
