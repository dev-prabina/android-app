package com.antigravity.iptv.domain.model

enum class AppTheme {
    SYSTEM,
    LIGHT,
    DARK
}

enum class LayoutStyle {
    GRID,
    LIST,
    COMPACT
}

enum class AspectRatioMode {
    FIT,
    FILL,
    ZOOM,
    SIXTEEN_NINE,
    FOUR_THREE
}

enum class AccentColor(val displayName: String, val primaryHex: Long) {
    TEAL("Emerald Teal", 0xFF00BFA5),
    INDIGO("Electric Indigo", 0xFF6366F1),
    PURPLE("Neon Purple", 0xFFA855F7),
    AMBER("Warm Amber", 0xFFF59E0B),
    CRIMSON("Ruby Crimson", 0xFFEF4444)
}

data class UserSettings(
    val theme: AppTheme = AppTheme.DARK,
    val accentColor: AccentColor = AccentColor.TEAL,
    val layoutStyle: LayoutStyle = LayoutStyle.GRID,
    val showLogos: Boolean = true,
    val autoPlay: Boolean = true,
    val autoReconnect: Boolean = true,
    val keepScreenAwake: Boolean = true,
    val defaultAspectRatio: AspectRatioMode = AspectRatioMode.FIT,
    val miniPlayerEnabled: Boolean = true,
    val historyEnabled: Boolean = true,
    val customUserAgent: String = "VLC/3.0.18 LibVLC/3.0.18",
    val pipEnabled: Boolean = true
)
