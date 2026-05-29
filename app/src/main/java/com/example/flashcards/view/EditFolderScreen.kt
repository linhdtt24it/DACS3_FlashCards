package com.example.flashcards.view

import android.widget.Toast
import com.example.flashcards.utils.CryptoUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.flashcards.model.Folder
import com.example.flashcards.ui.theme.FlowPrimary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFolderScreen(
    folder: Folder,
    navController: NavController
) {
    var selectedIconName by remember { mutableStateOf(folder.iconName) }
    var folderTitleState by remember { mutableStateOf(TextFieldValue(folder.name)) }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    val iconOptions = listOf("folder", "book", "star")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chỉnh sửa thư mục", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Icon lớn
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconVector(selectedIconName),
                    contentDescription = "Selected Icon",
                    tint = FlowPrimary,
                    modifier = Modifier.size(56.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // TextField nhập tên thư mục
            OutlinedTextField(
                value = folderTitleState,
                onValueChange = { folderTitleState = it },
                label = { Text("Tên thư mục") },
                placeholder = { Text("Nhập tên thư mục...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FlowPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Lựa chọn Icon bên dưới
            Text(
                text = "Chọn Icon",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start),
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                iconOptions.forEach { option ->
                    val isSelected = option == selectedIconName
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(72.dp)
                            .clickable { selectedIconName = option },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            width = 2.dp,
                            color = if (isSelected) FlowPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getIconVector(option),
                                contentDescription = option,
                                tint = if (isSelected) FlowPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Nút Lưu 💾
            Button(
                onClick = {
                    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                    if (uid.isEmpty()) {
                        Toast.makeText(context, "Vui lòng đăng nhập!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (folderTitleState.text.isBlank()) {
                        Toast.makeText(context, "Tên thư mục không được để trống!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    scope.launch {
                        try {
                            val encryptedName = CryptoUtils.encrypt(folderTitleState.text)
                            val updateData = mapOf(
                                "name" to encryptedName,
                                "title" to encryptedName,
                                "iconName" to selectedIconName,
                                "emoji" to when(selectedIconName) {
                                    "folder" -> "📁"
                                    "book" -> "📚"
                                    "star" -> "⭐"
                                    else -> "📁"
                                }
                            )
                            FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(uid)
                                .collection("folders")
                                .document(folder.id)
                                .update(updateData)
                                .await()
                            
                            Toast.makeText(context, "Cập nhật thư mục thành công! 🎉", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Lỗi cập nhật: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Lưu 💾", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            }
        }
    }
}

fun getIconVector(iconName: String): ImageVector {
    return when(iconName) {
        "folder" -> Icons.Default.Folder
        "book" -> Icons.Default.Book
        "star" -> Icons.Default.Star
        else -> Icons.Default.Folder // Icon mặc định an toàn
    }
}
