package com.resonance.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonance.player.app.ResonanceApp
import com.resonance.player.core.ui.theme.DEFAULT_ACCENT_HUE
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
            val themeMode by container.settingsRepository.themeMode.collectAsStateWithLifecycle(
                initialValue = ThemeMode.SYSTEM
            )
            val accentHue by container.settingsRepository.accentHue.collectAsStateWithLifecycle(
                initialValue = DEFAULT_ACCENT_HUE
            )
            ResonanceTheme(themeMode = themeMode, accentHue = accentHue) {
                ResonanceAppShell(container)
            }
        }
    }
}
