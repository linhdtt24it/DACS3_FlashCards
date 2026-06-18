package com.example.flashcards.view

import com.example.flashcards.utils.ContextUtils
import com.example.flashcards.R
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.flashcards.utils.CryptoUtils
import com.example.flashcards.viewmodel.AdminPackageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPackageDetailScreen(
    navController: NavController,
    packageName: String,
    viewModel: AdminPackageViewModel = viewModel()
) {
    val users by viewModel.users.collectAsState()
    val allPackages by viewModel.packages.collectAsState()

    val filteredUsers = remember(users) {
        users.filter { userData ->
            val encryptedRole = userData["role"] as? String
            CryptoUtils.decrypt(encryptedRole) == "user"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(ContextUtils.getString(R.string.ui_text_161), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(ContextUtils.getString(R.string.ui_text_162), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (filteredUsers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text(ContextUtils.getString(R.string.ui_text_163), color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(filteredUsers) { userData ->
                    val uid = userData["uid"] as? String ?: ""
                    val email = CryptoUtils.decrypt(userData["email"] as? String) ?: "no-email"
                    val name = CryptoUtils.decrypt(userData["name"] as? String)?.ifBlank { ContextUtils.getString(R.string.ui_text_164) } ?: ContextUtils.getString(R.string.ui_text_164)
                    val subscribedPackages = userData["subscribedPackages"] as? List<String> ?: listOf("FREE")

                    UserMultiPackageCard(
                        name = name,
                        email = email,
                        subscribedPackages = subscribedPackages,
                        allAvailablePackages = allPackages,
                        onTogglePackage = { pkgKey, shouldAdd ->
                            if (uid.isNotEmpty()) {
                                viewModel.togglePackageForUser(uid, pkgKey, shouldAdd)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun UserMultiPackageCard(
    name: String,
    email: String,
    subscribedPackages: List<String>,
    allAvailablePackages: List<com.example.flashcards.viewmodel.PackageItem>,
    onTogglePackage: (String, Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(email, color = Color.Gray, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(12.dp))

            Text(ContextUtils.getString(R.string.ui_text_165), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            // SỬA LỖI: Thay FlowRow bằng Row + Scroll để tránh crash NoSuchMethodError
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                subscribedPackages.forEach { pkgKey ->
                    AssistChip(
                        onClick = { },
                        label = { Text(pkgKey, fontSize = 10.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            allAvailablePackages.filter { it.key != "FREE" }.forEach { pkg ->
                val isSubscribed = subscribedPackages.contains(pkg.key)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(pkg.name, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Button(
                        onClick = { onTogglePackage(pkg.key, !isSubscribed) },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isSubscribed) Color.Red else Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(if (isSubscribed) ContextUtils.getString(R.string.ui_text_166) else ContextUtils.getString(R.string.ui_text_167), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
