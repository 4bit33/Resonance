package com.resonance.player.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import com.resonance.player.domain.settings.ThemeMode

val LocalResonanceColors = compositionLocalOf { stitchDarkColors() }
val LocalResonanceTypography = compositionLocalOf { stitchTypography() }
val LocalResonanceSpacing = compositionLocalOf { ResonanceSpacing() }
val LocalResonanceRadii = compositionLocalOf { ResonanceRadii() }
val LocalResonanceDimensions = compositionLocalOf { ResonanceDimensions() }

/** Idiomatic access: `ResonanceTheme.colors.accent`, `.typography.titleMd`, `.spacing.lg`, ... */
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
}

/**
 * App theme. Stitch dark is authoritative; the persisted [ThemeMode] still
 * selects dark/light/system, but LIGHT currently maps to a stock-M3 interim
 * scheme (Stitch shipped dark tokens only — see STEP 12). Dynamic color is
 * intentionally NOT applied so Stitch tokens stay authoritative.
 */
@Composable
fun ResonanceTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val colors = when (themeMode) {
        ThemeMode.LIGHT -> interimLightColors()
        ThemeMode.DARK -> stitchDarkColors()
        ThemeMode.SYSTEM -> if (systemDark) stitchDarkColors() else interimLightColors()
    }
    val typography = stitchTypography()
    CompositionLocalProvider(
        LocalResonanceColors provides colors,
        LocalResonanceTypography provides typography,
        LocalResonanceSpacing provides ResonanceSpacing(),
        LocalResonanceRadii provides ResonanceRadii(),
        LocalResonanceDimensions provides ResonanceDimensions()
    ) {
        MaterialTheme(
            colorScheme = colors.scheme,
            typography = typography.material,
            content = content
        )
    }
}
