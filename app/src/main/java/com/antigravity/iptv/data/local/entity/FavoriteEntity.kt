package com.antigravity.iptv.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.antigravity.iptv.domain.model.Channel

@Entity(
    tableName = "favorites",
    indices = [
        Index(value = ["channelKey"], unique = true),
        Index(value = ["streamUrl"]),
        Index(value = ["tvgId"])
    ]
)
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val channelKey: String,
    val tvgId: String = "",
    val tvgName: String = "",
    val name: String,
    val logoUrl: String = "",
    val groupTitle: String = "General",
    val language: String = "",
    val country: String = "",
    val streamUrl: String,
    val playlistId: Long = 0,
    val addedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Channel = Channel(
        id = id,
        playlistId = playlistId,
        tvgId = tvgId,
        tvgName = tvgName,
        name = name,
        logoUrl = logoUrl,
        groupTitle = groupTitle,
        language = language,
        country = country,
        streamUrl = streamUrl,
        orderIndex = 0,
        isFavorite = true,
        lastWatchedAt = addedAt
    )

    companion object {
        fun createKey(tvgId: String, name: String, streamUrl: String): String {
            val idPart = if (tvgId.isNotBlank()) tvgId.trim().lowercase() else name.trim().lowercase()
            return "$idPart|${streamUrl.trim()}"
        }

        fun fromChannel(channel: Channel): FavoriteEntity = FavoriteEntity(
            channelKey = createKey(channel.tvgId, channel.name, channel.streamUrl),
            tvgId = channel.tvgId,
            tvgName = channel.tvgName,
            name = channel.name,
            logoUrl = channel.logoUrl,
            groupTitle = channel.groupTitle,
            language = channel.language,
            country = channel.country,
            streamUrl = channel.streamUrl,
            playlistId = channel.playlistId,
            addedAt = System.currentTimeMillis()
        )
    }
}
