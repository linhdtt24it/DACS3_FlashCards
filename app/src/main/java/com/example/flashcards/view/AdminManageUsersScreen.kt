package com.example.flashcards.view

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore

// 1. Kiến trúc dữ liệu: Data class hứng dữ liệu từ Firestore
data class AdminUserItem(
    val id: String = "",
    val email: String = "",
    val role: String = "user"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminManageUsersScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    
    // Trạng thái danh sách và tải dữ liệu
    var userList by remember { mutableStateOf<List<AdminUserItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Trạng thái chọn User để xóa
    var selectedUserId by remember { mutableStateOf<String?>(null) }
    
    // Trạng thái hiển thị Dialog
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Logic lắng nghe Real-time từ Firestore
    LaunchedEffect(Unit) {
        db.collection("users").addSnapshotListener { snapshot, error ->
            if (error != null) {
                isLoading = false
                return@addSnapshotListener
            }
            if (snapshot != null) {
                userList = snapshot.documents.map { doc ->
                    AdminUserItem(
                        id = doc.id,
                        email = doc.getString("email") ?: "",
                        role = doc.getString("role") ?: "user"
                    )
                }
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý tài khoản", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            // 3. Hàng chức năng dưới đáy
            BottomAppBar(
                actions = {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Nút Thêm tài khoản
                        Button(
                            onClick = { showAddDialog = true },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Thêm tài khoản")
                        }

                        // Nút Xóa tài khoản (Chỉ sáng khi đã chọn user)
                        Button(
                            onClick = { showDeleteConfirmDialog = true },
                            enabled = selectedUserId != null,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(4.dp))
                            Text("Xóa", color = Color.White)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            // 2. Giao diện danh sách (LazyColumn)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(userList) { user ->
                    val isSelected = selectedUserId == user.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedUserId = if (isSelected) null else user.id },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = user.email,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (user.role == "admin") "Quyền: ADMIN" else "Quyền: USER",
                                fontSize = 14.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }

    // --- DIALOGS ---

    // Dialog Thêm User
    if (showAddDialog) {
        var newEmail by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Thêm tài khoản mới") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Mật khẩu") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newEmail.isNotEmpty()) {
                        val newUser = mapOf("email" to newEmail, "role" to "user")
                        db.collection("users").add(newUser)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Đã thêm tài khoản!", Toast.LENGTH_SHORT).show()
                                showAddDialog = false
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Lỗi khi thêm!", Toast.LENGTH_SHORT).show()
                            }
                    }
                }) { Text("Xác nhận") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Hủy") }
            }
        )
    }

    // Dialog Xác nhận Xóa
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Xác nhận xóa") },
            text = { Text("Bạn có chắc chắn muốn xóa tài khoản này không?") },
            confirmButton = {
                TextButton(onClick = {
                    selectedUserId?.let { id ->
                        db.collection("users").document(id).delete()
                            .addOnSuccessListener {
                                Toast.makeText(context, "Đã xóa tài khoản!", Toast.LENGTH_SHORT).show()
                                selectedUserId = null
                                showDeleteConfirmDialog = false
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Lỗi khi xóa!", Toast.LENGTH_SHORT).show()
                            }
                    }
                }) { Text("Xác nhận", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Hủy") }
            }
        )
    }
}
