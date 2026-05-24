package com.example.flashcards.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flashcards.repository.AuthRepository
import com.example.flashcards.utils.CryptoUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUserRole = MutableStateFlow<String?>(null)
    val currentUserRole: StateFlow<String?> = _currentUserRole.asStateFlow()

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            fetchUserRole(user.uid)
        }
    }

    private fun fetchUserRole(uid: String) {
        viewModelScope.launch {
            try {
                val document = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .get()
                    .await()
                val encryptedRole = document.getString("role")
                // Giải mã quyền hạn để ứng dụng nhận diện (admin/user)
                _currentUserRole.value = CryptoUtils.decrypt(encryptedRole).ifBlank { "user" }
            } catch (e: Exception) {
                _currentUserRole.value = "user"
            }
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Please fill in all fields")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.login(email, password)
            if (result.isSuccess) {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    fetchUserRole(uid)
                }
                _authState.value = AuthState.Success
            } else {
                _authState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Login failed")
            }
        }
    }

    fun register(name: String, email: String, password: String) {
        if (email.isBlank() || password.isBlank() || name.isBlank()) {
            _authState.value = AuthState.Error("Please fill in all fields")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.register(name, email, password)
            if (result.isSuccess) {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    // MÃ HÓA thông tin người dùng trước khi lưu lên Firestore
                    FirebaseFirestore.getInstance().collection("users").document(uid).set(
                        mapOf(
                            "name" to CryptoUtils.encrypt(name),
                            "email" to CryptoUtils.encrypt(email),
                            "role" to CryptoUtils.encrypt("user"),
                            "premiumStatus" to false,
                            "premiumPackages" to emptyMap<String, Boolean>()
                        )
                    ).await()
                    _currentUserRole.value = "user"
                }
                _authState.value = AuthState.Success
            } else {
                _authState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Registration failed")
            }
        }
    }

    fun signOut() {
        repository.logout()
        _currentUserRole.value = null
        _authState.value = AuthState.Idle
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}
