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
import androidx.compose.ui.res.stringResource
import com.example.flashcards.R
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
    var showGameBottomSheet by remember { mutableStateOf(false) }
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
                    Text(stringResource(R.string.ui_text_1),
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
                Text(stringResource(R.string.ui_text_2),
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
                        title = "Giải Trí",
                        description = "Nối từ, Minigames",
                        icon = Icons.Default.VideogameAsset,
                        gradientColors = listOf(Color(0xFF8B5CF6), Color(0xFF7C3AED)),
                        modifier = Modifier.weight(1f),
                        onClick = { showGameBottomSheet = true }
                    )
                }
            }
        }

        // --- 4. HỌC GẦN ĐÂY ---
        item {
            Column {
                Text(stringResource(R.string.ui_text_3),
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
                    Text(stringResource(R.string.ui_text_4),
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
                Text(stringResource(R.string.ui_text_5),
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

    if (showGameBottomSheet) {
        GameSelectionBottomSheet(
            hasBattleFeature = false,
            onDismiss = { showGameBottomSheet = false },
            onPlayMatch = {
                showGameBottomSheet = false
                onFeatureClick("match")
            }
        )
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

