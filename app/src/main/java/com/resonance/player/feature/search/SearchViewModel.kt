package com.resonance.player.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonance.player.core.common.Result
import com.resonance.player.core.common.userMessage
import com.resonance.player.core.model.Playlist
import com.resonance.player.core.model.Song
import com.resonance.player.domain.library.GetAlbumSongsUseCase
import com.resonance.player.domain.library.GetArtistSongsUseCase
import com.resonance.player.domain.library.GetGenreSongsUseCase
import com.resonance.player.domain.playback.AppendToQueueUseCase
import com.resonance.player.domain.playback.PlayNextUseCase
import com.resonance.player.domain.playback.PlaySongsUseCase
import com.resonance.player.domain.playlists.AddSongToPlaylistUseCase
import com.resonance.player.domain.playlists.CreatePlaylistUseCase
import com.resonance.player.domain.playlists.ObservePlaylistsUseCase
import com.resonance.player.domain.search.SearchAllUseCase
import com.resonance.player.domain.search.SearchLibraryUseCase
import com.resonance.player.domain.search.SearchResults
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val getGenreSongs: GetGenreSongsUseCase,
    private val playNextUseCase: PlayNextUseCase,
    private val appendToQueueUseCase: AppendToQueueUseCase,
    observePlaylists: ObservePlaylistsUseCase,
    private val addSongToPlaylist: AddSongToPlaylistUseCase,
    private val createPlaylist: CreatePlaylistUseCase
) : ViewModel() {
    private val query = MutableStateFlow("")

    val playlists: StateFlow<List<Playlist>> = observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val playlistErrorMutable = MutableStateFlow<String?>(null)
    val playlistError: StateFlow<String?> = playlistErrorMutable.asStateFlow()

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

    fun playNext(song: Song) {
        viewModelScope.launch { playNextUseCase(song) }
    }

    fun addToQueue(song: Song) {
        viewModelScope.launch { appendToQueueUseCase(listOf(song)) }
    }

    fun addToPlaylist(playlistId: Long, songId: Long, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            when (val result = addSongToPlaylist(playlistId, songId)) {
                is Result.Success -> {
                    playlistErrorMutable.value = null
                    onDone()
                }
                is Result.Failure -> playlistErrorMutable.value = result.error.userMessage()
                is Result.Loading -> Unit
            }
        }
    }

    fun createPlaylistAndAdd(name: String, songId: Long, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            when (val result = createPlaylist(name)) {
                is Result.Success -> {
                    playlistErrorMutable.value = null
                    addSongToPlaylist(result.value.id, songId)
                    onDone()
                }
                is Result.Failure -> playlistErrorMutable.value = result.error.userMessage()
                is Result.Loading -> Unit
            }
        }
    }

    fun clearPlaylistError() {
        playlistErrorMutable.value = null
    }
}
