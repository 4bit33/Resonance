package com.resonance.player.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A genre the user set by hand. Files are never rewritten: the override is
 * applied over whatever the tags say after every scan, so it survives
 * "Refresh library". [genre] null = the user cleared the genre.
 */
@Entity(tableName = "genre_overrides")
data class GenreOverrideEntity(
    @PrimaryKey val songId: Long,
    val genre: String?
)
