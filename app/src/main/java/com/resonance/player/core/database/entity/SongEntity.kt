package com.resonance.player.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Cached song row. Primary key mirrors the MediaStore audio id captured at
 * import time (stable identifier). Query indexes match the spec: title,
 * artist, album, path, date added, last played, play count.
 */
@Entity(
    tableName = "songs",
    indices = [
        Index(value = ["title"]),
        Index(value = ["artistName"]),
        Index(value = ["albumName"]),
        Index(value = ["path"]),
        Index(value = ["mediaStoreId"]),
        Index(value = ["dateAddedEpochSec"]),
        Index(value = ["lastPlayedEpochSec"]),
        Index(value = ["playCount"])
    ]
)
data class SongEntity(
    @PrimaryKey val id: Long,
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
    val bpm: Float? = null,
    val musicalKey: String? = null
)
