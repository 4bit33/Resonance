package com.resonance.player.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonance.player.core.model.Album
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
import com.resonance.player.domain.playback.PlaySongsUseCase
import com.resonance.player.domain.playback.SetShuffleModeUseCase
import com.resonance.player.core.common.Result
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    private val rescanLibrary: RescanLibraryUseCase
) : ViewModel() {

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
}
