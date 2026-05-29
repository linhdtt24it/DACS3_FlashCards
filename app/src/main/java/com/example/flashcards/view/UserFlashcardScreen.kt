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
import androidx.compose.ui.draw.scale
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
import com.example.flashcards.model.CardState
import com.example.flashcards.model.ReviewRating
import com.example.flashcards.viewmodel.SpacedRepetitionViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

data class VocabCard(
    val id: String,
    val front: String,
    val back: String,
    val levelId: String = "",
    val state: CardState = CardState.NEW,
    val interval: Int = 0,
    val easeFactor: Double = 2.5
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserFlashcardScreen(
    navController: NavController,
    language: String,
    levelId: String
) {
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid ?: "anonymous"
    
    val spacedRepetitionViewModel: SpacedRepetitionViewModel = viewModel()
    val context = LocalContext.current
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
        } else if (levelId.startsWith("day_") || language.uppercase() == "SPACED") {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
            if (uid != "anonymous") {
                firestore.collection("users").document(uid).get()
                    .addOnSuccessListener { userDoc ->
                        val dailyLimit = userDoc.getLong("dailyWordLimit")?.toInt() ?: 10
                        
                        // Query progress and user_progress in parallel to be extremely robust
                        firestore.collection("progress")
                            .whereEqualTo("uid", uid)
                            .get()
                            .addOnSuccessListener { progressSnapshot ->
                                firestore.collection("user_progress")
                                    .whereEqualTo("uid", uid)
                                    .get()
                                    .addOnSuccessListener { userProgressSnapshot ->
                                        val allProgressDocs = (progressSnapshot?.documents ?: emptyList()) + (userProgressSnapshot?.documents ?: emptyList())
                                        val uniqueProgressDocs = allProgressDocs.associateBy { it.id }.values
                                        
                                        val dayIndex = levelId.substringAfter("day_").toIntOrNull() ?: 1
                                        val calendar = java.util.Calendar.getInstance()
                                        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                                        calendar.set(java.util.Calendar.MINUTE, 0)
                                        calendar.set(java.util.Calendar.SECOND, 0)
                                        calendar.set(java.util.Calendar.MILLISECOND, 0)
                                        val startOfToday = calendar.timeInMillis
                                        
                                        val dayEndTimes = LongArray(7)
                                        for (i in 0..6) {
                                            calendar.timeInMillis = startOfToday
                                            calendar.add(java.util.Calendar.DAY_OF_YEAR, i)
                                            calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
                                            calendar.set(java.util.Calendar.MINUTE, 59)
                                            calendar.set(java.util.Calendar.SECOND, 59)
                                            calendar.set(java.util.Calendar.MILLISECOND, 999)
                                            dayEndTimes[i] = calendar.timeInMillis
                                        }
                                        
                                        val dueVocabIds = mutableListOf<String>()
                                        val statusMap = mutableMapOf<String, String>()
                                        val sm2DataMap = mutableMapOf<String, Triple<CardState, Int, Double>>()
                                        
                                        uniqueProgressDocs.forEach { doc ->
                                            val vocabId = doc.getString("vocabId") ?: ""
                                            val status = doc.getString("status") ?: ""
                                            val nextReview = doc.getTimestamp("nextReview")
                                            val interval = doc.getLong("interval")?.toInt() ?: 0
                                            val easeFactor = doc.getDouble("easeFactor") ?: 2.5
                                            val cardState = try { CardState.valueOf(status) } catch (e: Exception) { CardState.NEW }
                                            
                                            if (vocabId.isNotEmpty() && status != "MASTERED" && status != "MASTER") {
                                                statusMap[vocabId] = status
                                                sm2DataMap[vocabId] = Triple(cardState, interval, easeFactor)
                                                if (nextReview != null) {
                                                    val reviewTime = nextReview.toDate().time
                                                    if (dayIndex == 1) {
                                                        if (reviewTime <= dayEndTimes[0]) {
                                                            dueVocabIds.add(vocabId)
                                                        }
                                                    } else {
                                                        val startRange = dayEndTimes[dayIndex - 2]
                                                        val endRange = dayEndTimes[dayIndex - 1]
                                                        if (reviewTime > startRange && reviewTime <= endRange) {
                                                            dueVocabIds.add(vocabId)
                                                        }
                                                    }
                                                } else if (dayIndex == 1) {
                                                    dueVocabIds.add(vocabId)
                                                }
                                            }
                                        }
                                        
                                        val finalVocabIds = if (dayIndex == 1) dueVocabIds.take(dailyLimit) else dueVocabIds
                                        
                                        if (finalVocabIds.isEmpty()) {
                                            vocabCards = emptyList()
                                            isLoading = false
                                        } else {
                                            // Load custom user deck cards from Firestore to match custom IDs
                                            val userCardsMap = mutableMapOf<String, VocabCard>()
                                            firestore.collection("users").document(uid).collection("studySets").get()
                                                .addOnSuccessListener { studySetsSnapshot ->
                                                    if (studySetsSnapshot != null) {
                                                        studySetsSnapshot.documents.forEach { sDoc ->
                                                            val cards = sDoc.get("cards") as? List<Map<String, Any>> ?: emptyList()
                                                            cards.forEach { cardMap ->
                                                                val id = cardMap["id"] as? String
                                                                val question = cardMap["question"] as? String
                                                                val answer = cardMap["answer"] as? String
                                                                if (id != null && question != null && answer != null) {
                                                                    userCardsMap[id] = VocabCard(id, question, answer, sDoc.id)
                                                                }
                                                            }
                                                        }
                                                    }
                                                    
                                                    val chunks = finalVocabIds.chunked(30)
                                                    val loadedCards = mutableListOf<VocabCard>()
                                                    var chunksLeft = chunks.size
                                                    
                                                    chunks.forEach { chunk ->
                                                        firestore.collection("system_vocabulary")
                                                            .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                                                            .get()
                                                            .addOnSuccessListener { vocabSnapshot ->
                                                                vocabSnapshot.documents.forEach { d ->
                                                                    try {
                                                                        val id = d.id
                                                                        val rawFront = d.getString("front") ?: ""
                                                                        val rawBack = d.getString("back") ?: ""
                                                                        val front = CryptoUtils.decrypt(rawFront)
                                                                        val back = CryptoUtils.decrypt(rawBack)
                                                                        if (front.isNotEmpty() && back.isNotEmpty()) {
                                                                            val sm2Data = sm2DataMap[id] ?: Triple(CardState.NEW, 0, 2.5)
                                                                            loadedCards.add(VocabCard(id, front, back, levelId, sm2Data.first, sm2Data.second, sm2Data.third))
                                                                        }
                                                                    } catch (e: Exception) {}
                                                                }
                                                                chunksLeft--
                                                                if (chunksLeft == 0) {
                                                                    // Merge custom cards
                                                                    finalVocabIds.forEach { id ->
                                                                        if (loadedCards.none { it.id == id }) {
                                                                            userCardsMap[id]?.let {
                                                                                val sm2Data = sm2DataMap[id] ?: Triple(CardState.NEW, 0, 2.5)
                                                                                loadedCards.add(it.copy(levelId = levelId, state = sm2Data.first, interval = sm2Data.second, easeFactor = sm2Data.third))
                                                                            }
                                                                        }
                                                                    }
                                                                    
                                                                    // Priority sorting: HARD (1) -> GOOD (2) -> EASY/NEW (3)
                                                                    val sortedCards = loadedCards.sortedWith(compareBy { card ->
                                                                        val status = statusMap[card.id] ?: ""
                                                                        when (status.uppercase()) {
                                                                            "HARD" -> 1
                                                                            "GOOD" -> 2
                                                                            else -> 3
                                                                        }
                                                                    })
                                                                    vocabCards = sortedCards
                                                                    isLoading = false
                                                                }
                                                            }
                                                            .addOnFailureListener {
                                                                chunksLeft--
                                                                if (chunksLeft == 0) {
                                                                    // Fallback custom cards
                                                                    finalVocabIds.forEach { id ->
                                                                        userCardsMap[id]?.let {
                                                                            loadedCards.add(it.copy(levelId = levelId))
                                                                        }
                                                                    }
                                                                    val sortedCards = loadedCards.sortedWith(compareBy { card ->
                                                                        val status = statusMap[card.id] ?: ""
                                                                        when (status.uppercase()) {
                                                                            "HARD" -> 1
                                                                            "GOOD" -> 2
                                                                            else -> 3
                                                                        }
                                                                    })
                                                                    vocabCards = sortedCards
                                                                    isLoading = false
                                                                }
                                                            }
                                                    }
                                                }
                                                .addOnFailureListener {
                                                    isLoading = false
                                                }
                                        }
                                    }
                                    .addOnFailureListener {
                                        isLoading = false
                                    }
                            }
                            .addOnFailureListener {
                                isLoading = false
                            }
                    }
                    .addOnFailureListener {
                        isLoading = false
                    }
            } else {
                isLoading = false
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
    var shuffleTrigger by remember { mutableIntStateOf(0) }
    var isStudyMode by remember { mutableStateOf(language.uppercase() == "SPACED" || levelId.startsWith("day_")) }

    var starredCardIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    // val uid is already defined at the top of the composable

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

    val recordProgress = { card: VocabCard, rating: ReviewRating ->
        if (uid.isNotEmpty() && uid != "anonymous") {
            spacedRepetitionViewModel.recordProgress(
                vocabId = card.id,
                currentState = card.state,
                currentInterval = card.interval,
                currentEaseFactor = card.easeFactor,
                rating = rating
            )
        }
    }

    val onCardRated = { card: VocabCard, ratingName: String ->
        val rating = try { ReviewRating.valueOf(ratingName) } catch (e: Exception) { ReviewRating.GOOD }
        recordProgress(card, rating)
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
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isStudyMode) "Học" else "Duyệt",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isStudyMode) FlowPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Switch(
                            checked = isStudyMode,
                            onCheckedChange = { isStudyMode = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = FlowPrimary, checkedTrackColor = FlowPrimary.copy(alpha = 0.5f)),
                            modifier = Modifier.scale(0.8f)
                        )
                    }
                    if (vocabCards.isNotEmpty()) {
                        IconButton(onClick = {
                            vocabCards = vocabCards.shuffled()
                            currentIndex = 0
                            isFlipped = false
                            shuffleTrigger++
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Học lại", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            if (vocabCards.isNotEmpty() && !isStudyMode) {
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
                val isStarred = starredCardIds.contains(currentCard.id)

                val onToggleStar: () -> Unit = {
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

                val onPlayTts: (String) -> Unit = { text: String ->
                    val cleanWord = text.substringBefore("(").trim()
                    val speakLocale = when {
                        cleanWord.any { it.code in 0x3040..0x30FF || it.code in 0x31F0..0x31FF } -> Locale.JAPANESE
                        cleanWord.any { it.code in 0x4E00..0x9FFF } && !cleanWord.any { it.code in 0x3040..0x309F } -> Locale.CHINESE
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
                }

                StudyFlashcard(
                    modifier = Modifier.weight(1f),
                    frontText = currentCard.front,
                    backText = currentCard.back,
                    isFlipped = isFlipped,
                    onFlip = { isFlipped = !isFlipped },
                    isStarred = isStarred,
                    onToggleStar = onToggleStar,
                    onPlayTts = onPlayTts
                )

                if (isFlipped) {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    val againResult = com.example.flashcards.model.SpacedRepetition.calculateNextReview(currentCard.state, currentCard.interval, currentCard.easeFactor, ReviewRating.AGAIN)
                    val hardResult = com.example.flashcards.model.SpacedRepetition.calculateNextReview(currentCard.state, currentCard.interval, currentCard.easeFactor, ReviewRating.HARD)
                    val goodResult = com.example.flashcards.model.SpacedRepetition.calculateNextReview(currentCard.state, currentCard.interval, currentCard.easeFactor, ReviewRating.GOOD)
                    val easyResult = com.example.flashcards.model.SpacedRepetition.calculateNextReview(currentCard.state, currentCard.interval, currentCard.easeFactor, ReviewRating.EASY)

                    val formatInterval = { days: Int ->
                        when (days) {
                            0 -> "<1p"
                            else -> "${days}n"
                        }
                    }

                    if (isStudyMode) {
                        AnkiRatingButtons(
                            onRate = { rating -> 
                                onCardRated(currentCard, rating.name) 
                            },
                            againInterval = formatInterval(againResult.intervalDays),
                            hardInterval = formatInterval(hardResult.intervalDays),
                            goodInterval = formatInterval(goodResult.intervalDays),
                            easyInterval = formatInterval(easyResult.intervalDays)
                        )
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
            },
            onReviewAgain = {
                showWishDialog = false
                vocabCards = vocabCards.shuffled()
                currentIndex = 0
                isFlipped = false
                shuffleTrigger++
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
