package com.resonance.player.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.libraryPrefsStore: DataStore<Preferences> by preferencesDataStore(
    name = "library_prefs"
)

/** Tiny library prefs: last scan stamp + the ignore-short-files switch. */
class LibraryPreferences(private val context: Context) {

    val lastScanEpochSec: Flow<Long?> = context.libraryPrefsStore.data.map { it[Keys.LAST_SCAN] }

    val ignoreShortFiles: Flow<Boolean> = context.libraryPrefsStore.data
        .map { it[Keys.IGNORE_SHORT] ?: false }

    suspend fun setLastScan(epochSec: Long) {
        context.libraryPrefsStore.edit { it[Keys.LAST_SCAN] = epochSec }
    }

    suspend fun setIgnoreShortFiles(ignore: Boolean) {
        context.libraryPrefsStore.edit { it[Keys.IGNORE_SHORT] = ignore }
    }

    private object Keys {
        val LAST_SCAN = longPreferencesKey("last_scan_epoch_sec")
        val IGNORE_SHORT = booleanPreferencesKey("ignore_short_files")
    }
}
