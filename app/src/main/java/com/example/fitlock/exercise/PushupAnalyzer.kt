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
    private val REP_COOLDOWN_MS = 1000L

    // Default values if no calibration exists
    private var minElbowAngle = calibration?.bottomValue ?: 90.0
    private var maxElbowAngle = calibration?.topValue ?: 160.0
    
    private var baselineShoulderY: Float? = null
    private var imageWidth: Int = 0
    private var imageHeight: Int = 0

    fun analyzePose(pose: Pose, width: Int, height: Int) {
        this.imageWidth = width
        this.imageHeight = height

        // Check for full body visibility
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)

        val bodyInShot = (leftShoulder != null && rightShoulder != null) &&
                (leftAnkle != null || rightAnkle != null || (leftKnee != null && rightKnee != null))

        if (!bodyInShot) {
            manager.provideCorrection("Step back! Ensure full body is visible.")
            return
        }
        
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW) ?: return
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST) ?: return
        
        val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)

        val leftAngle = calculateAngle(leftShoulder!!, leftElbow, leftWrist)
        val rightAngle = if (rightShoulder != null && rightElbow != null && rightWrist != null) {
            calculateAngle(rightShoulder, rightElbow, rightWrist)
        } else {
            leftAngle
        }

        val avgElbowAngle = (leftAngle + rightAngle) / 2.0

        if (baselineShoulderY == null) {
            baselineShoulderY = leftShoulder.position.y
        }

        val MOVEMENT_THRESHOLD = height * 0.1f 

        // Rep detection logic using calibrated or default angles
        // For pushups, "Down" (Bottom) is a smaller angle, "Up" (Top) is a larger angle
        if (!isDown && avgElbowAngle <= minElbowAngle) {
            isDown = true
        } else if (isDown && avgElbowAngle >= maxElbowAngle) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastRepTime > REP_COOLDOWN_MS) {
                
                val verticalDisplacement = abs(leftShoulder.position.y - (baselineShoulderY ?: 0f))
                
                // If calibrated, we might also use a threshold for movement, otherwise default to 10% screen height
                if (verticalDisplacement > MOVEMENT_THRESHOLD) {
                    isDown = false
                    lastRepTime = currentTime
                    manager.onRepDetected()
                } else {
                    manager.provideCorrection("Go deeper and move your whole body!")
                }
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
