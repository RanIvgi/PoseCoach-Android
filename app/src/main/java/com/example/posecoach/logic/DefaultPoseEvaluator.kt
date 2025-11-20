package com.example.posecoach.logic

import android.util.Log
import com.example.posecoach.data.FeedbackMessage
import com.example.posecoach.data.FeedbackSeverity
import com.example.posecoach.data.PoseLandmarkIndex
import com.example.posecoach.data.PoseResult
import java.util.concurrent.TimeUnit

/**
 * Default implementation of PoseEvaluator for Student 3.
 * 
 * This is a STARTER IMPLEMENTATION with basic logic and TODOs.
 * Student 3 should expand this with real evaluation algorithms.
 * 
 * Current implementation provides:
 * - Basic pose detection feedback
 * - Placeholder exercise evaluation
 * - Example angle calculations
 * 
 * Student 3 TODO List:
 * 1. Implement real squat evaluation logic in evaluateSquat()
 * 2. Implement real push-up evaluation logic in evaluatePushup()
 * 3. Implement real lunge evaluation logic in evaluateLunge()
 * 4. Add rep counting logic (track movement patterns)
 * 5. Add more specific feedback messages
 * 6. Implement metrics collection (max depth, speed, etc.)
 * 7. Add state tracking for progressive feedback
 * 8. Consider adding exercise-specific classes for complex logic
 */
class DefaultPoseEvaluator : PoseEvaluator {
    
    // Internal state for squat evaluation
    private var squatState: SquatState = SquatState.UP
    private var minSquatAngleAchieved: Float = 180f // Track deepest point of the squat
    private var repCount: Int = 0
    private var insufficientDepthReps: Int = 0
    private var sessionStartTime: Long = 0L
    
    override fun evaluate(poseResult: PoseResult, exerciseType: String): FeedbackMessage? {
        // Log PoseResult for every frame (as requested)
        Log.d("PoseEvaluator", "PoseResult: ${poseResult.landmarks.size} landmarks, timestamp: ${poseResult.timestamp}")

        // No pose detected
        if (!poseResult.hasPose()) {
            return FeedbackMessage(
                text = "No pose detected. Step back or adjust camera.",
                severity = FeedbackSeverity.WARNING
            )
        }
        
        // Route to specific exercise evaluator
        return when (exerciseType.lowercase()) {
            "squat" -> evaluateSquat(poseResult)
            "pushup", "push-up", "push_up" -> evaluatePushup(poseResult)
            "lunge" -> evaluateLunge(poseResult)
            else -> evaluateGeneral(poseResult)
        }
    }
    
