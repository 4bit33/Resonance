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
