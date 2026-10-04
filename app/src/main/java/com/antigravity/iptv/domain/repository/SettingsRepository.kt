package com.antigravity.iptv.domain.repository

import com.antigravity.iptv.domain.model.AccentColor
import com.antigravity.iptv.domain.model.AppTheme
import com.antigravity.iptv.domain.model.AspectRatioMode
import com.antigravity.iptv.domain.model.LayoutStyle
import com.antigravity.iptv.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settingsFlow: Flow<UserSettings>
    suspend fun setTheme(theme: AppTheme)
    suspend fun setAccentColor(accentColor: AccentColor)
    suspend fun setLayoutStyle(layoutStyle: LayoutStyle)
    suspend fun setShowLogos(show: Boolean)
    suspend fun setAutoPlay(enabled: Boolean)
    suspend fun setAutoReconnect(enabled: Boolean)
    suspend fun setKeepScreenAwake(enabled: Boolean)
    suspend fun setDefaultAspectRatio(mode: AspectRatioMode)
    suspend fun setMiniPlayerEnabled(enabled: Boolean)
    suspend fun setHistoryEnabled(enabled: Boolean)
    suspend fun setCustomUserAgent(userAgent: String)
    suspend fun setPipEnabled(enabled: Boolean)
}
