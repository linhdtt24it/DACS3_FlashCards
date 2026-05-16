package com.example.flashcards.repository

import android.util.Log
import com.example.flashcards.model.Flashcard
import com.example.flashcards.model.StudySet
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
            studySetsCollection.document(studySet.id).set(studySet).await()
        } catch (e: Exception) {
            Log.e("StudySetRepository", "Lỗi cập nhật thẻ", e)
        }
    }

    suspend fun deleteStudySet(id: String) {
        try {
            studySetsCollection.document(id).delete().await()
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
}
