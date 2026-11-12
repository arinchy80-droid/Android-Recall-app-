package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.PersonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {

    @Query("SELECT * FROM people ORDER BY lastContactedAt DESC, name ASC")
    fun getAllPeople(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM people WHERE id = :personId")
    fun getPersonById(personId: String): Flow<PersonEntity?>

    @Query("SELECT * FROM people WHERE id = :personId")
    suspend fun getPersonByIdOnce(personId: String): PersonEntity?

    @Query("SELECT * FROM people WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findPersonByName(name: String): PersonEntity?

    @Query("""
        SELECT * FROM people 
        WHERE name LIKE '%' || :query || '%' 
           OR relationship LIKE '%' || :query || '%' 
           OR factsJson LIKE '%' || :query || '%'
        ORDER BY name ASC
    """)
    fun searchPeople(query: String): Flow<List<PersonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(person: PersonEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeople(people: List<PersonEntity>)

    @Update
    suspend fun updatePerson(person: PersonEntity)

    @Delete
    suspend fun deletePerson(person: PersonEntity)

    @Query("DELETE FROM people WHERE id = :personId")
    suspend fun deletePersonById(personId: String)

    @Query("DELETE FROM people")
    suspend fun deleteAllPeople()

    @Query("SELECT COUNT(*) FROM people")
    fun getPeopleCount(): Flow<Int>

    @Query("SELECT * FROM people ORDER BY name ASC")
    suspend fun getAllPeopleRaw(): List<PersonEntity>
}
