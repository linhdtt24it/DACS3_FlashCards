package com.example.flashcards.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flashcards.model.*
import com.example.flashcards.repository.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class FlashcardViewModel(
    private val repository: StudySetRepository = StudySetRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val folderRepository: FolderRepository = FolderRepository(),
    private val battleRepository: BattleRepository = BattleRepository()
) : ViewModel() {

    private val _studySets = MutableStateFlow<List<StudySet>>(emptyList())
    val studySets: StateFlow<List<StudySet>> = _studySets.asStateFlow()

    private val _selectedSet = MutableStateFlow<StudySet?>(null)
    val selectedSet: StateFlow<StudySet?> = _selectedSet.asStateFlow()

    private val _userStats = MutableStateFlow(UserStats())
    val userStats: StateFlow<UserStats> = _userStats.asStateFlow()

    private val _notifications = MutableStateFlow<List<SocialNotification>>(emptyList())
    val notifications: StateFlow<List<SocialNotification>> = _notifications.asStateFlow()

    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    val folders: StateFlow<List<Folder>> = _folders.asStateFlow()

    private val _publicStudySets = MutableStateFlow<List<StudySet>>(emptyList())
    val publicStudySets: StateFlow<List<StudySet>> = _publicStudySets.asStateFlow()

    private val _currentComments = MutableStateFlow<List<Comment>>(emptyList())
    val currentComments: StateFlow<List<Comment>> = _currentComments.asStateFlow()

    // Battle State
    private val _currentBattle = MutableStateFlow<BattleRoom?>(null)
    val currentBattle: StateFlow<BattleRoom?> = _currentBattle.asStateFlow()

    private val _availableBattles = MutableStateFlow<List<BattleRoom>>(emptyList())
    val availableBattles: StateFlow<List<BattleRoom>> = _availableBattles.asStateFlow()

    private var battleJob: Job? = null
    private var commentsJob: Job? = null

    init {
        loadData()
        observeAvailableBattles()
        viewModelScope.launch {
            battleRepository.cleanupOldBattles()
        }
    }

    fun onUserSignedIn() {
        loadUserStats()
        loadNotifications()
        loadFolders()
    }

    fun loadData() {
        viewModelScope.launch {
            repository.getStudySets().collectLatest { sets ->
                _studySets.value = sets
                _selectedSet.value?.let { current ->
                    _selectedSet.value = sets.find { it.id == current.id }
                }
            }
        }
    }

    // --- Explore & Public Decks ---
    fun loadPublicDecks() {
        viewModelScope.launch {
            val sets = repository.getPublicStudySets()
            _publicStudySets.value = sets.sortedByDescending { it.rating }
        }
    }

    fun searchPublicDecks(query: String) {
        viewModelScope.launch {
            val sets = repository.getPublicStudySets()
            if (query.isBlank()) {
                _publicStudySets.value = sets.sortedByDescending { it.rating }
            } else {
                _publicStudySets.value = sets.filter {
                    it.title.contains(query, ignoreCase = true) ||
                            it.description.contains(query, ignoreCase = true) ||
                            it.creatorName.contains(query, ignoreCase = true)
                }.sortedByDescending { it.rating }
            }
        }
    }

    fun importDeckByCode(code: String, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            val publicSet = repository.getStudySetByShareCode(code)
            if (publicSet != null) {
                val user = FirebaseAuth.getInstance().currentUser
                val newSet = publicSet.copy(
                    id = UUID.randomUUID().toString(),
                    isPublic = false,
                    shareCode = null,
                    rating = 0f,
                    ratingCount = 0,
                    creatorId = user?.uid ?: "",
                    creatorName = user?.displayName ?: user?.email?.substringBefore("@") ?: "User"
                )
                repository.saveStudySet(newSet)
                onSuccess()
            } else {
                onError()
            }
        }
    }

    fun ratePublicStudySet(setId: String, rating: Float) {
        viewModelScope.launch {
            repository.ratePublicStudySet(setId, rating)
            loadPublicDecks()
        }
    }

    // --- Battle Logic ---
    private fun observeAvailableBattles() {
        viewModelScope.launch {
            battleRepository.getAvailableBattles().collectLatest { rooms ->
                _availableBattles.value = rooms
            }
        }
    }

    fun createBattle(studySet: StudySet, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        if (studySet.cards.size < 2) {
            onError("Bộ thẻ cần ít nhất 2 thẻ để đấu!")
            return
        }
        viewModelScope.launch {
            try {
                val battleId = battleRepository.createBattle(studySet)
                observeBattle(battleId)
                onSuccess(battleId)
            } catch (e: Exception) {
                onError(e.message ?: "Lỗi tạo phòng")
            }
        }
    }

    fun joinBattle(battleId: String) {
        viewModelScope.launch {
            battleRepository.joinBattle(battleId)
            observeBattle(battleId)
        }
    }

    fun joinBattleByCode(code: String, onSuccess: (String) -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            val battleId = battleRepository.joinBattleByCode(code)
            if (battleId != null) {
                joinBattle(battleId)
                onSuccess(battleId)
            } else {
                onError()
            }
        }
    }

    private fun observeBattle(battleId: String) {
        battleJob?.cancel()
        battleJob = viewModelScope.launch {
            battleRepository.observeBattle(battleId).collectLatest { room ->
                _currentBattle.value = room
                if (room?.status == "FINISHED") {
                    battleRepository.saveBattleHistory(room)
                }
            }
        }
    }

    fun startBattle(battleId: String) {
        viewModelScope.launch {
            battleRepository.startBattle(battleId)
        }
    }

    fun updateBattleProgress(battleId: String, score: Int, progress: Int) {
        viewModelScope.launch {
            battleRepository.updateProgress(battleId, score, progress)
            if (progress >= (_currentBattle.value?.questions?.size ?: 5) && score >= 4) {
                userRepository.addAchievement("SPEED_MASTER")
            }
        }
    }

    fun leaveAndDeleteRoom(battleId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                battleRepository.leaveAndDeleteRoom(battleId, userId)
                battleJob?.cancel()
                _currentBattle.value = null
                onComplete()
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete()
            }
        }
    }

    fun rematch(onSuccess: (String) -> Unit, onError: () -> Unit) {
        val currentBattle = _currentBattle.value
        val studySet = _studySets.value.find { it.id == currentBattle?.setId }

        if (currentBattle == null || studySet == null) {
            onError()
            return
        }

        viewModelScope.launch {
            try {
                val newBattleId = battleRepository.rematch(currentBattle.id, studySet)
                if (newBattleId != null) {
                    battleJob?.cancel()
                    observeBattle(newBattleId)
                    onSuccess(newBattleId)
                } else {
                    onError()
                }
            } catch (e: Exception) {
                onError()
            }
        }
    }

    fun cleanupOldBattles() {
        viewModelScope.launch {
            battleRepository.cleanupOldBattles()
        }
    }

    // --- Comments ---
    fun loadComments(setId: String) {
        commentsJob?.cancel()
        commentsJob = viewModelScope.launch {
            repository.getComments(setId).collectLatest {
                _currentComments.value = it
            }
        }
    }

    fun addComment(setId: String, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val user = FirebaseAuth.getInstance().currentUser
            val comment = Comment(
                setId = setId,
                userId = user?.uid ?: "",
                userName = user?.displayName ?: user?.email?.substringBefore("@") ?: "User",
                content = content
            )
            repository.addComment(setId, comment)
        }
    }

    // --- Study Set Management ---
    fun selectSet(set: StudySet) { _selectedSet.value = set }

    fun addStudySet(title: String, description: String) {
        viewModelScope.launch {
            val user = FirebaseAuth.getInstance().currentUser
            val newSet = StudySet(
                id = UUID.randomUUID().toString(),
                title = title,
                description = description,
                creatorId = user?.uid ?: "",
                creatorName = user?.displayName ?: user?.email?.substringBefore("@") ?: "User"
            )
            repository.saveStudySet(newSet)
        }
    }

    fun updateStudySet(set: StudySet) {
        viewModelScope.launch { repository.saveStudySet(set) }
    }

    fun deleteStudySet(id: String) {
        viewModelScope.launch { repository.deleteStudySet(id) }
    }

    // --- User Stats & Folders ---
    fun markNotificationAsRead(id: String) {
        viewModelScope.launch { userRepository.markNotificationAsRead(id) }
    }

    fun recordStudySession(c: Int, cor: Int, w: Int) {
        viewModelScope.launch { userRepository.recordStudySession(c, cor, w) }
    }

    private fun loadUserStats() {
        viewModelScope.launch {
            userRepository.getUserStats().collect { _userStats.value = it }
        }
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            userRepository.getNotifications().collect { _notifications.value = it }
        }
    }

    private fun loadFolders() {
        viewModelScope.launch {
            folderRepository.getFolders().collect { _folders.value = it }
        }
    }

    fun createFolder(n: String, e: String) {
        viewModelScope.launch {
            folderRepository.saveFolder(Folder(name = n, emoji = e, userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""))
        }
    }

    fun addSetToFolder(f: String, s: String) {
        viewModelScope.launch { folderRepository.addSetToFolder(f, s) }
    }

    fun removeSetFromFolder(f: String, s: String) {
        viewModelScope.launch { folderRepository.removeSetFromFolder(f, s) }
    }

    fun renameFolder(id: String, n: String, e: String) {
        viewModelScope.launch {
            val old = _folders.value.find { it.id == id } ?: return@launch
            folderRepository.saveFolder(old.copy(name = n, emoji = e))
        }
    }

    fun deleteFolder(id: String) {
        viewModelScope.launch { folderRepository.deleteFolder(id) }
    }

    fun updateCardQuality(card: Flashcard, q: Int) {
        val set = _selectedSet.value ?: return
        val updated = card.update(q)
        val newCards = set.cards.map { if (it.id == card.id) updated else it }
        updateStudySet(set.copy(cards = newCards))
    }
}