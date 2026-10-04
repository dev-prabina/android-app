package com.antigravity.iptv.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.antigravity.iptv.domain.model.Playlist
import com.antigravity.iptv.domain.model.SyncStatus

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val url: String,
    val epgUrl: String = "",
    val channelCount: Int = 0,
    val lastUpdated: Long = 0,
    val isActive: Boolean = true,
    val isDefault: Boolean = false,
    val syncStatus: String = SyncStatus.IDLE.name,
    val errorMessage: String? = null
) {
    fun toDomain(): Playlist = Playlist(
        id = id,
        name = name,
        description = description,
        url = url,
        epgUrl = epgUrl,
        channelCount = channelCount,
        lastUpdated = lastUpdated,
        isActive = isActive,
        isDefault = isDefault,
        syncStatus = runCatching { SyncStatus.valueOf(syncStatus) }.getOrDefault(SyncStatus.IDLE),
        errorMessage = errorMessage
    )

    companion object {
        fun fromDomain(playlist: Playlist): PlaylistEntity = PlaylistEntity(
            id = playlist.id,
            name = playlist.name,
            description = playlist.description,
            url = playlist.url,
            epgUrl = playlist.epgUrl,
            channelCount = playlist.channelCount,
            lastUpdated = playlist.lastUpdated,
            isActive = playlist.isActive,
            isDefault = playlist.isDefault,
            syncStatus = playlist.syncStatus.name,
            errorMessage = playlist.errorMessage
        )
    }
}
