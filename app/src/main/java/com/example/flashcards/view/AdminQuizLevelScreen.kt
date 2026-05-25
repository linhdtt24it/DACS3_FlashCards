package com.example.flashcards.view

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.flashcards.viewmodel.AdminQuizViewModel
import com.example.flashcards.viewmodel.QuizSubLevel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminQuizLevelScreen(
    navController: NavController,
    quizType: String,
    language: String,
    viewModel: AdminQuizViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allSubLevels by viewModel.quizSubLevels.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Form inputs
    var levelIdInput by remember { mutableStateOf("") }
    var levelNameInput by remember { mutableStateOf("") }
    var parentInput by remember { mutableStateOf("") }
    var reqPkgInput by remember { mutableStateOf("FREE") }
    var descInput by remember { mutableStateOf("") }
    var selectedLevelToDelete by remember { mutableStateOf("") }

    // English expansion/filtering state
    var selectedEnglishParent by remember { mutableStateOf<String?>(null) }

    // Dynamic translation
    val displayQuizTypeName = when(quizType) {
        "multiple_choice" -> "Trắc nghiệm"
        "text_input" -> "Tự luận"
        else -> quizType
    }
    val displayLanguageName = when(language) {
        "japanese" -> "Tiếng Nhật"
        "english" -> "Tiếng Anh"
        "chinese" -> "Tiếng Trung"
        "pali" -> "Tiếng Pali"
        else -> language
    }

    // Filter levels by current language
    val filteredLevels = remember(allSubLevels, language) {
        allSubLevels.filter { it.language == language }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Thêm cấp độ mới ($displayLanguageName)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = levelIdInput,
                        onValueChange = { levelIdInput = it },
                        label = { Text("Mã cấp độ (e.g. QUIZ_JA_N5)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = levelNameInput,
                        onValueChange = { levelNameInput = it },
                        label = { Text("Tên cấp độ (e.g. Cấp độ N5 / TOEIC 450+)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (language == "english") {
                        OutlinedTextField(
                            value = parentInput,
                            onValueChange = { parentInput = it },
                            label = { Text("Chứng chỉ cha (e.g. TOEIC / IELTS / Cambridge)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedTextField(
                        value = reqPkgInput,
                        onValueChange = { reqPkgInput = it },
                        label = { Text("Yêu cầu VIP (e.g. FREE, VIP_ENGLISH, VIP_JAPANESE)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = descInput,
                        onValueChange = { descInput = it },
                        label = { Text("Mô tả khóa học") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (levelIdInput.isNotBlank() && levelNameInput.isNotBlank()) {
                            scope.launch {
                                val result = viewModel.addQuizSubLevel(
                                    id = levelIdInput.trim(),
                                    language = language,
                                    name = levelNameInput.trim(),
                                    parent = if (language == "english") parentInput.trim() else "",
                                    requiredPackage = reqPkgInput.trim(),
                                    description = descInput.trim()
                                )
                                if (result.isSuccess) {
                                    Toast.makeText(context, "Đã thêm cấp độ thành công!", Toast.LENGTH_SHORT).show()
                                    showAddDialog = false
                                    levelIdInput = ""; levelNameInput = ""; parentInput = ""; reqPkgInput = "FREE"; descInput = ""
                                } else {
                                    Toast.makeText(context, "Lỗi: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Vui lòng nhập đầy đủ mã và tên!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Thêm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    if (showDeleteDialog) {
        var expanded by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xóa cấp độ ($displayLanguageName)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Chọn cấp độ muốn xóa:")
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val currentSelectionName = filteredLevels.find { it.id == selectedLevelToDelete }?.name ?: "Chọn cấp độ"
                        Button(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(currentSelectionName)
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            filteredLevels.forEach { lvl ->
                                DropdownMenuItem(
                                    text = { Text("${lvl.name} (${lvl.id})") },
                                    onClick = {
                                        selectedLevelToDelete = lvl.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedLevelToDelete.isNotBlank()) {
                            scope.launch {
                                val result = viewModel.deleteQuizSubLevel(selectedLevelToDelete)
                                if (result.isSuccess) {
                                    Toast.makeText(context, "Đã xóa cấp độ!", Toast.LENGTH_SHORT).show()
                                    showDeleteDialog = false
                                    selectedLevelToDelete = ""
                                } else {
                                    Toast.makeText(context, "Lỗi: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Vui lòng chọn cấp độ để xóa!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Xóa")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Phân chia Cấp độ (Tầng 3)", fontWeight = FontWeight.Bold)
                        Text("$displayQuizTypeName -> $displayLanguageName", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(when(language) {
                            "japanese" -> "Thêm cấp độ Nhật"
                            "english" -> "Thêm chứng chỉ Anh"
                            else -> "Thêm cấp độ mới"
                        })
                    }
                    Button(
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(when(language) {
                            "japanese" -> "Xóa cấp độ Nhật"
                            "english" -> "Xóa mức độ Anh"
                            else -> "Xóa cấp độ"
                        })
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    // English certificate navigation breadcrumb
                    if (language == "english" && selectedEnglishParent != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            InputChip(
                                selected = true,
                                onClick = { selectedEnglishParent = null },
                                label = { Text("Quay lại chọn chứng chỉ") },
                                trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Đang chọn: $selectedEnglishParent",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        if (language == "english" && selectedEnglishParent == null) {
                            // Render distinct certificate parents: TOEIC, IELTS, Cambridge
                            val parents = filteredLevels.map { it.parent }.filter { it.isNotBlank() }.distinct()
                            items(parents) { parent ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedEnglishParent = parent
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(20.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Chứng chỉ $parent",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            val count = filteredLevels.count { it.parent == parent }
                                            Text(
                                                text = "$count cấp độ điểm số",
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = Color.Gray
                                        )
                                    }
                                }
                            }

                            if (parents.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillParentMaxSize()
                                            .padding(bottom = 100.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Chưa có chứng chỉ Tiếng Anh nào. Nhấn Thêm chứng chỉ.", color = Color.Gray)
                                    }
                                }
                            }
                        } else {
                            // Filter either standard language sub-levels or sub-levels of selected English parent
                            val activeLevels = if (language == "english") {
                                filteredLevels.filter { it.parent == selectedEnglishParent }
                            } else {
                                filteredLevels
                            }

                            items(activeLevels) { lvl ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            navController.navigate("admin_quiz_final_editor/$quizType/$language/${lvl.id}/${lvl.name}")
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(20.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = lvl.name,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (lvl.description.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = lvl.description,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Mã: ${lvl.id} | VIP: ${lvl.requiredPackage}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = Color.Gray
                                        )
                                    }
                                }
                            }

                            if (activeLevels.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillParentMaxSize()
                                            .padding(bottom = 100.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Chưa có cấp độ chi tiết nào. Vui lòng nhấn nút Thêm dưới đáy.", color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
