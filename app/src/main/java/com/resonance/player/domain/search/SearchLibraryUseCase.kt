package com.resonance.player.domain.search

import com.resonance.player.core.common.Result
import com.resonance.player.core.model.Album
import com.resonance.player.core.model.Artist
import com.resonance.player.core.model.Genre
import com.resonance.player.core.model.Playlist
import com.resonance.player.core.model.Song
import com.resonance.player.domain.library.MusicRepository
import com.resonance.player.domain.playlists.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

/** Grouped global-search results; every category capped for fast display. */
data class SearchResults(
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val genres: List<Genre> = emptyList()
) {
    fun isEmpty(): Boolean =
        songs.isEmpty() && albums.isEmpty() && artists.isEmpty() &&
            playlists.isEmpty() && genres.isEmpty()
}

/** Local-only search over the indexed library (title / artist / album). */
class SearchLibraryUseCase(private val repository: MusicRepository) {
    operator fun invoke(query: String): Flow<List<Song>> {
        val q = query.trim()
        if (q.isEmpty()) return flowOf(emptyList())
        return repository.searchSongs(q)
    }
}

/** Global categorized search; blank queries short-circuit without DB hits. */
class SearchAllUseCase(
    private val music: MusicRepository,
    private val playlists: PlaylistRepository
) {
    operator fun invoke(query: String): Flow<SearchResults> {
        if (query.trim().isEmpty()) return flowOf(SearchResults())
        return combine(
            music.searchSongs(query),
            music.searchAlbums(query),
            music.searchArtists(query),
            playlists.searchPlaylists(query),
            music.searchGenres(query)
        ) { songs, albums, artists, playlistResults, genres ->
            SearchResults(songs, albums, artists, playlistResults, genres)
        }
    }
}
