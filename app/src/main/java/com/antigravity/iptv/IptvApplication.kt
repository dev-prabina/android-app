package com.antigravity.iptv

import android.app.Application
import androidx.media3.common.util.UnstableApi
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.antigravity.iptv.data.local.IptvDatabase
import com.antigravity.iptv.data.remote.PlaylistDownloader
import com.antigravity.iptv.data.repository.ChannelRepositoryImpl
import com.antigravity.iptv.data.repository.PlaylistRepositoryImpl
import com.antigravity.iptv.data.repository.SettingsRepositoryImpl
import com.antigravity.iptv.domain.repository.ChannelRepository
import com.antigravity.iptv.domain.repository.PlaylistRepository
import com.antigravity.iptv.domain.repository.SettingsRepository
import com.antigravity.iptv.player.PlayerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@UnstableApi
class IptvApplication : Application(), ImageLoaderFactory {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: IptvDatabase
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var channelRepository: ChannelRepository
        private set

    lateinit var playlistRepository: PlaylistRepository
        private set

    lateinit var playerManager: PlayerManager
        private set

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("channel_logo_cache"))
                    .maxSizeBytes(64L * 1024L * 1024L) // 64 MB
                    .build()
            }
            .crossfade(true)
            .respectCacheHeaders(false)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = IptvDatabase.getInstance(this)
        settingsRepository = SettingsRepositoryImpl(this)
        val playlistDownloader = PlaylistDownloader()
        val favoriteDao = database.favoriteDao()

        channelRepository = ChannelRepositoryImpl(
            channelDao = database.channelDao(),
            watchHistoryDao = database.watchHistoryDao(),
            settingsRepository = settingsRepository,
            favoriteDao = favoriteDao
        )

        playlistRepository = PlaylistRepositoryImpl(
            playlistDao = database.playlistDao(),
            channelDao = database.channelDao(),
            playlistDownloader = playlistDownloader,
            favoriteDao = favoriteDao
        )

        playerManager = PlayerManager(this)

        // Automatically ensure default playlist is registered and synced on first launch
        applicationScope.launch {
            playlistRepository.ensureDefaultPlaylist()
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        playerManager.release()
    }

    companion object {
        lateinit var instance: IptvApplication
            private set
    }
}
