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
import com.example.posecoach.data.LiveSessionResult
import com.example.posecoach.data.PoseResult
import com.example.posecoach.logic.DefaultPoseEvaluator
import com.example.posecoach.logic.PoseEvaluator
import com.example.posecoach.logic.FeedbackAnalyzer
import com.example.posecoach.pose.PoseEngine
import com.example.posecoach.data.ExerciseSessionSummary
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

enum class SessionState { IDLE, COUNTDOWN, ACTIVE, FINISHED }

class CameraViewModel : ViewModel() {

    // ============================================================================
    // 🔍 PERFORMANCE DEBUGGING FLAGS
    // ============================================================================
    companion object {
        // Set to true to SKIP pose evaluation (test if angle calculations are the bottleneck)
        const val SKIP_POSE_EVALUATION = false
        
        // Set to true to ENABLE detailed timing logs for pose evaluation
        const val ENABLE_EVALUATION_TIMING = true
    }
    // ============================================================================

    private lateinit var poseEngine: PoseEngine
    private val poseEvaluator: PoseEvaluator = DefaultPoseEvaluator()
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    // Store references for camera switching
    private var currentContext: android.content.Context? = null
    private var currentLifecycleOwner: androidx.lifecycle.LifecycleOwner? = null
    private var currentCameraProvider: ProcessCameraProvider? = null
    private var currentPreviewView: PreviewView? = null

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

    private val _targetReps = MutableStateFlow(10)
    val targetReps: StateFlow<Int> = _targetReps.asStateFlow()

