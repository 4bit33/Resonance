package com.resonance.player

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonance.player.app.ResonanceApp
import com.resonance.player.core.ui.theme.DEFAULT_ACCENT_HUE
import com.resonance.player.core.ui.theme.ResonanceTheme
import com.resonance.player.domain.importer.extractImportUrl
import com.resonance.player.domain.settings.LookPreferences
import com.resonance.player.domain.settings.ThemeMode
import com.resonance.player.navigation.ResonanceAppShell

/** Single-activity shell. Edge-to-edge; insets are handled by Material3. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as ResonanceApp).container
        handleShare(intent)
        setContent {
            val themeMode by container.settingsRepository.themeMode.collectAsStateWithLifecycle(
                initialValue = ThemeMode.SYSTEM
            )
            val accentHue by container.settingsRepository.accentHue.collectAsStateWithLifecycle(
                initialValue = DEFAULT_ACCENT_HUE
            )
            val look by container.settingsRepository.look.collectAsStateWithLifecycle(
                initialValue = LookPreferences()
            )
            ResonanceTheme(themeMode = themeMode, accentHue = accentHue, look = look) {
                ResonanceAppShell(container)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShare(intent)
    }

    /** A link shared from YouTube / YouTube Music / a browser goes to the import screen. */
    private fun handleShare(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        val url = extractImportUrl(intent.getStringExtra(Intent.EXTRA_TEXT)) ?: return
        (application as ResonanceApp).container.importManager.pendingShare.value = url
    }
}
