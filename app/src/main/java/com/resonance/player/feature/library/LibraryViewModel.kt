package com.resonance.player.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonance.player.core.common.Result
import com.resonance.player.core.media.ScanState
import com.resonance.player.core.model.Album
import com.resonance.player.core.model.Artist
import com.resonance.player.core.model.Genre
import com.resonance.player.core.model.MusicFolder
import com.resonance.player.core.model.Song
import com.resonance.player.domain.library.GetAlbumSongsUseCase
import com.resonance.player.domain.library.ObserveAlbumsUseCase
import com.resonance.player.domain.library.ObserveArtistsUseCase
import com.resonance.player.domain.library.ObserveFoldersUseCase
import com.resonance.player.domain.library.ObserveGenresUseCase
import com.resonance.player.domain.library.ObserveScanStateUseCase
import com.resonance.player.domain.library.ObserveSongsUseCase
import com.resonance.player.domain.library.RescanLibraryUseCase
import com.resonance.player.domain.library.SongSort
import com.resonance.player.domain.playback.PlaySongsUseCase
import kotlinx.coroutines.flow.SharingStarted
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
    observeSongs: ObserveSongsUseCase,
    private val playSongs: PlaySongsUseCase,
    observeScanState: ObserveScanStateUseCase,
    observeAlbums: ObserveAlbumsUseCase,
    observeArtists: ObserveArtistsUseCase,
    observeGenres: ObserveGenresUseCase,
    observeFolders: ObserveFoldersUseCase,
    private val getAlbumSongs: GetAlbumSongsUseCase,
    private val rescanLibrary: RescanLibraryUseCase
) : ViewModel() {
    val uiState: StateFlow<LibraryUiState> = observeSongs(SongSort.TITLE)
        .map { songs ->
            if (songs.isEmpty()) LibraryUiState.Empty else LibraryUiState.Content(songs)
        }
        .catch { e -> emit(LibraryUiState.Error(e.message ?: "Library query failed")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState.Loading)

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

    fun playFrom(songs: List<Song>, index: Int) {
        viewModelScope.launch { playSongs(songs, index) }
    }

    fun playAlbum(albumName: String, albumArtist: String?) {
        viewModelScope.launch {
            val songs = (getAlbumSongs(albumName, albumArtist) as? Result.Success)?.value
            if (!songs.isNullOrEmpty()) playSongs(songs, 0)
        }
    }

    fun rescan() {
        viewModelScope.launch { rescanLibrary() }
    }
}
