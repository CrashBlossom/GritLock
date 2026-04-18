package com.example.fitlock.exercise

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.abs
import kotlin.math.atan2

class HingeAnalyzer(private val manager: ExerciseTrackerManager) {

    private var isBent = false
    private var lastRepTime = 0L
    private val REP_COOLDOWN_MS = 1000L

    // Hinge logic: Bend at hips (Shoulder-Hip-Knee angle decreases)
    private var bentThresholdAngle = 110.0   // Hinge position
    private var straightThresholdAngle = 160.0 // Standing position

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

        if (!isBent && avgHipAngle < bentThresholdAngle) {
            isBent = true
        } else if (isBent && avgHipAngle > straightThresholdAngle) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastRepTime > REP_COOLDOWN_MS) {
                isBent = false
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
