package com.antigravity.iptv.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.antigravity.iptv.domain.model.AccentColor
import com.antigravity.iptv.domain.model.AppTheme
import com.antigravity.iptv.domain.model.AspectRatioMode
import com.antigravity.iptv.domain.model.LayoutStyle
import com.antigravity.iptv.domain.model.UserSettings
import com.antigravity.iptv.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "iptv_settings")

class SettingsRepositoryImpl(
    private val context: Context
) : SettingsRepository {

    private object PreferencesKeys {
        val THEME = stringPreferencesKey("theme")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val LAYOUT_STYLE = stringPreferencesKey("layout_style")
        val SHOW_LOGOS = booleanPreferencesKey("show_logos")
        val AUTO_PLAY = booleanPreferencesKey("auto_play")
        val AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
        val KEEP_SCREEN_AWAKE = booleanPreferencesKey("keep_screen_awake")
        val DEFAULT_ASPECT_RATIO = stringPreferencesKey("default_aspect_ratio")
        val MINI_PLAYER_ENABLED = booleanPreferencesKey("mini_player_enabled")
        val HISTORY_ENABLED = booleanPreferencesKey("history_enabled")
        val CUSTOM_USER_AGENT = stringPreferencesKey("custom_user_agent")
        val PIP_ENABLED = booleanPreferencesKey("pip_enabled")
    }

    override val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { preferences ->
        val theme = runCatching {
            AppTheme.valueOf(preferences[PreferencesKeys.THEME] ?: AppTheme.DARK.name)
        }.getOrDefault(AppTheme.DARK)

        val accent = runCatching {
            AccentColor.valueOf(preferences[PreferencesKeys.ACCENT_COLOR] ?: AccentColor.TEAL.name)
        }.getOrDefault(AccentColor.TEAL)

        val layout = runCatching {
            LayoutStyle.valueOf(preferences[PreferencesKeys.LAYOUT_STYLE] ?: LayoutStyle.GRID.name)
        }.getOrDefault(LayoutStyle.GRID)

        val aspectRatio = runCatching {
            AspectRatioMode.valueOf(preferences[PreferencesKeys.DEFAULT_ASPECT_RATIO] ?: AspectRatioMode.FIT.name)
        }.getOrDefault(AspectRatioMode.FIT)

        UserSettings(
            theme = theme,
            accentColor = accent,
            layoutStyle = layout,
            showLogos = preferences[PreferencesKeys.SHOW_LOGOS] ?: true,
            autoPlay = preferences[PreferencesKeys.AUTO_PLAY] ?: true,
            autoReconnect = preferences[PreferencesKeys.AUTO_RECONNECT] ?: true,
            keepScreenAwake = preferences[PreferencesKeys.KEEP_SCREEN_AWAKE] ?: true,
            defaultAspectRatio = aspectRatio,
            miniPlayerEnabled = preferences[PreferencesKeys.MINI_PLAYER_ENABLED] ?: true,
            historyEnabled = preferences[PreferencesKeys.HISTORY_ENABLED] ?: true,
            customUserAgent = preferences[PreferencesKeys.CUSTOM_USER_AGENT] ?: "VLC/3.0.18 LibVLC/3.0.18",
            pipEnabled = preferences[PreferencesKeys.PIP_ENABLED] ?: true
        )
    }

    override suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { it[PreferencesKeys.THEME] = theme.name }
    }

    override suspend fun setAccentColor(accentColor: AccentColor) {
        context.dataStore.edit { it[PreferencesKeys.ACCENT_COLOR] = accentColor.name }
    }

    override suspend fun setLayoutStyle(layoutStyle: LayoutStyle) {
        context.dataStore.edit { it[PreferencesKeys.LAYOUT_STYLE] = layoutStyle.name }
    }

    override suspend fun setShowLogos(show: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.SHOW_LOGOS] = show }
    }

    override suspend fun setAutoPlay(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AUTO_PLAY] = enabled }
    }

    override suspend fun setAutoReconnect(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AUTO_RECONNECT] = enabled }
    }

    override suspend fun setKeepScreenAwake(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.KEEP_SCREEN_AWAKE] = enabled }
    }

    override suspend fun setDefaultAspectRatio(mode: AspectRatioMode) {
        context.dataStore.edit { it[PreferencesKeys.DEFAULT_ASPECT_RATIO] = mode.name }
    }

    override suspend fun setMiniPlayerEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.MINI_PLAYER_ENABLED] = enabled }
    }

    override suspend fun setHistoryEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.HISTORY_ENABLED] = enabled }
    }

    override suspend fun setCustomUserAgent(userAgent: String) {
        context.dataStore.edit { it[PreferencesKeys.CUSTOM_USER_AGENT] = userAgent }
    }

    override suspend fun setPipEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.PIP_ENABLED] = enabled }
    }
}
