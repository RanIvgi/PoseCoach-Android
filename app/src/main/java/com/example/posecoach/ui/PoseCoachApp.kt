package com.example.posecoach.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

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
    }
}
