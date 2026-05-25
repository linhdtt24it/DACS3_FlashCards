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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
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
import com.example.flashcards.viewmodel.AdminQuizViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminQuizLanguageScreen(
    navController: NavController,
    quizType: String,
    viewModel: AdminQuizViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val quizLanguages by viewModel.quizLanguages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var langIdInput by remember { mutableStateOf("") }
    var langNameInput by remember { mutableStateOf("") }
    var selectedLangToDelete by remember { mutableStateOf("") }

    val displayQuizTypeName = when(quizType) {
        "multiple_choice" -> "Trắc nghiệm"
        "text_input" -> "Tự luận"
        else -> quizType
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Thêm ngôn ngữ mới", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = langIdInput,
                        onValueChange = { langIdInput = it },
                        label = { Text("Mã Ngôn ngữ (e.g. japanese)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = langNameInput,
                        onValueChange = { langNameInput = it },
                        label = { Text("Tên Ngôn ngữ (e.g. Tiếng Nhật)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (langIdInput.isNotBlank() && langNameInput.isNotBlank()) {
                            scope.launch {
                                val result = viewModel.addQuizLanguage(langIdInput.trim().lowercase(), langNameInput.trim())
                                if (result.isSuccess) {
                                    Toast.makeText(context, "Đã thêm ngôn ngữ!", Toast.LENGTH_SHORT).show()
                                    showAddDialog = false
                                    langIdInput = ""
                                    langNameInput = ""
                                } else {
                                    Toast.makeText(context, "Lỗi: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Vui lòng điền đầy đủ!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Thêm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    if (showDeleteDialog) {
        var expanded by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xóa ngôn ngữ", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Chọn ngôn ngữ muốn xóa:")
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val currentSelectionName = quizLanguages.find { it.id == selectedLangToDelete }?.name ?: "Chọn ngôn ngữ"
                        Button(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(currentSelectionName)
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            quizLanguages.forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text("${lang.name} (${lang.id})") },
                                    onClick = {
                                        selectedLangToDelete = lang.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedLangToDelete.isNotBlank()) {
                            scope.launch {
                                val result = viewModel.deleteQuizLanguage(selectedLangToDelete)
                                if (result.isSuccess) {
                                    Toast.makeText(context, "Đã xóa ngôn ngữ!", Toast.LENGTH_SHORT).show()
                                    showDeleteDialog = false
                                    selectedLangToDelete = ""
                                } else {
                                    Toast.makeText(context, "Lỗi: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Vui lòng chọn ngôn ngữ để xóa!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Xóa")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Chọn Ngôn Ngữ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Phân hệ: $displayQuizTypeName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            BottomAppBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Thêm ngôn ngữ")
                    }
                    Button(
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Xóa ngôn ngữ")
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(quizLanguages) { lang ->
                        Card(
                            onClick = {
                                navController.navigate("admin_quiz_level/$quizType/${lang.id}")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = lang.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Mã ngôn ngữ: ${lang.id}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = Color.Gray
                                )
                            }
                        }
                    }

                    if (quizLanguages.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillParentMaxSize()
                                    .padding(bottom = 100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Chưa có ngôn ngữ nào. Vui lòng thêm ngôn ngữ mới.", color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}
