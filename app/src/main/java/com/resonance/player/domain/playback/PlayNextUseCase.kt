package com.resonance.player.domain.playback

import com.resonance.player.core.common.Result
import com.resonance.player.core.model.Song
import com.resonance.player.core.playback.PlaybackController

/**
 * Queues a song to play right after the current item (index+1), or at the
 * head when the queue is empty. Reads intent from the live snapshot.
 */
class PlayNextUseCase(private val controller: PlaybackController) {
    suspend operator fun invoke(song: Song): Result<Unit> {
        val snapshot = controller.snapshot.value
        if (snapshot.queue.isEmpty()) {
            return controller.play(listOf(song), 0)
        }
        val at = (snapshot.queueIndex + 1).coerceIn(0, snapshot.queue.size)
        return controller.insertIntoQueue(at, listOf(song))
    }
}
