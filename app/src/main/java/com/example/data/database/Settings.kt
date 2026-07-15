package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings_table")
data class Settings(
    @PrimaryKey val id: Int = 1,
    val intervalMinutes: Int = 60,
    val dailyGoalMl: Int = 2000,
    val soundEnabled: Boolean = true,
    val streakModeEnabled: Boolean = true,
    val shameModeEnabled: Boolean = true,
    val startHour: Int = 9,
    val endHour: Int = 22
)
