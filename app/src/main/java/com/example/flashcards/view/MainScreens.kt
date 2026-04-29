package com.example.flashcards.view

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.flashcards.ui.theme.*
import com.example.flashcards.utils.ImageUtils
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.border

@Composable
fun AddDeckDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var newTitle by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Deck", fontWeight = FontWeight.Bold, color = FlowTextPrimary) },
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
            TextButton(onClick = onDismiss) { Text("Cancel", color = FlowTextSecondary) }
        },
        containerColor = FlowSurface
    )
}

@Composable
fun HomeScreen(userName: String, studySets: List<StudySet>, onAddDeck: (String, String) -> Unit, onEditDeck: (String) -> Unit, onDeleteDeck: (String) -> Unit, onSetSelected: (StudySet) -> Unit) {
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddDeckDialog(onDismiss = { showAddDialog = false }, onSave = onAddDeck)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(FlowBackground).padding(24.dp),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    val firstName = userName.split(" ").firstOrNull()?.ifEmpty { "User" } ?: "User"
                    Text("Welcome back, $firstName", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = FlowTextPrimary)
                    Text("You have ${studySets.size} decks to review.\nReady to achieve flow?", style = MaterialTheme.typography.bodyMedium, color = FlowTextSecondary)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary)) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add New Deck", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }

        item {
            OutlinedTextField(
                value = "", onValueChange = {},
                placeholder = { Text("Search your library...", color = FlowTextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = FlowTextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = FlowCardStroke, focusedBorderColor = FlowPrimary, unfocusedContainerColor = FlowSurface, focusedContainerColor = FlowSurface)
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = FlowSurface), border = BorderStroke(1.dp, FlowCardStroke)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Weekly Streak", style = MaterialTheme.typography.labelSmall, color = FlowTextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("12 Days", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = FlowTextPrimary)
                    }
                }
                Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = FlowPrimary)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Daily Mastery", style = MaterialTheme.typography.labelSmall, color = FlowPrimaryLight)
                        Text("84% Correct", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        item {
            Text("Recent Decks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = FlowTextPrimary)
        }

        items(studySets) { set ->
            LibraryDeckCard(set, onClick = { onSetSelected(set) }, onEdit = { onEditDeck(set.id) }, onDelete = { onDeleteDeck(set.id) })
        }
    }
}

@Composable
fun LibraryScreen(studySets: List<StudySet>, onAddDeck: (String, String) -> Unit, onEditDeck: (String) -> Unit, onDeleteDeck: (String) -> Unit, onSetSelected: (StudySet) -> Unit) {
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddDeckDialog(onDismiss = { showAddDialog = false }, onSave = onAddDeck)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = FlowPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Deck")
            }
        },
        containerColor = FlowBackground
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            Text("Your Library", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = FlowTextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Text("All your study sets in one place.", style = MaterialTheme.typography.bodyMedium, color = FlowTextSecondary)
            Spacer(modifier = Modifier.height(24.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(studySets) { set ->
                    LibraryDeckCard(set, onClick = { onSetSelected(set) }, onEdit = { onEditDeck(set.id) }, onDelete = { onDeleteDeck(set.id) })
                }
            }
        }
    }
}

