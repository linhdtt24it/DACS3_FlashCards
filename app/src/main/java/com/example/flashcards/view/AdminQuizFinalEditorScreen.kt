package com.example.flashcards.view

import android.widget.Toast
import androidx.compose.foundation.background
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
fun AdminQuizFinalEditorScreen(
    navController: NavController,
    quizType: String,
    language: String,
    levelCode: String,
    levelName: String,
    viewModel: AdminQuizViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var isProcessing by remember { mutableStateOf(false) }

    // Multiple Choice Inputs
    var mcQuestion by remember { mutableStateOf("") }
    var ansA by remember { mutableStateOf("") }
    var ansB by remember { mutableStateOf("") }
    var ansC by remember { mutableStateOf("") }
    var ansD by remember { mutableStateOf("") }
    var correctAnswer by remember { mutableStateOf("A") }
    var mcBulkText by remember { mutableStateOf("") }

    // Text Input Inputs
    var textQuestion by remember { mutableStateOf("") }
    var textCorrectAnswer by remember { mutableStateOf("") }
    var textBulkText by remember { mutableStateOf("") }

    val displayQuizTypeName = when(quizType) {
        "multiple_choice" -> "Trắc nghiệm"
        "text_input" -> "Tự luận"
        else -> quizType
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Biên soạn: $levelName", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Phân nhóm: $displayQuizTypeName | Mã: $levelCode", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                                } else {
                                    Toast.makeText(context, "Lỗi khi xóa câu hỏi!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (quizType == "multiple_choice") {
                    // --- GIAO DIỆN TRẮC NGHIỆM ---
                    Text("Thêm câu hỏi trắc nghiệm mới", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                    
                    OutlinedTextField(
                        value = mcQuestion,
                        onValueChange = { mcQuestion = it },
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
                            if (mcQuestion.isNotBlank() && ansA.isNotBlank() && ansB.isNotBlank()) {
                                scope.launch {
                                    isProcessing = true
                                    val result = viewModel.addMultipleChoiceQuiz(levelCode, mcQuestion, ansA, ansB, ansC, ansD, correctAnswer)
                                    isProcessing = false
                                    if (result.isSuccess) {
                                        Toast.makeText(context, "Đã lưu câu hỏi trắc nghiệm!", Toast.LENGTH_SHORT).show()
                                        mcQuestion = ""; ansA = ""; ansB = ""; ansC = ""; ansD = ""
                                    } else {
                                        Toast.makeText(context, "Thất bại: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Vui lòng nhập câu hỏi và ít nhất 2 đáp án!", Toast.LENGTH_SHORT).show()
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
                    Text("Định dạng: Câu hỏi | A | B | C | D | Đáp án đúng (A/B/C/D)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    OutlinedTextField(
                        value = mcBulkText,
                        onValueChange = { mcBulkText = it },
                        label = { Text("Dán dữ liệu phân tách dấu '|' tại đây...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 5,
                        enabled = !isProcessing
                    )
                    
                    Button(
                        onClick = {
                            if (mcBulkText.isNotBlank()) {
                                scope.launch {
                                    isProcessing = true
                                    val result = viewModel.bulkImport(levelCode, "multiple_choice", mcBulkText)
                                    isProcessing = false
                                    if (result.isSuccess) {
                                        Toast.makeText(context, "Đã import ${result.getOrNull()} câu hỏi trắc nghiệm!", Toast.LENGTH_SHORT).show()
                                        mcBulkText = ""
                                    } else {
                                        Toast.makeText(context, "Thất bại: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
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

                } else {
                    // --- GIAO DIỆN TỰ LUẬN ---
                    Text("Thêm câu hỏi tự luận mới", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                    
                    OutlinedTextField(
                        value = textQuestion,
                        onValueChange = { textQuestion = it },
                        label = { Text("Nội dung/Từ vựng gợi ý hiển thị") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessing
                    )

                    OutlinedTextField(
                        value = textCorrectAnswer,
                        onValueChange = { textCorrectAnswer = it },
                        label = { Text("Đáp án chữ bắt buộc nhập đúng") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessing
                    )

                    Button(
                        onClick = {
                            if (textQuestion.isNotBlank() && textCorrectAnswer.isNotBlank()) {
                                scope.launch {
                                    isProcessing = true
                                    val result = viewModel.addTextInputQuiz(levelCode, textQuestion, textCorrectAnswer)
                                    isProcessing = false
                                    if (result.isSuccess) {
                                        Toast.makeText(context, "Đã lưu câu hỏi tự luận!", Toast.LENGTH_SHORT).show()
                                        textQuestion = ""; textCorrectAnswer = ""
                                    } else {
                                        Toast.makeText(context, "Thất bại: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Vui lòng điền đầy đủ câu hỏi và đáp án!", Toast.LENGTH_SHORT).show()
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
                    Text("Định dạng: Câu hỏi/Từ vựng | Đáp án đúng", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    OutlinedTextField(
                        value = textBulkText,
                        onValueChange = { textBulkText = it },
                        label = { Text("Dán dữ liệu phân tách dấu '|' tại đây...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 5,
                        enabled = !isProcessing
                    )
                    
                    Button(
                        onClick = {
                            if (textBulkText.isNotBlank()) {
                                scope.launch {
                                    isProcessing = true
                                    val result = viewModel.bulkImport(levelCode, "text_input", textBulkText)
                                    isProcessing = false
                                    if (result.isSuccess) {
                                        Toast.makeText(context, "Đã import ${result.getOrNull()} câu hỏi tự luận!", Toast.LENGTH_SHORT).show()
                                        textBulkText = ""
                                    } else {
                                        Toast.makeText(context, "Thất bại: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
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
            }

            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
