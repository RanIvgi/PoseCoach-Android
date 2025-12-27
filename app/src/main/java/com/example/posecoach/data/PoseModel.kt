package com.example.posecoach.data

/**
 * Enumeration of available MediaPipe Pose Landmarker models.
 * Each model has different performance and accuracy characteristics.
 */
enum class PoseModel(
    val displayName: String,
    val assetPath: String,
    val description: String
) {
    LITE(
        displayName = "Lite",
        assetPath = "pose_landmarker_lite.task",
        description = "Fastest model, lower accuracy. Best for high FPS."
    ),
    FULL(
        displayName = "Full",
        assetPath = "pose_landmarker_full.task",
        description = "Balanced model. Good accuracy and performance."
    ),
    HEAVY(
        displayName = "Heavy",
        assetPath = "pose_landmarker_heavy.task",
        description = "Most accurate model. Slower processing."
    );
    
    companion object {
        fun fromAssetPath(path: String): PoseModel? {
            return values().find { it.assetPath == path }
        }
    }
}
