package com.resonance.player.core.model

/** User playlist header. Items live in [PlaylistItem] ordered by [position]. */
data class Playlist(
    val id: Long,
    val name: String,
    val createdAtEpochSec: Long,
    val updatedAtEpochSec: Long,
    val itemCount: Int = 0
)

data class PlaylistItem(
    val id: Long,
    val playlistId: Long,
    val songId: Long,
    val position: Int
)

/** A browsable storage folder containing audio. */
data class MusicFolder(
    val path: String,
    val name: String,
    val songCount: Int
)

data class Favorite(
    val songId: Long,
    val addedAtEpochSec: Long
)

data class PlaybackHistoryEntry(
    val id: Long,
    val songId: Long,
    val playedAtEpochSec: Long,
    val completed: Boolean
)
