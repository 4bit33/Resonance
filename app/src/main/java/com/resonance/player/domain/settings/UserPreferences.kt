package com.resonance.player.domain.settings

import kotlinx.coroutines.flow.Flow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Settings owned by DataStore (never by the database, never by the UI). */
interface UserPreferencesRepository {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)

    /** Accent hue (0-360); see [com.resonance.player.core.ui.theme.DEFAULT_ACCENT_HUE]. */
    val accentHue: Flow<Float>
    suspend fun setAccentHue(hue: Float)
}
