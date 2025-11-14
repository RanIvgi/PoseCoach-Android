package com.example.posecoach.data

/**
 * Complete result from pose detection for a single frame.
 * This is passed from Student 2's PoseEngine to Student 3's evaluation logic.
 * 
 * @property landmarks List of 33 pose landmarks (or empty if no pose detected)
 * @property timestamp Frame timestamp in milliseconds
 * @property imageWidth Original image width in pixels
 * @property imageHeight Original image height in pixels
 * @property isFrontCamera Whether this frame is from the front camera
 */
data class PoseResult(
    val landmarks: List<PoseLandmark>,
    val timestamp: Long = System.currentTimeMillis(),
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val isFrontCamera: Boolean = true
) {
    /**
     * Check if a valid pose was detected.
     */
    fun hasPose(): Boolean = landmarks.isNotEmpty()
    
    /**
     * Get a landmark by its index safely.
     * Returns null if index is out of bounds or landmark is unreliable.
     */
    fun getLandmark(index: Int, reliabilityThreshold: Float = 0.5f): PoseLandmark? {
        return landmarks.getOrNull(index)?.takeIf { it.isReliable(reliabilityThreshold) }
    }
    
    /**
     * Calculate angle between three landmarks (in degrees).
     * Useful for measuring joint angles like elbow, knee, hip, etc.
     * 
     * @param pointA First landmark (e.g., shoulder)
     * @param pointB Middle landmark/joint (e.g., elbow)
     * @param pointC Third landmark (e.g., wrist)
     * @return Angle in degrees (0-180), or null if any landmark is missing
     */
    fun calculateAngle(pointA: Int, pointB: Int, pointC: Int): Float? {
        val a = getLandmark(pointA) ?: return null
        val b = getLandmark(pointB) ?: return null
        val c = getLandmark(pointC) ?: return null
        
        // Calculate vectors
        val ba = Pair(a.x - b.x, a.y - b.y)
        val bc = Pair(c.x - b.x, c.y - b.y)
        
        // Calculate dot product and magnitudes
        val dotProduct = ba.first * bc.first + ba.second * bc.second
        val magnitudeBA = kotlin.math.sqrt(ba.first * ba.first + ba.second * ba.second)
        val magnitudeBC = kotlin.math.sqrt(bc.first * bc.first + bc.second * bc.second)
        
        // Calculate angle in radians then convert to degrees
        val angleRadians = kotlin.math.acos(dotProduct / (magnitudeBA * magnitudeBC))
        return Math.toDegrees(angleRadians.toDouble()).toFloat()
    }
    
    /**
     * Calculate distance between two landmarks.
     * Returns normalized distance (0.0 to ~1.4 for diagonal of frame).
     */
    fun calculateDistance(pointA: Int, pointB: Int): Float? {
        val a = getLandmark(pointA) ?: return null
        val b = getLandmark(pointB) ?: return null
        
        val dx = a.x - b.x
        val dy = a.y - b.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}
