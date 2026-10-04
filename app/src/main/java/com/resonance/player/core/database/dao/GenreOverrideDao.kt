package com.resonance.player.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.resonance.player.core.database.entity.GenreOverrideEntity

@Dao
interface GenreOverrideDao {
    @Upsert
    suspend fun upsert(overrides: List<GenreOverrideEntity>)

    @Query("SELECT * FROM genre_overrides WHERE songId = :songId")
    suspend fun get(songId: Long): GenreOverrideEntity?

    @Query("DELETE FROM genre_overrides WHERE songId = :songId")
    suspend fun delete(songId: Long)

    @Query("UPDATE songs SET genreName = :genre WHERE id IN (:songIds)")
    suspend fun setSongGenre(songIds: List<Long>, genre: String?)

    /** Sets the genre now and remembers it for later scans. */
    @Transaction
    suspend fun override(songIds: List<Long>, genre: String?) {
        upsert(songIds.map { GenreOverrideEntity(it, genre) })
        setSongGenre(songIds, genre)
    }

    /** Re-applies every override after a scan wrote the tags' genres back. */
    @Query(
        "UPDATE songs SET genreName = (SELECT genre FROM genre_overrides WHERE songId = songs.id) " +
            "WHERE id IN (SELECT songId FROM genre_overrides)"
    )
    suspend fun applyAll()
}
