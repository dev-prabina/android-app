package com.antigravity.iptv.data.repository

import com.antigravity.iptv.data.local.dao.ChannelDao
import com.antigravity.iptv.data.local.dao.FavoriteDao
import com.antigravity.iptv.data.local.dao.WatchHistoryDao
import com.antigravity.iptv.data.local.entity.FavoriteEntity
import com.antigravity.iptv.data.local.entity.WatchHistoryEntity
import com.antigravity.iptv.domain.model.Channel
import com.antigravity.iptv.domain.model.WatchHistoryItem
import com.antigravity.iptv.domain.repository.ChannelRepository
import com.antigravity.iptv.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ChannelRepositoryImpl(
    private val channelDao: ChannelDao,
    private val watchHistoryDao: WatchHistoryDao,
    private val settingsRepository: SettingsRepository,
    private val favoriteDao: FavoriteDao? = null
) : ChannelRepository {

    override fun getAllChannels(): Flow<List<Channel>> {
        return channelDao.getAllChannels().map { list -> list.map { it.toDomain() } }
    }

    override fun getActiveChannels(): Flow<List<Channel>> {
        return channelDao.getActiveChannels().map { list -> list.map { it.toDomain() } }
    }

    override fun getChannelsByPlaylist(playlistId: Long): Flow<List<Channel>> {
        return channelDao.getChannelsByPlaylist(playlistId).map { list -> list.map { it.toDomain() } }
    }

    override fun getFavoriteChannels(): Flow<List<Channel>> {
        if (favoriteDao == null) {
            return channelDao.getFavoriteChannels().map { list -> list.map { it.toDomain() } }
        }
        return combine(
            channelDao.getActiveChannels(),
            favoriteDao.getAllFavorites()
        ) { activeChannels, storedFavorites ->
            val activeByUrl = activeChannels.associateBy { it.streamUrl.trim() }
            val activeByTvgId = activeChannels.filter { it.tvgId.isNotBlank() }.associateBy { it.tvgId.trim().lowercase() }
            val activeByName = activeChannels.associateBy { it.name.trim().lowercase() }

            storedFavorites.map { fav ->
                val matched = activeByUrl[fav.streamUrl.trim()]
                    ?: (if (fav.tvgId.isNotBlank()) activeByTvgId[fav.tvgId.trim().lowercase()] else null)
                    ?: activeByName[fav.name.trim().lowercase()]

                matched?.toDomain()?.copy(isFavorite = true) ?: fav.toDomain()
            }
        }
    }

    override fun getChannelsByCategory(category: String): Flow<List<Channel>> {
        return channelDao.getChannelsByCategory(category).map { list -> list.map { it.toDomain() } }
    }

    override fun getChannelsByLanguage(language: String): Flow<List<Channel>> {
        return channelDao.getChannelsByLanguage(language).map { list -> list.map { it.toDomain() } }
    }

    override fun getCategories(): Flow<List<String>> {
        return channelDao.getCategories()
    }

    override fun getLanguages(): Flow<List<String>> {
        return channelDao.getLanguages()
    }

    override fun searchChannels(query: String): Flow<List<Channel>> {
        return channelDao.searchChannels(query).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getChannelById(channelId: Long): Channel? = withContext(Dispatchers.IO) {
        channelDao.getChannelById(channelId)?.toDomain()
    }

    override suspend fun toggleFavorite(channelId: Long): Unit = withContext(Dispatchers.IO) {
        val channel = channelDao.getChannelById(channelId)
        if (channel != null) {
            val willBeFav = !channel.isFavorite
            channelDao.setFavorite(channelId, willBeFav)
            if (favoriteDao != null) {
                val key = FavoriteEntity.createKey(channel.tvgId, channel.name, channel.streamUrl)
                if (willBeFav) {
                    favoriteDao.insertFavorite(FavoriteEntity.fromChannel(channel.toDomain()))
                } else {
                    favoriteDao.deleteFavoriteByKey(key)
                    favoriteDao.deleteFavoriteByStreamUrl(channel.streamUrl.trim())
                }
            }
        } else if (favoriteDao != null) {
            val favs = favoriteDao.getAllFavoritesSync()
            val target = favs.find { it.id == channelId }
            if (target != null) {
                favoriteDao.deleteFavoriteByKey(target.channelKey)
                favoriteDao.deleteFavoriteByStreamUrl(target.streamUrl.trim())
            }
        }
        Unit
    }

    override suspend fun recordChannelWatch(channel: Channel): Unit = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        channelDao.updateLastWatched(channel.id, now)

        val settings = settingsRepository.settingsFlow.first()
        if (settings.historyEnabled) {
            watchHistoryDao.insertHistory(
                WatchHistoryEntity(
                    channelId = channel.id,
                    channelName = channel.displayName,
                    channelLogo = channel.logoUrl,
                    streamUrl = channel.streamUrl,
                    groupTitle = channel.groupTitle,
                    playlistId = channel.playlistId,
                    watchedAt = now
                )
            )
        }
        Unit
    }

    override fun getWatchHistory(): Flow<List<WatchHistoryItem>> {
        return watchHistoryDao.getRecentHistory().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun deleteHistoryItem(id: Long): Unit = withContext(Dispatchers.IO) {
        watchHistoryDao.deleteHistoryItem(id)
        Unit
    }

    override suspend fun clearWatchHistory(): Unit = withContext(Dispatchers.IO) {
        watchHistoryDao.clearHistory()
        Unit
    }
}
