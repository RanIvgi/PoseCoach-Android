package com.example.posecoach.logic

import com.example.posecoach.data.FeedbackMessage
import com.example.posecoach.data.PoseResult

/**
 * Interface for Student 3 - Pose Evaluation Logic.
 * 
 * This interface defines how pose evaluation should work.
 * Student 3 will implement this interface to provide real-time feedback
 * on exercise form by analyzing joint angles and body positioning.
 * 
 * The evaluator receives PoseResult from Student 2's PoseEngine
 * and returns FeedbackMessage to Student 1's UI.
 */
interface PoseEvaluator {
    
    /**
     * Evaluate the current pose and return feedback.
     * This is the main method called for each frame.
     * 
     * @param poseResult The detected pose with all landmarks
     * @param exerciseType The type of exercise being performed (e.g., "squat", "pushup")
     * @return Feedback message to display to the user, or null if no feedback needed
     */
    fun evaluate(poseResult: PoseResult, exerciseType: String = "squat"): FeedbackMessage?
    
    /**
     * Evaluate squat form specifically.
     * 
     * Key checks for squats:
     * - Knee angle (should reach ~90 degrees at bottom)
     * - Hip angle (hips should move back and down)
     * - Back angle (should stay relatively straight)
     * - Knee alignment (knees shouldn't go past toes)
     * 
     * @param poseResult The detected pose
     * @return Feedback about squat form
     */
    fun evaluateSquat(poseResult: PoseResult): FeedbackMessage?
    
    /**
     * Evaluate push-up form specifically.
     * 
     * Key checks for push-ups:
     * - Elbow angle (should reach ~90 degrees at bottom)
     * - Body alignment (shoulders, hips, ankles should be in a line)
     * - Elbow position (elbows should be at ~45 degrees from body)
     * - Head position (neutral neck, not looking up or down)
     * 
     * @param poseResult The detected pose
     * @return Feedback about push-up form
     */
    fun evaluatePushup(poseResult: PoseResult): FeedbackMessage?
    
    /**
     * Evaluate lunge form specifically.
     * 
     * Key checks for lunges:
     * - Front knee angle (should reach ~90 degrees)
     * - Back knee angle (should reach ~90 degrees)
     * - Front knee alignment (shouldn't go past front toe)
     * - Torso alignment (should stay upright)
     * 
     * @param poseResult The detected pose
     * @return Feedback about lunge form
     */
    fun evaluateLunge(poseResult: PoseResult): FeedbackMessage?
    
    /**
     * Reset any internal state (e.g., rep counters, movement tracking).
     * Called when starting a new exercise session.
     */
    fun reset()
    
    /**
     * Get current rep count if tracking is enabled.
     * Student 3 can implement rep counting logic here.
     * 
     * @return Number of reps completed, or null if not tracking
     */
    fun getRepCount(): Int
    
    /**
     * Get exercise-specific metrics (e.g., max depth, average speed).
     * Student 3 can return custom metrics here for display.
     * 
     * @return Map of metric names to values
     */
    fun getMetrics(): Map<String, Any> = emptyMap()

    /**
     * Signals the start of an exercise session to begin tracking metrics like time.
     */
    fun startSession()

    /**
     * Generates a summary of the completed exercise session.
     *
     * @return A formatted string with session metrics, or null if no session was active.
     */
    fun getEvaluationSummary(): String?
}

/**
 * Helper object with common angle thresholds for different exercises.
 * Student 3 can adjust these values based on testing.
 */
object AngleThresholds {
    // Squat thresholds
    const val SQUAT_KNEE_MIN = 70f  // Minimum knee angle at bottom (degrees)
    const val SQUAT_KNEE_MAX = 110f // Maximum knee angle at bottom
    const val SQUAT_HIP_MIN = 60f   // Minimum hip angle at bottom
    
    // Push-up thresholds
    const val PUSHUP_ELBOW_MIN = 70f  // Minimum elbow angle at bottom
    const val PUSHUP_ELBOW_MAX = 110f // Maximum elbow angle at bottom
    
    // Lunge thresholds
    const val LUNGE_FRONT_KNEE_MIN = 70f  // Minimum front knee angle
    const val LUNGE_FRONT_KNEE_MAX = 110f // Maximum front knee angle
    const val LUNGE_BACK_KNEE_MIN = 70f   // Minimum back knee angle
    const val LUNGE_BACK_KNEE_MAX = 110f  // Maximum back knee angle
    
    // General thresholds
    const val LANDMARK_VISIBILITY_THRESHOLD = 0.5f // Minimum visibility to trust a landmark
}
