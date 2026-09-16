package com.resonance.player.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Semantic Resonance colors. M3 roles come from [scheme] (so stock M3
 * components keep working); Stitch-only roles (container tiers, text tiers,
 * copper/cyan accents, status colors, outlines) live here by intent —
 * never `color1`/`orange2` style names.
 */
@Immutable
data class ResonanceColors(
    val scheme: ColorScheme,
    val background: Color,
    val surfaceLowest: Color,
    val surfaceLow: Color,
    val surfaceContainer: Color,
    val surfaceHigh: Color,
    val surfaceHighest: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val accentGlow: Color,
    val accentDim: Color,
    val onAccent: Color,
    val accentSecondary: Color,
    val accentSecondaryDim: Color,
    val onAccentSecondary: Color,
    val amber: Color,
    val error: Color,
    val statusError: Color,
    val statusErrorContainer: Color,
    val outlineSubtle: Color,
    val outlineStrong: Color
)

/** Default accent hue (0-360), reproducing the original Stitch copper. */
const val DEFAULT_ACCENT_HUE = 22f

/** HSV->RGB via the platform converter — same saturation/value as the
 *  original copper accent, only the hue varies, so contrast stays constant. */
private fun accentTone(hue: Float, saturation: Float, value: Float): Color =
    Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))

/** The [accent] tone for a hue, for swatch previews in the picker UI. */
fun accentPreviewColor(hue: Float): Color = accentTone(hue, 0.81f, 1f)

/** Curated accent hues (name to degrees) for the palette picker. */
val ACCENT_PRESETS = listOf(
    "Copper" to DEFAULT_ACCENT_HUE,
    "Green" to 142f,
    "Blue" to 211f,
    "Purple" to 262f,
    "Pink" to 330f,
    "Teal" to 174f
)

/** Authoritative Stitch dark scheme (dark-first per DESIGN.md). [accentHue]
 *  drives the user-selectable primary accent; everything else is fixed. */
fun stitchDarkColors(accentHue: Float = DEFAULT_ACCENT_HUE): ResonanceColors {
    val accent = accentTone(accentHue, 0.81f, 1f)
    val accentGlow = accentTone(accentHue, 0.75f, 1f)
    val accentDim = accentTone(accentHue, 0.85f, 0.29f)
    val onAccentContainer = accentTone(accentHue, 0.42f, 1f)
    val scheme = darkColorScheme(
        primary = accent,
        onPrimary = StitchOnCopper,
        primaryContainer = accentDim,
        onPrimaryContainer = onAccentContainer,
        secondary = StitchCyan,
        onSecondary = StitchOnCyan,
        secondaryContainer = StitchCyanDim,
        onSecondaryContainer = StitchCyan,
        tertiary = StitchAmber,
        onTertiary = StitchOnAmber,
        background = StitchBackground,
        onBackground = StitchTextOnSurface,
        surface = StitchBackground,
        onSurface = StitchTextPrimary,
        surfaceVariant = StitchSurfaceContainer,
        onSurfaceVariant = StitchTextSecondary,
        surfaceContainerLowest = StitchSurfaceLowest,
        surfaceContainerLow = StitchSurfaceLow,
        surfaceContainer = StitchSurfaceContainer,
        surfaceContainerHigh = StitchSurfaceHigh,
        surfaceContainerHighest = StitchSurfaceHighest,
        surfaceTint = onAccentContainer,
        outline = StitchOutlineStrong,
        outlineVariant = StitchOutlineSubtle,
        error = StitchError,
        onError = StitchOnError,
        errorContainer = StitchErrorContainer,
        onErrorContainer = StitchOnErrorContainer
    )
    return ResonanceColors(
        scheme = scheme,
        background = StitchBackground,
        surfaceLowest = StitchSurfaceLowest,
        surfaceLow = StitchSurfaceLow,
        surfaceContainer = StitchSurfaceContainer,
        surfaceHigh = StitchSurfaceHigh,
        surfaceHighest = StitchSurfaceHighest,
        textPrimary = StitchTextPrimary,
        textSecondary = StitchTextSecondary,
        textMuted = StitchTextMuted,
        accent = accent,
        accentGlow = accentGlow,
        accentDim = accentDim,
        onAccent = StitchOnCopper,
        accentSecondary = StitchCyan,
        accentSecondaryDim = StitchCyanDim,
        onAccentSecondary = StitchOnCyan,
        amber = StitchAmber,
        error = StitchError,
        statusError = StitchStatusError,
        statusErrorContainer = StitchStatusErrorContainer,
        outlineSubtle = StitchOutlineSubtle,
        outlineStrong = StitchOutlineStrong
    )
}

/**
 * Interim stock-M3 light scheme. Stitch delivered dark tokens only; this
 * keeps the persisted LIGHT preference working without fabricating
 * unapproved light colors (STEP 12).
 */
fun interimLightColors(): ResonanceColors {
    val scheme = lightColorScheme()
    return ResonanceColors(
        scheme = scheme,
        background = scheme.background,
        surfaceLowest = scheme.surface,
        surfaceLow = scheme.surface,
        surfaceContainer = scheme.surfaceContainer,
        surfaceHigh = scheme.surfaceContainerHigh,
        surfaceHighest = scheme.surfaceContainerHighest,
        textPrimary = scheme.onSurface,
        textSecondary = scheme.onSurfaceVariant,
        textMuted = scheme.onSurfaceVariant,
        accent = scheme.primary,
        accentGlow = scheme.primary,
        accentDim = scheme.primaryContainer,
        onAccent = scheme.onPrimary,
        accentSecondary = scheme.secondary,
        accentSecondaryDim = scheme.secondaryContainer,
        onAccentSecondary = scheme.onSecondary,
        amber = scheme.tertiary,
        error = scheme.error,
        statusError = scheme.error,
        statusErrorContainer = scheme.errorContainer,
        outlineSubtle = scheme.outlineVariant,
        outlineStrong = scheme.outline
    )
}
