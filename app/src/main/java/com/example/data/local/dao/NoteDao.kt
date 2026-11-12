package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.NotePersonCrossRef
import com.example.data.model.NoteWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(crossRefs: List<NotePersonCrossRef>)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: String)

    @Query("DELETE FROM note_person_cross_ref WHERE noteId = :noteId")
    suspend fun deleteCrossRefsForNote(noteId: String)

    @Query("DELETE FROM note_person_cross_ref WHERE personId = :personId")
    suspend fun deleteCrossRefsForPerson(personId: String)

    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()

    @Query("DELETE FROM note_person_cross_ref")
    suspend fun deleteAllCrossRefs()

    /**
     * Transaction 1: Creating a note while automatically binding multiple NotePersonCrossRef records.
     */
    @Transaction
    suspend fun insertNoteWithPeople(note: NoteEntity, personIds: List<String>) {
        insertNote(note)
        deleteCrossRefsForNote(note.id)
        if (personIds.isNotEmpty()) {
            val refs = personIds.map { NotePersonCrossRef(noteId = note.id, personId = it) }
            insertCrossRefs(refs)
        }
    }

    @Transaction
    @Query("SELECT * FROM notes ORDER BY isPinned DESC, createdAt DESC")
    fun getAllNotesWithDetails(): Flow<List<NoteWithDetails>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :noteId")
    fun getNoteWithDetailsById(noteId: String): Flow<NoteWithDetails?>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteWithDetailsByIdOnce(noteId: String): NoteWithDetails?

    @Transaction
    @Query("SELECT * FROM notes WHERE category = :category ORDER BY isPinned DESC, createdAt DESC")
    fun getNotesByCategory(category: String): Flow<List<NoteWithDetails>>

    @Transaction
    @Query("SELECT * FROM notes WHERE memoryType = :memoryType ORDER BY isPinned DESC, occurredAt DESC")
    fun getNotesByMemoryType(memoryType: String): Flow<List<NoteWithDetails>>

    @Transaction
    @Query("SELECT * FROM notes WHERE projectId = :projectId ORDER BY isPinned DESC, occurredAt DESC")
    fun getNotesForProject(projectId: String): Flow<List<NoteWithDetails>>

    @Transaction
    @Query("SELECT * FROM notes WHERE placeId = :placeId ORDER BY isPinned DESC, occurredAt DESC")
    fun getNotesForPlace(placeId: String): Flow<List<NoteWithDetails>>

    @Transaction
    @Query("SELECT * FROM notes WHERE verificationStatus = :verificationStatus ORDER BY createdAt DESC")
    fun getNotesByVerificationStatus(verificationStatus: String): Flow<List<NoteWithDetails>>

    /**
     * Transaction 2: Querying all notes that reference a specific personId.
     */
    @Transaction
    @Query("""
        SELECT notes.* FROM notes 
        INNER JOIN note_person_cross_ref ON notes.id = note_person_cross_ref.noteId 
        WHERE note_person_cross_ref.personId = :personId 
        ORDER BY notes.createdAt DESC
    """)
    fun getNotesForPerson(personId: String): Flow<List<NoteWithDetails>>

    /**
     * Combined search returning full details with relation bindings.
     */
    @Transaction
    @Query("""
        SELECT * FROM notes 
        WHERE title LIKE '%' || :query || '%' 
           OR content LIKE '%' || :query || '%' 
           OR rawTranscription LIKE '%' || :query || '%' 
           OR aiSummary LIKE '%' || :query || '%'
        ORDER BY isPinned DESC, createdAt DESC
    """)
    fun searchNotesWithDetails(query: String): Flow<List<NoteWithDetails>>

    @Query("SELECT COUNT(*) FROM notes")
    fun getNotesCount(): Flow<Int>

    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    suspend fun getAllNotesRaw(): List<NoteEntity>
}
