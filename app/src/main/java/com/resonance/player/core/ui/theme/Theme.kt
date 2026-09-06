package com.resonance.player.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.resonance.player.domain.settings.ThemeMode

private val LightScheme = lightColorScheme(
    primary = ResonancePrimary,
    onPrimary = ResonanceOnPrimary,
    primaryContainer = ResonancePrimaryContainer,
    onPrimaryContainer = ResonanceOnPrimaryContainer,
    secondaryContainer = ResonanceSecondaryContainer,
    surface = ResonanceSurface,
    onSurface = ResonanceOnSurface,
    surfaceVariant = ResonanceSurfaceVariant
)

private val DarkScheme = darkColorScheme(
    primary = ResonancePrimaryContainer,
    onPrimary = ResonanceOnPrimaryContainer,
    primaryContainer = ResonancePrimary,
    surface = ResonanceDarkSurface,
    onSurface = ResonanceDarkOnSurface
)

/**
 * App theme. Respects the persisted [ThemeMode]; dynamic color is used only
 * where the platform supports it (API 31+), otherwise seed tokens apply.
 */
@Composable
fun ResonanceTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemDark
    }
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && useDark ->
            dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicLightColorScheme(context)
        useDark -> DarkScheme
        else -> LightScheme
    }
    MaterialTheme(colorScheme = scheme, typography = ResonanceTypography, content = content)
}
