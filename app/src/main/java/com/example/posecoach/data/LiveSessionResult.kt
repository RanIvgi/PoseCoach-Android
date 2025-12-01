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
            return ScoreCalculator.calculateScore(feedbackMessages, defaultScore = 50)
        }
    }
}
