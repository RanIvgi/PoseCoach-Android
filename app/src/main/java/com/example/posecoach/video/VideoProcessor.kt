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
        frameIntervalMs: Long = 500,
        maxFrames: Int = 60,
        progressCallback: (Float) -> Unit = {}
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val frames = mutableListOf<Bitmap>()
        val retriever = MediaMetadataRetriever()
        
        try {
            retriever.setDataSource(context, videoUri)
            
            // Get video duration
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L
            
            if (durationMs == 0L) {
                return@withContext emptyList()
            }
            
            // Calculate frame extraction parameters
            var currentTimeMs = 0L
            var frameCount = 0
            
            while (currentTimeMs < durationMs && frameCount < maxFrames) {
                try {
                    // Extract frame at current time
                    val frame = retriever.getFrameAtTime(
                        currentTimeMs * 1000, // Convert to microseconds
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    )
                    
                    frame?.let {
                        // Convert to ARGB_8888 if needed (MediaPipe requirement)
                        val convertedFrame = if (it.config != Bitmap.Config.ARGB_8888) {
                            it.copy(Bitmap.Config.ARGB_8888, false).also { copied ->
                                it.recycle() // Recycle original to free memory
                            }
                        } else {
                            it
                        }
                        
                        frames.add(convertedFrame)
                        frameCount++
                        
                        // Update progress
                        val progress = frameCount.toFloat() / maxFrames.coerceAtMost((durationMs / frameIntervalMs).toInt())
                        progressCallback(progress.coerceIn(0f, 1f))
                    }
                    
                    currentTimeMs += frameIntervalMs
                } catch (e: Exception) {
                    // Log frame extraction failure for debugging
                    Log.w(TAG, "Failed to extract frame at ${currentTimeMs}ms", e)
                    // Skip failed frame and continue
                    currentTimeMs += frameIntervalMs
                }
            }
            
            progressCallback(1f)
            
        } catch (e: Exception) {
            // Clean up any accumulated frames to prevent memory leak
            frames.forEach { bitmap ->
                try {
                    bitmap.recycle()
                } catch (recycleError: Exception) {
                    // Ignore recycle errors
                }
            }
            frames.clear()
            throw VideoProcessingException("Failed to extract frames: ${e.message}", e)
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Ignore release errors
            }
        }
        
        return@withContext frames
    }
    
    /**
     * Get video metadata including duration and resolution.
     * 
     * @param videoUri The URI of the video file
     * @return VideoMetadata containing video information
     */
    suspend fun getVideoMetadata(videoUri: Uri): VideoMetadata = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        
        try {
            retriever.setDataSource(context, videoUri)
            
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            
            VideoMetadata(
                durationMs = durationStr?.toLongOrNull() ?: 0L,
                width = widthStr?.toIntOrNull() ?: 0,
                height = heightStr?.toIntOrNull() ?: 0,
                rotation = rotationStr?.toIntOrNull() ?: 0
            )
        } catch (e: Exception) {
            throw VideoProcessingException("Failed to get video metadata: ${e.message}", e)
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Ignore release errors
            }
        }
    }
}

/**
 * Video metadata information
 */
data class VideoMetadata(
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val rotation: Int
)

/**
 * Exception thrown when video processing fails
 */
class VideoProcessingException(message: String, cause: Throwable? = null) : Exception(message, cause)
