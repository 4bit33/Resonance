package com.resonance.player.core.model

/** Cheap aggregate counts for Settings/Library headers (COUNT queries only). */
data class LibraryStats(
    val songCount: Int,
    val albumCount: Int,
    val artistCount: Int,
    val genreCount: Int,
    val lastScanEpochSec: Long?
)
