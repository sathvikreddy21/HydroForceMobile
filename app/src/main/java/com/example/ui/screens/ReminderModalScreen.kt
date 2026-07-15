package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.WaterViewModel
import kotlinx.coroutines.delay

@Composable
fun ReminderModalScreen(
    viewModel: WaterViewModel,
    modifier: Modifier = Modifier
) {
    // Lock Android Back Button interaction!
    BackHandler(enabled = true) {
        // Block! User CANNOT dismiss this without completing verification or override
    }

    val activeTime by viewModel.reminderActiveTime.collectAsState()
    val failedAttempts by viewModel.cameraFailedAttempts.collectAsState()
    val analysisMessage by viewModel.analysisMessage.collectAsState()

    var secondsElapsed by remember { mutableStateOf(0L) }
    var showCameraView by remember { mutableStateOf(false) }
    var overrideReasonInput by remember { mutableStateOf("") }

    // Tick timer loop showing how long reminder is active
    LaunchedEffect(activeTime) {
        if (activeTime > 0) {
            while (true) {
                secondsElapsed = (System.currentTimeMillis() - activeTime) / 1000
                delay(1000)
            }
        }
    }

    val formattedTime = remember(secondsElapsed) {
        val hrs = secondsElapsed / 3600
        val mins = (secondsElapsed % 3600) / 60
        val secs = secondsElapsed % 60
        if (hrs > 0) {
            String.format("%02d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format("%02d:%02d", mins, secs)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020617)) // Solid deep space lock background
    ) {
        // Core crimson alert ambient glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x1BF43F5E), Color.Transparent),
                        radius = 1200f
                    )
                )
        )
        if (showCameraView) {
            // Overlay camera preview modal in front of lock screen
            CameraScreen(
                viewModel = viewModel,
                onSuccessDismiss = {
                    showCameraView = false
                }
            )
        } else {
            // Main Alerts Panel
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Warning Indicator
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 40.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(Color(0x22F43F5E), CircleShape)
                            .border(2.dp, Color(0xFFF43F5E), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalDrink,
                            contentDescription = "Alert",
                            tint = Color(0xFFF43F5E),
                            modifier = Modifier.size(54.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "HYDROFORCE ALERT LOCK",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFF43F5E),
                            letterSpacing = 2.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Hydration alert has been active for:",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = formattedTime,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // Center explanation of proof & failure counters
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp),
                    border = borderStrokeForFailedCount(failedAttempts)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "AI Gulp Verification",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = analysisMessage ?: "We need to verify that you are drinking water using our advanced Hydro Vision systems.",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )

                        if (failedAttempts > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Verification Failed Attempts: $failedAttempts",
                                color = Color(0xFFF43F5E),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                // Bottom actions (either Camera verify OR override options)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (failedAttempts >= 3) {
                        // Display Emergency Manual Override Inputs!
                        Text(
                            text = "🚨 Sarcastic Warning Locked (3+ Fails). Emergency Manual Override authorized with reason logged:",
                            color = Color(0xFFFBBF24),
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center
                        )

                        OutlinedTextField(
                            value = overrideReasonInput,
                            onValueChange = { overrideReasonInput = it },
                            placeholder = { Text("Reason (e.g. Left bottle in car)", color = Color.White.copy(alpha = 0.4f)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFBBF24),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Button(
                            onClick = {
                                if (overrideReasonInput.isNotBlank()) {
                                    viewModel.overrideManualReminder(overrideReasonInput)
                                }
                            },
                            enabled = overrideReasonInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBBF24)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text("EXECUTE MANUAL OVERRIDE", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Camera Verification active trigger
                    Button(
                        onClick = { showCameraView = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Camera verify")
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Open Camera & Take Photo",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    if (failedAttempts < 3) {
                        Text(
                            text = "You cannot leave this screen without taking a verified drink.",
                            color = Color.White.copy(alpha = 0.3f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun borderStrokeForFailedCount(attempts: Int): androidx.compose.foundation.BorderStroke? {
    if (attempts == 1) {
        return androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFBBF24))
    }
    if (attempts >= 2) {
        return androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF43F5E))
    }
    return null
}
