package com.example.posecoach.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.material.TabRow
import androidx.compose.material.Tab
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.posecoach.data.FeedbackMessage
import com.example.posecoach.data.FeedbackSeverity
import com.example.posecoach.data.LiveSessionResult

@Composable
fun LiveSessionResultsScreen(
    sessionResult: LiveSessionResult,
    navBackToStart: () -> Unit,
    onStartNewExercise: () -> Unit
) {
    // 1. Extract unique exercise types from history
    // If history is empty (legacy/fallback), just use the single result we have
    val exerciseTypes = remember(sessionResult.sessionHistory) {
        if (sessionResult.sessionHistory.isNotEmpty()) {
            sessionResult.sessionHistory.map { it.exerciseName }.distinct()
        } else {
            listOf(sessionResult.exerciseName)
        }
    }

    // 2. State for selected exercise type (default to the one just finished)
    var selectedExerciseType by remember { mutableStateOf(sessionResult.exerciseName) }

    // 3. Filter data based on selected type
    val historyForType = remember(sessionResult.sessionHistory, selectedExerciseType) {
        if (sessionResult.sessionHistory.isNotEmpty()) {
            sessionResult.sessionHistory.filter { it.exerciseName == selectedExerciseType }
        } else {
            // Fallback if history is empty
            emptyList()
        }
    }

    // 4. Calculate derived data for the selected type
    val currentSessionForType = historyForType.lastOrNull()
    
    // "Current" feedback: The last session of this type
    val currentFeedback = if (sessionResult.sessionHistory.isNotEmpty()) {
        currentSessionForType?.feedbackMessages ?: emptyList()
    } else {
        sessionResult.feedbackMessages
    }

    // "Common" feedback: Intersection for this type
    val commonFeedback = remember(historyForType) {
        if (historyForType.isNotEmpty()) {
            val firstTexts = historyForType.first().feedbackMessages.map { it.text }.toSet()
            var intersection = firstTexts
            for (session in historyForType.drop(1)) {
                intersection = intersection.intersect(session.feedbackMessages.map { it.text }.toSet())
            }
            intersection.mapNotNull { text ->
                historyForType.first().feedbackMessages.find { it.text == text }
            }
        } else {
            sessionResult.commonFeedbackMessages // Fallback
        }
    }

    // "All" feedback: Union for this type
    val allFeedback = remember(historyForType) {
        if (historyForType.isNotEmpty()) {
            historyForType.flatMap { it.feedbackMessages }
        } else {
            sessionResult.allFeedbackMessages // Fallback
        }
    }

    // Stats for the selected type
    val typeReps = if (historyForType.isNotEmpty()) historyForType.sumOf { it.reps } else sessionResult.completedReps
    val typeDuration = if (historyForType.isNotEmpty()) historyForType.sumOf { it.durationMillis } else sessionResult.durationMillis
    val typeExercisesCount = if (historyForType.isNotEmpty()) historyForType.size else 1
    
    // Plank specific stats
    val isPlank = selectedExerciseType.lowercase().contains("plank")
    val currentFormBreaks = currentSessionForType?.formBreakCount ?: sessionResult.formBreakCount
    
    // Tabs for feedback view
    var selectedFeedbackTabIndex by remember { mutableStateOf(0) }
    val feedbackTabs = listOf("Current", "Common", "All")

    val gradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0D47A1),
            Color(0xFF1976D2),
            Color(0xFF42A5F5)
        )
    )

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top bar (Fixed)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Session Complete !",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = navBackToStart) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                }

                // Floating Exercise Type Selector (Fixed below top bar)
                if (exerciseTypes.size > 1) {
                    ScrollableTabRow(
                        selectedTabIndex = exerciseTypes.indexOf(selectedExerciseType),
                        backgroundColor = Color.Transparent,
                        contentColor = Color.White,
                        edgePadding = 0.dp,
                        indicator = { tabPositions ->
                            TabRowDefaults.Indicator(
                                Modifier.tabIndicatorOffset(tabPositions[exerciseTypes.indexOf(selectedExerciseType)]),
                                color = Color.White,
                                height = 3.dp
                            )
                        }
                    ) {
                        exerciseTypes.forEach { type ->
                            Tab(
                                selected = selectedExerciseType == type,
                                onClick = { selectedExerciseType = type },
                                text = { 
                                    Text(
                                        text = type,
                                        fontWeight = if (selectedExerciseType == type) FontWeight.Bold else FontWeight.Normal
                                    ) 
                                }
                            )
                        }
                    }
                }

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    
                    // Exercise title (Selected Type)
                    item {
                        Text(
                            text = selectedExerciseType,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Overall score card
                    item {
                        Card(
                            backgroundColor = Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(16.dp),
                            elevation = 0.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Overall Form Score",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${sessionResult.overallScore}%",
                                    color = Color.White,
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = when {
                                        sessionResult.overallScore >= 80 -> "Excellent Form!"
                                        sessionResult.overallScore >= 60 -> "Good Form - Minor improvements needed"
                                        else -> "Form needs improvement"
                                    },
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Session Stats Card (Filtered by Type)
                    item {
                        Card(
                            backgroundColor = Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(16.dp),
                            elevation = 0.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "$selectedExerciseType Stats",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                if (isPlank) {
                                    // Plank specific stats
                                    val currentDuration = currentSessionForType?.durationMillis ?: sessionResult.durationMillis
                                    val goodFormDuration = currentSessionForType?.goodFormDurationMillis ?: sessionResult.goodFormDurationMillis
                                    
                                    // Show Target Time if it was set
                                    if (sessionResult.targetDurationMillis != null && sessionResult.targetDurationMillis > 0) {
                                        StatRow("Target Time", formatDuration(sessionResult.targetDurationMillis))
                                    }
                                    
                                    StatRow("Total Duration", formatDuration(currentDuration))
                                    StatRow("Time Held Correctly", formatDuration(goodFormDuration))
                                    StatRow("Form Breaks", "$currentFormBreaks")
                                } else {
                                    // Rep-based stats
                                    // For "Session Stats" we usually show the LAST session's stats, not the total for the type.
                                    // But the user asked for "Total Squat Workout Summary" in the NEXT card.
                                    // This card is "Session Stats". Let's show the stats for the *current* (last) session of this type.
                                    val currentReps = currentSessionForType?.reps ?: sessionResult.completedReps
                                    val currentDuration = currentSessionForType?.durationMillis ?: sessionResult.durationMillis
                                    
                                    StatRow("Completed Reps", "$currentReps")
                                    // Target reps might vary, but usually constant per session type. Using global target for now.
                                    StatRow("Target Reps", "${sessionResult.targetReps}")
                                    StatRow("Duration", formatDuration(currentDuration))
                                }
                            }
                        }
                    }

                    // Total Workout Summary (Specific to Exercise Type)
                    if (sessionResult.totalExercises > 1) {
                        item {
                            Card(
                                backgroundColor = Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(16.dp),
                                elevation = 0.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "Total $selectedExerciseType Workout Summary",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    if (isPlank) {
                                        StatRow("Total Exercises", "$typeExercisesCount")
                                        StatRow("Total Time", formatDuration(typeDuration))
                                    } else {
                                        StatRow("Total Exercises", "$typeExercisesCount")
                                        StatRow("Total Reps", "$typeReps")
                                        StatRow("Total Time", formatDuration(typeDuration))
                                    }
                                }
                            }
                        }
                    }

                    // Feedback section
                    item {
                        Text(
                            text = "Form Feedback ($selectedExerciseType):",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    item {
                        TabRow(
                            selectedTabIndex = selectedFeedbackTabIndex,
                            backgroundColor = Color.Transparent,
                            contentColor = Color.White,
                            indicator = { tabPositions ->
                                TabRowDefaults.Indicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedFeedbackTabIndex]),
                                    color = Color.White
                                )
                            }
                        ) {
                            feedbackTabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedFeedbackTabIndex == index,
                                    onClick = { selectedFeedbackTabIndex = index },
                                    text = { Text(title) }
                                )
                            }
                        }
                    }

                    // Feedback list
                    val feedbackToShow = when (selectedFeedbackTabIndex) {
                        0 -> currentFeedback
                        1 -> commonFeedback
                        else -> allFeedback
                    }

                    // Show unique feedback messages (deduplicate) and group by severity
                    val uniqueFeedback = feedbackToShow
                        .distinctBy { it.text }
                    
                    // Group by severity type
                    val errorMessages = uniqueFeedback.filter { it.severity == FeedbackSeverity.ERROR }
                    val warningMessages = uniqueFeedback.filter { it.severity == FeedbackSeverity.WARNING }
                    val infoMessages = uniqueFeedback.filter { it.severity == FeedbackSeverity.INFO }

                    if (uniqueFeedback.isEmpty()) {
                            item {
                            Text(
                                text = when (selectedFeedbackTabIndex) {
                                    0 -> "No feedback for the last $selectedExerciseType session."
                                    1 -> "No common feedback across $selectedExerciseType sessions."
                                    else -> "No feedback recorded for $selectedExerciseType."
                                },
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(16.dp)
                            )
                            }
                    } else {
                        // Show INFO section (Good Form first)
                        if (infoMessages.isNotEmpty()) {
                            item {
                                FeedbackSectionHeader(
                                    title = "Good Form",
                                    count = infoMessages.size,
                                    icon = Icons.Default.CheckCircle,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                            items(infoMessages) { feedback ->
                                FeedbackCard(feedback)
                            }
                        }

                        // Show WARNING section
                        if (warningMessages.isNotEmpty()) {
                            item {
                                FeedbackSectionHeader(
                                    title = "Warnings",
                                    count = warningMessages.size,
                                    icon = Icons.Default.Warning,
                                    color = Color(0xFFFF9800)
                                )
                            }
                            items(warningMessages) { feedback ->
                                FeedbackCard(feedback)
                            }
                        }

                        // Show ERROR section (Errors last)
                        if (errorMessages.isNotEmpty()) {
                            item {
                                FeedbackSectionHeader(
                                    title = "Errors",
                                    count = errorMessages.size,
                                    icon = Icons.Default.Error,
                                    color = Color(0xFFF44336)
                                )
                            }
                            items(errorMessages) { feedback ->
                                FeedbackCard(feedback)
                            }
                        }
                    }
                }

                // Action buttons (Fixed at bottom)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onStartNewExercise,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "New Exercise",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Button(
                        onClick = navBackToStart,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF0B3C91)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Home",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedbackSectionHeader(
    title: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        backgroundColor = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(12.dp),
        elevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Card(
                backgroundColor = color.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp),
                elevation = 0.dp
            ) {
                Text(
                    text = "$count",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun FeedbackCard(feedback: FeedbackMessage) {
    val backgroundColor = when (feedback.severity) {
        FeedbackSeverity.INFO -> Color(0xFF2E7D32).copy(alpha = 0.3f)
        FeedbackSeverity.WARNING -> Color(0xFFF57C00).copy(alpha = 0.3f)
        FeedbackSeverity.ERROR -> Color(0xFFB71C1C).copy(alpha = 0.3f)
    }

    val iconColor = when (feedback.severity) {
        FeedbackSeverity.INFO -> Color(0xFF4CAF50)
        FeedbackSeverity.WARNING -> Color(0xFFFF9800)
        FeedbackSeverity.ERROR -> Color(0xFFF44336)
    }

    val icon = when (feedback.severity) {
        FeedbackSeverity.INFO -> Icons.Default.CheckCircle
        FeedbackSeverity.WARNING -> Icons.Default.Warning
        FeedbackSeverity.ERROR -> Icons.Default.Error
    }

    Card(
        backgroundColor = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        elevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = feedback.text,
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            
            // Show point deduction for warnings and errors
            if (feedback.severity == FeedbackSeverity.WARNING || feedback.severity == FeedbackSeverity.ERROR) {
                val points = if (feedback.severity == FeedbackSeverity.WARNING) "-5" else "-10"
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$points pts",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(
                            color = Color.Black.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
