package com.example.flashcards.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
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
import com.example.flashcards.viewmodel.AdminEnglishVocabViewModel
import com.example.flashcards.viewmodel.EnglishCategory
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEnglishVocabMenuScreen(
    navController: NavController,
    viewModel: AdminEnglishVocabViewModel = viewModel()
) {
    val groupedCategories by viewModel.groupedCategories.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var selectedCategory by remember { mutableStateOf<EnglishCategory?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý Tiếng Anh", fontWeight = FontWeight.Bold) },
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
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text(" Thêm")
                    }

                    Button(
                        onClick = { 
                            selectedCategory?.let { viewModel.deleteCategory(it.id) }
                            selectedCategory = null
                        },
                        enabled = selectedCategory != null,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                        Text(" Xóa", color = Color.White)
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    groupedCategories.forEach { group ->
                        item {
                            Text(
                                text = group.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                            )
                        }
                        items(group.items) { category ->
                            val isSelected = selectedCategory?.id == category.id
                            val safeCode = category.code.ifEmpty { "empty" }
                            // RÀO CHẮN: Mã hóa URL để tránh crash route
                            val safeName = URLEncoder.encode(category.name.ifEmpty { "English" }, "UTF-8")

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { 
                                        if (isSelected) {
                                            if (safeCode.isNotEmpty()) {
                                                navController.navigate("english_card_editor/$safeCode/$safeName")
                                            }
                                        } else {
                                            selectedCategory = category
                                        }
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = category.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                                    )
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }

                    if (groupedCategories.isEmpty()) {
                        item {
                            Text(
                                "Chưa có danh mục. Nhấn nút thêm để khởi tạo.",
                                modifier = Modifier.padding(16.dp),
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var newCategoryName by remember { mutableStateOf("") }
        var newCategoryCode by remember { mutableStateOf("") }
        var selectedGroupId by remember { mutableStateOf("") }
        var selectedGroupName by remember { mutableStateOf("") }
        
        val groups = listOf(
            "BASIC" to "TIẾNG ANH CƠ BẢN",
            "TOEIC" to "TOEIC",
            "IELTS" to "IELTS",
            "CAMBRIDGE" to "CAMBRIDGE"
        )

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Thêm cấp bậc mới") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    groups.forEach { (id, name) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { 
                                selectedGroupId = id
                                selectedGroupName = name
                            }
                        ) {
                            RadioButton(selected = selectedGroupId == id, onClick = { 
                                selectedGroupId = id
                                selectedGroupName = name
                            })
                            Text(name)
                        }
                    }
                    OutlinedTextField(value = newCategoryName, onValueChange = { newCategoryName = it }, label = { Text("Tên cấp bậc") })
                    OutlinedTextField(value = newCategoryCode, onValueChange = { newCategoryCode = it }, label = { Text("Mã (ví dụ: en_n5)") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newCategoryName.isNotBlank() && newCategoryCode.isNotBlank() && selectedGroupId.isNotBlank()) {
                        viewModel.addCategory(selectedGroupId, selectedGroupName, newCategoryName, newCategoryCode)
                        showAddDialog = false
                    }
                }) { Text("Thêm") }
            }
        )
    }
}
