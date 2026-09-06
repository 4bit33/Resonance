package com.resonance.player.playback

import com.resonance.player.core.common.AppError
import com.resonance.player.core.model.PlaybackSnapshot
import com.resonance.player.core.model.QueueItem
import com.resonance.player.core.model.RepeatMode
import com.resonance.player.core.model.ShuffleMode
import com.resonance.player.core.model.Song

/**
 * Raw values read from the real player on its thread. Plain data so the
 * snapshot assembly below stays JVM-testable without ExoPlayer.
 */
data class PlayerSnapshotInput(
    val isPlaying: Boolean,
    val isBuffering: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val bufferedPositionMs: Long,
    val shuffle: ShuffleMode,
    val repeat: RepeatMode
)

/**
 * Builds the authoritative [PlaybackSnapshot] from real player values plus
 * the bookkeeper queue. No timers, no estimation: every number originates
 * from the player; unknown durations fall back to the library value.
 */
object SnapshotMapper {

    fun build(
        input: PlayerSnapshotInput,
        songs: List<Song>,
        index: Int,
        error: AppError?
    ): PlaybackSnapshot {
        if (songs.isEmpty()) return PlaybackSnapshot.Idle.copy(error = error)
        val safeIndex = index.coerceIn(0, songs.size - 1)
        val song = songs[safeIndex]
        val duration = input.durationMs.takeIf { it > 0L } ?: song.durationMs
        return PlaybackSnapshot(
            song = song,
            isPlaying = input.isPlaying,
            isBuffering = input.isBuffering,
            positionMs = input.positionMs.coerceAtLeast(0L).coerceAtMost(duration),
            durationMs = duration,
            bufferedPositionMs = input.bufferedPositionMs.coerceAtLeast(0L)
                .coerceAtMost(duration),
            queue = songs.mapIndexed { i, s ->
                QueueItem(queueId = i.toLong(), song = s, position = i)
            },
            queueIndex = safeIndex,
            shuffle = input.shuffle,
            repeat = input.repeat,
            error = error
        )
    }
}
