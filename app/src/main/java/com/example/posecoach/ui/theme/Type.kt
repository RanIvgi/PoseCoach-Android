package com.example.posecoach.ui.theme

import androidx.compose.material.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Modern Health App Typography
 *
 * Clean, professional typography optimized for fitness and health apps.
 * Uses system default font (Roboto on Android) for excellent readability.
 *
 * To use custom fonts like Inter or Manrope:
 * 1. Download .ttf files (inter_regular.ttf, inter_medium.ttf, etc.)
 * 2. Place in app/src/main/res/font/
 * 3. Replace HealthFont with:
 *    val HealthFont = FontFamily(
 *        Font(R.font.inter_regular, FontWeight.Normal),
 *        Font(R.font.inter_medium, FontWeight.Medium),
 *        Font(R.font.inter_semibold, FontWeight.SemiBold),
 *        Font(R.font.inter_bold, FontWeight.Bold)
 *    )
 */

// Using system default font (Roboto) - clean and professional
val HealthFont = FontFamily.Default

val Typography = Typography(
    // Big titles (for main screens, hero numbers)
    h4 = TextStyle(
        fontFamily = HealthFont,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        letterSpacing = (-0.5).sp
    ),

    // Section headers
    h5 = TextStyle(
        fontFamily = HealthFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        letterSpacing = 0.sp
    ),

    // Subsection headers / Card titles
    h6 = TextStyle(
        fontFamily = HealthFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        letterSpacing = 0.sp
    ),

    // Primary body text
    body1 = TextStyle(
        fontFamily = HealthFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),

    // Secondary body text
    body2 = TextStyle(
        fontFamily = HealthFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),

    // Small labels, captions
    caption = TextStyle(
        fontFamily = HealthFont,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.4.sp
    ),

    // Button text
    button = TextStyle(
        fontFamily = HealthFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        letterSpacing = 0.5.sp
    )
)
