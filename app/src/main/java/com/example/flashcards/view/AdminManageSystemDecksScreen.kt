package com.example.flashcards.view

import com.example.flashcards.utils.ContextUtils
import com.example.flashcards.R
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.flashcards.ui.theme.FlowPrimary

// Data class định nghĩa cấu trúc danh mục từ vựng hệ thống
data class SystemCategoryItem(
    val code: String,
    val title: String,
    val languageGroup: String,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AdminManageSystemDecksScreen(navController: NavController) {
    
    // Danh sách toàn bộ các danh mục từ vựng hệ thống
    val categories = remember {
        listOf(
            SystemCategoryItem("JA_NEW", ContextUtils.getString(R.string.ui_text_132), ContextUtils.getString(R.string.ui_text_133), ContextUtils.getString(R.string.ui_text_134)),
            SystemCategoryItem("JA_N5", ContextUtils.getString(R.string.ui_text_135), ContextUtils.getString(R.string.ui_text_133), ContextUtils.getString(R.string.ui_text_136)),
            SystemCategoryItem("JA_N4", ContextUtils.getString(R.string.ui_text_137), ContextUtils.getString(R.string.ui_text_133), ContextUtils.getString(R.string.ui_text_138)),
            SystemCategoryItem("JA_N3", ContextUtils.getString(R.string.ui_text_139), ContextUtils.getString(R.string.ui_text_133), ContextUtils.getString(R.string.ui_text_140)),
            SystemCategoryItem("JA_N2", ContextUtils.getString(R.string.ui_text_141), ContextUtils.getString(R.string.ui_text_133), ContextUtils.getString(R.string.ui_text_142)),
            SystemCategoryItem("JA_N1", ContextUtils.getString(R.string.ui_text_143), ContextUtils.getString(R.string.ui_text_133), ContextUtils.getString(R.string.ui_text_144)),
            SystemCategoryItem("EN_TOEIC", ContextUtils.getString(R.string.ui_text_145), ContextUtils.getString(R.string.ui_text_146), ContextUtils.getString(R.string.ui_text_147)),
            SystemCategoryItem("EN_IELTS", ContextUtils.getString(R.string.ui_text_148), ContextUtils.getString(R.string.ui_text_146), ContextUtils.getString(R.string.ui_text_149))
        )
    }

    // Nhóm các danh mục lại theo Ngôn ngữ (Tiếng Anh / Tiếng Nhật)
    val groupedCategories = remember(categories) {
        categories.groupBy { it.languageGroup }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ContextUtils.getString(R.string.ui_text_150), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Nút để Admin thêm một danh mục mới
                    IconButton(onClick = { /* Xử lý thêm danh mục mới */ }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Category", tint = FlowPrimary)
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Duyệt qua từng nhóm ngôn ngữ
            groupedCategories.forEach { (languageGroup, itemsInCategory) ->
                stickyHeader {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = languageGroup,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = FlowPrimary,
                            letterSpacing = 1.sp
                        )
                    }
                }

                items(itemsInCategory) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navController.navigate("edit_system_cards/${item.code}")
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Translate, contentDescription = null, tint = FlowPrimary)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = item.title,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.description,
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        maxLines = 2
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}
