package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class MemoryCategory(
    val displayName: String,
    val description: String,
    val color: Color,
    val icon: ImageVector
) {
    MEETING("Meeting", "Discussions, 1-on-1s & alignments", Color(0xFF6366F1), Icons.Default.Groups),
    WORK("Work", "Tasks, company strategy & operations", Color(0xFF3B82F6), Icons.Default.Work),
    PROJECTS("Projects", "Sprint milestones & product roadmap", Color(0xFF8B5CF6), Icons.Default.Folder),
    IDEAS("Ideas", "Inventions, brainstorms & insights", Color(0xFFF59E0B), Icons.Default.Lightbulb),
    PERSONAL("Personal", "Life events, reflections & personal notes", Color(0xFF10B981), Icons.Default.Person),
    FINANCE("Finance", "Budgets, pricing terms & contracts", Color(0xFF059669), Icons.Default.Payment),
    HEALTH("Health", "Wellness, medical notes & fitness", Color(0xFFEC4899), Icons.Default.Favorite),
    TRAVEL("Travel", "Flights, packing lists & itineraries", Color(0xFF06B6D4), Icons.Default.Flight),
    FAMILY("Family", "Kids, birthdays & family gatherings", Color(0xFFF97316), Icons.Default.Home),
    LEARNING("Learning", "Book takeaways, papers & research", Color(0xFF7C3AED), Icons.AutoMirrored.Filled.MenuBook),
    JOURNAL("Journal", "Evening debriefs & stream of thoughts", Color(0xFF14B8A6), Icons.Default.Bookmark),
    VISUALS("Visuals", "Whiteboard diagrams & camera captures", Color(0xFF00E5FF), Icons.Default.CameraAlt),
    CLIENTS("Clients", "Client accounts, briefs & deliverables", Color(0xFF38BDF8), Icons.Default.Business),
    CODE_TECH("Code & Tech", "Architecture snippets, tools & configs", Color(0xFF64748B), Icons.Default.Code),
    SHOPPING("Shopping", "Wishlists, purchases & recommendations", Color(0xFFFB7185), Icons.Default.ShoppingBag),
    ENTERTAINMENT("Culture & Media", "Books, movies, podcasts & music", Color(0xFFA855F7), Icons.Default.Movie),
    HABITS("Habits", "Daily streaks, routines & rituals", Color(0xFF22C55E), Icons.Default.Repeat),
    HOMELIFE("Home & Living", "House projects, repairs & chores", Color(0xFFF59E0B), Icons.Default.Home),
    GENERAL("General", "Uncategorized episodic notes", Color(0xFF94A3B8), Icons.Default.Bookmark);

    companion object {
        val allCategories = values().toList()
        val allDisplayNames = values().map { it.displayName }

        fun fromString(name: String): MemoryCategory {
            return values().find { it.displayName.equals(name, ignoreCase = true) }
                ?: values().find { it.name.equals(name, ignoreCase = true) }
                ?: GENERAL
        }

        fun getColor(categoryName: String): Color {
            return fromString(categoryName).color
        }

        fun getIcon(categoryName: String): ImageVector {
            return fromString(categoryName).icon
        }
    }
}
