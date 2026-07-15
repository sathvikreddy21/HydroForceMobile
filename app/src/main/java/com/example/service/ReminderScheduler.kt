package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.database.WaterDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val TAG = "ReminderScheduler"
    private const val UNIQUE_WORK_NAME = "hydroforce_reminder_work"

    fun scheduleWaterReminders(context: Context, intervalMinutes: Int) {
        Log.d(TAG, "Scheduling reminders. Interval: $intervalMinutes minutes.")

        // 1. Cancel all existing alarms and notifications first to prevent duplicates
        disableWaterReminders(context)

        // 2. Schedule single-shot Exact Alarm via AlarmManager
        scheduleExactAlarm(context, intervalMinutes)
    }

    private fun scheduleExactAlarm(context: Context, intervalMinutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1234,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = WaterDatabase.getDatabase(context)
                val latestLog = db.waterDao().getLatestDrinkLogDirect()
                
                val referenceTime = latestLog?.timestamp ?: System.currentTimeMillis()
                var triggerTime = referenceTime + (intervalMinutes * 60L * 1000L)

                if (triggerTime <= System.currentTimeMillis()) {
                    triggerTime = System.currentTimeMillis() + (intervalMinutes * 60L * 1000L)
                }

                Log.d(TAG, "Exact Alarm Scheduled. Interval: $intervalMinutes mins. Next alarm in ${(triggerTime - System.currentTimeMillis()) / 1000}s")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed scheduling alarm", e)
            }
        }
    }

    fun disableWaterReminders(context: Context) {
        val workManager = WorkManager.getInstance(context)
        Log.d(TAG, "Disabling reminders.")
        workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
        workManager.cancelAllWorkByTag("water_reminder")

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1234,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
