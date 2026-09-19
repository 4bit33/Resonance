package com.resonance.player.data.local

import com.resonance.player.core.common.AppError
import com.resonance.player.core.common.AppDispatchers
import com.resonance.player.core.common.Result
import com.resonance.player.core.database.ResonanceDatabase
import com.resonance.player.core.database.entity.SongEntity
import com.resonance.player.core.model.StorageOverview
import com.resonance.player.core.database.entity.HistoryEntryEntity
import com.resonance.player.core.database.toDomain
import com.resonance.player.core.media.AudioScanner
import com.resonance.player.core.media.ScanReport
import com.resonance.player.core.media.ScanState
import com.resonance.player.core.model.Album
import com.resonance.player.core.model.Artist
import com.resonance.player.core.model.Genre
import com.resonance.player.core.model.LibraryStats
import com.resonance.player.core.model.MusicFolder
import com.resonance.player.core.model.Song
import com.resonance.player.domain.library.MusicRepository
import com.resonance.player.domain.library.SongSort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Real library repository backed by Room.
 * - Reads combine songs with favorite ids so [Song.isFavorite] is truthful.
 * - All work is confined to [AppDispatchers.io]; callers are main-safe.
 * - [scanAndImport] delegates to the real incremental [AudioScanner].
 * - Browse aggregates (albums/artists/genres/folders) come from GROUP BY
 *   queries — counts and lists without loading full tables.
 */
