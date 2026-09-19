package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.DailyQuotaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyQuotaDao {
    @Query("SELECT * FROM daily_quota WHERE dateString = :dateString")
    suspend fun getQuota(dateString: String): DailyQuotaEntity?

    @Query("SELECT * FROM daily_quota WHERE dateString = :dateString")
    fun getQuotaFlow(dateString: String): Flow<DailyQuotaEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateQuota(quota: DailyQuotaEntity)
}
