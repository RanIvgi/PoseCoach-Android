package com.example.posecoach.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun PoseCoachApp() {
    val navController = rememberNavController()
    val videoAnalysisViewModel: VideoAnalysisViewModel = viewModel()
    val cameraViewModel: CameraViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "start"
    ) {
        composable("start") {
            StartScreen(
                onLetsBeginClicked = {
                    navController.navigate("camera")
                },
                onAnalyzeVideoClicked = {
                    // Clear previous results when starting new analysis
                    videoAnalysisViewModel.clearAnalysisResult()
                    navController.navigate("video_upload")
                }
            )
        }

        composable("camera") {
            CameraScreen(
                viewModel = cameraViewModel,
                navBackToStart = {
                    navController.navigate("start") {
                        popUpTo("camera") { inclusive = true }
                    }
                },
                navToSessionResults = {
                    navController.navigate("live_session_results")
                }
            )
        }

        composable("live_session_results") {
            val sessionResult by cameraViewModel.sessionResult.collectAsState()

            sessionResult?.let { result ->
                LiveSessionResultsScreen(
                    sessionResult = result,
                    navBackToStart = {
                        cameraViewModel.resetSession()
                        navController.navigate("start") {
                            popUpTo("start") { inclusive = false }
                        }
                    },
                    onStartNewExercise = {
                        cameraViewModel.resetSession()
                        navController.navigate("camera") {
                            popUpTo("live_session_results") { inclusive = true }
                        }
                    }
                )
            }
        }

        composable("video_upload") {
            VideoUploadScreen(
                viewModel = videoAnalysisViewModel,
                navBackToStart = {
                    navController.navigate("start") {
                        popUpTo("video_upload") { inclusive = true }
                    }
                },
                navToResults = { videoUri, exerciseId ->
                    navController.navigate("video_results")
                }
            )
        }

        composable("video_results") {
            val analysisResult by videoAnalysisViewModel.analysisResult.collectAsState()

            analysisResult?.let { result ->
                VideoResultsScreen(
                    analysisResult = result,
                    navBackToStart = {
                        videoAnalysisViewModel.clearAnalysisResult()
                        navController.navigate("start") {
                            popUpTo("start") { inclusive = false }
                        }
                    }
                )
            }
        }
    }
}
