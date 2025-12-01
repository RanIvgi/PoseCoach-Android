package com.example.posecoach.video

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * VideoProcessor extracts frames from a video file for pose analysis.
 * 
 * This class handles:
 * - Loading video from URI
 * - Extracting frames at specified intervals
 * - Converting frames to Bitmap format for pose detection
 */
class VideoProcessor(private val context: Context) {
    
    companion object {
        private const val TAG = "VideoProcessor"
    }
    
    /**
     * Extract frames from a video at regular intervals.
     * 
     * @param videoUri The URI of the video file
     * @param frameIntervalMs Interval between frames in milliseconds (default: 500ms = 2 FPS)
     * @param maxFrames Maximum number of frames to extract (default: 60)
     * @param progressCallback Callback for progress updates (0.0 to 1.0)
     * @return List of extracted frames as Bitmaps
     */
    suspend fun extractFrames(
        videoUri: Uri,
        frameIntervalMs: Long = 1000,
        maxFrames: Int = 120,
        progressCallback: ((Float) -> Unit)? = null
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val frames = mutableListOf<Bitmap>()
        var retriever: MediaMetadataRetriever? = null
        
        try {
            Log.d(TAG, "Initializing MediaMetadataRetriever...")
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, videoUri)
            
            // Extract metadata once at the beginning
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            
            if (duration == 0L) {
                Log.e(TAG, "Could not determine video duration")
                return@withContext emptyList()
            }
            
            Log.d(TAG, "Video duration: ${duration}ms, extracting up to $maxFrames frames at ${frameIntervalMs}ms intervals")
            
            var currentTimeMs = 0L
            var frameCount = 0
            val estimatedTotalFrames = maxFrames.coerceAtMost((duration / frameIntervalMs).toInt())
            
            while (currentTimeMs < duration && frameCount < maxFrames) {
                try {
                    // Use OPTION_CLOSEST instead of OPTION_CLOSEST_SYNC for faster extraction
                    val frame = retriever.getFrameAtTime(
                        currentTimeMs * 1000, // Convert to microseconds
                        MediaMetadataRetriever.OPTION_CLOSEST
                    )
                    
                    if (frame != null) {
                        // MediaPipe requires ARGB_8888 format, convert if necessary
                        val convertedFrame = if (frame.config != Bitmap.Config.ARGB_8888) {
                            Log.d(TAG, "Converting frame from ${frame.config} to ARGB_8888")
                            val argbFrame = frame.copy(Bitmap.Config.ARGB_8888, false)
                            frame.recycle() // Recycle the original frame
                            argbFrame
                        } else {
                            frame
                        }
                        
                        frames.add(convertedFrame)
                        frameCount++
                        
                        // Report progress
                        val progress = frameCount.toFloat() / estimatedTotalFrames
                        progressCallback?.invoke(progress)
                        
                        if (frameCount % 10 == 0) {
                            Log.d(TAG, "Extracted $frameCount/$estimatedTotalFrames frames (${(progress * 100).toInt()}%)")
                        }
                    } else {
                        Log.w(TAG, "Failed to extract frame at ${currentTimeMs}ms")
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error extracting frame at ${currentTimeMs}ms: ${e.message}", e)
                }
                
                currentTimeMs += frameIntervalMs
            }
            
            Log.d(TAG, "✓ Extracted ${frames.size} frames from video")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting frames from video: ${e.message}", e)
        } finally {
            try {
                retriever?.release()
                Log.d(TAG, "MediaMetadataRetriever released")
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing MediaMetadataRetriever: ${e.message}", e)
            }
        }
        
        frames
    }
}
