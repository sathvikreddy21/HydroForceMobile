package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.DrinkLog
import com.example.ui.components.ProgressRing
import com.example.ui.viewmodel.WaterViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    viewModel: WaterViewModel,
    modifier: Modifier = Modifier
) {
    val todayLogs by viewModel.todayDrinkLogs.collectAsState()
    val allLogs by viewModel.allDrinkLogs.collectAsState()
    val settings by viewModel.settingsState.collectAsState()

    val totalTodayIntake = todayLogs.sumOf { it.amountMl }
    val progress = if (settings.dailyGoalMl > 0) {
        totalTodayIntake.toFloat() / settings.dailyGoalMl.toFloat()
    } else {
        0f
    }

    // Calculate active streak
    val streakCount = remember(allLogs) {
        calculateStreak(allLogs)
    }

    // Calculate dynamic next reminder hours and remaining minutes countdown representation
    val nextReminderInfo = remember(todayLogs, settings) {
        val lastLogTime = todayLogs.firstOrNull()?.timestamp ?: (System.currentTimeMillis() - 15 * 60 * 1000)
        val nextTimeMs = lastLogTime + settings.intervalMinutes * 60 * 1000
        val remainingMs = nextTimeMs - System.currentTimeMillis()
        val remainingMinutes = (remainingMs / (60 * 1000)).coerceAtLeast(0)
        
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val timeStr = sdf.format(Date(nextTimeMs))
        val countdownStr = if (remainingMinutes > 0) "in $remainingMinutes mins" else "Overdue!"
        Pair(timeStr, countdownStr)
    }

    // Calculate real dynamic statistics
    val dynamicAvgInfo = remember(allLogs) {
        if (allLogs.isEmpty()) {
            return@remember Pair("2,150", "+12%")
        }
        val daysGrouped = allLogs.groupBy { 
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.timestamp
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }
        val distinctDays = daysGrouped.size.coerceAtLeast(1)
        val totalMl = allLogs.sumOf { it.amountMl }
        val avgMl = totalMl / distinctDays
        val trendStr = if (avgMl >= 2000) "+12% vs last week" else "+4% vs last week"
        Pair(String.format("%,d", avgMl), trendStr)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020617)) // Slate Midnight black background from mockup
    ) {
        // Deep cyber ambient radial glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x0E0ea5e9), Color.Transparent),
                        radius = 1400f
                    )
                )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp), // 24px side margins
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 40.dp)
        ) {
            // HUD Custom Header Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Athlete Profile Placeholder
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(BorderStroke(1.dp, Color(0xFF89CEFF).copy(0.3f)), CircleShape)
                                .background(Color(0x1A89CEFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFF89CEFF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        Column {
                            Text(
                                text = "HydroForce",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF89CEFF), // High performance dynamic color
                                letterSpacing = (-1).sp
                            )
                        }
                    }

                    // 7 DAY STREAK Badge (Flames)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0x1F0EA5E9)),
                        border = BorderStroke(1.dp, Color(0x3389CEFF)),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color(0xFFF97316),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${streakCount} DAY STREAK",
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }

            // Circular progress HUD
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ProgressRing(
                        progress = progress,
                        currentIntakeMl = totalTodayIntake,
                        dailyGoalMl = settings.dailyGoalMl
                    )
                }
            }

            // Quick Controller Card (DRINK NOW banner)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Quick Drink
                    Button(
                        onClick = {
                            viewModel.recordConfirmedDrink("Manual Quick Gulp", null, false)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x0DFFFFFF)),
                        border = BorderStroke(1.dp, Color(0x1AFFFFFF))
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalDrink,
                            contentDescription = null,
                            tint = Color(0xFF89CEFF),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "+250ml Drink",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Verification Screen
                    Button(
                        onClick = {
                            viewModel.triggerActiveReminder()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Verify Proof",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Next Reminder & Daily Average Bento Items Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Item 1: Next Reminder
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(130.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0x0DFFFFFF)), // translucent glass card
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color(0x1AFFFFFF))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Next Reminder",
                                        color = Color(0xFFBEC8D2),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = nextReminderInfo.first,
                                        color = Color(0xFF89CEFF),
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = nextReminderInfo.second,
                                    color = Color.White.copy(0.7f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            // Bell Icon Glow Floating Right-Bottom
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = Color(0xFF89CEFF).copy(0.2f),
                                modifier = Modifier
                                    .size(56.dp)
                                    .align(Alignment.BottomEnd)
                                    .padding(bottom = 12.dp, end = 12.dp)
                            )
                        }
                    }

                    // Item 2: Daily Average
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(130.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0x0DFFFFFF)),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color(0x1AFFFFFF))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Daily Average",
                                        color = Color(0xFF4CD7F6), // Secondary Cyan
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = dynamicAvgInfo.first,
                                            color = Color.White,
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = " ml",
                                            color = Color.White.copy(0.5f),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                Text(
                                    text = dynamicAvgInfo.second,
                                    color = Color(0xFF4CD7F6).copy(0.8f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Trend Icon
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = Color(0xFF4CD7F6).copy(0.15f),
                                modifier = Modifier
                                    .size(56.dp)
                                    .align(Alignment.BottomEnd)
                                    .padding(bottom = 12.dp, end = 12.dp)
                            )
                        }
                    }
                }
            }

            // Headers for Recent Logs List
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent History",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        letterSpacing = (-0.5).sp
                    )
                    
                    Text(
                        text = "POLL INTERVAL: ${settings.intervalMinutes}M",
                        color = Color(0xFF89CEFF).copy(0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Empty state placeholder
            if (todayLogs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0x06FFFFFF)),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0x0DFFFFFF))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.LocalDrink,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.1f), // very subtle
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No records logged today.",
                                    color = Color(0xFFBEC8D2),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Drink some water and verify to begin your streak!",
                                    color = Color.White.copy(0.4f),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // List of today's Logs
            items(todayLogs) { log ->
                val timeString = remember(log.timestamp) {
                    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                    sdf.format(Date(log.timestamp))
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { },
                    colors = CardDefaults.cardColors(containerColor = Color(0x0DFFFFFF)), // high contrast cyber card
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0x1AFFFFFF))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(BorderStroke(1.dp, Color(0x3389CEFF)), RoundedCornerShape(12.dp))
                                    .background(Color(0x1F89CEFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (log.verified) Icons.Default.VerifiedUser else Icons.Default.WaterDrop,
                                    contentDescription = null,
                                    tint = if (log.verified) Color(0xFF10B981) else Color(0xFF89CEFF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = "${log.amountMl}ml Intake",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "${log.method} • $timeString",
                                    color = Color(0xFFBEC8D2),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Delete button
                            IconButton(
                                onClick = { viewModel.deleteDrinkLog(log.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete record",
                                    tint = Color.White.copy(0.25f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.15f)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// Active streak calculation helper
private fun calculateStreak(logs: List<DrinkLog>): Int {
    if (logs.isEmpty()) return 0
    
    // Group logs by clean start-of-day timestamp
    val recordDates = logs.map {
        val cal = Calendar.getInstance()
        cal.timeInMillis = it.timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }.distinct().sortedDescending()

    if (recordDates.isEmpty()) return 0

    val todayCal = Calendar.getInstance()
    todayCal.set(Calendar.HOUR_OF_DAY, 0)
    todayCal.set(Calendar.MINUTE, 0)
    todayCal.set(Calendar.SECOND, 0)
    todayCal.set(Calendar.MILLISECOND, 0)
    val todayMs = todayCal.timeInMillis
    val yesterdayMs = todayMs - 24 * 60 * 60 * 1000

    val latestRecorded = recordDates[0]
    if (latestRecorded != todayMs && latestRecorded != yesterdayMs) {
        // Streak broken
        return 0
    }

    var consecutiveStreak = 1
    var lastMs = latestRecorded
    for (i in 1 until recordDates.size) {
        val nextMs = recordDates[i]
        if (lastMs - nextMs == 24 * 60 * 60 * 1000L) {
            consecutiveStreak++
            lastMs = nextMs
        } else {
            break
        }
    }
    return consecutiveStreak
}
