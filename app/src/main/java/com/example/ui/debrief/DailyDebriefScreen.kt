package com.example.ui.debrief

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.foundation.border
import com.example.ui.theme.NeuralAppBackground
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DailyDebriefResult
import com.example.ui.RecallViewModel
import com.example.ui.components.AudioPlayerCard
import com.example.ui.theme.AppVisualPreferences
import com.example.ui.theme.NeonBrainIcon
import com.example.ui.theme.RecallCyanSecondary
import com.example.ui.theme.RecallDarkBackground
import com.example.ui.theme.RecallVioletPrimary
import com.example.util.AppIntegrationsHelper
import com.example.util.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import com.example.ui.components.ExportAndShareDialog
import com.example.ui.components.ReminderSchedulerDialog
import com.example.ui.theme.TaskbarDropdownButton
import com.example.ui.theme.UnifiedTaskbarInputBox
import com.example.ui.theme.VibrantTagBadge
import com.example.ui.theme.getVibrantTagColor
import com.example.ui.theme.seamlessInnerTextFieldColors
import com.example.ui.theme.translucentWhiteTextFieldColors
import com.example.util.ExportAndShareHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyDebriefScreen(viewModel: RecallViewModel) {
    val context = LocalContext.current
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val durationSeconds by viewModel.recordingDuration.collectAsStateWithLifecycle()
    val amplitude by viewModel.recordingAmplitude.collectAsStateWithLifecycle()
    val liveTranscript by viewModel.liveTranscript.collectAsStateWithLifecycle()
    val lastRecordedFile by viewModel.lastRecordedAudioFile.collectAsStateWithLifecycle()
    val selectedDebriefMode by viewModel.debriefMode.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessingDebrief.collectAsStateWithLifecycle()
    val isCleaningUp by viewModel.isCleaningUpNote.collectAsStateWithLifecycle()
    val debriefResult by viewModel.debriefResult.collectAsStateWithLifecycle()
    val showTriageSheet by viewModel.showTriageSheet.collectAsStateWithLifecycle()
    val hapticsEnabled by AppVisualPreferences.isHapticsEnabled.collectAsStateWithLifecycle()

    val debriefModes = remember {
        listOf(
            "Freestyle" to listOf("Who did I meet today?", "Key decisions made", "Follow-up tasks & deadlines"),
            "Work & Standup" to listOf("What I shipped today", "Blockers or open questions", "Action items for tomorrow"),
            "Social & People" to listOf("New facts about friends/colleagues", "Upcoming birthdays or trips", "Who to catch up with next"),
            "Mindful & Gratitude" to listOf("Best moment of the day", "Something I learned today", "What I'm grateful for")
        )
    }
    val activePrompts = remember(selectedDebriefMode) {
        debriefModes.find { it.first == selectedDebriefMode }?.second ?: debriefModes.first().second
    }

    val audioRetentionPolicy by com.example.util.PrivacySecurityManager.audioRetentionPolicy.collectAsStateWithLifecycle()
    val requireConsentPrompt by com.example.util.PrivacySecurityManager.requireRecordingConsentPrompt.collectAsStateWithLifecycle()
    var showRecordingConsentDialog by remember { mutableStateOf(false) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startDebriefRecording()
        } else {
            viewModel.showFeedback("Microphone permission is required to record daily debrief.")
        }
    }

    fun startRecordingWithPermissionCheck() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            viewModel.startDebriefRecording()
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun toggleRecordingSafely() {
        AppIntegrationsHelper.triggerHapticClick(context, hapticsEnabled)
        if (isRecording) {
            viewModel.finishAndProcessDebrief()
        } else if (requireConsentPrompt) {
            showRecordingConsentDialog = true
        } else {
            startRecordingWithPermissionCheck()
        }
    }

    if (showRecordingConsentDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRecordingConsentDialog = false },
            containerColor = Color(0xFF0F172A),
            title = {
                Text(
                    "🎙️ Recording Consent & Privacy Check",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Recall only records when you explicitly initiate capture. Never record private conversations without the consent of all participants.",
                        color = Color(0xFFCBD5E1),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Active Audio Retention Policy: ${audioRetentionPolicy.displayName} (${audioRetentionPolicy.subtitle})",
                        color = RecallCyanSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRecordingConsentDialog = false
                        com.example.util.PrivacySecurityManager.recordAuditEvent(
                            context,
                            "Recording Consent Confirmed",
                            "User confirmed explicit consent before starting Daily Debrief capture"
                        )
                        startRecordingWithPermissionCheck()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RecallVioletPrimary),
                    modifier = Modifier.testTag("confirm_recording_consent_button")
                ) {
                    Text("I Confirm • Start Recording", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecordingConsentDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    var showTextInputDialog by remember { mutableStateOf(false) }
    var textInputDraft by remember { mutableStateOf("") }

    val dateFormatted = remember {
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
    }

    val scrollState = rememberScrollState()

    NeuralAppBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Sleek, Uncluttered Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeonBrainIcon(size = 38.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daily Debrief",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = dateFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            color = RecallCyanSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0B1120).copy(alpha = 0.72f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isRecording) Color(0xFFEF4444) else Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isRecording) "Recording" else "Mic Ready",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isRecording) Color(0xFFEF4444) else RecallCyanSecondary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Phase 7: Explicit Privacy & Audio Retention Indicator Pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.78f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("debrief_privacy_retention_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "🛡️ Push-to-Record Only • Audio: ${audioRetentionPolicy.displayName}",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (requireConsentPrompt) {
                            Text(
                                text = "Consent Gate Active",
                                color = Color(0xFF10B981),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Debrief Mode Dropdown Selector (replaces horizontal pill row)
                var showModeDropdown by remember { mutableStateOf(false) }
                val activeModeColor = getVibrantTagColor(selectedDebriefMode)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Debrief Focus Mode:",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFE2E8F0),
                        fontWeight = FontWeight.SemiBold
                    )

                    Box {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0F172A).copy(alpha = 0.92f),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, activeModeColor),
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showModeDropdown = true }
                                .testTag("debrief_mode_dropdown")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(activeModeColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = selectedDebriefMode,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Debrief Mode",
                                    tint = activeModeColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showModeDropdown,
                            onDismissRequest = { showModeDropdown = false },
                            modifier = Modifier
                                .background(Color(0xFF0F172A))
                                .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                        ) {
                            debriefModes.forEach { (modeName, _) ->
                                val isSelected = selectedDebriefMode == modeName
                                val mColor = getVibrantTagColor(modeName)
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(mColor)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = modeName,
                                                color = if (isSelected) mColor else Color.White,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    },
                                    trailingIcon = {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = mColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    },
                                    onClick = {
                                        AppIntegrationsHelper.triggerHapticClick(context, hapticsEnabled)
                                        viewModel.debriefMode.value = modeName
                                        showModeDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Guided Topic Prompts for the active Debrief Mode (Recall-themed interactive buttons)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(activePrompts) { prompt ->
                        OutlinedButton(
                            onClick = {
                                textInputDraft = if (textInputDraft.isBlank()) "$prompt: " else "$textInputDraft\n$prompt: "
                                showTextInputDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.65f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFF0F172A).copy(alpha = 0.88f),
                                contentColor = Color(0xFFF8FAFC)
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "💡 $prompt",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFF8FAFC),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isRecording) {
                        "Recording full-length raw audio ($selectedDebriefMode)... Speak freely without rushing—tap Stop when finished."
                    } else {
                        "Speak freely about who you met, discussions, or tasks. Full raw audio is saved permanently and unpacked with Gemini AI."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isRecording) RecallCyanSecondary else Color(0xFFCBD5E1),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Centerpiece: Circular audio visualizer ring (balanced 180dp size)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(180.dp)
                ) {
                    VisualizerRing(isRecording = isRecording, amplitude = amplitude)

                    // Central Microphone Button (Themed with glowing border)
                    Surface(
                        shape = CircleShape,
                        color = if (isRecording) Color(0xFFEF4444) else RecallVioletPrimary,
                        border = BorderStroke(
                            2.dp,
                            if (isRecording) Color.White else RecallCyanSecondary
                        ),
                        shadowElevation = 10.dp,
                        modifier = Modifier
                            .size(78.dp)
                            .clip(CircleShape)
                            .clickable { toggleRecordingSafely() }
                            .testTag("debrief_mic_toggle_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = if (isRecording) "Stop" else "Record",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Timer: Numeric stop-clock (00:00) + Smart Audio Timestamp Bookmark Pin
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = DateUtils.formatSecondsToTime(durationSeconds),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )
                    if (isRecording) {
                        val stamp = DateUtils.formatSecondsToTime(durationSeconds)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFBBF24).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFFFBBF24)),
                            modifier = Modifier.clickable {
                                val marker = "\n[📌 Bookmark $stamp] Key highlight: "
                                textInputDraft = if (textInputDraft.isBlank()) marker.trimStart() else textInputDraft + marker
                                viewModel.showFeedback("📌 Pinned audio bookmark at $stamp")
                            }
                        ) {
                            Text(
                                text = "📌 Pin $stamp",
                                color = Color(0xFFFBBF24),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Permanent In-App Audio Player Card when a recording is available
                if (!isRecording && lastRecordedFile != null && lastRecordedFile!!.exists() && lastRecordedFile!!.length() > 400) {
                    AudioPlayerCard(
                        audioPath = lastRecordedFile!!.absolutePath,
                        title = "Latest Debrief Recording ($selectedDebriefMode)",
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // Live Speech Transcript Snippet (Scrollable & Unclipped)
                if (liveTranscript.isNotBlank() || isRecording) {
                    val transcriptScrollState = rememberScrollState()
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1E293B).copy(alpha = 0.92f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp, max = 160.dp)
                                    .verticalScroll(transcriptScrollState)
                            ) {
                                Text(
                                    text = if (liveTranscript.isBlank()) "Listening..." else liveTranscript,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (liveTranscript.isNotBlank() && !isRecording) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = {
                                            viewModel.aiCleanUpNote(liveTranscript) { cleaned ->
                                                textInputDraft = cleaned
                                                showTextInputDialog = true
                                            }
                                        },
                                        enabled = !isCleaningUp,
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = RecallCyanSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            if (isCleaningUp) "Cleaning Up..." else "Clean Up with AI",
                                            color = RecallCyanSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Action Buttons (Unclipped, responsive padding & single-line readable labels)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Manual Text Input option (Themed Cyan-bordered Dark Glass Button)
                    OutlinedButton(
                        onClick = {
                            textInputDraft = liveTranscript
                            showTextInputDialog = true
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.5.dp, RecallCyanSecondary),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF0F172A).copy(alpha = 0.92f),
                            contentColor = RecallCyanSecondary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 50.dp)
                            .testTag("debrief_type_instead_button")
                    ) {
                        Icon(
                            Icons.Default.Keyboard,
                            contentDescription = null,
                            tint = RecallCyanSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Type Instead",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = RecallCyanSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Finish & Process Floating CTA (Themed Recall Violet Primary Button)
                    val isProcessReady = (durationSeconds >= 2 || liveTranscript.isNotBlank() || isRecording) && !isProcessing
                    Button(
                        onClick = { viewModel.finishAndProcessDebrief() },
                        enabled = isProcessReady,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.5.dp,
                            if (isRecording) Color.White.copy(alpha = 0.85f)
                            else if (isProcessReady) RecallCyanSecondary
                            else RecallVioletPrimary.copy(alpha = 0.55f)
                        ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording) Color(0xFFEF4444) else RecallVioletPrimary,
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFF1E1B4B).copy(alpha = 0.92f),
                            disabledContentColor = Color(0xFFE2E8F0)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .heightIn(min = 50.dp)
                            .testTag("finish_and_process_button")
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Unpacking...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isRecording) "Stop & Unpack" else "Process & Unpack",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    // Manual Text Dialog with Single-Output AI Clean Up
    if (showTextInputDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTextInputDialog = false },
            containerColor = Color(0xFF0F172A),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Write Daily Debrief",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = {
                            if (textInputDraft.isNotBlank()) {
                                viewModel.aiCleanUpNote(textInputDraft) { cleaned ->
                                    textInputDraft = cleaned
                                }
                            }
                        },
                        enabled = textInputDraft.isNotBlank() && !isCleaningUp,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, RecallCyanSecondary),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF1E293B),
                            contentColor = RecallCyanSecondary,
                            disabledContentColor = Color(0xFF64748B)
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.heightIn(min = 36.dp)
                    ) {
                        if (isCleaningUp) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = RecallCyanSecondary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Cleaning...",
                                color = RecallCyanSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        } else {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = RecallCyanSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Clean Up with AI",
                                color = RecallCyanSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            },
            text = {
                var showDialogModeMenu by remember { mutableStateOf(false) }
                var showDialogTemplateMenu by remember { mutableStateOf(false) }
                var showDialogAiMenu by remember { mutableStateOf(false) }

                UnifiedTaskbarInputBox(
                    borderColor = RecallVioletPrimary,
                    taskbarContent = {
                        // (A) Debrief Mode Dropdown
                        Box {
                            TaskbarDropdownButton(
                                label = selectedDebriefMode,
                                dotColor = getVibrantTagColor(selectedDebriefMode),
                                accentColor = getVibrantTagColor(selectedDebriefMode),
                                onClick = { showDialogModeMenu = true }
                            )
                            DropdownMenu(
                                expanded = showDialogModeMenu,
                                onDismissRequest = { showDialogModeMenu = false },
                                modifier = Modifier
                                    .background(Color(0xFF0F172A))
                                    .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                            ) {
                                debriefModes.forEach { (mode, _) ->
                                    val c = getVibrantTagColor(mode)
                                    DropdownMenuItem(
                                        text = { Text(mode, color = c, fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            viewModel.debriefMode.value = mode
                                            showDialogModeMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        // (B) Quick Templates Dropdown
                        Box {
                            TaskbarDropdownButton(
                                label = "Templates",
                                icon = Icons.Default.AutoAwesome,
                                accentColor = RecallVioletPrimary,
                                onClick = { showDialogTemplateMenu = true }
                            )
                            DropdownMenu(
                                expanded = showDialogTemplateMenu,
                                onDismissRequest = { showDialogTemplateMenu = false },
                                modifier = Modifier
                                    .background(Color(0xFF0F172A))
                                    .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                            ) {
                                listOf(
                                    "Meetings Recap" to "Today I met with:\nKey decisions:\nNext steps:",
                                    "People & Facts" to "Person I spoke with:\nNew facts learned:\nFollow-up date:",
                                    "Tasks & Ideas" to "Top priorities:\nAction items:\nNew ideas:"
                                ).forEach { (label, tpl) ->
                                    DropdownMenuItem(
                                        text = { Text(label, color = Color.White, fontWeight = FontWeight.SemiBold) },
                                        onClick = {
                                            textInputDraft = if (textInputDraft.isBlank()) tpl else "$textInputDraft\n\n$tpl"
                                            showDialogTemplateMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        // (C) AI & Draft Actions Dropdown
                        Box {
                            TaskbarDropdownButton(
                                label = "Actions",
                                accentColor = Color(0xFF0284C7),
                                onClick = { showDialogAiMenu = true }
                            )
                            DropdownMenu(
                                expanded = showDialogAiMenu,
                                onDismissRequest = { showDialogAiMenu = false },
                                modifier = Modifier
                                    .background(Color(0xFF0F172A))
                                    .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("✨ Clean Up with AI", color = Color.White, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        showDialogAiMenu = false
                                        if (textInputDraft.isNotBlank()) {
                                            viewModel.aiCleanUpNote(textInputDraft) { cleaned ->
                                                textInputDraft = cleaned
                                            }
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🧹 Clear Draft", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        showDialogAiMenu = false
                                        textInputDraft = ""
                                    }
                                )
                            }
                        }
                    },
                    bodyContent = {
                        OutlinedTextField(
                            value = textInputDraft,
                            onValueChange = { textInputDraft = it },
                            placeholder = { Text("Describe meetings, who you talked with, facts learned...", color = Color(0xFF94A3B8)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 160.dp, max = 260.dp),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                            colors = seamlessInnerTextFieldColors()
                        )
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showTextInputDialog = false
                        viewModel.finishAndProcessDebrief(textInputDraft)
                    },
                    enabled = textInputDraft.isNotBlank() && !isCleaningUp,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RecallVioletPrimary,
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF1E1B4B),
                        disabledContentColor = Color(0xFF94A3B8)
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Process & Unpack", fontWeight = FontWeight.Bold, maxLines = 1)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showTextInputDialog = false },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    border = BorderStroke(1.dp, RecallCyanSecondary)
                ) {
                    Text("Cancel", color = RecallCyanSecondary, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        )
    }

    // Triage Confirmation Modal
    if (showTriageSheet && debriefResult != null) {
        TriageConfirmationModal(
            result = debriefResult!!,
            onDismiss = { viewModel.dismissTriageSheet() },
            onTogglePerson = { viewModel.toggleDebriefPersonIncluded(it) },
            onToggleNote = { viewModel.toggleDebriefNoteIncluded(it) },
            onToggleNoteUpdate = { viewModel.toggleDebriefNoteUpdateIncluded(it) },
            onToggleAction = { viewModel.toggleDebriefActionIncluded(it) },
            onToggleCommitment = { viewModel.toggleDebriefCommitmentIncluded(it) },
            onScheduleActionReminder = { rTitle, rDetails, rTime, rTone, rVibrate ->
                viewModel.scheduleReminder(
                    title = rTitle,
                    details = rDetails,
                    noteId = null,
                    timeMillis = rTime,
                    tone = rTone,
                    vibratePattern = rVibrate
                )
                viewModel.showFeedback("🔔 Scheduled alarm for debrief action item!")
            },
            onPreviewTone = { tone, vibrate -> viewModel.previewReminderTone(tone, vibrate) },
            onCommit = { viewModel.commitDebriefToMemory() }
        )
    }
}

@Composable
fun VisualizerRing(isRecording: Boolean, amplitude: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "ring_transition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = this.center
        val baseRadius = size.minDimension / 2.8f
        val dynamicAdd = if (isRecording) (amplitude * 45f) else 0f
        val currentRadius = (baseRadius + dynamicAdd) * if (isRecording) pulseScale else 1f

        // Outer glow circle
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    RecallVioletPrimary.copy(alpha = if (isRecording) 0.35f else 0.15f),
                    RecallCyanSecondary.copy(alpha = if (isRecording) 0.15f else 0.05f),
                    Color.Transparent
                ),
                center = center,
                radius = currentRadius + 30f
            ),
            center = center,
            radius = currentRadius + 30f
        )

        // Middle ring
        drawCircle(
            color = RecallCyanSecondary.copy(alpha = if (isRecording) 0.5f else 0.2f),
            center = center,
            radius = currentRadius,
            style = Stroke(width = 3.dp.toPx())
        )

        // Inner ring
        drawCircle(
            color = RecallVioletPrimary.copy(alpha = if (isRecording) 0.8f else 0.3f),
            center = center,
            radius = currentRadius - 16f,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriageConfirmationModal(
    result: DailyDebriefResult,
    onDismiss: () -> Unit,
    onTogglePerson: (Int) -> Unit,
    onToggleNote: (Int) -> Unit,
    onToggleNoteUpdate: (Int) -> Unit = {},
    onToggleAction: (Int) -> Unit,
    onToggleCommitment: (Int) -> Unit = {},
    onScheduleActionReminder: (
        title: String,
        details: String,
        timeMillis: Long,
        tone: com.example.data.local.entity.ReminderTone,
        vibrate: com.example.data.local.entity.ReminderVibratePattern
    ) -> Unit = { _, _, _, _, _ -> },
    onPreviewTone: (com.example.data.local.entity.ReminderTone, com.example.data.local.entity.ReminderVibratePattern) -> Unit = { _, _ -> },
    onCommit: () -> Unit
) {
    val context = LocalContext.current
    var showExportDebriefDialog by remember { mutableStateOf(false) }
    var actionTaskForReminder by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        contentColor = Color.White,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Memory Routing & Unpack",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Decomposed into ${result.distinctNotes.size} notes, ${result.peopleUpdates.size} people, ${result.commitments.size} commitments & ${result.actionItems.size} tasks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = RecallCyanSecondary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = RecallCyanSecondary.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, RecallCyanSecondary),
                    modifier = Modifier.clickable { showExportDebriefDialog = true }
                ) {
                    Text(
                        text = "📤 Export",
                        color = RecallCyanSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            if (result.isLocalFallback) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Unpacked via Smart On-Device Engine (AI server was busy)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 0A: In-App Audio Player if recorded
                if (!result.audioFilePath.isNullOrBlank()) {
                    item {
                        AudioPlayerCard(
                            audioPath = result.audioFilePath,
                            title = "Original Raw Audio Memory"
                        )
                    }
                }

                // Section 0B: Full Audio Verbatim Transcript from Gemini (Translucent White)
                if (!result.rawTranscription.isNullOrBlank()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0F172A).copy(alpha = 0.90f),
                            border = BorderStroke(1.5.dp, RecallCyanSecondary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.RecordVoiceOver,
                                        contentDescription = null,
                                        tint = RecallVioletPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Full Audio Transcript (Verbatim)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = RecallVioletPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    result.rawTranscription,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                // Section 1: Daily Summary (Dark Card)
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.90f),
                        border = BorderStroke(1.5.dp, RecallVioletPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                "Daily Summary",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = RecallVioletPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                result.dailySummary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Section 2: People Updates (Horizontal scrolling cards with 'X')
                if (result.peopleUpdates.isNotEmpty()) {
                    item {
                        Text(
                            "People Updates (${result.peopleUpdates.count { it.isIncluded }})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            itemsIndexed(result.peopleUpdates) { index, person ->
                                Card(
                                    modifier = Modifier
                                        .width(220.dp)
                                        .testTag("triage_person_$index"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (person.isIncluded) MaterialTheme.colorScheme.surfaceVariant
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Person,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                person.personName,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = { onTogglePerson(index) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    if (person.isIncluded) Icons.Default.Close else Icons.Default.Check,
                                                    contentDescription = "Toggle",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        if (!person.relationshipHint.isNullOrBlank()) {
                                            Text(
                                                person.relationshipHint,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        person.newFacts.forEach { fact ->
                                            Text(
                                                "• $fact",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White
                                            )
                                        }
                                        person.datesToRemember.forEach { d ->
                                            Text(
                                                "📅 ${d.label}: ${d.dateIso}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = RecallCyanSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 2B: Updates to Existing Notes (Stateful Memory Reconciliation)
                if (result.noteUpdates.isNotEmpty()) {
                    item {
                        Text(
                            "🔄 Updates to Existing Notes (${result.noteUpdates.count { it.isIncluded }})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF34D399)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    itemsIndexed(result.noteUpdates) { index, noteUpdate ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.2.dp,
                                    Color(0xFF10B981).copy(alpha = 0.65f),
                                    RoundedCornerShape(12.dp)
                                )
                                .testTag("triage_note_update_$index"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (noteUpdate.isIncluded) Color(0xFF064E3B).copy(alpha = 0.35f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Checkbox(
                                    checked = noteUpdate.isIncluded,
                                    onCheckedChange = { onToggleNoteUpdate(index) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF10B981).copy(alpha = 0.25f)
                                        ) {
                                            Text(
                                                "🔄 RECONCILED UPDATE",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF34D399),
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontSize = 10.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                noteUpdate.category,
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "${noteUpdate.originalTitle}  ➔  ${noteUpdate.updatedTitle}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = noteUpdate.changeSummary,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFF6EE7B7),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = noteUpdate.updatedContent,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFE2E8F0)
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 3: Notes Created (Vertical Accordion)
                if (result.distinctNotes.isNotEmpty()) {
                    item {
                        Text(
                            "Notes Created (${result.distinctNotes.count { it.isIncluded }})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    itemsIndexed(result.distinctNotes) { index, note ->
                        val isAiInferred = note.verificationStatus == "AI_INFERRED_PENDING" || note.confidenceScore < 0.80f
                        val confidencePct = (note.confidenceScore.coerceIn(0.1f, 1.0f) * 100).toInt()
                        val unifiedType = com.example.data.model.UnifiedMemoryType.fromString(note.memoryType)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (note.isIncluded) MaterialTheme.colorScheme.surfaceVariant
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Checkbox(
                                    checked = note.isIncluded,
                                    onCheckedChange = { onToggleNote(index) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            note.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                "${unifiedType.iconEmoji} ${note.category}",
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // Provenance & Confidence Badge
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isAiInferred) Color(0xFFF59E0B).copy(alpha = 0.20f) else Color(0xFF10B981).copy(alpha = 0.18f),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isAiInferred) Color(0xFFF59E0B).copy(alpha = 0.6f) else Color(0xFF10B981).copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Text(
                                            text = if (isAiInferred) {
                                                "🤖 AI Interpretation ($confidencePct% confidence) • Flagged for Review"
                                            } else {
                                                "✓ Explicit Fact ($confidencePct% confidence) • ${unifiedType.displayName}"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isAiInferred) Color(0xFFFBBF24) else Color(0xFF34D399),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        note.content,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (note.tags.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            note.tags.take(4).forEach { tag ->
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = RecallCyanSecondary.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = if (tag.startsWith("#")) tag else "#$tag",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = RecallCyanSecondary,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    if (note.peopleInvolved.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "➡️ Routes to profiles of: ${note.peopleInvolved.joinToString()}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = RecallVioletPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 3B: Extracted Commitments & Promises
                if (result.commitments.isNotEmpty()) {
                    item {
                        Text(
                            "🤝 Commitments & Promises (${result.commitments.count { it.isIncluded }})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFBBF24)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    itemsIndexed(result.commitments) { index, commitment ->
                        val isByMe = commitment.direction.equals("BY_ME", ignoreCase = true)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    Color(0xFFF59E0B).copy(alpha = 0.5f),
                                    RoundedCornerShape(12.dp)
                                )
                                .testTag("triage_commitment_$index"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (commitment.isIncluded) Color(0xFF451A03).copy(alpha = 0.35f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = commitment.isIncluded,
                                    onCheckedChange = { onToggleCommitment(index) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isByMe) Color(0xFFF59E0B).copy(alpha = 0.22f) else RecallCyanSecondary.copy(alpha = 0.22f)
                                        ) {
                                            Text(
                                                text = if (isByMe) "🤝 OWED BY ME" else "🤝 OWED TO ME",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isByMe) Color(0xFFFBBF24) else RecallCyanSecondary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        if (!commitment.personName.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "👤 ${commitment.personName}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = commitment.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 4: Extracted Actions
                if (result.actionItems.isNotEmpty()) {
                    item {
                        Text(
                            "Action Items (${result.actionItems.count { it.isIncluded }})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    itemsIndexed(result.actionItems) { index, action ->
                        val localContext = LocalContext.current
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (action.isIncluded) MaterialTheme.colorScheme.surfaceVariant
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = action.isIncluded,
                                    onCheckedChange = { onToggleAction(index) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    action.task,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                if (!action.dueDateHint.isNullOrBlank()) {
                                    Text(
                                        action.dueDateHint,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = RecallCyanSecondary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFBBF24).copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.45f)),
                                    modifier = Modifier.clickable {
                                        actionTaskForReminder = action.task
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
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = {
                                        AppIntegrationsHelper.scheduleInCalendar(
                                            context = localContext,
                                            title = action.task,
                                            description = "Action item from Recall Daily Debrief",
                                            dateHintOrIso = action.dueDateHint
                                        )
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = "Schedule in Calendar",
                                        tint = RecallCyanSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary CTA: Commit to Memory (Themed Recall Primary Button)
            Button(
                onClick = onCommit,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, RecallCyanSecondary),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("commit_to_memory_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RecallVioletPrimary,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Commit to Memory", fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showExportDebriefDialog) {
        ExportAndShareDialog(
            documentTitle = "Daily Debrief Summary",
            subtitle = "${result.distinctNotes.size} notes • ${result.peopleUpdates.size} people • ${result.actionItems.size} tasks",
            markdownContent = ExportAndShareHelper.formatDebriefAsMarkdown(result),
            fileNamePrefix = "daily_debrief",
            onShowFeedback = { _ -> },
            onDismiss = { showExportDebriefDialog = false }
        )
    }

    actionTaskForReminder?.let { taskText ->
        ReminderSchedulerDialog(
            initialTitle = "Debrief Action: $taskText",
            initialDetails = result.dailySummary.take(120),
            noteId = null,
            existingReminder = null,
            onDismiss = { actionTaskForReminder = null },
            onSave = { rTitle, rDetails, rTime, rTone, rVibrate ->
                onScheduleActionReminder(rTitle, rDetails, rTime, rTone, rVibrate)
                actionTaskForReminder = null
            },
            onDelete = null,
            onPreviewTone = { tone, vibrate ->
                onPreviewTone(tone, vibrate)
            }
        )
    }
}
