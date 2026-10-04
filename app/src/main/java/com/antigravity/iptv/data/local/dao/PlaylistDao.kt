package com.antigravity.iptv.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.antigravity.iptv.data.local.entity.PlaylistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY isDefault DESC, id ASC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    fun getPlaylistById(id: Long): PlaylistEntity?

    @Query("SELECT * FROM playlists WHERE isDefault = 1 LIMIT 1")
    fun getDefaultPlaylist(): PlaylistEntity?

    @Query("SELECT * FROM playlists WHERE url = :url LIMIT 1")
    fun getPlaylistByUrl(url: String): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    fun updatePlaylist(playlist: PlaylistEntity): Int

    @Delete
    fun deletePlaylist(playlist: PlaylistEntity): Int

    @Query("""
        UPDATE playlists 
        SET syncStatus = :status, 
            channelCount = :channelCount, 
            lastUpdated = :timestamp, 
            errorMessage = :errorMessage 
        WHERE id = :id
    """)
    fun updateSyncStatus(
        id: Long,
        status: String,
        channelCount: Int,
        timestamp: Long,
        errorMessage: String?
    ): Int

    @Query("UPDATE playlists SET isActive = :isActive WHERE id = :id")
    fun setPlaylistActive(id: Long, isActive: Boolean): Int
}
