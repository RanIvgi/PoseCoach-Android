package com.example.posecoach.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * PoseCoach Color Palette
 * 
 * Student 1 TODO:
 * - Customize these colors to match your design
 * - Add more color variations if needed
 * - Consider accessibility (contrast ratios)
 */
private val DarkColorPalette = darkColors(
    primary = Color(0xFF4CAF50),      // Green
    primaryVariant = Color(0xFF388E3C),
    secondary = Color(0xFF03DAC5),    // Teal
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    error = Color(0xFFCF6679),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onError = Color.Black
)

private val LightColorPalette = lightColors(
    primary = Color(0xFF4CAF50),      // Green
    primaryVariant = Color(0xFF388E3C),
    secondary = Color(0xFF03DAC5),    // Teal
    background = Color.White,
    surface = Color(0xFFF5F5F5),
    error = Color(0xFFB00020),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onError = Color.White
)

/**
 * PoseCoach App Theme
 * 
 * Wraps the app content with Material Design theming.
 * Automatically switches between light and dark themes based on system settings.
 */
@Composable
fun PoseCoachTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