    // Computed property for countdown display (reps remaining)
    val repsRemaining: StateFlow<Int> = combine(
        _repCount,
        _targetReps
    ) { completed, target ->
        // For counted mode (target > 0), show remaining reps
        // For free mode (target = 0 or negative), show completed reps
        if (target > 0) {
            (target - completed).coerceAtLeast(0)
        } else {
            completed
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = _targetReps.value
    )

    private val _sessionState = MutableStateFlow(SessionState.IDLE)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private val _countdownValue = MutableStateFlow(5)
    val countdownValue: StateFlow<Int> = _countdownValue.asStateFlow()

    private val _summaryText = MutableStateFlow<String?>(null)
    val summaryText: StateFlow<String?> = _summaryText.asStateFlow()

    private val _currentExercise = MutableStateFlow("squat")
    val currentExercise: StateFlow<String> = _currentExercise.asStateFlow()

    private var sessionStartTimeMillis: Long? = null

    private val _workoutSessions = MutableStateFlow<List<ExerciseSessionSummary>>(emptyList())
    val workoutSessions: StateFlow<List<ExerciseSessionSummary>> = _workoutSessions.asStateFlow()

    private val _navigateHomeAfterSummary = MutableStateFlow(false)
    val navigateHomeAfterSummary: StateFlow<Boolean> = _navigateHomeAfterSummary.asStateFlow()

    private val _sessionResult = MutableStateFlow<LiveSessionResult?>(null)
    val sessionResult: StateFlow<LiveSessionResult?> = _sessionResult.asStateFlow()

    // Exercise timer:
    // remainingSeconds == null => free mode (count up)
    private val _exerciseRemainingSeconds = MutableStateFlow<Int?>(null)
    val exerciseRemainingSeconds: StateFlow<Int?> = _exerciseRemainingSeconds.asStateFlow()

    // Store the initial target duration for score calculation
    private var _targetDurationSeconds: Int? = null

    private val _exerciseElapsedSeconds = MutableStateFlow(0)
    val exerciseElapsedSeconds: StateFlow<Int> = _exerciseElapsedSeconds.asStateFlow()

    // Track feedback history during active session
    private val sessionFeedbackHistory = mutableListOf<FeedbackMessage>()

    fun setTargetReps(target: Int) {
        _targetReps.value = target
    }

    fun setExercise(exercise: String, durationSeconds: Int? = null) {
        _currentExercise.value = exercise
        poseEvaluator.reset()
        _repCount.value = 0
        // Store the duration for plank exercises
        if (exercise == "plank") {
            _exerciseRemainingSeconds.value = durationSeconds
        }
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
        // Store references for camera switching
        currentContext = context
        currentLifecycleOwner = lifecycleOwner
        currentCameraProvider = cameraProvider
        currentPreviewView = previewView
        if (!::poseEngine.isInitialized) {
            // PERFORMANCE FIX: Use pre-warmed PoseEngine instead of creating new one
            // This eliminates the 2+ second freeze that would occur on first camera use
            val warmedEngine = ModelWarmer.getInstance(context).getWarmedEngine()
            
            if (warmedEngine != null) {
                // Use the pre-warmed engine (instant, no freeze!)
                android.util.Log.i("CameraViewModel", "✓ Using pre-warmed PoseEngine (0ms delay)")
                poseEngine = warmedEngine
            } else {
                PoseEngine(context).also { it.initialize()
                }
            }

            viewModelScope.launch {
                poseEngine.poseResults.collect { result ->
                    if (_sessionState.value == SessionState.ACTIVE) {
                        _poseResult.value = result
                        result?.let {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                                val feedbackMsg = if (SKIP_POSE_EVALUATION) {
                                    null
                                } else {
                                    poseEvaluator.evaluate(it, _currentExercise.value)
                                }

                                if (feedbackMsg != _feedback.value) {
                                    _feedback.value = feedbackMsg
                                }

                                feedbackMsg?.let { msg ->
                                    if (sessionFeedbackHistory.isEmpty() ||
                                        sessionFeedbackHistory.last().text != msg.text) {
                                        sessionFeedbackHistory.add(msg)
                                    }
                                }

                                val newRepCount = poseEvaluator.getRepCount()
                                if (newRepCount != _repCount.value) {
                                    _repCount.value = newRepCount
                                    
                                    // Auto-finish when target reps reached
                                    // Only if: 1) Not plank (time-based), 2) Target reps is set (not Free mode)
                                    if (_currentExercise.value != "plank" && 
                                        _targetReps.value > 0 &&
                                        newRepCount >= _targetReps.value) {
                                        finishSession()
                                    }
                                }
                            }
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
                    val currentState = _sessionState.value
                    if (currentState == SessionState.ACTIVE || currentState == SessionState.COUNTDOWN) {
                        poseEngine.detectPose(imageProxy, _cameraState.value.isFront())
                    }
                    imageProxy.close()
                }
            }

        var selectedCameraSelector: CameraSelector? = null
        val preferredCamera = _cameraState.value.toCameraSelector()
        selectedCameraSelector = when {
            cameraProvider.hasCamera(preferredCamera) -> preferredCamera
            cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
            else -> null
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
        
        // Rebind camera with new camera selector
        val provider = currentCameraProvider
        val previewView = currentPreviewView
        
        if (provider != null && previewView != null) {
            bindCamera(context, lifecycleOwner, provider, previewView)
        }
    }

    fun toggleDelegate(context: android.content.Context, lifecycleOwner: androidx.lifecycle.LifecycleOwner) {
        viewModelScope.launch {
            if (::poseEngine.isInitialized) poseEngine.close()
            poseEngine = PoseEngine(context)
            poseEngine.toggleDelegate()
            poseEngine.initialize()
        }
    }

    fun startSessionCountdown(durationSeconds: Int?) {
        if (_sessionState.value != SessionState.IDLE) return

        viewModelScope.launch {
            _sessionState.value = SessionState.COUNTDOWN

            for (i in 5 downTo 1) {
                _countdownValue.value = i
                delay(1000)
            }

            poseEvaluator.startSession()
            sessionStartTimeMillis = System.currentTimeMillis()
            sessionFeedbackHistory.clear()
            _sessionState.value = SessionState.ACTIVE

            _exerciseElapsedSeconds.value = 0
            _exerciseRemainingSeconds.value = durationSeconds
            _targetDurationSeconds = durationSeconds

            while (_sessionState.value == SessionState.ACTIVE) {
                delay(1000)

                val remaining = _exerciseRemainingSeconds.value
                if (remaining == null) {
                    _exerciseElapsedSeconds.value += 1
                } else {
                    val next = remaining - 1
                    _exerciseRemainingSeconds.value = next
                    if (next <= 0) {
                        finishSession()
                        break
                    }
                }
            }
        }
    }

    fun finishSession() {
        if (_sessionState.value != SessionState.ACTIVE) return

        val now = System.currentTimeMillis()
        val durationMillis = sessionStartTimeMillis?.let { start -> now - start } ?: 0L

        // Use the exercise-aware summary version (the one you are already using later)
        val formSummary = poseEvaluator.getEvaluationSummary(_currentExercise.value)
        val formBreakCount = poseEvaluator.getFormBreakCount()

        val overallScore = LiveSessionResult.calculateScore(
            sessionFeedbackHistory,
            _repCount.value,
            _targetReps.value,
            _currentExercise.value,
            durationMillis,
            _targetDurationSeconds?.times(1000L),
            formBreakCount
        )

        // Analyze feedback to generate summary
        val summarizedFeedback = FeedbackAnalyzer.analyze(
            sessionFeedbackHistory,
            _currentExercise.value,
            _repCount.value
        )

        val current = ExerciseSessionSummary(
            exerciseId = _currentExercise.value,
            exerciseName = _currentExercise.value.replaceFirstChar { it.uppercase() },
            reps = _repCount.value,
            durationMillis = durationMillis,
            feedbackMessages = summarizedFeedback,
            formBreakCount = formBreakCount
        )

        val updatedWorkoutSessions = _workoutSessions.value + current
        _workoutSessions.value = updatedWorkoutSessions

        val totalReps = updatedWorkoutSessions.sumOf { it.reps }
        val totalDurationMillis = updatedWorkoutSessions.sumOf { it.durationMillis }
        val totalExercises = updatedWorkoutSessions.size

        // Common feedback (intersection by text)
        val commonFeedback = if (updatedWorkoutSessions.isNotEmpty()) {
            val firstTexts = updatedWorkoutSessions.first().feedbackMessages.map { it.text }.toSet()
            var intersection = firstTexts
            for (session in updatedWorkoutSessions.drop(1)) {
                intersection = intersection.intersect(session.feedbackMessages.map { it.text }.toSet())
            }
            intersection.mapNotNull { text ->
                updatedWorkoutSessions.first().feedbackMessages.find { it.text == text }
            }
        } else {
            emptyList()
        }

        // All feedback (concatenation)
        val allFeedback = updatedWorkoutSessions.flatMap { it.feedbackMessages }

        val sessionResult = LiveSessionResult(
            exerciseType = _currentExercise.value,
            exerciseName = _currentExercise.value.replaceFirstChar { it.uppercase() },
            targetReps = _targetReps.value,
            completedReps = _repCount.value,
            durationMillis = durationMillis,
            feedbackMessages = summarizedFeedback,
            evaluationSummary = formSummary,
            overallScore = overallScore,
            totalExercises = totalExercises,
            totalReps = totalReps,
            totalDurationMillis = totalDurationMillis,
            commonFeedbackMessages = commonFeedback,
            allFeedbackMessages = allFeedback,
            sessionHistory = updatedWorkoutSessions,
            formBreakCount = formBreakCount
        )

        _sessionResult.value = sessionResult
        _sessionState.value = SessionState.FINISHED
        sessionStartTimeMillis = null

        _exerciseRemainingSeconds.value = null
        _exerciseElapsedSeconds.value = 0
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

        _exerciseRemainingSeconds.value = null
        _exerciseElapsedSeconds.value = 0
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
