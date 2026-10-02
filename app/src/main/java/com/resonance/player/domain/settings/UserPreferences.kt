package com.resonance.player.domain.settings

import kotlinx.coroutines.flow.Flow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * How the app looks and moves. Every value is the user's to change; the
 * defaults are only a starting point.
 */
data class LookPreferences(
    /** Glow behind the artwork, 0 (off) .. 1 (strongest). */
    val glowStrength: Float = 0.75f,
    /** The glow slowly breathes while music plays. */
    val glowBreathing: Boolean = true,
    /** Player colors follow the artwork; off = the accent color everywhere. */
    val artworkColors: Boolean = true,
    /** Animation speed: 0 = off, 1 = normal, 2 = twice as fast. */
    val motionSpeed: Float = 1f
)

/** Settings owned by DataStore (never by the database, never by the UI). */
interface UserPreferencesRepository {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)

    /** Accent hue (0-360); see [com.resonance.player.core.ui.theme.DEFAULT_ACCENT_HUE]. */
    val accentHue: Flow<Float>
    suspend fun setAccentHue(hue: Float)

    val look: Flow<LookPreferences>
    suspend fun updateLook(transform: (LookPreferences) -> LookPreferences)
}
