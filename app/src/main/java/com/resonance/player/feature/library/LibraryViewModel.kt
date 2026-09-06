package com.resonance.player.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonance.player.core.model.Song
import com.resonance.player.domain.library.ObserveSongsUseCase
import com.resonance.player.domain.library.SongSort
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data object Empty : LibraryUiState
    data class Content(val songs: List<Song>) : LibraryUiState
    data class Error(val message: String) : LibraryUiState
}

/** Thin ViewModel: maps the repository Flow to render states. No logic here. */
class LibraryViewModel(observeSongs: ObserveSongsUseCase) : ViewModel() {
    val uiState: StateFlow<LibraryUiState> = observeSongs(SongSort.TITLE)
        .map { songs ->
            if (songs.isEmpty()) LibraryUiState.Empty else LibraryUiState.Content(songs)
        }
        .catch { e -> emit(LibraryUiState.Error(e.message ?: "Library query failed")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState.Loading)
}
