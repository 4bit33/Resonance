package com.resonance.player.domain.favorites

import com.resonance.player.core.common.Result
import com.resonance.player.core.model.Song
import kotlinx.coroutines.flow.Flow

/**
 * Favorites repository contract. Favorites are a thin id-set over the
 * library (FavoriteDao); song rows stay untouched.
 */
interface FavoritesRepository {
    fun observeFavoriteIds(): Flow<List<Long>>
    fun observeFavoriteSongs(): Flow<List<Song>>
    suspend fun toggle(songId: Long): Result<Boolean>
    suspend fun isFavorite(songId: Long): Boolean
}

/** Toggles favorite state; returns the NEW state (true = now favorite). */
class ToggleFavoriteUseCase(private val repository: FavoritesRepository) {
    suspend operator fun invoke(songId: Long): Result<Boolean> = repository.toggle(songId)
}

class ObserveFavoriteSongsUseCase(private val repository: FavoritesRepository) {
    operator fun invoke(): Flow<List<Song>> = repository.observeFavoriteSongs()
}

class ObserveFavoriteIdsUseCase(private val repository: FavoritesRepository) {
    operator fun invoke(): Flow<List<Long>> = repository.observeFavoriteIds()
}
