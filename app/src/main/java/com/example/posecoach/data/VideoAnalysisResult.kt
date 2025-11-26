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
    /**
     * Calculate overall score based on feedback severity.
     * More INFO messages = higher score
     * More WARNING/ERROR messages = lower score
     */
    companion object {
        fun calculateScore(feedbackMessages: List<FeedbackMessage>): Int {
            if (feedbackMessages.isEmpty()) return 0
            
            val infoCount = feedbackMessages.count { it.severity == FeedbackSeverity.INFO }
            val warningCount = feedbackMessages.count { it.severity == FeedbackSeverity.WARNING }
            val errorCount = feedbackMessages.count { it.severity == FeedbackSeverity.ERROR }
            
            val totalMessages = feedbackMessages.size
            
            // Score calculation: INFO adds points, WARNING/ERROR subtracts
            val rawScore = ((infoCount * 100.0 - warningCount * 50.0 - errorCount * 75.0) / totalMessages).toInt()
            
            // Clamp between 0 and 100
            return rawScore.coerceIn(0, 100)
        }
    }
}
