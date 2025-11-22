package com.example.posecoach.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * App-wide Blue Theme for PoseCoach.
 *
 * The app uses a blue primary color for most UI elements (buttons, app bar, etc.).
 * Success / error / warning colors are handled separately in components
 * like the feedback overlay, so they stay green / red / yellow.
 */

// Main blue brand colors
private val BluePrimary = Color(0xFF2196F3)        // Blue 500
private val BluePrimaryVariant = Color(0xFF1976D2) // Blue 700
private val BlueSecondary = Color(0xFF03A9F4)      // Light Blue 400

// Light mode color palette
private val LightColorPalette = lightColors(
    primary = BluePrimary,
    primaryVariant = BluePrimaryVariant,
    secondary = BlueSecondary,

    background = Color(0xFFE3F2FD),   // very light blue background
    surface = Color(0xFFF5F9FF),      // light blue surface for cards, etc.
    error = Color(0xFFB00020),

    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onError = Color.White
)

// Dark mode color palette
private val DarkColorPalette = darkColors(
    primary = BluePrimary,
    primaryVariant = BluePrimaryVariant,
    secondary = BlueSecondary,

    background = Color(0xFF0D1B2A),   // deep navy blue
    surface = Color(0xFF1B263B),      // dark blue/gray for cards
    error = Color(0xFFCF6679),

    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onError = Color.Black
)

/**
 * Main theme wrapper for the entire app.
 * It chooses between the light and dark blue palettes based on system settings.
 */
@Composable
fun PoseCoachTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorPalette else LightColorPalette

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
