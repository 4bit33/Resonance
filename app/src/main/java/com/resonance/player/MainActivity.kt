package com.resonance.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.resonance.player.app.ResonanceApp
import com.resonance.player.core.ui.theme.ResonanceTheme
import com.resonance.player.domain.settings.ThemeMode
import com.resonance.player.navigation.ResonanceAppShell

/** Single-activity shell. Edge-to-edge; insets are handled by Material3. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as ResonanceApp).container
        setContent {
            val themeMode by container.settingsRepository.themeMode.collectAsState(
                initial = ThemeMode.SYSTEM
            )
            ResonanceTheme(themeMode = themeMode) {
                ResonanceAppShell(container)
            }
        }
    }
}
