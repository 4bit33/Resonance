package com.resonance.player.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.resonance.player.domain.settings.ThemeMode
import com.resonance.player.domain.settings.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Real DataStore-backed settings. Small, synchronous-feeling, crash-safe. */
class DataStoreSettingsRepository(private val context: Context) : UserPreferencesRepository {

    override val themeMode: Flow<ThemeMode> = context.settingsStore.data.map { prefs ->
        when (prefs[Keys.THEME]) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.DARK.name -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsStore.edit { prefs -> prefs[Keys.THEME] = mode.name }
    }

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
    }
}
