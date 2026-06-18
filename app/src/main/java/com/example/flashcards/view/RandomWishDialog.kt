package com.example.flashcards.view

import com.example.flashcards.utils.ContextUtils
import com.example.flashcards.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun RandomWishDialog(
    onDismiss: () -> Unit,
    onReviewAgain: (() -> Unit)? = null
) {
    val firestore = remember { FirebaseFirestore.getInstance() }
    var wishText by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        firestore.collection("wishes")
            .get()
            .addOnSuccessListener { snapshot ->
                isLoading = false
                if (snapshot != null && !snapshot.isEmpty) {
                    val wishes = snapshot.documents.mapNotNull { it.getString("text") }
                    if (wishes.isNotEmpty()) {
                        wishText = wishes.random()
                    }
                }
                if (wishText == null) {
                    // Fallback default beautiful motivational quotes
                    wishText = listOf(
                        ContextUtils.getString(R.string.ui_text_401),
                        ContextUtils.getString(R.string.ui_text_402),
                        ContextUtils.getString(R.string.ui_text_403),
                        ContextUtils.getString(R.string.ui_text_404),
                        ContextUtils.getString(R.string.ui_text_405)
                    ).random()
                }
            }
            .addOnFailureListener {
                isLoading = false
                wishText = listOf(
                    ContextUtils.getString(R.string.ui_text_401),
                    ContextUtils.getString(R.string.ui_text_402),
                    ContextUtils.getString(R.string.ui_text_403),
                    ContextUtils.getString(R.string.ui_text_404),
                    ContextUtils.getString(R.string.ui_text_405)
                ).random()
            }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = ContextUtils.getString(R.string.ui_text_406),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                } else {
                    Text(
                        text = wishText ?: "",
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            if (onReviewAgain != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Text(ContextUtils.getString(R.string.ui_text_407), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Button(
                        onClick = onReviewAgain,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(ContextUtils.getString(R.string.ui_text_408), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(ContextUtils.getString(R.string.ui_text_80), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}
