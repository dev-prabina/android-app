package com.antigravity.iptv.ui.theme

import androidx.compose.ui.graphics.Color
import com.antigravity.iptv.domain.model.AccentColor

// Dark background palette
val DarkBackground = Color(0xFF0A0D14)
val DarkSurface = Color(0xFF131722)
val DarkSurfaceVariant = Color(0xFF1C2230)
val DarkSurfaceElevated = Color(0xFF242C3D)
val DarkBorder = Color(0xFF283145)

// Light background palette
val LightBackground = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF1F5F9)
val LightBorder = Color(0xFFE2E8F0)

// Text colors
val TextPrimaryDark = Color(0xFFF8FAFC)
val TextSecondaryDark = Color(0xFF94A3B8)
val TextMutedDark = Color(0xFF64748B)

val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF475569)
val TextMutedLight = Color(0xFF94A3B8)

// Accent definitions
fun getAccentPrimary(accent: AccentColor): Color = when (accent) {
    AccentColor.TEAL -> Color(0xFF00C896)
    AccentColor.INDIGO -> Color(0xFF6366F1)
    AccentColor.PURPLE -> Color(0xFFA855F7)
    AccentColor.AMBER -> Color(0xFFF59E0B)
    AccentColor.CRIMSON -> Color(0xFFEF4444)
}

fun getAccentContainer(accent: AccentColor, isDark: Boolean): Color = if (isDark) {
    when (accent) {
        AccentColor.TEAL -> Color(0xFF00382B)
        AccentColor.INDIGO -> Color(0xFF1E1B4B)
        AccentColor.PURPLE -> Color(0xFF2E1065)
        AccentColor.AMBER -> Color(0xFF451A03)
        AccentColor.CRIMSON -> Color(0xFF450A0A)
    }
} else {
    when (accent) {
        AccentColor.TEAL -> Color(0xFFCCFBF1)
        AccentColor.INDIGO -> Color(0xFFE0E7FF)
        AccentColor.PURPLE -> Color(0xFFF3E8FF)
        AccentColor.AMBER -> Color(0xFFFEF3C7)
        AccentColor.CRIMSON -> Color(0xFFFEE2E2)
    }
}

// Indicator colors
val LiveBadgeColor = Color(0xFFEF4444)
val SuccessGreen = Color(0xFF10B981)
val WarningOrange = Color(0xFFF59E0B)
