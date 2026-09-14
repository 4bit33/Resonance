package com.resonance.player.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonance.player.core.common.Result
import com.resonance.player.core.common.userMessage
import com.resonance.player.core.media.ScanState
import com.resonance.player.core.model.Album
import com.resonance.player.core.model.Artist
import com.resonance.player.core.model.Genre
import com.resonance.player.core.model.MusicFolder
import com.resonance.player.core.model.Playlist
import com.resonance.player.core.model.ShuffleMode
import com.resonance.player.core.model.Song
import com.resonance.player.domain.library.GetAlbumSongsUseCase
import com.resonance.player.domain.library.GetArtistSongsUseCase
import com.resonance.player.domain.library.GetFolderSongsUseCase
import com.resonance.player.domain.library.GetGenreSongsUseCase
import com.resonance.player.domain.library.ObserveAlbumsUseCase
import com.resonance.player.domain.library.ObserveArtistsUseCase
import com.resonance.player.domain.library.ObserveFoldersUseCase
import com.resonance.player.domain.library.ObserveGenresUseCase
import com.resonance.player.domain.library.ObserveScanStateUseCase
import com.resonance.player.domain.library.ObserveSongsUseCase
import com.resonance.player.domain.library.RescanLibraryUseCase
import com.resonance.player.domain.library.SongSort
import com.resonance.player.domain.playback.AppendToQueueUseCase
import com.resonance.player.domain.playback.PlayNextUseCase
import com.resonance.player.domain.playback.PlaySongsUseCase
import com.resonance.player.domain.playback.SetShuffleModeUseCase
import com.resonance.player.domain.playlists.AddSongToPlaylistUseCase
import com.resonance.player.domain.playlists.CreatePlaylistUseCase
import com.resonance.player.domain.playlists.ObservePlaylistsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data object Empty : LibraryUiState
    data class Content(val songs: List<Song>) : LibraryUiState
    data class Error(val message: String) : LibraryUiState
}

/**
 * Thin ViewModel: maps repository Flows to render states. Tapping a song
 * plays the visible list from that position; tapping an album plays the
 * album in disc/track order. No MediaStore/Room access here.
 */
class LibraryViewModel(
    private val observeSongs: ObserveSongsUseCase,
    private val playSongs: PlaySongsUseCase,
    observeScanState: ObserveScanStateUseCase,
    observeAlbums: ObserveAlbumsUseCase,
    observeArtists: ObserveArtistsUseCase,
    observeGenres: ObserveGenresUseCase,
    observeFolders: ObserveFoldersUseCase,
    private val getAlbumSongs: GetAlbumSongsUseCase,
    private val getArtistSongs: GetArtistSongsUseCase,
    private val getGenreSongs: GetGenreSongsUseCase,
    private val getFolderSongs: GetFolderSongsUseCase,
    private val setShuffleMode: SetShuffleModeUseCase,
    private val playNextUseCase: PlayNextUseCase,
    private val appendToQueueUseCase: AppendToQueueUseCase,
    observePlaylists: ObservePlaylistsUseCase,
    private val addSongToPlaylist: AddSongToPlaylistUseCase,
    private val createPlaylist: CreatePlaylistUseCase,
    private val rescanLibrary: RescanLibraryUseCase
) : ViewModel() {
    private val sortMutable = MutableStateFlow(SongSort.TITLE)
    val sort: StateFlow<SongSort> = sortMutable.asStateFlow()

    fun setSort(sort: SongSort) {
        sortMutable.value = sort
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<LibraryUiState> = sortMutable.flatMapLatest { sort ->
        observeSongs(sort)
            .map { songs ->
                if (songs.isEmpty()) LibraryUiState.Empty else LibraryUiState.Content(songs)
            }
            .catch { e -> emit(LibraryUiState.Error(e.message ?: "Library query failed")) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState.Loading)

    val scanState: StateFlow<ScanState> = observeScanState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScanState.Idle)

    val albums: StateFlow<List<Album>> = observeAlbums()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val artists: StateFlow<List<Artist>> = observeArtists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val genres: StateFlow<List<Genre>> = observeGenres()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val folders: StateFlow<List<MusicFolder>> = observeFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playlists: StateFlow<List<Playlist>> = observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val playlistErrorMutable = MutableStateFlow<String?>(null)
    val playlistError: StateFlow<String?> = playlistErrorMutable.asStateFlow()

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

    /** [relativePath] mirrors [com.resonance.player.core.model.MusicFolder.path]: "" means the root folder. */
    fun playFolder(relativePath: String) {
        viewModelScope.launch {
            val songs = (getFolderSongs(relativePath.ifBlank { null }) as? Result.Success)?.value
            if (!songs.isNullOrEmpty()) playSongs(songs, 0)
        }
    }

    /** Shuffle-all: play the visible list from the head, engine shuffles. */
    fun shuffleAll() {
        val songs = (uiState.value as? LibraryUiState.Content)?.songs ?: return
        if (songs.isEmpty()) return
        viewModelScope.launch {
            playSongs(songs, 0)
            setShuffleMode(ShuffleMode.ON)
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

    fun rescan() {
        viewModelScope.launch { rescanLibrary() }
    }
}
