package com.example.flashcards.view

import com.example.flashcards.utils.ContextUtils
import com.example.flashcards.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.flashcards.viewmodel.AdminEnglishVocabViewModel
import com.example.flashcards.viewmodel.EnglishCategory
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEnglishVocabMenuScreen(
    navController: NavController,
    viewModel: AdminEnglishVocabViewModel = viewModel()
) {
    val groupedCategories by viewModel.groupedCategories.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var selectedCategory by remember { mutableStateOf<EnglishCategory?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ContextUtils.getString(R.string.ui_text_111), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            BottomAppBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(ContextUtils.getString(R.string.ui_text_112))
                    }

                    Button(
                        onClick = { 
                            selectedCategory?.let { viewModel.deleteCategory(it.id) }
                            selectedCategory = null
                        },
                        enabled = selectedCategory != null,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(ContextUtils.getString(R.string.ui_text_20), color = Color.White)
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    groupedCategories.forEach { group ->
                        item {
                            Text(
                                text = group.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 4.dp)
                            )
                        }
                        items(group.items) { category ->
                            val isSelected = selectedCategory?.id == category.id
                            val safeCode = category.code.ifEmpty { "empty" }
                            val safeName = URLEncoder.encode(category.name.ifEmpty { "English" }, "UTF-8")

                            Card(
                                onClick = { 
                                    if (isSelected) {
                                        if (safeCode.isNotEmpty()) {
                                            navController.navigate("english_card_editor/$safeCode/$safeName")
                                        }
                                    } else {
                                        selectedCategory = category
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = category.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                                    )
                                }
                            }
                        }
                    }

                    if (groupedCategories.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillParentMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    ContextUtils.getString(R.string.ui_text_114),
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var newCategoryName by remember { mutableStateOf("") }
        var newCategoryCode by remember { mutableStateOf("") }
        var selectedGroupId by remember { mutableStateOf("") }
        var selectedGroupName by remember { mutableStateOf("") }
        
        val groups = listOf(
            "BASIC" to ContextUtils.getString(R.string.ui_text_115),
            "TOEIC" to "TOEIC",
            "IELTS" to "IELTS",
            "CAMBRIDGE" to "CAMBRIDGE"
        )

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(ContextUtils.getString(R.string.ui_text_116), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(ContextUtils.getString(R.string.ui_text_117), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    groups.forEach { (id, name) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { 
                                selectedGroupId = id
                                selectedGroupName = name
                            }
                        ) {
                            RadioButton(selected = selectedGroupId == id, onClick = { 
                                selectedGroupId = id
                                selectedGroupName = name
                            })
                            Text(name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = newCategoryName, onValueChange = { newCategoryName = it }, label = { Text(ContextUtils.getString(R.string.ui_text_118)) }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = newCategoryCode, onValueChange = { newCategoryCode = it }, label = { Text(ContextUtils.getString(R.string.ui_text_119)) }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newCategoryName.isNotBlank() && newCategoryCode.isNotBlank() && selectedGroupId.isNotBlank()) {
                        viewModel.addCategory(selectedGroupId, selectedGroupName, newCategoryName, newCategoryCode)
                        showAddDialog = false
                    }
                }) { Text(ContextUtils.getString(R.string.ui_text_112)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text(ContextUtils.getString(R.string.ui_text_21)) }
            }
        )
    }
}
