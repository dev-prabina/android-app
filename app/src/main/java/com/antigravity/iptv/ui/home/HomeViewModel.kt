package com.antigravity.iptv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.antigravity.iptv.domain.model.Channel
import com.antigravity.iptv.domain.model.Playlist
import com.antigravity.iptv.domain.model.SyncStatus
import com.antigravity.iptv.domain.model.UserSettings
import com.antigravity.iptv.domain.model.WatchHistoryItem
import com.antigravity.iptv.domain.repository.ChannelRepository
import com.antigravity.iptv.domain.repository.PlaylistRepository
import com.antigravity.iptv.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import com.antigravity.iptv.domain.util.OdiaChannelMatcher
import kotlinx.coroutines.launch

data class HomeUiState(
    val defaultPlaylist: Playlist? = null,
    val playlists: List<Playlist> = emptyList(),
    val categories: List<String> = emptyList(),
    val favorites: List<Channel> = emptyList(),
    val recentHistory: List<WatchHistoryItem> = emptyList(),
    val popularChannels: List<Channel> = emptyList(),
    val popularOdiaChannels: List<Channel> = emptyList(),
    val previewChannels: List<Channel> = emptyList(),
    val selectedCategory: String? = null,
    val isRefreshing: Boolean = false,
    val userSettings: UserSettings = UserSettings()
)

class HomeViewModel(
    private val channelRepository: ChannelRepository,
    private val playlistRepository: PlaylistRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _isRefreshing = MutableStateFlow(false)

    val uiState: StateFlow<HomeUiState> = combine(
        playlistRepository.getAllPlaylists(),
        channelRepository.getCategories(),
        channelRepository.getFavoriteChannels(),
        channelRepository.getWatchHistory(),
        channelRepository.getActiveChannels(),
        _selectedCategory,
        _isRefreshing,
        settingsRepository.settingsFlow
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val playlists = values[0] as List<Playlist>
        @Suppress("UNCHECKED_CAST")
        val categories = values[1] as List<String>
        @Suppress("UNCHECKED_CAST")
        val favorites = values[2] as List<Channel>
        @Suppress("UNCHECKED_CAST")
        val history = values[3] as List<WatchHistoryItem>
        @Suppress("UNCHECKED_CAST")
        val allChannels = values[4] as List<Channel>
        val selectedCategory = values[5] as String?
        val isRefreshing = values[6] as Boolean
        val settings = values[7] as UserSettings

        val filteredChannels = if (selectedCategory != null) {
            allChannels.filter { it.groupTitle.equals(selectedCategory, ignoreCase = true) }
        } else {
            allChannels.take(30)
        }

        val defaultPlaylist = playlists.firstOrNull { it.isDefault } ?: playlists.firstOrNull()

        val popularChannels = computePopularChannels(
            allChannels = allChannels,
            history = history,
            favorites = favorites
        )

        val popularOdiaChannels = OdiaChannelMatcher.sortOdiaChannels(
            channels = allChannels,
            watchHistory = history,
            favorites = favorites
        )

        HomeUiState(
            defaultPlaylist = defaultPlaylist,
            playlists = playlists,
            categories = categories,
            favorites = favorites,
            recentHistory = history,
            popularChannels = popularChannels,
            popularOdiaChannels = popularOdiaChannels,
            previewChannels = filteredChannels,
            selectedCategory = selectedCategory,
            isRefreshing = isRefreshing || (defaultPlaylist?.syncStatus == SyncStatus.SYNCING),
            userSettings = settings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun toggleFavorite(channelId: Long) {
        viewModelScope.launch {
            channelRepository.toggleFavorite(channelId)
        }
    }

    fun refreshDefaultPlaylist() {
        val defaultId = uiState.value.defaultPlaylist?.id ?: return
        viewModelScope.launch {
            _isRefreshing.value = true
            playlistRepository.refreshPlaylist(defaultId)
            _isRefreshing.value = false
        }
    }

    class Factory(
        private val channelRepository: ChannelRepository,
        private val playlistRepository: PlaylistRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(channelRepository, playlistRepository, settingsRepository) as T
        }
    }

    companion object {
        fun computePopularChannels(
            allChannels: List<Channel>,
            history: List<WatchHistoryItem>,
            favorites: List<Channel>
        ): List<Channel> {
            if (allChannels.isEmpty()) return emptyList()

            val channelMap = allChannels.associateBy { it.id }

            // 1. If user has watch history, count watch frequency from real data
            val watchCounts = history.groupingBy { it.channelId }.eachCount()

            // Channels user has watched most frequently
            val watchedPopular = watchCounts.entries
                .sortedByDescending { it.value }
                .mapNotNull { channelMap[it.key] }

            // User favorites
            val favoritePopular = favorites.filter { it.id !in watchCounts.keys }

            // 2. Sensible selection of commonly available channels from the loaded playlist
            val popularKeywords = listOf(
                "Aaj Tak", "NDTV", "India Today", "Zee", "ABP", "Republic",
                "9XM", "9X", "&TV", "WION", "CNBC", "DD", "News18",
                "Times Now", "Sony", "Star", "Colors", "Sun", "Music", "Cinema"
            )

            // Channels matching popular keywords with logos
            val matchedChannels = allChannels
                .filter { it.logoUrl.isNotBlank() }
                .filter { ch ->
                    popularKeywords.any { keyword ->
                        ch.name.contains(keyword, ignoreCase = true) ||
                        ch.tvgName.contains(keyword, ignoreCase = true)
                    }
                }

            // Top channels with logos across diverse categories from the playlist
            val categoryLeaders = allChannels
                .filter { it.logoUrl.isNotBlank() }
                .groupBy { it.groupTitle }
                .values
                .flatMap { it.take(2) }

            // Any remaining channels with logos
            val channelsWithLogos = allChannels.filter { it.logoUrl.isNotBlank() }

            val combinedList = mutableListOf<Channel>()
            val addedIds = mutableSetOf<Long>()

            fun addIfNew(channel: Channel) {
                if (addedIds.add(channel.id)) {
                    combinedList.add(channel)
                }
            }

            // Prioritize user's watched and favorites
            watchedPopular.forEach { addIfNew(it) }
            favoritePopular.forEach { addIfNew(it) }

            // For new users or to fill list: add popular matched channels from playlist
            matchedChannels.forEach { addIfNew(it) }

            // Add diverse category channels from playlist
            categoryLeaders.forEach { addIfNew(it) }

            // Add any remaining channels with logos from playlist
            channelsWithLogos.forEach { addIfNew(it) }

            // Fallback to all channels in playlist
            allChannels.forEach { addIfNew(it) }

            return combinedList.take(20)
        }
    }
}

