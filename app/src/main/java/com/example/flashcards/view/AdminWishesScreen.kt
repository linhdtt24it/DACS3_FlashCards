package com.example.flashcards.view

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

data class WishItem(
    val id: String,
    val text: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminWishesScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val firestore = remember { FirebaseFirestore.getInstance() }
    var wishesList by remember { mutableStateOf<List<WishItem>>(emptyList()) }
    var selectedWishId by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Real-time Firestore sync
    DisposableEffect(Unit) {
        val listener = firestore.collection("wishes")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                isLoading = false
                if (error != null) {
                    Log.e("AdminWishesScreen", "Firestore sync failed", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    wishesList = snapshot.documents.mapNotNull { doc ->
                        val id = doc.getString("wishId") ?: doc.id
                        val text = doc.getString("text") ?: ""
                        if (id.isNotEmpty() && text.isNotEmpty()) {
                            WishItem(id, text)
                        } else null
                    }
                }
            }
        onDispose {
            listener.remove()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý Lời chúc", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            BottomAppBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Thêm")
                    }
                    Button(
                        onClick = {
                            selectedWishId?.let { id ->
                                firestore.collection("wishes")
                                    .document(id)
                                    .delete()
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Xóa lời chúc thành công!", Toast.LENGTH_SHORT).show()
                                        selectedWishId = null
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(context, "Lỗi khi xóa lời chúc!", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        },
                        enabled = selectedWishId != null,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Xóa", color = Color.White)
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (wishesList.isEmpty()) {
                Text(
                    text = "Chưa có lời chúc nào. Hãy bấm Thêm để nhập!",
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(wishesList) { wish ->
                        val isSelected = selectedWishId == wish.id
                        Card(
                            onClick = {
                                selectedWishId = if (isSelected) null else wish.id
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 72.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = wish.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var bulkInputText by remember { mutableStateOf("") }
        var isSaving by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSaving) showAddDialog = false },
            title = { Text("Thêm lời chúc mới", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Nhập mỗi lời chúc trên một dòng khác nhau để Import hàng loạt cùng lúc.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    OutlinedTextField(
                        value = bulkInputText,
                        onValueChange = { bulkInputText = it },
                        placeholder = { Text("Chúc bạn học tập thật tốt! 🎉\nBạn đang tiến bộ mỗi ngày! 🌟") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        maxLines = 10,
                        singleLine = false,
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isSaving
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val lines = bulkInputText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                        if (lines.isNotEmpty()) {
                            isSaving = true
                            val batch = firestore.batch()
                            lines.forEach { line ->
                                val ref = firestore.collection("wishes").document()
                                val data = hashMapOf(
                                    "wishId" to ref.id,
                                    "text" to line,
                                    "createdAt" to com.google.firebase.Timestamp.now()
                                )
                                batch.set(ref, data)
                            }
                            batch.commit()
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Đã import ${lines.size} lời chúc thành công!", Toast.LENGTH_SHORT).show()
                                    showAddDialog = false
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Lỗi khi import lời chúc!", Toast.LENGTH_SHORT).show()
                                    isSaving = false
                                }
                        } else {
                            Toast.makeText(context, "Vui lòng nhập ít nhất một lời chúc!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isSaving && bulkInputText.isNotBlank()
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Text("Import Lời Chúc")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddDialog = false },
                    enabled = !isSaving
                ) {
                    Text("Hủy")
                }
            }
        )
    }
}
