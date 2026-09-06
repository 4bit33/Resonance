package com.resonance.player.core.model

/**
 * Playback-domain models. There is exactly ONE authoritative playback state in
 * the app: [PlaybackSnapshot], exposed by PlaybackController as a StateFlow
 * (ADR-003). UI, notification and lock-screen controls all render this state.
 */
enum class RepeatMode { OFF, ALL, ONE }

enum class ShuffleMode { OFF, ON }

data class QueueItem(
    val queueId: Long,
    val song: Song,
    val position: Int
)

data class PlaybackSnapshot(
    val song: Song? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<QueueItem> = emptyList(),
    val queueIndex: Int = -1,
    val shuffle: ShuffleMode = ShuffleMode.OFF,
    val repeat: RepeatMode = RepeatMode.OFF
) {
    companion object {
        val Idle = PlaybackSnapshot()
    }
}
