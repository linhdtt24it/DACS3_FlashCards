package com.example.flashcards.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.flashcards.repository.AuthRepository
import com.example.flashcards.utils.CryptoUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AuthRepository = AuthRepository()
    private val prefs = application.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUserRole = MutableStateFlow<String?>(prefs.getString("user_role", null))
    val currentUserRole: StateFlow<String?> = _currentUserRole.asStateFlow()

    private val _subscribedPackages = MutableStateFlow<List<String>>(
        prefs.getStringSet("subscribed_packages", setOf("FREE"))?.toList() ?: listOf("FREE")
    )
    val subscribedPackages: StateFlow<List<String>> = _subscribedPackages.asStateFlow()

    private val _isAuthChecked = MutableStateFlow(false)
    val isAuthChecked: StateFlow<Boolean> = _isAuthChecked.asStateFlow()

    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        val user = auth.currentUser
        if (user != null) {
            viewModelScope.launch {
                fetchUserDataFromServer(user.uid)
            }
        } else {
            _currentUserRole.value = null
            _isAuthChecked.value = false
        }
    }

    init {
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
    }

    suspend fun fetchUserDataFromServer(uid: String, useCacheIfFailed: Boolean = true) = withContext(Dispatchers.IO) {
        try {
            // Ép lấy dữ liệu từ SERVER để tránh cache cũ
            val snapshot = FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get(Source.SERVER)
                .await()
            
            processUserSnapshot(snapshot)
        } catch (e: Exception) {
            if (useCacheIfFailed) {
                try {
                    val cacheSnapshot = FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .get(Source.CACHE)
                        .await()
                    processUserSnapshot(cacheSnapshot)
                } catch (cacheEx: Exception) {
                    setAuthChecked(true) // Vẫn cho vào với quyền mặc định nếu thất bại hoàn toàn
                }
            } else {
                setAuthChecked(true)
            }
        }
    }

    private suspend fun processUserSnapshot(snapshot: com.google.firebase.firestore.DocumentSnapshot) {
        if (snapshot.exists()) {
            val encryptedRole = snapshot.getString("role")
            val decryptedRole = if (encryptedRole != null) {
                try { CryptoUtils.decrypt(encryptedRole).ifBlank { "user" } } catch (e: Exception) { "user" }
            } else {
                "user"
            }
            
            val packages = snapshot.get("subscribedPackages") as? List<*>
            val packageList = packages?.filterIsInstance<String>() ?: listOf("FREE")
            
            prefs.edit().apply {
                putString("user_role", decryptedRole)
                putStringSet("subscribed_packages", packageList.toSet())
                apply()
            }

            withContext(Dispatchers.Main) {
                _currentUserRole.value = decryptedRole
                _subscribedPackages.value = packageList
                _isAuthChecked.value = true
            }
        } else {
            setAuthChecked(true)
        }
    }

    private fun setAuthChecked(value: Boolean) {
        viewModelScope.launch(Dispatchers.Main) {
            _isAuthChecked.value = value
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Vui lòng điền đầy đủ thông tin")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.login(email, password)
            if (result.isSuccess) {
                // Không cần fetch ở đây vì AuthStateListener sẽ lo
                _authState.value = AuthState.Success
            } else {
                _authState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Đăng nhập thất bại")
            }
        }
    }

    fun register(name: String, email: String, password: String) {
        if (email.isBlank() || password.isBlank() || name.isBlank()) {
            _authState.value = AuthState.Error("Vui lòng điền đầy đủ thông tin")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.register(name, email, password)
            if (result.isSuccess) {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    val initialPackages = listOf("FREE")
                    val encryptedRole = CryptoUtils.encrypt("user")
                    
                    FirebaseFirestore.getInstance().collection("users").document(uid).set(
                        mapOf(
                            "name" to CryptoUtils.encrypt(name),
                            "email" to CryptoUtils.encrypt(email),
                            "role" to encryptedRole,
                            "subscribedPackages" to initialPackages
                        )
                    ).await()
                    
                    _currentUserRole.value = "user"
                    _subscribedPackages.value = initialPackages
                    _isAuthChecked.value = true
                }
                _authState.value = AuthState.Success
            } else {
                _authState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Đăng ký thất bại")
            }
        }
    }

    fun signOut() {
        repository.logout()
        prefs.edit().clear().apply()
        _currentUserRole.value = null
        _subscribedPackages.value = listOf("FREE")
        _isAuthChecked.value = false
        _authState.value = AuthState.Idle
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}
