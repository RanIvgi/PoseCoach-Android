package com.example.posecoach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import com.example.posecoach.ui.CameraScreen
import com.example.posecoach.ui.theme.PoseCoachTheme

/**
 * MainActivity - Entry point for PoseCoach app
 * 
 * This activity:
 * - Sets up the Compose UI with Material theme
 * - Displays the CameraScreen (Student 1's main UI)
 * - Handles lifecycle properly for camera operations
 * 
 * The app flow:
 * 1. User grants camera permission
 * 2. CameraScreen opens camera and starts pose detection
 * 3. PoseEngine (Student 2) detects poses from camera frames
 * 4. DefaultPoseEvaluator (Student 3) evaluates form and generates feedback
 * 5. CameraScreen (Student 1) displays skeleton overlay and feedback
 * 
 * Student 1 TODO:
 * - Add navigation if multiple screens are needed
 * - Add splash screen
 * - Handle deep links if needed
 * - Add app shortcuts
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            PoseCoachTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    CameraScreen()
                }
            }
        }
    }
}
