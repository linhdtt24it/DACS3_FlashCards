package com.example.flashcards.viewmodel

import androidx.lifecycle.ViewModel
import com.example.flashcards.model.AdminUserItem
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

class AdminManageUsersViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val _userList = MutableStateFlow<List<AdminUserItem>>(emptyList())
    val userList: StateFlow<List<AdminUserItem>> = _userList

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        listenToUsers()
    }

    private fun listenToUsers() {
        _isLoading.value = true
        db.collection("users").addSnapshotListener { snapshot, error ->
            if (error != null) {
                _isLoading.value = false
                return@addSnapshotListener
            }
            if (snapshot != null) {
                _userList.value = snapshot.documents.map { doc ->
                    AdminUserItem(
                        id = doc.id,
                        email = doc.getString("email") ?: "",
                        role = doc.getString("role") ?: "user"
                    )
                }
                _isLoading.value = false
            }
        }
    }

    // Yêu cầu 3: Tạo user bên Auth trước, sau đó lấy UID làm Doc ID bên Firestore
    suspend fun addUser(email: String, password: String, context: android.content.Context): Result<Unit> {
        return try {
            // Sử dụng Secondary App để tránh việc Admin hiện tại bị đăng xuất khi tạo tài khoản mới
            val options = FirebaseApp.getInstance().options
            val secondaryApp = try {
                FirebaseApp.initializeApp(context, options, "Secondary")
            } catch (e: Exception) {
                FirebaseApp.getInstance("Secondary")
            }
            val secondaryAuth = FirebaseAuth.getInstance(secondaryApp)

            val authResult = secondaryAuth.createUserWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid ?: return Result.failure(Exception("Không thể lấy UID từ Authentication"))

            // Tạo Document với ID chính là UID (Yêu cầu 1 & 3)
            val userData = mapOf(
                "uid" to uid,
                "email" to email,
                "role" to "user" // Mặc định gán role = "user"
            )
            db.collection("users").document(uid).set(userData).await()
            
            secondaryAuth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Yêu cầu 4: Xóa document dựa theo UID trên Firestore
    suspend fun deleteUser(uid: String): Result<Unit> {
        return try {
            db.collection("users").document(uid).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
