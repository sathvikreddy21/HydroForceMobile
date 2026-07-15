package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.database.WaterDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlarmService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AlarmService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "AlarmService onStartCommand action: ${intent?.action}")

        if (intent?.action == ACTION_STOP) {
            stopAlarm()
            stopSelf()
            return START_NOT_STICKY
        }

        // Run asynchronously to query settings database before sound triggering
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = WaterDatabase.getDatabase(this@AlarmService)
                val settings = db.waterDao().getSettingsDirect() ?: com.example.data.database.Settings()
                
                withContext(Dispatchers.Main) {
                    startAlarm(settings)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load settings in AlarmService", e)
                withContext(Dispatchers.Main) {
                    // Fallback to starting with default settings if database fails
                    startAlarm(com.example.data.database.Settings())
                }
            }
        }

        return START_STICKY
    }

    private fun startAlarm(settings: com.example.data.database.Settings) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create a unique notification channel to enforce high priority and override caches
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "HydroForce Realtime Alarms"
            val descriptionText = "Loud intrusive alarms to nudge you to hydrate"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                // Sound and vibration are played manually via MediaPlayer/Vibrator in the service for custom looping
                setSound(null, null)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent to launch MainActivity with trigger flag
        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra("action_trigger_reminder", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            1001,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Notification designed to show on lock screen
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("🚨 HYDROFORCE GULP ALARM! 🚨")
            .setContentText("Your scheduled timer completed! Drink up to stop the ring!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        // Play continuous looping sound if enabled
        if (settings.soundEnabled) {
            try {
                if (mediaPlayer == null) {
                    // Check if custom raw file 'alarm.mp3' exists in the project
                    val customRawId = resources.getIdentifier("alarm", "raw", packageName)
                    val soundUri: Uri = if (customRawId != 0) {
                        Uri.parse("android.resource://$packageName/$customRawId")
                    } else {
                        // System fallback alarm
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    }

                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(this@AlarmService, soundUri)
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        isLooping = true
                        prepare()
                        start()
                    }
                    Log.d(TAG, "Started looping media playback with sound: $soundUri")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start media player", e)
            }

            // Start continuous vibration pattern
            try {
                vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vibratorManager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }

                vibrator?.let {
                    val pattern = longArrayOf(0, 1000, 1000) // vibrate 1s, sleep 1s
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        it.vibrate(VibrationEffect.createWaveform(pattern, 0))
                    } else {
                        @Suppress("DEPRECATION")
                        it.vibrate(pattern, 0)
                    }
                    Log.d(TAG, "Started looping vibration")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start vibration in AlarmService", e)
            }
        }
    }

    private fun stopAlarm() {
        Log.d(TAG, "Stopping alarm ringers and vibrators")
        
        // Reset persistent alarm active state so new alarms can be scheduled/triggered in the future
        val prefs = getSharedPreferences("HydroForcePrefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_alarm_active", false).apply()

        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media player", e)
        }

        try {
            vibrator?.cancel()
            vibrator = null
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling vibrator", e)
        }
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
        Log.d(TAG, "AlarmService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val TAG = "AlarmService"
        const val CHANNEL_ID = "hydroforce_alarm_service_v3"
        const val NOTIFICATION_ID = 9999
        const val ACTION_STOP = "action_stop_alarm"
    }
}
