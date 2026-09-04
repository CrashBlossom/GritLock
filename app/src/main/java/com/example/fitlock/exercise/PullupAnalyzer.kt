package com.example.fitlock.exercise

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import com.example.fitlock.data.ExerciseCalibration
import kotlin.math.abs
import kotlin.math.atan2

class PullupAnalyzer(
    private val manager: ExerciseTrackerManager,
    private val calibration: ExerciseCalibration? = null
) {

    private var isUp = false
    private var lastRepTime = 0L
    private val REP_COOLDOWN_MS = 1500L

    // Pullup logic: Top is a small angle, bottom is a large angle
    private var upThresholdAngle = calibration?.topValue ?: 60.0
    private var downThresholdAngle = calibration?.bottomValue ?: 150.0

    fun analyzePose(pose: Pose, width: Int, height: Int) {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER) ?: return
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW) ?: return
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST) ?: return

        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)

        val leftAngle = calculateAngle(leftShoulder, leftElbow, leftWrist)
        val rightAngle = if (rightShoulder != null && rightElbow != null && rightWrist != null) {
            calculateAngle(rightShoulder, rightElbow, rightWrist)
        } else {
            leftAngle
        }

        val avgElbowAngle = (leftAngle + rightAngle) / 2.0

        if (!isUp && avgElbowAngle <= upThresholdAngle) {
            // Check if hands are above shoulders (vertical pull-up)
            if (leftWrist.position.y < leftShoulder.position.y) {
                isUp = true
            }
        } else if (isUp && avgElbowAngle >= downThresholdAngle) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastRepTime > REP_COOLDOWN_MS) {
                isUp = false
                lastRepTime = currentTime
                // manager.onRepDetected()
            }
        }

        manager.onMovementUpdate(
            isMovingDown = avgElbowAngle > upThresholdAngle + 15,
            isAtBottom = avgElbowAngle >= downThresholdAngle - 15,
            isAtTop = avgElbowAngle <= upThresholdAngle + 15
        )
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
