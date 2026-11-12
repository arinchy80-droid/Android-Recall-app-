package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.NoteRevisionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteRevisionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRevision(revision: NoteRevisionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRevisions(revisions: List<NoteRevisionEntity>)

    @Query("SELECT * FROM note_revisions WHERE noteId = :noteId ORDER BY createdAt DESC")
    fun getRevisionsForNote(noteId: String): Flow<List<NoteRevisionEntity>>

    @Query("SELECT * FROM note_revisions WHERE noteId = :noteId ORDER BY createdAt DESC")
    suspend fun getRevisionsForNoteOnce(noteId: String): List<NoteRevisionEntity>

    @Query("SELECT * FROM note_revisions ORDER BY createdAt DESC")
    suspend fun getAllRevisionsRaw(): List<NoteRevisionEntity>

    @Query("DELETE FROM note_revisions WHERE noteId = :noteId")
    suspend fun deleteRevisionsForNote(noteId: String)

    @Query("DELETE FROM note_revisions")
    suspend fun deleteAllRevisions()
}
