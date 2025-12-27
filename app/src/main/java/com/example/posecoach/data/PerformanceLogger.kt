package com.example.posecoach.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import kotlin.math.sqrt

/**
 * Collects and tracks performance metrics during an exercise session.
 * Provides methods to export data as JSON for analysis.
 */
class PerformanceLogger(private val context: Context) {
    
    private val _isLogging = MutableStateFlow(false)
    val isLogging: StateFlow<Boolean> = _isLogging.asStateFlow()
    
    // Session metadata
    private var sessionStartTime = 0L
    private var testNumber = 0
    private var exerciseType = ""
    private var modelType: PoseModel = PoseModel.FULL
    private var delegate = "CPU"
    
    // FPS tracking
    private val fpsValues = mutableListOf<Float>()
    private var frameCount = 0
    
    // Timing tracking (milliseconds)
    private val inferenceTimes = mutableListOf<Float>()
    private val bitmapConversionTimes = mutableListOf<Float>()
    private val rotationTimes = mutableListOf<Float>()
    private val totalProcessingTimes = mutableListOf<Float>()
    
    // Detection tracking
    private var framesWithPose = 0
    private var framesWithoutPose = 0
    private val landmarkConfidences = mutableListOf<Float>()
    private val visibilityScores = mutableListOf<Float>()
    
    // Device tracking
    private var batteryStart = 0
    private var batteryEnd = 0
    
    companion object {
        private const val TAG = "PerformanceLogger"
    }
    
    /**
     * Start logging a new test session.
     */
    fun startLogging(
        testNumber: Int,
        exerciseType: String,
        modelType: PoseModel,
        delegate: String
    ) {
        Log.d(TAG, "Starting performance logging for test #$testNumber")
        
        // Clear previous data
        reset()
        
        // Set metadata
        this.testNumber = testNumber
        this.exerciseType = exerciseType
        this.modelType = modelType
        this.delegate = delegate
        
        // Record start time and battery
        sessionStartTime = System.currentTimeMillis()
        batteryStart = getBatteryLevel()
        
        _isLogging.value = true
    }
    
    /**
     * Stop logging and prepare data for export.
     */
    fun stopLogging() {
        Log.d(TAG, "Stopping performance logging")
        
        batteryEnd = getBatteryLevel()
        _isLogging.value = false
    }
    
    /**
     * Log FPS value.
     */
    fun logFps(fps: Float) {
        if (!_isLogging.value) return
        fpsValues.add(fps)
    }
    
    /**
     * Log frame processing metrics.
     */
    fun logFrameMetrics(
        inferenceMs: Float,
        bitmapConversionMs: Float,
        rotationMs: Float,
        totalMs: Float,
        poseDetected: Boolean,
        avgLandmarkConfidence: Float = 0f,
        avgVisibilityScore: Float = 0f
    ) {
        if (!_isLogging.value) return
        
        frameCount++
        
        if (inferenceMs > 0) inferenceTimes.add(inferenceMs)
        if (bitmapConversionMs > 0) bitmapConversionTimes.add(bitmapConversionMs)
        if (rotationMs > 0) rotationTimes.add(rotationMs)
        if (totalMs > 0) totalProcessingTimes.add(totalMs)
        
        if (poseDetected) {
            framesWithPose++
            if (avgLandmarkConfidence > 0) landmarkConfidences.add(avgLandmarkConfidence)
            if (avgVisibilityScore > 0) visibilityScores.add(avgVisibilityScore)
        } else {
            framesWithoutPose++
        }
    }
    
