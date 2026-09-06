package com.resonance.player.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.resonance.player.core.database.entity.SongEntity
import kotlinx.coroutines.flow.Flow

/** Lightweight row for incremental diffing (never full entities). */
data class ScanFingerprint(
    val id: Long,
    val mediaStoreId: Long,
    val volumeName: String,
    val dateModifiedEpochSec: Long,
    val fileSizeBytes: Long,
    val playCount: Long,
    val lastPlayedEpochSec: Long?,
    val artworkKey: String?
)

/** Album grouping with compilation detection support. */
data class AlbumRow(
    val albumName: String,
    val albumArtist: String?,
    val songCount: Int,
    val totalDurationMs: Long,
    val year: Int?,
    val distinctArtists: Int,
    val sampleArtist: String?,
    val sampleArtworkUri: String?
)

data class ArtistRow(
    val artistName: String,
    val songCount: Int,
    val albumCount: Int
)

data class GenreRow(
    val genreName: String,
    val songCount: Int
)

data class FolderRow(
    val relativePath: String?,
    val songCount: Int
)

/** Cheap aggregate for the Home storage card (COUNT + SUM only). */
data class StorageTotals(
    val trackCount: Int,
    val totalBytes: Long
)

/**
 * Paged-style access only: every read is a Flow or a suspend lookup.
 * Nothing here ever loads the whole collection into memory at once;
 * screens collect with Lazy layouts.
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
        "SELECT * FROM songs WHERE lastPlayedEpochSec IS NOT NULL AND lastPlayedEpochSec > 0 " +
            "ORDER BY lastPlayedEpochSec DESC LIMIT :limit"
    )
    fun observeRecentlyPlayedLimited(limit: Int): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY playCount DESC LIMIT :limit")
    fun observeMostPlayedLimited(limit: Int): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY dateAddedEpochSec DESC LIMIT :limit")
    fun observeRecentlyAddedLimited(limit: Int): Flow<List<SongEntity>>

    @Query("SELECT COUNT(*) AS trackCount, COALESCE(SUM(fileSizeBytes), 0) AS totalBytes FROM songs")
    fun observeStorageTotals(): Flow<StorageTotals>

    @Query(
        "SELECT * FROM songs WHERE title LIKE :q ESCAPE '\\' OR artistName LIKE :q ESCAPE '\\' " +
            "OR albumName LIKE :q ESCAPE '\\' OR albumArtist LIKE :q ESCAPE '\\' " +
            "OR genreName LIKE :q ESCAPE '\\' " +
            "ORDER BY playCount DESC LIMIT 200"
    )
    fun search(q: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getById(id: Long): SongEntity?

    @Query("SELECT * FROM songs WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<SongEntity>

    @Query("SELECT * FROM songs WHERE mediaStoreId = :mediaStoreId AND volumeName = :volumeName")
    suspend fun getByMediaKey(mediaStoreId: Long, volumeName: String): SongEntity?

    @Query("SELECT id, mediaStoreId, volumeName, dateModifiedEpochSec, fileSizeBytes, playCount, lastPlayedEpochSec, artworkKey FROM songs")
    suspend fun getFingerprints(): List<ScanFingerprint>

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM (SELECT 1 FROM songs GROUP BY albumName, albumArtist)")
    suspend fun countAlbums(): Int

    @Query("SELECT COUNT(*) FROM (SELECT 1 FROM songs GROUP BY artistName)")
    suspend fun countArtists(): Int

    @Query("SELECT COUNT(*) FROM (SELECT 1 FROM songs WHERE genreName IS NOT NULL AND genreName != '' GROUP BY genreName)")
    suspend fun countGenres(): Int

    @Query(
        "SELECT albumName, albumArtist, COUNT(*) AS songCount, " +
            "SUM(durationMs) AS totalDurationMs, MAX(year) AS year, " +
            "COUNT(DISTINCT artistName) AS distinctArtists, " +
            "MIN(artistName) AS sampleArtist, MAX(artworkUri) AS sampleArtworkUri " +
            "FROM songs GROUP BY albumName COLLATE NOCASE, albumArtist COLLATE NOCASE " +
            "ORDER BY albumName COLLATE NOCASE ASC"
    )
    fun observeAlbumGroups(): Flow<List<AlbumRow>>

    @Query(
        "SELECT artistName, COUNT(*) AS songCount, " +
            "COUNT(DISTINCT albumName) AS albumCount FROM songs " +
            "GROUP BY artistName COLLATE NOCASE ORDER BY artistName COLLATE NOCASE ASC"
    )
    fun observeArtistGroups(): Flow<List<ArtistRow>>

    @Query(
        "SELECT genreName, COUNT(*) AS songCount FROM songs " +
            "WHERE genreName IS NOT NULL AND genreName != '' " +
            "GROUP BY genreName COLLATE NOCASE ORDER BY genreName COLLATE NOCASE ASC"
    )
    fun observeGenreGroups(): Flow<List<GenreRow>>

    @Query(
        "SELECT relativePath, COUNT(*) AS songCount FROM songs " +
            "GROUP BY relativePath ORDER BY relativePath ASC"
    )
    fun observeFolderGroups(): Flow<List<FolderRow>>

    @Query(
        "SELECT albumName, albumArtist, COUNT(*) AS songCount, " +
            "SUM(durationMs) AS totalDurationMs, MAX(year) AS year, " +
            "COUNT(DISTINCT artistName) AS distinctArtists, " +
            "MIN(artistName) AS sampleArtist, MAX(artworkUri) AS sampleArtworkUri " +
            "FROM songs WHERE albumName LIKE :q ESCAPE '\\' " +
            "GROUP BY albumName COLLATE NOCASE, albumArtist COLLATE NOCASE " +
            "ORDER BY albumName COLLATE NOCASE ASC LIMIT :limit"
    )
    fun searchAlbumGroups(q: String, limit: Int): Flow<List<AlbumRow>>

    @Query(
        "SELECT artistName, COUNT(*) AS songCount, " +
            "COUNT(DISTINCT albumName) AS albumCount FROM songs " +
            "WHERE artistName LIKE :q ESCAPE '\\' " +
            "GROUP BY artistName COLLATE NOCASE ORDER BY artistName COLLATE NOCASE ASC LIMIT :limit"
    )
    fun searchArtistGroups(q: String, limit: Int): Flow<List<ArtistRow>>

    @Query(
        "SELECT genreName, COUNT(*) AS songCount FROM songs " +
            "WHERE genreName LIKE :q ESCAPE '\\' " +
            "GROUP BY genreName COLLATE NOCASE ORDER BY genreName COLLATE NOCASE ASC LIMIT :limit"
    )
    fun searchGenreGroups(q: String, limit: Int): Flow<List<GenreRow>>

    @Query("SELECT * FROM songs WHERE artistName = :artistName ORDER BY title COLLATE NOCASE ASC")
    suspend fun getSongsOfArtist(artistName: String): List<SongEntity>

    @Query("SELECT * FROM songs WHERE genreName = :genreName ORDER BY title COLLATE NOCASE ASC")
    suspend fun getSongsOfGenre(genreName: String): List<SongEntity>

    @Query(
        "SELECT * FROM songs WHERE albumName = :albumName " +
            "AND ((albumArtist IS NULL AND :albumArtist IS NULL) OR albumArtist = :albumArtist) " +
            "ORDER BY COALESCE(discNumber, 1) ASC, COALESCE(trackNumber, 9999) ASC, " +
            "title COLLATE NOCASE ASC"
    )
    suspend fun getSongsOfAlbum(albumName: String, albumArtist: String?): List<SongEntity>

    @Query("SELECT DISTINCT artworkKey FROM songs WHERE artworkKey IS NOT NULL")
    suspend fun getReferencedArtworkKeys(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(songs: List<SongEntity>)

    @Upsert
    suspend fun upsertBatch(songs: List<SongEntity>)


    @Query("UPDATE songs SET playCount = playCount + 1, lastPlayedEpochSec = :nowSec WHERE id = :id")
    suspend fun incrementPlayCount(id: Long, nowSec: Long)

    @Query("DELETE FROM songs WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM songs WHERE id NOT IN (:keepIds)")
    suspend fun pruneMissing(keepIds: List<Long>)

    @Query("DELETE FROM songs")
    suspend fun clearAll()
}
