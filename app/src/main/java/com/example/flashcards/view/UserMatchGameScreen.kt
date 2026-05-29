package com.example.flashcards.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.flashcards.ui.theme.FlowPrimary
import com.example.flashcards.utils.CryptoUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers
import java.util.UUID

enum class MatchGameState { CHOOSE_SOURCE, SETUP, PLAYING, FINISHED }
enum class MatchPlayMode { SINGLE, PVP_LOCAL }

data class MatchCardItem(
    val id: String = "",
    val text: String = "",
    val isFront: Boolean = false,
    var isSelected: Boolean = false,
    var isMatched: Boolean = false,
    var isError: Boolean = false,
    var matchedBy: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserMatchGameScreen(
    navController: NavController,
    levelId: String
) {
    val firestore = remember { FirebaseFirestore.getInstance() }
    var rawVocabList by remember { mutableStateOf<List<VocabCard>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    
    val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
    var subscribedPackages by remember { mutableStateOf<List<String>>(listOf("FREE")) }
    var customStudySets by remember { mutableStateOf<List<com.example.flashcards.model.StudySet>>(emptyList()) }

    var activeLevelId by remember { mutableStateOf(levelId) }
    var gameState by remember { 
        mutableStateOf(
            if (levelId == "select" || levelId == "study_method_root") MatchGameState.CHOOSE_SOURCE 
            else MatchGameState.SETUP
        )
    }
    var playMode by remember { mutableStateOf(MatchPlayMode.SINGLE) }
    var gridSize by remember { mutableIntStateOf(5) }

    // Gameplay States
    var p1Items by remember { mutableStateOf<List<MatchCardItem>>(emptyList()) }
    var p1SelectedFirst by remember { mutableStateOf<Int?>(null) }
    var p1SelectedSecond by remember { mutableStateOf<Int?>(null) }
    var p2Items by remember { mutableStateOf<List<MatchCardItem>>(emptyList()) }
    var p2SelectedFirst by remember { mutableStateOf<Int?>(null) }
    var p2SelectedSecond by remember { mutableStateOf<Int?>(null) }

    var elapsedTimeMs by remember { mutableLongStateOf(0L) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var winnerMessage by remember { mutableStateOf("") }
    var showUpgradeDialog by remember { mutableStateOf(false) }

    val hasVip = remember(subscribedPackages) {
        subscribedPackages.contains("VIP") || subscribedPackages.any { it.startsWith("VIP") }
    }

    // Fetch user packages & custom sets for CHOOSE_SOURCE screen
    LaunchedEffect(currentUserUid) {
        if (currentUserUid != "anonymous") {
            firestore.collection("users").document(currentUserUid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val pkgs = doc.get("subscribedPackages") as? List<String>
                        if (pkgs != null) {
                            subscribedPackages = pkgs
                        }
                    }
                }
            
            firestore.collection("users").document(currentUserUid).collection("studySets").get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                val id = doc.id
                                val title = doc.getString("title") ?: ""
                                val description = doc.getString("description") ?: ""
                                val isPublic = doc.getBoolean("isPublic") ?: false
                                val cardsList = doc.get("cards") as? List<Map<String, Any>> ?: emptyList()
                                val cards = cardsList.mapNotNull { cardMap ->
                                    val cardId = cardMap["id"] as? String ?: return@mapNotNull null
                                    val question = cardMap["question"] as? String ?: ""
                                    val answer = cardMap["answer"] as? String ?: ""
                                    com.example.flashcards.model.Flashcard(cardId, question, answer)
                                }
                                com.example.flashcards.model.StudySet(
                                    id = id,
                                    title = title,
                                    description = description,
                                    cards = cards,
                                    isPublic = isPublic
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        customStudySets = list
                    }
                }
        }
    }

    // Load selected vocabulary from VIP system or custom user deck
    LaunchedEffect(activeLevelId) {
        if (activeLevelId == "select" || activeLevelId == "study_method_root") {
            isLoading = false
            return@LaunchedEffect
        }
        isLoading = true
        if (activeLevelId.startsWith("deck_")) {
            val deckId = activeLevelId.replace("deck_", "")
            firestore.collection("users").document(currentUserUid).collection("studySets").document(deckId).get()
                .addOnSuccessListener { doc ->
                    val cards = doc.get("cards") as? List<Map<String, Any>> ?: emptyList()
                    val list = mutableListOf<VocabCard>()
                    cards.forEach { cardMap ->
                        val id = cardMap["id"] as? String ?: return@forEach
                        val front = cardMap["question"] as? String ?: return@forEach
                        val back = cardMap["answer"] as? String ?: return@forEach
                        list.add(VocabCard(id, front, back, activeLevelId))
                    }
                    if (list.isEmpty()) {
                        rawVocabList = getFallbackVocabList(activeLevelId)
                    } else {
                        rawVocabList = list
                    }
                    isLoading = false
                }
                .addOnFailureListener {
                    rawVocabList = getFallbackVocabList(activeLevelId)
                    isLoading = false
                }
        } else {
            firestore.collection("system_vocabulary").whereEqualTo("levelId", activeLevelId).get()
                .addOnSuccessListener { snapshot ->
                    val list = mutableListOf<VocabCard>()
                    snapshot.documents.forEach { d ->
                        val id = d.id
                        val rawFront = d.getString("front") ?: ""
                        val rawBack = d.getString("back") ?: ""
                        val front = try { CryptoUtils.decrypt(rawFront) } catch (e: Exception) { rawFront }
                        val back = try { CryptoUtils.decrypt(rawBack) } catch (e: Exception) { rawBack }
                        if (front.isNotBlank() && back.isNotBlank()) {
                            list.add(VocabCard(id, front, back, activeLevelId))
                        }
                    }
                    if (list.isEmpty()) {
                        rawVocabList = getFallbackVocabList(activeLevelId)
                    } else {
                        rawVocabList = list
                    }
                    isLoading = false
                }
                .addOnFailureListener {
                    rawVocabList = getFallbackVocabList(activeLevelId)
                    isLoading = false
                }
        }
    }

    // Timer coroutine
    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            val startTime = System.currentTimeMillis() - elapsedTimeMs
            while (isTimerRunning) {
                delay(10)
                elapsedTimeMs = System.currentTimeMillis() - startTime
            }
        }
    }

    val displayLevelName = when {
        activeLevelId.startsWith("deck_") -> "Bộ thẻ Tự Tạo"
        activeLevelId == "QUIZ_JA_N5" -> "Cấp độ N5"
        activeLevelId == "QUIZ_JA_N4" -> "Cấp độ N4"
        activeLevelId == "QUIZ_JA_N3" -> "Cấp độ N3"
        activeLevelId == "QUIZ_JA_N2" -> "Cấp độ N2"
        activeLevelId == "QUIZ_JA_N1" -> "Cấp độ N1"
        activeLevelId == "QUIZ_TOEIC_450" -> "TOEIC 450+"
        activeLevelId == "QUIZ_TOEIC_650" -> "TOEIC 650+"
        activeLevelId == "QUIZ_TOEIC_800" -> "TOEIC 800+"
        activeLevelId == "QUIZ_IELTS_55" -> "IELTS Band 5.5"
        activeLevelId == "QUIZ_IELTS_65" -> "IELTS Band 6.5"
        activeLevelId == "QUIZ_IELTS_75" -> "IELTS Band 7.5+"
        activeLevelId == "QUIZ_ZH_BASIC" -> "Trung Cơ Bản"
        activeLevelId == "QUIZ_PA_INTRO" -> "Pali Sơ Cấp"
        activeLevelId == "select" || activeLevelId == "study_method_root" -> "Chọn bộ từ vựng"
        else -> activeLevelId.replace("QUIZ_", "").replace("_", " ")
    }

    val handleStartGame: () -> Unit = {
        val isSystemVip = !activeLevelId.startsWith("deck_")
        if (isSystemVip && !hasVip) {
            showUpgradeDialog = true
        } else {
            p1Items = generateGameItems(rawVocabList, gridSize)
            if (playMode == MatchPlayMode.PVP_LOCAL) {
                p2Items = generateGameItems(rawVocabList, gridSize)
            }
            elapsedTimeMs = 0L
            isTimerRunning = true
            gameState = MatchGameState.PLAYING
        }
    }

    Scaffold(
        topBar = {
            if (playMode == MatchPlayMode.SINGLE || gameState == MatchGameState.SETUP || gameState == MatchGameState.CHOOSE_SOURCE) {
                TopAppBar(
                    title = { Text(if (gameState == MatchGameState.CHOOSE_SOURCE) "Giải trí (Match)" else "Game Nối Từ: $displayLevelName", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.Close, contentDescription = "Đóng")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FlowPrimary)
                }
            } else {
                when (gameState) {
                    MatchGameState.CHOOSE_SOURCE -> {
                        MatchSelectionFlow(
                            customStudySets = customStudySets,
                            subscribedPackages = subscribedPackages,
                            onSelectionCompleted = { selectedId ->
                                activeLevelId = selectedId
                                gameState = MatchGameState.SETUP
                            }
                        )
                    }
                    MatchGameState.SETUP -> {
                        // Shared Setup Dialog pops up above a blurred background
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            SharedMatchSetupDialog(
                                displayLevelName = displayLevelName,
                                playMode = playMode,
                                gridSize = gridSize,
                                onPlayModeChange = { playMode = it },
                                onGridSizeChange = { gridSize = it },
                                onStart = handleStartGame,
                                onCancel = {
                                    if (levelId == "select" || levelId == "study_method_root") {
                                        gameState = MatchGameState.CHOOSE_SOURCE
                                    } else {
                                        navController.popBackStack()
                                    }
                                }
                            )
                        }
                    }
                    MatchGameState.PLAYING -> {
                        if (playMode == MatchPlayMode.PVP_LOCAL) {
                            // Split-screen PVP layout
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Player 2 (Top, Rotated 180)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .graphicsLayer(rotationZ = 180f)
                                ) {
                                    PlayerMatchGrid(
                                        items = p2Items,
                                        selectedIndex1 = p2SelectedFirst,
                                        selectedIndex2 = p2SelectedSecond,
                                        onSelect = { idx ->
                                            handleCardSelection(idx, p2SelectedFirst, p2SelectedSecond, p2Items,
                                                onUpdateFirst = { p2SelectedFirst = it },
                                                onUpdateSecond = { p2SelectedSecond = it },
                                                onUpdateItems = { newItems ->
                                                    p2Items = newItems
                                                    if (newItems.all { it.isMatched }) {
                                                        isTimerRunning = false
                                                        winnerMessage = "Người chơi 2 THẮNG! 🎉"
                                                        gameState = MatchGameState.FINISHED
                                                    }
                                                }
                                            )
                                        }
                                    )
                                }

                                // Shared Timer in the Middle
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black)
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val seconds = (elapsedTimeMs / 1000)
                                    val millis = (elapsedTimeMs % 1000) / 10
                                    Text(
                                        text = String.format("%02d:%02d", seconds, millis),
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Player 1 (Bottom)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                ) {
                                    PlayerMatchGrid(
                                        items = p1Items,
                                        selectedIndex1 = p1SelectedFirst,
                                        selectedIndex2 = p1SelectedSecond,
                                        onSelect = { idx ->
                                            handleCardSelection(idx, p1SelectedFirst, p1SelectedSecond, p1Items,
                                                onUpdateFirst = { p1SelectedFirst = it },
                                                onUpdateSecond = { p1SelectedSecond = it },
                                                onUpdateItems = { newItems ->
                                                    p1Items = newItems
                                                    if (newItems.all { it.isMatched }) {
                                                        isTimerRunning = false
                                                        winnerMessage = "Người chơi 1 THẮNG! 🎉"
                                                        gameState = MatchGameState.FINISHED
                                                    }
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                        } else {
                            // SOLO full-screen layout
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 24.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val seconds = (elapsedTimeMs / 1000)
                                val millis = (elapsedTimeMs % 1000) / 10
                                Text(
                                    text = String.format("Thời gian: %02d:%02d", seconds, millis),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = FlowPrimary,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                                PlayerMatchGrid(
                                    items = p1Items,
                                    selectedIndex1 = p1SelectedFirst,
                                    selectedIndex2 = p1SelectedSecond,
                                    onSelect = { idx ->
                                        handleCardSelection(idx, p1SelectedFirst, p1SelectedSecond, p1Items,
                                            onUpdateFirst = { p1SelectedFirst = it },
                                            onUpdateSecond = { p1SelectedSecond = it },
                                            onUpdateItems = { newItems ->
                                                p1Items = newItems
                                                if (newItems.all { it.isMatched }) {
                                                    isTimerRunning = false
                                                    winnerMessage = "Hoàn thành xuất sắc! 🎉"
                                                    gameState = MatchGameState.FINISHED
                                                }
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }
                    MatchGameState.FINISHED -> {
                        // Finished dialog pops up
                        SharedMatchResultDialog(
                            winnerMessage = winnerMessage,
                            timeMs = elapsedTimeMs,
                            playMode = playMode,
                            onPlayAgain = {
                                gameState = MatchGameState.SETUP
                            },
                            onExit = {
                                if (levelId == "select" || levelId == "study_method_root") {
                                    gameState = MatchGameState.CHOOSE_SOURCE
                                } else {
                                    navController.popBackStack()
                                }
                            }
                        )
                    }
                }
            }

            // Premium Upgrade Dialog for VIP level locks
            if (showUpgradeDialog) {
                AlertDialog(
                    onDismissRequest = { showUpgradeDialog = false },
                    title = {
                        Text(
                            text = "Nâng Cấp FlowCards VIP 👑",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    text = {
                        Text(
                            text = "Tính năng học tập và chơi game nối từ với Bộ Từ Vựng VIP Hệ Thống yêu cầu quyền VIP của FlowCards. Hãy nâng cấp để trải nghiệm các gói từ vựng Premium cực kì hấp dẫn!"
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { showUpgradeDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary)
                        ) {
                            Text("Đồng ý", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showUpgradeDialog = false }) {
                            Text("Hủy", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = MaterialTheme.colorScheme.surface
                )
            }
        }
    }
}

@Composable
fun MatchSelectionFlow(
    customStudySets: List<com.example.flashcards.model.StudySet>,
    subscribedPackages: List<String>,
    onSelectionCompleted: (String) -> Unit
) {
    var selectedSource by remember { mutableStateOf<String?>(null) } // "CUSTOM", "VIP"
    var selectedDeckId by remember { mutableStateOf<String?>(null) }
    var selectedLevelId by remember { mutableStateOf<String?>(null) }

    // VIP system levels
    val systemLevels = remember {
        listOf(
            "QUIZ_JA_N5" to "Tiếng Nhật N5",
            "QUIZ_JA_N4" to "Tiếng Nhật N4",
            "QUIZ_JA_N3" to "Tiếng Nhật N3",
            "QUIZ_JA_N2" to "Tiếng Nhật N2",
            "QUIZ_JA_N1" to "Tiếng Nhật N1",
            "QUIZ_TOEIC_450" to "TOEIC 450+",
            "QUIZ_TOEIC_650" to "TOEIC 650+",
            "QUIZ_TOEIC_800" to "TOEIC 800+",
            "QUIZ_IELTS_55" to "IELTS 5.5",
            "QUIZ_IELTS_65" to "IELTS 6.5",
            "QUIZ_IELTS_75" to "IELTS 7.5+",
            "QUIZ_ZH_BASIC" to "Trung Cơ Bản",
            "QUIZ_PA_INTRO" to "Pali Sơ Cấp"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Lựa chọn nguồn từ vựng học tập",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Source Selector Rows
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Button 1: CUSTOM SET
            val isCustomSelected = selectedSource == "CUSTOM"
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        selectedSource = "CUSTOM"
                        selectedLevelId = null
                    },
                border = if (isCustomSelected) BorderStroke(2.dp, FlowPrimary) else null,
                colors = CardDefaults.cardColors(
                    containerColor = if (isCustomSelected) FlowPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Style,
                        contentDescription = null,
                        tint = if (isCustomSelected) FlowPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Bộ Thẻ Tự Tạo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isCustomSelected) FlowPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Button 2: VIP SYSTEM
            val isVipSelected = selectedSource == "VIP"
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        selectedSource = "VIP"
                        selectedDeckId = null
                    },
                border = if (isVipSelected) BorderStroke(2.dp, FlowPrimary) else null,
                colors = CardDefaults.cardColors(
                    containerColor = if (isVipSelected) FlowPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = if (isVipSelected) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Hệ Thống (VIP)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isVipSelected) FlowPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sub-list showing Decks or Levels
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (selectedSource == "CUSTOM") {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Chọn Bộ Thẻ Tự Tạo của bạn:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    if (customStudySets.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Bạn chưa có bộ thẻ tự tạo nào. Hãy tạo một bộ thẻ mới!", color = Color.Gray, textAlign = TextAlign.Center)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(customStudySets) { set ->
                                val isDeckSelected = selectedDeckId == set.id
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { selectedDeckId = set.id },
                                    border = if (isDeckSelected) BorderStroke(2.dp, FlowPrimary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isDeckSelected) FlowPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(set.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            if (set.description.isNotEmpty()) {
                                                Text(set.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                            }
                                        }
                                        Surface(
                                            color = FlowPrimary.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "${set.cards.size} thẻ",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                color = FlowPrimary,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (selectedSource == "VIP") {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Chọn Bộ Từ Vựng VIP Hệ Thống:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(systemLevels) { level ->
                            val isLevelSelected = selectedLevelId == level.first
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedLevelId = level.first },
                                border = if (isLevelSelected) BorderStroke(2.dp, FlowPrimary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isLevelSelected) FlowPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color(0xFFFFD700),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(level.second, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                    if (isLevelSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = FlowPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Vui lòng chọn nguồn học tập phía trên 👆", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Next button
        val canGoNext = (selectedSource == "CUSTOM" && selectedDeckId != null) || (selectedSource == "VIP" && selectedLevelId != null)
        Button(
            onClick = {
                val targetId = if (selectedSource == "CUSTOM") "deck_$selectedDeckId" else selectedLevelId!!
                onSelectionCompleted(targetId)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary),
            enabled = canGoNext
        ) {
            Text("Tiếp theo", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SharedMatchSetupDialog(
    displayLevelName: String,
    playMode: MatchPlayMode,
    gridSize: Int,
    onPlayModeChange: (MatchPlayMode) -> Unit,
    onGridSizeChange: (Int) -> Unit,
    onStart: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Thiết Lập Trò Chơi 🎮",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = FlowPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = displayLevelName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                // Grid Size Options
                Text(
                    text = "Kích Thước Lưới:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(5 to "Lưới 5x5", 7 to "Lưới 7x7", 10 to "Lưới 10x10").forEach { (size, label) ->
                        val isSelected = gridSize == size
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onGridSizeChange(size) },
                            border = if (isSelected) BorderStroke(2.dp, FlowPrimary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) FlowPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) FlowPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Game Mode Options
                Text(
                    text = "Chế Độ Chơi:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Option 1: Solo
                    val isSoloSelected = playMode == MatchPlayMode.SINGLE
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onPlayModeChange(MatchPlayMode.SINGLE) },
                        border = if (isSoloSelected) BorderStroke(2.dp, FlowPrimary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSoloSelected) FlowPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Solo 🎯", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (isSoloSelected) FlowPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("1 Người", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    }

                    // Option 2: PVP
                    val isPvpSelected = playMode == MatchPlayMode.PVP_LOCAL
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onPlayModeChange(MatchPlayMode.PVP_LOCAL) },
                        border = if (isPvpSelected) BorderStroke(2.dp, FlowPrimary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isPvpSelected) FlowPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Đối Kháng ⚔️", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (isPvpSelected) FlowPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Chia đôi màn hình", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Control Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, FlowPrimary)
                    ) {
                        Text("Hủy", color = FlowPrimary, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onStart,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary)
                    ) {
                        Text("Bắt Đầu 🚀", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun SharedMatchResultDialog(
    winnerMessage: String,
    timeMs: Long,
    playMode: MatchPlayMode,
    onPlayAgain: () -> Unit,
    onExit: () -> Unit
) {
    val congratsWishes = remember {
        listOf(
            "Tuyệt vời! Bạn là một bậc thầy nối từ thực thụ! 🏆",
            "Quá đỉnh! Phản xạ nhanh như chớp! ⚡",
            "Xuất sắc! Trí nhớ siêu phàm! 🧠",
            "Đẳng cấp! Chiến thắng hoàn toàn xứng đáng! 🥇",
            "Chiến thắng vang dội! Chúc mừng nhà vô địch! 👑"
        )
    }
    val randomWish = remember { congratsWishes.random() }

    val seconds = (timeMs / 1000)
    val millis = (timeMs % 1000)
    val timeStr = String.format("%d.%03ds", seconds, millis)

    Dialog(onDismissRequest = onExit) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFFBBF24),
                    modifier = Modifier.size(96.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = winnerMessage,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF10B981),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = randomWish,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Thời gian hoàn thành:",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.Gray
                )
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = FlowPrimary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
                )

                Button(
                    onClick = onPlayAgain,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary)
                ) {
                    Text("Chơi Lại 🔄", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onExit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, FlowPrimary)
                ) {
                    Text("Thoát Màn Hình", color = FlowPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun PlayerMatchGrid(
    items: List<MatchCardItem>,
    selectedIndex1: Int?,
    selectedIndex2: Int?,
    onSelect: (Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(if (items.size > 14) 4 else if (items.size > 10) 3 else 2),
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items.size) { index ->
            val item = items[index]
            val isSelected = selectedIndex1 == index || selectedIndex2 == index

            val cardBgColor = when {
                item.isMatched -> Color(0xFFD1FAE5)
                item.isError && isSelected -> Color(0xFFFEE2E2)
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
            val cardBorderColor = when {
                item.isMatched -> Color(0xFF10B981)
                item.isError && isSelected -> Color(0xFFEF4444)
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
                    .aspectRatio(if (items.size > 14) 1.2f else 1.5f)
                    .clickable(enabled = !item.isMatched) { onSelect(index) },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(2.dp, cardBorderColor),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize().padding(4.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = item.text,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

fun generateGameItems(vocabList: List<VocabCard>, gridSize: Int): List<MatchCardItem> {
    if (vocabList.isEmpty()) return emptyList()
    val pairsNeeded = gridSize
    val selectedVocab = mutableListOf<VocabCard>()
    
    for (i in 0 until pairsNeeded) {
        selectedVocab.add(vocabList.random())
    }
    
    val list = mutableListOf<MatchCardItem>()
    selectedVocab.forEachIndexed { index, card ->
        val uniqueId = "${card.id}_$index"
        list.add(MatchCardItem(uniqueId, card.front, isFront = true))
        list.add(MatchCardItem(uniqueId, card.back, isFront = false))
    }
    return list.shuffled()
}

fun handleCardSelection(
    index: Int,
    firstIdx: Int?,
    secondIdx: Int?,
    items: List<MatchCardItem>,
    onUpdateFirst: (Int?) -> Unit,
    onUpdateSecond: (Int?) -> Unit,
    onUpdateItems: (List<MatchCardItem>) -> Unit
) {
    if (items[index].isMatched) return
    if (secondIdx != null) return

    if (firstIdx == null) {
        onUpdateFirst(index)
    } else if (firstIdx == index) {
        onUpdateFirst(null)
    } else {
        onUpdateSecond(index)
        val firstItem = items[firstIdx]
        val secondItem = items[index]

        if (firstItem.id == secondItem.id && firstItem.isFront != secondItem.isFront) {
            val newItems = items.mapIndexed { idx, cell ->
                if (idx == firstIdx || idx == index) cell.copy(isMatched = true) else cell
            }
            onUpdateItems(newItems)
            onUpdateFirst(null)
            onUpdateSecond(null)
        } else {
            val newItems = items.mapIndexed { idx, cell ->
                if (idx == firstIdx || idx == index) cell.copy(isError = true) else cell
            }
            onUpdateItems(newItems)
            
            GlobalScope.launch(Dispatchers.Main) {
                delay(600)
                val resetItems = newItems.map { it.copy(isError = false) }
                onUpdateItems(resetItems)
                onUpdateFirst(null)
                onUpdateSecond(null)
            }
        }
    }
}

private fun getFallbackVocabList(levelId: String): List<VocabCard> {
    return listOf(
        VocabCard(UUID.randomUUID().toString(), "Confirm", "Xác nhận"),
        VocabCard(UUID.randomUUID().toString(), "Submit", "Nộp / Trình"),
        VocabCard(UUID.randomUUID().toString(), "Delay", "Trì hoãn"),
        VocabCard(UUID.randomUUID().toString(), "Negotiate", "Thương lượng"),
        VocabCard(UUID.randomUUID().toString(), "Implement", "Thi hành"),
        VocabCard(UUID.randomUUID().toString(), "Collaborate", "Hợp tác"),
        VocabCard(UUID.randomUUID().toString(), "Analyze", "Phân tích"),
        VocabCard(UUID.randomUUID().toString(), "Synthesize", "Tổng hợp"),
        VocabCard(UUID.randomUUID().toString(), "Hypothesis", "Giả thuyết"),
        VocabCard(UUID.randomUUID().toString(), "Evaluate", "Đánh giá")
    )
}
