package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterDao {
    @Query("SELECT * FROM drink_logs ORDER BY timestamp DESC")
    fun getAllDrinkLogs(): Flow<List<DrinkLog>>

    @Query("SELECT * FROM drink_logs WHERE timestamp >= :startOfToday ORDER BY timestamp DESC")
    fun getTodayDrinkLogs(startOfToday: Long): Flow<List<DrinkLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrinkLog(log: DrinkLog)

    @Query("DELETE FROM drink_logs WHERE id = :id")
    suspend fun deleteDrinkLogById(id: Int)

    @Query("SELECT * FROM drink_logs ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestDrinkLogDirect(): DrinkLog?

    // Settings queries
    @Query("SELECT * FROM settings_table WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<Settings?>

    @Query("SELECT * FROM settings_table WHERE id = 1 LIMIT 1")
    suspend fun getSettingsDirect(): Settings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: Settings)
}
