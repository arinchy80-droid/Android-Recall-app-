package com.example.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.draw.clip
import com.example.ui.theme.NeonBrainIcon
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ChatMessage
import com.example.data.model.Citation
import com.example.data.model.MessageSender
import com.example.ui.RecallViewModel
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.example.data.local.entity.CommitmentEntity
import com.example.ui.theme.RecallCyanSecondary
import com.example.ui.theme.RecallEmeraldAccent
import com.example.ui.theme.RecallVioletPrimary
import com.example.ui.theme.MemoraeCircleIconButton
import com.example.ui.theme.MemoraeCompanionAvatar
import com.example.ui.theme.NeuralAppBackground
import com.example.ui.theme.TaskbarDropdownButton
import com.example.ui.theme.UnifiedTaskbarInputBox
import com.example.ui.theme.seamlessInnerTextFieldColors
import com.example.ui.theme.translucentWhiteTextFieldColors
import com.example.util.AppIntegrationsHelper
import com.example.util.RecallLifeOrganiserManager
import java.util.Locale

@Composable
fun AskAiScreen(
    viewModel: RecallViewModel,
    onNavigateToNote: (String) -> Unit
) {
    val context = LocalContext.current
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isQueryingAi by viewModel.isQueryingAi.collectAsStateWithLifecycle()
    val enableHighThinking by viewModel.enableHighThinking.collectAsStateWithLifecycle()
    val notesCount by viewModel.notesCount.collectAsStateWithLifecycle()
    val peopleCount by viewModel.peopleCount.collectAsStateWithLifecycle()
    val allPeople by viewModel.people.collectAsStateWithLifecycle()
    val allPlaces by viewModel.allPlaces.collectAsStateWithLifecycle()
    val allProjects by viewModel.allProjects.collectAsStateWithLifecycle()
    val allCommitments by viewModel.allCommitments.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    var toneDropdownExpanded by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val naturalDateSuggestion = remember(inputText, allPeople) {
        RecallLifeOrganiserManager.parseNaturalDateTimeSuggestion(
            text = inputText,
            knownPeopleNames = allPeople.map { it.name }
        )
    }

    val scope = rememberCoroutineScope()
    var isDictatingQuery by remember { mutableStateOf(false) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.audioRecorder.startRecording(scope)
            isDictatingQuery = true
        } else {
            viewModel.showFeedback("Microphone permission is required for voice query.")
        }
    }

    fun toggleVoiceDictationSafely() {
        if (isDictatingQuery) {
            val (_, spokenText) = viewModel.audioRecorder.stopRecording()
            isDictatingQuery = false
            if (spokenText.isNotBlank()) {
                inputText = spokenText
            }
        } else {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                viewModel.audioRecorder.startRecording(scope)
                isDictatingQuery = true
            } else {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    // Android TextToSpeech engine
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        val tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsEngine?.language = Locale.US
            }
        }
        ttsEngine = tts
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    val chatbotTone by viewModel.chatbotTone.collectAsStateWithLifecycle()

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    NeuralAppBackground {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // 1. Memorae Header Bar matching Screenshot 4
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MemoraeCompanionAvatar(size = 38.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "☕", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Coffee with Memorae",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    val openCommitmentsCount = allCommitments.count { !it.isCompleted }
                    Text(
                        text = "$notesCount memories • $openCommitmentsCount open promises",
                        style = MaterialTheme.typography.labelSmall,
                        color = RecallCyanSecondary,
                        fontSize = 11.sp
                    )
                }

                // Active Mode Pill (Fast vs Deep Reasoning)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (enableHighThinking) RecallVioletPrimary.copy(alpha = 0.35f) else Color(0xFF1E293B).copy(alpha = 0.85f),
                    border = BorderStroke(
                        1.dp,
                        if (enableHighThinking) RecallVioletPrimary else Color.White.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { viewModel.enableHighThinking.value = !enableHighThinking }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "Reasoning Mode",
                            tint = if (enableHighThinking) RecallVioletPrimary else RecallCyanSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (enableHighThinking) "Deep" else "Fast",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                MemoraeCircleIconButton(
                    icon = Icons.Default.DeleteSweep,
                    onClick = { viewModel.clearChatHistory() },
                    contentDescription = "Clear Chat",
                    size = 36.dp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Suggested Prompt Chips (Translucent White)
            if (chatMessages.size <= 1) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val prompts = listOf(
                        "What commitments or promises do I have open?",
                        "Summarize my active projects and deliverables",
                        "Who did I meet with recently and where?",
                        "What action items do I have pending?"
                    )
                    items(prompts) { p ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF1E293B).copy(alpha = 0.90f),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    inputText = p
                                    viewModel.sendChatMessage(p)
                                    inputText = ""
                                }
                        ) {
                            Text(
                                text = p,
                                fontSize = 12.sp,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            // 2. Dark Frosted Glass Chat Screen Container
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF0B1120).copy(alpha = 0.68f),
                border = BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.35f)),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(chatMessages, key = { it.id }) { msg ->
                        ChatMessageItem(
                            message = msg,
                            onCitationClick = { citation ->
                                citation.noteId?.let { id -> onNavigateToNote(id) }
                            },
                            onSpeak = { text ->
                                ttsEngine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_${msg.id}")
                            },
                            onSaveAsNote = { text ->
                                val title = text.lineSequence().firstOrNull()?.take(48)?.trim()?.ifBlank { "AI Concierge Insight" } ?: "AI Concierge Insight"
                                viewModel.saveFullNote(
                                    id = null,
                                    title = title,
                                    content = text,
                                    category = "AI Insights",
                                    linkedPersonIds = emptyList(),
                                    tags = listOf("ai-concierge", "synthesis")
                                )
                                viewModel.showFeedback("Saved AI answer as Note: \"$title\"")
                            },
                            onSaveAsCommitment = { text ->
                                val desc = text.lineSequence().firstOrNull()?.take(90)?.trim()?.ifBlank { text.take(90) } ?: text.take(90)
                                viewModel.saveCommitment(
                                    CommitmentEntity(
                                        description = desc,
                                        direction = "BY_ME"
                                    )
                                )
                            },
                            onSaveAsTask = { text ->
                                val taskTitle = text.lineSequence().firstOrNull()?.take(80)?.trim()?.ifBlank { text.take(80) } ?: text.take(80)
                                viewModel.createRichActionItem(
                                    task = taskTitle,
                                    priority = com.example.util.TaskPriority.HIGH,
                                    column = com.example.util.TaskBoardColumn.TODAY
                                )
                            },
                            onShareMessage = { text ->
                                AppIntegrationsHelper.shareNoteAsMarkdown(
                                    context = context,
                                    title = "Recall AI Concierge Synthesis",
                                    content = text,
                                    category = "AI Intelligence",
                                    tags = listOf("recall", "ai-concierge"),
                                    peopleNames = emptyList()
                                )
                            }
                        )
                    }

                    if (isQueryingAi) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF0F172A).copy(alpha = 0.95f),
                                border = BorderStroke(1.dp, RecallCyanSecondary),
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = RecallCyanSecondary
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (enableHighThinking) "Recall Concierge is reasoning through personal memories..." else "Recall Concierge is searching memory graph...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Natural-language date/reminder banner when user types a temporal query/command in chat
            naturalDateSuggestion?.let { suggestion ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF1E293B).copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔔 ${suggestion.formattedDateLabel}: ${suggestion.suggestedAction}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFDE68A),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF59E0B),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    viewModel.scheduleReminder(
                                        title = suggestion.suggestedAction,
                                        details = suggestion.originalText,
                                        timeMillis = suggestion.suggestedTimeMillis,
                                        recurrence = suggestion.recurrenceHint ?: "NONE"
                                    )
                                    inputText = ""
                                }
                                .testTag("chat_quick_schedule_reminder_btn")
                        ) {
                            Text(
                                text = "+ Schedule Alarm",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF050811),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // 3. Unified Dark Chat Input Box with Internal Top Taskbar Dropdowns
            var showPromptsDropdown by remember { mutableStateOf(false) }
            var showReasoningDropdown by remember { mutableStateOf(false) }
            var showActionsDropdown by remember { mutableStateOf(false) }

            UnifiedTaskbarInputBox(
                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                borderColor = Color(0xFF334155),
                modifier = Modifier.fillMaxWidth(),
                taskbarContent = {
                    // (A) Quick Prompts Dropdown
                    Box {
                        TaskbarDropdownButton(
                            label = "Quick Prompts",
                            icon = Icons.Default.AutoAwesome,
                            accentColor = RecallVioletPrimary,
                            onClick = { showPromptsDropdown = true }
                        )
                        DropdownMenu(
                            expanded = showPromptsDropdown,
                            onDismissRequest = { showPromptsDropdown = false },
                            modifier = Modifier
                                .background(Color(0xFF0F172A))
                                .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                        ) {
                            listOf(
                                "What commitments or promises do I have open?",
                                "Summarize my active projects and deliverables",
                                "Who did I meet with recently and where?",
                                "What action items do I have pending?",
                                "What key dates should I remember?"
                            ).forEach { prompt ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = prompt,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp
                                        )
                                    },
                                    onClick = {
                                        showPromptsDropdown = false
                                        inputText = prompt
                                    }
                                )
                            }
                        }
                    }

                    // (B) Tone Dropdown inside Chat Input Taskbar
                    Box {
                        TaskbarDropdownButton(
                            label = "Tone: $chatbotTone",
                            accentColor = Color(0xFF0284C7),
                            dotColor = Color(0xFF0284C7),
                            onClick = { toneDropdownExpanded = true },
                            modifier = Modifier.testTag("chat_tone_dropdown")
                        )
                        DropdownMenu(
                            expanded = toneDropdownExpanded,
                            onDismissRequest = { toneDropdownExpanded = false },
                            modifier = Modifier
                                .background(Color(0xFF0F172A))
                                .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                        ) {
                            listOf("Conversational", "Concise", "Formal").forEach { tone ->
                                val isSelected = chatbotTone == tone
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = tone,
                                            color = Color.White,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                        )
                                    },
                                    trailingIcon = {
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = RecallVioletPrimary, modifier = Modifier.size(16.dp))
                                        }
                                    },
                                    onClick = {
                                        viewModel.chatbotTone.value = tone
                                        toneDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // (C) Reasoning Depth Dropdown
                    Box {
                        TaskbarDropdownButton(
                            label = if (enableHighThinking) "Deep Reasoning" else "Fast Recall",
                            icon = Icons.Default.Psychology,
                            accentColor = if (enableHighThinking) RecallVioletPrimary else Color(0xFF0D9488),
                            onClick = { showReasoningDropdown = true }
                        )
                        DropdownMenu(
                            expanded = showReasoningDropdown,
                            onDismissRequest = { showReasoningDropdown = false },
                            modifier = Modifier
                                .background(Color(0xFF0F172A))
                                .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "⚡ Fast Recall (Instant)",
                                        color = Color.White,
                                        fontWeight = if (!enableHighThinking) FontWeight.ExtraBold else FontWeight.Medium
                                    )
                                },
                                trailingIcon = {
                                    if (!enableHighThinking) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF0D9488), modifier = Modifier.size(16.dp))
                                    }
                                },
                                onClick = {
                                    viewModel.enableHighThinking.value = false
                                    showReasoningDropdown = false
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "🧠 Deep Reasoning (Multi-step)",
                                        color = RecallVioletPrimary,
                                        fontWeight = if (enableHighThinking) FontWeight.ExtraBold else FontWeight.Medium
                                    )
                                },
                                trailingIcon = {
                                    if (enableHighThinking) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = RecallVioletPrimary, modifier = Modifier.size(16.dp))
                                    }
                                },
                                onClick = {
                                    viewModel.enableHighThinking.value = true
                                    showReasoningDropdown = false
                                }
                            )
                        }
                    }

                    // (D) Voice & Input Options Dropdown
                    Box {
                        TaskbarDropdownButton(
                            label = "Options",
                            icon = Icons.Default.Mic,
                            accentColor = Color(0xFF475569),
                            onClick = { showActionsDropdown = true }
                        )
                        DropdownMenu(
                            expanded = showActionsDropdown,
                            onDismissRequest = { showActionsDropdown = false },
                            modifier = Modifier
                                .background(Color(0xFF0F172A))
                                .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (isDictatingQuery) "⏹️ Stop Voice Input" else "🎙️ Start Voice Input",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                onClick = {
                                    showActionsDropdown = false
                                    toggleVoiceDictationSafely()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("🧹 Clear Input Box", color = Color.White, fontWeight = FontWeight.Medium) },
                                onClick = {
                                    showActionsDropdown = false
                                    inputText = ""
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("🗑️ Clear Chat Conversation", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold) },
                                onClick = {
                                    showActionsDropdown = false
                                    viewModel.clearChatHistory()
                                }
                            )
                        }
                    }
                },
                bodyContent = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    "Ask your memory concierge...",
                                    color = Color(0xFF94A3B8)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_input_field"),
                            maxLines = 4,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            ),
                            colors = seamlessInnerTextFieldColors()
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        // Voice Dictation Button in Chat Dock
                        IconButton(
                            onClick = { toggleVoiceDictationSafely() },
                            modifier = Modifier
                                .size(42.dp)
                                .background(
                                    if (isDictatingQuery) Color(0xFFEF4444) else RecallVioletPrimary.copy(alpha = 0.14f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = if (isDictatingQuery) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = "Voice Input",
                                tint = if (isDictatingQuery) Color.White else RecallVioletPrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = {
                                val text = inputText
                                inputText = ""
                                viewModel.sendChatMessage(text)
                            },
                            enabled = inputText.isNotBlank() && !isQueryingAi,
                            modifier = Modifier
                                .size(46.dp)
                                .background(
                                    if (inputText.isNotBlank() && !isQueryingAi) RecallVioletPrimary
                                    else Color(0xFF1E293B),
                                    CircleShape
                                )
                                .testTag("chat_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (inputText.isNotBlank() && !isQueryingAi) Color.White else Color(0xFF64748B)
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    onCitationClick: (Citation) -> Unit,
    onSpeak: (String) -> Unit,
    onSaveAsNote: (String) -> Unit = {},
    onSaveAsCommitment: (String) -> Unit = {},
    onSaveAsTask: (String) -> Unit = {},
    onShareMessage: (String) -> Unit = {}
) {
    val isUser = message.sender == MessageSender.USER

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Translucent White Bubble Container (White card for user, frosted dark glass for AI)
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            color = if (isUser) Color.White.copy(alpha = 0.94f)
            else Color(0xFF0F172A).copy(alpha = 0.68f),
            border = BorderStroke(
                width = 1.dp,
                color = if (isUser) Color.White.copy(alpha = 0.60f) else Color.White.copy(alpha = 0.22f)
            ),
            shadowElevation = 2.dp,
            modifier = Modifier.widthIn(max = 325.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (!isUser) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = RecallVioletPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Recall Concierge",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = RecallVioletPrimary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { onSpeak(message.text) },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Read aloud",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF94A3B8)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isUser) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isUser) Color(0xFF0F172A) else Color(0xFFF1F5F9)
                )
            }
        }

        // Interactive Citation Chips Underneath AI Bubble (Dark Surface)
        if (!isUser && message.citations.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(message.citations) { citation ->
                    AssistChip(
                        onClick = { onCitationClick(citation) },
                        label = {
                            Text(
                                "Source: ${citation.noteTitle}",
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = RecallVioletPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color(0xFF1E293B)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.height(28.dp)
                    )
                }
            }
        }

        // 1-Tap Conversational Action Bridge under AI Responses
        if (!isUser && message.text.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E293B).copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSaveAsNote(message.text) }
                            .testTag("chat_action_save_note_${message.id}")
                    ) {
                        Text(
                            text = "📌 Save Note",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = RecallCyanSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E293B).copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, RecallVioletPrimary.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSaveAsCommitment(message.text) }
                            .testTag("chat_action_save_promise_${message.id}")
                    ) {
                        Text(
                            text = "🤝 + Promise",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E293B).copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, RecallEmeraldAccent.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSaveAsTask(message.text) }
                            .testTag("chat_action_save_task_${message.id}")
                    ) {
                        Text(
                            text = "✅ + Task",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = RecallEmeraldAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E293B).copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, Color(0xFF475569)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onShareMessage(message.text) }
                            .testTag("chat_action_share_${message.id}")
                    ) {
                        Text(
                            text = "📤 Share",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCBD5E1),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

