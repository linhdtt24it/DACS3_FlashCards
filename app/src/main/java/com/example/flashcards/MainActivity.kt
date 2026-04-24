package com.example.flashcards

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.flashcards.ui.theme.FlashCardsTheme
import com.example.flashcards.view.*
import com.example.flashcards.viewmodel.FlashcardViewModel
import java.util.Locale

class MainActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        tts = TextToSpeech(this) { status ->
            if (status != TextToSpeech.ERROR) {
                tts?.language = Locale.US
            }
        }

        setContent {
            FlashCardsTheme {
                MainApp(tts)
            }
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@Composable
fun MainApp(tts: TextToSpeech?, viewModel: FlashcardViewModel = viewModel()) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute !in listOf("login", "register", "study")) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text("Trang chủ") },
                        selected = currentRoute == "dashboard",
                        onClick = { navController.navigate("dashboard") }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                        label = { Text("Bộ thẻ") },
                        selected = currentRoute == "sets",
                        onClick = { navController.navigate("sets") }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Info, contentDescription = null) },
                        label = { Text("Thống kê") },
                        selected = currentRoute == "stats",
                        onClick = { navController.navigate("stats") }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            AppNavHost(navController = navController, viewModel = viewModel, tts = tts)
        }
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    viewModel: FlashcardViewModel,
    tts: TextToSpeech?
) {
    // Thu thập State từ ViewModel
    val studySets by viewModel.studySets.collectAsState()
    val selectedSet by viewModel.selectedSet.collectAsState()

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onLoginSuccess = { navController.navigate("dashboard") { popUpTo("login") { inclusive = true } } },
                onNavigateToRegister = { navController.navigate("register") }
            )
        }
        composable("register") {
            RegisterScreen(
                onRegisterSuccess = { navController.navigate("login") { popUpTo("register") { inclusive = true } } },
                onNavigateToLogin = { navController.navigate("login") { popUpTo("register") { inclusive = true } } }
            )
        }
        composable("dashboard") {
            DashboardScreen(onStartStudy = { navController.navigate("sets") })
        }
        composable("sets") {
            StudySetsListScreen(
                studySets = studySets,
                onSetSelected = { set ->
                    viewModel.selectSet(set)
                    navController.navigate("study")
                }
            )
        }
        composable("study") {
            selectedSet?.let { studySet ->
                StudySessionScreen(
                    studySet = studySet,
                    onBack = { navController.popBackStack() },
                    onSpeak = { text ->
                        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
                    },
                    onUpdateCard = { card, quality ->
                        viewModel.updateCardQuality(card, quality)
                    }
                )
            } ?: run {
                Text("Không tìm thấy bộ thẻ", modifier = Modifier.padding(16.dp))
            }
        }
        composable("stats") {
            StatisticsScreen()
        }
    }
}
