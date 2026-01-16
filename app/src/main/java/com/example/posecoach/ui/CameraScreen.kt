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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.posecoach.data.CameraState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.core.content.ContextCompat
import kotlin.math.min

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
    val repsRemaining by viewModel.repsRemaining.collectAsState()
    val sessionState by viewModel.sessionState.collectAsState()
    val countdownValue by viewModel.countdownValue.collectAsState()
    val sessionResult by viewModel.sessionResult.collectAsState()
    val exerciseRemainingSeconds by viewModel.exerciseRemainingSeconds.collectAsState()
    val exerciseElapsedSeconds by viewModel.exerciseElapsedSeconds.collectAsState()

    val isPlank = currentExercise == "plank"

    LaunchedEffect(sessionResult) {
        if (sessionResult != null) navToSessionResults()
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
                repsRemaining = if (isPlank) 0 else repsRemaining,
                sessionState = sessionState,
                currentExercise = currentExercise,
                targetReps = targetReps,
                onCameraSwitch = { viewModel.switchCamera(context, lifecycleOwner) },
                onToggleDelegate = { viewModel.toggleDelegate(context, lifecycleOwner) },
                onResetRepCount = { viewModel.resetRepCount() },
                onStartSession = { viewModel.startSessionCountdown(null) },
                onFinishSession = { viewModel.finishSession() },
                onBackToHome = {
                    if (sessionState == SessionState.ACTIVE) viewModel.finishSessionAndGoHome()
                    else navBackToStart()
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
                    TimerOverlayTop(
                        text = formatMmSs(shownSeconds),
                        sizeMultiplier = 0.8f
                    )
                }
            }

            if (sessionState == SessionState.COUNTDOWN) {
                CountdownOverlay(countdownValue)
            }

            cameraError?.let { ErrorOverlay(it) }

        } else {
            PermissionDeniedScreen { cameraPermission.launchPermissionRequest() }
        }
    }
}

@Composable
fun TimerOverlayTop(
    text: String,
    sizeMultiplier: Float = 1f
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter
    ) {
        val cfg = LocalConfiguration.current
        val w = maxWidth.value

        // Samsung-friendly: scale from WIDTH, and compensate for fontScale
        val scale = (w / 360f).clamp(0.85f, 1.35f)
        val fontScale = cfg.fontScale.clamp(0.85f, 1.25f)
        val finalTextScale = (scale / fontScale).clamp(0.85f, 1.35f)

        val topPad = (10f * scale).dp
        val font = (24f * finalTextScale * sizeMultiplier)
            .clamp(14f, 30f)
            .sp
        val corner = (14f * scale).clamp(12f, 22f).dp
        val hPad = (14f * scale).clamp(10f, 22f).dp
        val vPad = (7f * scale).clamp(6f, 14f).dp

        Text(
            text = text,
            color = Color.White,
            fontSize = font,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(top = topPad)
                .background(Color(0xFF1976D2).copy(alpha = 0.85f), RoundedCornerShape(corner))
                .padding(horizontal = hPad, vertical = vPad)
        )
    }
}

@Composable
fun PlankTimerOverlay(
    timeText: String,
    caption: String,
    sizeMultiplier: Float = 1f
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopStart
    ) {
        val cfg = LocalConfiguration.current
        val w = maxWidth.value
        val h = maxHeight.value
        val base = min(w, h)

        // Mix width + min dimension for better behavior on tablets/landscape
        val scale = ((0.75f * (w / 360f)) + (0.25f * (base / 360f))).clamp(0.85f, 1.45f)

        val fontScale = cfg.fontScale.clamp(0.85f, 1.25f)
        val finalTextScale = (scale / fontScale).clamp(0.85f, 1.45f)

        val topPad = (8f * scale).dp
        val corner = (16f * scale).clamp(12f, 24f).dp
        val hPad = (16f * scale).clamp(10f, 26f).dp
        val vPad = (11f * scale).clamp(8f, 20f).dp

        val captionSp = (11f * finalTextScale * sizeMultiplier)
            .clamp(9f, 14f)
            .sp

        val timeSp = (14f * finalTextScale * sizeMultiplier)
            .clamp(12f, 24f)
            .sp

        Column(
            modifier = Modifier
                .padding(top = topPad)
                .background(Color(0xFF1976D2).copy(alpha = 0.85f), RoundedCornerShape(corner))
                .padding(horizontal = hPad, vertical = vPad),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = caption,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = captionSp
            )
            Text(
                text = timeText,
                color = Color.White,
                fontSize = timeSp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CountdownOverlay(countdownValue: Int) {
    val scale by animateFloatAsState(
        targetValue = if (countdownValue > 0) 1.2f else 0.8f,
        animationSpec = tween(400)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Exercise about to start",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Please position yourself\nin the middle of the camera",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
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
}

@Composable
fun PermissionDeniedScreen(onRequestPermission: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Camera permission is required", textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRequestPermission) { Text("Grant permission") }
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
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }

    // Use a stable provider future (don’t block with .get() on composition thread)
    val providerFuture = remember { ProcessCameraProvider.getInstance(context) }

    DisposableEffect(Unit) {
        val listener = Runnable {
            // This Runnable runs on MAIN thread because we pass mainExecutor
            val provider = providerFuture.get()
            onCameraReady(provider, previewView)
        }

        providerFuture.addListener(listener, mainExecutor)

        onDispose {
            // Ensure unbindAll happens on MAIN thread
            mainExecutor.execute {
                runCatching { providerFuture.get().unbindAll() }
            }
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

private fun Float.clamp(minV: Float, maxV: Float): Float = when {
    this < minV -> minV
    this > maxV -> maxV
    else -> this
}

private fun formatMmSs(sec: Int): String =
    String.format("%02d:%02d", sec / 60, sec % 60)
