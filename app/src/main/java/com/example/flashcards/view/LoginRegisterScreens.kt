package com.example.flashcards.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flashcards.ui.theme.FlowPrimary
import com.example.flashcards.viewmodel.AuthViewModel
import com.example.flashcards.viewmodel.AuthState
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AuthScreen(
    onLoginSuccess: () -> Unit, 
    onRegisterSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Log In", "Sign Up")
    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            if (selectedTabIndex == 0) onLoginSuccess() else onRegisterSuccess()
            viewModel.resetState()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Tab Row
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = FlowPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            height = 2.dp,
                            color = FlowPrimary
                        )
                    },
                    divider = { HorizontalDivider(color = Color(0xFFF1F5F9)) }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { 
                                Text(
                                    title, 
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTabIndex == index) FlowPrimary else Color(0xFF64748B)
                                ) 
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Heading
                Text("FlowCards", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, color = FlowPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (selectedTabIndex == 0) "Welcome back" else "Start your journey", 
                    style = MaterialTheme.typography.bodyLarge, 
                    color = Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Error Message
                if (authState is AuthState.Error) {
                    Text(
                        text = (authState as AuthState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // Forms
                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    if (selectedTabIndex == 0) {
                        LoginForm(
                            isLoading = authState is AuthState.Loading,
                            onLoginClick = { email, password -> viewModel.login(email, password) }
                        )
                    } else {
                        RegisterForm(
                            isLoading = authState is AuthState.Loading,
                            onRegisterClick = { name, email, password -> viewModel.register(name, email, password) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Social Logins Wrapper
                SocialLogins()

                Spacer(modifier = Modifier.height(32.dp))

                // Footer
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (selectedTabIndex == 0) {
                        Text("Don't have an account? ", color = Color(0xFF64748B), style = MaterialTheme.typography.bodyMedium)
                        Text("Start for free", color = FlowPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { selectedTabIndex = 1 }, style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text("Already have an account? ", color = Color(0xFF64748B), style = MaterialTheme.typography.bodyMedium)
                        Text("Log In", color = FlowPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { selectedTabIndex = 0 }, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun LoginForm(isLoading: Boolean, onLoginClick: (String, String) -> Unit) {
    var email by remember { mutableStateOf(TextFieldValue("")) }
    var password by remember { mutableStateOf(TextFieldValue("")) }
    var passwordVisible by remember { mutableStateOf(false) }

    Column {
        Text("Email Address", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, color = Color(0xFF1E293B))
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("name@example.com", color = Color(0xFF94A3B8)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF94A3B8)) },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color(0xFFE2E8F0),
                focusedBorderColor = FlowPrimary
            ),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Password", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, color = Color(0xFF1E293B))
            Text("Forgot password?", style = MaterialTheme.typography.labelMedium, color = FlowPrimary, fontWeight = FontWeight.Medium, modifier = Modifier.clickable { })
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("••••••••", color = Color(0xFF94A3B8)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF94A3B8)) },
            trailingIcon = {
                val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(icon, contentDescription = null, tint = Color(0xFF94A3B8))
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color(0xFFE2E8F0),
                focusedBorderColor = FlowPrimary
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onLoginClick(email.text, password.text) },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Sign In", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun RegisterForm(isLoading: Boolean, onRegisterClick: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf(TextFieldValue("")) }
    var email by remember { mutableStateOf(TextFieldValue("")) }
    var password by remember { mutableStateOf(TextFieldValue("")) }

    Column {
        Text("Full Name", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, color = Color(0xFF1E293B))
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = { Text("Alex Johnson", color = Color(0xFF94A3B8)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF94A3B8)) },
            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0)),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Text("Email Address", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, color = Color(0xFF1E293B))
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("name@example.com", color = Color(0xFF94A3B8)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF94A3B8)) },
            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0)),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Text("Password", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, color = Color(0xFF1E293B))
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("••••••••", color = Color(0xFF94A3B8)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            visualTransformation = PasswordVisualTransformation(),
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF94A3B8)) },
            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0)),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onRegisterClick(name.text, email.text, password.text) },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Sign Up", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun SocialLogins() {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFF1F5F9))
            Text("OR", color = Color(0xFF64748B), modifier = Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.labelMedium)
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFF1F5F9))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = { /* TODO Firebase Google Login */ },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
            ) {
                Icon(Icons.Default.Android, contentDescription = "Google", tint = Color.Unspecified) // PlaceHolder icon
                Spacer(modifier = Modifier.width(8.dp))
                Text("Google", color = Color(0xFF1E293B), fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = { /* TODO Firebase FB Login */ },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2))
            ) {
                Icon(Icons.Default.ThumbUp, contentDescription = "Facebook", tint = Color.White) // Placeholder icon
                Spacer(modifier = Modifier.width(8.dp))
                Text("Facebook", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
