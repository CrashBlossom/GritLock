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
    private val REP_COOLDOWN_MS = 1500L
    private val MIN_CONFIDENCE = 0.8f // Option 1: Higher confidence gate

    private var downThresholdAngle = calibration?.bottomValue ?: 100.0
    private var upThresholdAngle = calibration?.topValue ?: 160.0
    
    private var baselineHipY: Float? = null

    fun analyzePose(pose: Pose, width: Int, height: Int) {
        // 1. Confidence Check
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)

        if (leftHip == null || leftKnee == null || leftAnkle == null) return
        
        if (leftHip.inFrameLikelihood < MIN_CONFIDENCE || 
            leftKnee.inFrameLikelihood < MIN_CONFIDENCE ||
            leftAnkle.inFrameLikelihood < MIN_CONFIDENCE) {
            return // Ignore jittery objects like plants
        }

        // 2. Body Shot Validation
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

        val bodyInShot = leftShoulder != null && rightShoulder != null && 
                         leftAnkle != null && rightAnkle != null

        if (!bodyInShot) {
            manager.provideCorrection("Step back! Ensure full body (shoulders to feet) is in shot.")
            return
        }

        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)

        val leftAngle = calculateAngle(leftHip, leftKnee, leftAnkle)
        val rightAngle = if (rightHip != null && rightKnee != null && rightAnkle != null && rightKnee.inFrameLikelihood > MIN_CONFIDENCE) {
            calculateAngle(rightHip, rightKnee, rightAnkle)
        } else {
            leftAngle
        }

        val avgKneeAngle = (leftAngle + rightAngle) / 2.0
        
        // 3. Motion Delta Check
        val MOVEMENT_THRESHOLD = height * 0.15f // Squats require more movement than pushups

        if (!isDown && avgKneeAngle <= downThresholdAngle) {
            isDown = true
            baselineHipY = leftHip.position.y
        } else if (isDown && avgKneeAngle >= upThresholdAngle) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastRepTime > REP_COOLDOWN_MS) {
                
                val verticalDisplacement = abs(leftHip.position.y - (baselineHipY ?: 0f))
                
                if (verticalDisplacement > MOVEMENT_THRESHOLD) {
                    isDown = false
                    lastRepTime = currentTime
                    // manager.onRepDetected()
                } else {
                    isDown = false // Likely just jitter
                }
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
