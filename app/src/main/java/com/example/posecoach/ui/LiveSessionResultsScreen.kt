package com.example.posecoach.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Current Exercise", "Common Feedback", "All Exercises")

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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = navBackToStart) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "Session Complete!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Exercise title
                    item {
                        Text(
                            text = sessionResult.exerciseName,
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

                    // Session stats card
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
                                    text = "Session Stats",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                StatRow("Completed Reps", "${sessionResult.completedReps}")
                                StatRow("Target Reps", "${sessionResult.targetReps}")
                                StatRow("Duration", formatDuration(sessionResult.durationMillis))
                            }
                        }
                    }

                    // Workout totals card
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
                                        text = "Workout Totals",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    StatRow("Exercises", "${sessionResult.totalExercises}")
                                    StatRow("Total Reps", "${sessionResult.totalReps}")
                                    StatRow("Total Time", formatDuration(sessionResult.totalDurationMillis))
                                }
                            }
                        }
                    }

                    // Feedback section
                    if (sessionResult.feedbackMessages.isNotEmpty() || sessionResult.commonFeedbackMessages.isNotEmpty()) {
                        item {
                            Text(
                                text = "Form Feedback:",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (sessionResult.totalExercises > 1) {
                            item {
                                TabRow(
                                    selectedTabIndex = selectedTabIndex,
                                    backgroundColor = Color.Transparent,
                                    contentColor = Color.White,
                                    indicator = { tabPositions ->
                                        TabRowDefaults.Indicator(
                                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                            color = Color.White
                                        )
                                    }
                                ) {
                                    tabs.forEachIndexed { index, title ->
                                        Tab(
                                            selected = selectedTabIndex == index,
                                            onClick = { selectedTabIndex = index },
                                            text = { Text(title) }
                                        )
                                    }
                                }
                            }
                        }

                        // Feedback list
                        val feedbackToShow = when (selectedTabIndex) {
                            0 -> sessionResult.feedbackMessages
                            1 -> sessionResult.commonFeedbackMessages
                            else -> sessionResult.allFeedbackMessages
                        }

                        // Show unique feedback messages (deduplicate)
                        val uniqueFeedback = feedbackToShow
                            .distinctBy { it.text }
                            .take(20) // Limit to 20 most relevant messages
                        
                        items(uniqueFeedback) { feedback ->
                            FeedbackCard(feedback)
                        }

                        if (uniqueFeedback.isEmpty()) {
                             item {
                                Text(
                                    text = when (selectedTabIndex) {
                                        0 -> "No feedback for this session."
                                        1 -> "No common feedback across all sessions."
                                        else -> "No feedback recorded across any session."
                                    },
                                    color = Color.White.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                                )
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
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
