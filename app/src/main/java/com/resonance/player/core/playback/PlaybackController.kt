package com.resonance.player.core.playback

import com.resonance.player.core.common.Result
import com.resonance.player.core.model.RepeatMode
import com.resonance.player.core.model.ShuffleMode
import com.resonance.player.core.model.Song
import kotlinx.coroutines.flow.StateFlow

/**
 * THE playback boundary (ADR-003).
 *
 * - Implemented in Phase 2 by a Media3-backed controller owned by the
 *   foreground playback service (ExoPlayer + MediaSession).
 * - UI layers (ViewModels) depend ONLY on this interface — never on
 *   ExoPlayer, MediaController, or any global player singleton.
 * - [snapshot] is the single authoritative playback state; notification,
 *   lock-screen and UI all render it.
 */
interface PlaybackController {
    val snapshot: StateFlow<com.resonance.player.core.model.PlaybackSnapshot>

    suspend fun play(queue: List<Song>, startIndex: Int = 0): Result<Unit>
    suspend fun resume(): Result<Unit>
    suspend fun pause(): Result<Unit>
    suspend fun stop(): Result<Unit>
    suspend fun seekTo(positionMs: Long): Result<Unit>
    suspend fun skipToNext(): Result<Unit>
    suspend fun skipToPrevious(): Result<Unit>
    suspend fun setShuffle(mode: ShuffleMode): Result<Unit>
    suspend fun setRepeat(mode: RepeatMode): Result<Unit>
    suspend fun moveQueueItem(fromIndex: Int, toIndex: Int): Result<Unit>
}
