package com.example.flashcards.view

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.example.flashcards.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import coil.compose.AsyncImage
import com.example.flashcards.model.Flashcard
import com.example.flashcards.model.StudySet
import com.example.flashcards.model.UserStats
import com.example.flashcards.ui.theme.*
import com.example.flashcards.utils.ImageUtils
import com.example.flashcards.utils.FlashcardUtils
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.border
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.graphics.Brush
import com.example.flashcards.ui.theme.LocalFlowColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.lazy.rememberLazyListState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.navigation.NavController
import java.util.Calendar





@Composable
fun AddDeckDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var newTitle by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ui_text_68), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = newTitle, onValueChange = { newTitle = it }, label = { Text(stringResource(R.string.ui_text_69)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = newDesc, onValueChange = { newDesc = it }, label = { Text(stringResource(R.string.ui_text_70)) }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (newTitle.isNotBlank()) {
                    onSave(newTitle, newDesc)
                    onDismiss()
                }
            }) { Text(stringResource(R.string.ui_text_71), fontWeight = FontWeight.Bold, color = FlowPrimary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_text_21), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}


@Composable
fun ImportDeckDialog(onDismiss: () -> Unit, onImport: (String) -> Unit) {
    var shareCode by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ui_text_72), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.ui_text_73), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(value = shareCode, onValueChange = { shareCode = it.uppercase() }, label = { Text(stringResource(R.string.ui_text_74)) }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (shareCode.isNotBlank()) {
                    onImport(shareCode)
                    onDismiss()
                }
            }) { Text(stringResource(R.string.ui_text_75), fontWeight = FontWeight.Bold, color = FlowPrimary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_text_21), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}


@Composable
fun BulkImportDialog(
    onDismiss: () -> Unit,
    onImport: (List<Flashcard>) -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    var separator by remember { mutableStateOf("-") }
    val separators = listOf("-", ":", "|", "Tab")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ui_text_76), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Paste your cards here. One card per line.\nExample: Front $separator Back",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Hello - Xin chào\nApple - Quả táo") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FlowPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Text(stringResource(R.string.ui_text_77), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    separators.forEach { sep ->
                        val displaySep = if (sep == "Tab") "\t" else sep
                        FilterChip(
                            selected = separator == displaySep,
                            onClick = { separator = displaySep },
                            label = { Text(sep) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = FlowPrimary
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            val parsedCards = FlashcardUtils.parseBulkText(textInput, separator)
            Button(
                onClick = {
                    onImport(parsedCards)
                    onDismiss()
                },
                enabled = parsedCards.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary)
            ) {
                Text("Import (${parsedCards.size})", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ui_text_21), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}


@Composable
fun PremiumUpgradeDialog(
    packageName: String,
    onDismiss: () -> Unit
) {
    val displayPackageName = when (packageName) {
        "VIP_JAPANESE" -> "VIP Nhật Ngữ"
        "VIP_ENGLISH" -> "VIP Anh Ngữ"
        "VIP_CHINESE" -> "VIP Hoa Ngữ"
        "VIP_PALI" -> "VIP Pali Học"
        else -> "VIP Đặc Quyền"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Stars, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.ui_text_78), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
            }
        },
        text = {
            Text(
                text = "Nội dung này thuộc gói $displayPackageName. Vui lòng liên hệ Admin để nâng cấp và mở khóa đặc quyền học tập không giới hạn!",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.ui_text_79), fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ui_text_80), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    )
}

data class SystemSetItem(
    val title: String,
    val description: String,
    val language: String,
    val levelId: String,
    val requiredPackage: String = "FREE",
    val iconEmoji: String
)

