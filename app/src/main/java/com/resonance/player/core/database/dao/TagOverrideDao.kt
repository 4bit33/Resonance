package com.resonance.player.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.resonance.player.core.database.entity.TagOverrideEntity

@Dao
interface TagOverrideDao {
    @Query("SELECT * FROM tag_overrides WHERE songId = :songId")
    suspend fun get(songId: Long): TagOverrideEntity?

    @Upsert
    suspend fun upsert(override: TagOverrideEntity)

    @Query("DELETE FROM tag_overrides WHERE songId = :songId")
    suspend fun delete(songId: Long)

    @Query(
        "UPDATE songs SET " +
            "title = COALESCE((SELECT o.title FROM tag_overrides o WHERE o.songId = songs.id), title), " +
            "artistName = COALESCE((SELECT o.artist FROM tag_overrides o WHERE o.songId = songs.id), artistName), " +
            "albumName = COALESCE((SELECT o.album FROM tag_overrides o WHERE o.songId = songs.id), albumName), " +
            "albumArtist = COALESCE((SELECT o.albumArtist FROM tag_overrides o WHERE o.songId = songs.id), albumArtist), " +
            "year = COALESCE((SELECT o.year FROM tag_overrides o WHERE o.songId = songs.id), year), " +
            "trackNumber = COALESCE((SELECT o.trackNumber FROM tag_overrides o WHERE o.songId = songs.id), trackNumber), " +
            "artworkKey = COALESCE((SELECT o.artworkKey FROM tag_overrides o WHERE o.songId = songs.id), artworkKey), " +
            "artworkUri = COALESCE((SELECT o.artworkUri FROM tag_overrides o WHERE o.songId = songs.id), artworkUri) " +
            "WHERE id IN (SELECT songId FROM tag_overrides)"
    )
    suspend fun applyAll()

    /** Makes the next scan re-read this song's file (its fingerprint no longer matches). */
    @Query("UPDATE songs SET dateModifiedEpochSec = 0 WHERE id = :songId")
    suspend fun markForReread(songId: Long)

    @Query("DELETE FROM genre_overrides WHERE songId = :songId")
    suspend fun deleteGenreOverride(songId: Long)

    /** Saves the fix and shows it at once. */
    @Transaction
    suspend fun save(override: TagOverrideEntity) {
        upsert(override)
        applyAll()
    }

    /** Drops every fix for the song; the caller rescans so the file's tags come back. */
    @Transaction
    suspend fun reset(songId: Long) {
        delete(songId)
        deleteGenreOverride(songId)
        markForReread(songId)
    }
}
