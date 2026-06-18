package com.example.flashcards.view

import com.example.flashcards.utils.ContextUtils
import com.example.flashcards.R
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
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
    var showGameBottomSheet by remember { mutableStateOf(false) }

    var hardVocabList by remember { mutableStateOf<List<VocabItem>>(emptyList()) }
    var goodVocabList by remember { mutableStateOf<List<VocabItem>>(emptyList()) }
    var easyVocabList by remember { mutableStateOf<List<VocabItem>>(emptyList()) }
    var selectedTab by remember { mutableStateOf("HARD") }

    var isTtsReady by remember { mutableStateOf(false) }
    var tts by remember { mutableStateOf<android.speech.tts.TextToSpeech?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        tts = android.speech.tts.TextToSpeech(context) { status ->
            if (status != android.speech.tts.TextToSpeech.ERROR) {
                tts?.language = java.util.Locale.US
                isTtsReady = true
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    val dayIndex = remember(selectedDay) {
        selectedDay.substringAfter("day_").toIntOrNull() ?: 1
    }

    val prettyDayName = when (dayIndex) {
        1 -> ContextUtils.getString(R.string.ui_text_410)
        2 -> ContextUtils.getString(R.string.ui_text_411)
        3 -> ContextUtils.getString(R.string.ui_text_412)
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
                                
                                val hList = mutableListOf<VocabItem>()
                                val gList = mutableListOf<VocabItem>()
                                val eList = mutableListOf<VocabItem>()

                                progressSnapshot.documents.forEach { doc ->
                                    val status = doc.getString("status") ?: ""
                                    val nextReview = doc.getTimestamp("nextReview")
                                    val lastReviewed = doc.getTimestamp("lastReviewed")

                                     var isDue = false
                                     if (status != "MASTERED") {
                                         if (nextReview != null) {
                                             val reviewTime = nextReview.toDate().time
                                             if (dayIndex == 1) {
                                                 if (reviewTime <= dayEndTimes[0]) {
                                                     isDue = true
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
                                                     isDue = true
                                                     totalCount++
                                                 }
                                             }
                                         } else if (dayIndex == 1) {
                                             // New unscheduled card is due today
                                             isDue = true
                                             totalCount++
                                         }
                                     }

                                     if (isDue) {
                                         val rawFront = doc.getString("front") ?: ""
                                         val rawBack = doc.getString("back") ?: ""
                                         val front = try { com.example.flashcards.utils.CryptoUtils.decrypt(rawFront) } catch (e: Exception) { rawFront }
                                         val back = try { com.example.flashcards.utils.CryptoUtils.decrypt(rawBack) } catch (e: Exception) { rawBack }
                                         if (front.isNotBlank() && back.isNotBlank()) {
                                             val item = VocabItem(doc.id, front, back, status)
                                             when (status) {
                                                 "HARD" -> hList.add(item)
                                                 "GOOD" -> gList.add(item)
                                                 else -> eList.add(item)
                                             }
                                         }
                                     }
                                }

                                hardVocabList = hList
                                goodVocabList = gList
                                easyVocabList = eList

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
                title = { Text(stringResource(R.string.ui_text_81), fontWeight = FontWeight.Bold) },
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
                                    Text(stringResource(R.string.ui_text_82),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                    Surface(
                                        color = Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(stringResource(R.string.ui_text_83),
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
                                Text(stringResource(R.string.ui_text_84),
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
                                description = ContextUtils.getString(R.string.ui_text_294),
                                icon = Icons.Default.Style,
                                tintColor = Color(0xFF2563EB),
                                onClick = {
                                    navController.navigate("user_flashcard/spaced/$selectedDay")
                                }
                            )
                        }
                        item {
                            LevelFeatureCard(
                                title = ContextUtils.getString(R.string.ui_text_295),
                                description = ContextUtils.getString(R.string.ui_text_296),
                                icon = Icons.Default.Quiz,
                                tintColor = Color(0xFFF97316),
                                onClick = {
                                    navController.navigate("user_play_quiz/$selectedDay")
                                }
                            )
                        }
                        item {
                            LevelFeatureCard(
                                title = ContextUtils.getString(R.string.ui_text_297),
                                description = ContextUtils.getString(R.string.ui_text_298),
                                icon = Icons.Default.EditNote,
                                tintColor = Color(0xFF10B981),
                                onClick = {
                                    showEssayBottomSheet = true
                                }
                            )
                        }
                        item {
                            LevelFeatureCard(
                                title = ContextUtils.getString(R.string.ui_text_299),
                                description = ContextUtils.getString(R.string.ui_text_319),
                                icon = Icons.Default.VideogameAsset,
                                tintColor = Color(0xFF8B5CF6),
                                onClick = { showGameBottomSheet = true }
                            )
                        }
                    }
                }

                // 3. Tab Title Section
                item {
                    Text(stringResource(R.string.ui_text_85) + totalCardsCount + ContextUtils.getString(R.string.ui_text_413),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // 4. Premium Tab Selector
                item {
                    val tabs = listOf(
                        "HARD" to (ContextUtils.getString(R.string.ui_text_414) + hardVocabList.size + ")"),
                        "GOOD" to (ContextUtils.getString(R.string.ui_text_415) + goodVocabList.size + ")"),
                        "EASY" to (ContextUtils.getString(R.string.ui_text_416) + easyVocabList.size + ")")
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        tabs.forEach { (tabId, label) ->
                            val isSelected = selectedTab == tabId
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) FlowPrimary else Color.Transparent)
                                    .clickable { selectedTab = tabId }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                // 5. Active List Items
                val activeList = when (selectedTab) {
                    "HARD" -> hardVocabList
                    "GOOD" -> goodVocabList
                    else -> easyVocabList
                }

                if (activeList.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(stringResource(R.string.ui_text_86),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(activeList) { vocab ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left: Word
                                Column(modifier = Modifier.weight(1.2f)) {
                                    val cleanFront = vocab.front.substringBefore("(").trim()
                                    val reading = if (vocab.front.contains("(")) {
                                        "(" + vocab.front.substringAfter("(")
                                    } else ""
                                    
                                    Text(
                                        text = cleanFront,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    if (reading.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = reading,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Center: Definition
                                Box(
                                    modifier = Modifier.weight(1.2f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = vocab.back,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                // Right: Speak
                                Row(
                                    modifier = Modifier.weight(0.6f),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            val wordToSpeak = vocab.front
                                            val cleanWord = wordToSpeak.substringBefore("(").trim()
                                            val speakLocale = if (cleanWord.any { it.code in 0x3040..0x30FF || it.code in 0x4E00..0x9FFF }) {
                                                java.util.Locale.JAPANESE
                                            } else {
                                                java.util.Locale.US
                                            }
                                            tts?.language = speakLocale
                                            tts?.speak(cleanWord, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
                                        },
                                        enabled = isTtsReady,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VolumeUp,
                                            contentDescription = ContextUtils.getString(R.string.ui_text_301),
                                            tint = FlowPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
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
            description = ContextUtils.getString(R.string.ui_text_417),
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

    if (showGameBottomSheet) {
        GameSelectionBottomSheet(
            hasBattleFeature = false,
            onDismiss = { showGameBottomSheet = false },
            onPlayMatch = {
                showGameBottomSheet = false
                navController.navigate("user_play_match/$selectedDay")
            }
        )
    }
}
