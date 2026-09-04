package com.example.fitlock.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.fitlock.data.ExerciseCalibration
import com.example.fitlock.exercise.ExerciseType
import com.example.fitlock.exercise.PocketExerciseGuide
import com.example.fitlock.exercise.TrackingMode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationScreen(
    exerciseType: ExerciseType,
    variantName: String? = null,
    mode: TrackingMode,
    onCalibrationComplete: (ExerciseCalibration) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val displayName = variantName ?: exerciseType.name
    
    var calibrationStep by remember { mutableStateOf(0) } // 0: Guide, 1: Top, 2: Bottom, 3: Success
    var countdown by remember { mutableStateOf(5) }
    var topValue by remember { mutableStateOf<Double?>(null) }
    var bottomValue by remember { mutableStateOf<Double?>(null) }
    
    // Camera-specific states
    var currentPose by remember { mutableStateOf<Pose?>(null) }
    val detector = remember {
        val options = PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
        PoseDetection.getClient(options)
    }

    // Sensor-specific states
    var currentAccelY by remember { mutableStateOf(0f) }
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val sensorListener = remember {
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_LINEAR_ACCELERATION) {
                    currentAccelY = event.values[1]
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }

    if (mode == TrackingMode.POCKET) {
        DisposableEffect(Unit) {
            val accel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
            sensorManager.registerListener(sensorListener, accel, SensorManager.SENSOR_DELAY_UI)
            onDispose { sensorManager.unregisterListener(sensorListener) }
        }
    }

    // Timer Logic for automated calibration
    LaunchedEffect(calibrationStep) {
        if (calibrationStep == 1 || calibrationStep == 2) {
            countdown = 5
            while (countdown > 0) {
                delay(1000)
                countdown--
            }
            // Capture value at end of countdown
            val value = if (mode == TrackingMode.CAMERA) {
                getCurrentValueCamera(exerciseType, currentPose) ?: 0.0
            } else {
                currentAccelY.toDouble()
            }
            
            if (calibrationStep == 1) {
                topValue = value
                calibrationStep = 2
            } else {
                bottomValue = value
                calibrationStep = 3
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calibrate ${exerciseType.name}", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121216))
            )
        },
        containerColor = Color(0xFF121216)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(Color.Black, RoundedCornerShape(16.dp))
            ) {
                if (mode == TrackingMode.CAMERA) {
                    CameraPreview(detector) { currentPose = it }
                } else {
                    PocketModeIndicator(currentAccelY)
                }
                
                // Calibration Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(displayName, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))

                        when (calibrationStep) {
                            0 -> {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C21)),
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFD0BCFF), modifier = Modifier.size(48.dp))
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            if (mode == TrackingMode.POCKET) PocketExerciseGuide.getPlacementInstruction(exerciseType)
                                            else "Place your phone where your full body is visible.",
                                            color = Color.White,
                                            textAlign = TextAlign.Center,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Button(onClick = { calibrationStep = 1 }) {
                                            Text("Ready")
                                        }
                                    }
                                }
                            }
                            1, 2 -> {
                                Text(
                                    if (calibrationStep == 1) "Move to TOP position" else "Move to BOTTOM position",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp).clip(CircleShape).background(Color(0xFFD0BCFF))) {
                                    Text(countdown.toString(), color = Color.Black, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            3 -> {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C21)),
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Calibration Success!", color = Color.Green, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("Top: ${String.format(Locale.US, "%.1f", topValue ?: 0.0)}", color = Color.White)
                                        Text("Bottom: ${String.format(Locale.US, "%.1f", bottomValue ?: 0.0)}", color = Color.White)
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Button(
                                            onClick = {
                                                if (topValue != null && bottomValue != null) {
                                                    val key = if (variantName != null) "${variantName}_${mode.name}" else "${exerciseType.name}_${mode.name}"
                                                    onCalibrationComplete(ExerciseCalibration(key, topValue!!, bottomValue!!, 0.0, variantName))
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                        ) {
                                            Text("Save & Exit")
                                        }
                                    }
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
fun CameraPreview(detector: com.google.mlkit.vision.pose.PoseDetector, onPoseDetected: (Pose) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
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
                        it.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                            processImageProxy(imageProxy, detector) { pose ->
                                onPoseDetected(pose)
                            }
                        }
                    }
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {}
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun PocketModeIndicator(accelY: Float) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Y-Acceleration", color = Color.Gray, fontSize = 14.sp)
            Text("${String.format(Locale.US, "%.2f", accelY)} m/s²", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun processImageProxy(imageProxy: ImageProxy, detector: com.google.mlkit.vision.pose.PoseDetector, onPoseDetected: (Pose) -> Unit) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        detector.process(image)
            .addOnSuccessListener { pose -> onPoseDetected(pose) }
            .addOnCompleteListener { imageProxy.close() }
    } else {
        imageProxy.close()
    }
}

private fun getCurrentValueCamera(type: ExerciseType, pose: Pose?): Double? {
    if (pose == null) return null
    return when (type) {
        ExerciseType.PUSHUP, ExerciseType.PULLUP, ExerciseType.DIP, ExerciseType.ROW -> {
            val shoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER) ?: return null
            val elbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW) ?: return null
            val wrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST) ?: return null
            calculateAngle(shoulder, elbow, wrist)
        }
        ExerciseType.SQUAT -> {
            val hip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP) ?: return null
            val knee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE) ?: return null
            val ankle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE) ?: return null
            calculateAngle(hip, knee, ankle)
        }
        ExerciseType.SITUP, ExerciseType.HINGE -> {
            val shoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER) ?: return null
            val hip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP) ?: return null
            val knee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE) ?: return null
            calculateAngle(shoulder, hip, knee)
        }
        ExerciseType.PLANK -> {
            val shoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER) ?: return null
            val hip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP) ?: return null
            val ankle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE) ?: return null
            calculateAngle(shoulder, hip, ankle)
        }
        else -> null
    }
}

private fun calculateAngle(first: PoseLandmark, mid: PoseLandmark, last: PoseLandmark): Double {
    var result = Math.toDegrees(
        atan2(last.position.y - mid.position.y, last.position.x - mid.position.x).toDouble() -
        atan2(first.position.y - mid.position.y, first.position.x - mid.position.x).toDouble()
    )
    result = abs(result)
    if (result > 180) result = 360.0 - result
    return result
}
