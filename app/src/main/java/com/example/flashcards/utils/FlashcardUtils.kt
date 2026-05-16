package com.example.flashcards.utils

import com.example.flashcards.model.Flashcard
import java.util.UUID

object FlashcardUtils {
    /**
     * Tách văn bản thô thành danh sách Flashcard.
     * Mỗi dòng là một thẻ, phân cách bởi [separator].
     */
    fun parseBulkText(inputText: String, separator: String = "-"): List<Flashcard> {
        return inputText.lines()
            .filter { it.isNotBlank() && it.contains(separator) }
            .mapNotNull { line ->
                val parts = line.split(separator, limit = 2)
                val front = parts[0].trim()
                val back = parts[1].trim()

                if (front.isNotEmpty() && back.isNotEmpty()) {
                    Flashcard(
                        id = UUID.randomUUID().toString(),
                        question = front,
                        answer = back
                    )
                } else null
            }
    }
}
