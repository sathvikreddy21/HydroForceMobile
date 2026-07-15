package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ReminderModalScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.WaterViewModel

class MainActivity : ComponentActivity() {
  private val viewModel: WaterViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Configure window/activity flags to show on top of keyguard and keep screen awake
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
      setShowWhenLocked(true)
      setTurnScreenOn(true)
    } else {
      @Suppress("DEPRECATION")
      window.addFlags(
        android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            or android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            or android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
      )
    }
    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    // Intercept active trigger reminder launch extra
    checkIntentForReminder(intent)

    setContent {
      MyApplicationTheme {
        val isReminderActive by viewModel.isReminderActive.collectAsState()

        if (isReminderActive) {
          // Absolute fullscreen blocking modal that cannot be dismissed
          ReminderModalScreen(
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize()
          )
        } else {
          // Core scaffold system
          var selectedTab by remember { mutableStateOf(0) }

          Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
              NavigationBar(
                containerColor = Color(0xFF020617),
                contentColor = Color.White
              ) {
                NavigationBarItem(
                  selected = selectedTab == 0,
                  onClick = { selectedTab = 0 },
                  icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Home") },
                  label = { Text("Dashboard") },
                  colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF0EA5E9),
                    selectedTextColor = Color(0xFF0EA5E9),
                    indicatorColor = Color(0x330EA5E9),
                    unselectedIconColor = Color.White.copy(alpha = 0.5f),
                    unselectedTextColor = Color.White.copy(alpha = 0.5f)
                  )
                )

                NavigationBarItem(
                  selected = selectedTab == 1,
                  onClick = { selectedTab = 1 },
                  icon = { Icon(imageVector = Icons.Default.BarChart, contentDescription = "Stats") },
                  label = { Text("Analytics") },
                  colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF0EA5E9),
                    selectedTextColor = Color(0xFF0EA5E9),
                    indicatorColor = Color(0x330EA5E9),
                    unselectedIconColor = Color.White.copy(alpha = 0.5f),
                    unselectedTextColor = Color.White.copy(alpha = 0.5f)
                  )
                )

                NavigationBarItem(
                  selected = selectedTab == 2,
                  onClick = { selectedTab = 2 },
                  icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings") },
                  label = { Text("Settings") },
                  colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF0EA5E9),
                    selectedTextColor = Color(0xFF0EA5E9),
                    indicatorColor = Color(0x330EA5E9),
                    unselectedIconColor = Color.White.copy(alpha = 0.5f),
                    unselectedTextColor = Color.White.copy(alpha = 0.5f)
                  )
                )
              }
            }
          ) { innerPadding ->
            val contentModifier = Modifier
              .fillMaxSize()
              .padding(innerPadding)

            when (selectedTab) {
              0 -> HomeScreen(viewModel = viewModel, modifier = contentModifier)
              1 -> StatsScreen(viewModel = viewModel, modifier = contentModifier)
              2 -> SettingsScreen(viewModel = viewModel, modifier = contentModifier)
            }
          }
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    checkIntentForReminder(intent)
  }

  private fun checkIntentForReminder(intent: Intent?) {
    if (intent != null && intent.getBooleanExtra("action_trigger_reminder", false)) {
      viewModel.triggerActiveReminder()
    }
  }
}

