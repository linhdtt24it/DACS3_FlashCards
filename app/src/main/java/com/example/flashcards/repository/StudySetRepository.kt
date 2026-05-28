package com.example.flashcards.repository

import android.util.Log
import com.example.flashcards.model.Flashcard
import com.example.flashcards.model.StudySet
import com.example.flashcards.model.Comment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class StudySetRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val studySetsCollection get() = db.collection("users").document(auth.currentUser?.uid ?: "anonymous").collection("studySets")
    private val publicStudySetsCollection = db.collection("publicStudySets")

    fun getStudySets(): Flow<List<StudySet>> = callbackFlow {
        val listener = studySetsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("StudySetRepository", "Listen failed.", error)
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val sets = snapshot.toObjects(StudySet::class.java)
                trySend(sets)
            } else {
                trySend(emptyList())
            }
        }

        awaitClose {
            listener.remove()
        }
    }

    suspend fun saveStudySet(studySet: StudySet) {
        try {
            val batch = db.batch()
            val userSetRef = studySetsCollection.document(studySet.id)
            val publicSetRef = publicStudySetsCollection.document(studySet.id)
            
            batch.set(userSetRef, studySet)
            if (studySet.isPublic) {
                batch.set(publicSetRef, studySet)
            } else {
                batch.delete(publicSetRef)
            }
            
            // Sync cards to user_decks -> deckId -> cards sub-collection
            val userDecksCardsRef = db.collection("user_decks").document(studySet.id).collection("cards")
            studySet.cards.forEach { card ->
                batch.set(userDecksCardsRef.document(card.id), card)
            }
            
            batch.commit().await()
        } catch (e: Exception) {
            Log.e("StudySetRepository", "Lỗi cập nhật thẻ", e)
        }
    }

    suspend fun deleteStudySet(id: String) {
        try {
            val batch = db.batch()
            batch.delete(studySetsCollection.document(id))
            batch.delete(publicStudySetsCollection.document(id))
            batch.commit().await()
        } catch (e: Exception) {
            Log.e("StudySetRepository", "Lỗi xóa bộ thẻ", e)
        }
    }

    suspend fun seedInitialData() {
        val initialSets = listOf(
            StudySet(
                title = "FlowCards Guide",
                description = "Learn how to use FlowCards effectively",
                cards = listOf(
                    Flashcard(question = "What is FlowCards?", answer = "It's an app that helps you learn efficiently using flashcards and spaced repetition."),
                    Flashcard(question = "How to navigate cards?", answer = "Tap the card to flip it, use the bottom arrows to switch cards, or tap the difficulty buttons (Hard/Good/Easy)."),
                    Flashcard(question = "How to delete a deck?", answer = "Tap the red trash bin icon on the top right corner of any deck card.")
                )
            )
        )

        for (set in initialSets) {
            try {
                studySetsCollection.document(set.id).set(set).await()
            } catch (e: Exception) {
                Log.w("StudySetRepository", "Error writing document", e)
            }
        }
    }

    suspend fun getPublicStudySets(): List<StudySet> {
        return try {
            val snapshot = publicStudySetsCollection.get().await()
            snapshot.toObjects(StudySet::class.java)
        } catch (e: Exception) {
            Log.e("StudySetRepository", "Error getting public study sets", e)
            emptyList()
        }
    }

    suspend fun getStudySetByShareCode(code: String): StudySet? {
        return try {
            val snapshot = publicStudySetsCollection.whereEqualTo("shareCode", code).get().await()
            if (!snapshot.isEmpty) {
                snapshot.documents[0].toObject(StudySet::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("StudySetRepository", "Error getting study set by share code", e)
            null
        }
    }

    suspend fun ratePublicStudySet(setId: String, rating: Float) {
        try {
            db.runTransaction { transaction ->
                val docRef = publicStudySetsCollection.document(setId)
                val snapshot = transaction.get(docRef)
                if (snapshot.exists()) {
                    val currentRating = snapshot.getDouble("rating")?.toFloat() ?: 0f
                    val currentCount = snapshot.getLong("ratingCount")?.toInt() ?: 0

                    val newCount = currentCount + 1
                    val newRating = ((currentRating * currentCount) + rating) / newCount

                    transaction.update(docRef, "rating", newRating)
                    transaction.update(docRef, "ratingCount", newCount)
                }
            }.await()
        } catch (e: Exception) {
            Log.e("StudySetRepository", "Error rating study set", e)
        }
    }

    fun getComments(setId: String): Flow<List<Comment>> = callbackFlow {
        val listener = publicStudySetsCollection.document(setId).collection("comments")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("StudySetRepository", "Listen comments failed.", error)
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val comments = snapshot.toObjects(Comment::class.java)
                    trySend(comments)
                } else {
                    trySend(emptyList())
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    suspend fun addComment(setId: String, comment: Comment) {
        try {
            publicStudySetsCollection.document(setId).collection("comments").document(comment.id).set(comment).await()
        } catch (e: Exception) {
            Log.e("StudySetRepository", "Error adding comment", e)
        }
    }
}