package com.resonance.player.feature.home

import com.resonance.player.domain.playback.PlaybackSource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonance.player.core.common.Result
import com.resonance.player.core.common.userMessage
import com.resonance.player.core.media.ScanState
import com.resonance.player.core.model.Album
import com.resonance.player.core.model.Genre
import com.resonance.player.core.model.Playlist
import com.resonance.player.core.model.ShuffleMode
import com.resonance.player.core.model.Song
import com.resonance.player.core.model.StorageOverview
import com.resonance.player.domain.library.ListeningStats
import com.resonance.player.domain.library.ObserveGenresUseCase
import com.resonance.player.domain.library.ObserveListeningStatsUseCase
import com.resonance.player.domain.library.ObserveMostPlayedUseCase
import com.resonance.player.domain.library.ObserveRecentlyAddedUseCase
import com.resonance.player.domain.library.ObserveRecentlyPlayedUseCase
import com.resonance.player.domain.library.ObserveScanStateUseCase
import com.resonance.player.domain.library.ObserveSongsUseCase
import com.resonance.player.domain.library.ObserveStorageOverviewUseCase
import com.resonance.player.domain.library.recentAlbumsFromSongs
import com.resonance.player.domain.playback.AppendToQueueUseCase
import com.resonance.player.domain.playback.PlayNextUseCase
import com.resonance.player.domain.playback.PlaySongsUseCase
import com.resonance.player.domain.playback.SetShuffleModeUseCase
import com.resonance.player.domain.playback.TogglePlayPauseUseCase
import com.resonance.player.domain.playlists.AddSongToPlaylistUseCase
import com.resonance.player.domain.playlists.CreatePlaylistUseCase
import com.resonance.player.domain.playlists.ObservePlaylistsUseCase
import com.resonance.player.domain.settings.HomeLayout
import com.resonance.player.domain.settings.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Use cases Home reads from and acts through (grouped so the constructor stays readable). */
class HomeDependencies(
    val observeRecentlyPlayed: ObserveRecentlyPlayedUseCase,
    val observeMostPlayed: ObserveMostPlayedUseCase,
    val observeRecentlyAdded: ObserveRecentlyAddedUseCase,
    val observeStorageOverview: ObserveStorageOverviewUseCase,
    val observeScanState: ObserveScanStateUseCase,
    val observeGenres: ObserveGenresUseCase,
    val observeListeningStats: ObserveListeningStatsUseCase,
    val observeSongs: ObserveSongsUseCase,
    val observePlaylists: ObservePlaylistsUseCase,
    val settings: UserPreferencesRepository,
    val playSongs: PlaySongsUseCase,
    val setShuffleMode: SetShuffleModeUseCase,
    val togglePlayPause: TogglePlayPauseUseCase,
    val playNext: PlayNextUseCase,
    val appendToQueue: AppendToQueueUseCase,
    val addSongToPlaylist: AddSongToPlaylistUseCase,
    val createPlaylist: CreatePlaylistUseCase
)

/**
 * Home: the blocks the user chose ([layout]), each backed by real library
 * data. Blocks with nothing to show hide themselves; an empty library
 * offers to add music, never demo content.
 */
class HomeViewModel(private val deps: HomeDependencies) : ViewModel() {

    private fun <T> Flow<T>.state(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    val layout: StateFlow<HomeLayout> = deps.settings.homeLayout.state(HomeLayout.Default)

    val playlists: StateFlow<List<Playlist>> = deps.observePlaylists().state(emptyList())

    private val playlistErrorMutable = MutableStateFlow<String?>(null)
    val playlistError: StateFlow<String?> = playlistErrorMutable.asStateFlow()

    val recentAlbums: StateFlow<List<Album>> = deps.observeRecentlyPlayed(40)
        .map { recentAlbumsFromSongs(it, 12) }
        .state(emptyList())

    /** The last song heard: what "Continue" offers when nothing is loaded in the player. */
    val lastPlayed: StateFlow<Song?> = deps.observeRecentlyPlayed(1).map { it.firstOrNull() }.state(null)

    val recentlyAddedAlbums: StateFlow<List<Album>> = deps.observeRecentlyAdded(80)
        .map { recentAlbumsFromSongs(it, 12) }
        .state(emptyList())

    val mostPlayed: StateFlow<List<Song>> = deps.observeMostPlayed(5).state(emptyList())

    /** Biggest genres first: they are the ones worth a tile. */
    val genres: StateFlow<List<Genre>> = deps.observeGenres()
        .map { all -> all.sortedByDescending { it.songCount }.take(6) }
        .state(emptyList())

    val stats: StateFlow<ListeningStats?> = deps.observeListeningStats(7).state<ListeningStats?>(null)

    val storage: StateFlow<StorageOverview?> = deps.observeStorageOverview().state(null)

    val scanState: StateFlow<ScanState> = deps.observeScanState().state(ScanState.Idle)

    fun playFrom(songs: List<Song>, index: Int, source: PlaybackSource? = null) {
        viewModelScope.launch { deps.playSongs(songs, index, source) }
    }

    fun togglePlayPause() {
        viewModelScope.launch { deps.togglePlayPause() }
    }

    fun shuffleAll() {
        viewModelScope.launch { shuffle(deps.observeSongs().first()) }
    }

    private suspend fun shuffle(songs: List<Song>) {
        if (songs.isEmpty()) return
        deps.playSongs(songs, songs.indices.random(), PlaybackSource(PlaybackSource.Kind.SHUFFLE_ALL))
        deps.setShuffleMode(ShuffleMode.ON)
    }

    fun playNext(song: Song) {
        viewModelScope.launch { deps.playNext(song) }
    }

    fun addToQueue(song: Song) {
        viewModelScope.launch { deps.appendToQueue(listOf(song)) }
    }

    fun addToPlaylist(playlistId: Long, songId: Long, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            when (val result = deps.addSongToPlaylist(playlistId, songId)) {
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
            when (val result = deps.createPlaylist(name)) {
                is Result.Success -> {
                    playlistErrorMutable.value = null
                    deps.addSongToPlaylist(result.value.id, songId)
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
