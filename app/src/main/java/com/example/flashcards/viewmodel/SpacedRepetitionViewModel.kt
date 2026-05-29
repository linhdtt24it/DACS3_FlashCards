package com.example.flashcards.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flashcards.model.CardState
import com.example.flashcards.model.ReviewRating
import com.example.flashcards.model.SpacedRepetition
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class SpacedRepetitionCard(
    val vocabId: String,
    val state: CardState,
    val interval: Int,
    val easeFactor: Double,
    val nextReview: Long
)

class SpacedRepetitionViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val uid: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val _dailyWordLimit = MutableStateFlow(10)
    val dailyWordLimit = _dailyWordLimit.asStateFlow()

    private val _timelineDayCounts = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val timelineDayCounts = _timelineDayCounts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _totalCards = MutableStateFlow(0)
    val totalCards = _totalCards.asStateFlow()

    private val _cardsStudiedToday = MutableStateFlow(0)
    val cardsStudiedToday = _cardsStudiedToday.asStateFlow()
    
    private val _heatmapData = MutableStateFlow<List<Int>>(emptyList())
    val heatmapData = _heatmapData.asStateFlow()
    
    private val _accuracyTrend = MutableStateFlow<List<Float>>(emptyList())
    val accuracyTrend = _accuracyTrend.asStateFlow()

    init {
        fetchDailyLimit()
        fetchTimeline()
        fetchLearningStats()
    }

    private fun fetchDailyLimit() {
        if (uid.isEmpty()) return
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    _dailyWordLimit.value = doc.getLong("dailyWordLimit")?.toInt() ?: 10
                }
            }
    }

    fun saveDailyWordLimit(limit: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (uid.isEmpty()) {
            onError("Lỗi xác thực người dùng")
            return
        }
        if (limit <= 0) {
            onError("Vui lòng nhập số hợp lệ lớn hơn 0!")
            return
        }
        
        firestore.collection("users").document(uid)
            .set(hashMapOf("dailyWordLimit" to limit), SetOptions.merge())
            .addOnSuccessListener {
                _dailyWordLimit.value = limit
                onSuccess()
            }
            .addOnFailureListener {
                onError("Không thể lưu giới hạn từ vựng: ${it.message}")
            }
    }

    private fun getDayEndTimes(): LongArray {
        val calendar = Calendar.getInstance()
        // Start of today
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfToday = calendar.timeInMillis

        val dayEndTimes = LongArray(7)
        for (i in 0..6) {
            calendar.timeInMillis = startOfToday
            calendar.add(Calendar.DAY_OF_YEAR, i)
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            dayEndTimes[i] = calendar.timeInMillis
        }
        return dayEndTimes
    }

    private fun fetchLearningStats() {
        if (uid.isEmpty()) return
        firestore.collection("progress")
            .whereEqualTo("uid", uid)
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    _totalCards.value = snapshot.size()
                    
                    val calendar = Calendar.getInstance()
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val startOfToday = calendar.timeInMillis
                    
                    var todayCount = 0
                    snapshot.documents.forEach { doc ->
                        val lastRev = doc.getTimestamp("lastReviewed")?.toDate()?.time ?: 0
                        if (lastRev >= startOfToday) {
                            todayCount++
                        }
                    }
                    _cardsStudiedToday.value = todayCount
                    
                    // Generate mock heatmap & trend data for UI demonstration
                    _heatmapData.value = List(35) { if (it == 34) todayCount else (0..20).random() }
                    _accuracyTrend.value = List(7) { (60..100).random().toFloat() / 100f }
                }
            }
    }

    private fun fetchTimeline() {
        if (uid.isEmpty()) return
        
        _isLoading.value = true
        firestore.collection("progress")
            .whereEqualTo("uid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                val dayEndTimes = getDayEndTimes()
                val counts = mutableMapOf<Int, Int>()
                for (i in 1..7) counts[i] = 0

                snapshot.documents.forEach { doc ->
                    val nextReview = doc.getTimestamp("nextReview")
                    if (nextReview != null) {
                        val nextReviewMs = nextReview.toDate().time
                        when {
                            nextReviewMs <= dayEndTimes[0] -> counts[1] = counts.getOrDefault(1, 0) + 1
                            nextReviewMs <= dayEndTimes[1] -> counts[2] = counts.getOrDefault(2, 0) + 1
                            nextReviewMs <= dayEndTimes[2] -> counts[3] = counts.getOrDefault(3, 0) + 1
                            nextReviewMs <= dayEndTimes[3] -> counts[4] = counts.getOrDefault(4, 0) + 1
                            nextReviewMs <= dayEndTimes[4] -> counts[5] = counts.getOrDefault(5, 0) + 1
                            nextReviewMs <= dayEndTimes[5] -> counts[6] = counts.getOrDefault(6, 0) + 1
                            nextReviewMs <= dayEndTimes[6] -> counts[7] = counts.getOrDefault(7, 0) + 1
                        }
                    } else {
                        // New or unscheduled, falls to Today (Day 1)
                        counts[1] = counts.getOrDefault(1, 0) + 1
                    }
                }
                
                _timelineDayCounts.value = counts
                _isLoading.value = false
            }
    }

    fun recordProgress(
        vocabId: String,
        currentState: CardState,
        currentInterval: Int,
        currentEaseFactor: Double,
        rating: ReviewRating
    ) {
        if (uid.isEmpty()) return

        val result = SpacedRepetition.calculateNextReview(
            currentState = currentState,
            currentInterval = currentInterval,
            currentEaseFactor = currentEaseFactor,
            rating = rating
        )

        val calendar = Calendar.getInstance()
        val lastReviewed = com.google.firebase.Timestamp(calendar.time)
        
        calendar.add(Calendar.DAY_OF_YEAR, result.intervalDays)
        val nextReview = com.google.firebase.Timestamp(calendar.time)

        val progressData = hashMapOf(
            "uid" to uid,
            "vocabId" to vocabId,
            "status" to result.state.name,
            "interval" to result.intervalDays,
            "easeFactor" to result.easeFactor,
            "lastReviewed" to lastReviewed,
            "nextReview" to nextReview
        )

        // Only save to progress collection to avoid duplication
        firestore.collection("progress").document("${uid}_$vocabId")
            .set(progressData, SetOptions.merge())
    }
}
