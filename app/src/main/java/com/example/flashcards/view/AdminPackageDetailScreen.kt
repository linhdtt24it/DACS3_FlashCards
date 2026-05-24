package com.example.flashcards.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.flashcards.utils.CryptoUtils
import com.example.flashcards.viewmodel.AdminPackageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPackageDetailScreen(
    navController: NavController,
    packageName: String,
    viewModel: AdminPackageViewModel = viewModel()
) {
    val users by viewModel.users.collectAsState()

    // Lọc danh sách: Chỉ hiển thị tài khoản người dùng (role == "user")
    // Sử dụng CryptoUtils.decrypt để kiểm tra quyền hạn thực tế
    val filteredUsers = remember(users) {
        users.filter { userData ->
            val encryptedRole = userData["role"] as? String
            CryptoUtils.decrypt(encryptedRole) == "user"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Quản lý VIP: $packageName", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Danh sách học viên", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (filteredUsers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Chưa có tài khoản người dùng nào.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(filteredUsers) { userData ->
                    val uid = userData["uid"] as? String ?: ""
                    
                    // Giải mã thông tin hiển thị nếu cần
                    val email = CryptoUtils.decrypt(userData["email"] as? String)
                    val name = CryptoUtils.decrypt(userData["name"] as? String).ifBlank { "Học viên" }
                    
                    // Kiểm tra trạng thái gói VIP từ Map premiumPackages (Real-time sync qua addSnapshotListener)
                    val premiumPackages = userData["premiumPackages"] as? Map<String, Boolean> ?: emptyMap()
                    val isActivated = premiumPackages[packageName] ?: false

                    UserPackageActionRow(
                        name = name,
                        email = email,
                        isActivated = isActivated,
                        onAction = {
                            if (uid.isNotEmpty()) {
                                // Gọi cập nhật trực tiếp lên Firestore
                                viewModel.togglePackageForUser(uid, packageName, !isActivated)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun UserPackageActionRow(
    name: String,
    email: String,
    isActivated: Boolean,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActivated) Color(0xFFFFF5F5) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = email,
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }

            if (!isActivated) {
                // Button Thêm vào gói - Màu Xanh lá
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("[Thêm vào gói VIP]", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                // Button Kích khỏi gói - Màu Đỏ
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("[Kích khỏi gói VIP]", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
