package com.example.flashcards.repository

import android.util.Log
import com.example.flashcards.model.SocialNotification
import com.example.flashcards.model.UserStats
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
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
        if (receiverId.isEmpty() || receiverId == currentUserId) return
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

                val calendar = Calendar.getInstance()
                calendar.timeInMillis = System.currentTimeMillis()
                val today = calendar.get(Calendar.DAY_OF_YEAR)
                val currentYear = calendar.get(Calendar.YEAR)

                // Format today's date as "yyyy-MM-dd" key for studyHistory
                val todayKey = String.format(
                    "%04d-%02d-%02d",
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.DAY_OF_MONTH)
                )

                val lastCalendar = Calendar.getInstance()
                lastCalendar.timeInMillis = currentStats.lastStudyDate
                val lastDay = lastCalendar.get(Calendar.DAY_OF_YEAR)
                val lastYear = lastCalendar.get(Calendar.YEAR)

                var newStreak = currentStats.streakDays
                var newCardsToday = currentStats.cardsStudiedToday

                if (currentYear == lastYear && today == lastDay) {
                    newCardsToday += cardsStudied
                } else if ((currentYear == lastYear && today - lastDay == 1) ||
                    (currentYear > lastYear && today == 1 && lastDay >= 365)) {
                    newStreak += 1
                    newCardsToday = cardsStudied
                } else {
                    newStreak = 1
                    newCardsToday = cardsStudied
                }

                // XP Calculation
                val earnedXp = (cardsStudied * 1) + (correct * 2) + 10
                val newXp = currentStats.xp + earnedXp

                // Badge Logic
                val newTotalCards = currentStats.totalCardsStudied + cardsStudied
                val newAchievements = currentStats.achievements.toMutableList()
                
                if (newTotalCards > 0 && !newAchievements.contains("FIRST_BLOOD")) newAchievements.add("FIRST_BLOOD")
                if (newTotalCards >= 100 && !newAchievements.contains("CENTURION")) newAchievements.add("CENTURION")
                if (newStreak >= 3 && !newAchievements.contains("STREAK_3")) newAchievements.add("STREAK_3")
                if (newStreak >= 7 && !newAchievements.contains("STREAK_7")) newAchievements.add("STREAK_7")

                // Merge into studyHistory map: accumulate per-day counts
                val updatedHistory = currentStats.studyHistory.toMutableMap()
                updatedHistory[todayKey] = (updatedHistory[todayKey] ?: 0) + cardsStudied

                val currentUserProfileName = auth.currentUser?.displayName ?: auth.currentUser?.email?.substringBefore("@") ?: "User"

                val newStats = currentStats.copy(
                    userId = currentUserId,
                    userName = currentUserProfileName,
                    xp = newXp,
                    streakDays = newStreak,
                    lastStudyDate = System.currentTimeMillis(),
                    cardsStudiedToday = newCardsToday,
                    totalCardsStudied = newTotalCards,
                    correctAnswers = currentStats.correctAnswers + correct,
                    wrongAnswers = currentStats.wrongAnswers + wrong,
                    achievements = newAchievements,
                    studyHistory = updatedHistory
                )
                transaction.set(statsDoc, newStats)
            }.await()
        } catch (e: Exception) {
            Log.e("UserRepository", "Error updating stats", e)
        }
    }

    suspend fun addAchievement(achievementName: String) {
        if (currentUserId.isEmpty()) return
        try {
            statsDoc.update("achievements", FieldValue.arrayUnion(achievementName)).await()
            Log.d("UserRepository", "Added achievement: $achievementName")
        } catch (e: Exception) {
            Log.e("UserRepository", "Error adding achievement", e)
        }
    }

    fun getLeaderboard(): Flow<List<UserStats>> = callbackFlow {
        val listener = db.collectionGroup("stats")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("UserRepository", "Listen leaderboard failed.", error)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    // Because stats is a collection group, it might match other collections named "stats".
                    // But in our schema, we only use it for UserStats.
                    val leaders = snapshot.toObjects(UserStats::class.java)
                        .filter { it.userId.isNotEmpty() }
                        .sortedByDescending { it.xp }
                        .take(50)
                    trySend(leaders)
                } else {
                    trySend(emptyList())
                }
            }
        awaitClose { listener.remove() }
    }
}