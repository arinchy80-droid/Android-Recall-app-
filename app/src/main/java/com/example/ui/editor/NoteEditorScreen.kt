package com.example.ui.editor

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import java.io.File
import com.example.util.PhotoStorageHelper
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tag
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import com.example.data.model.MemoryCategory
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlin.math.abs
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.ActionItemEntity
import com.example.data.local.entity.MediaEntity
import com.example.data.local.entity.MediaType
import com.example.data.local.entity.PersonEntity
import com.example.data.model.NoteWithDetails
import com.example.ui.RecallViewModel
import com.example.ui.components.AudioPlayerCard
import com.example.ui.theme.CategoryIdeas
import com.example.ui.theme.CategoryMeeting
import com.example.ui.theme.CategoryPersonal
import com.example.ui.theme.CategoryWork
import com.example.ui.theme.RecallCyanSecondary
import com.example.ui.theme.RecallVioletPrimary
import androidx.compose.foundation.horizontalScroll
import com.example.ui.theme.NeuralAppBackground
import com.example.ui.theme.TaskbarDropdownButton
import com.example.ui.theme.UnifiedTaskbarInputBox
import com.example.ui.theme.VibrantTagBadge
import com.example.ui.theme.getVibrantTagColor
import com.example.ui.theme.seamlessInnerTextFieldColors
import com.example.ui.theme.translucentWhiteTextFieldColors
import com.example.util.AppIntegrationsHelper
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Share
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.ReminderTone
import com.example.data.local.entity.ReminderVibratePattern
import com.example.ui.components.ExportAndShareDialog
import com.example.ui.components.ReminderSchedulerDialog
import com.example.util.ExportAndShareHelper
import com.example.util.MemoryIntelligenceHelper
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    viewModel: RecallViewModel,
    noteWithDetails: NoteWithDetails?,
    allPeople: List<PersonEntity>,
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val clipboardManager = LocalClipboardManager.current
    val currentNoteId = remember { noteWithDetails?.note?.id ?: UUID.randomUUID().toString() }

    var title by remember { mutableStateOf(noteWithDetails?.note?.title ?: "") }
    var content by remember { mutableStateOf(noteWithDetails?.note?.content ?: "") }
    var selectedCategory by remember { mutableStateOf(noteWithDetails?.note?.category ?: "Meeting") }
    var rawTranscription by remember { mutableStateOf(noteWithDetails?.note?.rawTranscription ?: "") }
    var autoSaveStatus by remember { mutableStateOf(if (noteWithDetails != null) "Saved" else "Draft") }
    var showMoreMenu by remember { mutableStateOf(false) }

    val attachedPeopleIds = remember {
        mutableStateListOf<String>().apply {
            noteWithDetails?.people?.map { it.id }?.let { addAll(it) }
        }
    }

    val attachedMedia = remember {
        mutableStateListOf<MediaEntity>().apply {
            noteWithDetails?.media?.let { addAll(it) }
        }
    }

    val attachedActions = remember {
        mutableStateListOf<ActionItemEntity>().apply {
            noteWithDetails?.actionItems?.let { addAll(it) }
        }
    }

    val tagsList = remember {
        mutableStateListOf<String>().apply {
            noteWithDetails?.note?.getDisplayTagsList()?.let { addAll(it) }
        }
    }
    var locationName by remember { mutableStateOf(noteWithDetails?.note?.getLocationName()) }
    var showLocationDropdown by remember { mutableStateOf(false) }
    var showCustomLocationDialog by remember { mutableStateOf(false) }
    var customLocationInput by remember { mutableStateOf("") }
    var selectedProjectId by remember { mutableStateOf(noteWithDetails?.note?.projectId) }
    var showProjectDropdown by remember { mutableStateOf(false) }
    var showNewProjectInEditorDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var actionItemForReminder by remember { mutableStateOf<ActionItemEntity?>(null) }
    var newActionTaskInput by remember { mutableStateOf("") }
    var customTagInput by remember { mutableStateOf("") }

    val allNotes by viewModel.notes.collectAsStateWithLifecycle()
    val allPlaces by viewModel.allPlaces.collectAsStateWithLifecycle()
    val allProjects by viewModel.allProjects.collectAsStateWithLifecycle()
    val recordingDuration by viewModel.recordingDuration.collectAsStateWithLifecycle()

    val noteTextColors = remember {
        listOf(
            "Crisp White" to Color(0xFFF8FAFC),
            "Electric Cyan" to Color(0xFF38BDF8),
            "Vibrant Violet" to Color(0xFFA78BFA),
            "Emerald Mint" to Color(0xFF34D399),
            "Amber Gold" to Color(0xFFFBBF24),
            "Rose Coral" to Color(0xFFFB7185)
        )
    }
    var selectedTextColorIndex by remember { mutableStateOf(0) }
    val currentNoteTextColor = noteTextColors[selectedTextColorIndex].second

    val activeReminder by viewModel.getActiveReminderForNote(currentNoteId).collectAsStateWithLifecycle(initialValue = null)
    var showReminderSchedulerDialog by remember { mutableStateOf(false) }
    var showReminderDropdown by remember { mutableStateOf(false) }
    val reminderTimeFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

    // Auto-save continuously with debounce
    LaunchedEffect(title, content, selectedCategory, attachedPeopleIds.size, attachedMedia.size, tagsList.size, locationName, selectedProjectId, attachedActions.size) {
        if (title.isNotBlank() || content.isNotBlank()) {
            autoSaveStatus = "Saving..."
            kotlinx.coroutines.delay(1200)
            viewModel.autoSaveNote(
                id = currentNoteId,
                title = title,
                content = content,
                category = selectedCategory,
                linkedPersonIds = attachedPeopleIds.toList(),
                rawTranscription = rawTranscription.ifBlank { null },
                mediaAttachments = attachedMedia.toList(),
                actionItems = attachedActions.toList(),
                tags = NoteEntity.mergeTagsWithLocation(tagsList.toList(), locationName),
                projectId = selectedProjectId
            )
            autoSaveStatus = "Auto-saved"
        }
    }

    var isRecordingInEditor by remember { mutableStateOf(false) }
    var isAiCleaningUp by remember { mutableStateOf(false) }
    var isSmartTagging by remember { mutableStateOf(false) }
    var editorFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }

    LaunchedEffect(editorFeedbackMessage) {
        if (editorFeedbackMessage != null) {
            kotlinx.coroutines.delay(3000)
            editorFeedbackMessage = null
        }
    }

    val categories = MemoryCategory.allCategories
    val context = LocalContext.current

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val detected = com.example.util.MemoryIntelligenceHelper.detectDevicePlaceName(context)
            locationName = detected ?: "Current GPS Location"
            editorFeedbackMessage = "Geotagged: 📍 $locationName"
        } else {
            locationName = "Local Area"
            editorFeedbackMessage = "Geotagged: 📍 Local Area"
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val savedPath = PhotoStorageHelper.saveBitmap(context, bitmap)
            val mediaId = UUID.randomUUID().toString()
            val media = MediaEntity(
                id = mediaId,
                noteId = currentNoteId,
                mediaType = MediaType.IMAGE,
                localUri = savedPath,
                aiDescription = "Analyzing photo with Gemini..."
            )
            attachedMedia.add(media)
            if (title.isBlank()) {
                title = "Photo Memory"
            }
            if (selectedCategory == "General") {
                selectedCategory = "Visuals"
            }
            // Trigger AI vision synthesis into the SAME note
            viewModel.analyzeAndAttachPhotoToCurrentNote(
                bitmap = bitmap,
                savedPath = savedPath,
                noteId = currentNoteId
            ) { ocr, desc, labels ->
                val index = attachedMedia.indexOfFirst { it.id == mediaId }
                if (index != -1) {
                    attachedMedia[index] = attachedMedia[index].copy(
                        aiDescription = desc ?: "Photo Memory",
                        ocrText = ocr,
                        labelsJson = labels.joinToString { "\"$it\"" }
                    )
                }
                if (!desc.isNullOrBlank()) {
                    content = if (content.isBlank()) desc else "$content\n\n$desc"
                }
                if (!ocr.isNullOrBlank() && rawTranscription.isBlank()) {
                    rawTranscription = ocr
                }
                if (title == "Photo Memory" && labels.isNotEmpty()) {
                    title = "Visual: ${labels.first()}"
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                cameraLauncher.launch()
            } catch (e: Throwable) {
                viewModel.showFeedback("Could not open camera: ${e.message}")
            }
        } else {
            viewModel.showFeedback("Camera permission is needed to take photos.")
        }
    }

    fun launchCameraSafely() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            try {
                cameraLauncher.launch()
            } catch (e: Throwable) {
                viewModel.showFeedback("Could not open camera: ${e.message}")
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val saved = PhotoStorageHelper.saveUri(context, uri)
            if (saved != null) {
                val (savedPath, bitmap) = saved
                val mediaId = UUID.randomUUID().toString()
                val media = MediaEntity(
                    id = mediaId,
                    noteId = currentNoteId,
                    mediaType = MediaType.IMAGE,
                    localUri = savedPath,
                    aiDescription = "Analyzing photo with Gemini..."
                )
                attachedMedia.add(media)
                if (title.isBlank()) {
                    title = "Photo Memory"
                }
                if (selectedCategory == "General") {
                    selectedCategory = "Visuals"
                }
                // Trigger AI vision synthesis into the SAME note
                viewModel.analyzeAndAttachPhotoToCurrentNote(
                    bitmap = bitmap,
                    savedPath = savedPath,
                    noteId = currentNoteId
                ) { ocr, desc, labels ->
                    val index = attachedMedia.indexOfFirst { it.id == mediaId }
                    if (index != -1) {
                        attachedMedia[index] = attachedMedia[index].copy(
                            aiDescription = desc ?: "Photo Memory",
                            ocrText = ocr,
                            labelsJson = labels.joinToString { "\"$it\"" }
                        )
                    }
                    if (!desc.isNullOrBlank()) {
                        content = if (content.isBlank()) desc else "$content\n\n$desc"
                    }
                    if (!ocr.isNullOrBlank() && rawTranscription.isBlank()) {
                        rawTranscription = ocr
                    }
                    if (title == "Photo Memory" && labels.isNotEmpty()) {
                        title = "Visual: ${labels.first()}"
                    }
                }
            } else {
                viewModel.showFeedback("Could not save chosen photo")
            }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.audioRecorder.startRecording(scope)
            isRecordingInEditor = true
        } else {
            viewModel.showFeedback("Microphone permission is required to dictate notes.")
        }
    }

    fun toggleDictationSafely() {
        if (isRecordingInEditor) {
            val (audioFile, liveText) = viewModel.audioRecorder.stopRecording()
            isRecordingInEditor = false
            if (audioFile != null && audioFile.exists() && audioFile.length() > 400) {
                attachedMedia.add(
                    MediaEntity(
                        id = UUID.randomUUID().toString(),
                        noteId = currentNoteId,
                        mediaType = MediaType.AUDIO,
                        localUri = audioFile.absolutePath,
                        aiDescription = "Voice Dictation Recording"
                    )
                )
            }
            if (liveText.isNotBlank()) {
                rawTranscription = liveText
                content = if (content.isBlank()) liveText else "$content\n$liveText"
                val autoExtracted = com.example.util.MemoryIntelligenceHelper.extractActionItemsFromText(liveText)
                var addedCount = 0
                for (task in autoExtracted) {
                    if (attachedActions.none { it.task.equals(task, ignoreCase = true) }) {
                        attachedActions.add(
                            ActionItemEntity(
                                noteId = currentNoteId,
                                task = task
                            )
                        )
                        addedCount++
                    }
                }
                if (addedCount > 0) {
                    editorFeedbackMessage = "Auto-extracted $addedCount action item(s) from voice note!"
                }
            }
        } else {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                viewModel.audioRecorder.startRecording(scope)
                isRecordingInEditor = true
            } else {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    NeuralAppBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
            ) {
            // 1. Small Compact Page Heading Bar
            Surface(
                color = Color(0xFF0F172A).copy(alpha = 0.90f),
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (noteWithDetails == null) "New Memory" else "Edit Memory",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = if (autoSaveStatus == "Auto-saved" || autoSaveStatus == "Saved") Color(0xFF10B981) else Color(0xFFF59E0B),
                            modifier = Modifier.size(6.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = autoSaveStatus,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    }

                    // Compact Save CTA
                    Button(
                        onClick = {
                            viewModel.saveFullNote(
                                id = currentNoteId,
                                title = title,
                                content = content,
                                category = selectedCategory,
                                linkedPersonIds = attachedPeopleIds.toList(),
                                rawTranscription = rawTranscription.ifBlank { null },
                                mediaAttachments = attachedMedia.toList(),
                                actionItems = attachedActions.toList(),
                                tags = NoteEntity.mergeTagsWithLocation(tagsList.toList(), locationName),
                                projectId = selectedProjectId
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RecallVioletPrimary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                            .heightIn(min = 34.dp)
                            .testTag("editor_save_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                    }

                    Spacer(modifier = Modifier.width(2.dp))

                    // Direct Delete button in top bar
                    IconButton(
                        onClick = { showDeleteConfirmationDialog = true },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("editor_delete_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Memory",
                            tint = Color(0xFFF87171),
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // More options (...)
                    Box {
                        IconButton(
                            onClick = { showMoreMenu = true },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = Color.White, modifier = Modifier.size(18.dp))
                        }

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false },
                            properties = PopupProperties(clippingEnabled = true),
                            modifier = Modifier
                                .background(Color(0xFF0F172A))
                                .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text("📤 Export / Share (PDF & Markdown)", color = Color.White, fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = RecallCyanSecondary) },
                                onClick = {
                                    showMoreMenu = false
                                    showExportDialog = true
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Schedule in Calendar", color = Color.White, fontWeight = FontWeight.Medium) },
                                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = RecallCyanSecondary) },
                                onClick = {
                                    showMoreMenu = false
                                    AppIntegrationsHelper.scheduleInCalendar(
                                        context = context,
                                        title = title.ifBlank { "Recall Memory Follow-Up" },
                                        description = content,
                                        dateHintOrIso = null
                                    )
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (activeReminder != null) "Edit Physical Tone Reminder" else "Set Physical Tone Reminder",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B)
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    scope.launch {
                                        delay(60)
                                        showReminderSchedulerDialog = true
                                    }
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Copy Note Text", color = Color.White, fontWeight = FontWeight.Medium) },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = RecallVioletPrimary) },
                                onClick = {
                                    showMoreMenu = false
                                    clipboardManager.setText(AnnotatedString("$title\n\n$content"))
                                    viewModel.showFeedback("Note text copied to clipboard.")
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Clear Body", color = Color.White, fontWeight = FontWeight.Medium) },
                                leadingIcon = { Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF94A3B8)) },
                                onClick = {
                                    showMoreMenu = false
                                    content = ""
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Delete Memory", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMoreMenu = false
                                    scope.launch {
                                        delay(60)
                                        showDeleteConfirmationDialog = true
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Dropdown states for the integrated taskbar across the top of the inside of the text box
            var showCategoryDropdown by remember { mutableStateOf(false) }
            var showPeopleDropdown by remember { mutableStateOf(false) }
            var showTagSuggestionsDropdown by remember { mutableStateOf(false) }
            var showAiToolsDropdown by remember { mutableStateOf(false) }
            var showMediaInsertDropdown by remember { mutableStateOf(false) }
            var showInkColorDropdown by remember { mutableStateOf(false) }
            var showCustomTagDialog by remember { mutableStateOf(false) }
            val activeCatColor = getVibrantTagColor(selectedCategory)
            val taskbarScroll = rememberScrollState()
            val popularTagSuggestions = remember {
                listOf("urgent", "follow-up", "proposal", "idea", "decision", "quarterly", "client", "priority", "milestone", "budget", "action", "personal")
            }

            val triggerSmartTag: () -> Unit = {
                val textToAnalyze = when {
                    content.isNotBlank() && title.isNotBlank() -> "$title\n$content"
                    content.isNotBlank() -> content
                    title.isNotBlank() -> title
                    else -> ""
                }
                if (textToAnalyze.isNotBlank()) {
                    isSmartTagging = true
                    viewModel.smartTagNote(textToAnalyze) { suggestedTitle, suggestedCat, mentionedPeople, suggestedTags ->
                        isSmartTagging = false
                        if (title.isBlank() || title == "Untitled Note" || title == "New Note") {
                            title = suggestedTitle
                        }
                        selectedCategory = suggestedCat
                        for (tag in suggestedTags) {
                            val clean = tag.trim().removePrefix("#").lowercase()
                            if (clean.isNotBlank() && !tagsList.contains(clean)) {
                                tagsList.add(clean)
                            }
                        }
                        allPeople.filter { p ->
                            p.name.isNotBlank() && mentionedPeople.any {
                                it.contains(p.name, ignoreCase = true) || p.name.contains(it, ignoreCase = true)
                            }
                        }.forEach { p ->
                            if (!attachedPeopleIds.contains(p.id)) {
                                attachedPeopleIds.add(p.id)
                            }
                        }
                        val tagSummary = if (suggestedTags.isNotEmpty()) {
                            "#" + suggestedTags.take(3).joinToString(" #")
                        } else suggestedCat
                        editorFeedbackMessage = "Smart tags applied: $tagSummary"
                    }
                } else {
                    editorFeedbackMessage = "Please write a title or content first"
                }
            }

            val triggerAiCleanUp: () -> Unit = {
                val textToClean = if (content.isNotBlank()) content else title
                if (textToClean.isNotBlank()) {
                    isAiCleaningUp = true
                    viewModel.aiCleanUpNote(textToClean) { polished ->
                        isAiCleaningUp = false
                        if (content.isNotBlank()) {
                            content = polished
                        } else {
                            title = polished
                        }
                        editorFeedbackMessage = "Note cleaned & polished ✨"
                    }
                } else {
                    editorFeedbackMessage = "Please write some text to clean up"
                }
            }

            // 2. ONE LARGE UNIFIED TEXT BOX UNDER THE SMALL HEADING
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.94f),
                border = BorderStroke(1.2.dp, RecallCyanSecondary.copy(alpha = 0.45f)),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // INTEGRATED TOP TASKBAR INSIDE THE UNIFIED TEXT BOX (All options in drop-down menu style)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E293B).copy(alpha = 0.85f))
                            .horizontalScroll(taskbarScroll)
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // (A) Category Drop-Down Menu
                        Box {
                            TaskbarDropdownButton(
                                label = selectedCategory,
                                dotColor = activeCatColor,
                                accentColor = activeCatColor,
                                onClick = { showCategoryDropdown = true },
                                modifier = Modifier.testTag("editor_category_dropdown")
                            )
                            DropdownMenu(
                                expanded = showCategoryDropdown,
                                onDismissRequest = { showCategoryDropdown = false },
                                properties = PopupProperties(clippingEnabled = true),
                                modifier = Modifier
                                    .background(Color(0xFF0F172A))
                                    .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                                    .height(320.dp)
                            ) {
                                categories.forEach { catEnum ->
                                    val cat = catEnum.displayName
                                    val isSelected = selectedCategory.equals(cat, ignoreCase = true)
                                    val catColor = getVibrantTagColor(cat)
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(catColor)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = cat,
                                                    color = if (isSelected) catColor else Color.White,
                                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                                                )
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = catEnum.icon,
                                                contentDescription = null,
                                                tint = catColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        trailingIcon = {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = catColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedCategory = cat
                                            showCategoryDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        // (B) Tags (#) Drop-Down Menu
                        Box {
                            TaskbarDropdownButton(
                                label = if (tagsList.isEmpty()) "#Tags" else "#Tags (${tagsList.size})",
                                icon = Icons.Default.Tag,
                                accentColor = RecallVioletPrimary,
                                onClick = { showTagSuggestionsDropdown = true },
                                modifier = Modifier.testTag("editor_tags_dropdown")
                            )
                            DropdownMenu(
                                expanded = showTagSuggestionsDropdown,
                                onDismissRequest = { showTagSuggestionsDropdown = false },
                                properties = PopupProperties(clippingEnabled = true),
                                modifier = Modifier
                                    .background(Color(0xFF0F172A))
                                    .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                                    .height(340.dp)
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "+ Add Custom #Tag...",
                                            color = RecallVioletPrimary,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = RecallVioletPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        showTagSuggestionsDropdown = false
                                        scope.launch {
                                            delay(60)
                                            showCustomTagDialog = true
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "✨ Auto-Extract Smart Tags",
                                            color = Color(0xFF0284C7),
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = Color(0xFF0284C7),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        showTagSuggestionsDropdown = false
                                        triggerSmartTag()
                                    }
                                )
                                if (tagsList.isNotEmpty()) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Clear All Tags (${tagsList.size})",
                                                color = Color(0xFFDC2626),
                                                fontWeight = FontWeight.Bold
                                            )
                                        },
                                        onClick = {
                                            tagsList.clear()
                                            showTagSuggestionsDropdown = false
                                        }
                                    )
                                }
                                popularTagSuggestions.forEach { preset ->
                                    val isAdded = tagsList.contains(preset)
                                    val tagColor = getVibrantTagColor(preset)
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(tagColor)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "#$preset",
                                                    color = tagColor,
                                                    fontWeight = if (isAdded) FontWeight.ExtraBold else FontWeight.SemiBold
                                                )
                                            }
                                        },
                                        trailingIcon = {
                                            if (isAdded) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Added",
                                                    tint = tagColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        },
                                        onClick = {
                                            if (isAdded) {
                                                tagsList.remove(preset)
                                            } else {
                                                tagsList.add(preset)
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // (C) People (@) Drop-Down Menu
                        Box {
                            TaskbarDropdownButton(
                                label = if (attachedPeopleIds.isEmpty()) "@People" else "@People (${attachedPeopleIds.size})",
                                icon = Icons.Default.Person,
                                accentColor = Color(0xFF0284C7),
                                onClick = { showPeopleDropdown = true },
                                modifier = Modifier.testTag("editor_people_dropdown")
                            )
                            DropdownMenu(
                                expanded = showPeopleDropdown,
                                onDismissRequest = { showPeopleDropdown = false },
                                properties = PopupProperties(clippingEnabled = true),
                                modifier = Modifier
                                    .background(Color(0xFF0F172A))
                                    .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                                    .height(300.dp)
                            ) {
                                if (allPeople.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No people in CRM yet", color = Color(0xFF94A3B8)) },
                                        onClick = { showPeopleDropdown = false }
                                    )
                                } else {
                                    if (attachedPeopleIds.isNotEmpty()) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    "Clear Linked People (${attachedPeopleIds.size})",
                                                    color = Color(0xFFDC2626),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            },
                                            onClick = {
                                                attachedPeopleIds.clear()
                                                showPeopleDropdown = false
                                            }
                                        )
                                    }
                                    allPeople.forEach { person ->
                                        val isLinked = attachedPeopleIds.contains(person.id)
                                        val personColor = getVibrantTagColor(person.relationship.orEmpty().ifBlank { person.name })
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(
                                                        text = "@${person.name}",
                                                        color = if (isLinked) personColor else Color.White,
                                                        fontWeight = if (isLinked) FontWeight.ExtraBold else FontWeight.SemiBold
                                                    )
                                                    val rel = person.relationship.orEmpty()
                                                    if (rel.isNotBlank()) {
                                                        Text(
                                                            text = rel,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color(0xFF94A3B8)
                                                        )
                                                    }
                                                }
                                            },
                                            trailingIcon = {
                                                if (isLinked) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Linked",
                                                        tint = personColor,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            },
                                            onClick = {
                                                if (isLinked) {
                                                    attachedPeopleIds.remove(person.id)
                                                } else {
                                                    attachedPeopleIds.add(person.id)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // (D) AI Tools Drop-Down Menu
                        Box {
                            TaskbarDropdownButton(
                                label = "AI Tools",
                                icon = Icons.Default.AutoAwesome,
                                accentColor = Color(0xFF7C3AED),
                                onClick = { showAiToolsDropdown = true },
                                modifier = Modifier.testTag("editor_ai_dropdown")
                            )
                            DropdownMenu(
                                expanded = showAiToolsDropdown,
                                onDismissRequest = { showAiToolsDropdown = false },
                                properties = PopupProperties(clippingEnabled = true),
                                modifier = Modifier
                                    .background(Color(0xFF0F172A))
                                    .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("✨ AI Clean Up & Polish", color = Color.White, fontWeight = FontWeight.Bold) },
                                    leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = RecallCyanSecondary) },
                                    onClick = {
                                        showAiToolsDropdown = false
                                        triggerAiCleanUp()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🏷️ AI Smart Tag & Categorize", color = Color.White, fontWeight = FontWeight.Bold) },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null, tint = RecallVioletPrimary) },
                                    onClick = {
                                        showAiToolsDropdown = false
                                        triggerSmartTag()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("⚡ Extract Action Items", color = Color.White, fontWeight = FontWeight.Bold) },
                                    leadingIcon = { Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981)) },
                                    onClick = {
                                        showAiToolsDropdown = false
                                        val source = "$title\n$content\n$rawTranscription"
                                        viewModel.extractActionItemsFromNoteText(source) { extracted ->
                                            extracted.forEach { task ->
                                                if (attachedActions.none { it.task.equals(task, ignoreCase = true) }) {
                                                    attachedActions.add(
                                                        ActionItemEntity(
                                                            noteId = currentNoteId,
                                                            task = task
                                                        )
                                                    )
                                                }
                                            }
                                            editorFeedbackMessage = "Extracted ${extracted.size} action item(s) ✅"
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("📤 Export PDF / Markdown", color = Color.White, fontWeight = FontWeight.Bold) },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = RecallCyanSecondary) },
                                    onClick = {
                                        showAiToolsDropdown = false
                                        showExportDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("📋 Paste from Clipboard", color = Color.White, fontWeight = FontWeight.Medium) },
                                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF0284C7)) },
                                    onClick = {
                                        showAiToolsDropdown = false
                                        val clip = clipboardManager.getText()?.text
                                        if (!clip.isNullOrBlank()) {
                                            content = if (content.isBlank()) clip else "$content\n$clip"
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🧹 Clear Note Text", color = Color(0xFFDC2626), fontWeight = FontWeight.Medium) },
                                    leadingIcon = { Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFDC2626)) },
                                    onClick = {
                                        showAiToolsDropdown = false
                                        content = ""
                                    }
                                )
                            }
                        }

                        // (E) Insert / Voice / Media Drop-Down Menu
                        Box {
                            TaskbarDropdownButton(
                                label = if (isRecordingInEditor) "🎙️ Recording..." else "Insert",
                                icon = if (isRecordingInEditor) Icons.Default.Stop else Icons.Default.CameraAlt,
                                accentColor = if (isRecordingInEditor) Color(0xFFEF4444) else Color(0xFF0D9488),
                                onClick = { showMediaInsertDropdown = true },
                                modifier = Modifier.testTag("editor_media_dropdown")
                            )
                            DropdownMenu(
                                expanded = showMediaInsertDropdown,
                                onDismissRequest = { showMediaInsertDropdown = false },
                                properties = PopupProperties(clippingEnabled = true),
                                modifier = Modifier
                                    .background(Color(0xFF0F172A))
                                    .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (isRecordingInEditor) "⏹️ Stop Voice Dictation" else "🎙️ Start Voice Dictation",
                                            color = if (isRecordingInEditor) Color(0xFFEF4444) else Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (isRecordingInEditor) Icons.Default.Stop else Icons.Default.Mic,
                                            contentDescription = null,
                                            tint = if (isRecordingInEditor) Color(0xFFEF4444) else RecallVioletPrimary
                                        )
                                    },
                                    onClick = {
                                        showMediaInsertDropdown = false
                                        toggleDictationSafely()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("📷 Take Photo (Camera)", color = Color.White, fontWeight = FontWeight.Medium) },
                                    leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null, tint = RecallCyanSecondary) },
                                    onClick = {
                                        showMediaInsertDropdown = false
                                        scope.launch {
                                            delay(60)
                                            launchCameraSafely()
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🖼️ Choose Photo (Gallery)", color = Color.White, fontWeight = FontWeight.Medium) },
                                    leadingIcon = { Icon(Icons.Default.Collections, contentDescription = null, tint = RecallVioletPrimary) },
                                    onClick = {
                                        showMediaInsertDropdown = false
                                        scope.launch {
                                            delay(60)
                                            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                        }
                                    }
                                )
                            }
                        }

                        // (F) Ink Color Drop-Down Menu
                        Box {
                            val (activeColorName, activeInkColor) = noteTextColors[selectedTextColorIndex]
                            TaskbarDropdownButton(
                                label = activeColorName,
                                dotColor = activeInkColor,
                                accentColor = activeInkColor,
                                onClick = { showInkColorDropdown = true },
                                modifier = Modifier.testTag("editor_color_dropdown")
                            )
                            DropdownMenu(
                                expanded = showInkColorDropdown,
                                onDismissRequest = { showInkColorDropdown = false },
                                properties = PopupProperties(clippingEnabled = true),
                                modifier = Modifier
                                    .background(Color(0xFF0F172A))
                                    .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                            ) {
                                noteTextColors.forEachIndexed { index, (name, color) ->
                                    val isSelected = selectedTextColorIndex == index
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .clip(CircleShape)
                                                        .background(color)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = name,
                                                    color = color,
                                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                                                )
                                            }
                                        },
                                        trailingIcon = {
                                            if (isSelected) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                                            }
                                        },
                                        onClick = {
                                            selectedTextColorIndex = index
                                            showInkColorDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        // (G) Physical Tone Reminder Drop-Down Menu
                        Box {
                            val reminder = activeReminder
                            val reminderLabel = if (reminder != null) {
                                "🔔 " + reminderTimeFormat.format(reminder.reminderTimeMillis)
                            } else {
                                "Reminder"
                            }
                            val reminderAccent = if (reminder != null) Color(0xFF10B981) else Color(0xFFF59E0B)

                            TaskbarDropdownButton(
                                label = reminderLabel,
                                icon = if (reminder != null) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                accentColor = reminderAccent,
                                dotColor = if (reminder != null) Color(0xFF10B981) else null,
                                onClick = {
                                    if (reminder == null) {
                                        showReminderSchedulerDialog = true
                                    } else {
                                        showReminderDropdown = true
                                    }
                                },
                                modifier = Modifier.testTag("editor_reminder_dropdown")
                            )

                            if (reminder != null) {
                                DropdownMenu(
                                    expanded = showReminderDropdown,
                                    onDismissRequest = { showReminderDropdown = false },
                                    properties = PopupProperties(clippingEnabled = true),
                                    modifier = Modifier
                                        .background(Color(0xFF0F172A))
                                        .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text("🔔 Scheduled Alarm", fontWeight = FontWeight.Bold, color = Color.White)
                                                Text(
                                                    reminderTimeFormat.format(reminder.reminderTimeMillis),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF10B981),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        },
                                        onClick = {
                                            showReminderDropdown = false
                                            scope.launch {
                                                delay(60)
                                                showReminderSchedulerDialog = true
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "🎵 Test Tone: ${reminder.getReminderTone().displayName}",
                                                fontWeight = FontWeight.SemiBold,
                                                color = RecallVioletPrimary
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.NotificationsActive,
                                                contentDescription = null,
                                                tint = RecallVioletPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            viewModel.previewReminderTone(reminder.getReminderTone(), reminder.getVibrationPattern())
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("⏰ Snooze +10 Minutes", color = Color(0xFF0284C7), fontWeight = FontWeight.SemiBold) },
                                        onClick = {
                                            showReminderDropdown = false
                                            viewModel.snoozeReminder(reminder.id, 10)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("✏️ Edit Reminder...", color = Color.White, fontWeight = FontWeight.Medium) },
                                        onClick = {
                                            showReminderDropdown = false
                                            scope.launch {
                                                delay(60)
                                                showReminderSchedulerDialog = true
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("🗑️ Remove Reminder", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            showReminderDropdown = false
                                            viewModel.deleteReminder(reminder.id)
                                        }
                                    )
                                }
                            }
                        }

                        // (H) Location / Geotag Drop-Down Menu
                        Box {
                            TaskbarDropdownButton(
                                label = if (!locationName.isNullOrBlank()) "📍 $locationName" else "📍 Place",
                                accentColor = Color(0xFF10B981),
                                dotColor = if (!locationName.isNullOrBlank()) Color(0xFF10B981) else null,
                                onClick = { showLocationDropdown = true },
                                modifier = Modifier.testTag("editor_location_dropdown")
                            )
                            DropdownMenu(
                                expanded = showLocationDropdown,
                                onDismissRequest = { showLocationDropdown = false },
                                properties = PopupProperties(clippingEnabled = true),
                                modifier = Modifier
                                    .background(Color(0xFF0F172A))
                                    .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("🛰️ Detect GPS Location", color = RecallCyanSecondary, fontWeight = FontWeight.ExtraBold) },
                                    onClick = {
                                        showLocationDropdown = false
                                        val hasLocPerm = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (hasLocPerm) {
                                            val detected = com.example.util.MemoryIntelligenceHelper.detectDevicePlaceName(context)
                                            locationName = detected ?: "Current GPS Location"
                                            editorFeedbackMessage = "Geotagged: 📍 $locationName"
                                        } else {
                                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                        }
                                    }
                                )
                                val savedPlaceNames = allPlaces.map { it.name }
                                val placeOptions = (savedPlaceNames + listOf("Office", "Coffee Shop", "Home", "Client Site", "Conference", "Transit / Airport")).distinct().take(10)
                                placeOptions.forEach { place ->
                                    DropdownMenuItem(
                                        text = { Text("📍 $place", color = Color.White, fontWeight = FontWeight.SemiBold) },
                                        onClick = {
                                            locationName = place
                                            showLocationDropdown = false
                                            editorFeedbackMessage = "Geotagged: 📍 $place"
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("✏️ Custom Place...", color = RecallVioletPrimary, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        showLocationDropdown = false
                                        customLocationInput = locationName.orEmpty()
                                        showCustomLocationDialog = true
                                    }
                                )
                                if (!locationName.isNullOrBlank()) {
                                    DropdownMenuItem(
                                        text = { Text("🧹 Clear Location", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            locationName = null
                                            showLocationDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        // (I) Project Selector Drop-Down Menu
                        val activeProject = allProjects.firstOrNull { it.id == selectedProjectId }
                        Box {
                            TaskbarDropdownButton(
                                label = if (activeProject != null) "🚀 ${activeProject.name}" else "🚀 Project",
                                accentColor = Color(0xFF60A5FA),
                                dotColor = if (activeProject != null) Color(0xFF60A5FA) else null,
                                onClick = { showProjectDropdown = true },
                                modifier = Modifier.testTag("editor_project_dropdown")
                            )
                            DropdownMenu(
                                expanded = showProjectDropdown,
                                onDismissRequest = { showProjectDropdown = false },
                                properties = PopupProperties(clippingEnabled = true),
                                modifier = Modifier
                                    .background(Color(0xFF0F172A))
                                    .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                            ) {
                                allProjects.forEach { proj ->
                                    DropdownMenuItem(
                                        text = { Text("🚀 ${proj.name}", color = Color.White, fontWeight = FontWeight.SemiBold) },
                                        onClick = {
                                            selectedProjectId = proj.id
                                            showProjectDropdown = false
                                            editorFeedbackMessage = "Linked to project: 🚀 ${proj.name}"
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("+ New Project...", color = RecallCyanSecondary, fontWeight = FontWeight.ExtraBold) },
                                    onClick = {
                                        showProjectDropdown = false
                                        showNewProjectInEditorDialog = true
                                    }
                                )
                                if (selectedProjectId != null) {
                                    DropdownMenuItem(
                                        text = { Text("🧹 Unlink Project", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            selectedProjectId = null
                                            showProjectDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Hairline divider under the integrated taskbar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFF334155))
                    )

                    // Scrollable unified writing area inside the single large text box
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 8.dp)
                    ) {
                        // Inline Title Input at top of the unified text box
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = {
                                Text(
                                    "Note Title (or auto-generate with Smart Tag)...",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("editor_title_input"),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                fontSize = 19.sp
                            ),
                            colors = seamlessInnerTextFieldColors(textColor = Color.White)
                        )

                        // Subtle divider between inline title line and main note body
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .height(1.dp)
                                .background(Color(0xFF334155))
                        )

                        // Compact inline strip for active 📍Location, 🚀Project, @People, and #Tags inside the box (only shown when non-empty)
                        val linkedPeople = allPeople.filter { attachedPeopleIds.contains(it.id) }
                        val linkedProject = allProjects.firstOrNull { it.id == selectedProjectId }
                        if (!locationName.isNullOrBlank() || linkedProject != null || linkedPeople.isNotEmpty() || tagsList.isNotEmpty()) {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                locationName?.takeIf { it.isNotBlank() }?.let { loc ->
                                    item {
                                        VibrantTagBadge(
                                            text = "📍 $loc",
                                            color = Color(0xFF10B981),
                                            onRemove = { locationName = null }
                                        )
                                    }
                                }
                                linkedProject?.let { proj ->
                                    item {
                                        VibrantTagBadge(
                                            text = "🚀 ${proj.name}",
                                            color = Color(0xFF60A5FA),
                                            onRemove = { selectedProjectId = null }
                                        )
                                    }
                                }
                                items(linkedPeople) { person ->
                                    VibrantTagBadge(
                                        text = "@${person.name}",
                                        color = getVibrantTagColor(person.relationship.orEmpty().ifBlank { person.name }),
                                        onRemove = { attachedPeopleIds.remove(person.id) }
                                    )
                                }
                                items(tagsList) { tag ->
                                    VibrantTagBadge(
                                        text = "#$tag",
                                        color = getVibrantTagColor(tag),
                                        onRemove = { tagsList.remove(tag) }
                                    )
                                }
                            }
                        }

                        // Audio & Visual Media Attachments inside the box (only shown when non-empty)
                        val audioAttachments = attachedMedia.filter { it.mediaType == MediaType.AUDIO && it.localUri.isNotBlank() }
                        if (audioAttachments.isNotEmpty()) {
                            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                audioAttachments.forEach { audioMedia ->
                                    AudioPlayerCard(
                                        audioPath = audioMedia.localUri,
                                        title = audioMedia.aiDescription ?: "Recorded Voice Memory",
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                }
                            }
                        }

                        if (attachedMedia.isNotEmpty()) {
                            LazyRow(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(attachedMedia) { media ->
                                    val isImage = media.mediaType == MediaType.IMAGE
                                    val mediaFile = remember(media.localUri) { File(media.localUri) }
                                    val fileExists = remember(mediaFile) { mediaFile.exists() }

                                    Card(
                                        modifier = Modifier
                                            .width(if (isImage && fileExists) 220.dp else 160.dp)
                                            .height(if (isImage && fileExists) 96.dp else 74.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                        border = BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.5f))
                                    ) {
                                        if (isImage && fileExists) {
                                            Row(modifier = Modifier.fillMaxSize()) {
                                                AsyncImage(
                                                    model = mediaFile,
                                                    contentDescription = "Memory Photo",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(96.dp)
                                                        .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                                                )
                                                Column(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .padding(6.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = "Photo",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = RecallCyanSecondary,
                                                            fontSize = 11.sp
                                                        )
                                                        Spacer(modifier = Modifier.weight(1f))
                                                        IconButton(
                                                            onClick = { attachedMedia.remove(media) },
                                                            modifier = Modifier.size(18.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Close,
                                                                contentDescription = "Remove",
                                                                tint = Color(0xFFCBD5E1),
                                                                modifier = Modifier.size(12.dp)
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        media.aiDescription ?: "Captured Photo",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color(0xFFF1F5F9),
                                                        fontSize = 10.sp,
                                                        maxLines = 3,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        } else {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.Photo,
                                                        contentDescription = null,
                                                        tint = RecallCyanSecondary,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        media.mediaType.name,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        fontSize = 10.sp
                                                    )
                                                    Spacer(modifier = Modifier.weight(1f))
                                                    IconButton(
                                                        onClick = { attachedMedia.remove(media) },
                                                        modifier = Modifier.size(16.dp)
                                                    ) {
                                                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color(0xFFCBD5E1), modifier = Modifier.size(12.dp))
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    media.aiDescription ?: "Visual Attachment",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFFF1F5F9),
                                                    fontSize = 11.sp,
                                                    maxLines = 2
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (rawTranscription.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF1E293B).copy(alpha = 0.90f),
                                border = BorderStroke(1.dp, RecallCyanSecondary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = null,
                                        tint = RecallVioletPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Transcript: $rawTranscription",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFE2E8F0),
                                        maxLines = 2
                                    )
                                }
                            }
                        }

                        // Expansive Note Body Input inside the unified text box
                        OutlinedTextField(
                            value = content,
                            onValueChange = { content = it },
                            placeholder = {
                                Text(
                                    "Write your memory here, or use the top taskbar dropdowns for #Tags, @People, Voice Dictation, Camera & AI tools...",
                                    color = Color(0xFF64748B),
                                    fontSize = 16.sp,
                                    lineHeight = 24.sp
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 360.dp)
                                .testTag("editor_body_input"),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = currentNoteTextColor,
                                fontSize = 17.sp,
                                lineHeight = 26.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            colors = seamlessInnerTextFieldColors(textColor = currentNoteTextColor)
                        )

                        // ✅ Action Items & Extracted Checklists Section
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .background(Color(0xFF0F172A).copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                                .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "✅ Action Items (${attachedActions.count { !it.isCompleted }} open)",
                                    color = RecallCyanSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF34D399).copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, Color(0xFF34D399).copy(alpha = 0.4f)),
                                    modifier = Modifier.clickable {
                                        val extracted = MemoryIntelligenceHelper.extractActionItemsFromText(content)
                                        var added = 0
                                        extracted.forEach { taskText ->
                                            if (attachedActions.none { it.task.equals(taskText, ignoreCase = true) }) {
                                                attachedActions.add(
                                                    ActionItemEntity(
                                                        noteId = currentNoteId,
                                                        task = taskText,
                                                        isCompleted = false
                                                    )
                                                )
                                                added++
                                            }
                                        }
                                        editorFeedbackMessage = if (added > 0) "⚡ Extracted $added action item(s)!" else "No new action items detected"
                                    }
                                ) {
                                    Text(
                                        text = "⚡ Auto-Extract",
                                        color = Color(0xFF34D399),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (attachedActions.isNotEmpty()) {
                                attachedActions.forEachIndexed { idx, act ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (act.isCompleted) Color(0xFF34D399) else Color.Transparent,
                                            border = BorderStroke(1.5.dp, if (act.isCompleted) Color(0xFF34D399) else RecallCyanSecondary),
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clickable {
                                                    val updated = act.copy(isCompleted = !act.isCompleted)
                                                    attachedActions[idx] = updated
                                                    viewModel.toggleActionItem(act.id, updated.isCompleted)
                                                }
                                        ) {
                                            if (act.isCompleted) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = "Completed",
                                                    tint = Color(0xFF0F172A),
                                                    modifier = Modifier.padding(2.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = act.task,
                                            color = if (act.isCompleted) Color(0xFF64748B) else Color.White,
                                            fontSize = 13.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFFBBF24).copy(alpha = 0.16f),
                                            border = BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.4f)),
                                            modifier = Modifier.clickable {
                                                actionItemForReminder = act
                                            }
                                        ) {
                                            Text(
                                                text = "🔔 Remind",
                                                color = Color(0xFFFBBF24),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove task",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable {
                                                    attachedActions.removeAt(idx)
                                                }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            // Quick Add Action Item Input
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newActionTaskInput,
                                    onValueChange = { newActionTaskInput = it },
                                    placeholder = { Text("Add follow-up task or action item...", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                                    colors = translucentWhiteTextFieldColors(),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = {
                                        val clean = newActionTaskInput.trim()
                                        if (clean.isNotEmpty()) {
                                            attachedActions.add(ActionItemEntity(noteId = currentNoteId, task = clean))
                                            newActionTaskInput = ""
                                        }
                                    })
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = {
                                        val clean = newActionTaskInput.trim()
                                        if (clean.isNotEmpty()) {
                                            attachedActions.add(ActionItemEntity(noteId = currentNoteId, task = clean))
                                            newActionTaskInput = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = RecallVioletPrimary),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.heightIn(min = 40.dp)
                                ) {
                                    Text("+ Add", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                                }
                            }
                        }

                        // 🕸️ Semantic Linked Memories Drawer
                        val relatedMatches = remember(currentNoteId, title, content, selectedCategory, tagsList.size, attachedPeopleIds.size, locationName, allNotes.size) {
                            MemoryIntelligenceHelper.findRelatedMemories(
                                currentNoteId = currentNoteId,
                                title = title,
                                content = content,
                                category = selectedCategory,
                                tags = tagsList.toList(),
                                linkedPersonIds = attachedPeopleIds.toSet(),
                                locationName = locationName,
                                allNotes = allNotes
                            )
                        }

                        if (relatedMatches.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .background(Color(0xFF0F172A).copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                                    .border(BorderStroke(1.dp, RecallVioletPrimary.copy(alpha = 0.45f)), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "🕸️ Linked & Related Memories (${relatedMatches.size})",
                                    color = Color(0xFFA78BFA),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                relatedMatches.forEach { match ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF1E293B),
                                        border = BorderStroke(1.dp, Color(0xFF334155)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                            .clickable {
                                                viewModel.openNoteEditor(match.noteWithDetails)
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = match.noteWithDetails.note.title.ifBlank { "Untitled Memory" },
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = match.matchReason,
                                                    color = RecallCyanSecondary,
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = RecallVioletPrimary.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = "${match.similarityPercent}%",
                                                    color = Color(0xFFA78BFA),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 🛡️ Memory Provenance & Fact Verification Card (Phase 2)
                        val currentSourceType = noteWithDetails?.note?.getMemorySourceType()
                            ?: if (rawTranscription.isNotBlank()) com.example.data.model.MemorySourceType.AUDIO_TRANSCRIPT
                            else com.example.data.model.MemorySourceType.USER_EXPLICIT
                        val currentVerification = noteWithDetails?.note?.getMemoryVerificationStatus()
                            ?: com.example.data.model.MemoryVerificationStatus.VERIFIED_FACT
                        val currentConfidence = ((noteWithDetails?.note?.confidenceScore ?: 1.0f).coerceIn(0.1f, 1.0f) * 100).toInt()
                        val currentUnifiedType = noteWithDetails?.note?.getUnifiedMemoryType()
                            ?: com.example.data.model.UnifiedMemoryType.inferFromLegacyCategory(
                                category = selectedCategory,
                                hasLocation = !locationName.isNullOrBlank(),
                                hasAudio = rawTranscription.isNotBlank(),
                                hasImage = attachedMedia.isNotEmpty(),
                                contentHint = "$title $content"
                            )
                        val isAiInferredPending = currentVerification == com.example.data.model.MemoryVerificationStatus.AI_INFERRED_PENDING

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .background(Color(0xFF0F172A).copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (isAiInferredPending) Color(0xFFF59E0B).copy(alpha = 0.7f) else Color(0xFF10B981).copy(alpha = 0.45f)
                                    ),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "🛡️ Memory Provenance & Verification",
                                    color = if (isAiInferredPending) Color(0xFFFBBF24) else Color(0xFF34D399),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = RecallCyanSecondary.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = "${currentUnifiedType.iconEmoji} ${currentUnifiedType.displayName}",
                                        color = RecallCyanSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Source: ${currentSourceType.badgeEmoji} ${currentSourceType.label} • Confidence: $currentConfidence%",
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.sp
                            )
                            if (noteWithDetails != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isAiInferredPending) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF10B981).copy(alpha = 0.22f),
                                            border = BorderStroke(1.dp, Color(0xFF34D399)),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    viewModel.verifyMemoryFact(noteWithDetails.note)
                                                    editorFeedbackMessage = "✓ Confirmed as Verified Fact"
                                                }
                                        ) {
                                            Text(
                                                text = "✓ Confirm as Verified Fact",
                                                color = Color(0xFF34D399),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFF59E0B).copy(alpha = 0.16f),
                                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.55f)),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    viewModel.markMemoryAsAiInferred(noteWithDetails.note)
                                                    editorFeedbackMessage = "🤖 Flagged as AI Interpretation"
                                                }
                                        ) {
                                            Text(
                                                text = "🤖 Flag as Unverified AI Interpretation",
                                                color = Color(0xFFFBBF24),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 📜 Non-Destructive Revision History Viewer (Phase 2)
                        val noteRevisions by viewModel.repository.getRevisionsForNote(currentNoteId)
                            .collectAsStateWithLifecycle(initialValue = noteWithDetails?.revisions ?: emptyList())

                        if (noteRevisions.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .background(Color(0xFF0F172A).copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                                    .border(BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.45f)), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "📜 Revision & Reconciliation History (${noteRevisions.size})",
                                    color = RecallCyanSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                noteRevisions.take(5).forEach { rev ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF1E293B),
                                        border = BorderStroke(1.dp, Color(0xFF334155)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${rev.previousTitle} • ${com.example.util.DateUtils.formatRelativeTime(rev.createdAt)}",
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (rev.changeSummary.isNotBlank()) {
                                                    Text(
                                                        text = rev.changeSummary,
                                                        color = Color(0xFF6EE7B7),
                                                        fontSize = 11.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Text(
                                                    text = rev.previousContent,
                                                    color = Color(0xFF94A3B8),
                                                    fontSize = 11.sp,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            if (noteWithDetails != null) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = RecallVioletPrimary.copy(alpha = 0.25f),
                                                    border = BorderStroke(1.dp, RecallVioletPrimary.copy(alpha = 0.6f)),
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .clickable {
                                                            viewModel.restoreNoteRevision(noteWithDetails.note, rev) { restoredTitle, restoredContent ->
                                                                title = restoredTitle
                                                                content = restoredContent
                                                                editorFeedbackMessage = "↺ Restored snapshot!"
                                                            }
                                                        }
                                                ) {
                                                    Text(
                                                        text = "↺ Restore",
                                                        color = Color.White,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // (Phase 4) Connected Memories (Graph Links) Section
                        val noteLinks by viewModel.getLinksForNote(currentNoteId).collectAsStateWithLifecycle(initialValue = emptyList())
                        var showLinkMemoryDropdown by remember { mutableStateOf(false) }
                        val connectedNotes = remember(noteLinks, allNotes) {
                            noteLinks.mapNotNull { link ->
                                val otherId = if (link.sourceNoteId == currentNoteId) link.targetNoteId else link.sourceNoteId
                                val otherNote = allNotes.firstOrNull { it.note.id == otherId }
                                if (otherNote != null) Pair(link, otherNote) else null
                            }
                        }
                        val linkableNotes = remember(allNotes, connectedNotes, currentNoteId) {
                            val alreadyLinkedIds = connectedNotes.map { it.second.note.id }.toSet() + currentNoteId
                            allNotes.filter { it.note.id !in alreadyLinkedIds }.take(8)
                        }

                        if (connectedNotes.isNotEmpty() || linkableNotes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                                    .background(Color(0xFF0F172A).copy(alpha = 0.72f), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFF60A5FA).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "🕸️ Connected Memories (${connectedNotes.size})",
                                        color = Color(0xFF93C5FD),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (linkableNotes.isNotEmpty()) {
                                        Box {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFF60A5FA).copy(alpha = 0.2f),
                                                border = BorderStroke(1.dp, Color(0xFF60A5FA).copy(alpha = 0.5f)),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .clickable { showLinkMemoryDropdown = true }
                                            ) {
                                                Text(
                                                    text = "+ Link Memory",
                                                    color = Color(0xFF93C5FD),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = showLinkMemoryDropdown,
                                                onDismissRequest = { showLinkMemoryDropdown = false },
                                                modifier = Modifier
                                                    .background(Color(0xFF0F172A))
                                                    .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                                            ) {
                                                linkableNotes.forEach { candidate ->
                                                    DropdownMenuItem(
                                                        text = {
                                                            Text(
                                                                text = "🔗 ${candidate.note.title.take(32)}",
                                                                color = Color.White,
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                        },
                                                        onClick = {
                                                            showLinkMemoryDropdown = false
                                                            viewModel.linkMemories(currentNoteId, candidate.note.id, "RELATED_TO")
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                if (connectedNotes.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    connectedNotes.forEach { (link, otherNote) ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF1E293B),
                                            border = BorderStroke(1.dp, Color(0xFF334155)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp)
                                                .clickable { viewModel.openNoteEditor(otherNote) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = otherNote.note.title,
                                                        color = Color.White,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "${link.relationType} • ${otherNote.note.category}",
                                                        color = Color(0xFF93C5FD),
                                                        fontSize = 11.sp
                                                    )
                                                }
                                                Text(
                                                    text = "Open →",
                                                    color = RecallCyanSecondary,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // (Phase 9 & Section 3.3) Natural-Language Date/Person Reminder Detection + Cross-Entity Memory-to-Organise Bridge
                        val naturalSuggestion = remember(content, allPeople) {
                            com.example.util.RecallLifeOrganiserManager.parseNaturalDateTimeSuggestion(
                                text = content,
                                knownPeopleNames = allPeople.map { it.name }
                            )
                        }
                        var showConvertToListMenu by remember { mutableStateOf(false) }
                        var showConvertToCommitmentMenu by remember { mutableStateOf(false) }

                        if (naturalSuggestion != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF59E0B).copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.65f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                                    .testTag("editor_natural_reminder_banner")
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "🔔 Smart Reminder Detected",
                                            color = Color(0xFFFBBF24),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = "${naturalSuggestion.suggestedAction} • ${naturalSuggestion.formattedDateLabel}" +
                                                    (naturalSuggestion.mentionedPersonName?.let { " (with $it)" } ?: ""),
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFFBBF24),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                viewModel.scheduleReminder(
                                                    title = naturalSuggestion.suggestedAction,
                                                    details = content.take(160),
                                                    noteId = currentNoteId,
                                                    timeMillis = naturalSuggestion.suggestedTimeMillis,
                                                    recurrence = naturalSuggestion.recurrenceHint ?: "NONE"
                                                )
                                                editorFeedbackMessage = "🔔 Scheduled reminder for ${naturalSuggestion.formattedDateLabel}!"
                                            }
                                    ) {
                                        Text(
                                            text = "Schedule",
                                            color = Color(0xFF040711),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Memory-to-Organise & Cross-Entity Navigation Card
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                                .background(Color(0xFF0F172A).copy(alpha = 0.78f), RoundedCornerShape(12.dp))
                                .border(1.dp, RecallCyanSecondary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                                .testTag("editor_organise_bridge_card")
                        ) {
                            Text(
                                text = "⚡ Connect Memory to Organise & Entities",
                                color = RecallCyanSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Turn this memory into a Commitment or Dynamic List without retyping, or jump to linked entities.",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. Turn into Commitment / Promise
                                Box {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.6f)),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { showConvertToCommitmentMenu = true }
                                            .testTag("editor_convert_commitment_btn")
                                    ) {
                                        Text(
                                            text = "🤝 Save as Commitment",
                                            color = Color(0xFFFBBF24),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showConvertToCommitmentMenu,
                                        onDismissRequest = { showConvertToCommitmentMenu = false },
                                        modifier = Modifier.background(Color(0xFF0F172A))
                                    ) {
                                        listOf(
                                            "BY_ME" to "🤝 Owed by Me (Explicit Promise)",
                                            "TO_ME" to "🤝 Owed to Me (Waiting On)"
                                        ).forEach { (dirKey, dirLabel) ->
                                            DropdownMenuItem(
                                                text = { Text(dirLabel, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                                onClick = {
                                                    showConvertToCommitmentMenu = false
                                                    val desc = content.trim().ifBlank { title.trim() }.ifBlank { "Commitment from memory" }
                                                    viewModel.saveCommitment(
                                                        com.example.data.local.entity.CommitmentEntity(
                                                            id = UUID.randomUUID().toString(),
                                                            noteId = currentNoteId,
                                                            personId = attachedPeopleIds.firstOrNull(),
                                                            projectId = selectedProjectId,
                                                            description = desc.lines().first().take(140),
                                                            direction = dirKey,
                                                            dueDate = naturalSuggestion?.suggestedTimeMillis,
                                                            isCompleted = false
                                                        )
                                                    )
                                                    editorFeedbackMessage = "🤝 Saved to Commitments tracker!"
                                                }
                                            )
                                        }
                                    }
                                }

                                // 2. Extract to Dynamic List
                                Box {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, Color(0xFF34D399).copy(alpha = 0.6f)),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { showConvertToListMenu = true }
                                            .testTag("editor_convert_list_btn")
                                    ) {
                                        Text(
                                            text = "📋 Extract to Dynamic List",
                                            color = Color(0xFF34D399),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showConvertToListMenu,
                                        onDismissRequest = { showConvertToListMenu = false },
                                        modifier = Modifier.background(Color(0xFF0F172A))
                                    ) {
                                        com.example.util.DynamicListType.entries.forEach { listType ->
                                            DropdownMenuItem(
                                                text = { Text("${listType.icon} ${listType.displayName}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                                onClick = {
                                                    showConvertToListMenu = false
                                                    val sourceBody = content.ifBlank { rawTranscription }
                                                    val items = com.example.util.RecallLifeOrganiserManager.extractListItemsFromText(sourceBody)
                                                    viewModel.createDynamicList(
                                                        title = title.ifBlank { listType.displayName },
                                                        type = listType,
                                                        initialItems = if (items.isNotEmpty()) items else listOfNotNull(sourceBody.take(80).takeIf { it.isNotBlank() }),
                                                        sourceNoteId = currentNoteId
                                                    )
                                                    editorFeedbackMessage = "${listType.icon} Extracted ${items.size.coerceAtLeast(1)} item(s) to ${listType.displayName}!"
                                                }
                                            )
                                        }
                                    }
                                }

                                // 3. Jump to Linked Person Dossier
                                val linkedPeopleEntities = allPeople.filter { it.id in attachedPeopleIds }
                                linkedPeopleEntities.forEach { person ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = RecallVioletPrimary.copy(alpha = 0.25f),
                                        border = BorderStroke(1.dp, RecallVioletPrimary),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                viewModel.selectPerson(person)
                                                viewModel.closeNoteEditor()
                                                viewModel.navigateToHubSection(com.example.ui.navigation.Screen.Memories, "People")
                                            }
                                    ) {
                                        Text(
                                            text = "👤 Open ${person.name} →",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }

                                // 4. Jump to Linked Place
                                if (!locationName.isNullOrBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = RecallCyanSecondary.copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, RecallCyanSecondary),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                val matchedPlace = allPlaces.firstOrNull { it.name.equals(locationName, ignoreCase = true) }
                                                if (matchedPlace != null) {
                                                    viewModel.selectPlace(matchedPlace)
                                                }
                                                viewModel.closeNoteEditor()
                                                viewModel.navigateToHubSection(com.example.ui.navigation.Screen.Memories, "Places")
                                            }
                                    ) {
                                        Text(
                                            text = "📍 Open $locationName →",
                                            color = RecallCyanSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }

                                // 5. Jump to Linked Project
                                val linkedProject = allProjects.firstOrNull { it.id == selectedProjectId }
                                if (linkedProject != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF38BDF8).copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                viewModel.selectProject(linkedProject)
                                                viewModel.closeNoteEditor()
                                                viewModel.navigateToHubSection(com.example.ui.navigation.Screen.Organise, "Projects")
                                            }
                                    ) {
                                        Text(
                                            text = "🚀 Open ${linkedProject.name} →",
                                            color = Color(0xFF38BDF8),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Compact Bottom Quick-Action Footer inside the unified text box (Dark Slate)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFF334155))
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E293B).copy(alpha = 0.92f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(
                                onClick = { toggleDictationSafely() },
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(
                                        if (isRecordingInEditor) Color(0xFFEF4444) else RecallVioletPrimary.copy(alpha = 0.2f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = if (isRecordingInEditor) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = "Voice Dictate",
                                    tint = if (isRecordingInEditor) Color.White else RecallVioletPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            if (isRecordingInEditor) {
                                val mins = recordingDuration / 60
                                val secs = recordingDuration % 60
                                val stamp = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFBBF24).copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, Color(0xFFFBBF24)),
                                    modifier = Modifier.clickable {
                                        val bookmarkLine = "\n[📌 Bookmark $stamp] Key moment: "
                                        content = if (content.isBlank()) bookmarkLine.trimStart() else content + bookmarkLine
                                        editorFeedbackMessage = "📌 Pinned audio bookmark at $stamp"
                                    }
                                ) {
                                    Text(
                                        text = "📌 Pin $stamp",
                                        color = Color(0xFFFBBF24),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { launchCameraSafely() },
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(RecallCyanSecondary.copy(alpha = 0.16f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Capture Photo",
                                    tint = RecallCyanSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(RecallVioletPrimary.copy(alpha = 0.2f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Collections,
                                    contentDescription = "Pick Photo from Gallery",
                                    tint = RecallVioletPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = triggerSmartTag,
                                enabled = !isSmartTagging,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RecallVioletPrimary,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .heightIn(min = 34.dp)
                                    .testTag("editor_smart_tag_button")
                            ) {
                                if (isSmartTagging) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
                                } else {
                                    Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Smart Tag", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                            }

                            Button(
                                onClick = triggerAiCleanUp,
                                enabled = !isAiCleaningUp,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RecallCyanSecondary,
                                    contentColor = Color(0xFF0F172A)
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .heightIn(min = 34.dp)
                                    .testTag("editor_ai_clean_button")
                            ) {
                                if (isAiCleaningUp) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color(0xFF0F172A))
                                } else {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("AI Clean", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }

            // Custom Tag Dialog (with unified taskbar input box)
            if (showCustomTagDialog) {
                var showDialogPresetMenu by remember { mutableStateOf(false) }
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showCustomTagDialog = false },
                    containerColor = Color(0xFF0F172A),
                    title = { Text("Add Custom #Tag", fontWeight = FontWeight.Bold, color = Color.White) },
                    text = {
                        UnifiedTaskbarInputBox(
                            borderColor = RecallVioletPrimary,
                            taskbarContent = {
                                Box {
                                    TaskbarDropdownButton(
                                        label = "Quick Tag Ideas",
                                        icon = Icons.AutoMirrored.Filled.Label,
                                        accentColor = RecallVioletPrimary,
                                        onClick = { showDialogPresetMenu = true }
                                    )
                                    DropdownMenu(
                                        expanded = showDialogPresetMenu,
                                        onDismissRequest = { showDialogPresetMenu = false },
                                        properties = PopupProperties(clippingEnabled = true),
                                        modifier = Modifier
                                            .background(Color(0xFF0F172A))
                                            .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                                    ) {
                                        listOf("important", "deadline", "milestone", "recipe", "gift-idea", "book-notes").forEach { idea ->
                                            DropdownMenuItem(
                                                text = { Text("#$idea", color = getVibrantTagColor(idea), fontWeight = FontWeight.Bold) },
                                                onClick = {
                                                    customTagInput = idea
                                                    showDialogPresetMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                                TaskbarDropdownButton(
                                    label = "Clear",
                                    icon = Icons.Default.Close,
                                    accentColor = Color(0xFF94A3B8),
                                    onClick = { customTagInput = "" }
                                )
                            },
                            bodyContent = {
                                OutlinedTextField(
                                    value = customTagInput,
                                    onValueChange = { customTagInput = it },
                                    placeholder = { Text("Type custom tag name...", color = Color(0xFF94A3B8)) },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    colors = seamlessInnerTextFieldColors(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val clean = customTagInput.trim().removePrefix("#").lowercase()
                                if (clean.isNotBlank() && !tagsList.contains(clean)) {
                                    tagsList.add(clean)
                                }
                                customTagInput = ""
                                showCustomTagDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RecallVioletPrimary)
                        ) {
                            Text("Add #Tag", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(onClick = { showCustomTagDialog = false }) {
                            Text("Cancel", color = Color(0xFF94A3B8))
                        }
                    }
                )
            }

            // Custom Location / Geotag Dialog
            if (showCustomLocationDialog) {
                AlertDialog(
                    onDismissRequest = { showCustomLocationDialog = false },
                    containerColor = Color(0xFF0F172A),
                    title = { Text("📍 Set Custom Place / Location", fontWeight = FontWeight.Bold, color = Color.White) },
                    text = {
                        OutlinedTextField(
                            value = customLocationInput,
                            onValueChange = { customLocationInput = it },
                            placeholder = { Text("e.g., Downtown Studio, HQ Room 4B, London...", color = Color(0xFF94A3B8)) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                            colors = translucentWhiteTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val clean = customLocationInput.trim()
                                if (clean.isNotBlank()) {
                                    locationName = clean
                                    editorFeedbackMessage = "📍 Tagged location: $clean"
                                }
                                showCustomLocationDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34D399))
                        ) {
                            Text("Save Location", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCustomLocationDialog = false }) {
                            Text("Cancel", color = Color(0xFF94A3B8))
                        }
                    }
                )
            }
        }

        // Floating Status & Feedback Pill Banner inside Note Editor
        androidx.compose.animation.AnimatedVisibility(
            visible = isAiCleaningUp || isSmartTagging || editorFeedbackMessage != null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 70.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.95f),
                border = BorderStroke(1.dp, RecallCyanSecondary),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isAiCleaningUp || isSmartTagging) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = RecallCyanSecondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = when {
                            isSmartTagging -> "Analyzing note & extracting tags..."
                            isAiCleaningUp -> "Cleaning & structuring with AI..."
                            else -> editorFeedbackMessage ?: ""
                        },
                        color = Color(0xFFF8FAFC),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Floating "Scroll Down" and "Scroll to Top" Quick-Jump Buttons
        androidx.compose.animation.AnimatedVisibility(
            visible = scrollState.canScrollForward,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 76.dp, end = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = RecallVioletPrimary.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.6f)),
                shadowElevation = 8.dp,
                modifier = Modifier.clickable {
                    scope.launch {
                        scrollState.animateScrollTo(scrollState.maxValue)
                    }
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Scroll down",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Scroll down",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = scrollState.canScrollBackward && !scrollState.canScrollForward,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 76.dp, end = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1E293B).copy(alpha = 0.95f),
                border = BorderStroke(1.dp, Color(0xFF475569)),
                shadowElevation = 8.dp,
                modifier = Modifier.clickable {
                    scope.launch {
                        scrollState.animateScrollTo(0)
                    }
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Scroll to top",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Top",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (showReminderSchedulerDialog) {
            val reminder = activeReminder
            ReminderSchedulerDialog(
                initialTitle = title.ifBlank { "Recall Memory Follow-Up" },
                initialDetails = content.take(120),
                noteId = currentNoteId,
                existingReminder = reminder,
                onDismiss = { showReminderSchedulerDialog = false },
                onSave = { rTitle, rDetails, rTime, rTone, rVibrate ->
                    if (reminder != null) {
                        viewModel.updateReminder(
                            reminder.copy(
                                title = rTitle,
                                details = rDetails,
                                reminderTimeMillis = rTime,
                                toneType = rTone.name,
                                vibratePattern = rVibrate.name,
                                isCompleted = false
                            )
                        )
                    } else {
                        viewModel.scheduleReminder(
                            title = rTitle,
                            details = rDetails,
                            noteId = currentNoteId,
                            timeMillis = rTime,
                            tone = rTone,
                            vibratePattern = rVibrate
                        )
                    }
                },
                onDelete = if (reminder != null) {
                    {
                        viewModel.deleteReminder(reminder.id)
                        showReminderSchedulerDialog = false
                    }
                } else null,
                onPreviewTone = { tone, vibrate ->
                    viewModel.previewReminderTone(tone, vibrate)
                }
            )
        }

        actionItemForReminder?.let { actionItem ->
            ReminderSchedulerDialog(
                initialTitle = "Action: ${actionItem.task}",
                initialDetails = "From note: ${title.ifBlank { "Recall Memory" }}",
                noteId = currentNoteId,
                existingReminder = null,
                onDismiss = { actionItemForReminder = null },
                onSave = { rTitle, rDetails, rTime, rTone, rVibrate ->
                    viewModel.scheduleReminder(
                        title = rTitle,
                        details = rDetails,
                        noteId = currentNoteId,
                        timeMillis = rTime,
                        tone = rTone,
                        vibratePattern = rVibrate
                    )
                    actionItemForReminder = null
                    editorFeedbackMessage = "🔔 Scheduled alarm for action item!"
                },
                onDelete = null,
                onPreviewTone = { tone, vibrate ->
                    viewModel.previewReminderTone(tone, vibrate)
                }
            )
        }

        if (showExportDialog) {
            val currentExportNote = NoteWithDetails(
                note = NoteEntity(
                    id = currentNoteId,
                    title = title.ifBlank { "Untitled Memory" },
                    content = content,
                    rawTranscription = rawTranscription.ifBlank { null },
                    category = selectedCategory,
                    createdAt = noteWithDetails?.note?.createdAt ?: System.currentTimeMillis(),
                    tagsJson = org.json.JSONArray(NoteEntity.mergeTagsWithLocation(tagsList.toList(), locationName)).toString()
                ),
                people = allPeople.filter { attachedPeopleIds.contains(it.id) },
                media = attachedMedia.toList(),
                actionItems = attachedActions.toList()
            )
            ExportAndShareDialog(
                documentTitle = title.ifBlank { "Recall Memory" },
                subtitle = "$selectedCategory • ${tagsList.size} tags • ${attachedActions.size} action items",
                markdownContent = ExportAndShareHelper.formatNoteAsMarkdown(currentExportNote),
                fileNamePrefix = title.ifBlank { "recall_note" },
                onShowFeedback = { msg -> editorFeedbackMessage = msg },
                onDismiss = { showExportDialog = false }
            )
        }

        if (showDeleteConfirmationDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmationDialog = false },
                title = {
                    Text(
                        text = "Delete Memory?",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to delete this memory? This action cannot be undone.",
                        color = Color(0xFFCBD5E1)
                    )
                },
                containerColor = Color(0xFF0F172A),
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirmationDialog = false
                            viewModel.deleteNote(currentNoteId)
                            onClose()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Text("Delete", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmationDialog = false }) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                }
            )
        }

        if (showNewProjectInEditorDialog) {
            com.example.ui.people.AddOrEditProjectDialog(
                initialProject = null,
                onDismiss = { showNewProjectInEditorDialog = false },
                onSave = { project ->
                    viewModel.saveProject(project)
                    selectedProjectId = project.id
                    editorFeedbackMessage = "Created & linked project: 🚀 ${project.name}"
                    showNewProjectInEditorDialog = false
                }
            )
        }
    }
}
}
