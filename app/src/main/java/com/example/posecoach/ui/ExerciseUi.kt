package com.example.posecoach.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.posecoach.R

data class ExerciseUi(
    val id: String,
    val title: String,
    val description: String,
    val instructions: List<String>,
    val logoRes: Int
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
        ),
        logoRes = R.drawable.squat_logo
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
        ),
        logoRes = R.drawable.pushup_logo
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
        ),
        logoRes = R.drawable.plank_logo
    )
)

@Composable
fun ExerciseInfoOverlay(
    exercise: ExerciseUi,
    onCancel: () -> Unit,
    onConfirmStart: (durationSeconds: Int?) -> Unit
) {
    // You have this in your project; keep it if it exists
    LogCompositions("ExerciseInfoOverlay")

    val understood = remember { mutableStateOf(false) }

    val isPlank = exercise.id == "plank"
    val plankTimed = remember { mutableStateOf(true) }          // timed vs free
    val plankDurationSeconds = remember { mutableStateOf(60) }  // default 01:00
    val showDurationPicker = remember { mutableStateOf(false) } // open/close list

    fun formatMmSs(totalSeconds: Int): String {
        val m = totalSeconds / 60
        val s = totalSeconds % 60
        return "%02d:%02d".format(m, s)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Background scrim: only this closes the overlay
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onCancel() }
        )

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
                Image(
                    painter = painterResource(exercise.logoRes),
                    contentDescription = exercise.title,
                    modifier = Modifier
                        .size(90.dp)
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 8.dp)
                )

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

                // ---- Plank options ----
                if (isPlank) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Plank settings:",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { plankTimed.value = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = if (plankTimed.value) Color(0xFF0B3C91) else Color(0xFF5476A8)
                            )
                        ) { Text("Timed", color = Color.White) }

                        Button(
                            onClick = { plankTimed.value = false; showDurationPicker.value = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = if (!plankTimed.value) Color(0xFF0B3C91) else Color(0xFF5476A8)
                            )
                        ) { Text("Free", color = Color.White) }
                    }

                    if (plankTimed.value) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "Duration", color = Color.White)
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedButton(
                            onClick = { showDurationPicker.value = !showDurationPicker.value },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                backgroundColor = Color.White,
                                contentColor = Color.Black
                            )
                        ) {
                            Text(
                                text = "Selected: ${formatMmSs(plankDurationSeconds.value)}",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (showDurationPicker.value) {
                            Spacer(modifier = Modifier.height(6.dp))
                            DurationScrollTapPicker(
                                valueSeconds = plankDurationSeconds.value,
                                onValueChange = {
                                    plankDurationSeconds.value = it
                                    showDurationPicker.value = false // close after selection
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .zIndex(10f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // ---- confirmation checkbox ----
                Row(verticalAlignment = Alignment.CenterVertically) {
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

                // ---- actions ----
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onCancel) {
                        Text(text = "Cancel", color = Color.White)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        enabled = understood.value,
                        onClick = {
                            val durationOrNull =
                                if (exercise.id == "plank" && !plankTimed.value) null
                                else if (exercise.id == "plank") plankDurationSeconds.value
                                else null

                            onConfirmStart(durationOrNull)
                        },
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

@Composable
fun DurationScrollTapPicker(
    valueSeconds: Int,
    onValueChange: (Int) -> Unit,
    minSeconds: Int = 0,
    maxSeconds: Int = 600,
    stepSeconds: Int = 5,
    modifier: Modifier = Modifier,
) {
    val options = remember(minSeconds, maxSeconds, stepSeconds) {
        (minSeconds..maxSeconds step stepSeconds).toList()
    }

    fun mmss(totalSeconds: Int): String {
        val m = totalSeconds / 60
        val s = totalSeconds % 60
        return "%02d:%02d".format(m, s)
    }

    val state = rememberLazyListState()

    LaunchedEffect(options, valueSeconds) {
        val idx = options.indexOf(valueSeconds).takeIf { it >= 0 } ?: 0
        state.scrollToItem(idx.coerceAtLeast(0))
    }

    LazyColumn(
        state = state,
        modifier = modifier
            .background(Color.White, shape = RoundedCornerShape(12.dp))
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        itemsIndexed(options) { _, sec ->
            val selected = sec == valueSeconds

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onValueChange(sec) }
                    .padding(vertical = 10.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = mmss(sec),
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) Color(0xFF0B3C91) else Color.Black,
                    modifier = Modifier.weight(1f)
                )

                if (selected) {
                    Text(
                        text = "✓",
                        color = Color(0xFF0B3C91),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}