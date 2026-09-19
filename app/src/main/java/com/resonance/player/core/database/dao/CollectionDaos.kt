package com.resonance.player.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.resonance.player.core.database.entity.FavoriteEntity
import com.resonance.player.core.database.entity.HistoryEntryEntity
import com.resonance.player.core.database.entity.PlaylistEntity
import com.resonance.player.core.database.entity.PlaylistItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY updatedAtEpochSec DESC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE name LIKE :q ESCAPE '\\' ORDER BY name COLLATE NOCASE ASC LIMIT :limit")
    fun searchPlaylists(q: String, limit: Int): Flow<List<PlaylistEntity>>

    // Inner join: an item whose song left the library (source removed) is not shown, so it must not be counted.
    @Query(
        "SELECT pi.playlistId AS playlistId, COUNT(*) AS itemCount FROM playlist_items pi " +
            "INNER JOIN songs s ON s.id = pi.songId GROUP BY pi.playlistId"
    )
    fun observeItemCounts(): Flow<List<PlaylistCount>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylist(id: Long): PlaylistEntity?

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY position ASC")
    fun observeItems(playlistId: Long): Flow<List<PlaylistItemEntity>>

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getItemsOnce(playlistId: Long): List<PlaylistItemEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItems(items: List<PlaylistItemEntity>)

    @Query("UPDATE playlists SET name = :name, updatedAtEpochSec = :updatedAt WHERE id = :id")
    suspend fun renamePlaylist(id: Long, name: String, updatedAt: Long)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSong(playlistId: Long, songId: Long)
}

/** Playlist item counts for list subtitles (GROUP BY, no full loads). */
data class PlaylistCount(
    val playlistId: Long,
    val itemCount: Int
)

@Dao
interface FavoriteDao {
    @Query("SELECT songId FROM favorites")
    fun observeFavoriteIds(): Flow<List<Long>>

    @Query("SELECT songId FROM favorites")
    suspend fun getFavoriteIds(): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE songId = :songId")
    suspend fun remove(songId: Long)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM playback_history ORDER BY playedAtEpochSec DESC LIMIT :limit")
    fun observeRecent(limit: Int = 500): Flow<List<HistoryEntryEntity>>

    @Insert
    suspend fun insert(entry: HistoryEntryEntity): Long

    @Query("DELETE FROM playback_history WHERE playedAtEpochSec < :olderThanSec")
    suspend fun pruneOlderThan(olderThanSec: Long)
}
