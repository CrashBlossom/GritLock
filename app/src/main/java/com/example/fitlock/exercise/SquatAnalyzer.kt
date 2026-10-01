package com.example.fitlock.exercise

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import com.example.fitlock.data.ExerciseCalibration
import kotlin.math.abs
import kotlin.math.atan2

class SquatAnalyzer(
    private val manager: ExerciseTrackerManager,
    private val calibration: ExerciseCalibration? = null
) {

    private var isDown = false
    private var lastRepTime = 0L
    private val REP_COOLDOWN_MS = 600L
    private val MIN_CONFIDENCE = 0.35f

    private var downThresholdAngle = 105.0
    private var upThresholdAngle = 155.0

    fun analyzePose(pose: Pose, width: Int, height: Int) {
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)

        if (leftHip == null || leftKnee == null || leftAnkle == null) return
        
        if (leftHip.inFrameLikelihood < MIN_CONFIDENCE || 
            leftKnee.inFrameLikelihood < MIN_CONFIDENCE ||
            leftAnkle.inFrameLikelihood < MIN_CONFIDENCE) {
            return
        }

        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

        val leftAngle = calculateAngle(leftHip, leftKnee, leftAnkle)
        val rightAngle = if (rightHip != null && rightKnee != null && rightAnkle != null && rightKnee.inFrameLikelihood > MIN_CONFIDENCE) {
            calculateAngle(rightHip, rightKnee, rightAnkle)
        } else {
            leftAngle
        }

        val avgKneeAngle = (leftAngle + rightAngle) / 2.0

        if (!isDown && avgKneeAngle <= downThresholdAngle) {
            isDown = true
        } else if (isDown && avgKneeAngle >= upThresholdAngle) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastRepTime > REP_COOLDOWN_MS) {
                isDown = false
                lastRepTime = currentTime
                manager.onRepDetected()
            }
        }

        manager.onMovementUpdate(
            isMovingDown = avgKneeAngle < upThresholdAngle - 15,
            isAtBottom = avgKneeAngle <= downThresholdAngle + 15,
            isAtTop = avgKneeAngle >= upThresholdAngle - 15
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
