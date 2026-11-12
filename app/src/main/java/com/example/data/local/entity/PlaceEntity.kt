package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "places",
    indices = [Index(value = ["name"])]
)
data class PlaceEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String = "Location",
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val notes: String = "",
    val visitCount: Int = 1,
    val lastVisitedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
