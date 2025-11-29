package com.example.posecoach.ui

import android.Manifest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    navBackToStart: () -> Unit,
    viewModel: CameraViewModel = viewModel()
) {
    val currentExercise by viewModel.currentExercise.collectAsState()
    val targetReps by viewModel.targetReps.collectAsState()
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
    val navigateHomeAfterSummary by viewModel.navigateHomeAfterSummary.collectAsState()

    var infoExerciseId by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (cameraPermission.status.isGranted) {
            if (sessionState == SessionState.COUNTDOWN || sessionState == SessionState.ACTIVE) {
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
            }

            CameraControls(
                feedback = feedback,
                fps = fps,
                useGpu = useGpu,
                repCount = repCount,
                sessionState = sessionState,
                currentExercise = currentExercise,
                targetReps = targetReps,
                onCameraSwitch = { viewModel.switchCamera(context, lifecycleOwner) },
                onToggleDelegate = { viewModel.toggleDelegate(context, lifecycleOwner) },
                onResetRepCount = { viewModel.resetRepCount() },
                onStartSession = { viewModel.startSessionCountdown() },
                onFinishSession = { viewModel.finishSession() },
                onExerciseSelected = { id ->
                    viewModel.setExercise(id)
                    infoExerciseId = id
                },
                onTargetRepsChange = { value -> viewModel.setTargetReps(value) },
                onBackToHome = {
                    if (sessionState == SessionState.ACTIVE) {
                        viewModel.finishSessionAndGoHome()
                    } else {
                        navBackToStart()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (sessionState == SessionState.COUNTDOWN) {
                CountdownOverlay(countdownValue = countdownValue)
            }

            if (sessionState == SessionState.FINISHED && summaryText != null) {
                SummaryDialog(
                    summaryText = summaryText!!,
                    onDismiss = {
                        val shouldGoHome = navigateHomeAfterSummary
                        viewModel.resetSession()
                        if (shouldGoHome) {
                            navBackToStart()
                        }
                    }
                )
            }

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
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
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

private fun Modifier.scale(scale: Float) =
    this.then(Modifier.graphicsLayer(scaleX = scale, scaleY = scale))
