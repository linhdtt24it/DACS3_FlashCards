package com.example.flashcards

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.flashcards.ui.theme.*
import com.example.flashcards.view.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.flashcards.viewmodel.FlashcardViewModel
import com.example.flashcards.viewmodel.AuthViewModel
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

fun navigateByRole(uid: String, navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    
    db.collection("users").document(uid).get()
        .addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val role = document.getString("role") ?: "user"
                
                if (role == "admin") {
                    navController.navigate("admin_dashboard_screen") {
                        popUpTo("login_screen") { inclusive = true }
                    }
                } else {
                    navController.navigate("home_screen") {
                        popUpTo("login_screen") { inclusive = true }
                    }
                }
            }
        }
}

@Composable
fun MainApp(
    tts: TextToSpeech?, 
    viewModel: FlashcardViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val userRole by authViewModel.currentUserRole.collectAsState()

    Scaffold(
        bottomBar = {
            if (userRole != "admin" && currentRoute !in listOf(
                "login_screen", "study_session", "quiz_session", "deck_detail", 
                "create_deck", "battle_session", "admin_dashboard_screen", 
                "manage_users", "manage_system_decks", "folder_detail",
                "manage_packages", "package_detail/{packageName}"
            )) {
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
                        selected = currentRoute == "home_screen",
                        onClick = { navController.navigate("home_screen") },
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
            AppNavHost(navController = navController, viewModel = viewModel, authViewModel = authViewModel, tts = tts)
        }
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    viewModel: FlashcardViewModel,
    authViewModel: AuthViewModel,
    tts: TextToSpeech?
) {
    val studySets by viewModel.studySets.collectAsState()
    val selectedSet by viewModel.selectedSet.collectAsState()
    val publicStudySets by viewModel.publicStudySets.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val userStats by viewModel.userStats.collectAsState()
    val userRole by authViewModel.currentUserRole.collectAsState()
    
    var selectedFolder by remember { mutableStateOf<com.example.flashcards.model.Folder?>(null) }
    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser
    val currentUserId = user?.uid ?: ""
    val userName = user?.displayName ?: user?.email?.substringBefore("@") ?: "User"
    val userEmail = user?.email ?: ""

    val startDestination = if (currentUserId.isNotEmpty()) {
        if (userRole == "admin") "admin_dashboard_screen" else "home_screen"
    } else "login_screen"

    LaunchedEffect(Unit) {
        if (currentUserId.isNotEmpty()) {
            viewModel.onUserSignedIn()
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login_screen") {
            AuthScreen(
                onLoginSuccess = {
                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                    if (uid != null) {
                        viewModel.loadData()
                        viewModel.onUserSignedIn()
                        navigateByRole(uid, navController)
                    }
                },
                onRegisterSuccess = {
                    viewModel.loadData()
                    viewModel.onUserSignedIn()
                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                    if (uid != null) {
                        navigateByRole(uid, navController)
                    }
                }
            )
        }

        composable("home_screen") {
            if (userRole == "admin") {
                LaunchedEffect(Unit) {
                    navController.navigate("admin_dashboard_screen") {
                        popUpTo("home_screen") { inclusive = true }
                    }
                }
            } else {
                val notifications by viewModel.notifications.collectAsState()
                val unreadNotifCount = notifications.count { !it.isRead }

                HomeScreen(
                    userName = userName,
                    studySets = studySets,
                    userStats = userStats,
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
        }

        composable("library") {
            if (userRole == "admin") {
                LaunchedEffect(Unit) {
                    navController.navigate("admin_dashboard_screen") {
                        popUpTo("library") { inclusive = true }
                    }
                }
            } else {
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
                        viewModel.importDeckByCode(code,
                            onSuccess = { Toast.makeText(context, "Success!", Toast.LENGTH_SHORT).show() },
                            onError = { Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show() }
                        )
                    },
                    onCreateFolder = { name, emoji -> viewModel.createFolder(name, emoji) },
                    onFolderClick = { folder ->
                        selectedFolder = folder
                        navController.navigate("folder_detail")
                    }
                )
            }
        }

        composable("folder_detail") {
            selectedFolder?.let { folder ->
                FolderDetailScreen(
                    folder = folder,
                    allSets = studySets,
                    onBack = { navController.popBackStack() },
                    onSetSelected = { set ->
                        viewModel.selectSet(set)
                        navController.navigate("deck_detail")
                    },
                    onAddSet = { setId -> viewModel.addSetToFolder(folder.id, setId) },
                    onRemoveSet = { setId -> viewModel.removeSetFromFolder(folder.id, setId) },
                    onRenameFolder = { n, e -> viewModel.renameFolder(folder.id, n, e) },
                    onDeleteFolder = { viewModel.deleteFolder(folder.id); navController.popBackStack() }
                )
            }
        }

        composable("explore") {
            if (userRole == "admin") {
                LaunchedEffect(Unit) {
                    navController.navigate("admin_dashboard_screen") {
                        popUpTo("explore") { inclusive = true }
                    }
                }
            } else {
                ExploreScreen(
                    publicDecks = publicStudySets,
                    onSearch = { viewModel.searchPublicDecks(it) },
                    onImportDeck = { code ->
                        viewModel.importDeckByCode(code,
                            onSuccess = { Toast.makeText(context, "Success", Toast.LENGTH_SHORT).show() },
                            onError = { Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show() }
                        )
                    },
                    onRateDeck = { id, rating -> viewModel.ratePublicStudySet(id, rating) },
                    onDeckClick = { set ->
                        viewModel.selectSet(set)
                        navController.navigate("deck_detail")
                    }
                )
            }
        }

        composable("profile") {
            ProfileScreen(
                userName = userName,
                userEmail = userEmail,
                onLogout = {
                    authViewModel.signOut()
                    navController.navigate("login_screen") { popUpTo(0) { inclusive = true } }
                },
                onNavigateToPersonalInfo = {},
                onNavigateToSecurity = {},
                onNavigateToNotifications = {},
                onNavigateToLearningPrefs = {},
                onNavigateToStats = {}
            )
        }

        composable("notifications") {
            val notifications by viewModel.notifications.collectAsState()
            NotificationInboxScreen(
                notifications = notifications,
                onBack = { navController.popBackStack() },
                onNotificationClick = { notif ->
                    viewModel.markNotificationAsRead(notif.id)
                    if (notif.deckId.isNotEmpty()) {
                        val set = studySets.find { it.id == notif.deckId }
                        if (set != null) {
                            viewModel.selectSet(set)
                            navController.navigate("deck_detail")
                        }
                    }
                }
            )
        }

        composable("admin_dashboard_screen") {
            if (userRole == "admin") {
                AdminDashboardScreen(navController = navController)
            } else {
                LaunchedEffect(Unit) { navController.navigate("home_screen") }
            }
        }

        composable("manage_users") {
            if (userRole != "admin" && userRole != null) {
                LaunchedEffect(Unit) { navController.navigate("home_screen") }
            } else {
                AdminManageUsersScreen(navController = navController)
            }
        }

        composable("manage_packages") {
            if (userRole != "admin" && userRole != null) {
                LaunchedEffect(Unit) { navController.navigate("home_screen") }
            } else {
                AdminManagePackagesScreen(navController = navController)
            }
        }

        composable("package_detail/{packageName}") { backStackEntry ->
            val packageName = backStackEntry.arguments?.getString("packageName") ?: ""
            if (userRole != "admin" && userRole != null) {
                LaunchedEffect(Unit) { navController.navigate("home_screen") }
            } else {
                AdminPackageDetailScreen(navController = navController, packageName = packageName)
            }
        }

        composable("manage_system_decks") {
            if (userRole != "admin" && userRole != null) {
                LaunchedEffect(Unit) { navController.navigate("home_screen") }
            } else {
                AdminManageSystemDecksScreen(navController = navController)
            }
        }

        composable("create_deck") {
            if (userRole == "admin") {
                LaunchedEffect(Unit) {
                    navController.navigate("admin_dashboard_screen") {
                        popUpTo("create_deck") { inclusive = true }
                    }
                }
            } else {
                CreateDeckScreen(
                    onSave = { title, desc, isPublic ->
                        viewModel.addStudySet(title, desc, isPublic)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable("edit_deck/{setId}") { backStackEntry ->
            val setId = backStackEntry.arguments?.getString("setId") ?: ""
            val studySet = studySets.find { it.id == setId }
            if (studySet != null) {
                DeckEditorScreen(
                    studySet = studySet,
                    onSave = { updatedSet -> viewModel.updateStudySet(updatedSet) },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable("deck_detail") {
            selectedSet?.let { set ->
                val comments by viewModel.currentComments.collectAsState()
                DeckDetailScreen(
                    studySet = set,
                    userName = userName,
                    currentUserId = currentUserId,
                    comments = comments,
                    onAddComment = { content -> viewModel.addComment(set.id, content) },
                    onBack = { navController.popBackStack() },
                    onStudyFlashcards = { navController.navigate("study_session") },
                    onQuiz = { navController.navigate("quiz_session") },
                    onMatch = {
                        viewModel.createBattle(set, 
                            onSuccess = { battleId -> navController.navigate("battle_session/$battleId") },
                            onError = { error -> Toast.makeText(context, error, Toast.LENGTH_SHORT).show() }
                        )
                    },
                    onEditDeck = { navController.navigate("edit_deck/${set.id}") },
                    onDeleteDeck = { viewModel.deleteStudySet(set.id); navController.popBackStack() }
                )
            }
        }

        composable("study_session") {
            selectedSet?.let { set ->
                StudySessionScreen(
                    studySet = set,
                    onBack = { navController.popBackStack() },
                    onSpeak = { text, lang ->
                        tts?.language = Locale.forLanguageTag(lang)
                        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
                    },
                    onUpdateCard = { card, q -> viewModel.updateCardQuality(card, q) }
                )
            }
        }

        composable("quiz_session") {
            selectedSet?.let { set ->
                QuizScreen(
                    studySet = set,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable("battle_session/{battleId}") { backStackEntry ->
            val battleId = backStackEntry.arguments?.getString("battleId") ?: ""
            val battleRoom by viewModel.currentBattle.collectAsState()

            BattleScreen(
                battleRoom = battleRoom,
                onStartBattle = { viewModel.startBattle(battleId) },
                onAnswerSelected = { isCorrect -> 
                    val currentPlayer = battleRoom?.players?.get(currentUserId)
                    val newScore = (currentPlayer?.score ?: 0) + (if (isCorrect) 1 else 0)
                    val newProgress = (currentPlayer?.progress ?: 0) + 1
                    viewModel.updateBattleProgress(battleId, newScore, newProgress)
                },
                onBack = { navController.popBackStack() },
                onLeaveRoom = {
                    viewModel.leaveAndDeleteRoom(battleId) {
                        navController.navigate("home_screen") { popUpTo("home_screen") { inclusive = true } }
                    }
                },
                onRematch = {
                    viewModel.rematch(
                        onSuccess = { newId -> navController.navigate("battle_session/$newId") },
                        onError = { Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show() }
                    )
                }
            )
        }
    }
}
