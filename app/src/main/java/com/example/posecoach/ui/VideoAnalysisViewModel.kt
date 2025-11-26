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
            try {
                _isProcessing.value = true
                _errorMessage.value = null
                _processingProgress.value = 0f
                
                Log.d(TAG, "Starting video analysis for exercise: $exerciseId")
                
                // Step 1: Extract frames from video
                val uri = Uri.parse(videoUri)
                val frames = videoProcessor.extractFrames(
                    videoUri = uri,
                    frameIntervalMs = 500, // Extract 2 frames per second
                    maxFrames = 120, // Max 1 minute of analysis
                    progressCallback = { progress ->
                        _processingProgress.value = progress * 0.3f // Frame extraction is 30% of progress
                    }
                )
                
                if (frames.isEmpty()) {
                    _errorMessage.value = "Failed to extract frames from video"
                    _isProcessing.value = false
                    return@launch
                }
                
                Log.d(TAG, "Extracted ${frames.size} frames from video")
                
                // Step 2: Initialize pose detection and evaluation
                val poseEngine = PoseEngine(getApplication())
                val poseEvaluator = DefaultPoseEvaluator()
                
                if (!poseEngine.initialize()) {
                    _errorMessage.value = "Failed to initialize pose detection engine"
                    _isProcessing.value = false
                    return@launch
                }
                
                poseEvaluator.reset()
                poseEvaluator.startSession()
                
                // Step 3: Process each frame
                val feedbackSet = mutableSetOf<FeedbackMessage>() // Use set to avoid duplicates
                var framesWithPose = 0
                
                frames.forEachIndexed { index, frame ->
                    withContext(Dispatchers.Default) {
                        try {
                            // Detect pose on frame
                            val poseResult = poseEngine.detectPoseFromBitmap(frame)
                            
                            if (poseResult != null && poseResult.hasPose()) {
                                framesWithPose++
                                
                                // Evaluate the pose
                                val feedback = poseEvaluator.evaluate(poseResult, exerciseId)
                                
                                // Collect unique feedback messages
                                if (feedback != null) {
                                    feedbackSet.add(feedback)
                                }
                            }
                            
                            // Update progress (frame processing is 60% of total progress)
                            val frameProgress = (index + 1).toFloat() / frames.size
                            _processingProgress.value = 0.3f + (frameProgress * 0.6f)
                            
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing frame $index", e)
                        }
                    }
                }
                
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
                
                Log.d(TAG, "Analysis complete: $repCount reps, $framesWithPose/$frames.size frames with pose, score: $overallScore")
                
                // Cleanup
                poseEngine.close()
                frames.forEach { it.recycle() }
                
                _processingProgress.value = 1f
                _isProcessing.value = false
                onComplete()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during video analysis", e)
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
