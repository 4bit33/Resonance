package com.resonance.player.data.local

import com.resonance.player.core.common.AppError
import com.resonance.player.core.common.AppDispatchers
import com.resonance.player.core.common.Result
import com.resonance.player.core.database.ResonanceDatabase
import com.resonance.player.core.database.entity.HistoryEntryEntity
import com.resonance.player.core.database.toDomain
import com.resonance.player.core.media.ScanReport
import com.resonance.player.core.model.Song
import com.resonance.player.domain.library.MusicRepository
import com.resonance.player.domain.library.SongSort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

/**
 * Real library repository backed by Room.
 * - Reads combine songs with favorite ids so [Song.isFavorite] is truthful.
 * - All work is confined to [AppDispatchers.io]; callers are main-safe.
 * - [scanAndImport] honestly reports FeatureUnavailable until the Phase 2
 *   MediaStore scanner lands (no fake scan, no demo data).
 */
class RoomMusicRepository(
    private val database: ResonanceDatabase,
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
            Result.Success(entity.toDomain())
        } catch (t: Exception) {
            Result.Failure(AppError.DatabaseError(t.message))
        }
    }

    override fun searchSongs(query: String): Flow<List<Song>> {
        val like = "%" + query.trim().replace("%", "\\%").replace("_", "\\_") + "%"
        return combine(
            database.songDao().search(like),
            database.favoriteDao().observeFavoriteIds()
        ) { entities, favorites ->
            val favoriteSet = favorites.toSet()
            entities.map { it.toDomain(isFavorite = favoriteSet.contains(it.id)) }
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

    override suspend fun scanAndImport(): Result<ScanReport> =
        Result.Failure(AppError.FeatureUnavailable("Media scanner (Phase 2)"))
}
