package com.example.posecoach.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VideoAnalysisViewModel : ViewModel() {
    
    private val _selectedVideoUri = MutableStateFlow<Uri?>(null)
    val selectedVideoUri: StateFlow<Uri?> = _selectedVideoUri.asStateFlow()
    
    private val _selectedExercise = MutableStateFlow<String?>(null)
    val selectedExercise: StateFlow<String?> = _selectedExercise.asStateFlow()
    
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()
    
    private val _processingProgress = MutableStateFlow(0f)
    val processingProgress: StateFlow<Float> = _processingProgress.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    fun setVideoUri(uri: Uri) {
        _selectedVideoUri.value = uri
        _errorMessage.value = null
    }
    
    fun setExercise(exerciseId: String) {
        _selectedExercise.value = exerciseId
        _errorMessage.value = null
    }
    
    fun startAnalysis(videoUri: String, exerciseId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                _errorMessage.value = null
                _processingProgress.value = 0f
                
                // Simulate processing with progress updates
                // In real implementation, this will process the video
                for (i in 1..10) {
                    kotlinx.coroutines.delay(200)
                    _processingProgress.value = i / 10f
                }
                
                _isProcessing.value = false
                onComplete()
            } catch (e: Exception) {
                _isProcessing.value = false
                _errorMessage.value = "Error processing video: ${e.message}"
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}
