package com.example.flashcards.view

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
    onDismiss: () -> Unit
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
                        "Hãy tiếp tục nỗ lực nhé! Bạn đang làm rất tốt! 🎉",
                        "Mỗi ngày học thêm một chút, thành công sẽ đến với bạn! 🌟",
                        "Bạn đã tiến bộ hơn ngày hôm qua rất nhiều! 🚀",
                        "Sự kiên trì chính là chìa khóa của thành công! 💪",
                        "Đừng bỏ cuộc, hành trình vạn dặm bắt đầu từ một bước chân! 🌸"
                    ).random()
                }
            }
            .addOnFailureListener {
                isLoading = false
                wishText = listOf(
                    "Hãy tiếp tục nỗ lực nhé! Bạn đang làm rất tốt! 🎉",
                    "Mỗi ngày học thêm một chút, thành công sẽ đến với bạn! 🌟",
                    "Bạn đã tiến bộ hơn ngày hôm qua rất nhiều! 🚀",
                    "Sự kiên trì chính là chìa khóa của thành công! 💪",
                    "Đừng bỏ cuộc, hành trình vạn dặm bắt đầu từ một bước chân! 🌸"
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
                    text = "🎉 LỜI CHÚC Ý NGHĨA 🎉",
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
                    Text("Đóng", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}
