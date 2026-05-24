package com.example.flashcards.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.flashcards.viewmodel.SystemQuiz
import com.example.flashcards.viewmodel.SystemVocabulary
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCardListScreen(
    navController: NavController,
    categoryCode: String
) {
    val db = FirebaseFirestore.getInstance()
    val isQuizMode = categoryCode.startsWith("QUIZ_")
    
    var vocabList by remember { mutableStateOf<List<SystemVocabulary>>(emptyList()) }
    var quizList by remember { mutableStateOf<List<SystemQuiz>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    DisposableEffect(categoryCode) {
        val collectionName = if (isQuizMode) "system_quizzes" else "system_vocabulary"
        val filterField = if (isQuizMode) "level" else "category"
        
        val query = db.collection(collectionName).whereEqualTo(filterField, categoryCode)
        
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                isLoading = false
                return@addSnapshotListener
            }
            if (snapshot != null) {
                if (isQuizMode) {
                    quizList = snapshot.documents.map { doc ->
                        SystemQuiz(
                            id = doc.id,
                            level = doc.getString("level") ?: "",
                            type = doc.getString("type") ?: "",
                            question = doc.getString("question") ?: "",
                            answerA = doc.getString("answerA") ?: "",
                            answerB = doc.getString("answerB") ?: "",
                            answerC = doc.getString("answerC") ?: "",
                            answerD = doc.getString("answerD") ?: "",
                            correctAnswer = doc.getString("correctAnswer") ?: ""
                        )
                    }
                } else {
                    vocabList = snapshot.documents.map { doc ->
                        SystemVocabulary(
                            id = doc.id,
                            category = doc.getString("category") ?: "",
                            front = doc.getString("front") ?: "",
                            back = doc.getString("back") ?: ""
                        )
                    }
                }
                isLoading = false
            }
        }
        onDispose {
            listener.remove()
        }
    }

    val filteredVocabs = remember(searchQuery, vocabList) {
        vocabList.filter { it.front.contains(searchQuery, ignoreCase = true) || it.back.contains(searchQuery, ignoreCase = true) }
    }
    
    val filteredQuizzes = remember(searchQuery, quizList) {
        quizList.filter { it.question.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(if (isQuizMode) "Danh sách Câu hỏi" else "Danh sách Thẻ từ", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(categoryCode, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Tìm kiếm...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Box(modifier = Modifier.weight(1f)) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    val isEmpty = if (isQuizMode) filteredQuizzes.isEmpty() else filteredVocabs.isEmpty()
                    if (isEmpty) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Không có dữ liệu hiển thị", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (isQuizMode) {
                                items(filteredQuizzes) { quiz ->
                                    QuizListItem(quiz = quiz, onDelete = {
                                        db.collection("system_quizzes").document(quiz.id).delete()
                                    })
                                }
                            } else {
                                items(filteredVocabs) { card ->
                                    VocabListItem(card = card, onDelete = {
                                        db.collection("system_vocabulary").document(card.id).delete()
                                    })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VocabListItem(card: SystemVocabulary, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = card.front, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(text = card.back, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
            }
        }
    }
}

@Composable
fun QuizListItem(quiz: SystemQuiz, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = quiz.question, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                if (quiz.type == "multiple_choice") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("A. ${quiz.answerA}", fontSize = 14.sp, color = if(quiz.correctAnswer == "A") Color(0xFF4CAF50) else Color.Unspecified)
                    Text("B. ${quiz.answerB}", fontSize = 14.sp, color = if(quiz.correctAnswer == "B") Color(0xFF4CAF50) else Color.Unspecified)
                    Text("C. ${quiz.answerC}", fontSize = 14.sp, color = if(quiz.correctAnswer == "C") Color(0xFF4CAF50) else Color.Unspecified)
                    Text("D. ${quiz.answerD}", fontSize = 14.sp, color = if(quiz.correctAnswer == "D") Color(0xFF4CAF50) else Color.Unspecified)
                    Text("Đáp án đúng: ${quiz.correctAnswer}", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50), modifier = Modifier.padding(top = 4.dp))
                } else {
                    Text("Đáp án: ${quiz.correctAnswer}", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50), modifier = Modifier.padding(top = 8.dp))
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
            }
        }
    }
}
