package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CommitmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommitmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommitment(commitment: CommitmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommitments(commitments: List<CommitmentEntity>)

    @Update
    suspend fun updateCommitment(commitment: CommitmentEntity)

    @Delete
    suspend fun deleteCommitment(commitment: CommitmentEntity)

    @Query("DELETE FROM commitments WHERE id = :id")
    suspend fun deleteCommitmentById(id: String)

    @Query("DELETE FROM commitments")
    suspend fun deleteAllCommitments()

    @Query("SELECT * FROM commitments ORDER BY isCompleted ASC, createdAt DESC")
    fun getAllCommitments(): Flow<List<CommitmentEntity>>

    @Query("SELECT * FROM commitments ORDER BY createdAt DESC")
    suspend fun getAllCommitmentsRaw(): List<CommitmentEntity>

    @Query("SELECT * FROM commitments WHERE isCompleted = 0 ORDER BY createdAt DESC")
    fun getActiveCommitments(): Flow<List<CommitmentEntity>>

    @Query("SELECT * FROM commitments WHERE personId = :personId ORDER BY isCompleted ASC, createdAt DESC")
    fun getCommitmentsForPerson(personId: String): Flow<List<CommitmentEntity>>

    @Query("SELECT * FROM commitments WHERE projectId = :projectId ORDER BY isCompleted ASC, createdAt DESC")
    fun getCommitmentsForProject(projectId: String): Flow<List<CommitmentEntity>>

    @Query("SELECT * FROM commitments WHERE noteId = :noteId ORDER BY createdAt DESC")
    fun getCommitmentsForNote(noteId: String): Flow<List<CommitmentEntity>>

    @Query("UPDATE commitments SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun setCommitmentCompleted(id: String, isCompleted: Boolean)
}
