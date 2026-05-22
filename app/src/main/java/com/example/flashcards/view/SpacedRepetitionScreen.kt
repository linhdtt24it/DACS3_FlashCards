package com.example.flashcards.view

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.flashcards.model.Flashcard
import com.example.flashcards.ui.theme.*
import com.example.flashcards.utils.ImageUtils

/**
 * 2.1.2.5 – Smart Review with Spaced Repetition
 *
 * Displays all cards due today one by one.
 * After flipping the card, user selects memory level:
 *   - Again (quality=1)  → review interval decreases to 1 day
 *   - Good  (quality=3)  → review interval stays or increases slightly
 *   - Easy  (quality=5)  → review interval increases significantly
 * SM-2 algorithm in SpacedRepetition.kt calculates the next schedule.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpacedRepetitionScreen(
    dueCards: List<Flashcard>,
    onUpdateCard: (Flashcard, Int) -> Unit,
    onRecordSession: (cards: Int, correct: Int, wrong: Int) -> Unit,
    onBack: () -> Unit
) {
    val fc = LocalFlowColors.current
    val heroBrush = Brush.linearGradient(
        colors = listOf(fc.gradientStart, fc.gradientEnd),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    // Work-list: start with all due cards; "Again" re-queues card at end
    val workList = remember(dueCards) { dueCards.toMutableStateList() }
    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var correctCount by remember { mutableIntStateOf(0) }
    var wrongCount by remember { mutableIntStateOf(0) }
    var sessionDone by remember { mutableStateOf(false) }

    val total = remember(dueCards) { dueCards.size }
    val reviewed = currentIndex.coerceAtMost(total)
    val progress = if (total > 0) reviewed.toFloat() / total else 0f

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "flip"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Smart Review",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "$total cards due today",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = FlowPrimary,
                trackColor = MaterialTheme.colorScheme.surface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "$reviewed / $total",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (!sessionDone && currentIndex < workList.size) {
                val card = workList[currentIndex]
                val isBackVisible = rotation >= 90f

                // Flip card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .graphicsLayer {
                            rotationY = rotation
                            cameraDistance = 12f * density
                        }
                        .clickable { isFlipped = !isFlipped },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { if (isBackVisible) rotationY = 180f },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                        ) {
                            // Label badge
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                Surface(
                                    color = if (!isBackVisible)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        FlowSuccessLight,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        if (!isBackVisible) "QUESTION" else "ANSWER",
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                        color = if (!isBackVisible) FlowPrimary else FlowSuccess,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // Card image (front only)
                            if (!isBackVisible && card.imageUrl != null) {
                                val imageModel = remember(card.imageUrl) { ImageUtils.getImageModel(card.imageUrl) }
                                AsyncImage(
                                    model = imageModel,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .padding(bottom = 12.dp)
                                )
                            }

                            Text(
                                text = if (!isBackVisible) card.question else card.answer,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center
                            )

                            // Explanation on back
                            if (isBackVisible && card.explanation.isNotBlank()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        card.explanation,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // Hint text on front
                            if (!isBackVisible) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.TouchApp,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Tap to see answer",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        // Rating buttons (bottom of card, visible after flip)
                        if (isBackVisible) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // SM-2 interval hints
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    IntervalHintChip("Again", "< 1 day", FlowWarning)
                                    IntervalHintChip("Good", "≈ ${maxOf(1, card.interval)} days", FlowPrimary)
                                    IntervalHintChip("Easy", "≥ ${maxOf(3, (card.interval * card.easeFactor).toInt())} days", FlowSuccess)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Again / Quên  → quality 1
                                    Button(
                                        onClick = {
                                            onUpdateCard(card, 1)
                                            wrongCount++
                                            // Re-queue card at end so user sees it again
                                            workList.add(card)
                                            isFlipped = false
                                            currentIndex++
                                            if (currentIndex >= workList.size) {
                                                onRecordSession(total, correctCount, wrongCount)
                                                sessionDone = true
                                            }
                                        },
                                        modifier = Modifier.weight(1f).height(52.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = FlowWarningLight)
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("🔁 Again", color = FlowWarning, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }

                                    // Good → quality 3
                                    Button(
                                        onClick = {
                                            onUpdateCard(card, 3)
                                            correctCount++
                                            isFlipped = false
                                            currentIndex++
                                            if (currentIndex >= workList.size) {
                                                onRecordSession(total, correctCount, wrongCount)
                                                sessionDone = true
                                            }
                                        },
                                        modifier = Modifier.weight(1f).height(52.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                    ) {
                                        Text("👍 Good", color = FlowPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }

                                    // Easy → quality 5
                                    Button(
                                        onClick = {
                                            onUpdateCard(card, 5)
                                            correctCount++
                                            isFlipped = false
                                            currentIndex++
                                            if (currentIndex >= workList.size) {
                                                onRecordSession(total, correctCount, wrongCount)
                                                sessionDone = true
                                            }
                                        },
                                        modifier = Modifier.weight(1f).height(52.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = FlowSuccessLight)
                                    ) {
                                        Text("⚡ Easy", color = FlowSuccess, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

            } else {
                // ── Session Complete ──
                val accuracy = if (correctCount + wrongCount > 0)
                    (correctCount * 100) / (correctCount + wrongCount) else 0

                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(heroBrush),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎉", fontSize = 48.sp)
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Complete!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "You reviewed $total cards today",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))

                // Result chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                ) {
                    ResultChip("Correct", correctCount.toString(), FlowSuccess, FlowSuccessLight)
                    ResultChip("Wrong / Again", wrongCount.toString(), FlowWarning, FlowWarningLight)
                    ResultChip("Accuracy", "$accuracy%", FlowPrimary, MaterialTheme.colorScheme.primaryContainer)
                }

                Spacer(modifier = Modifier.height(40.dp))
                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(heroBrush, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Go Back", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun IntervalHintChip(label: String, interval: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
        Text(interval, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ResultChip(label: String, value: String, textColor: Color, bgColor: Color) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bgColor
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = textColor)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}