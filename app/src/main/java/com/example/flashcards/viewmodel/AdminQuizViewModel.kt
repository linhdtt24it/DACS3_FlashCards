package com.example.flashcards.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

data class QuizCategory(
    val name: String,
    val code: String
)

data class SystemQuiz(
    val id: String = "",
    val level: String = "",
    val type: String = "", // "multiple_choice" or "text_input"
    val question: String = "",
    val answerA: String = "",
    val answerB: String = "",
    val answerC: String = "",
    val answerD: String = "",
    val correctAnswer: String = "",
    val createdAt: Timestamp? = null
)

class AdminQuizViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    val categories = listOf(
        QuizCategory("Cấp độ N5", "QUIZ_JA_N5"),
        QuizCategory("Cấp độ N4", "QUIZ_JA_N4"),
        QuizCategory("Cấp độ N3", "QUIZ_JA_N3"),
        QuizCategory("Cấp độ N2", "QUIZ_JA_N2"),
        QuizCategory("Cấp độ N1", "QUIZ_JA_N1"),
        QuizCategory("Gói TOEIC 450+", "QUIZ_EN_TOEIC_450"),
        QuizCategory("Gói TOEIC 650+", "QUIZ_EN_TOEIC_650"),
        QuizCategory("Gói TOEIC 800+", "QUIZ_EN_TOEIC_800"),
        QuizCategory("Gói IELTS", "QUIZ_EN_IELTS")
    )

    suspend fun addMultipleChoiceQuiz(
        level: String,
        question: String,
        a: String,
        b: String,
        c: String,
        d: String,
        correct: String
    ): Result<Unit> {
        return try {
            val data = mapOf(
                "level" to level,
                "type" to "multiple_choice",
                "question" to question,
                "answerA" to a,
                "answerB" to b,
                "answerC" to c,
                "answerD" to d,
                "correctAnswer" to correct,
                "createdAt" to Timestamp.now()
            )
            db.collection("system_quizzes").add(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addTextInputQuiz(
        level: String,
        question: String,
        correct: String
    ): Result<Unit> {
        return try {
            val data = mapOf(
                "level" to level,
                "type" to "text_input",
                "question" to question,
                "correctAnswer" to correct,
                "createdAt" to Timestamp.now()
            )
            db.collection("system_quizzes").add(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun bulkImport(level: String, type: String, rawText: String): Result<Int> {
        return try {
            val lines = rawText.split("\n").filter { it.isNotBlank() }
            val batch = db.batch()
            var count = 0

            lines.forEach { line ->
                val parts = line.split("|").map { it.trim() }
                if (type == "multiple_choice" && parts.size >= 6) {
                    val docRef = db.collection("system_quizzes").document()
                    batch.set(docRef, mapOf(
                        "level" to level,
                        "type" to "multiple_choice",
                        "question" to parts[0],
                        "answerA" to parts[1],
                        "answerB" to parts[2],
                        "answerC" to parts[3],
                        "answerD" to parts[4],
                        "correctAnswer" to parts[5],
                        "createdAt" to Timestamp.now()
                    ))
                    count++
                } else if (type == "text_input" && parts.size >= 2) {
                    val docRef = db.collection("system_quizzes").document()
                    batch.set(docRef, mapOf(
                        "level" to level,
                        "type" to "text_input",
                        "question" to parts[0],
                        "correctAnswer" to parts[1],
                        "createdAt" to Timestamp.now()
                    ))
                    count++
                }
            }

            if (count > 0) {
                batch.commit().await()
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearLevelQuizzes(level: String): Result<Unit> {
        return try {
            val snapshot = db.collection("system_quizzes")
                .whereEqualTo("level", level)
                .get()
                .await()
            val batch = db.batch()
            snapshot.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
