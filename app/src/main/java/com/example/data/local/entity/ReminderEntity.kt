package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class ReminderTone(val displayName: String, val description: String) {
    DIGITAL_CRYSTAL("Digital Crystal", "Bright dual-pulse notification chime"),
    COSMIC_CHIME("Cosmic Tri-Tone", "Melodic ascending three-tone harmonic chime"),
    GENTLE_HARP("Gentle Bell", "Warm soft resonant chime"),
    URGENT_PULSE("Urgent Double-Beep", "Crisp high-priority alert tones"),
    SYSTEM_NOTIFICATION("System Default", "Android device notification sound")
}

enum class ReminderVibratePattern(val displayName: String, val description: String) {
    STRONG_PULSE("Strong Pulse", "3 sharp tactile physical pulses"),
    DOUBLE_TAP("Double Tap", "Modern dual haptic message taps"),
    GENTLE_BUZZ("Gentle Buzz", "Soft gentle vibration wave"),
    NONE("No Vibration", "Audible chime only")
}

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("noteId")]
)
data class ReminderEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val noteId: String? = null,
    val title: String,
    val details: String = "",
    val reminderTimeMillis: Long,
    val toneType: String = ReminderTone.DIGITAL_CRYSTAL.name,
    val vibratePattern: String = ReminderVibratePattern.STRONG_PULSE.name,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getReminderTone(): ReminderTone {
        return try {
            ReminderTone.valueOf(toneType)
        } catch (_: Exception) {
            ReminderTone.DIGITAL_CRYSTAL
        }
    }

    fun getVibrationPattern(): ReminderVibratePattern {
        return try {
            ReminderVibratePattern.valueOf(vibratePattern)
        } catch (_: Exception) {
            ReminderVibratePattern.STRONG_PULSE
        }
    }
}
