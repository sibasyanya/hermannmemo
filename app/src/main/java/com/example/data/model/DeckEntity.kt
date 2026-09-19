package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "decks")
data class DeckEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val category: String,
    val iconName: String = "school",
    val colorHex: String = "#4F46E5",
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
