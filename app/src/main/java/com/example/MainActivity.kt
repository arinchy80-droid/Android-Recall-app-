package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.RecallViewModel
import com.example.ui.chat.AskAiScreen
import com.example.ui.chat.MemoraeAiIntelligenceHubScreen
import com.example.ui.debrief.DailyDebriefScreen
import com.example.ui.debrief.MemoraeCaptureHubScreen
import com.example.ui.editor.NoteEditorScreen
import com.example.ui.gallery.GalleryScreen
import com.example.ui.home.MemoraeHomeDashboardScreen
import com.example.ui.memories.MemoraeMemoriesHubScreen
import com.example.ui.navigation.Screen
import com.example.ui.organise.MemoraeOrganiseHubScreen
import com.example.ui.people.PeopleScreen
import com.example.ui.reminders.RemindersScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.stream.StreamScreen
import com.example.ui.theme.AppVisualPreferences
import com.example.ui.theme.RecallCyanSecondary
import com.example.ui.theme.RecallTheme
import com.example.ui.theme.RecallVioletPrimary
import com.example.util.NotificationHelper

class MainActivity : ComponentActivity() {

    private val viewModel: RecallViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyLockScreenWakeFlags(intent)
        enableEdgeToEdge()

        try {
            AppVisualPreferences.ensureInitialized(this)
            com.example.util.PrivacySecurityManager.ensureInitialized(this)
            // Schedule or restore user's configured daily debrief reminder alarm
            NotificationHelper.rescheduleSavedDailyDebriefAlarm(this)
        } catch (t: Throwable) {
            // Safe alarm scheduling fallback
        }

        // Check if launched from notification intent
        val startDestination = when (intent?.getStringExtra("navigate_to")) {
            "debrief" -> {
                viewModel.navigateToHubSection(Screen.Capture, "Daily Debrief")
                Screen.Capture
            }
            "reminders" -> {
                viewModel.navigateToHubSection(Screen.Organise, "Reminders")
                Screen.Organise
            }
            "stream" -> Screen.Memories
            else -> Screen.Home
        }

        val openNoteId = intent?.getStringExtra("open_note_id")
        if (!openNoteId.isNullOrBlank()) {
            viewModel.openNoteById(openNoteId)
        }

        setContent {
            RecallTheme {
                MainAppScaffold(
                    viewModel = viewModel,
                    initialScreen = startDestination
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyLockScreenWakeFlags(intent)

        when (intent.getStringExtra("navigate_to")) {
            "debrief" -> viewModel.navigateToHubSection(Screen.Capture, "Daily Debrief")
            "reminders" -> viewModel.navigateToHubSection(Screen.Organise, "Reminders")
            "stream" -> viewModel.navigateToHubSection(Screen.Memories, "All Memories")
            "home" -> viewModel.requestScreenNavigation(Screen.Home)
        }

        val openNoteId = intent.getStringExtra("open_note_id")
        if (!openNoteId.isNullOrBlank()) {
            viewModel.openNoteById(openNoteId)
        }
    }

    private fun applyLockScreenWakeFlags(currentIntent: Intent?) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            } else {
                @Suppress("DEPRECATION")
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                )
            }
            if (currentIntent?.getBooleanExtra("from_alarm_or_reminder", false) == true) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        } catch (_: Throwable) {
        }
    }
}