    override fun evaluateSquat(poseResult: PoseResult): FeedbackMessage? {
        val leftKneeAngle = poseResult.calculateAngle(
            PoseLandmarkIndex.LEFT_HIP,
            PoseLandmarkIndex.LEFT_KNEE,
            PoseLandmarkIndex.LEFT_ANKLE
        )
        val rightKneeAngle = poseResult.calculateAngle(
            PoseLandmarkIndex.RIGHT_HIP,
            PoseLandmarkIndex.RIGHT_KNEE,
            PoseLandmarkIndex.RIGHT_ANKLE
        )
        
        val leftHipAngle = poseResult.calculateAngle(
            PoseLandmarkIndex.LEFT_SHOULDER,
            PoseLandmarkIndex.LEFT_HIP,
            PoseLandmarkIndex.LEFT_KNEE
        )
        val rightHipAngle = poseResult.calculateAngle(
            PoseLandmarkIndex.RIGHT_SHOULDER,
            PoseLandmarkIndex.RIGHT_HIP,
            PoseLandmarkIndex.RIGHT_KNEE
        )
        
        val leftAnkleX = poseResult.getLandmark(PoseLandmarkIndex.LEFT_ANKLE)?.x
        val leftKneeX = poseResult.getLandmark(PoseLandmarkIndex.LEFT_KNEE)?.x
        val leftHipX = poseResult.getLandmark(PoseLandmarkIndex.LEFT_HIP)?.x
        val rightAnkleX = poseResult.getLandmark(PoseLandmarkIndex.RIGHT_ANKLE)?.x
        val rightKneeX = poseResult.getLandmark(PoseLandmarkIndex.RIGHT_KNEE)?.x
        val rightHipX = poseResult.getLandmark(PoseLandmarkIndex.RIGHT_HIP)?.x

        // Handle missing landmarks
        if (leftKneeAngle == null || rightKneeAngle == null || leftHipAngle == null || rightHipAngle == null) {
            return FeedbackMessage(
                text = "Adjust camera to see your hips, knees, and ankles clearly.",
                severity = FeedbackSeverity.WARNING
            )
        }

        val avgKneeAngle = (leftKneeAngle + rightKneeAngle) / 2
        val avgHipAngle = (leftHipAngle + rightHipAngle) / 2

        var currentFeedback: FeedbackMessage? = null

        // Rep Counting Logic
        when (squatState) {
            SquatState.UP -> {
                if (avgKneeAngle < AngleThresholds.SQUAT_KNEE_TRANSITION_DOWN) {
                    squatState = SquatState.DOWN
                    minSquatAngleAchieved = avgKneeAngle // Start tracking deepest angle for this rep
                    currentFeedback = FeedbackMessage("Keep going down!", FeedbackSeverity.INFO)
                }
            }
            SquatState.DOWN -> {
                minSquatAngleAchieved = kotlin.math.min(minSquatAngleAchieved, avgKneeAngle)
                if (avgKneeAngle > AngleThresholds.SQUAT_KNEE_TRANSITION_UP) {
                    // Transitioning up, check if depth was sufficient
                    if (minSquatAngleAchieved < AngleThresholds.SQUAT_KNEE_MIN_DEPTH) {
                        repCount++
                        currentFeedback = FeedbackMessage("Rep " + repCount + "!", FeedbackSeverity.INFO)
                        Log.d("PoseEvaluator", "Rep Count: $repCount")
                    } else {
                        insufficientDepthReps++
                        currentFeedback = FeedbackMessage("Go deeper on the next rep!", FeedbackSeverity.WARNING)
                    }
                    squatState = SquatState.UP
                    minSquatAngleAchieved = 180f // Reset for next rep
                } else if (avgKneeAngle < AngleThresholds.SQUAT_KNEE_MIN_DEPTH) {
                    currentFeedback = FeedbackMessage("Good depth!", FeedbackSeverity.INFO)
                }
            }
        }

        // Form Correction Logic (prioritize critical issues)
        if (currentFeedback == null || currentFeedback.severity != FeedbackSeverity.ERROR) {
            // Knees past toes check (simplified: check if knee X is significantly forward of ankle X)
            val leftKneeForward = leftAnkleX != null && leftKneeX != null && leftKneeX < leftAnkleX - RelativePositionThresholds.KNEE_TO_TOE_OFFSET
            val rightKneeForward = rightAnkleX != null && rightKneeX != null && rightKneeX < rightAnkleX - RelativePositionThresholds.KNEE_TO_TOE_OFFSET

            if (leftKneeForward || rightKneeForward) {
                currentFeedback = FeedbackMessage("Knees over toes! Push hips back.", FeedbackSeverity.WARNING)
            }

            // Back straightness check (simplified: check if hip is too far back relative to shoulder/knee)
            val leftHipTooFarBack = leftHipX != null && leftKneeX != null && leftHipX < leftKneeX - RelativePositionThresholds.HIP_TO_KNEE_OFFSET
            val rightHipTooFarBack = rightHipX != null && rightKneeX != null && rightHipX < rightKneeX - RelativePositionThresholds.HIP_TO_KNEE_OFFSET

            if (currentFeedback == null && (leftHipTooFarBack || rightHipTooFarBack)) {
                 currentFeedback = FeedbackMessage("Keep your chest up and back straight.", FeedbackSeverity.WARNING)
            }
        }

        // Default feedback if no specific issues are found
        return currentFeedback ?: when (squatState) {
            SquatState.UP -> FeedbackMessage("Ready to squat. Bend your knees.", FeedbackSeverity.INFO)
            SquatState.DOWN -> FeedbackMessage("Hold the squat.", FeedbackSeverity.INFO)
        }
    }
    
