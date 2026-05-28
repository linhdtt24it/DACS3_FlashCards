package com.example.flashcards.view

import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.flashcards.ui.theme.*
import androidx.compose.material.icons.filled.*
import com.example.flashcards.utils.CryptoUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material.icons.filled.Star
import java.util.Locale
import java.util.UUID

data class VocabCard(
    val id: String,
    val front: String, // original word, e.g. "食べる"
    val back: String, // meaning, e.g. "Ăn"
    val levelId: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserFlashcardScreen(
    navController: NavController,
    language: String,
    levelId: String
) {
    val context = LocalContext.current
    val firestore = remember { FirebaseFirestore.getInstance() }
    var vocabCards by remember { mutableStateOf<List<VocabCard>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // 1. Cấu hình khởi tạo và giải phóng Text-to-Speech (TTS) an toàn phòng tránh Memory Leak
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsReady by remember { mutableStateOf(false) }

    LaunchedEffect(language) {
        tts = TextToSpeech(context) { status ->
            if (status != TextToSpeech.ERROR) {
                val locale = when (language.uppercase()) {
                    "JAPANESE" -> Locale.JAPANESE
                    "ENGLISH" -> Locale.US
                    "CHINESE" -> Locale.CHINESE
                    else -> Locale.US
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

    // 2. Tải câu hỏi/từ vựng từ Firestore theo cấp độ được lọc
    LaunchedEffect(levelId) {
        if (levelId == "STARRED") {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
            firestore.collection("user_favorites")
                .whereEqualTo("uid", uid)
                .whereEqualTo("starred", true)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    isLoading = false
                    val list = querySnapshot.documents.mapNotNull { doc ->
                        val id = doc.getString("vocabId") ?: doc.id
                        val front = doc.getString("front") ?: ""
                        val back = doc.getString("back") ?: ""
                        val originLevelId = doc.getString("levelId") ?: ""
                        if (front.isNotEmpty() && back.isNotEmpty()) {
                            VocabCard(id, front, back, originLevelId)
                        } else null
                    }
                    vocabCards = list
                }
                .addOnFailureListener {
                    isLoading = false
                    vocabCards = emptyList()
                }
        } else {
            val vocabCategory = getVocabCategoryFromLevelId(levelId)
            firestore.collection("system_vocabulary")
                .whereIn("category", listOf(vocabCategory, levelId))
                .get()
                .addOnSuccessListener { querySnapshot ->
                    isLoading = false
                    val list = querySnapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.id
                            val rawFront = doc.getString("front") ?: ""
                            val rawBack = doc.getString("back") ?: ""
                            val front = CryptoUtils.decrypt(rawFront)
                            val back = CryptoUtils.decrypt(rawBack)
                            if (front.isNotEmpty() && back.isNotEmpty()) {
                                VocabCard(id, front, back, levelId)
                            } else null
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (list.isNotEmpty()) {
                        vocabCards = list
                    } else {
                        // Fallback to default level vocabulary list
                        vocabCards = getFallbackVocabList(levelId)
                    }
                }
                .addOnFailureListener {
                    isLoading = false
                    vocabCards = getFallbackVocabList(levelId)
                }
        }
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var showWishDialog by remember { mutableStateOf(false) }

    var starredCardIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val uid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous" }

    LaunchedEffect(uid) {
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

    val recordProgress = { cardId: String, status: String ->
        if (uid.isNotEmpty() && uid != "anonymous") {
            val progressDocRef = firestore.collection("progress").document("${uid}_$cardId")
            
            val calendar = java.util.Calendar.getInstance()
            val lastReviewed = com.google.firebase.Timestamp(calendar.time)
            
            val nextReview: com.google.firebase.Timestamp? = when (status) {
                "HARD" -> {
                    calendar.add(java.util.Calendar.DAY_OF_YEAR, 2)
                    com.google.firebase.Timestamp(calendar.time)
                }
                "GOOD" -> {
                    calendar.add(java.util.Calendar.DAY_OF_YEAR, 7)
                    com.google.firebase.Timestamp(calendar.time)
                }
                else -> null // EASY (MASTERED)
            }
            
            val finalStatus = if (status == "EASY") "MASTERED" else status
            
            val progressData = hashMapOf(
                "uid" to uid,
                "vocabId" to cardId,
                "status" to finalStatus,
                "lastReviewed" to lastReviewed,
                "nextReview" to nextReview
            )
            progressDocRef.set(progressData, com.google.firebase.firestore.SetOptions.merge())
        }
    }

    val onCardRated = { card: VocabCard, status: String ->
        recordProgress(card.id, status)
        if (currentIndex == vocabCards.size - 1) {
            showWishDialog = true
        } else {
            isFlipped = false
            currentIndex++
        }
    }



    val progress = if (vocabCards.isNotEmpty()) (currentIndex.toFloat() / vocabCards.size) else 0f
    val displayLevelName = when (levelId) {
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thẻ ghi nhớ: $displayLevelName", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            if (vocabCards.isNotEmpty()) {
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.background
                ) {
                    BottomAppBar(containerColor = Color.Transparent, tonalElevation = 0.dp) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { if (currentIndex > 0) { currentIndex--; isFlipped = false } },
                                enabled = currentIndex > 0
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Previous",
                                    tint = if (currentIndex > 0) MaterialTheme.colorScheme.onBackground else Color.Gray
                                )
                            }
                            Text(
                                text = "Thẻ: ${currentIndex + 1}/${vocabCards.size}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            if (currentIndex == vocabCards.size - 1) {
                                TextButton(
                                    onClick = {
                                        if (vocabCards.isNotEmpty()) {
                                            recordProgress(vocabCards[currentIndex].id, "GOOD")
                                        }
                                        showWishDialog = true
                                    }
                                ) {
                                    Text(
                                        text = "Hoàn thành",
                                        color = FlowPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        if (vocabCards.isNotEmpty()) {
                                            recordProgress(vocabCards[currentIndex].id, "GOOD")
                                        }
                                        currentIndex++;
                                        isFlipped = false
                                    },
                                    enabled = currentIndex < vocabCards.size - 1
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Next",
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                    }
                }
            }
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
            } else if (vocabCards.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Cấp độ này hiện chưa có từ vựng.", color = Color.Gray)
                }
            } else {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = FlowPrimary,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(32.dp))

                val currentCard = vocabCards[currentIndex]
                val rotationState by animateFloatAsState(
                    targetValue = if (isFlipped) 180f else 0f,
                    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                    label = "flip"
                )

                // Flashcard container with 3D Vertical Flip animation
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .graphicsLayer {
                            rotationX = rotationState
                            cameraDistance = 12f * density
                        }
                        .clickable { isFlipped = !isFlipped },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (rotationState <= 90f) {
                            // Giao diện Mặt trước (Từ gốc + Loa + Sao)
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "MẶT TRƯỚC",
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            color = FlowPrimary,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Nút Đánh dấu sao (Star)
                                        val isStarred = starredCardIds.contains(currentCard.id)
                                        IconButton(
                                            onClick = {
                                                val favDocRef = firestore.collection("user_favorites").document("${uid}_${currentCard.id}")
                                                if (isStarred) {
                                                    favDocRef.update("starred", false)
                                                } else {
                                                    val favData = hashMapOf(
                                                        "uid" to uid,
                                                        "vocabId" to currentCard.id,
                                                        "front" to currentCard.front,
                                                        "back" to currentCard.back,
                                                        "levelId" to if (levelId == "STARRED") currentCard.levelId else levelId,
                                                        "starred" to true,
                                                        "updatedAt" to com.google.firebase.Timestamp.now()
                                                    )
                                                    favDocRef.set(favData, com.google.firebase.firestore.SetOptions.merge())
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = "Đánh dấu sao",
                                                tint = if (isStarred) Color(0xFFFFD700) else Color.Gray,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // Volume up phát âm tiếng TTS
                                        IconButton(
                                            onClick = {
                                                val wordToSpeak = currentCard.front
                                                val cleanWord = wordToSpeak.substringBefore("(").trim()
                                                val speakLocale = when {
                                                    currentCard.levelId.contains("JA") -> Locale.JAPANESE
                                                    currentCard.levelId.contains("ZH") -> Locale.CHINESE
                                                    currentCard.levelId.contains("PA") -> Locale.US
                                                    else -> {
                                                        when (language.uppercase()) {
                                                            "JAPANESE" -> Locale.JAPANESE
                                                            "CHINESE" -> Locale.CHINESE
                                                            else -> Locale.US
                                                        }
                                                    }
                                                }
                                                tts?.language = speakLocale
                                                tts?.speak(cleanWord, TextToSpeech.QUEUE_FLUSH, null, null)
                                            },
                                            enabled = isTtsReady
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.VolumeUp,
                                                contentDescription = "Phát âm",
                                                tint = FlowPrimary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                Text(
                                    text = currentCard.front,
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.weight(1f))
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.TouchApp,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Bấm vào thẻ để lật mặt sau",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        } else {
                            // Giao diện Mặt sau (Nghĩa tiếng Việt + Bộ 3 nút Hard/Good/Easy)
                            // Bắt buộc xoay ngược lại 180 độ theo trục X bằng graphicsLayer
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { rotationX = 180f }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "MẶT SAU",
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                color = FlowPrimary,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                        
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Nút Đánh dấu sao (Star)
                                            val isStarred = starredCardIds.contains(currentCard.id)
                                            IconButton(
                                                onClick = {
                                                    val favDocRef = firestore.collection("user_favorites").document("${uid}_${currentCard.id}")
                                                    if (isStarred) {
                                                        favDocRef.update("starred", false)
                                                    } else {
                                                        val favData = hashMapOf(
                                                            "uid" to uid,
                                                            "vocabId" to currentCard.id,
                                                            "front" to currentCard.front,
                                                            "back" to currentCard.back,
                                                            "levelId" to if (levelId == "STARRED") currentCard.levelId else levelId,
                                                            "starred" to true,
                                                            "updatedAt" to com.google.firebase.Timestamp.now()
                                                        )
                                                        favDocRef.set(favData, com.google.firebase.firestore.SetOptions.merge())
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = "Đánh dấu sao",
                                                    tint = if (isStarred) Color(0xFFFFD700) else Color.Gray,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            // Volume up phát âm tiếng TTS
                                            IconButton(
                                                onClick = {
                                                    val wordToSpeak = currentCard.back
                                                    val cleanWord = wordToSpeak.substringBefore("(").trim()
                                                    val speakLocale = when {
                                                        currentCard.levelId.contains("JA") -> Locale.JAPANESE
                                                        currentCard.levelId.contains("ZH") -> Locale.CHINESE
                                                        currentCard.levelId.contains("PA") -> Locale.US
                                                        else -> {
                                                            when (language.uppercase()) {
                                                                "JAPANESE" -> Locale.JAPANESE
                                                                "CHINESE" -> Locale.CHINESE
                                                                else -> Locale.US
                                                            }
                                                        }
                                                    }
                                                    tts?.language = speakLocale
                                                    tts?.speak(cleanWord, TextToSpeech.QUEUE_FLUSH, null, null)
                                                },
                                                enabled = isTtsReady
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.VolumeUp,
                                                    contentDescription = "Phát âm",
                                                    tint = FlowPrimary,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.weight(1f))

                                    Text(
                                        text = currentCard.back,
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.weight(1f))
                                    Spacer(modifier = Modifier.height(48.dp))
                                }

                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .padding(24.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Button(
                                            onClick = { onCardRated(currentCard, "HARD") },
                                            modifier = Modifier.weight(1f).height(52.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = FlowWarningLight)
                                        ) {
                                            Text("Khó", color = FlowWarning, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = { onCardRated(currentCard, "GOOD") },
                                            modifier = Modifier.weight(1f).height(52.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                        ) {
                                            Text("Tốt", color = FlowPrimary, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = { onCardRated(currentCard, "EASY") },
                                            modifier = Modifier.weight(1f).height(52.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = FlowSuccessLight)
                                        ) {
                                            Text("Dễ", color = FlowSuccess, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showWishDialog) {
        RandomWishDialog(
            onDismiss = {
                showWishDialog = false
                navController.popBackStack()
            }
        )
    }
}

// Maps levelId to the category code used in Firestore vocabulary
fun getVocabCategoryFromLevelId(levelId: String): String {
    return when (levelId) {
        "QUIZ_JA_N5" -> "n5"
        "QUIZ_JA_N4" -> "n4"
        "QUIZ_JA_N3" -> "n3"
        "QUIZ_JA_N2" -> "n2"
        "QUIZ_JA_N1" -> "n1"
        "QUIZ_TOEIC_450" -> "EN_TOEIC_450"
        "QUIZ_TOEIC_650" -> "EN_TOEIC_650"
        "QUIZ_TOEIC_800" -> "EN_TOEIC_800"
        "QUIZ_IELTS_55" -> "EN_IELTS_50_60"
        "QUIZ_IELTS_65" -> "EN_IELTS_65_75"
        "QUIZ_IELTS_75" -> "EN_IELTS_80"
        "QUIZ_ZH_BASIC" -> "ZH_BASIC"
        "QUIZ_PA_INTRO" -> "PA_INTRO"
        else -> levelId
    }
}

// Default fallback list in case Firebase Firestore is empty
fun getFallbackVocabList(levelId: String): List<VocabCard> {
    return when {
        levelId.startsWith("QUIZ_JA") -> {
            when (levelId) {
                "QUIZ_JA_N5" -> listOf(
                    VocabCard(UUID.randomUUID().toString(), "食べる (taberu)", "Ăn"),
                    VocabCard(UUID.randomUUID().toString(), "飲む (nomu)", "Uống"),
                    VocabCard(UUID.randomUUID().toString(), "行く (iku)", "Đi"),
                    VocabCard(UUID.randomUUID().toString(), "見る (miru)", "Xem / Nhìn"),
                    VocabCard(UUID.randomUUID().toString(), "先生 (sensei)", "Giáo viên")
                )
                "QUIZ_JA_N4" -> listOf(
                    VocabCard(UUID.randomUUID().toString(), "覚える (oboyeru)", "Nhớ / Ghi nhớ"),
                    VocabCard(UUID.randomUUID().toString(), "簡単 (kantan)", "Đơn giản / Dễ dàng"),
                    VocabCard(UUID.randomUUID().toString(), "重い (omoi)", "Nặng"),
                    VocabCard(UUID.randomUUID().toString(), "軽い (karui)", "Nhẹ")
                )
                else -> listOf(
                    VocabCard(UUID.randomUUID().toString(), "一生懸命 (isshoukenmei)", "Nỗ lực hết sức"),
                    VocabCard(UUID.randomUUID().toString(), "調査 (chousa)", "Điều tra / Khảo sát"),
                    VocabCard(UUID.randomUUID().toString(), "緊張 (kinchou)", "Căng thẳng / Hồi hộp")
                )
            }
        }
        levelId.startsWith("QUIZ_TOEIC") -> {
            when (levelId) {
                "QUIZ_TOEIC_450" -> listOf(
                    VocabCard(UUID.randomUUID().toString(), "Confirm", "Xác nhận"),
                    VocabCard(UUID.randomUUID().toString(), "Submit", "Nộp / Trình"),
                    VocabCard(UUID.randomUUID().toString(), "Delay", "Trì hoãn")
                )
                else -> listOf(
                    VocabCard(UUID.randomUUID().toString(), "Negotiate", "Thương lượng / Đàm phán"),
                    VocabCard(UUID.randomUUID().toString(), "Implement", "Thi hành / Thực hiện"),
                    VocabCard(UUID.randomUUID().toString(), "Collaborate", "Hợp tác")
                )
            }
        }
        levelId.startsWith("QUIZ_IELTS") -> {
            listOf(
                VocabCard(UUID.randomUUID().toString(), "Analyze", "Phân tích"),
                VocabCard(UUID.randomUUID().toString(), "Synthesize", "Tổng hợp"),
                VocabCard(UUID.randomUUID().toString(), "Hypothesis", "Giả thuyết")
            )
        }
        levelId == "QUIZ_ZH_BASIC" -> {
            listOf(
                VocabCard(UUID.randomUUID().toString(), "你好 (nǐ hǎo)", "Xin chào"),
                VocabCard(UUID.randomUUID().toString(), "谢谢 (xièxie)", "Cảm ơn"),
                VocabCard(UUID.randomUUID().toString(), "再见 (zàijiàn)", "Tạm biệt")
            )
        }
        levelId == "QUIZ_PA_INTRO" -> {
            listOf(
                VocabCard(UUID.randomUUID().toString(), "Buddha", "Đức Phật / Bậc Giác Ngộ"),
                VocabCard(UUID.randomUUID().toString(), "Dhamma", "Giáo Pháp"),
                VocabCard(UUID.randomUUID().toString(), "Sangha", "Tăng Đoàn")
            )
        }
        else -> {
            listOf(
                VocabCard(UUID.randomUUID().toString(), "Hello", "Xin chào")
            )
        }
    }
}
