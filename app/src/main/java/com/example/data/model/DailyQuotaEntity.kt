package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_quota")
data class DailyQuotaEntity(
    @PrimaryKey
    val dateString: String, // format YYYY-MM-DD
    val newCardsCount: Int = 0,
    val reviewCardsCount: Int = 0
)
