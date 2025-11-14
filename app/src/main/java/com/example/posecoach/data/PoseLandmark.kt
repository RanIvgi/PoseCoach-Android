package com.example.posecoach.data

/**
 * Represents a single pose landmark point detected by MediaPipe.
 * 
 * MediaPipe detects 33 landmarks on the human body including:
 * - Face (nose, eyes, ears, mouth)
 * - Upper body (shoulders, elbows, wrists)
 * - Torso (hips)
 * - Lower body (knees, ankles, feet)
 * 
 * @property x Normalized x-coordinate (0.0 to 1.0, relative to image width)
 * @property y Normalized y-coordinate (0.0 to 1.0, relative to image height)
 * @property z Depth coordinate (roughly in meters from the camera)
 * @property visibility Confidence score (0.0 to 1.0) that this landmark is visible
 * @property presence Confidence score (0.0 to 1.0) that this landmark is present in the frame
 */
data class PoseLandmark(
    val x: Float,
    val y: Float,
    val z: Float,
    val visibility: Float = 1.0f,
    val presence: Float = 1.0f
) {
    /**
     * Check if this landmark is reliable enough to use.
     * A landmark should have high visibility and presence scores.
     */
    fun isReliable(threshold: Float = 0.5f): Boolean {
        return visibility >= threshold && presence >= threshold
    }
}

/**
 * MediaPipe Pose Landmark indices.
 * Use these constants to access specific body parts from the landmarks list.
 */
object PoseLandmarkIndex {
    // Face
    const val NOSE = 0
    const val LEFT_EYE_INNER = 1
    const val LEFT_EYE = 2
    const val LEFT_EYE_OUTER = 3
    const val RIGHT_EYE_INNER = 4
    const val RIGHT_EYE = 5
    const val RIGHT_EYE_OUTER = 6
    const val LEFT_EAR = 7
    const val RIGHT_EAR = 8
    const val MOUTH_LEFT = 9
    const val MOUTH_RIGHT = 10
    
    // Upper Body
    const val LEFT_SHOULDER = 11
    const val RIGHT_SHOULDER = 12
    const val LEFT_ELBOW = 13
    const val RIGHT_ELBOW = 14
    const val LEFT_WRIST = 15
    const val RIGHT_WRIST = 16
    
    // Hands
    const val LEFT_PINKY = 17
    const val RIGHT_PINKY = 18
    const val LEFT_INDEX = 19
    const val RIGHT_INDEX = 20
    const val LEFT_THUMB = 21
    const val RIGHT_THUMB = 22
    
    // Lower Body
    const val LEFT_HIP = 23
    const val RIGHT_HIP = 24
    const val LEFT_KNEE = 25
    const val RIGHT_KNEE = 26
    const val LEFT_ANKLE = 27
    const val RIGHT_ANKLE = 28
    const val LEFT_HEEL = 29
    const val RIGHT_HEEL = 30
    const val LEFT_FOOT_INDEX = 31
    const val RIGHT_FOOT_INDEX = 32
}
