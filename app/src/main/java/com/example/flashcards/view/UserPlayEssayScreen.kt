package com.example.flashcards.view

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.flashcards.ui.theme.FlowPrimary
import com.example.flashcards.utils.CryptoUtils
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

data class EssayQuestion(
    val id: String,
    val question: String, // original word, e.g. "食べる"
    val correctAnswer: String, // translation, e.g. "Ăn"
    val explanation: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserPlayEssayScreen(
    navController: NavController,
    categoryId: String,
    playMode: String // "1" = Nhập nghĩa Tiếng Việt, "2" = Nhập từ vựng gốc
) {
    val firestore = remember { FirebaseFirestore.getInstance() }
    var questions by remember { mutableStateOf<List<EssayQuestion>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Load questions from Firestore with local fallback
    LaunchedEffect(categoryId) {
        try {
            val snapshot = firestore.collection("system_quizzes")
                .whereEqualTo("level", categoryId)
                .whereEqualTo("type", "text_input")
                .get()
                .addOnSuccessListener { querySnapshot ->
                    isLoading = false
                    val list = querySnapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.id
                            val rawQuest = doc.getString("question") ?: ""
                            val rawAns = doc.getString("correctAnswer") ?: ""
                            val question = CryptoUtils.decrypt(rawQuest)
                            val answer = CryptoUtils.decrypt(rawAns)
                            if (question.isNotEmpty() && answer.isNotEmpty()) {
                                EssayQuestion(id, question, answer)
                            } else null
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (list.isNotEmpty()) {
                        questions = list
                    } else {
                        // Fallback to defaults
                        questions = getFallbackEssayQuestions(categoryId)
                    }
                }
                .addOnFailureListener {
                    isLoading = false
                    questions = getFallbackEssayQuestions(categoryId)
                }
        } catch (e: Exception) {
            isLoading = false
            questions = getFallbackEssayQuestions(categoryId)
        }
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var userAnswer by remember { mutableStateOf("") }
    var isAnswerChecked by remember { mutableStateOf(false) }

    val isFinished = currentIndex >= questions.size
    val progress = if (questions.isNotEmpty()) (currentIndex.toFloat() / questions.size) else 0f

    val title = when (categoryId) {
        "QUIZ_JA_N5" -> "Tự luận Tiếng Nhật N5"
        "QUIZ_JA_N4" -> "Tự luận Tiếng Nhật N4"
        "QUIZ_JA_N3" -> "Tự luận Tiếng Nhật N3"
        "QUIZ_JA_N2" -> "Tự luận Tiếng Nhật N2"
        "QUIZ_JA_N1" -> "Tự luận Tiếng Nhật N1"
        "QUIZ_TOEIC_450" -> "Tự luận TOEIC 450+"
        "QUIZ_TOEIC_650" -> "Tự luận TOEIC 650+"
        "QUIZ_TOEIC_800" -> "Tự luận TOEIC 800+"
        "QUIZ_IELTS_55" -> "Tự luận IELTS Band 5.5"
        "QUIZ_IELTS_65" -> "Tự luận IELTS Band 6.5"
        "QUIZ_IELTS_75" -> "Tự luận IELTS Band 7.5+"
        "QUIZ_ZH_BASIC" -> "Tự luận HSK Trung Cơ Bản"
        "QUIZ_PA_INTRO" -> "Tự luận Pali Sơ Cấp"
        else -> "Tự luận Ôn tập"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    if (!isFinished && questions.isNotEmpty()) {
                        Text(
                            text = "${currentIndex + 1}/${questions.size}",
                            modifier = Modifier.padding(end = 16.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FlowPrimary)
                }
            } else if (questions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Mục này hiện chưa có câu hỏi tự luận.", color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { navController.popBackStack() }) {
                            Text("Quay lại")
                        }
                    }
                }
            } else if (!isFinished) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = Color(0xFF10B981), // Emerald green for essay progress
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(32.dp))

                val currentQuestion = questions[currentIndex]

                // Xác định nội dung hiển thị & đáp án đối chiếu dựa trên chế độ chơi
                // Chế độ 1: Hiển thị từ gốc -> Đoán nghĩa Tiếng Việt
                // Chế độ 2: Hiển thị nghĩa Tiếng Việt -> Đoán từ gốc
                val promptText = if (playMode == "1") currentQuestion.question else currentQuestion.correctAnswer
                val expectedAnswer = if (playMode == "1") currentQuestion.correctAnswer else currentQuestion.question

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (playMode == "1") "Gõ nghĩa Tiếng Việt của từ sau:" else "Gõ từ vựng gốc có nghĩa sau:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = promptText,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Trạng thái kiểm tra câu trả lời
                val isCorrect = checkAnswerCorrect(userAnswer, expectedAnswer)

                // Thiết lập màu sắc viền dựa trên kết quả
                val borderStrokeColor = when {
                    isAnswerChecked && isCorrect -> Color(0xFF10B981) // Green border
                    isAnswerChecked && !isCorrect -> Color(0xFFEF4444) // Red border
                    else -> MaterialTheme.colorScheme.outline
                }

                OutlinedTextField(
                    value = userAnswer,
                    onValueChange = { if (!isAnswerChecked) userAnswer = it },
                    placeholder = { Text("Nhập câu trả lời của bạn tại đây...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    enabled = !isAnswerChecked,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = borderStrokeColor,
                        unfocusedBorderColor = borderStrokeColor,
                        disabledBorderColor = borderStrokeColor
                    )
                )

                // Feedback panel
                if (isAnswerChecked) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCorrect) Color(0xFFECFDF5) else Color(0xFFFEF2F2)
                        ),
                        border = BorderStroke(1.dp, if (isCorrect) Color(0xFFD1FAE5) else Color(0xFFFEE2E2))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (isCorrect) Color(0xFF10B981) else Color(0xFFEF4444),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isCorrect) "Chính xác tuyệt vời!" else "Chưa chính xác!",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCorrect) Color(0xFF065F46) else Color(0xFF991B1B)
                                )
                                if (!isCorrect) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Đáp án đúng: $expectedAnswer",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFB91C1C)
                                    )
                                }
                            }
                        }
                    }

                    // Next/Finish button
                    Button(
                        onClick = {
                            if (isCorrect) score++
                            currentIndex++
                            userAnswer = ""
                            isAnswerChecked = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCorrect) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                    ) {
                        Text(
                            text = if (currentIndex == questions.size - 1) "Hoàn thành" else "Tiếp theo",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                } else {
                    // Check button
                    Button(
                        onClick = {
                            if (userAnswer.isNotBlank()) {
                                isAnswerChecked = true
                            }
                        },
                        enabled = userAnswer.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary)
                    ) {
                        Text("Kiểm tra đáp án", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            } else {
                // Kết quả chung cuộc
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Hoàn Thành Tự Luận!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    val percentage = if (questions.isNotEmpty()) (score.toFloat() / questions.size * 100).toInt() else 0

                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                        CircularProgressIndicator(
                            progress = { percentage / 100f },
                            modifier = Modifier.fillMaxSize(),
                            color = Color(0xFF10B981),
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            strokeWidth = 14.dp
                        )
                        Text(
                            text = "$percentage%",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Bạn đã viết đúng $score trên tổng số ${questions.size} từ vựng",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(48.dp))
                    Button(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary)
                    ) {
                        Text("Quay lại thư viện", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

// Fallback essay questions synchronized with QuizGenerator
fun getFallbackEssayQuestions(categoryId: String): List<EssayQuestion> {
    return when {
        categoryId.startsWith("QUIZ_JA") -> {
            when (categoryId) {
                "QUIZ_JA_N5" -> listOf(
                    EssayQuestion(UUID.randomUUID().toString(), "食べる (taberu)", "Ăn"),
                    EssayQuestion(UUID.randomUUID().toString(), "飲む (nomu)", "Uống"),
                    EssayQuestion(UUID.randomUUID().toString(), "行く (iku)", "Đi"),
                    EssayQuestion(UUID.randomUUID().toString(), "見る (miru)", "Xem / Nhìn"),
                    EssayQuestion(UUID.randomUUID().toString(), "先生 (sensei)", "Giáo viên")
                )
                "QUIZ_JA_N4" -> listOf(
                    EssayQuestion(UUID.randomUUID().toString(), "覚える (oboyeru)", "Nhớ / Ghi nhớ"),
                    EssayQuestion(UUID.randomUUID().toString(), "簡単 (kantan)", "Đơn giản / Dễ dàng"),
                    EssayQuestion(UUID.randomUUID().toString(), "重i (omoi)", "Nặng"),
                    EssayQuestion(UUID.randomUUID().toString(), "軽い (karui)", "Nhẹ")
                )
                else -> listOf(
                    EssayQuestion(UUID.randomUUID().toString(), "一生懸命 (isshoukenmei)", "Nỗ lực hết sức"),
                    EssayQuestion(UUID.randomUUID().toString(), "調査 (chousa)", "Điều tra / Khảo sát"),
                    EssayQuestion(UUID.randomUUID().toString(), "緊張 (kinchou)", "Căng thẳng / Hồi hộp")
                )
            }
        }
        categoryId.startsWith("QUIZ_TOEIC") -> {
            when (categoryId) {
                "QUIZ_TOEIC_450" -> listOf(
                    EssayQuestion(UUID.randomUUID().toString(), "Confirm", "Xác nhận"),
                    EssayQuestion(UUID.randomUUID().toString(), "Submit", "Nộp / Trình"),
                    EssayQuestion(UUID.randomUUID().toString(), "Delay", "Trì hoãn")
                )
                else -> listOf(
                    EssayQuestion(UUID.randomUUID().toString(), "Negotiate", "Thương lượng / Đàm phán"),
                    EssayQuestion(UUID.randomUUID().toString(), "Implement", "Thi hành / Thực hiện"),
                    EssayQuestion(UUID.randomUUID().toString(), "Collaborate", "Hợp tác")
                )
            }
        }
        categoryId.startsWith("QUIZ_IELTS") -> {
            listOf(
                EssayQuestion(UUID.randomUUID().toString(), "Analyze", "Phân tích"),
                EssayQuestion(UUID.randomUUID().toString(), "Synthesize", "Tổng hợp"),
                EssayQuestion(UUID.randomUUID().toString(), "Hypothesis", "Giả thuyết")
            )
        }
        categoryId == "QUIZ_ZH_BASIC" -> {
            listOf(
                EssayQuestion(UUID.randomUUID().toString(), "你好 (nǐ hǎo)", "Xin chào"),
                EssayQuestion(UUID.randomUUID().toString(), "谢谢 (xièxie)", "Cảm ơn"),
                EssayQuestion(UUID.randomUUID().toString(), "再见 (zàijiàn)", "Tạm biệt")
            )
        }
        categoryId == "QUIZ_PA_INTRO" -> {
            listOf(
                EssayQuestion(UUID.randomUUID().toString(), "Buddha", "Đức Phật / Bậc Giác Ngộ"),
                EssayQuestion(UUID.randomUUID().toString(), "Dhamma", "Giáo Pháp"),
                EssayQuestion(UUID.randomUUID().toString(), "Sangha", "Tăng Đoàn")
            )
        }
        else -> {
            listOf(
                EssayQuestion(UUID.randomUUID().toString(), "Hello", "Xin chào")
            )
        }
    }
}

fun checkAnswerCorrect(user: String, expected: String): Boolean {
    val userClean = user.trim().lowercase()
    val expectedClean = expected.trim().lowercase()
    
    // 1. Khớp chính xác hoàn toàn (không phân biệt hoa thường)
    if (userClean == expectedClean) return true
    
    // 2. Khớp sau khi loại bỏ tất cả dấu diacritics (cho tiếng Anh, Pali, tiếng Trung Pinyin, tiếng Việt không dấu)
    if (normalizeForComparison(user) == normalizeForComparison(expected)) return true
    
    // 3. Khớp một trong hai vế trước hoặc trong ngoặc đơn (ví dụ: "食べる (taberu)" -> khớp "食べる" hoặc "taberu")
    if (expected.contains("(") && expected.contains(")")) {
        val mainWord = expected.substringBefore("(").trim()
        val secondaryWord = expected.substringAfter("(").substringBefore(")").trim()
        
        if (userClean == mainWord.lowercase() || 
            normalizeForComparison(user) == normalizeForComparison(mainWord)) {
            return true
        }
        
        if (userClean == secondaryWord.lowercase() || 
            normalizeForComparison(user) == normalizeForComparison(secondaryWord)) {
            return true
        }
    }
    
    // 4. Khớp các phương án thay thế cách nhau bởi dấu gạch chéo "/" (ví dụ: "Xem / Nhìn" -> khớp "xem" hoặc "nhìn")
    if (expected.contains("/")) {
        val alternatives = expected.split("/").map { it.trim() }
        for (alt in alternatives) {
            if (userClean == alt.lowercase() || 
                normalizeForComparison(user) == normalizeForComparison(alt)) {
                return true
            }
        }
    }
    
    return false
}

fun normalizeForComparison(str: String): String {
    val temp = java.text.Normalizer.normalize(str.trim().lowercase(), java.text.Normalizer.Form.NFD)
    return "\\p{InCombiningDiacriticalMarks}+".toRegex().replace(temp, "")
}
