package com.resonance.player.domain.search

import com.resonance.player.core.model.Song
import com.resonance.player.domain.library.MusicRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Local-only search over the indexed library (title / artist / album). */
class SearchLibraryUseCase(private val repository: MusicRepository) {
    operator fun invoke(query: String): Flow<List<Song>> {
        val q = query.trim()
        if (q.isEmpty()) return flowOf(emptyList())
        return repository.searchSongs(q)
    }
}
