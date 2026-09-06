package com.resonance.player.data.local

import android.os.Environment
import android.os.StatFs
import com.resonance.player.core.common.AppError
import com.resonance.player.core.common.Result
import com.resonance.player.core.database.ResonanceDatabase
import com.resonance.player.core.database.entity.FavoriteEntity
import com.resonance.player.core.database.toDomain
import com.resonance.player.core.model.Song
import com.resonance.player.domain.favorites.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import com.resonance.player.core.common.AppDispatchers

/**
 * Favorites backed by FavoriteDao. Toggle is idempotent; unknown ids simply
 * add (Remove of absent rows is a no-op in the DAO).
 */
class RoomFavoritesRepository(
    private val database: ResonanceDatabase,
    private val dispatchers: AppDispatchers,
    private val clock: () -> Long = { System.currentTimeMillis() / 1000L }
) : FavoritesRepository {

    override fun observeFavoriteIds(): Flow<List<Long>> =
        database.favoriteDao().observeFavoriteIds()

    override fun observeFavoriteSongs(): Flow<List<Song>> = combine(
        database.favoriteDao().observeFavoriteIds(),
        database.songDao().observeAllByTitle()
    ) { ids, songs ->
        val wanted = ids.toSet()
        songs.filter { it.id in wanted }.map { it.toDomain(isFavorite = true) }
    }

    override suspend fun toggle(songId: Long): Result<Boolean> = withContext(dispatchers.io) {
        try {
            val current = database.favoriteDao().getFavoriteIds().toSet()
            if (current.contains(songId)) {
                database.favoriteDao().remove(songId)
                Result.Success(false)
            } else {
                database.favoriteDao().add(FavoriteEntity(songId, clock()))
                Result.Success(true)
            }
        } catch (t: Exception) {
            Result.Failure(AppError.DatabaseError(t.message))
        }
    }

    override suspend fun isFavorite(songId: Long): Boolean = withContext(dispatchers.io) {
        try {
            database.favoriteDao().getFavoriteIds().contains(songId)
        } catch (t: Exception) {
            false
        }
    }
}

/** Device storage counters behind a seam (StatFs needs a real device). */
data class DeviceStorage(val usedBytes: Long, val totalBytes: Long)

/**
 * Reads primary external-storage counters via StatFs (no permission needed
 * for stats). Pure filesystem facts — no MediaStore, no estimates.
 */
class StorageStatsProvider {
    fun read(): DeviceStorage {
        return try {
            val path = Environment.getExternalStorageDirectory()
            val fs = StatFs(path.path)
            val blockSize = fs.blockSizeLong
            val total = blockSize * fs.blockCountLong
            val free = blockSize * fs.availableBlocksLong
            DeviceStorage(usedBytes = (total - free).coerceAtLeast(0L), totalBytes = total)
        } catch (t: Exception) {
            DeviceStorage(usedBytes = 0L, totalBytes = 0L)
        }
    }
}
