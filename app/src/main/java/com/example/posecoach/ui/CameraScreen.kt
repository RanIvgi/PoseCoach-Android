package com.example.posecoach.ui

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
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

    private val _currentExercise = MutableStateFlow("squat")
    val currentExercise: StateFlow<String> = _currentExercise.asStateFlow()

    private val _targetReps = MutableStateFlow(10)
    val targetReps: StateFlow<Int> = _targetReps.asStateFlow()

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

    private var sessionStartTimeMillis: Long? = null
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

    fun resetRepCount() {
        poseEvaluator.reset()
        _repCount.value = 0
    }

    fun finishSessionAndGoHome() {
        _navigateHomeAfterSummary.value = true
        finishSession()
    }

    fun bindCamera(
        context: android.content.Context,
        lifecycleOwner: LifecycleOwner,
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
                            val feedbackMsg =
                                if (SKIP_POSE_EVALUATION) null
                                else poseEvaluator.evaluate(pose, _currentExercise.value)

                            feedbackMsg?.let { msg ->
                                if (sessionFeedbackHistory.isEmpty() ||
                                    sessionFeedbackHistory.last().text != msg.text
                                ) {
                                    sessionFeedbackHistory.add(msg)
                                }
                                _feedback.value = msg
                            }

                            val newRepCount = poseEvaluator.getRepCount()
                            if (newRepCount != _repCount.value) {
                                _repCount.value = newRepCount
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

        val preferred = _cameraState.value.toCameraSelector()
        val selected: CameraSelector? = when {
            cameraProvider.hasCamera(preferred) -> preferred
            cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
            else -> null
        }

        if (selected == null) {
            _cameraError.value = "No suitable camera found on this device."
            return
        }

        _cameraError.value = null

        try {
            cameraProvider.bindToLifecycle(lifecycleOwner, selected, preview, imageAnalysis)
        } catch (e: Exception) {
            _cameraError.value = "Camera binding failed: ${e.message}"
        }
    }

    fun switchCamera(context: android.content.Context, lifecycleOwner: LifecycleOwner) {
        _cameraState.value = _cameraState.value.toggle()
    }

    fun toggleDelegate(context: android.content.Context, lifecycleOwner: LifecycleOwner) {
        viewModelScope.launch {
            if (::poseEngine.isInitialized) poseEngine.close()
            poseEngine = PoseEngine(context).also { it.initialize() }
            poseEngine.toggleDelegate()
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
            _feedback.value = null
            _sessionState.value = SessionState.ACTIVE

            _exerciseElapsedSeconds.value = 0
            _exerciseRemainingSeconds.value = durationSeconds

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

        val sortedFeedback =
            sessionFeedbackHistory
                .distinctBy { it.text }
                .sortedWith(compareBy { severityPriority(it.severity) })

        val current = ExerciseSessionSummary(
            exerciseId = _currentExercise.value,
            exerciseName = _currentExercise.value.replaceFirstChar { it.uppercase() },
            reps = _repCount.value,
            durationMillis = durationMillis,
            feedbackMessages = sortedFeedback
        )

        val updatedWorkoutSessions = _workoutSessions.value + current
        _workoutSessions.value = updatedWorkoutSessions

        val commonFeedback =
            if (updatedWorkoutSessions.isNotEmpty()) {
                val firstTexts = updatedWorkoutSessions.first().feedbackMessages.map { it.text }.toSet()
                var intersection = firstTexts
                for (session in updatedWorkoutSessions.drop(1)) {
                    intersection = intersection.intersect(session.feedbackMessages.map { it.text }.toSet())
                }
                intersection
                    .mapNotNull { text -> updatedWorkoutSessions.first().feedbackMessages.find { it.text == text } }
                    .sortedWith(compareBy { severityPriority(it.severity) })
            } else emptyList()

        val allFeedback =
            updatedWorkoutSessions
                .flatMap { it.feedbackMessages }
                .distinctBy { it.text }
                .sortedWith(compareBy { severityPriority(it.severity) })

        _sessionResult.value = LiveSessionResult(
            exerciseType = _currentExercise.value,
            exerciseName = _currentExercise.value.replaceFirstChar { it.uppercase() },
            targetReps = _targetReps.value,
            completedReps = _repCount.value,
            durationMillis = durationMillis,
            feedbackMessages = sortedFeedback,
            evaluationSummary = poseEvaluator.getEvaluationSummary(_currentExercise.value),
            overallScore = LiveSessionResult.calculateScore(sessionFeedbackHistory),
            totalExercises = updatedWorkoutSessions.size,
            totalReps = updatedWorkoutSessions.sumOf { it.reps },
            totalDurationMillis = updatedWorkoutSessions.sumOf { it.durationMillis },
            commonFeedbackMessages = commonFeedback,
            allFeedbackMessages = allFeedback
        )

        _sessionState.value = SessionState.FINISHED
        sessionStartTimeMillis = null
        _exerciseRemainingSeconds.value = null
        _exerciseElapsedSeconds.value = 0
    }

    override fun onCleared() {
        super.onCleared()
        if (::poseEngine.isInitialized) poseEngine.close()
        cameraExecutor.shutdown()
    }
}