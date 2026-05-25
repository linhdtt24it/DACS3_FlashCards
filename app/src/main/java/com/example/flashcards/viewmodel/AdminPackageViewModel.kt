package com.example.flashcards.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class PackageItem(
    val id: String = "",
    val name: String = "",
    val key: String = "" // e.g., "FREE", "VIP_ENGLISH", "VIP_JAPANESE"
)

class AdminPackageViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _packages = MutableStateFlow<List<PackageItem>>(emptyList())
    val packages: StateFlow<List<PackageItem>> = _packages

    private val _users = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val users: StateFlow<List<Map<String, Any>>> = _users

    init {
        fetchPackages()
        fetchUsers()
    }

    private fun fetchPackages() {
        // Cấu hình danh sách các gói học theo yêu cầu mới
        _packages.value = listOf(
            PackageItem("1", "Gói Miễn Phí", "FREE"),
            PackageItem("2", "VIP Tiếng Anh", "VIP_ENGLISH"),
            PackageItem("3", "VIP Tiếng Nhật", "VIP_JAPANESE"),
            PackageItem("4", "VIP Tiếng Trung", "VIP_CHINESE"),
            PackageItem("5", "VIP Tiếng Pali", "VIP_PALI")
        )
    }

    private fun fetchUsers() {
        // Lắng nghe thay đổi real-time từ Firestore
        db.collection("users").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) {
                _users.value = snapshot.documents.map { doc ->
                    val data = doc.data?.toMutableMap() ?: mutableMapOf()
                    data["uid"] = doc.id
                    // Đảm bảo subscribedPackages luôn là một mảng
                    if (data["subscribedPackages"] == null) {
                        data["subscribedPackages"] = listOf("FREE")
                    }
                    data
                }
            }
        }
    }

    fun togglePackageForUser(userId: String, packageKey: String, shouldAdd: Boolean) {
        // Không cho phép xóa gói FREE (mặc định)
        if (packageKey == "FREE" && !shouldAdd) return
        
        val docRef = db.collection("users").document(userId)
        if (shouldAdd) {
            docRef.update("subscribedPackages", FieldValue.arrayUnion(packageKey))
        } else {
            docRef.update("subscribedPackages", FieldValue.arrayRemove(packageKey))
        }
    }

    fun addPackage(name: String) {
        val newId = ((_packages.value.mapNotNull { it.id.toIntOrNull() }.maxOrNull() ?: 0) + 1).toString()
        val newKey = "VIP_" + name.uppercase().replace(" ", "_")
        _packages.value = _packages.value + PackageItem(newId, name, newKey)
    }

    fun deletePackage(id: String) {
        _packages.value = _packages.value.filter { it.id != id }
    }
}
