package com.example.posecoach.data

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Data class containing performance metrics for a test session.
 * Used for exporting performance data to JSON for analysis.
 */
data class PerformanceMetrics(
    // Test metadata
    val testNumber: Int = 0,
    val deviceModel: String,
    val androidVersion: String,
    val modelType: PoseModel,
    val exerciseType: String,
    val delegate: String,
    val timestamp: Long,
    val durationSeconds: Int,
    
    // FPS metrics
    val fpsAverage: Float,
    val fpsMin: Float,
    val fpsMax: Float,
    val fpsStdDev: Float,
    val frameCount: Int,
    
    // Processing time metrics (milliseconds)
    val inferenceTimeAvgMs: Float,
    val inferenceTimeMinMs: Float,
    val inferenceTimeMaxMs: Float,
    val bitmapConversionAvgMs: Float,
    val rotationAvgMs: Float,
    val totalProcessingAvgMs: Float,
    
    // Detection metrics
    val framesWithPose: Int,
    val framesWithoutPose: Int,
    val detectionSuccessRate: Float,
    val avgLandmarkConfidence: Float,
    val avgVisibilityScore: Float,
    
    // Device metrics
    val batteryStart: Int,
    val batteryEnd: Int,
    val batteryDrainPercent: Int,
    val temperatureCelsius: Float = 0f
) {
    /**
     * Convert metrics to JSON format for export.
     */
    fun toJson(): JSONObject {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        
        return JSONObject().apply {
            put("test_metadata", JSONObject().apply {
                put("test_number", testNumber)
                put("device_model", deviceModel)
                put("android_version", androidVersion)
                put("model_type", modelType.name)
                put("exercise_type", exerciseType)
                put("delegate", delegate)
                put("timestamp", dateFormat.format(Date(timestamp)))
                put("duration_seconds", durationSeconds)
            })
            
            put("performance_metrics", JSONObject().apply {
                put("fps_average", fpsAverage.toDouble())
                put("fps_min", fpsMin.toDouble())
                put("fps_max", fpsMax.toDouble())
                put("fps_stddev", fpsStdDev.toDouble())
                put("frame_count", frameCount)
                put("inference_time_avg_ms", inferenceTimeAvgMs.toDouble())
                put("inference_time_min_ms", inferenceTimeMinMs.toDouble())
                put("inference_time_max_ms", inferenceTimeMaxMs.toDouble())
                put("bitmap_conversion_avg_ms", bitmapConversionAvgMs.toDouble())
                put("rotation_avg_ms", rotationAvgMs.toDouble())
                put("total_processing_avg_ms", totalProcessingAvgMs.toDouble())
            })
            
            put("detection_metrics", JSONObject().apply {
                put("frames_with_pose", framesWithPose)
                put("frames_without_pose", framesWithoutPose)
                put("detection_success_rate", detectionSuccessRate.toDouble())
                put("avg_landmark_confidence", avgLandmarkConfidence.toDouble())
                put("avg_visibility_score", avgVisibilityScore.toDouble())
            })
            
            put("device_metrics", JSONObject().apply {
                put("battery_start", batteryStart)
                put("battery_end", batteryEnd)
                put("battery_drain_percent", batteryDrainPercent)
                put("temperature_celsius", temperatureCelsius.toDouble())
            })
        }
    }
    
    /**
     * Generate filename for this test's export.
     * Format: test_1_xiaomimi8_full_squat_2025-12-27_14-30-45.json
     */
    fun generateFilename(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
        val dateTime = dateFormat.format(Date(timestamp))
        val deviceShort = deviceModel.replace(" ", "").toLowerCase(Locale.US)
        val exerciseLower = exerciseType.toLowerCase(Locale.US)
        val modelLower = modelType.name.toLowerCase(Locale.US)
        
        return "test_${testNumber}_${deviceShort}_${modelLower}_${exerciseLower}_${dateTime}.json"
    }
}
