package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.MediaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: MediaEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaList(list: List<MediaEntity>)

    @Update
    suspend fun updateMedia(media: MediaEntity)

    @Delete
    suspend fun deleteMedia(media: MediaEntity)

    @Query("DELETE FROM media_attachments WHERE id = :id")
    suspend fun deleteMediaById(id: String)

    @Query("DELETE FROM media_attachments")
    suspend fun deleteAllMedia()

    @Query("SELECT * FROM media_attachments WHERE noteId = :noteId ORDER BY capturedAt DESC")
    fun getMediaForNote(noteId: String): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_attachments ORDER BY capturedAt DESC")
    fun getAllMedia(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_attachments WHERE noteId IS NULL ORDER BY capturedAt DESC")
    fun getStandaloneMedia(): Flow<List<MediaEntity>>
}
