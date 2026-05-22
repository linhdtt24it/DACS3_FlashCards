package com.example.flashcards.view

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
    userStats: UserStats,  // ← THÊM DÒNG NÀY
    unreadNotifCount: Int = 0,
    onAddDeck: (String, String) -> Unit,
    onEditDeck: (String) -> Unit,
    onDeleteDeck: (String) -> Unit,
    onSetSelected: (StudySet) -> Unit,
    onQuizDeck: (StudySet) -> Unit,
    onNotificationsClick: () -> Unit = {}
) {
    val fc = LocalFlowColors.current
    val heroBrush = Brush.linearGradient(
        colors = listOf(fc.gradientStart, fc.gradientEnd),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 16.dp, vertical = 24.dp),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = "", onValueChange = {},
                    placeholder = { Text("Search", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.Transparent, focusedBorderColor = Color.Transparent, unfocusedContainerColor = MaterialTheme.colorScheme.surface, focusedContainerColor = MaterialTheme.colorScheme.surface)
                )
                Box(modifier = Modifier.size(48.dp)) {
                    IconButton(onClick = onNotificationsClick, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    if (unreadNotifCount > 0) {
                        Box(
                            modifier = Modifier.align(Alignment.TopEnd).size(12.dp).clip(CircleShape).background(FlowWarning),
                            contentAlignment = Alignment.Center
                        ) {}
                    }
                }
                Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(FlowPrimary), contentAlignment = Alignment.Center) {
                    Text(userName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            }
        }

        // --- STREAK SECTION ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(50.dp).clip(CircleShape).background(FlowWarning.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = FlowWarning, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("${userStats.streakDays} Day Streak", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("You've studied ${userStats.cardsStudiedToday} cards today", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (studySets.isNotEmpty()) {
            item {
                Text("Continue studying", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(16.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(studySets.take(3)) { set ->
                        Card(
                            modifier = Modifier.width(300.dp).clickable { onSetSelected(set) },
                            shape = RoundedCornerShape(24.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                // Gradient strip at top
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .background(heroBrush)
                                )
                                Column(modifier = Modifier.padding(24.dp)) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                "${set.cards.size} terms",
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = FlowPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Icon(if (set.isPublic) Icons.Default.Public else Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(set.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, maxLines = 2)
                                    Spacer(modifier = Modifier.height(28.dp))
                                    Button(
                                        onClick = { onSetSelected(set) },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(24.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize().background(heroBrush, RoundedCornerShape(24.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("Continue", fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Text("Recent", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        }

        items(studySets) { set ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onSetSelected(set) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(set.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = if (set.isPublic) Icons.Default.Public else Icons.Default.Lock,
                                contentDescription = if (set.isPublic) "Public" else "Private",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text("${set.cards.size} cards • author: ${if (set.creatorName.isNotBlank()) set.creatorName else "you"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun LibraryScreen(
    studySets: List<StudySet>,
    folders: List<com.example.flashcards.model.Folder> = emptyList(),
    onAddDeck: (String, String) -> Unit,
    onEditDeck: (String) -> Unit,
    onDeleteDeck: (String) -> Unit,
    onSetSelected: (StudySet) -> Unit,
    onQuizDeck: (StudySet) -> Unit,
    onImportDeck: (String) -> Unit,
    onCreateFolder: (String, String) -> Unit = {
    _, _ -> },
    onFolderClick: (com.example.flashcards.model.Folder) -> Unit = {}
) {
    var showImportDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreateFolder by remember { mutableStateOf(false) }

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

    val tabs = listOf("Sets", "Folders", "Classes")

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(top = 24.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Library", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Row {
                if (selectedTab == 1) {
                    IconButton(onClick = { showCreateFolder = true }) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder", tint = FlowPrimary)
                    }
                }
                IconButton(onClick = { showImportDialog = true }) {
                    Icon(Icons.Default.Download, contentDescription = "Import", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = FlowPrimary,
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        height = 3.dp,
                        color = FlowPrimary
                    )
                }
            },
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) FlowPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (selectedTab) {
            0 -> SetsTabContent(studySets, onSetSelected)
            1 -> FoldersTabContent(folders, onFolderClick) { showCreateFolder = true }
            2 -> ClassesPlaceholder()
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
    onSpeak: (String, String) -> Unit,  // 👈 GIỮ của bạn tôi (thêm languageCode)
    onUpdateCard: (Flashcard, Int) -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    val progress = if (studySet.cards.isNotEmpty()) (currentIndex.toFloat() / studySet.cards.size) else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground) }
                },
                actions = {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.width(200.dp).height(8.dp).clip(CircleShape), color = FlowPrimary, trackColor = MaterialTheme.colorScheme.background)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            if (currentIndex < studySet.cards.size) {
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.background
                ) {
                    BottomAppBar(containerColor = Color.Transparent, tonalElevation = 0.dp) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (currentIndex > 0) { currentIndex--; isFlipped = false } }, enabled = currentIndex > 0) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Previous", tint = if (currentIndex > 0) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("Card: ${currentIndex + 1}/${studySet.cards.size}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                            IconButton(onClick = { if (currentIndex < studySet.cards.size - 1) { currentIndex++; isFlipped = false } }, enabled = currentIndex < studySet.cards.size - 1) {
                                Icon(Icons.Default.ArrowForward, contentDescription = "Next", tint = if (currentIndex < studySet.cards.size - 1) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("${studySet.title} Mastery", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(24.dp))

            if (currentIndex < studySet.cards.size) {
                val card = studySet.cards[currentIndex]

                val rotation by animateFloatAsState(
                    targetValue = if (isFlipped) 180f else 0f,
                    animationSpec = tween(durationMillis = 400), label = "flip"
                )

                Card(
                    modifier = Modifier.fillMaxWidth().weight(1f)
                        .graphicsLayer {
                            rotationY = rotation
                            cameraDistance = 12f * density
                        }
                        .clickable { isFlipped = !isFlipped },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    val isBackVisible = rotation >= 90f
                    Box(modifier = Modifier.fillMaxSize().graphicsLayer { if (isBackVisible) rotationY = 180f }, contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Spacer(modifier = Modifier.size(48.dp))
                                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp)) {
                                    Text(if (!isBackVisible) "QUESTION" else "ANSWER", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = FlowPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                }
                                // 👈 GIỮ của bạn tôi: truyền thêm languageCode
                                IconButton(onClick = { onSpeak(if (!isBackVisible) card.question else card.answer, studySet.languageCode) }) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "Speak", tint = FlowPrimary)
                                }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            if (!isBackVisible && card.imageUrl != null) {
                                val imageModel = remember(card.imageUrl) { ImageUtils.getImageModel(card.imageUrl) } // 👈 GIỮ của bạn
                                AsyncImage(
                                    model = imageModel,
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxWidth().height(160.dp).padding(bottom = 16.dp)
                                )
                            }
                            Text(
                                if (!isBackVisible) card.question else card.answer,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center
                            )
                            if (isBackVisible && card.explanation.isNotBlank()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), shape = RoundedCornerShape(12.dp)) {
                                    Text(
                                        card.explanation,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.weight(1f))

                            if (!isBackVisible) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.TouchApp, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Tap to reveal answer", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                }
                            } else {
                                Spacer(modifier = Modifier.height(48.dp))
                            }
                        }

                        if (isBackVisible) {
                            Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(
                                        onClick = { onUpdateCard(card, 1); isFlipped = false; currentIndex++ },
                                        modifier = Modifier.weight(1f).height(52.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = FlowWarningLight)
                                    ) {
                                        Text("Hard", color = FlowWarning, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { onUpdateCard(card, 3); isFlipped = false; currentIndex++ },
                                        modifier = Modifier.weight(1f).height(52.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                    ) {
                                        Text("Good", color = FlowPrimary, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { onUpdateCard(card, 5); isFlipped = false; currentIndex++ },
                                        modifier = Modifier.weight(1f).height(52.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = FlowSuccessLight)
                                    ) {
                                        Text("Easy", color = FlowSuccess, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

            } else {
                Text("Session Complete!", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary)
                ) {
                    Text("Return to Library", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
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
                                        try {
                                            val keywordEncoded = java.net.URLEncoder.encode(card.question.trim(), "UTF-8")
                                            // 🌟 FIX CHÍ MẠNG 1: Sửa đoạn đuôi thành keywordEncoded chuẩn xác
                                            val serverUrl = "http://10.0.2.2:8080/api/suggest-images?keyword=$keywordEncoded"
                                            
                                            val connection = java.net.URL(serverUrl).openConnection() as java.net.HttpURLConnection
                                            connection.requestMethod = "GET"
                                            connection.connectTimeout = 5000
                                            connection.readTimeout = 5000
                                            
                                            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                                            
                                            // 🌟 FIX CHÍ MẠNG 2: Dùng bộ bóc tách chuỗi URL sạch, chấp nhận mọi kiểu khoảng trắng của JSON
                                            val imageUrlList = mutableListOf<String>()
                                            val regex = "\"images\"\\s*:\\s*\\[(.*?)\\]".toRegex(kotlin.text.RegexOption.DOT_MATCHES_ALL)
                                            val match = regex.find(responseText)
                                            
                                            match?.let {
                                                val arrayContent = it.groupValues[1]
                                                if (arrayContent.isNotBlank()) {
                                                    // Tách các link và làm sạch dấu ngoặc kép, dấu xuyệt ngược
                                                    val links = arrayContent.split(",")
                                                    for (link in links) {
                                                        val cleanLink = link.replace("\"", "").replace("\\", "").trim()
                                                        if (cleanLink.startsWith("http")) {
                                                            imageUrlList.add(cleanLink)
                                                        }
                                                    }
                                                }
                                            }
                                            
                                            withContext(Dispatchers.Main) {
                                                suggestedImages = imageUrlList
                                                isImagesLoading = false
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            withContext(Dispatchers.Main) {
                                                suggestedImages = emptyList()
                                                isImagesLoading = false
                                            }
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
            onImport = { newCards ->
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
                    value = description,
                    onValueChange = { description = it },
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