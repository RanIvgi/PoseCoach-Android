package com.example.posecoach.logic

import com.example.posecoach.data.FeedbackMessage
import com.example.posecoach.data.FeedbackSeverity

/**
 * Analyzes and summarizes feedback messages from an exercise session.
 * 
 * This object is responsible for:
 * - Aggregating raw feedback messages into a coherent summary.
 * - Detecting patterns in feedback (e.g., consistent form errors).
 * - Applying scoring penalties based on the frequency and severity of errors.
 * - Generating the final list of feedback messages for the session result.
 */
object FeedbackAnalyzer {

    fun analyze(
        rawFeedback: List<FeedbackMessage>,
        exerciseType: String,
        totalReps: Int,
        goodFormDurationMillis: Long = 0,
        targetDurationMillis: Long? = null,
        formBreakCount: Int = 0
    ): List<FeedbackMessage> {
        val summarizedFeedback = mutableListOf<FeedbackMessage>()
        val feedbackTexts = rawFeedback.map { it.text }

        // 0. Analyze Plank Time (if applicable)
        if (exerciseType.lowercase().contains("plank")) {
            if (targetDurationMillis != null && targetDurationMillis > 0) {
                val timeOutOfPositionMillis = targetDurationMillis - goodFormDurationMillis
                if (timeOutOfPositionMillis > 1000) { // Ignore small differences (< 1s)
                    val secondsLost = timeOutOfPositionMillis / 1000
                    // We calculate points lost for display/info, but we DO NOT apply explicit deduction
                    // because the base score is already calculated from (goodFormDuration / targetDuration).
                    // Applying deduction here would be double-counting the penalty.
                    
                    summarizedFeedback.add(FeedbackMessage(
                        text = "Time out of position: ${secondsLost}s",
                        severity = FeedbackSeverity.WARNING,
                        explicitPointDeduction = 0
                    ))
                }
            }
            
            // Explicitly report form breaks if they occurred
            if (formBreakCount > 0) {
                summarizedFeedback.add(
                    FeedbackMessage(
                        text = "Form broken $formBreakCount times (hips sagged/piked)",
                        severity = FeedbackSeverity.WARNING,
                        explicitPointDeduction = -(formBreakCount * 5)
                    )
                )
            }
        }

        // 1. Analyze Depth (Squat & Pushup)
        // "Good depth!" is emitted continuously but de-duplicated in history, so roughly 1 per good rep.
        // "Lower your chest more on the next rep!" (Pushup) or "Go deeper on the next rep!" (Squat) is emitted once per bad rep.
        val goodDepthCount = feedbackTexts.count { it == "Good depth!" }
        val badDepthCount = feedbackTexts.count { it == "Go deeper on the next rep!" || it == "Lower your chest more on the next rep!" }
        
        // Logic: If we have reps, check depth consistency
        if (totalReps > 0) {
            if (badDepthCount == 0 && goodDepthCount > 0) {
                summarizedFeedback.add(FeedbackMessage("Perfect! You went down deep enough on all reps.", FeedbackSeverity.INFO))
            } else if (badDepthCount > 0 && goodDepthCount > 0) {
                if (badDepthCount > 1) {
                    summarizedFeedback.add(FeedbackMessage(
                        text = "Multiple occurrences: Inconsistent depth. You missed proper depth on several reps. (-10 pts)", 
                        severity = FeedbackSeverity.ERROR,
                        explicitPointDeduction = -10
                    ))
                } else {
                    summarizedFeedback.add(FeedbackMessage(
                        text = "Inconsistent depth. You hit good depth on some reps, but missed others. (-5 pts)", 
                        severity = FeedbackSeverity.WARNING,
                        explicitPointDeduction = -5
                    ))
                }
                summarizedFeedback.add(FeedbackMessage("Good job hitting proper depth on $goodDepthCount reps.", FeedbackSeverity.INFO))
            } else if (badDepthCount > 0 && goodDepthCount == 0) {
                if (badDepthCount > 1) {
                    summarizedFeedback.add(FeedbackMessage(
                        text = "Multiple occurrences: Depth was insufficient on all reps. (-10 pts)", 
                        severity = FeedbackSeverity.ERROR,
                        explicitPointDeduction = -10
                    ))
                } else {
                    summarizedFeedback.add(FeedbackMessage(
                        text = "Depth was insufficient. Try to go lower on every rep. (-5 pts)", 
                        severity = FeedbackSeverity.WARNING,
                        explicitPointDeduction = -5
                    ))
                }
            }
        }

        // 2. Analyze Specific Form Errors
        // We check for specific error strings emitted by DefaultPoseEvaluator
        analyzeError(feedbackTexts, "Knees over toes! Push hips back.", "Knees consistently extended past toes. Try sitting back more.", summarizedFeedback)
        analyzeError(feedbackTexts, "Keep your chest up and back straight.", "Tendency to lean forward. Keep chest lifted.", summarizedFeedback)
        
        // Updated string to match DefaultPoseEvaluator for Pushups
        analyzeError(feedbackTexts, "Hips sagging! Engage your core and keep body straight.", "Core stability issue: Hips were sagging. Engage your abs.", summarizedFeedback)
        // Also check for the old string just in case (or for other exercises if reused)
        analyzeError(feedbackTexts, "Hips sagging! Engage your core and lift hips.", "Core stability issue: Hips were sagging. Engage your abs.", summarizedFeedback)
        
        analyzeError(feedbackTexts, "Hips too high! Lower them to form a straight line.", "Hips were too high. Try to keep a straight body line.", summarizedFeedback)

        // 3. Analyze Positive Form (Absence of errors)
        if (totalReps > 0) {
            analyzePositive(feedbackTexts, "Knees over toes! Push hips back.", "Excellent knee stability! Knees stayed behind toes.", summarizedFeedback)
            analyzePositive(feedbackTexts, "Keep your chest up and back straight.", "Good posture! Back stayed straight.", summarizedFeedback)
            
            if (exerciseType.lowercase().contains("plank") || exerciseType.lowercase().contains("pushup") || exerciseType.lowercase().contains("push-up")) {
                 analyzePositive(feedbackTexts, "Hips sagging! Engage your core and keep body straight.", "Core engaged well! No hip sagging.", summarizedFeedback)
                 analyzePositive(feedbackTexts, "Hips sagging! Engage your core and lift hips.", "Core engaged well! No hip sagging.", summarizedFeedback)
                 analyzePositive(feedbackTexts, "Hips too high! Lower them to form a straight line.", "Good body alignment! Hips stayed in line.", summarizedFeedback)
            }
        }

        // 4. Camera/Detection Issues
        val cameraIssues = feedbackTexts.count { it.contains("Adjust camera") || it.contains("No pose detected") }
        if (cameraIssues > 5) { 
             summarizedFeedback.add(FeedbackMessage(
                 text = "Camera positioning needs adjustment for better detection. (-5 pts)", 
                 severity = FeedbackSeverity.WARNING,
                 explicitPointDeduction = -5
             ))
        }

        // 4. If no specific feedback was generated but reps were done
        if (summarizedFeedback.isEmpty() && totalReps > 0) {
             summarizedFeedback.add(FeedbackMessage("Great form! No major issues detected.", FeedbackSeverity.INFO))
        }
        
        return summarizedFeedback
    }

    private fun analyzeError(
        allTexts: List<String>, 
        rawErrorText: String, 
        summaryText: String, 
        outputList: MutableList<FeedbackMessage>
    ) {
        val count = allTexts.count { it == rawErrorText }
        if (count > 1) { 
            // More than once -> ERROR (-10 pts)
            outputList.add(FeedbackMessage(
                text = "Multiple occurrences: $summaryText (-10 pts)", 
                severity = FeedbackSeverity.ERROR,
                explicitPointDeduction = -10
            ))
        } else if (count == 1) {
             // Once -> WARNING (-5 pts)
             outputList.add(FeedbackMessage(
                text = "Occasional issue: $summaryText (-5 pts)", 
                severity = FeedbackSeverity.WARNING,
                explicitPointDeduction = -5
            ))
        }
    }

    private fun analyzePositive(
        allTexts: List<String>,
        rawErrorText: String,
        positiveText: String,
        outputList: MutableList<FeedbackMessage>
    ) {
        val count = allTexts.count { it == rawErrorText }
        if (count == 0) {
            outputList.add(FeedbackMessage(positiveText, FeedbackSeverity.INFO))
        }
    }
}
