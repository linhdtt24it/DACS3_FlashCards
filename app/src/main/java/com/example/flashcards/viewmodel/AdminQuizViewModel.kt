package com.example.flashcards.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flashcards.utils.CryptoUtils
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class QuizCategory(
    val categoryId: String = "",
    val categoryName: String = ""
)

data class QuizType(
    val id: String = "",
    val name: String = ""
)

data class QuizLanguage(
    val id: String = "",
    val name: String = ""
)

data class QuizSubLevel(
    val id: String = "",
    val language: String = "", // e.g. "japanese", "english", "chinese", "pali"
    val name: String = "", // e.g. "Cấp độ N5", "TOEIC 450+", "Trung Cơ Bản"
    val parent: String = "", // for English certificate sub-groups (e.g. "TOEIC", "IELTS", "Cambridge")
    val requiredPackage: String = "FREE",
    val description: String = ""
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

    // Lắng nghe Tầng 1: Các loại câu hỏi
    private val _quizTypes = MutableStateFlow<List<QuizType>>(emptyList())
    val quizTypes: StateFlow<List<QuizType>> = _quizTypes

    // Lắng nghe Tầng 2: Các ngôn ngữ học thuật
    private val _quizLanguages = MutableStateFlow<List<QuizLanguage>>(emptyList())
    val quizLanguages: StateFlow<List<QuizLanguage>> = _quizLanguages

    // Lắng nghe Tầng 3: Các cấp độ chi tiết
    private val _quizSubLevels = MutableStateFlow<List<QuizSubLevel>>(emptyList())
    val quizSubLevels: StateFlow<List<QuizSubLevel>> = _quizSubLevels

    // Tương thích ngược với các thành phần cũ (nếu có)
    private val _categories = MutableStateFlow<List<QuizCategory>>(emptyList())
    val categories: StateFlow<List<QuizCategory>> = _categories

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        listenToQuizTypes()
        listenToQuizLanguages()
        listenToQuizSubLevels()
        listenToCategoriesLegacy()
    }

    private fun listenToQuizTypes() {
        _isLoading.value = true
        db.collection("quiz_types")
            .addSnapshotListener { snapshot, error ->
                _isLoading.value = false
                if (error != null) {
                    Log.e("AdminQuizViewModel", "Lỗi lắng nghe quiz_types", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.getString("id") ?: doc.id
                            val name = doc.getString("name") ?: ""
                            if (id.isNotEmpty() && name.isNotEmpty()) QuizType(id, name) else null
                        } catch (e: Exception) {
                            null
                        }
                    }
                    _quizTypes.value = list
                }
            }
    }

    private fun listenToQuizLanguages() {
        db.collection("quiz_languages")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("AdminQuizViewModel", "Lỗi lắng nghe quiz_languages", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.getString("id") ?: doc.id
                            val name = doc.getString("name") ?: ""
                            if (id.isNotEmpty() && name.isNotEmpty()) QuizLanguage(id, name) else null
                        } catch (e: Exception) {
                            null
                        }
                    }
                    _quizLanguages.value = list
                }
            }
    }

    private fun listenToQuizSubLevels() {
        db.collection("quiz_sub_levels")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("AdminQuizViewModel", "Lỗi lắng nghe quiz_sub_levels", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.getString("id") ?: doc.id
                            val language = doc.getString("language") ?: ""
                            val name = doc.getString("name") ?: ""
                            val parent = doc.getString("parent") ?: ""
                            val requiredPackage = doc.getString("requiredPackage") ?: "FREE"
                            val description = doc.getString("description") ?: ""
                            if (id.isNotEmpty() && name.isNotEmpty()) {
                                QuizSubLevel(id, language, name, parent, requiredPackage, description)
                            } else null
                        } catch (e: Exception) {
                            null
                        }
                    }
                    _quizSubLevels.value = list
                }
            }
    }

    private fun listenToCategoriesLegacy() {
        db.collection("quiz_categories")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("AdminQuizViewModel", "Listen failed.", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.getString("categoryId") ?: ""
                            val name = doc.getString("categoryName") ?: "Danh mục lỗi"
                            if (id.isEmpty()) null else QuizCategory(id, name)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    _categories.value = list
                }
            }
    }

    // --- CÁC THAO TÁC THÊM / XÓA FIRESTORE THỜI GIAN THỰC ---

    suspend fun addQuizType(id: String, name: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            db.collection("quiz_types").document(id).set(mapOf("id" to id, "name" to name)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteQuizType(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            db.collection("quiz_types").document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addQuizLanguage(id: String, name: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            db.collection("quiz_languages").document(id).set(mapOf("id" to id, "name" to name)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteQuizLanguage(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            db.collection("quiz_languages").document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addQuizSubLevel(
        id: String,
        language: String,
        name: String,
        parent: String,
        requiredPackage: String,
        description: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val data = mapOf(
                "id" to id,
                "language" to language,
                "name" to name,
                "parent" to parent,
                "requiredPackage" to requiredPackage,
                "description" to description
            )
            db.collection("quiz_sub_levels").document(id).set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteQuizSubLevel(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            db.collection("quiz_sub_levels").document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- KHỞI TẠO HỆ THỐNG MẶC ĐỊNH ---
    fun initializeDefaultQuizSystem() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val batch = db.batch()

                // 1. Types
                val defaultTypes = listOf(
                    QuizType("multiple_choice", "Câu hỏi trắc nghiệm"),
                    QuizType("text_input", "Câu hỏi tự luận")
                )
                defaultTypes.forEach { type ->
                    val docRef = db.collection("quiz_types").document(type.id)
                    batch.set(docRef, type)
                }

                // 2. Languages
                val defaultLanguages = listOf(
                    QuizLanguage("japanese", "Tiếng Nhật"),
                    QuizLanguage("english", "Tiếng Anh"),
                    QuizLanguage("chinese", "Tiếng Trung"),
                    QuizLanguage("pali", "Tiếng Pali")
                )
                defaultLanguages.forEach { lang ->
                    val docRef = db.collection("quiz_languages").document(lang.id)
                    batch.set(docRef, lang)
                }

                // 3. Sub Levels
                val defaultSubLevels = listOf(
                    // Japanese
                    QuizSubLevel("QUIZ_JA_N5", "japanese", "Cấp độ N5", "", "FREE", "Học tiếng Nhật nhập môn N5"),
                    QuizSubLevel("QUIZ_JA_N4", "japanese", "Cấp độ N4", "", "VIP_JAPANESE", "Ôn luyện từ vựng ngữ pháp N4"),
                    QuizSubLevel("QUIZ_JA_N3", "japanese", "Cấp độ N3", "", "VIP_JAPANESE", "Cấp độ trung cấp N3 nâng cao"),
                    QuizSubLevel("QUIZ_JA_N2", "japanese", "Cấp độ N2", "", "VIP_JAPANESE", "Đọc hiểu và từ vựng N2"),
                    QuizSubLevel("QUIZ_JA_N1", "japanese", "Cấp độ N1", "", "VIP_JAPANESE", "Chinh phục đỉnh cao N1"),

                    // English - TOEIC
                    QuizSubLevel("QUIZ_TOEIC_450", "english", "TOEIC 450+", "TOEIC", "FREE", "Từ vựng & ngữ pháp cơ bản"),
                    QuizSubLevel("QUIZ_TOEIC_650", "english", "TOEIC 650+", "TOEIC", "VIP_ENGLISH", "Chiến thuật nâng điểm 650+"),
                    QuizSubLevel("QUIZ_TOEIC_800", "english", "TOEIC 800+", "TOEIC", "VIP_ENGLISH", "Chinh phục điểm số cao 800+"),

                    // English - IELTS
                    QuizSubLevel("QUIZ_IELTS_55", "english", "IELTS Band 5.5", "IELTS", "VIP_ENGLISH", "Từ vựng cốt lõi 5.5"),
                    QuizSubLevel("QUIZ_IELTS_65", "english", "IELTS Band 6.5", "IELTS", "VIP_ENGLISH", "Từ vựng học thuật 6.5"),
                    QuizSubLevel("QUIZ_IELTS_75", "english", "IELTS Band 7.5+", "IELTS", "VIP_ENGLISH", "Từ vựng đỉnh cao 7.5+"),

                    // English - Cambridge
                    QuizSubLevel("QUIZ_CAM_KET", "english", "Cambridge KET", "Cambridge", "VIP_ENGLISH", "Trình độ tiếng Anh sơ cấp A2"),
                    QuizSubLevel("QUIZ_CAM_PET", "english", "Cambridge PET", "Cambridge", "VIP_ENGLISH", "Trình độ tiếng Anh trung cấp B1"),

                    // Chinese
                    QuizSubLevel("QUIZ_ZH_BASIC", "chinese", "Trung Cơ Bản", "", "VIP_CHINESE", "Học phát âm và từ vựng HSK 1-2"),
                    QuizSubLevel("QUIZ_ZH_INTER", "chinese", "Trung Trung Cấp", "", "VIP_CHINESE", "Từ vựng học thuật HSK 3-4"),

                    // Pali
                    QuizSubLevel("QUIZ_PA_INTRO", "pali", "Pali Sơ Cấp", "", "VIP_PALI", "Từ vựng kinh điển Pali sơ cấp"),
                    QuizSubLevel("QUIZ_PA_INTER", "pali", "Pali Trung Cấp", "", "VIP_PALI", "Cú pháp và ngữ nghĩa kinh điển")
                )
                defaultSubLevels.forEach { sub ->
                    val docRef = db.collection("quiz_sub_levels").document(sub.id)
                    batch.set(docRef, sub)
                }

                batch.commit().await()
                Log.d("AdminQuizViewModel", "Khởi tạo thành công hệ thống dữ liệu mặc định.")
            } catch (e: Exception) {
                Log.e("AdminQuizViewModel", "Lỗi khởi tạo mặc định toàn hệ thống: ", e)
            }
        }
    }

    // --- GIỮ LẠI CÁC PHƯƠNG THỨC TRƯỚC ĐÓ ĐỂ ĐẢM BẢO KHÔNG LỖI BIÊN DỊCH ---
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
