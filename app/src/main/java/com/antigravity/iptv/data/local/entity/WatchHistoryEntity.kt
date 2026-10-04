package com.antigravity.iptv.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.antigravity.iptv.domain.model.WatchHistoryItem

@Entity(
    tableName = "watch_history",
    indices = [
        Index(value = ["watchedAt"]),
        Index(value = ["channelId"])
    ]
)
data class WatchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val channelId: Long,
    val channelName: String,
    val channelLogo: String,
    val streamUrl: String,
    val groupTitle: String,
    val playlistId: Long,
    val watchedAt: Long
) {
    fun toDomain(): WatchHistoryItem = WatchHistoryItem(
        id = id,
        channelId = channelId,
        channelName = channelName,
        channelLogo = channelLogo,
        streamUrl = streamUrl,
        groupTitle = groupTitle,
        playlistId = playlistId,
        watchedAt = watchedAt
    )

    companion object {
        fun fromDomain(item: WatchHistoryItem): WatchHistoryEntity = WatchHistoryEntity(
            id = item.id,
            channelId = item.channelId,
            channelName = item.channelName,
            channelLogo = item.channelLogo,
            streamUrl = item.streamUrl,
            groupTitle = item.groupTitle,
            playlistId = item.playlistId,
            watchedAt = item.watchedAt
        )
    }
}
