package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ActionItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActionItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActionItem(item: ActionItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActionItems(items: List<ActionItemEntity>)

    @Update
    suspend fun updateActionItem(item: ActionItemEntity)

    @Delete
    suspend fun deleteActionItem(item: ActionItemEntity)

    @Query("DELETE FROM action_items WHERE id = :id")
    suspend fun deleteActionItemById(id: String)

    @Query("DELETE FROM action_items")
    suspend fun deleteAllActionItems()

    @Query("UPDATE action_items SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun setCompleted(id: String, isCompleted: Boolean)

    @Query("SELECT * FROM action_items ORDER BY isCompleted ASC, dueDate ASC, createdAt DESC")
    fun getAllActionItems(): Flow<List<ActionItemEntity>>

    @Query("SELECT * FROM action_items WHERE isCompleted = 0 ORDER BY dueDate ASC, createdAt DESC")
    fun getPendingActionItems(): Flow<List<ActionItemEntity>>

    @Query("SELECT * FROM action_items WHERE noteId = :noteId ORDER BY isCompleted ASC, createdAt DESC")
    fun getActionItemsForNote(noteId: String): Flow<List<ActionItemEntity>>

    @Query("SELECT COUNT(*) FROM action_items WHERE isCompleted = 0")
    fun getPendingCount(): Flow<Int>
}
