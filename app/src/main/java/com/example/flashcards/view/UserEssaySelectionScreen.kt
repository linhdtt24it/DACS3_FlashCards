package com.example.flashcards.view

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.navigation.NavController
import com.example.flashcards.ui.theme.FlowPrimary
import com.example.flashcards.ui.theme.FlowWarning
import com.example.flashcards.viewmodel.AuthViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.example.flashcards.utils.CryptoUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserEssaySelectionScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    val subscribedPackages by authViewModel.subscribedPackages.collectAsState()
    var selectedVipPackage by remember { mutableStateOf<String?>(null) }
    var selectedCategoryForSheet by remember { mutableStateOf<QuizCategoryItem?>(null) }

    // Logic đồng bộ danh mục từ Firestore
    val firestore = remember { FirebaseFirestore.getInstance() }
    var firestoreCategories by remember { mutableStateOf<List<QuizCategoryItem>>(emptyList()) }
    var isLoadingFirestore by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        val listener = firestore.collection("quiz_sub_levels")
            .addSnapshotListener { snapshot, error ->
                isLoadingFirestore = false
                if (error != null) {
                    Log.e("UserEssaySelectionScreen", "Lỗi đồng bộ danh mục từ Firestore: ", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.getString("id") ?: doc.id
                            val rawName = doc.getString("name") ?: ""
                            val name = CryptoUtils.decrypt(rawName)
                            val rawDesc = doc.getString("description") ?: ""
                            val description = if (rawDesc.isNotEmpty()) CryptoUtils.decrypt(rawDesc) else "Tự luận rèn luyện"
                            val requiredPackage = doc.getString("requiredPackage") ?: when {
                                id.contains("N5") || id.contains("450") -> "FREE"
                                id.contains("JA") -> "VIP_JAPANESE"
                                id.contains("TOEIC") || id.contains("IELTS") -> "VIP_ENGLISH"
                                id.contains("ZH") -> "VIP_CHINESE"
                                id.contains("PA") -> "VIP_PALI"
                                else -> "FREE"
                            }
                            if (id.isNotEmpty() && name.isNotEmpty()) {
                                QuizCategoryItem(id, name, description, requiredPackage)
                            } else null
                        } catch (e: Exception) {
                            Log.e("UserEssaySelectionScreen", "Lỗi phân tích danh mục: ${doc.id}", e)
                            null
                        }
                    }
                    firestoreCategories = list
                }
            }
        onDispose {
            listener.remove()
        }
    }

    // Các bộ danh mục mặc định đồng bộ cấu trúc trắc nghiệm
    val defaultJapaneseEssay = listOf(
        QuizCategoryItem("QUIZ_JA_N5", "Tự luận N5", "Luyện viết từ vựng tiếng Nhật N5", "FREE"),
        QuizCategoryItem("QUIZ_JA_N4", "Tự luận N4", "Luyện viết từ vựng tiếng Nhật N4", "VIP_JAPANESE"),
        QuizCategoryItem("QUIZ_JA_N3", "Tự luận N3", "Luyện viết từ vựng tiếng Nhật N3", "VIP_JAPANESE"),
        QuizCategoryItem("QUIZ_JA_N2", "Tự luận N2", "Luyện viết từ vựng tiếng Nhật N2", "VIP_JAPANESE"),
        QuizCategoryItem("QUIZ_JA_N1", "Tự luận N1", "Luyện viết từ vựng tiếng Nhật N1", "VIP_JAPANESE")
    )

    val defaultToeicEssay = listOf(
        QuizCategoryItem("QUIZ_TOEIC_450", "Tự luận TOEIC 450+", "Viết từ vựng TOEIC cơ bản", "FREE"),
        QuizCategoryItem("QUIZ_TOEIC_650", "Tự luận TOEIC 650+", "Viết từ vựng TOEIC 650+", "VIP_ENGLISH"),
        QuizCategoryItem("QUIZ_TOEIC_800", "Tự luận TOEIC 800+", "Viết từ vựng TOEIC 800+", "VIP_ENGLISH")
    )

    val defaultIeltsEssay = listOf(
        QuizCategoryItem("QUIZ_IELTS_55", "Tự luận IELTS 5.5", "Viết từ vựng IELTS Band 5.5", "VIP_ENGLISH"),
        QuizCategoryItem("QUIZ_IELTS_65", "Tự luận IELTS 6.5", "Viết từ vựng IELTS Band 6.5", "VIP_ENGLISH"),
        QuizCategoryItem("QUIZ_IELTS_75", "Tự luận IELTS 7.5+", "Viết từ vựng IELTS Band 7.5+", "VIP_ENGLISH")
    )

    val defaultOtherEssay = listOf(
        QuizCategoryItem("QUIZ_ZH_BASIC", "Tự luận Trung Cơ Bản", "Viết chữ Hán và Pinyin HSK 1-2", "VIP_CHINESE"),
        QuizCategoryItem("QUIZ_PA_INTRO", "Tự luận Pali Sơ Cấp", "Viết từ vựng Pali sơ cấp", "VIP_PALI")
    )

    // Đồng bộ an toàn và bảo vệ dữ liệu null-safety
    val japaneseQuizzes = remember(firestoreCategories) {
        defaultJapaneseEssay.map { defaultItem ->
            val firestoreItem = firestoreCategories.find { it.id == defaultItem.id }
            if (firestoreItem != null) {
                defaultItem.copy(
                    name = if (firestoreItem.name.isNotEmpty()) firestoreItem.name.replace("Trắc nghiệm", "Tự luận") else defaultItem.name,
                    description = if (firestoreItem.description.isNotEmpty()) firestoreItem.description.replace("Trắc nghiệm", "Tự luận") else defaultItem.description,
                    requiredPackage = firestoreItem.requiredPackage
                )
            } else {
                defaultItem
            }
        }
    }

    val toeicQuizzes = remember(firestoreCategories) {
        defaultToeicEssay.map { defaultItem ->
            val firestoreItem = firestoreCategories.find { it.id == defaultItem.id }
            if (firestoreItem != null) {
                defaultItem.copy(
                    name = if (firestoreItem.name.isNotEmpty()) firestoreItem.name.replace("Trắc nghiệm", "Tự luận") else defaultItem.name,
                    description = if (firestoreItem.description.isNotEmpty()) firestoreItem.description.replace("Trắc nghiệm", "Tự luận") else defaultItem.description,
                    requiredPackage = firestoreItem.requiredPackage
                )
            } else {
                defaultItem
            }
        }
    }

    val ieltsQuizzes = remember(firestoreCategories) {
        defaultIeltsEssay.map { defaultItem ->
            val firestoreItem = firestoreCategories.find { it.id == defaultItem.id }
            if (firestoreItem != null) {
                defaultItem.copy(
                    name = if (firestoreItem.name.isNotEmpty()) firestoreItem.name.replace("Trắc nghiệm", "Tự luận") else defaultItem.name,
                    description = if (firestoreItem.description.isNotEmpty()) firestoreItem.description.replace("Trắc nghiệm", "Tự luận") else defaultItem.description,
                    requiredPackage = firestoreItem.requiredPackage
                )
            } else {
                defaultItem
            }
        }
    }

    val otherQuizzes = remember(firestoreCategories) {
        defaultOtherEssay.map { defaultItem ->
            val firestoreItem = firestoreCategories.find { it.id == defaultItem.id }
            if (firestoreItem != null) {
                defaultItem.copy(
                    name = if (firestoreItem.name.isNotEmpty()) firestoreItem.name.replace("Trắc nghiệm", "Tự luận") else defaultItem.name,
                    description = if (firestoreItem.description.isNotEmpty()) firestoreItem.description.replace("Trắc nghiệm", "Tự luận") else defaultItem.description,
                    requiredPackage = firestoreItem.requiredPackage
                )
            } else {
                defaultItem
            }
        }
    }

    val unmappedQuizzes = remember(firestoreCategories) {
        val allMappedIds = (defaultJapaneseEssay + defaultToeicEssay + defaultIeltsEssay + defaultOtherEssay).map { it.id }.toSet()
        firestoreCategories.filter { it.id !in allMappedIds }.map { item ->
            item.copy(
                name = item.name.replace("Trắc nghiệm", "Tự luận"),
                description = item.description.replace("Trắc nghiệm", "Tự luận")
            )
        }
    }

    if (selectedVipPackage != null) {
        PremiumUpgradeDialog(
            packageName = selectedVipPackage!!,
            onDismiss = { selectedVipPackage = null }
        )
    }

    if (selectedCategoryForSheet != null) {
        EssayPlayModeBottomSheet(
            category = selectedCategoryForSheet!!,
            onDismiss = { selectedCategoryForSheet = null },
            onModeSelected = { playMode ->
                val categoryId = selectedCategoryForSheet!!.id
                selectedCategoryForSheet = null
                navController.navigate("user_play_essay/$categoryId/$playMode")
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Luyện viết tự luận", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // THANH NGANG 1: TỰ LUẬN TIẾNG NHẬT
            item {
                EssayGroupRow(
                    title = "Tự luận Tiếng Nhật",
                    items = japaneseQuizzes,
                    subscribedPackages = subscribedPackages,
                    onItemClick = { item -> selectedCategoryForSheet = item },
                    onLockClick = { pkg -> selectedVipPackage = pkg }
                )
            }

            // THANH NGANG 2: TỰ LUẬN TOEIC
            item {
                EssayGroupRow(
                    title = "Tự luận chứng chỉ TOEIC",
                    items = toeicQuizzes,
                    subscribedPackages = subscribedPackages,
                    onItemClick = { item -> selectedCategoryForSheet = item },
                    onLockClick = { pkg -> selectedVipPackage = pkg }
                )
            }

            // THANH NGANG 3: TỰ LUẬN IELTS
            item {
                EssayGroupRow(
                    title = "Tự luận chứng chỉ IELTS",
                    items = ieltsQuizzes,
                    subscribedPackages = subscribedPackages,
                    onItemClick = { item -> selectedCategoryForSheet = item },
                    onLockClick = { pkg -> selectedVipPackage = pkg }
                )
            }

            // THANH NGANG 4: TỰ LUẬN TIẾNG TRUNG & PALI
            item {
                EssayGroupRow(
                    title = "Tự luận Tiếng Trung & Pali",
                    items = otherQuizzes,
                    subscribedPackages = subscribedPackages,
                    onItemClick = { item -> selectedCategoryForSheet = item },
                    onLockClick = { pkg -> selectedVipPackage = pkg }
                )
            }

            // THANH NGANG 5: BỘ TỰ LUẬN MỞ RỘNG
            if (unmappedQuizzes.isNotEmpty()) {
                item {
                    EssayGroupRow(
                        title = "Bộ tự luận mở rộng",
                        items = unmappedQuizzes,
                        subscribedPackages = subscribedPackages,
                        onItemClick = { item -> selectedCategoryForSheet = item },
                        onLockClick = { pkg -> selectedVipPackage = pkg }
                    )
                }
            }
        }
    }
}

