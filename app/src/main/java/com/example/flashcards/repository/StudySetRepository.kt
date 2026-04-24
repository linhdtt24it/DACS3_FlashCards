package com.example.flashcards.repository

import android.util.Log
import com.example.flashcards.model.Flashcard
import com.example.flashcards.model.StudySet
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class StudySetRepository {

    private val db = FirebaseFirestore.getInstance()
    private val studySetsCollection = db.collection("studySets")

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

    suspend fun seedInitialData() {
        val initialSets = listOf(
            StudySet(
                title = "Tiếng Anh Giao Tiếp",
                description = "Các mẫu câu cơ bản hằng ngày",
                cards = listOf(
                    Flashcard(question = "How are you?", answer = "Bạn khỏe không?"),
                    Flashcard(question = "Nice to meet you", answer = "Rất vui được gặp bạn")
                )
            ),
            StudySet(
                title = "Từ vựng IELTS",
                description = "Chủ đề Environment & Technology",
                cards = List(15) { Flashcard("Từ vựng ${it + 1}", "Nghĩa của từ ${it + 1}") }
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
