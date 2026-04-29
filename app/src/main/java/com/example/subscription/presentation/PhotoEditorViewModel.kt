package com.example.subscription.presentation

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subscription.data.remote.GeminiService
import com.example.subscription.utils.ImageFilters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PhotoEditorState {
    data object Idle : PhotoEditorState
    data object Loading : PhotoEditorState
    data class AiResult(val text: String) : PhotoEditorState
    data class Error(val message: String) : PhotoEditorState
}

class PhotoEditorViewModel(
    private val geminiService: GeminiService
) : ViewModel() {

    private val _originalBitmap = MutableStateFlow<Bitmap?>(null)
    val originalBitmap: StateFlow<Bitmap?> = _originalBitmap.asStateFlow()

    private val _filteredBitmap = MutableStateFlow<Bitmap?>(null)
    val filteredBitmap: StateFlow<Bitmap?> = _filteredBitmap.asStateFlow()

    private val _state = MutableStateFlow<PhotoEditorState>(PhotoEditorState.Idle)
    val state: StateFlow<PhotoEditorState> = _state.asStateFlow()

    fun setOriginal(bitmap: Bitmap) {
        _originalBitmap.value = bitmap
        _filteredBitmap.value = bitmap
    }

    fun applyGrayscale() {
        val source = _originalBitmap.value ?: return
        _filteredBitmap.value = ImageFilters.applyGrayscale(source)
    }

    fun applySepia() {
        val source = _originalBitmap.value ?: return
        _filteredBitmap.value = ImageFilters.applySepia(source)
    }

    fun applyBrightness() {
        val source = _originalBitmap.value ?: return
        _filteredBitmap.value = ImageFilters.applyBrightness(source, 40f)
    }

    fun applyContrast() {
        val source = _originalBitmap.value ?: return
        _filteredBitmap.value = ImageFilters.applyContrast(source, 1.5f)
    }

    fun reset() {
        _filteredBitmap.value = _originalBitmap.value
    }

    fun analyzeWithAi(prompt: String) {
        val source = _originalBitmap.value ?: return
        viewModelScope.launch {
            _state.value = PhotoEditorState.Loading
            val result = geminiService.analyzeImage(source, prompt)
            _state.value = result.fold(
                onSuccess = { PhotoEditorState.AiResult(it) },
                onFailure = { PhotoEditorState.Error(it.message ?: "Ошибка AI") }
            )
        }
    }

    fun clearState() {
        _state.value = PhotoEditorState.Idle
    }
}