class RoomMusicRepository(
    private val database: ResonanceDatabase,
    private val scanner: AudioScanner,
    private val prefs: LibraryPreferences,
    private val storageStats: StorageStatsProvider,
    private val dispatchers: AppDispatchers,
    private val clock: () -> Long = { System.currentTimeMillis() / 1000L }
) : MusicRepository {

    override fun observeSongs(sort: SongSort): Flow<List<Song>> {
        val songs = when (sort) {
            SongSort.TITLE -> database.songDao().observeAllByTitle()
            SongSort.ARTIST -> database.songDao().observeAllByArtist()
            SongSort.ALBUM -> database.songDao().observeAllByAlbum()
            SongSort.DATE_ADDED -> database.songDao().observeAllByDateAdded()
            SongSort.LAST_PLAYED -> database.songDao().observeRecentlyPlayed()
            SongSort.PLAY_COUNT -> database.songDao().observeMostPlayed()
        }
        return combine(songs, database.favoriteDao().observeFavoriteIds()) { entities, favorites ->
            val favoriteSet = favorites.toSet()
            entities.map { it.toDomain(isFavorite = favoriteSet.contains(it.id)) }
        }
    }

    override suspend fun getSong(id: Long): Result<Song> = withContext(dispatchers.io) {
        try {
            val entity = database.songDao().getById(id)
                ?: return@withContext Result.Failure(AppError.MissingFile("song id=$id"))
            val favorites = database.favoriteDao().getFavoriteIds().toSet()
            Result.Success(entity.toDomain(isFavorite = favorites.contains(id)))
        } catch (t: Exception) {
            Result.Failure(AppError.DatabaseError(t.message))
        }
    }

    override fun searchSongs(query: String): Flow<List<Song>> {
        val like = likePattern(query)
        return combine(
            database.songDao().search(like),
            database.favoriteDao().observeFavoriteIds()
        ) { entities, favorites ->
            val favoriteSet = favorites.toSet()
            entities.map { it.toDomain(isFavorite = favoriteSet.contains(it.id)) }
        }
    }

    override fun searchAlbums(query: String): Flow<List<Album>> =
        database.songDao().searchAlbumGroups(likePattern(query), SEARCH_LIMIT)
            .map { rows -> rows.map { it.toDomain() } }

    override fun searchArtists(query: String): Flow<List<Artist>> =
        database.songDao().searchArtistGroups(likePattern(query), SEARCH_LIMIT)
            .map { rows -> rows.map { it.toDomain() } }

    override fun searchGenres(query: String): Flow<List<Genre>> =
        database.songDao().searchGenreGroups(likePattern(query), SEARCH_LIMIT)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun getArtistSongs(artistName: String): Result<List<Song>> =
        withContext(dispatchers.io) {
            try {
                val entities = database.songDao().getSongsOfArtist(artistName)
                if (entities.isEmpty()) {
                    return@withContext Result.Failure(AppError.EmptyLibrary)
                }
                val favorites = database.favoriteDao().getFavoriteIds().toSet()
                Result.Success(entities.map { it.toDomain(isFavorite = favorites.contains(it.id)) })
            } catch (t: Exception) {
                Result.Failure(AppError.DatabaseError(t.message))
            }
        }

    override suspend fun getGenreSongs(genreName: String): Result<List<Song>> =
        withContext(dispatchers.io) {
            try {
                val entities = database.songDao().getSongsOfGenre(genreName)
                if (entities.isEmpty()) {
                    return@withContext Result.Failure(AppError.EmptyLibrary)
                }
                val favorites = database.favoriteDao().getFavoriteIds().toSet()
                Result.Success(entities.map { it.toDomain(isFavorite = favorites.contains(it.id)) })
            } catch (t: Exception) {
                Result.Failure(AppError.DatabaseError(t.message))
            }
        }

    override suspend fun recordPlay(songId: Long, completed: Boolean): Result<Unit> =
        withContext(dispatchers.io) {
            try {
                val now = clock()
                database.songDao().incrementPlayCount(songId, now)
                database.historyDao().insert(
                    HistoryEntryEntity(songId = songId, playedAtEpochSec = now, completed = completed)
                )
                Result.Success(Unit)
            } catch (t: Exception) {
                Result.Failure(AppError.DatabaseError(t.message))
            }
        }

    override suspend fun scanAndImport(): Result<ScanReport> = scanner.scanLibrary()

    override fun observeScanState(): Flow<ScanState> = scanner.state

    override fun observeAlbums(): Flow<List<Album>> =
        database.songDao().observeAlbumGroups().map { rows -> rows.map { it.toDomain() } }

    override fun observeArtists(): Flow<List<Artist>> =
        database.songDao().observeArtistGroups().map { rows -> rows.map { it.toDomain() } }

    override fun observeGenres(): Flow<List<Genre>> =
        database.songDao().observeGenreGroups().map { rows -> rows.map { it.toDomain() } }

    override fun observeFolders(): Flow<List<MusicFolder>> =
        database.songDao().observeFolderGroups().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getAlbumSongs(albumName: String, albumArtist: String?): Result<List<Song>> =
        withContext(dispatchers.io) {
            try {
                val entities = database.songDao().getSongsOfAlbum(albumName, albumArtist)
                if (entities.isEmpty()) {
                    return@withContext Result.Failure(AppError.EmptyLibrary)
                }
                val favorites = database.favoriteDao().getFavoriteIds().toSet()
                Result.Success(entities.map { it.toDomain(isFavorite = favorites.contains(it.id)) })
            } catch (t: Exception) {
                Result.Failure(AppError.DatabaseError(t.message))
            }
        }

    override suspend fun getFolderSongs(relativePath: String?): Result<List<Song>> =
        withContext(dispatchers.io) {
            try {
                val entities = database.songDao().getSongsOfFolder(relativePath)
                if (entities.isEmpty()) {
                    return@withContext Result.Failure(AppError.EmptyLibrary)
                }
                val favorites = database.favoriteDao().getFavoriteIds().toSet()
                Result.Success(entities.map { it.toDomain(isFavorite = favorites.contains(it.id)) })
            } catch (t: Exception) {
                Result.Failure(AppError.DatabaseError(t.message))
            }
        }

    override suspend fun getLibraryStats(): LibraryStats = withContext(dispatchers.io) {
        val dao = database.songDao()
        val lastScan: Long? = try {
            prefs.lastScanEpochSec.first()
        } catch (t: Exception) {
            null
        }
        LibraryStats(
            songCount = dao.count(),
            albumCount = dao.countAlbums(),
            artistCount = dao.countArtists(),
            genreCount = dao.countGenres(),
            lastScanEpochSec = lastScan
        )
    }

    override fun observeLastScanEpochSec(): Flow<Long?> = prefs.lastScanEpochSec

    private fun likePattern(query: String): String =
        "%" + query.trim().replace("%", "\\%").replace("_", "\\_") + "%"

    private companion object {
        const val SEARCH_LIMIT = 8
    }

    private fun withFavorites(
        songs: Flow<List<SongEntity>>
    ): Flow<List<Song>> = combine(
        songs,
        database.favoriteDao().observeFavoriteIds()
    ) { entities, favorites ->
        val favoriteSet = favorites.toSet()
        entities.map { it.toDomain(isFavorite = favoriteSet.contains(it.id)) }
    }

    override fun observeRecentlyPlayed(limit: Int): Flow<List<Song>> =
        withFavorites(database.songDao().observeRecentlyPlayedLimited(limit))

    override fun observeMostPlayed(limit: Int): Flow<List<Song>> =
        withFavorites(database.songDao().observeMostPlayedLimited(limit))

    override fun observeRecentlyAdded(limit: Int): Flow<List<Song>> =
        withFavorites(database.songDao().observeRecentlyAddedLimited(limit))

    override fun observeStorageOverview(): Flow<StorageOverview> =
        database.songDao().observeStorageTotals().map { totals ->
            val device = try {
                storageStats.read()
            } catch (t: Exception) {
                DeviceStorage(usedBytes = 0L, totalBytes = 0L)
            }
            StorageOverview(
                trackCount = totals.trackCount,
                libraryBytes = totals.totalBytes,
                deviceUsedBytes = device.usedBytes,
                deviceTotalBytes = device.totalBytes
            )
        }.flowOn(dispatchers.io)
}
