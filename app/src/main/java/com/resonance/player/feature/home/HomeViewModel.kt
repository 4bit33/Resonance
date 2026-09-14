package com.resonance.player.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonance.player.core.model.Album
import com.resonance.player.core.model.Playlist
import com.resonance.player.core.model.ShuffleMode
import com.resonance.player.core.model.Song
import com.resonance.player.core.model.StorageOverview
import com.resonance.player.core.media.ScanState
import com.resonance.player.domain.favorites.ObserveFavoriteIdsUseCase
import com.resonance.player.domain.library.ObserveMostPlayedUseCase
import com.resonance.player.domain.library.ObserveRecentlyAddedUseCase
import com.resonance.player.domain.library.ObserveRecentlyPlayedUseCase
import com.resonance.player.domain.library.ObserveStorageOverviewUseCase
import com.resonance.player.domain.library.ObserveScanStateUseCase
import com.resonance.player.domain.library.GetAlbumSongsUseCase
import com.resonance.player.domain.library.RescanLibraryUseCase
import com.resonance.player.domain.library.recentAlbumsFromSongs
import com.resonance.player.domain.playback.AppendToQueueUseCase
import com.resonance.player.domain.playback.PlayNextUseCase
import com.resonance.player.domain.playback.PlaySongsUseCase
import com.resonance.player.domain.playback.SetShuffleModeUseCase
import com.resonance.player.domain.playlists.AddSongToPlaylistUseCase
import com.resonance.player.domain.playlists.CreatePlaylistUseCase
import com.resonance.player.domain.playlists.ObservePlaylistsUseCase
import com.resonance.player.core.common.Result
import com.resonance.player.core.common.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Home sections backed by real repository queries. Carousel albums derive
 * from recently played songs; tiles navigate to real destinations; every
 * tap plays through PlaySongsUseCase — no demo content anywhere.
 */
class HomeViewModel(
    observeRecentlyPlayed: ObserveRecentlyPlayedUseCase,
    observeMostPlayed: ObserveMostPlayedUseCase,
    observeRecentlyAdded: ObserveRecentlyAddedUseCase,
    observeStorageOverview: ObserveStorageOverviewUseCase,
    observeScanState: ObserveScanStateUseCase,
    observeFavoriteIds: ObserveFavoriteIdsUseCase,
    private val playSongs: PlaySongsUseCase,
    private val getAlbumSongs: GetAlbumSongsUseCase,
    private val setShuffleMode: SetShuffleModeUseCase,
    private val rescanLibrary: RescanLibraryUseCase,
    private val playNextUseCase: PlayNextUseCase,
    private val appendToQueueUseCase: AppendToQueueUseCase,
    observePlaylists: ObservePlaylistsUseCase,
    private val addSongToPlaylist: AddSongToPlaylistUseCase,
    private val createPlaylist: CreatePlaylistUseCase
) : ViewModel() {

    val playlists: StateFlow<List<Playlist>> = observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val playlistErrorMutable = MutableStateFlow<String?>(null)
    val playlistError: StateFlow<String?> = playlistErrorMutable.asStateFlow()

    val recentAlbums: StateFlow<List<Album>> = observeRecentlyPlayed(25)
        .map { recentAlbumsFromSongs(it, 10) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentSongs: StateFlow<List<Song>> = observeRecentlyAdded(5)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val mostPlayed: StateFlow<List<Song>> = observeMostPlayed(10)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val storage: StateFlow<StorageOverview?> = observeStorageOverview()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val scanState: StateFlow<ScanState> = observeScanState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScanState.Idle)

    val favoriteCount: StateFlow<Int> = observeFavoriteIds()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun playFrom(songs: List<Song>, index: Int) {
        viewModelScope.launch { playSongs(songs, index) }
    }

    fun playAlbum(albumName: String, albumArtist: String?) {
        viewModelScope.launch {
            val songs = (getAlbumSongs(albumName, albumArtist) as? Result.Success)?.value
            if (!songs.isNullOrEmpty()) playSongs(songs, 0)
        }
    }

    /** Shuffle-play: start at head, then let the engine shuffle the timeline. */
    fun shufflePlay(songs: List<Song>) {
        viewModelScope.launch {
            if (songs.isEmpty()) return@launch
            playSongs(songs, 0)
            setShuffleMode(ShuffleMode.ON)
        }
    }

    fun rescan() {
        viewModelScope.launch { rescanLibrary() }
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
