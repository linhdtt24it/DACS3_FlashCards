package com.example.flashcards.view

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore

// Data class tạm thời để hứng dữ liệu User từ Firestore về hiển thị
data class AdminUserItem(
    val id: String = "",
    val email: String = "",
    val role: String = "user",
    val premiumStatus: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminManageUsersScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    var userList by remember { mutableStateOf<List<AdminUserItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Tự động lội vào Firestore kéo sạch danh sách tài khoản về khi mở màn hình lên
    LaunchedEffect(Unit) {
        db.collection("users").addSnapshotListener { snapshot, error ->
            if (error != null) {
                isLoading = false
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.map { doc ->
                    AdminUserItem(
                        id = doc.id,
                        email = doc.getString("email") ?: "No Email",
                        role = doc.getString("role") ?: "user",
                        premiumStatus = doc.getBoolean("premiumStatus") ?: false
                    )
                }
                userList = list
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý User & Gói VIP", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(userList) { user ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.email, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Quyền: ${user.role.uppercase()}", fontSize = 12.sp, color = Color.Gray)
                                Text(
                                    text = if (user.premiumStatus) "Trạng thái: VIP 👑" else "Trạng thái: Thường",
                                    fontSize = 12.sp,
                                    color = if (user.premiumStatus) Color(0xFFFFB300) else Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Nút bấm kích hoạt / hủy kích hoạt gói VIP cho User đó thẳng lên Firestore
                            IconButton(
                                onClick = {
                                    val nextStatus = !user.premiumStatus
                                    db.collection("users").document(user.id)
                                        .update("premiumStatus", nextStatus)
                                        .addOnSuccessListener {
                                            Toast.makeText(context, "Đã cập nhật trạng thái VIP!", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            ) {
                                Icon(
                                    imageVector = if (user.premiumStatus) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Toggle VIP",
                                    tint = if (user.premiumStatus) Color(0xFFFFB300) else Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
