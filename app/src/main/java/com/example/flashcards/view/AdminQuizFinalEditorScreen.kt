package com.example.flashcards.view

import com.example.flashcards.utils.ContextUtils
import com.example.flashcards.R
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
        "multiple_choice" -> ContextUtils.getString(R.string.ui_text_186)
        "text_input" -> ContextUtils.getString(R.string.ui_text_187)
        else -> quizType
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Biên soạn: $levelName", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Phân nhóm: $displayQuizTypeName | Mã: $levelCode", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
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
                        onClick = { navController.navigate("admin_card_list/$levelCode") },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(ContextUtils.getString(R.string.ui_text_100))
                    }
                    
                    Button(
                        onClick = {
                            scope.launch {
                                isProcessing = true
                                val result = viewModel.clearLevelQuizzes(levelCode)
                                isProcessing = false
                                if (result.isSuccess) {
                                    Toast.makeText(context, ContextUtils.getString(R.string.ui_text_168), Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, ContextUtils.getString(R.string.ui_text_188), Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f),
                        enabled = !isProcessing
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(4.dp))
                        Text(ContextUtils.getString(R.string.ui_text_102), color = Color.White)
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                if (quizType == "multiple_choice") {
                    // --- GIAO DIỆN TRẮC NGHIỆM ---
                    Text(ContextUtils.getString(R.string.ui_text_171), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    
                    OutlinedTextField(
                        value = mcQuestion,
                        onValueChange = { mcQuestion = it },
                        label = { Text(ContextUtils.getString(R.string.ui_text_172)) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessing,
                        shape = RoundedCornerShape(16.dp)
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
                                    enabled = !isProcessing,
                                    shape = RoundedCornerShape(16.dp)
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
                                        Toast.makeText(context, ContextUtils.getString(R.string.ui_text_189), Toast.LENGTH_SHORT).show()
                                        mcQuestion = ""; ansA = ""; ansB = ""; ansC = ""; ansD = ""
                                    } else {
                                        Toast.makeText(context, "Thất bại: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Toast.makeText(context, ContextUtils.getString(R.string.ui_text_190), Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isProcessing
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(ContextUtils.getString(R.string.ui_text_174))
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Text(ContextUtils.getString(R.string.ui_text_175), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(ContextUtils.getString(R.string.ui_text_176), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    OutlinedTextField(
                        value = mcBulkText,
                        onValueChange = { mcBulkText = it },
                        label = { Text(ContextUtils.getString(R.string.ui_text_191)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        enabled = !isProcessing,
                        shape = RoundedCornerShape(16.dp)
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
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        enabled = !isProcessing
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(ContextUtils.getString(R.string.ui_text_178))
                    }

                } else {
                    // --- GIAO DIỆN TỰ LUẬN ---
                    Text(ContextUtils.getString(R.string.ui_text_179), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    
                    OutlinedTextField(
                        value = textQuestion,
                        onValueChange = { textQuestion = it },
                        label = { Text(ContextUtils.getString(R.string.ui_text_180)) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessing,
                        shape = RoundedCornerShape(16.dp)
                    )

                    OutlinedTextField(
                        value = textCorrectAnswer,
                        onValueChange = { textCorrectAnswer = it },
                        label = { Text(ContextUtils.getString(R.string.ui_text_181)) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessing,
                        shape = RoundedCornerShape(16.dp)
                    )

                    Button(
                        onClick = {
                            if (textQuestion.isNotBlank() && textCorrectAnswer.isNotBlank()) {
                                scope.launch {
                                    isProcessing = true
                                    val result = viewModel.addTextInputQuiz(levelCode, textQuestion, textCorrectAnswer)
                                    isProcessing = false
                                    if (result.isSuccess) {
                                        Toast.makeText(context, ContextUtils.getString(R.string.ui_text_192), Toast.LENGTH_SHORT).show()
                                        textQuestion = ""; textCorrectAnswer = ""
                                    } else {
                                        Toast.makeText(context, "Thất bại: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Toast.makeText(context, ContextUtils.getString(R.string.ui_text_193), Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isProcessing
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(ContextUtils.getString(R.string.ui_text_182))
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Text(ContextUtils.getString(R.string.ui_text_183), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(ContextUtils.getString(R.string.ui_text_194), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    OutlinedTextField(
                        value = textBulkText,
                        onValueChange = { textBulkText = it },
                        label = { Text(ContextUtils.getString(R.string.ui_text_191)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        enabled = !isProcessing,
                        shape = RoundedCornerShape(16.dp)
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
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        enabled = !isProcessing
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(ContextUtils.getString(R.string.ui_text_185))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
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
