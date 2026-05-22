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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
            if (currentRoute !in listOf("auth", "study_session", "quiz_session", "deck_detail", "create_deck", "battle_session")) {
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
    val studySets by viewModel.studySets.collectAsState()
    val selectedSet by viewModel.selectedSet.collectAsState()
    val publicStudySets by viewModel.publicStudySets.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val userStats by viewModel.userStats.collectAsState()
    val dueCount by viewModel.dueCount.collectAsState()
    val dueCards by viewModel.dueCards.collectAsState()
    val totalDecks by viewModel.totalDecks.collectAsState()
    val totalCards by viewModel.totalCards.collectAsState()
    var selectedFolder by remember { mutableStateOf<com.example.flashcards.model.Folder?>(null) }
    val context = LocalContext.current

    val startDestination = if (FirebaseAuth.getInstance().currentUser != null) "home" else "auth"

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
                userStats = userStats,
                dueCount = dueCount,
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
                onNotificationsClick = { navController.navigate("notifications") },
                onReviewDue = { navController.navigate("spaced_review") }
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
                    languageCode = "en"
                )
            }

            DeckEditorScreen(
                studySet = emptySet,
                isCreateMode = true,
                onSave = { updatedSet ->
                    viewModel.updateStudySet(updatedSet)
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
                    if (code.length == 6) {
                        viewModel.joinBattleByCode(code,
                            onSuccess = { battleId ->
                                navController.navigate("battle_session/$battleId")
                            },
                            onError = {
                                viewModel.importDeckByCode(code, {
                                    Toast.makeText(context, "Đã nhập bộ thẻ thành công!", Toast.LENGTH_SHORT).show()
                                }, {
                                    Toast.makeText(context, "Mã không hợp lệ", Toast.LENGTH_SHORT).show()
                                })
                            }
                        )
                    }
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
                    viewModel.importDeckByCode(code, {
                        navController.navigate("library")
                    }, {
                        Toast.makeText(context, "Không tìm thấy bộ thẻ", Toast.LENGTH_SHORT).show()
                    })
                },
                onRateDeck = { setId, rating -> viewModel.ratePublicStudySet(setId, rating) },
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
                    onMatch = {
                        viewModel.createBattle(studySet,
                            onSuccess = { battleId ->
                                navController.navigate("battle_session/$battleId")
                            },
                            onError = { error ->
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    onEditDeck = { navController.navigate("edit_deck/${studySet.id}") },
                    onDeleteDeck = {
                        viewModel.deleteStudySet(studySet.id)
                        navController.popBackStack()
                    }
                )
            }
        }

        composable("study_session") {
            selectedSet?.let { studySet ->
                StudySessionScreen(
                    studySet = studySet,
                    onBack = { navController.popBackStack() },
                    onSpeak = { text, lang ->
                        tts?.language = Locale.forLanguageTag(lang)
                        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
                    },
                    onUpdateCard = { card, quality ->
                        viewModel.updateCardQuality(card, quality)
                        viewModel.recordStudySession(1, if (quality > 2) 1 else 0, if (quality <= 2) 1 else 0)
                    }
                )
            }
        }

        composable("quiz_session") {
            selectedSet?.let { studySet ->
                QuizScreen(studySet = studySet, onBack = { navController.popBackStack() })
            }
        }

        composable("battle_session/{battleId}") { backStackEntry ->
            val battleId = backStackEntry.arguments?.getString("battleId") ?: ""
            val battleRoom by viewModel.currentBattle.collectAsState()
            val context = LocalContext.current

            LaunchedEffect(battleId) {
                try {
                    viewModel.joinBattle(battleId)
                } catch (e: Exception) {
                    Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                }
            }

            BattleScreen(
                battleRoom = battleRoom,
                onStartBattle = {
                    try {
                        viewModel.startBattle(battleId)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Không thể bắt đầu trận", Toast.LENGTH_SHORT).show()
                    }
                },
                onAnswerSelected = { isCorrect ->
                    try {
                        val room = battleRoom ?: return@BattleScreen
                        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@BattleScreen
                        val currentPlayer = room.players[currentUserId] ?: return@BattleScreen
                        val newScore = if (isCorrect) currentPlayer.score + 1 else currentPlayer.score
                        viewModel.updateBattleProgress(battleId, newScore, currentPlayer.progress + 1)
                    } catch (e: Exception) {
                        // Bỏ qua lỗi
                    }
                },
                onBack = { navController.popBackStack() },
                onLeaveRoom = {
                    viewModel.leaveAndDeleteRoom(battleId) {
                        navController.popBackStack()
                    }
                },
                onRematch = {
                    viewModel.rematch(
                        onSuccess = { newBattleId ->
                            navController.popBackStack()
                            navController.navigate("battle_session/$newBattleId")
                        },
                        onError = {
                            Toast.makeText(context, "Không thể thi lại", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                onBattleFinished = {
                    navController.popBackStack()
                }
            )
        }

        composable("spaced_review") {
            SpacedRepetitionScreen(
                dueCards = dueCards,
                onUpdateCard = { card, quality ->
                    viewModel.updateCardQualityGlobal(card, quality)
                },
                onRecordSession = { cards, correct, wrong ->
                    viewModel.recordStudySession(cards, correct, wrong)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable("stats") {
            StatisticsScreen(
                userStats = userStats,
                totalDecks = totalDecks,
                totalCards = totalCards,
                dueCount = dueCount,
                onBack = { navController.popBackStack() }
            )
        }

        composable("notifications") {
            val notifications by viewModel.notifications.collectAsState()
            NotificationInboxScreen(
                notifications = notifications,
                onBack = { navController.popBackStack() },
                onNotificationClick = { notif ->
                    viewModel.markNotificationAsRead(notif.id)
                    if (notif.type == "BATTLE_INVITE") {
                        navController.navigate("battle_session/${notif.content}")
                    }
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
            PersonalInfoScreen(onBack = { navController.popBackStack() })
        }

        composable("security") {
            SecurityScreen(onBack = { navController.popBackStack() })
        }

        composable("notification_settings") {
            NotificationSettingsScreen(onBack = { navController.popBackStack() })
        }

        composable("learning_prefs") {
            LearningPreferencesScreen(onBack = { navController.popBackStack() })
        }

        composable("edit_deck/{deckId}") { backStackEntry ->
            val deckId = backStackEntry.arguments?.getString("deckId")
            val studySetToEdit = studySets.find { it.id == deckId }
            if (studySetToEdit != null) {
                DeckEditorScreen(
                    studySet = studySetToEdit,
                    isCreateMode = false,
                    onSave = { updatedSet ->
                        viewModel.updateStudySet(updatedSet)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}