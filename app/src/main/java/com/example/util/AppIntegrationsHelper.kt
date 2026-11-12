package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.CalendarContract
import android.widget.Toast
import com.example.data.local.converter.RoomConverters
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.PersonEntity
import com.example.data.model.NoteWithDetails
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object AppIntegrationsHelper {

    private val converters = RoomConverters()

    /**
     * Launches the native Android / Google Calendar event insertion screen
     * pre-populated with title, description, and parsed date.
     */
    fun scheduleInCalendar(
        context: Context,
        title: String,
        description: String = "",
        dateHintOrIso: String? = null,
        explicitTimeMillis: Long? = null
    ) {
        val startMillis = explicitTimeMillis ?: parseDateHintToMillis(dateHintOrIso)
        val endMillis = startMillis + (60 * 60 * 1000L) // Default 1 hour duration

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            if (description.isNotBlank()) {
                putExtra(CalendarContract.Events.DESCRIPTION, description)
            }
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback for emulators without a pre-installed calendar activity
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val dateStr = SimpleDateFormat("EEE, MMM d, yyyy h:mm a", Locale.getDefault()).format(Date(startMillis))
            clipboard?.setPrimaryClip(
                ClipData.newPlainText("Calendar Event", "$title — $dateStr\n$description")
            )
            Toast.makeText(
                context,
                "Event details copied for Calendar ($dateStr)",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Parses common date strings (YYYY-MM-DD, MM/DD, "tomorrow", "next week") into epoch millis.
     */
    fun parseDateHintToMillis(hint: String?): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (hint.isNullOrBlank()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            return cal.timeInMillis
        }

        val lower = hint.trim().lowercase()
        when {
            lower.contains("today") || lower.contains("tonight") -> {
                cal.set(Calendar.HOUR_OF_DAY, 18)
                return cal.timeInMillis
            }
            lower.contains("tomorrow") -> {
                cal.add(Calendar.DAY_OF_YEAR, 1)
                return cal.timeInMillis
            }
            lower.contains("next week") -> {
                cal.add(Calendar.DAY_OF_YEAR, 7)
                return cal.timeInMillis
            }
        }

        val formats = listOf("yyyy-MM-dd", "MM/dd/yyyy", "MMM d, yyyy", "MMM d", "MM-dd")
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.US)
                val parsed = sdf.parse(hint.trim())
                if (parsed != null) {
                    val parsedCal = Calendar.getInstance().apply { time = parsed }
                    if (fmt == "MMM d" || fmt == "MM-dd") {
                        parsedCal.set(Calendar.YEAR, cal.get(Calendar.YEAR))
                        if (parsedCal.before(Calendar.getInstance())) {
                            parsedCal.add(Calendar.YEAR, 1)
                        }
                    }
                    parsedCal.set(Calendar.HOUR_OF_DAY, 10)
                    parsedCal.set(Calendar.MINUTE, 0)
                    return parsedCal.timeInMillis
                }
            } catch (_: Exception) {
            }
        }

        cal.add(Calendar.DAY_OF_YEAR, 1)
        return cal.timeInMillis
    }

    /**
     * Formats a Person's living dossier and linked memories into clean Markdown and opens the Share Sheet.
     */
    fun sharePersonDossierAsMarkdown(
        context: Context,
        person: PersonEntity,
        linkedNotes: List<NoteWithDetails>
    ) {
        val facts = converters.toStringList(person.factsJson)
        val dates = converters.toStringMap(person.keyDatesJson)
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

        val md = buildString {
            appendLine("# ${person.name}")
            appendLine("**Relationship:** ${person.relationship ?: "Contact"}")
            val lastContactStr = person.lastContactedAt?.let { dateFormat.format(Date(it)) } ?: "Recent"
            appendLine("**Last Interaction:** $lastContactStr")
            appendLine()
            if (facts.isNotEmpty()) {
                appendLine("## Key Facts & Dossier")
                facts.forEach { fact ->
                    appendLine("- $fact")
                }
                appendLine()
            }
            if (dates.isNotEmpty()) {
                appendLine("## Important Dates")
                dates.forEach { (label, dateVal) ->
                    appendLine("- **$label:** $dateVal")
                }
                appendLine()
            }
            if (linkedNotes.isNotEmpty()) {
                appendLine("## Linked Memories (${linkedNotes.size})")
                linkedNotes.forEach { item ->
                    val noteDate = dateFormat.format(Date(item.note.createdAt))
                    appendLine("### ${item.note.title} ($noteDate)")
                    appendLine(item.note.content)
                    appendLine()
                }
            }
            appendLine("---")
            appendLine("*Exported from Recall Personal Memory CRM*")
        }

        shareMarkdownText(context, "Dossier: ${person.name}", md)
    }

    /**
     * Formats a Note into clean Markdown and opens the Share Sheet.
     */
    fun shareNoteAsMarkdown(
        context: Context,
        title: String,
        content: String,
        category: String,
        tags: List<String>,
        peopleNames: List<String>,
        rawTranscript: String? = null
    ) {
        val dateFormat = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
        val md = buildString {
            appendLine("# ${title.ifBlank { "Untitled Memory" }}")
            appendLine("**Category:** $category | **Date:** ${dateFormat.format(Date())}")
            if (peopleNames.isNotEmpty()) {
                appendLine("**People Linked:** ${peopleNames.joinToString(", ")}")
            }
            if (tags.isNotEmpty()) {
                appendLine("**Tags:** ${tags.joinToString(" ") { "#$it" }}")
            }
            appendLine()
            appendLine("## Notes")
            appendLine(content)
            if (!rawTranscript.isNullOrBlank() && rawTranscript != content) {
                appendLine()
                appendLine("## Full Audio Transcript")
                appendLine(rawTranscript)
            }
            appendLine()
            appendLine("---")
            appendLine("*Exported from Recall*")
        }

        shareMarkdownText(context, title.ifBlank { "Recall Memory" }, md)
    }

    private fun shareMarkdownText(context: Context, subject: String, markdown: String) {
        // Also copy to clipboard for instant pasting convenience
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText(subject, markdown))
        } catch (_: Exception) {
        }

        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, markdown)
            }
            val chooser = Intent.createChooser(sendIntent, "Export Markdown").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Markdown copied to clipboard!", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Triggers a subtle tactile haptic click when Haptic Feedback is enabled in user preferences.
     */
    fun triggerHapticClick(context: Context, enabled: Boolean = true) {
        if (!enabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(18L, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createOneShot(18L, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    v?.vibrate(18L)
                }
            }
        } catch (_: Exception) {
        }
    }
}
