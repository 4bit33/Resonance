package com.resonance.player.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonance.player.core.common.AppError
import com.resonance.player.core.common.Result
import com.resonance.player.core.model.PlaybackSnapshot
import com.resonance.player.core.model.RepeatMode
import com.resonance.player.core.model.ShuffleMode
import com.resonance.player.core.model.Song
import com.resonance.player.core.playback.PlaybackController
import com.resonance.player.domain.library.GetSongUseCase
import com.resonance.player.domain.playback.SeekToUseCase
import com.resonance.player.domain.playback.SetRepeatModeUseCase
import com.resonance.player.domain.playback.SetShuffleModeUseCase
import com.resonance.player.domain.playback.SkipToNextUseCase
import com.resonance.player.domain.playback.SkipToPreviousUseCase
import com.resonance.player.domain.playback.TogglePlayPauseUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Now Playing ViewModel. Renders the authoritative [PlaybackSnapshot] and
 * forwards intents to use cases. Command failures (e.g. service
 * unavailable) surface via [commandError], never silently.
 */
class PlayerViewModel(
    private val songId: Long,
    private val getSong: GetSongUseCase,
    playback: PlaybackController,
    private val togglePlayPause: TogglePlayPauseUseCase,
    private val seekTo: SeekToUseCase,
    private val skipToNext: SkipToNextUseCase,
    private val skipToPrevious: SkipToPreviousUseCase,
    private val setShuffleMode: SetShuffleModeUseCase,
    private val setRepeatMode: SetRepeatModeUseCase
) : ViewModel() {

    val snapshot: StateFlow<PlaybackSnapshot> = playback.snapshot

    private val detailsMutable = MutableStateFlow<Song?>(null)
    val details: StateFlow<Song?> = detailsMutable.asStateFlow()

    private val commandErrorMutable = MutableStateFlow<AppError?>(null)
    val commandError: StateFlow<AppError?> = commandErrorMutable.asStateFlow()

    init {
        viewModelScope.launch {
            detailsMutable.value = (getSong(songId) as? Result.Success)?.value
        }
    }

    fun onTogglePlayPause() = runCommand { togglePlayPause() }
    fun onSeek(positionMs: Long) = runCommand { seekTo(positionMs) }
    fun onNext() = runCommand { skipToNext() }
    fun onPrevious() = runCommand { skipToPrevious() }

    fun onToggleShuffle(current: ShuffleMode) = runCommand {
        val next = if (current == ShuffleMode.ON) ShuffleMode.OFF else ShuffleMode.ON
        setShuffleMode(next)
    }

    fun onCycleRepeat(current: RepeatMode) = runCommand {
        val next = when (current) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        setRepeatMode(next)
    }

    fun clearCommandError() {
        commandErrorMutable.value = null
    }

    private fun runCommand(block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            val result = block()
            if (result is Result.Failure) commandErrorMutable.value = result.error
        }
    }
}
