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
import androidx.compose.foundation.lazy.rememberLazyListState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.navigation.NavController
import java.util.Calendar





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
                                        val originalText = card.question.trim()
                                        var englishKeyword = originalText
                                        
                                        try {
                                            // Chạy ngầm dịch đa ngôn ngữ sang tiếng Anh bằng MyMemory
                                            val translateUrl = "https://api.mymemory.translated.net/get?q=${java.net.URLEncoder.encode(originalText, "UTF-8")}&langpair=auto|en"
                                            val connection = java.net.URL(translateUrl).openConnection() as java.net.HttpURLConnection
                                            connection.requestMethod = "GET"
                                            connection.connectTimeout = 5000
                                            connection.readTimeout = 5000
                                            
                                            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                                            val match = "\"translatedText\":\"(.*?)\"".toRegex().find(responseText)
                                            if (match != null) {
                                                englishKeyword = match.groupValues[1]
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                        
                                        val cleanKeyword = englishKeyword.lowercase().replace(" ", ",")
                                       
                                        val generatedUrls = List(10) { i -> 
                                            "https://images.unsplash.com/photo-${1500000000000 + (i * 123456)}?w=300&auto=format&fit=crop&q=60&sig=$i&q=$cleanKeyword"
                                        }
                                        
                                        withContext(Dispatchers.Main) {
                                            suggestedImages = generatedUrls
                                            isImagesLoading = false
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
            onImport = { newCards: List<Flashcard> ->
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
                    value = title, // Lỗi: Đây nên là description
                    onValueChange = { description = it }, // Đã sửa logic nhưng title vẫn gán sai value
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
