package com.antigravity.iptv.player

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultAllocator
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import com.antigravity.iptv.domain.model.AspectRatioMode
import com.antigravity.iptv.domain.model.Channel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

enum class PlaybackStatus {
    IDLE,
    CONNECTING,
    BUFFERING,
    RECOVERING,
    READY,
    ENDED,
    ERROR;

    val isBufferingOrConnecting: Boolean
        get() = this == BUFFERING || this == CONNECTING || this == RECOVERING
}

data class TrackInfo(
    val id: String,
    val name: String,
    val language: String? = null,
    val isSelected: Boolean = false,
    val trackGroup: TrackGroup,
    val trackIndex: Int
)

data class PlaybackDiagnostics(
    val channelName: String = "",
    val startupDurationMs: Long = 0L,
    val totalBufferingDurationMs: Long = 0L,
    val bufferedDurationMs: Long = 0L,
    val rebufferCount: Int = 0,
    val lastEstimatedBandwidth: Long = 0L,
    val currentResolution: String = "",
    val currentBitrate: Int = 0,
    val liveOffsetMs: Long = 0L,
    val isLive: Boolean = false,
    val isNetworkAvailable: Boolean = true,
    val recoveryAttempts: Int = 0
)

@UnstableApi
class PlayerManager(
    private val context: Context,
    private val defaultUserAgent: String = "VLC/3.0.18 LibVLC/3.0.18"
) {

    companion object {
        private const val TAG = "PlayerManager"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_BASE_DELAY_MS = 1500L

        // Buffering & Tuning Parameters
        private const val MIN_BUFFER_MS = 15_000          // 15 seconds (holds 2-3 full HLS segments)
        private const val MAX_BUFFER_MS = 35_000          // 35 seconds (bounds RAM, absorbs server jitter)
        private const val BUFFER_FOR_PLAYBACK_MS = 1_000  // 1.0 second (instant startup)
        private const val BUFFER_FOR_REBUFFER_MS = 2_500  // 2.5 seconds (prevents stutter looping)
        private const val BACK_BUFFER_DURATION_MS = 5_000 // 5 seconds back-buffer for seamless micro-seeks

        // Live Edge Constraints
        private const val TARGET_LIVE_OFFSET_MS = 4_000L  // 4s target live offset
        private const val MIN_LIVE_OFFSET_MS = 2_000L     // 2s minimum live offset
        private const val MAX_LIVE_OFFSET_MS = 20_000L    // 20s maximum live offset
        private const val MIN_PLAYBACK_SPEED = 0.97f      // Gentle slowdown to build buffer
        private const val MAX_PLAYBACK_SPEED = 1.03f      // Gentle speedup to catch live edge
    }

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private var retryJob: Job? = null
    private var retryCount = 0

    // Watchdog & Quality Recovery
    private var rebufferWatchdogJob: Job? = null
    private var stablePlaybackJob: Job? = null
    private var channelLoadStartTimeMs = 0L
    private var isInitialConnecting = true
    private var rebufferCountForCurrentChannel = 0
    private var isAdaptiveBitrateConstrained = false
    private var lastBitrateConstraintTimeMs = 0L

    // Network Connectivity Monitor
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var isNetworkAvailable = true

    // Bandwidth meter for real-time throughput estimation
    val bandwidthMeter = DefaultBandwidthMeter.Builder(context).build()

    // Tuned Track Selector
    val trackSelector = DefaultTrackSelector(context).apply {
        parameters = buildUponParameters()
            .setExceedRendererCapabilitiesIfNecessary(true)
            .setAllowVideoMixedMimeTypeAdaptiveness(true)
            .setAllowVideoNonSeamlessAdaptiveness(true)
            .setAllowMultipleAdaptiveSelections(true)
            .build()
    }

    // High performance OkHttpClient with connection pooling & fast DNS/TLS reuse
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectionPool(ConnectionPool(10, 60L, TimeUnit.SECONDS))
        .connectTimeout(8000L, TimeUnit.MILLISECONDS)
        .readTimeout(15000L, TimeUnit.MILLISECONDS)
        .writeTimeout(10000L, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(true)
        .followRedirects(true)
        .followSslRedirects(true)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", defaultUserAgent)
                .header("Accept", "*/*")
                .header("Connection", "keep-alive")
                .build()
            chain.proceed(req)
        }
        .build()

    private val httpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
        .setUserAgent(defaultUserAgent)

    private val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

    // Controlled LoadErrorHandlingPolicy with exponential backoff & fast IPTV retry
    private val loadErrorHandlingPolicy = object : DefaultLoadErrorHandlingPolicy(3) {
        override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
            val count = loadErrorInfo.errorCount
            return if (count <= 3) {
                (800L * (1 shl (count - 1))).coerceAtMost(4000L)
            } else {
                C.TIME_UNSET
            }
        }
    }

    // Explicit HLS MediaSource Factory with chunkless preparation for fast start
    private val hlsMediaSourceFactory = HlsMediaSource.Factory(dataSourceFactory)
        .setAllowChunklessPreparation(true)
        .setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)

    private val defaultMediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
        .setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)

    // Professional DefaultLoadControl tuned for IPTV live streaming
    private val allocator = DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE)
    private val loadControl = DefaultLoadControl.Builder()
        .setAllocator(allocator)
        .setBufferDurationsMs(
            MIN_BUFFER_MS,
            MAX_BUFFER_MS,
            BUFFER_FOR_PLAYBACK_MS,
            BUFFER_FOR_REBUFFER_MS
        )
        .setTargetBufferBytes(C.LENGTH_UNSET)
        .setPrioritizeTimeOverSizeThresholds(true)
        .setBackBuffer(BACK_BUFFER_DURATION_MS, true)
        .build()

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setRenderersFactory(
            DefaultRenderersFactory(context)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
        )
        .setTrackSelector(trackSelector)
        .setLoadControl(loadControl)
        .setBandwidthMeter(bandwidthMeter)
        .setMediaSourceFactory(defaultMediaSourceFactory)
        .build()

    // State flows for UI
    private val _playbackStatus = MutableStateFlow(PlaybackStatus.IDLE)
    val playbackStatus: StateFlow<PlaybackStatus> = _playbackStatus.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentChannel = MutableStateFlow<Channel?>(null)
    val currentChannel: StateFlow<Channel?> = _currentChannel.asStateFlow()

    private val _channelQueue = MutableStateFlow<List<Channel>>(emptyList())
    val channelQueue: StateFlow<List<Channel>> = _channelQueue.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _videoSize = MutableStateFlow(Pair(0, 0))
    val videoSize: StateFlow<Pair<Int, Int>> = _videoSize.asStateFlow()

    private val _aspectRatio = MutableStateFlow(AspectRatioMode.FIT)
    val aspectRatio: StateFlow<AspectRatioMode> = _aspectRatio.asStateFlow()

    private val _audioTracks = MutableStateFlow<List<TrackInfo>>(emptyList())
    val audioTracks: StateFlow<List<TrackInfo>> = _audioTracks.asStateFlow()

    private val _subtitleTracks = MutableStateFlow<List<TrackInfo>>(emptyList())
    val subtitleTracks: StateFlow<List<TrackInfo>> = _subtitleTracks.asStateFlow()

    private val _videoQualities = MutableStateFlow<List<TrackInfo>>(emptyList())
    val videoQualities: StateFlow<List<TrackInfo>> = _videoQualities.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _diagnostics = MutableStateFlow(PlaybackDiagnostics())
    val diagnostics: StateFlow<PlaybackDiagnostics> = _diagnostics.asStateFlow()

    private var previousVolume = 1.0f

    init {
        registerNetworkCallback()

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_IDLE -> {
                        if (_playbackStatus.value != PlaybackStatus.ERROR) {
                            _playbackStatus.value = PlaybackStatus.IDLE
                        }
                    }

                    Player.STATE_BUFFERING -> {
                        if (isInitialConnecting) {
                            _playbackStatus.value = PlaybackStatus.CONNECTING
                        } else {
                            _playbackStatus.value = PlaybackStatus.BUFFERING
                            rebufferCountForCurrentChannel++
                            updateDiagnostics()

                            // If rebuffering happens repeatedly, immediately step down ABR to protect playback
                            if (rebufferCountForCurrentChannel >= 2) {
                                applyLowBitrateConstraint()
                            }
                        }
                        startBufferingWatchdog()
                    }

                    Player.STATE_READY -> {
                        val wasConnecting = isInitialConnecting
                        isInitialConnecting = false
                        _playbackStatus.value = PlaybackStatus.READY
                        _errorMessage.value = null
                        retryCount = 0
                        cancelBufferingWatchdog()

                        if (wasConnecting && channelLoadStartTimeMs > 0L) {
                            val startupDuration = System.currentTimeMillis() - channelLoadStartTimeMs
                            _diagnostics.value = _diagnostics.value.copy(
                                startupDurationMs = startupDuration
                            )
                            Log.i(TAG, "Channel started successfully in ${startupDuration}ms")
                        }

                        updateDiagnostics()
                        startStablePlaybackTimer()
                    }

                    Player.STATE_ENDED -> {
                        cancelBufferingWatchdog()
                        _playbackStatus.value = PlaybackStatus.ENDED
                    }
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
                updateDiagnostics()
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "ExoPlayer error occurred: ${error.errorCodeName} (${error.errorCode})", error)
                cancelBufferingWatchdog()

                // 1. Behind Live Window Recovery: seek to live edge and re-prepare without full reset
                if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW ||
                    error.cause?.javaClass?.simpleName == "BehindLiveWindowException"
                ) {
                    Log.w(TAG, "BehindLiveWindowException detected! Seeking to live edge...")
                    _playbackStatus.value = PlaybackStatus.RECOVERING
                    applyLowBitrateConstraint()
                    exoPlayer.seekToDefaultPosition()
                    exoPlayer.prepare()
                    return
                }

                // 2. Malformed manifest fallback attempt
                if (error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ||
                    error.errorCode == PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED
                ) {
                    val current = _currentChannel.value
                    if (current != null && retryCount == 0) {
                        retryCount++
                        Log.w(TAG, "Manifest error on initial parse. Retrying with direct MPEG-TS container...")
                        _playbackStatus.value = PlaybackStatus.RECOVERING
                        val fallbackItem = MediaItem.Builder()
                            .setUri(Uri.parse(current.streamUrl))
                            .setMediaId(current.id.toString())
                            .setMimeType(MimeTypes.VIDEO_MP2T)
                            .build()
                        exoPlayer.setMediaItem(fallbackItem)
                        exoPlayer.prepare()
                        return
                    }
                }

                _playbackStatus.value = PlaybackStatus.ERROR
                _errorMessage.value = formatErrorMessage(error)
                handleAutoRetry(error)
            }

            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                _videoSize.value = Pair(videoSize.width, videoSize.height)
                _diagnostics.value = _diagnostics.value.copy(
                    currentResolution = "${videoSize.width}x${videoSize.height}"
                )
            }

            override fun onTracksChanged(tracks: Tracks) {
                extractTracks(tracks)
                updateDiagnostics()
            }
        })
    }

    private fun detectMimeType(url: String): String? {
        val cleanUrl = url.substringBefore('?').substringBefore('#').lowercase()
        return when {
            cleanUrl.endsWith(".m3u8") || cleanUrl.contains("/hls/") || cleanUrl.contains(".m3u8") -> MimeTypes.APPLICATION_M3U8
            cleanUrl.endsWith(".mpd") -> MimeTypes.APPLICATION_MPD
            cleanUrl.endsWith(".ism") || cleanUrl.endsWith(".isml") -> MimeTypes.APPLICATION_SS
            cleanUrl.endsWith(".ts") -> MimeTypes.VIDEO_MP2T
            cleanUrl.endsWith(".mp4") -> MimeTypes.APPLICATION_MP4
            cleanUrl.endsWith(".mkv") -> MimeTypes.VIDEO_MATROSKA
            else -> MimeTypes.APPLICATION_M3U8
        }
    }

    private fun buildMediaItem(channel: Channel): MediaItem {
        val uri = Uri.parse(channel.streamUrl)
        val mimeType = detectMimeType(channel.streamUrl)

        val liveConfig = MediaItem.LiveConfiguration.Builder()
            .setTargetOffsetMs(TARGET_LIVE_OFFSET_MS)
            .setMinOffsetMs(MIN_LIVE_OFFSET_MS)
            .setMaxOffsetMs(MAX_LIVE_OFFSET_MS)
            .setMinPlaybackSpeed(MIN_PLAYBACK_SPEED)
            .setMaxPlaybackSpeed(MAX_PLAYBACK_SPEED)
            .build()

        return MediaItem.Builder()
            .setUri(uri)
            .setMediaId(channel.id.toString())
            .apply {
                if (mimeType != null) {
                    setMimeType(mimeType)
                }
            }
            .setLiveConfiguration(liveConfig)
            .build()
    }

    private fun createMediaSource(mediaItem: MediaItem): MediaSource {
        val isHls = mediaItem.localConfiguration?.mimeType == MimeTypes.APPLICATION_M3U8 ||
                mediaItem.localConfiguration?.uri?.toString()?.contains(".m3u8", ignoreCase = true) == true
        return if (isHls) {
            hlsMediaSourceFactory.createMediaSource(mediaItem)
        } else {
            defaultMediaSourceFactory.createMediaSource(mediaItem)
        }
    }

    fun playChannel(channel: Channel, queue: List<Channel> = emptyList()) {
        playChannelInternal(channel, queue, isRetry = false)
    }

    private fun playChannelInternal(channel: Channel, queue: List<Channel> = emptyList(), isRetry: Boolean = false) {
        retryJob?.cancel()
        cancelBufferingWatchdog()
        stablePlaybackJob?.cancel()

        if (!isRetry) {
            retryCount = 0
            rebufferCountForCurrentChannel = 0
            isAdaptiveBitrateConstrained = false
            channelLoadStartTimeMs = System.currentTimeMillis()
        }

        _currentChannel.value = channel
        if (queue.isNotEmpty()) {
            _channelQueue.value = queue
        }

        _errorMessage.value = null
        isInitialConnecting = !isRetry
        _playbackStatus.value = if (isRetry) PlaybackStatus.RECOVERING else PlaybackStatus.CONNECTING

        val mediaItem = buildMediaItem(channel)
        val mediaSource = createMediaSource(mediaItem)

        // Clean previous channel cleanly without UI blocking
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true

        updateDiagnostics()
    }

    // Active Watchdog that prevents indefinite buffering on live streams
    private fun startBufferingWatchdog() {
        rebufferWatchdogJob?.cancel()
        rebufferWatchdogJob = scope.launch {
            var elapsedSeconds = 0
            while (true) {
                delay(1000L)
                elapsedSeconds++

                if (_playbackStatus.value == PlaybackStatus.READY || _playbackStatus.value == PlaybackStatus.ERROR) {
                    break
                }

                val isLive = exoPlayer.isCurrentMediaItemLive
                val bufferedDuration = (exoPlayer.bufferedPosition - exoPlayer.currentPosition).coerceAtLeast(0L)
                val currentOffset = if (isLive) exoPlayer.currentLiveOffset else -1L

                Log.d(TAG, "Watchdog: Buffering ${elapsedSeconds}s (live=$isLive, buffered=${bufferedDuration}ms, offset=${currentOffset}ms)")

                // Check connectivity
                if (!isNetworkConnected()) {
                    _playbackStatus.value = PlaybackStatus.RECOVERING
                    _errorMessage.value = "Waiting for internet connection..."
                    continue
                }

                // Stage 1 (3 seconds): Live drift / buffer starvation recovery
                if (elapsedSeconds == 3 && isLive) {
                    Log.w(TAG, "Watchdog: 3s live rebuffer. Applying ABR constraint and seeking to live edge...")
                    applyLowBitrateConstraint()
                    if (currentOffset > 20_000L || currentOffset <= 0L || bufferedDuration <= 0L) {
                        _playbackStatus.value = PlaybackStatus.RECOVERING
                        exoPlayer.seekToDefaultPosition()
                    }
                }

                // Stage 2 (7 seconds): Soft re-prepare
                if (elapsedSeconds == 7) {
                    Log.w(TAG, "Watchdog: 7s rebuffer. Executing soft re-prepare...")
                    _playbackStatus.value = PlaybackStatus.RECOVERING
                    if (isLive) {
                        exoPlayer.seekToDefaultPosition()
                    }
                    exoPlayer.prepare()
                }

                // Stage 3 (12 seconds): Timeout unresponsive server
                if (elapsedSeconds >= 12) {
                    Log.e(TAG, "Watchdog: Stream server unresponsive after 12s.")
                    _playbackStatus.value = PlaybackStatus.ERROR
                    _errorMessage.value = "The live stream server is not responding. Please retry or choose another channel."
                    break
                }
            }
        }
    }

    private fun cancelBufferingWatchdog() {
        rebufferWatchdogJob?.cancel()
        rebufferWatchdogJob = null
    }

    // Adaptive Bitrate Protection: Clamp to 720p/2Mbps on unstable connections to prevent stalling
    private fun applyLowBitrateConstraint() {
        if (isAdaptiveBitrateConstrained) return
        isAdaptiveBitrateConstrained = true
        lastBitrateConstraintTimeMs = System.currentTimeMillis()
        Log.w(TAG, "Applying ABR constraint: prioritizing smooth 720p/SD playback over max bitrate")
        trackSelector.parameters = trackSelector.parameters
            .buildUpon()
            .setMaxVideoBitrate(2_200_000)
            .setMaxVideoSize(1280, 720)
            .build()
    }

    private fun startStablePlaybackTimer() {
        stablePlaybackJob?.cancel()
        stablePlaybackJob = scope.launch {
            // Require 30 seconds of stable uninterrupted playback before releasing bitrate constraint
            delay(30_000L)
            if (isAdaptiveBitrateConstrained && _playbackStatus.value == PlaybackStatus.READY) {
                isAdaptiveBitrateConstrained = false
                Log.i(TAG, "Playback stable for 30s. Releasing ABR bitrate constraints back to full quality.")
                trackSelector.parameters = trackSelector.parameters
                    .buildUpon()
                    .clearVideoSizeConstraints()
                    .setMaxVideoBitrate(Int.MAX_VALUE)
                    .build()
            }
        }
    }

    private fun handleAutoRetry(error: PlaybackException) {
        if (retryCount < MAX_RETRY_ATTEMPTS) {
            retryCount++
            val backoffDelay = (RETRY_BASE_DELAY_MS * (1 shl (retryCount - 1))).coerceAtMost(6000L)
            Log.i(TAG, "Scheduling retry attempt $retryCount of $MAX_RETRY_ATTEMPTS in ${backoffDelay}ms...")
            _playbackStatus.value = PlaybackStatus.RECOVERING
            _diagnostics.value = _diagnostics.value.copy(recoveryAttempts = retryCount)

            retryJob?.cancel()
            retryJob = scope.launch {
                delay(backoffDelay)
                if (!isNetworkConnected()) {
                    Log.w(TAG, "Network disconnected during retry. Waiting for connection...")
                    _errorMessage.value = "Waiting for internet connection..."
                    return@launch
                }
                Log.i(TAG, "Executing retry attempt $retryCount")
                val channel = _currentChannel.value ?: return@launch
                playChannelInternal(channel, _channelQueue.value, isRetry = true)
            }
        } else {
            Log.e(TAG, "Max retries reached for channel ${_currentChannel.value?.name}")
            _playbackStatus.value = PlaybackStatus.ERROR
        }
    }

    // Network Callback Registration
    private fun registerNetworkCallback() {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    val wasOffline = !isNetworkAvailable
                    isNetworkAvailable = true
                    _diagnostics.value = _diagnostics.value.copy(isNetworkAvailable = true)
                    Log.i(TAG, "Network available (wasOffline=$wasOffline)")
                    if (wasOffline) {
                        scope.launch {
                            if (_playbackStatus.value == PlaybackStatus.ERROR ||
                                _playbackStatus.value == PlaybackStatus.RECOVERING
                            ) {
                                Log.i(TAG, "Network restored. Automatically resuming playback...")
                                retryCurrentStream()
                            }
                        }
                    }
                }

                override fun onLost(network: Network) {
                    isNetworkAvailable = isNetworkConnected()
                    _diagnostics.value = _diagnostics.value.copy(isNetworkAvailable = isNetworkAvailable)
                    Log.w(TAG, "Network lost. Active internet: $isNetworkAvailable")
                    if (!isNetworkAvailable) {
                        scope.launch {
                            if (exoPlayer.isPlaying || _playbackStatus.value.isBufferingOrConnecting) {
                                _playbackStatus.value = PlaybackStatus.RECOVERING
                                _errorMessage.value = "Internet connection lost. Waiting to reconnect..."
                            }
                        }
                    }
                }
            }
            connectivityManager?.registerNetworkCallback(request, networkCallback!!)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to register network callback", e)
        }
    }

    private fun isNetworkConnected(): Boolean {
        val cm = connectivityManager ?: return true
        val activeNet = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNet) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun updateDiagnostics() {
        val channel = _currentChannel.value
        val isLive = exoPlayer.isCurrentMediaItemLive
        val liveOffset = if (isLive) exoPlayer.currentLiveOffset else 0L
        val buffered = (exoPlayer.bufferedPosition - exoPlayer.currentPosition).coerceAtLeast(0L)

        _diagnostics.value = _diagnostics.value.copy(
            channelName = channel?.name ?: "",
            lastEstimatedBandwidth = bandwidthMeter.bitrateEstimate,
            isLive = isLive,
            liveOffsetMs = liveOffset,
            bufferedDurationMs = buffered,
            rebufferCount = rebufferCountForCurrentChannel,
            isNetworkAvailable = isNetworkAvailable
        )
    }

    fun playNextChannel() {
        val queue = _channelQueue.value
        val current = _currentChannel.value ?: return
        if (queue.isEmpty()) return

        val currentIndex = queue.indexOfFirst { it.id == current.id }
        if (currentIndex != -1 && currentIndex < queue.size - 1) {
            playChannel(queue[currentIndex + 1], queue)
        } else if (queue.isNotEmpty()) {
            playChannel(queue.first(), queue)
        }
    }

    fun playPreviousChannel() {
        val queue = _channelQueue.value
        val current = _currentChannel.value ?: return
        if (queue.isEmpty()) return

        val currentIndex = queue.indexOfFirst { it.id == current.id }
        if (currentIndex > 0) {
            playChannel(queue[currentIndex - 1], queue)
        } else if (queue.isNotEmpty()) {
            playChannel(queue.last(), queue)
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (_playbackStatus.value == PlaybackStatus.ERROR) {
                retryCurrentStream()
            } else {
                exoPlayer.play()
            }
        }
    }

    fun retryCurrentStream() {
        val channel = _currentChannel.value ?: return
        playChannelInternal(channel, _channelQueue.value, isRetry = true)
    }

    fun setAspectRatio(mode: AspectRatioMode) {
        _aspectRatio.value = mode
    }

    fun cycleAspectRatio() {
        val modes = AspectRatioMode.values()
        val nextIndex = (_aspectRatio.value.ordinal + 1) % modes.size
        _aspectRatio.value = modes[nextIndex]
    }

    fun toggleMute() {
        if (_isMuted.value) {
            exoPlayer.volume = previousVolume
            _isMuted.value = false
        } else {
            previousVolume = exoPlayer.volume
            exoPlayer.volume = 0f
            _isMuted.value = true
        }
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        exoPlayer.volume = clamped
        _isMuted.value = clamped == 0f
    }

    fun selectTrack(trackInfo: TrackInfo, trackType: @C.TrackType Int) {
        val override = TrackSelectionOverride(trackInfo.trackGroup, trackInfo.trackIndex)
        trackSelector.parameters = trackSelector.parameters
            .buildUpon()
            .setTrackTypeDisabled(trackType, false)
            .setOverrideForType(override)
            .build()
    }

    fun disableTrackType(trackType: @C.TrackType Int) {
        trackSelector.parameters = trackSelector.parameters
            .buildUpon()
            .setTrackTypeDisabled(trackType, true)
            .build()
    }

    fun clearTrackOverride(trackType: @C.TrackType Int) {
        trackSelector.parameters = trackSelector.parameters
            .buildUpon()
            .setTrackTypeDisabled(trackType, false)
            .clearOverridesOfType(trackType)
            .build()
    }

    private fun extractTracks(tracks: Tracks) {
        val audioList = mutableListOf<TrackInfo>()
        val subList = mutableListOf<TrackInfo>()
        val videoList = mutableListOf<TrackInfo>()

        for (group in tracks.groups) {
            val trackGroup = group.mediaTrackGroup
            val trackType = group.type

            for (i in 0 until group.length) {
                val format = trackGroup.getFormat(i)
                val isSelected = group.isTrackSelected(i)

                val info = TrackInfo(
                    id = format.id ?: "$i",
                    name = format.label ?: format.language ?: "Track ${i + 1}",
                    language = format.language,
                    isSelected = isSelected,
                    trackGroup = trackGroup,
                    trackIndex = i
                )

                when (trackType) {
                    C.TRACK_TYPE_AUDIO -> audioList.add(info)
                    C.TRACK_TYPE_TEXT -> subList.add(info)
                    C.TRACK_TYPE_VIDEO -> {
                        val resolution = if (format.width > 0 && format.height > 0) {
                            "${format.width}x${format.height} (${format.height}p)"
                        } else {
                            "Auto"
                        }
                        if (isSelected && format.bitrate > 0) {
                            _diagnostics.value = _diagnostics.value.copy(currentBitrate = format.bitrate)
                        }
                        videoList.add(info.copy(name = resolution))
                    }
                }
            }
        }

        _audioTracks.value = audioList
        _subtitleTracks.value = subList
        _videoQualities.value = videoList
    }

    private fun formatErrorMessage(error: PlaybackException): String {
        return when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                "Network connection timed out. Please check your internet connection."

            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                "Stream server returned an error (HTTP ${error.message}). The channel might be offline."

            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED ->
                "Stream manifest format is unsupported or malformed."

            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ->
                "Device video decoder failed to initialize for this stream format."

            PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW ->
                "Fell behind the live broadcast window. Re-syncing stream..."

            else ->
                "Unable to play this live stream. The broadcast source may be offline."
        }
    }

    fun release() {
        retryJob?.cancel()
        cancelBufferingWatchdog()
        stablePlaybackJob?.cancel()
        try {
            networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering network callback", e)
        }
        exoPlayer.release()
    }
}
