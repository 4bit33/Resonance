package com.resonance.player.core.model

/** Aggregated album view derived from songs. [artistName] is display text
 * (album artist, single artist, or Various Artists); [albumArtist] is the
 * stored grouping key (null for compilations without one). */
data class Album(
    val id: Long,
    val name: String,
    val artistName: String,
    val albumArtist: String?,
    val songCount: Int,
    val totalDurationMs: Long,
    val year: Int?,
    val artUri: String?
)

/** Aggregated artist view derived from songs. */
data class Artist(
    val id: Long,
    val name: String,
    val songCount: Int,
    val albumCount: Int
)

/** Genre view derived from song tags. */
data class Genre(
    val name: String,
    val songCount: Int
)
