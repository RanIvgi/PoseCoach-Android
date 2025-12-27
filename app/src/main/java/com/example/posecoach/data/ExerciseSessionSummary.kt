package com.example.posecoach.data

data class ExerciseSessionSummary(
    val exerciseId: String,
    val exerciseName: String,
    val reps: Int,
    val durationMillis: Long,
    val feedbackMessages: List<FeedbackMessage> = emptyList(),
    val formBreakCount: Int = 0,
    val goodFormDurationMillis: Long = 0,
    val targetDurationMillis: Long? = null
)