    /**
     * Generate PerformanceMetrics object from collected data.
     */
    fun generateMetrics(): PerformanceMetrics {
        val duration = ((System.currentTimeMillis() - sessionStartTime) / 1000).toInt()
        
        // Helper function to safely calculate average
        fun List<Float>.safeAverage(): Float = if (isEmpty()) 0f else average().toFloat()
        
        // Safely calculate detection rate
        val detectionRate = if (frameCount > 0) {
            (framesWithPose.toFloat() / frameCount.toFloat() * 100f)
        } else {
            0f
        }
        
        return PerformanceMetrics(
            testNumber = testNumber,
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            androidVersion = Build.VERSION.RELEASE,
            modelType = modelType,
            exerciseType = exerciseType,
            delegate = delegate,
            timestamp = sessionStartTime,
            durationSeconds = duration,
            
            // FPS metrics
            fpsAverage = fpsValues.safeAverage(),
            fpsMin = fpsValues.minOrNull() ?: 0f,
            fpsMax = fpsValues.maxOrNull() ?: 0f,
            fpsStdDev = calculateStdDev(fpsValues),
            frameCount = frameCount,
            
            // Processing time metrics
            inferenceTimeAvgMs = inferenceTimes.safeAverage(),
            inferenceTimeMinMs = inferenceTimes.minOrNull() ?: 0f,
            inferenceTimeMaxMs = inferenceTimes.maxOrNull() ?: 0f,
            bitmapConversionAvgMs = bitmapConversionTimes.safeAverage(),
            rotationAvgMs = rotationTimes.safeAverage(),
            totalProcessingAvgMs = totalProcessingTimes.safeAverage(),
            
            // Detection metrics
            framesWithPose = framesWithPose,
            framesWithoutPose = framesWithoutPose,
            detectionSuccessRate = detectionRate,
            avgLandmarkConfidence = landmarkConfidences.safeAverage(),
            avgVisibilityScore = visibilityScores.safeAverage(),
            
            // Device metrics
            batteryStart = batteryStart,
            batteryEnd = batteryEnd,
            batteryDrainPercent = batteryStart - batteryEnd
        )
    }
    
    /**
     * Export performance metrics to JSON file.
     * Returns the file path if successful, null otherwise.
     */
    fun exportToJson(): String? {
        try {
            val metrics = generateMetrics()
            val json = metrics.toJson()
            
            // Create file in app's documents directory
            val documentsDir = File(context.getExternalFilesDir(null), "performance_tests")
            if (!documentsDir.exists()) {
                documentsDir.mkdirs()
            }
            
            val filename = metrics.generateFilename()
            val file = File(documentsDir, filename)
            
            // Write JSON to file
            file.writeText(json.toString(2)) // Pretty print with 2-space indent
            
            Log.d(TAG, "✓ Performance data exported to: ${file.absolutePath}")
            return file.absolutePath
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export performance data", e)
            return null
        }
    }
    
    /**
     * Share the exported JSON file via Android share sheet.
     */
    fun shareExportedFile(filePath: String): Intent? {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                Log.e(TAG, "File does not exist: $filePath")
                return null
            }
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            return Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "PoseCoach Performance Test Data")
                putExtra(Intent.EXTRA_TEXT, "Test #$testNumber - $modelType - $exerciseType")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share file", e)
            return null
        }
    }
    
    /**
     * Get current battery level percentage.
     */
    private fun getBatteryLevel(): Int {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }
        
        val batteryPct: Int? = batteryStatus?.let { intent ->
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) {
                (level * 100 / scale.toFloat()).toInt()
            } else {
                null
            }
        }
        
        return batteryPct ?: 100
    }
    
    /**
     * Calculate standard deviation of a list of floats.
     */
    private fun calculateStdDev(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        
        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return sqrt(variance).toFloat()
    }
    
    /**
     * Reset all collected data.
     */
    private fun reset() {
        fpsValues.clear()
        frameCount = 0
        
        inferenceTimes.clear()
        bitmapConversionTimes.clear()
        rotationTimes.clear()
        totalProcessingTimes.clear()
        
        framesWithPose = 0
        framesWithoutPose = 0
        landmarkConfidences.clear()
        visibilityScores.clear()
        
        batteryStart = 0
        batteryEnd = 0
    }
}
