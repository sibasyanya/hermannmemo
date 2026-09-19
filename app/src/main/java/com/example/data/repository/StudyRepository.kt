package com.example.data.repository

import com.example.algorithm.EbbinghausEngine
import com.example.data.dao.DailyQuotaDao
import com.example.data.dao.DeckDao
import com.example.data.dao.FlashcardDao
import com.example.data.model.CardStatus
import com.example.data.model.DailyQuotaEntity
import com.example.data.model.DeckEntity
import com.example.data.model.FlashcardEntity
import com.example.data.model.ReviewGrade
import com.example.data.preload.DemoDecksProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StudyRepository(
    private val deckDao: DeckDao,
    private val flashcardDao: FlashcardDao,
    private val dailyQuotaDao: DailyQuotaDao
) {

    val allDecks: Flow<List<DeckEntity>> = deckDao.getAllDecks()
    val totalCardsCount: Flow<Int> = flashcardDao.countTotalCards()
    val newCardsCount: Flow<Int> = flashcardDao.countCardsByStatus(CardStatus.NEW)
    val learningCardsCount: Flow<Int> = flashcardDao.countCardsByStatus(CardStatus.LEARNING)
    val reviewCardsCount: Flow<Int> = flashcardDao.countCardsByStatus(CardStatus.REVIEW)
    val masteredCardsCount: Flow<Int> = flashcardDao.countCardsByStatus(CardStatus.MASTERED)

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun getTodayDateString(): String = dateFormatter.format(Date())

    fun getCardsForDeck(deckId: Long): Flow<List<FlashcardEntity>> = flashcardDao.getCardsForDeck(deckId)

    fun getDeckById(deckId: Long): Flow<DeckEntity?> = deckDao.getDeckById(deckId)

    suspend fun initializePreloadIfEmpty() = withContext(Dispatchers.IO) {
        val count = deckDao.countDecks()
        if (count == 0) {
            populateDemoDecks()
        }
    }

    suspend fun resetDemoDecks() = withContext(Dispatchers.IO) {
        flashcardDao.clearAllCards()
        deckDao.clearAllDecks()
        populateDemoDecks()
    }

    private suspend fun populateDemoDecks() {
        val demos = DemoDecksProvider.getDemoDecks()
        for (item in demos) {
            val deckId = deckDao.insertDeck(item.deck)
            val mappedCards = item.cards.map { it.copy(deckId = deckId) }
            flashcardDao.insertCards(mappedCards)
        }
    }

    suspend fun createDeck(
        title: String,
        description: String,
        category: String,
        colorHex: String,
        iconName: String
    ): Long = withContext(Dispatchers.IO) {
        val deck = DeckEntity(
            title = title,
            description = description,
            category = category,
            colorHex = colorHex,
            iconName = iconName,
            isDefault = false
        )
        deckDao.insertDeck(deck)
    }

    suspend fun updateDeck(deck: DeckEntity) = withContext(Dispatchers.IO) {
        deckDao.updateDeck(deck)
    }

    suspend fun deleteDeck(deckId: Long) = withContext(Dispatchers.IO) {
        deckDao.deleteDeckById(deckId)
    }

    suspend fun createCard(
        deckId: Long,
        question: String,
        answer: String,
        hint: String
    ): Long = withContext(Dispatchers.IO) {
        val card = FlashcardEntity(
            deckId = deckId,
            question = question,
            answer = answer,
            hint = hint,
            status = CardStatus.NEW
        )
        flashcardDao.insertCard(card)
    }

    suspend fun updateCard(card: FlashcardEntity) = withContext(Dispatchers.IO) {
        flashcardDao.updateCard(card)
    }

    suspend fun deleteCard(cardId: Long) = withContext(Dispatchers.IO) {
        flashcardDao.deleteCardById(cardId)
    }

    suspend fun getTodayQuota(): DailyQuotaEntity = withContext(Dispatchers.IO) {
        val today = getTodayDateString()
        dailyQuotaDao.getQuota(today) ?: DailyQuotaEntity(dateString = today)
    }

    suspend fun isBacklogActive(deckId: Long?, currentTime: Long = System.currentTimeMillis()): Boolean = withContext(Dispatchers.IO) {
        val overdueCount = if (deckId != null) {
            flashcardDao.countOverdueCardsForDeck(deckId, currentTime)
        } else {
            flashcardDao.countOverdueCards(currentTime)
        }
        overdueCount >= EbbinghausEngine.BACKLOG_TRIGGER_THRESHOLD
    }

    suspend fun getOverdueCount(deckId: Long?, currentTime: Long = System.currentTimeMillis()): Int = withContext(Dispatchers.IO) {
        if (deckId != null) {
            flashcardDao.countOverdueCardsForDeck(deckId, currentTime)
        } else {
            flashcardDao.countOverdueCards(currentTime)
        }
    }

    suspend fun prepareStudySession(
        deckId: Long?,
        currentTime: Long = System.currentTimeMillis()
    ): StudySessionQueue = withContext(Dispatchers.IO) {
        val todayQuota = getTodayQuota()

        // 1. Get due review cards (dueDate <= now)
        val allDueCards = if (deckId != null) {
            flashcardDao.getDueCardsForDeck(deckId, currentTime).first()
        } else {
            flashcardDao.getDueCards(currentTime).first()
        }

        val overdueCount = allDueCards.size
        val isBacklog = overdueCount >= EbbinghausEngine.BACKLOG_TRIGGER_THRESHOLD

        // Dose review cards to portion limit (e.g. 35 cards)
        val reviewPortion = allDueCards.take(EbbinghausEngine.DAILY_REVIEW_PORTION)

        // 2. New cards logic:
        // If backlog defense is active or overdue cards remain, block new material!
        val newCards = if (isBacklog || overdueCount > 0) {
            emptyList()
        } else {
            // Check daily quota for new cards (Miller's wallet: max 20 per day)
            val remainingNewQuota = (EbbinghausEngine.MAX_NEW_CARDS_PER_DAY - todayQuota.newCardsCount).coerceAtLeast(0)
            if (remainingNewQuota > 0) {
                val availableNew = if (deckId != null) {
                    flashcardDao.getNewCardsForDeck(deckId).first()
                } else {
                    emptyList()
                }
                availableNew.take(remainingNewQuota)
            } else {
                emptyList()
            }
        }

        StudySessionQueue(
            cards = (reviewPortion + newCards).distinctBy { it.id },
            isBacklogActive = isBacklog,
            totalOverdueCount = overdueCount,
            todayQuota = todayQuota,
            isNewMaterialBlocked = isBacklog || overdueCount > 0
        )
    }

    suspend fun recordCardReview(
        card: FlashcardEntity,
        grade: ReviewGrade,
        currentTime: Long = System.currentTimeMillis()
    ): FlashcardEntity = withContext(Dispatchers.IO) {
        val wasNew = card.status == CardStatus.NEW
        val result = EbbinghausEngine.processReview(card, grade, currentTime)
        flashcardDao.updateCard(result.updatedCard)

        // Update daily quota
        val today = getTodayDateString()
        val currentQuota = dailyQuotaDao.getQuota(today) ?: DailyQuotaEntity(dateString = today)
        val updatedQuota = if (wasNew) {
            currentQuota.copy(newCardsCount = currentQuota.newCardsCount + 1)
        } else {
            currentQuota.copy(reviewCardsCount = currentQuota.reviewCardsCount + 1)
        }
        dailyQuotaDao.insertOrUpdateQuota(updatedQuota)

        result.updatedCard
    }
}

data class StudySessionQueue(
    val cards: List<FlashcardEntity>,
    val isBacklogActive: Boolean,
    val totalOverdueCount: Int,
    val todayQuota: DailyQuotaEntity,
    val isNewMaterialBlocked: Boolean
)
