package com.resonance.player.feature.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonance.player.core.model.Song
import com.resonance.player.domain.favorites.ObserveFavoriteSongsUseCase
import com.resonance.player.domain.playback.PlaySongsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Favorites list: real favorite songs, tap to play from the list. */
class FavoritesViewModel(
    observeFavorites: ObserveFavoriteSongsUseCase,
    private val playSongs: PlaySongsUseCase
) : ViewModel() {
    val songs: StateFlow<List<Song>> = observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun playFrom(songs: List<Song>, index: Int) {
        viewModelScope.launch { playSongs(songs, index) }
    }
}
