package com.example.posecoach.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.posecoach.data.FeedbackMessage
import com.example.posecoach.data.FeedbackSeverity

@Composable
fun VideoResultsScreen(
    videoUri: String,
    exerciseId: String,
    navBackToStart: () -> Unit
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0D47A1),
            Color(0xFF1976D2),
            Color(0xFF42A5F5)
        )
    )

    // Mock results - in real implementation, this would come from the analysis
    val mockResults = listOf(
        FeedbackMessage("Good back alignment throughout the exercise", FeedbackSeverity.INFO),
        FeedbackMessage("Knees tracking well over toes", FeedbackSeverity.INFO),
        FeedbackMessage("Depth could be improved - aim for thighs parallel to ground", FeedbackSeverity.WARNING),
        FeedbackMessage("Maintain chest up position", FeedbackSeverity.INFO)
    )
    
    val exerciseName = when (exerciseId) {
        "pushup" -> "Push-ups"
        "squat" -> "Squats"
        "plank" -> "Plank"
        else -> "Exercise"
    }
    
    val overallScore = 75 // Mock score

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Top bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                        text = "Analysis Results",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Exercise title
                Text(
                    text = exerciseName,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Overall score card
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
                            text = "$overallScore%",
                            color = Color.White,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = when {
                                overallScore >= 80 -> "Excellent Form!"
                                overallScore >= 60 -> "Good Form - Minor improvements needed"
                                else -> "Form needs improvement"
                            },
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Feedback section
                Text(
                    text = "Detailed Feedback:",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Feedback list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(mockResults) { feedback ->
                        FeedbackCard(feedback)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                        Text(
                            text = "Done",
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
