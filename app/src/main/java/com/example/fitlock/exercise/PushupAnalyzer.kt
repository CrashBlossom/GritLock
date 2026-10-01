package com.example.fitlock.exercise

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import com.example.fitlock.data.ExerciseCalibration
import kotlin.math.abs
import kotlin.math.atan2

class PushupAnalyzer(
    private val manager: ExerciseTrackerManager,
    private val calibration: ExerciseCalibration? = null
) {

    private var isDown = false
    private var lastRepTime = 0L
    private val REP_COOLDOWN_MS = 600L
    private val MIN_CONFIDENCE = 0.35f

    // Dynamic, reliable elbow angle thresholds (not blocked by rigid calibration)
    private val minElbowAngle = 100.0 // Down phase
    private val maxElbowAngle = 150.0 // Up phase

    fun analyzePose(pose: Pose, width: Int, height: Int) {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)

        if (leftShoulder == null || leftElbow == null || leftWrist == null) return

        if (leftShoulder.inFrameLikelihood < MIN_CONFIDENCE || 
            leftElbow.inFrameLikelihood < MIN_CONFIDENCE) {
            return
        }
        
        val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)

        val leftAngle = calculateAngle(leftShoulder, leftElbow, leftWrist)
        val rightAngle = if (rightShoulder != null && rightElbow != null && rightWrist != null && rightElbow.inFrameLikelihood > MIN_CONFIDENCE) {
            calculateAngle(rightShoulder, rightElbow, rightWrist)
        } else {
            leftAngle
        }

        val avgElbowAngle = (leftAngle + rightAngle) / 2.0

        if (!isDown && avgElbowAngle <= minElbowAngle) {
            isDown = true
        } else if (isDown && avgElbowAngle >= maxElbowAngle) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastRepTime > REP_COOLDOWN_MS) {
                isDown = false
                lastRepTime = currentTime
                manager.onRepDetected()
            }
        }

        manager.onMovementUpdate(
            isMovingDown = avgElbowAngle < maxElbowAngle - 15,
            isAtBottom = avgElbowAngle <= minElbowAngle + 15,
            isAtTop = avgElbowAngle >= maxElbowAngle - 15
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
