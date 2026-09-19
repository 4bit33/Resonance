package com.resonance.player.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Stitch "Resonance Audio" dark palette (DESIGN.md — the single source of
 * truth; no hex values may live in UI code).
 *
 * Two deliberate resolutions of Stitch-internal conflicts, documented:
 * - `surface-container-highest`: DESIGN.md prose (twice) says #2C3038 while
 *   one generated tailwind config says #343538 (= surface-variant). The
 *   prose wins; highest must read darker than variant-adjacent usages.
 * - Accent role: prose assigns active states/scrubbers/CTAs to copper
 *   #FF7A30 (screenshots confirm); the M3-style `primary: #ffb693` token is
 *   therefore mapped to onPrimaryContainer duty, not the accent.
 */
internal val StitchBackground = Color(0xFF121316)
internal val StitchSurfaceLowest = Color(0xFF0D0E11)
internal val StitchSurfaceLow = Color(0xFF1B1B1F)
internal val StitchSurfaceContainer = Color(0xFF1A1C20)
internal val StitchSurfaceHigh = Color(0xFF22252B)
internal val StitchSurfaceHighest = Color(0xFF2C3038)
internal val StitchSurfaceVariant = Color(0xFF343538)
internal val StitchTextPrimary = Color(0xFFF2F3F5)
internal val StitchTextSecondary = Color(0xFF9CA3AF)
internal val StitchTextMuted = Color(0xFF606775)
internal val StitchTextOnSurface = Color(0xFFE3E2E6)
// Accent (was fixed copper #FF7A30) is now user-selectable — see
// accentColors(hue) in ResonanceColors.kt. StitchOnCopper is the fixed dark
// "text on accent" color; it stays constant because every accent hue is
// generated at the same high saturation/value, so dark text always contrasts.
internal val StitchOnCopper = Color(0xFF121316)
internal val StitchCyan = Color(0xFF2EE5C8)
internal val StitchCyanDim = Color(0xFF063B34)
internal val StitchOnCyan = Color(0xFF00382F)
internal val StitchAmber = Color(0xFFFABC4D)
internal val StitchOnAmber = Color(0xFF432C00)
internal val StitchError = Color(0xFFFFB4AB)
internal val StitchOnError = Color(0xFF690005)
internal val StitchErrorContainer = Color(0xFF93000A)
internal val StitchOnErrorContainer = Color(0xFFFFDAD6)
internal val StitchStatusError = Color(0xFFFF5C5C)
internal val StitchStatusErrorContainer = Color(0xFF3E1616)
internal val StitchOutlineSubtle = Color(0xFF2B2E37)
internal val StitchOutlineStrong = Color(0xFF3F4450)
