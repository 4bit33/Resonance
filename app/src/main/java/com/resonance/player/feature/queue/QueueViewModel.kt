package com.resonance.player.feature.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonance.player.core.model.PlaybackSnapshot
import com.resonance.player.core.playback.PlaybackController
import com.resonance.player.domain.playback.ClearQueueUseCase
import com.resonance.player.domain.playback.MoveQueueItemUseCase
import com.resonance.player.domain.playback.RemoveQueueItemUseCase
import com.resonance.player.domain.playback.SkipToQueueItemUseCase
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Authoritative queue: reads the snapshot, mutates through use cases only. */
class QueueViewModel(
    controller: PlaybackController,
    private val moveQueueItem: MoveQueueItemUseCase,
    private val removeQueueItem: RemoveQueueItemUseCase,
    private val clearQueue: ClearQueueUseCase,
    private val skipToQueueItem: SkipToQueueItemUseCase
) : ViewModel() {
    val snapshot: StateFlow<PlaybackSnapshot> = controller.snapshot

    fun playAt(index: Int) {
        viewModelScope.launch { skipToQueueItem(index) }
    }

    fun move(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch { moveQueueItem(fromIndex, toIndex) }
    }

    fun remove(index: Int) {
        viewModelScope.launch { removeQueueItem(index) }
    }

    fun clear() {
        viewModelScope.launch { clearQueue() }
    }
}
