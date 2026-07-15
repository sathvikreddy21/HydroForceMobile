package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drink_logs")
data class DrinkLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val verified: Boolean,
    val method: String, // "Gemini AI", "Google Vision", "Manual Confirm", etc.
    val imageUri: String?,
    val amountMl: Int
)
