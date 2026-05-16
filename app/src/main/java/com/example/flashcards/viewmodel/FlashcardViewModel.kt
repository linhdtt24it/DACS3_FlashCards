package com.example.flashcards.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flashcards.model.Flashcard
import com.example.flashcards.model.StudySet
import com.example.flashcards.model.Comment
import com.example.flashcards.model.update
import com.example.flashcards.model.SocialNotification
import com.example.flashcards.model.UserStats
import com.example.flashcards.repository.StudySetRepository
import com.example.flashcards.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth

class FlashcardViewModel(
    private val repository: StudySetRepository = StudySetRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _studySets = MutableStateFlow<List<StudySet>>(emptyList())
    val studySets: StateFlow<List<StudySet>> = _studySets.asStateFlow()

    private val _selectedSet = MutableStateFlow<StudySet?>(null)
    val selectedSet: StateFlow<StudySet?> = _selectedSet.asStateFlow()

    private val _publicStudySets = MutableStateFlow<List<StudySet>>(emptyList())
    val publicStudySets: StateFlow<List<StudySet>> = _publicStudySets.asStateFlow()

    private val _currentComments = MutableStateFlow<List<Comment>>(emptyList())
    val currentComments: StateFlow<List<Comment>> = _currentComments.asStateFlow()

    private val _notifications = MutableStateFlow<List<SocialNotification>>(emptyList())
    val notifications: StateFlow<List<SocialNotification>> = _notifications.asStateFlow()

    private val _userStats = MutableStateFlow(UserStats())
    val userStats: StateFlow<UserStats> = _userStats.asStateFlow()

    private var commentsJob: Job? = null
    private var statsJob: Job? = null
    private var notifJob: Job? = null

    init {
        loadData()
        loadUserStats()
        loadNotifications()
    }

    fun loadData() {
        viewModelScope.launch {
            repository.getStudySets().collectLatest { sets ->
                if (sets.isEmpty()) {
                    repository.seedInitialData()
                } else {
                    _studySets.value = sets
                    // Sync selectedSet with updated data from Firestore
                    _selectedSet.value?.let { current ->
                        _selectedSet.value = sets.find { it.id == current.id }
                    }
                }
            }
        }
    }

    fun selectSet(studySet: StudySet) {
        _selectedSet.value = studySet
    }

    fun updateCardQuality(card: Flashcard, quality: Int) {
        val updatedCard = card.update(quality)
        val currentSet = _selectedSet.value ?: return

        val newCards = currentSet.cards.map {
            if (it.id == card.id) updatedCard else it
        }
        val updatedSet = currentSet.copy(cards = newCards)

        viewModelScope.launch {
            repository.saveStudySet(updatedSet)
        }
    }

    fun addStudySet(title: String, description: String, isPublic: Boolean = false) {
        viewModelScope.launch {
            val user = FirebaseAuth.getInstance().currentUser
            val creatorId = user?.uid ?: ""
            val creatorName = user?.displayName ?: user?.email?.substringBefore("@") ?: "Unknown User"

            val shareCode = if (isPublic) {
                val charPool : List<Char> = ('A'..'Z') + ('0'..'9')
                (1..6).map { kotlin.random.Random.nextInt(0, charPool.size).let { charPool[it] } }.joinToString("")
            } else null

            val newSet = StudySet(
                id = java.util.UUID.randomUUID().toString(),
                title = title,
                description = description,
                cards = emptyList(),
                isPublic = isPublic,
                shareCode = shareCode,
                creatorId = creatorId,
                creatorName = creatorName
            )
            repository.saveStudySet(newSet)
        }
    }

    fun updateStudySet(studySet: StudySet) {
        viewModelScope.launch {
            val user = FirebaseAuth.getInstance().currentUser
            val updatedCreatorId = if (studySet.creatorId.isEmpty()) user?.uid ?: "" else studySet.creatorId
            val updatedCreatorName = if (studySet.creatorName.isEmpty()) user?.displayName ?: user?.email?.substringBefore("@") ?: "Unknown User" else studySet.creatorName

            val updatedShareCode = if (studySet.isPublic && studySet.shareCode == null) {
                val charPool : List<Char> = ('A'..'Z') + ('0'..'9')
                (1..6).map { kotlin.random.Random.nextInt(0, charPool.size).let { charPool[it] } }.joinToString("")
            } else if (!studySet.isPublic) null else studySet.shareCode

            val finalSet = studySet.copy(
                creatorId = updatedCreatorId,
                creatorName = updatedCreatorName,
                shareCode = updatedShareCode
            )
            repository.saveStudySet(finalSet)
            if (_selectedSet.value?.id == finalSet.id) {
                _selectedSet.value = finalSet
            }
        }
    }

    fun deleteStudySet(id: String) {
        viewModelScope.launch {
            repository.deleteStudySet(id)
            if (_selectedSet.value?.id == id) {
                _selectedSet.value = null
            }
        }
    }

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
                val newSet = publicSet.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    isPublic = false, // Keep imported sets private by default
                    shareCode = null, // Remove share code
                    rating = 0f,
                    ratingCount = 0
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
            val currentSet = selectedSet.value ?: publicStudySets.value.find { it.id == setId }
            if (currentSet != null) {
                val user = FirebaseAuth.getInstance().currentUser
                val senderName = user?.displayName ?: user?.email?.substringBefore("@") ?: "Anonymous"
                userRepository.sendNotification(
                    receiverId = currentSet.creatorId,
                    notification = SocialNotification(
                        receiverId = currentSet.creatorId,
                        senderId = user?.uid ?: "",
                        senderName = senderName,
                        deckId = currentSet.id,
                        deckTitle = currentSet.title,
                        type = "RATING",
                        content = rating.toString()
                    )
                )
            }
            loadPublicDecks() // Refresh the list
        }
    }

    fun loadComments(setId: String) {
        commentsJob?.cancel()
        commentsJob = viewModelScope.launch {
            repository.getComments(setId).collectLatest { comments ->
                _currentComments.value = comments
            }
        }
    }

    fun addComment(setId: String, content: String) {
        if (content.isBlank()) return

        viewModelScope.launch {
            val user = FirebaseAuth.getInstance().currentUser
            val senderName = user?.displayName ?: user?.email?.substringBefore("@") ?: "Anonymous"
            val comment = Comment(
                setId = setId,
                userId = user?.uid ?: "",
                userName = senderName,
                content = content
            )
            repository.addComment(setId, comment)

            val currentSet = selectedSet.value ?: publicStudySets.value.find { it.id == setId }
            if (currentSet != null) {
                userRepository.sendNotification(
                    receiverId = currentSet.creatorId,
                    notification = SocialNotification(
                        receiverId = currentSet.creatorId,
                        senderId = user?.uid ?: "",
                        senderName = senderName,
                        deckId = currentSet.id,
                        deckTitle = currentSet.title,
                        type = "COMMENT",
                        content = content
                    )
                )
            }
        }
    }

    private fun loadUserStats() {
        statsJob?.cancel()
        statsJob = viewModelScope.launch {
            userRepository.getUserStats().collectLatest { stats ->
                _userStats.value = stats
            }
        }
    }

    private fun loadNotifications() {
        notifJob?.cancel()
        notifJob = viewModelScope.launch {
            userRepository.getNotifications().collectLatest { notifs ->
                _notifications.value = notifs
            }
        }
    }

    fun markNotificationAsRead(notificationId: String) {
        viewModelScope.launch {
            userRepository.markNotificationAsRead(notificationId)
        }
    }

    fun recordStudySession(cardsStudied: Int, correct: Int, wrong: Int) {
        viewModelScope.launch {
            userRepository.recordStudySession(cardsStudied, correct, wrong)
        }
    }
}