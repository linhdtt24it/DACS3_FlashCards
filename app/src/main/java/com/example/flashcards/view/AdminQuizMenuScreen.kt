package com.example.flashcards.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.flashcards.viewmodel.AdminQuizViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminQuizMenuScreen(
    navController: NavController,
    viewModel: AdminQuizViewModel = viewModel()
) {
    // 2. Bổ sung giao diện Chờ tải dữ liệu (Loading State)
    val categories by viewModel.categories.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý Câu hỏi Trắc nghiệm", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // 3. Nút hỗ trợ khởi tạo dữ liệu mẫu
                    IconButton(onClick = { viewModel.initializeSampleCategories() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Khởi tạo dữ liệu")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                // Hiển thị loading ở chính giữa
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(categories) { category ->
                        // 1. Rào chắn Null Safety: Nếu ID hoặc Name trống thì hiển thị mặc định
                        val catId = category.categoryId.ifEmpty { "" }
                        val catName = category.categoryName.ifEmpty { "Danh mục lỗi" }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // 4. Xử lý an toàn cho nút bấm điều hướng
                                    if (catId.isNotEmpty()) {
                                        navController.navigate("admin_quiz_editor/$catId/$catName")
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = catName,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = Color.Gray
                                )
                            }
                        }
                    }

                    if (categories.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillParentMaxSize()
                                    .padding(bottom = 100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Chưa có danh mục nào. Hãy nhấn nút Refresh để khởi tạo.")
                            }
                        }
                    }
                }
            }
        }
    }
}
