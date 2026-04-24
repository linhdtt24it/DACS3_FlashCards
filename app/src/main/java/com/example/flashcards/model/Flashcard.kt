package com.example.flashcards.model

import java.util.UUID

data class Flashcard(
    val id: String = UUID.randomUUID().toString(),
    val question: String = "",
    val answer: String = "",
    val imageUrl: String? = null,
    val audioUrl: String? = null,
    // Spaced Repetition (SM-2 Algorithm)
    val nextReviewDate: Long = System.currentTimeMillis(),
    val interval: Int = 0, // days
    val easeFactor: Double = 2.5,
    val repetitions: Int = 0
)

data class StudySet(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val description: String = "",
    val cards: List<Flashcard> = emptyList()
)
