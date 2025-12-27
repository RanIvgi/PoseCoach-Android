package com.example.posecoach.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.posecoach.data.FeedbackMessage
import com.example.posecoach.data.FeedbackSeverity
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image

@Composable
fun CameraControls(
    feedback: FeedbackMessage?,
    fps: Float,
    useGpu: Boolean,
    repCount: Int,
    repsRemaining: Int,
    sessionState: SessionState,
    currentExercise: String,
    targetReps: Int,
    onCameraSwitch: () -> Unit,
    onToggleDelegate: () -> Unit,
    onResetRepCount: () -> Unit,
    onStartSession: () -> Unit,
    onFinishSession: () -> Unit,
    onBackToHome: () -> Unit,
    showReps: Boolean,
    modifier: Modifier = Modifier
) {
    LogCompositions("CameraControls")
    
    Box(modifier = modifier) {

        if (sessionState == SessionState.ACTIVE) {
            if (showReps) {
                // Show countdown if target reps are set, otherwise show count-up
                val displayText = if (targetReps > 0) {
                    "$repsRemaining left"
                } else {
                    "Reps: $repCount"
                }
                
                Text(
                    text = displayText,
                    color = Color.White,
                    style = MaterialTheme.typography.h5,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(8.dp)
                )
            }
        }

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

        // Play button in center when IDLE
        if (sessionState == SessionState.IDLE) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Place camera on a stable surface\nPress start when you are ready to get in position",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .background(Color(0xFF1976D2).copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                FloatingActionButton(
                    onClick = onStartSession,
                    backgroundColor = MaterialTheme.colors.primary,
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Start Session",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (sessionState == SessionState.ACTIVE) {
                FloatingActionButton(
                    onClick = onFinishSession,
                    backgroundColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = "Finish Session",
                        tint = Color(0xFFEF5350)
                    )
                }
            }

            if (sessionState == SessionState.ACTIVE) {
                FloatingActionButton(
                    onClick = onResetRepCount,
                    backgroundColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Reset Rep Count",
                        tint = MaterialTheme.colors.primary
                    )
                }
            }

            FloatingActionButton(
                onClick = onToggleDelegate,
                backgroundColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Filled.Memory,
                    contentDescription = if (useGpu) "Switch to CPU" else "Switch to GPU",
                    tint = MaterialTheme.colors.primary
                )
            }

            FloatingActionButton(
                onClick = onCameraSwitch,
                backgroundColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Filled.Cameraswitch,
                    contentDescription = "Switch Camera",
                    tint = MaterialTheme.colors.primary
                )
            }
        }
    }
}

@Composable
fun FeedbackDisplay(
    feedback: FeedbackMessage,
    modifier: Modifier = Modifier
) {
    LogCompositions("FeedbackDisplay")
    
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
