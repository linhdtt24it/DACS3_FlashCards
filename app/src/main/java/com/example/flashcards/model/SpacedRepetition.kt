package com.example.flashcards.model

import kotlin.math.max
import kotlin.math.roundToInt

enum class CardState {
    NEW, LEARNING, REVIEW, RELEARNING
}

enum class ReviewRating(val label: String) {
    AGAIN("Quên"),
    HARD("Khó"),
    GOOD("Tốt"),
    EASY("Dễ")
}

data class ReviewResult(
    val intervalDays: Int,
    val easeFactor: Double,
    val state: CardState
)

object SpacedRepetition {
    const val MAX_INTERVAL_DAYS = 180
    const val INITIAL_EASE_FACTOR = 2.5

    /**
     * Tính toán lịch trình review tiếp theo theo kiểu Anki (dựa trên SM-2).
     */
    fun calculateNextReview(
        currentState: CardState,
        currentInterval: Int,
        currentEaseFactor: Double,
        rating: ReviewRating
    ): ReviewResult {
        var newInterval: Int
        var newEaseFactor = currentEaseFactor
        var newState = currentState

        when (currentState) {
            CardState.NEW, CardState.LEARNING -> {
                // Thẻ mới hoặc đang học
                when (rating) {
                    ReviewRating.AGAIN -> {
                        newInterval = 0 // Học lại ngay trong ngày
                        newState = CardState.LEARNING
                    }
                    ReviewRating.HARD -> {
                        newInterval = 0 
                        newState = CardState.LEARNING
                    }
                    ReviewRating.GOOD -> {
                        newInterval = 1 // Ngày mai
                        newState = CardState.REVIEW
                    }
                    ReviewRating.EASY -> {
                        newInterval = 4 // 4 ngày sau
                        newState = CardState.REVIEW
                    }
                }
            }
            CardState.REVIEW -> {
                // Thẻ đã biết, đang trong chu kỳ ôn
                when (rating) {
                    ReviewRating.AGAIN -> {
                        newInterval = 1 // Bắt đầu lại
                        newEaseFactor = max(1.3, currentEaseFactor - 0.2)
                        newState = CardState.RELEARNING
                    }
                    ReviewRating.HARD -> {
                        newInterval = max(1, (currentInterval * 1.2).roundToInt())
                        newEaseFactor = max(1.3, currentEaseFactor - 0.15)
                    }
                    ReviewRating.GOOD -> {
                        newInterval = max(1, (currentInterval * currentEaseFactor).roundToInt())
                        // Ease factor không đổi
                    }
                    ReviewRating.EASY -> {
                        newInterval = max(1, (currentInterval * currentEaseFactor * 1.3).roundToInt())
                        newEaseFactor += 0.15
                    }
                }
            }
            CardState.RELEARNING -> {
                // Thẻ quên đang học lại
                when (rating) {
                    ReviewRating.AGAIN -> {
                        newInterval = 1
                        newState = CardState.RELEARNING
                    }
                    ReviewRating.HARD -> {
                        newInterval = 1
                        newState = CardState.RELEARNING
                    }
                    ReviewRating.GOOD -> {
                        newInterval = 2
                        newState = CardState.REVIEW
                    }
                    ReviewRating.EASY -> {
                        newInterval = 4
                        newState = CardState.REVIEW
                    }
                }
            }
        }

        // Đảm bảo interval không vượt quá giới hạn tối đa để thẻ không biến mất
        if (newInterval > MAX_INTERVAL_DAYS) {
            newInterval = MAX_INTERVAL_DAYS
        }

        return ReviewResult(
            intervalDays = newInterval,
            easeFactor = newEaseFactor,
            state = newState
        )
    }

    /**
     * Legacy SM-2 support for existing StudySession functionality.
     * Can be refactored out later.
     */
    fun updateLegacy(card: Flashcard, quality: Int): Flashcard {
        val rating = when {
            quality < 3 -> ReviewRating.AGAIN
            quality == 3 -> ReviewRating.HARD
            quality == 4 -> ReviewRating.GOOD
            else -> ReviewRating.EASY
        }
        
        val currentState = if (card.interval == 0) CardState.NEW else CardState.REVIEW
        
        val result = calculateNextReview(
            currentState = currentState,
            currentInterval = card.interval,
            currentEaseFactor = card.easeFactor,
            rating = rating
        )
        
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, result.intervalDays)
        
        return card.copy(
            interval = result.intervalDays,
            easeFactor = result.easeFactor,
            repetitions = if (rating == ReviewRating.AGAIN) 0 else card.repetitions + 1,
            nextReviewDate = calendar.timeInMillis
        )
    }
}

// Cập nhật extension function để giữ khả năng tương thích ngược
fun Flashcard.update(quality: Int): Flashcard {
    return SpacedRepetition.updateLegacy(this, quality)
}
