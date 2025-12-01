package com.example.posecoach.data

/**
 * Holds the results of a video analysis session.
 * Contains feedback messages, statistics, and evaluation summary.
 */
data class VideoAnalysisResult(
    val feedbackMessages: List<FeedbackMessage>,
    val repCount: Int,
    val evaluationSummary: String?,
    val exerciseType: String,
    val totalFramesAnalyzed: Int,
    val framesWithPoseDetected: Int,
    val overallScore: Int // 0-100 score based on feedback
) {
    companion object {
        /**
         * Calculate overall score based on feedback severity.
         * More INFO messages = higher score
         * More WARNING/ERROR messages = lower score
         */
        fun calculateScore(feedbackMessages: List<FeedbackMessage>): Int {
            return ScoreCalculator.calculateScore(feedbackMessages, defaultScore = 0)
        }
    }
}
