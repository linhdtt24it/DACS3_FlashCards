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
import com.example.flashcards.viewmodel.AdminQuizViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminQuizEditorScreen(
    navController: NavController,
    levelCode: String,
    levelName: String,
    viewModel: AdminQuizViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var isProcessing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Biên soạn câu hỏi: $levelName", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Mã: $levelCode", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
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
                    // CẬP NHẬT: Kích hoạt nút Xem danh sách cho Quiz
                    TextButton(onClick = { 
                        navController.navigate("admin_card_list/$levelCode")
                    }) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Xem danh sách")
                    }
                    
                    Button(
                        onClick = {
                            scope.launch {
                                isProcessing = true
                                val result = viewModel.clearLevelQuizzes(levelCode)
                                isProcessing = false
                                if (result.isSuccess) {
                                    Toast.makeText(context, "Đã xóa sạch câu hỏi trong mục này!", Toast.LENGTH_SHORT).show()
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
                    Text("TRẮC NGHIỆM", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("TỰ LUẬN", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
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
                    MultipleChoiceTab(viewModel, levelCode, isProcessing) { isProcessing = it }
                } else {
                    TextInputTab(viewModel, levelCode, isProcessing) { isProcessing = it }
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

@Composable
fun MultipleChoiceTab(
    viewModel: AdminQuizViewModel,
    levelCode: String,
    isProcessing: Boolean,
    onProcessingChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var question by remember { mutableStateOf("") }
    var ansA by remember { mutableStateOf("") }
    var ansB by remember { mutableStateOf("") }
    var ansC by remember { mutableStateOf("") }
    var ansD by remember { mutableStateOf("") }
    var correctAnswer by remember { mutableStateOf("A") }
    var bulkText by remember { mutableStateOf("") }

    Text("Thêm câu hỏi trắc nghiệm mới", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    
    OutlinedTextField(
        value = question,
        onValueChange = { question = it },
        label = { Text("Nội dung câu hỏi (Dùng ___ cho chỗ trống)") },
        modifier = Modifier.fillMaxWidth(),
        enabled = !isProcessing
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("A", "B", "C", "D").forEach { label ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = correctAnswer == label,
                    onClick = { correctAnswer = label },
                    enabled = !isProcessing
                )
                OutlinedTextField(
                    value = when(label) {
                        "A" -> ansA
                        "B" -> ansB
                        "C" -> ansC
                        else -> ansD
                    },
                    onValueChange = {
                        when(label) {
                            "A" -> ansA = it
                            "B" -> ansB = it
                            "C" -> ansC = it
                            else -> ansD = it
                        }
                    },
                    label = { Text("Đáp án $label") },
                    modifier = Modifier.weight(1f),
                    enabled = !isProcessing
                )
            }
        }
    }

    Button(
        onClick = {
            if (question.isNotBlank() && ansA.isNotBlank() && ansB.isNotBlank()) {
                scope.launch {
                    onProcessingChange(true)
                    val result = viewModel.addMultipleChoiceQuiz(levelCode, question, ansA, ansB, ansC, ansD, correctAnswer)
                    onProcessingChange(false)
                    if (result.isSuccess) {
                        Toast.makeText(context, "Đã lưu câu hỏi!", Toast.LENGTH_SHORT).show()
                        question = ""; ansA = ""; ansB = ""; ansC = ""; ansD = ""
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
        Text("Lưu câu hỏi trắc nghiệm")
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    
    Text("Import hàng loạt (Trắc nghiệm)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    Text("Định dạng: Câu hỏi | A | B | C | D | Đáp án đúng (A/B/C/D)", fontSize = 12.sp)
    
    OutlinedTextField(
        value = bulkText,
        onValueChange = { bulkText = it },
        label = { Text("Dán dữ liệu tại đây...") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 5,
        enabled = !isProcessing
    )
    
    Button(
        onClick = {
            if (bulkText.isNotBlank()) {
                scope.launch {
                    onProcessingChange(true)
                    val result = viewModel.bulkImport(levelCode, "multiple_choice", bulkText)
                    onProcessingChange(false)
                    if (result.isSuccess) {
                        Toast.makeText(context, "Đã import ${result.getOrNull()} câu hỏi!", Toast.LENGTH_SHORT).show()
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
        Text("Xử lý Import Trắc nghiệm")
    }
}

@Composable
fun TextInputTab(
    viewModel: AdminQuizViewModel,
    levelCode: String,
    isProcessing: Boolean,
    onProcessingChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var question by remember { mutableStateOf("") }
    var correct by remember { mutableStateOf("") }
    var bulkText by remember { mutableStateOf("") }

    Text("Thêm câu hỏi tự luận mới", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    
    OutlinedTextField(
        value = question,
        onValueChange = { question = it },
        label = { Text("Nội dung/Từ vựng gợi ý hiển thị") },
        modifier = Modifier.fillMaxWidth(),
        enabled = !isProcessing
    )

    OutlinedTextField(
        value = correct,
        onValueChange = { correct = it },
        label = { Text("Đáp án chữ bắt buộc nhập đúng") },
        modifier = Modifier.fillMaxWidth(),
        enabled = !isProcessing
    )

    Button(
        onClick = {
            if (question.isNotBlank() && correct.isNotBlank()) {
                scope.launch {
                    onProcessingChange(true)
                    val result = viewModel.addTextInputQuiz(levelCode, question, correct)
                    onProcessingChange(false)
                    if (result.isSuccess) {
                        Toast.makeText(context, "Đã lưu câu hỏi!", Toast.LENGTH_SHORT).show()
                        question = ""; correct = ""
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
        Text("Lưu câu hỏi tự luận")
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    
    Text("Import hàng loạt (Tự luận)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    Text("Định dạng: Câu hỏi | Đáp án đúng", fontSize = 12.sp)
    
    OutlinedTextField(
        value = bulkText,
        onValueChange = { bulkText = it },
        label = { Text("Dán dữ liệu tại đây...") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 5,
        enabled = !isProcessing
    )
    
    Button(
        onClick = {
            if (bulkText.isNotBlank()) {
                scope.launch {
                    onProcessingChange(true)
                    val result = viewModel.bulkImport(levelCode, "text_input", bulkText)
                    onProcessingChange(false)
                    if (result.isSuccess) {
                        Toast.makeText(context, "Đã import ${result.getOrNull()} câu hỏi!", Toast.LENGTH_SHORT).show()
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
        Text("Xử lý Import Tự luận")
    }
}
