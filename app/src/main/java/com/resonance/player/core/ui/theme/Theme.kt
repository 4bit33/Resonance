package com.resonance.player.core.ui.theme

import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
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
 * scheme. The full dynamic scheme is not applied (the player takes its colors
 * from the artwork, [rememberArtworkPalette]); the wallpaper can only supply
 * the accent hue ([WALLPAPER_ACCENT_HUE]).
 *
 * Motion speed is the user's setting multiplied by the system animator scale,
 * so "Remove animations" in Android accessibility settings turns them off here too.
 */
/** Stored accent hue that means "follow the wallpaper" (Material You, Android 12+). */
const val WALLPAPER_ACCENT_HUE = -1f

val wallpaperAccentAvailable: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/** The hue to draw with: the wallpaper's accent for [WALLPAPER_ACCENT_HUE], else the stored hue. */
@Composable
fun resolveAccentHue(stored: Float): Float {
    if (stored >= 0f) return stored
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return DEFAULT_ACCENT_HUE
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(colorResource(android.R.color.system_accent1_500).toArgb(), hsv)
    return hsv[0]
}

@Composable
fun ResonanceTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentHue: Float = DEFAULT_ACCENT_HUE,
    look: LookPreferences = LookPreferences(),
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    @Suppress("NAME_SHADOWING") val accentHue = resolveAccentHue(accentHue)
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
