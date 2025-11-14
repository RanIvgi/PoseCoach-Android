package com.example.posecoach.data

import androidx.camera.core.CameraSelector

/**
 * Represents the current camera configuration.
 * Used by Student 1's UI to control which camera is active.
 */
sealed class CameraState {
    /**
     * Front-facing camera (selfie camera).
     * Default for most workout scenarios.
     */
    object Front : CameraState()
    
    /**
     * Rear-facing camera (back camera).
     * Can be useful for some exercises or when using a mirror.
     */
    object Rear : CameraState()
    
    /**
     * Toggle between front and rear camera.
     */
    fun toggle(): CameraState = when (this) {
        is Front -> Rear
        is Rear -> Front
    }
    
    /**
     * Check if this is the front camera.
     * Needed for mirroring the pose overlay correctly.
     */
    fun isFront(): Boolean = this is Front
    
    /**
     * Convert to CameraX CameraSelector.
     */
    fun toCameraSelector(): CameraSelector = when (this) {
        is Front -> CameraSelector.DEFAULT_FRONT_CAMERA
        is Rear -> CameraSelector.DEFAULT_BACK_CAMERA
    }
}
