package com.resonance.player.data.media

import com.resonance.player.core.media.AudioCandidate

/** Minimal stored state needed to diff without loading full entities. */
data class StoredSong(
    val id: Long,
    val sourceId: Long,
    val dateModifiedSec: Long,
    val sizeBytes: Long,
    val durationMs: Long
)

/** Pure diff result: what to (re)import vs which stored rows to delete. */
data class SourcePlan(
    val toImport: List<AudioCandidate>,
    val toDelete: List<Long>
)

/**
 * Pure, JVM-tested incremental reconciliation across user-added sources.
 * Identity is the stable song id; a row is UNCHANGED only when modified time,
 * size AND owning source all match.
 *
 * Safety rule: a stored row may only be deleted because its file vanished when
 * its source was enumerated COMPLETELY. Rows of an unavailable (grant lost,
 * unmounted) or partially listed source are never deleted, so one flaky
 * provider cannot wipe the library.
 */
object SourceReconciler {

    /** "Ignore files under 30s" threshold (ringtones / voice memos filter). */
    const val MIN_DURATION_MS = 30_000L

    /** Unknown durations (<= 0) always pass: an unreadable length never drops a file. */
    fun passesDurationFilter(durationMs: Long, ignoreShort: Boolean): Boolean =
        !ignoreShort || durationMs <= 0L || durationMs >= MIN_DURATION_MS

    fun plan(
        stored: List<StoredSong>,
        seen: Map<Long, AudioCandidate>,
        completeSourceIds: Set<Long>,
        ignoreShort: Boolean
    ): SourcePlan {
        val storedById = stored.associateBy { it.id }
        val toImport = seen.values.filter { candidate ->
            val existing = storedById[candidate.id]
            existing == null ||
                existing.dateModifiedSec != candidate.dateModifiedSec ||
                existing.sizeBytes != candidate.sizeBytes ||
                existing.sourceId != candidate.sourceId
        }
        val toDelete = stored
            .filter { song ->
                (song.sourceId in completeSourceIds && song.id !in seen) ||
                    !passesDurationFilter(song.durationMs, ignoreShort)
            }
            .map { it.id }
        return SourcePlan(toImport, toDelete)
    }
}
