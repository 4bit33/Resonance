package com.resonance.player.domain.settings

import kotlinx.coroutines.flow.Flow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Settings owned by DataStore (never by the database, never by the UI). */
interface UserPreferencesRepository {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}
