package com.example.flashcards.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
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
            // Welcome Section Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(FlowPrimary, FlowPrimary.copy(alpha = 0.7f))
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column {
                        Text(
                            text = "Trung Tâm Cấp Độ",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Học từ vựng và câu hỏi cấp độ $prettyLevel với các phương pháp ghi nhớ hiện đại, thông minh.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            Text(
                text = "Phương Thức Rèn Luyện",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            // Grid 2x2 of Feature Cards
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SquareFeatureCard(
                        title = "Flashcard",
                        description = "Ghi nhớ 3D & Audio",
                        icon = Icons.Default.Style,
                        gradientColors = listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            navController.navigate("user_flashcard/$language/$normalizedLevelId")
                        }
                    )
                    SquareFeatureCard(
                        title = "Trắc Nghiệm",
                        description = "Luyện đề trắc nghiệm",
                        icon = Icons.Default.Quiz,
                        gradientColors = listOf(Color(0xFFF97316), Color(0xFFEA580C)),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            navController.navigate("user_play_quiz/$normalizedLevelId")
                        }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SquareFeatureCard(
                        title = "Tự Luận",
                        description = "Gõ từ vựng nhớ lâu",
                        icon = Icons.Default.EditNote,
                        gradientColors = listOf(Color(0xFF10B981), Color(0xFF059669)),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            showEssayBottomSheet = true
                        }
                    )
                    SquareFeatureCard(
                        title = "Nối Từ (Match)",
                        description = "Game ghép từ nhanh",
                        icon = Icons.Default.Extension,
                        gradientColors = listOf(Color(0xFF8B5CF6), Color(0xFF7C3AED)),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            navController.navigate("user_play_match/$normalizedLevelId")
                        }
                    )
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
fun SquareFeatureCard(
    title: String,
    description: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradientColors))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
