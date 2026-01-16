package com.example.posecoach.ui

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun VideoPlayer(
    videoRes: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    AndroidView(
        factory = { ctx ->
            VideoView(ctx).apply {
                val videoUri = Uri.parse("android.resource://${ctx.packageName}/$videoRes")
                setVideoURI(videoUri)
                
                setOnPreparedListener { mediaPlayer ->
                    mediaPlayer.isLooping = true
                    mediaPlayer.setVolume(0f, 0f) // Mute the video
                }
                
                start()
            }
        },
        modifier = modifier.fillMaxWidth(),
        update = { videoView ->
            // Restart video if it's not playing
            if (!videoView.isPlaying) {
                videoView.start()
            }
        }
    )
    
    // Clean up when composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            // VideoView will be cleaned up automatically
        }
    }
}
