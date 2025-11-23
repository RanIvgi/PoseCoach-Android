package com.example.posecoach.ui

import android.Manifest
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.posecoach.data.CameraState
import com.example.posecoach.data.FeedbackMessage
import com.example.posecoach.data.FeedbackSeverity
import com.example.posecoach.data.PoseResult
import com.example.posecoach.logic.DefaultPoseEvaluator
import com.example.posecoach.logic.PoseEvaluator
import com.example.posecoach.pose.PoseEngine
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

// Exercise model and definitions

private data class ExerciseUi(
    val id: String,
    val title: String,
    val description: String,
    val instructions: List<String>
)

private val exercises = listOf(
    ExerciseUi(
        id = "squat",
        title = "Squat",
        description = "Lower body exercise focusing on quads, glutes and core.",
        instructions = listOf(
            "Stand with feet shoulder-width apart.",
            "Push your hips back as if sitting into a chair.",
            "Keep your chest up and back straight.",
            "Bend your knees until thighs are parallel to the ground.",
            "Push through heels to stand back up."
        )
    ),
    ExerciseUi(
        id = "pushup",
        title = "Push-up",
        description = "Upper body exercise working chest, shoulders, arms and core.",
        instructions = listOf(
            "Place hands slightly wider than shoulder-width.",
            "Keep your body in a straight line from head to heels.",
            "Lower yourself until your chest nearly touches the floor.",
            "Keep elbows tucked at about 45 degrees.",
            "Push back up while keeping core engaged."
        )
    ),
    ExerciseUi(
        id = "plank",
        title = "Plank",
        description = "Core stability exercise working abs, glutes and back.",
        instructions = listOf(
            "Place elbows under shoulders and extend legs back.",
            "Keep body in a straight line (no arching).",
            "Engage your core and squeeze glutes.",
            "Look down to keep neck neutral.",
            "Hold as long as you can with proper form."
        )
    )
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    navBackToStart: () -> Unit,
    viewModel: CameraViewModel = viewModel()
) {
    val currentExercise by viewModel.currentExercise.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    val poseResult by viewModel.poseResult.collectAsState()
    val feedback by viewModel.feedback.collectAsState()
    val fps by viewModel.fps.collectAsState()
    val cameraState by viewModel.cameraState.collectAsState()
    val useGpu by viewModel.useGpuDelegate.collectAsState()
    val cameraError by viewModel.cameraError.collectAsState()
    val repCount by viewModel.repCount.collectAsState()
    val sessionState by viewModel.sessionState.collectAsState()
    val countdownValue by viewModel.countdownValue.collectAsState()
    val summaryText by viewModel.summaryText.collectAsState()

    // Which exercise info screen is currently open (if any)
    var infoExerciseId by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (cameraPermission.status.isGranted) {
            CameraPreview(
                cameraState = cameraState,
                onCameraReady = { provider, previewView ->
                    viewModel.bindCamera(context, lifecycleOwner, provider, previewView)
                },
                modifier = Modifier.fillMaxSize()
            )

            PoseOverlay(
                poseResult = poseResult,
                modifier = Modifier.fillMaxSize()
            )

            CameraControls(
                feedback = feedback,
                fps = fps,
                useGpu = useGpu,
                repCount = repCount,
                sessionState = sessionState,
                currentExercise = currentExercise,
                onCameraSwitch = { viewModel.switchCamera(context, lifecycleOwner) },
                onToggleDelegate = { viewModel.toggleDelegate(context, lifecycleOwner) },
                onResetRepCount = { viewModel.resetRepCount() },
                onStartSession = { viewModel.startSessionCountdown() },
                onFinishSession = { viewModel.finishSession() },
                onExerciseSelected = { id ->
                    viewModel.setExercise(id)
                    infoExerciseId = id
                },
                onBackToHome = navBackToStart,
                modifier = Modifier.fillMaxSize()
            )

            if (sessionState == SessionState.COUNTDOWN) {
                CountdownOverlay(countdownValue = countdownValue)
            }

            if (sessionState == SessionState.FINISHED && summaryText != null) {
                SummaryDialog(
                    summaryText = summaryText!!,
                    onDismiss = { viewModel.resetSession() }
                )
            }

            // Exercise info overlay (only when IDLE and exercise chosen)
            if (sessionState == SessionState.IDLE) {
                infoExerciseId?.let { id ->
                    val exercise = exercises.find { it.id == id }
                    if (exercise != null) {
                        ExerciseInfoOverlay(
                            exercise = exercise,
                            onCancel = { infoExerciseId = null },
                            onConfirmStart = {
                                infoExerciseId = null
                                viewModel.startSessionCountdown()
                            }
                        )
                    }
                }
            }

            cameraError?.let { errorMessage ->
                ErrorOverlay(errorMessage = errorMessage)
            }
        } else {
            PermissionDeniedScreen(
                onRequestPermission = { cameraPermission.launchPermissionRequest() }
            )
        }
    }
}

