package com.antigravity.iptv.domain.model

data class WatchHistoryItem(
    val id: Long = 0,
    val channelId: Long,
    val channelName: String,
    val channelLogo: String,
    val streamUrl: String,
    val groupTitle: String,
    val playlistId: Long,
    val watchedAt: Long
)
