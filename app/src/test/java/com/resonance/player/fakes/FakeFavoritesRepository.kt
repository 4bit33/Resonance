package com.resonance.player.fakes

import com.resonance.player.core.common.Result
import com.resonance.player.core.model.Song
import com.resonance.player.domain.favorites.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** In-memory favorites fake: toggle flips membership, songs resolve from a fixed list. */
class FakeFavoritesRepository(
    songs: List<Song> = listOf(testSong(1L), testSong(2L)),
    initialFavorites: Set<Long> = emptySet()
) : FavoritesRepository {
    private val backing = songs
    private val ids = MutableStateFlow(initialFavorites)

    override fun observeFavoriteIds(): Flow<List<Long>> = ids.map { it.toList() }

    override fun observeFavoriteSongs(): Flow<List<Song>> =
        combine(ids, flowOf(backing)) { wanted, all ->
            all.filter { it.id in wanted }.map { it.copy(isFavorite = true) }
        }

    override suspend fun toggle(songId: Long): Result<Boolean> {
        val next = ids.value.toMutableSet()
        val nowFavorite = if (next.contains(songId)) {
            next.remove(songId)
            false
        } else {
            next.add(songId)
            true
        }
        ids.value = next
        return Result.Success(nowFavorite)
    }

    override suspend fun isFavorite(songId: Long): Boolean = ids.value.contains(songId)
}
