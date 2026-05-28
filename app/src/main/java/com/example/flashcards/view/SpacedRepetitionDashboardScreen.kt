package com.example.flashcards.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.flashcards.ui.theme.FlowPrimary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpacedRepetitionDashboardScreen(
    navController: NavController,
    selectedDay: String
) {
    val firestore = remember { FirebaseFirestore.getInstance() }
    val uid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }

    var dailyWordLimit by remember { mutableIntStateOf(10) }
    var totalCardsCount by remember { mutableIntStateOf(0) }
    var studiedCount by remember { mutableIntStateOf(0) }
    var isLoadingData by remember { mutableStateOf(true) }
    var showEssayBottomSheet by remember { mutableStateOf(false) }

    val dayIndex = remember(selectedDay) {
        selectedDay.substringAfter("day_").toIntOrNull() ?: 1
    }

    val prettyDayName = when (dayIndex) {
        1 -> "Hôm nay (Ngày 1)"
        2 -> "Ngày mai (Ngày 2)"
        3 -> "Ngày kia (Ngày 3)"
        else -> "Ngày $dayIndex (Kế tiếp)"
    }

    LaunchedEffect(uid, dayIndex) {
        if (uid.isNotEmpty()) {
            isLoadingData = true
            firestore.collection("users").document(uid).get()
                .addOnSuccessListener { userDoc ->
                    if (userDoc.exists()) {
                        dailyWordLimit = userDoc.getLong("dailyWordLimit")?.toInt() ?: 10
                    }

                    firestore.collection("progress")
                        .whereEqualTo("uid", uid)
                        .addSnapshotListener { progressSnapshot, _ ->
                            if (progressSnapshot != null) {
                                val calendar = Calendar.getInstance()
                                
                                // Start of today
                                calendar.set(Calendar.HOUR_OF_DAY, 0)
                                calendar.set(Calendar.MINUTE, 0)
                                calendar.set(Calendar.SECOND, 0)
                                calendar.set(Calendar.MILLISECOND, 0)
                                val startOfToday = calendar.timeInMillis

                                val dayEndTimes = LongArray(7)
                                for (i in 0..6) {
                                    calendar.timeInMillis = startOfToday
                                    calendar.add(Calendar.DAY_OF_YEAR, i)
                                    calendar.set(Calendar.HOUR_OF_DAY, 23)
                                    calendar.set(Calendar.MINUTE, 59)
                                    calendar.set(Calendar.SECOND, 59)
                                    calendar.set(Calendar.MILLISECOND, 999)
                                    dayEndTimes[i] = calendar.timeInMillis
                                }

                                var totalCount = 0
                                var studied = 0

                                progressSnapshot.documents.forEach { doc ->
                                    val status = doc.getString("status") ?: ""
                                    val nextReview = doc.getTimestamp("nextReview")
                                    val lastReviewed = doc.getTimestamp("lastReviewed")

                                    if (status != "MASTERED") {
                                        if (nextReview != null) {
                                            val reviewTime = nextReview.toDate().time
                                            if (dayIndex == 1) {
                                                if (reviewTime <= dayEndTimes[0]) {
                                                    totalCount++
                                                    if (lastReviewed != null) {
                                                        // Checked if reviewed today
                                                        val reviewDate = lastReviewed.toDate().time
                                                        if (reviewDate >= startOfToday) {
                                                            studied++
                                                        }
                                                    }
                                                }
                                            } else {
                                                val startRange = dayEndTimes[dayIndex - 2]
                                                val endRange = dayEndTimes[dayIndex - 1]
                                                if (reviewTime > startRange && reviewTime <= endRange) {
                                                    totalCount++
                                                }
                                            }
                                        } else if (dayIndex == 1) {
                                            // New unscheduled card is due today
                                            totalCount++
                                        }
                                    }
                                }

                                totalCardsCount = if (dayIndex == 1) minOf(totalCount, dailyWordLimit) else totalCount
                                studiedCount = minOf(studied, totalCardsCount)
                                isLoadingData = false
                            } else {
                                isLoadingData = false
                            }
                        }
                }
                .addOnFailureListener {
                    isLoadingData = false
                }
        } else {
            isLoadingData = false
        }
    }

    val progressPercent = if (totalCardsCount > 0) (studiedCount.toFloat() / totalCardsCount) else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Luyện Tập Theo Lịch Trình", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isLoadingData) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FlowPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 1. Premium Spaced Repetition Header Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF1D4ED8), Color(0xFF3B82F6))
                                    )
                                )
                                .padding(24.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "SPACED REPETITION",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                    Surface(
                                        color = Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "LỊCH TRÌNH 7 NGÀY",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = prettyDayName,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Phương pháp học lặp lại ngắt quãng thông minh giúp tối ưu hóa khả năng ghi nhớ dài hạn của bạn.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Hạn mức ôn tập: $totalCardsCount từ",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (dayIndex == 1) {
                                        Text(
                                            text = "Tiến độ: $studiedCount/$totalCardsCount từ",
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontWeight = FontWeight.Medium,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                                if (dayIndex == 1) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { progressPercent },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(16.dp)),
                                        color = Color(0xFF10B981),
                                        trackColor = Color.White.copy(alpha = 0.25f)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Premium 2x2 Grid Layout
                item {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        userScrollEnabled = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            LevelFeatureCard(
                                title = "Flashcard",
                                description = "Học thẻ ghi nhớ",
                                icon = Icons.Default.Style,
                                tintColor = Color(0xFF2563EB),
                                onClick = {
                                    navController.navigate("user_flashcard/spaced/$selectedDay")
                                }
                            )
                        }
                        item {
                            LevelFeatureCard(
                                title = "Trắc Nghiệm",
                                description = "Luyện trắc nghiệm",
                                icon = Icons.Default.Quiz,
                                tintColor = Color(0xFFF97316),
                                onClick = {
                                    navController.navigate("user_play_quiz/$selectedDay")
                                }
                            )
                        }
                        item {
                            LevelFeatureCard(
                                title = "Tự Luận",
                                description = "Viết câu trả lời",
                                icon = Icons.Default.EditNote,
                                tintColor = Color(0xFF10B981),
                                onClick = {
                                    showEssayBottomSheet = true
                                }
                            )
                        }
                        item {
                            LevelFeatureCard(
                                title = "Nối Từ (Match)",
                                description = "Trò chơi ghép từ",
                                icon = Icons.Default.Extension,
                                tintColor = Color(0xFF8B5CF6),
                                onClick = {
                                    navController.navigate("user_play_match/$selectedDay")
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEssayBottomSheet) {
        val category = QuizCategoryItem(
            id = selectedDay,
            name = "Tự luận $prettyDayName",
            description = "Luyện viết từ vựng Spaced Repetition",
            requiredPackage = "FREE"
        )
        EssayPlayModeBottomSheet(
            category = category,
            onDismiss = { showEssayBottomSheet = false },
            onModeSelected = { mode ->
                showEssayBottomSheet = false
                navController.navigate("user_play_essay/$selectedDay/$mode")
            }
        )
    }
}
