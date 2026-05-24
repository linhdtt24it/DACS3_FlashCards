package com.example.flashcards.view

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.flashcards.viewmodel.AdminEnglishVocabViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEnglishCardEditorScreen(
    navController: NavController,
    categoryCode: String,
    categoryName: String,
    viewModel: AdminEnglishVocabViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    
    // Manual Input State
    var frontText by remember { mutableStateOf("") }
    var backText by remember { mutableStateOf("") }
    
    // Bulk Import State
    var bulkText by remember { mutableStateOf("") }
    
    var isProcessing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Biên soạn: $categoryName", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Mã: $categoryCode", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                },
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
                    TextButton(onClick = { 
                        navController.navigate("admin_card_list/$categoryCode") 
                    }) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Xem danh sách")
                    }
                    
                    Button(
                        onClick = {
                            scope.launch {
                                isProcessing = true
                                val result = viewModel.clearCategoryCards(categoryCode)
                                isProcessing = false
                                if (result.isSuccess) {
                                    Toast.makeText(context, "Đã xóa sạch thẻ trong mục này!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isProcessing
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(4.dp))
                        Text("Xóa sạch", color = Color.White)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("NHẬP THỦ CÔNG", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("IMPORT HÀNG LOẠT", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (selectedTab == 0) {
                    Text("Thêm thẻ mới vào kho hệ thống", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    
                    OutlinedTextField(
                        value = frontText,
                        onValueChange = { frontText = it },
                        label = { Text("Mặt trước (Từ vựng Tiếng Anh)") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessing
                    )
                    
                    OutlinedTextField(
                        value = backText,
                        onValueChange = { backText = it },
                        label = { Text("Mặt sau (Nghĩa / Phiên âm / Ví dụ)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        enabled = !isProcessing
                    )
                    
                    Button(
                        onClick = {
                            if (frontText.isNotBlank() && backText.isNotBlank()) {
                                scope.launch {
                                    isProcessing = true
                                    val result = viewModel.addManualCard(categoryCode, frontText, backText)
                                    isProcessing = false
                                    if (result.isSuccess) {
                                        Toast.makeText(context, "Đã thêm thẻ thành công!", Toast.LENGTH_SHORT).show()
                                        frontText = ""
                                        backText = ""
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isProcessing
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Thêm vào kho hệ thống")
                    }
                } else {
                    Text("Import từ vựng từ văn bản", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Định dạng: Mặt trước | Mặt sau\nMỗi thẻ nằm trên một dòng mới.",
                            modifier = Modifier.padding(12.dp),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    OutlinedTextField(
                        value = bulkText,
                        onValueChange = { bulkText = it },
                        label = { Text("Dán dữ liệu tại đây...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 10,
                        placeholder = { Text("Ví dụ:\nHello | Xin chào\nApple | Quả táo /'æpl/") },
                        enabled = !isProcessing
                    )
                    
                    Button(
                        onClick = {
                            if (bulkText.isNotBlank()) {
                                scope.launch {
                                    isProcessing = true
                                    val result = viewModel.bulkImport(categoryCode, bulkText)
                                    isProcessing = false
                                    if (result.isSuccess) {
                                        val count = result.getOrNull() ?: 0
                                        Toast.makeText(context, "Đã import thành công $count từ vựng!", Toast.LENGTH_LONG).show()
                                        bulkText = ""
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        enabled = !isProcessing
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Xử lý Import Hàng Loạt")
                    }
                }
                
                if (isProcessing) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}
