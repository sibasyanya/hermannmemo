package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CardStatus
import com.example.data.model.FlashcardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcards ORDER BY id ASC")
    fun getAllCards(): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId ORDER BY id ASC")
    fun getCardsForDeck(deckId: Long): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId")
    suspend fun getCardsForDeckSync(deckId: Long): List<FlashcardEntity>

    @Query("SELECT * FROM flashcards WHERE id = :id")
    suspend fun getCardById(id: Long): FlashcardEntity?

    @Query("SELECT * FROM flashcards WHERE status != 'NEW' AND dueDate <= :currentTime ORDER BY dueDate ASC")
    fun getDueCards(currentTime: Long): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId AND status != 'NEW' AND dueDate <= :currentTime ORDER BY dueDate ASC")
    fun getDueCardsForDeck(deckId: Long, currentTime: Long): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId AND status = 'NEW' ORDER BY id ASC")
    fun getNewCardsForDeck(deckId: Long): Flow<List<FlashcardEntity>>

    @Query("SELECT COUNT(*) FROM flashcards WHERE status != 'NEW' AND dueDate <= :currentTime")
    suspend fun countOverdueCards(currentTime: Long): Int

    @Query("SELECT COUNT(*) FROM flashcards WHERE deckId = :deckId AND status != 'NEW' AND dueDate <= :currentTime")
    suspend fun countOverdueCardsForDeck(deckId: Long, currentTime: Long): Int

    @Query("SELECT COUNT(*) FROM flashcards WHERE deckId = :deckId AND status = 'NEW'")
    suspend fun countNewCardsForDeck(deckId: Long): Int

    @Query("SELECT COUNT(*) FROM flashcards WHERE status = :status")
    fun countCardsByStatus(status: CardStatus): Flow<Int>

    @Query("SELECT COUNT(*) FROM flashcards WHERE deckId = :deckId AND status = :status")
    fun countCardsByStatusForDeck(deckId: Long, status: CardStatus): Flow<Int>

    @Query("SELECT COUNT(*) FROM flashcards")
    fun countTotalCards(): Flow<Int>

    @Query("SELECT COUNT(*) FROM flashcards WHERE deckId = :deckId")
    fun countCardsInDeck(deckId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: FlashcardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<FlashcardEntity>): List<Long>

    @Update
    suspend fun updateCard(card: FlashcardEntity)

    @Delete
    suspend fun deleteCard(card: FlashcardEntity)

    @Query("DELETE FROM flashcards WHERE id = :id")
    suspend fun deleteCardById(id: Long)

    @Query("DELETE FROM flashcards")
    suspend fun clearAllCards()
}
