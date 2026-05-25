package com.example.flashcards.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flashcards.utils.CryptoUtils
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class QuizCategory(
    val categoryId: String = "",
    val categoryName: String = ""
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

    private val _categories = MutableStateFlow<List<QuizCategory>>(emptyList())
    val categories: StateFlow<List<QuizCategory>> = _categories

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        listenToCategories()
    }

    private fun listenToCategories() {
        _isLoading.value = true
        db.collection("quiz_categories")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("AdminQuizViewModel", "Listen failed.", error)
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            // Rào chắn Null Safety khi đọc Firestore
                            val id = doc.getString("categoryId") ?: ""
                            val name = doc.getString("categoryName") ?: "Danh mục lỗi"
                            if (id.isEmpty()) null else QuizCategory(id, name)
                        } catch (e: Exception) {
                            Log.e("AdminQuizViewModel", "Error parsing category", e)
                            null
                        }
                    }
                    _categories.value = list
                }
                _isLoading.value = false
            }
    }

    fun initializeSampleCategories() {
        viewModelScope.launch {
            try {
                val sampleData = listOf(
                    QuizCategory("QUIZ_JA_N5", "Cấp độ N5"),
                    QuizCategory("QUIZ_JA_N4", "Cấp độ N4"),
                    QuizCategory("QUIZ_JA_N3", "Cấp độ N3"),
                    QuizCategory("QUIZ_JA_N2", "Cấp độ N2"),
                    QuizCategory("QUIZ_JA_N1", "Cấp độ N1"),
                    QuizCategory("QUIZ_EN_TOEIC_450", "Gói TOEIC 450+"),
                    QuizCategory("QUIZ_EN_TOEIC_650", "Gói TOEIC 650+"),
                    QuizCategory("QUIZ_EN_TOEIC_800", "Gói TOEIC 800+"),
                    QuizCategory("QUIZ_EN_IELTS", "Gói IELTS")
                )

                val batch = db.batch()
                sampleData.forEach { category ->
                    val docRef = db.collection("quiz_categories").document(category.categoryId)
                    batch.set(docRef, category)
                }
                batch.commit().await()
            } catch (e: Exception) {
                Log.e("AdminQuizViewModel", "Error initializing sample data", e)
            }
        }
    }

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
                "question" to CryptoUtils.encrypt(question),
                "answerA" to CryptoUtils.encrypt(a),
                "answerB" to CryptoUtils.encrypt(b),
                "answerC" to CryptoUtils.encrypt(c),
                "answerD" to CryptoUtils.encrypt(d),
                "correctAnswer" to CryptoUtils.encrypt(correct),
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
                "question" to CryptoUtils.encrypt(question),
                "correctAnswer" to CryptoUtils.encrypt(correct),
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
                        "question" to CryptoUtils.encrypt(parts[0]),
                        "answerA" to CryptoUtils.encrypt(parts[1]),
                        "answerB" to CryptoUtils.encrypt(parts[2]),
                        "answerC" to CryptoUtils.encrypt(parts[3]),
                        "answerD" to CryptoUtils.encrypt(parts[4]),
                        "correctAnswer" to CryptoUtils.encrypt(parts[5]),
                        "createdAt" to Timestamp.now()
                    ))
                    count++
                } else if (type == "text_input" && parts.size >= 2) {
                    val docRef = db.collection("system_quizzes").document()
                    batch.set(docRef, mapOf(
                        "level" to level,
                        "type" to "text_input",
                        "question" to CryptoUtils.encrypt(parts[0]),
                        "correctAnswer" to CryptoUtils.encrypt(parts[1]),
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
