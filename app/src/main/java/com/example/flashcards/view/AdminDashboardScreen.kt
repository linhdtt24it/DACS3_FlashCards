package com.example.flashcards.view

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.flashcards.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val userRole by authViewModel.currentUserRole.collectAsState()

    // BẢO VỆ RÀO CHẮN: Kiểm tra quyền Admin ngay tại màn hình Dashboard
    LaunchedEffect(userRole) {
        if (userRole != "admin" && userRole != null) {
            navController.navigate("login_screen") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

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
                }
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

            AdminMenuItem(
                icon = Icons.Default.People,
                label = "Quản lý tài khoản",
                onClick = { navigateSafe(navController, "manage_users") }
            )

            AdminMenuItem(
                icon = Icons.Default.CardMembership,
                label = "Quản lý gói học",
                onClick = { navigateSafe(navController, "manage_packages") }
            )

            AdminMenuItem(
                icon = Icons.Default.Translate,
                label = "Từ vựng Tiếng Nhật",
                onClick = { navigateSafe(navController, "vocab_japanese") }
            )

            AdminMenuItem(
                icon = Icons.Default.Language,
                label = "Từ vựng Tiếng Anh",
                onClick = { navigateSafe(navController, "vocab_english") }
            )

            AdminMenuItem(
                icon = Icons.Default.Quiz,
                label = "Quản lý Câu hỏi",
                onClick = { navigateSafe(navController, "admin_quiz_menu") }
            )

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(
                onClick = {
                    authViewModel.signOut()
                    navController.navigate("login_screen") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ĐĂNG XUẤT", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

fun navigateSafe(navController: NavController, route: String) {
    try {
        navController.navigate(route)
    } catch (e: Exception) {
        Log.e("AdminDashboard", "Navigation failed for route: $route", e)
    }
}

@Composable
fun AdminMenuItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(64.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = label, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.weight(1f))
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}
