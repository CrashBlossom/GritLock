package com.example.fitlock.exercise

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.fitlock.data.ExerciseCalibration
import kotlin.math.abs
import kotlin.math.sqrt

class SensorTracker(
    context: Context,
    private val manager: ExerciseTrackerManager,
    private val calibration: ExerciseCalibration? = null,
    private val exerciseType: ExerciseType? = null
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) 
        ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var lastRepTime = 0L
    private val REP_COOLDOWN_MS = 800L

    fun start() {
        sensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        val currentTime = System.currentTimeMillis()

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val magnitude = sqrt((x * x + y * y + z * z).toDouble())
            val delta = abs(magnitude - 9.8)
            if (delta > 2.0 && currentTime - lastRepTime > REP_COOLDOWN_MS) {
                lastRepTime = currentTime
                manager.onRepDetected()
            }
        } else if (event.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) {
            val yAcc = event.values[1]
            if (abs(yAcc) > 1.8 && currentTime - lastRepTime > REP_COOLDOWN_MS) {
                lastRepTime = currentTime
                manager.onRepDetected()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
