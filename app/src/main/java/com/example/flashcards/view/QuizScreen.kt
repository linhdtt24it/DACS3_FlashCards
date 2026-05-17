package com.example.flashcards.view

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.List
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
import com.example.flashcards.model.Flashcard
import com.example.flashcards.model.StudySet
import com.example.flashcards.ui.theme.*

// ----- Fuzzy matching helper -----
private fun similarity(a: String, b: String): Float {
    val s1 = a.trim().lowercase()
    val s2 = b.trim().lowercase()
    if (s1 == s2) return 1f
    if (s1.isEmpty() || s2.isEmpty()) return 0f
    val maxLen = maxOf(s1.length, s2.length)
    val distance = levenshtein(s1, s2)
    return 1f - distance.toFloat() / maxLen
}

private fun levenshtein(a: String, b: String): Int {
    val dp = Array(a.length + 1) { IntArray(b.length + 1) }
    for (i in 0..a.length) dp[i][0] = i
    for (j in 0..b.length) dp[0][j] = j
    for (i in 1..a.length) {
        for (j in 1..b.length) {
            dp[i][j] = if (a[i - 1] == b[j - 1]) dp[i - 1][j - 1]
            else 1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
        }
    }
    return dp[a.length][b.length]
}

private const val FUZZY_THRESHOLD = 0.75f // 75% similar = correct

// ----- Question types -----
private enum class QuestionType { MULTIPLE_CHOICE, TYPING }

