package com.example.flashcards.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flashcards.model.StudySet
import com.example.flashcards.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    studySet: StudySet,
    onBack: () -> Unit
) {
    if (studySet.cards.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().background(FlowBackground), contentAlignment = Alignment.Center) {
            Text("This deck has no cards.", color = FlowTextPrimary)
            Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) {
                Text("Go Back")
            }
        }
        return
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableStateOf<String?>(null) }

    // Generate options for the current question
    val options = remember(currentIndex) {
        if (currentIndex < studySet.cards.size) {
            val currentCard = studySet.cards[currentIndex]
            val allAnswers = studySet.cards.map { it.answer }.filter { it.isNotBlank() }.distinct()
            val distractors = allAnswers.filter { it != currentCard.answer }.shuffled().take(3)
            (distractors + currentCard.answer).shuffled()
        } else {
            emptyList()
        }
    }

    val progress = if (studySet.cards.isNotEmpty()) (currentIndex.toFloat() / studySet.cards.size) else 0f
    val isFinished = currentIndex >= studySet.cards.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quiz Mode", fontWeight = FontWeight.Bold, color = FlowTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.Close, contentDescription = "Close", tint = FlowTextPrimary) }
                },
                actions = {
                    if (!isFinished) {
                        Text("${currentIndex + 1}/${studySet.cards.size}", modifier = Modifier.padding(end = 16.dp), color = FlowTextSecondary, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FlowBackground)
            )
        },
        containerColor = FlowBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isFinished) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = FlowPrimary,
                    trackColor = FlowCardStroke
                )
                Spacer(modifier = Modifier.height(32.dp))

                val currentCard = studySet.cards[currentIndex]

                Card(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = FlowSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = currentCard.question,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = FlowTextPrimary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Options
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    options.forEach { option ->
                        val isSelected = selectedOption == option
                        val isCorrect = option == currentCard.answer
                        val showCorrectness = selectedOption != null

                        val backgroundColor = when {
                            showCorrectness && isCorrect -> FlowSuccessLight
                            showCorrectness && isSelected && !isCorrect -> FlowWarningLight
                            else -> FlowSurface
                        }
                        
                        val borderColor = when {
                            showCorrectness && isCorrect -> FlowSuccess
                            showCorrectness && isSelected && !isCorrect -> FlowWarning
                            isSelected -> FlowPrimary
                            else -> FlowCardStroke
                        }

                        val textColor = when {
                            showCorrectness && isCorrect -> FlowSuccess
                            showCorrectness && isSelected && !isCorrect -> FlowWarning
                            else -> FlowTextPrimary
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth().height(60.dp)
                                .clickable(enabled = selectedOption == null) {
                                    selectedOption = option
                                    if (option == currentCard.answer) {
                                        score++
                                    }
                                },
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, borderColor),
                            colors = CardDefaults.cardColors(containerColor = backgroundColor)
                        ) {
                            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), contentAlignment = Alignment.CenterStart) {
                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = textColor
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (selectedOption != null) {
                    Button(
                        onClick = {
                            currentIndex++
                            selectedOption = null
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary)
                    ) {
                        Text(if (currentIndex == studySet.cards.size - 1) "Finish" else "Next Question", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                } else {
                    Spacer(modifier = Modifier.height(56.dp))
                }

            } else {
                // Score Screen
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Quiz Complete!", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = FlowTextPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val percentage = if (studySet.cards.isNotEmpty()) (score.toFloat() / studySet.cards.size * 100).toInt() else 0
                    
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(150.dp)) {
                        CircularProgressIndicator(
                            progress = { percentage / 100f },
                            modifier = Modifier.fillMaxSize(),
                            color = FlowPrimary,
                            trackColor = FlowCardStroke,
                            strokeWidth = 12.dp
                        )
                        Text("$percentage%", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = FlowPrimary)
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("You scored $score out of ${studySet.cards.size}", style = MaterialTheme.typography.bodyLarge, color = FlowTextSecondary)
                    
                    Spacer(modifier = Modifier.height(48.dp))
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
}
