package com.example.flashcards.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

data class VocabItem(
    val id: String,
    val front: String,
    val back: String,
    val category: String = ""
)

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
        else -> normalizedLevelId.replace("QUIZ_", "").replace("_", " ")
    }

    val screenTitle = "$prettyLang - $prettyLevel"
    var showEssayBottomSheet by remember { mutableStateOf(false) }
    var showGameBottomSheet by remember { mutableStateOf(false) }

    val firestore = remember { FirebaseFirestore.getInstance() }
    val uid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    var totalCardsCount by remember { mutableIntStateOf(0) }
    var learnedCardsCount by remember { mutableIntStateOf(0) }

    var vocabList by remember { mutableStateOf<List<VocabItem>>(emptyList()) }
    var starredCardIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isTtsReady by remember { mutableStateOf(false) }
    var tts by remember { mutableStateOf<android.speech.tts.TextToSpeech?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(language) {
        tts = android.speech.tts.TextToSpeech(context) { status ->
            if (status != android.speech.tts.TextToSpeech.ERROR) {
                val locale = when (language.uppercase()) {
                    "JAPANESE" -> java.util.Locale.JAPANESE
                    "ENGLISH" -> java.util.Locale.US
                    "CHINESE" -> java.util.Locale.CHINESE
                    else -> java.util.Locale.US
                }
                tts?.language = locale
                isTtsReady = true
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) {
            firestore.collection("user_favorites")
                .whereEqualTo("uid", uid)
                .whereEqualTo("starred", true)
                .addSnapshotListener { snapshot, error ->
                    if (snapshot != null) {
                        val ids = snapshot.documents.mapNotNull { it.getString("vocabId") }.toSet()
                        starredCardIds = ids
                    }
                }
        }
    }

    LaunchedEffect(normalizedLevelId, uid) {
        val category = getVocabCategoryFromLevelId(normalizedLevelId)
        firestore.collection("system_vocabulary")
            .whereIn("category", listOf(category, normalizedLevelId))
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot != null && !snapshot.isEmpty) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.id
                            val rawFront = doc.getString("front") ?: ""
                            val rawBack = doc.getString("back") ?: ""
                            val front = com.example.flashcards.utils.CryptoUtils.decrypt(rawFront)
                            val back = com.example.flashcards.utils.CryptoUtils.decrypt(rawBack)
                            if (front.isNotEmpty() && back.isNotEmpty()) {
                                VocabItem(id, front, back, category)
                            } else null
                        } catch (e: Exception) {
                            null
                        }
                    }
                    
                    if (list.isNotEmpty()) {
                        vocabList = list
                    } else {
                        vocabList = getFallbackDashboardVocabList(normalizedLevelId)
                    }
                    totalCardsCount = vocabList.size
                    
                    if (uid.isNotEmpty() && vocabList.isNotEmpty()) {
                        val ids = vocabList.map { it.id }.toSet()
                        firestore.collection("progress")
                            .whereEqualTo("uid", uid)
                            .addSnapshotListener { progressSnapshot, error ->
                                if (progressSnapshot != null) {
                                    val progressVocabIds = progressSnapshot.documents.mapNotNull { it.getString("vocabId") }.toSet()
                                    learnedCardsCount = ids.count { progressVocabIds.contains(it) }
                                }
                            }
                    }
                } else {
                    vocabList = getFallbackDashboardVocabList(normalizedLevelId)
                    totalCardsCount = vocabList.size
                }
            }
            .addOnFailureListener {
                vocabList = getFallbackDashboardVocabList(normalizedLevelId)
                totalCardsCount = vocabList.size
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
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        LevelFeatureCard(
                            title = "Flashcard",
                            description = "Học thẻ ghi nhớ",
                            icon = Icons.Default.Style,
                            tintColor = Color(0xFF2563EB),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                navController.navigate("user_flashcard/$language/$normalizedLevelId")
                            }
                        )
                        LevelFeatureCard(
                            title = "Trắc Nghiệm",
                            description = "Luyện trắc nghiệm",
                            icon = Icons.Default.Quiz,
                            tintColor = Color(0xFFF97316),
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
                        LevelFeatureCard(
                            title = "Tự Luận",
                            description = "Viết câu trả lời",
                            icon = Icons.Default.EditNote,
                            tintColor = Color(0xFF10B981),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                showEssayBottomSheet = true
                            }
                        )
                        LevelFeatureCard(
                            title = "Giải Trí",
                            description = "Nối từ, Minigames",
                            icon = Icons.Default.VideogameAsset,
                            tintColor = Color(0xFF8B5CF6),
                            modifier = Modifier.weight(1f),
                            onClick = { showGameBottomSheet = true }
                        )
                    }
                }
            }

            // 3. Vocabulary List Title Section
            item {
                Text(
                    text = "Danh sách từ vựng ($totalCardsCount từ)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // 4. Vocabulary List Items Section
            items(vocabList) { vocab ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Word + Reading Caption
                        Column(modifier = Modifier.weight(1.2f)) {
                            val cleanFront = vocab.front.substringBefore("(").trim()
                            val reading = if (vocab.front.contains("(")) {
                                "(" + vocab.front.substringAfter("(")
                            } else ""
                            
                            Text(
                                text = cleanFront,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (reading.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = reading,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Center: Vietnamese definition
                        Box(
                            modifier = Modifier.weight(1.2f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = vocab.back,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Right: Actions (Speak + Star)
                        Row(
                            modifier = Modifier.weight(0.8f),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val wordToSpeak = vocab.front
                                    val cleanWord = wordToSpeak.substringBefore("(").trim()
                                    val speakLocale = when {
                                        normalizedLevelId.contains("JA") -> java.util.Locale.JAPANESE
                                        normalizedLevelId.contains("ZH") -> java.util.Locale.CHINESE
                                        normalizedLevelId.contains("PA") -> java.util.Locale.US
                                        else -> {
                                            when (language.uppercase()) {
                                                "JAPANESE" -> java.util.Locale.JAPANESE
                                                "CHINESE" -> java.util.Locale.CHINESE
                                                else -> java.util.Locale.US
                                            }
                                        }
                                    }
                                    tts?.language = speakLocale
                                    tts?.speak(cleanWord, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
                                },
                                enabled = isTtsReady,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Phát âm",
                                    tint = FlowPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            val isStarred = starredCardIds.contains(vocab.id)
                            IconButton(
                                onClick = {
                                    val favDocRef = firestore.collection("user_favorites").document("${uid}_${vocab.id}")
                                    if (isStarred) {
                                        favDocRef.update("starred", false)
                                    } else {
                                        val favData = hashMapOf(
                                            "uid" to uid,
                                            "vocabId" to vocab.id,
                                            "front" to vocab.front,
                                            "back" to vocab.back,
                                            "levelId" to normalizedLevelId,
                                            "starred" to true,
                                            "updatedAt" to com.google.firebase.Timestamp.now()
                                        )
                                        favDocRef.set(favData, com.google.firebase.firestore.SetOptions.merge())
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Đánh dấu sao",
                                    tint = if (isStarred) Color(0xFFFFD700) else Color.Gray,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
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

    if (showGameBottomSheet) {
        GameSelectionBottomSheet(
            hasBattleFeature = false,
            onDismiss = { showGameBottomSheet = false },
            onPlayMatch = {
                showGameBottomSheet = false
                navController.navigate("user_play_match/$normalizedLevelId")
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
        "QUIZ_JA_N5" -> 10
        "QUIZ_JA_N4" -> 10
        "QUIZ_JA_N3" -> 10
        "QUIZ_JA_N2" -> 10
        "QUIZ_JA_N1" -> 10
        "QUIZ_TOEIC_450" -> 10
        "QUIZ_TOEIC_650" -> 10
        "QUIZ_TOEIC_800" -> 10
        "QUIZ_IELTS_55" -> 10
        "QUIZ_IELTS_65" -> 10
        "QUIZ_IELTS_75" -> 10
        "QUIZ_ZH_BASIC" -> 5
        "QUIZ_PA_INTRO" -> 5
        else -> 5
    }
}

fun getFallbackDashboardVocabList(levelId: String): List<VocabItem> {
    return when {
        levelId.contains("JA") || levelId == "QUIZ_JA_N5" -> listOf(
            VocabItem("fb_1", "食べる (taberu)", "Ăn"),
            VocabItem("fb_2", "飲む (nomu)", "Uống"),
            VocabItem("fb_3", "行く (iku)", "Đi"),
            VocabItem("fb_4", "見る (miru)", "Xem / Nhìn"),
            VocabItem("fb_5", "先生 (sensei)", "Giáo viên"),
            VocabItem("fb_6", "学校 (gakko)", "Trường học"),
            VocabItem("fb_7", "本 (hon)", "Sách"),
            VocabItem("fb_8", "水 (mizu)", "Nước"),
            VocabItem("fb_9", "猫 (neko)", "Con mèo"),
            VocabItem("fb_10", "犬 (inu)", "Con chó")
        )
        levelId.contains("TOEIC") || levelId.contains("IELTS") -> listOf(
            VocabItem("fb_11", "Abandon (v)", "Từ bỏ / Ruồng bỏ"),
            VocabItem("fb_12", "Accumulate (v)", "Tích lũy / Gom góp"),
            VocabItem("fb_13", "Beneficial (adj)", "Có lợi / Có ích"),
            VocabItem("fb_14", "Collaborate (v)", "Hợp tác / Cộng tác"),
            VocabItem("fb_15", "Diverse (adj)", "Đa dạng / Phong phú"),
            VocabItem("fb_16", "Evaluate (v)", "Đánh giá / Định giá"),
            VocabItem("fb_17", "Fluctuate (v)", "Dao động / Biến động"),
            VocabItem("fb_18", "Guarantee (n/v)", "Cam kết / Bảo hành"),
            VocabItem("fb_19", "Hinder (v)", "Cản trở / Gây trở ngại"),
            VocabItem("fb_20", "Implement (v)", "Triển khai / Thực thi")
        )
        else -> listOf(
            VocabItem("fb_21", "Hello", "Xin chào"),
            VocabItem("fb_22", "Thank you", "Cảm ơn"),
            VocabItem("fb_23", "Goodbye", "Tạm biệt"),
            VocabItem("fb_24", "Please", "Làm ơn"),
            VocabItem("fb_25", "Sorry", "Xin lỗi")
        )
    }
}
