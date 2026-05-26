package com.example.flashcards.view

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flashcards.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userName: String,
    userEmail: String,
    onLogout: () -> Unit,
    onNavigateToPersonalInfo: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToLearningPrefs: () -> Unit,
    onNavigateToStats: () -> Unit
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)

    // State for user info
    var currentName by remember { mutableStateOf(userName) }
    val initials = currentName.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")

    // We refresh the user name from Firebase just in case it was updated in the sub-screen
    LaunchedEffect(Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null && user.displayName != null && user.displayName != currentName) {
            currentName = user.displayName!!
        }
    }

    // Read directly from prefs on composition so it updates when returning from sub-screens
    val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)
    val dailyGoal = prefs.getInt("daily_goal", 50)
    val reviewAlgorithm = prefs.getString("review_algorithm", "Standard") ?: "Standard"

    val firestore = remember { FirebaseFirestore.getInstance() }
    val uid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    var totalFavoriteCount by remember { mutableIntStateOf(0) }
    var learnedTodayCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) {
            // Count total favorites
            firestore.collection("user_favorites")
                .whereEqualTo("uid", uid)
                .whereEqualTo("starred", true)
                .addSnapshotListener { snapshot, error ->
                    if (snapshot != null) {
                        totalFavoriteCount = snapshot.size()
                    }
                }

            // Count learned today from progress collection
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfToday = calendar.time
            val startOfTodayTimestamp = com.google.firebase.Timestamp(startOfToday)

            firestore.collection("progress")
                .whereEqualTo("uid", uid)
                .whereGreaterThanOrEqualTo("lastReviewed", startOfTodayTimestamp)
                .addSnapshotListener { snapshot, error ->
                    if (snapshot != null) {
                        learnedTodayCount = snapshot.size()
                    }
                }
        }
    }

    val targetWords = if (totalFavoriteCount < 10) totalFavoriteCount else 10
    val progressPercent = if (targetWords > 0) (learnedTodayCount.toFloat() / targetWords).coerceIn(0f, 1f) else 0f
    val isGoalCompleted = targetWords > 0 && learnedTodayCount >= targetWords

    var currentStreakState by remember { mutableIntStateOf(0) }
    var longestStreakState by remember { mutableIntStateOf(0) }

    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) {
            firestore.collection("users").document(uid)
                .addSnapshotListener { snapshot, error ->
                    if (snapshot != null && snapshot.exists()) {
                        currentStreakState = snapshot.getLong("currentStreak")?.toInt() ?: 0
                        longestStreakState = snapshot.getLong("longestStreak")?.toInt() ?: 0
                    }
                }
        }
    }

    val updateUserStreak = { userId: String ->
        val userDocRef = firestore.collection("users").document(userId)
        userDocRef.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                val currentStreak = documentSnapshot.getLong("currentStreak")?.toInt() ?: 0
                val longestStreak = documentSnapshot.getLong("longestStreak")?.toInt() ?: 0
                val lastActiveDate = documentSnapshot.getString("lastActiveDate") ?: ""

                val calendar = Calendar.getInstance()
                val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(calendar.time)
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                val yesterdayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(calendar.time)

                if (lastActiveDate == todayStr) {
                    // Already counted today
                    return@addOnSuccessListener
                }

                val newStreak = when (lastActiveDate) {
                    "" -> 1
                    yesterdayStr -> currentStreak + 1
                    else -> 1
                }

                val newLongest = if (newStreak > longestStreak) newStreak else longestStreak

                val updateData = hashMapOf(
                    "currentStreak" to newStreak,
                    "longestStreak" to newLongest,
                    "lastActiveDate" to todayStr
                )

                userDocRef.update(updateData as Map<String, Any>)
                    .addOnSuccessListener {
                        android.util.Log.d("ProfileScreen", "Streak updated: $newStreak")
                    }
            }
        }
    }

    LaunchedEffect(isGoalCompleted, uid) {
        if (isGoalCompleted && uid.isNotEmpty()) {
            updateUserStreak(uid)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text("Profile", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (initials.isNotEmpty()) initials else "U", style = MaterialTheme.typography.headlineMedium, color = FlowPrimary, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(currentName.ifEmpty { "User" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text(userEmail.ifEmpty { "No Email" }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(12.dp)) {
                    Text("PRO", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = FlowPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = FlowPrimary),
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("FlowCards Pro", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Your plan unlocks all features", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primaryContainer)
                    }
                    TextButton(onClick = { Toast.makeText(context, "Pro features coming soon!", Toast.LENGTH_SHORT).show() }) {
                        Text("Manage", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFFFF5722).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Chuỗi ngày học liên tục",
                            tint = Color(0xFFFF5722),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$currentStreakState Ngày liên tục 🔥",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Kỷ lục: $longestStreakState ngày",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Mục tiêu hằng ngày",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (isGoalCompleted) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Hoàn thành mục tiêu",
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Surface(
                            color = if (isGoalCompleted) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isGoalCompleted) "HOÀN THÀNH" else "ĐANG THỰC HIỆN",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = if (isGoalCompleted) Color(0xFF2E7D32) else FlowPrimary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    Text(
                        text = "Đã học: $learnedTodayCount / $targetWords từ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LinearProgressIndicator(
                        progress = { progressPercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = if (isGoalCompleted) Color(0xFF4CAF50) else FlowPrimary,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                }
            }
        }

        item {
            SettingSectionHeader("Account Settings")
            SettingsItemRow(icon = Icons.Default.Person, title = "Personal Information", onClick = onNavigateToPersonalInfo)
            SettingsItemRow(icon = Icons.Default.Lock, title = "Password & Security", onClick = onNavigateToSecurity)
            SettingsItemRow(icon = Icons.Default.Notifications, title = "Notifications", value = if (notificationsEnabled) "On" else "Off", onClick = onNavigateToNotifications)
        }

        item {
            SettingSectionHeader("Learning Preferences")
            SettingsItemRow(icon = Icons.Default.TrendingUp, title = "Learning Progress (Stats)", onClick = onNavigateToStats)
            SettingsItemRow(icon = Icons.Default.Flag, title = "Daily Goal", value = "$dailyGoal cards", onClick = onNavigateToLearningPrefs)
            SettingsItemRow(icon = Icons.Default.Psychology, title = "Review Algorithm", value = reviewAlgorithm, onClick = onNavigateToLearningPrefs)
        }

        item {
            SettingSectionHeader("Support")
            SettingsItemRow(icon = Icons.Default.Help, title = "Help Center", onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/"))
                context.startActivity(intent)
            })
            SettingsItemRow(icon = Icons.Default.Mail, title = "Contact Us", onClick = {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:support@flowcards.com")
                    putExtra(Intent.EXTRA_SUBJECT, "FlowCards Support Request")
                }
                context.startActivity(intent)
            })
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FlowWarningLight)
            ) {
                Text("Log Out", color = FlowWarning, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun SettingSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
    )
}

@Composable
fun SettingsItemRow(icon: ImageVector, title: String, value: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))

        if (value != null) {
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(8.dp))
        }

        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}