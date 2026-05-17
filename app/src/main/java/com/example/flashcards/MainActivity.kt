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
import com.google.firebase.firestore.FirebaseFirestore
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

        // Firebase Firestore — enable offline cache (giảm network wait khi mở app)
        FirebaseFirestore.getInstance().firestoreSettings =
            com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()

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
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    val navItemColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = FlowPrimary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedTextColor = FlowPrimary,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text("Home", fontSize = 10.sp) },
                        selected = currentRoute == "home",
                        onClick = { navController.navigate("home") },
                        colors = navItemColors
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(28.dp)) },
                        label = { Text("Create", fontSize = 10.sp) },
                        selected = currentRoute == "create_deck",
                        onClick = { navController.navigate("create_deck") },
                        colors = navItemColors
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.FolderOpen, contentDescription = null) },
                        label = { Text("Library", fontSize = 10.sp) },
                        selected = currentRoute == "library",
                        onClick = { navController.navigate("library") },
                        colors = navItemColors
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Explore, contentDescription = null) },
                        label = { Text("Explore", fontSize = 10.sp) },
                        selected = currentRoute == "explore",
                        onClick = { navController.navigate("explore") },
                        colors = navItemColors
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.PersonOutline, contentDescription = null) },
                        label = { Text("Profile", fontSize = 10.sp) },
                        selected = currentRoute == "profile",
                        onClick = { navController.navigate("profile") },
                        colors = navItemColors
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
    val folders by viewModel.folders.collectAsState()
    var selectedFolder by remember { mutableStateOf<com.example.flashcards.model.Folder?>(null) }

    val startDestination = if (FirebaseAuth.getInstance().currentUser != null) "home" else "auth"

    // Nếu user đã đăng nhập sẵn (không qua Auth screen), load data xã hội ngay
    LaunchedEffect(Unit) {
        if (FirebaseAuth.getInstance().currentUser != null) {
            viewModel.onUserSignedIn()
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("auth") {
            AuthScreen(
                onLoginSuccess = {
                    viewModel.loadData()
                    viewModel.onUserSignedIn()
                    navController.navigate("home") { popUpTo("auth") { inclusive = true } }
                },
                onRegisterSuccess = {
                    viewModel.loadData()
                    viewModel.onUserSignedIn()
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
                folders = folders,
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
                        onSuccess = { },
                        onError = { }
                    )
                },
                onCreateFolder = { name, emoji -> viewModel.createFolder(name, emoji) },
                onFolderClick = { folder ->
                    selectedFolder = folder
                    navController.navigate("folder_detail")
                }
            )
        }
        composable("folder_detail") {
            selectedFolder?.let { folder ->
                // Keep folder in sync with latest data from Firebase
                val liveFolder = folders.find { it.id == folder.id } ?: folder
                FolderDetailScreen(
                    folder = liveFolder,
                    allSets = studySets,
                    onBack = { navController.popBackStack() },
                    onSetSelected = { set ->
                        viewModel.selectSet(set)
                        navController.navigate("deck_detail")
                    },
                    onAddSet = { setId -> viewModel.addSetToFolder(liveFolder.id, setId) },
                    onRemoveSet = { setId -> viewModel.removeSetFromFolder(liveFolder.id, setId) },
                    onRenameFolder = { name, emoji -> viewModel.renameFolder(liveFolder.id, name, emoji) },
                    onDeleteFolder = {
                        viewModel.deleteFolder(liveFolder.id)
                        navController.popBackStack()
                    }
                )
            } ?: run {
                Text("Folder not found", modifier = Modifier.padding(16.dp))
            }
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