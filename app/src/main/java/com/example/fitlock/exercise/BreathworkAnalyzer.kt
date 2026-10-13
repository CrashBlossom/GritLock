package com.example.fitlock.exercise

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark

class BreathworkAnalyzer(private val manager: ExerciseTrackerManager) {

    private var isFacing = false
    private var lastTickTime = 0L
    private val TICK_INTERVAL_MS = 1000L
    private var secondsElapsed = 0

    fun analyzePose(pose: Pose, width: Int, height: Int) {
        val nose = pose.getPoseLandmark(PoseLandmark.NOSE)
        val leftEye = pose.getPoseLandmark(PoseLandmark.LEFT_EYE)
        val rightEye = pose.getPoseLandmark(PoseLandmark.RIGHT_EYE)

        val hasFace = nose != null || (leftEye != null && rightEye != null)
        val currentTime = System.currentTimeMillis()

        if (hasFace) {
            if (!isFacing) {
                isFacing = true
                lastTickTime = currentTime
            } else {
                if (currentTime - lastTickTime >= TICK_INTERVAL_MS) {
                    lastTickTime = currentTime
                    secondsElapsed++
                    manager.onRepDetected() // Each second counted toward breathwork goal

                    // Box Breathing Guidance (Inhale 4s -> Hold 4s -> Exhale 4s -> Hold 4s)
                    val phaseSeconds = secondsElapsed % 16
                    when (phaseSeconds) {
                        0 -> manager.provideCorrection("🫁 Inhale slowly... (4s)")
                        4 -> manager.provideCorrection("⏸️ Hold breath... (4s)")
                        8 -> manager.provideCorrection("💨 Exhale fully... (4s)")
                        12 -> manager.provideCorrection("⏸️ Pause & rest... (4s)")
                    }
                }
            }
        } else {
            if (isFacing) {
                isFacing = false
                manager.provideCorrection("Face the screen to continue breathwork session.")
            }
        }
    }
}
