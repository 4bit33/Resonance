package com.resonance.player.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.resonance.player.core.ui.theme.DEFAULT_ACCENT_HUE
import com.resonance.player.domain.settings.LookPreferences
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

    override val accentHue: Flow<Float> = context.settingsStore.data.map { prefs ->
        prefs[Keys.ACCENT_HUE] ?: DEFAULT_ACCENT_HUE
    }

    override suspend fun setAccentHue(hue: Float) {
        context.settingsStore.edit { prefs -> prefs[Keys.ACCENT_HUE] = hue }
    }

    override val look: Flow<LookPreferences> = context.settingsStore.data.map { it.toLook() }

    override suspend fun updateLook(transform: (LookPreferences) -> LookPreferences) {
        context.settingsStore.edit { prefs ->
            val next = transform(prefs.toLook())
            prefs[Keys.GLOW_STRENGTH] = next.glowStrength.coerceIn(0f, 1f)
            prefs[Keys.GLOW_BREATHING] = next.glowBreathing
            prefs[Keys.ARTWORK_COLORS] = next.artworkColors
            prefs[Keys.MOTION_SPEED] = next.motionSpeed.coerceIn(0f, 3f)
        }
    }

    private fun Preferences.toLook(): LookPreferences {
        val defaults = LookPreferences()
        return LookPreferences(
            glowStrength = this[Keys.GLOW_STRENGTH] ?: defaults.glowStrength,
            glowBreathing = this[Keys.GLOW_BREATHING] ?: defaults.glowBreathing,
            artworkColors = this[Keys.ARTWORK_COLORS] ?: defaults.artworkColors,
            motionSpeed = this[Keys.MOTION_SPEED] ?: defaults.motionSpeed
        )
    }

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val ACCENT_HUE = floatPreferencesKey("accent_hue")
        val GLOW_STRENGTH = floatPreferencesKey("look_glow_strength")
        val GLOW_BREATHING = booleanPreferencesKey("look_glow_breathing")
        val ARTWORK_COLORS = booleanPreferencesKey("look_artwork_colors")
        val MOTION_SPEED = floatPreferencesKey("look_motion_speed")
    }
}
