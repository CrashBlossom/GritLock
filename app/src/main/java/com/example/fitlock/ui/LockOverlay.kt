package com.example.fitlock.ui

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.fitlock.data.ExerciseRequirement
import com.example.fitlock.exercise.PoseAnalyzer
import com.example.fitlock.exercise.TrackingMode
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import java.util.concurrent.Executors

@Composable
fun LockOverlayScreen(
    targetApp: String,
    repCount: Int,
    exerciseRequirements: List<ExerciseRequirement>,
    currentExerciseIndex: Int,
    trackingMode: TrackingMode,
    bankedReps: Map<String, Int>,
    onModeChange: (TrackingMode) -> Unit,
    onPoseDetected: (Pose, Int, Int) -> Unit,
    onEmergencyBypass: () -> Unit,
    onNextExercise: () -> Unit,
    onStopExercise: () -> Unit,
    onUseBankedReps: (String, Int) -> Unit,
    onLaunchRequiredApp: () -> Unit = {}
) {
    var currentPose by remember { mutableStateOf<Pose?>(null) }
    var imageWidth by remember { mutableIntStateOf(0) }
    var imageHeight by remember { mutableIntStateOf(0) }
    var showBypassDialog by remember { mutableStateOf(false) }

    val currentReq = exerciseRequirements.getOrNull(currentExerciseIndex) ?: return
    val currentBanked = bankedReps[currentReq.type] ?: 0
    val isComplete = repCount >= currentReq.count

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "LOCKED: $targetApp",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Progress Indicator
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                exerciseRequirements.forEachIndexed { index, req ->
                    Box(
                        modifier = Modifier
                            .size(width = 40.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == currentExerciseIndex) MaterialTheme.colorScheme.primary
                                else if (index < currentExerciseIndex) Color.Green
                                else Color.Gray.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Requirement ${currentExerciseIndex + 1}/${exerciseRequirements.size}: ${currentReq.type}",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )

            if (currentReq.type != "APP_USAGE") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text("Pocket", color = Color.Gray, fontSize = 12.sp)
                    Switch(
                        checked = trackingMode == TrackingMode.CAMERA,
                        onCheckedChange = { onModeChange(if (it) TrackingMode.CAMERA else TrackingMode.POCKET) },
                        modifier = Modifier.scale(0.8f)
                    )
                    Text("Camera", color = Color.Gray, fontSize = 12.sp)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (currentReq.type == "APP_USAGE") {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Icon(Icons.Default.Launch, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "You must spend ${currentReq.count} seconds in the following app:",
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            currentReq.targetPackageName ?: "Unknown App",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onLaunchRequiredApp,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Launch App Now")
                        }
                    }
                } else if (trackingMode == TrackingMode.CAMERA) {
                    CameraPreview(
                        modifier = Modifier.fillMaxSize(),
                        onPoseDetected = { pose, width, height ->
                            currentPose = pose
                            imageWidth = width
                            imageHeight = height
                            onPoseDetected(pose, width, height)
                        }
                    )
                    PoseOverlay(
                        pose = currentPose, 
                        imageWidth = imageWidth,
                        imageHeight = imageHeight,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
                        Text("Place phone in pocket", color = Color.White.copy(alpha = 0.5f))
                    }
                }
                
                if (currentReq.type != "APP_USAGE") {
                    // Rep counter overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(24.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "$repCount / ${currentReq.count}",
                            style = MaterialTheme.typography.displayMedium,
                            color = if (isComplete) Color.Green else Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bank usage interaction
            if (currentBanked > 0 && !isComplete && currentReq.type != "APP_USAGE") {
                Button(
                    onClick = { onUseBankedReps(currentReq.type, 1) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Savings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Use 1 Banked Rep ($currentBanked left)", fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Action Buttons
            if (currentReq.type != "APP_USAGE") {
                AnimatedVisibility(visible = isComplete, enter = fadeIn() + expandVertically()) {
                    Button(
                        onClick = onNextExercise,
                        modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Green, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        val isLast = currentExerciseIndex == exerciseRequirements.size - 1
                        Text(if (isLast) "Finish & Unlock" else "Next Exercise", fontWeight = FontWeight.Bold)
                        Icon(if (isLast) Icons.Default.Check else Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStopExercise,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Stop", color = MaterialTheme.colorScheme.onErrorContainer)
                }
                
                Button(
                    onClick = { showBypassDialog = true },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Bypass", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    if (showBypassDialog) {
        EmergencyBypassDialog(
            onDismiss = { showBypassDialog = false },
            onBypassConfirmed = {
                showBypassDialog = false
                onEmergencyBypass()
            }
        )
    }
}

@Composable
fun EmergencyBypassDialog(onDismiss: () -> Unit, onBypassConfirmed: () -> Unit) {
    val targetPhrase = "I am bypassing my fitness goals because I lack discipline today. I acknowledge that skipping this session makes me weaker, yet I desperately need to scroll..."
    var inputText by remember { mutableStateOf("") }
    
    val progress = if (targetPhrase.isEmpty()) 0f else (inputText.length.toFloat() / targetPhrase.length.toFloat()).coerceIn(0f, 1f)
    val isCorrect = inputText == targetPhrase

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC000000))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C21)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "🚨 EMERGENCY BYPASS 🚨",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "You chose not to sweat. To unlock this app, you must type the text below PERFECTLY.\nNo copy-pasting.",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF2D2D35), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            "\"$targetPhrase\"",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Type here:", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                    
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { 
                            if (it.length <= targetPhrase.length) {
                                inputText = it 
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF121216),
                            unfocusedContainerColor = Color(0xFF121216),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        placeholder = { Text("Start typing...", color = Color.DarkGray) }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Progress: ${(progress * 100).toInt()}%", color = Color.Gray, fontSize = 12.sp)
                        Text("Errors allowed: 0", color = Color.Gray, fontSize = 12.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = onBypassConfirmed,
                        enabled = isCorrect,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black,
                            disabledContainerColor = Color(0xFF2D2D35),
                            disabledContentColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("[ UNLOCK ]", fontWeight = FontWeight.Bold)
                    }
                    
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun PoseOverlay(pose: Pose?, imageWidth: Int, imageHeight: Int, modifier: Modifier) {
    Canvas(modifier = modifier) {
        if (pose == null || imageWidth == 0 || imageHeight == 0) return@Canvas

        val canvasWidth = size.width
        val canvasHeight = size.height
        
        val scale: Float
        val offsetX: Float
        val offsetY: Float

        val canvasRatio = canvasWidth / canvasHeight
        val imageRatio = imageWidth.toFloat() / imageHeight.toFloat()

        if (canvasRatio > imageRatio) {
            scale = canvasWidth / imageWidth.toFloat()
            offsetX = 0f
            offsetY = (canvasHeight - imageHeight * scale) / 2f
        } else {
            scale = canvasHeight / imageHeight.toFloat()
            offsetY = 0f
            offsetX = (canvasWidth - imageWidth * scale) / 2f
        }

        fun mapPoint(x: Float, y: Float): Offset {
            val mirroredX = imageWidth - x
            return Offset(mirroredX * scale + offsetX, y * scale + offsetY)
        }

        pose.allPoseLandmarks.forEach { landmark ->
            val pos = mapPoint(landmark.position.x, landmark.position.y)
            drawCircle(
                color = Color.Green,
                radius = 8f,
                center = pos
            )
        }
        
        val connections = listOf(
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.RIGHT_SHOULDER,
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_ELBOW,
            PoseLandmark.LEFT_ELBOW to PoseLandmark.LEFT_WRIST,
            PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_ELBOW,
            PoseLandmark.RIGHT_ELBOW to PoseLandmark.RIGHT_WRIST,
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_HIP,
            PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_HIP,
            PoseLandmark.LEFT_HIP to PoseLandmark.RIGHT_HIP,
            PoseLandmark.LEFT_HIP to PoseLandmark.LEFT_KNEE,
            PoseLandmark.LEFT_KNEE to PoseLandmark.LEFT_ANKLE,
            PoseLandmark.RIGHT_HIP to PoseLandmark.RIGHT_KNEE,
            PoseLandmark.RIGHT_KNEE to PoseLandmark.RIGHT_ANKLE
        )

        connections.forEach { (start, end) ->
            val startLandmark = pose.getPoseLandmark(start)
            val endLandmark = pose.getPoseLandmark(end)
            if (startLandmark != null && endLandmark != null) {
                val startPos = mapPoint(startLandmark.position.x, startLandmark.position.y)
                val endPos = mapPoint(endLandmark.position.x, endLandmark.position.y)
                drawLine(
                    color = Color.Green,
                    start = startPos,
                    end = endPos,
                    strokeWidth = 4f
                )
            }
        }
    }
}

@Composable
fun CameraPreview(
    modifier: Modifier,
    onPoseDetected: (Pose, Int, Int) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, PoseAnalyzer(onPoseDetected))
                    }

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (exc: Exception) {
                    Log.e("CameraPreview", "Use case binding failed", exc)
                }

            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier
    )
    
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
}
