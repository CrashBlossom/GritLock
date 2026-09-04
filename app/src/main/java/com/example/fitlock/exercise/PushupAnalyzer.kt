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
    private val MIN_CONFIDENCE = 0.8f // Option 1: Higher confidence gate

    // Default values if no calibration exists
    private var minElbowAngle = calibration?.bottomValue ?: 90.0
    private var maxElbowAngle = calibration?.topValue ?: 160.0
    
    private var baselineShoulderY: Float? = null
    private var imageWidth: Int = 0
    private var imageHeight: Int = 0

    fun analyzePose(pose: Pose, width: Int, height: Int) {
        this.imageWidth = width
        this.imageHeight = height

        // 1. Confidence Check (Option 1)
        // Ensure the AI is very sure it's seeing human landmarks
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)

        if (leftShoulder == null || rightShoulder == null || leftElbow == null || leftWrist == null) return

        if (leftShoulder.inFrameLikelihood < MIN_CONFIDENCE || 
            rightShoulder.inFrameLikelihood < MIN_CONFIDENCE ||
            leftElbow.inFrameLikelihood < MIN_CONFIDENCE) {
            return // Skip this frame if confidence is low (e.g. looking at a plant)
        }

        // 2. Full Body Visibility
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)

        val bodyInShot = (leftAnkle != null && leftAnkle.inFrameLikelihood > 0.5f) || 
                         (rightAnkle != null && rightAnkle.inFrameLikelihood > 0.5f) || 
                         (leftKnee != null && leftKnee.inFrameLikelihood > 0.5f)

        if (!bodyInShot) {
            manager.provideCorrection("Step back! Let me see your full body to track your form accurately.")
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

        // Reset baseline if needed
        if (baselineShoulderY == null) {
            baselineShoulderY = leftShoulder.position.y
        }

        // 3. Motion Delta Check (Option 3)
        // Require at least 12% vertical displacement of the torso for a rep
        val MOVEMENT_THRESHOLD = height * 0.12f 

        if (!isDown && avgElbowAngle <= minElbowAngle) {
            isDown = true
            // Capture Y position at the "Bottom" of the pushup
            baselineShoulderY = leftShoulder.position.y
        } else if (isDown && avgElbowAngle >= maxElbowAngle) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastRepTime > REP_COOLDOWN_MS) {
                
                // Calculate how much the shoulder actually moved since the "Down" phase
                val verticalDisplacement = abs(leftShoulder.position.y - (baselineShoulderY ?: 0f))
                
                if (verticalDisplacement > MOVEMENT_THRESHOLD) {
                    isDown = false
                    lastRepTime = currentTime
                    // manager.onRepDetected() // Now handled by TempoTracker via onMovementUpdate
                } else {
                    // It was likely just camera jitter or a non-human object "flickering"
                    // We don't trigger correction immediately to avoid noise, but we reset "isDown"
                    // if they come all the way up without enough movement.
                    isDown = false
                }
            }
        }

        // Continuous reporting for TempoTracker
        manager.onMovementUpdate(
            isMovingDown = avgElbowAngle < maxElbowAngle - 15, // Increased lenience (was 20)
            isAtBottom = avgElbowAngle <= minElbowAngle + 15,  // Increased lenience (was 10)
            isAtTop = avgElbowAngle >= maxElbowAngle - 15      // Increased lenience (was 10)
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
