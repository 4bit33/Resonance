package com.resonance.player.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Semantic colors. M3 roles come from [scheme] (so stock M3 components keep
 * working); the app's own roles (container tiers, text tiers, accent, status
 * colors, outlines) live here by intent, never `color1`/`orange2` style names.
 * There is ONE accent: the "secondary" roles are kept for old call sites and
 * point at it.
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

/** Default accent hue (0-360): a warm copper. */
const val DEFAULT_ACCENT_HUE = 22f

/** HSV->RGB via the platform converter: only the hue varies between accents,
 *  so contrast with the dark text on them stays constant. */
private fun accentTone(hue: Float, saturation: Float, value: Float): Color =
    Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))

/** The [accent] tone for a hue, for swatch previews in the picker UI. */
fun accentPreviewColor(hue: Float): Color = accentTone(hue, 0.55f, 1f)

/** Curated accent hues (name to degrees) for the palette picker. */
val ACCENT_PRESETS = listOf(
    "Copper" to DEFAULT_ACCENT_HUE,
    "Green" to 142f,
    "Blue" to 211f,
    "Purple" to 262f,
    "Pink" to 330f,
    "Teal" to 174f
)

/** The dark scheme. [accentHue] drives the one accent; everything else is neutral. */
fun darkColors(accentHue: Float = DEFAULT_ACCENT_HUE): ResonanceColors {
    val accent = accentTone(accentHue, 0.55f, 1f)
    val accentGlow = accentTone(accentHue, 0.75f, 1f)
    val accentDim = accentTone(accentHue, 0.6f, 0.26f)
    val onAccentContainer = accentTone(accentHue, 0.35f, 1f)
    val scheme = darkColorScheme(
        primary = accent,
        onPrimary = InkOnAccent,
        primaryContainer = accentDim,
        onPrimaryContainer = onAccentContainer,
        secondary = accent,
        onSecondary = InkOnAccent,
        secondaryContainer = accentDim,
        onSecondaryContainer = onAccentContainer,
        tertiary = accentGlow,
        onTertiary = InkOnAccent,
        background = InkBackground,
        onBackground = InkTextPrimary,
        surface = InkBackground,
        onSurface = InkTextPrimary,
        surfaceVariant = InkSurfaceContainer,
        onSurfaceVariant = InkTextSecondary,
        surfaceContainerLowest = InkSurfaceLowest,
        surfaceContainerLow = InkSurfaceLow,
        surfaceContainer = InkSurfaceContainer,
        surfaceContainerHigh = InkSurfaceHigh,
        surfaceContainerHighest = InkSurfaceHighest,
        surfaceTint = onAccentContainer,
        outline = InkOutlineStrong,
        outlineVariant = InkOutlineSubtle,
        error = InkError,
        onError = InkOnError,
        errorContainer = InkErrorContainer,
        onErrorContainer = InkOnErrorContainer
    )
    return ResonanceColors(
        scheme = scheme,
        background = InkBackground,
        surfaceLowest = InkSurfaceLowest,
        surfaceLow = InkSurfaceLow,
        surfaceContainer = InkSurfaceContainer,
        surfaceHigh = InkSurfaceHigh,
        surfaceHighest = InkSurfaceHighest,
        textPrimary = InkTextPrimary,
        textSecondary = InkTextSecondary,
        textMuted = InkTextMuted,
        accent = accent,
        accentGlow = accentGlow,
        accentDim = accentDim,
        onAccent = InkOnAccent,
        accentSecondary = accent,
        accentSecondaryDim = accentDim,
        onAccentSecondary = InkOnAccent,
        amber = accentGlow,
        error = InkError,
        statusError = InkStatusError,
        statusErrorContainer = InkStatusErrorContainer,
        outlineSubtle = InkOutlineSubtle,
        outlineStrong = InkOutlineStrong
    )
}

/** Interim stock-M3 light scheme until the light theme gets its own design. */
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
