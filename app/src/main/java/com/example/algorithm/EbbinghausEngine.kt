package com.example.algorithm

import com.example.data.model.CardStatus
import com.example.data.model.FlashcardEntity
import com.example.data.model.ReviewGrade
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object EbbinghausEngine {

    const val MAX_NEW_CARDS_PER_DAY = 20
    const val DAILY_REVIEW_PORTION = 35
    const val BACKLOG_TRIGGER_THRESHOLD = 30
    const val LONG_TERM_ARCHIVE_DAYS = 90.0

    // Approximate day fractions in milliseconds
    private const val MS_PER_MINUTE = 60 * 1000L
    private const val MS_PER_HOUR = 60 * MS_PER_MINUTE
    private const val MS_PER_DAY = 24 * MS_PER_HOUR

    data class CalculationResult(
        val updatedCard: FlashcardEntity,
        val nextIntervalDays: Double,
        val nextDueDate: Long
    )

    /**
     * Calculates the next spaced interval according to the Ebbinghaus algorithm.
     */
    fun processReview(
        card: FlashcardEntity,
        grade: ReviewGrade,
        currentTime: Long = System.currentTimeMillis()
    ): CalculationResult {
        var newRepetitionCount = card.repetitionCount
        var newEaseFactor = card.easeFactor
        var newIntervalDays: Double
        var newStatus: CardStatus
        var isHeavy = card.isHeavy

        when (grade) {
            ReviewGrade.BAD -> {
                // «Плохо»: сброс счетчика успехов, снижение фактора легкости, перевод в тяжелые
                newRepetitionCount = 0
                newEaseFactor = max(1.3, newEaseFactor - 0.2)
                isHeavy = true
                // Повторение через 15 минут (0.0104 дня)
                newIntervalDays = 15.0 / (24 * 60)
                newStatus = CardStatus.LEARNING
            }

            ReviewGrade.NORMAL -> {
                // «Нормально»: планомерное математическое увеличение
                newRepetitionCount += 1
                isHeavy = false
                newIntervalDays = when (newRepetitionCount) {
                    1 -> 7.0 / 24.0 // 7 часов (3-е базовое повторение)
                    2 -> 1.0        // 1 день
                    3 -> 3.0        // 3 дня
                    else -> {
                        val base = if (card.intervalDays > 0.5) card.intervalDays else 3.0
                        roundToDecimals(base * newEaseFactor, 1)
                    }
                }
                newStatus = if (newIntervalDays >= LONG_TERM_ARCHIVE_DAYS) {
                    CardStatus.MASTERED
                } else {
                    CardStatus.REVIEW
                }
            }

            ReviewGrade.EXCELLENT -> {
                // «Отлично»: резкий скачок вперед (множитель 2.5-3x или фактор 1.3x)
                newRepetitionCount += 1
                newEaseFactor = min(3.0, newEaseFactor + 0.15)
                isHeavy = false
                newIntervalDays = when (newRepetitionCount) {
                    1 -> 1.0        // Сразу 1 день (вместо 7 часов)
                    2 -> 3.5        // 3.5 дня (вместо 1 дня)
                    3 -> 8.0        // 8 дней
                    else -> {
                        val base = if (card.intervalDays > 0.5) card.intervalDays else 3.5
                        roundToDecimals(base * newEaseFactor * 1.3, 1)
                    }
                }
                newStatus = if (newIntervalDays >= LONG_TERM_ARCHIVE_DAYS) {
                    CardStatus.MASTERED
                } else {
                    CardStatus.REVIEW
                }
            }
        }

        val nextDueDate = currentTime + (newIntervalDays * MS_PER_DAY).toLong()

        val updated = card.copy(
            intervalDays = newIntervalDays,
            repetitionCount = newRepetitionCount,
            easeFactor = roundToDecimals(newEaseFactor, 2),
            dueDate = nextDueDate,
            lastReviewedAt = currentTime,
            status = newStatus,
            isHeavy = isHeavy
        )

        return CalculationResult(
            updatedCard = updated,
            nextIntervalDays = newIntervalDays,
            nextDueDate = nextDueDate
        )
    }

    /**
     * Preview text for review buttons (e.g. «15 мин», «7 ч», «3 дня»)
     */
    fun getIntervalPreview(card: FlashcardEntity, grade: ReviewGrade): String {
        return when (grade) {
            ReviewGrade.BAD -> "15 мин"
            ReviewGrade.NORMAL -> {
                when (card.repetitionCount + 1) {
                    1 -> "7 ч"
                    2 -> "1 дн"
                    3 -> "3 дн"
                    else -> {
                        val days = roundToDecimals((if (card.intervalDays > 0.5) card.intervalDays else 3.0) * card.easeFactor, 0).roundToInt()
                        "$days дн"
                    }
                }
            }
            ReviewGrade.EXCELLENT -> {
                when (card.repetitionCount + 1) {
                    1 -> "1 дн"
                    2 -> "3.5 дн"
                    3 -> "8 дн"
                    else -> {
                        val days = roundToDecimals((if (card.intervalDays > 0.5) card.intervalDays else 3.5) * card.easeFactor * 1.3, 0).roundToInt()
                        "$days дн"
                    }
                }
            }
        }
    }

    private fun roundToDecimals(value: Double, decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return kotlin.math.round(value * multiplier) / multiplier
    }
}
