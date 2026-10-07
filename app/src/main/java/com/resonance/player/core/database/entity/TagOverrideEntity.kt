package com.resonance.player.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tags the user fixed by hand or from an online lookup. Like genre overrides,
 * files are never rewritten: every non-null field is laid over the file's
 * tags after each scan, so fixes survive "Refresh library". A cover picked
 * here lives in app-private storage ([artworkKey] / [artworkUri]).
 */
@Entity(tableName = "tag_overrides")
data class TagOverrideEntity(
    @PrimaryKey val songId: Long,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val albumArtist: String? = null,
    val year: Int? = null,
    val trackNumber: Int? = null,
    val artworkKey: String? = null,
    val artworkUri: String? = null
)
