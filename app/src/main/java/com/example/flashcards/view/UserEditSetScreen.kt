package com.example.flashcards.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flashcards.model.Flashcard
import com.example.flashcards.model.StudySet
import com.example.flashcards.ui.theme.FlowPrimary
import androidx.compose.ui.text.input.TextFieldValue
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserEditSetScreen(
    studySet: StudySet,
    onSave: (StudySet) -> Unit,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf(TextFieldValue(studySet.title)) }
    var description by remember { mutableStateOf(TextFieldValue(studySet.description)) }
    var isPublic by remember { mutableStateOf(studySet.isPublic) }
    val cardsList = remember { studySet.cards.toMutableStateList() }
    var showImportDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chỉnh sửa bộ thẻ", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    cardsList.add(Flashcard(id = UUID.randomUUID().toString(), question = "", answer = ""))
                },
                containerColor = FlowPrimary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Term", modifier = Modifier.size(32.dp))
            }
        },
        bottomBar = {
            BottomAppBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (title.text.isNotBlank()) {
                                onSave(studySet.copy(
                                    title = title.text,
                                    description = description.text,
                                    cards = cardsList.filter { it.question.isNotBlank() || it.answer.isNotBlank() },
                                    isPublic = isPublic
                                ))
                                onBack()
                            }
                        },
                        enabled = title.text.isNotBlank(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text("Cập nhật bộ thẻ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tên bộ thẻ") },
                    placeholder = { Text("Nhập tên bộ thẻ...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FlowPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Mô tả ngắn") },
                    placeholder = { Text("Nhập mô tả bộ thẻ...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FlowPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Bộ thẻ công khai", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Text("Mọi người đều có thể tìm thấy và học bộ thẻ này", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = isPublic,
                        onCheckedChange = { isPublic = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = FlowPrimary, checkedTrackColor = FlowPrimary.copy(alpha = 0.5f))
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Thuật ngữ trong bộ thẻ (${cardsList.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    Button(
                        onClick = { showImportDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Nhập hàng loạt bằng văn bản 📋", color = FlowPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            itemsIndexed(cardsList) { index, card ->
                CardInputRow(
                    index = index,
                    card = card,
                    onDelete = { cardsList.removeAt(index) },
                    onCardChange = { newQ, newA ->
                        cardsList[index] = cardsList[index].copy(question = newQ, answer = newA)
                    }
                )
            }
        }
    }

    if (showImportDialog) {
        BulkImportDialog(
            onDismiss = { showImportDialog = false },
            onImport = { parsed: List<Pair<String, String>> ->
                parsed.forEach { pair ->
                    cardsList.add(Flashcard(id = UUID.randomUUID().toString(), question = pair.first, answer = pair.second))
                }
            }
        )
    }
}
