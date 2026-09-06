package com.resonance.player.domain.mix

import com.resonance.player.core.common.Result
import com.resonance.player.core.model.MixConfiguration
import com.resonance.player.core.model.MixPreset
import com.resonance.player.core.model.PlaybackHistoryEntry
import com.resonance.player.core.model.Song
import com.resonance.player.core.model.TrackScore
import com.resonance.player.core.model.TransitionPlan

/**
 * Smart Mix contracts (algorithm lands in a later phase).
 *
 * Hard constraints for every future implementation:
 * - LOCAL ONLY: inputs are library songs, tags (genre/artist/album/BPM/key),
 *   play counts, favorites and on-device history. No network calls.
 * - TESTABLE: pure functions of (candidates, config, history). No ExoPlayer,
 *   no Context, no database access inside these interfaces.
 * - TEMPORARY OUTPUT: a generated queue; the user library is never mutated.
 */
data class MixContext(
    val history: List<PlaybackHistoryEntry> = emptyList(),
    val favoriteSongIds: Set<Long> = emptySet(),
    val nowEpochSec: Long = 0L
)

interface TrackScorer {
    fun score(track: Song, context: MixContext): TrackScore
}

interface MixQueueGenerator {
    fun generate(
        candidates: List<Song>,
        scores: Map<Long, TrackScore>,
        config: MixConfiguration
    ): List<Song>
}

interface TransitionPlanner {
    fun plan(queue: List<Song>, config: MixConfiguration): List<TransitionPlan>
}

interface MixEngine {
    suspend fun buildMix(
        preset: MixPreset,
        candidates: List<Song>,
        context: MixContext
    ): Result<List<Song>>
}
