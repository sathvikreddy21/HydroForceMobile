package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.database.WaterDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device rebooted. Rescheduling all HydroForce pending alarms.")
            
            // Clear any active alarm state from the previous boot
            val prefs = context.getSharedPreferences("HydroForcePrefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("is_alarm_active", false).apply()

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = WaterDatabase.getDatabase(context)
                    val settings = db.waterDao().getSettingsDirect() ?: com.example.data.database.Settings()
                    
                    Log.d("BootReceiver", "Boot read interval: ${settings.intervalMinutes} minutes. Updating alarm system.")
                    ReminderScheduler.scheduleWaterReminders(context, settings.intervalMinutes)
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Error rescheduling alarms after boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
