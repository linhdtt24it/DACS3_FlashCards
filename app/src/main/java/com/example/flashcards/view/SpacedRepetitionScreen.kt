package com.example.flashcards.view

import com.example.flashcards.utils.ContextUtils
import com.example.flashcards.R
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.flashcards.ui.theme.FlowPrimary
import com.example.flashcards.ui.theme.FlowWarning
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpacedRepetitionScreen(navController: NavController) {
    val context = LocalContext.current
    val firestore = remember { FirebaseFirestore.getInstance() }
    val uid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }

    var dailyWordLimitState by remember { mutableStateOf("10") }
    var isLoadingLimit by remember { mutableStateOf(true) }
    var isSavingLimit by remember { mutableStateOf(false) }

    // Day counts map (Day index 1..7 -> word count)
    val dayCounts = remember { mutableStateMapOf<Int, Int>() }
    var isLoadingTimeline by remember { mutableStateOf(true) }

    // 1. Fetch current dailyWordLimit
    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) {
            firestore.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val limit = doc.getLong("dailyWordLimit")?.toInt() ?: 10
                        dailyWordLimitState = limit.toString()
                    }
                    isLoadingLimit = false
                }
                .addOnFailureListener {
                    isLoadingLimit = false
                }
        } else {
            isLoadingLimit = false
        }
    }

    // 2. Fetch and calculate Timeline scheduled reviews for 7 days
    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) {
            firestore.collection("progress")
                .whereEqualTo("uid", uid)
                .addSnapshotListener { snapshot, error ->
                    if (snapshot != null) {
                        isLoadingTimeline = true
                        
                        // Clear previous counts
                        for (i in 1..7) {
                            dayCounts[i] = 0
                        }

                        val calendar = Calendar.getInstance()
                        
                        // Start of today
                        calendar.set(Calendar.HOUR_OF_DAY, 0)
                        calendar.set(Calendar.MINUTE, 0)
                        calendar.set(Calendar.SECOND, 0)
                        calendar.set(Calendar.MILLISECOND, 0)
                        val startOfToday = calendar.timeInMillis

                        // End of days boundaries
                        val dayEndTimes = LongArray(7)
                        for (i in 0..6) {
                            calendar.timeInMillis = startOfToday
                            calendar.add(Calendar.DAY_OF_YEAR, i)
                            calendar.set(Calendar.HOUR_OF_DAY, 23)
                            calendar.set(Calendar.MINUTE, 59)
                            calendar.set(Calendar.SECOND, 59)
                            calendar.set(Calendar.MILLISECOND, 999)
                            dayEndTimes[i] = calendar.timeInMillis
                        }

                        snapshot.documents.forEach { doc ->
                            val status = doc.getString("status") ?: ""
                            if (status != "MASTERED") {
                                val nextReview = doc.getTimestamp("nextReview")
                                if (nextReview != null) {
                                    val nextReviewMs = nextReview.toDate().time
                                    
                                    if (nextReviewMs <= dayEndTimes[0]) {
                                        // Due today (Day 1)
                                        dayCounts[1] = (dayCounts[1] ?: 0) + 1
                                    } else if (nextReviewMs <= dayEndTimes[1]) {
                                        // Tomorrow (Day 2)
                                        dayCounts[2] = (dayCounts[2] ?: 0) + 1
                                    } else if (nextReviewMs <= dayEndTimes[2]) {
                                        // Day 3
                                        dayCounts[3] = (dayCounts[3] ?: 0) + 1
                                    } else if (nextReviewMs <= dayEndTimes[3]) {
                                        dayCounts[4] = (dayCounts[4] ?: 0) + 1
                                    } else if (nextReviewMs <= dayEndTimes[4]) {
                                        dayCounts[5] = (dayCounts[5] ?: 0) + 1
                                    } else if (nextReviewMs <= dayEndTimes[5]) {
                                        dayCounts[6] = (dayCounts[6] ?: 0) + 1
                                    } else if (nextReviewMs <= dayEndTimes[6]) {
                                        dayCounts[7] = (dayCounts[7] ?: 0) + 1
                                    }
                                } else {
                                    // New or unscheduled, falls to Today (Day 1)
                                    dayCounts[1] = (dayCounts[1] ?: 0) + 1
                                }
                            }
                        }
                        isLoadingTimeline = false
                    } else {
                        isLoadingTimeline = false
                    }
                }
        }
    }

    val saveDailyWordLimit: () -> Unit = {
        val limitVal = dailyWordLimitState.toIntOrNull()
        if (limitVal == null || limitVal <= 0) {
            Toast.makeText(context, ContextUtils.getString(R.string.ui_text_418), Toast.LENGTH_SHORT).show()
        } else if (uid.isEmpty()) {
            Toast.makeText(context, ContextUtils.getString(R.string.ui_text_419), Toast.LENGTH_SHORT).show()
        } else {
            isSavingLimit = true
            firestore.collection("users").document(uid)
                .update("dailyWordLimit", limitVal)
                .addOnSuccessListener {
                    isSavingLimit = false
                    Toast.makeText(context, "Đã lưu hạn mức hằng ngày: $limitVal từ", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    isSavingLimit = false
                    Toast.makeText(context, "Lỗi: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spaced Repetition", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. HEADER DESCRIPTION
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF1D4ED8), Color(0xFF3B82F6))
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = ContextUtils.getString(R.string.ui_text_420),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = ContextUtils.getString(R.string.ui_text_421),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            // 2. DAILY TARGET SETTER SECTION
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TrackChanges,
                                contentDescription = null,
                                tint = FlowPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = ContextUtils.getString(R.string.ui_text_422),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        
                        Text(
                            text = ContextUtils.getString(R.string.ui_text_423),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = dailyWordLimitState,
                                onValueChange = { dailyWordLimitState = it },
                                label = { Text(ContextUtils.getString(R.string.ui_text_424)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = FlowPrimary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.weight(1f),
                                enabled = !isLoadingLimit && !isSavingLimit
                            )
                            
                            Button(
                                onClick = saveDailyWordLimit,
                                colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(56.dp),
                                enabled = !isLoadingLimit && !isSavingLimit
                            ) {
                                if (isSavingLimit) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(ContextUtils.getString(R.string.ui_text_71), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 3. TIMELINE DAYS SECTION
            item {
                Text(
                    text = ContextUtils.getString(R.string.ui_text_426),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            if (isLoadingTimeline) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = FlowPrimary)
                    }
                }
            } else {
                val daysList = listOf(
                    1 to Pair(ContextUtils.getString(R.string.ui_text_427), ContextUtils.getString(R.string.ui_text_428)),
                    2 to Pair(ContextUtils.getString(R.string.ui_text_429), ContextUtils.getString(R.string.ui_text_430)),
                    3 to Pair(ContextUtils.getString(R.string.ui_text_431), ContextUtils.getString(R.string.ui_text_432)),
                    4 to Pair(ContextUtils.getString(R.string.ui_text_433), ContextUtils.getString(R.string.ui_text_434)),
                    5 to Pair(ContextUtils.getString(R.string.ui_text_435), ContextUtils.getString(R.string.ui_text_434)),
                    6 to Pair(ContextUtils.getString(R.string.ui_text_436), ContextUtils.getString(R.string.ui_text_434)),
                    7 to Pair(ContextUtils.getString(R.string.ui_text_437), ContextUtils.getString(R.string.ui_text_434))
                )
                
                daysList.forEach { (index, names) ->
                    val (title, subtitle) = names
                    val count = dayCounts[index] ?: 0
                    
                    item {
                        TimelineDayCard(
                            dayIndex = index,
                            title = title,
                            subtitle = subtitle,
                            wordCount = count,
                            onClick = {
                                navController.navigate("spaced_repetition_dashboard/day_$index")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineDayCard(
    dayIndex: Int,
    title: String,
    subtitle: String,
    wordCount: Int,
    onClick: () -> Unit
) {
    val isToday = dayIndex == 1
    val containerColor = if (isToday) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    }
    
    val borderColor = if (isToday) {
        FlowPrimary.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (isToday) FlowPrimary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isToday) Icons.Default.EventNote else Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = if (isToday) FlowPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (wordCount > 0) {
                            if (isToday) FlowPrimary.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                        }
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$wordCount từ",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (wordCount > 0) {
                        if (isToday) FlowPrimary else MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}
