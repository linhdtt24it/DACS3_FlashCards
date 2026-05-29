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
    val spacedRepetitionViewModel: com.example.flashcards.viewmodel.SpacedRepetitionViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
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
                                val interval = d.getLong("interval")?.toInt() ?: 0
                                val easeFactor = d.getDouble("easeFactor") ?: 2.5
                                progressMap[vocabId] = mapOf(
                                    "status" to status,
                                    "nextReview" to nextReview,
                                    "interval" to interval,
                                    "easeFactor" to easeFactor
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
        studySet.cards.mapNotNull { card ->
            val progress = userProgressMap[card.id]
            if (progress != null) {
                val status = progress["status"] as? String ?: ""
                val nextReview = progress["nextReview"] as? com.google.firebase.Timestamp
                val interval = progress["interval"] as? Int ?: 0
                val easeFactor = progress["easeFactor"] as? Double ?: 2.5
                val cardState = try { com.example.flashcards.model.CardState.valueOf(status) } catch(e: Exception) { com.example.flashcards.model.CardState.NEW }
                
                if (status == "MASTERED" || status == "MASTER") {
                    null
                } else if (nextReview != null && nextReview.toDate().time > endOfToday) {
                    null
                } else {
                    card.copy(interval = interval, easeFactor = easeFactor, state = cardState)
                }
            } else {
                card.copy(state = com.example.flashcards.model.CardState.NEW, interval = 0, easeFactor = 2.5)
            }
        }
    }

    val activeCards = remember(allDueCards, dailyWordLimit) {
        allDueCards.take(dailyWordLimit)
    }

    val remainingCards = remember(allDueCards, dailyWordLimit) {
        allDueCards.drop(dailyWordLimit)
    }

    var isStudyMode by remember { mutableStateOf(false) }
    var sessionCards by remember { mutableStateOf<List<Flashcard>>(emptyList()) }
    
    LaunchedEffect(isStudyMode, activeCards, studySet.cards) {
        sessionCards = if (isStudyMode) activeCards else studySet.cards
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var shuffleTrigger by remember { mutableIntStateOf(0) }
    val progress = if (sessionCards.isNotEmpty()) (currentIndex.toFloat() / sessionCards.size) else 0f

    val recordProgress = { card: Flashcard, rating: com.example.flashcards.model.ReviewRating ->
        if (uid.isNotEmpty() && uid != "anonymous") {
            spacedRepetitionViewModel.recordProgress(
                vocabId = card.id,
                currentState = card.state,
                currentInterval = card.interval,
                currentEaseFactor = card.easeFactor,
                rating = rating
            )
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

    val onCardRated = { card: Flashcard, ratingName: String ->
        val rating = try { com.example.flashcards.model.ReviewRating.valueOf(ratingName) } catch (e: Exception) { com.example.flashcards.model.ReviewRating.GOOD }
        recordProgress(card, rating)
        
        val oldQuality = when (rating) {
            com.example.flashcards.model.ReviewRating.AGAIN -> 1
            com.example.flashcards.model.ReviewRating.HARD -> 2
            com.example.flashcards.model.ReviewRating.GOOD -> 3
            com.example.flashcards.model.ReviewRating.EASY -> 4
        }
        onUpdateCard(card, oldQuality)
        
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isStudyMode) "Học" else "Duyệt",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isStudyMode) FlowPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Switch(
                            checked = isStudyMode,
                            onCheckedChange = { isStudyMode = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = FlowPrimary, checkedTrackColor = FlowPrimary.copy(alpha = 0.5f))
                        )
                    }
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
            // Đã thay thế thanh BottomBar cũ bằng AnkiRatingButtons hiển thị bên dưới Flashcard
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
                    Text(stringResource(R.string.ui_text_25), color = Color.Gray)
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
                        Text(stringResource(R.string.ui_text_26),
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
                            Text(stringResource(R.string.ui_text_27), fontWeight = FontWeight.Bold)
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
                val isStarred = starredCardIds.contains(currentCard.id)

                val onToggleStar: () -> Unit = {
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

                val onPlayTts = { text: String ->
                    val cleanWord = text.substringBefore("(").trim()
                    onSpeak(cleanWord, studySet.languageCode)
                }

                StudyFlashcard(
                    modifier = Modifier.weight(1f),
                    frontText = currentCard.question,
                    backText = currentCard.answer,
                    isFlipped = isFlipped,
                    onFlip = { isFlipped = !isFlipped },
                    isStarred = isStarred,
                    onToggleStar = onToggleStar,
                    onPlayTts = onPlayTts,
                    imageUrl = currentCard.imageUrl,
                    explanation = currentCard.explanation
                )

                if (isFlipped) {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    if (isStudyMode) {
                        val againResult = com.example.flashcards.model.SpacedRepetition.calculateNextReview(currentCard.state, currentCard.interval, currentCard.easeFactor, com.example.flashcards.model.ReviewRating.AGAIN)
                        val hardResult = com.example.flashcards.model.SpacedRepetition.calculateNextReview(currentCard.state, currentCard.interval, currentCard.easeFactor, com.example.flashcards.model.ReviewRating.HARD)
                        val goodResult = com.example.flashcards.model.SpacedRepetition.calculateNextReview(currentCard.state, currentCard.interval, currentCard.easeFactor, com.example.flashcards.model.ReviewRating.GOOD)
                        val easyResult = com.example.flashcards.model.SpacedRepetition.calculateNextReview(currentCard.state, currentCard.interval, currentCard.easeFactor, com.example.flashcards.model.ReviewRating.EASY)

                        val formatInterval = { days: Int ->
                            when (days) {
                                0 -> "<1p"
                                else -> "${days}n"
                            }
                        }

                        AnkiRatingButtons(
                            onRate = { rating -> 
                                onCardRated(currentCard, rating.name) 
                            },
                            againInterval = formatInterval(againResult.intervalDays),
                            hardInterval = formatInterval(hardResult.intervalDays),
                            goodInterval = formatInterval(goodResult.intervalDays),
                            easyInterval = formatInterval(easyResult.intervalDays)
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(
                                onClick = { 
                                    if (currentIndex > 0) {
                                        currentIndex--
                                        isFlipped = false
                                    }
                                },
                                enabled = currentIndex > 0
                            ) {
                                Text(stringResource(R.string.ui_text_27), fontWeight = FontWeight.Bold)
                            }
                            
                            Button(
                                onClick = { 
                                    if (currentIndex < sessionCards.size - 1) {
                                        currentIndex++
                                        isFlipped = false
                                    } else {
                                        showWishDialog = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(if (currentIndex == sessionCards.size - 1) "Hoàn thành" else "Tiếp theo", fontWeight = FontWeight.Bold)
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