@Composable
fun EssayGroupRow(
    title: String,
    items: List<QuizCategoryItem>,
    subscribedPackages: List<String>,
    onItemClick: (QuizCategoryItem) -> Unit,
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
                val isUnlocked = when {
                    item.requiredPackage == "FREE" -> true
                    subscribedPackages.contains(item.requiredPackage) -> true
                    item.id.contains("IELTS") && (subscribedPackages.contains("VIP_ENGLISH") || subscribedPackages.contains("VIP_IELTS")) -> true
                    else -> false
                }

                Card(
                    modifier = Modifier
                        .width(180.dp)
                        .clickable {
                            if (isUnlocked) onItemClick(item) else onLockClick(item.requiredPackage)
                        },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier.padding(18.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isUnlocked) Color(0xFFECFDF5) // premium green for essay
                                        else Color(0xFFFEF3C7)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isUnlocked) Icons.Default.EditNote else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (isUnlocked) Color(0xFF059669) else Color(0xFFD97706),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isUnlocked) "Sẵn sàng" else "Yêu cầu VIP",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isUnlocked) Color(0xFF059669) else Color(0xFFE11D48)
                            )
                        }

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EssayPlayModeBottomSheet(
    category: QuizCategoryItem,
    onDismiss: () -> Unit,
    onModeSelected: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp, top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Chọn Chế Độ Luyện Tập",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Luyện nhớ sâu từ vựng bằng cách gõ tay chính xác đáp án. Vui lòng lựa chọn chế độ chơi:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(28.dp))

            // Option 1 Card
            Card(
                onClick = { onModeSelected("1") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE0F2FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = null,
                            tint = Color(0xFF0284C7)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Nhập nghĩa Tiếng Việt",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Hiển thị từ vựng nước ngoài, bạn gõ nghĩa Tiếng Việt tương ứng.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Option 2 Card
            Card(
                onClick = { onModeSelected("2") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = null,
                            tint = Color(0xFFD97706)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Nhập từ vựng gốc",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Hiển thị nghĩa Tiếng Việt, bạn gõ đúng từ gốc (tiếng Nhật/Anh/Trung...).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
            }
        }
    }
}
