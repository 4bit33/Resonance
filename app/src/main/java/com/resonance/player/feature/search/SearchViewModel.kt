package com.resonance.player.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonance.player.core.common.Result
import com.resonance.player.core.model.Song
import com.resonance.player.domain.library.GetAlbumSongsUseCase
import com.resonance.player.domain.library.GetArtistSongsUseCase
import com.resonance.player.domain.library.GetGenreSongsUseCase
import com.resonance.player.domain.playback.PlaySongsUseCase
import com.resonance.player.domain.search.SearchAllUseCase
import com.resonance.player.domain.search.SearchLibraryUseCase
import com.resonance.player.domain.search.SearchResults
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Global search: debounced query in, categorized local results out.
 * Category rows play their songs through PlaySongsUseCase (albums /
 * artists / genres resolve via the matching getters).
 */
class SearchViewModel(
    searchLibrary: SearchLibraryUseCase,
    private val searchAll: SearchAllUseCase,
    private val playSongs: PlaySongsUseCase,
    private val getAlbumSongs: GetAlbumSongsUseCase,
    private val getArtistSongs: GetArtistSongsUseCase,
    private val getGenreSongs: GetGenreSongsUseCase
) : ViewModel() {
    private val query = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val results: StateFlow<List<Song>> = query
        .flatMapLatest { searchLibrary(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val grouped: StateFlow<SearchResults> = query
        .debounce(250L)
        .flatMapLatest { searchAll(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchResults())

    val currentQuery: StateFlow<String> = query

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun playFrom(songs: List<Song>, index: Int) {
        viewModelScope.launch { playSongs(songs, index) }
    }

    fun playAlbum(albumName: String, albumArtist: String?) {
        viewModelScope.launch {
            val songs = (getAlbumSongs(albumName, albumArtist) as? Result.Success)?.value
            if (!songs.isNullOrEmpty()) playSongs(songs, 0)
        }
    }

    fun playArtist(artistName: String) {
        viewModelScope.launch {
            val songs = (getArtistSongs(artistName) as? Result.Success)?.value
            if (!songs.isNullOrEmpty()) playSongs(songs, 0)
        }
    }

    fun playGenre(genreName: String) {
        viewModelScope.launch {
            val songs = (getGenreSongs(genreName) as? Result.Success)?.value
            if (!songs.isNullOrEmpty()) playSongs(songs, 0)
        }
    }
}
