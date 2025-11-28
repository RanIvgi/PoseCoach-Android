package com.example.posecoach.ui

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posecoach.data.CameraState
import com.example.posecoach.data.FeedbackMessage
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

// Per-exercise session summary
data class ExerciseSessionSummary(
    val exerciseId: String,
    val exerciseName: String,
    val reps: Int,
    val durationMillis: Long
)

class CameraViewModel : ViewModel() {

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

    // Track feedback history during active session
    private val sessionFeedbackHistory = mutableListOf<FeedbackMessage>()

    fun setTargetReps(target: Int) {
        _targetReps.value = target
    }

    fun setExercise(exercise: String) {
        _currentExercise.value = exercise
        poseEvaluator.reset()
        _repCount.value = 0
    }

    fun finishSessionAndGoHome() {
        _navigateHomeAfterSummary.value = true
        finishSession()
    }

    fun bindCamera(
        context: android.content.Context,
        lifecycleOwner: androidx.lifecycle.LifecycleOwner,
        cameraProvider: ProcessCameraProvider,
        previewView: PreviewView
    ) {
        if (!::poseEngine.isInitialized) {
            poseEngine = PoseEngine(context)
            poseEngine.initialize()

            viewModelScope.launch {
                poseEngine.poseResults.collect { result ->
                    if (_sessionState.value == SessionState.ACTIVE) {
                        _poseResult.value = result
                        result?.let {
                            val feedbackMsg = poseEvaluator.evaluate(it, _currentExercise.value)
                            _feedback.value = feedbackMsg
                            
                            // Collect feedback for session summary
                            feedbackMsg?.let { msg ->
                                // Avoid duplicates of the same message
                                if (sessionFeedbackHistory.isEmpty() || 
                                    sessionFeedbackHistory.last().text != msg.text) {
                                    sessionFeedbackHistory.add(msg)
                                }
                            }
                            
                            _repCount.value = poseEvaluator.getRepCount()
                        }
                    } else {
                        _poseResult.value = null
                    }
                }
            }

            viewModelScope.launch { poseEngine.fps.collect { _fps.value = it } }
            viewModelScope.launch { poseEngine.useGpuDelegate.collect { _useGpuDelegate.value = it } }
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
                    // PERFORMANCE OPTIMIZATION: Only process frames during ACTIVE or COUNTDOWN sessions
                    // This prevents wasted CPU cycles when user is idle on the camera screen.
                    // Frame processing involves expensive operations (bitmap conversion, MediaPipe inference)
                    // that should only run when actively needed.
                    // During IDLE state, the camera preview still shows but pose detection is skipped.
                    val currentState = _sessionState.value
                    if (currentState == SessionState.ACTIVE || currentState == SessionState.COUNTDOWN) {
                        poseEngine.detectPose(imageProxy, _cameraState.value.isFront())
                    }
                    imageProxy.close()
                }
            }

        var selectedCameraSelector: CameraSelector? = null
        val preferredCamera = _cameraState.value.toCameraSelector()
        if (cameraProvider.hasCamera(preferredCamera)) {
            selectedCameraSelector = preferredCamera
        } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
            selectedCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
            selectedCameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        }

        if (selectedCameraSelector == null) {
            _cameraError.value = "No suitable camera found on this device."
            return
        }
        _cameraError.value = null

        try {
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                selectedCameraSelector,
                preview,
                imageAnalysis
            )
        } catch (e: Exception) {
            _cameraError.value = "Camera binding failed: ${e.message}"
        }
    }

    fun switchCamera(context: android.content.Context, lifecycleOwner: androidx.lifecycle.LifecycleOwner) {
        _cameraState.value = _cameraState.value.toggle()
    }

    fun toggleDelegate(context: android.content.Context, lifecycleOwner: androidx.lifecycle.LifecycleOwner) {
        viewModelScope.launch {
            if (::poseEngine.isInitialized) poseEngine.close()
            poseEngine = PoseEngine(context)
            poseEngine.toggleDelegate()
            poseEngine.initialize()
        }
    }

    fun startSessionCountdown() {
        if (_sessionState.value == SessionState.IDLE) {
            viewModelScope.launch {
                _sessionState.value = SessionState.COUNTDOWN

                // 5-second countdown
                for (i in 5 downTo 1) {
                    _countdownValue.value = i
                    delay(1000)
                }

                // Countdown done: start session & timer
                poseEvaluator.startSession()
                sessionStartTimeMillis = System.currentTimeMillis()
                sessionFeedbackHistory.clear() // Clear previous feedback
                _sessionState.value = SessionState.ACTIVE
            }
        }
    }

    fun finishSession() {
        if (_sessionState.value == SessionState.ACTIVE) {

            val now = System.currentTimeMillis()

            // how long the session lasted in ms (if start time is null, use 0)
            val durationMillis = sessionStartTimeMillis?.let { start ->
                now - start
            } ?: 0L

            val formSummary = poseEvaluator.getEvaluationSummary()

            // Calculate overall score from feedback history
            val overallScore = LiveSessionResult.calculateScore(sessionFeedbackHistory)

            // Save this exercise into the "workout" list
            val current = ExerciseSessionSummary(
                exerciseId = _currentExercise.value,
                exerciseName = _currentExercise.value.replaceFirstChar { it.uppercase() },
                reps = _repCount.value,
                durationMillis = durationMillis
            )
            _workoutSessions.value = _workoutSessions.value + current

            // Compute workout totals
            val totalReps = _workoutSessions.value.sumOf { it.reps }
            val totalDurationMillis = _workoutSessions.value.sumOf { it.durationMillis }
            val totalDurationText = formatDuration(totalDurationMillis)
            val totalExercises = _workoutSessions.value.size

            // Create comprehensive session result
            val sessionResult = LiveSessionResult(
                exerciseType = _currentExercise.value,
                exerciseName = _currentExercise.value.replaceFirstChar { it.uppercase() },
                targetReps = _targetReps.value,
                completedReps = _repCount.value,
                durationMillis = durationMillis,
                feedbackMessages = sessionFeedbackHistory.toList(), // Copy the list
                evaluationSummary = formSummary,
                overallScore = overallScore,
                totalExercises = totalExercises,
                totalReps = totalReps,
                totalDurationMillis = totalDurationMillis
            )

            _sessionResult.value = sessionResult
            _sessionState.value = SessionState.FINISHED
            sessionStartTimeMillis = null
        }
    }

    fun resetSession() {
        poseEvaluator.reset()
        _repCount.value = 0
        _sessionState.value = SessionState.IDLE
        _summaryText.value = null
        _sessionResult.value = null
        _feedback.value = null
        sessionStartTimeMillis = null
        sessionFeedbackHistory.clear()
        _navigateHomeAfterSummary.value = false
    }

    fun resetRepCount() {
        poseEvaluator.reset()
        _repCount.value = 0
    }

    override fun onCleared() {
        super.onCleared()
        if (::poseEngine.isInitialized) {
            poseEngine.close()
        }
        cameraExecutor.shutdown()
    }

    private fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    fun resetWorkout() {
        _workoutSessions.value = emptyList()
        resetSession()
    }
}
