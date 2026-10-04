package com.antigravity.iptv.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.antigravity.iptv.domain.model.Channel

@Entity(
    tableName = "channels",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["playlistId"]),
        Index(value = ["groupTitle"]),
        Index(value = ["language"]),
        Index(value = ["isFavorite"]),
        Index(value = ["name"])
    ]
)
data class ChannelEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playlistId: Long,
    val tvgId: String = "",
    val tvgName: String = "",
    val name: String,
    val logoUrl: String = "",
    val groupTitle: String = "General",
    val language: String = "",
    val country: String = "",
    val streamUrl: String,
    val orderIndex: Int = 0,
    val isFavorite: Boolean = false,
    val lastWatchedAt: Long? = null
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
        orderIndex = orderIndex,
        isFavorite = isFavorite,
        lastWatchedAt = lastWatchedAt
    )

    companion object {
        fun fromDomain(channel: Channel): ChannelEntity = ChannelEntity(
            id = channel.id,
            playlistId = channel.playlistId,
            tvgId = channel.tvgId,
            tvgName = channel.tvgName,
            name = channel.name,
            logoUrl = channel.logoUrl,
            groupTitle = channel.groupTitle,
            language = channel.language,
            country = channel.country,
            streamUrl = channel.streamUrl,
            orderIndex = channel.orderIndex,
            isFavorite = channel.isFavorite,
            lastWatchedAt = channel.lastWatchedAt
        )
    }
}
