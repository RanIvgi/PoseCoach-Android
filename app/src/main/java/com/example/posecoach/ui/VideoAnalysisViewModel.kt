package com.example.posecoach.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.posecoach.data.FeedbackMessage
import com.example.posecoach.data.VideoAnalysisResult
import com.example.posecoach.logic.DefaultPoseEvaluator
import com.example.posecoach.pose.PoseEngine
import com.example.posecoach.video.VideoProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException

class VideoAnalysisViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _selectedVideoUri = MutableStateFlow<Uri?>(null)
    val selectedVideoUri: StateFlow<Uri?> = _selectedVideoUri.asStateFlow()
    
    private val _selectedExercise = MutableStateFlow<String?>(null)
    val selectedExercise: StateFlow<String?> = _selectedExercise.asStateFlow()
    
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()
    
    private val _processingProgress = MutableStateFlow(0f)
    val processingProgress: StateFlow<Float> = _processingProgress.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _analysisResult = MutableStateFlow<VideoAnalysisResult?>(null)
    val analysisResult: StateFlow<VideoAnalysisResult?> = _analysisResult.asStateFlow()
    
    private val videoProcessor = VideoProcessor(application)
    
    fun setVideoUri(uri: Uri) {
        _selectedVideoUri.value = uri
        _errorMessage.value = null
    }
    
    fun setExercise(exerciseId: String) {
        _selectedExercise.value = exerciseId
        _errorMessage.value = null
    }
    
    /**
     * Analyze video using real pose detection and evaluation.
     * 
     * This method:
     * 1. Extracts frames from the video
     * 2. Detects poses on each frame using PoseEngine
     * 3. Evaluates each pose using PoseEvaluator
     * 4. Collects feedback and statistics
     * 5. Generates final results
     */
    fun startAnalysis(videoUri: String, exerciseId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            var frames: List<android.graphics.Bitmap> = emptyList()
            try {
                _isProcessing.value = true
                _errorMessage.value = null
                _processingProgress.value = 0f
                
                Log.d(TAG, "Starting video analysis for exercise: $exerciseId")
                
                // Step 1: Extract frames from video
                val uri = Uri.parse(videoUri)
                frames = videoProcessor.extractFrames(
                    videoUri = uri,
                    frameIntervalMs = 1000, // Extract 1 frame per second (reduced from 500ms for performance)
                    maxFrames = 30, // Max 30 seconds of analysis (reduced from 120 for testing)
                    progressCallback = { progress ->
                        _processingProgress.value = progress * 0.3f // Frame extraction is 30% of progress
                    }
                )
                
                if (frames.isEmpty()) {
                    _errorMessage.value = "Failed to extract frames from video"
                    _isProcessing.value = false
                    return@launch
                }
                
                Log.d(TAG, "✓ Extracted ${frames.size} frames from video")
                Log.d(TAG, "Step 2/4: Initializing pose detection engine...")
                
                // Step 2: Initialize pose detection engine for video analysis
                val poseEngine = PoseEngine(getApplication())
                val imageLandmarker = withContext(Dispatchers.Default) {
                    poseEngine.createImageModeLandmarker()
                }
                
                if (imageLandmarker == null) {
                    Log.e(TAG, "✗ Failed to create image mode landmarker")
                    _errorMessage.value = "Failed to initialize pose detection engine"
                    _isProcessing.value = false
                    frames.forEach { it.recycle() }
                    return@launch
                }
                
                Log.d(TAG, "✓ Pose detection engine initialized")
                Log.d(TAG, "Step 3/4: Processing ${frames.size} frames...")
                
                val poseEvaluator = DefaultPoseEvaluator()
                
                poseEvaluator.reset()
                poseEvaluator.startSession()
                
                // Step 3: Process each frame with timeout protection
                val feedbackSet = mutableSetOf<FeedbackMessage>() // Use set to avoid duplicates
                var framesWithPose = 0
                var framesFailed = 0
                
                frames.forEachIndexed { index, frame ->
                    try {
                        // Log progress every 5 frames
                        if (index % 5 == 0 || index == frames.size - 1) {
                            Log.d(TAG, "Processing frame ${index + 1}/${frames.size}...")
                        }
                        
                        // Add timeout to prevent hanging on slow frames
                        withTimeout(10000L) { // 10 second timeout per frame (increased for first-frame model warmup)
                            withContext(Dispatchers.Default) {
                                try {
                                    // Detect pose on frame using reusable landmarker
                                    val poseResult = poseEngine.detectPoseFromBitmap(frame, imageLandmarker)
                                    
                                    if (poseResult != null && poseResult.hasPose()) {
                                        framesWithPose++
                                        
                                        // Evaluate the pose
                                        val feedback = poseEvaluator.evaluate(poseResult, exerciseId)
                                        
                                        // Collect unique feedback messages
                                        if (feedback != null) {
                                            feedbackSet.add(feedback)
                                        }
                                    }
                                    Unit // Explicitly return Unit to avoid if-expression issues
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error processing frame $index: ${e.message}", e)
                                    framesFailed++
                                }
                            }
                        }
                        
                        // Update progress (frame processing is 60% of total progress)
                        val frameProgress = (index + 1).toFloat() / frames.size
                        _processingProgress.value = 0.3f + (frameProgress * 0.6f)
                        
                    } catch (e: TimeoutCancellationException) {
                        Log.w(TAG, "Frame $index timed out after 10 seconds, skipping")
                        framesFailed++
                    } catch (e: Exception) {
                        Log.e(TAG, "Unexpected error processing frame $index: ${e.message}", e)
                        framesFailed++
                    }
                }
                
                // Clean up the landmarker
                imageLandmarker.close()
                
                Log.d(TAG, "✓ Frame processing complete: ${framesWithPose} with pose, $framesFailed failed")
                Log.d(TAG, "Step 4/4: Generating analysis results...")
                
                // Step 4: Generate final results
                val repCount = poseEvaluator.getRepCount()
                val evaluationSummary = poseEvaluator.getEvaluationSummary()
                val feedbackList = feedbackSet.toList()
                val overallScore = VideoAnalysisResult.calculateScore(feedbackList)
                
                _analysisResult.value = VideoAnalysisResult(
                    feedbackMessages = feedbackList,
                    repCount = repCount,
                    evaluationSummary = evaluationSummary,
                    exerciseType = exerciseId,
                    totalFramesAnalyzed = frames.size,
                    framesWithPoseDetected = framesWithPose,
                    overallScore = overallScore
                )
                
                Log.d(TAG, "✓ Analysis complete: $repCount reps, $framesWithPose/${frames.size} frames with pose, score: $overallScore")
                Log.d(TAG, "Cleaning up resources...")
                
                // Cleanup frames
                frames.forEach { it.recycle() }
                
                _processingProgress.value = 1f
                _isProcessing.value = false
                onComplete()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during video analysis", e)
                frames.forEach { it.recycle() }
                _isProcessing.value = false
                _errorMessage.value = "Error processing video: ${e.message}"
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    fun clearAnalysisResult() {
        _analysisResult.value = null
    }
    
    companion object {
        private const val TAG = "VideoAnalysisViewModel"
    }
}
