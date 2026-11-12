package com.example.data.model

data class SmartTagExtractionResult(
    val title: String,
    val category: String,
    val mentionedPeople: List<String> = emptyList(),
    val tags: List<String> = emptyList()
)
