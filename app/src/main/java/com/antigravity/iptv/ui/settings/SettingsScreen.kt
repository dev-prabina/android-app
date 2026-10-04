package com.antigravity.iptv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antigravity.iptv.domain.model.AccentColor
import com.antigravity.iptv.domain.model.AppTheme
import com.antigravity.iptv.domain.model.AspectRatioMode
import com.antigravity.iptv.domain.model.LayoutStyle
import com.antigravity.iptv.ui.components.IptvSwitch
import com.antigravity.iptv.ui.theme.LiveBadgeColor
import com.antigravity.iptv.ui.theme.getAccentPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val settings by viewModel.settings.collectAsState()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showAccentDialog by remember { mutableStateOf(false) }
    var showAspectDialog by remember { mutableStateOf(false) }
    var showLayoutDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Appearance Section
            item {
                SettingsSection(title = "Appearance") {
                    SettingsSwitchItem(
                        icon = Icons.Filled.DarkMode,
                        title = "Dark Mode",
                        subtitle = if (settings.theme == AppTheme.DARK) "Dark theme enabled" else "Light theme enabled",
                        checked = settings.theme == AppTheme.DARK,
                        onCheckedChange = { isDark ->
                            viewModel.setTheme(if (isDark) AppTheme.DARK else AppTheme.LIGHT)
                        }
                    )
                    SettingsClickableItem(
                        icon = Icons.Filled.DarkMode,
                        title = "Theme Preference",
                        subtitle = when (settings.theme) {
                            AppTheme.SYSTEM -> "System Default"
                            AppTheme.LIGHT -> "Light Theme"
                            AppTheme.DARK -> "Dark Theme"
                        },
                        onClick = { showThemeDialog = true }
                    )
                    SettingsClickableItem(
                        icon = Icons.Filled.ColorLens,
                        title = "Accent Color",
                        subtitle = settings.accentColor.displayName,
                        trailing = {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(getAccentPrimary(settings.accentColor))
                            )
                        },
                        onClick = { showAccentDialog = true }
                    )
                    SettingsClickableItem(
                        icon = Icons.Filled.GridView,
                        title = "Channel Card Layout",
                        subtitle = when (settings.layoutStyle) {
                            LayoutStyle.GRID -> "Grid Layout"
                            LayoutStyle.LIST -> "List Layout"
                            LayoutStyle.COMPACT -> "Compact Layout"
                        },
                        onClick = { showLayoutDialog = true }
                    )
                    SettingsSwitchItem(
                        icon = Icons.Filled.Tv,
                        title = "Show Channel Logos",
                        subtitle = "Display station logos in channel cards",
                        checked = settings.showLogos,
                        onCheckedChange = { viewModel.setShowLogos(it) }
                    )
                }
            }

            // Playback Section
            item {
                SettingsSection(title = "Playback") {
                    SettingsSwitchItem(
                        icon = Icons.Filled.PlayCircle,
                        title = "Auto Play",
                        subtitle = "Automatically start playing stream when channel opens",
                        checked = settings.autoPlay,
                        onCheckedChange = { viewModel.setAutoPlay(it) }
                    )
                    SettingsSwitchItem(
                        icon = Icons.Filled.Refresh,
                        title = "Auto Reconnect",
                        subtitle = "Automatically attempt to reconnect if live stream drops",
                        checked = settings.autoReconnect,
                        onCheckedChange = { viewModel.setAutoReconnect(it) }
                    )
                    SettingsSwitchItem(
                        icon = Icons.Filled.Tv,
                        title = "Keep Screen Awake",
                        subtitle = "Prevent screen from dimming or turning off during playback",
                        checked = settings.keepScreenAwake,
                        onCheckedChange = { viewModel.setKeepScreenAwake(it) }
                    )
                    SettingsClickableItem(
                        icon = Icons.Filled.AspectRatio,
                        title = "Default Aspect Ratio",
                        subtitle = when (settings.defaultAspectRatio) {
                            AspectRatioMode.FIT -> "Fit to Screen"
                            AspectRatioMode.FILL -> "Fill Screen (Crop)"
                            AspectRatioMode.ZOOM -> "Zoom"
                            AspectRatioMode.SIXTEEN_NINE -> "16:9 Widescreen"
                            AspectRatioMode.FOUR_THREE -> "4:3 Standard"
                        },
                        onClick = { showAspectDialog = true }
                    )
                    SettingsSwitchItem(
                        icon = Icons.Filled.ViewStream,
                        title = "Mini Player Bar",
                        subtitle = "Show floating mini player when navigating outside player screen",
                        checked = settings.miniPlayerEnabled,
                        onCheckedChange = { viewModel.setMiniPlayerEnabled(it) }
                    )
                    SettingsSwitchItem(
                        icon = Icons.Filled.PictureInPicture,
                        title = "Picture in Picture",
                        subtitle = "Enable PiP background playback on supported devices",
                        checked = settings.pipEnabled,
                        onCheckedChange = { viewModel.setPipEnabled(it) }
                    )
                }
            }

            // History & Data
            item {
                SettingsSection(title = "Watch History") {
                    SettingsSwitchItem(
                        icon = Icons.Filled.PlayCircle,
                        title = "Record Watch History",
                        subtitle = "Save recently watched channels for quick resumption",
                        checked = settings.historyEnabled,
                        onCheckedChange = { viewModel.setHistoryEnabled(it) }
                    )
                    SettingsClickableItem(
                        icon = Icons.Filled.Delete,
                        title = "Clear Watch History",
                        subtitle = "Delete all saved recent channels",
                        onClick = { showClearHistoryDialog = true }
                    )
                }
            }

            // About & Legal
            item {
                SettingsSection(title = "About & Legal") {
                    SettingsClickableItem(
                        icon = Icons.Filled.Info,
                        title = "IPTV Player v1.0.0",
                        subtitle = "Production Android Streaming Engine"
                    )
                    SettingsClickableItem(
                        icon = Icons.Filled.NetworkCheck,
                        title = "Built-in Playlist Source",
                        subtitle = "iptv-org / countries / India (in.m3u)"
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Text(
                            text = "Legal Notice:\nIPTV is an independent media player and does not provide, host, or distribute copyrighted media streams. Users are solely responsible for ensuring they have the legal right to access streams and playlists they add or play.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        // Theme Dialog
        if (showThemeDialog) {
            AlertDialog(
                onDismissRequest = { showThemeDialog = false },
                title = { Text("Choose Theme") },
                text = {
                    Column {
                        AppTheme.values().forEach { theme ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setTheme(theme)
                                        showThemeDialog = false
                                    }
                                    .padding(vertical = 6.dp)
                            ) {
                                RadioButton(
                                    selected = settings.theme == theme,
                                    onClick = {
                                        viewModel.setTheme(theme)
                                        showThemeDialog = false
                                    }
                                )
                                Text(
                                    text = when (theme) {
                                        AppTheme.SYSTEM -> "System Default"
                                        AppTheme.LIGHT -> "Light Theme"
                                        AppTheme.DARK -> "Dark Theme"
                                    }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showThemeDialog = false }) { Text("Cancel") }
                }
            )
        }

        // Accent Dialog
        if (showAccentDialog) {
            AlertDialog(
                onDismissRequest = { showAccentDialog = false },
                title = { Text("Choose Accent Color") },
                text = {
                    Column {
                        AccentColor.values().forEach { accent ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setAccentColor(accent)
                                        showAccentDialog = false
                                    }
                                    .padding(vertical = 6.dp)
                            ) {
                                RadioButton(
                                    selected = settings.accentColor == accent,
                                    onClick = {
                                        viewModel.setAccentColor(accent)
                                        showAccentDialog = false
                                    }
                                )
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(getAccentPrimary(accent))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(text = accent.displayName)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAccentDialog = false }) { Text("Cancel") }
                }
            )
        }

        // Layout Dialog
        if (showLayoutDialog) {
            AlertDialog(
                onDismissRequest = { showLayoutDialog = false },
                title = { Text("Channel Card Style") },
                text = {
                    Column {
                        LayoutStyle.values().forEach { layout ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setLayoutStyle(layout)
                                        showLayoutDialog = false
                                    }
                                    .padding(vertical = 6.dp)
                            ) {
                                RadioButton(
                                    selected = settings.layoutStyle == layout,
                                    onClick = {
                                        viewModel.setLayoutStyle(layout)
                                        showLayoutDialog = false
                                    }
                                )
                                Text(
                                    text = when (layout) {
                                        LayoutStyle.GRID -> "Grid"
                                        LayoutStyle.LIST -> "List"
                                        LayoutStyle.COMPACT -> "Compact"
                                    }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLayoutDialog = false }) { Text("Cancel") }
                }
            )
        }

        // Aspect Ratio Dialog
        if (showAspectDialog) {
            AlertDialog(
                onDismissRequest = { showAspectDialog = false },
                title = { Text("Default Aspect Ratio") },
                text = {
                    Column {
                        AspectRatioMode.values().forEach { mode ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setDefaultAspectRatio(mode)
                                        showAspectDialog = false
                                    }
                                    .padding(vertical = 6.dp)
                            ) {
                                RadioButton(
                                    selected = settings.defaultAspectRatio == mode,
                                    onClick = {
                                        viewModel.setDefaultAspectRatio(mode)
                                        showAspectDialog = false
                                    }
                                )
                                Text(
                                    text = when (mode) {
                                        AspectRatioMode.FIT -> "Fit to Screen"
                                        AspectRatioMode.FILL -> "Fill Screen (Crop)"
                                        AspectRatioMode.ZOOM -> "Zoom"
                                        AspectRatioMode.SIXTEEN_NINE -> "16:9 Widescreen"
                                        AspectRatioMode.FOUR_THREE -> "4:3 Standard"
                                    }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAspectDialog = false }) { Text("Cancel") }
                }
            )
        }

        // Clear History Confirmation Dialog
        if (showClearHistoryDialog) {
            AlertDialog(
                onDismissRequest = { showClearHistoryDialog = false },
                title = { Text("Clear Watch History") },
                text = { Text("Are you sure you want to clear your entire watch history?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearAllHistory()
                            showClearHistoryDialog = false
                        }
                    ) {
                        Text("Clear All", color = LiveBadgeColor)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearHistoryDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        )
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun SettingsClickableItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (trailing != null) {
            trailing()
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
        }
        IptvSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}
