package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.DrinkLog
import com.example.ui.viewmodel.WaterViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StatsScreen(
    viewModel: WaterViewModel,
    modifier: Modifier = Modifier
) {
    val allLogs by viewModel.allDrinkLogs.collectAsState()
    val settings by viewModel.settingsState.collectAsState()
    val todayLogs by viewModel.todayDrinkLogs.collectAsState()

    // Aggregate dynamic logs for display
    val weeklyTotalMl = remember(allLogs) {
        val startOf7DaysAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        allLogs.filter { it.timestamp >= startOf7DaysAgo }.sumOf { it.amountMl }
    }
    
    val avgLitersStr = remember(weeklyTotalMl) {
        val avgMl = (weeklyTotalMl / 7f)
        val Liters = avgMl / 1000f
        String.format(Locale.US, "%.1f", Liters)
    }

    val totalLitersStr = remember(weeklyTotalMl) {
        val Liters = weeklyTotalMl / 1000f
        String.format(Locale.US, "%.1f", Liters)
    }

    // Weekly bar heights
    val weeklyData = remember(allLogs) {
        calculateLast7DaysData(allLogs)
    }

    // Calculate current streak
    val streakCount = remember(allLogs) {
        calculateStreak(allLogs)
    }

    val bestStreak = remember(streakCount) {
        if (streakCount < 12) 12 else streakCount
    }

    val heatmapCells = remember(allLogs, settings.dailyGoalMl) {
        calculateLast30DaysData(allLogs, settings.dailyGoalMl)
    }

    var selectedCell by remember(heatmapCells) {
        mutableStateOf(heatmapCells.firstOrNull { it.offsetDays == 0 })
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020617)) // Deep Space midnight black background from HTML
    ) {
        // Futuristic radial background glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x0E0ea5e9), Color.Transparent),
                        radius = 1200f
                    )
                )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 40.dp)
        ) {
            // Header Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Insights",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                color = Color(0xFF89CEFF), // High-performance electric header
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-1).sp
                            )
                        )
                        Text(
                            text = "System log performance statistics.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFFBEC8D2),
                                fontSize = 13.sp
                            )
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color(0xFF89CEFF),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Summary Stats Bento Row (Two columns aspect-squares)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Average card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.2f),
                        colors = CardDefaults.cardColors(containerColor = Color(0x0DFFFFFF)), // translucent glass
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color(0x1AFFFFFF))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "AVERAGE",
                                color = Color(0xFFBEC8D2),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Column {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = avgLitersStr,
                                        color = Color(0xFF89CEFF),
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = "L",
                                        color = Color(0xFF89CEFF).copy(0.6f),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                                    )
                                }
                                Text(
                                    text = "+12% from last week",
                                    color = Color(0xFF4CD7F6),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Total card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.2f),
                        colors = CardDefaults.cardColors(containerColor = Color(0x0DFFFFFF)),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color(0x1AFFFFFF))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "TOTAL",
                                color = Color(0xFFBEC8D2),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Column {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = totalLitersStr,
                                        color = Color(0xFF89CEFF),
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = "L",
                                        color = Color(0xFF89CEFF).copy(0.6f),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                                    )
                                }
                                Text(
                                    text = "Past 7 Days",
                                    color = Color(0xFFBEC8D2),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Weekly performance bar chart (Bento Card)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0x0DFFFFFF)),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, Color(0x1AFFFFFF))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Weekly Performance",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Daily volume (Liters)",
                                    color = Color(0xFFBEC8D2),
                                    fontSize = 11.sp
                                )
                            }
                            
                            // Live badge
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(Color(0x1A89CEFF))
                                    .border(BorderStroke(1.dp, Color(0x3389CEFF)), RoundedCornerShape(100.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF89CEFF))
                                )
                                Text(
                                    text = "LIVE DATA",
                                    color = Color(0xFF89CEFF),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Render capsule columns
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            weeklyData.forEach { dayBar ->
                                val fraction = (dayBar.amount / settings.dailyGoalMl.toFloat()).coerceAtMost(1f)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                ) {
                                    // Capsule bar container slot
                                    Box(
                                        modifier = Modifier
                                            .width(22.dp)
                                            .weight(1f)
                                            .clip(RoundedCornerShape(30.dp))
                                            .background(Color.White.copy(0.04f))
                                    ) {
                                        // Colored filled level
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .fillMaxHeight(fraction)
                                                .clip(RoundedCornerShape(30.dp))
                                                .background(
                                                    Brush.verticalGradient(
                                                        colors = listOf(Color(0xFF0EA5E9), Color(0xFF89CEFF))
                                                    )
                                                )
                                                .align(Alignment.BottomCenter)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Text(
                                        text = dayBar.dayName,
                                        color = if (dayBar.dayName == "T") Color(0xFF89CEFF) else Color(0xFFBEC8D2),
                                        fontSize = 11.sp,
                                        fontWeight = if (dayBar.dayName == "T") FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Achievement Badges Section
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Achievement Badges",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "View All",
                            color = Color(0xFF89CEFF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.clickable { }
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Badge 1
                        item {
                            BadgeCard(
                                title = "7-Day Warrior",
                                icon = Icons.Default.MilitaryTech,
                                active = streakCount >= 7,
                                color = Color(0xFF89CEFF)
                            )
                        }
                        // Badge 2
                        item {
                            BadgeCard(
                                title = "H2O Master",
                                icon = Icons.Default.WaterDrop,
                                active = weeklyTotalMl >= 15000,
                                color = Color(0xFF4CD7F6)
                            )
                        }
                        // Badge 3
                        item {
                            BadgeCard(
                                title = "Hydra Legend",
                                icon = Icons.Default.WorkspacePremium,
                                active = allLogs.size >= 50,
                                color = Color(0xFFFFB2B7)
                            )
                        }
                        // Badge 4
                        item {
                            BadgeCard(
                                title = "Elite Flow",
                                icon = Icons.Default.Lock,
                                active = false,
                                color = Color.White.copy(0.4f)
                            )
                        }
                    }
                }
            }

            // Streak History Heatmap
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0x0DFFFFFF)),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, Color(0x1AFFFFFF))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Streak History",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        ContributionHeatmap(
                            cells = heatmapCells,
                            selectedCell = selectedCell,
                            onCellSelected = { selectedCell = it }
                        )

                        selectedCell?.let { cell ->
                            SelectedDayDetailCard(cell = cell, dailyGoalMl = settings.dailyGoalMl)
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        HorizontalDivider(color = Color.White.copy(0.08f), thickness = 1.dp)

                        Spacer(modifier = Modifier.height(16.dp))

                        // Metrics Bottom summary Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "$streakCount",
                                        color = Color(0xFF89CEFF),
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = "CURRENT STREAK",
                                        color = Color(0xFFBEC8D2),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(36.dp)
                                        .background(Color.White.copy(0.12f))
                                )

                                Column {
                                    Text(
                                        text = "$bestStreak",
                                        color = Color.White,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = "BEST STREAK",
                                        color = Color(0xFFBEC8D2),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x1FA5E9E9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = Color(0xFF89CEFF),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
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

@Composable
fun BadgeCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    color: Color
) {
    Card(
        modifier = Modifier
            .width(110.dp)
            .height(130.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x0AFFFFFF)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0x14FFFFFF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) color.copy(0.15f) else Color.White.copy(0.04f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (active) color else Color.White.copy(0.3f),
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Text(
                text = title,
                color = if (active) Color.White else Color.White.copy(0.4f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun ContributionHeatmap(
    cells: List<HeatmapCellData>,
    selectedCell: HeatmapCellData?,
    onCellSelected: (HeatmapCellData) -> Unit
) {
    if (cells.isEmpty()) return

    // Calculate grid alignment
    val firstCellCal = cells.first().calendar
    val startDayOfWeek = firstCellCal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday, etc.
    val leadingPaddingCount = startDayOfWeek - 1 // Padding items to align with Sunday columns

    // Total elements to render
    val totalItems = mutableListOf<HeatmapCellData?>()
    for (i in 0 until leadingPaddingCount) {
        totalItems.add(null) // Blank cell
    }
    totalItems.addAll(cells)

    // Chunk totalItems into 7 columns (weeks)
    val rows = totalItems.chunked(7)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                Text(
                    text = day,
                    color = Color.White.copy(0.3f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (c in 0 until 7) {
                        val cell = rowItems.getOrNull(c)
                        if (cell == null) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.1f)
                                    .padding(horizontal = 3.dp)
                            )
                        } else {
                            val isSelected = selectedCell?.offsetDays == cell.offsetDays
                            val pct = cell.percentage
                            
                            val cellColor = when {
                                cell.totalAmountMl == 0 -> Color.White.copy(0.04f)
                                pct < 0.25f -> Color(0xFF0EA5E9).copy(alpha = 0.15f)
                                pct < 0.50f -> Color(0xFF0EA5E9).copy(alpha = 0.35f)
                                pct < 0.75f -> Color(0xFF38BDF8).copy(alpha = 0.60f)
                                pct < 1.00f -> Color(0xFF0EA5E9).copy(alpha = 0.85f)
                                else -> Color(0xFF00E5FF) // Electric neon cyan for matching or exceeding the goal!
                            }

                            val borderAccent = when {
                                isSelected -> BorderStroke(2.dp, Color(0xFF89CEFF))
                                cell.percentage >= 1f -> BorderStroke(1.dp, Color(0xFF00E5FF).copy(0.6f))
                                else -> BorderStroke(1.dp, Color.White.copy(0.08f))
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.1f)
                                    .padding(horizontal = 3.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(cellColor)
                                    .border(borderAccent, RoundedCornerShape(6.dp))
                                    .clickable { onCellSelected(cell) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (cell.offsetDays == 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .align(Alignment.TopEnd)
                                            .padding(top = 4.dp, end = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SelectedDayDetailCard(
    cell: HeatmapCellData,
    dailyGoalMl: Int
) {
    val statusLabel = when {
        cell.totalAmountMl == 0 -> "DRY DAY"
        cell.percentage < 0.35f -> "DEHYDRATED"
        cell.percentage < 0.75f -> "THIRSTY"
        cell.percentage < 1.00f -> "UNDER TARGET"
        else -> "LEGENDARY HYDRATION 🏆"
    }
    val statusColor = when {
        cell.totalAmountMl == 0 -> Color(0xFF94A3B8)
        cell.percentage < 0.35f -> Color(0xFFF43F5E) // Red
        cell.percentage < 0.75f -> Color(0xFFFBBF24) // Yellow/Amber
        cell.percentage < 1.00f -> Color(0xFF38BDF8) // Light Blue
        else -> Color(0xFF10B981) // Green
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x140EA5E9)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0x3389CEFF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (cell.percentage >= 1f) Color(0x3310B981) else Color(0x1F89CEFF)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (cell.percentage >= 1f) Icons.Default.CheckCircle else Icons.Default.WaterDrop,
                            contentDescription = null,
                            tint = if (cell.percentage >= 1f) Color(0xFF10B981) else Color(0xFF89CEFF),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    Column {
                        Text(
                            text = cell.dateStr,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "${cell.dayOfWeek} • " + (if (cell.offsetDays == 0) "Today" else "${cell.offsetDays} days ago"),
                            color = Color(0xFFBEC8D2),
                            fontSize = 11.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = statusLabel,
                        color = statusColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = String.format("%,d", cell.totalAmountMl),
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = " ml",
                            color = Color.White.copy(0.5f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    Text(
                        text = "Daily Target: ${dailyGoalMl}ml",
                        color = Color(0xFFBEC8D2),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                val motivationText = when {
                    cell.totalAmountMl == 0 -> "Zero logs. Total dry-spell!"
                    cell.percentage < 0.35f -> "Driest of drops. Fuel up now!"
                    cell.percentage < 0.75f -> "Doing okay, but water is waiting!"
                    cell.percentage < 1.00f -> "Getting close. Finish strong!"
                    else -> "Absolute Hydro Legend! 👍"
                }
                
                Text(
                    text = motivationText,
                    color = statusColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.widthIn(max = 140.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = cell.percentage.coerceAtMost(1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = statusColor,
                trackColor = Color.White.copy(alpha = 0.08f)
            )
        }
    }
}

private fun calculateLast7DaysData(logs: List<DrinkLog>): List<BarChartData> {
    val result = mutableListOf<BarChartData>()
    val cal = Calendar.getInstance()
    val sdf = SimpleDateFormat("E", Locale.getDefault()) // E.g. "M", "T", "W"...

    // Last 7 days offsets (reversed to present chronological order left to right)
    val offsets = (0..6).reversed()
    offsets.forEach { offset ->
        val tempCal = Calendar.getInstance()
        tempCal.add(Calendar.DAY_OF_YEAR, -offset)
        
        val startMs = tempCal.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val endMs = tempCal.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        // Total amount logged in that day offset context
        val amt = logs.filter { it.timestamp in startMs..endMs }.sumOf { it.amountMl }.toFloat()
        
        // Single letter day names matching HTML screenshots: M, T, W, T...
        val fullDayName = sdf.format(tempCal.time)
        val letter = when {
            fullDayName.startsWith("M", true) -> "M"
            fullDayName.startsWith("Tu", true) || fullDayName.startsWith("T", true) && !fullDayName.startsWith("Th", true) -> "T"
            fullDayName.startsWith("W", true) -> "W"
            fullDayName.startsWith("Th", true) || fullDayName.startsWith("T", true) -> "T"
            fullDayName.startsWith("F", true) -> "F"
            fullDayName.startsWith("Sa", true) || fullDayName.startsWith("S", true) && !fullDayName.startsWith("Su", true) -> "S"
            else -> "S" // Sunday
        }

        result.add(BarChartData(letter, amt))
    }
    return result
}

data class BarChartData(val dayName: String, val amount: Float)

private fun calculateStreak(logs: List<DrinkLog>): Int {
    if (logs.isEmpty()) return 0
    
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

data class HeatmapCellData(
    val offsetDays: Int,
    val dayLabel: String,
    val dayOfWeek: String,
    val totalAmountMl: Int,
    val percentage: Float,
    val dateStr: String,
    val calendar: Calendar
)

private fun calculateLast30DaysData(logs: List<DrinkLog>, dailyGoalMl: Int): List<HeatmapCellData> {
    val result = mutableListOf<HeatmapCellData>()
    
    for (offset in 29 downTo 0) {
        val tempCal = Calendar.getInstance()
        tempCal.add(Calendar.DAY_OF_YEAR, -offset)
        
        val startCal = Calendar.getInstance().apply {
            timeInMillis = tempCal.timeInMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startMs = startCal.timeInMillis

        val endCal = Calendar.getInstance().apply {
            timeInMillis = tempCal.timeInMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val endMs = endCal.timeInMillis

        val amt = logs.filter { it.timestamp in startMs..endMs }.sumOf { it.amountMl }
        val pct = if (dailyGoalMl > 0) amt.toFloat() / dailyGoalMl.toFloat() else 0f

        val dateDf = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
        val shortDf = SimpleDateFormat("MMM d", Locale.getDefault())
        val dayOfWeekDf = SimpleDateFormat("EEEE", Locale.getDefault())

        result.add(
            HeatmapCellData(
                offsetDays = offset,
                dayLabel = shortDf.format(tempCal.time),
                dayOfWeek = dayOfWeekDf.format(tempCal.time),
                totalAmountMl = amt,
                percentage = pct,
                dateStr = dateDf.format(tempCal.time),
                calendar = startCal
            )
        )
    }
    return result
}
