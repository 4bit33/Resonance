package com.resonance.player.core.model

/**
 * Stable domain Song. Identity is [id] (MediaStore audio id at import time,
 * Room primary key). [path]/[contentUri] are indexed for lookup but are NEVER
 * the sole identity of a song (ADR-005).
 *
 * Phase 3 additions: storage volume, file size, album artist, relative path,
 * track/disc totals, artwork references and last-scan stamp. BPM / musicalKey
 * stay reserved for the Smart Mix engine (later phase, still no fake data).
 *
 * UI layers may only observe this type — never Room entities (ADR-004).
 */
data class Song(
    val id: Long,
    val mediaStoreId: Long,
    val volumeName: String,
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
    val fileSizeBytes: Long,
    val dateAddedEpochSec: Long,
    val dateModifiedEpochSec: Long,
    val lastScannedAtSec: Long,
    val artworkKey: String?,
    val artworkUri: String?,
    val playCount: Long = 0L,
    val lastPlayedEpochSec: Long? = null,
    val isFavorite: Boolean = false,
    val bpm: Float? = null,
    val musicalKey: String? = null
)
