package com.example.ui.viewmodel

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.DrinkLog
import com.example.data.database.Settings
import com.example.data.database.WaterDatabase
import com.example.data.repository.WaterRepository
import com.example.service.ReminderScheduler
import com.example.service.ReminderWorker
import com.example.service.AlarmService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class WaterViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "WaterViewModel"
    private val repository: WaterRepository
    private val appLaunchTime = System.currentTimeMillis()
    private var timerCheckJob: kotlinx.coroutines.Job? = null

    val todayDrinkLogs: StateFlow<List<DrinkLog>>
    val allDrinkLogs: StateFlow<List<DrinkLog>>
    val settingsState: StateFlow<Settings>

    // Active full screen reminder state
    private val _isReminderActive = MutableStateFlow(false)
    val isReminderActive: StateFlow<Boolean> = _isReminderActive.asStateFlow()

    private val _reminderActiveTime = MutableStateFlow<Long>(0L)
    val reminderActiveTime: StateFlow<Long> = _reminderActiveTime.asStateFlow()

    private val _cameraFailedAttempts = MutableStateFlow(0)
    val cameraFailedAttempts: StateFlow<Int> = _cameraFailedAttempts.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _analysisMessage = MutableStateFlow<String?>(null)
    val analysisMessage: StateFlow<String?> = _analysisMessage.asStateFlow()

    init {
        val database = WaterDatabase.getDatabase(application)
        repository = WaterRepository(database.waterDao())

        todayDrinkLogs = repository.getTodayLogs()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        allDrinkLogs = repository.allDrinkLogs
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        settingsState = repository.settings
            .map { it ?: Settings() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = Settings()
            )

        // Initialize background scheduling based on saved settings
        viewModelScope.launch {
            val s = repository.getSettingsDirect()
            
            // Clear any stale running service, ringing, active reminder UI, or delivered notifications
            dismissActiveReminder()
            
            // Schedule the single fresh single-shot alarm starting from now
            ReminderScheduler.scheduleWaterReminders(application, s.intervalMinutes)
        }

        startTimerChecker()
    }

    private fun startTimerChecker() {
        timerCheckJob?.cancel()
        timerCheckJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000)

                // Skip checking if alarm is already active (either in UI or persistent state)
                val prefs = getApplication<Application>().getSharedPreferences("HydroForcePrefs", Context.MODE_PRIVATE)
                val isAlarmActive = prefs.getBoolean("is_alarm_active", false)
                if (isAlarmActive || _isReminderActive.value) continue

                val logs = todayDrinkLogs.value
                val settings = settingsState.value

                // Verify timeframe
                val calendar = Calendar.getInstance()
                val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                val isWithinTimeframe = if (settings.startHour <= settings.endHour) {
                    currentHour in settings.startHour until settings.endHour
                } else {
                    currentHour >= settings.startHour || currentHour < settings.endHour
                }

                if (!isWithinTimeframe) continue

                // If no logs yet today, reference from app launch time so we don't lock on initial open
                val referenceTime = logs.firstOrNull()?.timestamp ?: appLaunchTime
                val nextReminderTime = referenceTime + (settings.intervalMinutes * 60 * 1000)

                if (System.currentTimeMillis() >= nextReminderTime) {
                    Log.d(TAG, "Foreground Timer expired! Automatically triggering HydroForce Lock & Siren Alarm.")
                    triggerActiveReminder()
                }
            }
        }
    }

    private var mediaPlayer: android.media.MediaPlayer? = null

    fun startRinging() {
        val settings = settingsState.value
        if (!settings.soundEnabled) return
        
        if (mediaPlayer == null) {
            try {
                val alertUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                    ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)
                    ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                mediaPlayer = android.media.MediaPlayer.create(getApplication(), alertUri).apply {
                    isLooping = true
                    start()
                }
                Log.d(TAG, "Started looping reminder alarm sound.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start ringing", e)
            }
        }
    }

    fun stopRinging() {
        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
                Log.d(TAG, "Stopped looping reminder alarm sound.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop media player for ringing", e)
            }
            mediaPlayer = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerCheckJob?.cancel()
        stopRinging()
    }

    fun triggerActiveReminder() {
        _isReminderActive.value = true
        _reminderActiveTime.value = System.currentTimeMillis()
        _cameraFailedAttempts.value = 0
        _analysisMessage.value = "Take a photo of yourself drinking to resolve the HydroForce lock!"
        startRinging()

        // Explicitly start continuous AlarmService to play loud loop in foreground and background
        try {
            val serviceIntent = Intent(getApplication(), AlarmService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                getApplication<Application>().startForegroundService(serviceIntent)
            } else {
                getApplication<Application>().startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed starting AlarmService on triggerActiveReminder", e)
        }
    }

    fun dismissActiveReminder() {
        _isReminderActive.value = false
        _cameraFailedAttempts.value = 0
        _analysisMessage.value = null
        stopRinging()
        
        // Reset persistent alarm active state
        try {
            val prefs = getApplication<Application>().getSharedPreferences("HydroForcePrefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("is_alarm_active", false).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset is_alarm_active in SharedPreferences", e)
        }

        // Stop the loud looping AlarmService
        try {
            val stopIntent = Intent(getApplication(), AlarmService::class.java).apply {
                action = AlarmService.ACTION_STOP
            }
            getApplication<Application>().startService(stopIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed stopping AlarmService", e)
        }
        
        // Remove ongoing notification if active
        try {
            val notificationManager = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(ReminderWorker.NOTIFICATION_ID)
            notificationManager.cancel(AlarmService.NOTIFICATION_ID)
            notificationManager.cancelAll()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dismiss ongoing notification", e)
        }
    }

    fun incrementFailedResult(shameMessage: String) {
        _cameraFailedAttempts.value += 1
        _analysisMessage.value = shameMessage
    }

    fun setAnalyzing(analyzing: Boolean, status: String? = null) {
        _isAnalyzing.value = analyzing
        _analysisMessage.value = status
    }

    fun recordConfirmedDrink(method: String, imageUri: String?, isVerified: Boolean = true) {
        viewModelScope.launch {
            // Log 250ml (generic glass)
            val log = DrinkLog(
                timestamp = System.currentTimeMillis(),
                verified = isVerified,
                method = method,
                imageUri = imageUri,
                amountMl = 250
            )
            repository.insertDrinkLog(log)
            dismissActiveReminder()

            // Schedule the next single-shot exact alarm fresh, only after successful verification!
            val s = repository.getSettingsDirect()
            ReminderScheduler.scheduleWaterReminders(getApplication(), s.intervalMinutes)
        }
    }

    fun overrideManualReminder(reason: String) {
        viewModelScope.launch {
            val log = DrinkLog(
                timestamp = System.currentTimeMillis(),
                verified = false,
                method = "Manual override: $reason",
                imageUri = null,
                amountMl = 250
            )
            repository.insertDrinkLog(log)
            dismissActiveReminder()

            // Schedule the next single-shot exact alarm fresh, only after successful verification!
            val s = repository.getSettingsDirect()
            ReminderScheduler.scheduleWaterReminders(getApplication(), s.intervalMinutes)
        }
    }

    fun deleteDrinkLog(id: Int) {
        viewModelScope.launch {
            repository.deleteDrinkLogById(id)
        }
    }

    fun updateSettings(intervalMinutes: Int, dailyGoalMl: Int, sound: Boolean, streakMode: Boolean, shameMode: Boolean, startHour: Int, endHour: Int) {
        viewModelScope.launch {
            val current = settingsState.value
            val update = current.copy(
                intervalMinutes = intervalMinutes,
                dailyGoalMl = dailyGoalMl,
                soundEnabled = sound,
                streakModeEnabled = streakMode,
                shameModeEnabled = shameMode,
                startHour = startHour,
                endHour = endHour
            )
            repository.saveSettings(update)
            // Reschedule
            ReminderScheduler.scheduleWaterReminders(getApplication(), intervalMinutes)
        }
    }

    fun sendTestNotificationImmediately() {
        viewModelScope.launch {
            val s = repository.getSettingsDirect()
            ReminderWorker.sendReminderNotification(getApplication(), s.soundEnabled)
        }
    }
}
