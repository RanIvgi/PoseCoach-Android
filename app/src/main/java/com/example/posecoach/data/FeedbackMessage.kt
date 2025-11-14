package com.example.posecoach.data

/**
 * Feedback message from Student 3's evaluation logic to Student 1's UI.
 * This is what gets displayed to the user during their workout.
 * 
 * @property text The feedback message (e.g., "Bend knees more", "Good form!")
 * @property severity How critical this feedback is (info, warning, error)
 * @property timestamp When this feedback was generated
 */
data class FeedbackMessage(
    val text: String,
    val severity: FeedbackSeverity = FeedbackSeverity.INFO,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Severity levels for feedback messages.
 * Student 1 can use this to color-code the feedback in the UI.
 */
enum class FeedbackSeverity {
    /**
     * Positive feedback or general information (green)
     * Example: "Good form!", "Well done!", "Keep it up!"
     */
    INFO,
    
    /**
     * Minor form issue that should be corrected (yellow/orange)
     * Example: "Bend knees slightly more", "Keep your back straighter"
     */
    WARNING,
    
    /**
     * Significant form issue that could cause injury (red)
     * Example: "Knees too far forward!", "Back not straight - stop!"
     */
    ERROR
}
