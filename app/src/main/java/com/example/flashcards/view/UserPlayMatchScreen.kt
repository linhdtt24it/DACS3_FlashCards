package com.example.flashcards.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
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
import kotlinx.coroutines.delay
import java.util.UUID

data class MatchCardItem(
    val id: String, // maps to the same ID for matching pairs
    val text: String,
    val isFront: Boolean,
    var isSelected: Boolean = false,
    var isMatched: Boolean = false,
    var isError: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserPlayMatchScreen(
    navController: NavController,
    levelId: String
) {
    val firestore = remember { FirebaseFirestore.getInstance() }
    var rawVocabList by remember { mutableStateOf<List<VocabCard>>(emptyList()) }
    var gridItems by remember { mutableStateOf<List<MatchCardItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Load vocabulary cards from Firestore
    LaunchedEffect(levelId) {
        if (levelId.startsWith("day_")) {
            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
            if (uid != "anonymous") {
                val onFallback = {
                    firestore.collection("user_favorites")
                        .whereEqualTo("uid", uid)
                        .whereEqualTo("starred", true)
                        .get()
                        .addOnSuccessListener { favSnapshot ->
                            val favCards = favSnapshot.documents.mapNotNull { doc ->
                                val id = doc.getString("vocabId") ?: doc.id
                                val front = doc.getString("front") ?: ""
                                val back = doc.getString("back") ?: ""
                                val originLevelId = doc.getString("levelId") ?: ""
                                if (front.isNotEmpty() && back.isNotEmpty()) {
                                    VocabCard(id, front, back, originLevelId)
                                } else null
                            }
                            if (favCards.isNotEmpty()) {
                                rawVocabList = favCards.take(8)
                                isLoading = false
                                initializeGame(rawVocabList) { gridItems = it }
                            } else {
                                rawVocabList = getFallbackVocabList("QUIZ_JA_N5")
                                isLoading = false
                                initializeGame(rawVocabList) { gridItems = it }
                            }
                        }
                        .addOnFailureListener {
                            rawVocabList = getFallbackVocabList("QUIZ_JA_N5")
                            isLoading = false
                            initializeGame(rawVocabList) { gridItems = it }
                        }
                }

                firestore.collection("users").document(uid).get()
                    .addOnSuccessListener { userDoc ->
                        val dailyLimit = userDoc.getLong("dailyWordLimit")?.toInt() ?: 10
                        
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
                                        
                                        uniqueProgressDocs.forEach { doc ->
                                            val vocabId = doc.getString("vocabId") ?: ""
                                            val status = doc.getString("status") ?: ""
                                            val nextReview = doc.getTimestamp("nextReview")
                                            if (vocabId.isNotEmpty() && status != "MASTERED" && status != "MASTER") {
                                                statusMap[vocabId] = status
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
                                            onFallback()
                                        } else {
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
                                                                            loadedCards.add(VocabCard(id, front, back, levelId))
                                                                        }
                                                                    } catch (e: Exception) {}
                                                                }
                                                                chunksLeft--
                                                                if (chunksLeft == 0) {
                                                                    finalVocabIds.forEach { id ->
                                                                        if (loadedCards.none { it.id == id }) {
                                                                            userCardsMap[id]?.let {
                                                                                loadedCards.add(it.copy(levelId = levelId))
                                                                            }
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
                                                                    if (sortedCards.isEmpty()) {
                                                                        onFallback()
                                                                    } else {
                                                                        rawVocabList = sortedCards
                                                                        isLoading = false
                                                                        initializeGame(rawVocabList) { gridItems = it }
                                                                    }
                                                                }
                                                            }
                                                            .addOnFailureListener {
                                                                chunksLeft--
                                                                if (chunksLeft == 0) {
                                                                    finalVocabIds.forEach { id ->
                                                                        if (loadedCards.none { it.id == id }) {
                                                                            userCardsMap[id]?.let {
                                                                                loadedCards.add(it.copy(levelId = levelId))
                                                                            }
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
                                                                    if (sortedCards.isEmpty()) {
                                                                        onFallback()
                                                                    } else {
                                                                        rawVocabList = sortedCards
                                                                        isLoading = false
                                                                        initializeGame(rawVocabList) { gridItems = it }
                                                                    }
                                                                }
                                                            }
                                                    }
                                                }
                                                .addOnFailureListener {
                                                    onFallback()
                                                }
                                        }
                                    }
                                    .addOnFailureListener {
                                        onFallback()
                                    }
                            }
                            .addOnFailureListener {
                                isLoading = false
                                rawVocabList = getFallbackVocabList(levelId)
                                initializeGame(rawVocabList) { gridItems = it }
                            }
                    }
                    .addOnFailureListener {
                        isLoading = false
                        rawVocabList = getFallbackVocabList(levelId)
                        initializeGame(rawVocabList) { gridItems = it }
                    }
            } else {
                isLoading = false
                rawVocabList = getFallbackVocabList(levelId)
                initializeGame(rawVocabList) { gridItems = it }
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
                                VocabCard(id, front, back)
                            } else null
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (list.isNotEmpty()) {
                        rawVocabList = list
                    } else {
                        rawVocabList = getFallbackVocabList(levelId)
                    }
                    initializeGame(rawVocabList) { gridItems = it }
                }
                .addOnFailureListener {
                    isLoading = false
                    rawVocabList = getFallbackVocabList(levelId)
                    initializeGame(rawVocabList) { gridItems = it }
                }
        }
    }

    var selectedFirstIndex by remember { mutableStateOf<Int?>(null) }
    var selectedSecondIndex by remember { mutableStateOf<Int?>(null) }
    var score by remember { mutableIntStateOf(0) }
    var gameCompleted by remember { mutableStateOf(false) }
    var showWishDialog by remember { mutableStateOf(true) }

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
                title = { Text("Game Nối Từ: $displayLevelName", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng")
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
            } else if (gridItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Cấp độ này hiện chưa có từ vựng.", color = Color.Gray)
                }
            } else if (!gameCompleted) {
                Text(
                    text = "Hãy ghép đôi từ vựng gốc với nghĩa chính xác!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Lưới hiển thị các thẻ nối từ
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(gridItems.size) { index ->
                        val item = gridItems[index]
                        val isSelected = selectedFirstIndex == index || selectedSecondIndex == index

                        val cardBgColor = when {
                            item.isMatched -> Color(0xFFD1FAE5) // Light Green for matched
                            item.isError && isSelected -> Color(0xFFFEE2E2) // Light Red for error
                            isSelected -> MaterialTheme.colorScheme.primaryContainer // Blue for selected
                            else -> MaterialTheme.colorScheme.surface
                        }

                        val cardBorderColor = when {
                            item.isMatched -> Color(0xFF10B981) // Green border
                            item.isError && isSelected -> Color(0xFFEF4444) // Red border
                            isSelected -> FlowPrimary
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                        }

                        val textColor = when {
                            item.isMatched -> Color(0xFF065F46)
                            item.isError && isSelected -> Color(0xFFB91C1C)
                            isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onBackground
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clickable(enabled = !item.isMatched && selectedSecondIndex == null) {
                                    if (selectedFirstIndex == null) {
                                        selectedFirstIndex = index
                                    } else if (selectedFirstIndex == index) {
                                        selectedFirstIndex = null
                                    } else {
                                        selectedSecondIndex = index
                                        // Tiến hành kiểm tra cặp ghép đôi
                                        val firstItem = gridItems[selectedFirstIndex!!]
                                        val secondItem = gridItems[index]

                                        if (firstItem.id == secondItem.id && firstItem.isFront != secondItem.isFront) {
                                            // Ghép đôi chính xác!
                                            gridItems = gridItems.mapIndexed { idx, cell ->
                                                if (idx == selectedFirstIndex || idx == index) {
                                                    cell.copy(isMatched = true)
                                                } else cell
                                            }
                                            score += 10
                                            selectedFirstIndex = null
                                            selectedSecondIndex = null

                                            // Kiểm tra xem đã thắng chưa
                                            if (gridItems.all { it.isMatched }) {
                                                gameCompleted = true
                                            }
                                        } else {
                                            // Ghép đôi sai
                                            gridItems = gridItems.mapIndexed { idx, cell ->
                                                if (idx == selectedFirstIndex || idx == index) {
                                                    cell.copy(isError = true)
                                                } else cell
                                            }
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(2.dp, cardBorderColor),
                            colors = CardDefaults.cardColors(containerColor = cardBgColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = item.text,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Xử lý Reset sau khi báo lỗi sai
                if (selectedSecondIndex != null) {
                    LaunchedEffect(selectedSecondIndex) {
                        delay(600)
                        gridItems = gridItems.map { it.copy(isError = false) }
                        selectedFirstIndex = null
                        selectedSecondIndex = null
                    }
                }
            } else {
                // Game Completed Screen
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(100.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Chiến Thắng!",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF10B981)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Bạn đã hoàn thành ghép đôi xuất sắc!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Tổng điểm: $score điểm",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = FlowPrimary
                    )

                    Spacer(modifier = Modifier.height(48.dp))
                    Button(
                        onClick = {
                            initializeGame(rawVocabList) { gridItems = it }
                            score = 0
                            gameCompleted = false
                            showWishDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary)
                    ) {
                        Text("Chơi lại", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, FlowPrimary)
                    ) {
                        Text("Trở về Trung tâm", color = FlowPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }

    if (gameCompleted && showWishDialog) {
        RandomWishDialog(
            onDismiss = { showWishDialog = false }
        )
    }
}

// Helper to shuffle and initialize 4 pairs (8 cards) from vocabulary list
fun initializeGame(vocabList: List<VocabCard>, onInitialized: (List<MatchCardItem>) -> Unit) {
    if (vocabList.isEmpty()) return
    // Lấy tối đa 4 từ ngẫu nhiên để tạo 8 thẻ
    val selectedVocab = vocabList.shuffled().take(4)
    val list = mutableListOf<MatchCardItem>()
    selectedVocab.forEach { card ->
        list.add(MatchCardItem(card.id, card.front, isFront = true))
        list.add(MatchCardItem(card.id, card.back, isFront = false))
    }
    onInitialized(list.shuffled())
}
