package com.resonance.player.feature.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonance.player.core.common.Result
import com.resonance.player.core.common.userMessage
import com.resonance.player.core.model.Playlist
import com.resonance.player.core.model.Song
import com.resonance.player.domain.favorites.ObserveFavoriteSongsUseCase
import com.resonance.player.domain.playback.AppendToQueueUseCase
import com.resonance.player.domain.playback.PlayNextUseCase
import com.resonance.player.domain.playback.PlaySongsUseCase
import com.resonance.player.domain.playlists.AddSongToPlaylistUseCase
import com.resonance.player.domain.playlists.CreatePlaylistUseCase
import com.resonance.player.domain.playlists.ObservePlaylistsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Favorites list: real favorite songs, tap to play from the list. */
class FavoritesViewModel(
    observeFavorites: ObserveFavoriteSongsUseCase,
    private val playSongs: PlaySongsUseCase,
    private val playNextUseCase: PlayNextUseCase,
    private val appendToQueueUseCase: AppendToQueueUseCase,
    observePlaylists: ObservePlaylistsUseCase,
    private val addSongToPlaylist: AddSongToPlaylistUseCase,
    private val createPlaylist: CreatePlaylistUseCase
) : ViewModel() {
    val songs: StateFlow<List<Song>> = observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playlists: StateFlow<List<Playlist>> = observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val playlistErrorMutable = MutableStateFlow<String?>(null)
    val playlistError: StateFlow<String?> = playlistErrorMutable.asStateFlow()

    fun playFrom(songs: List<Song>, index: Int) {
        viewModelScope.launch { playSongs(songs, index) }
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
