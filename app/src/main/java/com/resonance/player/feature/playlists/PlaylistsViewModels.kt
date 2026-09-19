package com.resonance.player.feature.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonance.player.core.common.Result
import com.resonance.player.core.common.userMessage
import com.resonance.player.core.model.Playlist
import com.resonance.player.core.model.ShuffleMode
import com.resonance.player.core.model.Song
import com.resonance.player.domain.playback.PlaySongsUseCase
import com.resonance.player.domain.playback.SetShuffleModeUseCase
import com.resonance.player.domain.playlists.AddSongToPlaylistUseCase
import com.resonance.player.domain.playlists.CreatePlaylistUseCase
import com.resonance.player.domain.playlists.DeletePlaylistUseCase
import com.resonance.player.domain.playlists.MovePlaylistItemUseCase
import com.resonance.player.domain.playlists.ObservePlaylistSongsUseCase
import com.resonance.player.domain.playlists.ObservePlaylistsUseCase
import com.resonance.player.domain.playlists.RemoveSongFromPlaylistUseCase
import com.resonance.player.domain.playlists.RenamePlaylistUseCase
import com.resonance.player.domain.library.ObserveSongsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Playlists tab: list, create, delete, play. Detail screen has its own VM. */
class PlaylistsViewModel(
    observePlaylists: ObservePlaylistsUseCase,
    private val createPlaylist: CreatePlaylistUseCase,
    private val deletePlaylist: DeletePlaylistUseCase,
    private val renamePlaylist: RenamePlaylistUseCase,
    private val songsOf: ObservePlaylistSongsUseCase,
    private val playSongs: PlaySongsUseCase
) : ViewModel() {
    val playlists: StateFlow<List<Playlist>> = observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val errorMutable = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = errorMutable.asStateFlow()

    fun create(name: String, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            when (val result = createPlaylist(name)) {
                is Result.Success -> {
                    errorMutable.value = null
                    onCreated(result.value.id)
                }
                is Result.Failure -> errorMutable.value = result.error.userMessage()
                is Result.Loading -> Unit
            }
        }
    }

    fun delete(playlistId: Long) {
        viewModelScope.launch { deletePlaylist(playlistId) }
    }

    fun rename(playlistId: Long, name: String) {
        viewModelScope.launch {
            when (val result = renamePlaylist(playlistId, name)) {
                is Result.Success -> errorMutable.value = null
                is Result.Failure -> errorMutable.value = result.error.userMessage()
                is Result.Loading -> Unit
            }
        }
    }

    fun play(playlistId: Long, onPlaying: () -> Unit = {}, onEmpty: () -> Unit = {}) {
        viewModelScope.launch {
            val songs = (songsOf(playlistId) as? Result.Success)?.value
            if (!songs.isNullOrEmpty()) {
                playSongs(songs, 0)
                onPlaying()
            } else {
                onEmpty()
            }
        }
    }

    fun clearError() {
        errorMutable.value = null
    }
}

/** Playlist detail: songs, play-all, rename, delete, remove, reorder. */
class PlaylistDetailViewModel(
    private val playlistId: Long,
    private val songsOf: ObservePlaylistSongsUseCase,
    private val playSongs: PlaySongsUseCase,
    private val setShuffleMode: SetShuffleModeUseCase,
    private val renameOp: RenamePlaylistUseCase,
    private val deleteOp: DeletePlaylistUseCase,
    private val removeSongOp: RemoveSongFromPlaylistUseCase,
    private val moveItemOp: MovePlaylistItemUseCase,
    observeAllSongs: ObserveSongsUseCase,
    private val addSongOp: AddSongToPlaylistUseCase
) : ViewModel() {
    private val songsMutable = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = songsMutable.asStateFlow()

    /** Full library, for the "add songs" picker. */
    val allSongs: StateFlow<List<Song>> = observeAllSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val errorMutable = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = errorMutable.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            when (val result = songsOf(playlistId)) {
                is Result.Success -> {
                    songsMutable.value = result.value
                    errorMutable.value = null
                }
                is Result.Failure -> errorMutable.value = result.error.userMessage()
                is Result.Loading -> Unit
            }
        }
    }

    fun playAll(shuffled: Boolean = false) {
        val list = songsMutable.value
        if (list.isEmpty()) return
        viewModelScope.launch {
            playSongs(list, 0)
            if (shuffled) setShuffleMode(ShuffleMode.ON)
        }
    }

    fun playFrom(index: Int) {
        val list = songsMutable.value
        if (index !in list.indices) return
        viewModelScope.launch { playSongs(list, index) }
    }

    fun remove(songId: Long) {
        viewModelScope.launch {
            removeSongOp(playlistId, songId)
            refresh()
        }
    }

    fun addSongs(songIds: List<Long>, onDone: () -> Unit = {}) {
        if (songIds.isEmpty()) return
        viewModelScope.launch {
            songIds.forEach { addSongOp(playlistId, it) }
            refresh()
            onDone()
        }
    }

    fun move(fromPosition: Int, toPosition: Int) {
        viewModelScope.launch {
            moveItemOp(playlistId, fromPosition, toPosition)
            refresh()
        }
    }

    fun rename(name: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            when (val result = renameOp(playlistId, name)) {
                is Result.Success -> {
                    errorMutable.value = null
                    refresh()
                    onDone()
                }
                is Result.Failure -> errorMutable.value = result.error.userMessage()
                is Result.Loading -> Unit
            }
        }
    }

    fun delete(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            deleteOp(playlistId)
            onDone()
        }
    }
}
