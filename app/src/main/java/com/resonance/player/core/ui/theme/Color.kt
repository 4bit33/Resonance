package com.resonance.player.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Dark neutrals. Deliberately quiet and close together: the color in the app
 * comes from the artwork and the one accent, never from the chrome.
 * No hex values may live in UI code; screens use [ResonanceColors].
 */
internal val InkBackground = Color(0xFF0B0B0E)
internal val InkSurfaceLowest = Color(0xFF070709)
internal val InkSurfaceLow = Color(0xFF111115)
internal val InkSurfaceContainer = Color(0xFF16161B)
internal val InkSurfaceHigh = Color(0xFF1D1D23)
internal val InkSurfaceHighest = Color(0xFF26262D)
internal val InkTextPrimary = Color(0xFFF4F3F6)
internal val InkTextSecondary = Color(0xFFA9A7B0)
internal val InkTextMuted = Color(0xFF75737D)
/** Dark text on the accent: every accent tone is generated light enough for it. */
internal val InkOnAccent = Color(0xFF121014)
internal val InkError = Color(0xFFFFB4AB)
internal val InkOnError = Color(0xFF690005)
internal val InkErrorContainer = Color(0xFF93000A)
internal val InkOnErrorContainer = Color(0xFFFFDAD6)
internal val InkStatusError = Color(0xFFFF6B6B)
internal val InkStatusErrorContainer = Color(0xFF3A1416)
internal val InkOutlineSubtle = Color(0xFF24242B)
internal val InkOutlineStrong = Color(0xFF3A3A44)
