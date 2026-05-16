package com.example.flashcards.data

import android.util.Log
import com.example.flashcards.model.Flashcard
import com.example.flashcards.model.StudySet
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FlashcardRepository {
    private val db = Firebase.firestore
    private val collection = db.collection("studySets")

    // Lắng nghe dữ liệu thay đổi theo thời gian thực (Real-time)
    fun getStudySets(): Flow<List<StudySet>> = callbackFlow {
        val listener = collection.addSnapshotListener { snapshot, e ->
            if (e != null) {
                close(e)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val sets = snapshot.toObjects(StudySet::class.java)
                trySend(sets)
            }
        }
        awaitClose { listener.remove() }
    }

    fun saveStudySet(studySet: StudySet) {
        collection.document(studySet.id).set(studySet)
            .addOnFailureListener { Log.e("Repository", "Error saving set", it) }
    }

    fun seedInitialData() {
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
        for (set in initialSets) saveStudySet(set)
    }
}
