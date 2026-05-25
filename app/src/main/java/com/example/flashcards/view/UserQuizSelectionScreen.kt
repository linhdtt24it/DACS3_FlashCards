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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.flashcards.ui.theme.FlowPrimary
import com.example.flashcards.ui.theme.FlowWarning
import com.example.flashcards.viewmodel.AuthViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.example.flashcards.utils.CryptoUtils

data class QuizCategoryItem(
    val id: String,
    val name: String,
    val description: String,
    val requiredPackage: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserQuizSelectionScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    val subscribedPackages by authViewModel.subscribedPackages.collectAsState()
    var selectedVipPackage by remember { mutableStateOf<String?>(null) }

    // Logic đồng bộ danh mục từ Firestore real-time
    val firestore = remember { FirebaseFirestore.getInstance() }
    var firestoreCategories by remember { mutableStateOf<List<QuizCategoryItem>>(emptyList()) }
    var isLoadingFirestore by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        val listener = firestore.collection("quiz_sub_levels")
            .addSnapshotListener { snapshot, error ->
                isLoadingFirestore = false
                if (error != null) {
                    Log.e("UserQuizSelectionScreen", "Lỗi đồng bộ danh mục từ Firestore: ", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.getString("id") ?: doc.id
                            val rawName = doc.getString("name") ?: ""
                            val name = CryptoUtils.decrypt(rawName)
                            val rawDesc = doc.getString("description") ?: ""
                            val description = if (rawDesc.isNotEmpty()) CryptoUtils.decrypt(rawDesc) else "Trắc nghiệm trực tuyến"
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
                            Log.e("UserQuizSelectionScreen", "Lỗi phân tích danh mục: ${doc.id}", e)
                            null
                        }
                    }
                    firestoreCategories = list
                } else {
                    // Bộ sưu tập cũ quiz_categories dự phòng
                    firestore.collection("quiz_categories")
                        .get()
                        .addOnSuccessListener { legacySnapshot ->
                            if (legacySnapshot != null) {
                                val list = legacySnapshot.documents.mapNotNull { doc ->
                                    try {
                                        val id = doc.getString("categoryId") ?: doc.id
                                        val rawName = doc.getString("categoryName") ?: doc.getString("name") ?: ""
                                        val name = CryptoUtils.decrypt(rawName)
                                        val rawDesc = doc.getString("description") ?: ""
                                        val description = if (rawDesc.isNotEmpty()) CryptoUtils.decrypt(rawDesc) else "Trắc nghiệm trực tuyến"
                                        val requiredPackage = doc.getString("requiredPackage") ?: "FREE"
                                        if (id.isNotEmpty() && name.isNotEmpty()) {
                                            QuizCategoryItem(id, name, description, requiredPackage)
                                        } else null
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                                if (list.isNotEmpty()) {
                                    firestoreCategories = list
                                }
                            }
                        }
                }
            }
        onDispose {
            listener.remove()
        }
    }

    // Định nghĩa bộ danh mục mặc định (sẽ đồng bộ đè khi có Firestore)
    val defaultJapaneseQuizzes = listOf(
        QuizCategoryItem("QUIZ_JA_N5", "Trắc nghiệm N5", "Học tiếng Nhật nhập môn N5", "FREE"),
        QuizCategoryItem("QUIZ_JA_N4", "Trắc nghiệm N4", "Ôn luyện từ vựng ngữ pháp N4", "VIP_JAPANESE"),
        QuizCategoryItem("QUIZ_JA_N3", "Trắc nghiệm N3", "Cấp độ trung cấp N3 nâng cao", "VIP_JAPANESE"),
        QuizCategoryItem("QUIZ_JA_N2", "Trắc nghiệm N2", "Đọc hiểu và từ vựng N2", "VIP_JAPANESE"),
        QuizCategoryItem("QUIZ_JA_N1", "Trắc nghiệm N1", "Chinh phục đỉnh cao N1 thượng đẳng", "VIP_JAPANESE")
    )

    val defaultToeicQuizzes = listOf(
        QuizCategoryItem("QUIZ_TOEIC_450", "TOEIC 450+", "Từ vựng & ngữ pháp cơ bản", "FREE"),
        QuizCategoryItem("QUIZ_TOEIC_650", "TOEIC 650+", "Chiến thuật nâng điểm 650+", "VIP_ENGLISH"),
        QuizCategoryItem("QUIZ_TOEIC_800", "TOEIC 800+", "Chinh phục điểm số cao 800+", "VIP_ENGLISH")
    )

    val defaultIeltsQuizzes = listOf(
        QuizCategoryItem("QUIZ_IELTS_55", "IELTS Band 5.5", "Từ vựng cốt lõi cho mục tiêu 5.5", "VIP_ENGLISH"),
        QuizCategoryItem("QUIZ_IELTS_65", "IELTS Band 6.5", "Từ vựng học thuật nâng cao 6.5", "VIP_ENGLISH"),
        QuizCategoryItem("QUIZ_IELTS_75", "IELTS Band 7.5+", "Chinh phục từ vựng đỉnh cao 7.5+", "VIP_ENGLISH")
    )

    val defaultOtherQuizzes = listOf(
        QuizCategoryItem("QUIZ_ZH_BASIC", "Trung Cơ Bản", "Học phát âm và từ vựng HSK 1-2", "VIP_CHINESE"),
        QuizCategoryItem("QUIZ_PA_INTRO", "Pali Sơ Cấp", "Từ vựng kinh điển Pali sơ cấp", "VIP_PALI")
    )

    // Đồng bộ an toàn và bảo vệ dữ liệu null-safety
    val japaneseQuizzes = remember(firestoreCategories) {
        defaultJapaneseQuizzes.map { defaultItem ->
            val firestoreItem = firestoreCategories.find { it.id == defaultItem.id }
            if (firestoreItem != null) {
                defaultItem.copy(
                    name = if (firestoreItem.name.isNotEmpty()) firestoreItem.name else defaultItem.name,
                    description = if (firestoreItem.description.isNotEmpty()) firestoreItem.description else defaultItem.description,
                    requiredPackage = firestoreItem.requiredPackage
                )
            } else {
                defaultItem
            }
        }
    }

    val toeicQuizzes = remember(firestoreCategories) {
        defaultToeicQuizzes.map { defaultItem ->
            val firestoreItem = firestoreCategories.find { it.id == defaultItem.id }
            if (firestoreItem != null) {
                defaultItem.copy(
                    name = if (firestoreItem.name.isNotEmpty()) firestoreItem.name else defaultItem.name,
                    description = if (firestoreItem.description.isNotEmpty()) firestoreItem.description else defaultItem.description,
                    requiredPackage = firestoreItem.requiredPackage
                )
            } else {
                defaultItem
            }
        }
    }

    val ieltsQuizzes = remember(firestoreCategories) {
        defaultIeltsQuizzes.map { defaultItem ->
            val firestoreItem = firestoreCategories.find { it.id == defaultItem.id }
            if (firestoreItem != null) {
                defaultItem.copy(
                    name = if (firestoreItem.name.isNotEmpty()) firestoreItem.name else defaultItem.name,
                    description = if (firestoreItem.description.isNotEmpty()) firestoreItem.description else defaultItem.description,
                    requiredPackage = firestoreItem.requiredPackage
                )
            } else {
                defaultItem
            }
        }
    }

    val otherQuizzes = remember(firestoreCategories) {
        defaultOtherQuizzes.map { defaultItem ->
            val firestoreItem = firestoreCategories.find { it.id == defaultItem.id }
            if (firestoreItem != null) {
                defaultItem.copy(
                    name = if (firestoreItem.name.isNotEmpty()) firestoreItem.name else defaultItem.name,
                    description = if (firestoreItem.description.isNotEmpty()) firestoreItem.description else defaultItem.description,
                    requiredPackage = firestoreItem.requiredPackage
                )
            } else {
                defaultItem
            }
        }
    }

    val unmappedQuizzes = remember(firestoreCategories) {
        val allMappedIds = (defaultJapaneseQuizzes + defaultToeicQuizzes + defaultIeltsQuizzes + defaultOtherQuizzes).map { it.id }.toSet()
        firestoreCategories.filter { it.id !in allMappedIds }
    }

    if (selectedVipPackage != null) {
        PremiumUpgradeDialog(
            packageName = selectedVipPackage!!,
            onDismiss = { selectedVipPackage = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Luyện thi trắc nghiệm", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
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
            // THANH NGANG 1: TRẮC NGHIỆM TIẾNG NHẬT
            item {
                QuizGroupRow(
                    title = "Trắc nghiệm Tiếng Nhật",
                    items = japaneseQuizzes,
                    subscribedPackages = subscribedPackages,
                    onItemClick = { item ->
                        navController.navigate("user_play_quiz/${item.id}")
                    },
                    onLockClick = { pkg -> selectedVipPackage = pkg }
                )
            }

            // THANH NGANG 2: TRẮC NGHIỆM TOEIC
            item {
                QuizGroupRow(
                    title = "Trắc nghiệm chứng chỉ TOEIC",
                    items = toeicQuizzes,
                    subscribedPackages = subscribedPackages,
                    onItemClick = { item ->
                        navController.navigate("user_play_quiz/${item.id}")
                    },
                    onLockClick = { pkg -> selectedVipPackage = pkg }
                )
            }

            // THANH NGANG 3: TRẮC NGHIỆM IELTS
            item {
                QuizGroupRow(
                    title = "Trắc nghiệm chứng chỉ IELTS",
                    items = ieltsQuizzes,
                    subscribedPackages = subscribedPackages,
                    onItemClick = { item ->
                        navController.navigate("user_play_quiz/${item.id}")
                    },
                    onLockClick = { pkg -> selectedVipPackage = pkg }
                )
            }

            // THANH NGANG 4: TRẮC NGHIỆM TIẾNG TRUNG & PALI
            item {
                QuizGroupRow(
                    title = "Trắc nghiệm Tiếng Trung & Pali",
                    items = otherQuizzes,
                    subscribedPackages = subscribedPackages,
                    onItemClick = { item ->
                        navController.navigate("user_play_quiz/${item.id}")
                    },
                    onLockClick = { pkg -> selectedVipPackage = pkg }
                )
            }

            // THANH NGANG 5: CÁC BỘ ĐỀ KHÁC (Tự động tải từ Firestore của Admin)
            if (unmappedQuizzes.isNotEmpty()) {
                item {
                    QuizGroupRow(
                        title = "Bộ trắc nghiệm mở rộng",
                        items = unmappedQuizzes,
                        subscribedPackages = subscribedPackages,
                        onItemClick = { item ->
                            navController.navigate("user_play_quiz/${item.id}")
                        },
                        onLockClick = { pkg -> selectedVipPackage = pkg }
                    )
                }
            }
        }
    }
}

@Composable
fun QuizGroupRow(
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
                // Kiểm tra điều kiện mở khoá: miễn phí hoặc người dùng đã đăng ký gói VIP tương ứng
                // Với IELTS: Cho phép mở bằng VIP_ENGLISH hoặc VIP_IELTS
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
                                        if (isUnlocked) Color(0xFFE0F2FE)
                                        else Color(0xFFFEF3C7) // soft golden background for locked ones!
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isUnlocked) Icons.Default.Quiz else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (isUnlocked) FlowPrimary else Color(0xFFD97706), // golden padlock!
                                    modifier = Modifier.size(22.dp)
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
