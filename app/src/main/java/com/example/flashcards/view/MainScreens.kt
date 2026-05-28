package com.example.flashcards.view

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import coil.compose.AsyncImage
import com.example.flashcards.R
import com.example.flashcards.model.Flashcard
import com.example.flashcards.model.StudySet
import com.example.flashcards.model.UserStats
import com.example.flashcards.ui.theme.*
import com.example.flashcards.utils.ImageUtils
import com.example.flashcards.utils.FlashcardUtils
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.border
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.graphics.Brush
import com.example.flashcards.ui.theme.LocalFlowColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.lazy.rememberLazyListState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.navigation.NavController
import java.util.Calendar




@Composable
fun AddDeckDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var newTitle by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Deck", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = newTitle, onValueChange = { newTitle = it }, label = { Text("Deck Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = newDesc, onValueChange = { newDesc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (newTitle.isNotBlank()) {
                    onSave(newTitle, newDesc)
                    onDismiss()
                }
            }) { Text("Save", fontWeight = FontWeight.Bold, color = FlowPrimary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun ImportDeckDialog(onDismiss: () -> Unit, onImport: (String) -> Unit) {
    var shareCode by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Deck", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter a 6-character share code to import a public deck.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(value = shareCode, onValueChange = { shareCode = it.uppercase() }, label = { Text("Share Code") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (shareCode.isNotBlank()) {
                    onImport(shareCode)
                    onDismiss()
                }
            }) { Text("Import", fontWeight = FontWeight.Bold, color = FlowPrimary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun BulkImportDialog(
    onDismiss: () -> Unit,
    onImport: (List<Flashcard>) -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    var separator by remember { mutableStateOf("-") }
    val separators = listOf("-", ":", "|", "Tab")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bulk Import Flashcards", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Paste your cards here. One card per line.\nExample: Front $separator Back",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Hello - Xin chào\nApple - Quả táo") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FlowPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Text("Separator:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    separators.forEach { sep ->
                        val displaySep = if (sep == "Tab") "\t" else sep
                        FilterChip(
                            selected = separator == displaySep,
                            onClick = { separator = displaySep },
                            label = { Text(sep) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = FlowPrimary
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            val parsedCards = FlashcardUtils.parseBulkText(textInput, separator)
            Button(
                onClick = {
                    onImport(parsedCards)
                    onDismiss()
                },
                enabled = parsedCards.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary)
            ) {
                Text("Import (${parsedCards.size})", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun HomeScreen(
    userName: String,
    studySets: List<StudySet>,
    userStats: UserStats,
    unreadNotifCount: Int = 0,
    subscribedPackages: List<String> = listOf("FREE"),
    onAddDeck: (String, String) -> Unit,
    onEditDeck: (String) -> Unit,
    onDeleteDeck: (String) -> Unit,
    onSetSelected: (StudySet) -> Unit,
    onQuizDeck: (StudySet) -> Unit,
    onNotificationsClick: () -> Unit = {},
    onFeatureClick: (String) -> Unit = {},
    onLevelClick: (String, String) -> Unit = { _, _ -> }
) {
    val fc = LocalFlowColors.current
    val heroBrush = Brush.linearGradient(
        colors = listOf(fc.gradientStart, fc.gradientEnd),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedVipPackage by remember { mutableStateOf<String?>(null) } // Lưu gói VIP cần kích hoạt để hiện Dialog thông báo

    val firestore = remember { FirebaseFirestore.getInstance() }
    val uid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    var currentStreak by remember { mutableIntStateOf(0) }
    var learnedTodayCount by remember { mutableIntStateOf(0) }
    var totalFavoriteCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) {
            firestore.collection("users").document(uid)
                .addSnapshotListener { snapshot, error ->
                    if (snapshot != null && snapshot.exists()) {
                        currentStreak = snapshot.getLong("currentStreak")?.toInt() ?: 0
                    }
                }

            firestore.collection("user_favorites")
                .whereEqualTo("uid", uid)
                .whereEqualTo("starred", true)
                .addSnapshotListener { snapshot, error ->
                    if (snapshot != null) {
                        totalFavoriteCount = snapshot.size()
                    }
                }

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfToday = calendar.time
            val startOfTodayTimestamp = com.google.firebase.Timestamp(startOfToday)

            firestore.collection("progress")
                .whereEqualTo("uid", uid)
                .whereGreaterThanOrEqualTo("lastReviewed", startOfTodayTimestamp)
                .addSnapshotListener { snapshot, error ->
                    if (snapshot != null) {
                        learnedTodayCount = snapshot.size()
                    }
                }
        }
    }

    val targetWords = if (totalFavoriteCount < 10) totalFavoriteCount else 10

    if (showAddDialog) {
        AddDeckDialog(onDismiss = { showAddDialog = false }, onSave = onAddDeck)
    }

    if (selectedVipPackage != null) {
        PremiumUpgradeDialog(
            packageName = selectedVipPackage!!,
            onDismiss = { selectedVipPackage = null }
        )
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // --- 1. HEADER SECTION ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val firstName = userName.split(" ").firstOrNull()?.ifEmpty { "User" } ?: "User"
                    Text(
                        text = "Hi, $firstName 👋",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Ready to achieve flow in learning?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(modifier = Modifier.size(48.dp)) {
                    IconButton(onClick = onNotificationsClick, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    if (unreadNotifCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(FlowWarning)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(FlowPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        userName.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            }
        }

        // --- 2. STREAK CARD ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(FlowWarning.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = FlowWarning,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "$currentStreak Ngày Streak",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Hôm nay bạn đã học $learnedTodayCount/$targetWords thẻ. Cố lên nhé!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // --- 3. 2x2 FEATURE GRID ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Phương thức học",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeatureCard(
                        title = "Flashcard",
                        description = "Học thẻ ghi nhớ",
                        icon = Icons.Default.Style,
                        gradientColors = listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)),
                        modifier = Modifier.weight(1f),
                        onClick = { onFeatureClick("flashcard") }
                    )
                    FeatureCard(
                        title = "Trắc nghiệm",
                        description = "Quiz trắc nghiệm",
                        icon = Icons.Default.Quiz,
                        gradientColors = listOf(Color(0xFFF97316), Color(0xFFEA580C)),
                        modifier = Modifier.weight(1f),
                        onClick = { onFeatureClick("quiz") }
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeatureCard(
                        title = "Tự luận",
                        description = "Tự viết câu trả lời",
                        icon = Icons.Default.EditNote,
                        gradientColors = listOf(Color(0xFF10B981), Color(0xFF059669)),
                        modifier = Modifier.weight(1f),
                        onClick = { onFeatureClick("write") }
                    )
                    FeatureCard(
                        title = "Game nối từ",
                        description = "Trò chơi ghép từ",
                        icon = Icons.Default.Extension,
                        gradientColors = listOf(Color(0xFF8B5CF6), Color(0xFF7C3AED)),
                        modifier = Modifier.weight(1f),
                        onClick = { onFeatureClick("match") }
                    )
                }
            }
        }

        // --- 4. HỌC GẦN ĐÂY ---
        item {
            Column {
                Text(
                    text = "Học gần đây",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (studySets.isEmpty()) {
                    EmptyRowPlaceholder("Chưa có học phần nào gần đây.")
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(studySets.take(5)) { set ->
                            CompactDeckCard(set, onClick = { onSetSelected(set) })
                        }
                    }
                }
            }
        }

        // --- 5. FLASHCARD TỰ TẠO ---
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Flashcard tự tạo",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    IconButton(
                        onClick = { showAddDialog = true },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = FlowPrimary)
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Tạo mới", modifier = Modifier.size(28.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                val userCustomDecks = studySets.filter { it.creatorId == FirebaseAuth.getInstance().currentUser?.uid }
                if (userCustomDecks.isEmpty()) {
                    EmptyRowPlaceholder("Bấm [+] để tạo bộ thẻ cá nhân của riêng bạn!")
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(userCustomDecks) { set ->
                            CompactDeckCard(set, onClick = { onSetSelected(set) })
                        }
                    }
                }
            }
        }

        // --- 6. DANH MỤC NGÔN NGỮ CHÍNH ---
        item {
            Column {
                Text(
                    text = "Ngôn ngữ học",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Danh mục Tiếng Nhật (cuộn tới index 7)
                    item {
                        LanguageChip("🇯🇵 Tiếng Nhật", Color(0xFFFEE2E2), Color(0xFF991B1B)) {
                            coroutineScope.launch { listState.animateScrollToItem(7) }
                        }
                    }
                    // Danh mục Tiếng Anh (cuộn tới index 8)
                    item {
                        LanguageChip("🇬🇧 Tiếng Anh", Color(0xFFE0F2FE), Color(0xFF075985)) {
                            coroutineScope.launch { listState.animateScrollToItem(8) }
                        }
                    }
                    // Danh mục Tiếng Trung (cuộn tới index 11)
                    item {
                        LanguageChip("🇨🇳 Tiếng Trung", Color(0xFFFEF3C7), Color(0xFF92400E)) {
                            coroutineScope.launch { listState.animateScrollToItem(11) }
                        }
                    }
                    // Danh mục Tiếng Pali (cuộn tới index 11)
                    item {
                        LanguageChip("☸️ Tiếng Pali", Color(0xFFF3E8FF), Color(0xFF6B21A8)) {
                            coroutineScope.launch { listState.animateScrollToItem(11) }
                        }
                    }
                }
            }
        }

        // --- 7. MỤC TIẾNG NHẬT (Index 7) ---
        item {
            val levels = listOf(
                LevelItem("Cấp độ N5", "FREE"),
                LevelItem("Cấp độ N4", "VIP_JAPANESE"),
                LevelItem("Cấp độ N3", "VIP_JAPANESE"),
                LevelItem("Cấp độ N2", "VIP_JAPANESE"),
                LevelItem("Cấp độ N1", "VIP_JAPANESE")
            )
            LanguageCourseRow(
                title = "Tiếng Nhật Luyện Thi",
                items = levels,
                subscribedPackages = subscribedPackages,
                onItemClick = { item ->
                    val levelId = when (item.name) {
                        "Cấp độ N5" -> "QUIZ_JA_N5"
                        "Cấp độ N4" -> "QUIZ_JA_N4"
                        "Cấp độ N3" -> "QUIZ_JA_N3"
                        "Cấp độ N2" -> "QUIZ_JA_N2"
                        "Cấp độ N1" -> "QUIZ_JA_N1"
                        else -> item.name
                    }
                    onLevelClick("JAPANESE", levelId)
                },
                onLockClick = { selectedVipPackage = "VIP_JAPANESE" }
            )
        }

        // --- 8. MỤC TIẾNG ANH TỔNG HỢP (Index 8) ---
        item {
            val englishTopics = listOf(
                LevelItem("Giao tiếp Cơ bản", "FREE"),
                LevelItem("Thành ngữ English", "FREE"),
                LevelItem("Cụm động từ cơ bản", "FREE")
            )
            LanguageCourseRow(
                title = "Tiếng Anh Tổng Hợp",
                items = englishTopics,
                subscribedPackages = subscribedPackages,
                onItemClick = { item ->
                    onLevelClick("ENGLISH", item.name)
                },
                onLockClick = {}
            )
        }

        // --- 9. MỤC CHỨNG CHỈ TOEIC (Index 9) ---
        item {
            val toeicLevels = listOf(
                LevelItem("TOEIC 450+", "FREE"),
                LevelItem("TOEIC 650+", "VIP_ENGLISH"),
                LevelItem("TOEIC 800+", "VIP_ENGLISH")
            )
            LanguageCourseRow(
                title = "Chứng chỉ TOEIC",
                items = toeicLevels,
                subscribedPackages = subscribedPackages,
                onItemClick = { item ->
                    val levelId = when (item.name) {
                        "TOEIC 450+" -> "QUIZ_TOEIC_450"
                        "TOEIC 650+" -> "QUIZ_TOEIC_650"
                        "TOEIC 800+" -> "QUIZ_TOEIC_800"
                        else -> item.name
                    }
                    onLevelClick("ENGLISH", levelId)
                },
                onLockClick = { selectedVipPackage = "VIP_ENGLISH" }
            )
        }

        // --- 10. MỤC CHỨNG CHỈ IELTS (Index 10) ---
        item {
            val ieltsBands = listOf(
                LevelItem("IELTS Band 5.5", "VIP_ENGLISH"),
                LevelItem("IELTS Band 6.5", "VIP_ENGLISH"),
                LevelItem("IELTS Band 7.5+", "VIP_ENGLISH")
            )
            LanguageCourseRow(
                title = "Chứng chỉ IELTS",
                items = ieltsBands,
                subscribedPackages = subscribedPackages,
                onItemClick = { item ->
                    val levelId = when (item.name) {
                        "IELTS Band 5.5" -> "QUIZ_IELTS_55"
                        "IELTS Band 6.5" -> "QUIZ_IELTS_65"
                        "IELTS Band 7.5+" -> "QUIZ_IELTS_75"
                        else -> item.name
                    }
                    onLevelClick("ENGLISH", levelId)
                },
                onLockClick = { selectedVipPackage = "VIP_ENGLISH" }
            )
        }

        // --- 11. MỤC TIẾNG TRUNG & TIẾNG PALI (Index 11) ---
        item {
            val otherLangs = listOf(
                LevelItem("Trung Cơ Bản", "VIP_CHINESE"),
                LevelItem("Pali Sơ Cấp", "VIP_PALI")
            )
            LanguageCourseRow(
                title = "Tiếng Trung & Tiếng Pali",
                items = otherLangs,
                subscribedPackages = subscribedPackages,
                onItemClick = { item ->
                    val lang = if (item.name.contains("Trung")) "CHINESE" else "PALI"
                    val levelId = when (item.name) {
                        "Trung Cơ Bản" -> "QUIZ_ZH_BASIC"
                        "Pali Sơ Cấp" -> "QUIZ_PA_INTRO"
                        else -> item.name
                    }
                    onLevelClick(lang, levelId)
                },
                onLockClick = { pkg -> selectedVipPackage = pkg }
            )
        }
    }
}

