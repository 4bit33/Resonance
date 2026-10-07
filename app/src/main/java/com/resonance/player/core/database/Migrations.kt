package com.resonance.player.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Schema v1 -> v2 (Phase 3 library pipeline). Purely additive: nine new
 * columns for volume/size/album-artist/path/totals/scan-stamp/artwork.
 * Playlists, favorites and history are untouched — no user data is lost and
 * nothing is cascade-deleted (those tables hold no FK to songs).
 */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE songs ADD COLUMN volumeName TEXT NOT NULL DEFAULT 'external'")
        db.execSQL("ALTER TABLE songs ADD COLUMN fileSizeBytes INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE songs ADD COLUMN albumArtist TEXT")
        db.execSQL("ALTER TABLE songs ADD COLUMN relativePath TEXT")
        db.execSQL("ALTER TABLE songs ADD COLUMN totalTracks INTEGER")
        db.execSQL("ALTER TABLE songs ADD COLUMN totalDiscs INTEGER")
        db.execSQL("ALTER TABLE songs ADD COLUMN lastScannedAtSec INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE songs ADD COLUMN artworkKey TEXT")
        db.execSQL("ALTER TABLE songs ADD COLUMN artworkUri TEXT")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_songs_albumArtist ON songs(albumArtist)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_songs_genreName ON songs(genreName)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_songs_artworkKey ON songs(artworkKey)")
    }
}

/**
 * Schema v2 -> v3 (music sources, ADR-010): the library is no longer built by
 * a device-wide scan. CLEAN START by decision: the old songs (MediaStore ids,
 * no source) are dropped and the tables that pointed at them (playlist items,
 * favorites, play history) are emptied; playlist NAMES are kept. New `sources`
 * table, new `songs` table with a `sourceId` FK (ON DELETE CASCADE).
 *
 * The CREATE statements are copied verbatim from the Room-generated
 * `ResonanceDatabase_Impl.createAllTables` so a migrated DB is identical to a
 * fresh install (Room validates it on open). If an entity changes, re-copy.
 */
val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `songs`")
        db.execSQL("CREATE TABLE IF NOT EXISTS `sources` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `kind` TEXT NOT NULL, `uri` TEXT NOT NULL, `displayName` TEXT NOT NULL, `addedAtEpochSec` INTEGER NOT NULL, `lastScannedAtEpochSec` INTEGER)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_sources_uri` ON `sources` (`uri`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `songs` (`id` INTEGER NOT NULL, `sourceId` INTEGER NOT NULL, `title` TEXT NOT NULL, `artistName` TEXT NOT NULL, `albumName` TEXT NOT NULL, `albumArtist` TEXT, `genreName` TEXT, `trackNumber` INTEGER, `totalTracks` INTEGER, `discNumber` INTEGER, `totalDiscs` INTEGER, `year` INTEGER, `durationMs` INTEGER NOT NULL, `path` TEXT NOT NULL, `contentUri` TEXT NOT NULL, `relativePath` TEXT, `mimeType` TEXT, `bitrate` INTEGER, `sampleRate` INTEGER, `fileSizeBytes` INTEGER NOT NULL, `dateAddedEpochSec` INTEGER NOT NULL, `dateModifiedEpochSec` INTEGER NOT NULL, `lastScannedAtSec` INTEGER NOT NULL, `artworkKey` TEXT, `artworkUri` TEXT, `playCount` INTEGER NOT NULL, `lastPlayedEpochSec` INTEGER, `bpm` REAL, `musicalKey` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`sourceId`) REFERENCES `sources`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_title` ON `songs` (`title`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_artistName` ON `songs` (`artistName`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_albumName` ON `songs` (`albumName`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_albumArtist` ON `songs` (`albumArtist`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_genreName` ON `songs` (`genreName`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_path` ON `songs` (`path`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_sourceId` ON `songs` (`sourceId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_artworkKey` ON `songs` (`artworkKey`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_dateAddedEpochSec` ON `songs` (`dateAddedEpochSec`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_lastPlayedEpochSec` ON `songs` (`lastPlayedEpochSec`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_playCount` ON `songs` (`playCount`)")
        db.execSQL("DELETE FROM playlist_items")
        db.execSQL("DELETE FROM favorites")
        db.execSQL("DELETE FROM playback_history")
    }
}
