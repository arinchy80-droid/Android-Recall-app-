package com.example.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.example.ui.theme.NeuralAppBackground
import com.example.ui.theme.RecallCyanSecondary
import com.example.ui.theme.RecallVioletPrimary
import com.example.ui.theme.TaskbarDropdownButton
import com.example.ui.theme.UnifiedTaskbarInputBox
import com.example.ui.theme.seamlessInnerTextFieldColors

data class GuideModule(
    val number: Int,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val steps: List<String>,
    val proTip: String
)

data class FaqItem(
    val id: Int,
    val category: String,
    val question: String,
    val answer: String
)

private val guideModules = listOf(
    GuideModule(
        number = 1,
        title = "Quick Start & 6 Bottom Tabs",
        subtitle = "Learn your way around Recall in under 60 seconds",
        icon = Icons.Default.ViewStream,
        accentColor = RecallVioletPrimary,
        steps = listOf(
            "Stream Tab (Home): Your main feed of text notes, voice recordings, and photo memories, plus your interactive Calendar and Intelligence Hub.",
            "Reminders Tab: View, create, snooze, and complete alarms with custom physical tones and vibration patterns.",
            "People Tab: Your personal relationship manager (CRM) that builds living profiles, facts, and reminders for everyone you mention.",
            "Gallery Tab: A visual vault for photos, whiteboards, and receipts with automatic OCR text extraction.",
            "Ask AI Tab: Chat naturally with your notes and contacts—or speak updates that automatically revise older notes.",
            "Debrief Tab: Record or type a daily brain dump to automatically extract notes, update existing balances, log people facts, and create action items.",
            "Settings & Help (Top-Right Icons in Stream): Tap '?' anytime for this guide or the Gear icon to customize wallpapers, tones, security, and backups."
        ),
        proTip = "Badges on the Reminders and Debrief tabs show your active alarms and pending action items at a glance."
    ),
    GuideModule(
        number = 2,
        title = "Living Notes & Automatic Updates",
        subtitle = "Older notes automatically stay up to date when facts or numbers change",
        icon = Icons.Default.Refresh,
        accentColor = Color(0xFF10B981),
        steps = listOf(
            "Step 1 — Record an initial fact: Say or type 'I owe Jason 600 dollars' in a Note, Daily Debrief, or Ask AI. Recall saves a note titled 'Owe Jason $600' and links it to Jason's profile.",
            "Step 2 — Mention a change later: The next day, say or type 'I paid Jason $200' in a new Note, Daily Debrief, or Ask AI.",
            "Step 3 — Automatic Math & Fact Reconciliation: Recall finds your older note about Jason, subtracts $200 from $600, and updates the note in-place to '$400'!",
            "Step 4 — Full Audit Trail: Updated notes display a green '🔄 Updated' badge in the Stream and include a timestamped 'Update History' log at the bottom of the note so you never lose track of how a balance or schedule changed.",
            "Works for Schedules & Statuses Too: Saying 'Update: moved meeting with Jason to Friday at 4pm' updates the existing meeting note and linked person profile automatically."
        ),
        proTip = "During a Daily Debrief, any automatic updates to older notes are previewed in a green 'Living Note Updates' card before you tap Commit!"
    ),
    GuideModule(
        number = 3,
        title = "Memory Stream, Note Editor & Calendar",
        subtitle = "Create, color-code, geotag, pin, and filter memories effortlessly",
        icon = Icons.AutoMirrored.Filled.MenuBook,
        accentColor = RecallCyanSecondary,
        steps = listOf(
            "Tap the '+' button in the bottom-right corner of the Stream screen to create a Text Note, Voice Memory, Camera OCR snapshot, or Photo Gallery import.",
            "Inside the Note Editor, use the top taskbar inside the writing box to pick a Category, Ink Text Color (White, Cyan, Gold, Emerald, Rose, Violet), Geotag Location, or Reminder.",
            "Tap 'Polish' to clean up rough notes or 'Auto-Tag' to automatically detect categories, tags, and mentioned people.",
            "Tap 'Calendar' in the Stream search bar to expand the monthly Memory Calendar—days with memories show a violet dot and days with reminders show an amber dot.",
            "Swipe Right on any memory card in the Stream to Pin/Unpin it to the top, or Swipe Left to Delete it."
        ),
        proTip = "Tap '📊 Digest & Habits', '🕸️ Memory Graph', or '📍 Places' right below the Stream top bar to explore visual connections across all your notes."
    ),
    GuideModule(
        number = 4,
        title = "Reminders & Physical Tone Studio",
        subtitle = "Set custom acoustic alarms visible in both Reminders and Calendar",
        icon = Icons.Default.NotificationsActive,
        accentColor = Color(0xFFF59E0B),
        steps = listOf(
            "Open the 'Reminders' tab (or tap '+ Reminder' on the Stream calendar) to schedule an alarm for any upcoming task or note.",
            "Choose a quick time preset (In 15 mins, In 1 hour, Tonight 8 PM, Tomorrow 9 AM) or pick a custom date and time.",
            "Select from 4 synthesized physical acoustic tones: Digital Crystal, Ceramic Chime, Cosmic Ping, or Gentle Rain—tap the speaker icon to preview the sound live!",
            "Pick a tactile vibration cadence: Heartbeat Pulse, Double Tap, Crescendo Wave, or Silent.",
            "View all upcoming and completed alarms in the Reminders tab or click any date in the Stream Calendar to see reminders scheduled for that day."
        ),
        proTip = "You can test all 4 physical acoustic tones anytime in Settings under 'Physical Message Tone Studio'."
    ),
    GuideModule(
        number = 5,
        title = "Daily Debrief & Audio Playback",
        subtitle = "Speak freely, bookmark moments, play back at 1x–2x, and unpack with AI",
        icon = Icons.Default.Mic,
        accentColor = Color(0xFFF59E0B),
        steps = listOf(
            "Open the 'Debrief' tab and choose a Focus Mode from the input taskbar: Freestyle, Work & Standup, Social & People, or Mindful & Gratitude.",
            "Tap the central Microphone Orb to record continuous high-definition audio—or type directly in the text box.",
            "While recording, tap '📌 Pin Moment' to bookmark key timestamps you want to revisit.",
            "Play back your recording anytime with the built-in Audio Player featuring an interactive waveform and 1x / 1.25x / 1.5x / 2x speed control.",
            "Tap 'Unpack My Day' to review new notes, Living Note Updates (reconciled balances/facts), People profile updates, and Action Items before saving."
        ),
        proTip = "Tap 'Clean Up' inside the Debrief box to strip speech filler words ('um', 'uh', 'basically') and format your transcript before unpacking."
    ),
    GuideModule(
        number = 6,
        title = "People CRM, Pulse & Meeting Prep",
        subtitle = "Automatic contact profiles, catch-up alerts, and 1-tap briefings",
        icon = Icons.Default.People,
        accentColor = RecallVioletPrimary,
        steps = listOf(
            "Whenever you mention someone in a note or debrief, Recall automatically creates or updates their profile in the 'People' tab.",
            "Check the 'Relationship Pulse' banner at the top of the People tab to see who is overdue for a catch-up or has an upcoming birthday/milestone.",
            "Open any Person Dossier to set a custom Catch-Up Cadence (Every 7d, 14d, 30d, or 90d), add a profile photo, or log key facts and dates.",
            "Tap 'Meeting Prep' inside a Person Dossier to generate an instant cheat-sheet of past notes, open tasks, and conversation starters.",
            "Tap 'AI Synthesize' to scan all notes linked to that person and automatically extract new facts and updated balances."
        ),
        proTip = "Tap the Share icon inside any Person Dossier or Note to export a clean PDF report or Markdown document."
    ),
    GuideModule(
        number = 7,
        title = "Visual Memories Gallery & OCR",
        subtitle = "Snap photos of whiteboards, receipts, or documents with searchable text",
        icon = Icons.Default.Image,
        accentColor = RecallCyanSecondary,
        steps = listOf(
            "Open the 'Gallery' tab and tap the Camera button to snap a photo or choose 'Import Photo from Gallery' inside the taskbar.",
            "Recall automatically extracts visible text using Optical Character Recognition (OCR) and generates a searchable AI visual summary.",
            "Use the 'Filter' dropdown in the Gallery search bar to view All Media, Camera Captures, or Photos With OCR Text.",
            "Tap any photo card to view the full-resolution image, extracted OCR text, and jump straight to its linked note."
        ),
        proTip = "Because OCR text is indexed automatically, searching for a word written on a whiteboard or receipt will find the photo in both Stream and Gallery!"
    ),
    GuideModule(
        number = 8,
        title = "Ask AI Concierge & Smart Search",
        subtitle = "Get instant answers grounded in your notes with clickable citations",
        icon = Icons.Default.AutoAwesome,
        accentColor = Color(0xFFEC4899),
        steps = listOf(
            "Open the 'Ask AI' tab and pick a prompt or type/speak any question about your notes, people, balances, or schedule.",
            "Use the taskbar inside the chat box to switch Tone (Conversational, Concise, Formal) or toggle between Fast Recall and Deep Reasoning.",
            "Tap any clickable Source Citation pill under an AI answer to open the exact note it referenced, or tap the Speaker icon to hear the answer read aloud.",
            "You can also tell Ask AI about updates—like 'I paid Jason $200'—and it will update the older note for you immediately!"
        ),
        proTip = "On the Stream screen, toggle 'Mode: AI Smart Search' in the search box taskbar to search across Notes, Photos, People, and Tasks simultaneously."
    ),
    GuideModule(
        number = 9,
        title = "Settings, Themes, Security & Backups",
        subtitle = "Personalize your wallpaper, lock sensitive notes, and back up your data",
        icon = Icons.Default.Security,
        accentColor = Color(0xFF10B981),
        steps = listOf(
            "Wallpaper Themes: In Settings, choose between Twilight Cosmic, Nebula Violet, Cyber Ocean, OLED Midnight, or pick your own Custom Photo Wallpaper.",
            "Display & Haptics: Toggle 'Compact Stream Density' to fit more cards on screen, or toggle 'Tactile Haptic Feedback'.",
            "Evening Debrief Reminder: Choose what time (7 PM – 10 PM) Recall reminds you to unpack your day.",
            "Extracted Action Items: Filter, complete, add, or schedule tasks directly to your device/Google Calendar.",
            "Biometric Security & Backups: Enable 'Biometric Security Guard' for privacy, or use 'Export JSON' and 'Restore JSON' for complete offline backups."
        ),
        proTip = "100% of your database is stored locally on your device using Room SQLite + FTS4—your memories work offline anytime."
    )
)