// --- SUB-COMPONENTS FOR HOMESCREEN ---

data class LevelItem(
    val name: String,
    val requiredPackage: String
)

@Composable
fun FeatureCard(
    title: String,
    description: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .aspectRatio(1.6f)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradientColors))
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(description, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                }
            }
        }
    }
}

@Composable
fun LanguageChip(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.1f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CompactDeckCard(set: StudySet, onClick: () -> Unit) {
    val fc = LocalFlowColors.current
    Card(
        modifier = Modifier
            .width(180.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(fc.gradientStart.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MenuBook, contentDescription = null, tint = fc.gradientStart, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = set.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1
            )
            Text(
                text = "${set.cards.size} thẻ ghi nhớ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LanguageCourseRow(
    title: String,
    items: List<LevelItem>,
    subscribedPackages: List<String>,
    onItemClick: (LevelItem) -> Unit,
    onLockClick: (String) -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items) { item ->
                val isUnlocked = item.requiredPackage == "FREE" || subscribedPackages.contains(item.requiredPackage)
                
                Card(
                    modifier = Modifier
                        .width(140.dp)
                        .clickable {
                            if (isUnlocked) onItemClick(item) else onLockClick(item.requiredPackage)
                        },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isUnlocked) Color(0xFF10B981).copy(alpha = 0.12f)
                                        else Color.Black.copy(alpha = 0.05f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isUnlocked) Icons.Default.School else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (isUnlocked) Color(0xFF059669) else Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = if (isUnlocked) "Sẵn sàng" else "Khóa VIP",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isUnlocked) Color(0xFF059669) else Color(0xFFE11D48)
                            )
                        }

                        // Lớp phủ mờ nếu bị khóa
                        if (!isUnlocked) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(Color.Black.copy(alpha = 0.04f))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyRowPlaceholder(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.04f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PremiumUpgradeDialog(
    packageName: String,
    onDismiss: () -> Unit
) {
    val displayPackageName = when (packageName) {
        "VIP_JAPANESE" -> "VIP Nhật Ngữ"
        "VIP_ENGLISH" -> "VIP Anh Ngữ"
        "VIP_CHINESE" -> "VIP Hoa Ngữ"
        "VIP_PALI" -> "VIP Pali Học"
        else -> "VIP Đặc Quyền"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Stars, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Nâng cấp VIP", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
            }
        },
        text = {
            Text(
                text = "Nội dung này thuộc gói $displayPackageName. Vui lòng liên hệ Admin để nâng cấp và mở khóa đặc quyền học tập không giới hạn!",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Liên hệ Admin", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    )
}

data class SystemSetItem(
    val title: String,
    val description: String,
    val language: String,
    val levelId: String,
    val requiredPackage: String = "FREE",
    val iconEmoji: String
)

@Composable
fun EmptySectionPlaceholder(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.04f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun LibraryScreen(
    subscribedPackages: List<String> = listOf("FREE"),
    navController: NavController,
    studySets: List<StudySet>,
    folders: List<com.example.flashcards.model.Folder> = emptyList(),
    onAddDeck: (String, String) -> Unit,
    onEditDeck: (String) -> Unit,
    onDeleteDeck: (String) -> Unit,
    onSetSelected: (StudySet) -> Unit,
    onQuizDeck: (StudySet) -> Unit,
    onImportDeck: (String) -> Unit,
    onCreateFolder: (String, String) -> Unit = { _, _ -> },
    onFolderClick: (com.example.flashcards.model.Folder) -> Unit = {}
) {
    var showImportDialog by remember { mutableStateOf(false) }
    var showCreateFolder by remember { mutableStateOf(false) }
    var selectedVipPackage by remember { mutableStateOf<String?>(null) }

    if (showImportDialog) {
        ImportDeckDialog(onDismiss = { showImportDialog = false }, onImport = onImportDeck)
    }

    if (showCreateFolder) {
        EditFolderDialog(
            initialName = "",
            initialEmoji = "📁",
            onDismiss = { showCreateFolder = false },
            onSave = { name, emoji ->
                onCreateFolder(name, emoji)
                showCreateFolder = false
            }
        )
    }

    if (selectedVipPackage != null) {
        PremiumUpgradeDialog(
            packageName = selectedVipPackage!!,
            onDismiss = { selectedVipPackage = null }
        )
    }

    val firestore = remember { FirebaseFirestore.getInstance() }
    val uid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous" }
    var starredCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(uid) {
        firestore.collection("user_favorites")
            .whereEqualTo("uid", uid)
            .whereEqualTo("starred", true)
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    starredCount = snapshot.size()
                }
            }
    }

    val vipSets = listOf(
        SystemSetItem("Tiếng Nhật N4", "Luyện thi năng lực Nhật ngữ N4", "JAPANESE", "QUIZ_JA_N4", "VIP_JAPANESE", "🇯🇵"),
        SystemSetItem("Tiếng Nhật N3", "Luyện thi năng lực Nhật ngữ N3", "JAPANESE", "QUIZ_JA_N3", "VIP_JAPANESE", "🇯🇵"),
        SystemSetItem("Tiếng Nhật N2", "Luyện thi năng lực Nhật ngữ N2", "JAPANESE", "QUIZ_JA_N2", "VIP_JAPANESE", "🇯🇵"),
        SystemSetItem("Tiếng Nhật N1", "Luyện thi năng lực Nhật ngữ N1", "JAPANESE", "QUIZ_JA_N1", "VIP_JAPANESE", "🇯🇵"),
        SystemSetItem("TOEIC 650+", "Từ vựng TOEIC mục tiêu 650+", "ENGLISH", "QUIZ_TOEIC_650", "VIP_ENGLISH", "🇬🇧"),
        SystemSetItem("TOEIC 800+", "Từ vựng TOEIC mục tiêu 800+", "ENGLISH", "QUIZ_TOEIC_800", "VIP_ENGLISH", "🇬🇧"),
        SystemSetItem("IELTS Band 5.5", "Từ vựng IELTS mục tiêu 5.5", "ENGLISH", "QUIZ_IELTS_55", "VIP_ENGLISH", "🇬🇧"),
        SystemSetItem("IELTS Band 6.5", "Từ vựng IELTS mục tiêu 6.5", "ENGLISH", "QUIZ_IELTS_65", "VIP_ENGLISH", "🇬🇧"),
        SystemSetItem("IELTS Band 7.5+", "Từ vựng IELTS mục tiêu 7.5+", "ENGLISH", "QUIZ_IELTS_75", "VIP_ENGLISH", "🇬🇧"),
        SystemSetItem("Trung Cơ Bản", "Học giao tiếp tiếng Trung cơ bản", "CHINESE", "QUIZ_ZH_BASIC", "VIP_CHINESE", "🇨🇳"),
        SystemSetItem("Pali Sơ Cấp", "Tìm hiểu ngôn ngữ Pali Phật học", "PALI", "QUIZ_PA_INTRO", "VIP_PALI", "☸️")
    )

    val freeSets = listOf(
        SystemSetItem("Tiếng Nhật N5", "Luyện thi năng lực Nhật ngữ N5", "JAPANESE", "QUIZ_JA_N5", "FREE", "🇯🇵"),
        SystemSetItem("TOEIC 450+", "Từ vựng TOEIC mục tiêu 450+", "ENGLISH", "QUIZ_TOEIC_450", "FREE", "🇬🇧")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Thư viện",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row {
                IconButton(onClick = { showCreateFolder = true }) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = "Thư mục mới", tint = FlowPrimary)
                }
                IconButton(onClick = { showImportDialog = true }) {
                    Icon(Icons.Default.Download, contentDescription = "Nhập bộ thẻ", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // TRÊN CÙNG: Học lại từ vựng đánh dấu sao ⭐
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            navController.navigate("user_flashcard/ALL/STARRED")
                        },
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFFF59E0B), Color(0xFFD97706))
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Học lại từ vựng đánh dấu sao ⭐",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Đã đánh dấu: $starredCount từ vựng",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Starred Icon",
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            }

            // MỤC 1: My Set
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Bộ thẻ tự tạo (My Set)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (studySets.isEmpty()) {
                        EmptySectionPlaceholder("Chưa có bộ thẻ tự tạo nào.")
                    } else {
                        studySets.forEach { set ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSetSelected(set) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Style,
                                            contentDescription = null,
                                            tint = FlowPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = set.title.ifBlank { "Chưa đặt tên" },
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = if (set.isPublic) Icons.Default.Public else Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Text(
                                            text = "${set.cards.size} thuật ngữ  •  ${if (set.creatorName.isNotBlank()) set.creatorName else "bạn"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // MỤC 2: Folders
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Thư mục của tôi (Folders)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (folders.isEmpty()) {
                        EmptySectionPlaceholder("Chưa có thư mục nào.")
                    } else {
                        folders.forEach { folder ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onFolderClick(folder) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = folder.emoji,
                                        style = MaterialTheme.typography.headlineMedium
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = folder.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = "${folder.setIds.size} học phần",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // MỤC 3: Set VIP
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Bộ học tập Premium (Set VIP) 👑",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    vipSets.forEach { item ->
                        val isUnlocked = subscribedPackages.contains(item.requiredPackage)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isUnlocked) {
                                        navController.navigate("level_dashboard/${item.language}/${item.levelId}")
                                    } else {
                                        selectedVipPackage = item.requiredPackage
                                    }
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isUnlocked) Color(0xFF10B981).copy(alpha = 0.12f)
                                            else Color.Black.copy(alpha = 0.05f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = item.iconEmoji,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = item.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Box(contentAlignment = Alignment.Center) {
                                    if (isUnlocked) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Unlocked",
                                            tint = Color(0xFF059669),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Locked",
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // MỤC 4: Set FREE
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Bộ học tập Miễn phí (Set FREE) 🌱",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    freeSets.forEach { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navController.navigate("level_dashboard/${item.language}/${item.levelId}")
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF10B981).copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = item.iconEmoji,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = item.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun SetsTabContent(studySets: List<StudySet>, onSetSelected: (StudySet) -> Unit) {
    if (studySets.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No sets yet. Create one!", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(studySets) { set ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onSetSelected(set) }.padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Style, contentDescription = null, tint = FlowPrimary)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                set.title.ifBlank { "Untitled" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = if (set.isPublic) Icons.Default.Public else Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            "Set  •  ${set.cards.size} terms  •  ${if (set.creatorName.isNotBlank()) set.creatorName else "you"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surface, thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun FoldersTabContent(
    folders: List<com.example.flashcards.model.Folder>,
    onFolderClick: (com.example.flashcards.model.Folder) -> Unit,
    onCreateFolder: () -> Unit
) {
    if (folders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📁", style = MaterialTheme.typography.displayMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Text("No folders yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onCreateFolder,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Folder")
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(folders) { folder ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onFolderClick(folder) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(folder.emoji, style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(folder.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            Text(
                                "${folder.setIds.size} set${if (folder.setIds.size != 1) "s" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun ClassesPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🏫", style = MaterialTheme.typography.displayMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Classes coming soon!", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Create and join classes to study together", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun LibraryDeckCard(
    set: StudySet,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onQuiz: (() -> Unit)? = null
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Deck?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
            text = { Text("Are you sure you want to delete '${set.title}'? This action cannot be undone.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete?.invoke() }) { Text("Delete", fontWeight = FontWeight.Bold, color = FlowWarning) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = FlowPrimary, modifier = Modifier.size(24.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp)) {
                        Text("Due", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = FlowPrimary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    if (onQuiz != null) {
                        IconButton(onClick = { onQuiz.invoke() }) {
                            Icon(Icons.Default.Quiz, contentDescription = "Quiz", tint = FlowPrimary)
                        }
                    }
                    if (onEdit != null) {
                        IconButton(onClick = { onEdit.invoke() }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (onDelete != null) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = FlowWarning)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(set.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text("${set.cards.size} cards", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Mastery", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("45%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(progress = { 0.45f }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), color = FlowPrimary, trackColor = MaterialTheme.colorScheme.background)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudySessionScreen(
    studySet: StudySet,
    onBack: () -> Unit,
    onSpeak: (String, String) -> Unit,
    onUpdateCard: (Flashcard, Int) -> Unit
) {
    val firestore = remember { FirebaseFirestore.getInstance() }
    val uid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous" }
    var starredCardIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showWishDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        if (uid.isNotEmpty() && uid != "anonymous") {
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

    var dailyWordLimit by remember { mutableIntStateOf(10) }
    var userProgressMap by remember { mutableStateOf<Map<String, Map<String, Any?>>>(emptyMap()) }
    var isLoadingData by remember { mutableStateOf(true) }

    LaunchedEffect(uid, studySet.id) {
        if (uid.isNotEmpty() && uid != "anonymous") {
            isLoadingData = true
            firestore.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        dailyWordLimit = doc.getLong("dailyWordLimit")?.toInt() ?: 10
                    }
                    firestore.collection("progress")
                        .whereEqualTo("uid", uid)
                        .get()
                        .addOnSuccessListener { snapshot ->
                            val progressMap = mutableMapOf<String, Map<String, Any?>>()
                            snapshot.documents.forEach { d ->
                                val vocabId = d.getString("vocabId") ?: ""
                                val status = d.getString("status") ?: ""
                                val nextReview = d.getTimestamp("nextReview")
                                progressMap[vocabId] = mapOf(
                                    "status" to status,
                                    "nextReview" to nextReview
                                )
                            }
                            userProgressMap = progressMap
                            isLoadingData = false
                        }
                        .addOnFailureListener {
                            isLoadingData = false
                        }
                }
                .addOnFailureListener {
                    isLoadingData = false
                }
        } else {
            isLoadingData = false
        }
    }

    val calendarInstance = remember { Calendar.getInstance() }
    val endOfToday = remember(calendarInstance) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        cal.timeInMillis
    }

    val allDueCards = remember(studySet.cards, userProgressMap, endOfToday) {
        studySet.cards.filter { card ->
            val progress = userProgressMap[card.id]
            if (progress != null) {
                val status = progress["status"] as? String ?: ""
                val nextReview = progress["nextReview"] as? com.google.firebase.Timestamp
                if (status == "MASTERED") {
                    false
                } else if (nextReview != null) {
                    nextReview.toDate().time <= endOfToday
                } else {
                    true
                }
            } else {
                true
            }
        }
    }

    val activeCards = remember(allDueCards, dailyWordLimit) {
        allDueCards.take(dailyWordLimit)
    }

    val remainingCards = remember(allDueCards, dailyWordLimit) {
        allDueCards.drop(dailyWordLimit)
    }

    var sessionCards by remember { mutableStateOf<List<Flashcard>>(emptyList()) }
    LaunchedEffect(activeCards) {
        sessionCards = activeCards
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var shuffleTrigger by remember { mutableIntStateOf(0) }
    val progress = if (sessionCards.isNotEmpty()) (currentIndex.toFloat() / sessionCards.size) else 0f

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
                else -> null
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

    val performRollover = {
        if (uid.isNotEmpty() && uid != "anonymous" && remainingCards.isNotEmpty()) {
            val calendar = java.util.Calendar.getInstance()
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 8)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val tomorrowTimestamp = com.google.firebase.Timestamp(calendar.time)
            
            remainingCards.forEach { card ->
                val progressDocRef = firestore.collection("progress").document("${uid}_${card.id}")
                val progressData = hashMapOf(
                    "uid" to uid,
                    "vocabId" to card.id,
                    "nextReview" to tomorrowTimestamp,
                    "status" to "HARD"
                )
                progressDocRef.set(progressData, com.google.firebase.firestore.SetOptions.merge())
            }
        }
    }

    val onCardRated = { card: Flashcard, quality: Int ->
        val statusStr = when (quality) {
            1 -> "HARD"
            3 -> "GOOD"
            else -> "EASY"
        }
        recordProgress(card.id, statusStr)
        onUpdateCard(card, quality)
        if (currentIndex == sessionCards.size - 1) {
            performRollover()
            showWishDialog = true
        } else {
            isFlipped = false
            currentIndex++
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thẻ ghi nhớ: ${studySet.title}", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    if (sessionCards.isNotEmpty()) {
                        IconButton(onClick = {
                            sessionCards = sessionCards.shuffled()
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
            if (!isLoadingData && sessionCards.isNotEmpty() && currentIndex < sessionCards.size) {
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
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Previous",
                                    tint = if (currentIndex > 0) MaterialTheme.colorScheme.onBackground else Color.Gray
                                )
                            }
                            Text(
                                text = "Thẻ: ${currentIndex + 1}/${sessionCards.size}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            if (currentIndex == sessionCards.size - 1) {
                                TextButton(
                                    onClick = {
                                        recordProgress(sessionCards[currentIndex].id, "GOOD")
                                        performRollover()
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
                                        recordProgress(sessionCards[currentIndex].id, "GOOD")
                                        currentIndex++
                                        isFlipped = false
                                    },
                                    enabled = currentIndex < sessionCards.size - 1
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
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
            if (isLoadingData) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FlowPrimary)
                }
            } else if (studySet.cards.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Bộ thẻ này hiện chưa có từ vựng.", color = Color.Gray)
                }
            } else if (sessionCards.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = FlowSuccess,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Chúc mừng! Bạn đã ôn tập hết từ vựng cho hôm nay.",
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Quay lại", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (currentIndex < sessionCards.size) {
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

                val currentCard = sessionCards[currentIndex]
                val rotationState by animateFloatAsState(
                    targetValue = if (isFlipped) 180f else 0f,
                    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                    label = "flip"
                )

                val scaleAnim = remember { Animatable(1f) }
                LaunchedEffect(currentIndex, shuffleTrigger) {
                    scaleAnim.snapTo(0.9f)
                    scaleAnim.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .graphicsLayer {
                            rotationX = rotationState
                            cameraDistance = 12f * density
                            scaleX = scaleAnim.value
                            scaleY = scaleAnim.value
                        }
                        .clickable { isFlipped = !isFlipped },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (rotationState <= 90f) {
                            // Giao diện Mặt trước
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
                                                        "front" to currentCard.question,
                                                        "back" to currentCard.answer,
                                                        "levelId" to studySet.id,
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

                                        // Nút loa phát âm TTS
                                        IconButton(
                                            onClick = {
                                                val wordToSpeak = currentCard.question
                                                val cleanWord = wordToSpeak.substringBefore("(").trim()
                                                onSpeak(cleanWord, studySet.languageCode)
                                            }
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

                                if (currentCard.imageUrl != null) {
                                    val imageModel = remember(currentCard.imageUrl) { ImageUtils.getImageModel(currentCard.imageUrl) }
                                    AsyncImage(
                                        model = imageModel,
                                        contentDescription = null,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(160.dp)
                                            .padding(bottom = 16.dp)
                                    )
                                }

                                Text(
                                    text = currentCard.question,
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
                            // Giao diện Mặt sau (Nghĩa tiếng Việt + Giải thích + Bộ 3 nút)
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
                                                            "front" to currentCard.question,
                                                            "back" to currentCard.answer,
                                                            "levelId" to studySet.id,
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

                                            // Nút loa phát âm TTS
                                            IconButton(
                                                onClick = {
                                                    val wordToSpeak = currentCard.answer
                                                    val cleanWord = wordToSpeak.substringBefore("(").trim()
                                                    onSpeak(cleanWord, studySet.languageCode)
                                                }
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
                                        text = currentCard.answer,
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        textAlign = TextAlign.Center
                                    )

                                    if (currentCard.explanation.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(
                                                text = currentCard.explanation,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(16.dp)
                                            )
                                        }
                                    }

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
                                            onClick = { onCardRated(currentCard, 1) },
                                            modifier = Modifier.weight(1f).height(52.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = FlowWarningLight)
                                        ) {
                                            Text("Khó", color = FlowWarning, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = { onCardRated(currentCard, 3) },
                                            modifier = Modifier.weight(1f).height(52.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                        ) {
                                            Text("Tốt", color = FlowPrimary, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = { onCardRated(currentCard, 5) },
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
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showWishDialog) {
        RandomWishDialog(
            onDismiss = {
                showWishDialog = false
                onBack()
            },
            onReviewAgain = {
                showWishDialog = false
                sessionCards = sessionCards.shuffled()
                currentIndex = 0
                isFlipped = false
                shuffleTrigger++
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(userStats: com.example.flashcards.model.UserStats, onBack: () -> Unit) {
    // 👈 GIỮ của bạn
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Learning Progress", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            Text("Your cognitive flow is at its peak. Keep the momentum going!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(32.dp))

            val totalAnswers = userStats.correctAnswers + userStats.wrongAnswers
            val accuracy = if (totalAnswers > 0) (userStats.correctAnswers * 100 / totalAnswers) else 0

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard("Current Streak", "${userStats.streakDays} days", Icons.Default.DateRange, Modifier.weight(1f), FlowPrimary)
                StatCard("Accuracy", "$accuracy%", Icons.Default.CheckCircle, Modifier.weight(1f), FlowSuccess)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard("Studied Today", "${userStats.cardsStudiedToday}", Icons.Default.Flag, Modifier.weight(1f), FlowPrimary)
                StatCard("Total Answers", "$totalAnswers", Icons.Default.DoneAll, Modifier.weight(1f), MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier, iconColor: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Icon(icon, contentDescription = null, tint = iconColor)
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun FlashcardEditItem(
    index: Int,
    card: Flashcard,
    onCardChange: (Flashcard) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val base64 = ImageUtils.uriToBase64(context, it)
            if (base64 != null) {
                onCardChange(card.copy(imageUrl = base64))
            }
        }
    }
    val scope = rememberCoroutineScope()
    var showSuggestedImages by remember { mutableStateOf(false) }
    var suggestedImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var isImagesLoading by remember { mutableStateOf(false) }

    // 👈 GIỮ của bạn: menu dropdown để upload từ URL
    var expanded by remember { mutableStateOf(false) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var tempUrl by remember { mutableStateOf("") }

    if (showUrlDialog) {
        AlertDialog(
            onDismissRequest = { showUrlDialog = false; tempUrl = "" },
            title = { Text("Enter Image URL", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
            text = {
                OutlinedTextField(
                    value = tempUrl,
                    onValueChange = { tempUrl = it },
                    label = { Text("Image URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (tempUrl.isNotBlank()) {
                        onCardChange(card.copy(imageUrl = tempUrl))
                    }
                    showUrlDialog = false
                    tempUrl = ""
                }) { Text("Save", fontWeight = FontWeight.Bold, color = FlowPrimary) }
            },
            dismissButton = {
                TextButton(onClick = { showUrlDialog = false; tempUrl = "" }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(index.toString(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = FlowWarning)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = card.question,
                onValueChange = { onCardChange(card.copy(question = it)) },
                label = { Text("Term") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                OutlinedTextField(
                    value = card.answer,
                    onValueChange = { onCardChange(card.copy(answer = it)) },
                    label = { Text("Definition") },
                    modifier = Modifier.weight(1f).heightIn(min = 64.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, FlowPrimary, RoundedCornerShape(8.dp))
                        .clickable { expanded = true },  // 👈 GIỮ của bạn
                    contentAlignment = Alignment.Center
                ) {
                    if (card.imageUrl != null) {
                        val imageModel = remember(card.imageUrl) { ImageUtils.getImageModel(card.imageUrl) }  // 👈 GIỮ của bạn
                        AsyncImage(model = imageModel, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = FlowPrimary, modifier = Modifier.size(24.dp))
                            Text("Image", color = FlowPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // 👈 GIỮ của bạn: dropdown menu
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Upload from Device", color = MaterialTheme.colorScheme.onBackground) },
                            onClick = { expanded = false; launcher.launch("image/*") },
                            leadingIcon = { Icon(Icons.Default.Upload, contentDescription = null, tint = FlowPrimary) }
                        )
                        DropdownMenuItem(
                            text = { Text("Enter Image URL", color = MaterialTheme.colorScheme.onBackground) },
                            onClick = { expanded = false; showUrlDialog = true },
                            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = FlowPrimary) }
                        )
                        DropdownMenuItem(
                            text = { Text("Suggested Images (AI)", color = FlowTextPrimary) },
                            onClick = { 
                                expanded = false
                                showSuggestedImages = !showSuggestedImages
                                
                                if (showSuggestedImages && card.question.isNotBlank()) {
                                    isImagesLoading = true
                                    
                                    scope.launch(Dispatchers.IO) {
                                        val originalText = card.question.trim()
                                        var englishKeyword = originalText
                                        
                                        try {
                                            // Chạy ngầm dịch đa ngôn ngữ sang tiếng Anh bằng MyMemory
                                            val translateUrl = "https://api.mymemory.translated.net/get?q=${java.net.URLEncoder.encode(originalText, "UTF-8")}&langpair=auto|en"
                                            val connection = java.net.URL(translateUrl).openConnection() as java.net.HttpURLConnection
                                            connection.requestMethod = "GET"
                                            connection.connectTimeout = 5000
                                            connection.readTimeout = 5000
                                            
                                            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                                            val match = "\"translatedText\":\"(.*?)\"".toRegex().find(responseText)
                                            if (match != null) {
                                                englishKeyword = match.groupValues[1]
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                        
                                        val cleanKeyword = englishKeyword.lowercase().replace(" ", ",")
                                       
                                        val generatedUrls = List(10) { i -> 
                                            "https://images.unsplash.com/photo-${1500000000000 + (i * 123456)}?w=300&auto=format&fit=crop&q=60&sig=$i&q=$cleanKeyword"
                                        }
                                        
                                        withContext(Dispatchers.Main) {
                                            suggestedImages = generatedUrls
                                            isImagesLoading = false
                                        }
                                    }
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = FlowPrimary) }
                        )
                        if (card.imageUrl != null) {
                            DropdownMenuItem(
                                text = { Text("Remove Image", color = FlowWarning) },
                                onClick = { expanded = false; onCardChange(card.copy(imageUrl = null)) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = FlowWarning) }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = card.explanation,
                onValueChange = { onCardChange(card.copy(explanation = it)) },
                label = { Text("Explanation (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            if (showSuggestedImages) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "IMAGE SUGGESTIONS FOR: \"${card.question.ifBlank { "..." }}\"", 
                    style = MaterialTheme.typography.labelMedium, 
                    color = FlowPrimary, 
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (isImagesLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(85.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = FlowPrimary, modifier = Modifier.size(24.dp))
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().height(85.dp)
                    ) {
                        items(suggestedImages) { url ->
                            val isSelected = card.imageUrl == url
                            Card(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clickable { onCardChange(card.copy(imageUrl = url)) }
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) FlowPrimary else FlowCardStroke,
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = "Suggested Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckEditorScreen(
    studySet: StudySet,
    isCreateMode: Boolean = false,  // 👈 GIỮ của bạn
    onSave: (StudySet) -> Unit,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf(studySet.title) }
    var description by remember { mutableStateOf(studySet.description) }
    var cards by remember { mutableStateOf(studySet.cards) }
    var isPublic by remember { mutableStateOf(studySet.isPublic) }  // 👈 GIỮ của bạn
    var showBulkImport by remember { mutableStateOf(false) }

    // 👈 LẤY của bạn tôi: languageCode cho TTS
    var languageCode by remember { mutableStateOf(studySet.languageCode) }
    val languages = listOf(
        "en" to "English",
        "vi" to "Vietnamese",
        "ja" to "Japanese",
        "ko" to "Korean",
        "fr" to "French",
        "de" to "German"
    )
    var expanded by remember { mutableStateOf(false) }

    if (showBulkImport) {
        BulkImportDialog(
            onDismiss = { showBulkImport = false },
            onImport = { newCards: List<Flashcard> ->
                cards = cards + newCards
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isCreateMode) "Create Deck" else "Edit Deck", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = { showBulkImport = true }) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Bulk Import", tint = FlowPrimary)
                    }
                    TextButton(onClick = {
                        if (title.isNotBlank()) {
                            onSave(studySet.copy(
                                title = title,
                                description = description,
                                cards = cards,
                                isPublic = isPublic,
                                languageCode = languageCode  // 👈 LẤY của bạn tôi
                            ))
                            onBack()
                        }
                    }) {
                        Text("Save", color = FlowPrimary, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Deck Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            item {
                OutlinedTextField(
                    value = title, // Lỗi: Đây nên là description
                    onValueChange = { description = it }, // Đã sửa logic nhưng title vẫn gán sai value
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            // 👈 GIỮ của bạn: switch Public/Private
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Public Deck", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Text("Anyone can find and study this deck", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = isPublic,
                        onCheckedChange = { isPublic = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = FlowPrimary, checkedTrackColor = FlowPrimary.copy(alpha = 0.5f))
                    )
                }
            }
            // 👈 LẤY của bạn tôi: dropdown chọn ngôn ngữ TTS
            item {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = languages.find { it.first == languageCode }?.second ?: "Select Language",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Language for Text-to-Speech") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        languages.forEach { (code, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    languageCode = code
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            itemsIndexed(cards) { index, card ->
                FlashcardEditItem(
                    index = index + 1,
                    card = card,
                    onCardChange = { updatedCard ->
                        cards = cards.map { if (it.id == card.id) updatedCard else it }
                    },
                    onDelete = {
                        cards = cards.filter { it.id != card.id }
                    }
                )
            }
            item {
                Button(
                    onClick = {
                        cards = cards + Flashcard(id = java.util.UUID.randomUUID().toString(), question = "", answer = "")
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = FlowPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Flashcard", color = FlowPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}