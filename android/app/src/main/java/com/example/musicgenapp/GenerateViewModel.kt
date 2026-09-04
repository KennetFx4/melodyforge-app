package com.example.musicgenapp

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Ready(val filePath: String) : UiState()
    data class Blocked(val reason: String) : UiState()
    data class Failed(val message: String) : UiState()
}

class GenerateViewModel(app: Application) : AndroidViewModel(app) {

    private val api = MusicApiClient()

    private val _state = MutableStateFlow<UiState>(UiState.Idle)
    val state: StateFlow<UiState> = _state

    fun generate(prompt: String, durationSeconds: Int) {
        if (prompt.isBlank()) {
            _state.value = UiState.Failed("Describe the music you want first.")
            return
        }
        _state.value = UiState.Loading
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                api.generate(prompt.trim(), durationSeconds)
            }
            _state.value = when (result) {
                is GenerateResult.Success -> {
                    val path = saveToDevice(result.audioBytes, result.trackId)
                    UiState.Ready(path)
                }
                is GenerateResult.Blocked -> UiState.Blocked(result.reason)
                is GenerateResult.Error -> UiState.Failed(result.message)
            }
        }
    }

    private fun saveToDevice(bytes: ByteArray, trackId: String): String {
        val musicDir = getApplication<Application>()
            .getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        val file = File(musicDir, "melodyforge_$trackId.wav")
        FileOutputStream(file).use { it.write(bytes) }
        return file.absolutePath
    }

    fun reset() {
        _state.value = UiState.Idle
    }
}
