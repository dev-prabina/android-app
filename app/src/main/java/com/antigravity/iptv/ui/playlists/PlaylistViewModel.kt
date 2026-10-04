package com.antigravity.iptv.ui.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.antigravity.iptv.domain.model.Playlist
import com.antigravity.iptv.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlaylistManagerUiState(
    val playlists: List<Playlist> = emptyList(),
    val isTestingUrl: Boolean = false,
    val testUrlResult: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

class PlaylistViewModel(
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistManagerUiState())
    val uiState: StateFlow<PlaylistManagerUiState> = _uiState.asStateFlow()

    val playlists: StateFlow<List<Playlist>> = playlistRepository.getAllPlaylists()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun refreshPlaylist(playlistId: Long) {
        viewModelScope.launch {
            playlistRepository.refreshPlaylist(playlistId)
        }
    }

    fun togglePlaylistActive(playlistId: Long, isActive: Boolean) {
        viewModelScope.launch {
            playlistRepository.setPlaylistActive(playlistId, isActive)
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlist)
        }
    }

    fun testUrl(url: String) {
        if (url.isBlank()) {
            _uiState.value = _uiState.value.copy(testUrlResult = "URL cannot be empty")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTestingUrl = true, testUrlResult = null)
            val result = playlistRepository.testPlaylistUrl(url.trim())
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isTestingUrl = false,
                        testUrlResult = "Success: Stream host is reachable!"
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isTestingUrl = false,
                        testUrlResult = "Failed: ${error.localizedMessage ?: "Cannot connect to server"}"
                    )
                }
            )
        }
    }

    fun addPlaylist(name: String, description: String, url: String, epgUrl: String) {
        if (name.isBlank() || url.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Playlist name and URL are required")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            try {
                playlistRepository.addPlaylist(name, description, url, epgUrl)
                _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = e.localizedMessage ?: "Failed to save playlist"
                )
            }
        }
    }

    fun updatePlaylist(playlist: Playlist, name: String, description: String, url: String, epgUrl: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            try {
                val updated = playlist.copy(
                    name = name.trim(),
                    description = description.trim(),
                    url = url.trim(),
                    epgUrl = epgUrl.trim()
                )
                playlistRepository.updatePlaylist(updated)
                playlistRepository.refreshPlaylist(updated.id)
                _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = e.localizedMessage ?: "Failed to update playlist"
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = PlaylistManagerUiState()
    }

    class Factory(
        private val playlistRepository: PlaylistRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlaylistViewModel(playlistRepository) as T
        }
    }
}
