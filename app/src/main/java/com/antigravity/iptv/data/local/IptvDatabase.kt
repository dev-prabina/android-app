package com.antigravity.iptv.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.antigravity.iptv.data.local.dao.ChannelDao
import com.antigravity.iptv.data.local.dao.FavoriteDao
import com.antigravity.iptv.data.local.dao.PlaylistDao
import com.antigravity.iptv.data.local.dao.WatchHistoryDao
import com.antigravity.iptv.data.local.entity.ChannelEntity
import com.antigravity.iptv.data.local.entity.FavoriteEntity
import com.antigravity.iptv.data.local.entity.PlaylistEntity
import com.antigravity.iptv.data.local.entity.WatchHistoryEntity

@Database(
    entities = [
        ChannelEntity::class,
        PlaylistEntity::class,
        WatchHistoryEntity::class,
        FavoriteEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class IptvDatabase : RoomDatabase() {

    abstract fun channelDao(): ChannelDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        @Volatile
        private var INSTANCE: IptvDatabase? = null

        fun getInstance(context: Context): IptvDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    IptvDatabase::class.java,
                    "iptv_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
