package com.example.posecoach.ui

import android.Manifest
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Memory
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import com.google.accompanist.permissions.isGranted

/**
 * CameraScreen - Student 1's Main UI Screen
 * 
 * This composable combines:
 * - CameraX preview (live camera feed)
 * - PoseOverlay (skeleton drawing)
 * - Feedback display (from Student 3's evaluation)
 * - Camera switch button
 * - FPS counter
 * 
 * Student 1 TODO List:
 * 1. Add exercise selection UI (dropdown or buttons)
 * 2. Add start/stop session buttons
 * 3. Display rep count from evaluator
 * 4. Add settings screen navigation
 * 5. Add session summary screen
 * 6. Improve feedback display styling
 * 7. Add sound/haptic feedback for form issues
 * 8. Add onboarding/tutorial overlay
 * 9. Implement dark/light theme toggle
 * 10. Add recording/playback functionality
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    viewModel: CameraViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Request camera permission
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    
    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }
    
    // Collect UI state
    val poseResult by viewModel.poseResult.collectAsState()
    val feedback by viewModel.feedback.collectAsState()
    val fps by viewModel.fps.collectAsState()
    val cameraState by viewModel.cameraState.collectAsState()
    val useGpu by viewModel.useGpuDelegate.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (cameraPermission.status.isGranted) {
            // Camera preview
            CameraPreview(
                cameraState = cameraState,
                onCameraReady = { provider, previewView ->
                    viewModel.bindCamera(context, lifecycleOwner, provider, previewView)
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // Pose skeleton overlay
            PoseOverlay(
                poseResult = poseResult,
                modifier = Modifier.fillMaxSize()
            )
            
            // UI Controls overlay
            CameraControls(
                feedback = feedback,
                fps = fps,
                useGpu = useGpu,
                onCameraSwitch = { viewModel.switchCamera(context, lifecycleOwner) },
                onToggleDelegate = { viewModel.toggleDelegate(context, lifecycleOwner) },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Permission not granted
            PermissionDeniedScreen(
                onRequestPermission = { cameraPermission.launchPermissionRequest() }
            )
        }
    }
}

/**
 * Camera preview using CameraX.
 */
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
    
    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}

/**
 * UI controls overlaying the camera.
 */
@Composable
private fun CameraControls(
    feedback: FeedbackMessage?,
    fps: Float,
    useGpu: Boolean,
    onCameraSwitch: () -> Unit,
    onToggleDelegate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // FPS counter and GPU/CPU indicator (top-right)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
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
        
        // Feedback display (bottom center)
        feedback?.let {
            FeedbackDisplay(
                feedback = it,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
            )
        }
        
        // Bottom buttons row
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // GPU/CPU toggle button
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
            
            // Camera switch button
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

/**
 * Feedback message display.
 * TODO (Student 1): Enhance styling and animations
 */
@Composable
private fun FeedbackDisplay(
    feedback: FeedbackMessage,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (feedback.severity) {
        FeedbackSeverity.INFO -> Color(0xFF4CAF50) // Green
        FeedbackSeverity.WARNING -> Color(0xFFFF9800) // Orange
        FeedbackSeverity.ERROR -> Color(0xFFF44336) // Red
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

/**
 * Screen shown when camera permission is denied.
 */
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

/**
 * ViewModel for CameraScreen.
 * Manages camera state, pose detection, and feedback evaluation.
 */
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
    
    private var currentExercise = "general" // TODO: Make this configurable by Student 1
    
    /**
     * Bind camera to lifecycle and start pose detection.
     */
    fun bindCamera(
        context: android.content.Context,
        lifecycleOwner: androidx.lifecycle.LifecycleOwner,
        cameraProvider: ProcessCameraProvider,
        previewView: PreviewView
    ) {
        // Initialize PoseEngine if not done
        if (!::poseEngine.isInitialized) {
            poseEngine = PoseEngine(context)
            poseEngine.initialize()
            
            // Collect pose results
            viewModelScope.launch {
                poseEngine.poseResults.collect { result ->
                    Log.d("CameraViewModel", "New PoseResult received: hasPose=${result?.hasPose()}, landmarks=${result?.landmarks?.size ?: 0}")
                    _poseResult.value = result
                    
                    // Evaluate pose and generate feedback
                    result?.let {
                        val feedbackMsg = poseEvaluator.evaluate(it, currentExercise)
                        _feedback.value = feedbackMsg
                    }
                }
            }
            
            // Collect FPS
            viewModelScope.launch {
                poseEngine.fps.collect { fpsValue ->
                    _fps.value = fpsValue
                }
            }
            
            // Collect GPU delegate state
            viewModelScope.launch {
                poseEngine.useGpuDelegate.collect { useGpu ->
                    _useGpuDelegate.value = useGpu
                }
            }
        }
        
        // Unbind previous use cases
        cameraProvider.unbindAll()
        
        // Preview use case
        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
        
        // Image analysis use case for pose detection
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor) { imageProxy ->
                    poseEngine.detectPose(imageProxy, _cameraState.value.isFront())
                    imageProxy.close()
                }
            }
        
        // Bind use cases to lifecycle
        try {
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                _cameraState.value.toCameraSelector(),
                preview,
                imageAnalysis
            )
        } catch (e: Exception) {
            android.util.Log.e("CameraViewModel", "Camera binding failed", e)
        }
    }
    
    /**
     * Switch between front and rear camera.
     */
    fun switchCamera(
        context: android.content.Context,
        lifecycleOwner: androidx.lifecycle.LifecycleOwner
    ) {
        _cameraState.value = _cameraState.value.toggle()
        
        // Rebind camera with new state
        viewModelScope.launch {
            val cameraProvider = ProcessCameraProvider.getInstance(context).get()
            // Note: previewView needs to be passed here
            // TODO (Student 1): Refactor to handle this better
        }
    }
    
    /**
     * Toggle between GPU and CPU delegate.
     * Reinitializes PoseEngine with new delegate.
     */
    fun toggleDelegate(
        context: android.content.Context,
        lifecycleOwner: androidx.lifecycle.LifecycleOwner
    ) {
        viewModelScope.launch {
            // Close existing engine
            if (::poseEngine.isInitialized) {
                poseEngine.close()
            }
            
            // Create new engine with toggled delegate
            poseEngine = PoseEngine(context)
            poseEngine.toggleDelegate()
            poseEngine.initialize()
            
            // Restart result collection
            launch {
                poseEngine.poseResults.collect { result ->
                    Log.d("CameraViewModel", "New PoseResult received: hasPose=${result?.hasPose()}, landmarks=${result?.landmarks?.size ?: 0}")
                    _poseResult.value = result
                    
                    result?.let {
                        val feedbackMsg = poseEvaluator.evaluate(it, currentExercise)
                        _feedback.value = feedbackMsg
                    }
                }
            }
            
            launch {
                poseEngine.fps.collect { fpsValue ->
                    _fps.value = fpsValue
                }
            }
            
            launch {
                poseEngine.useGpuDelegate.collect { useGpu ->
                    _useGpuDelegate.value = useGpu
                }
            }
            
            // Rebind camera to restart analysis with new engine
            val cameraProvider = ProcessCameraProvider.getInstance(context).get()
            // Note: This is a simplified approach
            // TODO (Student 1): Pass previewView properly for complete rebinding
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        poseEngine.close()
        cameraExecutor.shutdown()
    }
}
