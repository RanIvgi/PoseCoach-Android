package com.example.posecoach.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shape configuration for the PoseCoach app.
 *
 * We use rounded corners to give the UI a softer, more modern look
 * that fits a fitness app. Different sizes are used for different
 * component types:
 *  - small: buttons, chips
 *  - medium: cards, small panels
 *  - large: dialogs, bottom sheets
 */
val Shapes = Shapes(
    small = RoundedCornerShape(8.dp),   // compact elements (e.g. buttons, chips)
    medium = RoundedCornerShape(16.dp), // cards and containers
    large = RoundedCornerShape(24.dp)   // dialogs, sheets, bigger surfaces
)