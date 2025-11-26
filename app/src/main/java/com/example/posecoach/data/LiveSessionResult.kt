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
    val totalDurationMillis: Long
) {
    companion object {
        /**
         * Calculate overall score based on feedback severity.
         * More INFO messages = higher score
         * More WARNING/ERROR messages = lower score
         */
        fun calculateScore(feedbackMessages: List<FeedbackMessage>): Int {
            if (feedbackMessages.isEmpty()) return 50 // Neutral score if no feedback

            val infoCount = feedbackMessages.count { it.severity == FeedbackSeverity.INFO }
            val warningCount = feedbackMessages.count { it.severity == FeedbackSeverity.WARNING }
            val errorCount = feedbackMessages.count { it.severity == FeedbackSeverity.ERROR }

            val totalMessages = feedbackMessages.size

            // Score calculation: INFO adds points, WARNING/ERROR subtracts
            val rawScore =
                ((infoCount * 100.0 - warningCount * 50.0 - errorCount * 75.0) / totalMessages).toInt()

            // Clamp between 0 and 100
            return rawScore.coerceIn(0, 100)
        }
    }
}
