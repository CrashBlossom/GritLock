package com.example.fitlock.exercise

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.fitlock.data.ExerciseCalibration
import kotlin.math.abs

class SensorTracker(
    context: Context,
    private val manager: ExerciseTrackerManager,
    private val calibration: ExerciseCalibration? = null,
    private val exerciseType: ExerciseType? = null
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    private val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)

    private var isDown = false
    private var lastRepTime = 0L
    private var downStartTime = 0L
    private val REP_COOLDOWN_MS = 1000L
    
    // Default thresholds for Pocket Mode (Linear Acceleration in m/s^2)
    private var downThreshold = (calibration?.bottomValue?.toFloat()) ?: -2.5f 
    private var upThreshold = (calibration?.topValue?.toFloat()) ?: 3.5f
    
    // Smoothing buffer
    private val bufferSize = 5
    private val accelerationBuffer = FloatArray(bufferSize)
    private var bufferIndex = 0

    private var currentGravity = FloatArray(3)

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        gravitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type == Sensor.TYPE_GRAVITY) {
            currentGravity = event.values.clone()
            return
        }

        if (event.sensor.type != Sensor.TYPE_LINEAR_ACCELERATION) return

        // Check positioning if we have a target orientation (simplified for now)
        // For Squats in pocket, Y-axis gravity should be high (phone vertical)
        if (exerciseType == ExerciseType.SQUAT) {
            if (abs(currentGravity[1]) < 5f) { // roughly 45 degrees
                // manager.provideCorrection("Keep your phone vertical in your pocket!")
            }
        }

        // We use Y-axis (length of phone) as it usually aligns with the thigh/body
        val yAcc = event.values[1] 

        // Apply a simple moving average to filter noise
        accelerationBuffer[bufferIndex] = yAcc
        bufferIndex = (bufferIndex + 1) % bufferSize
        val smoothY = accelerationBuffer.sum() / bufferSize

        // State Machine with Tempo Tracking
        if (!isDown && smoothY < downThreshold) {
            isDown = true
            downStartTime = System.currentTimeMillis()
        } else if (isDown && smoothY > upThreshold) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastRepTime > REP_COOLDOWN_MS) {
                val repDuration = currentTime - downStartTime
                
                // Example Tempo Logic: If it took > 2s, maybe it's a "Slow Rep"
                // manager.onRepDetected(repDuration)

                isDown = false
                lastRepTime = currentTime
                manager.onRepDetected()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
