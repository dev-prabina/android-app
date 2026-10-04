package com.antigravity.iptv.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.antigravity.iptv.domain.model.AccentColor
import com.antigravity.iptv.domain.model.AppTheme
import com.antigravity.iptv.domain.model.AspectRatioMode
import com.antigravity.iptv.domain.model.LayoutStyle
import com.antigravity.iptv.domain.model.UserSettings
import com.antigravity.iptv.domain.repository.ChannelRepository
import com.antigravity.iptv.domain.repository.PlaylistRepository
import com.antigravity.iptv.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val playlistRepository: PlaylistRepository,
    private val channelRepository: ChannelRepository
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings()
        )

    fun setTheme(theme: AppTheme) = viewModelScope.launch {
        settingsRepository.setTheme(theme)
    }

    fun setAccentColor(accentColor: AccentColor) = viewModelScope.launch {
        settingsRepository.setAccentColor(accentColor)
    }

    fun setLayoutStyle(layoutStyle: LayoutStyle) = viewModelScope.launch {
        settingsRepository.setLayoutStyle(layoutStyle)
    }

    fun setShowLogos(show: Boolean) = viewModelScope.launch {
        settingsRepository.setShowLogos(show)
    }

    fun setAutoPlay(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setAutoPlay(enabled)
    }

    fun setAutoReconnect(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setAutoReconnect(enabled)
    }

    fun setKeepScreenAwake(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setKeepScreenAwake(enabled)
    }

    fun setDefaultAspectRatio(mode: AspectRatioMode) = viewModelScope.launch {
        settingsRepository.setDefaultAspectRatio(mode)
    }

    fun setMiniPlayerEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setMiniPlayerEnabled(enabled)
    }

    fun setHistoryEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setHistoryEnabled(enabled)
    }

    fun setCustomUserAgent(userAgent: String) = viewModelScope.launch {
        settingsRepository.setCustomUserAgent(userAgent)
    }

    fun setPipEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setPipEnabled(enabled)
    }

    fun clearAllHistory() = viewModelScope.launch {
        channelRepository.clearWatchHistory()
    }

    fun syncAllPlaylists() = viewModelScope.launch {
        playlistRepository.ensureDefaultPlaylist()
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val playlistRepository: PlaylistRepository,
        private val channelRepository: ChannelRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(settingsRepository, playlistRepository, channelRepository) as T
        }
    }
}
