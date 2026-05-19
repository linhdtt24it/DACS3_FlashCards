package com.example.flashcards.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.flashcards.model.BattlePlayer
import com.example.flashcards.model.BattleRoom
import com.example.flashcards.ui.theme.FlowPrimary
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BattleScreen(
    battleRoom: BattleRoom?,
    onStartBattle: () -> Unit,
    onAnswerSelected: (Boolean) -> Unit,
    onBack: () -> Unit,
    onLeaveRoom: () -> Unit,
    onRematch: () -> Unit,
    onBattleFinished: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var showExitDialog by remember { mutableStateOf(false) }

    // Xử lý khi battleRoom = null
    if (battleRoom == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Đang tải phòng đấu...")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBack) {
                    Text("Quay lại")
                }
            }
        }
        return
    }

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Nếu chưa đăng nhập thì quay về
    if (currentUserId.isEmpty()) {
        LaunchedEffect(Unit) {
            onBack()
        }
        return
    }

    val currentPlayer = battleRoom.players[currentUserId]
    val opponentPlayer = battleRoom.players.values.find { it.uid != currentUserId }
    val isHost = battleRoom.players.keys.firstOrNull() == currentUserId

    // Xử lý khi trận đấu kết thúc
    LaunchedEffect(battleRoom.status) {
        if (battleRoom.status == "FINISHED") {
            delay(3000)
            onBattleFinished?.invoke()
        }
    }

    fun handleBackPressed() {
        if (battleRoom.status == "WAITING") {
            showExitDialog = true
        } else {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Battle 1vs1", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { handleBackPressed() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    if (battleRoom.status == "WAITING") {
                        IconButton(onClick = { showExitDialog = true }) {
                            Icon(Icons.Default.Logout, contentDescription = "Rời phòng")
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BattleHeader(currentPlayer, opponentPlayer, battleRoom.status)

            Spacer(modifier = Modifier.height(24.dp))

            when (battleRoom.status) {
                "WAITING" -> WaitingLobby(
                    battleRoom = battleRoom,
                    isHost = isHost,
                    onStart = onStartBattle,
                    onLeave = { showExitDialog = true }
                )
                "STARTED" -> {
                    if (currentPlayer != null) {
                        BattleQuiz(
                            battleRoom = battleRoom,
                            currentPlayer = currentPlayer,
                            onAnswerSelected = onAnswerSelected
                        )
                    } else {
                        Text("Không tìm thấy thông tin người chơi")
                    }
                }
                "FINISHED" -> BattleResults(
                    battleRoom = battleRoom,
                    currentUserId = currentUserId,
                    onBack = onBack,
                    onRematch = onRematch
                )
            }
        }
    }

    // Dialog xác nhận rời phòng
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF9800))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Rời phòng?")
                }
            },
            text = {
                Text(
                    "Nếu bạn rời khỏi phòng này, phòng sẽ bị xóa ngay lập tức.",
                    color = Color.Gray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        onLeaveRoom()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                ) {
                    Text("Rời phòng")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Ở lại")
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
fun WaitingLobby(
    battleRoom: BattleRoom,
    isHost: Boolean,
    onStart: () -> Unit,
    onLeave: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Group, null, modifier = Modifier.size(80.dp), tint = FlowPrimary.copy(alpha = 0.5f))
        Text("Sảnh chờ đấu 1vs1", style = MaterialTheme.typography.titleMedium)

        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("MÃ PIN PHÒNG", style = MaterialTheme.typography.labelSmall)
                Text(
                    battleRoom.id.takeLast(6).uppercase(),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = FlowPrimary
                )
            }
        }

        Text("Gửi mã này cho đối thủ để tham gia", style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(32.dp))

        if (isHost) {
            Button(
                onClick = onStart,
                enabled = battleRoom.players.size >= 2,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("BẮT ĐẦU TRẬN ĐẤU")
            }
        } else {
            Text("Đang chờ chủ phòng bắt đầu...", color = Color.Gray)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onLeave,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF44336))
        ) {
            Icon(Icons.Default.ExitToApp, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Rời phòng")
        }
    }
}

@Composable
fun BattleQuiz(
    battleRoom: BattleRoom,
    currentPlayer: BattlePlayer,
    onAnswerSelected: (Boolean) -> Unit
) {
    val progress = currentPlayer.progress
    if (progress >= battleRoom.questions.size) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Đã xong! Chờ đối thủ...", fontWeight = FontWeight.Bold)
        }
        return
    }

    val question = battleRoom.questions.getOrNull(progress) ?: return

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Câu ${progress + 1}/${battleRoom.questions.size}", color = FlowPrimary, fontWeight = FontWeight.Bold)
        LinearProgressIndicator(
            progress = (progress + 1).toFloat() / battleRoom.questions.size,
            modifier = Modifier.fillMaxWidth().clip(CircleShape).height(10.dp),
            color = FlowPrimary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Text(
                question.question,
                modifier = Modifier.padding(24.dp),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        question.options.forEach { option ->
            OutlinedButton(
                onClick = { onAnswerSelected(option == question.correctAnswer) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(option)
            }
        }
    }
}

@Composable
fun BattleResults(
    battleRoom: BattleRoom,
    currentUserId: String,
    onBack: () -> Unit,
    onRematch: () -> Unit
) {
    val me = battleRoom.players[currentUserId]
    val opponent = battleRoom.players.values.find { it.uid != currentUserId }
    val isWin = (me?.score ?: 0) > (opponent?.score ?: 0)
    val isDraw = (me?.score ?: 0) == (opponent?.score ?: 0)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            when {
                isWin -> "CHIẾN THẮNG! 🎉"
                isDraw -> "HÒA! 🤝"
                else -> "CỐ GẮNG LẦN SAU! ✌️"
            },
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            color = when {
                isWin -> Color(0xFF4CAF50)
                isDraw -> Color(0xFFFF9800)
                else -> Color(0xFFF44336)
            }
        )
        Spacer(modifier = Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            ResultItem("Bạn", me?.score ?: 0)
            ResultItem("Đối thủ", opponent?.score ?: 0)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onRematch,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("THI LẠI")
            }

            Button(
                onClick = onBack,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
            ) {
                Icon(Icons.Default.ExitToApp, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("THOÁT")
            }
        }
    }
}

@Composable
fun BattleHeader(me: BattlePlayer?, opponent: BattlePlayer?, status: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlayerInfo(me, isMe = true)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("VS", fontSize = 24.sp, fontWeight = FontWeight.Black, color = FlowPrimary)
            if (status == "STARTED") {
                Text("LIVE", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            }
        }
        PlayerInfo(opponent, isMe = false)
    }
}

@Composable
fun PlayerInfo(player: BattlePlayer?, isMe: Boolean) {
    Column(horizontalAlignment = if (isMe) Alignment.Start else Alignment.End) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isMe) FlowPrimary else Color.Gray),
            contentAlignment = Alignment.Center
        ) {
            Text(player?.name?.take(1)?.uppercase() ?: "?", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Text(if (isMe) "Bạn" else player?.name ?: "Đang chờ...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Điểm: ${player?.score ?: 0}", color = FlowPrimary, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ResultItem(label: String, score: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(score.toString(), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
        Text("điểm")
    }
}