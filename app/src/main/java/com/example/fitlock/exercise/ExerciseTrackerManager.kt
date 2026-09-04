package com.example.fitlock.exercise

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.speech.tts.TextToSpeech
import com.example.fitlock.data.ExerciseCalibration
import java.util.*
import kotlin.math.sqrt

enum class ExerciseType { PUSHUP, SQUAT, PULLUP, DIP, SITUP, HINGE, ROW, PLANK, STEPS, APP_USAGE, FLASHCARDS }
enum class TrackingMode { CAMERA, POCKET }

class ExerciseTrackerManager(
    private val context: Context,
    private val onRepCountChanged: (Int) -> Unit,
    private val onWorkoutComplete: (Int, String?, Int) -> Unit, // reps, familyId, level
    private val onStationaryStatusChanged: (Boolean) -> Unit = {}
) : TextToSpeech.OnInitListener, SensorEventListener {

    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var currentReps = 0
    private var targetReps = 0
    private var isTtsReady = false
    private var currentExerciseType: ExerciseType? = null
    private var currentCalibration: ExerciseCalibration? = null
    private var sensorTracker: SensorTracker? = null
    private var goalReachedSpoken = false
    private var workoutStartTime: Long = 0

    // Stationary detection
    private var sensorManager: SensorManager? = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var isStationary = false
    private var lastStationaryStartTime = 0L
    private val SETTLING_TIME_MS = 2000L // Must be still for 2 seconds
    private var currentMode: TrackingMode = TrackingMode.CAMERA

    private var tempoTracker: TempoTracker? = null
    private var currentFamilyId: String? = null
    private var currentLevel: Int = 1
    private var currentVariantName: String? = null

    fun startTracking(
        type: ExerciseType, 
        mode: TrackingMode, 
        goal: Int, 
        calibration: ExerciseCalibration? = null, 
        tempo: String = "3-1-1-1",
        familyId: String? = null,
        level: Int = 1,
        variantName: String? = null
    ) {
        this.targetReps = goal
        this.currentReps = 0
        this.currentExerciseType = type
        this.currentCalibration = calibration
        this.currentFamilyId = familyId
        this.currentLevel = level
        this.currentVariantName = variantName
        this.goalReachedSpoken = false
        this.workoutStartTime = System.currentTimeMillis()
        this.currentMode = mode
        this.isStationary = false
        this.lastStationaryStartTime = 0L
        onStationaryStatusChanged(false)

        if (mode == TrackingMode.CAMERA) {
            tempoTracker = TempoTracker(tempo, ::speak, ::onRepDetected)
            tempoTracker?.start()
        }
        
        if (mode == TrackingMode.POCKET) {
            sensorTracker?.stop()
            sensorTracker = SensorTracker(context, this, calibration, type)
            sensorTracker?.start()
            
            val guide = PocketExerciseGuide.getPlacementInstruction(type)
            speak("Placement guide: $guide")
            // Unregister stationary detection for pocket mode
            sensorManager?.unregisterListener(this)
        } else {
            sensorTracker?.stop()
            sensorTracker = null
            // Register stationary detection for camera mode
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }
        
        val modeStr = if (mode == TrackingMode.CAMERA) "Camera" else "Pocket"
        val calStr = if (calibration != null) "calibrated " else ""
        
        if (type == ExerciseType.APP_USAGE) {
            speak("Starting app usage requirement. You need to spend ${goal} seconds in the required app.")
        } else {
            speak("Starting ${calStr}${type.name.lowercase()} workout in $modeStr mode. Goal is $goal reps.")
        }
    }

    fun switchToPocketMode() {
        if (currentMode == TrackingMode.POCKET) return
        
        currentMode = TrackingMode.POCKET
        tempoTracker?.stop()
        tempoTracker = null
        
        sensorTracker = SensorTracker(context, this, currentCalibration, currentExerciseType ?: ExerciseType.PUSHUP)
        sensorTracker?.start()
        
        sensorManager?.unregisterListener(this)
        
        val guide = PocketExerciseGuide.getPlacementInstruction(currentExerciseType ?: ExerciseType.PUSHUP)
        speak("Switching to Pocket Mode. $guide")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (currentExerciseType == null || currentMode != TrackingMode.CAMERA || event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
        
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        
        // Calculate magnitude minus gravity (roughly 9.806)
        val g = sqrt(x*x + y*y + z*z)
        val movement = Math.abs(g - 9.80665f)
        
        val MOVEMENT_THRESHOLD = 0.4f // Reverting to balanced sensitivity
        val now = System.currentTimeMillis()

        if (movement > MOVEMENT_THRESHOLD) {
            if (isStationary || lastStationaryStartTime != 0L) {
                isStationary = false
                lastStationaryStartTime = 0L
                onStationaryStatusChanged(false)
            }
        } else {
            if (lastStationaryStartTime == 0L) {
                lastStationaryStartTime = now
            } else if (!isStationary && (now - lastStationaryStartTime > SETTLING_TIME_MS)) {
                isStationary = true
                onStationaryStatusChanged(true)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun getDurationSeconds(): Long {
        return (System.currentTimeMillis() - workoutStartTime) / 1000
    }

    fun onWorkoutCompleteInternal() {
        onWorkoutComplete(currentReps, currentFamilyId, currentLevel)
    }

    fun onMovementUpdate(isMovingDown: Boolean, isAtBottom: Boolean, isAtTop: Boolean) {
        tempoTracker?.onMovementDetected(isMovingDown, isAtBottom, isAtTop)
    }

    fun onRepDetected() {
        if (currentMode == TrackingMode.CAMERA && !isStationary) {
            // Movement detected or still settling, ignore the rep
            return
        }

        currentReps++
        onRepCountChanged(currentReps)
        
        if (currentReps >= targetReps) {
            if (!goalReachedSpoken) {
                speak("Goal reached! Keep going or move to next.")
                goalReachedSpoken = true
                onWorkoutComplete(currentReps, currentFamilyId, currentLevel)
            } else {
                speak("$currentReps")
            }
        } else {
            speak("$currentReps")
        }
    }

    fun addManualReps(count: Int) {
        currentReps += count
        onRepCountChanged(currentReps)
        if (currentReps >= targetReps && !goalReachedSpoken) {
            speak("Goal reached via banked reps!")
            goalReachedSpoken = true
            onWorkoutComplete(currentReps, currentFamilyId, currentLevel)
        }
    }

    fun provideCorrection(message: String) {
        speak(message)
    }

    private fun speak(text: String) {
        if (isTtsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "SweatLockTTS")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            isTtsReady = true
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        sensorTracker?.stop()
        sensorManager?.unregisterListener(this)
        currentExerciseType = null
        isStationary = false
        lastStationaryStartTime = 0L
    }
}
