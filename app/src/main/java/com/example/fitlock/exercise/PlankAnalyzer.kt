package com.example.fitlock.exercise

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.abs
import kotlin.math.atan2

class PlankAnalyzer(private val manager: ExerciseTrackerManager) {

    private var isPlanking = false
    private var startTime = 0L
    private var lastTickTime = 0L
    private val TICK_INTERVAL_MS = 1000L // Count every second as a "rep" for goal tracking

    // Plank logic: Body should be relatively straight (Shoulder-Hip-Ankle angle ~180)
    private var straightThresholdAngle = 150.0 

    fun analyzePose(pose: Pose, width: Int, height: Int) {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER) ?: return
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP) ?: return
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE) ?: return

        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

        val leftAngle = calculateAngle(leftShoulder, leftHip, leftAnkle)
        val rightAngle = if (rightShoulder != null && rightHip != null && rightAnkle != null) {
            calculateAngle(rightShoulder, rightHip, rightAnkle)
        } else {
            leftAngle
        }

        val avgBodyAngle = (leftAngle + rightAngle) / 2.0
        val currentTime = System.currentTimeMillis()

        if (avgBodyAngle > straightThresholdAngle) {
            if (!isPlanking) {
                isPlanking = true
                startTime = currentTime
                lastTickTime = currentTime
            } else {
                if (currentTime - lastTickTime >= TICK_INTERVAL_MS) {
                    lastTickTime = currentTime
                    manager.onRepDetected() // Using "rep" as a second for the goal
                }
            }
        } else {
            if (isPlanking) {
                isPlanking = false
                manager.provideCorrection("Keep your back straight!")
            }
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
}