@Composable
fun MainAppScaffold(
    viewModel: RecallViewModel,
    initialScreen: Screen = Screen.Home
) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf<Screen>(initialScreen) }
    val snackbarHostState = remember { SnackbarHostState() }

    val pendingNavigation by viewModel.pendingScreenNavigation.collectAsStateWithLifecycle()
    val userFeedback by viewModel.userFeedbackMessage.collectAsStateWithLifecycle()
    val pendingActionCount by viewModel.pendingActionCount.collectAsStateWithLifecycle()
    val activeReminders by viewModel.activeReminders.collectAsStateWithLifecycle()

    val isNoteEditorOpen by viewModel.isNoteEditorOpen.collectAsStateWithLifecycle()
    val noteBeingEdited by viewModel.noteBeingEdited.collectAsStateWithLifecycle()
    val allPeople by viewModel.people.collectAsStateWithLifecycle()

    val isAppLockEnabled by com.example.util.PrivacySecurityManager.isAppLockEnabled.collectAsStateWithLifecycle()
    val isSessionUnlocked by com.example.util.PrivacySecurityManager.isSessionUnlocked.collectAsStateWithLifecycle()

    var showMadeByPopup by remember { mutableStateOf(true) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            NotificationHelper.rescheduleSavedDailyDebriefAlarm(context)
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPerm) {
                try {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } catch (_: Throwable) {
                }
            }
        }
        kotlinx.coroutines.delay(3000L)
        showMadeByPopup = false
    }

    LaunchedEffect(pendingNavigation) {
        pendingNavigation?.let { target ->
            currentScreen = target
            viewModel.consumeScreenNavigation()
        }
    }

    LaunchedEffect(userFeedback) {
        userFeedback?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissFeedback()
        }
    }

    if (currentScreen != Screen.Home && !isNoteEditorOpen) {
        BackHandler {
            currentScreen = Screen.Home
        }
    }

    com.example.ui.theme.NeuralAppBackground {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!isNoteEditorOpen) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Transparent)
                    ) {
                        // Universal Floating Quick-Capture Pill Dock (Screenshots 1, 2, 4)
                        if (currentScreen == Screen.Home || currentScreen == Screen.Memories || currentScreen == Screen.Organise) {
                            var quickDockText by remember { mutableStateOf("") }
                            val isRecordingDebrief by viewModel.isRecording.collectAsStateWithLifecycle()
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                com.example.ui.theme.MemoraeFloatingInputDock(
                                    value = quickDockText,
                                    onValueChange = { quickDockText = it },
                                    onSend = {
                                        val text = quickDockText.trim()
                                        if (text.isNotBlank()) {
                                            viewModel.saveFullNote(
                                                id = null,
                                                title = text.lineSequence().firstOrNull()?.take(40)?.trim() ?: text.take(40),
                                                content = text,
                                                category = "Quick Thoughts",
                                                linkedPersonIds = emptyList(),
                                                tags = listOf("quick-capture")
                                            )
                                            quickDockText = ""
                                            viewModel.showFeedback("Thought captured to Memories")
                                        }
                                    },
                                    onMicClick = {
                                        viewModel.captureSubTab.value = "Quick Capture"
                                        currentScreen = Screen.Capture
                                    },
                                    onPlusClick = {
                                        viewModel.openNoteEditor(null)
                                    },
                                    isRecording = isRecordingDebrief,
                                    placeholderText = "Type or record a new thought"
                                )
                            }
                        }

                        com.example.ui.theme.HighDefinitionNavBar(
                            modifier = Modifier.testTag("bottom_nav_bar")
                        ) {
                        // 1. HOME — Dashboard Command Centre
                        com.example.ui.theme.HighDefinitionTabItem(
                            selected = currentScreen == Screen.Home,
                            onClick = { currentScreen = Screen.Home },
                            icon = Icons.Default.Home,
                            label = "Home",
                            activeColor = RecallCyanSecondary,
                            testTag = "nav_home"
                        )

                        // 2. CAPTURE — Voice, Text, Camera, Daily Debrief
                        com.example.ui.theme.HighDefinitionTabItem(
                            selected = currentScreen == Screen.Capture || currentScreen == Screen.Debrief,
                            onClick = { currentScreen = Screen.Capture },
                            icon = Icons.Default.Mic,
                            label = "Capture",
                            activeColor = RecallVioletPrimary,
                            testTag = "nav_debrief"
                        )

                        // 3. MEMORIES — All Memories, People, Places, Events, Timeline, Graph, Docs
                        com.example.ui.theme.HighDefinitionTabItem(
                            selected = currentScreen == Screen.Memories ||
                                    currentScreen == Screen.Stream ||
                                    currentScreen == Screen.People ||
                                    currentScreen == Screen.Gallery,
                            onClick = { currentScreen = Screen.Memories },
                            icon = Icons.Default.ViewStream,
                            label = "Memories",
                            activeColor = RecallVioletPrimary,
                            testTag = "nav_stream"
                        )

                        // 4. AI INTELLIGENCE — Ask Recall, Life Rewind, Insights, Relationships, Search
                        com.example.ui.theme.HighDefinitionTabItem(
                            selected = currentScreen == Screen.AiIntelligence || currentScreen == Screen.AskAi,
                            onClick = { currentScreen = Screen.AiIntelligence },
                            icon = Icons.Default.AutoAwesome,
                            label = "AI",
                            activeColor = RecallCyanSecondary,
                            testTag = "nav_ask_ai"
                        )

                        // 5. ORGANISE — Commitments, Reminders, Kanban Tasks, Dynamic Lists, Projects, Calendar
                        com.example.ui.theme.HighDefinitionTabItem(
                            selected = currentScreen == Screen.Organise || currentScreen == Screen.Reminders,
                            onClick = { currentScreen = Screen.Organise },
                            icon = Icons.Default.Checklist,
                            label = "Organise",
                            activeColor = Color(0xFF10B981),
                            badgeCount = activeReminders.size + pendingActionCount,
                        )
                    }
                }
            }
        }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentScreen) {
                    Screen.Home -> MemoraeHomeDashboardScreen(
                        viewModel = viewModel,
                        onNavigateToScreen = { targetScreen, subTab ->
                            viewModel.navigateToHubSection(targetScreen, subTab)
                            currentScreen = targetScreen
                        },
                        onNavigateToPerson = { person ->
                            viewModel.selectPerson(person)
                            viewModel.navigateToHubSection(Screen.Memories, "People")
                            currentScreen = Screen.Memories
                        },
                        onNavigateToSettings = {
                            currentScreen = Screen.Settings
                        }
                    )

                    Screen.Capture, Screen.Debrief -> MemoraeCaptureHubScreen(
                        viewModel = viewModel
                    )

                    Screen.Memories, Screen.Stream -> MemoraeMemoriesHubScreen(
                        viewModel = viewModel,
                        onNavigateToScreen = { targetScreen, subTab ->
                            viewModel.navigateToHubSection(targetScreen, subTab)
                            currentScreen = targetScreen
                        },
                        onNavigateToSettings = {
                            currentScreen = Screen.Settings
                        }
                    )

                    Screen.AiIntelligence, Screen.AskAi -> MemoraeAiIntelligenceHubScreen(
                        viewModel = viewModel,
                        onNavigateToNote = { noteId -> viewModel.openNoteById(noteId) },
                        onNavigateToPerson = { person ->
                            viewModel.selectPerson(person)
                            viewModel.navigateToHubSection(Screen.Memories, "People")
                            currentScreen = Screen.Memories
                        }
                    )

                    Screen.Organise, Screen.Reminders -> MemoraeOrganiseHubScreen(
                        viewModel = viewModel,
                        onOpenNote = { noteId -> viewModel.openNoteById(noteId) }
                    )

                    Screen.People -> PeopleScreen(
                        viewModel = viewModel
                    )

                    Screen.Gallery -> GalleryScreen(
                        viewModel = viewModel,
                        onOpenNote = { noteId ->
                            viewModel.openNoteById(noteId)
                        }
                    )

                    Screen.Settings -> SettingsScreen(
                        viewModel = viewModel,
                        onBack = {
                            currentScreen = Screen.Home
                        }
                    )
                }

                // Note Detail / Editor full screen overlay
                if (isNoteEditorOpen) {
                    NoteEditorScreen(
                        viewModel = viewModel,
                        noteWithDetails = noteBeingEdited,
                        allPeople = allPeople,
                        onClose = { viewModel.closeNoteEditor() }
                    )
                }

                // Quick 3-second fresh launch pop-up message in a clear text box
                androidx.compose.animation.AnimatedVisibility(
                    visible = showMadeByPopup,
                    enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(initialScale = 0.92f),
                    exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(targetScale = 0.95f),
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.Center)
                        .padding(horizontal = 32.dp)
                ) {
                    androidx.compose.material3.Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.95f),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    RecallCyanSecondary,
                                    Color(0xFF166093),
                                    RecallVioletPrimary
                                )
                            )
                        ),
                        shadowElevation = 12.dp,
                        modifier = Modifier.testTag("made_by_aaron_popup")
                    ) {
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                        ) {
                            com.example.ui.theme.NeonBrainIcon(size = 30.dp)
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(start = 12.dp))
                            Text(
                                text = "Made by Aaron Lissa\nPerth S and M AI Innovations",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Phase 7: Persistent PIN Vault Lock Gate Overlay
                if (isAppLockEnabled && !isSessionUnlocked) {
                    RecallVaultLockOverlay(
                        onUnlockSuccess = {
                            viewModel.showFeedback("🔓 Recall Vault unlocked")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecallVaultLockOverlay(
    onUnlockSuccess: () -> Unit
) {
    val context = LocalContext.current
    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    com.example.ui.theme.NeuralAppBackground(
        modifier = Modifier
            .fillMaxSize()
            .testTag("vault_pin_lock_overlay")
    ) {
        androidx.compose.material3.Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF050811).copy(alpha = 0.45f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                androidx.compose.material3.Card(
                    modifier = Modifier.fillMaxSize(0.92f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = Color(0xFF0F172A).copy(alpha = 0.85f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        Brush.linearGradient(listOf(RecallCyanSecondary, RecallVioletPrimary))
                    )
                ) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                    ) {
                        com.example.ui.theme.NeonBrainIcon(size = 56.dp)
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 14.dp))
                        Text(
                            text = "🔒 Recall Memory Vault Locked",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 6.dp))
                        Text(
                            text = "Enter your 4-digit security PIN to access your personal memories (default: 1234).",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 18.dp))
                        androidx.compose.material3.OutlinedTextField(
                            value = pinInput,
                            onValueChange = { next ->
                                if (next.length <= 8 && next.all { it.isDigit() }) {
                                    pinInput = next
                                    errorMessage = null
                                }
                            },
                            label = { Text("Security PIN", color = RecallCyanSecondary) },
                            singleLine = true,
                            modifier = Modifier.testTag("vault_pin_input")
                        )
                        if (errorMessage != null) {
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 6.dp))
                            Text(
                                text = errorMessage!!,
                                color = Color(0xFFEF4444),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 16.dp))
                        androidx.compose.material3.Button(
                            onClick = {
                                if (com.example.util.PrivacySecurityManager.unlockSession(context, pinInput)) {
                                    pinInput = ""
                                    errorMessage = null
                                    onUnlockSuccess()
                                } else {
                                    errorMessage = "Incorrect PIN. Try again."
                                }
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = RecallVioletPrimary
                            ),
                            modifier = Modifier.testTag("vault_unlock_button")
                        ) {
                            Text("Unlock Vault", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

