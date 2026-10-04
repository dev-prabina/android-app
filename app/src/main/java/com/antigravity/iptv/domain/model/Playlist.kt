package com.antigravity.iptv.domain.model

enum class SyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR
}

data class Playlist(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val url: String,
    val epgUrl: String = "",
    val channelCount: Int = 0,
    val lastUpdated: Long = 0,
    val isActive: Boolean = true,
    val isDefault: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.IDLE,
    val errorMessage: String? = null
)
