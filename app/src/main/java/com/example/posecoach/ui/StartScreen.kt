package com.example.posecoach.ui

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.posecoach.R

@Composable
fun StartScreen(
    onLetsBeginClicked: () -> Unit,
    onAnalyzeVideoClicked: () -> Unit
) {
    // Blue vertical gradient background
    val gradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0D47A1), // dark blue
            Color(0xFF1976D2), // medium blue
            Color(0xFF42A5F5)  // light blue
        )
    )

    val activity = LocalContext.current as? Activity

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Spacer(Modifier.height(12.dp))

                // Logo at the top
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "PoseCoach Logo",
                    modifier = Modifier
                        .size(160.dp)
                        .padding(top = 8.dp)
                )

                // App title
                Text(
                    text = "PoseCoach",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                // Subtitle
                Text(
                    text = "Improve your exercise technique with real-time AI pose feedback.",
                    fontSize = 17.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(Modifier.height(12.dp))

                // How it works card
                Card(
                    backgroundColor = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp),
                    elevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "How it works",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )

                        Spacer(Modifier.height(10.dp))

                        Step("Press \"Let's Begin\" to start.")
                        Step("Place your camera so your body is clearly visible.")
                        Step("Choose the exercise you want to practice.")
                        Step("Follow the feedback on screen to correct your form.")
                    }
                }

                Spacer(Modifier.height(20.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Main button
                    Button(
                        onClick = onLetsBeginClicked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF0B3C91)
                        )
                    ) {
                        Text(
                            text = "Let's Begin",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Analyze Video button
                    Button(
                        onClick = onAnalyzeVideoClicked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF1565C0)
                        )
                    ) {
                        Text(
                            text = "Analyze Video",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Exit app button
                    Button(
                        onClick = { activity?.finish() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFFB71C1C) // red-ish
                        )
                    ) {
                        Text(
                            text = "Exit App",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Step(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = "•",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.body1,
            fontSize = 15.sp,
            color = Color.White
        )
    }
}
