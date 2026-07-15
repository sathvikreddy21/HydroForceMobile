package com.example.data.repository

import com.example.data.database.DrinkLog
import com.example.data.database.Settings
import com.example.data.database.WaterDao
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class WaterRepository(private val waterDao: WaterDao) {

    val allDrinkLogs: Flow<List<DrinkLog>> = waterDao.getAllDrinkLogs()

    fun getTodayLogs(): Flow<List<DrinkLog>> {
        return waterDao.getTodayDrinkLogs(getStartOfToday())
    }

    val settings: Flow<Settings?> = waterDao.getSettings()

    suspend fun getSettingsDirect(): Settings {
        return waterDao.getSettingsDirect() ?: Settings()
    }

    suspend fun insertDrinkLog(log: DrinkLog) {
        waterDao.insertDrinkLog(log)
    }

    suspend fun deleteDrinkLogById(id: Int) {
        waterDao.deleteDrinkLogById(id)
    }

    suspend fun saveSettings(settings: Settings) {
        waterDao.insertSettings(settings)
    }

    companion object {
        fun getStartOfToday(): Long {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }
    }
}
