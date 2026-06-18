package com.example.flashcards.view

import com.example.flashcards.utils.ContextUtils
import com.example.flashcards.R
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.flashcards.viewmodel.AdminManageUsersViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminManageUsersScreen(
    navController: NavController,
    viewModel: AdminManageUsersViewModel = viewModel()
) {
    val context = LocalContext.current
    val userList by viewModel.userList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val scope = rememberCoroutineScope()
    
    var selectedUserId by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ContextUtils.getString(R.string.ui_text_93), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { showAddDialog = true },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(ContextUtils.getString(R.string.ui_text_151))
                        }

                        Button(
                            onClick = { showDeleteConfirmDialog = true },
                            enabled = selectedUserId != null,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(4.dp))
                            Text(ContextUtils.getString(R.string.ui_text_20), color = Color.White)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading && userList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(userList) { user ->
                    val isSelected = selectedUserId == user.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedUserId = if (isSelected) null else user.id },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = user.email,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            // Yêu cầu 2: Hiển thị nhãn loại tài khoản theo Role
                            Surface(
                                color = if (user.role == "admin") Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (user.role == "admin") "ADMIN" else "USER",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (user.role == "admin") Color(0xFFC62828) else Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog Thêm User: Tạo trên Auth trước -> Lấy UID làm Doc ID Firestore (Yêu cầu 3)
    if (showAddDialog) {
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var isProcessing by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isProcessing) showAddDialog = false },
            title = { Text(ContextUtils.getString(R.string.ui_text_152)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(ContextUtils.getString(R.string.ui_text_153)) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessing
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(ContextUtils.getString(R.string.ui_text_154)) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessing
                    )
                    if (isProcessing) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isProcessing,
                    onClick = {
                        if (email.isNotBlank() && password.length >= 6) {
                            isProcessing = true
                            scope.launch {
                                val result = viewModel.addUser(email, password, context)
                                if (result.isSuccess) {
                                    Toast.makeText(context, ContextUtils.getString(R.string.ui_text_155), Toast.LENGTH_SHORT).show()
                                    showAddDialog = false
                                } else {
                                    Toast.makeText(context, "Lỗi: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                }
                                isProcessing = false
                            }
                        } else {
                            Toast.makeText(context, ContextUtils.getString(R.string.ui_text_156), Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { Text(ContextUtils.getString(R.string.ui_text_157)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }, enabled = !isProcessing) { Text(ContextUtils.getString(R.string.ui_text_21)) }
            }
        )
    }

    // Dialog Xóa User theo UID (Yêu cầu 4)
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(ContextUtils.getString(R.string.ui_text_158)) },
            text = { Text(ContextUtils.getString(R.string.ui_text_159)) },
            confirmButton = {
                TextButton(onClick = {
                    selectedUserId?.let { uid ->
                        scope.launch {
                            val result = viewModel.deleteUser(uid)
                            if (result.isSuccess) {
                                Toast.makeText(context, ContextUtils.getString(R.string.ui_text_160), Toast.LENGTH_SHORT).show()
                                selectedUserId = null
                                showDeleteConfirmDialog = false
                            }
                        }
                    }
                }) { Text(ContextUtils.getString(R.string.ui_text_157), color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text(ContextUtils.getString(R.string.ui_text_21)) }
            }
        )
    }
}
