package com.example.posecoach.ui

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.posecoach.R

@Composable
fun StartScreen(
    onLetsBeginClicked: () -> Unit,
    onAnalyzeVideoClicked: () -> Unit
) {
    val activity = LocalContext.current as? Activity

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colors.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Background image
            Image(
                painter = painterResource(id = R.drawable.background_image),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // White fade overlay (70%)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.7f))
            )
            
            // Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Spacer(Modifier.height(12.dp))

                // App title above logo
                Text(
                    text = "PoseCoach",
                    fontSize = 42.sp,  // Larger for Passion One
                    fontFamily = FontFamily(Font(R.font.passion_one_black)),  // Custom font
                    letterSpacing = 1.5.sp,  // Letter spacing
                    color = Color(0xFF0D47A1),  // Darker blue
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                // Logo
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "PoseCoach Logo",
                    modifier = Modifier.size(160.dp)
                )

                // Subtitle
                Text(
                    text = "Improve your exercise technique with real-time AI pose feedback.",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(Modifier.height(12.dp))

                // How it works card
                Card(
                    backgroundColor = MaterialTheme.colors.surface,
                    shape = RoundedCornerShape(16.dp),
                    elevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "How it works",
                            color = MaterialTheme.colors.primary,
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
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.primary,
                            contentColor = MaterialTheme.colors.onPrimary
                        ),
                        elevation = ButtonDefaults.elevation(
                            defaultElevation = 8.dp,
                            pressedElevation = 12.dp
                        )
                    ) {
                        Text(
                            text = "Let's Begin",
                            style = MaterialTheme.typography.button,
                            fontSize = 20.sp
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Analyze Video button
                    Button(
                        onClick = onAnalyzeVideoClicked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFFD0DEFF),
                            contentColor = MaterialTheme.colors.primary
                        ),
                        elevation = ButtonDefaults.elevation(
                            defaultElevation = 2.dp,
                            pressedElevation = 4.dp
                        )
                    ) {
                        Text(
                            text = "Analyze Video",
                            style = MaterialTheme.typography.button,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Exit app button
                    Button(
                        onClick = { activity?.finish() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFFEF5350),
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.elevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        Text(
                            text = "Exit App",
                            style = MaterialTheme.typography.button,
                            fontSize = 20.sp
                        )
                    }
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
            color = MaterialTheme.colors.secondary
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.body1,
            fontSize = 15.sp,
            color = MaterialTheme.colors.onSurface
        )
    }
}
