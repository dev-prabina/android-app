package com.antigravity.iptv.domain.repository

import com.antigravity.iptv.domain.model.Playlist
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun getAllPlaylists(): Flow<List<Playlist>>
    suspend fun getPlaylistById(id: Long): Playlist?
    suspend fun getDefaultPlaylist(): Playlist?
    suspend fun addPlaylist(name: String, description: String, url: String, epgUrl: String = ""): Long
    suspend fun updatePlaylist(playlist: Playlist)
    suspend fun deletePlaylist(playlist: Playlist)
    suspend fun setPlaylistActive(id: Long, isActive: Boolean)
    suspend fun refreshPlaylist(id: Long): Result<Int>
    suspend fun testPlaylistUrl(url: String): Result<Boolean>
    suspend fun ensureDefaultPlaylist()
}
