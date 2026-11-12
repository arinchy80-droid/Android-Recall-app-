package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY reminderTimeMillis ASC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 ORDER BY reminderTimeMillis ASC")
    fun getActiveReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE noteId = :noteId ORDER BY reminderTimeMillis ASC")
    fun getRemindersForNote(noteId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE noteId = :noteId AND isCompleted = 0 ORDER BY reminderTimeMillis ASC LIMIT 1")
    fun getActiveReminderForNote(noteId: String): Flow<ReminderEntity?>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: String): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 AND reminderTimeMillis > :currentTime ORDER BY reminderTimeMillis ASC")
    suspend fun getUpcomingActiveReminders(currentTime: Long): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity)

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: String)

    @Query("DELETE FROM reminders WHERE noteId = :noteId")
    suspend fun deleteRemindersForNote(noteId: String)

    @Query("UPDATE reminders SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun setCompleted(id: String, isCompleted: Boolean)

    @Query("UPDATE reminders SET reminderTimeMillis = :newTimeMillis, isCompleted = 0 WHERE id = :id")
    suspend fun snoozeReminder(id: String, newTimeMillis: Long)

    @Query("DELETE FROM reminders")
    suspend fun deleteAllReminders()
}
