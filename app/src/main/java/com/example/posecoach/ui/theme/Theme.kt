package com.example.posecoach.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Modern Health App Theme for PoseCoach
 *
 * Clean, modern design with blue/teal accents.
 * Optimized for fitness and health applications.
 */

// Primary Colors
private val HealthBlue = Color(0xFF1D4ED8)
private val HealthBlueDark = Color(0xFF163FAE)
private val HealthTeal = Color(0xFF06B6D4)
private val HealthMint = Color(0xFF10B981)

// Backgrounds & Surfaces
private val BgLight = Color(0xFFF7F9FC)
private val SurfaceLight = Color(0xFFFFFFFF)
private val SurfaceTinted = Color(0xFFF1F5FF)
private val BorderLight = Color(0xFFE6EAF2)

// Text Colors
private val TextStrong = Color(0xFF0F172A)
private val TextMid = Color(0xFF334155)
private val TextMuted = Color(0xFF64748B)

// Status Colors
private val Danger = Color(0xFFEF4444)
private val Warning = Color(0xFFF59E0B)

// Light mode color palette
private val LightColorPalette = lightColors(
    primary = HealthBlue,
    primaryVariant = HealthBlueDark,
    secondary = HealthTeal,
    background = BgLight,
    surface = SurfaceLight,
    error = Danger,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextStrong,
    onSurface = TextStrong,
    onError = Color.White
)

// Dark mode color palette
private val DarkColorPalette = darkColors(
    primary = Color(0xFF6EA8FF),
    primaryVariant = Color(0xFF4B89E8),
    secondary = Color(0xFF4CD7EA),
    background = Color(0xFF0B1220),
    surface = Color(0xFF0F172A),
    error = Color(0xFFFF6B6B),
    onPrimary = Color(0xFF071226),
    onSecondary = Color(0xFF041317),
    onBackground = Color(0xFFE7EDF8),
    onSurface = Color(0xFFE7EDF8),
    onError = Color(0xFF1B0B0B)
)

/**
 * Main theme wrapper for the entire app.
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
