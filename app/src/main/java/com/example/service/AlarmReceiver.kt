package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.database.WaterDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "Exact alarm fired! Verifying active window & triggering lock-screen alarm.")
        
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        val wakeLock = powerManager.newWakeLock(
            android.os.PowerManager.PARTIAL_WAKE_LOCK,
            "HydroForce:AlarmReceiverWakeLock"
        )
        // Acquire wake lock to keep the CPU awake while we query database and build notification
        wakeLock.acquire(15000L)

        val pendingResult = goAsync()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Check persistent alarm active state first to prevent duplicate trigger queueing
                val prefs = context.getSharedPreferences("HydroForcePrefs", Context.MODE_PRIVATE)
                val isAlarmActive = prefs.getBoolean("is_alarm_active", false)
                if (isAlarmActive) {
                    Log.d("AlarmReceiver", "An alarm is already active/ringing. Ignoring new broadcast trigger to prevent queueing.")
                    return@launch
                }

                val db = WaterDatabase.getDatabase(context)
                val settings = db.waterDao().getSettingsDirect() ?: com.example.data.database.Settings()

                // Check active window
                val calendar = Calendar.getInstance()
                val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                val isWithinTimeframe = if (settings.startHour <= settings.endHour) {
                    currentHour in settings.startHour until settings.endHour
                } else {
                    currentHour >= settings.startHour || currentHour < settings.endHour
                }

                if (isWithinTimeframe) {
                    Log.d("AlarmReceiver", "Triggering alarm notification ($currentHour is within active timeframe).")
                    
                    // Mark alarm as active and ringing
                    prefs.edit().putBoolean("is_alarm_active", true).apply()

                    ReminderWorker.sendReminderNotification(context, settings.soundEnabled)
                    
                    // Also start continuous intrusive foreground AlarmService
                    try {
                        val serviceIntent = Intent(context, AlarmService::class.java)
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                    } catch (e: Exception) {
                        Log.e("AlarmReceiver", "Failed starting AlarmService on broadcast", e)
                    }
                } else {
                    Log.d("AlarmReceiver", "Muffling alarm notification ($currentHour is outside active timeframe).")
                }
            } catch (e: Exception) {
                Log.e("AlarmReceiver", "Error receiving Alarm", e)
            } finally {
                try {
                    if (wakeLock.isHeld) {
                        wakeLock.release()
                    }
                } catch (e: Exception) {
                    Log.e("AlarmReceiver", "Error releasing wake lock", e)
                }
                pendingResult.finish()
            }
        }
    }
}
