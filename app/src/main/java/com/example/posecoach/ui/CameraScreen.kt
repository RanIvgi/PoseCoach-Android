package com.example.posecoach.ui

import android.Manifest
import android.util.Log
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.posecoach.data.CameraState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

// If these are not in this package, change the import paths to where they actually are.
// (If they ARE in com.example.posecoach.ui, you can delete these imports and it will still work.)
import com.example.posecoach.ui.SessionState
import com.example.posecoach.ui.exercises
import com.example.posecoach.ui.ExerciseInfoOverlay
import com.example.posecoach.ui.PoseOverlay
import com.example.posecoach.ui.CameraControls

@Composable
fun LogCompositions(tag: String) {
    class Ref(var value: Int)
    val ref = remember { Ref(0) }
    SideEffect {
        ref.value++
        Log.d("Recomposition-Track", "$tag recomposed ${ref.value} times")
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    navBackToStart: () -> Unit,
    navToSessionResults: () -> Unit,
    viewModel: CameraViewModel = viewModel()
) {
    LogCompositions("CameraScreen")

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    val currentExercise by viewModel.currentExercise.collectAsState()
    val targetReps by viewModel.targetReps.collectAsState()
    val feedback by viewModel.feedback.collectAsState()
    val fps by viewModel.fps.collectAsState()
    val cameraState by viewModel.cameraState.collectAsState()
    val useGpu by viewModel.useGpuDelegate.collectAsState()
    val cameraError by viewModel.cameraError.collectAsState()
    val repCount by viewModel.repCount.collectAsState()
    val sessionState by viewModel.sessionState.collectAsState()
    val countdownValue by viewModel.countdownValue.collectAsState()
    val sessionResult by viewModel.sessionResult.collectAsState()
    val exerciseRemainingSeconds by viewModel.exerciseRemainingSeconds.collectAsState()
    val exerciseElapsedSeconds by viewModel.exerciseElapsedSeconds.collectAsState()

    val isPlank = currentExercise == "plank"

    var infoExerciseId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(sessionResult) {
        if (sessionResult != null) {
            navToSessionResults()
        }
    }

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
                poseResultFlow = viewModel.poseResult,
                cameraState = cameraState,
                modifier = Modifier.fillMaxSize()
            )

            CameraControls(
                feedback = feedback,
                fps = fps,
                useGpu = useGpu,
                repCount = if (isPlank) 0 else repCount,
                sessionState = sessionState,
                currentExercise = currentExercise,
                targetReps = targetReps,
                onCameraSwitch = { viewModel.switchCamera(context, lifecycleOwner) },
                onToggleDelegate = { viewModel.toggleDelegate(context, lifecycleOwner) },
                onResetRepCount = { viewModel.resetRepCount() },
                onStartSession = { viewModel.startSessionCountdown(null) },
                onFinishSession = { viewModel.finishSession() },
                onExerciseSelected = {
                    viewModel.setExercise(it)
                    infoExerciseId = it
                },
                onTargetRepsChange = { viewModel.setTargetReps(it) },
                onBackToHome = {
                    if (sessionState == SessionState.ACTIVE) {
                        viewModel.finishSessionAndGoHome()
                    } else {
                        navBackToStart()
                    }
                },
                showReps = !isPlank,
                modifier = Modifier.fillMaxSize()
            )

            if (sessionState == SessionState.ACTIVE) {
                val shownSeconds = exerciseRemainingSeconds ?: exerciseElapsedSeconds

                if (isPlank) {
                    PlankTimerOverlay(
                        timeText = formatMmSs(shownSeconds),
                        caption = if (exerciseRemainingSeconds != null) "Time left" else "Time"
                    )
                } else {
                    TimerOverlayTop(text = formatMmSs(shownSeconds))
                }
            }

            if (sessionState == SessionState.COUNTDOWN) {
                CountdownOverlay(countdownValue)
            }

            if (sessionState == SessionState.IDLE) {
                infoExerciseId?.let { id ->
                    exercises.find { it.id == id }?.let { exercise ->
                        ExerciseInfoOverlay(
                            exercise = exercise,
                            onCancel = { infoExerciseId = null },
                            onConfirmStart = {
                                infoExerciseId = null
                                viewModel.startSessionCountdown(it)
                            }
                        )
                    }
                }
            }

            cameraError?.let {
                ErrorOverlay(it)
            }

        } else {
            PermissionDeniedScreen {
                cameraPermission.launchPermissionRequest()
            }
        }
    }
}

@Composable
fun TimerOverlayTop(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(top = 24.dp)
                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun PlankTimerOverlay(timeText: String, caption: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopStart
    ) {
        Column(
            modifier = Modifier
                .padding(top = 18.dp)
                .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = caption,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 10.sp
            )
            Text(
                text = timeText,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CountdownOverlay(countdownValue: Int) {
    val scale by animateFloatAsState(
        targetValue = 1.2f,
        animationSpec = tween(500)
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
            modifier = Modifier.graphicsLayer(
                scaleX = scale,
                scaleY = scale
            )
        )
    }
}

@Composable
fun PermissionDeniedScreen(onRequestPermission: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Camera permission is required", textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRequestPermission) {
                Text("Grant permission")
            }
        }
    }
}

@Composable
fun ErrorOverlay(message: String) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Text(message, color = Color.White)
    }
}

@Composable
fun CameraPreview(
    cameraState: CameraState,
    onCameraReady: (ProcessCameraProvider, PreviewView) -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    val previewView = remember { PreviewView(context) }

    DisposableEffect(cameraState) {
        val provider = ProcessCameraProvider.getInstance(context).get()
        onCameraReady(provider, previewView)
        onDispose { provider.unbindAll() }
    }

    AndroidView({ previewView }, modifier)
}

private fun formatMmSs(sec: Int): String =
    String.format("%02d:%02d", sec / 60, sec % 60)
