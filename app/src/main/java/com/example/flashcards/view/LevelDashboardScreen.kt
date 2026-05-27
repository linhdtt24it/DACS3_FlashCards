package com.example.flashcards.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.flashcards.ui.theme.FlowPrimary
import com.example.flashcards.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelDashboardScreen(
    navController: NavController,
    language: String,
    levelId: String
) {
    // Normalize level ID and title
    val normalizedLevelId = remember(levelId, language) {
        if (levelId.startsWith("QUIZ_")) levelId else {
            when (language.uppercase()) {
                "JAPANESE" -> {
                    val clean = levelId.replace("Cấp độ ", "").replace("Cáp độ ", "").trim()
                    "QUIZ_JA_$clean"
                }
                "ENGLISH" -> {
                    if (levelId.contains("450")) "QUIZ_TOEIC_450"
                    else if (levelId.contains("650")) "QUIZ_TOEIC_650"
                    else if (levelId.contains("800")) "QUIZ_TOEIC_800"
                    else if (levelId.contains("5.5")) "QUIZ_IELTS_55"
                    else if (levelId.contains("6.5")) "QUIZ_IELTS_65"
                    else if (levelId.contains("7.5")) "QUIZ_IELTS_75"
                    else "QUIZ_TOEIC_450"
                }
                "CHINESE" -> "QUIZ_ZH_BASIC"
                "PALI" -> "QUIZ_PA_INTRO"
                else -> levelId
            }
        }
    }

    val prettyLang = when (language.uppercase()) {
        "JAPANESE" -> "Tiếng Nhật"
        "ENGLISH" -> "Tiếng Anh"
        "CHINESE" -> "Tiếng Trung"
        "PALI" -> "Tiếng Pali"
        else -> language
    }

    val prettyLevel = when (normalizedLevelId) {
        "QUIZ_JA_N5" -> "Cấp độ N5"
        "QUIZ_JA_N4" -> "Cấp độ N4"
        "QUIZ_JA_N3" -> "Cấp độ N3"
        "QUIZ_JA_N2" -> "Cấp độ N2"
        "QUIZ_JA_N1" -> "Cấp độ N1"
        "QUIZ_TOEIC_450" -> "TOEIC 450+"
        "QUIZ_TOEIC_650" -> "TOEIC 650+"
        "QUIZ_TOEIC_800" -> "TOEIC 800+"
        "QUIZ_IELTS_55" -> "IELTS Band 5.5"
        "QUIZ_IELTS_65" -> "IELTS Band 6.5"
        "QUIZ_IELTS_75" -> "IELTS Band 7.5+"
        "QUIZ_ZH_BASIC" -> "Trung Cơ Bản"
        "QUIZ_PA_INTRO" -> "Pali Sơ Cấp"
        else -> levelId.replace("QUIZ_", "").replace("_", " ")
    }

    val screenTitle = "$prettyLang - $prettyLevel"
    var showEssayBottomSheet by remember { mutableStateOf(false) }

    val firestore = remember { FirebaseFirestore.getInstance() }
    val uid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    var totalCardsCount by remember { mutableIntStateOf(0) }
    var learnedCardsCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(normalizedLevelId, uid) {
        val category = getVocabCategoryFromLevelId(normalizedLevelId)
        firestore.collection("system_vocabulary")
            .whereIn("category", listOf(category, normalizedLevelId))
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot != null) {
                    val ids = snapshot.documents.map { it.id }.toSet()
                    totalCardsCount = if (ids.isNotEmpty()) ids.size else getFallbackCount(normalizedLevelId)
                    
                    if (uid.isNotEmpty() && ids.isNotEmpty()) {
                        firestore.collection("progress")
                            .whereEqualTo("uid", uid)
                            .addSnapshotListener { progressSnapshot, error ->
                                if (progressSnapshot != null) {
                                    val progressVocabIds = progressSnapshot.documents.mapNotNull { it.getString("vocabId") }.toSet()
                                    learnedCardsCount = ids.count { progressVocabIds.contains(it) }
                                }
                            }
                    } else if (uid.isNotEmpty()) {
                        firestore.collection("progress")
                            .whereEqualTo("uid", uid)
                            .addSnapshotListener { progressSnapshot, error ->
                                if (progressSnapshot != null) {
                                    val progressVocabIds = progressSnapshot.documents.mapNotNull { it.getString("vocabId") }.toSet()
                                    learnedCardsCount = progressVocabIds.size.coerceAtMost(totalCardsCount)
                                }
                            }
                    }
                }
            }
            .addOnFailureListener {
                totalCardsCount = getFallbackCount(normalizedLevelId)
            }
    }

    val progressPercent = if (totalCardsCount > 0) learnedCardsCount.toFloat() / totalCardsCount else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Level Dashboard", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Premium System Header Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF1D4ED8), Color(0xFF3B82F6))
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CẤP ĐỘ HỆ THỐNG",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Surface(
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "VIP/PREMIUM",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = screenTitle,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Luyện tập từ vựng và câu hỏi cấp độ $prettyLevel với các phương pháp ghi nhớ hiện đại nhất hệ thống.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tổng số từ: $totalCardsCount từ",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Tiến độ: $learnedCardsCount/$totalCardsCount thẻ",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.Medium,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { progressPercent },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                                color = Color(0xFF10B981),
                                trackColor = Color.White.copy(alpha = 0.25f)
                            )
                        }
                    }
                }
            }

            // 2. Premium 2x2 Grid Layout
            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    userScrollEnabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        LevelFeatureCard(
                            title = "Flashcard",
                            description = "Học thẻ ghi nhớ",
                            icon = Icons.Default.Style,
                            tintColor = Color(0xFF2563EB),
                            onClick = {
                                navController.navigate("user_flashcard/$language/$normalizedLevelId")
                            }
                        )
                    }
                    item {
                        LevelFeatureCard(
                            title = "Trắc Nghiệm",
                            description = "Luyện trắc nghiệm",
                            icon = Icons.Default.Quiz,
                            tintColor = Color(0xFFF97316),
                            onClick = {
                                navController.navigate("user_play_quiz/$normalizedLevelId")
                            }
                        )
                    }
                    item {
                        LevelFeatureCard(
                            title = "Tự Luận",
                            description = "Viết câu trả lời",
                            icon = Icons.Default.EditNote,
                            tintColor = Color(0xFF10B981),
                            onClick = {
                                showEssayBottomSheet = true
                            }
                        )
                    }
                    item {
                        LevelFeatureCard(
                            title = "Nối Từ (Match)",
                            description = "Trò chơi ghép từ",
                            icon = Icons.Default.Extension,
                            tintColor = Color(0xFF8B5CF6),
                            onClick = {
                                navController.navigate("user_play_match/$normalizedLevelId")
                            }
                        )
                    }
                }
            }
        }
    }

    if (showEssayBottomSheet) {
        val category = QuizCategoryItem(
            id = normalizedLevelId,
            name = "Tự luận $prettyLevel",
            description = "Luyện viết từ vựng cấp độ $prettyLevel",
            requiredPackage = "FREE"
        )
        EssayPlayModeBottomSheet(
            category = category,
            onDismiss = { showEssayBottomSheet = false },
            onModeSelected = { mode ->
                showEssayBottomSheet = false
                navController.navigate("user_play_essay/$normalizedLevelId/$mode")
            }
        )
    }
}

@Composable
fun LevelFeatureCard(
    title: String,
    description: String,
    icon: ImageVector,
    tintColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = tintColor.copy(alpha = 0.15f)),
        border = BorderStroke(1.dp, tintColor.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(tintColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tintColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = tintColor,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = tintColor.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

fun getFallbackCount(levelId: String): Int {
    return when (levelId) {
        "QUIZ_JA_N5" -> 50
        "QUIZ_JA_N4" -> 60
        "QUIZ_JA_N3" -> 80
        "QUIZ_JA_N2" -> 100
        "QUIZ_JA_N1" -> 120
        "QUIZ_TOEIC_450" -> 150
        "QUIZ_TOEIC_650" -> 180
        "QUIZ_TOEIC_800" -> 220
        "QUIZ_IELTS_55" -> 150
        "QUIZ_IELTS_65" -> 180
        "QUIZ_IELTS_75" -> 250
        "QUIZ_ZH_BASIC" -> 40
        "QUIZ_PA_INTRO" -> 30
        else -> 50
    }
}
