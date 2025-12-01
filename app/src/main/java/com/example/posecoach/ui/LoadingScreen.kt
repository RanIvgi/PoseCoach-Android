package com.example.posecoach.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.posecoach.R

/**
 * LoadingScreen - Custom loading screen for API 24+ compatibility
 * 
 * PERFORMANCE FIX: Custom Compose Loading Screen
 * This replaces the Android 12+ SplashScreen API to support API 24+ devices.
 * Displays during model warm-up to hide the 2+ second initialization delay.
 * 
 * Features:
 * - Professional branded loading experience
 * - Animated progress indicator
 * - Status messages for user feedback
 * - Compatible with API 24+ (Android 7.0+)
 * - Smooth transition to main app
 * 
 * Usage:
 * ```
 * LoadingScreen(
 *     message = "Initializing AI model...",
 *     progress = 0.6f // Optional progress value
 * )
 * ```
 */
@Composable
fun LoadingScreen(
    message: String = "Loading PoseCoach...",
    progress: Float? = null // Optional: show determinate progress
) {
    // Animated alpha for pulsing effect
    val infiniteTransition = rememberInfiniteTransition(label = "loading_animation")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha_animation"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // App logo with fade animation
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "PoseCoach Logo",
                modifier = Modifier
                    .size(120.dp)
                    .padding(bottom = 32.dp),
                alpha = alpha
            )
            
            // App name
            Text(
                text = "PoseCoach",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Tagline
            Text(
                text = "AI-Powered Form Correction",
                fontSize = 16.sp,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 48.dp)
            )
            
            // Progress indicator
            if (progress != null) {
                // Determinate progress (when we can calculate percentage)
                CircularProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(bottom = 16.dp),
                    color = MaterialTheme.colors.primary,
                    strokeWidth = 4.dp
                )
            } else {
                // Indeterminate progress (default)
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(48.dp)
                        .padding(bottom = 16.dp),
                    color = MaterialTheme.colors.primary,
                    strokeWidth = 4.dp
                )
            }
            
            // Status message
            Text(
                text = message,
                fontSize = 14.sp,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
        
        // Bottom attribution
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Powered by MediaPipe",
                fontSize = 12.sp,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.4f)
            )
        }
    }
}

/**
 * ErrorScreen - Display when model initialization fails
 */
@Composable
fun LoadingErrorScreen(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "⚠️",
                fontSize = 64.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = "Initialization Failed",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.error,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = error,
                fontSize = 14.sp,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
            )
            
            androidx.compose.material.Button(
                onClick = onRetry,
                colors = androidx.compose.material.ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.primary
                )
            ) {
                Text("Retry", color = Color.White)
            }
        }
    }
}
