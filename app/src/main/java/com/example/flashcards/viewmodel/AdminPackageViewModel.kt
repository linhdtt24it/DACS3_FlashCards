package com.example.flashcards.viewmodel

import androidx.lifecycle.ViewModel
import com.example.flashcards.model.AdminUserItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class PackageItem(
    val id: String = "",
    val name: String = ""
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
        // Lắng nghe danh sách gói từ collection "packages"
        db.collection("packages").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val list = snapshot.documents.map { doc ->
                    PackageItem(id = doc.id, name = doc.getString("name") ?: "")
                }
                if (list.isEmpty()) {
                    // Nếu trống, khởi tạo các gói mặc định như yêu cầu
                    initializeDefaultPackages()
                } else {
                    _packages.value = list
                }
            }
        }
    }

    private fun initializeDefaultPackages() {
        val defaults = listOf("Vip Nhật", "Vip Anh", "Vip Trung", "Vip Pali", "Gói Free")
        defaults.forEach { name ->
            db.collection("packages").add(mapOf("name" to name))
        }
    }

    private fun fetchUsers() {
        db.collection("users").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                _users.value = snapshot.documents.map { it.data ?: emptyMap() }
            }
        }
    }

    fun togglePackageForUser(userId: String, packageName: String, isEnabled: Boolean) {
        // Cập nhật trường premiumPackages.{packageName} trên Firestore
        // Ví dụ: premiumPackages: { "Vip Nhật": true }
        db.collection("users").document(userId)
            .update("premiumPackages.$packageName", isEnabled)
    }

    fun addPackage(name: String) {
        db.collection("packages").add(mapOf("name" to name))
    }

    fun deletePackage(packageId: String) {
        db.collection("packages").document(packageId).delete()
    }
}
