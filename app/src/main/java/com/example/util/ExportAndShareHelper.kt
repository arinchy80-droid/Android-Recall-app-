package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.local.converter.RoomConverters
import com.example.data.local.entity.ActionItemEntity
import com.example.data.local.entity.PersonEntity
import com.example.data.model.DailyDebriefResult
import com.example.data.model.NoteWithDetails
import java.io.File
import java.io.FileOutputStream

object ExportAndShareHelper {

    fun formatNoteAsMarkdown(noteWithDetails: NoteWithDetails): String {
        val note = noteWithDetails.note
        val sb = StringBuilder()
        sb.appendLine("# ${note.title.ifBlank { "Untitled Memory" }}")
        sb.appendLine()
        sb.appendLine("- **Category:** ${note.category}")
        sb.appendLine("- **Created:** ${DateUtils.formatDateTime(note.createdAt)}")
        note.getLocationName()?.let { loc ->
            sb.appendLine("- **Location:** 📍 $loc")
        }
        val tags = note.getDisplayTagsList()
        if (tags.isNotEmpty()) {
            sb.appendLine("- **Tags:** ${tags.joinToString(" ") { "#$it" }}")
        }
        if (noteWithDetails.people.isNotEmpty()) {
            sb.appendLine("- **Linked People:** ${noteWithDetails.people.joinToString(", ") { it.name }}")
        }
        sb.appendLine()

        if (!note.aiSummary.isNullOrBlank()) {
            sb.appendLine("## ✨ AI Summary")
            sb.appendLine("> ${note.aiSummary}")
            sb.appendLine()
        }

        sb.appendLine("## 📝 Memory Notes")
        sb.appendLine(note.content.ifBlank { "_No written content._" })
        sb.appendLine()

        if (noteWithDetails.actionItems.isNotEmpty()) {
            sb.appendLine("## ✅ Action Items & Checklists")
            noteWithDetails.actionItems.forEach { item ->
                val mark = if (item.isCompleted) "[x]" else "[ ]"
                sb.appendLine("- $mark ${item.task}")
            }
            sb.appendLine()
        }

        if (!note.rawTranscription.isNullOrBlank()) {
            sb.appendLine("## 🎙️ Verbatim Audio Transcript")
            sb.appendLine(note.rawTranscription)
            sb.appendLine()
        }

        sb.appendLine("---")
        sb.appendLine("_Exported from Recall Personal Memory AI_")
        return sb.toString()
    }

    fun formatPersonDossierAsMarkdown(
        person: PersonEntity,
        linkedNotes: List<NoteWithDetails>,
        openActions: List<ActionItemEntity>
    ): String {
        val converters = RoomConverters()
        val facts = converters.toStringList(person.factsJson)
        val dates = converters.toStringMap(person.keyDatesJson)
            .filterKeys { it != "CatchUpCadenceDays" }

        val sb = StringBuilder()
        sb.appendLine("# 👤 Dossier: ${person.name}")
        sb.appendLine("- **Circle / Relationship:** ${person.relationship ?: "General"}")
        sb.appendLine("- **Catch-Up Cadence:** ${person.getCadenceLabel()}")
        sb.appendLine("- **Last Contacted:** ${person.lastContactedAt?.let { DateUtils.formatDate(it) } ?: "No recent log"} (${person.daysSinceLastContact()}d ago)")
        sb.appendLine()

        if (facts.isNotEmpty()) {
            sb.appendLine("## 🧠 Key Facts & Preferences")
            facts.forEach { fact ->
                sb.appendLine("- $fact")
            }
            sb.appendLine()
        }

        if (dates.isNotEmpty()) {
            sb.appendLine("## 📅 Key Dates & Milestones")
            dates.forEach { (k, v) ->
                sb.appendLine("- **$k:** $v")
            }
            sb.appendLine()
        }

        if (openActions.isNotEmpty()) {
            sb.appendLine("## ⚡ Open Action Items")
            openActions.forEach { action ->
                sb.appendLine("- [ ] ${action.task}")
            }
            sb.appendLine()
        }

        if (linkedNotes.isNotEmpty()) {
            sb.appendLine("## 🗂️ Shared Memory History (${linkedNotes.size})")
            linkedNotes.take(10).forEach { n ->
                sb.appendLine("### ${n.note.title} (${DateUtils.formatDate(n.note.createdAt)})")
                sb.appendLine(n.note.content.take(240))
                sb.appendLine()
            }
        }

        sb.appendLine("---")
        sb.appendLine("_Exported from Recall Personal Memory AI_")
        return sb.toString()
    }

    fun formatDebriefAsMarkdown(debrief: DailyDebriefResult): String {
        val sb = StringBuilder()
        sb.appendLine("# 🎙️ Daily Debrief Summary")
        sb.appendLine("- **Date:** ${DateUtils.formatDateTime(System.currentTimeMillis())}")
        sb.appendLine()
        sb.appendLine("## ✨ Executive Summary")
        sb.appendLine(debrief.dailySummary)
        sb.appendLine()

        if (debrief.actionItems.isNotEmpty()) {
            sb.appendLine("## ✅ Extracted Action Items")
            debrief.actionItems.forEach { item ->
                val dueSuffix = if (!item.dueDateHint.isNullOrBlank()) " (Due: ${item.dueDateHint})" else ""
                sb.appendLine("- [ ] ${item.task}$dueSuffix")
            }
            sb.appendLine()
        }

        if (debrief.peopleUpdates.isNotEmpty()) {
            sb.appendLine("## 👥 People & CRM Updates")
            debrief.peopleUpdates.forEach { p ->
                sb.appendLine("- **${p.personName}** (${p.relationshipHint ?: "Contact"}): ${p.newFacts.joinToString("; ")}")
            }
            sb.appendLine()
        }

        if (debrief.distinctNotes.isNotEmpty()) {
            sb.appendLine("## 📝 Unpacked Memories")
            debrief.distinctNotes.forEach { n ->
                sb.appendLine("### ${n.title} [${n.category}]")
                sb.appendLine(n.content)
                sb.appendLine()
            }
        }

        sb.appendLine("---")
        sb.appendLine("_Exported from Recall Personal Memory AI_")
        return sb.toString()
    }

    fun shareTextSummary(context: Context, title: String, markdownContent: String) {
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, markdownContent)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(sendIntent, "Share via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            copyToClipboard(context, title, markdownContent)
        }
    }

