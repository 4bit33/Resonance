package com.resonance.player.domain.playback

import com.resonance.player.core.common.Result
import com.resonance.player.core.model.Song
import com.resonance.player.core.playback.PlaybackController

/** Thin queue-mutation use cases; the engine owns ordering (incl. shuffle). */
class MoveQueueItemUseCase(private val controller: PlaybackController) {
    suspend operator fun invoke(fromIndex: Int, toIndex: Int): Result<Unit> =
        controller.moveQueueItem(fromIndex, toIndex)
}

class RemoveQueueItemUseCase(private val controller: PlaybackController) {
    suspend operator fun invoke(index: Int): Result<Unit> =
        controller.removeQueueItem(index)
}

class ClearQueueUseCase(private val controller: PlaybackController) {
    suspend operator fun invoke(): Result<Unit> = controller.clearQueue()
}

class SkipToQueueItemUseCase(private val controller: PlaybackController) {
    suspend operator fun invoke(index: Int): Result<Unit> =
        controller.skipToQueueItem(index)
}

class AppendToQueueUseCase(private val controller: PlaybackController) {
    suspend operator fun invoke(songs: List<Song>): Result<Unit> =
        controller.appendToQueue(songs)
}

class InsertIntoQueueUseCase(private val controller: PlaybackController) {
    suspend operator fun invoke(index: Int, songs: List<Song>): Result<Unit> =
        controller.insertIntoQueue(index, songs)
}
