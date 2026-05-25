package com.example.flashcards.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.flashcards.utils.CryptoUtils
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

data class JapaneseCategory(
    val id: String = "",
    val name: String = "",
    val code: String = ""
)

class AdminJapaneseVocabViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _categories = MutableStateFlow<List<JapaneseCategory>>(emptyList())
    val categories: StateFlow<List<JapaneseCategory>> = _categories

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        listenCategories()
    }

    private fun listenCategories() {
        _isLoading.value = true
        db.collection("japanese_categories").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("AdminJapaneseVocab", "Listen failed.", error)
                _isLoading.value = false
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        val name = doc.getString("name") ?: "Danh mục không tên"
                        val code = doc.getString("code") ?: ""
                        if (code.isEmpty()) null else JapaneseCategory(id = doc.id, name = name, code = code)
                    } catch (e: Exception) {
                        null
                    }
                }
                
                if (list.isEmpty()) {
                    initializeDefaultCategories()
                } else {
                    _categories.value = list
                }
            }
            _isLoading.value = false
        }
    }

    private fun initializeDefaultCategories() {
        val defaults = listOf(
            "Bảng chữ cái Hiragana" to "hiragana",
            "Bảng chữ cái Katakana" to "katakana",
            "Kanji đơn giản" to "kanji_basic",
            "Từ vựng cơ bản" to "vocab_basic",
            "Từ vựng N5" to "n5",
            "Từ vựng N4" to "n4",
            "Từ vựng N3" to "n3",
            "Từ vựng N2" to "n2",
            "Từ vựng N1" to "n1"
        )
        defaults.forEach { (name, code) ->
            db.collection("japanese_categories").add(mapOf("name" to name, "code" to code))
        }
    }

    fun addCategory(name: String, code: String) {
        db.collection("japanese_categories").add(mapOf("name" to name, "code" to code))
    }

    fun deleteCategory(id: String) {
        db.collection("japanese_categories").document(id).delete()
    }

    suspend fun addManualCard(category: String, front: String, back: String): Result<Unit> {
        return try {
            val card = mapOf(
                "category" to category,
                "front" to CryptoUtils.encrypt(front),
                "back" to CryptoUtils.encrypt(back),
                "createdAt" to Timestamp.now()
            )
            db.collection("system_vocabulary").add(card).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun bulkImport(category: String, rawText: String): Result<Int> {
        return try {
            val lines = rawText.split("\n").filter { it.isNotBlank() }
            val batch = db.batch()
            var count = 0

            lines.forEach { line ->
                val parts = line.split("|").map { it.trim() }
                if (parts.size >= 2) {
                    val docRef = db.collection("system_vocabulary").document()
                    batch.set(docRef, mapOf(
                        "category" to category,
                        "front" to CryptoUtils.encrypt(parts[0]),
                        "back" to CryptoUtils.encrypt(parts[1]),
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

    suspend fun clearCategoryCards(category: String): Result<Unit> {
        return try {
            val snapshot = db.collection("system_vocabulary")
                .whereEqualTo("category", category)
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
