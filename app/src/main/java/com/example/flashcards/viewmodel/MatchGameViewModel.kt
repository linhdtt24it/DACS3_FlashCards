package com.example.flashcards.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.flashcards.view.MatchCardItem
import com.example.flashcards.view.VocabCard
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

        db.collection("game_rooms").document(roomId).set(room)
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

        val roomRef = db.collection("game_rooms").document(roomId)
        
        roomRef.get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val currentStatus = doc.getString("status") ?: "WAITING"
                val currentP2 = doc.getString("player2Id") ?: ""
                
                if (currentStatus == "WAITING" && currentP2.isEmpty()) {
                    db.runTransaction { transaction ->
                        transaction.update(roomRef, mapOf(
                            "player2Id" to userId,
                            "player2Name" to userName,
                            "status" to "PLAYING"
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
        roomListener = db.collection("game_rooms").document(roomId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("MatchGameViewModel", "Snapshot listener failed.", error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val room = snapshot.toObject(MatchOnlineRoom::class.java)
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
                            db.collection("game_rooms").document(roomId).update(
                                mapOf(
                                    "status" to "FINISHED",
                                    "winnerId" to winnerId
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

        db.collection("game_rooms").document(room.roomId)
            .update(scoreField, timeMs)
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
            db.collection("game_rooms").document(room.roomId).delete()
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
            if (room.status == "PLAYING") {
                // Forfeit: The other player wins instantly!
                val winnerId = if (currentUserId == room.player1Id) room.player2Id else room.player1Id
                db.collection("game_rooms").document(room.roomId).update(
                    mapOf(
                        "status" to "FINISHED",
                        "winnerId" to winnerId,
                        // Set the forfeiting player's time to a very high score so UI displays correctly
                        (if (currentUserId == room.player1Id) "player1Score" else "player2Score") to 999999L
                    )
                )
            } else if (room.status == "WAITING") {
                db.collection("game_rooms").document(room.roomId).delete()
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