    fun exportAndShareMarkdownFile(
        context: Context,
        fileNamePrefix: String,
        title: String,
        markdownContent: String,
        onResult: (String) -> Unit
    ) {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val safeName = fileNamePrefix.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(36).ifBlank { "recall_export" }
            val mdFile = File(exportDir, "${safeName}_${System.currentTimeMillis()}.md")
            mdFile.writeText(markdownContent)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                mdFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/markdown"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, markdownContent)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(shareIntent, "Export Markdown ($safeName.md)").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            onResult("Markdown file exported (${mdFile.name}) & ready to share!")
        } catch (e: Exception) {
            copyToClipboard(context, title, markdownContent)
            onResult("Markdown copied to clipboard!")
        }
    }

    fun exportAndSharePdfDocument(
        context: Context,
        fileNamePrefix: String,
        documentTitle: String,
        subtitle: String,
        markdownContent: String,
        onResult: (String) -> Unit
    ) {
        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595 // A4 width in points
            val pageHeight = 842 // A4 height in points
            val margin = 44f
            val contentWidth = pageWidth - (margin * 2)

            val headerBgPaint = Paint().apply {
                color = AndroidColor.parseColor("#0F172A")
                style = Paint.Style.FILL
            }
            val accentPaint = Paint().apply {
                color = AndroidColor.parseColor("#06B6D4")
                style = Paint.Style.FILL
            }
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.WHITE
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.parseColor("#38BDF8")
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            val h2Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.parseColor("#6D28D9")
                textSize = 13.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.parseColor("#1E293B")
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.parseColor("#64748B")
                textSize = 9f
            }

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas: Canvas = page.canvas

            // Draw Top Banner on Page 1
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 96f, headerBgPaint)
            canvas.drawRect(0f, 92f, pageWidth.toFloat(), 96f, accentPaint)
            canvas.drawText(documentTitle.take(48), margin, 46f, titlePaint)
            canvas.drawText(subtitle.take(80), margin, 70f, subPaint)

            var yPos = 126f

            fun startNewPage() {
                canvas.drawText("Recall Personal Memory AI • Page $pageNumber", margin, pageHeight - 24f, footerPaint)
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPos = 54f
            }

            val rawLines = markdownContent.lines()
            for (rawLine in rawLines) {
                val trimmed = rawLine.trim()
                if (trimmed.isEmpty()) {
                    yPos += 8f
                    if (yPos > pageHeight - 56f) startNewPage()
                    continue
                }

                val isHeading = trimmed.startsWith("#")
                val cleanText = trimmed.removePrefix("### ").removePrefix("## ").removePrefix("# ")
                val activePaint = if (isHeading) h2Paint else bodyPaint
                val lineHeight = if (isHeading) 20f else 16f

                if (isHeading) {
                    yPos += 6f
                }

                // Wrap line to contentWidth
                val words = cleanText.split(" ")
                var currentLine = ""
                for (word in words) {
                    val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                    if (activePaint.measureText(testLine) <= contentWidth) {
                        currentLine = testLine
                    } else {
                        if (yPos > pageHeight - 56f) startNewPage()
                        canvas.drawText(currentLine, margin, yPos, activePaint)
                        yPos += lineHeight
                        currentLine = word
                    }
                }
                if (currentLine.isNotEmpty()) {
                    if (yPos > pageHeight - 56f) startNewPage()
                    canvas.drawText(currentLine, margin, yPos, activePaint)
                    yPos += lineHeight
                }
            }

            canvas.drawText("Recall Personal Memory AI • Page $pageNumber", margin, pageHeight - 24f, footerPaint)
            pdfDocument.finishPage(page)

            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val safeName = fileNamePrefix.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(36).ifBlank { "recall_document" }
            val pdfFile = File(exportDir, "${safeName}_${System.currentTimeMillis()}.pdf")
            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_SUBJECT, documentTitle)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(shareIntent, "Export PDF ($safeName.pdf)").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            onResult("PDF generated (${pdfFile.name}) & ready to share!")
        } catch (e: Exception) {
            copyToClipboard(context, documentTitle, markdownContent)
            onResult("Formatted text copied to clipboard (${e.localizedMessage ?: "PDF fallback"})")
        }
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
    }
}