private data class QuizQuestion(
    val card: Flashcard,
    val type: QuestionType,
    val options: List<String> // only used for MULTIPLE_CHOICE
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
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

    val allAnswers = studySet.cards.map { it.answer }.filter { it.isNotBlank() }.distinct()

    // Build interleaved question list: alternate MC and Typing
    val questions = remember {
        studySet.cards.shuffled().mapIndexed { index, card ->
            val type = if (index % 2 == 0) QuestionType.MULTIPLE_CHOICE else QuestionType.TYPING
            val options = if (type == QuestionType.MULTIPLE_CHOICE) {
                val distractors = allAnswers.filter { it != card.answer }.shuffled().take(3)
                (distractors + card.answer).shuffled()
            } else emptyList()
            QuizQuestion(card, type, options)
        }
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var wrongCount by remember { mutableIntStateOf(0) }

    // MC state
    var selectedOption by remember { mutableStateOf<String?>(null) }

    // Typing state
    var typedAnswer by remember { mutableStateOf("") }
    var answerState by remember { mutableStateOf<Boolean?>(null) } // null/true/false
    val focusRequester = remember { FocusRequester() }

    val isFinished = currentIndex >= questions.size
    val progress = if (questions.isNotEmpty()) currentIndex.toFloat() / questions.size else 0f
    val currentQuestion = if (!isFinished) questions[currentIndex] else null

    fun goNext() {
        currentIndex++
        selectedOption = null
        typedAnswer = ""
        answerState = null
    }

    fun checkTyping() {
        if (answerState != null || typedAnswer.isBlank()) return
        val correct = currentQuestion!!.card.answer
        val sim = similarity(typedAnswer, correct)
        val isCorrect = sim >= FUZZY_THRESHOLD
        answerState = isCorrect
        if (isCorrect) score++ else wrongCount++
    }

    LaunchedEffect(currentIndex, answerState) {
        if (currentQuestion?.type == QuestionType.TYPING && answerState == null && !isFinished) {
            try { focusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Test", fontWeight = FontWeight.Bold, color = FlowTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = {
                        onRecordResult(score, wrongCount)
                        onBack()
                    }) { Icon(Icons.Default.Close, contentDescription = "Close", tint = FlowTextPrimary) }
                },
                actions = {
                    if (!isFinished && currentQuestion != null) {
                        // Show mode badge
                        val isMC = currentQuestion.type == QuestionType.MULTIPLE_CHOICE
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isMC) FlowPrimaryLight else FlowSuccessLight,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (isMC) Icons.Default.List else Icons.Default.EditNote,
                                    contentDescription = null,
                                    tint = if (isMC) FlowPrimary else FlowSuccess,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (isMC) "Choice" else "Typing",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isMC) FlowPrimary else FlowSuccess,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            "${currentIndex + 1}/${questions.size}",
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
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isFinished && currentQuestion != null) {

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = FlowPrimary,
                    trackColor = FlowCardStroke
                )
                Spacer(modifier = Modifier.height(32.dp))

                // Question card (animated slide)
                AnimatedContent(
                    targetState = currentIndex,
                    transitionSpec = {
                        slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                    },
                    label = "question"
                ) { _ ->
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
                            Text(
                                text = currentQuestion.card.question,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = FlowTextPrimary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // ---- MULTIPLE CHOICE ----
                if (currentQuestion.type == QuestionType.MULTIPLE_CHOICE) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        currentQuestion.options.forEach { option ->
                            val isSelected = selectedOption == option
                            val isCorrect = option == currentQuestion.card.answer
                            val revealed = selectedOption != null

                            val bgColor = when {
                                revealed && isCorrect -> FlowSuccessLight
                                revealed && isSelected && !isCorrect -> FlowWarningLight
                                else -> FlowSurface
                            }
                            val borderColor = when {
                                revealed && isCorrect -> FlowSuccess
                                revealed && isSelected && !isCorrect -> FlowWarning
                                isSelected -> FlowPrimary
                                else -> FlowCardStroke
                            }
                            val textColor = when {
                                revealed && isCorrect -> FlowSuccess
                                revealed && isSelected && !isCorrect -> FlowWarning
                                else -> FlowTextPrimary
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth().height(60.dp)
                                    .clickable(enabled = selectedOption == null) {
                                        selectedOption = option
                                        if (option == currentQuestion.card.answer) score++ else wrongCount++
                                    },
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.5.dp, borderColor),
                                colors = CardDefaults.cardColors(containerColor = bgColor)
                            ) {
                                Box(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), contentAlignment = Alignment.CenterStart) {
                                    Text(option, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = textColor)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    if (selectedOption != null) {
                        Button(
                            onClick = { goNext() },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary)
                        ) {
                            Text(if (currentIndex == questions.size - 1) "Finish" else "Next →", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }

                // ---- TYPING ----
                if (currentQuestion.type == QuestionType.TYPING) {
                    val inputBorderColor = when (answerState) {
                        true -> FlowSuccess; false -> FlowWarning; null -> FlowPrimary
                    }
                    val inputBgColor = when (answerState) {
                        true -> FlowSuccessLight; false -> FlowWarningLight; null -> FlowSurface
                    }

                    OutlinedTextField(
                        value = typedAnswer,
                        onValueChange = { if (answerState == null) typedAnswer = it },
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        placeholder = { Text("Type the definition...", color = FlowTextSecondary) },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = inputBorderColor,
                            unfocusedBorderColor = inputBorderColor,
                            focusedContainerColor = inputBgColor,
                            unfocusedContainerColor = inputBgColor
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { checkTyping() }),
                        readOnly = answerState != null,
                        singleLine = false,
                        maxLines = 3
                    )

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
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (answerState == true) Icons.Default.Check else Icons.Default.Close,
                                        contentDescription = null,
                                        tint = if (answerState == true) FlowSuccess else FlowWarning,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            if (answerState == true) "Close enough! ✓" else "Not quite...",
                                            fontWeight = FontWeight.Bold,
                                            color = if (answerState == true) FlowSuccess else FlowWarning
                                        )
                                        if (answerState == false) {
                                            Text("Answer: ${currentQuestion.card.answer}", color = FlowTextSecondary, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    if (answerState == null) {
                        Button(
                            onClick = { checkTyping() },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary),
                            enabled = typedAnswer.isNotBlank()
                        ) {
                            Text("Check", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    } else {
                        Button(
                            onClick = { goNext() },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary)
                        ) {
                            Text(if (currentIndex == questions.size - 1) "Finish" else "Next →", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }

            } else {
                // Results Screen
                val percentage = if (questions.isNotEmpty()) (score.toFloat() / questions.size * 100).toInt() else 0

                LaunchedEffect(Unit) { onRecordResult(score, wrongCount) }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        when {
                            percentage >= 90 -> "Excellent! 🎉"
                            percentage >= 70 -> "Good job! 👍"
                            percentage >= 50 -> "Keep practicing! 💪"
                            else -> "Need more review 📚"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = FlowTextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                        CircularProgressIndicator(
                            progress = { percentage / 100f },
                            modifier = Modifier.fillMaxSize(),
                            color = when {
                                percentage >= 70 -> FlowSuccess
                                percentage >= 50 -> FlowPrimary
                                else -> FlowWarning
                            },
                            trackColor = FlowCardStroke,
                            strokeWidth = 14.dp
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$percentage%", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = FlowPrimary)
                            Text("Score", style = MaterialTheme.typography.labelSmall, color = FlowTextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.size(52.dp).clip(CircleShape).background(FlowSuccessLight), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = FlowSuccess)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("$score", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = FlowSuccess)
                            Text("Correct", style = MaterialTheme.typography.bodySmall, color = FlowTextSecondary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.size(52.dp).clip(CircleShape).background(FlowWarningLight), contentAlignment = Alignment.Center) {
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
