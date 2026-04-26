/**
 * SensorTracker handles "Pocket Mode" tracking.
 * It uses the phone's physical sensors (Accelerometer and Gravity) to detect movement
 * when the phone is not looking at you with the camera.
 */
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

    // SensorManager is the system service that gives us access to hardware sensors
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    
    // Linear Acceleration is the "force" applied to the phone (excluding gravity)
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    
    // Gravity sensor tells us which way is "down" relative to the phone's orientation
    private val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)

    // State machine variables (similar to Camera Analyzers)
    private var isDown = false
    private var lastRepTime = 0L
    private var downStartTime = 0L
    private val REP_COOLDOWN_MS = 1000L
    
    // Acceleration thresholds: How hard do you have to move to trigger a "Down" or "Up"?
    private var downThreshold = (calibration?.bottomValue?.toFloat()) ?: -2.5f 
    private var upThreshold = (calibration?.topValue?.toFloat()) ?: 3.5f
    
    // Smoothing buffer: We average the last 5 readings to ignore small accidental vibrations
    private val bufferSize = 5
    private val accelerationBuffer = FloatArray(bufferSize)
    private var bufferIndex = 0

    private var currentGravity = FloatArray(3) // X, Y, Z coordinates of gravity

    /**
     * Start listening to the hardware sensors.
     */
    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        gravitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    /**
     * Stop listening to save battery.
     */
    fun stop() {
        sensorManager.unregisterListener(this)
    }

    /**
     * This is called by Android every time the phone moves even slightly.
     */
    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        // Update our knowledge of phone orientation
        if (event.sensor.type == Sensor.TYPE_GRAVITY) {
            currentGravity = event.values.clone()
            return
        }

        // Only process linear acceleration for rep counting
        if (event.sensor.type != Sensor.TYPE_LINEAR_ACCELERATION) return

        /**
         * Orientation Check:
         * For Squats in pocket, the phone should be mostly vertical.
         * We check the Gravity Y-axis to see if it's pointing down.
         */
        if (exerciseType == ExerciseType.SQUAT) {
            if (abs(currentGravity[1]) < 5f) { 
                // Potential to warn user here: "Keep your phone vertical!"
            }
        }

        // We use Y-axis acceleration (up/down the length of the phone)
        val yAcc = event.values[1] 

        // 1. Smoothing: Add new value to buffer and calculate average
        accelerationBuffer[bufferIndex] = yAcc
        bufferIndex = (bufferIndex + 1) % bufferSize
        val smoothY = accelerationBuffer.sum() / bufferSize

        /**
         * 2. Rep Detection State Machine:
         * A rep is a "Negative acceleration" (moving down) followed by "Positive acceleration" (stopping/moving up).
         */
        if (!isDown && smoothY < downThreshold) {
            // User started the downward motion
            isDown = true
            downStartTime = System.currentTimeMillis()
        } 
        else if (isDown && smoothY > upThreshold) {
            // User finished the motion and returned to start
            val currentTime = System.currentTimeMillis()
            
            // Ensure they didn't just shake the phone (cooldown)
            if (currentTime - lastRepTime > REP_COOLDOWN_MS) {
                isDown = false
                lastRepTime = currentTime
                manager.onRepDetected() // Success!
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used, but required by the interface
    }
}
