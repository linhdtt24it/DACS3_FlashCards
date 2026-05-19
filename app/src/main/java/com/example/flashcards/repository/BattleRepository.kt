package com.example.flashcards.repository

import android.util.Log
import com.example.flashcards.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class BattleRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId get() = auth.currentUser?.uid ?: ""
    private val currentUserName get() = auth.currentUser?.displayName ?: auth.currentUser?.email?.substringBefore("@") ?: "Player"

    private val battlesCollection = db.collection("battles")
    private val battleHistoryCollection = db.collection("battle_history")

    // Observe 1 phòng đấu cụ thể
    fun observeBattle(battleId: String): Flow<BattleRoom?> = callbackFlow {
        val listener = battlesCollection.document(battleId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(BattleRoom::class.java))
            }
        awaitClose { listener.remove() }
    }

    // Lấy danh sách phòng đang chờ
    fun getAvailableBattles(): Flow<List<BattleRoom>> = callbackFlow {
        val tenMinutesAgo = System.currentTimeMillis() - (10 * 60 * 1000)

        val listener = battlesCollection
            .whereEqualTo("status", "WAITING")
            .whereGreaterThan("createdAt", tenMinutesAgo)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val rooms = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(BattleRoom::class.java)
                } ?: emptyList()
                trySend(rooms)
            }
        awaitClose { listener.remove() }
    }

    // Tạo phòng đấu mới
    suspend fun createBattle(studySet: StudySet): String {
        if (studySet.cards.size < 2) throw Exception("Cần ít nhất 2 thẻ để đấu!")

        val questions = studySet.cards.shuffled().take(5).map { card ->
            val otherAnswers = studySet.cards
                .filter { it.id != card.id }
                .map { it.answer }
                .distinct()
                .shuffled()
                .take(3)

            BattleQuestion(
                question = card.question,
                correctAnswer = card.answer,
                options = (otherAnswers + card.answer).shuffled()
            )
        }

        val battleId = battlesCollection.document().id
        val room = BattleRoom(
            id = battleId,
            setId = studySet.id,
            deckTitle = studySet.title,
            players = mapOf(
                currentUserId to BattlePlayer(uid = currentUserId, name = currentUserName)
            ),
            status = "WAITING",
            questions = questions,
            createdAt = System.currentTimeMillis()
        )

        battlesCollection.document(battleId).set(room).await()
        return battleId
    }

    // Tham gia phòng
    suspend fun joinBattle(battleId: String) {
        try {
            db.runTransaction { transaction ->
                val docRef = battlesCollection.document(battleId)
                val snapshot = transaction.get(docRef)
                val room = snapshot.toObject(BattleRoom::class.java) ?: return@runTransaction

                if (room.players.size < 2 && !room.players.containsKey(currentUserId)) {
                    val updatedPlayers = room.players.toMutableMap()
                    updatedPlayers[currentUserId] = BattlePlayer(uid = currentUserId, name = currentUserName)
                    transaction.update(docRef, "players", updatedPlayers)
                }
            }.await()
        } catch (e: Exception) {
            Log.e("BattleRepository", "joinBattle error: ${e.message}")
        }
    }

    // Tham gia bằng mã
    suspend fun joinBattleByCode(code: String): String? {
        return try {
            val snapshot = battlesCollection
                .whereEqualTo("status", "WAITING")
                .get()
                .await()
            val roomDoc = snapshot.documents.find { it.id.takeLast(6).uppercase() == code.uppercase() }
            roomDoc?.id
        } catch (e: Exception) {
            null
        }
    }

    // Cập nhật tiến độ
    suspend fun updateProgress(battleId: String, score: Int, progress: Int) {
        try {
            db.runTransaction { transaction ->
                val docRef = battlesCollection.document(battleId)
                val snapshot = transaction.get(docRef)
                val room = snapshot.toObject(BattleRoom::class.java) ?: return@runTransaction

                val updatedPlayers = room.players.toMutableMap()
                val currentPlayer = updatedPlayers[currentUserId] ?: return@runTransaction

                updatedPlayers[currentUserId] = currentPlayer.copy(
                    score = score,
                    progress = progress,
                    finishedTime = if (progress >= room.questions.size) System.currentTimeMillis() else 0
                )

                transaction.update(docRef, "players", updatedPlayers)

                if (updatedPlayers.values.all { it.progress >= room.questions.size }) {
                    transaction.update(docRef, "status", "FINISHED")
                    transaction.update(docRef, "finishedAt", System.currentTimeMillis())
                }
            }.await()
        } catch (e: Exception) {
            Log.e("BattleRepository", "updateProgress error: ${e.message}")
        }
    }

    // Bắt đầu trận
    suspend fun startBattle(battleId: String) {
        try {
            battlesCollection.document(battleId).update(
                mapOf(
                    "status" to "STARTED",
                    "startedAt" to System.currentTimeMillis()
                )
            ).await()
        } catch (e: Exception) {
            Log.e("BattleRepository", "startBattle error: ${e.message}")
        }
    }

    // Rời phòng
    suspend fun leaveAndDeleteRoom(battleId: String, userId: String) {
        try {
            val snapshot = battlesCollection.document(battleId).get().await()
            val room = snapshot.toObject(BattleRoom::class.java)

            if (room == null) return

            when (room.status) {
                "WAITING" -> {
                    battlesCollection.document(battleId).delete().await()
                    Log.d("BattleRepository", "Đã xóa phòng $battleId")
                }
                "STARTED" -> {
                    db.runTransaction { transaction ->
                        val currentSnapshot = transaction.get(battlesCollection.document(battleId))
                        val currentRoom = currentSnapshot.toObject(BattleRoom::class.java) ?: return@runTransaction

                        val updatedPlayers = currentRoom.players.toMutableMap()
                        updatedPlayers[userId] = updatedPlayers[userId]?.copy(
                            progress = currentRoom.questions.size,
                            score = 0,
                            finishedTime = System.currentTimeMillis()
                        ) ?: return@runTransaction

                        transaction.update(battlesCollection.document(battleId), "players", updatedPlayers)

                        if (updatedPlayers.values.all { it.progress >= currentRoom.questions.size }) {
                            transaction.update(battlesCollection.document(battleId), "status", "FINISHED")
                            transaction.update(battlesCollection.document(battleId), "finishedAt", System.currentTimeMillis())
                        }
                    }.await()
                }
            }
        } catch (e: Exception) {
            Log.e("BattleRepository", "leaveAndDeleteRoom error: ${e.message}")
        }
    }

    // Thi lại
    suspend fun rematch(oldBattleId: String, studySet: StudySet): String? {
        return try {
            val oldRoom = battlesCollection.document(oldBattleId).get().await().toObject(BattleRoom::class.java)
            if (oldRoom == null) return null

            val questions = studySet.cards.shuffled().take(5).map { card ->
                val otherAnswers = studySet.cards
                    .filter { it.id != card.id }
                    .map { it.answer }
                    .distinct()
                    .shuffled()
                    .take(3)

                BattleQuestion(
                    question = card.question,
                    correctAnswer = card.answer,
                    options = (otherAnswers + card.answer).shuffled()
                )
            }

            val players = mutableMapOf<String, BattlePlayer>()
            oldRoom.players.forEach { (playerId, player) ->
                players[playerId] = BattlePlayer(
                    uid = playerId,
                    name = player.name
                )
            }

            val newBattleId = battlesCollection.document().id
            val newRoom = BattleRoom(
                id = newBattleId,
                setId = studySet.id,
                deckTitle = studySet.title,
                players = players,
                status = "WAITING",
                questions = questions,
                createdAt = System.currentTimeMillis()
            )

            battlesCollection.document(newBattleId).set(newRoom).await()
            newBattleId
        } catch (e: Exception) {
            Log.e("BattleRepository", "rematch error: ${e.message}")
            null
        }
    }

    // Lưu lịch sử trận đấu
    suspend fun saveBattleHistory(battleRoom: BattleRoom) {
        try {
            if (battleRoom.status != "FINISHED") return

            val winner = battleRoom.players.values.maxByOrNull { it.score }

            val history = BattleHistory(
                id = battleRoom.id,
                setId = battleRoom.setId,
                deckTitle = battleRoom.deckTitle,
                players = battleRoom.players.values.toList(),
                playerIds = battleRoom.players.keys.toList(),
                winner = winner,
                finishedAt = battleRoom.finishedAt
            )

            battleHistoryCollection.document(battleRoom.id).set(history).await()

            battleRoom.players.values.forEach { player ->
                try {
                    val userRef = db.collection("users").document(player.uid)
                    userRef.update(
                        "battleStats.totalPlayed", FieldValue.increment(1),
                        "battleStats.totalScore", FieldValue.increment(player.score.toLong()),
                        "battleStats.totalWins", if (winner?.uid == player.uid) FieldValue.increment(1) else FieldValue.increment(0)
                    ).await()
                } catch (e: Exception) {
                    Log.e("BattleRepository", "Update user stats error: ${e.message}")
                }
            }

            battlesCollection.document(battleRoom.id).delete().await()
        } catch (e: Exception) {
            Log.e("BattleRepository", "saveBattleHistory error: ${e.message}")
        }
    }

    // Dọn dẹp phòng cũ
    suspend fun cleanupOldBattles(minutesOld: Int = 10) {
        try {
            val cutoffTime = System.currentTimeMillis() - (minutesOld * 60 * 1000)

            val oldBattles = battlesCollection
                .whereEqualTo("status", "WAITING")
                .whereLessThan("createdAt", cutoffTime)
                .get()
                .await()

            for (doc in oldBattles.documents) {
                doc.reference.delete().await()
                Log.d("BattleRepository", "Đã xóa phòng cũ: ${doc.id}")
            }
        } catch (e: Exception) {
            Log.e("BattleRepository", "cleanupOldBattles error: ${e.message}")
        }
    }

    // Lấy lịch sử trận đấu của user
    fun getUserBattleHistory(): Flow<List<BattleHistory>> = callbackFlow {
        val listener = battleHistoryCollection
            .whereArrayContains("playerIds", currentUserId)
            .orderBy("finishedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val histories = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(BattleHistory::class.java)
                } ?: emptyList()
                trySend(histories)
            }
        awaitClose { listener.remove() }
    }
}