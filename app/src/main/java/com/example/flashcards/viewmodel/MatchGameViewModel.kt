package com.example.flashcards.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.flashcards.view.MatchCardItem
import com.example.flashcards.view.VocabCard
import com.example.flashcards.utils.CryptoUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class MatchOnlineRoom(
    val roomId: String = "",
    val levelId: String = "",
    val gridSize: Int = 5,
    val player1Id: String = "",
    val player1Name: String = "",
    val player2Id: String = "",
    val player2Name: String = "",
    val status: String = "WAITING", // WAITING, PLAYING, FINISHED, CANCELLED
    val player1Score: Long = -1L,   // Completion time in ms (-1 if not finished yet)
    val player2Score: Long = -1L,   // Completion time in ms (-1 if not finished yet)
    val gameData: List<Map<String, Any>> = emptyList(), // Shared vocabulary items layout
    val winnerId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

class MatchGameViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private var roomListener: ListenerRegistration? = null

    private val _roomState = MutableStateFlow<MatchOnlineRoom?>(null)
    val roomState: StateFlow<MatchOnlineRoom?> = _roomState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _matchmakingStatus = MutableStateFlow<String>("IDLE") // IDLE, SEARCHING, HOSTING, MATCHED, CANCELLED
    val matchmakingStatus: StateFlow<String> = _matchmakingStatus.asStateFlow()

    private val _p1Items = MutableStateFlow<List<MatchCardItem>>(emptyList())
    val p1Items: StateFlow<List<MatchCardItem>> = _p1Items.asStateFlow()

    private fun encryptRoom(room: MatchOnlineRoom): Map<String, Any> {
        return mapOf(
            "roomId" to CryptoUtils.encrypt(room.roomId),
            "levelId" to CryptoUtils.encrypt(room.levelId),
            "gridSize" to room.gridSize,
            "player1Id" to CryptoUtils.encrypt(room.player1Id),
            "player1Name" to CryptoUtils.encrypt(room.player1Name),
            "player2Id" to CryptoUtils.encrypt(room.player2Id),
            "player2Name" to CryptoUtils.encrypt(room.player2Name),
            "status" to CryptoUtils.encrypt(room.status),
            "player1Score" to CryptoUtils.encrypt(room.player1Score.toString()),
            "player2Score" to CryptoUtils.encrypt(room.player2Score.toString()),
            "gameData" to room.gameData.map { item ->
                item.mapValues { (key, value) ->
                    if (key == "text" && value is String) CryptoUtils.encrypt(value) else value
                }
            },
            "winnerId" to CryptoUtils.encrypt(room.winnerId),
            "createdAt" to room.createdAt
        )
    }

    private fun decryptRoom(map: Map<String, Any>): MatchOnlineRoom {
        return MatchOnlineRoom(
            roomId = CryptoUtils.decrypt(map["roomId"] as? String),
            levelId = CryptoUtils.decrypt(map["levelId"] as? String),
            gridSize = (map["gridSize"] as? Long)?.toInt() ?: 5,
            player1Id = CryptoUtils.decrypt(map["player1Id"] as? String),
            player1Name = CryptoUtils.decrypt(map["player1Name"] as? String),
            player2Id = CryptoUtils.decrypt(map["player2Id"] as? String),
            player2Name = CryptoUtils.decrypt(map["player2Name"] as? String),
            status = CryptoUtils.decrypt(map["status"] as? String).ifBlank { "WAITING" },
            player1Score = CryptoUtils.decrypt(map["player1Score"] as? String).toLongOrNull() ?: -1L,
            player2Score = CryptoUtils.decrypt(map["player2Score"] as? String).toLongOrNull() ?: -1L,
            gameData = (map["gameData"] as? List<Map<String, Any>> ?: emptyList()).map { item ->
                item.mapValues { (key, value) ->
                    if (key == "text" && value is String) CryptoUtils.decrypt(value) else value
                }
            },
            winnerId = CryptoUtils.decrypt(map["winnerId"] as? String),
            createdAt = map["createdAt"] as? Long ?: System.currentTimeMillis()
        )
    }

    fun createRoomCode(
        userId: String,
        userName: String,
        levelId: String,
        gridSize: Int,
        vocabList: List<VocabCard>
    ) {
        _errorMessage.value = null
        _matchmakingStatus.value = "HOSTING"
        _roomState.value = null
        _p1Items.value = emptyList()

        val roomId = (100000..999999).random().toString() // Generates 6-digit room code
        
        // Host generates the shared grid layout
        val items = generateSharedGameItems(vocabList, gridSize)
        val gameDataMap = items.map { 
            mapOf(
                "id" to it.id,
                "text" to it.text,
                "isFront" to it.isFront,
                "isMatched" to it.isMatched,
                "matchedBy" to it.matchedBy
            )
        }

        val room = MatchOnlineRoom(
            roomId = roomId,
            levelId = levelId,
            gridSize = gridSize,
            player1Id = userId,
            player1Name = userName,
            player2Id = "",
            player2Name = "",
            status = "WAITING",
            player1Score = -1L,
            player2Score = -1L,
            gameData = gameDataMap,
            winnerId = "",
            createdAt = System.currentTimeMillis()
        )

        val encryptedDocId = CryptoUtils.encrypt(roomId)
        val encryptedRoomMap = encryptRoom(room)

        db.collection("game_rooms").document(encryptedDocId).set(encryptedRoomMap)
            .addOnSuccessListener {
                listenToRoom(roomId, userId)
            }
            .addOnFailureListener { e ->
                Log.e("MatchGameViewModel", "Lỗi tạo phòng code", e)
                _matchmakingStatus.value = "IDLE"
                _errorMessage.value = "Không thể tạo phòng đấu trực tuyến"
            }
    }

    fun joinRoomByCode(roomId: String, userId: String, userName: String) {
        _errorMessage.value = null
        _matchmakingStatus.value = "SEARCHING"
        _roomState.value = null
        _p1Items.value = emptyList()

        val encryptedDocId = CryptoUtils.encrypt(roomId)
        val roomRef = db.collection("game_rooms").document(encryptedDocId)
        
        roomRef.get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val data = doc.data ?: emptyMap()
                val currentStatus = CryptoUtils.decrypt(data["status"] as? String).ifBlank { "WAITING" }
                val currentP2 = CryptoUtils.decrypt(data["player2Id"] as? String)
                
                if (currentStatus == "WAITING" && currentP2.isEmpty()) {
                    db.runTransaction { transaction ->
                        transaction.update(roomRef, mapOf(
                            "player2Id" to CryptoUtils.encrypt(userId),
                            "player2Name" to CryptoUtils.encrypt(userName),
                            "status" to CryptoUtils.encrypt("PLAYING")
                        ))
                        null
                    }.addOnSuccessListener {
                        _matchmakingStatus.value = "MATCHED"
                        listenToRoom(roomId, userId)
                    }.addOnFailureListener { e ->
                        Log.e("MatchGameViewModel", "Lỗi giao dịch tham gia phòng", e)
                        _matchmakingStatus.value = "IDLE"
                        _errorMessage.value = "Không thể tham gia phòng: Lỗi giao dịch"
                    }
                } else {
                    _matchmakingStatus.value = "IDLE"
                    _errorMessage.value = "Mã phòng đã đầy hoặc đã bắt đầu thi đấu!"
                }
            } else {
                _matchmakingStatus.value = "IDLE"
                _errorMessage.value = "Mã phòng không tồn tại!"
            }
        }.addOnFailureListener { e ->
            Log.e("MatchGameViewModel", "Lỗi kết nối kiểm tra mã phòng", e)
            _matchmakingStatus.value = "IDLE"
            _errorMessage.value = "Lỗi kết nối cơ sở dữ liệu"
        }
    }

    private fun listenToRoom(roomId: String, currentUserId: String) {
        roomListener?.remove()
        val encryptedDocId = CryptoUtils.encrypt(roomId)
        roomListener = db.collection("game_rooms").document(encryptedDocId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("MatchGameViewModel", "Snapshot listener failed.", error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val data = snapshot.data ?: return@addSnapshotListener
                    val room = decryptRoom(data)
                    _roomState.value = room

                    if (room != null) {
                        if (room.status == "PLAYING") {
                            _matchmakingStatus.value = "MATCHED"
                        }
                        
                        // Parse shared gameData grid for this local player
                        val mappedItems = room.gameData.map { map ->
                            MatchCardItem(
                                id = map["id"] as? String ?: "",
                                text = map["text"] as? String ?: "",
                                isFront = map["isFront"] as? Boolean ?: false,
                                isMatched = false, // Played locally!
                                matchedBy = ""
                            )
                        }

                        // Initialize the player grid locally if not initialized yet
                        if (_p1Items.value.isEmpty() && mappedItems.isNotEmpty()) {
                            _p1Items.value = mappedItems
                        }

                        // Realtime evaluation: check if both finished
                        val p1Score = room.player1Score
                        val p2Score = room.player2Score

                        if (p1Score > 0L && p2Score > 0L && room.status == "PLAYING") {
                            // Determine the winner and set to FINISHED status atomically
                            val winnerId = when {
                                p1Score < p2Score -> room.player1Id
                                p2Score < p1Score -> room.player2Id
                                else -> "DRAW"
                            }
                            db.collection("game_rooms").document(encryptedDocId).update(
                                mapOf(
                                    "status" to CryptoUtils.encrypt("FINISHED"),
                                    "winnerId" to CryptoUtils.encrypt(winnerId)
                                )
                            )
                        }
                    }
                }
            }
    }

    fun submitCompletionTime(timeMs: Long, currentUserId: String) {
        val room = _roomState.value ?: return
        val isPlayer1 = currentUserId == room.player1Id
        val scoreField = if (isPlayer1) "player1Score" else "player2Score"
        val encryptedDocId = CryptoUtils.encrypt(room.roomId)

        db.collection("game_rooms").document(encryptedDocId)
            .update(scoreField, CryptoUtils.encrypt(timeMs.toString()))
            .addOnSuccessListener {
                Log.d("MatchGameViewModel", "Đã gửi thời gian: $timeMs ms")
            }
            .addOnFailureListener { e ->
                Log.e("MatchGameViewModel", "Lỗi gửi kết quả lên server", e)
            }
    }

    fun updateLocalItems(items: List<MatchCardItem>) {
        _p1Items.value = items
    }

    fun cancelMatchmaking() {
        roomListener?.remove()
        roomListener = null
        val room = _roomState.value
        if (room != null && room.status == "WAITING") {
            val encryptedDocId = CryptoUtils.encrypt(room.roomId)
            db.collection("game_rooms").document(encryptedDocId).delete()
        }
        _roomState.value = null
        _matchmakingStatus.value = "IDLE"
        _p1Items.value = emptyList()
    }

    fun leaveRoom(currentUserId: String) {
        roomListener?.remove()
        roomListener = null
        val room = _roomState.value
        if (room != null) {
            val encryptedDocId = CryptoUtils.encrypt(room.roomId)
            if (room.status == "PLAYING") {
                // Forfeit: The other player wins instantly!
                val winnerId = if (currentUserId == room.player1Id) room.player2Id else room.player1Id
                db.collection("game_rooms").document(encryptedDocId).update(
                    mapOf(
                        "status" to CryptoUtils.encrypt("FINISHED"),
                        "winnerId" to CryptoUtils.encrypt(winnerId),
                        // Set the forfeiting player's time to a very high score so UI displays correctly
                        (if (currentUserId == room.player1Id) "player1Score" else "player2Score") to CryptoUtils.encrypt("999999")
                    )
                )
            } else if (room.status == "WAITING") {
                db.collection("game_rooms").document(encryptedDocId).delete()
            }
        }
        _roomState.value = null
        _matchmakingStatus.value = "IDLE"
        _p1Items.value = emptyList()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun generateSharedGameItems(vocabList: List<VocabCard>, gridSize: Int): List<MatchCardItem> {
        if (vocabList.isEmpty()) return emptyList()
        val pairsNeeded = gridSize
        val selectedVocab = mutableListOf<VocabCard>()
        
        for (i in 0 until pairsNeeded) {
            selectedVocab.add(vocabList.random())
        }
        
        val list = mutableListOf<MatchCardItem>()
        selectedVocab.forEachIndexed { index, card ->
            val uniqueId = "${card.id}_$index"
            list.add(MatchCardItem(uniqueId, card.front, isFront = true))
            list.add(MatchCardItem(uniqueId, card.back, isFront = false))
        }
        return list.shuffled()
    }

    override fun onCleared() {
        super.onCleared()
        roomListener?.remove()
    }
}
