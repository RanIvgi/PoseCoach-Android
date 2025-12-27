package com.example.posecoach.ui

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VideoUploadScreen(
    viewModel: VideoAnalysisViewModel,
    navBackToStart: () -> Unit,
    navToResults: (String, String) -> Unit
) {
    val selectedVideoUri by viewModel.selectedVideoUri.collectAsState()
    val selectedExercise by viewModel.selectedExercise.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val processingProgress by viewModel.processingProgress.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Determine which permission to request based on API level
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val storagePermission = rememberPermissionState(permission)

    // Request permission on screen load
    LaunchedEffect(Unit) {
        if (!storagePermission.status.isGranted) {
            storagePermission.launchPermissionRequest()
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setVideoUri(it) }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colors.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
        ) {
            if (storagePermission.status.isGranted) {
                // Main content - permission granted
                VideoUploadContent(
                    selectedVideoUri = selectedVideoUri,
                    selectedExercise = selectedExercise,
                    isProcessing = isProcessing,
                    processingProgress = processingProgress,
                    errorMessage = errorMessage,
                    navBackToStart = navBackToStart,
                    onVideoSelect = { videoPickerLauncher.launch("video/*") },
                    onExerciseSelect = { viewModel.setExercise(it) },
                    onAnalyze = { uri, exerciseId ->
                        viewModel.startAnalysis(uri, exerciseId) {
                            navToResults(uri, exerciseId)
                        }
                    }
                )
            } else {
                // Permission denied screen
                PermissionDeniedScreen(
                    permissionName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        "video access"
                    } else {
                        "storage access"
                    },
                    onRequestPermission = { storagePermission.launchPermissionRequest() },
                    onBack = navBackToStart
                )
            }
        }
    }
}

@Composable
private fun VideoUploadContent(
    selectedVideoUri: Uri?,
    selectedExercise: String?,
    isProcessing: Boolean,
    processingProgress: Float,
    errorMessage: String?,
    navBackToStart: () -> Unit,
    onVideoSelect: () -> Unit,
    onExerciseSelect: (String) -> Unit,
    onAnalyze: (String, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Top bar with back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = navBackToStart) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colors.primary
                )
            }
            Text(
                text = "Analyze Video",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Video selection card
        Card(
            backgroundColor = MaterialTheme.colors.surface,
            shape = RoundedCornerShape(16.dp),
            elevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isProcessing) {
                    onVideoSelect()
            }
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.VideoLibrary,
                    contentDescription = "Select Video",
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (selectedVideoUri != null) "Video Selected" else "Tap to Select Video",
                    color = MaterialTheme.colors.primary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (selectedVideoUri != null) {
                    Text(
                        text = "Tap to change video",
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Exercise selection
        Text(
            text = "Select Exercise:",
            color = MaterialTheme.colors.primary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        exercises.forEach { exercise ->
            ExerciseSelectionCard(
                exercise = exercise,
                isSelected = selectedExercise == exercise.id,
                onSelect = { onExerciseSelect(exercise.id) },
                enabled = !isProcessing
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        // Error message
        errorMessage?.let { error ->
            Card(
                backgroundColor = Color(0xFFB71C1C),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = error,
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Processing progress
        if (isProcessing) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = processingProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = MaterialTheme.colors.primary,
                    backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Processing video... ${(processingProgress * 100).toInt()}%",
                    color = MaterialTheme.colors.onSurface,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Analyze button
        Button(
            onClick = {
                selectedVideoUri?.let { uri ->
                    selectedExercise?.let { exerciseId ->
                        onAnalyze(uri.toString(), exerciseId)
                    }
                }
            },
            enabled = selectedVideoUri != null && selectedExercise != null && !isProcessing,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.primary,
                disabledBackgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.6f),
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 8.dp
            )
        ) {
            Text(
                text = if (isProcessing) "Processing..." else "Analyze Video",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PermissionDeniedScreen(
    permissionName: String,
    onRequestPermission: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Permission Required",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.primary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "PoseCoach needs $permissionName permission to analyze videos from your device.",
            fontSize = 16.sp,
            color = MaterialTheme.colors.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onRequestPermission,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 8.dp
            )
        ) {
            Text(
                text = "Grant Permission",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(onClick = onBack) {
            Text(
                text = "Go Back",
                color = MaterialTheme.colors.primary,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun ExerciseSelectionCard(
    exercise: ExerciseUi,
    isSelected: Boolean,
    onSelect: () -> Unit,
    enabled: Boolean
) {
    Card(
        backgroundColor = MaterialTheme.colors.surface,
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onSelect() }
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colors.primary,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Modifier
                }
            )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(exercise.logoRes),
                contentDescription = exercise.title,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.title,
                    color = MaterialTheme.colors.primary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = exercise.description,
                    color = MaterialTheme.colors.onSurface,
                    fontSize = 13.sp,
                    maxLines = 2
                )
            }
            if (isSelected) {
                Icon(
                    painter = painterResource(android.R.drawable.checkbox_on_background),
                    contentDescription = "Selected",
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
