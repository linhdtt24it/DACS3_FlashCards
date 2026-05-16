package com.example.flashcards.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.flashcards.model.SocialNotification
import com.example.flashcards.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationInboxScreen(
    notifications: List<SocialNotification>,
    onBack: () -> Unit,
    onNotificationClick: (SocialNotification) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.Bold, color = FlowTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = FlowTextPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FlowBackground)
            )
        },
        containerColor = FlowBackground
    ) { padding ->
        if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No notifications yet", color = FlowTextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notifications) { notif ->
                    NotificationItem(notif) { onNotificationClick(notif) }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(notification: SocialNotification, onClick: () -> Unit) {
    val isUnread = !notification.isRead
    val bgColor = if (isUnread) FlowPrimaryLight.copy(alpha = 0.3f) else FlowSurface
    
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(if (notification.type == "RATING") FlowWarningLight else FlowPrimaryLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (notification.type == "RATING") Icons.Default.Star else Icons.Default.Comment,
                    contentDescription = null,
                    tint = if (notification.type == "RATING") FlowWarning else FlowPrimary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                val actionText = if (notification.type == "RATING") "rated your deck" else "commented on"
                Text(
                    text = "${notification.senderName} $actionText ${notification.deckTitle}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal,
                    color = FlowTextPrimary
                )
                if (notification.type == "COMMENT") {
                    Text("\"${notification.content}\"", style = MaterialTheme.typography.bodySmall, color = FlowTextSecondary, maxLines = 1)
                } else if (notification.type == "RATING") {
                    Text("⭐ ${notification.content}", style = MaterialTheme.typography.bodySmall, color = FlowWarning, maxLines = 1)
                }
                val date = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(notification.timestamp))
                Text(date, style = MaterialTheme.typography.labelSmall, color = FlowTextSecondary)
            }
            if (isUnread) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(FlowPrimary))
            }
        }
    }
}
