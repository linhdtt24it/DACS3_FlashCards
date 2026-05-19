package com.example.flashcards.model

data class BattlePlayer(
    val uid: String = "",
    val name: String = "",
    val score: Int = 0,
    val progress: Int = 0,
    val finishedTime: Long = 0
)

data class BattleRoom(
    val id: String = "",
    val setId: String = "",
    val deckTitle: String = "",
    val players: Map<String, BattlePlayer> = emptyMap(),
    val status: String = "WAITING", // WAITING, STARTED, FINISHED
    val questions: List<BattleQuestion> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val startedAt: Long = 0,
    val finishedAt: Long = 0
)

data class BattleQuestion(
    val question: String = "",
    val correctAnswer: String = "",
    val options: List<String> = emptyList()
)

data class BattleHistory(
    val id: String = "",
    val setId: String = "",
    val deckTitle: String = "",
    val players: List<BattlePlayer> = emptyList(),
    val playerIds: List<String> = emptyList(),
    val winner: BattlePlayer? = null,
    val finishedAt: Long = 0
)