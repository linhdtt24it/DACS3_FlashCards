package com.example.flashcards.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flashcards.model.Flashcard
import com.example.flashcards.model.StudySet
import com.example.flashcards.model.update
import com.example.flashcards.repository.StudySetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FlashcardViewModel(
    private val repository: StudySetRepository = StudySetRepository()
) : ViewModel() {

    private val _studySets = MutableStateFlow<List<StudySet>>(emptyList())
    val studySets: StateFlow<List<StudySet>> = _studySets.asStateFlow()

    private val _selectedSet = MutableStateFlow<StudySet?>(null)
    val selectedSet: StateFlow<StudySet?> = _selectedSet.asStateFlow()

    init {
        loadData()
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

    fun addStudySet(title: String, description: String) {
        viewModelScope.launch {
            val newSet = StudySet(
                id = java.util.UUID.randomUUID().toString(),
                title = title,
                description = description,
                cards = emptyList()
            )
            repository.saveStudySet(newSet)
        }
    }

    fun updateStudySet(studySet: StudySet) {
        viewModelScope.launch {
            repository.saveStudySet(studySet)
            if (_selectedSet.value?.id == studySet.id) {
                _selectedSet.value = studySet
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
}