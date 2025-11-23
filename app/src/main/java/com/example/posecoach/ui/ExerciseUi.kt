package com.example.posecoach.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ExerciseUi(
    val id: String,
    val title: String,
    val description: String,
    val instructions: List<String>
)

val exercises = listOf(
    ExerciseUi(
        id = "squat",
        title = "Squat",
        description = "Lower body exercise focusing on quads, glutes and core.",
        instructions = listOf(
            "Stand with feet shoulder-width apart.",
            "Push your hips back as if sitting into a chair.",
            "Keep your chest up and back straight.",
            "Bend your knees until thighs are parallel to the ground.",
            "Push through heels to stand back up."
        )
    ),
    ExerciseUi(
        id = "pushup",
        title = "Push-up",
        description = "Upper body exercise working chest, shoulders, arms and core.",
        instructions = listOf(
            "Place hands slightly wider than shoulder-width.",
            "Keep your body in a straight line from head to heels.",
            "Lower yourself until your chest nearly touches the floor.",
            "Keep elbows tucked at about 45 degrees.",
            "Push back up while keeping core engaged."
        )
    ),
    ExerciseUi(
        id = "plank",
        title = "Plank",
        description = "Core stability exercise working abs, glutes and back.",
        instructions = listOf(
            "Place elbows under shoulders and extend legs back.",
            "Keep body in a straight line (no arching).",
            "Engage your core and squeeze glutes.",
            "Look down to keep neck neutral.",
            "Hold as long as you can with proper form."
        )
    )
)

@Composable
fun ExerciseInfoOverlay(
    exercise: ExerciseUi,
    onCancel: () -> Unit,
    onConfirmStart: () -> Unit
) {
    var understood = remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            backgroundColor = Color(0xFF0D47A1),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = exercise.title,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = exercise.description,
                    color = Color.White,
                    style = MaterialTheme.typography.body1
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "How to perform:",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )

                exercise.instructions.forEach { step ->
                    Text(
                        text = "• $step",
                        color = Color.White,
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = understood.value,
                        onCheckedChange = { understood.value = it }
                    )
                    Text(
                        text = "I understand the instructions",
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onCancel) {
                        Text(text = "Cancel", color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConfirmStart,
                        enabled = understood.value,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF0B3C91),
                            disabledBackgroundColor = Color(0xFF5476A8)
                        )
                    ) {
                        Text(text = "Start exercise", color = Color.White)
                    }
                }
            }
        }
    }
}
