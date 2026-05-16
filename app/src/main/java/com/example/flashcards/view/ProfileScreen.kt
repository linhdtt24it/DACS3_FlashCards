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

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(FlowBackground).padding(24.dp),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text("Profile", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = FlowTextPrimary)
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(80.dp).clip(CircleShape).background(FlowPrimaryLight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (initials.isNotEmpty()) initials else "U", style = MaterialTheme.typography.headlineMedium, color = FlowPrimary, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(currentName.ifEmpty { "User" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = FlowTextPrimary)
                    Text(userEmail.ifEmpty { "No Email" }, style = MaterialTheme.typography.bodyMedium, color = FlowTextSecondary)
                }
                Surface(color = FlowPrimaryLight, shape = RoundedCornerShape(12.dp)) {
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
                        Text("Your plan unlocks all features", style = MaterialTheme.typography.bodySmall, color = FlowPrimaryLight)
                    }
                    TextButton(onClick = { Toast.makeText(context, "Pro features coming soon!", Toast.LENGTH_SHORT).show() }) {
                        Text("Manage", color = Color.White, fontWeight = FontWeight.Bold)
                    }
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
        color = FlowTextPrimary,
        modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
    )
}

@Composable
fun SettingsItemRow(icon: ImageVector, title: String, value: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = FlowTextSecondary)
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, color = FlowTextPrimary, modifier = Modifier.weight(1f))
        
        if (value != null) {
            Text(value, style = MaterialTheme.typography.bodyMedium, color = FlowTextSecondary)
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = FlowTextSecondary)
    }
}
