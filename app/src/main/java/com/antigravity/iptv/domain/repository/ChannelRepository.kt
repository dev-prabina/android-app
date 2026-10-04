package com.antigravity.iptv.domain.repository

import com.antigravity.iptv.domain.model.Channel
import com.antigravity.iptv.domain.model.WatchHistoryItem
import kotlinx.coroutines.flow.Flow

interface ChannelRepository {
    fun getAllChannels(): Flow<List<Channel>>
    fun getActiveChannels(): Flow<List<Channel>>
    fun getChannelsByPlaylist(playlistId: Long): Flow<List<Channel>>
    fun getFavoriteChannels(): Flow<List<Channel>>
    fun getChannelsByCategory(category: String): Flow<List<Channel>>
    fun getChannelsByLanguage(language: String): Flow<List<Channel>>
    fun getCategories(): Flow<List<String>>
    fun getLanguages(): Flow<List<String>>
    fun searchChannels(query: String): Flow<List<Channel>>
    suspend fun getChannelById(channelId: Long): Channel?
    suspend fun toggleFavorite(channelId: Long)
    suspend fun recordChannelWatch(channel: Channel)
    fun getWatchHistory(): Flow<List<WatchHistoryItem>>
    suspend fun deleteHistoryItem(id: Long)
    suspend fun clearWatchHistory()
}
