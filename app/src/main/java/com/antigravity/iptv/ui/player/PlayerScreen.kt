package com.antigravity.iptv.ui.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Build
import android.util.Rational
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.antigravity.iptv.domain.model.AspectRatioMode
import com.antigravity.iptv.domain.model.Channel
import com.antigravity.iptv.player.PlaybackStatus
import com.antigravity.iptv.player.TrackInfo
import com.antigravity.iptv.ui.components.ChannelPlaceholder
import com.antigravity.iptv.ui.theme.LiveBadgeColor
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    channelId: Long,
    viewModel: PlayerViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val state by viewModel.uiState.collectAsState()
    val playerManager = viewModel.playerManager

    val isPlaying by playerManager.isPlaying.collectAsState()
    val playbackStatus by playerManager.playbackStatus.collectAsState()
    val errorMessage by playerManager.errorMessage.collectAsState()
    val currentChannel by playerManager.currentChannel.collectAsState()
    val aspectRatio by playerManager.aspectRatio.collectAsState()
    val audioTracks by playerManager.audioTracks.collectAsState()
    val subtitleTracks by playerManager.subtitleTracks.collectAsState()
    val videoQualities by playerManager.videoQualities.collectAsState()
    val videoSize by playerManager.videoSize.collectAsState()
    val isMuted by playerManager.isMuted.collectAsState()

    val activeChannel = currentChannel ?: state.channel

    // Gestures and dialogs state
    var showGestureIndicator by remember { mutableStateOf<String?>(null) }
    var gestureProgress by remember { mutableFloatStateOf(0.5f) }
    var showUnlockPrompt by remember { mutableStateOf(false) }

    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showAspectDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }

    val packageManager = context.packageManager
    val isPipSupported = remember(context) {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) &&
            state.userSettings.pipEnabled
    }

    val isLandscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    fun toggleFullscreen() {
        val window = activity?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        if (isLandscape) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            insetsController?.hide(WindowInsetsCompat.Type.systemBars())
            insetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    // Screen Keep Awake handling and Fullscreen teardown
    DisposableEffect(Unit) {
        val window = activity?.window
        if (state.userSettings.keepScreenAwake) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.let { win ->
                val insetsController = WindowCompat.getInsetsController(win, win.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Auto-hide controls timer during playback
    LaunchedEffect(state.isControlsVisible, isPlaying, state.isLocked) {
        if (state.isControlsVisible && isPlaying && !state.isLocked) {
            delay(4500)
            viewModel.setControlsVisibility(false)
        }
    }

    // Auto-dismiss unlock prompt after 3.5 seconds
    LaunchedEffect(showUnlockPrompt) {
        if (showUnlockPrompt) {
            delay(3500)
            showUnlockPrompt = false
        }
    }

    // Initialize channel
    LaunchedEffect(channelId) {
        if (playerManager.currentChannel.value?.id != channelId) {
            viewModel.loadChannel(channelId)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(state.isLocked) {
                detectTapGestures(
                    onTap = {
                        if (state.isLocked) {
                            showUnlockPrompt = !showUnlockPrompt
                        } else {
                            viewModel.toggleControlsVisibility()
                        }
                    },
                    onDoubleTap = {
                        if (!state.isLocked) {
                            playerManager.togglePlayPause()
                        }
                    }
                )
            }
            .pointerInput(state.isLocked) {
                if (!state.isLocked) {
                    detectDragGestures(
                        onDragEnd = { showGestureIndicator = null },
                        onDragCancel = { showGestureIndicator = null },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val isLeft = change.position.x < (size.width / 2)
                            val delta = -dragAmount.y / size.height

                            if (isLeft) {
                                // Brightness gesture
                                val window = activity?.window
                                if (window != null) {
                                    val currentBrightness = window.attributes.screenBrightness.let {
                                        if (it < 0) 0.5f else it
                                    }
                                    val newBrightness = (currentBrightness + delta).coerceIn(0.01f, 1f)
                                    val layoutParams = window.attributes
                                    layoutParams.screenBrightness = newBrightness
                                    window.attributes = layoutParams
                                    gestureProgress = newBrightness
                                    showGestureIndicator = "Brightness"
                                }
                            } else {
                                // Volume gesture
                                val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                val deltaVolume = (delta * maxVolume).toInt()
                                val newVolume = (currentVol + deltaVolume).coerceIn(0, maxVolume)
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                                gestureProgress = newVolume.toFloat() / maxVolume
                                showGestureIndicator = "Volume"
                            }
                        }
                    )
                }
            }
    ) {
        // Video Surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = playerManager.exoPlayer
                    useController = false
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.player = playerManager.exoPlayer
                playerView.resizeMode = when (aspectRatio) {
                    AspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    AspectRatioMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    AspectRatioMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    AspectRatioMode.SIXTEEN_NINE -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                    AspectRatioMode.FOUR_THREE -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // VLC-style Swipe Gesture Indicator
        AnimatedVisibility(
            visible = showGestureIndicator != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (showGestureIndicator == "Brightness") {
                            Icons.Filled.BrightnessMedium
                        } else {
                            if (gestureProgress == 0f) Icons.Filled.VolumeMute else Icons.Filled.VolumeUp
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "$showGestureIndicator: ${(gestureProgress * 100).toInt()}%",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { gestureProgress },
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.width(140.dp)
                    )
                }
            }
        }

        // Buffering / Loading Indicator
        if (playbackStatus.isBufferingOrConnecting) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .padding(24.dp)
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = when (playbackStatus) {
                            PlaybackStatus.CONNECTING -> "Connecting to live stream..."
                            PlaybackStatus.RECOVERING -> "Recovering live broadcast..."
                            else -> "Buffering stream..."
                        },
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Error Banner overlay
        if (playbackStatus == PlaybackStatus.ERROR) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        tint = LiveBadgeColor,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Unable to load this channel",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "The live stream may be offline or temporarily unavailable.",
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(
                            onClick = { playerManager.retryCurrentStream() },
                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text("Retry Stream")
                        }
                        TextButton(
                            onClick = { viewModel.toggleChannelList() },
                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                containerColor = Color.DarkGray,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Choose Channel")
                        }
                    }
                }
            }
        }

        // Screen Locked Floating Unlock Button
        AnimatedVisibility(
            visible = state.isLocked && showUnlockPrompt,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.85f),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .clickable {
                        viewModel.toggleLock()
                        showUnlockPrompt = false
                    }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Unlock",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Screen Locked • Tap to Unlock",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Main Controls Overlay
        AnimatedVisibility(
            visible = state.isControlsVisible && !state.isLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Overlay Bar: Navigation, Channel Info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.85f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Channel Logo
                        if (activeChannel?.logoUrl?.isNotBlank() == true) {
                            AsyncImage(
                                model = activeChannel.logoUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.DarkGray)
                                    .padding(3.dp),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(LiveBadgeColor)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "LIVE",
                                    color = LiveBadgeColor,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = activeChannel?.displayName ?: "Live TV",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = if (activeChannel?.language?.isNotBlank() == true) {
                                    "${activeChannel.groupTitle} • ${activeChannel.language}"
                                } else {
                                    activeChannel?.groupTitle ?: ""
                                },
                                color = Color.LightGray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Polished Bottom Control Bar with Translucent Gradient Overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.5f),
                                    Color.Black.copy(alpha = 0.85f),
                                    Color.Black.copy(alpha = 0.96f)
                                )
                            )
                        )
                        .navigationBarsPadding()
                        .padding(top = 20.dp, bottom = 10.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Upper row: Primary Playback & Channel Actions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Favorite Button
                            IconButton(
                                onClick = { viewModel.toggleFavorite() },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color.White.copy(alpha = 0.12f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (activeChannel?.isFavorite == true) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (activeChannel?.isFavorite == true) LiveBadgeColor else Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Center: Previous, Play/Pause, Next
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                IconButton(
                                    onClick = { playerManager.playPreviousChannel() },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(Color.White.copy(alpha = 0.16f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SkipPrevious,
                                        contentDescription = "Previous Channel",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { playerManager.togglePlayPause() },
                                    modifier = Modifier
                                        .size(54.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { playerManager.playNextChannel() },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(Color.White.copy(alpha = 0.16f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SkipNext,
                                        contentDescription = "Next Channel",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }

                            // Channel List Button
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.88f),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.clickable { viewModel.toggleChannelList() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlaylistPlay,
                                        contentDescription = "Channel List",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Channels",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Lower row: Stream-Dependent & Utility Controls (Horizontally Scrollable)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Volume / Mute
                            PlayerControlPill(
                                icon = if (isMuted) Icons.Filled.VolumeMute else Icons.Filled.VolumeUp,
                                label = if (isMuted) "Unmute" else "Mute",
                                isActive = isMuted,
                                onClick = { playerManager.toggleMute() }
                            )

                            // Fullscreen
                            PlayerControlPill(
                                icon = if (isLandscape) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                                label = if (isLandscape) "Exit Full" else "Fullscreen",
                                onClick = { toggleFullscreen() }
                            )

                            // Aspect Ratio
                            PlayerControlPill(
                                icon = Icons.Filled.AspectRatio,
                                label = when (aspectRatio) {
                                    AspectRatioMode.FIT -> "Fit"
                                    AspectRatioMode.FILL -> "Fill"
                                    AspectRatioMode.ZOOM -> "Zoom"
                                    AspectRatioMode.SIXTEEN_NINE -> "16:9"
                                    AspectRatioMode.FOUR_THREE -> "4:3"
                                },
                                onClick = { showAspectDialog = true }
                            )

                            // Audio track (ONLY WHEN SUPPORTED)
                            if (audioTracks.isNotEmpty()) {
                                PlayerControlPill(
                                    icon = Icons.Filled.Audiotrack,
                                    label = "Audio (${audioTracks.size})",
                                    onClick = { showAudioDialog = true }
                                )
                            }

                            // Subtitle (ONLY WHEN SUPPORTED)
                            if (subtitleTracks.isNotEmpty()) {
                                PlayerControlPill(
                                    icon = Icons.Filled.Subtitles,
                                    label = "Subtitles (${subtitleTracks.size})",
                                    onClick = { showSubtitleDialog = true }
                                )
                            }

                            // Quality (ONLY WHEN SUPPORTED)
                            if (videoQualities.isNotEmpty()) {
                                PlayerControlPill(
                                    icon = Icons.Filled.HighQuality,
                                    label = "Quality",
                                    onClick = { showQualityDialog = true }
                                )
                            }

                            // Picture-in-picture (ONLY WHEN SUPPORTED)
                            if (isPipSupported) {
                                PlayerControlPill(
                                    icon = Icons.Filled.PictureInPicture,
                                    label = "PiP",
                                    onClick = {
                                        val pipParams = PictureInPictureParams.Builder()
                                            .setAspectRatio(Rational(16, 9))
                                            .build()
                                        activity?.enterPictureInPictureMode(pipParams)
                                    }
                                )
                            }

                            // Player Settings
                            PlayerControlPill(
                                icon = Icons.Filled.Tune,
                                label = "Settings",
                                onClick = { showSettingsDialog = true }
                            )

                            // Screen Lock
                            PlayerControlPill(
                                icon = Icons.Filled.LockOpen,
                                label = "Lock",
                                onClick = { viewModel.toggleLock() }
                            )
                        }
                    }
                }
            }
        }

        // Quick Channel Switcher Overlay Drawer
        AnimatedVisibility(
            visible = state.isChannelListOpen,
            enter = slideInHorizontally { it },
            exit = slideOutHorizontally { it },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(320.dp)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.92f),
                modifier = Modifier.fillMaxSize()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Switch Channel",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { viewModel.closeChannelList() }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close drawer",
                                tint = Color.White
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        items(state.channelList) { ch ->
                            val isSelected = ch.id == activeChannel?.id
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clickable { viewModel.switchChannel(ch) },
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.DarkGray),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (ch.logoUrl.isNotBlank()) {
                                            AsyncImage(
                                                model = ch.logoUrl,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(2.dp),
                                                contentScale = ContentScale.Fit
                                            )
                                        } else {
                                            ChannelPlaceholder(name = ch.name, size = 12.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ch.displayName,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else Color.White,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = ch.groupTitle,
                                            color = Color.Gray,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Audio Track Dialog
        if (showAudioDialog) {
            AlertDialog(
                onDismissRequest = { showAudioDialog = false },
                title = { Text("Audio Tracks (${audioTracks.size})") },
                text = {
                    Column {
                        audioTracks.forEach { track ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        playerManager.selectTrack(track, C.TRACK_TYPE_AUDIO)
                                        showAudioDialog = false
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = track.isSelected,
                                    onClick = {
                                        playerManager.selectTrack(track, C.TRACK_TYPE_AUDIO)
                                        showAudioDialog = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(track.name, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAudioDialog = false }) { Text("Close") }
                }
            )
        }

        // Subtitle Track Dialog
        if (showSubtitleDialog) {
            AlertDialog(
                onDismissRequest = { showSubtitleDialog = false },
                title = { Text("Subtitles") },
                text = {
                    Column {
                        val anySelected = subtitleTracks.any { it.isSelected }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    playerManager.disableTrackType(C.TRACK_TYPE_TEXT)
                                    showSubtitleDialog = false
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = !anySelected,
                                onClick = {
                                    playerManager.disableTrackType(C.TRACK_TYPE_TEXT)
                                    showSubtitleDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Off (Disabled)", style = MaterialTheme.typography.bodyMedium)
                        }

                        subtitleTracks.forEach { track ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        playerManager.selectTrack(track, C.TRACK_TYPE_TEXT)
                                        showSubtitleDialog = false
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = track.isSelected,
                                    onClick = {
                                        playerManager.selectTrack(track, C.TRACK_TYPE_TEXT)
                                        showSubtitleDialog = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(track.name, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSubtitleDialog = false }) { Text("Close") }
                }
            )
        }

        // Video Quality Dialog
        if (showQualityDialog) {
            AlertDialog(
                onDismissRequest = { showQualityDialog = false },
                title = { Text("Video Quality") },
                text = {
                    Column {
                        val anySelected = videoQualities.any { it.isSelected }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    playerManager.clearTrackOverride(C.TRACK_TYPE_VIDEO)
                                    showQualityDialog = false
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = !anySelected,
                                onClick = {
                                    playerManager.clearTrackOverride(C.TRACK_TYPE_VIDEO)
                                    showQualityDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Auto (Adaptive)", style = MaterialTheme.typography.bodyMedium)
                        }

                        videoQualities.forEach { track ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        playerManager.selectTrack(track, C.TRACK_TYPE_VIDEO)
                                        showQualityDialog = false
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = track.isSelected,
                                    onClick = {
                                        playerManager.selectTrack(track, C.TRACK_TYPE_VIDEO)
                                        showQualityDialog = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(track.name, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showQualityDialog = false }) { Text("Close") }
                }
            )
        }

        // Aspect Ratio Dialog
        if (showAspectDialog) {
            AlertDialog(
                onDismissRequest = { showAspectDialog = false },
                title = { Text("Aspect Ratio") },
                text = {
                    Column {
                        AspectRatioMode.values().forEach { mode ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        playerManager.setAspectRatio(mode)
                                        showAspectDialog = false
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = aspectRatio == mode,
                                    onClick = {
                                        playerManager.setAspectRatio(mode)
                                        showAspectDialog = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when (mode) {
                                        AspectRatioMode.FIT -> "Fit to Screen"
                                        AspectRatioMode.FILL -> "Fill Screen (Crop)"
                                        AspectRatioMode.ZOOM -> "Zoom"
                                        AspectRatioMode.SIXTEEN_NINE -> "16:9 Widescreen"
                                        AspectRatioMode.FOUR_THREE -> "4:3 Standard"
                                    },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAspectDialog = false }) { Text("Close") }
                }
            )
        }

        // Player Settings Dialog
        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                title = { Text("Player Settings") },
                text = {
                    Column {
                        Text(
                            text = "Stream Information",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Channel: ${activeChannel?.displayName ?: "Unknown"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (videoSize.first > 0 && videoSize.second > 0) {
                            Text(
                                text = "Resolution: ${videoSize.first} x ${videoSize.second}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            text = "Status: $playbackStatus",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (activeChannel != null && activeChannel.groupTitle.isNotBlank()) {
                            Text(
                                text = "Category: ${activeChannel.groupTitle}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Quick Actions",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    playerManager.retryCurrentStream()
                                    showSettingsDialog = false
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reconnect Stream", style = MaterialTheme.typography.bodyMedium)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showSettingsDialog = false
                                    showAspectDialog = true
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AspectRatio,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Change Aspect Ratio", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSettingsDialog = false }) { Text("Close") }
                }
            )
        }
    }
}

@Composable
private fun PlayerControlPill(
    icon: ImageVector,
    label: String,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        color = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.14f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) MaterialTheme.colorScheme.primary else Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
