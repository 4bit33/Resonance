package com.resonance.player.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "playlists",
    indices = [Index(value = ["name"], unique = true)]
)
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val createdAtEpochSec: Long,
    val updatedAtEpochSec: Long
)

@Entity(
    tableName = "playlist_items",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["playlistId", "position"]), Index(value = ["songId"])]
)
data class PlaylistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val playlistId: Long,
    val songId: Long,
    val position: Int
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val songId: Long,
    val addedAtEpochSec: Long
)

@Entity(
    tableName = "playback_history",
    indices = [Index(value = ["songId"]), Index(value = ["playedAtEpochSec"])]
)
data class HistoryEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val songId: Long,
    val playedAtEpochSec: Long,
    val completed: Boolean
)
