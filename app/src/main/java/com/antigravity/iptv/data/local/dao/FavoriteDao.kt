package com.antigravity.iptv.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.antigravity.iptv.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getAllFavoritesSync(): List<FavoriteEntity>

    @Query("SELECT channelKey FROM favorites")
    fun getAllFavoriteKeys(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE channelKey = :key LIMIT 1)")
    fun isFavorite(key: String): Boolean

    @Query("SELECT * FROM favorites WHERE channelKey = :key LIMIT 1")
    fun getFavoriteByKey(key: String): FavoriteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFavorite(favorite: FavoriteEntity): Long

    @Query("DELETE FROM favorites WHERE channelKey = :key")
    fun deleteFavoriteByKey(key: String): Int

    @Query("DELETE FROM favorites WHERE streamUrl = :streamUrl")
    fun deleteFavoriteByStreamUrl(streamUrl: String): Int

    @Query("SELECT COUNT(*) FROM favorites")
    fun getFavoriteCount(): Int
}