    override fun evaluatePushup(poseResult: PoseResult): FeedbackMessage? {
        // TODO (Student 3): Implement real push-up evaluation logic
        
        // Example: Calculate elbow angle
        val leftElbowAngle = poseResult.calculateAngle(
            PoseLandmarkIndex.LEFT_SHOULDER,
            PoseLandmarkIndex.LEFT_ELBOW,
            PoseLandmarkIndex.LEFT_WRIST
        )
        
        // TODO: Add more checks:
        // - Check body alignment (plank position)
        // - Verify elbows are at proper angle from body
        // - Ensure hips don't sag or pike up
        // - Track push-up depth
        // - Count reps
        
        // Placeholder feedback
        return when {
            leftElbowAngle == null -> FeedbackMessage(
                text = "Adjust camera to see your full body",
                severity = FeedbackSeverity.WARNING
            )
            leftElbowAngle < AngleThresholds.PUSHUP_ELBOW_MAX -> FeedbackMessage(
                text = "Good form! Elbows bent properly.",
                severity = FeedbackSeverity.INFO
            )
            else -> FeedbackMessage(
                text = "Lower your body more - bend elbows to 90°",
                severity = FeedbackSeverity.WARNING
            )
        }
    }
    
    override fun evaluateLunge(poseResult: PoseResult): FeedbackMessage? {
        // TODO (Student 3): Implement real lunge evaluation logic
        
        // Example: Calculate front knee angle (assuming left leg is forward)
        val frontKneeAngle = poseResult.calculateAngle(
            PoseLandmarkIndex.LEFT_HIP,
            PoseLandmarkIndex.LEFT_KNEE,
            PoseLandmarkIndex.LEFT_ANKLE
        )
        
        // TODO: Add more checks:
        // - Detect which leg is forward
        // - Check both knee angles
        // - Verify front knee doesn't go past toe
        // - Check torso is upright
        // - Ensure proper stride length
        
        // Placeholder feedback
        return FeedbackMessage(
            text = "Lunge detected - maintain 90° angles in both knees",
            severity = FeedbackSeverity.INFO
        )
    }
    
    /**
     * General pose evaluation when no specific exercise is selected.
     */
    private fun evaluateGeneral(poseResult: PoseResult): FeedbackMessage {
        // TODO (Student 3): Add general posture checks
        // For now, just confirm we're detecting the pose
        
        val visibleLandmarks = poseResult.landmarks.count { it.visibility > 0.5f }
        
        return when {
            visibleLandmarks < 10 -> FeedbackMessage(
                text = "Detecting pose... ($visibleLandmarks landmarks visible)",
                severity = FeedbackSeverity.WARNING
            )
            else -> FeedbackMessage(
                text = "Pose detected! Select an exercise to begin.",
                severity = FeedbackSeverity.INFO
            )
        }
    }
    
    override fun reset() {
        repCount = 0
        squatState = SquatState.UP
        minSquatAngleAchieved = 180f
        insufficientDepthReps = 0
        sessionStartTime = 0L
    }
    
    override fun getRepCount(): Int {
        return repCount
    }
    
    override fun getMetrics(): Map<String, Any> {
        // TODO (Student 3): Return useful metrics
        return emptyMap()
    }

    override fun startSession() {
        sessionStartTime = System.currentTimeMillis()
    }

    override fun getEvaluationSummary(): String? {
        if (sessionStartTime == 0L) {
            return null // Session never started
        }

        val durationMillis = System.currentTimeMillis() - sessionStartTime
        val durationSeconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis)

        val summary = StringBuilder()
        summary.append("Session Summary:\n")
        summary.append("Total Reps: $repCount\n")
        summary.append("Duration: $durationSeconds seconds\n")

        if (insufficientDepthReps > 0) {
            summary.append("\nNotes:\n")
            summary.append("- You had $insufficientDepthReps reps with insufficient depth. Try to go lower next time!\n")
        } else if (repCount > 0) {
            summary.append("\nGreat work! All your reps had good depth.\n")
        } else {
            summary.append("\nNo reps were completed in this session.\n")
        }

        return summary.toString()
    }

    private enum class SquatState { UP, DOWN }

    private object AngleThresholds {
        const val SQUAT_KNEE_TRANSITION_DOWN = 150f // Angle to detect start of squat (from UP to DOWN)
        const val SQUAT_KNEE_TRANSITION_UP = 160f   // Angle to detect end of squat (from DOWN to UP)
        const val SQUAT_KNEE_MIN_DEPTH = 90f      // Target angle for full squat depth
        const val PUSHUP_ELBOW_MAX = 90f // Max elbow angle for a valid push-up at the bottom
    }

    private object RelativePositionThresholds {
        const val KNEE_TO_TOE_OFFSET = 0.05f // How far knee X can be from ankle X before warning
        const val HIP_TO_KNEE_OFFSET = 0.05f // How far hip X can be from knee X before warning for back straightness
    }
}
