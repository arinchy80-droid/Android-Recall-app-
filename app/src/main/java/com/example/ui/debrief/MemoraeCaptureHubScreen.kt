package com.example.ui.debrief

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CommitmentEntity
import com.example.ui.RecallViewModel
import com.example.ui.theme.NeuralAppBackground
import com.example.ui.theme.RecallCyanSecondary
import com.example.ui.theme.RecallEmeraldAccent
import com.example.ui.theme.RecallVioletPrimary
import com.example.ui.theme.seamlessInnerTextFieldColors
import com.example.util.DynamicListType
import com.example.util.MemoryIntelligenceHelper
import com.example.util.RecallLifeOrganiserManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MemoraeCaptureHubScreen(
    viewModel: RecallViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val selectedTab by viewModel.captureSubTab.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val recordingDuration by viewModel.recordingDuration.collectAsStateWithLifecycle()
    val liveTranscript by viewModel.liveTranscript.collectAsStateWithLifecycle()
    val isProcessingDebrief by viewModel.isProcessingDebrief.collectAsStateWithLifecycle()
    val allPeople by viewModel.people.collectAsStateWithLifecycle()

    var brainDumpText by remember { mutableStateOf("") }
    var processingStage by remember { mutableStateOf<String?>(null) }
    var lastAutoOrganisedSummary by remember { mutableStateOf<String?>(null) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startDebriefRecording()
        } else {
            viewModel.showFeedback("Microphone permission is needed to record voice notes.")
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            processingStage = "Analyzing camera capture (Vision OCR & Entity Extraction)..."
            viewModel.processCameraCapture(bitmap)
            scope.launch {
                delay(1800)
                processingStage = null
                lastAutoOrganisedSummary = "Visual memory captured, OCR text indexed, and saved to Memories."
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            try {
                cameraLauncher.launch()
            } catch (e: Throwable) {
                viewModel.showFeedback("Could not open camera: ${e.message}")
            }
        } else {
            viewModel.showFeedback("Camera permission is needed to capture photos.")
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            processingStage = "Processing uploaded image/document with AI Vision & OCR..."
            viewModel.processImageUriCapture(uri)
            scope.launch {
                delay(1800)
                processingStage = null
                lastAutoOrganisedSummary = "Uploaded image/document analyzed with OCR and linked to your Memory repository."
            }
        }
    }

    val naturalDateSuggestion = remember(brainDumpText, allPeople) {
        RecallLifeOrganiserManager.parseNaturalDateTimeSuggestion(
            text = brainDumpText,
            knownPeopleNames = allPeople.map { it.name }
        )
    }

    NeuralAppBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Mode Switcher: Quick Capture Studio vs Conversational Daily Debrief
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Quick Capture", "Daily Debrief").forEach { mode ->
                    val isSelected = selectedTab == mode
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) RecallVioletPrimary else Color(0xFF0F172A).copy(alpha = 0.85f),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) RecallCyanSecondary else Color(0xFF334155)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { viewModel.captureSubTab.value = mode }
                            .testTag("capture_tab_${mode.lowercase().replace(" ", "_")}")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (mode == "Quick Capture") Icons.Default.AutoAwesome else Icons.Default.Mic,
                                contentDescription = mode,
                                tint = if (isSelected) Color.White else RecallCyanSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (mode == "Quick Capture") "Quick Capture" else "Daily Debrief",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            if (selectedTab == "Daily Debrief") {
                Box(modifier = Modifier.weight(1f)) {
                    DailyDebriefScreen(viewModel = viewModel)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 104.dp)
                ) {
                    // Processing Status Banner
                    item {
                        AnimatedVisibility(visible = processingStage != null || isProcessingDebrief) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF0F172A).copy(alpha = 0.94f),
                                border = BorderStroke(1.dp, RecallCyanSecondary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            color = RecallCyanSecondary,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = processingStage ?: "Transcribing & extracting memories, people, and commitments...",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        color = RecallCyanSecondary,
                                        trackColor = Color(0xFF1E293B),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    // Last Auto-Organised Confirmation Banner
                    item {
                        AnimatedVisibility(visible = lastAutoOrganisedSummary != null) {
                            lastAutoOrganisedSummary?.let { msg ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = RecallEmeraldAccent.copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, RecallEmeraldAccent),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = RecallEmeraldAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = msg,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 1. Zero-Friction Brain Dump (No Category Selection Needed Before Capture)
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF0F172A).copy(alpha = 0.88f)
                            ),
                            border = BorderStroke(
                                1.2.dp,
                                Brush.horizontalGradient(listOf(RecallCyanSecondary, RecallVioletPrimary))
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "✨ Effortless Memory Capture",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Write or dictate any thought, conversation, promise, task, or shopping list. Recall automatically categorises, tags, and links it afterward.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCBD5E1)
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = brainDumpText,
                                    onValueChange = { brainDumpText = it },
                                    placeholder = {
                                        Text(
                                            "e.g. “Met Sarah at Kings Park Cafe. I promised to send her the Q4 design deck by Friday.”",
                                            fontSize = 13.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    },
                                    minLines = 3,
                                    maxLines = 6,
                                    colors = seamlessInnerTextFieldColors(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFF1E293B).copy(alpha = 0.85f))
                                        .testTag("capture_brain_dump_input")
                                )

                                // Natural Date Suggestion Banner if detected
                                AnimatedVisibility(visible = naturalDateSuggestion != null) {
                                    naturalDateSuggestion?.let { suggestion ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFF1E293B),
                                            border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.NotificationsActive,
                                                    contentDescription = null,
                                                    tint = Color(0xFFF59E0B),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "Suggested Reminder: ${suggestion.formattedDateLabel}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color(0xFFFDE68A),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = suggestion.suggestedAction,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color.White,
                                                        maxLines = 1
                                                    )
                                                }
                                                Button(
                                                    onClick = {
                                                        viewModel.scheduleReminder(
                                                            title = suggestion.suggestedAction,
                                                            details = suggestion.originalText,
                                                            timeMillis = suggestion.suggestedTimeMillis,
                                                            recurrence = suggestion.recurrenceHint ?: "NONE"
                                                        )
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(32.dp)
                                                ) {
                                                    Text("+ Alarm", color = Color(0xFF050811), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Quick Text Polish & Smart Format Bar
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (brainDumpText.isNotBlank()) RecallVioletPrimary.copy(alpha = 0.25f) else Color(0xFF1E293B),
                                        border = BorderStroke(
                                            1.dp,
                                            if (brainDumpText.isNotBlank()) RecallVioletPrimary else Color(0xFF334155)
                                        ),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable(enabled = brainDumpText.isNotBlank()) {
                                                val raw = brainDumpText.trim()
                                                if (raw.isNotBlank()) {
                                                    processingStage = "Polishing & structuring text with AI..."
                                                    viewModel.aiCleanUpNote(raw) { cleaned ->
                                                        brainDumpText = cleaned
                                                        processingStage = null
                                                    }
                                                }
                                            }
                                            .testTag("capture_ai_cleanup_chip")
                                    ) {
                                        Text(
                                            text = "✨ AI Clean Up",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (brainDumpText.isNotBlank()) RecallCyanSecondary else Color(0xFF64748B),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }

                                    listOf(
                                        "🤝 Meeting Notes" to "Met with [Name] at [Place].\nKey Takeaways:\n- \nNext Steps / Promises:\n- I promised to ",
                                        "💡 Quick Idea" to "Idea: \nWhy it matters: \nNext step to explore: ",
                                        "🛒 Shopping List" to "Buy:\n- \n- \n- "
                                    ).forEach { (label, templateText) ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFF1E293B),
                                            border = BorderStroke(1.dp, Color(0xFF334155)),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    brainDumpText = if (brainDumpText.isBlank()) {
                                                        templateText
                                                    } else {
                                                        "$brainDumpText\n$templateText"
                                                    }
                                                }
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFFE2E8F0),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val raw = brainDumpText.trim()
                                            if (raw.isNotBlank()) {
                                                processingStage = "Auto-organising memory, tags, people & commitments..."
                                                viewModel.smartTagNote(raw) { title, category, _, tags ->
                                                    viewModel.saveFullNote(
                                                        id = null,
                                                        title = title,
                                                        content = raw,
                                                        category = category,
                                                        linkedPersonIds = emptyList(),
                                                        tags = tags
                                                    )
                                                    // Also extract any explicit tasks or promises
                                                    val extractedTasks = MemoryIntelligenceHelper.extractActionItemsFromText(raw)
                                                    extractedTasks.forEach { t -> viewModel.createQuickActionItem(t) }
                                                    if (raw.contains(Regex("\\b(promised|i'll|i will|owe)\\b", RegexOption.IGNORE_CASE))) {
                                                        viewModel.saveCommitment(
                                                            CommitmentEntity(
                                                                description = title.ifBlank { raw.take(80) },
                                                                direction = "BY_ME"
                                                            )
                                                        )
                                                    }
                                                    processingStage = null
                                                    lastAutoOrganisedSummary = "Saved \"$title\" under $category (${tags.size} tags, ${extractedTasks.size} task(s))."
                                                    brainDumpText = ""
                                                }
                                            }
                                        },
                                        enabled = brainDumpText.isNotBlank(),
                                        colors = ButtonDefaults.buttonColors(containerColor = RecallVioletPrimary),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("capture_auto_organise_btn")
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Capture & Auto-Organise", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val raw = brainDumpText.trim()
                                            if (raw.isNotBlank()) {
                                                val items = RecallLifeOrganiserManager.extractListItemsFromText(raw)
                                                val listTitle = items.firstOrNull()?.take(28) ?: "Captured List"
                                                viewModel.createDynamicList(
                                                    title = listTitle,
                                                    type = if (raw.lowercase().contains("buy") || raw.lowercase().contains("shop"))
                                                        DynamicListType.SHOPPING else DynamicListType.TODO,
                                                    initialItems = items
                                                )
                                                lastAutoOrganisedSummary = "Created Dynamic List \"$listTitle\" with ${items.size} item(s) in Organise."
                                                brainDumpText = ""
                                            }
                                        },
                                        enabled = brainDumpText.isNotBlank(),
                                        border = BorderStroke(1.dp, RecallCyanSecondary)
                                    ) {
                                        Text("Save as List", color = RecallCyanSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // 2. Voice Recording & Automatic Transcription Card
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF0F172A).copy(alpha = 0.88f)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isRecording) Color(0xFFEF4444) else RecallVioletPrimary.copy(alpha = 0.7f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = if (isRecording) Color(0xFFEF4444) else RecallCyanSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isRecording) "🎙️ Recording Voice Memory (${recordingDuration}s)..."
                                            else "🎙️ Voice Recording & Smart Unpack",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Speak naturally — Recall transcribes, extracts commitments, and links people.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }

                                if (liveTranscript.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF1E293B),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = liveTranscript,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            if (isRecording) {
                                                viewModel.finishAndProcessDebrief()
                                                viewModel.captureSubTab.value = "Daily Debrief"
                                            } else {
                                                val hasPerm = ContextCompat.checkSelfPermission(
                                                    context,
                                                    Manifest.permission.RECORD_AUDIO
                                                ) == PackageManager.PERMISSION_GRANTED
                                                if (hasPerm) {
                                                    viewModel.startDebriefRecording()
                                                } else {
                                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isRecording) Color(0xFFEF4444) else RecallVioletPrimary
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("capture_voice_record_btn")
                                    ) {
                                        Icon(
                                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isRecording) "Stop & Unpack Memory" else "Start Voice Recording",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Image & Document Intelligence Capture (Phase 6: OCR for Business Cards, Receipts, Notes, Whiteboards)
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF0F172A).copy(alpha = 0.88f)
                            ),
                            border = BorderStroke(1.dp, RecallEmeraldAccent.copy(alpha = 0.65f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "📸 Image & Document Intelligence (Vision + OCR)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Capture or upload business cards, handwritten lists, receipts, whiteboards, or documents. Original images and source context are preserved.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCBD5E1)
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val hasPerm = ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.CAMERA
                                            ) == PackageManager.PERMISSION_GRANTED
                                            if (hasPerm) {
                                                try {
                                                    cameraLauncher.launch()
                                                } catch (e: Throwable) {
                                                    viewModel.showFeedback("Could not open camera: ${e.message}")
                                                }
                                            } else {
                                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = RecallEmeraldAccent),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color(0xFF050811), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Snap Camera", color = Color(0xFF050811), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                        border = BorderStroke(1.dp, RecallCyanSecondary),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = RecallCyanSecondary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Upload Image / Doc", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = { viewModel.openNoteEditor(null) },
                                    border = BorderStroke(1.dp, Color(0xFF475569)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open Full Rich Note & Document Editor", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
