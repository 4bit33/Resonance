package com.resonance.player.domain.playback

import com.resonance.player.core.common.AppError
import com.resonance.player.core.common.Result
import com.resonance.player.core.model.Song
import com.resonance.player.core.playback.PlaybackController

/** Starts playback of an explicit queue. ViewModels call this, never ExoPlayer. */
class PlaySongsUseCase(private val controller: PlaybackController) {
    suspend operator fun invoke(queue: List<Song>, startIndex: Int = 0): Result<Unit> {
        if (queue.isEmpty()) return Result.Failure(AppError.EmptyLibrary)
        if (startIndex !in queue.indices) {
            return Result.Failure(AppError.Unknown("Invalid queue position"))
        }
        return controller.play(queue, startIndex)
    }
}

/** Single toggle for play/pause buttons; derives intent from the snapshot. */
class TogglePlayPauseUseCase(private val controller: PlaybackController) {
    suspend operator fun invoke(): Result<Unit> =
        if (controller.snapshot.value.isPlaying) controller.pause() else controller.resume()
}
