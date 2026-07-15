package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.service.VisionService
import com.example.ui.viewmodel.WaterViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    viewModel: WaterViewModel,
    onSuccessDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var showResultsDialog by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf("") }
    var resultSuccess by remember { mutableStateOf(false) }
    var capturedImageUri by remember { mutableStateOf<String?>(null) }

    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val analysisMessage by viewModel.analysisMessage.collectAsState()
    val failedAttempts by viewModel.cameraFailedAttempts.collectAsState()

    val executor = remember { Executors.newSingleThreadExecutor() }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // Slate 900
    ) {
        if (cameraPermissionState.status.isGranted) {
            // Camera Preview Core
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().apply {
                            setSurfaceProvider(previewView.surfaceProvider)
                        }

                        imageCapture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()

                        val cameraSelector = CameraSelector.Builder()
                            .requireLensFacing(lensFacing)
                            .build()

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCapture
                            )
                        } catch (exc: Exception) {
                            Log.e("CameraScreen", "Use case binding failed", exc)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize(),
                update = { previewView ->
                    // Rebind camera when lensFacing changes
                    val ctx = previewView.context
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().apply {
                            setSurfaceProvider(previewView.surfaceProvider)
                        }

                        imageCapture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()

                        val cameraSelector = CameraSelector.Builder()
                            .requireLensFacing(lensFacing)
                            .build()

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCapture
                            )
                        } catch (exc: Exception) {
                            Log.e("CameraScreen", "Lens change binding failed", exc)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                }
            )

            // Dynamic UI Frame overlays
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                // Focus guide target
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .border(2.dp, Color(0x330EA5E9), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .border(1.dp, Color(0x7706B6D4), CircleShape)
                    )
                }

                // Header message guide
                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 40.dp)
                        .fillMaxWidth(0.9f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xCC1E293B)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Position face and water bottle/cup in the box",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White,
                                fontSize = 13.sp
                            ),
                            maxLines = 2
                        )
                    }
                }

                // Controls Row at Bottom
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 36.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back/Cancel Button
                    IconButton(
                        onClick = { onSuccessDismiss() },
                        modifier = Modifier
                            .size(54.dp)
                            .background(Color(0xBB334155), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Camera",
                            tint = Color.White
                        )
                    }

                    // Capture Trigger Button
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFF0EA5E9), Color(0xFF06B6D4))
                                ),
                                shape = CircleShape
                            )
                            .clickable(enabled = !isAnalyzing) {
                                captureAndVerify(
                                    context = context,
                                    imageCapture = imageCapture,
                                    executor = executor,
                                    viewModel = viewModel
                                ) { success, message, uri ->
                                    resultSuccess = success
                                    resultText = message
                                    capturedImageUri = uri
                                    showResultsDialog = true
                                }
                            }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(4.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Capture",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Flip Camera Lens
                    IconButton(
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                                CameraSelector.LENS_FACING_BACK
                            } else {
                                CameraSelector.LENS_FACING_FRONT
                            }
                        },
                        modifier = Modifier
                            .size(54.dp)
                            .background(Color(0xBB334155), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlipCameraAndroid,
                            contentDescription = "Flip Camera",
                            tint = Color.White
                        )
                    }
                }
            }

            // Radar Scanning loading screen active during Analysis
            AnimatedVisibility(
                visible = isAnalyzing,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xEE0F172A)), // Deep overlay
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        RadarLoadingIndicator()
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = "HYDRO VISION AI SCANNING...",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color(0xFF0EA5E9),
                                letterSpacing = 2.sp
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = analysisMessage ?: "Processing photo...",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.8f)),
                            modifier = Modifier.padding(horizontal = 40.dp)
                        )
                    }
                }
            }

        } else {
            // Permission Denied View
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Camera Permission Required",
                        style = MaterialTheme.typography.titleLarge.copy(color = Color.White),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        text = "HydroForce uses your camera to identify drink proof and unlock the hydration reminder. Please enable camera access in your device settings.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.7f)),
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    Button(
                        onClick = { cameraPermissionState.launchPermissionRequest() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
                    ) {
                        Text("Grant Permission")
                    }
                }
            }
        }
    }

    // Capture Result Dialog (Sarcasm Shame popup for failure, celebration popup for success!)
    if (showResultsDialog) {
        AlertDialog(
            onDismissRequest = { 
                // Force user to click "Complete" button to dismiss so it never goes away without consent!
            },
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (resultSuccess) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        tint = if (resultSuccess) Color(0xFF10B981) else Color(0xFFF43F5E),
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = if (resultSuccess) "GULP APPROVED! 🎉" else "VERIFICATION FAILED ❌",
                        color = if (resultSuccess) Color(0xFF10B981) else Color(0xFFF43F5E),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = resultText,
                        color = Color.White.copy(alpha = 0.95f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (!resultSuccess) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Failed Attempts: $failedAttempts/3",
                            color = Color.White.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.labelMedium
                        )
                        if (failedAttempts >= 3) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Emergency override unlocked in previous screen.",
                                color = Color(0xFFFBBF24),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Your daily hydration log has been saved by our AI agent. Feel that refreshing flow!",
                            color = Color(0xFF89CEFF).copy(0.8f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResultsDialog = false
                        if (resultSuccess) {
                            viewModel.recordConfirmedDrink("Camera Proof (AI verified)", capturedImageUri, true)
                            onSuccessDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (resultSuccess) Color(0xFF10B981) else Color(0xFF475569)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (resultSuccess) "Claim Gulp & Close" else "Try Again",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            containerColor = Color(0xFF1E293B),
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun RadarLoadingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val radiusRatio by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radius"
    )
    val radarAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Canvas(modifier = Modifier.size(150.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerRadius = size.width / 2f

        // Ripple 1
        drawCircle(
            color = Color(0xFF06B6D4).copy(alpha = radarAlpha * 0.3f),
            radius = outerRadius * radiusRatio,
            center = center,
            style = Stroke(width = 4.dp.toPx())
        )

        // Stationary crosshairs
        drawLine(
            color = Color(0xFF0EA5E9).copy(alpha = 0.4f),
            start = Offset(center.x - outerRadius, center.y),
            end = Offset(center.x + outerRadius, center.y),
            strokeWidth = 2.dp.toPx()
        )
        drawLine(
            color = Color(0xFF0EA5E9).copy(alpha = 0.4f),
            start = Offset(center.x, center.y - outerRadius),
            end = Offset(center.x, center.y + outerRadius),
            strokeWidth = 2.dp.toPx()
        )

        // Central beacon dot
        drawCircle(
            color = Color(0xFF06B6D4),
            radius = 8.dp.toPx(),
            center = center
        )
    }
}

private fun captureAndVerify(
    context: Context,
    imageCapture: ImageCapture?,
    executor: Executor,
    viewModel: WaterViewModel,
    onResult: (Boolean, String, String?) -> Unit
) {
    if (imageCapture == null) {
        onResult(false, "Camera is not ready yet.", null)
        return
    }

    viewModel.setAnalyzing(true, "Capturing snapshot...")

    // 1. Create a temporary output file
    val tempFile = File(context.cacheDir, "hydroforce_capture_${System.currentTimeMillis()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()

    imageCapture.takePicture(outputOptions, executor, object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
            val fileUri = Uri.fromFile(tempFile)
            Log.d("CameraScreen", "Image captured safely: $fileUri")

            // 2. Perform background processing & scaling
            viewModel.setAnalyzing(true, "Analyzing image labels with Hydro Vision AI...")

            val coroutineScope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO)
            coroutineScope.launch {
                try {
                    val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                        ?: throw Exception("Failed to decode image file")

                    // Scale Bitmap to maximum limit of 800px to keep request payload small!
                    val rotatedBitmap = rotateImageIfRequired(bitmap, tempFile.absolutePath)
                    val scaledBitmap = scaleBitmap(rotatedBitmap, 800)

                    // Convert to base64
                    val outputStream = ByteArrayOutputStream()
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                    val bytes = outputStream.toByteArray()
                    val base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP)

                    // Execute REST verification call on helper
                    val result = VisionService.verifyDrinkingAction(base64Image)

                    withContext(Dispatchers.Main) {
                        viewModel.setAnalyzing(false)
                        if (result.success) {
                            onResult(true, result.funnyMessage, fileUri.toString())
                        } else {
                            // Increment failed count and apply shame mode messages
                            viewModel.incrementFailedResult(result.funnyMessage)
                            onResult(false, result.funnyMessage, null)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("CameraScreen", "Analysis failure", e)
                    withContext(Dispatchers.Main) {
                        viewModel.setAnalyzing(false)
                        // General fallback
                        onResult(true, "AI system encountered a connection hiccup, but because we love hydration, we gave you a free pass! Keep it up: ${e.localizedMessage}", fileUri.toString())
                    }
                } finally {
                    // Cleanup temp file safely
                    try {
                        tempFile.delete()
                    } catch (ex: Exception) {
                        Log.e("CameraScreen", "Temp file deletion failed", ex)
                    }
                }
            }
        }

        override fun onError(exception: ImageCaptureException) {
            Log.e("CameraScreen", "Capture execution error", exception)
            viewModel.setAnalyzing(false)
            onResult(false, "Capture failed: ${exception.localizedMessage}", null)
        }
    })
}

// Helpers for rotation and compression
private fun rotateImageIfRequired(img: Bitmap, path: String): Bitmap {
    val ei = android.media.ExifInterface(path)
    val orientation = ei.getAttributeInt(
        android.media.ExifInterface.TAG_ORIENTATION,
        android.media.ExifInterface.ORIENTATION_NORMAL
    )

    return when (orientation) {
        android.media.ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(img, 90f)
        android.media.ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(img, 180f)
        android.media.ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(img, 270f)
        else -> img
    }
}

private fun rotateImage(img: Bitmap, degree: Float): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(degree)
    val rotated = Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
    img.recycle()
    return rotated
}

private fun scaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val bounding = maxDimension.toFloat()

    val widthRatio = bounding / width
    val heightRatio = bounding / height
    val scale = if (widthRatio < heightRatio) widthRatio else heightRatio

    val matrix = Matrix()
    matrix.postScale(scale, scale)

    return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
}

// Simple context compat executor helper
private object ContextCompat {
    fun getMainExecutor(context: Context): Executor {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            context.mainExecutor
        } else {
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            Executor { command -> handler.post(command) }
        }
    }
}