@Composable
private fun CameraPreview(
    cameraState: CameraState,
    onCameraReady: (ProcessCameraProvider, PreviewView) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val previewView = remember { PreviewView(context) }

    LaunchedEffect(cameraState) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
        onCameraReady(cameraProvider, previewView)
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

@Composable
private fun CameraControls(
    feedback: FeedbackMessage?,
    fps: Float,
    useGpu: Boolean,
    repCount: Int,
    sessionState: SessionState,
    currentExercise: String,
    onCameraSwitch: () -> Unit,
    onToggleDelegate: () -> Unit,
    onResetRepCount: () -> Unit,
    onStartSession: () -> Unit,
    onFinishSession: () -> Unit,
    onExerciseSelected: (String) -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        if (sessionState == SessionState.IDLE) {
            ExerciseSelector(
                currentExercise = currentExercise,
                onExerciseSelected = onExerciseSelected,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 90.dp, end = 16.dp)
            )
        }

        if (sessionState == SessionState.ACTIVE) {
            Text(
                text = "Reps: $repCount",
                color = Color.White,
                style = MaterialTheme.typography.h5,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(8.dp)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {

            if (sessionState == SessionState.ACTIVE) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onBackToHome,
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF1565C0)
                        )
                    ) {
                        Text(
                            text = "Home",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onFinishSession,
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFFD32F2F)
                        )
                    ) {
                        Text(
                            text = "End",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = "FPS: %.1f".format(fps),
                color = Color.White,
                style = MaterialTheme.typography.caption,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (useGpu) "GPU" else "CPU",
                color = if (useGpu) Color.Green else Color.Cyan,
                style = MaterialTheme.typography.caption,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(8.dp)
            )
        }

        if (sessionState == SessionState.ACTIVE) {
            feedback?.let {
                FeedbackDisplay(
                    feedback = it,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 100.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (sessionState == SessionState.IDLE) {
                FloatingActionButton(
                    onClick = onStartSession,
                    backgroundColor = Color(0xFF4CAF50)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Start Session",
                        tint = Color.White
                    )
                }
            }

            if (sessionState == SessionState.ACTIVE) {
                FloatingActionButton(
                    onClick = onFinishSession,
                    backgroundColor = Color(0xFFF44336)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = "Finish Session",
                        tint = Color.White
                    )
                }
            }

            if (sessionState == SessionState.ACTIVE) {
                FloatingActionButton(
                    onClick = onResetRepCount,
                    backgroundColor = Color.Gray
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Reset Rep Count",
                        tint = Color.White
                    )
                }
            }

            FloatingActionButton(
                onClick = onToggleDelegate,
                backgroundColor = MaterialTheme.colors.secondary
            ) {
                Icon(
                    imageVector = Icons.Filled.Memory,
                    contentDescription = if (useGpu) "Switch to CPU" else "Switch to GPU",
                    tint = Color.White
                )
            }

            FloatingActionButton(
                onClick = onCameraSwitch,
                backgroundColor = MaterialTheme.colors.primary
            ) {
                Icon(
                    imageVector = Icons.Filled.Cameraswitch,
                    contentDescription = "Switch Camera",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun ExerciseSelector(
    currentExercise: String,
    onExerciseSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        Text(
            text = "Choose exercise",
            color = Color.White,
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        exercises.forEach { exercise ->
            val selected = exercise.id == currentExercise

            Card(
                backgroundColor = if (selected) Color(0xFF0B3C91) else Color(0x330B3C91),
                shape = RoundedCornerShape(12.dp),
                elevation = if (selected) 6.dp else 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onExerciseSelected(exercise.id) }
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = exercise.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    if (selected) {
                        Text(
                            text = "Tap to view instructions",
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.caption
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseInfoOverlay(
    exercise: ExerciseUi,
    onCancel: () -> Unit,
    onConfirmStart: () -> Unit
) {
    var understood by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            backgroundColor = Color(0xFF0D47A1),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = exercise.title,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = exercise.description,
                    color = Color.White,
                    style = MaterialTheme.typography.body1
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "How to perform:",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )

                exercise.instructions.forEach { step ->
                    Text(
                        text = "• $step",
                        color = Color.White,
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = understood,
                        onCheckedChange = { understood = it }
                    )
                    Text(
                        text = "I understand the instructions",
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onCancel) {
                        Text(text = "Cancel", color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConfirmStart,
                        enabled = understood,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF0B3C91),
                            disabledBackgroundColor = Color(0xFF5476A8)
                        )
                    ) {
                        Text(
                            text = "Start exercise",
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CountdownOverlay(countdownValue: Int) {
    val animatedScale by animateFloatAsState(
        targetValue = 1.2f,
        animationSpec = tween(durationMillis = 500)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = countdownValue.toString(),
            fontSize = 120.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.scale(if (countdownValue > 0) animatedScale else 1f)
        )
    }
}

@Composable
private fun SummaryDialog(summaryText: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Session Summary") },
        text = { Text(summaryText) },
        confirmButton = {
            Button(onDismiss) {
                Text("OK")
            }
        }
    )
}

@Composable
private fun FeedbackDisplay(
    feedback: FeedbackMessage,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (feedback.severity) {
        FeedbackSeverity.INFO -> Color(0xFF4ED58A)
        FeedbackSeverity.WARNING -> Color(0xFFFFC93C)
        FeedbackSeverity.ERROR -> Color(0xFFE53935)
    }

    Card(
        modifier = modifier
            .widthIn(max = 350.dp)
            .padding(horizontal = 16.dp),
        backgroundColor = backgroundColor.copy(alpha = 0.9f),
        elevation = 4.dp
    ) {
        Text(
            text = feedback.text,
            color = Color.White,
            style = MaterialTheme.typography.body1,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun PermissionDeniedScreen(
    onRequestPermission: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Camera permission is required",
                style = MaterialTheme.typography.h6,
                textAlign = TextAlign.Center
            )
            Button(onRequestPermission) {
                Text("Grant Permission")
            }
        }
    }
}

@Composable
private fun ErrorOverlay(errorMessage: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            backgroundColor = MaterialTheme.colors.error,
            elevation = 8.dp,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Error:",
                    style = MaterialTheme.typography.h6,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.body1,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

enum class SessionState { IDLE, COUNTDOWN, ACTIVE, FINISHED }

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
                    poseEngine.detectPose(imageProxy, _cameraState.value.isFront())
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

    fun setExercise(exercise: String) {
        _currentExercise.value = exercise
        poseEvaluator.reset()
        _repCount.value = 0
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
                for (i in 5 downTo 1) {
                    _countdownValue.value = i
                    delay(1000)
                }
                poseEvaluator.startSession()
                _sessionState.value = SessionState.ACTIVE
            }
        }
    }

    fun finishSession() {
        if (_sessionState.value == SessionState.ACTIVE) {
            _summaryText.value = poseEvaluator.getEvaluationSummary()
            _sessionState.value = SessionState.FINISHED
        }
    }

    fun resetSession() {
        poseEvaluator.reset()
        _repCount.value = 0
        _sessionState.value = SessionState.IDLE
        _summaryText.value = null
        _feedback.value = null
    }

    fun resetRepCount() {
        poseEvaluator.reset()
        _repCount.value = 0
    }

    override fun onCleared() {
        super.onCleared()
        poseEngine.close()
        cameraExecutor.shutdown()
    }
}

private fun Modifier.scale(scale: Float) =
    this.then(Modifier.graphicsLayer(scaleX = scale, scaleY = scale))
