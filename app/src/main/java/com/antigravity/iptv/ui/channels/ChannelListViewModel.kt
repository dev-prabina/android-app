package com.antigravity.iptv.ui.channels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.antigravity.iptv.domain.model.Channel
import com.antigravity.iptv.domain.model.LayoutStyle
import com.antigravity.iptv.domain.model.UserSettings
import com.antigravity.iptv.domain.repository.ChannelRepository
import com.antigravity.iptv.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChannelListUiState(
    val channels: List<Channel> = emptyList(),
    val categories: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val selectedLanguage: String? = null,
    val searchQuery: String = "",
    val totalCount: Int = 0,
    val userSettings: UserSettings = UserSettings()
)

class ChannelListViewModel(
    private val channelRepository: ChannelRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _selectedLanguage = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<ChannelListUiState> = combine(
        channelRepository.getActiveChannels(),
        channelRepository.getCategories(),
        channelRepository.getLanguages(),
        _selectedCategory,
        _selectedLanguage,
        _searchQuery,
        settingsRepository.settingsFlow
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val allChannels = values[0] as List<Channel>
        @Suppress("UNCHECKED_CAST")
        val categories = values[1] as List<String>
        @Suppress("UNCHECKED_CAST")
        val languages = values[2] as List<String>
        val selectedCat = values[3] as String?
        val selectedLang = values[4] as String?
        val query = values[5] as String
        val settings = values[6] as UserSettings

        val filtered = allChannels.filter { channel ->
            val matchesCategory = selectedCat == null || channel.groupTitle.equals(selectedCat, ignoreCase = true)
            val matchesLanguage = selectedLang == null || channel.language.equals(selectedLang, ignoreCase = true)
            val matchesQuery = query.isBlank() || 
                channel.name.contains(query, ignoreCase = true) ||
                channel.tvgName.contains(query, ignoreCase = true) ||
                channel.groupTitle.contains(query, ignoreCase = true) ||
                channel.language.contains(query, ignoreCase = true)

            matchesCategory && matchesLanguage && matchesQuery
        }

        ChannelListUiState(
            channels = filtered,
            categories = categories,
            languages = languages.filter { it.isNotBlank() },
            selectedCategory = selectedCat,
            selectedLanguage = selectedLang,
            searchQuery = query,
            totalCount = allChannels.size,
            userSettings = settings
        )
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChannelListUiState()
    )

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun selectLanguage(language: String?) {
        _selectedLanguage.value = language
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavorite(channelId: Long) {
        viewModelScope.launch {
            channelRepository.toggleFavorite(channelId)
        }
    }

    fun setLayoutStyle(style: LayoutStyle) {
        viewModelScope.launch {
            settingsRepository.setLayoutStyle(style)
        }
    }

    class Factory(
        private val channelRepository: ChannelRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChannelListViewModel(channelRepository, settingsRepository) as T
        }
    }
}
