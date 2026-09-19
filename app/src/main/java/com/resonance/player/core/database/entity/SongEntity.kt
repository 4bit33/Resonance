package com.resonance.player.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Cached song row. The primary key is a stable hash of provider authority +
 * document id (see StableIds), so it survives rescans and re-adds; a moved or
 * renamed file is a new song. Every song belongs to one [SourceEntity]; the
 * FK cascade removes the songs when the source is removed.
 *
 * Playback stats (playCount/lastPlayed) live in the same row but are
 * PRESERVED across rescans by merging — the scanner never overwrites them.
 *
 * Query indexes: title, artist, album, albumArtist, genre, path, source,
 * artworkKey, date added, last played, play count.
 */
@Entity(
    tableName = "songs",
    foreignKeys = [
        ForeignKey(
            entity = SourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["title"]),
        Index(value = ["artistName"]),
        Index(value = ["albumName"]),
        Index(value = ["albumArtist"]),
        Index(value = ["genreName"]),
        Index(value = ["path"]),
        Index(value = ["sourceId"]),
        Index(value = ["artworkKey"]),
        Index(value = ["dateAddedEpochSec"]),
        Index(value = ["lastPlayedEpochSec"]),
        Index(value = ["playCount"])
    ]
)
data class SongEntity(
    @PrimaryKey val id: Long,
    val sourceId: Long,
    val title: String,
    val artistName: String,
    val albumName: String,
    val albumArtist: String?,
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
    val fileSizeBytes: Long,
    val dateAddedEpochSec: Long,
    val dateModifiedEpochSec: Long,
    val lastScannedAtSec: Long,
    val artworkKey: String?,
    val artworkUri: String?,
    val playCount: Long = 0L,
    val lastPlayedEpochSec: Long? = null,
    val bpm: Float? = null,
    val musicalKey: String? = null
)
