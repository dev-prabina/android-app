package com.antigravity.iptv

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.rememberNavController
import com.antigravity.iptv.domain.model.UserSettings
import com.antigravity.iptv.ui.navigation.AppNavHost
import com.antigravity.iptv.ui.theme.IPTVTheme

@UnstableApi
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(android.R.style.Theme_Material_NoActionBar)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as IptvApplication

        setContent {
            val settings by app.settingsRepository.settingsFlow.collectAsState(initial = UserSettings())

            IPTVTheme(
                appTheme = settings.theme,
                accentColor = settings.accentColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val navController = rememberNavController()
                    AppNavHost(
                        navController = navController,
                        app = app
                    )
                }
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        // Handled automatically through Compose state and PlayerView
    }
}
