package com.example.posecoach.ui

import android.app.Application
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.posecoach.data.CameraState
import com.example.posecoach.data.FeedbackMessage
import com.example.posecoach.logic.DefaultPoseEvaluator
import com.example.posecoach.logic.PoseEvaluator
import com.example.posecoach.pose.PoseEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class PoseCoachViewModel(app: Application) : AndroidViewModel(app) {

    private val poseEngine = PoseEngine(app)          // Student 2
    private val evaluator: PoseEvaluator = DefaultPoseEvaluator() // Student 3

    // Camera front/back state
    private val _cameraState = MutableStateFlow<CameraState>(CameraState.Front)
    val cameraState: StateFlow<CameraState> = _cameraState

    // Latest feedback from evaluator (for UI)
    private val _feedback = MutableStateFlow<FeedbackMessage?>(null)
    val feedback: StateFlow<FeedbackMessage?> = _feedback

    // Optional: expose FPS & rep count
    val fps = poseEngine.fps
    private val _repCount = MutableStateFlow(0)
    val repCount: StateFlow<Int> = _repCount

    // Simple exercise selection (you can change this from the UI later)
    private val _exerciseType = MutableStateFlow("squat")
    val exerciseType: StateFlow<String> = _exerciseType

    init {
        // Initialize model
        poseEngine.initialize()
        evaluator.startSession()

        // Listen to pose results and evaluate them
        viewModelScope.launch {
            poseEngine.poseResults
                .filterNotNull()
                .collect { poseResult ->
                    val feedbackMsg = evaluator.evaluate(
                        poseResult = poseResult,
                        exerciseType = _exerciseType.value
                    )
                    if (feedbackMsg != null) {
                        _feedback.value = feedbackMsg
                    }
                    _repCount.value = evaluator.getRepCount()
                }
        }
    }

    fun toggleCamera() {
        _cameraState.value = _cameraState.value.toggle()
    }

    fun setExerciseType(type: String) {
        _exerciseType.value = type
        evaluator.reset()
        evaluator.startSession()
        _repCount.value = 0
    }

    /**
     * Called by the Camera analyzer for each frame.
     */
    fun onFrame(imageProxy: ImageProxy) {
        poseEngine.detectPose(
            imageProxy = imageProxy,
            isFrontCamera = _cameraState.value.isFront()
        )
        // Remember to close the imageProxy in the analyzer where you call this
    }

    override fun onCleared() {
        super.onCleared()
        poseEngine.close()
    }
}