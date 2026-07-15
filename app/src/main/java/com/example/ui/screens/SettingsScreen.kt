package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ui.viewmodel.WaterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: WaterViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settingsState.collectAsState()

    var goalMl by remember { mutableStateOf(2000f) }
    var intervalMins by remember { mutableStateOf(60f) }
    var startHour by remember { mutableStateOf(9) }
    var endHour by remember { mutableStateOf(22) }
    
    var isSoundEnabled by remember { mutableStateOf(true) }
    var isStreakEnabled by remember { mutableStateOf(true) }
    var isShameEnabled by remember { mutableStateOf(true) }

    // Sync state once settings are loaded
    LaunchedEffect(settings) {
        goalMl = settings.dailyGoalMl.toFloat()
        intervalMins = settings.intervalMinutes.toFloat()
        startHour = settings.startHour
        endHour = settings.endHour
        isSoundEnabled = settings.soundEnabled
        isStreakEnabled = settings.streakModeEnabled
        isShameEnabled = settings.shameModeEnabled
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020617)) // Deep Space midnight black background from HTML
    ) {
        // Ambient background glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x0F0EA5E9), Color.Transparent),
                        radius = 1200f
                    )
                )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp), // strict 24px side margins per rules
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 40.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                color = Color(0xFF89CEFF), // High contrast premium primary header
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-1).sp
                            )
                        )
                        Text(
                            text = "Precision configuration of hydration HUD.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFFBEC8D2),
                                fontSize = 13.sp
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(Color(0x1F89CEFF), RoundedCornerShape(100.dp))
                            .border(BorderStroke(1.dp, Color(0x3389CEFF)), RoundedCornerShape(100.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "PRO VERSION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF89CEFF)
                        )
                    }
                }
            }

            // Hydration Goals Bento Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0x0DFFFFFF)), // translucent glass
                    shape = RoundedCornerShape(24.dp), // standard 24px container radius
                    border = BorderStroke(1.dp, Color(0x1AFFFFFF))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = Color(0xFF89CEFF)
                            )
                            Text(
                                text = "Hydration Targets",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        // Daily Goal Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Daily Goal",
                                    color = Color(0xFFBEC8D2),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "${goalMl.toInt()}ml",
                                    color = Color(0xFF89CEFF),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Slider(
                                value = goalMl,
                                onValueChange = { goalMl = it },
                                valueRange = 1000f..5000f,
                                steps = 39, // steps of 100ml
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF89CEFF),
                                    activeTrackColor = Color(0xFF0EA5E9),
                                    inactiveTrackColor = Color(0x1AFFFFFF)
                                )
                            )
                        }

                        // Reminder Interval
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Reminder Interval",
                                    color = Color(0xFFBEC8D2),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = formatInterval(intervalMins.toInt()),
                                    color = Color(0xFF89CEFF),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Slider(
                                value = intervalMins,
                                onValueChange = { intervalMins = it },
                                valueRange = 1f..180f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF89CEFF),
                                    activeTrackColor = Color(0xFF0EA5E9),
                                    inactiveTrackColor = Color(0x1AFFFFFF)
                                )
                            )
                            Text(
                                text = "Supports precise interval wake-ups from 1 to 180 minutes.",
                                fontSize = 11.sp,
                                color = Color.White.copy(0.4f)
                            )
                        }
                    }
                }
            }

            // Reminders Custom Active Timeframe (Bento)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0x0DFFFFFF)), // translucent glass
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0x1AFFFFFF))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = Color(0xFF89CEFF)
                            )
                            Text(
                                text = "Awake Active Window",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        
                        Text(
                            text = "Muffles loop notifications outside this active frame to preserve sleep indices.",
                            color = Color(0xFFBEC8D2),
                            fontSize = 12.sp
                        )

                        // Start Hour Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Start Morning Frame",
                                    color = Color(0xFFBEC8D2),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = formatHour(startHour),
                                    color = Color(0xFF89CEFF),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Slider(
                                value = startHour.toFloat(),
                                onValueChange = { startHour = it.toInt() },
                                valueRange = 0f..23f,
                                steps = 23,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF89CEFF),
                                    activeTrackColor = Color(0xFF0EA5E9),
                                    inactiveTrackColor = Color(0x1AFFFFFF)
                                )
                            )
                        }

                        // End Hour Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "End Night Frame",
                                    color = Color(0xFFBEC8D2),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = formatHour(endHour),
                                    color = Color(0xFF89CEFF),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Slider(
                                value = endHour.toFloat(),
                                onValueChange = { endHour = it.toInt() },
                                valueRange = 0f..23f,
                                steps = 23,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF89CEFF),
                                    activeTrackColor = Color(0xFF0EA5E9),
                                    inactiveTrackColor = Color(0x1AFFFFFF)
                                )
                            )
                        }
                    }
                }
            }

            // Preferences Switches Bento
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0x0DFFFFFF)), // translucent glass
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0x1AFFFFFF))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = Color(0xFF89CEFF)
                            )
                            Text(
                                text = "Preferences",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        // Sound switch
                        PreferenceToggleRow(
                            title = "Loop Reminder Sound",
                            description = "Plays ongoing chime/alarm sounds recursively until completed",
                            checked = isSoundEnabled,
                            onCheckedChange = { isSoundEnabled = it }
                        )

                        HorizontalDivider(color = Color.White.copy(0.08f), thickness = 1.dp)

                        // Sarcastic shame
                        PreferenceToggleRow(
                            title = "Humorous Shame Mode",
                            description = "Fails trigger witty, sarcastic verbal warnings to inspire compliance",
                            checked = isShameEnabled,
                            onCheckedChange = { isShameEnabled = it }
                        )

                        HorizontalDivider(color = Color.White.copy(0.08f), thickness = 1.dp)

                        // Streak mode
                        PreferenceToggleRow(
                            title = "Streak Shield Protection",
                            description = "Highlight streaks and logs with interactive fire indicators",
                            checked = isStreakEnabled,
                            onCheckedChange = { isStreakEnabled = it }
                        )
                    }
                }
            }

            // Diagnostics and Testing Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0x0DFFFFFF)),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0x1AFFFFFF))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "HUD Diagnostics & Verification",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.sendTestNotificationImmediately()
                                    Toast.makeText(context, "Test trigger pushed to shade!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0x1F89CEFF)),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 12.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Test Notification", color = Color(0xFF89CEFF), fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    viewModel.triggerActiveReminder()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 12.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Trigger Reminder Lock", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Save settings CTA button
            item {
                Button(
                    onClick = {
                        viewModel.updateSettings(
                            intervalMinutes = intervalMins.toInt(),
                            dailyGoalMl = goalMl.toInt(),
                            sound = isSoundEnabled,
                            streakMode = isStreakEnabled,
                            shameMode = isShameEnabled,
                            startHour = startHour,
                            endHour = endHour
                        )
                        Toast.makeText(context, "Core settings successfully logged!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF0EA5E9), Color(0xFF03B5D3))
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "SAVE SYSTEM PREFERENCES",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PreferenceToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            Text(
                text = description,
                color = Color(0xFFBEC8D2),
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF020617),
                checkedTrackColor = Color(0xFF89CEFF),
                uncheckedThumbColor = Color(0xFF88929b),
                uncheckedTrackColor = Color(0x33FFFFFF)
            )
        )
    }
}

private fun formatHour(hour: Int): String {
    return when {
        hour == 0 -> "12:00 AM"
        hour == 12 -> "12:00 PM"
        hour > 12 -> "${hour - 12}:00 PM"
        else -> "$hour:00 AM"
    }
}

private fun formatInterval(mins: Int): String {
    if (mins < 60) return "$mins mins"
    val hrs = mins / 60
    val rem = mins % 60
    return if (rem == 0) "Every $hrs hr" else "Every $hrs hr $rem mins"
}
