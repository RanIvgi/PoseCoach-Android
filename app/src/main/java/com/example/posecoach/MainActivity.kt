package com.example.posecoach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.posecoach.ui.LoadingErrorScreen
import com.example.posecoach.ui.LoadingScreen
import com.example.posecoach.ui.PoseCoachApp
import com.example.posecoach.ui.theme.PoseCoachTheme

/**
 * MainActivity - Entry point for PoseCoach app
 * 
 * PERFORMANCE FIX: Model Warm-up on Startup
 * - Starts background model initialization in onCreate() to eliminate 2s+ freeze
 * - Shows loading screen while model loads (smooth UX, no perceived lag)
 * - Main thread remains responsive during initialization
 * 
 * This activity:
 * - Initializes ModelWarmer to pre-load MediaPipe model
 * - Shows loading screen during warm-up (API 24+ compatible)
 * - Displays PoseCoachApp once model is ready
 * - Handles lifecycle properly for camera operations
 * 
 * The app flow:
 * 1. App starts → ModelWarmer begins background initialization
 * 2. Loading screen displays (branded, professional)
 * 3. Once ready → Navigate to main app
 * 4. User grants camera permission
 * 5. CameraScreen uses pre-warmed PoseEngine (instant, no freeze)
 * 6. PoseEngine (Student 2) detects poses from camera frames
 * 7. DefaultPoseEvaluator (Student 3) evaluates form and generates feedback
 * 8. CameraScreen (Student 1) displays skeleton overlay and feedback
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // PERFORMANCE FIX: Start model warm-up immediately on app launch
        // This runs in background and eliminates the 2+ second freeze
        ModelWarmer.getInstance(applicationContext).startWarmup()
        
        setContent {
            PoseCoachTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    // Observe warm-up state and show appropriate screen
                    val warmupState by ModelWarmer.getInstance(applicationContext)
                        .warmupState.collectAsState()
                    
                    when (warmupState) {
                        WarmupState.NotStarted, WarmupState.InProgress -> {
                            LoadingScreen(message = "Initializing AI model...")
                        }
                        WarmupState.Completed -> {
                            PoseCoachApp()
                        }
                        is WarmupState.Failed -> {
                            LoadingErrorScreen(
                                error = (warmupState as WarmupState.Failed).error,
                                onRetry = {
                                    ModelWarmer.getInstance(applicationContext).reset()
                                    ModelWarmer.getInstance(applicationContext).startWarmup()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
