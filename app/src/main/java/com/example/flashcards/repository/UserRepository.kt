package com.example.flashcards.repository

import android.util.Log
import com.example.flashcards.model.SocialNotification
import com.example.flashcards.model.UserStats
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId get() = auth.currentUser?.uid ?: ""

    // Notifications
    private val notificationsCollection get() = db.collection("users").document(currentUserId).collection("notifications")

    fun getNotifications(): Flow<List<SocialNotification>> = callbackFlow {
        if (currentUserId.isEmpty()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val listener = notificationsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("UserRepository", "Listen notifications failed.", error)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val notifications = snapshot.toObjects(SocialNotification::class.java)
                    trySend(notifications)
                } else {
                    trySend(emptyList())
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun markNotificationAsRead(notificationId: String) {
        if (currentUserId.isEmpty()) return
        try {
            notificationsCollection.document(notificationId).update("isRead", true).await()
        } catch (e: Exception) {
            Log.e("UserRepository", "Error marking notification as read", e)
        }
    }

    suspend fun sendNotification(receiverId: String, notification: SocialNotification) {
        if (receiverId.isEmpty() || receiverId == currentUserId) return // Don't notify yourself
        try {
            db.collection("users").document(receiverId)
                .collection("notifications").document(notification.id)
                .set(notification).await()
        } catch (e: Exception) {
            Log.e("UserRepository", "Error sending notification", e)
        }
    }

    // User Stats
    private val statsDoc get() = db.collection("users").document(currentUserId).collection("stats").document("main")

    fun getUserStats(): Flow<UserStats> = callbackFlow {
        if (currentUserId.isEmpty()) {
            trySend(UserStats())
            awaitClose { }
            return@callbackFlow
        }
        val listener = statsDoc.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("UserRepository", "Listen stats failed.", error)
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                trySend(snapshot.toObject(UserStats::class.java) ?: UserStats(userId = currentUserId))
            } else {
                trySend(UserStats(userId = currentUserId))
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun recordStudySession(cardsStudied: Int, correct: Int, wrong: Int) {
        if (currentUserId.isEmpty()) return
        try {
            db.runTransaction { transaction ->
                val snapshot = transaction.get(statsDoc)
                val currentStats = if (snapshot.exists()) {
                    snapshot.toObject(UserStats::class.java) ?: UserStats(userId = currentUserId)
                } else {
                    UserStats(userId = currentUserId)
                }

                // Check Streak logic
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = System.currentTimeMillis()
                val today = calendar.get(Calendar.DAY_OF_YEAR)
                val currentYear = calendar.get(Calendar.YEAR)

                val lastCalendar = Calendar.getInstance()
                lastCalendar.timeInMillis = currentStats.lastStudyDate
                val lastDay = lastCalendar.get(Calendar.DAY_OF_YEAR)
                val lastYear = lastCalendar.get(Calendar.YEAR)

                var newStreak = currentStats.streakDays
                var newCardsToday = currentStats.cardsStudiedToday

                if (currentYear == lastYear && today == lastDay) {
                    // Studied already today
                    newCardsToday += cardsStudied
                } else if ((currentYear == lastYear && today - lastDay == 1) || 
                           (currentYear > lastYear && today == 1 && lastDay >= 365)) {
                    // Studied yesterday
                    newStreak += 1
                    newCardsToday = cardsStudied
                } else {
                    // Missed a day or first time
                    newStreak = 1
                    newCardsToday = cardsStudied
                }

                val newStats = currentStats.copy(
                    streakDays = newStreak,
                    lastStudyDate = System.currentTimeMillis(),
                    cardsStudiedToday = newCardsToday,
                    correctAnswers = currentStats.correctAnswers + correct,
                    wrongAnswers = currentStats.wrongAnswers + wrong
                )
                transaction.set(statsDoc, newStats)
            }.await()
        } catch (e: Exception) {
            Log.e("UserRepository", "Error updating stats", e)
        }
    }
}
