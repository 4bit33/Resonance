package com.resonance.player.data.local

import android.database.sqlite.SQLiteConstraintException
import com.resonance.player.core.common.AppDispatchers
import com.resonance.player.core.common.AppError
import com.resonance.player.core.common.Result
import com.resonance.player.core.database.ResonanceDatabase
import com.resonance.player.core.database.entity.PlaylistEntity
import com.resonance.player.core.database.entity.PlaylistItemEntity
import com.resonance.player.core.database.toDomain
import com.resonance.player.core.database.toDomain
import com.resonance.player.core.model.Playlist
import com.resonance.player.core.model.PlaylistItem
import com.resonance.player.core.model.Song
import com.resonance.player.domain.playlists.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Playlists backed by Room. Item order is positional (0..n); moves rewrite
 * positions in one upsert. Deleting a playlist cascades its items via FK;
 * deleting songs never touches playlists (no song FK exists by design).
 */
class RoomPlaylistRepository(
    private val database: ResonanceDatabase,
    private val dispatchers: AppDispatchers,
    private val clock: () -> Long = { System.currentTimeMillis() / 1000L }
) : PlaylistRepository {

    override fun observePlaylists(): Flow<List<Playlist>> = combine(
        database.playlistDao().observePlaylists(),
        database.playlistDao().observeItemCounts()
    ) { playlists, counts ->
        val countById = counts.associate { it.playlistId to it.itemCount }
        playlists.map { it.toDomain(countById[it.id] ?: 0) }
    }

    override fun observeItems(playlistId: Long): Flow<List<PlaylistItem>> =
        database.playlistDao().observeItems(playlistId).map { rows ->
            rows.map { it.toDomain() }
        }

    override fun searchPlaylists(query: String): Flow<List<Playlist>> {
        val like = "%" + query.trim().replace("%", "\\%").replace("_", "\\_") + "%"
        return combine(
            database.playlistDao().searchPlaylists(like, SEARCH_LIMIT),
            database.playlistDao().observeItemCounts()
        ) { rows, counts ->
            val countById = counts.associate { it.playlistId to it.itemCount }
            rows.map { it.toDomain(countById[it.id] ?: 0) }
        }
    }

    private companion object {
        const val SEARCH_LIMIT = 8
    }

    override suspend fun create(name: String): Result<Playlist> = withContext(dispatchers.io) {
        try {
            val now = clock()
            val id = database.playlistDao()
                .insertPlaylist(PlaylistEntity(name = name, createdAtEpochSec = now, updatedAtEpochSec = now))
            val created = database.playlistDao().getPlaylist(id)
                ?: return@withContext Result.Failure(AppError.DatabaseError("Playlist was not created"))
            Result.Success(created.toDomain(0))
        } catch (t: SQLiteConstraintException) {
            Result.Failure(AppError.InvalidMetadata("A playlist with this name exists"))
        } catch (t: Exception) {
            Result.Failure(AppError.DatabaseError(t.message))
        }
    }

    override suspend fun delete(playlistId: Long): Result<Unit> = withContext(dispatchers.io) {
        try {
            database.playlistDao().deletePlaylist(playlistId)
            Result.Success(Unit)
        } catch (t: Exception) {
            Result.Failure(AppError.DatabaseError(t.message))
        }
    }

    override suspend fun addSong(playlistId: Long, songId: Long): Result<Unit> =
        withContext(dispatchers.io) {
            try {
                val dao = database.playlistDao()
                if (dao.getPlaylist(playlistId) == null) {
                    return@withContext Result.Failure(AppError.Unknown("Playlist not found"))
                }
                val items = dao.getItemsOnce(playlistId)
                if (items.any { it.songId == songId }) return@withContext Result.Success(Unit)
                val next = (items.maxOfOrNull { it.position } ?: -1) + 1
                dao.upsertItems(listOf(PlaylistItemEntity(playlistId = playlistId, songId = songId, position = next)))
                touch(playlistId)
                Result.Success(Unit)
            } catch (t: Exception) {
                Result.Failure(AppError.DatabaseError(t.message))
            }
        }

    override suspend fun removeSong(playlistId: Long, songId: Long): Result<Unit> =
        withContext(dispatchers.io) {
            try {
                database.playlistDao().removeSong(playlistId, songId)
                touch(playlistId)
                Result.Success(Unit)
            } catch (t: Exception) {
                Result.Failure(AppError.DatabaseError(t.message))
            }
        }

    override suspend fun rename(playlistId: Long, name: String): Result<Unit> =
        withContext(dispatchers.io) {
            try {
                val dao = database.playlistDao()
                if (dao.getPlaylist(playlistId) == null) {
                    return@withContext Result.Failure(AppError.Unknown("Playlist not found"))
                }
                dao.renamePlaylist(playlistId, name, clock())
                Result.Success(Unit)
            } catch (t: SQLiteConstraintException) {
                Result.Failure(AppError.InvalidMetadata("A playlist with this name exists"))
            } catch (t: Exception) {
                Result.Failure(AppError.DatabaseError(t.message))
            }
        }

    override suspend fun moveItem(playlistId: Long, fromPosition: Int, toPosition: Int): Result<Unit> =
        withContext(dispatchers.io) {
            try {
                val dao = database.playlistDao()
                val items = dao.getItemsOnce(playlistId).sortedBy { it.position }.toMutableList()
                if (fromPosition !in items.indices || toPosition !in items.indices) {
                    return@withContext Result.Failure(AppError.Unknown("Invalid playlist position"))
                }
                if (fromPosition == toPosition) return@withContext Result.Success(Unit)
                val moved = items.removeAt(fromPosition)
                items.add(toPosition, moved)
                dao.upsertItems(items.mapIndexed { index, item -> item.copy(position = index) })
                touch(playlistId)
                Result.Success(Unit)
            } catch (t: Exception) {
                Result.Failure(AppError.DatabaseError(t.message))
            }
        }

    override suspend fun getPlaylistSongs(playlistId: Long): Result<List<Song>> =
        withContext(dispatchers.io) {
            try {
                val items = database.playlistDao().getItemsOnce(playlistId)
                    .sortedBy { it.position }
                // An empty playlist is a valid state, not a "library is empty" error.
                if (items.isEmpty()) return@withContext Result.Success(emptyList())
                val byId = database.songDao().getByIds(items.map { it.songId })
                    .associateBy { it.id }
                val favorites = database.favoriteDao().getFavoriteIds().toSet()
                Result.Success(
                    items.mapNotNull { byId[it.songId]?.toDomain(isFavorite = favorites.contains(it.songId)) }
                )
            } catch (t: Exception) {
                Result.Failure(AppError.DatabaseError(t.message))
            }
        }

    private suspend fun touch(playlistId: Long) {
        val current = database.playlistDao().getPlaylist(playlistId) ?: return
        database.playlistDao().renamePlaylist(playlistId, current.name, clock())
    }
}

private fun PlaylistEntity.toDomain(itemCount: Int): Playlist =
    Playlist(
        id = id,
        name = name,
        createdAtEpochSec = createdAtEpochSec,
        updatedAtEpochSec = updatedAtEpochSec,
        itemCount = itemCount
    )

private fun PlaylistItemEntity.toDomain(): PlaylistItem =
    PlaylistItem(
        id = id,
        playlistId = playlistId,
        songId = songId,
        position = position
    )
