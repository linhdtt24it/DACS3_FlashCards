package com.example.flashcards.view

import com.example.flashcards.utils.ContextUtils
import com.example.flashcards.R
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
fun StatisticsScreen(
    srViewModel: com.example.flashcards.viewmodel.SpacedRepetitionViewModel,
    userStats: com.example.flashcards.model.UserStats,
    onBack: () -> Unit
) {
    val totalCards by srViewModel.totalCards.collectAsState()
    val cardsStudiedToday by srViewModel.cardsStudiedToday.collectAsState()
    val heatmapData by srViewModel.heatmapData.collectAsState()
    val accuracyTrend by srViewModel.accuracyTrend.collectAsState()
    val timelineData by srViewModel.timelineDayCounts.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ui_text_28), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp),
            contentPadding = PaddingValues(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text(stringResource(R.string.ui_text_29), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Stat Cards Grid
            item {
                val totalAnswers = userStats.correctAnswers + userStats.wrongAnswers
                val accuracy = if (totalAnswers > 0) (userStats.correctAnswers * 100 / totalAnswers) else 0

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatCard(ContextUtils.getString(R.string.ui_text_438), "${userStats.streakDays} ngày", Icons.Default.LocalFireDepartment, Modifier.weight(1f), Color(0xFFFF5722))
                        StatCard(ContextUtils.getString(R.string.ui_text_439), "$accuracy%", Icons.Default.CheckCircle, Modifier.weight(1f), FlowSuccess)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatCard(ContextUtils.getString(R.string.ui_text_440), "$cardsStudiedToday thẻ", Icons.Default.Flag, Modifier.weight(1f), FlowPrimary)
                        StatCard(ContextUtils.getString(R.string.ui_text_441), "$totalCards thẻ", Icons.Default.LibraryBooks, Modifier.weight(1f), MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Heatmap
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(stringResource(R.string.ui_text_30), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            for (week in 0 until 5) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    for (day in 0 until 7) {
                                        val index = week * 7 + day
                                        val count = if (index < heatmapData.size) heatmapData[index] else 0
                                        val color = when {
                                            count == 0 -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                            count < 5 -> Color(0xFFC8E6C9)
                                            count < 10 -> Color(0xFF81C784)
                                            count < 20 -> Color(0xFF4CAF50)
                                            else -> Color(0xFF2E7D32)
                                        }
                                        Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)).background(color))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Review Forecast Bar Chart
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(20.dp).fillMaxSize()) {
                        Text(stringResource(R.string.ui_text_31), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        val maxCount = (timelineData.values.maxOrNull() ?: 10).coerceAtLeast(10)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            for (i in 1..7) {
                                val count = timelineData[i] ?: 0
                                val heightFactor = (count.toFloat() / maxCount).coerceIn(0.1f, 1f)
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.fillMaxHeight()) {
                                    Text("$count", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(28.dp)
                                            .fillMaxHeight(heightFactor)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(FlowPrimary.copy(alpha = 0.8f))
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("N+$i", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
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

