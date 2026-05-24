package com.example.flashcards.view

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.flashcards.viewmodel.AdminPackageViewModel
import com.example.flashcards.viewmodel.PackageItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminManagePackagesScreen(
    navController: NavController,
    viewModel: AdminPackageViewModel = viewModel()
) {
    val packages by viewModel.packages.collectAsState()
    var selectedPackageId by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý gói học", fontWeight = FontWeight.Bold) },
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
                            Text("Thêm gói học mới")
                        }

                        Button(
                            onClick = { 
                                selectedPackageId?.let { viewModel.deletePackage(it) }
                                selectedPackageId = null
                            },
                            enabled = selectedPackageId != null,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(4.dp))
                            Text("Xóa gói học", color = Color.White)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(packages) { pkg ->
                val isSelected = selectedPackageId == pkg.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            // Nếu đã chọn rồi thì điều hướng sang chi tiết
                            if (isSelected) {
                                navController.navigate("package_detail/${pkg.name}")
                            } else {
                                selectedPackageId = pkg.id 
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = pkg.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Text("Nhấn lần nữa để cấu hình", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var newPackageName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Thêm gói học") },
            text = {
                OutlinedTextField(
                    value = newPackageName,
                    onValueChange = { newPackageName = it },
                    label = { Text("Tên gói học") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPackageName.isNotBlank()) {
                        viewModel.addPackage(newPackageName)
                        showAddDialog = false
                    }
                }) { Text("Thêm") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Hủy") }
            }
        )
    }
}
