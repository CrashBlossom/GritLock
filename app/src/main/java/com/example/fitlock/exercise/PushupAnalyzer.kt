/**
 * PushupAnalyzer is a specialized class that uses "Skeleton" data from the camera
 * to detect when a user has completed a pushup.
 */
package com.example.fitlock.exercise

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import com.example.fitlock.data.ExerciseCalibration
import kotlin.math.abs
import kotlin.math.atan2

class PushupAnalyzer(
    private val manager: ExerciseTrackerManager, // The manager we report "Reps" back to
    private val calibration: ExerciseCalibration? = null // Optional user-specific body angles
) {

    // Internal state tracking
    private var isDown = false // Is the user currently in the 'bottom' of the pushup?
    private var lastRepTime = 0L // Timestamp to prevent accidental double-counting
    private val REP_COOLDOWN_MS = 1000L // Wait at least 1 second between reps

    // Body angles (calibrated or default). 
    // minElbowAngle: The angle of the elbow when at the bottom (e.g., 90 degrees)
    // maxElbowAngle: The angle of the elbow when at the top (e.g., 160 degrees)
    private var minElbowAngle = calibration?.bottomValue ?: 90.0
    private var maxElbowAngle = calibration?.topValue ?: 160.0
    
    private var baselineShoulderY: Float? = null // Reference Y-coordinate to ensure body movement
    private var imageWidth: Int = 0
    private var imageHeight: Int = 0

    /**
     * analyzePose is called for every frame processed by ML Kit.
     */
    fun analyzePose(pose: Pose, width: Int, height: Int) {
        this.imageWidth = width
        this.imageHeight = height

        // 1. Landmark Extraction: Fetch specific body parts from the skeleton
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)

        // 2. Visibility Check: Ensure the user is actually in front of the camera
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

        // 3. Angle Calculation: Measure how bent the elbows are
        val leftAngle = calculateAngle(leftShoulder!!, leftElbow, leftWrist)
        val rightAngle = if (rightShoulder != null && rightElbow != null && rightWrist != null) {
            calculateAngle(rightShoulder, rightElbow, rightWrist)
        } else {
            leftAngle // Fallback to left if right side is hidden
        }

        val avgElbowAngle = (leftAngle + rightAngle) / 2.0

        // 4. Baseline Setup: Remember the starting height of the shoulders
        if (baselineShoulderY == null) {
            baselineShoulderY = leftShoulder.position.y
        }

        // We require the body to move at least 10% of the screen height to count a rep
        val MOVEMENT_THRESHOLD = height * 0.1f 

        // 5. State Machine: "Down" -> "Up" cycle
        // If user bends elbows enough, mark as "Down"
        if (!isDown && avgElbowAngle <= minElbowAngle) {
            isDown = true
        } 
        // If user was Down and now straightens elbows, it's a potential rep
        else if (isDown && avgElbowAngle >= maxElbowAngle) {
            val currentTime = System.currentTimeMillis()
            
            // Check cooldown and displacement to verify it's a real rep
            if (currentTime - lastRepTime > REP_COOLDOWN_MS) {
                val verticalDisplacement = abs(leftShoulder.position.y - (baselineShoulderY ?: 0f))
                
                if (verticalDisplacement > MOVEMENT_THRESHOLD) {
                    isDown = false
                    lastRepTime = currentTime
                    manager.onRepDetected() // Notify the app to increment the counter
                } else {
                    manager.provideCorrection("Go deeper and move your whole body!")
                }
            }
        }
    }

    /**
     * Helper to calculate the angle between three points (Shoulder -> Elbow -> Wrist)
     */
    private fun calculateAngle(first: PoseLandmark, mid: PoseLandmark, last: PoseLandmark): Double {
        var result = Math.toDegrees(
            atan2(last.position.y - mid.position.y, last.position.x - mid.position.x).toDouble() -
            atan2(first.position.y - mid.position.y, first.position.x - mid.position.x).toDouble()
    )
        result = abs(result) // Ensure angle is positive
        if (result > 180) result = 360.0 - result // Ensure we get the inner angle
        return result
    }
}
