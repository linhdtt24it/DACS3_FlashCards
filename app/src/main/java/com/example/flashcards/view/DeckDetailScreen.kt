package com.example.flashcards.view

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.flashcards.model.Flashcard
import com.example.flashcards.model.StudySet
import com.example.flashcards.ui.theme.*
import com.example.flashcards.utils.ImageUtils

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
    onMatch: () -> Unit,
    onEditDeck: () -> Unit,
    onDeleteDeck: () -> Unit
) {
    var showMenu by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var newCommentText by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }

    val isOwner = currentUserId == studySet.creatorId || studySet.creatorId.isEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = FlowTextPrimary) }
                },
                actions = {
                    IconButton(onClick = { }) { Icon(Icons.Default.BookmarkBorder, contentDescription = "Bookmark", tint = FlowTextPrimary) }
                    if (isOwner) {
                        Box {
                            IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, contentDescription = "More", tint = FlowTextPrimary) }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Edit Set") },
                                    onClick = { showMenu = false; onEditDeck() },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete Set", color = FlowWarning) },
                                    onClick = { showMenu = false; onDeleteDeck() },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = FlowWarning) }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FlowBackground)
            )
        },
        bottomBar = {
            Surface(
                color = FlowBackground,
                shadowElevation = 16.dp
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = onStudyFlashcards,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary)
                    ) {
                        Text("Study this set", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    }
                }
            }
        },
        containerColor = FlowBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            item {
                Text(studySet.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = FlowTextPrimary)
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(FlowPrimaryLight), contentAlignment = Alignment.Center) {
                        Text(userName.take(1).uppercase(), color = FlowPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(userName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = FlowTextPrimary)
                    Text("  |  ${studySet.cards.size} terms", style = MaterialTheme.typography.bodyMedium, color = FlowTextSecondary)
                    if (studySet.isPublic) {
                        Text("  |  ⭐ ${String.format("%.1f", studySet.rating)} (${studySet.ratingCount})", style = MaterialTheme.typography.bodyMedium, color = FlowTextSecondary)
                    }
                }
                if (studySet.isPublic && studySet.shareCode != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = FlowPrimaryLight), shape = RoundedCornerShape(8.dp)) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = FlowPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share Code: ${studySet.shareCode}", color = FlowPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Features List
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FeatureCard("Flashcards", Icons.Default.Style, onStudyFlashcards)
                    FeatureCard("Learn", Icons.Default.Autorenew, onStudyFlashcards)
                    FeatureCard("Test", Icons.Default.FactCheck, onQuiz)
                    FeatureCard("Match", Icons.Default.DashboardCustomize, onMatch)
                }
            }

            // Cards Preview Header
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Cards", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = FlowTextPrimary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Original order", style = MaterialTheme.typography.bodyMedium, color = FlowTextPrimary, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Sort, contentDescription = null, tint = FlowTextPrimary)
                    }
                }
            }

            // Cards Preview List
            items(studySet.cards) { card ->
                CardPreviewItem(card)
            }
            
            // Comments Section Header
            item {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = FlowSurface)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Comments (${comments.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = FlowTextPrimary)
            }

            // Comment Input
            item {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newCommentText,
                        onValueChange = { newCommentText = it },
                        placeholder = { Text("Add a comment...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FlowPrimary,
                            unfocusedBorderColor = FlowSurface
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
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                    }
                }
            }

            // Comments List
            items(comments) { comment ->
                CommentItem(comment)
            }
        }
    }
}

@Composable
fun FeatureCard(title: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FlowSurface)
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = FlowPrimary, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = FlowTextPrimary)
        }
    }
}

@Composable
fun CardPreviewItem(card: Flashcard) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FlowSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(card.question, style = MaterialTheme.typography.titleMedium, color = FlowTextPrimary, modifier = Modifier.weight(1f))
                Row {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = FlowTextSecondary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(Icons.Default.StarBorder, contentDescription = null, tint = FlowTextSecondary)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(card.answer, style = MaterialTheme.typography.bodyLarge, color = FlowTextPrimary)
            
            if (card.imageUrl != null) {
                Spacer(modifier = Modifier.height(16.dp))
                val imageModel = androidx.compose.runtime.remember(card.imageUrl) { ImageUtils.getImageModel(card.imageUrl) }
                AsyncImage(
                    model = imageModel,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(8.dp))
                )
            }
        }
    }
}

@Composable
fun CommentItem(comment: com.example.flashcards.model.Comment) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(FlowPrimaryLight),
            contentAlignment = Alignment.Center
        ) {
            Text(comment.userName.take(1).uppercase(), color = FlowPrimary, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(comment.userName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = FlowTextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(comment.content, style = MaterialTheme.typography.bodyMedium, color = FlowTextPrimary)
        }
    }
}
