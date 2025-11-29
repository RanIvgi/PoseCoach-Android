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
    sessionState: SessionState,
    currentExercise: String,
    targetReps: Int,
    onCameraSwitch: () -> Unit,
    onToggleDelegate: () -> Unit,
    onResetRepCount: () -> Unit,
    onStartSession: () -> Unit,
    onFinishSession: () -> Unit,
    onExerciseSelected: (String) -> Unit,
    onTargetRepsChange: (Int) -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    LogCompositions("CameraControls")
    
    Box(modifier = modifier) {

        if (sessionState == SessionState.IDLE) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 90.dp, end = 16.dp)
            ) {
                ExerciseSelector(
                    currentExercise = currentExercise,
                    onExerciseSelected = onExerciseSelected,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                TargetRepsSelector(
                    targetReps = targetReps,
                    onTargetRepsChange = onTargetRepsChange,
                    modifier = Modifier.fillMaxWidth()
                )
            }
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

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Home is always shown (IDLE and ACTIVE)
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

                // End is shown only when a session is ACTIVE
                if (sessionState == SessionState.ACTIVE) {
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
            }

            Spacer(modifier = Modifier.height(8.dp))

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
fun ExerciseSelector(
    currentExercise: String,
    onExerciseSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LogCompositions("ExerciseSelector")
    
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
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // Exercise Logo
                    Image(
                        painter = painterResource(exercise.logoRes),
                        contentDescription = exercise.title,
                        modifier = Modifier
                            .size(48.dp)
                            .padding(end = 12.dp)
                    )

                    Column {
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
}

@Composable
fun TargetRepsSelector(
    targetReps: Int,
    onTargetRepsChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LogCompositions("TargetRepsSelector")
    
    val options = (1..30).toList()

    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(8.dp)
    ) {
        Text(
            text = "Target reps",
            color = Color.White,
            style = MaterialTheme.typography.subtitle2,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            options.forEach { value ->
                val selected = value == targetReps

                Surface(
                    color = if (selected) Color(0xFF4CAF50) else Color.Transparent,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, Color.White),
                    modifier = Modifier
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .clickable { onTargetRepsChange(value) }
                ) {
                    Text(
                        text = value.toString(),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
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
