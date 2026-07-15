package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

@Composable
fun ProgressRing(
    progress: Float, // 0.0 to 1.0+
    currentIntakeMl: Int,
    dailyGoalMl: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    
    // Wave/Liquid pulse parameter
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * java.lang.Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )

    // Pulse size for backing glow
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    // Bubble animators
    val bubbleAnims = (0..5).map { index ->
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 2000 + (index * 400),
                    delayMillis = index * 300,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "bubble_$index"
        )
    }

    val primaryColor = Color(0xFE0EA5E9)   // Electric Blue
    val accentColor = Color(0xFE06B6D4)    // Cyan Accent
    val containerBg = Color(0xFF1E293B)   // Dark Slate card background

    Box(
        modifier = modifier
            .size(240.dp)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.width / 2f) * 0.85f

            // 1. Draw backing shadow glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.15f * glowScale), Color.Transparent),
                    center = center,
                    radius = radius * 1.3f
                ),
                center = center,
                radius = radius * 1.3f
            )

            // 2. Clear background track
            drawCircle(
                color = containerBg.copy(alpha = 0.4f),
                center = center,
                radius = radius,
                style = Stroke(width = 16.dp.toPx())
            )

            // 3. Draw gradient animated track
            val sweepAngle = (progress.coerceIn(0f, 1f) * 360f)
            drawArc(
                brush = Brush.linearGradient(
                    colors = listOf(primaryColor, accentColor),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                ),
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(
                    width = 16.dp.toPx(),
                    cap = StrokeCap.Round
                ),
                size = Size(radius * 2, radius * 2),
                topLeft = Offset(center.x - radius, center.y - radius)
            )

            // 4. Draw Floating Bubbles
            bubbleAnims.forEachIndexed { i, anim ->
                val bubbleProgress = anim.value // 1.0 goes down to 0.0 (floating up)
                val bubbleId = i + 1
                val startX = center.x - radius + (radius * 2f * (bubbleId / 7f))
                
                // Add winding sine wave lateral draft
                val xOffset = sin((waveOffset * 2f) + (bubbleId * 1.5f)) * 15f
                val currentY = center.y + radius - (radius * 2f * (1f - bubbleProgress))

                // Restrict bubbles strictly within the circular boundary
                val distanceSq = (startX + xOffset - center.x) * (startX + xOffset - center.x) + (currentY - center.y) * (currentY - center.y)
                if (distanceSq < radius * radius) {
                    drawCircle(
                        color = accentColor.copy(alpha = 0.4f * bubbleProgress),
                        radius = (4 + (bubbleId % 3) * 2).dp.toPx(),
                        center = Offset(startX + xOffset, currentY)
                    )
                }
            }
        }

        // 5. Central Info Column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val percentageText = "${(progress * 100).toInt()}%"
            Text(
                text = percentageText,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 44.sp,
                    color = Color.White
                )
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "$currentIntakeMl / $dailyGoalMl ml",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.8f)
                )
            )
            
            Text(
                text = "DAILY TARGET",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    letterSpacing = 1.5.sp
                )
            )
        }
    }
}
