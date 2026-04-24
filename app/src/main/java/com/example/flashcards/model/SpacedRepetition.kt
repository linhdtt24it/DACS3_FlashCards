package com.example.flashcards.model

import kotlin.math.max

/**
 * SuperMemo-2 (SM-2) algorithm implementation.
 */
fun Flashcard.update(quality: Int): Flashcard {
    val newRepetitions: Int
    val newInterval: Int
    val newEaseFactor: Double

    if (quality >= 3) {
        if (repetitions == 0) {
            newInterval = 1
        } else if (repetitions == 1) {
            newInterval = 6
        } else {
            newInterval = (interval * easeFactor).toInt()
        }
        newRepetitions = repetitions + 1
    } else {
        newRepetitions = 0
        newInterval = 1
    }

    newEaseFactor = max(1.3, easeFactor + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02)))

    val calendar = java.util.Calendar.getInstance()
    calendar.add(java.util.Calendar.DAY_OF_YEAR, newInterval)

    return this.copy(
        repetitions = newRepetitions,
        interval = newInterval,
        easeFactor = newEaseFactor,
        nextReviewDate = calendar.timeInMillis
    )
}
