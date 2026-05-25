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
import com.example.flashcards.viewmodel.AdminJapaneseVocabViewModel
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminJapaneseVocabMenuScreen(
    navController: NavController,
    viewModel: AdminJapaneseVocabViewModel = viewModel()
) {
    val categories by viewModel.categories.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý Tiếng Nhật", fontWeight = FontWeight.Bold) },
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
                    Button(onClick = { showAddDialog = true }, shape = RoundedCornerShape(8.dp)) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text(" Thêm")
                    }
                    Button(
                        onClick = { 
                            selectedCategoryId?.let { viewModel.deleteCategory(it) }
                            selectedCategoryId = null
                        },
                        enabled = selectedCategoryId != null,
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
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = selectedCategoryId == category.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    if (isSelected) {
                                        // RÀO CHẮN AN TOÀN: Mã hóa URL để tránh crash route
                                        val safeCode = category.code.ifEmpty { "n5" }
                                        val safeName = URLEncoder.encode(category.name.ifEmpty { "Japan" }, "UTF-8")
                                        navController.navigate("japanese_card_editor/$safeCode/$safeName")
                                    } else {
                                        selectedCategoryId = category.id
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(category.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = if (isSelected) Color.Blue else Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var code by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Thêm danh mục") },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Tên") })
                    OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Mã (ví dụ: n5)") })
                }
            },
            confirmButton = {
                TextButton(onClick = { if(name.isNotBlank()) { viewModel.addCategory(name, code); showAddDialog = false } }) { Text("Thêm") }
            }
        )
    }
}
