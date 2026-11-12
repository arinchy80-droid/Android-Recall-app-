package com.example.ui.gallery

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import java.io.File
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.MediaEntity
import com.example.data.local.entity.MediaType
import com.example.ui.RecallViewModel
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.ui.theme.RecallCyanSecondary
import com.example.ui.theme.RecallVioletPrimary
import com.example.ui.theme.NeuralAppBackground
import com.example.ui.theme.TaskbarDropdownButton
import com.example.ui.theme.UnifiedTaskbarInputBox
import com.example.ui.theme.seamlessInnerTextFieldColors
import com.example.ui.theme.translucentWhiteTextFieldColors
import com.example.util.DateUtils

@Composable
fun GalleryScreen(
    viewModel: RecallViewModel,
    onOpenNote: (String) -> Unit
) {
    val allMedia by viewModel.allMedia.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var selectedMedia by remember { mutableStateOf<MediaEntity?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            viewModel.processCameraCapture(bitmap)
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
            viewModel.showFeedback("Camera permission is needed to take photos. You can also pick from gallery.")
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.processImageUriCapture(uri)
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

    val filteredMedia = remember(allMedia, searchQuery) {
        if (searchQuery.isBlank()) allMedia
        else allMedia.filter {
            it.aiDescription?.contains(searchQuery, ignoreCase = true) == true ||
                    it.ocrText?.contains(searchQuery, ignoreCase = true) == true ||
                    it.labelsJson.contains(searchQuery, ignoreCase = true)
        }
    }

    NeuralAppBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // Sleek Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF0F172A).copy(alpha = 0.8f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.5f)),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Collections,
                                contentDescription = null,
                                tint = RecallCyanSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Visual Memories",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${filteredMedia.size} memories with on-device OCR",
                            style = MaterialTheme.typography.labelSmall,
                            color = RecallCyanSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Unified Search Box with Internal Top Taskbar Dropdowns
                var showCaptureDropdown by remember { mutableStateOf(false) }
                var showKeywordDropdown by remember { mutableStateOf(false) }

                UnifiedTaskbarInputBox(
                    shape = RoundedCornerShape(20.dp),
                    borderColor = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                    taskbarContent = {
                        // (A) Visual Filter Keywords Dropdown
                        Box {
                            TaskbarDropdownButton(
                                label = if (searchQuery.isBlank()) "Filter: All Visuals" else "Filter: $searchQuery",
                                icon = Icons.Default.Collections,
                                accentColor = RecallVioletPrimary,
                                onClick = { showKeywordDropdown = true }
                            )
                            DropdownMenu(
                                expanded = showKeywordDropdown,
                                onDismissRequest = { showKeywordDropdown = false },
                                properties = PopupProperties(clippingEnabled = true),
                                modifier = Modifier
                                    .background(Color(0xFF0F172A))
                                    .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All Visual Memories", color = RecallVioletPrimary, fontWeight = FontWeight.ExtraBold) },
                                    onClick = {
                                        searchQuery = ""
                                        showKeywordDropdown = false
                                    }
                                )
                                listOf("Whiteboard", "Document", "Receipt", "Notes", "Screenshot").forEach { kw ->
                                    DropdownMenuItem(
                                        text = { Text("🔍 $kw", color = Color.White, fontWeight = FontWeight.SemiBold) },
                                        onClick = {
                                            searchQuery = kw
                                            showKeywordDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        // (B) Add / Capture Visual Dropdown
                        Box {
                            TaskbarDropdownButton(
                                label = "Add Visual",
                                icon = Icons.Default.CameraAlt,
                                accentColor = Color(0xFF0284C7),
                                onClick = { showCaptureDropdown = true }
                            )
                            DropdownMenu(
                                expanded = showCaptureDropdown,
                                onDismissRequest = { showCaptureDropdown = false },
                                properties = PopupProperties(clippingEnabled = true),
                                modifier = Modifier
                                    .background(Color(0xFF0F172A))
                                    .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("📷 Take Photo (Camera OCR)", color = Color.White, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        showCaptureDropdown = false
                                        scope.launch {
                                            delay(60)
                                            launchCameraSafely()
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🖼️ Upload Photo (Phone Memory)", color = Color.White, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        showCaptureDropdown = false
                                        scope.launch {
                                            delay(60)
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                    }
                                )
                                if (searchQuery.isNotEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("🧹 Clear Search", color = Color(0xFFF87171), fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            showCaptureDropdown = false
                                            searchQuery = ""
                                        }
                                    )
                                }
                            }
                        }
                    },
                    bodyContent = {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search images, OCR text, labels...", color = Color(0xFF94A3B8)) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = RecallVioletPrimary)
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF94A3B8))
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            ),
                            colors = seamlessInnerTextFieldColors()
                        )
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

            if (filteredMedia.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 80.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(68.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = RecallCyanSecondary,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No visual memories matching \"$searchQuery\"" else "No Visual Memories Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Take a photo of a whiteboard, book, or receipt to trigger automatic OCR.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(filteredMedia, key = { it.id }) { media ->
                        VisualMemoryItem(
                            media = media,
                            onClick = { selectedMedia = media }
                        )
                    }
                }
            }
        }

        // Action FABs
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Photo Gallery Pick FAB
            FloatingActionButton(
                onClick = {
                    photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                containerColor = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("gallery_picker_fab")
            ) {
                Icon(Icons.Default.Collections, contentDescription = "Pick Photo from Gallery")
            }

            // Camera FAB
            FloatingActionButton(
                onClick = { launchCameraSafely() },
                containerColor = RecallCyanSecondary,
                modifier = Modifier.testTag("gallery_camera_fab")
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Capture Visual Memory")
            }
        }
    }
    }

    // Detail Modal Dialog
    selectedMedia?.let { media ->
        VisualMemoryDetailDialog(
            media = media,
            viewModel = viewModel,
            onDismiss = { selectedMedia = null },
            onOpenNote = { noteId ->
                selectedMedia = null
                onOpenNote(noteId)
            }
        )
    }
}