private val faqItems = listOf(
    FaqItem(
        id = 1,
        category = "Living Notes",
        question = "How does Recall automatically update older notes when I mention new information?",
        answer = "Recall includes a Stateful Memory Reconciliation Engine ('Living Notes'). For example, if you create a note or debrief saying 'I owe Jason 600 dollars', Recall saves that note ($600). If you later say or type 'I paid Jason $200' (in a new note, Daily Debrief, or Ask AI), Recall automatically finds the original Jason note, deducts $200 so the note now reads '$400', adds a green '🔄 Updated' badge, appends a timestamped 'Update History' log, and updates Jason's profile facts."
    ),
    FaqItem(
        id = 2,
        category = "Living Notes",
        question = "Does automatic note updating work for schedules and general facts too?",
        answer = "Yes! Beyond math, balances, debts, and budgets, if you record an update such as 'Update: moved meeting with Jason for kitchen cabinets to Friday at 4pm', Recall locates the matching note, updates its content with the latest status, and records the previous details in the note's Update History section."
    ),
    FaqItem(
        id = 3,
        category = "Reminders & Calendar",
        question = "Where can I see the reminders I set?",
        answer = "Your reminders are available in two places: (1) Tap the dedicated 'Reminders' tab in the bottom navigation bar to view all upcoming and completed alarms, create new reminders, snooze (+10m / +1h), or mark them done. (2) On the 'Stream' tab, expand the Calendar—days with reminders display an amber dot, and tapping a date shows all reminders and memories for that day."
    ),
    FaqItem(
        id = 4,
        category = "Reminders & Tones",
        question = "How do Physical Message Tones and Vibration Patterns work?",
        answer = "Recall synthesizes 4 custom acoustic waveforms in real time—Digital Crystal, Ceramic Chime, Cosmic Ping, and Gentle Rain—paired with 4 tactile vibration patterns (Heartbeat Pulse, Double Tap, Crescendo Wave, Silent). You can preview and select them when creating any reminder or inside Settings under 'Physical Message Tone Studio'."
    ),
    FaqItem(
        id = 5,
        category = "Privacy & Storage",
        question = "Where is my data stored? Does Recall work offline?",
        answer = "100% of your notes, people profiles, reminders, action items, and media records are stored locally on your device inside an encrypted-ready Room SQLite database with FTS4 full-text indexing. If you are offline or cloud AI is unreachable, Recall automatically uses its built-in On-Device NLU & Reconciliation Engine so every feature keeps working."
    ),
    FaqItem(
        id = 6,
        category = "Daily Debrief",
        question = "How does Daily Debrief record, play back, and unpack my voice?",
        answer = "Tap the microphone orb in the Debrief tab to record continuous AAC audio into permanent on-device storage while live speech-to-text transcribes your words. You can pin key moments while recording, replay the audio at 1x–2x speed, and tap 'Unpack My Day' to preview extracted notes, Living Note Updates, people facts, and tasks before committing."
    ),
    FaqItem(
        id = 7,
        category = "People CRM",
        question = "How do Relationship Pulse, Catch-Up Cadence, and Meeting Prep work?",
        answer = "In the People tab, the Relationship Pulse banner highlights contacts who are overdue based on their custom Catch-Up Cadence (7, 14, 30, or 90 days) as well as upcoming birthdays. Inside any Person Dossier, tap 'Meeting Prep' to generate an executive briefing of past interactions, open tasks, and talking points."
    ),
    FaqItem(
        id = 8,
        category = "Intelligence Hub",
        question = "What are Digest & Habits, Memory Graph, and Places on the Stream screen?",
        answer = "Right below the Stream header, tap '📊 Digest & Habits' to track daily habits with streaks and view a 7-day activity summary; tap '🕸️ Memory Graph' to explore an interactive node graph connecting your categories, people, and notes; or tap '📍 Places' to filter notes by their geotagged locations."
    ),
    FaqItem(
        id = 9,
        category = "Search & Organization",
        question = "What is the difference between Keyword Search and AI Smart Search?",
        answer = "Keyword Search matches words in titles, bodies, and tags instantly. Switching the Stream search bar to 'Mode: AI Smart Search' understands natural questions (like 'Who did I meet last week?'), searches across Notes, Photos, People, and Tasks simultaneously, and writes an AI summary."
    ),
    FaqItem(
        id = 10,
        category = "Visual Memories",
        question = "Can I search for text inside photos, receipts, or whiteboards?",
        answer = "Yes! When you snap a photo or import an image in Stream or Gallery, Recall extracts visible text via OCR and generates a semantic description. Both the OCR text and visual tags are searchable across Stream, Gallery, and Ask AI."
    ),
    FaqItem(
        id = 11,
        category = "Note Editor",
        question = "How do I change text color, add a location, or attach a reminder to a note?",
        answer = "Open any note in the Note Editor and use the horizontal taskbar along the top of the writing box. You can pick a Category, choose from 6 high-contrast Ink Colors, tag a Location (Office, Home, Cafe, etc.), attach a Physical Tone Reminder, or run AI Polish and Auto-Tag."
    ),
    FaqItem(
        id = 12,
        category = "Themes & Display",
        question = "How do I change the background wallpaper or switch to Compact view?",
        answer = "Open Settings (the Gear icon in the top-right of Stream) and go to 'Appearance, Themes & Reminders'. You can pick from 4 built-in cosmic themes or select any photo from your gallery as a Custom Wallpaper, and toggle 'Compact Stream Density' or 'Tactile Haptic Feedback'."
    ),
    FaqItem(
        id = 13,
        category = "Backup & Export",
        question = "How do I export a PDF/Markdown report or back up my entire database?",
        answer = "Inside any Note or Person Dossier, tap the Share icon in the top bar to export a formatted PDF document or Markdown text. For a full database backup, open Settings -> 'Data Backup, Restore & Security' and tap 'Export JSON' or 'Restore JSON'."
    ),
    FaqItem(
        id = 14,
        category = "Permissions",
        question = "Which permissions does Recall request and why?",
        answer = "Recall requests Microphone permission only when you start a voice recording or voice search, and Camera permission only when you take a photo. Selecting existing gallery photos or custom wallpapers uses the zero-permission Android Photo Picker."
    )
)

