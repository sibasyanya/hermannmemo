package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "flashcards",
    foreignKeys = [
        ForeignKey(
            entity = DeckEntity::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("deckId"),
        Index("dueDate"),
        Index("status")
    ]
)
data class FlashcardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val deckId: Long,
    val question: String,
    val answer: String,
    val hint: String = "",
    val intervalDays: Double = 0.0,
    val repetitionCount: Int = 0,
    val easeFactor: Double = 2.5,
    val dueDate: Long = 0L,
    val lastReviewedAt: Long? = null,
    val status: CardStatus = CardStatus.NEW,
    val isHeavy: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
