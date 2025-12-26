package com.example.posecoach.ui

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posecoach.ModelWarmer
import com.example.posecoach.data.CameraState
import com.example.posecoach.data.FeedbackMessage
import com.example.posecoach.data.FeedbackSeverity
import com.example.posecoach.data.LiveSessionResult
import com.example.posecoach.data.PoseResult
import com.example.posecoach.logic.DefaultPoseEvaluator
import com.example.posecoach.logic.PoseEvaluator
import com.example.posecoach.pose.PoseEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

enum class SessionState { IDLE, COUNTDOWN, ACTIVE, FINISHED }

data class ExerciseSessionSummary(
    val exerciseId: String,
    val exerciseName: String,
    val reps: Int,
    val durationMillis: Long,
    val feedbackMessages: List<FeedbackMessage> = emptyList()
)

class CameraViewModel : ViewModel() {

    companion object {
        const val SKIP_POSE_EVALUATION = false
        const val ENABLE_EVALUATION_TIMING = true
    }

    private lateinit var poseEngine: PoseEngine
    private val poseEvaluator: PoseEvaluator = DefaultPoseEvaluator()
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private val _poseResult = MutableStateFlow<PoseResult?>(null)
    val poseResult: StateFlow<PoseResult?> = _poseResult.asStateFlow()

    private val _feedback = MutableStateFlow<FeedbackMessage?>(null)
    val feedback: StateFlow<FeedbackMessage?> = _feedback.asStateFlow()

    private val _fps = MutableStateFlow(0f)
    val fps: StateFlow<Float> = _fps.asStateFlow()

    private val _cameraState = MutableStateFlow<CameraState>(CameraState.Front)
    val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()

    private val _useGpuDelegate = MutableStateFlow(false)
    val useGpuDelegate: StateFlow<Boolean> = _useGpuDelegate.asStateFlow()

    private val _cameraError = MutableStateFlow<String?>(null)
    val cameraError: StateFlow<String?> = _cameraError.asStateFlow()

    private val _repCount = MutableStateFlow(0)
    val repCount: StateFlow<Int> = _repCount.asStateFlow()

    private val _sessionState = MutableStateFlow(SessionState.IDLE)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private val _countdownValue = MutableStateFlow(5)
    val countdownValue: StateFlow<Int> = _countdownValue.asStateFlow()

    private val _summaryText = MutableStateFlow<String?>(null)
    val summaryText: StateFlow<String?> = _summaryText.asStateFlow()

    private val _currentExercise = MutableStateFlow("squat")
    val currentExercise: StateFlow<String> = _currentExercise.asStateFlow()

    private val _targetReps = MutableStateFlow(10)
    val targetReps: StateFlow<Int> = _targetReps.asStateFlow()

    private var sessionStartTimeMillis: Long? = null

    private val _workoutSessions = MutableStateFlow<List<ExerciseSessionSummary>>(emptyList())
    val workoutSessions: StateFlow<List<ExerciseSessionSummary>> = _workoutSessions.asStateFlow()

    private val _navigateHomeAfterSummary = MutableStateFlow(false)
    val navigateHomeAfterSummary: StateFlow<Boolean> = _navigateHomeAfterSummary.asStateFlow()

    private val _sessionResult = MutableStateFlow<LiveSessionResult?>(null)
    val sessionResult: StateFlow<LiveSessionResult?> = _sessionResult.asStateFlow()

    private val _exerciseRemainingSeconds = MutableStateFlow<Int?>(null)
    val exerciseRemainingSeconds: StateFlow<Int?> = _exerciseRemainingSeconds.asStateFlow()

    private val _exerciseElapsedSeconds = MutableStateFlow(0)
    val exerciseElapsedSeconds: StateFlow<Int> = _exerciseElapsedSeconds.asStateFlow()

    private val sessionFeedbackHistory = mutableListOf<FeedbackMessage>()

    private fun severityPriority(severity: FeedbackSeverity): Int =
        when (severity) {
            FeedbackSeverity.INFO -> 0
            FeedbackSeverity.WARNING -> 1
            FeedbackSeverity.ERROR -> 2
        }

    fun setTargetReps(target: Int) {
        _targetReps.value = target
    }

    fun setExercise(exercise: String) {
        _currentExercise.value = exercise
        poseEvaluator.reset()
        _repCount.value = 0
    }

    fun switchCamera() {
        _cameraState.value = _cameraState.value.toggle()
    }

    fun bindCamera(
        context: android.content.Context,
        lifecycleOwner: androidx.lifecycle.LifecycleOwner,
        cameraProvider: ProcessCameraProvider,
        previewView: PreviewView
    ) {
        if (!::poseEngine.isInitialized) {
            val warmedEngine = ModelWarmer.getInstance(context).getWarmedEngine()
            poseEngine = warmedEngine ?: PoseEngine(context).also { it.initialize() }

            viewModelScope.launch {
                poseEngine.poseResults.collect { result ->
                    if (_sessionState.value == SessionState.ACTIVE) {
                        _poseResult.value = result
                        result?.let { pose ->
                            val feedbackMsg = poseEvaluator.evaluate(pose, _currentExercise.value)
                            feedbackMsg?.let { msg ->
                                if (sessionFeedbackHistory.lastOrNull()?.text != msg.text) {
                                    sessionFeedbackHistory.add(msg)
                                }
                                _feedback.value = msg
                            }
                            _repCount.value = poseEvaluator.getRepCount()
                        }
                    } else {
                        _poseResult.value = null
                    }
                }
            }
        }

        cameraProvider.unbindAll()

        val preview = Preview.Builder()
            .build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor) { imageProxy ->
                    val state = _sessionState.value
                    if (state == SessionState.ACTIVE || state == SessionState.COUNTDOWN) {
                        poseEngine.detectPose(imageProxy, _cameraState.value.isFront())
                    }
                    imageProxy.close()
                }
            }

        val selector = _cameraState.value.toCameraSelector()
        cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, imageAnalysis)
    }

    override fun onCleared() {
        super.onCleared()
        if (::poseEngine.isInitialized) poseEngine.close()
        cameraExecutor.shutdown()
    }
}
