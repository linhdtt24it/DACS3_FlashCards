package com.example.flashcards.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(navController: NavController) {
    // Scaffold độc lập, không chứa Bottom Bar của người dùng
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "HỆ THỐNG QUẢN TRỊ ADMIN",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Danh sách các mục quản lý
            AdminMenuItem(
                icon = Icons.Default.People,
                label = "Quản lý tài khoản",
                onClick = { navController.navigate("manage_users") }
            )

            AdminMenuItem(
                icon = Icons.Default.CardMembership,
                label = "Quản lý gói học",
                onClick = { navController.navigate("manage_packages") }
            )

            AdminMenuItem(
                icon = Icons.Default.Translate,
                label = "Từ vựng Tiếng Nhật",
                onClick = { navController.navigate("vocab_japanese") }
            )

            AdminMenuItem(
                icon = Icons.Default.Language,
                label = "Từ vựng Tiếng Anh",
                onClick = { navController.navigate("vocab_english") }
            )

            AdminMenuItem(
                icon = Icons.Default.Brush,
                label = "Từ vựng Tiếng Trung",
                onClick = { navController.navigate("vocab_chinese") }
            )

            AdminMenuItem(
                icon = Icons.Default.MenuBook,
                label = "Từ vựng Tiếng Pali",
                onClick = { navController.navigate("vocab_pali") }
            )

            AdminMenuItem(
                icon = Icons.Default.Quiz,
                label = "Quản lý Câu hỏi",
                onClick = { navController.navigate("manage_questions") }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Nút Đăng xuất ở cuối màn hình
            TextButton(
                onClick = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate("login_screen") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "ĐĂNG XUẤT",
                    color = Color.Red,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun AdminMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}
