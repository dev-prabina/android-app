package com.antigravity.iptv.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.antigravity.iptv.domain.model.Channel
import com.antigravity.iptv.domain.model.UserSettings
import com.antigravity.iptv.domain.repository.ChannelRepository
import com.antigravity.iptv.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class FavoriteSortOrder(val title: String) {
    RECENT("Recently Watched"),
    NAME("Name (A-Z)"),
    CATEGORY("Category")
}

data class FavoritesUiState(
    val favorites: List<Channel> = emptyList(),
    val sortOrder: FavoriteSortOrder = FavoriteSortOrder.RECENT,
    val userSettings: UserSettings = UserSettings()
)

class FavoritesViewModel(
    private val channelRepository: ChannelRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _sortOrder = MutableStateFlow(FavoriteSortOrder.RECENT)

    val uiState: StateFlow<FavoritesUiState> = combine(
        channelRepository.getFavoriteChannels(),
        _sortOrder,
        settingsRepository.settingsFlow
    ) { channels, sort, settings ->
        val sorted = when (sort) {
            FavoriteSortOrder.RECENT -> channels.sortedByDescending { it.lastWatchedAt ?: 0 }
            FavoriteSortOrder.NAME -> channels.sortedBy { it.name.lowercase() }
            FavoriteSortOrder.CATEGORY -> channels.sortedBy { it.groupTitle.lowercase() }
        }
        FavoritesUiState(
            favorites = sorted,
            sortOrder = sort,
            userSettings = settings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FavoritesUiState()
    )

    fun setSortOrder(order: FavoriteSortOrder) {
        _sortOrder.value = order
    }

    fun toggleFavorite(channelId: Long) {
        viewModelScope.launch {
            channelRepository.toggleFavorite(channelId)
        }
    }

    class Factory(
        private val channelRepository: ChannelRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FavoritesViewModel(channelRepository, settingsRepository) as T
        }
    }
}
