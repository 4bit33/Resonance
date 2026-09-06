package com.resonance.player.data.media

import com.resonance.player.core.media.MediaItemCandidate

/** Minimal stored state needed to diff without loading full entities. */
data class StoredFingerprint(
    val id: Long,
    val mediaStoreId: Long,
    val volumeName: String,
    val dateModifiedSec: Long,
    val sizeBytes: Long,
    val playCount: Long,
    val lastPlayedSec: Long?
)

/** Pure diff result: what to (re)import vs which rows vanished. */
data class ReconcilePlan(
    val toImport: List<MediaItemCandidate>,
    val toDelete: List<Long>
)

/**
 * Pure incremental reconciliation (JVM-tested). Identity = (mediaStoreId,
 * volumeName); a row is UNCHANGED only when dateModified AND size both match.
 * Anything else is re-imported (metadata/artwork refresh); stored ids absent
 * from the scan are deleted. Duplicate candidates collapse last-wins.
 */
object Reconciler {

    /** Stitch "ignore files under 30s" threshold (ringtones/memos filter). */
    const val MIN_DURATION_MS = 30_000L

    /**
     * Duration gate for discovery. Unknown durations (<= 0) always pass —
     * an unreadable length must never silently drop a file.
     */
    fun passesDurationFilter(durationMs: Long, ignoreShort: Boolean): Boolean =
        !ignoreShort || durationMs <= 0L || durationMs >= MIN_DURATION_MS

    fun plan(
        stored: List<StoredFingerprint>,
        seen: Map<Pair<Long, String>, MediaItemCandidate>
    ): ReconcilePlan {
        val storedByKey = stored.associateBy { it.mediaStoreId to it.volumeName }
        val toImport = ArrayList<MediaItemCandidate>(seen.size)
        for ((key, candidate) in seen) {
            val existing = storedByKey[key]
            if (existing == null ||
                existing.dateModifiedSec != candidate.dateModifiedSec ||
                existing.sizeBytes != candidate.sizeBytes
            ) {
                toImport.add(candidate)
            }
        }
        val toDelete = storedByKey.values
            .filter { (it.mediaStoreId to it.volumeName) !in seen }
            .map { it.id }
        return ReconcilePlan(toImport, toDelete)
    }
}
