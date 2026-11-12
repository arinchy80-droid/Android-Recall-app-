package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.MemoryLinkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryLinkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemoryLink(link: MemoryLinkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemoryLinks(links: List<MemoryLinkEntity>)

    @Delete
    suspend fun deleteMemoryLink(link: MemoryLinkEntity)

    @Query("DELETE FROM memory_links WHERE id = :id")
    suspend fun deleteMemoryLinkById(id: String)

    @Query("DELETE FROM memory_links WHERE sourceNoteId = :noteId OR targetNoteId = :noteId")
    suspend fun deleteLinksForNote(noteId: String)

    @Query("DELETE FROM memory_links")
    suspend fun deleteAllMemoryLinks()

    @Query("SELECT * FROM memory_links ORDER BY createdAt DESC")
    fun getAllMemoryLinks(): Flow<List<MemoryLinkEntity>>

    @Query("SELECT * FROM memory_links ORDER BY createdAt DESC")
    suspend fun getAllMemoryLinksRaw(): List<MemoryLinkEntity>

    @Query("SELECT * FROM memory_links WHERE sourceNoteId = :noteId OR targetNoteId = :noteId ORDER BY strengthScore DESC, createdAt DESC")
    fun getLinksForNote(noteId: String): Flow<List<MemoryLinkEntity>>
}
