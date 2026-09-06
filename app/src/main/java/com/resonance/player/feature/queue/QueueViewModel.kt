package com.resonance.player.feature.queue

import androidx.lifecycle.ViewModel
import com.resonance.player.core.model.PlaybackSnapshot
import com.resonance.player.core.playback.PlaybackController
import kotlinx.coroutines.flow.StateFlow

/** Read-only view of the authoritative queue. Mutations arrive later. */
class QueueViewModel(controller: PlaybackController) : ViewModel() {
    val snapshot: StateFlow<PlaybackSnapshot> = controller.snapshot
}
