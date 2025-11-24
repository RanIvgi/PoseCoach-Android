package com.example.posecoach.pose

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Build
import android.util.Log
import androidx.camera.core.ImageProxy
import com.example.posecoach.data.PoseLandmark
import com.example.posecoach.data.PoseResult
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream

/**
 * PoseEngine - Student 2's ML Model Integration Module
 * 
 * This class handles all MediaPipe Pose Landmarker integration:
 * - Initializes the ML model from assets
 * - Processes camera frames from CameraX
 * - Extracts 33 pose landmarks per frame
 * - Converts results to app's data format
 * - Emits results via Kotlin Flow for reactive updates
 * 
 * Usage:
 * ```
 * val poseEngine = PoseEngine(context)
 * poseEngine.initialize()
 * 
 * // In CameraX ImageAnalysis callback:
 * imageProxy.use { proxy ->
 *     poseEngine.detectPose(proxy, isFrontCamera = true)
 * }
 * 
 * // Collect results:
 * poseEngine.poseResults.collect { result ->
 *     // Use result for evaluation or display
 * }
 * ```
 * 
 * Student 2 TODO List:
 * 1. Tune model confidence thresholds
 * 2. Experiment with GPU vs CPU delegate performance
 * 3. Add image preprocessing if needed (contrast, brightness)
 * 4. Handle edge cases (partial body visibility)
 * 5. Optimize frame processing rate based on device performance
 * 6. Add model warm-up on initialization
 * 7. Implement landmark smoothing/filtering for stability
 */
class PoseEngine(private val context: Context) {
    
    private var poseLandmarker: PoseLandmarker? = null
    
    // StateFlow to emit pose detection results
    private val _poseResults = MutableStateFlow<PoseResult?>(null)
    val poseResults: StateFlow<PoseResult?> = _poseResults.asStateFlow()
    
    // Performance tracking
    private val _fps = MutableStateFlow(0f)
    val fps: StateFlow<Float> = _fps.asStateFlow()
    
    // Delegate selection (GPU or CPU)
    private val _useGpuDelegate = MutableStateFlow(isRealDevice())
    val useGpuDelegate: StateFlow<Boolean> = _useGpuDelegate.asStateFlow()
    
    private var lastFrameTime = 0L
    private val frameTimeWindow = mutableListOf<Long>()
    
    /**
     * Toggle between GPU and CPU delegate.
     * Requires re-initialization to take effect.
     */
    fun toggleDelegate() {
        _useGpuDelegate.value = !_useGpuDelegate.value
        Log.d(TAG, "Delegate toggled to: ${if (_useGpuDelegate.value) "GPU" else "CPU"}")
    }
    
    /**
     * Set delegate manually.
     * Requires re-initialization to take effect.
     */
    fun setUseGpu(useGpu: Boolean) {
        _useGpuDelegate.value = useGpu
        Log.d(TAG, "Delegate set to: ${if (useGpu) "GPU" else "CPU"}")
    }
    
