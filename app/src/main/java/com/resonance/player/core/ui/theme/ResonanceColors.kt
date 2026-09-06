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

/** Authoritative Stitch dark scheme (dark-first per DESIGN.md). */
fun stitchDarkColors(): ResonanceColors {
    val scheme = darkColorScheme(
        primary = StitchCopper,
        onPrimary = StitchOnCopper,
        primaryContainer = StitchCopperDim,
        onPrimaryContainer = StitchCopperSoft,
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
        surfaceTint = StitchCopperSoft,
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
        accent = StitchCopper,
        accentGlow = StitchCopperGlow,
        accentDim = StitchCopperDim,
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
