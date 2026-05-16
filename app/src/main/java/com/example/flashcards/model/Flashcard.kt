package com.example.flashcards.model

import java.util.UUID

data class Flashcard(
    val id: String = UUID.randomUUID().toString(),
    val question: String = "",
    val answer: String = "",
    val imageUrl: String? = null,
    val audioUrl: String? = null,
    val explanation: String = "",
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
    val languageCode: String = "en", // 👈 Lấy từ nhánh của bạn tôi (sửa âm thanh)
    val cards: List<Flashcard> = emptyList(),
    @get:com.google.firebase.firestore.PropertyName("isPublic")
    @set:com.google.firebase.firestore.PropertyName("isPublic")
    var isPublic: Boolean = false,
    val shareCode: String? = null,
    val creatorId: String = "",
    val creatorName: String = "",
    val rating: Float = 0f,
    val ratingCount: Int = 0
)

data class Comment(
    val id: String = UUID.randomUUID().toString(),
    val setId: String = "",
    val userId: String = "",
    val userName: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class SocialNotification(
    val id: String = UUID.randomUUID().toString(),
    val receiverId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val deckId: String = "",
    val deckTitle: String = "",
    val type: String = "COMMENT", // "COMMENT" or "RATING"
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

data class UserStats(
    val userId: String = "",
    val streakDays: Int = 0,
    val lastStudyDate: Long = 0,
    val cardsStudiedToday: Int = 0,
    val correctAnswers: Int = 0,
    val wrongAnswers: Int = 0
)