package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.data.database.WaterDatabase
import java.io.File

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d("ReminderWorker", "Reminder worker started")
        
        val db = WaterDatabase.getDatabase(applicationContext)
        val settings = db.waterDao().getSettingsDirect() ?: com.example.data.database.Settings()

        // Verify if active timeframe morning/evening check fits
        val calendar = java.util.Calendar.getInstance()
        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY) // 24-hour hour

        val isWithinTimeframe = if (settings.startHour <= settings.endHour) {
            currentHour in settings.startHour until settings.endHour
        } else {
            currentHour >= settings.startHour || currentHour < settings.endHour
        }

        if (isWithinTimeframe) {
            Log.d("ReminderWorker", "Within active timeframe: $currentHour is in range [${settings.startHour}, ${settings.endHour}). Launching notification.")
            // Send Notification
            sendReminderNotification(applicationContext, settings.soundEnabled)
        } else {
            Log.d("ReminderWorker", "Muffling notification: $currentHour is OUTSIDE active timeframe [${settings.startHour}, ${settings.endHour}).")
        }

        return Result.success()
    }

    companion object {
        private const val CHANNEL_ID = "hydroforce_reminder_channel_v3"
        const val NOTIFICATION_ID = 4591

        fun sendReminderNotification(context: Context, playSound: Boolean) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val alarmSound = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)
                ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = "HydroForce Reminders"
                val descriptionText = "Water intake interval alerts"
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                    if (playSound) {
                        val audioAttributes = android.media.AudioAttributes.Builder()
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                            .build()
                        setSound(alarmSound, audioAttributes)
                        enableVibration(true)
                        vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
                    } else {
                        setSound(null, null)
                    }
                }
                notificationManager.createNotificationChannel(channel)
            }

            // Launch MainActivity with an extra to open the Fullscreen Reminder Modal
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("action_trigger_reminder", true)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val motivationalMessages = listOf(
                "Time for a fresh gulp! Your cells are thirsty.",
                "Hydrate or dry rate! Your body needs 1 glass of water now.",
                "Aqua level check! HydroForce is waiting for your drink proof.",
                "Don't let fatigue win. Drink up and verify your intake!",
                "Unlock your superpower. One glass of water at a time."
            )
            val randomMessage = motivationalMessages.random()

            val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("HydroForce Alarm 🚨")
                .setContentText(randomMessage)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setSound(if (playSound) alarmSound else null)
                .setContentIntent(pendingIntent)
                .setFullScreenIntent(pendingIntent, true)
                .setAutoCancel(false)
                .setOngoing(true) // Cannot be cleared easily from notification tray

            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
        }
    }
}
