package com.resonance.player.core.playback

import com.resonance.player.core.common.Result
import com.resonance.player.core.model.PlaybackSnapshot
import com.resonance.player.core.model.RepeatMode
import com.resonance.player.core.model.ShuffleMode
import com.resonance.player.core.model.Song
import kotlinx.coroutines.flow.StateFlow

/**
 * THE playback boundary (ADR-003).
 *
 * - Implemented by the Media3-backed controller owned by the foreground
 *   playback service (ExoPlayer + MediaSession).
 * - UI layers (ViewModels) depend ONLY on this interface — never on
 *   ExoPlayer, MediaController, or any global player singleton.
 * - [snapshot] is the single authoritative playback state; notification,
 *   lock-screen and UI all render it.
 *
 * Queue model: the playback queue is RUNTIME state (a temporary list of
 * songs), fully separate from saved playlists and library collections.
 * Mutating the queue never touches the database. Indices always refer to
 * queue positions in original (unshuffled) order; the engine resolves
 * shuffled-timeline translation internally.
 */
interface PlaybackController {
    val snapshot: StateFlow<PlaybackSnapshot>

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

    /** Appends songs to the end of the current queue (prepares if idle). */
    suspend fun appendToQueue(songs: List<Song>): Result<Unit>

    /** Inserts songs before [index] (clamped to queue bounds). */
    suspend fun insertIntoQueue(index: Int, songs: List<Song>): Result<Unit>

    /** Removes the entry at [index]; playback continues coherently. */
    suspend fun removeQueueItem(index: Int): Result<Unit>

    /** Stops playback and drops the whole queue (saved data untouched). */
    suspend fun clearQueue(): Result<Unit>

    /** Jumps to the entry at [index] and starts it. */
    suspend fun skipToQueueItem(index: Int): Result<Unit>
}
