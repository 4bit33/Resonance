package com.resonance.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import android.os.Build
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.resonance.player.core.permissions.MusicPermissions
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonance.player.app.ResonanceApp
import com.resonance.player.core.ui.theme.ResonanceTheme
import com.resonance.player.domain.settings.ThemeMode
import com.resonance.player.navigation.ResonanceAppShell

/** Single-activity shell. Edge-to-edge; insets are handled by Material3. */
class MainActivity : ComponentActivity() {
    override fun onResume() {
        super.onResume()
        // Permission may have changed while away (Settings toggle): refresh
        // the centralized status, then let the single-flight scanner
        // reconcile incrementally (never duplicated, never a loop).
        val container = (application as ResonanceApp).container
        lifecycleScope.launch {
            container.permissionManager.refresh(
                ActivityCompat.shouldShowRequestPermissionRationale(
                    this@MainActivity,
                    MusicPermissions.audioPermissionForSdk(Build.VERSION.SDK_INT)
                )
            )
            container.onForegrounded()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as ResonanceApp).container
        setContent {
            val themeMode by container.settingsRepository.themeMode.collectAsStateWithLifecycle(
                initialValue = ThemeMode.SYSTEM
            )
            ResonanceTheme(themeMode = themeMode) {
                ResonanceAppShell(container)
            }
        }
    }
}
