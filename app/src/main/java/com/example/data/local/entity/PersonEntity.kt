package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "people")
data class PersonEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val relationship: String? = null, // e.g., "Colleague", "Friend", "Client"
    val avatarUri: String? = null,
    val factsJson: String = "[]", // List<String> serialized: ["Prefers Earl Grey", "Daughter named Maya"]
    val keyDatesJson: String = "{}", // Map<String, String> serialized: {"Birthday": "1990-05-12"}
    val lastContactedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getCatchUpCadenceDays(): Int {
        return try {
            val regex = Regex(""""CatchUpCadenceDays"\s*:\s*"(\d+)"""")
            val match = regex.find(keyDatesJson)
            match?.groupValues?.getOrNull(1)?.toIntOrNull()?.coerceIn(1, 365) ?: 14
        } catch (e: Exception) {
            14
        }
    }

    fun getCadenceLabel(): String {
        return when (val days = getCatchUpCadenceDays()) {
            7 -> "Weekly (7d)"
            14 -> "Bi-Weekly (14d)"
            30 -> "Monthly (30d)"
            90 -> "Quarterly (90d)"
            else -> "Every ${days}d"
        }
    }

    fun daysSinceLastContact(nowMillis: Long = System.currentTimeMillis()): Int {
        val last = lastContactedAt ?: createdAt
        val diff = (nowMillis - last).coerceAtLeast(0L)
        return (diff / (24L * 60 * 60 * 1000L)).toInt()
    }

    fun isOverdueForCatchUp(nowMillis: Long = System.currentTimeMillis()): Boolean {
        return daysSinceLastContact(nowMillis) >= getCatchUpCadenceDays()
    }
}
