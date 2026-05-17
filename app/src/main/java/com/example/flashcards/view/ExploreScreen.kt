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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flashcards.model.StudySet
import com.example.flashcards.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    publicDecks: List<StudySet>,
    onSearch: (String) -> Unit,
    onImportDeck: (String) -> Unit,
    onRateDeck: (String, Float) -> Unit,
    onDeckClick: (StudySet) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    // Auto-search when query changes
    LaunchedEffect(searchQuery) {
        onSearch(searchQuery)
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(top = 24.dp)) {
        // Header
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Explore", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        // Search Bar
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search public decks...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent, 
                    focusedBorderColor = Color.Transparent, 
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface, 
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // List of Public Decks
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(publicDecks) { set ->
                PublicDeckCard(
                    set = set,
                    onClick = { onDeckClick(set) },
                    onImportClick = { onImportDeck(set.shareCode ?: "") },
                    onRateClick = { rating -> onRateDeck(set.id, rating) }
                )
            }
        }
    }
}

@Composable
fun PublicDeckCard(set: StudySet, onClick: () -> Unit, onImportClick: () -> Unit, onRateClick: (Float) -> Unit) {
    var showRatingDialog by remember { mutableStateOf(false) }

    if (showRatingDialog) {
        var userRating by remember { mutableFloatStateOf(5f) }
        AlertDialog(
            onDismissRequest = { showRatingDialog = false },
            title = { Text("Rate Deck", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("How would you rate '${set.title}'?", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.Center) {
                        for (i in 1..5) {
                            Icon(
                                imageVector = if (i <= userRating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Star $i",
                                tint = if (i <= userRating) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable { userRating = i.toFloat() }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onRateClick(userRating); showRatingDialog = false }) { Text("Submit", color = FlowPrimary) }
            },
            dismissButton = {
                TextButton(onClick = { showRatingDialog = false }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
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
                    Icon(Icons.Default.Public, contentDescription = null, tint = FlowPrimary, modifier = Modifier.size(24.dp))
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), 
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.clickable { showRatingDialog = true }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(String.format("%.1f", set.rating), color = FlowPrimary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text(" (${set.ratingCount})", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(set.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text("by ${set.creatorName}", style = MaterialTheme.typography.bodyMedium, color = FlowPrimary)
            if (set.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(set.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${set.cards.size} terms", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                Button(
                    onClick = onImportClick,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}