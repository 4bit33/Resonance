package com.resonance.player.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Cached song row. Primary key mirrors the MediaStore audio id captured at
 * import time (ADR-005: stable within a MediaStore generation; a deleted and
 * re-added file is treated as a new song — documented, not silent).
 *
 * Phase 3 (schema v2): storage volume, file size, album artist, relative
 * path, track/disc totals, artwork references and last-scan stamp. Playback
 * stats (playCount/lastPlayed) live in the same row but are PRESERVED across
 * rescans by merging — the scanner never overwrites them.
 *
 * Query indexes: title, artist, album, albumArtist, genre, path,
 * mediaStoreId, artworkKey, date added, last played, play count.
 */
@Entity(
    tableName = "songs",
    indices = [
        Index(value = ["title"]),
        Index(value = ["artistName"]),
        Index(value = ["albumName"]),
        Index(value = ["albumArtist"]),
        Index(value = ["genreName"]),
        Index(value = ["path"]),
        Index(value = ["mediaStoreId"]),
        Index(value = ["artworkKey"]),
        Index(value = ["dateAddedEpochSec"]),
        Index(value = ["lastPlayedEpochSec"]),
        Index(value = ["playCount"])
    ]
)
data class SongEntity(
    @PrimaryKey val id: Long,
    val mediaStoreId: Long,
    @ColumnInfo(defaultValue = "'external'") val volumeName: String,
    val title: String,
    val artistName: String,
    val albumName: String,
    val albumArtist: String?,
    val albumId: Long?,
    val artistId: Long?,
    val genreName: String?,
    val trackNumber: Int?,
    val totalTracks: Int?,
    val discNumber: Int?,
    val totalDiscs: Int?,
    val year: Int?,
    val durationMs: Long,
    val path: String,
    val contentUri: String,
    val relativePath: String?,
    val mimeType: String?,
    val bitrate: Int?,
    val sampleRate: Int?,
    @ColumnInfo(defaultValue = "0") val fileSizeBytes: Long,
    val dateAddedEpochSec: Long,
    val dateModifiedEpochSec: Long,
    @ColumnInfo(defaultValue = "0") val lastScannedAtSec: Long,
    val artworkKey: String?,
    val artworkUri: String?,
    val playCount: Long = 0L,
    val lastPlayedEpochSec: Long? = null,
    val bpm: Float? = null,
    val musicalKey: String? = null
)
