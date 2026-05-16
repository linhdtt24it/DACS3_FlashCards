package com.example.flashcards.view

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha
import com.example.flashcards.ui.theme.*
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalInfoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val prefs = context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)

    val userEmail = auth.currentUser?.email ?: "No email"
    var inputName by remember { mutableStateOf(auth.currentUser?.displayName ?: "") }
    var inputDob by remember { mutableStateOf(prefs.getString("user_dob", "") ?: "") }
    var inputPhone by remember { mutableStateOf(prefs.getString("user_phone", "") ?: "") }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Personal Information", fontWeight = FontWeight.Bold, color = FlowTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = FlowTextPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FlowBackground)
            )
        },
        containerColor = FlowBackground
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp).verticalScroll(rememberScrollState())) {
            
            // Email (Read Only)
            Text("Email", style = MaterialTheme.typography.titleMedium, color = FlowTextPrimary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = userEmail,
                onValueChange = { },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = FlowSurface,
                    focusedContainerColor = FlowSurface,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    disabledTextColor = FlowTextSecondary,
                    disabledContainerColor = FlowSurface
                ),
                enabled = false
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Display Name
            Text("Display Name", style = MaterialTheme.typography.titleMedium, color = FlowTextPrimary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = inputName,
                onValueChange = { inputName = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter your name") },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = FlowSurface,
                    focusedContainerColor = FlowSurface,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = FlowPrimary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Date of Birth
            Text("Date of Birth", style = MaterialTheme.typography.titleMedium, color = FlowTextPrimary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = inputDob,
                onValueChange = { inputDob = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("DD/MM/YYYY") },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = FlowSurface,
                    focusedContainerColor = FlowSurface,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = FlowPrimary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Phone Number
            Text("Phone Number", style = MaterialTheme.typography.titleMedium, color = FlowTextPrimary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = inputPhone,
                onValueChange = { inputPhone = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter your phone number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = FlowSurface,
                    focusedContainerColor = FlowSurface,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = FlowPrimary
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val user = auth.currentUser
                    if (user != null && inputName.isNotBlank()) {
                        isLoading = true
                        
                        // Save local preferences
                        prefs.edit()
                            .putString("user_dob", inputDob)
                            .putString("user_phone", inputPhone)
                            .apply()
                            
                        // Save Firebase profile
                        val request = UserProfileChangeRequest.Builder().setDisplayName(inputName).build()
                        user.updateProfile(request).addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                Toast.makeText(context, "Information updated successfully", Toast.LENGTH_SHORT).show()
                                onBack()
                            } else {
                                Toast.makeText(context, "Failed to update profile", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary),
                enabled = !isLoading && inputName.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    var oldPwdVisible by remember { mutableStateOf(false) }
    var newPwdVisible by remember { mutableStateOf(false) }
    var confirmPwdVisible by remember { mutableStateOf(false) }
    
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Password & Security", fontWeight = FontWeight.Bold, color = FlowTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = FlowTextPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FlowBackground)
            )
        },
        containerColor = FlowBackground
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp).verticalScroll(rememberScrollState())) {
            
            if (errorMessage != null) {
                Text(errorMessage!!, color = FlowWarning, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 16.dp))
            }

            // Old Password
            Text("Old Password", style = MaterialTheme.typography.titleMedium, color = FlowTextPrimary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = oldPassword,
                onValueChange = { oldPassword = it; errorMessage = null },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter old password") },
                shape = RoundedCornerShape(12.dp),
                visualTransformation = if (oldPwdVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { oldPwdVisible = !oldPwdVisible }) {
                        Icon(if (oldPwdVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null, tint = FlowTextSecondary)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = FlowSurface,
                    focusedContainerColor = FlowSurface,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = FlowPrimary
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // New Password
            Text("New Password", style = MaterialTheme.typography.titleMedium, color = FlowTextPrimary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it; errorMessage = null },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter new password") },
                shape = RoundedCornerShape(12.dp),
                visualTransformation = if (newPwdVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { newPwdVisible = !newPwdVisible }) {
                        Icon(if (newPwdVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null, tint = FlowTextSecondary)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = FlowSurface,
                    focusedContainerColor = FlowSurface,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = FlowPrimary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Confirm Password
            Text("Confirm New Password", style = MaterialTheme.typography.titleMedium, color = FlowTextPrimary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; errorMessage = null },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Confirm new password") },
                shape = RoundedCornerShape(12.dp),
                visualTransformation = if (confirmPwdVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { confirmPwdVisible = !confirmPwdVisible }) {
                        Icon(if (confirmPwdVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null, tint = FlowTextSecondary)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = FlowSurface,
                    focusedContainerColor = FlowSurface,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = FlowPrimary
                )
            )

            Spacer(modifier = Modifier.height(32.dp))
            
            val isFormValid = oldPassword.isNotEmpty() && newPassword.length >= 6 && confirmPassword == newPassword

            Button(
                onClick = {
                    if (newPassword != confirmPassword) {
                        errorMessage = "New passwords do not match"
                        return@Button
                    }
                    val user = auth.currentUser
                    if (user != null && user.email != null) {
                        isLoading = true
                        val credential = EmailAuthProvider.getCredential(user.email!!, oldPassword)
                        
                        user.reauthenticate(credential).addOnCompleteListener { authTask ->
                            if (authTask.isSuccessful) {
                                user.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                                    isLoading = false
                                    if (updateTask.isSuccessful) {
                                        Toast.makeText(context, "Password updated successfully", Toast.LENGTH_SHORT).show()
                                        onBack()
                                    } else {
                                        errorMessage = "Failed to update: ${updateTask.exception?.message}"
                                    }
                                }
                            } else {
                                isLoading = false
                                errorMessage = "Incorrect old password"
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary),
                enabled = !isLoading && isFormValid
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Update Password", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningPreferencesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)

    var dailyGoal by remember { mutableStateOf(prefs.getInt("daily_goal", 50).toString()) }
    var reviewAlgorithm by remember { mutableStateOf(prefs.getString("review_algorithm", "Standard") ?: "Standard") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Learning Preferences", fontWeight = FontWeight.Bold, color = FlowTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = FlowTextPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FlowBackground)
            )
        },
        containerColor = FlowBackground
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            // Daily Goal
            Text("Daily Goal (cards per day)", style = MaterialTheme.typography.titleMedium, color = FlowTextPrimary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = dailyGoal,
                onValueChange = { dailyGoal = it },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = FlowSurface,
                    focusedContainerColor = FlowSurface,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = FlowPrimary
                )
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Review Algorithm
            Text("Review Algorithm", style = MaterialTheme.typography.titleMedium, color = FlowTextPrimary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            val options = listOf("Standard", "Quick Review", "Strict")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = FlowSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    options.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { reviewAlgorithm = option }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (option == reviewAlgorithm),
                                onClick = { reviewAlgorithm = option },
                                colors = RadioButtonDefaults.colors(selectedColor = FlowPrimary)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(option, style = MaterialTheme.typography.bodyLarge, color = FlowTextPrimary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    val goalInt = dailyGoal.toIntOrNull() ?: 50
                    prefs.edit()
                        .putInt("daily_goal", goalInt)
                        .putString("review_algorithm", reviewAlgorithm)
                        .apply()
                    Toast.makeText(context, "Preferences saved", Toast.LENGTH_SHORT).show()
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary)
            ) {
                Text("Save Preferences", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)
    
    var notificationsEnabled by remember { mutableStateOf(prefs.getBoolean("notifications_enabled", true)) }
    var notifHour by remember { mutableIntStateOf(prefs.getInt("notif_hour", 20)) }
    var notifMinute by remember { mutableIntStateOf(prefs.getInt("notif_minute", 0)) }
    var soundEnabled by remember { mutableStateOf(prefs.getBoolean("notif_sound", true)) }
    var vibrateEnabled by remember { mutableStateOf(prefs.getBoolean("notif_vibrate", true)) }

    val timePickerDialog = android.app.TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            notifHour = hourOfDay
            notifMinute = minute
        },
        notifHour,
        notifMinute,
        true // 24-hour format
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.Bold, color = FlowTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = FlowTextPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FlowBackground)
            )
        },
        containerColor = FlowBackground
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            // Master Switch
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Enable Notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = FlowTextPrimary, modifier = Modifier.weight(1f))
                Switch(
                    checked = notificationsEnabled, 
                    onCheckedChange = { notificationsEnabled = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = FlowPrimary)
                )
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = FlowSurface)
            
            // Sub-settings (opacity changes if disabled)
            val alphaValue = if (notificationsEnabled) 1f else 0.5f
            Column(modifier = Modifier.alpha(alphaValue)) {
                Text("Daily Reminder", style = MaterialTheme.typography.titleSmall, color = FlowTextSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(enabled = notificationsEnabled) { timePickerDialog.show() }.padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Reminder Time", style = MaterialTheme.typography.bodyLarge, color = FlowTextPrimary, modifier = Modifier.weight(1f))
                    val timeText = String.format("%02d:%02d", notifHour, notifMinute)
                    Text(timeText, style = MaterialTheme.typography.bodyLarge, color = FlowPrimary, fontWeight = FontWeight.Bold)
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Play Sound", style = MaterialTheme.typography.bodyLarge, color = FlowTextPrimary, modifier = Modifier.weight(1f))
                    Switch(
                        checked = soundEnabled, 
                        onCheckedChange = { soundEnabled = it }, 
                        enabled = notificationsEnabled,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = FlowPrimary)
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vibrate", style = MaterialTheme.typography.bodyLarge, color = FlowTextPrimary, modifier = Modifier.weight(1f))
                    Switch(
                        checked = vibrateEnabled, 
                        onCheckedChange = { vibrateEnabled = it }, 
                        enabled = notificationsEnabled,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = FlowPrimary)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    prefs.edit()
                        .putBoolean("notifications_enabled", notificationsEnabled)
                        .putInt("notif_hour", notifHour)
                        .putInt("notif_minute", notifMinute)
                        .putBoolean("notif_sound", soundEnabled)
                        .putBoolean("notif_vibrate", vibrateEnabled)
                        .apply()
                    Toast.makeText(context, "Notification settings saved", Toast.LENGTH_SHORT).show()
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary)
            ) {
                Text("Save Settings", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            }
        }
    }
}