@Composable
fun HelpAndFaqScreen(
    initialTab: Int = 0,
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var searchFilter by remember { mutableStateOf("") }
    val expandedFaqIds = remember { mutableStateListOf<Int>(1, 3, 5) }

    val filteredModules = remember(searchFilter) {
        if (searchFilter.isBlank()) guideModules
        else {
            val q = searchFilter.trim().lowercase()
            guideModules.filter { m ->
                m.title.lowercase().contains(q) ||
                    m.subtitle.lowercase().contains(q) ||
                    m.proTip.lowercase().contains(q) ||
                    m.steps.any { it.lowercase().contains(q) }
            }
        }
    }

    val filteredFaqs = remember(searchFilter) {
        if (searchFilter.isBlank()) faqItems
        else {
            val q = searchFilter.trim().lowercase()
            faqItems.filter { f ->
                f.question.lowercase().contains(q) ||
                    f.answer.lowercase().contains(q) ||
                    f.category.lowercase().contains(q)
            }
        }
    }

    NeuralAppBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("help_and_faq_list"),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 56.dp)
        ) {
            // Top Header
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("close_help_guide_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Help, User Guide & FAQ",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Simple step-by-step guide to every feature & setting",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }
            }

            // Unified Search Bar inside Help & FAQ with Internal Top Taskbar Dropdowns
            item {
                var showSectionDropdown by remember { mutableStateOf(false) }
                var showTopicDropdown by remember { mutableStateOf(false) }

                UnifiedTaskbarInputBox(
                    shape = RoundedCornerShape(18.dp),
                    borderColor = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                    taskbarContent = {
                        // (A) Section View Dropdown
                        Box {
                            TaskbarDropdownButton(
                                label = if (selectedTab == 0) "View: User Guide" else "View: FAQ",
                                icon = if (selectedTab == 0) Icons.AutoMirrored.Filled.MenuBook else Icons.Default.QuestionAnswer,
                                accentColor = RecallVioletPrimary,
                                onClick = { showSectionDropdown = true }
                            )
                            DropdownMenu(
                                expanded = showSectionDropdown,
                                onDismissRequest = { showSectionDropdown = false },
                                properties = PopupProperties(clippingEnabled = true),
                                modifier = Modifier
                                    .background(Color(0xFF0F172A))
                                    .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("📖 Step-by-Step User Guide (9 Modules)", color = Color.White, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        selectedTab = 0
                                        showSectionDropdown = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("❓ Frequently Asked Questions (14 FAQs)", color = Color.White, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        selectedTab = 1
                                        showSectionDropdown = false
                                    }
                                )
                            }
                        }

                        // (B) Quick Topic Filter Dropdown
                        Box {
                            TaskbarDropdownButton(
                                label = if (searchFilter.isBlank()) "Topic: All" else "Topic: $searchFilter",
                                icon = Icons.Default.Lightbulb,
                                accentColor = Color(0xFF0284C7),
                                onClick = { showTopicDropdown = true }
                            )
                            DropdownMenu(
                                expanded = showTopicDropdown,
                                onDismissRequest = { showTopicDropdown = false },
                                properties = PopupProperties(clippingEnabled = true),
                                modifier = Modifier
                                    .background(Color(0xFF0F172A))
                                    .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All Topics", color = RecallVioletPrimary, fontWeight = FontWeight.ExtraBold) },
                                    onClick = {
                                        searchFilter = ""
                                        showTopicDropdown = false
                                    }
                                )
                                listOf(
                                    "Living Notes",
                                    "Reminders",
                                    "Daily Debrief",
                                    "People",
                                    "Ask AI",
                                    "Camera",
                                    "Settings",
                                    "Backup"
                                ).forEach { topic ->
                                    DropdownMenuItem(
                                        text = { Text("🔍 $topic", color = Color.White, fontWeight = FontWeight.SemiBold) },
                                        onClick = {
                                            searchFilter = topic
                                            showTopicDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    },
                    bodyContent = {
                        OutlinedTextField(
                            value = searchFilter,
                            onValueChange = { searchFilter = it },
                            placeholder = {
                                Text(
                                    "Search guide topics or FAQ questions...",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 13.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search Help",
                                    tint = RecallVioletPrimary
                                )
                            },
                            trailingIcon = {
                                if (searchFilter.isNotEmpty()) {
                                    IconButton(onClick = { searchFilter = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear search",
                                            tint = Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            ),
                            colors = seamlessInnerTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("help_search_input")
                        )
                    }
                )
            }

            // Segmented Tab Switcher (How to Use Guide vs FAQ)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0F172A).copy(alpha = 0.88f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val isGuideSelected = selectedTab == 0
                    Button(
                        onClick = { selectedTab = 0 },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isGuideSelected) RecallVioletPrimary else Color.Transparent,
                            contentColor = if (isGuideSelected) Color.White else Color(0xFFCBD5E1)
                        ),
                        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("help_tab_guide")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "User Guide (${filteredModules.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    val isFaqSelected = selectedTab == 1
                    Button(
                        onClick = { selectedTab = 1 },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFaqSelected) RecallCyanSecondary else Color.Transparent,
                            contentColor = if (isFaqSelected) Color(0xFF040711) else Color(0xFFCBD5E1)
                        ),
                        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("help_tab_faq")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QuestionAnswer,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "FAQ (${filteredFaqs.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            if (selectedTab == 0) {
                // TAB 0: COMPREHENSIVE STEP-BY-STEP USER GUIDE
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = RecallVioletPrimary.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, RecallVioletPrimary.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Welcome to Recall! Follow these 9 simple modules to master Living Notes, Reminders, Voice Debriefs, People profiles, and all Settings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                if (filteredModules.isEmpty()) {
                    item {
                        EmptyHelpSearchPlaceholder(searchFilter) { searchFilter = "" }
                    }
                } else {
                    itemsIndexed(filteredModules, key = { _, item -> "guide_${item.number}" }) { _, module ->
                        GuideModuleCard(module = module)
                    }
                }

                // Quick switch footer to FAQ
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.90f)),
                        border = BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Still have a specific question?",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Browse ${faqItems.size} detailed answers in the FAQ tab.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = { selectedTab = 1 },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RecallCyanSecondary,
                                    contentColor = Color(0xFF040711)
                                )
                            ) {
                                Text("View FAQ", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // TAB 1: FREQUENTLY ASKED QUESTIONS (FAQ)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tap any question to expand the answer",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE2E8F0)
                        )
                        val allExpanded = expandedFaqIds.size == filteredFaqs.size && filteredFaqs.isNotEmpty()
                        TextButton(
                            onClick = {
                                if (allExpanded) {
                                    expandedFaqIds.clear()
                                } else {
                                    expandedFaqIds.clear()
                                    expandedFaqIds.addAll(filteredFaqs.map { it.id })
                                }
                            },
                            modifier = Modifier.testTag("faq_expand_collapse_all_button")
                        ) {
                            Text(
                                text = if (allExpanded) "Collapse All" else "Expand All",
                                color = RecallCyanSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                if (filteredFaqs.isEmpty()) {
                    item {
                        EmptyHelpSearchPlaceholder(searchFilter) { searchFilter = "" }
                    }
                } else {
                    itemsIndexed(filteredFaqs, key = { _, item -> "faq_${item.id}" }) { _, faq ->
                        val isExpanded = expandedFaqIds.contains(faq.id)
                        FaqAccordionCard(
                            faq = faq,
                            isExpanded = isExpanded,
                            onToggle = {
                                if (isExpanded) expandedFaqIds.remove(faq.id)
                                else expandedFaqIds.add(faq.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideModuleCard(module: GuideModule) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("guide_module_card_${module.number}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.88f)),
        border = BorderStroke(1.dp, module.accentColor.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(module.accentColor.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = module.icon,
                        contentDescription = null,
                        tint = module.accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MODULE ${module.number}",
                        style = MaterialTheme.typography.labelSmall,
                        color = module.accentColor,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = module.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = module.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFF334155).copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(12.dp))

            module.steps.forEachIndexed { idx, stepText ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        shape = CircleShape,
                        color = module.accentColor.copy(alpha = 0.25f),
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${idx + 1}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stepText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFF1F5F9),
                        lineHeight = 20.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1E293B).copy(alpha = 0.90f),
                border = BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier
                            .padding(top = 1.dp)
                            .size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PRO TIP: ${module.proTip}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFDE68A),
                        fontWeight = FontWeight.Medium,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun FaqAccordionCard(
    faq: FaqItem,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .testTag("faq_item_card_${faq.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) {
                Color(0xFF0F172A).copy(alpha = 0.94f)
            } else {
                Color(0xFF0F172A).copy(alpha = 0.85f)
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isExpanded) RecallCyanSecondary.copy(alpha = 0.7f) else Color(0xFF334155).copy(alpha = 0.65f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = RecallCyanSecondary.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = faq.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = RecallCyanSecondary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse answer" else "Expand answer",
                    tint = if (isExpanded) RecallCyanSecondary else Color(0xFFCBD5E1)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = faq.question,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                lineHeight = 20.sp
            )

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0xFF334155).copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = faq.answer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE2E8F0),
                        lineHeight = 21.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyHelpSearchPlaceholder(
    query: String,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.88f)),
        border = BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                contentDescription = null,
                tint = RecallCyanSecondary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No help topics matching \"$query\"",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Try searching for keywords like 'Living Notes', 'Reminders', 'Debrief', 'People', 'OCR', or 'Backup'.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFCBD5E1)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onClear,
                colors = ButtonDefaults.buttonColors(containerColor = RecallVioletPrimary)
            ) {
                Text("Reset Search Filter", color = Color.White)
            }
        }
    }
}
