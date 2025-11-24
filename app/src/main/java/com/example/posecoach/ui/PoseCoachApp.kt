package com.example.posecoach.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun PoseCoachApp() {
    val navController = rememberNavController()

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
                    navController.navigate("video_upload")
                }
            )
        }

        composable("camera") {
            CameraScreen(
                navBackToStart = {
                    navController.navigate("start") {
                        popUpTo("camera") { inclusive = true }
                    }
                }
            )
        }

        composable("video_upload") {
            VideoUploadScreen(
                navBackToStart = {
                    navController.navigate("start") {
                        popUpTo("video_upload") { inclusive = true }
                    }
                },
                navToResults = { videoUri, exerciseId ->
                    val encodedUri = Uri.encode(videoUri)
                    navController.navigate("video_results/$encodedUri/$exerciseId")
                }
            )
        }

        composable(
            route = "video_results/{videoUri}/{exerciseId}",
            arguments = listOf(
                navArgument("videoUri") { type = NavType.StringType },
                navArgument("exerciseId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("videoUri") ?: ""
            val videoUri = Uri.decode(encodedUri)
            val exerciseId = backStackEntry.arguments?.getString("exerciseId") ?: ""
            VideoResultsScreen(
                videoUri = videoUri,
                exerciseId = exerciseId,
                navBackToStart = {
                    navController.navigate("start") {
                        popUpTo("start") { inclusive = false }
                    }
                }
            )
        }
    }
}
