package com.example.flashcards.viewmodel

import com.google.firebase.Timestamp

data class SystemVocabulary(
    val id: String = "",
    val category: String = "",
    val front: String = "",
    val back: String = "",
    val createdAt: Timestamp? = null
)
