package com.resonance.player.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A song the user removed from the library that lives inside an added FOLDER
 * (a folder cannot be edited, so the scanner must skip the song from now on).
 * Keyed by the stable song id. Rows die with their source (ON DELETE CASCADE):
 * removing the folder and adding it again brings the song back.
 */
@Entity(
    tableName = "excluded_songs",
    foreignKeys = [
        ForeignKey(
            entity = SourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sourceId"])]
)
data class ExcludedSongEntity(
    @PrimaryKey val id: Long,
    val sourceId: Long
)
