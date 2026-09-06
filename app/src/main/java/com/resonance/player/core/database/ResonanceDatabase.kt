package com.resonance.player.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.resonance.player.core.database.dao.FavoriteDao
import com.resonance.player.core.database.dao.HistoryDao
import com.resonance.player.core.database.dao.PlaylistDao
import com.resonance.player.core.database.dao.SongDao
import com.resonance.player.core.database.entity.FavoriteEntity
import com.resonance.player.core.database.entity.HistoryEntryEntity
import com.resonance.player.core.database.entity.PlaylistEntity
import com.resonance.player.core.database.entity.PlaylistItemEntity
import com.resonance.player.core.database.entity.SongEntity

/**
 * Single Room database for the app (ADR-005). Version 2 — Phase 3 library
 * pipeline columns (volume/size/album-artist/path/totals/scan/artwork).
 * Upgrade from v1 is a purely additive [MIGRATION_1_2]; user data
 * (playlists, favorites, history, play counts) is preserved.
 */
@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class,
        FavoriteEntity::class,
        HistoryEntryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ResonanceDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao
}
