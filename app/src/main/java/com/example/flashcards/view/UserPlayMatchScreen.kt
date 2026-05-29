package com.example.flashcards.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.flashcards.ui.theme.FlowPrimary
import com.example.flashcards.utils.CryptoUtils
import com.example.flashcards.viewmodel.MatchOnlineViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers

enum class MatchGameState { SETUP, PLAYING, FINISHED, ONLINE_WAITING, ONLINE_JOINING }
enum class MatchPlayMode { SINGLE, PVP_LOCAL, ONLINE }

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
fun UserPlayMatchScreen(
    navController: NavController,
    levelId: String,
    onlineViewModel: MatchOnlineViewModel = viewModel()
) {
    val firestore = remember { FirebaseFirestore.getInstance() }
    var rawVocabList by remember { mutableStateOf<List<VocabCard>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"

    var gameState by remember { mutableStateOf(MatchGameState.SETUP) }
    var playMode by remember { mutableStateOf(MatchPlayMode.SINGLE) }
    var gridSize by remember { mutableIntStateOf(5) }

    // Local PVP / Single State
    var p1Items by remember { mutableStateOf<List<MatchCardItem>>(emptyList()) }
    var p1SelectedFirst by remember { mutableStateOf<Int?>(null) }
    var p1SelectedSecond by remember { mutableStateOf<Int?>(null) }
    var p2Items by remember { mutableStateOf<List<MatchCardItem>>(emptyList()) }
    var p2SelectedFirst by remember { mutableStateOf<Int?>(null) }
    var p2SelectedSecond by remember { mutableStateOf<Int?>(null) }
    
    // Online State
    val roomState by onlineViewModel.roomState.collectAsState()
    val onlineGridItems by onlineViewModel.gridItems.collectAsState()
    val errorMessage by onlineViewModel.errorMessage.collectAsState()
    
    var onlineJoinCode by remember { mutableStateOf("") }
    var onlineSelectedFirst by remember { mutableStateOf<Int?>(null) }
    var onlineSelectedSecond by remember { mutableStateOf<Int?>(null) }

    var elapsedTimeMs by remember { mutableLongStateOf(0L) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var winnerMessage by remember { mutableStateOf("") }

    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            val startTime = System.currentTimeMillis() - elapsedTimeMs
            while (isTimerRunning) {
                delay(10)
                elapsedTimeMs = System.currentTimeMillis() - startTime
            }
        }
    }

    LaunchedEffect(roomState?.status) {
        if (roomState?.status == "PLAYING" && (gameState == MatchGameState.ONLINE_WAITING || gameState == MatchGameState.ONLINE_JOINING)) {
            gameState = MatchGameState.PLAYING
            elapsedTimeMs = 0L
            isTimerRunning = true
        } else if (roomState?.status == "FINISHED" && gameState == MatchGameState.PLAYING) {
            isTimerRunning = false
            winnerMessage = if (roomState?.winnerId == currentUserUid) "Bạn đã THẮNG! 🎉" else if (roomState?.winnerId == "DRAW") "HOÀ!" else "Bạn đã THUA! 😭"
            gameState = MatchGameState.FINISHED
        }
    }

    val displayLevelName = when {
        levelId.startsWith("deck_") -> "Bộ thẻ tùy chỉnh"
        levelId == "QUIZ_JA_N5" -> "Cấp độ N5"
        levelId == "QUIZ_JA_N4" -> "Cấp độ N4"
        levelId == "QUIZ_JA_N3" -> "Cấp độ N3"
        levelId == "QUIZ_JA_N2" -> "Cấp độ N2"
        levelId == "QUIZ_JA_N1" -> "Cấp độ N1"
        levelId == "QUIZ_TOEIC_450" -> "TOEIC 450+"
        levelId == "QUIZ_TOEIC_650" -> "TOEIC 650+"
        levelId == "QUIZ_TOEIC_800" -> "TOEIC 800+"
        levelId == "QUIZ_IELTS_55" -> "IELTS Band 5.5"
        levelId == "QUIZ_IELTS_65" -> "IELTS Band 6.5"
        levelId == "QUIZ_IELTS_75" -> "IELTS Band 7.5+"
        levelId == "QUIZ_ZH_BASIC" -> "Trung Cơ Bản"
        levelId == "QUIZ_PA_INTRO" -> "Pali Sơ Cấp"
        else -> levelId.replace("QUIZ_", "").replace("_", " ")
    }

    LaunchedEffect(levelId) {
        if (levelId.startsWith("deck_")) {
            val deckId = levelId.replace("deck_", "")
            firestore.collection("users").document(currentUserUid).collection("studySets").document(deckId).get()
                .addOnSuccessListener { doc ->
                    val cards = doc.get("cards") as? List<Map<String, Any>> ?: emptyList()
                    val list = mutableListOf<VocabCard>()
                    cards.forEach { cardMap ->
                        val id = cardMap["id"] as? String ?: return@forEach
                        val front = cardMap["question"] as? String ?: return@forEach
                        val back = cardMap["answer"] as? String ?: return@forEach
                        list.add(VocabCard(id, front, back, levelId))
                    }
                    rawVocabList = list
                    isLoading = false
                }
                .addOnFailureListener { isLoading = false }
        } else {
            firestore.collection("system_vocabulary").whereEqualTo("levelId", levelId).get()
                .addOnSuccessListener { snapshot ->
                    val list = mutableListOf<VocabCard>()
                    snapshot.documents.forEach { d ->
                        val id = d.id
                        val rawFront = d.getString("front") ?: ""
                        val rawBack = d.getString("back") ?: ""
                        try {
                            val front = CryptoUtils.decrypt(rawFront)
                            val back = CryptoUtils.decrypt(rawBack)
                            if (front.isNotEmpty() && back.isNotEmpty()) list.add(VocabCard(id, front, back, levelId))
                        } catch (e: Exception) {}
                    }
                    rawVocabList = list
                    isLoading = false
                }
                .addOnFailureListener { isLoading = false }
        }
    }

    Scaffold(
        topBar = {
            if (playMode == MatchPlayMode.SINGLE || gameState == MatchGameState.SETUP || gameState == MatchGameState.ONLINE_WAITING || gameState == MatchGameState.ONLINE_JOINING) {
                TopAppBar(
                    title = { Text("Game Nối Từ: $displayLevelName", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { 
                            onlineViewModel.leaveRoom()
                            navController.popBackStack() 
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Đóng")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FlowPrimary)
                }
            } else if (rawVocabList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Cấp độ này hiện chưa có từ vựng.", color = Color.Gray)
                }
            } else {
                when (gameState) {
                    MatchGameState.SETUP -> {
                        MatchSetupView(
                            playMode = playMode,
                            gridSize = gridSize,
                            onPlayModeChange = { playMode = it },
                            onGridSizeChange = { gridSize = it },
                            onStartLocal = {
                                val p1List = generateGameItems(rawVocabList, gridSize)
                                p1Items = p1List
                                if (playMode == MatchPlayMode.PVP_LOCAL) {
                                    val p2List = generateGameItems(rawVocabList, gridSize)
                                    p2Items = p2List
                                }
                                elapsedTimeMs = 0L
                                isTimerRunning = true
                                gameState = MatchGameState.PLAYING
                            },
                            onStartOnlineHost = {
                                onlineViewModel.createRoom(currentUserUid, levelId, gridSize, rawVocabList)
                                gameState = MatchGameState.ONLINE_WAITING
                            },
                            onJoinOnline = {
                                gameState = MatchGameState.ONLINE_JOINING
                            },
                            onJoinRandomOnline = {
                                onlineViewModel.joinRandomRoom(currentUserUid, levelId) {
                                    // No room found, create one
                                    onlineViewModel.createRoom(currentUserUid, levelId, gridSize, rawVocabList)
                                }
                                gameState = MatchGameState.ONLINE_WAITING
                            }
                        )
                    }
                    MatchGameState.ONLINE_WAITING -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Mã Phòng Của Bạn", style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = roomState?.roomId ?: "Đang tạo...",
                                        style = MaterialTheme.typography.displayMedium,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 8.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                            CircularProgressIndicator(color = FlowPrimary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Đang chờ đối thủ tham gia...", color = Color.Gray)
                            
                            Spacer(modifier = Modifier.height(64.dp))
                            OutlinedButton(onClick = {
                                onlineViewModel.leaveRoom()
                                gameState = MatchGameState.SETUP
                            }) {
                                Text("Hủy", color = Color.Red)
                            }
                        }
                    }
                    MatchGameState.ONLINE_JOINING -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Nhập Mã Phòng", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(24.dp))
                            OutlinedTextField(
                                value = onlineJoinCode,
                                onValueChange = { if (it.length <= 4) onlineJoinCode = it.uppercase() },
                                label = { Text("Mã 4 chữ số") },
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 24.sp, letterSpacing = 8.sp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (errorMessage != null) {
                                Text(errorMessage ?: "", color = Color.Red, modifier = Modifier.padding(top = 8.dp))
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(
                                onClick = { 
                                    onlineViewModel.joinRoom(onlineJoinCode, currentUserUid)
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary),
                                enabled = onlineJoinCode.length == 4
                            ) {
                                Text("Vào Phòng", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = {
                                    onlineViewModel.clearError()
                                    gameState = MatchGameState.SETUP 
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Trở lại", color = FlowPrimary)
                            }
                        }
                    }
                    MatchGameState.PLAYING -> {
                        if (playMode == MatchPlayMode.ONLINE) {
                            val isHost = roomState?.hostId == currentUserUid
                            val myScore = if (isHost) roomState?.hostScore else roomState?.guestScore
                            val opponentScore = if (isHost) roomState?.guestScore else roomState?.hostScore
                            
                            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text("Bạn", fontWeight = FontWeight.Bold, color = FlowPrimary)
                                        Text("${myScore} đ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                                    }
                                    
                                    val seconds = (elapsedTimeMs / 1000)
                                    val millis = (elapsedTimeMs % 1000) / 10
                                    Text(
                                        text = String.format("%02d:%02d", seconds, millis),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.DarkGray
                                    )
                                    
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Đối thủ", fontWeight = FontWeight.Bold, color = Color.Red)
                                        Text("${opponentScore} đ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color.Red)
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                PlayerMatchGrid(
                                    items = onlineGridItems,
                                    selectedIndex1 = onlineSelectedFirst,
                                    selectedIndex2 = onlineSelectedSecond,
                                    currentUserUid = currentUserUid,
                                    isOnline = true,
                                    onSelect = { idx ->
                                        handleCardSelection(idx, onlineSelectedFirst, onlineSelectedSecond, onlineGridItems,
                                            onUpdateFirst = { onlineSelectedFirst = it },
                                            onUpdateSecond = { onlineSelectedSecond = it },
                                            onUpdateItems = { newItems ->
                                                // Local update handled briefly, real update from viewModel snapshot
                                            }
                                        )
                                        
                                        // Trigger ViewModel match check if 2 selected
                                        if (onlineSelectedFirst != null && onlineSelectedFirst != idx) {
                                            val firstIdx = onlineSelectedFirst!!
                                            val secondIdx = idx
                                            val firstItem = onlineGridItems[firstIdx]
                                            val secondItem = onlineGridItems[secondIdx]
                                            if (firstItem.id == secondItem.id && firstItem.isFront != secondItem.isFront) {
                                                onlineViewModel.handleCardMatch(roomState!!.roomId, firstIdx, secondIdx, currentUserUid, isHost)
                                                onlineSelectedFirst = null
                                                onlineSelectedSecond = null
                                            }
                                        }
                                    }
                                )
                            }
                        } else if (playMode == MatchPlayMode.PVP_LOCAL) {
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

                                // Timer
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black)
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val seconds = (elapsedTimeMs / 1000)
                                    val millis = (elapsedTimeMs % 1000) / 10
                                    Text(
                                        text = String.format("%02d:%02d", seconds, millis),
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleLarge,
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
                            // SINGLE PLAYER
                            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                val seconds = (elapsedTimeMs / 1000)
                                val millis = (elapsedTimeMs % 1000) / 10
                                Text(
                                    text = String.format("Thời gian: %02d:%02d", seconds, millis),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = FlowPrimary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
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
                        MatchResultView(
                            winnerMessage = winnerMessage,
                            timeMs = elapsedTimeMs,
                            isOnline = playMode == MatchPlayMode.ONLINE,
                            onPlayAgain = {
                                onlineViewModel.leaveRoom()
                                gameState = MatchGameState.SETUP
                            },
                            onExit = {
                                onlineViewModel.leaveRoom()
                                navController.popBackStack()
                            }
                        )
                    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchSetupView(
    playMode: MatchPlayMode,
    gridSize: Int,
    onPlayModeChange: (MatchPlayMode) -> Unit,
    onGridSizeChange: (Int) -> Unit,
    onStartLocal: () -> Unit,
    onStartOnlineHost: () -> Unit,
    onJoinOnline: () -> Unit,
    onJoinRandomOnline: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Thiết Lập Trò Chơi", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = FlowPrimary)
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Chế Độ Chơi:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FilterChip(
                selected = playMode == MatchPlayMode.SINGLE,
                onClick = { onPlayModeChange(MatchPlayMode.SINGLE) },
                label = { Text("1 Người") }
            )
            FilterChip(
                selected = playMode == MatchPlayMode.PVP_LOCAL,
                onClick = { onPlayModeChange(MatchPlayMode.PVP_LOCAL) },
                label = { Text("Chung máy ⚔️") }
            )
            FilterChip(
                selected = playMode == MatchPlayMode.ONLINE,
                onClick = { onPlayModeChange(MatchPlayMode.ONLINE) },
                label = { Text("Online 🌐") }
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("Kích Thước Lưới:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf(5, 7, 10).forEach { size ->
                FilterChip(
                    selected = gridSize == size,
                    onClick = { onGridSizeChange(size) },
                    label = { Text("${size} cặp") }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        if (playMode == MatchPlayMode.ONLINE) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = onJoinRandomOnline,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                ) {
                    Text("Tìm Trận Ngẫu Nhiên", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = onStartOnlineHost,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary)
                    ) {
                        Text("Tạo Phòng", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    OutlinedButton(
                        onClick = onJoinOnline,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, FlowPrimary)
                    ) {
                        Text("Vào Phòng", color = FlowPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        } else {
            Button(
                onClick = onStartLocal,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary)
            ) {
                Text("Bắt Đầu", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun PlayerMatchGrid(
    items: List<MatchCardItem>,
    selectedIndex1: Int?,
    selectedIndex2: Int?,
    currentUserUid: String = "",
    isOnline: Boolean = false,
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
                item.isMatched && isOnline && item.matchedBy == currentUserUid -> Color(0xFFD1FAE5) // My match (Green)
                item.isMatched && isOnline && item.matchedBy != currentUserUid -> Color(0xFFFEE2E2) // Opponent match (Red)
                item.isMatched && !isOnline -> Color(0xFFD1FAE5)
                item.isError && isSelected -> Color(0xFFFEE2E2)
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
            val cardBorderColor = when {
                item.isMatched && isOnline && item.matchedBy == currentUserUid -> Color(0xFF10B981)
                item.isMatched && isOnline && item.matchedBy != currentUserUid -> Color(0xFFEF4444)
                item.isMatched && !isOnline -> Color(0xFF10B981)
                item.isError && isSelected -> Color(0xFFEF4444)
                isSelected -> FlowPrimary
                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
            }
            val textColor = when {
                item.isMatched && isOnline && item.matchedBy == currentUserUid -> Color(0xFF065F46)
                item.isMatched && isOnline && item.matchedBy != currentUserUid -> Color(0xFFB91C1C)
                item.isMatched && !isOnline -> Color(0xFF065F46)
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

@Composable
fun MatchResultView(
    winnerMessage: String,
    timeMs: Long,
    isOnline: Boolean,
    onPlayAgain: () -> Unit,
    onExit: () -> Unit
) {
    val seconds = (timeMs / 1000)
    val millis = (timeMs % 1000) / 10
    val timeStr = String.format("%02d.%02ds", seconds, millis)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.EmojiEvents,
            contentDescription = null,
            tint = Color(0xFFFBBF24),
            modifier = Modifier.size(120.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = winnerMessage,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF10B981),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Thời gian: ${timeStr}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = FlowPrimary
        )
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onPlayAgain,
            modifier = Modifier.width(200.dp).height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary)
        ) {
            Text(if(isOnline) "Thiết lập lại" else "Chơi lại 🔄", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = onExit,
            modifier = Modifier.width(200.dp).height(56.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, FlowPrimary)
        ) {
            Text("Thoát", color = FlowPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
