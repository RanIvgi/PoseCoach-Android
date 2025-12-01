package com.example.posecoach.data

/**
 * Utility object for calculating scores based on feedback messages.
 * Provides consistent scoring logic across different result types.
 */
object ScoreCalculator {
    /**
     * Calculate overall score based on feedback severity.
     * More INFO messages = higher score
     * More WARNING/ERROR messages = lower score
     * 
     * @param feedbackMessages List of feedback messages to evaluate
     * @param defaultScore Score to return when feedbackMessages is empty (default: 0)
     * @return Score between 0-100
     */
    fun calculateScore(
        feedbackMessages: List<FeedbackMessage>,
        defaultScore: Int = 0
    ): Int {
        if (feedbackMessages.isEmpty()) return defaultScore
        
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
