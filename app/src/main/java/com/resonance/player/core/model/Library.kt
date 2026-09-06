package com.resonance.player.core.model

/** Aggregated album view derived from songs; cache table optional in later phases. */
data class Album(
    val id: Long,
    val name: String,
    val artistName: String,
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
