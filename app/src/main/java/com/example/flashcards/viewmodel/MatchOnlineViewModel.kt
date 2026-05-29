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
import kotlin.random.Random

data class MatchRoom(
    val roomId: String = "",
    val levelId: String = "",
    val hostId: String = "",
    val guestId: String = "",
    val status: String = "WAITING", // WAITING, PLAYING, FINISHED, CANCELLED
    val gridSize: Int = 5,
    val gameData: List<Map<String, Any>> = emptyList(), // Store items as Maps to avoid serialization issues
    val hostScore: Int = 0,
    val guestScore: Int = 0,
    val winnerId: String = ""
)

class MatchOnlineViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private var roomListener: ListenerRegistration? = null

    private val _roomState = MutableStateFlow<MatchRoom?>(null)
    val roomState: StateFlow<MatchRoom?> = _roomState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _gridItems = MutableStateFlow<List<MatchCardItem>>(emptyList())
    val gridItems: StateFlow<List<MatchCardItem>> = _gridItems.asStateFlow()

    fun createRoom(hostId: String, levelId: String, gridSize: Int, vocabList: List<VocabCard>) {
        val roomId = (1000..9999).random().toString() // 4 digit code
        
        // Generate game data
        val selectedVocab = mutableListOf<VocabCard>()
        val pairsNeeded = gridSize
        if (vocabList.isNotEmpty()) {
            for (i in 0 until pairsNeeded) {
                selectedVocab.add(vocabList.random())
            }
        }
        
        val items = mutableListOf<MatchCardItem>()
        selectedVocab.forEachIndexed { index, card ->
            val uniqueId = "${card.id}_$index"
            items.add(MatchCardItem(uniqueId, card.front, isFront = true))
            items.add(MatchCardItem(uniqueId, card.back, isFront = false))
        }
        items.shuffle()

        val gameDataMap = items.map { 
            mapOf(
                "id" to it.id,
                "text" to it.text,
                "isFront" to it.isFront,
                "isMatched" to it.isMatched,
                "matchedBy" to it.matchedBy
            )
        }

        val room = MatchRoom(
            roomId = roomId,
            levelId = levelId,
            hostId = hostId,
            status = "WAITING",
            gridSize = gridSize,
            gameData = gameDataMap
        )

        db.collection("match_rooms").document(roomId).set(room)
            .addOnSuccessListener {
                listenToRoom(roomId)
            }
            .addOnFailureListener {
                _errorMessage.value = "Không thể tạo phòng"
            }
    }

    fun joinRandomRoom(guestId: String, levelId: String, onNoRoomFound: () -> Unit) {
        db.collection("match_rooms")
            .whereEqualTo("status", "WAITING")
            .whereEqualTo("levelId", levelId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot != null && !snapshot.isEmpty) {
                    val roomId = snapshot.documents.first().id
                    joinRoom(roomId, guestId)
                } else {
                    onNoRoomFound()
                }
            }
            .addOnFailureListener {
                _errorMessage.value = "Lỗi tìm phòng ngẫu nhiên"
            }
    }

    fun joinRoom(roomId: String, guestId: String) {
        db.collection("match_rooms").document(roomId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val status = doc.getString("status")
                    if (status == "WAITING") {
                        // Join room
                        db.collection("match_rooms").document(roomId)
                            .update(
                                mapOf(
                                    "guestId" to guestId,
                                    "status" to "PLAYING"
                                )
                            )
                            .addOnSuccessListener {
                                listenToRoom(roomId)
                            }
                    } else {
                        _errorMessage.value = "Phòng đã bắt đầu hoặc kết thúc"
                    }
                } else {
                    _errorMessage.value = "Không tìm thấy phòng"
                }
            }
            .addOnFailureListener {
                _errorMessage.value = "Lỗi kết nối"
            }
    }

    private fun listenToRoom(roomId: String) {
        roomListener?.remove()
        roomListener = db.collection("match_rooms").document(roomId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("MatchOnlineViewModel", "Listen failed.", error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val room = snapshot.toObject(MatchRoom::class.java)
                    _roomState.value = room
                    
                    // Parse gameData back to gridItems
                    val mappedItems = room?.gameData?.map { map ->
                        MatchCardItem(
                            id = map["id"] as? String ?: "",
                            text = map["text"] as? String ?: "",
                            isFront = map["isFront"] as? Boolean ?: false,
                            isMatched = map["isMatched"] as? Boolean ?: false,
                            matchedBy = map["matchedBy"] as? String ?: ""
                        )
                    } ?: emptyList()
                    
                    // Maintain current local selection state
                    val currentSelection = _gridItems.value.filter { it.isSelected }.map { it.id to it.isFront }
                    val updatedItems = mappedItems.map { newItem ->
                        if (currentSelection.any { it.first == newItem.id && it.second == newItem.isFront }) {
                            newItem.copy(isSelected = true)
                        } else {
                            newItem
                        }
                    }
                    _gridItems.value = updatedItems
                }
            }
    }

    fun handleCardMatch(roomId: String, firstIndex: Int, secondIndex: Int, playerId: String, isHost: Boolean) {
        val currentItems = _gridItems.value.toMutableList()
        val firstItem = currentItems[firstIndex]
        val secondItem = currentItems[secondIndex]

        if (firstItem.id == secondItem.id && firstItem.isFront != secondItem.isFront) {
            // Match success
            currentItems[firstIndex] = firstItem.copy(isMatched = true, matchedBy = playerId)
            currentItems[secondIndex] = secondItem.copy(isMatched = true, matchedBy = playerId)
            
            // Update Firestore
            val gameDataMap = currentItems.map { 
                mapOf(
                    "id" to it.id,
                    "text" to it.text,
                    "isFront" to it.isFront,
                    "isMatched" to it.isMatched,
                    "matchedBy" to it.matchedBy
                )
            }
            
            val scoreField = if (isHost) "hostScore" else "guestScore"
            val currentScore = if (isHost) _roomState.value?.hostScore ?: 0 else _roomState.value?.guestScore ?: 0
            
            val updates = mutableMapOf<String, Any>(
                "gameData" to gameDataMap,
                scoreField to currentScore + 10
            )
            
            // Check win condition
            if (currentItems.all { it.isMatched }) {
                updates["status"] = "FINISHED"
                val finalHostScore = if (isHost) currentScore + 10 else _roomState.value?.hostScore ?: 0
                val finalGuestScore = if (!isHost) currentScore + 10 else _roomState.value?.guestScore ?: 0
                
                updates["winnerId"] = if (finalHostScore > finalGuestScore) _roomState.value?.hostId ?: "" 
                                      else if (finalGuestScore > finalHostScore) _roomState.value?.guestId ?: "" 
                                      else "DRAW"
            }

            db.collection("match_rooms").document(roomId).update(updates)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun leaveRoom() {
        roomListener?.remove()
        roomListener = null
        val roomId = _roomState.value?.roomId
        if (roomId != null) {
            db.collection("match_rooms").document(roomId).update("status", "CANCELLED")
        }
        _roomState.value = null
        _gridItems.value = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        leaveRoom()
    }
}
