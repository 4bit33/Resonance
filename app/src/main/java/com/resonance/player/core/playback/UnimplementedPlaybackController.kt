package com.resonance.player.core.playback

import com.resonance.player.core.common.AppError
import com.resonance.player.core.common.Result
import com.resonance.player.core.model.PlaybackSnapshot
import com.resonance.player.core.model.RepeatMode
import com.resonance.player.core.model.ShuffleMode
import com.resonance.player.core.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Phase 1 honest stand-in. Exposes the idle snapshot and reports
 * FeatureUnavailable for every transport call INSTEAD of faking playback
 * (no timers, no fake progress — see implementation rules).
 * Replaced by the Media3 implementation in Phase 2 without touching callers.
 */
class UnimplementedPlaybackController : PlaybackController {
    private val idle = MutableStateFlow(PlaybackSnapshot.Idle)
    override val snapshot: StateFlow<PlaybackSnapshot> = idle.asStateFlow()

    private fun unavailable(): Result<Unit> =
        Result.Failure(AppError.FeatureUnavailable("Playback engine (Phase 2)"))

    override suspend fun play(queue: List<Song>, startIndex: Int): Result<Unit> = unavailable()
    override suspend fun resume(): Result<Unit> = unavailable()
    override suspend fun pause(): Result<Unit> = unavailable()
    override suspend fun stop(): Result<Unit> = unavailable()
    override suspend fun seekTo(positionMs: Long): Result<Unit> = unavailable()
    override suspend fun skipToNext(): Result<Unit> = unavailable()
    override suspend fun skipToPrevious(): Result<Unit> = unavailable()
    override suspend fun setShuffle(mode: ShuffleMode): Result<Unit> = unavailable()
    override suspend fun setRepeat(mode: RepeatMode): Result<Unit> = unavailable()
    override suspend fun moveQueueItem(fromIndex: Int, toIndex: Int): Result<Unit> = unavailable()
}
