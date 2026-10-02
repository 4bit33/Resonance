package com.resonance.player.core.ui.theme

import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.resonance.player.domain.settings.LookPreferences
import com.resonance.player.domain.settings.ThemeMode

val LocalResonanceColors = compositionLocalOf { darkColors() }
val LocalResonanceTypography = compositionLocalOf { appTypography() }
val LocalResonanceSpacing = compositionLocalOf { ResonanceSpacing() }
val LocalResonanceRadii = compositionLocalOf { ResonanceRadii() }
val LocalResonanceDimensions = compositionLocalOf { ResonanceDimensions() }
val LocalResonanceMotion = compositionLocalOf { ResonanceMotion() }
val LocalLookPreferences = compositionLocalOf { LookPreferences() }

/** Idiomatic access: `ResonanceTheme.colors.accent`, `.typography.titleMd`, `.motion.spatial()`, ... */
object ResonanceTheme {
    val colors: ResonanceColors
        @Composable @ReadOnlyComposable get() = LocalResonanceColors.current
    val typography: ResonanceTypography
        @Composable @ReadOnlyComposable get() = LocalResonanceTypography.current
    val spacing: ResonanceSpacing
        @Composable @ReadOnlyComposable get() = LocalResonanceSpacing.current
    val radii: ResonanceRadii
        @Composable @ReadOnlyComposable get() = LocalResonanceRadii.current
    val dimensions: ResonanceDimensions
        @Composable @ReadOnlyComposable get() = LocalResonanceDimensions.current
    val motion: ResonanceMotion
        @Composable @ReadOnlyComposable get() = LocalResonanceMotion.current
    val look: LookPreferences
        @Composable @ReadOnlyComposable get() = LocalLookPreferences.current
}

/**
 * App theme. Dark is the designed theme; LIGHT maps to a stock-M3 interim
 * scheme. Dynamic color is not applied: the player takes its colors from the
 * artwork instead ([rememberArtworkPalette]).
 *
 * Motion speed is the user's setting multiplied by the system animator scale,
 * so "Remove animations" in Android accessibility settings turns them off here too.
 */
@Composable
fun ResonanceTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentHue: Float = DEFAULT_ACCENT_HUE,
    look: LookPreferences = LookPreferences(),
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val colors = when (themeMode) {
        ThemeMode.LIGHT -> interimLightColors()
        ThemeMode.DARK -> darkColors(accentHue)
        ThemeMode.SYSTEM -> if (systemDark) darkColors(accentHue) else interimLightColors()
    }
    val typography = remember { appTypography() }
    val context = LocalContext.current
    val systemScale = remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }
    val motion = ResonanceMotion(speed = if (systemScale == 0f) 0f else look.motionSpeed)
    CompositionLocalProvider(
        LocalResonanceColors provides colors,
        LocalResonanceTypography provides typography,
        LocalResonanceSpacing provides ResonanceSpacing(),
        LocalResonanceRadii provides ResonanceRadii(),
        LocalResonanceDimensions provides ResonanceDimensions(),
        LocalResonanceMotion provides motion,
        LocalLookPreferences provides look
    ) {
        MaterialTheme(
            colorScheme = colors.scheme,
            typography = typography.material,
            content = content
        )
    }
}
