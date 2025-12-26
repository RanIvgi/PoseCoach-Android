package com.example.posecoach.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.zIndex

@Composable
fun ExerciseSelectionScreen(
    onBackToStart: () -> Unit,
    onStartSession: (exerciseId: String, durationSeconds: Int?) -> Unit
) {
    var selectedExercise by remember { mutableStateOf<ExerciseUi?>(null) }
    var understood by remember { mutableStateOf(false) }
    
    // Plank-specific state
    var plankTimed by remember { mutableStateOf(true) }
    var plankDurationSeconds by remember { mutableStateOf(60) }
    var showDurationPicker by remember { mutableStateOf(false) }

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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header with back button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackToStart) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Select Exercise",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedExercise == null) {
                    // Show exercise selection cards
                    Text(
                        text = "Choose your exercise:",
                        fontSize = 18.sp,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    exercises.forEach { exercise ->
                        ExerciseCard(
                            exercise = exercise,
                            onClick = {
                                selectedExercise = exercise
                                understood = false
                                // Reset plank settings when selecting
                                if (exercise.id == "plank") {
                                    plankTimed = true
                                    plankDurationSeconds = 60
                                    showDurationPicker = false
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                } else {
                    // Show selected exercise details
                    ExerciseDetailsView(
                        exercise = selectedExercise!!,
                        understood = understood,
                        onUnderstoodChange = { understood = it },
                        plankTimed = plankTimed,
                        onPlankTimedChange = { 
                            plankTimed = it
                            if (!it) showDurationPicker = false
                        },
                        plankDurationSeconds = plankDurationSeconds,
                        onPlankDurationChange = { plankDurationSeconds = it },
                        showDurationPicker = showDurationPicker,
                        onShowDurationPickerChange = { showDurationPicker = it },
                        onBack = { 
                            selectedExercise = null
                            understood = false
                        },
                        onStartSession = {
                            val duration = when {
                                selectedExercise!!.id == "plank" && plankTimed -> plankDurationSeconds
                                selectedExercise!!.id == "plank" && !plankTimed -> null
                                else -> null
                            }
                            onStartSession(selectedExercise!!.id, duration)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ExerciseCard(
    exercise: ExerciseUi,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        backgroundColor = Color.White.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(exercise.logoRes),
                contentDescription = exercise.title,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = exercise.description,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
fun ExerciseDetailsView(
    exercise: ExerciseUi,
    understood: Boolean,
    onUnderstoodChange: (Boolean) -> Unit,
    plankTimed: Boolean,
    onPlankTimedChange: (Boolean) -> Unit,
    plankDurationSeconds: Int,
    onPlankDurationChange: (Int) -> Unit,
    showDurationPicker: Boolean,
    onShowDurationPickerChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onStartSession: () -> Unit
) {
    var attemptedStart by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            backgroundColor = Color.White.copy(alpha = 0.15f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Exercise logo and title
                Image(
                    painter = painterResource(exercise.logoRes),
                    contentDescription = exercise.title,
                    modifier = Modifier
                        .size(90.dp)
                        .align(Alignment.CenterHorizontally)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = exercise.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = exercise.description,
                    fontSize = 16.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "How to perform:",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                exercise.instructions.forEach { instruction ->
                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "•",
                            fontSize = 16.sp,
                            color = Color.White,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = instruction,
                            fontSize = 15.sp,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // Plank-specific settings
                if (exercise.id == "plank") {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Plank Mode:",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onPlankTimedChange(true) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = if (plankTimed) Color(0xFF0B3C91) else Color(0xFF5476A8)
                            )
                        ) {
                            Text("Timed", color = Color.White)
                        }
                        
                        Button(
                            onClick = { onPlankTimedChange(false) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = if (!plankTimed) Color(0xFF0B3C91) else Color(0xFF5476A8)
                            )
                        ) {
                            Text("Free", color = Color.White)
                        }
                    }
                    
                    if (plankTimed) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "Duration:",
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        OutlinedButton(
                            onClick = { onShowDurationPickerChange(!showDurationPicker) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                backgroundColor = Color.White,
                                contentColor = Color.Black
                            )
                        ) {
                            Text(
                                text = "Selected: ${formatMmSs(plankDurationSeconds)}",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        if (showDurationPicker) {
                            Spacer(modifier = Modifier.height(8.dp))
                            DurationScrollTapPicker(
                                valueSeconds = plankDurationSeconds,
                                onValueChange = {
                                    onPlankDurationChange(it)
                                    onShowDurationPickerChange(false)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Confirmation checkbox with red highlight when needed
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (attemptedStart && !understood) 
                                Color.Red.copy(alpha = 0.3f) 
                            else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                        .clickable { 
                            onUnderstoodChange(!understood)
                            if (understood) attemptedStart = false
                        }
                ) {
                    Checkbox(
                        checked = understood,
                        onCheckedChange = { 
                            onUnderstoodChange(it)
                            if (it) attemptedStart = false
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF0B3C91),
                            uncheckedColor = if (attemptedStart && !understood) 
                                Color.Red 
                            else Color.White
                        )
                    )
                    Text(
                        text = "I understand the instructions",
                        color = if (attemptedStart && !understood) 
                            Color(0xFFFFCDD2) 
                        else Color.White,
                        fontSize = 16.sp,
                        fontWeight = if (attemptedStart && !understood) 
                            FontWeight.Bold 
                        else FontWeight.Normal
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f).height(52.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    backgroundColor = Color.Transparent,
                    contentColor = Color.White
                )
            ) {
                Text("Back", fontSize = 16.sp)
            }
            
            Button(
                onClick = {
                    if (!understood) {
                        attemptedStart = true
                    } else {
                        onStartSession()
                    }
                },
                modifier = Modifier.weight(1f).height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (understood) Color(0xFF0B3C91) else Color(0xFF5476A8),
                    contentColor = Color.White
                )
            ) {
                Text("Start Session", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun formatMmSs(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%02d:%02d".format(m, s)
}
