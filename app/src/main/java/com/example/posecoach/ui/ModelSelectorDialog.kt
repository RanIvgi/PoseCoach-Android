package com.example.posecoach.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.posecoach.data.PoseModel

/**
 * Dialog for selecting which MediaPipe Pose model to use.
 * Displays all available models with their descriptions and performance characteristics.
 */
@Composable
fun ModelSelectorDialog(
    currentModel: PoseModel,
    onModelSelected: (PoseModel) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colors.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Title
                Text(
                    text = "Select Pose Model",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Choose between speed and accuracy",
                    style = MaterialTheme.typography.body1,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Model options
                PoseModel.values().forEach { model ->
                    ModelOption(
                        model = model,
                        isSelected = model == currentModel,
                        onClick = {
                            onModelSelected(model)
                            onDismiss()
                        }
                    )
                    
                    if (model != PoseModel.values().last()) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Cancel button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun ModelOption(
    model: PoseModel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colors.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colors.surface
    }
    
    val contentColor = if (isSelected) {
        MaterialTheme.colors.primary
    } else {
        MaterialTheme.colors.onSurface
    }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = model.displayName,
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = contentColor
                )
                
                if (isSelected) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colors.primary
                    ) {
                        Text(
                            text = "Active",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = model.description,
                style = MaterialTheme.typography.body2,
                color = contentColor.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Performance indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PerformanceIndicator(
                    label = "Speed",
                    value = when (model) {
                        PoseModel.LITE -> "Fast"
                        PoseModel.FULL -> "Medium"
                        PoseModel.HEAVY -> "Slow"
                    },
                    color = contentColor
                )
                
                PerformanceIndicator(
                    label = "Accuracy",
                    value = when (model) {
                        PoseModel.LITE -> "Good"
                        PoseModel.FULL -> "Better"
                        PoseModel.HEAVY -> "Best"
                    },
                    color = contentColor
                )
            }
        }
    }
}

@Composable
private fun PerformanceIndicator(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            color = color.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.body2,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

/**
 * Simple button to open the model selector dialog.
 * Shows current model name.
 */
@Composable
fun ModelSelectorButton(
    currentModel: PoseModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(
            backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.9f)
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Model",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = currentModel.displayName,
                style = MaterialTheme.typography.button,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary
            )
        }
    }
}
