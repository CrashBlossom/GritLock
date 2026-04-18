package com.example.fitlock.exercise

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import com.example.fitlock.data.ExerciseCalibration
import kotlin.math.abs
import kotlin.math.atan2

class SitupAnalyzer(
    private val manager: ExerciseTrackerManager,
    private val calibration: ExerciseCalibration? = null
) {

    private var isUp = false
    private var lastRepTime = 0L
    private val REP_COOLDOWN_MS = 1500L

    // Angle between Shoulder, Hip and Knee
    private var upThresholdAngle = calibration?.topValue ?: 60.0   // Crouched/Up position
    private var downThresholdAngle = calibration?.bottomValue ?: 130.0 // Lying/Down position

    fun analyzePose(pose: Pose, width: Int, height: Int) {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER) ?: return
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP) ?: return
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE) ?: return

        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)

        val leftAngle = calculateAngle(leftShoulder, leftHip, leftKnee)
        val rightAngle = if (rightShoulder != null && rightHip != null && rightKnee != null) {
            calculateAngle(rightShoulder, rightHip, rightKnee)
        } else {
            leftAngle
        }

        val avgHipAngle = (leftAngle + rightAngle) / 2.0

        // For Situps, topValue (up) is usually a smaller angle than bottomValue (down)
        if (!isUp && avgHipAngle <= upThresholdAngle) {
            isUp = true
        } else if (isUp && avgHipAngle >= downThresholdAngle) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastRepTime > REP_COOLDOWN_MS) {
                isUp = false
                lastRepTime = currentTime
                manager.onRepDetected()
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
