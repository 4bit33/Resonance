package com.resonance.player.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonance.player.core.common.Result
import com.resonance.player.core.common.userMessage
import com.resonance.player.core.model.Song
import com.resonance.player.domain.library.GetSongUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PlayerUiState {
    data object Loading : PlayerUiState
    data class Details(val song: Song) : PlayerUiState
    data class Error(val message: String) : PlayerUiState
}

/** Loads real song metadata. Transport controls arrive with Phase 2. */
class PlayerViewModel(
    private val songId: Long,
    private val getSong: GetSongUseCase
) : ViewModel() {
    private val mutable = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState: StateFlow<PlayerUiState> = mutable.asStateFlow()

    init {
        viewModelScope.launch {
            mutable.value = when (val result = getSong(songId)) {
                is Result.Success -> PlayerUiState.Details(result.value)
                is Result.Failure -> PlayerUiState.Error(result.error.userMessage())
                Result.Loading -> PlayerUiState.Loading
            }
        }
    }
}
