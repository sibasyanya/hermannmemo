package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.DailyQuotaDao
import com.example.data.dao.DeckDao
import com.example.data.dao.FlashcardDao
import com.example.data.model.DailyQuotaEntity
import com.example.data.model.DeckEntity
import com.example.data.model.FlashcardEntity

@Database(
    entities = [
        DeckEntity::class,
        FlashcardEntity::class,
        DailyQuotaEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun dailyQuotaDao(): DailyQuotaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ebbinghaus_memory.db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
