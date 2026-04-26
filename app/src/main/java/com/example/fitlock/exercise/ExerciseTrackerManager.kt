/**
 * ExerciseTrackerManager acts as the "Brain" of the workout session.
 * it handles Text-to-Speech (TTS), manages rep state, and toggles between
 * Camera (ML Kit) and Pocket (Sensor) tracking modes.
 */
package com.example.fitlock.exercise

import android.content.Context
import android.speech.tts.TextToSpeech
import com.example.fitlock.data.ExerciseCalibration
import java.util.*

// Enumerations define specific sets of fixed values we can use throughout the app.
enum class ExerciseType { PUSHUP, SQUAT, PULLUP, DIP, SITUP, HINGE, ROW, PLANK, STEPS, APP_USAGE }
enum class TrackingMode { CAMERA, POCKET }

class ExerciseTrackerManager(
    private val context: Context,
    private val onRepCountChanged: (Int) -> Unit, // Function to call when a rep is counted
    private val onWorkoutComplete: (Int) -> Unit  // Function to call when the goal is met
) : TextToSpeech.OnInitListener {

    // TextToSpeech lets the app "talk" to the user
    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var currentReps = 0
    private var targetReps = 0
    private var isTtsReady = false
    private var currentExerciseType: ExerciseType? = null
    private var currentCalibration: ExerciseCalibration? = null
    private var sensorTracker: SensorTracker? = null // Component for "Pocket Mode"
    private var goalReachedSpoken = false

    /**
     * Prepares the manager for a new exercise session.
     */
    fun startTracking(type: ExerciseType, mode: TrackingMode, goal: Int, calibration: ExerciseCalibration? = null) {
        this.targetReps = goal
        this.currentReps = 0
        this.currentExerciseType = type
        this.currentCalibration = calibration
        this.goalReachedSpoken = false
        
        // If in Pocket Mode, we start listening to the Accelerometer/Gravity sensors
        if (mode == TrackingMode.POCKET) {
            sensorTracker?.stop()
            sensorTracker = SensorTracker(context, this, calibration, type)
            sensorTracker?.start()
            
            // Give the user spoken instructions on where to put the phone
            val guide = PocketExerciseGuide.getPlacementInstruction(type)
            speak("Placement guide: $guide")
        } else {
            // In Camera mode, we stop sensors as the Activity handles camera frames
            sensorTracker?.stop()
            sensorTracker = null
        }
        
        val modeStr = if (mode == TrackingMode.CAMERA) "Camera" else "Pocket"
        val calStr = if (calibration != null) "calibrated " else ""
        
        // Announce the start of the workout
        if (type == ExerciseType.APP_USAGE) {
            speak("Starting app usage requirement. You need to spend ${goal} seconds in the required app.")
        } else {
            speak("Starting ${calStr}${type.name.lowercase()} workout in $modeStr mode. Goal is $goal reps.")
        }
    }

    /**
     * Called by an Analyzer (Camera or Sensor) when it detects a completed movement.
     */
    fun onRepDetected() {
        currentReps++
        onRepCountChanged(currentReps) // Update the UI
        
        if (currentReps >= targetReps) {
            // Notify the user they finished
            if (!goalReachedSpoken) {
                speak("Goal reached! Keep going or move to next.")
                goalReachedSpoken = true
                onWorkoutComplete(currentReps)
            } else {
                speak("$currentReps") // Just count the extra reps
            }
        } else {
            speak("$currentReps") // Voice the current count
        }
    }

    /**
     * Provides voice feedback if the user's form is incorrect.
     */
    fun provideCorrection(message: String) {
        speak(message)
    }

    /**
     * Internal helper to make the phone speak.
     */
    private fun speak(text: String) {
        if (isTtsReady) {
            // QUEUE_FLUSH means stop talking current sentence and start the new one immediately
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "SweatLockTTS")
        }
    }

    /**
     * Callback from Android system when the speech engine is ready.
     */
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            isTtsReady = true
        }
    }

    /**
     * Cleanup resources to prevent memory leaks.
     */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        sensorTracker?.stop()
    }
}
