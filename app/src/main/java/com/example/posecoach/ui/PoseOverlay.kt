package com.example.posecoach.ui

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.posecoach.data.PoseLandmarkIndex
import com.example.posecoach.data.PoseResult

/**
 * PoseOverlay - Student 1's Skeleton Visualization Component
 * 
 * This composable draws the detected pose skeleton on top of the camera preview.
 * It shows:
 * - 33 landmark points as circles
 * - Connections between landmarks as lines (skeleton)
 * - Color-coded by body part (face, arms, legs, torso)
 * 
 * Student 1 TODO List:
 * 1. Add animation for smooth landmark transitions
 * 2. Add visual feedback for landmark confidence (fade out low-confidence points)
 * 3. Customize colors and styles (themes)
 * 4. Add "trail effect" showing recent positions
 * 5. Add visual cues for exercise-specific joint angles
 * 6. Add zoom/pan capabilities if needed
 * 7. Experiment with 3D visualization using z-coordinates
 * 8. Add accessibility features (high contrast mode)
 */
@Composable
fun PoseOverlay(
    poseResult: PoseResult?,
    modifier: Modifier = Modifier,
    showLandmarks: Boolean = true,
    showConnections: Boolean = true,
    mirrorForFrontCamera: Boolean = false // Disabled: CameraX preview already handles mirroring
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        // Only draw if we have a valid pose
        if (poseResult?.hasPose() == true) {
            Log.d("PoseOverlay", "Drawing overlay with ${poseResult.landmarks.size} landmarks")
            
            val shouldMirror = mirrorForFrontCamera && poseResult.isFrontCamera
            
            // Draw connections (skeleton lines) first so landmarks appear on top
            if (showConnections) {
                drawPoseConnections(poseResult, shouldMirror)
            }
            
            // Draw landmark points
            if (showLandmarks) {
                drawPoseLandmarks(poseResult, shouldMirror)
            }
        }
    }
}

/**
 * Draw all pose landmark points.
 */
private fun DrawScope.drawPoseLandmarks(poseResult: PoseResult, mirror: Boolean) {
    poseResult.landmarks.forEachIndexed { index, landmark ->
        // Only draw reliable landmarks
        if (landmark.isReliable(threshold = 0.5f)) {
            // Convert normalized coordinates (0-1) to screen coordinates
            var x = landmark.x * size.width
            val y = landmark.y * size.height
            
            // Mirror x-coordinate for front camera
            if (mirror) {
                x = size.width - x
            }
            
            // Color-code landmarks by body part
            val color = getLandmarkColor(index)
            
            // Draw circle for landmark
            drawCircle(
                color = color,
                radius = 8f,
                center = Offset(x, y),
                alpha = landmark.visibility
            )
            
            // Draw small white center for contrast
            drawCircle(
                color = Color.White,
                radius = 3f,
                center = Offset(x, y),
                alpha = landmark.visibility
            )
        }
    }
}

/**
 * Draw connections between landmarks to form the skeleton.
 */
