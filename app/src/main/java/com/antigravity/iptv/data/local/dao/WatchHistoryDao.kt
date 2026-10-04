package com.antigravity.iptv.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.antigravity.iptv.data.local.entity.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchHistoryDao {

    @Query("SELECT * FROM watch_history ORDER BY watchedAt DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 30): Flow<List<WatchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertHistory(item: WatchHistoryEntity): Long

    @Query("DELETE FROM watch_history WHERE id = :id")
    fun deleteHistoryItem(id: Long): Int

    @Query("DELETE FROM watch_history")
    fun clearHistory(): Int
}
