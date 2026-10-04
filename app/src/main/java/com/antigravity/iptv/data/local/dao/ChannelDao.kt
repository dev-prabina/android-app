package com.antigravity.iptv.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.antigravity.iptv.data.local.entity.ChannelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {

    @Query("SELECT * FROM channels ORDER BY orderIndex ASC")
    fun getAllChannels(): Flow<List<ChannelEntity>>

    @Query("""
        SELECT c.* FROM channels c 
        INNER JOIN playlists p ON c.playlistId = p.id 
        WHERE p.isActive = 1 
        ORDER BY c.orderIndex ASC
    """)
    fun getActiveChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId ORDER BY orderIndex ASC")
    fun getChannelsByPlaylist(playlistId: Long): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE isFavorite = 1 ORDER BY lastWatchedAt DESC, name ASC")
    fun getFavoriteChannels(): Flow<List<ChannelEntity>>

    @Query("""
        SELECT c.* FROM channels c 
        INNER JOIN playlists p ON c.playlistId = p.id 
        WHERE p.isActive = 1 AND c.groupTitle = :category 
        ORDER BY c.name ASC
    """)
    fun getChannelsByCategory(category: String): Flow<List<ChannelEntity>>

    @Query("""
        SELECT c.* FROM channels c 
        INNER JOIN playlists p ON c.playlistId = p.id 
        WHERE p.isActive = 1 AND c.language = :language 
        ORDER BY c.name ASC
    """)
    fun getChannelsByLanguage(language: String): Flow<List<ChannelEntity>>

    @Query("""
        SELECT DISTINCT c.groupTitle FROM channels c 
        INNER JOIN playlists p ON c.playlistId = p.id 
        WHERE p.isActive = 1 AND c.groupTitle != '' 
        ORDER BY c.groupTitle ASC
    """)
    fun getCategories(): Flow<List<String>>

    @Query("""
        SELECT DISTINCT c.language FROM channels c 
        INNER JOIN playlists p ON c.playlistId = p.id 
        WHERE p.isActive = 1 AND c.language != '' 
        ORDER BY c.language ASC
    """)
    fun getLanguages(): Flow<List<String>>

    @Query("""
        SELECT c.* FROM channels c 
        INNER JOIN playlists p ON c.playlistId = p.id 
        WHERE p.isActive = 1 AND (
            c.name LIKE '%' || :query || '%' OR 
            c.tvgName LIKE '%' || :query || '%' OR 
            c.groupTitle LIKE '%' || :query || '%' OR 
            c.language LIKE '%' || :query || '%' OR
            c.country LIKE '%' || :query || '%'
        )
        ORDER BY 
            CASE 
                WHEN c.name LIKE :query || '%' THEN 1 
                WHEN c.name LIKE '%' || :query || '%' THEN 2 
                ELSE 3 
            END,
            c.name ASC
    """)
    fun searchChannels(query: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE id = :channelId LIMIT 1")
    fun getChannelById(channelId: Long): ChannelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertChannels(channels: List<ChannelEntity>): List<Long>

    @Query("DELETE FROM channels WHERE playlistId = :playlistId")
    fun deleteChannelsByPlaylist(playlistId: Long): Int

    @Query("UPDATE channels SET isFavorite = :isFavorite WHERE id = :channelId")
    fun setFavorite(channelId: Long, isFavorite: Boolean): Int

    @Query("UPDATE channels SET lastWatchedAt = :timestamp WHERE id = :channelId")
    fun updateLastWatched(channelId: Long, timestamp: Long): Int

    @Query("SELECT COUNT(*) FROM channels WHERE playlistId = :playlistId")
    fun getChannelCount(playlistId: Long): Int
}
