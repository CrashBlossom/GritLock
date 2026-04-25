package com.example.fitlock.exercise

import android.content.Context
import android.speech.tts.TextToSpeech
import com.example.fitlock.data.ExerciseCalibration
import java.util.*

enum class ExerciseType { PUSHUP, SQUAT, PULLUP, DIP, SITUP, HINGE, ROW, PLANK, STEPS, APP_USAGE }
enum class TrackingMode { CAMERA, POCKET }

class ExerciseTrackerManager(
    private val context: Context,
    private val onRepCountChanged: (Int) -> Unit,
    private val onWorkoutComplete: (Int) -> Unit
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var currentReps = 0
    private var targetReps = 0
    private var isTtsReady = false
    private var currentExerciseType: ExerciseType? = null
    private var currentCalibration: ExerciseCalibration? = null
    private var sensorTracker: SensorTracker? = null
    private var goalReachedSpoken = false

    fun startTracking(type: ExerciseType, mode: TrackingMode, goal: Int, calibration: ExerciseCalibration? = null) {
        this.targetReps = goal
        this.currentReps = 0
        this.currentExerciseType = type
        this.currentCalibration = calibration
        this.goalReachedSpoken = false
        
        if (mode == TrackingMode.POCKET) {
            sensorTracker?.stop()
            sensorTracker = SensorTracker(context, this, calibration, type)
            sensorTracker?.start()
            
            val guide = PocketExerciseGuide.getPlacementInstruction(type)
            speak("Placement guide: $guide")
        } else {
            sensorTracker?.stop()
            sensorTracker = null
        }
        
        val modeStr = if (mode == TrackingMode.CAMERA) "Camera" else "Pocket"
        val calStr = if (calibration != null) "calibrated " else ""
        
        if (type == ExerciseType.APP_USAGE) {
            speak("Starting app usage requirement. You need to spend ${goal} seconds in the required app.")
        } else {
            speak("Starting ${calStr}${type.name.lowercase()} workout in $modeStr mode. Goal is $goal reps.")
        }
    }

    fun onRepDetected() {
        currentReps++
        onRepCountChanged(currentReps)
        
        if (currentReps >= targetReps) {
            if (!goalReachedSpoken) {
                speak("Goal reached! Keep going or move to next.")
                goalReachedSpoken = true
                onWorkoutComplete(currentReps)
            } else {
                speak("$currentReps")
            }
        } else {
            speak("$currentReps")
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
    }
}
