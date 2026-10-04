package com.antigravity.iptv.data.repository

import android.util.Log
import com.antigravity.iptv.data.local.dao.ChannelDao
import com.antigravity.iptv.data.local.dao.FavoriteDao
import com.antigravity.iptv.data.local.dao.PlaylistDao
import com.antigravity.iptv.data.local.entity.FavoriteEntity
import com.antigravity.iptv.data.local.entity.PlaylistEntity
import com.antigravity.iptv.data.parser.M3UParser
import com.antigravity.iptv.data.remote.PlaylistDownloader
import com.antigravity.iptv.domain.model.Playlist
import com.antigravity.iptv.domain.model.SyncStatus
import com.antigravity.iptv.domain.repository.PlaylistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class PlaylistRepositoryImpl(
    private val playlistDao: PlaylistDao,
    private val channelDao: ChannelDao,
    private val playlistDownloader: PlaylistDownloader,
    private val favoriteDao: FavoriteDao? = null
) : PlaylistRepository {

    companion object {
        private const val TAG = "PlaylistRepository"
        const val DEFAULT_PLAYLIST_URL = "https://iptv-org.github.io/iptv/countries/in.m3u"
        const val DEFAULT_PLAYLIST_NAME = "India Channels"
        const val DEFAULT_PLAYLIST_DESC = "Built-in India Live TV Channels"
    }

    override fun getAllPlaylists(): Flow<List<Playlist>> {
        return playlistDao.getAllPlaylists().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getPlaylistById(id: Long): Playlist? = withContext(Dispatchers.IO) {
        playlistDao.getPlaylistById(id)?.toDomain()
    }

    override suspend fun getDefaultPlaylist(): Playlist? = withContext(Dispatchers.IO) {
        playlistDao.getDefaultPlaylist()?.toDomain()
    }

    override suspend fun addPlaylist(
        name: String,
        description: String,
        url: String,
        epgUrl: String
    ): Long = withContext(Dispatchers.IO) {
        val entity = PlaylistEntity(
            name = name.trim(),
            description = description.trim(),
            url = url.trim(),
            epgUrl = epgUrl.trim(),
            channelCount = 0,
            lastUpdated = 0,
            isActive = true,
            isDefault = false,
            syncStatus = SyncStatus.IDLE.name
        )
        val id = playlistDao.insertPlaylist(entity)
        // Automatically start refresh
        refreshPlaylist(id)
        id
    }

    override suspend fun updatePlaylist(playlist: Playlist): Unit = withContext(Dispatchers.IO) {
        playlistDao.updatePlaylist(PlaylistEntity.fromDomain(playlist))
        Unit
    }

    override suspend fun deletePlaylist(playlist: Playlist): Unit = withContext(Dispatchers.IO) {
        // Cascade delete channels
        channelDao.deleteChannelsByPlaylist(playlist.id)
        playlistDao.deletePlaylist(PlaylistEntity.fromDomain(playlist))
        Unit
    }

    override suspend fun setPlaylistActive(id: Long, isActive: Boolean): Unit = withContext(Dispatchers.IO) {
        playlistDao.setPlaylistActive(id, isActive)
        Unit
    }

    override suspend fun refreshPlaylist(id: Long): Result<Int> = withContext(Dispatchers.IO) {
        val playlist = playlistDao.getPlaylistById(id)
            ?: return@withContext Result.failure(Exception("Playlist not found"))

        // Set status to syncing
        playlistDao.updateSyncStatus(
            id = id,
            status = SyncStatus.SYNCING.name,
            channelCount = playlist.channelCount,
            timestamp = playlist.lastUpdated,
            errorMessage = null
        )

        val downloadResult = playlistDownloader.downloadStream(playlist.url) { inputStream ->
            M3UParser.parse(inputStream, id)
        }

        downloadResult.fold(
            onSuccess = { channels ->
                if (channels.isEmpty()) {
                    playlistDao.updateSyncStatus(
                        id = id,
                        status = SyncStatus.ERROR.name,
                        channelCount = playlist.channelCount,
                        timestamp = playlist.lastUpdated,
                        errorMessage = "No valid channels found in playlist"
                    )
                    Result.failure(Exception("No valid channels found"))
                } else {
                    try {
                        val favoriteKeys = favoriteDao?.getAllFavoriteKeys()?.toSet() ?: emptySet()
                        val favoriteUrls = favoriteDao?.getAllFavoritesSync()?.map { it.streamUrl.trim() }?.toSet() ?: emptySet()

                        val restoredChannels = channels.map { ch ->
                            val key = FavoriteEntity.createKey(ch.tvgId, ch.name, ch.streamUrl)
                            if (key in favoriteKeys || ch.streamUrl.trim() in favoriteUrls) {
                                ch.copy(isFavorite = true)
                            } else {
                                ch
                            }
                        }

                        channelDao.deleteChannelsByPlaylist(id)
                        channelDao.insertChannels(restoredChannels)
                        val now = System.currentTimeMillis()
                        playlistDao.updateSyncStatus(
                            id = id,
                            status = SyncStatus.SUCCESS.name,
                            channelCount = channels.size,
                            timestamp = now,
                            errorMessage = null
                        )
                        Log.i(TAG, "Successfully synced playlist '${playlist.name}' with ${channels.size} channels")
                        Result.success(channels.size)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to persist channels into database", e)
                        playlistDao.updateSyncStatus(
                            id = id,
                            status = SyncStatus.ERROR.name,
                            channelCount = playlist.channelCount,
                            timestamp = playlist.lastUpdated,
                            errorMessage = e.localizedMessage ?: "Database error"
                        )
                        Result.failure(e)
                    }
                }
            },
            onFailure = { error ->
                Log.e(TAG, "Failed to download playlist '${playlist.name}'", error)
                // Keep previously cached channels, only mark status as error
                playlistDao.updateSyncStatus(
                    id = id,
                    status = SyncStatus.ERROR.name,
                    channelCount = playlist.channelCount,
                    timestamp = playlist.lastUpdated,
                    errorMessage = error.localizedMessage ?: "Failed to connect to stream host"
                )
                Result.failure(error)
            }
        )
    }

    override suspend fun testPlaylistUrl(url: String): Result<Boolean> {
        return playlistDownloader.testConnection(url)
    }

    override suspend fun ensureDefaultPlaylist(): Unit = withContext(Dispatchers.IO) {
        val existingDefault = playlistDao.getDefaultPlaylist()
        if (existingDefault == null) {
            val defaultEntity = PlaylistEntity(
                name = DEFAULT_PLAYLIST_NAME,
                description = DEFAULT_PLAYLIST_DESC,
                url = DEFAULT_PLAYLIST_URL,
                epgUrl = "",
                channelCount = 0,
                lastUpdated = 0,
                isActive = true,
                isDefault = true,
                syncStatus = SyncStatus.IDLE.name
            )
            val id = playlistDao.insertPlaylist(defaultEntity)
            Log.i(TAG, "Registered default playlist ID: $id")
            refreshPlaylist(id)
        } else {
            // If default playlist has 0 channels, trigger refresh
            val count = channelDao.getChannelCount(existingDefault.id)
            if (count == 0) {
                Log.i(TAG, "Default playlist has 0 channels, triggering refresh")
                refreshPlaylist(existingDefault.id)
            }
        }
        Unit
    }
}
