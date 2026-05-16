package com.example.flashcards

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.flashcards.ui.theme.*
import com.example.flashcards.view.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
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
            if (currentRoute !in listOf("auth", "study_session", "quiz_session", "deck_detail", "create_deck")) {
                NavigationBar(
                    containerColor = FlowBackground,
                    contentColor = FlowTextSecondary
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text("Home", fontSize = 10.sp) },
                        selected = currentRoute == "home",
                        onClick = { navController.navigate("home") },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = FlowPrimary, unselectedIconColor = FlowTextSecondary, selectedTextColor = FlowPrimary, unselectedTextColor = FlowTextSecondary, indicatorColor = Color.Transparent)
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(28.dp)) },
                        label = { Text("Create", fontSize = 10.sp) },
                        selected = currentRoute == "create_deck",
                        onClick = { navController.navigate("create_deck") },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = FlowPrimary, unselectedIconColor = FlowTextSecondary, selectedTextColor = FlowPrimary, unselectedTextColor = FlowTextSecondary, indicatorColor = Color.Transparent)
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.FolderOpen, contentDescription = null) },
                        label = { Text("Library", fontSize = 10.sp) },
                        selected = currentRoute == "library",
                        onClick = { navController.navigate("library") },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = FlowPrimary, unselectedIconColor = FlowTextSecondary, selectedTextColor = FlowPrimary, unselectedTextColor = FlowTextSecondary, indicatorColor = Color.Transparent)
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Explore, contentDescription = null) },
                        label = { Text("Explore", fontSize = 10.sp) },
                        selected = currentRoute == "explore",
                        onClick = { navController.navigate("explore") },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = FlowPrimary, unselectedIconColor = FlowTextSecondary, selectedTextColor = FlowPrimary, unselectedTextColor = FlowTextSecondary, indicatorColor = Color.Transparent)
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.PersonOutline, contentDescription = null) },
                        label = { Text("Profile", fontSize = 10.sp) },
                        selected = currentRoute == "profile",
                        onClick = { navController.navigate("profile") },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = FlowPrimary, unselectedIconColor = FlowTextSecondary, selectedTextColor = FlowPrimary, unselectedTextColor = FlowTextSecondary, indicatorColor = Color.Transparent)
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
    val publicStudySets by viewModel.publicStudySets.collectAsState()

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
            val user = FirebaseAuth.getInstance().currentUser
            val userName = user?.displayName ?: user?.email?.substringBefore("@") ?: "User"
            val notifications by viewModel.notifications.collectAsState()
            val unreadNotifCount = notifications.count { !it.isRead }

            HomeScreen(
                userName = userName,
                studySets = studySets,
                unreadNotifCount = unreadNotifCount,
                onAddDeck = { title, desc -> viewModel.addStudySet(title, desc) },
                onEditDeck = { id -> navController.navigate("edit_deck/$id") },
                onDeleteDeck = { id -> viewModel.deleteStudySet(id) },
                onSetSelected = { set ->
                    viewModel.selectSet(set)
                    navController.navigate("deck_detail")
                },
                onQuizDeck = { set ->
                    viewModel.selectSet(set)
                    navController.navigate("quiz_session")
                },
                onNotificationsClick = { navController.navigate("notifications") }
            )
        }
        composable("create_deck") {
            val user = FirebaseAuth.getInstance().currentUser
            val creatorId = user?.uid ?: ""
            val creatorName = user?.displayName ?: user?.email?.substringBefore("@") ?: "Unknown User"

            val emptySet = remember {
                com.example.flashcards.model.StudySet(
                    id = java.util.UUID.randomUUID().toString(),
                    title = "",
                    description = "",
                    cards = emptyList(),
                    isPublic = false,
                    shareCode = null,
                    creatorId = creatorId,
                    creatorName = creatorName,
                    languageCode = "en"  // 👈 Thêm languageCode mặc định
                )
            }

            DeckEditorScreen(
                studySet = emptySet,
                isCreateMode = true,
                onSave = { updatedSet ->
                    val finalSet = if (updatedSet.isPublic && updatedSet.shareCode == null) {
                        val charPool : List<Char> = ('A'..'Z') + ('0'..'9')
                        val code = (1..6).map { kotlin.random.Random.nextInt(0, charPool.size).let { charPool[it] } }.joinToString("")
                        updatedSet.copy(shareCode = code)
                    } else updatedSet

                    viewModel.updateStudySet(finalSet)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
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
                    navController.navigate("deck_detail")
                },
                onQuizDeck = { set ->
                    viewModel.selectSet(set)
                    navController.navigate("quiz_session")
                },
                onImportDeck = { code ->
                    viewModel.importDeckByCode(
                        code = code,
                        onSuccess = { /* Handle success if needed */ },
                        onError = { /* Handle error if needed */ }
                    )
                }
            )
        }
        composable("explore") {
            LaunchedEffect(Unit) {
                viewModel.loadPublicDecks()
            }
            ExploreScreen(
                publicDecks = publicStudySets,
                onSearch = { query -> viewModel.searchPublicDecks(query) },
                onImportDeck = { code ->
                    viewModel.importDeckByCode(
                        code = code,
                        onSuccess = {
                            navController.navigate("library") {
                                popUpTo("explore") { inclusive = false }
                            }
                        },
                        onError = { /* Show error toast/snackbar */ }
                    )
                },
                onRateDeck = { setId, rating ->
                    viewModel.ratePublicStudySet(setId, rating)
                },
                onDeckClick = { set ->
                    viewModel.selectSet(set)
                    navController.navigate("deck_detail")
                }
            )
        }
        composable("deck_detail") {
            selectedSet?.let { studySet ->
                LaunchedEffect(studySet.id) {
                    viewModel.loadComments(studySet.id)
                }
                val comments by viewModel.currentComments.collectAsState()

                DeckDetailScreen(
                    studySet = studySet,
                    userName = FirebaseAuth.getInstance().currentUser?.displayName ?: FirebaseAuth.getInstance().currentUser?.email?.substringBefore("@") ?: "User",
                    currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                    comments = comments,
                    onAddComment = { content -> viewModel.addComment(studySet.id, content) },
                    onBack = { navController.popBackStack() },
                    onStudyFlashcards = { navController.navigate("study_session") },
                    onQuiz = { navController.navigate("quiz_session") },
                    onTypingQuiz = { navController.navigate("typing_session") },
                    onMatch = { /* TODO Phase 2 */ },
                    onEditDeck = { navController.navigate("edit_deck/${studySet.id}") },
                    onDeleteDeck = {
                        viewModel.deleteStudySet(studySet.id)
                        navController.popBackStack()
                    }
                )
            } ?: run {
                Text("Deck not found", modifier = Modifier.padding(16.dp))
            }
        }
        composable("study_session") {
            selectedSet?.let { studySet ->
                StudySessionScreen(
                    studySet = studySet,
                    onBack = { navController.popBackStack() },
                    // 👈 LẤY của bạn tôi: thêm languageCode parameter
                    onSpeak = { text: String, lang: String ->
                        tts?.language = Locale.forLanguageTag(lang)
                        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
                    },
                    onUpdateCard = { card, quality ->
                        viewModel.updateCardQuality(card, quality)
                        viewModel.recordStudySession(
                            cardsStudied = 1,
                            correct = if (quality > 2) 1 else 0,
                            wrong = if (quality <= 2) 1 else 0
                        )
                    }
                )
            } ?: run {
                Text("Deck not found", modifier = Modifier.padding(16.dp))
            }
        }
        composable("quiz_session") {
            selectedSet?.let { studySet ->
                QuizScreen(
                    studySet = studySet,
                    onBack = { navController.popBackStack() }
                )
            } ?: run {
                Text("Deck not found", modifier = Modifier.padding(16.dp))
            }
        }
        composable("typing_session") {
            selectedSet?.let { studySet ->
                TypingQuizScreen(
                    studySet = studySet,
                    onBack = { navController.popBackStack() },
                    onRecordResult = { correct, wrong ->
                        viewModel.recordStudySession(
                            cardsStudied = correct + wrong,
                            correct = correct,
                            wrong = wrong
                        )
                    }
                )
            } ?: run {
                Text("Deck not found", modifier = Modifier.padding(16.dp))
            }
        }
        composable("stats") {
            val userStats by viewModel.userStats.collectAsState()
            StatisticsScreen(userStats = userStats, onBack = { navController.popBackStack() })
        }
        composable("notifications") {
            val notifications by viewModel.notifications.collectAsState()
            com.example.flashcards.view.NotificationInboxScreen(
                notifications = notifications,
                onBack = { navController.popBackStack() },
                onNotificationClick = { notif ->
                    viewModel.markNotificationAsRead(notif.id)
                    // Optionally navigate to the deck
                }
            )
        }
        composable("profile") {
            val user = FirebaseAuth.getInstance().currentUser
            ProfileScreen(
                userName = user?.displayName ?: user?.email?.substringBefore("@") ?: "User",
                userEmail = user?.email ?: "No email",
                onLogout = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate("auth") { popUpTo(0) }
                },
                onNavigateToPersonalInfo = { navController.navigate("personal_info") },
                onNavigateToSecurity = { navController.navigate("security") },
                onNavigateToNotifications = { navController.navigate("notification_settings") },
                onNavigateToLearningPrefs = { navController.navigate("learning_prefs") },
                onNavigateToStats = { navController.navigate("stats") }
            )
        }
        composable("personal_info") {
            com.example.flashcards.view.PersonalInfoScreen(onBack = { navController.popBackStack() })
        }
        composable("security") {
            com.example.flashcards.view.SecurityScreen(onBack = { navController.popBackStack() })
        }
        composable("notification_settings") {
            com.example.flashcards.view.NotificationSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable("learning_prefs") {
            com.example.flashcards.view.LearningPreferencesScreen(onBack = { navController.popBackStack() })
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