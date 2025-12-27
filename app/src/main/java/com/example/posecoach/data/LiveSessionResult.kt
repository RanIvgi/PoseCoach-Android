package com.example.posecoach.data

/**
 * Holds the results of a live camera session.
 * Similar to VideoAnalysisResult but tailored for live sessions with workout tracking.
 */
data class LiveSessionResult(
    val exerciseType: String,
    val exerciseName: String,
    val targetReps: Int,
    val completedReps: Int,
    val durationMillis: Long,
    val feedbackMessages: List<FeedbackMessage>,
    val evaluationSummary: String?,
    val overallScore: Int, // 0-100 score based on feedback
    val totalExercises: Int,
    val totalReps: Int,
    val totalDurationMillis: Long,
    val commonFeedbackMessages: List<FeedbackMessage> = emptyList(),
    val allFeedbackMessages: List<FeedbackMessage> = emptyList(),
    val sessionHistory: List<ExerciseSessionSummary> = emptyList(),
    val formBreakCount: Int = 0,
    val goodFormDurationMillis: Long = 0,
    val targetDurationMillis: Long? = null
) {
    companion object {
        /**
         * Calculate overall score based on reps completion and feedback severity.
         * Base score = % of target reps completed (max 100)
         * Deductions = Points subtracted for WARNING/ERROR messages
         */
        fun calculateScore(
            feedbackMessages: List<FeedbackMessage>,
            completedReps: Int,
            targetReps: Int,
            exerciseType: String,
            durationMillis: Long = 0,
            targetDurationMillis: Long? = null,
            formBreakCount: Int = 0,
            goodFormDurationMillis: Long = 0
        ): Int {
            // 1. Base Score from Reps or Duration
            var score = if (exerciseType.lowercase().contains("plank")) {
                // For plank, check if we have a target duration
                if (targetDurationMillis != null && targetDurationMillis > 0) {
                    (goodFormDurationMillis.toDouble() / targetDurationMillis.toDouble()) * 100.0
                } else {
                    // No target set? Calculate based on total elapsed time
                    if (durationMillis > 0) {
                        (goodFormDurationMillis.toDouble() / durationMillis.toDouble()) * 100.0
                    } else {
                        0.0
                    }
                }
            } else {
                if (targetReps > 0) {
                    (completedReps.toDouble() / targetReps.toDouble()) * 100.0
                } else {
                    // No target set? Default to 100 if they did at least one rep.
                    if (completedReps > 0) 100.0 else 0.0
                }
            }
            
            // Cap base score at 100 (e.g. if they did bonus reps)
            if (score > 100.0) score = 100.0

            // 2. Deductions from Feedback
            // We iterate through the unique feedback messages to avoid double penalizing for the same recurring issue.
            // If the user got "knees over toes" twice, we only deduct points once.
            val uniqueMessages = feedbackMessages.distinctBy { it.text }
            
            for (msg in uniqueMessages) {
                // Skip specific messages that shouldn't deduct points
                if (msg.text.contains("Camera positioning needs adjustment") || 
                    msg.text.contains("Time out of position") ||
                    msg.text.contains("Form broken")) {
                    continue
                }

                when (msg.severity) {
                    FeedbackSeverity.WARNING -> score -= 5.0 // Deduct 5 points for warnings
                    FeedbackSeverity.ERROR -> score -= 10.0  // Deduct 10 points for errors
                    FeedbackSeverity.INFO -> { /* No change */ }
                }
            }

            // 3. Deductions from Form Breaks (specifically for Plank)
            if (exerciseType.lowercase().contains("plank")) {
                score -= (formBreakCount * 5.0)
            }

            return score.toInt().coerceIn(0, 100)
        }
    }
}
