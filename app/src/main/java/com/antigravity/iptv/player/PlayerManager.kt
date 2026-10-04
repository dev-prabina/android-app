package com.antigravity.iptv.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
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

enum class PlaybackStatus {
    IDLE,
    BUFFERING,
    READY,
    ENDED,
    ERROR
}

data class TrackInfo(
    val id: String,
    val name: String,
    val language: String? = null,
    val isSelected: Boolean = false,
    val trackGroup: TrackGroup,
    val trackIndex: Int
)

@UnstableApi
class PlayerManager(
    private val context: Context,
    private val defaultUserAgent: String = "VLC/3.0.18 LibVLC/3.0.18"
) {

    companion object {
        private const val TAG = "PlayerManager"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 2500L
    }

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private var retryJob: Job? = null
    private var retryCount = 0

    val trackSelector = DefaultTrackSelector(context)

    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent(defaultUserAgent)
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(15000)
        .setReadTimeoutMs(25000)

    private val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

    private val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            2000,   // Min buffer ms (fast start)
            8000,   // Max buffer ms
            1000,   // Buffer for playback ms
            1500    // Buffer for playback after rebuffer ms
        )
        .build()

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setRenderersFactory(
            DefaultRenderersFactory(context)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
        )
        .setTrackSelector(trackSelector)
        .setLoadControl(loadControl)
        .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
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

    private var previousVolume = 1.0f

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_IDLE -> {
                        if (_playbackStatus.value != PlaybackStatus.ERROR) {
                            _playbackStatus.value = PlaybackStatus.IDLE
                        }
                    }
                    Player.STATE_BUFFERING -> {
                        _playbackStatus.value = PlaybackStatus.BUFFERING
                    }
                    Player.STATE_READY -> {
                        _playbackStatus.value = PlaybackStatus.READY
                        _errorMessage.value = null
                        retryCount = 0
                    }
                    Player.STATE_ENDED -> {
                        _playbackStatus.value = PlaybackStatus.ENDED
                    }
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "ExoPlayer error occurred: ${error.errorCodeName}", error)
                _playbackStatus.value = PlaybackStatus.ERROR
                _errorMessage.value = formatErrorMessage(error)

                handleAutoRetry()
            }

            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                _videoSize.value = Pair(videoSize.width, videoSize.height)
            }

            override fun onTracksChanged(tracks: Tracks) {
                extractTracks(tracks)
            }
        })
    }

    fun playChannel(channel: Channel, queue: List<Channel> = emptyList()) {
        retryJob?.cancel()
        retryCount = 0
        _currentChannel.value = channel
        if (queue.isNotEmpty()) {
            _channelQueue.value = queue
        }

        _errorMessage.value = null
        _playbackStatus.value = PlaybackStatus.BUFFERING

        val uri = Uri.parse(channel.streamUrl)
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMediaId(channel.id.toString())
            .build()

        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    fun playNextChannel() {
        val queue = _channelQueue.value
        val current = _currentChannel.value ?: return
        if (queue.isEmpty()) return

        val currentIndex = queue.indexOfFirst { it.id == current.id }
        if (currentIndex != -1 && currentIndex < queue.size - 1) {
            playChannel(queue[currentIndex + 1], queue)
        } else if (queue.isNotEmpty()) {
            // Loop back to start
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
            // Loop to last
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
        playChannel(channel, _channelQueue.value)
    }

    private fun handleAutoRetry() {
        if (retryCount < MAX_RETRY_ATTEMPTS) {
            retryCount++
            Log.i(TAG, "Scheduling retry attempt $retryCount of $MAX_RETRY_ATTEMPTS...")
            retryJob?.cancel()
            retryJob = scope.launch {
                delay(RETRY_DELAY_MS)
                Log.i(TAG, "Executing retry attempt $retryCount")
                retryCurrentStream()
            }
        }
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

            else ->
                "Unable to play this live stream. The broadcast source may be offline."
        }
    }

    fun release() {
        retryJob?.cancel()
        exoPlayer.release()
    }
}