    /**
     * Initialize MediaPipe Pose Landmarker.
     * Must be called before using detectPose().
     * 
     * @return true if initialization successful, false otherwise
     */
    fun initialize(): Boolean {
        try {
            // Determine which delegate to use
            val delegate = if (_useGpuDelegate.value) Delegate.GPU else Delegate.CPU
            
            Log.d(TAG, "Attempting to initialize with ${if (_useGpuDelegate.value) "GPU" else "CPU"} delegate")
            
            // Configure MediaPipe base options
            val baseOptions = BaseOptions.builder()
                .setDelegate(delegate)
                .setModelAssetPath("pose_landmarker_full.task") // Model in assets folder
                .build()
            
            // Configure Pose Landmarker options
            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM) // For real-time camera feed
                .setMinPoseDetectionConfidence(0.5f) // TODO (Student 2): Tune this
                .setMinPosePresenceConfidence(0.5f)  // TODO (Student 2): Tune this
                .setMinTrackingConfidence(0.5f)      // TODO (Student 2): Tune this
                .setNumPoses(1) // Track single person (change to 2+ for multi-person)
                .setOutputSegmentationMasks(false) // We don't need segmentation masks
                .setResultListener { result, image ->
                    // This callback runs on a background thread
                    handlePoseResult(result, image)
                }
                .setErrorListener { error ->
                    Log.e(TAG, "PoseLandmarker error: ${error.message}")
                }
                .build()
            
            // Create the landmarker
            poseLandmarker = PoseLandmarker.createFromOptions(context, options)
            
            Log.d(TAG, "PoseLandmarker initialized successfully with ${if (_useGpuDelegate.value) "GPU" else "CPU"} delegate")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize PoseLandmarker with ${if (_useGpuDelegate.value) "GPU" else "CPU"} delegate", e)
            
            // If GPU failed, automatically try CPU as fallback
            if (_useGpuDelegate.value) {
                Log.w(TAG, "GPU initialization failed, falling back to CPU delegate")
                _useGpuDelegate.value = false
                
                try {
                    val cpuBaseOptions = BaseOptions.builder()
                        .setDelegate(Delegate.CPU)
                        .setModelAssetPath("pose_landmarker_full.task")
                        .build()
                    
                    val cpuOptions = PoseLandmarker.PoseLandmarkerOptions.builder()
                        .setBaseOptions(cpuBaseOptions)
                        .setRunningMode(RunningMode.LIVE_STREAM)
                        .setMinPoseDetectionConfidence(0.5f)
                        .setMinPosePresenceConfidence(0.5f)
                        .setMinTrackingConfidence(0.5f)
                        .setNumPoses(1)
                        .setOutputSegmentationMasks(false)
                        .setResultListener { result, image ->
                            handlePoseResult(result, image)
                        }
                        .setErrorListener { error ->
                            Log.e(TAG, "PoseLandmarker error: ${error.message}")
                        }
                        .build()
                    
                    poseLandmarker = PoseLandmarker.createFromOptions(context, cpuOptions)
                    Log.d(TAG, "CPU fallback successful")
                    return true
                    
                } catch (fallbackException: Exception) {
                    Log.e(TAG, "CPU fallback also failed", fallbackException)
                    return false
                }
            }
            
            return false
        }
    }
    
    /**
     * Process a camera frame and detect pose landmarks.
     * Call this from CameraX ImageAnalysis.Analyzer.
     * 
     * @param imageProxy Camera frame from CameraX
     * @param isFrontCamera Whether this is from front camera (for mirroring)
     */
    fun detectPose(imageProxy: ImageProxy, isFrontCamera: Boolean) {
        val currentTime = System.currentTimeMillis()
        
        try {
            // Convert ImageProxy to Bitmap
            val bitmap = imageProxyToBitmap(imageProxy)
            
            // Apply rotation to bitmap based on device orientation
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val rotatedBitmap = if (rotationDegrees != 0) {
                rotateBitmap(bitmap, rotationDegrees)
            } else {
                bitmap
            }
            
            // Convert Bitmap to MPImage for MediaPipe
            val mpImage = BitmapImageBuilder(rotatedBitmap).build()
            
            // Log frame details before sending to MediaPipe
            Log.d(TAG, "Sending frame to MediaPipe at time=$currentTime, rotation=$rotationDegrees")
            
            // Detect pose asynchronously
            // The result will be delivered to the result listener callback
            poseLandmarker?.detectAsync(mpImage, currentTime)
            
            // Calculate FPS
            updateFPS(currentTime)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting pose", e)
        }
    }
    
    /**
     * Process a single image/bitmap and detect pose landmarks synchronously.
     * Use this for video frame analysis where you have pre-extracted frames.
     * 
     * @param bitmap The image to analyze
     * @return PoseResult containing detected landmarks, or null if detection fails
     */
    suspend fun detectPoseFromBitmap(bitmap: Bitmap): PoseResult? {
        return try {
            // For image mode, we need a different landmarker instance
            // Create a temporary one if needed
            val imageLandmarker = createImageModeLandmarker() ?: return null
            
            // Convert Bitmap to MPImage
            val mpImage = BitmapImageBuilder(bitmap).build()
            
            // Detect pose synchronously
            val result = imageLandmarker.detect(mpImage)
            
            // Extract landmarks
            val poseLandmarks = result.landmarks().firstOrNull()
            
            if (poseLandmarks != null && poseLandmarks.isNotEmpty()) {
                val landmarks = poseLandmarks.map { landmark ->
                    PoseLandmark(
                        x = landmark.x(),
                        y = landmark.y(),
                        z = landmark.z(),
                        visibility = landmark.visibility().orElse(1.0f),
                        presence = landmark.presence().orElse(1.0f)
                    )
                }
                
                PoseResult(
                    landmarks = landmarks,
                    timestamp = System.currentTimeMillis(),
                    imageWidth = bitmap.width,
                    imageHeight = bitmap.height,
                    isFrontCamera = false
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting pose from bitmap", e)
            null
        }
    }
    
    /**
     * Create a PoseLandmarker instance configured for IMAGE mode.
     * Used for analyzing pre-extracted video frames.
     */
    private fun createImageModeLandmarker(): PoseLandmarker? {
        return try {
            val delegate = if (_useGpuDelegate.value) Delegate.GPU else Delegate.CPU
            
            val baseOptions = BaseOptions.builder()
                .setDelegate(delegate)
                .setModelAssetPath("pose_landmarker_full.task")
                .build()
            
            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.IMAGE) // IMAGE mode for single frames
                .setMinPoseDetectionConfidence(0.5f)
                .setMinPosePresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setNumPoses(1)
                .setOutputSegmentationMasks(false)
                .build()
            
            PoseLandmarker.createFromOptions(context, options)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create image mode landmarker", e)
            null
        }
    }
    
    /**
     * Handle pose detection result from MediaPipe.
     * This is called by MediaPipe's result listener on a background thread.
     */
    private fun handlePoseResult(result: PoseLandmarkerResult, image: MPImage) {
        // Extract landmarks from the first (and likely only) detected pose
        val poseLandmarks = result.landmarks().firstOrNull()
        
        if (poseLandmarks != null && poseLandmarks.isNotEmpty()) {
            Log.d(TAG, "Pose detected. Landmarks count: ${poseLandmarks.size}")
            
            // Convert MediaPipe landmarks to our data format
            val landmarks = poseLandmarks.map { landmark ->
                PoseLandmark(
                    x = landmark.x(),
                    y = landmark.y(),
                    z = landmark.z(),
                    visibility = landmark.visibility().orElse(1.0f),
                    presence = landmark.presence().orElse(1.0f)
                )
            }
            
            // Create PoseResult
            val poseResult = PoseResult(
                landmarks = landmarks,
                timestamp = System.currentTimeMillis(),
                imageWidth = image.width,
                imageHeight = image.height,
                isFrontCamera = true // TODO: Pass this from detectPose
            )
            
            // Emit result via StateFlow
            _poseResults.value = poseResult
            
        } else {
            Log.d(TAG, "No pose detected in this frame.")
            
            // No pose detected in this frame
            _poseResults.value = PoseResult(
                landmarks = emptyList(),
                timestamp = System.currentTimeMillis(),
                imageWidth = image.width,
                imageHeight = image.height,
                isFrontCamera = true
            )
        }
    }
    
    /**
     * Rotate bitmap by specified degrees.
     * Handles device orientation for proper pose alignment.
     */
    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        
        val matrix = Matrix()
        matrix.postRotate(degrees.toFloat())
        
        val rotatedBitmap = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )
        
        // Recycle the original bitmap to free memory
        if (bitmap != rotatedBitmap) {
            bitmap.recycle()
        }
        
        Log.d(TAG, "Rotated bitmap by $degrees degrees: ${rotatedBitmap.width}x${rotatedBitmap.height}")
        
        return rotatedBitmap
    }
    
    /**
     * Convert CameraX ImageProxy to Bitmap.
     * Implements proper YUV_420_888 to RGB conversion using NV21 format.
     */
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        val yBuffer = imageProxy.planes[0].buffer
        val uBuffer = imageProxy.planes[1].buffer
        val vBuffer = imageProxy.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        
        // Convert YUV_420_888 to NV21 format
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, 
            imageProxy.width, imageProxy.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 100, out)
        val imageBytes = out.toByteArray()
        
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        
        Log.d(TAG, "Converted ImageProxy to Bitmap: ${bitmap.width}x${bitmap.height}")
        
        return bitmap
    }
    
    /**
     * Update FPS calculation.
     */
    private fun updateFPS(currentTime: Long) {
        if (lastFrameTime > 0) {
            val frameTime = currentTime - lastFrameTime
            frameTimeWindow.add(frameTime)
            
            // Keep only last 30 frames for moving average
            if (frameTimeWindow.size > 30) {
                frameTimeWindow.removeAt(0)
            }
            
            // Calculate average FPS
            val avgFrameTime = frameTimeWindow.average()
            _fps.value = 1000f / avgFrameTime.toFloat()
        }
        lastFrameTime = currentTime
    }
    
    /**
     * Clean up resources when done.
     * Call this when the camera is closed or app is destroyed.
     */
    fun close() {
        poseLandmarker?.close()
        poseLandmarker = null
        Log.i(TAG, "PoseEngine closed")
    }
    
    companion object {
        private const val TAG = "PoseEngine"
        
        /**
         * Check if running on a real device (not an emulator).
         * Used to automatically select GPU delegate on real devices.
         */
        private fun isRealDevice(): Boolean {
            // More comprehensive emulator detection
            val isEmulator = Build.FINGERPRINT.startsWith("generic")
                    || Build.FINGERPRINT.startsWith("unknown")
                    || Build.FINGERPRINT.contains("emulator")
                    || Build.MODEL.contains("google_sdk")
                    || Build.MODEL.contains("Emulator")
                    || Build.MODEL.contains("Android SDK built for x86")
                    || Build.MODEL.contains("sdk_gphone")
                    || Build.MANUFACTURER.contains("Genymotion")
                    || Build.HARDWARE.contains("goldfish")
                    || Build.HARDWARE.contains("ranchu")
                    || Build.PRODUCT.contains("sdk")
                    || Build.PRODUCT.contains("emulator")
                    || Build.PRODUCT.contains("simulator")
                    || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
            
            val result = !isEmulator
            Log.d(TAG, "Device detection - isRealDevice: $result (MODEL: ${Build.MODEL}, FINGERPRINT: ${Build.FINGERPRINT})")
            return result
        }
    }
}

/**
 * Helper extension for Student 2 to experiment with.
 * TODO (Student 2): Implement proper ImageProxy to Bitmap conversion.
 */
@androidx.camera.core.ExperimentalGetImage
private fun ImageProxy.toBitmapOrNull(): Bitmap? {
    val image = this.image ?: return null
    
    // This is a placeholder - needs proper implementation
    // based on image format (usually YUV_420_888)
    
    return null
}
