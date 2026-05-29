package com.example.flashcards.model

import java.util.UUID

data class Flashcard(
    val id: String = UUID.randomUUID().toString(),
    val question: String = "",
    val answer: String = "",
    val imageUrl: String? = null,
    val audioUrl: String? = null,
    val explanation: String = "",
    val nextReviewDate: Long = System.currentTimeMillis(),
    val interval: Int = 0,
    val easeFactor: Double = 2.5,
    val repetitions: Int = 0,
    val state: CardState = CardState.NEW
)

data class StudySet(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val description: String = "",
    val languageCode: String = "en",
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
    val type: String = "COMMENT",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

data class UserStats(
    val userId: String = "",
    val streakDays: Int = 0,
    val lastStudyDate: Long = 0,
    val cardsStudiedToday: Int = 0,
    val totalCardsStudied: Int = 0,
    val correctAnswers: Int = 0,
    val wrongAnswers: Int = 0,
    val achievements: List<String> = emptyList()
)

data class Folder(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val emoji: String = "📁",
    val userId: String = "",
    val setIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

