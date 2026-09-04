package com.example.fitlock.exercise

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.abs
import kotlin.math.atan2

class RowAnalyzer(private val manager: ExerciseTrackerManager) {

    private var isPulled = false
    private var lastRepTime = 0L
    private val REP_COOLDOWN_MS = 1000L

    // Row logic: Elbow flexion while pulling (Elbow angle)
    private var pulledThresholdAngle = 70.0   // Top of row
    private var extendedThresholdAngle = 150.0 // Bottom of row

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

        if (!isPulled && avgElbowAngle < pulledThresholdAngle) {
            isPulled = true
        } else if (isPulled && avgElbowAngle > extendedThresholdAngle) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastRepTime > REP_COOLDOWN_MS) {
                isPulled = false
                lastRepTime = currentTime
                // manager.onRepDetected()
            }
        }

        manager.onMovementUpdate(
            isMovingDown = avgElbowAngle > pulledThresholdAngle + 20,
            isAtBottom = avgElbowAngle >= extendedThresholdAngle - 10,
            isAtTop = avgElbowAngle <= pulledThresholdAngle + 10
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
