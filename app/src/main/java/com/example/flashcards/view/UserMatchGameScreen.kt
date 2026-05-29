package com.example.flashcards.view

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Login
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.flashcards.ui.theme.FlowPrimary
import com.example.flashcards.ui.theme.FlowWarning
import com.example.flashcards.utils.CryptoUtils
import com.example.flashcards.viewmodel.MatchGameViewModel
import com.example.flashcards.viewmodel.MatchOnlineRoom
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers
import java.util.UUID

enum class MatchGameState { CHOOSE_SOURCE, SETUP, ROOM_WAITING, PLAYING, FINISHED }
enum class MatchPlayMode { SINGLE, PVP_ONLINE }

data class MatchCardItem(
    val id: String = "",
    val text: String = "",
    val isFront: Boolean = false,
    val isSelected: Boolean = false,
    val isMatched: Boolean = false,
    val isError: Boolean = false,
    val matchedBy: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserMatchGameScreen(
    navController: NavController,
    levelId: String,
    viewModel: MatchGameViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val firestore = remember { FirebaseFirestore.getInstance() }
    var rawVocabList by remember { mutableStateOf<List<VocabCard>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    
    val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
    val userName = FirebaseAuth.getInstance().currentUser?.displayName 
        ?: FirebaseAuth.getInstance().currentUser?.email?.substringBefore("@") 
        ?: "Người chơi"
        
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

    // Collected states from ViewModel
    val roomState by viewModel.roomState.collectAsState()
    val matchmakingStatus by viewModel.matchmakingStatus.collectAsState()
    val p1Items by viewModel.p1Items.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Local gameplay states
    var selectedFirst by remember { mutableStateOf<Int?>(null) }
    var selectedSecond by remember { mutableStateOf<Int?>(null) }
    var elapsedTimeMs by remember { mutableLongStateOf(0L) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var winnerMessage by remember { mutableStateOf("") }
    var showUpgradeDialog by remember { mutableStateOf(false) }

    val hasVip = remember(subscribedPackages) {
        subscribedPackages.contains("VIP") || subscribedPackages.any { it.startsWith("VIP") }
    }

    // Display error messages from ViewModel
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    // Matchmaking status listener (transition to play once room starts)
    LaunchedEffect(matchmakingStatus) {
        if (matchmakingStatus == "MATCHED") {
            gameState = MatchGameState.PLAYING
            elapsedTimeMs = 0L
            isTimerRunning = true
        }
    }

    // Firestore game room state listener (handles online results transition)
    LaunchedEffect(roomState) {
        val room = roomState
        if (room != null && room.status == "FINISHED") {
            isTimerRunning = false
            
            val isPlayer1 = currentUserUid == room.player1Id
            val myScore = if (isPlayer1) room.player1Score else room.player2Score
            val oppScore = if (isPlayer1) room.player2Score else room.player1Score
            
            winnerMessage = when {
                room.winnerId == currentUserUid -> "BẠN CHIẾN THẮNG! 🏆"
                room.winnerId == "DRAW" -> "HÒA CÂN SỨC! 🤝"
                myScore == 999999L -> "BẠN ĐÃ BỎ CUỘC! 💔"
                oppScore == 999999L -> "ĐỐI THỦ ĐÃ BỎ CUỘC! 🏆"
                else -> "BẠN THUA RỒI! 💔"
            }
            
            gameState = MatchGameState.FINISHED
        }
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

    val handleStartSoloGame: () -> Unit = {
        val isSystemVip = !activeLevelId.startsWith("deck_")
        if (isSystemVip && !hasVip) {
            showUpgradeDialog = true
        } else {
            val items = generateGameItems(rawVocabList, gridSize)
            viewModel.updateLocalItems(items)
            elapsedTimeMs = 0L
            isTimerRunning = true
            gameState = MatchGameState.PLAYING
        }
    }

    val handleCreateRoom: () -> Unit = {
        val isSystemVip = !activeLevelId.startsWith("deck_")
        if (isSystemVip && !hasVip) {
            showUpgradeDialog = true
        } else {
            viewModel.createRoomCode(currentUserUid, userName, activeLevelId, gridSize, rawVocabList)
            gameState = MatchGameState.ROOM_WAITING
        }
    }

    val handleJoinRoom: (String) -> Unit = { code ->
        viewModel.joinRoomByCode(code, currentUserUid, userName)
    }

    Scaffold(
        topBar = {
            if (gameState != MatchGameState.PLAYING && gameState != MatchGameState.ROOM_WAITING) {
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
                                onStartSolo = handleStartSoloGame,
                                onCreateRoom = handleCreateRoom,
                                onJoinRoom = handleJoinRoom,
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
                    MatchGameState.ROOM_WAITING -> {
                        RoomWaitingScreen(
                            roomId = roomState?.roomId ?: "......",
                            onCancel = {
                                viewModel.cancelMatchmaking()
                                gameState = MatchGameState.SETUP
                            }
                        )
                    }
                    MatchGameState.PLAYING -> {
                        // Online score properties for PVP state overlay
                        val isPlayer1 = currentUserUid == roomState?.player1Id
                        val myScore = if (isPlayer1) roomState?.player1Score else roomState?.player2Score
                        val oppScore = if (isPlayer1) roomState?.player2Score else roomState?.player1Score
                        val oppName = if (isPlayer1) roomState?.player2Name.orEmpty() else roomState?.player1Name.orEmpty()

                        Column(modifier = Modifier.fillMaxSize()) {
                            // Top Bar Header in gameplay
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = {
                                    if (playMode == MatchPlayMode.PVP_ONLINE) {
                                        viewModel.leaveRoom(currentUserUid)
                                    }
                                    navController.popBackStack()
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Thoát")
                                }
                                
                                val seconds = (elapsedTimeMs / 1000)
                                val millis = (elapsedTimeMs % 1000) / 10
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Timer, contentDescription = null, tint = FlowPrimary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = String.format("%02d:%02d", seconds, millis),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = FlowPrimary
                                    )
                                }
                                
                                if (playMode == MatchPlayMode.PVP_ONLINE) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Wifi, contentDescription = null, tint = Color(0xFF10B981))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Phòng: ${roomState?.roomId}", style = MaterialTheme.typography.labelMedium, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Text("Solo Mode", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                                }
                            }

                            // Notification Banner if opponent finished first
                            if (playMode == MatchPlayMode.PVP_ONLINE && oppScore != null && oppScore > 0L && oppScore != 999999L) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    color = FlowWarning.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, FlowWarning)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = FlowWarning)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Đối thủ ($oppName) đã hoàn thành lưới! Hãy nhanh tay lên! ⚡",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = FlowWarning
                                        )
                                    }
                                }
                            }

                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                PlayerMatchGrid(
                                    items = p1Items,
                                    selectedIndex1 = selectedFirst,
                                    selectedIndex2 = selectedSecond,
                                    onSelect = { idx ->
                                        handleCardSelection(
                                            index = idx,
                                            firstIdx = selectedFirst,
                                            secondIdx = selectedSecond,
                                            items = p1Items,
                                            onUpdateFirst = { selectedFirst = it },
                                            onUpdateSecond = { selectedSecond = it },
                                            onUpdateItems = { newItems ->
                                                viewModel.updateLocalItems(newItems)
                                                if (newItems.all { it.isMatched }) {
                                                    isTimerRunning = false
                                                    if (playMode == MatchPlayMode.SINGLE) {
                                                        winnerMessage = "Hoàn thành xuất sắc! 🎉"
                                                        gameState = MatchGameState.FINISHED
                                                    } else {
                                                        viewModel.submitCompletionTime(elapsedTimeMs, currentUserUid)
                                                    }
                                                }
                                            }
                                        )
                                    }
                                )
                            }
                        }

                        // Local player finished overlay waiting screen (Online mode)
                        if (playMode == MatchPlayMode.PVP_ONLINE && myScore != null && myScore > 0L) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    CircularProgressIndicator(color = FlowPrimary, modifier = Modifier.size(64.dp))
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text(
                                        text = "Hoàn thành lưới xuất sắc! 🎉",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = FlowPrimary,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    val seconds = (elapsedTimeMs / 1000)
                                    val millis = (elapsedTimeMs % 1000) / 10
                                    Text(
                                        text = String.format("Thời gian của bạn: %02d:%02d giây", seconds, millis),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Đang chờ đối thủ ($oppName) hoàn thành cuộc đua... ⏳",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                    MatchGameState.FINISHED -> {
                        SharedMatchResultDialog(
                            winnerMessage = winnerMessage,
                            timeMs = elapsedTimeMs,
                            playMode = playMode,
                            roomState = roomState,
                            currentUserUid = currentUserUid,
                            onPlayAgain = {
                                viewModel.cancelMatchmaking()
                                selectedFirst = null
                                selectedSecond = null
                                elapsedTimeMs = 0L
                                gameState = MatchGameState.SETUP
                            },
                            onExit = {
                                viewModel.cancelMatchmaking()
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
fun RoomWaitingScreen(
    roomId: String,
    onCancel: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(FlowPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Style,
                    contentDescription = null,
                    tint = FlowPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "MÃ PHÒNG ĐẤU 🔑",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                letterSpacing = 2.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Displays 6-digit room code with premium spacing
            val formattedCode = if (roomId.length == 6) "${roomId.take(3)} ${roomId.takeLast(3)}" else roomId
            Text(
                text = formattedCode,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                color = FlowPrimary,
                letterSpacing = 4.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.graphicsLayer(alpha = alphaAnim)
            ) {
                CircularProgressIndicator(
                    color = FlowPrimary, 
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Đang chờ đối thủ tham gia trận đấu...",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Hãy gửi mã số phòng này cho bạn bè của bạn để họ có thể nhập mã và tham gia thi đấu trực tuyến thời gian thực!",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(64.dp))
            
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .width(220.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.2.dp, FlowWarning),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = FlowWarning)
            ) {
                Text("Hủy Chờ Phòng", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun SharedMatchResultDialog(
    winnerMessage: String,
    timeMs: Long,
    playMode: MatchPlayMode,
    roomState: MatchOnlineRoom?,
    currentUserUid: String,
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

    // Dynamic 3-minute Countdown timer at result screen (Arcade style buffer)
    var countdownSeconds by remember { mutableStateOf(180) } // 180 seconds = 3 minutes
    LaunchedEffect(Unit) {
        while (countdownSeconds > 0) {
            delay(1000)
            countdownSeconds--
        }
        onExit() // Auto close room when timeout
    }

    Dialog(
        onDismissRequest = {},
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
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
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = winnerMessage,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (winnerMessage.contains("THẮNG")) Color(0xFF10B981) else Color(0xFFEF4444),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = randomWish,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                
                Spacer(modifier = Modifier.height(16.dp))

                if (playMode == MatchPlayMode.PVP_ONLINE && roomState != null) {
                    // Online score comparative dashboard
                    val isP1 = currentUserUid == roomState.player1Id
                    val myTime = if (isP1) roomState.player1Score else roomState.player2Score
                    val oppTime = if (isP1) roomState.player2Score else roomState.player1Score
                    val oppName = if (isP1) roomState.player2Name else roomState.player1Name
                    
                    val myTimeStr = if (myTime == 999999L) "Đã bỏ cuộc" else String.format("%d.%03ds", myTime / 1000, myTime % 1000)
                    val oppTimeStr = if (oppTime == 999999L) "Đã bỏ cuộc" else String.format("%d.%03ds", oppTime / 1000, oppTime % 1000)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("BẢNG SO SÁNH TỐC ĐỘ ⏱️", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = FlowPrimary)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Bạn (Thời gian):", fontWeight = FontWeight.SemiBold)
                            Text(myTimeStr, color = FlowPrimary, fontWeight = FontWeight.Bold)
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Đối thủ ($oppName):", fontWeight = FontWeight.SemiBold)
                            Text(oppTimeStr, color = Color.Gray, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                } else {
                    // Solo time display
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
                        modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                    )
                }

                Button(
                    onClick = onPlayAgain,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary)
                ) {
                    Text("Chơi Lại 🔄", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onExit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, FlowPrimary)
                ) {
                    Text("Thoát phòng / Rời đi", color = FlowPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Premium countdown notice
                Text(
                    text = "Tự động đóng phòng sau $countdownSeconds giây...",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
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
    onStartSolo: () -> Unit,
    onCreateRoom: () -> Unit,
    onJoinRoom: (String) -> Unit,
    onCancel: () -> Unit
) {
    var roomCodeState by remember { mutableStateOf(TextFieldValue("")) }

    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
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
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    )
                }

                // Grid Size Options
                item {
                    Text(
                        text = "Kích Thước Lưới:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
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
                }

                // Game Mode Options
                item {
                    Text(
                        text = "Chế Độ Chơi:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
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
                                Text("1 Người chơi", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }

                        // Option 2: PVP Online
                        val isPvpSelected = playMode == MatchPlayMode.PVP_ONLINE
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(72.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onPlayModeChange(MatchPlayMode.PVP_ONLINE) },
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
                                Text("Phòng Mã Code", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }
                    }
                }

                // Dynamic Action Interface depending on playMode
                item {
                    if (playMode == MatchPlayMode.SINGLE) {
                        Button(
                            onClick = onStartSolo,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .padding(top = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary)
                        ) {
                            Text("Bắt Đầu Chơi 🚀", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Part A: Create Room Code
                            Button(
                                onClick = onCreateRoom,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary)
                            ) {
                                Icon(Icons.Default.Key, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Tạo Phòng Mới 🔑", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                            // Part B: Join Room Code
                            Column(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = roomCodeState,
                                    onValueChange = { roomCodeState = it },
                                    label = { Text("Nhập Mã Phòng (6 Số)") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = FlowPrimary,
                                        focusedLabelColor = FlowPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        if (roomCodeState.text.length == 6) {
                                            onJoinRoom(roomCodeState.text.trim())
                                        }
                                    },
                                    enabled = roomCodeState.text.length == 6,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (roomCodeState.text.length == 6) Color(0xFF10B981) else Color.Gray,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.Default.Login, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Tham Gia Phòng 🚪", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.2.dp, FlowWarning),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FlowWarning)
                    ) {
                        Text("Hủy Bỏ", fontWeight = FontWeight.Bold)
                    }
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
