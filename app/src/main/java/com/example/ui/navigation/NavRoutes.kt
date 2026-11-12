package com.example.ui.navigation

sealed class Screen(val route: String, val title: String) {
    // Primary Memorae-inspired Information Architecture (Section 3.3)
    data object Home : Screen("home", "Home")
    data object Capture : Screen("capture", "Capture")
    data object Memories : Screen("memories", "Memories")
    data object AiIntelligence : Screen("ai_intelligence", "AI")
    data object Organise : Screen("organise", "Organise")
    data object Settings : Screen("settings", "Profile & Settings")

    // Legacy / Direct Screen Routes (preserved for 100% backward compatibility)
    data object Stream : Screen("stream", "Stream")
    data object Reminders : Screen("reminders", "Reminders")
    data object People : Screen("people", "People")
    data object Gallery : Screen("gallery", "Gallery")
    data object Debrief : Screen("debrief", "Debrief")
    data object AskAi : Screen("ask_ai", "Ask AI")
}