@Composable
fun VisualMemoryItem(media: MediaEntity, onClick: () -> Unit) {
    val photoFile = remember(media.localUri) { if (media.mediaType == MediaType.IMAGE && media.localUri.isNotBlank()) File(media.localUri) else null }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("gallery_item_${media.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.85f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.4f))
    ) {
        Column {
            // Visual Banner with real photo or gradient fallback
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color(0xFF0F172A))
            ) {
                if (photoFile != null && photoFile.exists()) {
                    AsyncImage(
                        model = photoFile,
                        contentDescription = media.aiDescription ?: "Memory Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(RecallVioletPrimary.copy(alpha = 0.6f), RecallCyanSecondary.copy(alpha = 0.7f))
                                )
                            )
                    )
                }

                // Media Type Icon Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (media.mediaType == MediaType.IMAGE) Icons.Default.Image else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(media.mediaType.name, color = Color.White, fontSize = 10.sp)
                    }
                }

                // OCR Badge if text was detected
                if (!media.ocrText.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = RecallVioletPrimary.copy(alpha = 0.9f),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                    ) {
                        Text(
                            "OCR DETECTED",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Description info
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = media.aiDescription ?: "Visual memory snapshot",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = DateUtils.formatRelativeTime(media.capturedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

@Composable
fun VisualMemoryDetailDialog(
    media: MediaEntity,
    viewModel: RecallViewModel,
    onDismiss: () -> Unit,
    onOpenNote: (String) -> Unit
) {
    var showListTypeMenu by remember { mutableStateOf(false) }
    val combinedText = remember(media) {
        listOfNotNull(media.ocrText?.takeIf { it.isNotBlank() }, media.aiDescription?.takeIf { it.isNotBlank() })
            .joinToString("\n")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        titleContentColor = Color.White,
        textContentColor = Color(0xFFCBD5E1),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Image, contentDescription = null, tint = RecallCyanSecondary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Visual & Document Intelligence", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header Photo Banner
                val detailPhotoFile = remember(media.localUri) { if (media.mediaType == MediaType.IMAGE && media.localUri.isNotBlank()) File(media.localUri) else null }

                if (detailPhotoFile != null && detailPhotoFile.exists()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A))
                    ) {
                        AsyncImage(
                            model = detailPhotoFile,
                            contentDescription = "Original Memory Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(RecallVioletPrimary.copy(alpha = 0.8f), RecallCyanSecondary.copy(alpha = 0.8f))
                                ),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Captured Memory Snapshot", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text = "Captured: ${DateUtils.formatDate(media.capturedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )

                // AI Description Card (Dark Slate)
                if (!media.aiDescription.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E293B).copy(alpha = 0.90f),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, RecallCyanSecondary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("AI Semantic Description", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = RecallCyanSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(media.aiDescription, style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // OCR Text Box (Dark Slate)
                if (!media.ocrText.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E293B).copy(alpha = 0.90f),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, RecallVioletPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Extracted OCR Text", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = RecallVioletPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(media.ocrText, style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.Medium, maxLines = 5, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                // Document & Image Intelligence Actions (Convert OCR/Photo into List, Commitment, Task, or Reminder)
                if (combinedText.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0B1120),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "⚡ Act on Extracted Document Intelligence",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF34D399)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, Color(0xFF34D399)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { showListTypeMenu = true }
                                    ) {
                                        Text(
                                            text = "📋 To List",
                                            color = Color(0xFF34D399),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showListTypeMenu,
                                        onDismissRequest = { showListTypeMenu = false },
                                        modifier = Modifier.background(Color(0xFF0F172A))
                                    ) {
                                        com.example.util.DynamicListType.entries.forEach { listType ->
                                            DropdownMenuItem(
                                                text = { Text("${listType.icon} ${listType.displayName}", color = Color.White, fontSize = 12.sp) },
                                                onClick = {
                                                    showListTypeMenu = false
                                                    val items = com.example.util.RecallLifeOrganiserManager.extractListItemsFromText(combinedText)
                                                    viewModel.createDynamicList(
                                                        title = (media.aiDescription ?: listType.displayName).take(40),
                                                        type = listType,
                                                        initialItems = if (items.isNotEmpty()) items else listOf(combinedText.take(80)),
                                                        sourceNoteId = media.noteId
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, Color(0xFFFBBF24)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            viewModel.saveCommitment(
                                                com.example.data.local.entity.CommitmentEntity(
                                                    id = java.util.UUID.randomUUID().toString(),
                                                    noteId = media.noteId,
                                                    description = combinedText.lines().first().take(120),
                                                    direction = "BY_ME"
                                                )
                                            )
                                        }
                                ) {
                                    Text(
                                        text = "🤝 Promise",
                                        color = Color(0xFFFBBF24),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = RecallCyanSecondary.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, RecallCyanSecondary),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            viewModel.createRichActionItem(
                                                task = combinedText.lines().first().take(100),
                                                priority = com.example.util.TaskPriority.HIGH,
                                                column = com.example.util.TaskBoardColumn.TODAY,
                                                noteId = media.noteId
                                            )
                                        }
                                ) {
                                    Text(
                                        text = "✅ Task",
                                        color = RecallCyanSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (media.noteId != null) {
                Button(onClick = { onOpenNote(media.noteId) }) {
                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Linked Note", color = Color.White)
                }
            } else {
                TextButton(onClick = onDismiss) { Text("Done", color = RecallCyanSecondary) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = Color(0xFFCBD5E1)) }
        }
    )
}
