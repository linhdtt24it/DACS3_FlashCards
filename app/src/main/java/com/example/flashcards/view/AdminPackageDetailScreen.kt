package com.example.flashcards.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
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
import com.example.flashcards.viewmodel.AdminPackageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPackageDetailScreen(
    navController: NavController,
    packageName: String,
    viewModel: AdminPackageViewModel = viewModel()
) {
    val users by viewModel.users.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cấu hình gói: $packageName", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { navController.navigate("manage_users") }, // Chuyển sang màn quản lý user chung
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Thêm tài khoản mới", fontSize = 12.sp)
                    }

                    Button(
                        onClick = { /* Logic xóa tài khoản nếu cần, hoặc điều hướng */ },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PersonRemove, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Xóa tài khoản", fontSize = 12.sp)
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(users) { userData ->
                val uid = userData["uid"] as? String ?: ""
                val email = userData["email"] as? String ?: "No Email"
                val role = userData["role"] as? String ?: "user"
                
                // Kiểm tra trạng thái gói từ Map premiumPackages
                val premiumPackages = userData["premiumPackages"] as? Map<String, Boolean> ?: emptyMap()
                val isActivated = premiumPackages[packageName] ?: false

                UserPackageRow(
                    email = email,
                    role = role,
                    isActivated = isActivated,
                    onToggle = { newValue ->
                        if (uid.isNotEmpty()) {
                            viewModel.togglePackageForUser(uid, packageName, newValue)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun UserPackageRow(
    email: String,
    role: String,
    isActivated: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = email, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    text = if (role == "admin") "ADMIN" else "USER",
                    fontSize = 12.sp,
                    color = if (role == "admin") Color.Red else Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Switch(
                checked = isActivated,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}
