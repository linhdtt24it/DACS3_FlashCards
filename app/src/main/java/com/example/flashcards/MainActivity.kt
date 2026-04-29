package com.example.flashcards

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.flashcards.ui.theme.FlashCardsTheme
import com.example.flashcards.ui.theme.FlowPrimary
import com.example.flashcards.view.*
import com.google.firebase.auth.FirebaseAuth
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
            if (currentRoute !in listOf("auth", "study_session")) {
                NavigationBar(
                    containerColor = Color.White,
                    contentColor = Color(0xFF64748B)
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text("Home") },
                        selected = currentRoute == "home",
                        onClick = { navController.navigate("home") },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = FlowPrimary, selectedTextColor = FlowPrimary, indicatorColor = FlowPrimary.copy(alpha = 0.1f))
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                        label = { Text("Library") },
                        selected = currentRoute == "library",
                        onClick = { navController.navigate("library") },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = FlowPrimary, selectedTextColor = FlowPrimary, indicatorColor = FlowPrimary.copy(alpha = 0.1f))
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                        label = { Text("Stats") },
                        selected = currentRoute == "stats",
                        onClick = { navController.navigate("stats") },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = FlowPrimary, selectedTextColor = FlowPrimary, indicatorColor = FlowPrimary.copy(alpha = 0.1f))
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = null) },
                        label = { Text("Profile") },
                        selected = currentRoute == "profile",
                        onClick = { navController.navigate("profile") },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = FlowPrimary, selectedTextColor = FlowPrimary, indicatorColor = FlowPrimary.copy(alpha = 0.1f))
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

    val startDestination = if (FirebaseAuth.getInstance().currentUser != null) "home" else "auth"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("auth") {
            AuthScreen(
                onLoginSuccess = { 
                    viewModel.loadData()
                    navController.navigate("home") { popUpTo("auth") { inclusive = true } } 
                },
                onRegisterSuccess = { 
                    viewModel.loadData()
                    navController.navigate("home") { popUpTo("auth") { inclusive = true } } 
                }
            )
        }
        composable("home") {
            HomeScreen(
                userName = FirebaseAuth.getInstance().currentUser?.displayName ?: FirebaseAuth.getInstance().currentUser?.email?.substringBefore("@") ?: "User",
                studySets = studySets,
                onAddDeck = { title, desc -> viewModel.addStudySet(title, desc) },
                onEditDeck = { id -> navController.navigate("edit_deck/$id") },
                onDeleteDeck = { id -> viewModel.deleteStudySet(id) },
                onSetSelected = {
                    viewModel.selectSet(it)
                    navController.navigate("study_session")
                }
            )
        }
        composable("library") {
            LibraryScreen(
                studySets = studySets,
                onAddDeck = { title, desc -> viewModel.addStudySet(title, desc) },
                onEditDeck = { id -> navController.navigate("edit_deck/$id") },
                onDeleteDeck = { id -> viewModel.deleteStudySet(id) },
                onSetSelected = { set ->
                    viewModel.selectSet(set)
                    navController.navigate("study_session")
                }
            )
        }
        composable("study_session") {
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
        composable("profile") {
            val user = FirebaseAuth.getInstance().currentUser
            ProfileScreen(
                userName = user?.displayName ?: user?.email?.substringBefore("@") ?: "User",
                userEmail = user?.email ?: "No email",
                onLogout = { 
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate("auth") { popUpTo(0) } 
                }
            )
        }
        composable("edit_deck/{deckId}") { backStackEntry ->
            val deckId = backStackEntry.arguments?.getString("deckId")
            val studySetToEdit = studySets.find { it.id == deckId }
            if (studySetToEdit != null) {
                DeckEditorScreen(
                    studySet = studySetToEdit,
                    onSave = { updatedSet -> viewModel.updateStudySet(updatedSet) },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