@Composable
fun LibraryDeckCard(set: StudySet, onClick: () -> Unit, onEdit: (() -> Unit)? = null, onDelete: (() -> Unit)? = null) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Deck?", fontWeight = FontWeight.Bold, color = FlowTextPrimary) },
            text = { Text("Are you sure you want to delete '${set.title}'? This action cannot be undone.", color = FlowTextSecondary) },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete?.invoke() }) { Text("Delete", fontWeight = FontWeight.Bold, color = FlowWarning) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel", color = FlowTextSecondary) }
            },
            containerColor = FlowSurface
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FlowSurface),
        border = BorderStroke(1.dp, FlowCardStroke)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).background(FlowPrimaryLight, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = FlowPrimary, modifier = Modifier.size(24.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = FlowPrimaryLight.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp)) {
                        Text("Due", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = FlowPrimary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    if (onEdit != null) {
                        IconButton(onClick = { onEdit.invoke() }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = FlowTextSecondary)
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
            Text(set.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = FlowTextPrimary)
            Text("${set.cards.size} cards", style = MaterialTheme.typography.bodySmall, color = FlowTextSecondary)
            
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Mastery", style = MaterialTheme.typography.labelSmall, color = FlowTextSecondary)
                Text("45%", style = MaterialTheme.typography.labelSmall, color = FlowTextPrimary, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(progress = { 0.45f }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), color = FlowPrimary, trackColor = FlowBackground)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudySessionScreen(
    studySet: StudySet,
    onBack: () -> Unit,
    onSpeak: (String) -> Unit,
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
                    IconButton(onClick = onBack) { Icon(Icons.Default.Close, contentDescription = null, tint = FlowTextPrimary) }
                },
                actions = {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.width(200.dp).height(8.dp).clip(CircleShape), color = FlowPrimary, trackColor = FlowBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FlowBackground)
            )
        },
        bottomBar = {
            if (currentIndex < studySet.cards.size) {
                Surface(
                    shadowElevation = 8.dp,
                    color = FlowBackground
                ) {
                    BottomAppBar(containerColor = Color.Transparent, tonalElevation = 0.dp) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (currentIndex > 0) { currentIndex--; isFlipped = false } }, enabled = currentIndex > 0) { 
                            Icon(Icons.Default.ArrowBack, contentDescription = "Previous", tint = if (currentIndex > 0) FlowTextPrimary else FlowTextSecondary) 
                        }
                        Text("Card: ${currentIndex + 1}/${studySet.cards.size}", color = FlowTextSecondary, fontWeight = FontWeight.Medium)
                        IconButton(onClick = { if (currentIndex < studySet.cards.size - 1) { currentIndex++; isFlipped = false } }, enabled = currentIndex < studySet.cards.size - 1) { 
                            Icon(Icons.Default.ArrowForward, contentDescription = "Next", tint = if (currentIndex < studySet.cards.size - 1) FlowTextPrimary else FlowTextSecondary) 
                        }
                    }
                }
            }
        }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().background(FlowBackground).padding(padding).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("${studySet.title} Mastery", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = FlowTextPrimary)
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
                    colors = CardDefaults.cardColors(containerColor = FlowSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    val isBackVisible = rotation >= 90f
                    Box(modifier = Modifier.fillMaxSize().graphicsLayer { if (isBackVisible) rotationY = 180f }, contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Spacer(modifier = Modifier.size(48.dp)) // Placeholder for balance
                                Surface(color = FlowPrimaryLight, shape = RoundedCornerShape(8.dp)) {
                                    Text(if (!isBackVisible) "QUESTION" else "ANSWER", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = FlowPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                }
                                IconButton(onClick = { onSpeak(if (!isBackVisible) card.question else card.answer) }) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "Speak", tint = FlowPrimary)
                                }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            if (!isBackVisible && card.imageUrl != null) {
                                AsyncImage(
                                    model = card.imageUrl, 
                                    contentDescription = null, 
                                    contentScale = ContentScale.Fit, 
                                    modifier = Modifier.fillMaxWidth().height(160.dp).padding(bottom = 16.dp)
                                )
                            }
                            Text(
                                if (!isBackVisible) card.question else card.answer, 
                                style = MaterialTheme.typography.headlineMedium, 
                                fontWeight = FontWeight.Bold, 
                                color = FlowTextPrimary,
                                textAlign = TextAlign.Center
                            )
                            if (isBackVisible && card.explanation.isNotBlank()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Surface(color = FlowPrimaryLight.copy(alpha = 0.3f), shape = RoundedCornerShape(12.dp)) {
                                    Text(
                                        card.explanation,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = FlowTextSecondary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            
                            if (!isBackVisible) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.TouchApp, contentDescription = null, tint = FlowTextSecondary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Tap to reveal answer", color = FlowTextSecondary, fontWeight = FontWeight.Medium)
                                }
                            } else {
                                Spacer(modifier = Modifier.height(48.dp))
                            }
                        }
                        
                        // Action buttons overlay when flipped
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
                                        colors = ButtonDefaults.buttonColors(containerColor = FlowPrimaryLight)
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
                Text("Session Complete!", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, color = FlowTextPrimary)
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

@Composable
fun StatisticsScreen() {
    Column(modifier = Modifier.fillMaxSize().background(FlowBackground).padding(24.dp)) {
        Text("Learning Progress", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = FlowTextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Your cognitive flow is at its peak. Keep the momentum going!", style = MaterialTheme.typography.bodyMedium, color = FlowTextSecondary)
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard("Current Streak", "14", Icons.Default.DateRange, Modifier.weight(1f), FlowPrimary)
            StatCard("Accuracy", "92%", Icons.Default.CheckCircle, Modifier.weight(1f), FlowSuccess)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        StatCard("Daily Goal", "42/50", Icons.Default.Flag, Modifier.fillMaxWidth(), FlowPrimary)
    }
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier, iconColor: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FlowSurface),
        border = BorderStroke(1.dp, FlowCardStroke)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Icon(icon, contentDescription = null, tint = iconColor)
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = FlowTextPrimary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = FlowTextSecondary)
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FlowSurface),
        border = BorderStroke(1.dp, FlowCardStroke)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(index.toString(), fontWeight = FontWeight.Bold, color = FlowTextPrimary, fontSize = 18.sp)
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
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (card.imageUrl != null) {
                        AsyncImage(model = card.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = FlowPrimary, modifier = Modifier.size(24.dp))
                            Text("Image", color = FlowPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckEditorScreen(
    studySet: StudySet,
    onSave: (StudySet) -> Unit,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf(studySet.title) }
    var description by remember { mutableStateOf(studySet.description) }
    var cards by remember { mutableStateOf(studySet.cards) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Deck", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    TextButton(onClick = {
                        if (title.isNotBlank()) {
                            onSave(studySet.copy(title = title, description = description, cards = cards))
                            onBack()
                        }
                    }) {
                        Text("Save", color = FlowPrimary, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FlowBackground)
            )
        },
        containerColor = FlowBackground
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
                    colors = ButtonDefaults.buttonColors(containerColor = FlowPrimaryLight)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = FlowPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Flashcard", color = FlowPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
