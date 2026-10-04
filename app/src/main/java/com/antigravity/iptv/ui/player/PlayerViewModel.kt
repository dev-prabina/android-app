package com.antigravity.iptv.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.antigravity.iptv.domain.model.Channel
import com.antigravity.iptv.domain.model.UserSettings
import com.antigravity.iptv.domain.repository.ChannelRepository
import com.antigravity.iptv.domain.repository.SettingsRepository
import com.antigravity.iptv.player.PlayerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlayerUiState(
    val channel: Channel? = null,
    val channelList: List<Channel> = emptyList(),
    val isChannelListOpen: Boolean = false,
    val isLocked: Boolean = false,
    val isControlsVisible: Boolean = true,
    val userSettings: UserSettings = UserSettings()
)

class PlayerViewModel(
    val playerManager: PlayerManager,
    private val channelRepository: ChannelRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.value = _uiState.value.copy(userSettings = settings)
            }
        }

        viewModelScope.launch {
            channelRepository.getActiveChannels().collect { channels ->
                _uiState.value = _uiState.value.copy(channelList = channels)
            }
        }
    }

    fun loadChannel(channelId: Long) {
        viewModelScope.launch {
            val channel = channelRepository.getChannelById(channelId)
            if (channel != null) {
                _uiState.value = _uiState.value.copy(channel = channel)
                playerManager.playChannel(channel, _uiState.value.channelList)
                channelRepository.recordChannelWatch(channel)
            }
        }
    }

    fun toggleControlsVisibility() {
        if (_uiState.value.isLocked) return
        _uiState.value = _uiState.value.copy(isControlsVisible = !_uiState.value.isControlsVisible)
    }

    fun setControlsVisibility(visible: Boolean) {
        if (_uiState.value.isLocked) return
        _uiState.value = _uiState.value.copy(isControlsVisible = visible)
    }

    fun toggleLock() {
        val newLocked = !_uiState.value.isLocked
        _uiState.value = _uiState.value.copy(
            isLocked = newLocked,
            isControlsVisible = !newLocked
        )
    }

    fun toggleChannelList() {
        _uiState.value = _uiState.value.copy(isChannelListOpen = !_uiState.value.isChannelListOpen)
    }

    fun closeChannelList() {
        _uiState.value = _uiState.value.copy(isChannelListOpen = false)
    }

    fun toggleFavorite() {
        val current = playerManager.currentChannel.value ?: _uiState.value.channel ?: return
        viewModelScope.launch {
            channelRepository.toggleFavorite(current.id)
            val updated = channelRepository.getChannelById(current.id)
            if (updated != null) {
                _uiState.value = _uiState.value.copy(channel = updated)
            }
        }
    }

    fun switchChannel(channel: Channel) {
        _uiState.value = _uiState.value.copy(channel = channel, isChannelListOpen = false)
        playerManager.playChannel(channel, _uiState.value.channelList)
        viewModelScope.launch {
            channelRepository.recordChannelWatch(channel)
        }
    }

    class Factory(
        private val playerManager: PlayerManager,
        private val channelRepository: ChannelRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlayerViewModel(playerManager, channelRepository, settingsRepository) as T
        }
    }
}
