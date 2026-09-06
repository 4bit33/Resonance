package com.resonance.player.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.resonance.player.core.database.entity.SongEntity
import kotlinx.coroutines.flow.Flow

/**
 * Paged-style access only: every read is a Flow or a suspend lookup.
 * Nothing here ever loads the whole collection into memory at once;
 * screens collect with Lazy layouts + paging in later phases.
 */
@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE ASC")
    fun observeAllByTitle(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY artistName COLLATE NOCASE ASC, title COLLATE NOCASE ASC")
    fun observeAllByArtist(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY albumName COLLATE NOCASE ASC, trackNumber ASC")
    fun observeAllByAlbum(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY dateAddedEpochSec DESC")
    fun observeAllByDateAdded(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY lastPlayedEpochSec DESC")
    fun observeRecentlyPlayed(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY playCount DESC")
    fun observeMostPlayed(): Flow<List<SongEntity>>

    @Query(
        "SELECT * FROM songs WHERE title LIKE :q OR artistName LIKE :q OR albumName LIKE :q " +
            "ORDER BY playCount DESC LIMIT 200"
    )
    fun search(q: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getById(id: Long): SongEntity?

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(songs: List<SongEntity>)

    @Query("UPDATE songs SET playCount = playCount + 1, lastPlayedEpochSec = :nowSec WHERE id = :id")
    suspend fun incrementPlayCount(id: Long, nowSec: Long)

    @Query("DELETE FROM songs WHERE id NOT IN (:keepIds)")
    suspend fun pruneMissing(keepIds: List<Long>)

    @Query("DELETE FROM songs")
    suspend fun clearAll()
}
