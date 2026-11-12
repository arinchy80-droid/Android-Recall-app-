package com.example.data.model

import java.util.UUID

enum class MessageSender {
    USER,
    CONCIERGE
}

data class Citation(
    val noteId: String?,
    val noteTitle: String,
    val dateString: String
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val citations: List<Citation> = emptyList(),
    val isThinking: Boolean = false,
    val thinkingTrace: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
