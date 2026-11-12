package com.example.data.model

/**
 * The 12 canonical Personal Memory categories defined in the Recall Unified Memory System specification.
 * Operates alongside the 19 UI display categories in [MemoryCategory] with automatic bidirectional mapping.
 */
enum class UnifiedMemoryType(
    val displayName: String,
    val description: String,
    val iconEmoji: String
) {
    PEOPLE("People", "Profiles, relationships, personal facts & interactions", "👤"),
    PLACES("Places", "Locations, venues, travel spots & geographic context", "📍"),
    EVENTS("Events", "Meetings, gatherings, milestones & occurrences", "📅"),
    CONVERSATIONS("Conversations", "Spoken debriefs, dialogues, 1-on-1s & discussions", "🎙️"),
    IDEAS("Ideas", "Brainstorms, inventions, creative concepts & insights", "💡"),
    TASKS("Tasks", "To-dos, action items & operational checklists", "✅"),
    COMMITMENTS("Commitments", "Promises, obligations, debts & deliverables owed by or to the user", "🤝"),
    PROJECTS("Projects", "Ongoing initiatives, roadmaps & multi-step endeavors", "🚀"),
    EXPERIENCES("Experiences", "Journeys, stories, cultural moments & life memories", "🌟"),
    PREFERENCES("Preferences", "Likes, dislikes, favorites, routines & personal tastes", "❤️"),
    IMPORTANT_DATES("Important Dates", "Birthdays, anniversaries, deadlines & recurring milestones", "🎂"),
    PERSONAL_OBSERVATIONS("Personal Observations", "Reflections, journal entries, learnings & general notes", "📝");

    companion object {
        fun fromString(value: String?): UnifiedMemoryType {
            if (value.isNullOrBlank()) return PERSONAL_OBSERVATIONS
            return values().find {
                it.name.equals(value.trim(), ignoreCase = true) ||
                    it.displayName.equals(value.trim(), ignoreCase = true)
            } ?: inferFromLegacyCategory(value)
        }

        /**
         * Deterministically maps existing 19 display categories and note signals to the 12 canonical memory types.
         */
        fun inferFromLegacyCategory(
            category: String,
            hasAudio: Boolean = false,
            hasLocation: Boolean = false,
            hasImage: Boolean = false,
            contentHint: String = ""
        ): UnifiedMemoryType {
            val lowerContent = contentHint.lowercase()
            if (lowerContent.contains("owe ") || lowerContent.contains("owes ") ||
                lowerContent.contains("promised") || lowerContent.contains("committed to")
            ) {
                return COMMITMENTS
            }
            if (lowerContent.contains("birthday") || lowerContent.contains("anniversary")) {
                return IMPORTANT_DATES
            }
            if (lowerContent.contains("favorite") || lowerContent.contains("favourite") ||
                lowerContent.contains("prefers") || lowerContent.contains("allergic to")
            ) {
                return PREFERENCES
            }
            if (hasImage && category.equals("Visuals", ignoreCase = true)) {
                return EXPERIENCES
            }

            return when (MemoryCategory.fromString(category)) {
                MemoryCategory.MEETING -> if (hasAudio) CONVERSATIONS else EVENTS
                MemoryCategory.WORK -> TASKS
                MemoryCategory.PROJECTS -> PROJECTS
                MemoryCategory.IDEAS -> IDEAS
                MemoryCategory.PERSONAL -> PERSONAL_OBSERVATIONS
                MemoryCategory.FINANCE -> COMMITMENTS
                MemoryCategory.HEALTH -> PERSONAL_OBSERVATIONS
                MemoryCategory.TRAVEL -> if (hasLocation) PLACES else EXPERIENCES
                MemoryCategory.FAMILY -> PEOPLE
                MemoryCategory.LEARNING -> PERSONAL_OBSERVATIONS
                MemoryCategory.JOURNAL -> if (hasAudio) CONVERSATIONS else PERSONAL_OBSERVATIONS
                MemoryCategory.VISUALS -> EXPERIENCES
                MemoryCategory.CLIENTS -> PEOPLE
                MemoryCategory.CODE_TECH -> PROJECTS
                MemoryCategory.SHOPPING -> PREFERENCES
                MemoryCategory.ENTERTAINMENT -> EXPERIENCES
                MemoryCategory.HABITS -> PREFERENCES
                MemoryCategory.HOMELIFE -> TASKS
                MemoryCategory.GENERAL -> if (hasAudio) CONVERSATIONS else PERSONAL_OBSERVATIONS
            }
        }
    }
}

/**
 * Provenance metadata distinguishing explicit user statements from extracted or AI-inferred records.
 */
enum class MemorySourceType(val label: String, val badgeEmoji: String) {
    USER_EXPLICIT("Explicitly Stated", "✍️"),
    AUDIO_TRANSCRIPT("Extracted from Audio", "🎙️"),
    IMAGE_OCR("Extracted from Image", "📷"),
    AI_INTERPRETATION("AI Interpretation", "🤖");

    companion object {
        fun fromString(value: String?): MemorySourceType {
            if (value.isNullOrBlank()) return USER_EXPLICIT
            return values().find { it.name.equals(value.trim(), ignoreCase = true) } ?: USER_EXPLICIT
        }
    }
}

/**
 * Ensures AI never silently turns an uncertain interpretation into an established fact.
 */
enum class MemoryVerificationStatus(val label: String) {
    VERIFIED_FACT("Verified Fact"),
    AI_INFERRED_PENDING("AI Interpretation (Unverified)"),
    ARCHIVED("Archived");

    companion object {
        fun fromString(value: String?): MemoryVerificationStatus {
            if (value.isNullOrBlank()) return VERIFIED_FACT
            return values().find { it.name.equals(value.trim(), ignoreCase = true) } ?: VERIFIED_FACT
        }
    }
}

/**
 * Direction of a commitment tracked by Recall.
 */
enum class CommitmentDirection(val label: String) {
    BY_ME("Owed / Promised by Me"),
    TO_ME("Owed / Promised to Me");

    companion object {
        fun fromString(value: String?): CommitmentDirection {
            if (value.isNullOrBlank()) return BY_ME
            return values().find { it.name.equals(value.trim(), ignoreCase = true) } ?: BY_ME
        }
    }
}

/**
 * Relationship type linking two memories in the Personal Memory Graph.
 */
enum class MemoryRelationType(val label: String) {
    CONTINUES("Continues / Updates"),
    CONTRADICTS("Supersedes / Revises"),
    EXPANDS("Expands Upon"),
    CAUSED_BY("Caused By"),
    RELATED_TO("Related To");

    companion object {
        fun fromString(value: String?): MemoryRelationType {
            if (value.isNullOrBlank()) return RELATED_TO
            return values().find { it.name.equals(value.trim(), ignoreCase = true) } ?: RELATED_TO
        }
    }
}