private fun DrawScope.drawPoseConnections(poseResult: PoseResult, mirror: Boolean) {
    // Define all pose connections (pairs of landmark indices)
    // Based on MediaPipe Pose skeleton structure
    val connections = listOf(
        // Face
        Pair(PoseLandmarkIndex.NOSE, PoseLandmarkIndex.LEFT_EYE_INNER),
        Pair(PoseLandmarkIndex.LEFT_EYE_INNER, PoseLandmarkIndex.LEFT_EYE),
        Pair(PoseLandmarkIndex.LEFT_EYE, PoseLandmarkIndex.LEFT_EYE_OUTER),
        Pair(PoseLandmarkIndex.LEFT_EYE_OUTER, PoseLandmarkIndex.LEFT_EAR),
        Pair(PoseLandmarkIndex.NOSE, PoseLandmarkIndex.RIGHT_EYE_INNER),
        Pair(PoseLandmarkIndex.RIGHT_EYE_INNER, PoseLandmarkIndex.RIGHT_EYE),
        Pair(PoseLandmarkIndex.RIGHT_EYE, PoseLandmarkIndex.RIGHT_EYE_OUTER),
        Pair(PoseLandmarkIndex.RIGHT_EYE_OUTER, PoseLandmarkIndex.RIGHT_EAR),
        Pair(PoseLandmarkIndex.MOUTH_LEFT, PoseLandmarkIndex.MOUTH_RIGHT),
        
        // Left arm
        Pair(PoseLandmarkIndex.LEFT_SHOULDER, PoseLandmarkIndex.LEFT_ELBOW),
        Pair(PoseLandmarkIndex.LEFT_ELBOW, PoseLandmarkIndex.LEFT_WRIST),
        Pair(PoseLandmarkIndex.LEFT_WRIST, PoseLandmarkIndex.LEFT_THUMB),
        Pair(PoseLandmarkIndex.LEFT_WRIST, PoseLandmarkIndex.LEFT_INDEX),
        Pair(PoseLandmarkIndex.LEFT_WRIST, PoseLandmarkIndex.LEFT_PINKY),
        Pair(PoseLandmarkIndex.LEFT_INDEX, PoseLandmarkIndex.LEFT_PINKY),
        
        // Right arm
        Pair(PoseLandmarkIndex.RIGHT_SHOULDER, PoseLandmarkIndex.RIGHT_ELBOW),
        Pair(PoseLandmarkIndex.RIGHT_ELBOW, PoseLandmarkIndex.RIGHT_WRIST),
        Pair(PoseLandmarkIndex.RIGHT_WRIST, PoseLandmarkIndex.RIGHT_THUMB),
        Pair(PoseLandmarkIndex.RIGHT_WRIST, PoseLandmarkIndex.RIGHT_INDEX),
        Pair(PoseLandmarkIndex.RIGHT_WRIST, PoseLandmarkIndex.RIGHT_PINKY),
        Pair(PoseLandmarkIndex.RIGHT_INDEX, PoseLandmarkIndex.RIGHT_PINKY),
        
        // Torso
        Pair(PoseLandmarkIndex.LEFT_SHOULDER, PoseLandmarkIndex.RIGHT_SHOULDER),
        Pair(PoseLandmarkIndex.LEFT_SHOULDER, PoseLandmarkIndex.LEFT_HIP),
        Pair(PoseLandmarkIndex.RIGHT_SHOULDER, PoseLandmarkIndex.RIGHT_HIP),
        Pair(PoseLandmarkIndex.LEFT_HIP, PoseLandmarkIndex.RIGHT_HIP),
        
        // Left leg
        Pair(PoseLandmarkIndex.LEFT_HIP, PoseLandmarkIndex.LEFT_KNEE),
        Pair(PoseLandmarkIndex.LEFT_KNEE, PoseLandmarkIndex.LEFT_ANKLE),
        Pair(PoseLandmarkIndex.LEFT_ANKLE, PoseLandmarkIndex.LEFT_HEEL),
        Pair(PoseLandmarkIndex.LEFT_ANKLE, PoseLandmarkIndex.LEFT_FOOT_INDEX),
        Pair(PoseLandmarkIndex.LEFT_HEEL, PoseLandmarkIndex.LEFT_FOOT_INDEX),
        
        // Right leg
        Pair(PoseLandmarkIndex.RIGHT_HIP, PoseLandmarkIndex.RIGHT_KNEE),
        Pair(PoseLandmarkIndex.RIGHT_KNEE, PoseLandmarkIndex.RIGHT_ANKLE),
        Pair(PoseLandmarkIndex.RIGHT_ANKLE, PoseLandmarkIndex.RIGHT_HEEL),
        Pair(PoseLandmarkIndex.RIGHT_ANKLE, PoseLandmarkIndex.RIGHT_FOOT_INDEX),
        Pair(PoseLandmarkIndex.RIGHT_HEEL, PoseLandmarkIndex.RIGHT_FOOT_INDEX)
    )
    
    // Draw each connection
    connections.forEach { (startIdx, endIdx) ->
        val start = poseResult.getLandmark(startIdx)
        val end = poseResult.getLandmark(endIdx)
        
        if (start != null && end != null) {
            // Convert to screen coordinates
            var startX = start.x * size.width
            var endX = end.x * size.width
            val startY = start.y * size.height
            val endY = end.y * size.height
            
            // Mirror for front camera
            if (mirror) {
                startX = size.width - startX
                endX = size.width - endX
            }
            
            // Determine color based on connection type
            val color = getConnectionColor(startIdx, endIdx)
            
            // Draw line
            drawLine(
                color = color,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 4f,
                cap = StrokeCap.Round,
                alpha = minOf(start.visibility, end.visibility)
            )
        }
    }
}

/**
 * Get color for a landmark based on body part.
 * Student 1 TODO: Customize these colors or make them themeable
 */
private fun getLandmarkColor(index: Int): Color {
    return when (index) {
        // Face - Yellow
        in 0..10 -> Color(0xFFFFEB3B)
        
        // Left arm - Green
        in 11..16 -> Color(0xFF4CAF50)
        
        // Right arm - Blue
        in 17..22 -> Color(0xFF2196F3)
        
        // Left leg - Cyan
        in 23..28 -> Color(0xFF00BCD4)
        
        // Right leg - Magenta
        in 29..32 -> Color(0xFFE91E63)
        
        else -> Color.White
    }
}

/**
 * Get color for a connection based on body part.
 */
private fun getConnectionColor(startIdx: Int, endIdx: Int): Color {
    // Use the color of the start landmark
    return getLandmarkColor(startIdx).copy(alpha = 0.8f)
}
