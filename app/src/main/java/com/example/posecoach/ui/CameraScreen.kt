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
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
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

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    viewModel: CameraViewModel = viewModel()
) {
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
                onCameraSwitch = { viewModel.switchCamera(context, lifecycleOwner) },
                onToggleDelegate = { viewModel.toggleDelegate(context, lifecycleOwner) },
                onResetRepCount = { viewModel.resetRepCount() },
                onStartSession = { viewModel.startSessionCountdown() },
                onFinishSession = { viewModel.finishSession() },
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
    onCameraSwitch: () -> Unit,
    onToggleDelegate: () -> Unit,
    onResetRepCount: () -> Unit,
    onStartSession: () -> Unit,
    onFinishSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
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
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "FPS: %.1f".format(fps),
                color = Color.White,
                style = MaterialTheme.typography.caption,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f)).padding(8.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (useGpu) "GPU" else "CPU",
                color = if (useGpu) Color.Green else Color.Cyan,
                style = MaterialTheme.typography.caption,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f)).padding(8.dp)
            )
        }
        
        if (sessionState == SessionState.ACTIVE) {
            feedback?.let {
                FeedbackDisplay(
                    feedback = it,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp)
                )
            }
        }
        
        Row(
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (sessionState == SessionState.IDLE) {
                FloatingActionButton(onClick = onStartSession, backgroundColor = Color(0xFF4CAF50)) {
                    Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = "Start Session", tint = Color.White)
                }
            }

            if (sessionState == SessionState.ACTIVE) {
                FloatingActionButton(onClick = onFinishSession, backgroundColor = Color(0xFFF44336)) {
                    Icon(imageVector = Icons.Filled.Stop, contentDescription = "Finish Session", tint = Color.White)
                }
            }

            if (sessionState == SessionState.ACTIVE) {
                FloatingActionButton(onClick = onResetRepCount, backgroundColor = Color.Gray) {
                    Icon(imageVector = Icons.Filled.Refresh, contentDescription = "Reset Rep Count", tint = Color.White)
                }
            }
            
            FloatingActionButton(onClick = onToggleDelegate, backgroundColor = MaterialTheme.colors.secondary) {
                Icon(imageVector = Icons.Filled.Memory, contentDescription = if (useGpu) "Switch to CPU" else "Switch to GPU", tint = Color.White)
            }
            
            FloatingActionButton(onClick = onCameraSwitch, backgroundColor = MaterialTheme.colors.primary) {
                Icon(imageVector = Icons.Filled.Cameraswitch, contentDescription = "Switch Camera", tint = Color.White)
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
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
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
            Button(onClick = onDismiss) {
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
    // Semantic colors: green = info/success, yellow = warning, red = error
    val backgroundColor = when (feedback.severity) {
        FeedbackSeverity.INFO -> Color(0xFF4ED58A)     // green
        FeedbackSeverity.WARNING -> Color(0xFFFFC93C) // yellow / amber
        FeedbackSeverity.ERROR -> Color(0xFFE53935)   // strong red
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
            Button(onClick = onRequestPermission) {
                Text("Grant Permission")
            }
        }
    }
}

@Composable
private fun ErrorOverlay(errorMessage: String) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)),
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
                Text(text = "Error:", style = MaterialTheme.typography.h6, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = errorMessage, style = MaterialTheme.typography.body1, color = Color.White, textAlign = TextAlign.Center)
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
    
    private var currentExercise = "squat"
    
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
                            val feedbackMsg = poseEvaluator.evaluate(it, currentExercise)
                            _feedback.value = feedbackMsg
                            _repCount.value = poseEvaluator.getRepCount()
                        }
                    } else {
                        _poseResult.value = null // Clear pose when not active
                    }
                }
            }
            
            viewModelScope.launch { poseEngine.fps.collect { _fps.value = it } }
            viewModelScope.launch { poseEngine.useGpuDelegate.collect { _useGpuDelegate.value = it } }
        }
        
        cameraProvider.unbindAll()
        
        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
        
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
            cameraProvider.bindToLifecycle(lifecycleOwner, selectedCameraSelector, preview, imageAnalysis)
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
private fun Modifier.scale(scale: Float) = this.then(Modifier.graphicsLayer(scaleX = scale, scaleY = scale))
