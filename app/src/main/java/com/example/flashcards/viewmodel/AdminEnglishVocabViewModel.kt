package com.example.flashcards.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class EnglishGroup(
    val id: String,
    val name: String,
    val items: List<EnglishCategory>
)

data class EnglishCategory(
    val id: String = "",
    val groupId: String = "",
    val groupName: String = "",
    val name: String = "",
    val code: String = ""
)

class AdminEnglishVocabViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _groupedCategories = MutableStateFlow<List<EnglishGroup>>(emptyList())
    val groupedCategories: StateFlow<List<EnglishGroup>> = _groupedCategories

    init {
        listenCategories()
    }

    private fun listenCategories() {
        db.collection("english_categories")
            .orderBy("groupId")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val list = snapshot.documents.map { doc ->
                        EnglishCategory(
                            id = doc.id,
                            groupId = doc.getString("groupId") ?: "",
                            groupName = doc.getString("groupName") ?: "",
                            name = doc.getString("name") ?: "",
                            code = doc.getString("code") ?: ""
                        )
                    }
                    if (list.isEmpty()) {
                        initializeDefaultCategories()
                    } else {
                        _groupedCategories.value = list.groupBy { it.groupId }.map { (groupId, items) ->
                            EnglishGroup(
                                id = groupId,
                                name = items.firstOrNull()?.groupName ?: groupId,
                                items = items
                            )
                        }
                    }
                }
            }
    }

    private fun initializeDefaultCategories() {
        val defaults = listOf(
            Triple("BASIC", "TIẾNG ANH CƠ BẢN", listOf("Từ vựng Giao tiếp hàng ngày" to "EN_BASIC_COMM", "Từ vựng Ngữ pháp cốt lõi" to "EN_BASIC_GRAM")),
            Triple("TOEIC", "TOEIC", listOf("Mục tiêu TOEIC 450+" to "EN_TOEIC_450", "Mục tiêu TOEIC 650+" to "EN_TOEIC_650", "Mục tiêu TOEIC 800+" to "EN_TOEIC_800")),
            Triple("IELTS", "IELTS", listOf("IELTS Band 5.0 - 6.0" to "EN_IELTS_50_60", "IELTS Band 6.5 - 7.5" to "EN_IELTS_65_75", "IELTS Band 8.0+" to "EN_IELTS_80")),
            Triple("CAMBRIDGE", "CAMBRIDGE", listOf("Cấp độ KET / PET (A2 - B1)" to "EN_CAM_KETPET", "Cấp độ FCE (B2)" to "EN_CAM_FCE", "Cấp độ CAE / CPE (C1 - C2)" to "EN_CAM_CAECPE"))
        )
        
        defaults.forEach { (groupId, groupName, items) ->
            items.forEach { (name, code) ->
                db.collection("english_categories").add(
                    mapOf(
                        "groupId" to groupId,
                        "groupName" to groupName,
                        "name" to name,
                        "code" to code
                    )
                )
            }
        }
    }

    fun addCategory(groupId: String, groupName: String, name: String, code: String) {
        db.collection("english_categories").add(
            mapOf(
                "groupId" to groupId,
                "groupName" to groupName,
                "name" to name,
                "code" to code
            )
        )
    }

    fun deleteCategory(id: String) {
        db.collection("english_categories").document(id).delete()
    }

    suspend fun addManualCard(category: String, front: String, back: String): Result<Unit> {
        return try {
            val card = mapOf(
                "category" to category,
                "front" to front,
                "back" to back,
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
                        "front" to parts[0],
                        "back" to parts[1],
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
