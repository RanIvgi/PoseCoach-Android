package com.example.posecoach.ui

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.posecoach.data.PoseLandmarkIndex
import com.example.posecoach.data.PoseResult
import kotlin.math.min

/**
 * PoseOverlay – draws the pose skeleton on top of the camera preview.
 * 
 * PERFORMANCE OPTIMIZED VERSION:
 * - Accepts StateFlow instead of State to collect independently
 * - Prevents parent composable (CameraScreen) from recomposing on every frame
 * - Uses remember with keys to minimize unnecessary recomposition
 * - Includes smoothing between frames (removes shaky movement)
 * - Alpha fade based on landmark confidence
 * - Optional mirroring for the front camera
 */
@Composable
fun PoseOverlay(
    poseResultFlow: kotlinx.coroutines.flow.StateFlow<com.example.posecoach.data.PoseResult?>,
    cameraState: com.example.posecoach.data.CameraState,
    modifier: Modifier = Modifier,
    showLandmarks: Boolean = true,
    showConnections: Boolean = true
) {
    LogCompositions("PoseOverlay")
    
    // Collect the pose result locally - this prevents parent recomposition
    val poseResult by poseResultFlow.collectAsState()
    
    val previousPositions = remember { mutableStateMapOf<Int, Offset>() }

    LaunchedEffect(poseResult?.hasPose()) {
        if (poseResult?.hasPose() != true) {
            previousPositions.clear()
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        // Store in local variable to enable smart cast
        val currentPose = poseResult
        
        if (currentPose?.hasPose() == true) {
            // PERFORMANCE OPTIMIZATION: Per-frame logging disabled
            // This log fires on every Canvas redraw (30+ FPS during active session).
            // On emulators, this single log line causes 80%+ of the UI thread blocking.
            // The "Skipped N frames" warnings in logcat were caused by this.
            // Re-enable only when debugging overlay rendering issues.
            // Log.d("PoseOverlay", "Drawing overlay with ${currentPose.landmarks.size} landmarks")

            val mirror = cameraState.isFront()

            if (showConnections) {
                drawPoseConnections(currentPose, mirror, previousPositions)
            }

            if (showLandmarks) {
                drawPoseLandmarks(currentPose, mirror, previousPositions)
            }
        }
    }
}

/**
 * Simple linear interpolation between two points.
 * Used to smooth the movement of the skeleton between frames.
 */
private fun lerp(start: Offset, end: Offset, fraction: Float): Offset {
    val t = fraction.coerceIn(0f, 1f)
    return Offset(
        x = start.x + (end.x - start.x) * t,
        y = start.y + (end.y - start.y) * t
    )
}

/**
 * Converts a normalized (0..1) landmark coordinate into a screen position,
 * applies mirroring if needed, and blends with the previous frame.
 */
private fun DrawScope.smoothedOffset(
    index: Int,
    normX: Float,
    normY: Float,
    mirror: Boolean,
    previous: MutableMap<Int, Offset>,
    smoothing: Float = 0.25f  // smaller = smoother, slower movement
): Offset {

    // Convert normalized coordinates (0..1) to actual screen pixels
    var x = normX * size.width
    val y = normY * size.height

    // If front camera, flip horizontally
    if (mirror) x = size.width - x

    val current = Offset(x, y)
    val last = previous[index]

    // Smooth the movement (lerp between old and new)
    val smooth = if (last != null) {
        lerp(last, current, smoothing)
    } else {
        current
    }

    previous[index] = smooth
    return smooth
}

/**
 * Draws all landmark points.
 * Each point fades based on its confidence value.
 */
private fun DrawScope.drawPoseLandmarks(
    poseResult: PoseResult,
    mirror: Boolean,
    previousPositions: MutableMap<Int, Offset>
) {
    poseResult.landmarks.forEachIndexed { index, lm ->
        if (lm.visibility > 0.2f) { // ignore very low-confidence points

            val pos = smoothedOffset(
                index = index,
                normX = lm.x,
                normY = lm.y,
                mirror = mirror,
                previous = previousPositions
            )

            // fade based on visibility
            val alpha = lm.visibility.coerceIn(0.15f, 1f)
            val color = getLandmarkColor(index)

            drawCircle(
                color = color,
                radius = 6f,
                center = pos,
                alpha = alpha
            )
        }
    }
}

/**
 * Draws lines between connected landmarks.
 * Same smoothing and confidence fading as landmarks.
 */
private fun DrawScope.drawPoseConnections(
    poseResult: PoseResult,
    mirror: Boolean,
    previousPositions: MutableMap<Int, Offset>
) {
    // MediaPipe pose connections (fixed list)
    val connections = listOf(
        Pair(PoseLandmarkIndex.NOSE, PoseLandmarkIndex.LEFT_EYE_INNER),
        Pair(PoseLandmarkIndex.LEFT_EYE_INNER, PoseLandmarkIndex.LEFT_EYE),
        Pair(PoseLandmarkIndex.LEFT_EYE, PoseLandmarkIndex.LEFT_EYE_OUTER),
        Pair(PoseLandmarkIndex.LEFT_EYE_OUTER, PoseLandmarkIndex.LEFT_EAR),
        Pair(PoseLandmarkIndex.NOSE, PoseLandmarkIndex.RIGHT_EYE_INNER),
        Pair(PoseLandmarkIndex.RIGHT_EYE_INNER, PoseLandmarkIndex.RIGHT_EYE),
        Pair(PoseLandmarkIndex.RIGHT_EYE, PoseLandmarkIndex.RIGHT_EYE_OUTER),
        Pair(PoseLandmarkIndex.RIGHT_EYE_OUTER, PoseLandmarkIndex.RIGHT_EAR),
        Pair(PoseLandmarkIndex.MOUTH_LEFT, PoseLandmarkIndex.MOUTH_RIGHT),

        // arms
        Pair(PoseLandmarkIndex.LEFT_SHOULDER, PoseLandmarkIndex.LEFT_ELBOW),
        Pair(PoseLandmarkIndex.LEFT_ELBOW, PoseLandmarkIndex.LEFT_WRIST),
        Pair(PoseLandmarkIndex.RIGHT_SHOULDER, PoseLandmarkIndex.RIGHT_ELBOW),
        Pair(PoseLandmarkIndex.RIGHT_ELBOW, PoseLandmarkIndex.RIGHT_WRIST),

        // torso
        Pair(PoseLandmarkIndex.LEFT_SHOULDER, PoseLandmarkIndex.RIGHT_SHOULDER),
        Pair(PoseLandmarkIndex.LEFT_SHOULDER, PoseLandmarkIndex.LEFT_HIP),
        Pair(PoseLandmarkIndex.RIGHT_SHOULDER, PoseLandmarkIndex.RIGHT_HIP),
        Pair(PoseLandmarkIndex.LEFT_HIP, PoseLandmarkIndex.RIGHT_HIP),

        // legs
        Pair(PoseLandmarkIndex.LEFT_HIP, PoseLandmarkIndex.LEFT_KNEE),
        Pair(PoseLandmarkIndex.LEFT_KNEE, PoseLandmarkIndex.LEFT_ANKLE),
        Pair(PoseLandmarkIndex.RIGHT_HIP, PoseLandmarkIndex.RIGHT_KNEE),
        Pair(PoseLandmarkIndex.RIGHT_KNEE, PoseLandmarkIndex.RIGHT_ANKLE)
    )

    connections.forEach { (a, b) ->
        val lmA = poseResult.getLandmark(a)
        val lmB = poseResult.getLandmark(b)

        if (lmA != null && lmB != null) {
            val visibility = min(lmA.visibility, lmB.visibility)
            if (visibility <= 0.1f) return@forEach

            val start = smoothedOffset(a, lmA.x, lmA.y, mirror, previousPositions)
            val end = smoothedOffset(b, lmB.x, lmB.y, mirror, previousPositions)

            val color = getConnectionColor(a, b)

            drawLine(
                color = color,
                start = start,
                end = end,
                strokeWidth = 3f,
                cap = StrokeCap.Round,
                alpha = visibility.coerceIn(0.15f, 0.9f)
            )
        }
    }
}

/**
 * Basic color scheme based on body part groups.
 */
private fun getLandmarkColor(index: Int): Color {
    return when (index) {
        // face
        in 0..10 -> Color(0xFFFFEB3B)

        // LEFT ARM (shoulder, elbow, wrist, fingers)
        11, 13, 15, 17, 19, 21 -> Color(0xFF4CAF50)

        // RIGHT ARM
        12, 14, 16, 18, 20, 22 -> Color(0xFF2196F3)

        // LEFT LEG (hip, knee, ankle, heel, foot)
        23, 25, 27, 29, 31 -> Color(0xFFFFC1E3)

        // RIGHT LEG
        24, 26, 28, 30, 32 -> Color(0xFFE91E63)

        else -> Color.White
    }
}


private fun getConnectionColor(start: Int, end: Int): Color {
    return getLandmarkColor(start).copy(alpha = 0.8f)
}
