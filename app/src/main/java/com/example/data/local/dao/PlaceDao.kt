package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.PlaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlace(place: PlaceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaces(places: List<PlaceEntity>)

    @Update
    suspend fun updatePlace(place: PlaceEntity)

    @Delete
    suspend fun deletePlace(place: PlaceEntity)

    @Query("DELETE FROM places WHERE id = :id")
    suspend fun deletePlaceById(id: String)

    @Query("DELETE FROM places")
    suspend fun deleteAllPlaces()

    @Query("SELECT * FROM places ORDER BY lastVisitedAt DESC")
    fun getAllPlaces(): Flow<List<PlaceEntity>>

    @Query("SELECT * FROM places ORDER BY lastVisitedAt DESC")
    suspend fun getAllPlacesRaw(): List<PlaceEntity>

    @Query("SELECT * FROM places WHERE id = :id")
    fun getPlaceById(id: String): Flow<PlaceEntity?>

    @Query("SELECT * FROM places WHERE id = :id")
    suspend fun getPlaceByIdOnce(id: String): PlaceEntity?

    @Query("SELECT * FROM places WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findPlaceByName(name: String): PlaceEntity?

    @Query("SELECT * FROM places WHERE name LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%' ORDER BY lastVisitedAt DESC")
    fun searchPlaces(query: String): Flow<List<PlaceEntity>>
}
