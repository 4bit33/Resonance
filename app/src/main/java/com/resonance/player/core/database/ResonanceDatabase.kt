package com.resonance.player.core.database

import com.resonance.player.core.database.dao.GenreOverrideDao
import com.resonance.player.core.database.entity.GenreOverrideEntity
import androidx.room.Database
import androidx.room.RoomDatabase
import com.resonance.player.core.database.dao.ExcludedSongDao
import com.resonance.player.core.database.dao.FavoriteDao
import com.resonance.player.core.database.dao.HistoryDao
import com.resonance.player.core.database.dao.PlaylistDao
import com.resonance.player.core.database.dao.SongDao
import com.resonance.player.core.database.dao.SourceDao
import com.resonance.player.core.database.entity.ExcludedSongEntity
import com.resonance.player.core.database.entity.FavoriteEntity
import com.resonance.player.core.database.entity.HistoryEntryEntity
import com.resonance.player.core.database.entity.PlaylistEntity
import com.resonance.player.core.database.entity.PlaylistItemEntity
import com.resonance.player.core.database.entity.SongEntity
import com.resonance.player.core.database.entity.SourceEntity

/**
 * Single Room database for the app (ADR-005). Version 4 — the library is
 * built from user-added sources (folders / single songs) instead of a device
 * scan; songs carry a stable id and belong to a source (ADR-010). Upgrade
 * from v2 is the hand-written [MIGRATION_2_3] (clean start for songs); v3 -> v4
 * ([MIGRATION_3_4]) adds the excluded-songs table and keeps all data; v4 -> v5
 * ([MIGRATION_4_5]) adds playlist covers; v5 -> v6 ([MIGRATION_5_6]) adds genre overrides.
 */
@Database(
    entities = [
        SourceEntity::class,
        SongEntity::class,
        ExcludedSongEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class,
        FavoriteEntity::class,
        HistoryEntryEntity::class,
        GenreOverrideEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class ResonanceDatabase : RoomDatabase() {
    abstract fun sourceDao(): SourceDao
    abstract fun songDao(): SongDao
    abstract fun excludedSongDao(): ExcludedSongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao
    abstract fun genreOverrideDao(): GenreOverrideDao
}
