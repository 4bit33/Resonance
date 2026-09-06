package com.resonance.player.core.model

/**
 * Stable domain Song. Identity is [id] (mirrors the MediaStore audio id at
 * import time and is the Room primary key). [path]/[contentUri] are indexed
 * for lookup but are NEVER the sole identity of a song.
 *
 * UI layers may only observe this type — never Room entities (ADR-004).
 * BPM / musicalKey are reserved for the local Smart Mix engine (later phase).
 */
data class Song(
    val id: Long,
    val mediaStoreId: Long,
    val title: String,
    val artistName: String,
    val albumName: String,
    val albumId: Long?,
    val artistId: Long?,
    val genreName: String?,
    val trackNumber: Int?,
    val discNumber: Int?,
    val year: Int?,
    val durationMs: Long,
    val path: String,
    val contentUri: String,
    val mimeType: String?,
    val bitrate: Int?,
    val sampleRate: Int?,
    val dateAddedEpochSec: Long,
    val dateModifiedEpochSec: Long,
    val playCount: Long = 0L,
    val lastPlayedEpochSec: Long? = null,
    val isFavorite: Boolean = false,
    val bpm: Float? = null,
    val musicalKey: String? = null
)
