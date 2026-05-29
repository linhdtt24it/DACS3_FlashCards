package com.example.flashcards.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSelectionBottomSheet(
    hasBattleFeature: Boolean,
    onDismiss: () -> Unit,
    onPlayMatch: () -> Unit,
    onPlayBattle: () -> Unit = {},
    onJoinBattle: (String) -> Unit = {},
    onJoinRandomBattle: () -> Unit = {}
) {
    var showBattleDialog by remember { mutableStateOf(false) }
    var joinCode by remember { mutableStateOf("") }

    if (showBattleDialog) {
        AlertDialog(
            onDismissRequest = { showBattleDialog = false },
            title = { Text("Trận Đấu 1vs1", fontWeight = FontWeight.Bold, color = Color(0xFFEAB308)) },
            text = {
                Column {
                    Text("Tạo phòng mới để mời bạn bè, tìm trận ngẫu nhiên, hoặc nhập mã để vào phòng đã có.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            showBattleDialog = false
                            onPlayBattle()
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAB308))
                    ) {
                        Text("Tạo Phòng Mới", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            showBattleDialog = false
                            onJoinRandomBattle()
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                    ) {
                        Text("Tìm Trận Ngẫu Nhiên", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Hoặc nhập mã phòng:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = joinCode,
                        onValueChange = { if (it.length <= 6) joinCode = it.uppercase() },
                        label = { Text("Mã PIN (6 ký tự)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (joinCode.length == 6) {
                            showBattleDialog = false
                            onJoinBattle(joinCode)
                        }
                    },
                    enabled = joinCode.length == 6,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAB308))
                ) {
                    Text("Vào Phòng")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBattleDialog = false }) {
                    Text("Hủy", color = Color.Gray)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, top = 8.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Giải Trí (Minigames)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Chọn chế độ trò chơi bạn muốn tham gia",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            GameOptionItem(
                title = "Nối Từ (Match)",
                description = "Trò chơi ghép thẻ luyện nhớ",
                icon = Icons.Default.Extension,
                iconTint = Color(0xFF8B5CF6),
                onClick = onPlayMatch
            )
            
            if (hasBattleFeature) {
                Spacer(modifier = Modifier.height(16.dp))
                GameOptionItem(
                    title = "Trận Đấu 1vs1",
                    description = "Thi đấu trắc nghiệm đối kháng",
                    icon = Icons.Default.Group,
                    iconTint = Color(0xFFEAB308),
                    onClick = { showBattleDialog = true }
                )
            }
        }
    }
}

@Composable
fun GameOptionItem(
    title: String,
    description: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconTint.copy(alpha = 0.15f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
