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
    private var squatRepCount: Int = 0
    private var insufficientDepthSquats: Int = 0
    
    // Internal state for push-up evaluation
    private var pushupState: PushupState = PushupState.UP
    private var minElbowAngleAchieved: Float = 180f // Track deepest point of the push-up
    private var pushupRepCount: Int = 0
    private var insufficientDepthPushups: Int = 0
    
    // Internal state for plank evaluation
    private var plankState: PlankState = PlankState.NOT_IN_POSITION
    private var plankHoldStartTime: Long = 0L // When user achieved good form
    private var plankTotalGoodFormTimeMs: Long = 0L // Total time with good form
    private var plankFormBreakCount: Int = 0 // How many times form was broken
    
    // Session tracking
    private var sessionStartTime: Long = 0L
    private var currentExerciseType: String = "squat" // Track which exercise is active
    
    // Feedback filtering state
    private var lastStableFeedback: FeedbackMessage? = null
    private var pendingFeedback: FeedbackMessage? = null
    private var pendingFeedbackStartTime: Long = 0
    private val FEEDBACK_DEBOUNCE_MS = 200L // Ignore warnings lasting less than this

    override fun evaluate(poseResult: PoseResult, exerciseType: String): FeedbackMessage? {
        // PERFORMANCE OPTIMIZATION: Per-frame logging disabled
        // This log executes on every pose evaluation (30+ FPS during active session).
        // On emulators, this causes significant UI thread blocking and frame skips.
        // Together with PoseOverlay logging, these were the main cause of 1-6 FPS performance.
        // Re-enable only when debugging pose evaluation logic.
        // Log.d("PoseEvaluator", "PoseResult: ${poseResult.landmarks.size} landmarks, timestamp: ${poseResult.timestamp}")

        // Track current exercise type for rep count reporting
        currentExerciseType = exerciseType
        
        // No pose detected
        if (!poseResult.hasPose()) {
            // Treat "No pose detected" as a warning that needs debouncing too, 
            // to avoid flickering when pose is lost for a single frame.
            return filterFeedback(FeedbackMessage(
                text = "No pose detected. Step back or adjust camera.",
                severity = FeedbackSeverity.WARNING
            ))
        }
        
        // Route to specific exercise evaluator
        val rawFeedback = when (exerciseType.lowercase()) {
            "squat" -> evaluateSquat(poseResult)
            "pushup", "push-up", "push_up" -> evaluatePushup(poseResult)
            "lunge" -> evaluateLunge(poseResult)
            "plank" -> evaluatePlank(poseResult)
            else -> evaluateGeneral(poseResult)
        }

        return filterFeedback(rawFeedback)
    }

    private fun filterFeedback(newFeedback: FeedbackMessage?): FeedbackMessage? {
        val now = System.currentTimeMillis()

        // 1. INFO messages and NULL (cleared) pass through immediately
        // We assume these are "safe" states or important events (reps)
        if (newFeedback == null || newFeedback.severity == FeedbackSeverity.INFO) {
            lastStableFeedback = newFeedback
            pendingFeedback = null
            return newFeedback
        }

        // 2. WARNING/ERROR messages need to persist
        if (pendingFeedback?.text == newFeedback.text) {
            // Same warning is persisting
            if (now - pendingFeedbackStartTime >= FEEDBACK_DEBOUNCE_MS) {
                // Threshold reached, accept as new stable
                lastStableFeedback = newFeedback
                return newFeedback
            } else {
                // Threshold not reached, return previous stable state
                return lastStableFeedback
            }
        } else {
            // New warning detected (different from pending)
            pendingFeedback = newFeedback
            pendingFeedbackStartTime = now
            // Return previous stable state while we wait
            return lastStableFeedback
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
                        squatRepCount++
                        currentFeedback = FeedbackMessage("Rep $squatRepCount!", FeedbackSeverity.INFO)
                        Log.d("PoseEvaluator", "Squat Rep Count: $squatRepCount")
                    } else {
                        insufficientDepthSquats++
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
        // TODO (Future Enhancement): Multi-angle camera support
        // Current implementation is optimized for SIDE VIEW (90° to body)
        // This provides best visibility of:
        // - Body alignment (shoulder-hip-ankle line)
        // - Elbow flexion angle
        // - Hip sag/pike detection
        //
        // Future camera angles to support:
        // - ANGLED VIEW (45°): Requires adjusted thresholds, can still work
        // - FRONT VIEW (0°): Good for elbow symmetry, poor for body alignment
        // - TOP-DOWN VIEW: Not recommended, can't measure critical angles
        //
        // Detection approach for future multi-angle:
        // 1. Calculate shoulder width (distance between LEFT_SHOULDER and RIGHT_SHOULDER)
        // 2. If width > threshold → front view, adjust evaluation logic
        // 3. If width < threshold → side view, use current logic
        // 4. Compare visibility scores to determine which side (left/right) to use
        
        // ============================================================================
        // STEP 1: LANDMARK RETRIEVAL
        // ============================================================================
        // Calculate elbow angles (both sides for redundancy, will average)
        val leftElbowAngle = poseResult.calculateAngle(
            PoseLandmarkIndex.LEFT_SHOULDER,
            PoseLandmarkIndex.LEFT_ELBOW,
            PoseLandmarkIndex.LEFT_WRIST
        )
        val rightElbowAngle = poseResult.calculateAngle(
            PoseLandmarkIndex.RIGHT_SHOULDER,
            PoseLandmarkIndex.RIGHT_ELBOW,
            PoseLandmarkIndex.RIGHT_WRIST
        )
        
        // Get landmarks for body alignment check (shoulder-hip-ankle should form a straight line)
        val leftShoulder = poseResult.getLandmark(PoseLandmarkIndex.LEFT_SHOULDER)
        val leftHip = poseResult.getLandmark(PoseLandmarkIndex.LEFT_HIP)
        val leftAnkle = poseResult.getLandmark(PoseLandmarkIndex.LEFT_ANKLE)
        val rightShoulder = poseResult.getLandmark(PoseLandmarkIndex.RIGHT_SHOULDER)
        val rightHip = poseResult.getLandmark(PoseLandmarkIndex.RIGHT_HIP)
        val rightAnkle = poseResult.getLandmark(PoseLandmarkIndex.RIGHT_ANKLE)
        
        // Calculate body alignment angle (should be ~180° for straight plank)
        val leftBodyAlignmentAngle = poseResult.calculateAngle(
            PoseLandmarkIndex.LEFT_SHOULDER,
            PoseLandmarkIndex.LEFT_HIP,
            PoseLandmarkIndex.LEFT_ANKLE
        )
        val rightBodyAlignmentAngle = poseResult.calculateAngle(
            PoseLandmarkIndex.RIGHT_SHOULDER,
            PoseLandmarkIndex.RIGHT_HIP,
            PoseLandmarkIndex.RIGHT_ANKLE
        )
        
        // ============================================================================
        // STEP 2: LANDMARK VALIDATION
        // ============================================================================
        // Handle missing critical landmarks
        if (leftElbowAngle == null && rightElbowAngle == null) {
            return FeedbackMessage(
                text = "Adjust camera to see your shoulders, elbows, and wrists.",
                severity = FeedbackSeverity.WARNING
            )
        }
        
        if (leftBodyAlignmentAngle == null && rightBodyAlignmentAngle == null) {
            return FeedbackMessage(
                text = "Position camera to see your full body from the side.",
                severity = FeedbackSeverity.WARNING
            )
        }
        
        // ============================================================================
        // STEP 3: ANGLE CALCULATIONS
        // ============================================================================
        // Use the best available elbow angle (prefer left, fallback to right, or average both)
        val avgElbowAngle = when {
            leftElbowAngle != null && rightElbowAngle != null -> (leftElbowAngle + rightElbowAngle) / 2
            leftElbowAngle != null -> leftElbowAngle
            rightElbowAngle != null -> rightElbowAngle
            else -> return FeedbackMessage("Cannot detect elbow position.", FeedbackSeverity.WARNING)
        }
        
        // Use the best available body alignment angle
        val avgBodyAlignmentAngle = when {
            leftBodyAlignmentAngle != null && rightBodyAlignmentAngle != null -> 
                (leftBodyAlignmentAngle + rightBodyAlignmentAngle) / 2
            leftBodyAlignmentAngle != null -> leftBodyAlignmentAngle
            rightBodyAlignmentAngle != null -> rightBodyAlignmentAngle
            else -> 180f // Default to straight if we can't measure (won't trigger warnings)
        }
        
        // ============================================================================
        // STEP 4: REP COUNTING STATE MACHINE
        // ============================================================================
        var currentFeedback: FeedbackMessage? = null
        
        when (pushupState) {
            PushupState.UP -> {
                // Detect transition to DOWN state (elbows start bending)
                if (avgElbowAngle < AngleThresholds.PUSHUP_ELBOW_TRANSITION_DOWN) {
                    pushupState = PushupState.DOWN
                    minElbowAngleAchieved = avgElbowAngle // Start tracking depth for this rep
                    currentFeedback = FeedbackMessage("Keep going down!", FeedbackSeverity.INFO)
                }
            }
            PushupState.DOWN -> {
                // Continuously track the minimum elbow angle achieved during descent
                minElbowAngleAchieved = kotlin.math.min(minElbowAngleAchieved, avgElbowAngle)
                
                // Detect transition to UP state (elbows start extending)
                if (avgElbowAngle > AngleThresholds.PUSHUP_ELBOW_TRANSITION_UP) {
                    // Rep completed - check if depth was sufficient
                    if (minElbowAngleAchieved < AngleThresholds.PUSHUP_ELBOW_MIN_DEPTH) {
                        pushupRepCount++
                        currentFeedback = FeedbackMessage("Rep $pushupRepCount!", FeedbackSeverity.INFO)
                        Log.d("PoseEvaluator", "Push-up Rep Count: $pushupRepCount")
                    } else {
                        insufficientDepthPushups++
                        currentFeedback = FeedbackMessage("Lower your chest more on the next rep!", FeedbackSeverity.WARNING)
                    }
                    pushupState = PushupState.UP
                    minElbowAngleAchieved = 180f // Reset for next rep
                } else if (avgElbowAngle < AngleThresholds.PUSHUP_ELBOW_MIN_DEPTH) {
                    // Good depth achieved during this rep
                    currentFeedback = FeedbackMessage("Good depth!", FeedbackSeverity.INFO)
                }
            }
        }
        
        // ============================================================================
        // STEP 5: FORM VALIDATION (prioritize critical issues)
        // ============================================================================
        // Only override rep counting feedback if there's a form issue
        if (currentFeedback == null || currentFeedback.severity != FeedbackSeverity.ERROR) {
            
            // Check body alignment - detect hip sag or pike
            // A straight body should be ~180°. Deviation indicates sag (<165°) or pike (stays at 180° but hips rise)
            val bodyAlignmentDeviation = 180f - avgBodyAlignmentAngle
            
            if (bodyAlignmentDeviation > AngleThresholds.PUSHUP_BODY_ALIGNMENT_MAX) {
                // Hip sag detected (body angle is too small)
                currentFeedback = FeedbackMessage(
                    "Hips sagging! Engage your core and keep body straight.",
                    FeedbackSeverity.ERROR
                )
            } else if (bodyAlignmentDeviation < -AngleThresholds.PUSHUP_BODY_ALIGNMENT_MAX) {
                // Hip pike detected (hips too high, body angle is too large)
                currentFeedback = FeedbackMessage(
                    "Hips too high! Lower them to form a straight line.",
                    FeedbackSeverity.WARNING
                )
            }
            
            // TODO (Future Enhancement): Add elbow flare detection
            // Calculate angle between upper arm and torso (should be ~45°, not 90°)
            // Requires: shoulder-elbow-hip angle measurement
            // If angle > 75° → elbows flared out, warn user
            
            // TODO (Future Enhancement): Add head/neck alignment check
            // Ensure neck is neutral (not looking up or down excessively)
            // Requires: measuring angle between shoulder-ear-nose landmarks
        }
        
        // ============================================================================
        // STEP 6: DEFAULT FEEDBACK (if no specific issues detected)
        // ============================================================================
        return currentFeedback ?: when (pushupState) {
            PushupState.UP -> FeedbackMessage("Ready for push-up. Lower your body.", FeedbackSeverity.INFO)
            PushupState.DOWN -> FeedbackMessage("Hold the position.", FeedbackSeverity.INFO)
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
    
    override fun evaluatePlank(poseResult: PoseResult): FeedbackMessage? {
        // ============================================================================
        // PLANK EVALUATION - Optimized for SIDE VIEW
        // ============================================================================
        // Planks are isometric holds, so we focus on:
        // 1. Body alignment (shoulder-hip-ankle should form ~180° line)
        // 2. Hip position (not sagging or piking)
        // 3. Hold duration tracking with good form
        //
        // Unlike reps-based exercises, planks track time spent in good form
        // ============================================================================
        
        // ============================================================================
        // STEP 1: LANDMARK RETRIEVAL & ANGLE CALCULATIONS
        // ============================================================================
        // Calculate body alignment angle from both sides
        val leftBodyAlignmentAngle = poseResult.calculateAngle(
            PoseLandmarkIndex.LEFT_SHOULDER,
            PoseLandmarkIndex.LEFT_HIP,
            PoseLandmarkIndex.LEFT_ANKLE
        )
        val rightBodyAlignmentAngle = poseResult.calculateAngle(
            PoseLandmarkIndex.RIGHT_SHOULDER,
            PoseLandmarkIndex.RIGHT_HIP,
            PoseLandmarkIndex.RIGHT_ANKLE
        )
        
        // ============================================================================
        // STEP 2: LANDMARK VALIDATION
        // ============================================================================
        if (leftBodyAlignmentAngle == null && rightBodyAlignmentAngle == null) {
            // Can't see the critical landmarks for plank evaluation
            return FeedbackMessage(
                text = "Position camera to see your full body from the side.",
                severity = FeedbackSeverity.WARNING
            )
        }
        
        // ============================================================================
        // STEP 3: USE BEST AVAILABLE ANGLE
        // ============================================================================
        val avgBodyAlignmentAngle = when {
            leftBodyAlignmentAngle != null && rightBodyAlignmentAngle != null -> 
                (leftBodyAlignmentAngle + rightBodyAlignmentAngle) / 2
            leftBodyAlignmentAngle != null -> leftBodyAlignmentAngle
            rightBodyAlignmentAngle != null -> rightBodyAlignmentAngle
            else -> return FeedbackMessage("Cannot detect body alignment.", FeedbackSeverity.WARNING)
        }
        
        // ============================================================================
        // STEP 4: FORM VALIDATION
        // ============================================================================
        val currentTime = System.currentTimeMillis()
        var currentFeedback: FeedbackMessage? = null
        
        // Check if form is good (body is straight, not sagging or piking)
        val isFormGood = avgBodyAlignmentAngle >= AngleThresholds.PLANK_HIP_SAG_THRESHOLD &&
                         avgBodyAlignmentAngle <= AngleThresholds.PLANK_HIP_PIKE_THRESHOLD
        
        // Detect specific form issues
        if (avgBodyAlignmentAngle < AngleThresholds.PLANK_HIP_SAG_THRESHOLD) {
            // Hips are sagging (body angle too small)
            currentFeedback = FeedbackMessage(
                "Hips sagging! Engage your core and lift hips.",
                FeedbackSeverity.ERROR
            )
        } else if (avgBodyAlignmentAngle > AngleThresholds.PLANK_HIP_PIKE_THRESHOLD) {
            // Hips are too high/piked (body angle too large)
            currentFeedback = FeedbackMessage(
                "Hips too high! Lower them to form a straight line.",
                FeedbackSeverity.WARNING
            )
        }
        
        // ============================================================================
        // STEP 5: STATE MACHINE & TIME TRACKING
        // ============================================================================
        when (plankState) {
            PlankState.NOT_IN_POSITION -> {
                if (isFormGood) {
                    // Transitioning to good form
                    plankState = PlankState.HOLDING
                    plankHoldStartTime = currentTime
                    currentFeedback = FeedbackMessage("Good form! Hold this position.", FeedbackSeverity.INFO)
                } else {
                    // Still not in position
                    if (currentFeedback == null) {
                        currentFeedback = FeedbackMessage(
                            "Get into plank position: body straight, elbows under shoulders.",
                            FeedbackSeverity.INFO
                        )
                    }
                }
            }
            PlankState.HOLDING -> {
                if (isFormGood) {
                    // Continue holding with good form
                    val holdDurationMs = currentTime - plankHoldStartTime
                    
                    // Only start counting after grace period (avoid counting brief touches)
                    if (holdDurationMs >= AngleThresholds.PLANK_GOOD_FORM_GRACE_PERIOD_MS) {
                        plankTotalGoodFormTimeMs += (currentTime - plankHoldStartTime)
                        plankHoldStartTime = currentTime // Reset for next accumulation
                    }
                    
                    if (currentFeedback == null) {
                        // Calculate total accumulated time including current hold
                        val totalTimeMs = plankTotalGoodFormTimeMs + holdDurationMs
                        val totalTimeSec = TimeUnit.MILLISECONDS.toSeconds(totalTimeMs)
                        
                        currentFeedback = FeedbackMessage(
                            "Holding: ${totalTimeSec}s. Keep it steady!",
                            FeedbackSeverity.INFO
                        )
                    }
                } else {
                    // Form broke while holding
                    plankState = PlankState.FORM_BROKEN
                    plankFormBreakCount++
                    
                    // Add accumulated time from this hold (if it was long enough)
                    val holdDurationMs = currentTime - plankHoldStartTime
                    if (holdDurationMs >= AngleThresholds.PLANK_GOOD_FORM_GRACE_PERIOD_MS) {
                        plankTotalGoodFormTimeMs += holdDurationMs
                    }
                    
                    // currentFeedback already set by form validation above
                }
            }
            PlankState.FORM_BROKEN -> {
                if (isFormGood) {
                    // Recovered form, start holding again
                    plankState = PlankState.HOLDING
                    plankHoldStartTime = currentTime
                    currentFeedback = FeedbackMessage("Form recovered! Keep holding.", FeedbackSeverity.INFO)
                } else {
                    // Still broken, currentFeedback already set by form validation
                }
            }
        }
        
        // ============================================================================
        // STEP 6: RETURN FEEDBACK
        // ============================================================================
        return currentFeedback ?: FeedbackMessage("Hold the plank position.", FeedbackSeverity.INFO)
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
        // Reset squat state
        squatRepCount = 0
        squatState = SquatState.UP
        minSquatAngleAchieved = 180f
        insufficientDepthSquats = 0
        
        // Reset push-up state
        pushupRepCount = 0
        pushupState = PushupState.UP
        minElbowAngleAchieved = 180f
        insufficientDepthPushups = 0
        
        // Reset plank state
        plankState = PlankState.NOT_IN_POSITION
        plankHoldStartTime = 0L
        plankTotalGoodFormTimeMs = 0L
        plankFormBreakCount = 0
        
        // Reset session tracking
        sessionStartTime = 0L
        currentExerciseType = "squat"

        // Reset feedback filter
        lastStableFeedback = null
        pendingFeedback = null
        pendingFeedbackStartTime = 0L
    }
    
    override fun getRepCount(): Int {
        // Return rep count based on current exercise type
        return when (currentExerciseType.lowercase()) {
            "pushup", "push-up", "push_up" -> pushupRepCount
            "squat" -> squatRepCount
            else -> 0
        }
    }

    override fun getFormBreakCount(): Int {
        return plankFormBreakCount
    }
    
    override fun getMetrics(): Map<String, Any> {
        // TODO (Student 3): Return useful metrics
        return emptyMap()
    }

    override fun startSession() {
        sessionStartTime = System.currentTimeMillis()
    }

    override fun getEvaluationSummary(exerciseType: String): String? {
        if (sessionStartTime == 0L) {
            return null // Session never started
        }

        val durationMillis = System.currentTimeMillis() - sessionStartTime
        val durationSeconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis)

        val summary = StringBuilder()
        
        // Determine which exercise metrics to show based on requested exercise type
        when (exerciseType.lowercase()) {
            "pushup", "push-up", "push_up" -> {
                summary.append("Push-up Session Summary:\n")
                summary.append("Total Reps: $pushupRepCount\n")
                summary.append("Duration: $durationSeconds seconds\n")

                if (insufficientDepthPushups > 0) {
                    summary.append("\nNotes:\n")
                    summary.append("- You had $insufficientDepthPushups push-ups with insufficient depth. Lower your chest closer to the ground!\n")
                } else if (pushupRepCount > 0) {
                    summary.append("\nGreat work! All your push-ups had good depth.\n")
                } else {
                    summary.append("\nNo push-ups were completed in this session.\n")
                }
            }
            "squat" -> {
                summary.append("Squat Session Summary:\n")
                summary.append("Total Reps: $squatRepCount\n")
                summary.append("Duration: $durationSeconds seconds\n")

                if (insufficientDepthSquats > 0) {
                    summary.append("\nNotes:\n")
                    summary.append("- You had $insufficientDepthSquats squats with insufficient depth. Try to go lower next time!\n")
                } else if (squatRepCount > 0) {
                    summary.append("\nGreat work! All your squats had good depth.\n")
                } else {
                    summary.append("\nNo squats were completed in this session.\n")
                }
            }
            "plank" -> {
                // Add any remaining hold time if still in HOLDING state
                var totalGoodFormTime = plankTotalGoodFormTimeMs
                if (plankState == PlankState.HOLDING) {
                    val currentHoldDuration = System.currentTimeMillis() - plankHoldStartTime
                    if (currentHoldDuration >= AngleThresholds.PLANK_GOOD_FORM_GRACE_PERIOD_MS) {
                        totalGoodFormTime += currentHoldDuration
                    }
                }
                
                val totalGoodFormSeconds = TimeUnit.MILLISECONDS.toSeconds(totalGoodFormTime)
                
                summary.append("Plank Session Summary:\n")
                summary.append("Total Good Form Time: ${totalGoodFormSeconds}s\n")
                summary.append("Session Duration: $durationSeconds seconds\n")

                if (plankFormBreakCount > 0) {
                    summary.append("\nNotes:\n")
                    summary.append("- Form broke $plankFormBreakCount time(s). Focus on keeping your body straight throughout the hold!\n")
                } else if (totalGoodFormSeconds > 0) {
                    summary.append("\nExcellent! You maintained good form throughout the plank.\n")
                } else {
                    summary.append("\nNo plank hold was completed with good form in this session.\n")
                }
            }
            else -> {
                summary.append("Session Summary:\n")
                summary.append("Duration: $durationSeconds seconds\n")
                summary.append("\nNo reps were completed in this session.\n")
            }
        }

        return summary.toString()
    }

    override fun getOverallSessionSummary(): String? {
        // Since this evaluator resets between exercises, it mainly tracks the current active session.
        // For a multi-exercise summary, we'd need to persist state across resets or rely on the ViewModel.
        // Returning a simple aggregation of current internal state for now.
        val sb = StringBuilder()
        if (squatRepCount > 0) sb.append("Squats: $squatRepCount\n")
        if (pushupRepCount > 0) sb.append("Push-ups: $pushupRepCount\n")
        
        return if (sb.isNotEmpty()) sb.toString() else null
    }

    private enum class SquatState { UP, DOWN }
    private enum class PushupState { UP, DOWN }
    private enum class PlankState { NOT_IN_POSITION, HOLDING, FORM_BROKEN }

    private object AngleThresholds {
        // Squat thresholds
        const val SQUAT_KNEE_TRANSITION_DOWN = 150f // Angle to detect start of squat (from UP to DOWN)
        const val SQUAT_KNEE_TRANSITION_UP = 160f   // Angle to detect end of squat (from DOWN to UP)
        const val SQUAT_KNEE_MIN_DEPTH = 115f       // Target angle for full squat depth (Relaxed from 90f)
        
        // Push-up thresholds
        const val PUSHUP_ELBOW_TRANSITION_DOWN = 150f  // Angle to detect start of descent
        const val PUSHUP_ELBOW_TRANSITION_UP = 160f    // Angle to detect start of ascent
        const val PUSHUP_ELBOW_MIN_DEPTH = 90f         // Target elbow angle at bottom
        const val PUSHUP_BODY_ALIGNMENT_MAX = 15f      // Max deviation from straight line (degrees)
        
        // Plank thresholds
        const val PLANK_BODY_ALIGNMENT_MIN = 165f  // Minimum body angle (shoulder-hip-ankle)
        const val PLANK_BODY_ALIGNMENT_MAX = 195f  // Maximum body angle (to detect pike)
        const val PLANK_HIP_SAG_THRESHOLD = 165f   // Below this angle = hips sagging
        const val PLANK_HIP_PIKE_THRESHOLD = 185f  // Above this angle = hips too high
        const val PLANK_GOOD_FORM_GRACE_PERIOD_MS = 500L // Time to stabilize form before counting
    }

    private object RelativePositionThresholds {
        const val KNEE_TO_TOE_OFFSET = 0.05f // How far knee X can be from ankle X before warning
        const val HIP_TO_KNEE_OFFSET = 0.05f // How far hip X can be from knee X before warning for back straightness
    }
}
