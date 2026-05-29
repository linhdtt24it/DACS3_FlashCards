package com.example.flashcards.view

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.flashcards.model.Flashcard
import com.example.flashcards.model.StudySet
import com.example.flashcards.ui.theme.*
import com.example.flashcards.utils.ImageUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckDetailScreen(
    studySet: StudySet,
    userName: String,
    currentUserId: String,
    comments: List<com.example.flashcards.model.Comment> = emptyList(),
    onAddComment: (String) -> Unit = {},
    onBack: () -> Unit,
    onStudyFlashcards: () -> Unit,
    onQuiz: () -> Unit,
    onPlayMatchGame: () -> Unit,
    onPlayBattle: () -> Unit,
    onJoinBattle: (String) -> Unit = {},
    onJoinRandomBattle: () -> Unit = {},
    onEditDeck: () -> Unit,
    onDeleteDeck: () -> Unit
) {
    val context = LocalContext.current
    UserSetDashboardScreen(
        studySet = studySet,
        userName = userName,
        currentUserId = currentUserId,
        comments = comments,
        onAddComment = onAddComment,
        onBack = onBack,
        onStudyFlashcards = onStudyFlashcards,
        onQuiz = onQuiz,
        onEssay = {
            Toast.makeText(context, "Tính năng Tự luận cho bộ thẻ cá nhân đã đồng bộ!", Toast.LENGTH_SHORT).show()
            onStudyFlashcards() // Tự luận dùng chung flashcard học
        },
        onPlayMatchGame = onPlayMatchGame,
        onPlayBattle = onPlayBattle,
        onJoinBattle = onJoinBattle,
        onJoinRandomBattle = onJoinRandomBattle,
        onEditDeck = onEditDeck,
        onDeleteDeck = onDeleteDeck
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSetDashboardScreen(
    studySet: StudySet,
    userName: String,
    currentUserId: String,
    comments: List<com.example.flashcards.model.Comment> = emptyList(),
    onAddComment: (String) -> Unit = {},
    onBack: () -> Unit,
    onStudyFlashcards: () -> Unit,
    onQuiz: () -> Unit,
    onEssay: () -> Unit,
    onPlayMatchGame: () -> Unit,
    onPlayBattle: () -> Unit,
    onJoinBattle: (String) -> Unit = {},
    onJoinRandomBattle: () -> Unit = {},
    onEditDeck: () -> Unit,
    onDeleteDeck: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showGameBottomSheet by remember { mutableStateOf(false) }
    var newCommentText by remember { mutableStateOf("") }
    val isOwner = currentUserId == studySet.creatorId || studySet.creatorId.isEmpty()

    val firestore = remember { FirebaseFirestore.getInstance() }
    val uid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }

    // Listen to cards inside user_decks -> deckId -> cards collection
    var cardsList by remember { mutableStateOf<List<Flashcard>>(emptyList()) }
    DisposableEffect(studySet.id) {
        if (studySet.id.isEmpty()) {
            cardsList = studySet.cards
            return@DisposableEffect onDispose {}
        }
        val registration = firestore.collection("user_decks").document(studySet.id).collection("cards")
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null && !snapshot.isEmpty) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Flashcard::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    cardsList = list
                } else {
                    cardsList = studySet.cards
                }
            }
        onDispose {
            registration.remove()
        }
    }

    // Dynamic TextToSpeech initialization based on languageCode
    var isTtsReady by remember { mutableStateOf(false) }
    var tts by remember { mutableStateOf<android.speech.tts.TextToSpeech?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(studySet.languageCode) {
        tts = android.speech.tts.TextToSpeech(context) { status ->
            if (status != android.speech.tts.TextToSpeech.ERROR) {
                val locale = when (studySet.languageCode.uppercase()) {
                    "JA", "JAPANESE" -> java.util.Locale.JAPANESE
                    "EN", "ENGLISH" -> java.util.Locale.US
                    "ZH", "CHINESE" -> java.util.Locale.CHINESE
                    else -> java.util.Locale.US
                }
                tts?.language = locale
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

    // Real-time favorite card IDs tracking
    var starredCardIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    DisposableEffect(uid) {
        if (uid.isEmpty()) return@DisposableEffect onDispose {}
        val registration = firestore.collection("user_favorites")
            .whereEqualTo("uid", uid)
            .whereEqualTo("starred", true)
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    val ids = snapshot.documents.mapNotNull { it.getString("vocabId") }.toSet()
                    starredCardIds = ids
                }
            }
        onDispose {
            registration.remove()
        }
    }

    // Real-time progress tracking to compute mastery
    var progressVocabIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    DisposableEffect(uid) {
        if (uid.isEmpty()) return@DisposableEffect onDispose {}
        val registration = firestore.collection("progress")
            .whereEqualTo("uid", uid)
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    progressVocabIds = snapshot.documents.mapNotNull { it.getString("vocabId") }.toSet()
                }
            }
        onDispose {
            registration.remove()
        }
    }

    val learnedCardsCount = remember(progressVocabIds, cardsList) {
        cardsList.count { progressVocabIds.contains(it.id) }
    }

    val progressPercent = if (cardsList.isNotEmpty()) learnedCardsCount.toFloat() / cardsList.size else 0f
    val creatorName = studySet.creatorName.ifEmpty { userName }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Set Dashboard", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.BookmarkBorder, contentDescription = "Bookmark", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    if (isOwner) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = MaterialTheme.colorScheme.onBackground)
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Sửa bộ thẻ") },
                                    onClick = { showMenu = false; onEditDeck() },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Xóa bộ thẻ", color = FlowWarning) },
                                    onClick = { showMenu = false; onDeleteDeck() },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = FlowWarning) }
                                )
                            }
                        }
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
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Premium Header Section
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = creatorName.take(1).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = creatorName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                if (studySet.isPublic) {
                                    Surface(
                                        color = Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "PUBLIC",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = studySet.title,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (studySet.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = studySet.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tổng số từ: ${cardsList.size} từ",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Tiến độ: $learnedCardsCount/${cardsList.size} thẻ",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.Medium,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { progressPercent },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = Color(0xFF10B981),
                                trackColor = Color.White.copy(alpha = 0.25f)
                            )
                        }
                    }
                }
            }

            // 2. Premium 2x2 Grid Layout
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        FeatureGridCard(
                            title = "Flashcard",
                            description = "Học thẻ ghi nhớ",
                            icon = Icons.Default.Style,
                            tintColor = Color(0xFF2563EB),
                            modifier = Modifier.weight(1f),
                            onClick = onStudyFlashcards
                        )
                        FeatureGridCard(
                            title = "Trắc Nghiệm",
                            description = "Luyện trắc nghiệm",
                            icon = Icons.Default.Quiz,
                            tintColor = Color(0xFFF97316),
                            modifier = Modifier.weight(1f),
                            onClick = onQuiz
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        FeatureGridCard(
                            title = "Tự Luận",
                            description = "Viết câu trả lời",
                            icon = Icons.Default.EditNote,
                            tintColor = Color(0xFF10B981),
                            modifier = Modifier.weight(1f),
                            onClick = onEssay
                        )
                        FeatureGridCard(
                            title = "Giải Trí",
                            description = "Nối từ, Đấu 1vs1",
                            icon = Icons.Default.VideogameAsset,
                            tintColor = Color(0xFF8B5CF6),
                            modifier = Modifier.weight(1f),
                            onClick = { showGameBottomSheet = true }
                        )
                    }
                }
            }

            // 3. Vocabulary List Title Section
            item {
                Text(
                    text = "Danh sách thẻ từ vựng (${cardsList.size} từ)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // 4. Vocabulary List Items Section
            items(cardsList) { card ->
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
                        // Left: Word + Reading Caption
                        Column(modifier = Modifier.weight(1.2f)) {
                            val cleanFront = card.question.substringBefore("(").trim()
                            val reading = if (card.question.contains("(")) {
                                "(" + card.question.substringAfter("(")
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

                        // Center: Vietnamese definition
                        Box(
                            modifier = Modifier.weight(1.2f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = card.answer,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Right: Actions (Speak + Star)
                        Row(
                            modifier = Modifier.weight(0.8f),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val wordToSpeak = card.question
                                    val cleanWord = wordToSpeak.substringBefore("(").trim()
                                    val speakLocale = when (studySet.languageCode.uppercase()) {
                                        "JA", "JAPANESE" -> java.util.Locale.JAPANESE
                                        "ZH", "CHINESE" -> java.util.Locale.CHINESE
                                        else -> java.util.Locale.US
                                    }
                                    tts?.language = speakLocale
                                    tts?.speak(cleanWord, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
                                },
                                enabled = isTtsReady,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Phát âm",
                                    tint = FlowPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            val isStarred = starredCardIds.contains(card.id)
                            IconButton(
                                onClick = {
                                    val favDocRef = firestore.collection("user_favorites").document("${uid}_${card.id}")
                                    if (isStarred) {
                                        favDocRef.update("starred", false)
                                    } else {
                                        val favData = hashMapOf(
                                            "uid" to uid,
                                            "vocabId" to card.id,
                                            "front" to card.question,
                                            "back" to card.answer,
                                            "levelId" to studySet.id,
                                            "starred" to true,
                                            "updatedAt" to com.google.firebase.Timestamp.now()
                                        )
                                        favDocRef.set(favData, com.google.firebase.firestore.SetOptions.merge())
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Đánh dấu sao",
                                    tint = if (isStarred) Color(0xFFFFD700) else Color.Gray,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 5. Comments Section Header
            item {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Bình luận (${comments.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // 6. Comment Input
            item {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newCommentText,
                        onValueChange = { newCommentText = it },
                        placeholder = { Text("Thêm bình luận...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FlowPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        ),
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newCommentText.isNotBlank()) {
                                onAddComment(newCommentText)
                                newCommentText = ""
                            }
                        },
                        modifier = Modifier.background(FlowPrimary, CircleShape)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Gửi", tint = Color.White)
                    }
                }
            }

            // 7. Comments List
            items(comments) { comment ->
                CommentItem(comment)
            }
        }
    }

    if (showGameBottomSheet) {
        GameSelectionBottomSheet(
            hasBattleFeature = true,
            onDismiss = { showGameBottomSheet = false },
            onPlayMatch = {
                showGameBottomSheet = false
                onPlayMatchGame()
            },
            onPlayBattle = {
                showGameBottomSheet = false
                onPlayBattle()
            },
            onJoinBattle = { code ->
                showGameBottomSheet = false
                onJoinBattle(code)
            },
            onJoinRandomBattle = {
                showGameBottomSheet = false
                onJoinRandomBattle()
            }
        )
    }
}

@Composable
fun FeatureGridCard(
    title: String,
    description: String,
    icon: ImageVector,
    tintColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = tintColor.copy(alpha = 0.15f)),
        border = BorderStroke(1.dp, tintColor.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(tintColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tintColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = tintColor,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = tintColor.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun CardPreviewItem(card: Flashcard) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(card.question, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(card.answer, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
            
            if (card.imageUrl != null) {
                Spacer(modifier = Modifier.height(12.dp))
                val imageModel = remember(card.imageUrl) { ImageUtils.getImageModel(card.imageUrl) }
                AsyncImage(
                    model = imageModel,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }
        }
    }
}

@Composable
fun CommentItem(comment: com.example.flashcards.model.Comment) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(comment.userName.take(1).uppercase(), color = FlowPrimary, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(comment.userName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(4.dp))
            Text(comment.content, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}
