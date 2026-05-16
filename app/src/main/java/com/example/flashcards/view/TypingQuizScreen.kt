package com.example.flashcards.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flashcards.model.StudySet
import com.example.flashcards.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypingQuizScreen(
    studySet: StudySet,
    onBack: () -> Unit,
    onRecordResult: (correct: Int, wrong: Int) -> Unit = { _, _ -> }
) {
    if (studySet.cards.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().background(FlowBackground), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("This deck has no cards.", color = FlowTextPrimary)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBack) { Text("Go Back") }
            }
        }
        return
    }

    val cards = remember { studySet.cards.shuffled() }
    var currentIndex by remember { mutableIntStateOf(0) }
    var typedAnswer by remember { mutableStateOf("") }
    var answerState by remember { mutableStateOf<Boolean?>(null) } // null = not answered, true = correct, false = wrong
    var score by remember { mutableIntStateOf(0) }
    var wrongCount by remember { mutableIntStateOf(0) }
    val focusRequester = remember { FocusRequester() }

    val isFinished = currentIndex >= cards.size
    val progress = if (cards.isNotEmpty()) (currentIndex.toFloat() / cards.size) else 0f

    LaunchedEffect(currentIndex, answerState) {
        if (answerState == null && !isFinished) {
            focusRequester.requestFocus()
        }
    }

    fun checkAnswer() {
        if (answerState != null || typedAnswer.isBlank()) return
        val correctAnswer = cards[currentIndex].answer.trim()
        val isCorrect = typedAnswer.trim().equals(correctAnswer, ignoreCase = true)
        answerState = isCorrect
        if (isCorrect) score++ else wrongCount++
    }

    fun nextCard() {
        currentIndex++
        typedAnswer = ""
        answerState = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Typing Mode", fontWeight = FontWeight.Bold, color = FlowTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = {
                        onRecordResult(score, wrongCount)
                        onBack()
                    }) { Icon(Icons.Default.Close, contentDescription = "Close", tint = FlowTextPrimary) }
                },
                actions = {
                    if (!isFinished) {
                        Text(
                            "${currentIndex + 1}/${cards.size}",
                            modifier = Modifier.padding(end = 16.dp),
                            color = FlowTextSecondary,
                            fontWeight = FontWeight.Bold
                        )
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
                // Progress bar
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = FlowPrimary,
                    trackColor = FlowCardStroke
                )
                Spacer(modifier = Modifier.height(32.dp))

                val currentCard = cards[currentIndex]

                // Question card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = FlowSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            color = FlowPrimaryLight,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "TERM",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                color = FlowPrimary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = currentCard.question,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = FlowTextPrimary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Answer input area
                val inputBorderColor = when (answerState) {
                    true -> FlowSuccess
                    false -> FlowWarning
                    null -> FlowPrimary
                }
                val inputContainerColor = when (answerState) {
                    true -> FlowSuccessLight
                    false -> FlowWarningLight
                    null -> FlowSurface
                }

                OutlinedTextField(
                    value = typedAnswer,
                    onValueChange = { if (answerState == null) typedAnswer = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = { Text("Type the definition...", color = FlowTextSecondary) },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = inputBorderColor,
                        unfocusedBorderColor = inputBorderColor,
                        focusedContainerColor = inputContainerColor,
                        unfocusedContainerColor = inputContainerColor
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { checkAnswer() }),
                    readOnly = answerState != null,
                    singleLine = false,
                    maxLines = 3
                )

                // Feedback after answering
                AnimatedVisibility(visible = answerState != null, enter = fadeIn(), exit = fadeOut()) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (answerState == true) FlowSuccessLight else FlowWarningLight
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (answerState == true) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    tint = if (answerState == true) FlowSuccess else FlowWarning,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        if (answerState == true) "Correct! 🎉" else "Not quite...",
                                        fontWeight = FontWeight.Bold,
                                        color = if (answerState == true) FlowSuccess else FlowWarning,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    if (answerState == false) {
                                        Text(
                                            "Answer: ${currentCard.answer}",
                                            color = FlowTextSecondary,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Buttons
                if (answerState == null) {
                    Button(
                        onClick = { checkAnswer() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary),
                        enabled = typedAnswer.isNotBlank()
                    ) {
                        Text("Check Answer", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                } else {
                    Button(
                        onClick = { nextCard() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary)
                    ) {
                        Text(
                            if (currentIndex == cards.size - 1) "Finish" else "Next →",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

            } else {
                // Results Screen
                val percentage = if (cards.isNotEmpty()) (score.toFloat() / cards.size * 100).toInt() else 0

                LaunchedEffect(Unit) { onRecordResult(score, wrongCount) }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Typing Quiz Complete!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = FlowTextPrimary, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(32.dp))

                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                        CircularProgressIndicator(
                            progress = { percentage / 100f },
                            modifier = Modifier.fillMaxSize(),
                            color = if (percentage >= 70) FlowSuccess else FlowWarning,
                            trackColor = FlowCardStroke,
                            strokeWidth = 14.dp
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$percentage%", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = FlowPrimary)
                            Text("Accuracy", style = MaterialTheme.typography.labelSmall, color = FlowTextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(FlowSuccessLight), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = FlowSuccess)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("$score", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = FlowSuccess)
                            Text("Correct", style = MaterialTheme.typography.bodySmall, color = FlowTextSecondary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(FlowWarningLight), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = FlowWarning)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("$wrongCount", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = FlowWarning)
                            Text("Wrong", style = MaterialTheme.typography.bodySmall, color = FlowTextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    Button(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
