package com.example.flashcards.view

import com.example.flashcards.utils.ContextUtils
import com.example.flashcards.R
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.flashcards.model.ReviewRating
import com.example.flashcards.ui.theme.FlowPrimary
import androidx.compose.ui.platform.LocalDensity

import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.flashcards.utils.ImageUtils

@Composable
fun StudyFlashcard(
    frontText: String,
    backText: String,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    isStarred: Boolean,
    onToggleStar: () -> Unit,
    onPlayTts: (String) -> Unit,
    imageUrl: String? = null,
    explanation: String? = null,
    modifier: Modifier = Modifier
) {
    val rotationState by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "flip_y"
    )
    
    val density = LocalDensity.current.density

    // Flashcard container with 3D Horizontal Flip animation (rotationY)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                rotationY = rotationState
                cameraDistance = 16f * density
            }
            .clickable { onFlip() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (rotationState <= 90f) {
                // MẶT TRƯỚC
                FlashcardFace(
                    label = ContextUtils.getString(R.string.ui_text_442),
                    text = frontText,
                    imageUrl = imageUrl,
                    explanation = null,
                    isStarred = isStarred,
                    onToggleStar = onToggleStar,
                    onPlayTts = { onPlayTts(frontText) },
                    hint = ContextUtils.getString(R.string.ui_text_443)
                )
            } else {
                // MẶT SAU (Phải xoay ngược 180 độ theo trục Y để chữ không bị ngược)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationY = 180f }
                ) {
                    FlashcardFace(
                        label = ContextUtils.getString(R.string.ui_text_444),
                        text = backText,
                        imageUrl = null,
                        explanation = explanation,
                        isStarred = isStarred,
                        onToggleStar = onToggleStar,
                        onPlayTts = { onPlayTts(backText) },
                        hint = ContextUtils.getString(R.string.ui_text_445)
                    )
                }
            }
        }
    }
}

@Composable
private fun FlashcardFace(
    label: String,
    text: String,
    imageUrl: String?,
    explanation: String?,
    isStarred: Boolean,
    onToggleStar: () -> Unit,
    onPlayTts: () -> Unit,
    hint: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = FlowPrimary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleStar) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = ContextUtils.getString(R.string.ui_text_302),
                        tint = if (isStarred) Color(0xFFFFD700) else Color.Gray,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = onPlayTts) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = ContextUtils.getString(R.string.ui_text_301),
                        tint = FlowPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (imageUrl != null) {
            val imageModel = remember(imageUrl) { ImageUtils.getImageModel(imageUrl) }
            AsyncImage(
                model = imageModel,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .padding(bottom = 16.dp)
            )
        }

        Text(
            text = text,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        if (explanation?.isNotBlank() == true) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = explanation,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.TouchApp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = hint,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun AnkiRatingButtons(
    onRate: (ReviewRating) -> Unit,
    againInterval: String = "1p",
    hardInterval: String = "3n",
    goodInterval: String = "7n",
    easyInterval: String = "15n",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        RatingButton(
            label = ContextUtils.getString(R.string.ui_text_446),
            interval = againInterval,
            color = Color(0xFFE53935),
            onClick = { onRate(ReviewRating.AGAIN) }
        )
        RatingButton(
            label = ContextUtils.getString(R.string.ui_text_447),
            interval = hardInterval,
            color = Color(0xFFFB8C00),
            onClick = { onRate(ReviewRating.HARD) }
        )
        RatingButton(
            label = ContextUtils.getString(R.string.ui_text_448),
            interval = goodInterval,
            color = Color(0xFF43A047),
            onClick = { onRate(ReviewRating.GOOD) }
        )
        RatingButton(
            label = ContextUtils.getString(R.string.ui_text_449),
            interval = easyInterval,
            color = Color(0xFF1E88E5),
            onClick = { onRate(ReviewRating.EASY) }
        )
    }
}

@Composable
private fun RatingButton(
    label: String,
    interval: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }.padding(8.dp)
    ) {
        Text(
            text = interval,
            color = color,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = color.copy(alpha = 0.15f),
            contentColor = color
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
