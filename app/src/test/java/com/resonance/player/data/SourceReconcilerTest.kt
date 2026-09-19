package com.resonance.player.data

import com.resonance.player.core.media.AudioCandidate
import com.resonance.player.data.media.SourceReconciler
import com.resonance.player.data.media.StoredSong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private fun stored(
    id: Long,
    source: Long = 1L,
    modified: Long = 100L,
    size: Long = 1000L,
    duration: Long = 200_000L
) = StoredSong(id, source, modified, size, duration)

private fun cand(id: Long, source: Long = 1L, modified: Long = 100L, size: Long = 1000L) =
    AudioCandidate(
        id = id, sourceId = source, contentUri = "content://x/$id", displayName = "$id.mp3",
        mimeType = "audio/mpeg", sizeBytes = size, dateModifiedSec = modified, relativePath = null
    )

private fun seenOf(vararg c: AudioCandidate) = c.associateBy { it.id }

class SourceReconcilerTest {

    private val complete = setOf(1L, 2L)

    @Test
    fun emptyDb_importsEverything() {
        val plan = SourceReconciler.plan(emptyList(), seenOf(cand(1), cand(2)), complete, false)
        assertEquals(2, plan.toImport.size)
        assertTrue(plan.toDelete.isEmpty())
    }

    @Test
    fun unchangedItems_areSkipped() {
        val plan = SourceReconciler.plan(listOf(stored(1)), seenOf(cand(1)), complete, false)
        assertTrue(plan.toImport.isEmpty())
        assertTrue(plan.toDelete.isEmpty())
    }

    @Test
    fun changedTimeSizeOrOwner_isReimported() {
        val s = listOf(stored(1), stored(2), stored(3))
        val plan = SourceReconciler.plan(
            s,
            seenOf(cand(1, modified = 999), cand(2, size = 5), cand(3, source = 2)),
            complete,
            false
        )
        assertEquals(setOf(1L, 2L, 3L), plan.toImport.map { it.id }.toSet())
        assertTrue(plan.toDelete.isEmpty())
    }

    @Test
    fun vanishedFile_ofCompleteSource_isDeleted() {
        val plan = SourceReconciler.plan(
            listOf(stored(1), stored(2)), seenOf(cand(1)), complete, false
        )
        assertEquals(listOf(2L), plan.toDelete)
    }

    @Test
    fun rowsOfIncompleteOrUnavailableSource_areNeverDeleted() {
        // source 2 was not enumerated completely: its rows must survive.
        val plan = SourceReconciler.plan(
            listOf(stored(1, source = 1), stored(2, source = 2)),
            seenOf(),
            setOf(1L),
            false
        )
        assertEquals(listOf(1L), plan.toDelete)
    }

    @Test
    fun fileNowReachableThroughAnotherSource_isMovedNotDeleted() {
        val plan = SourceReconciler.plan(
            listOf(stored(1, source = 1)), seenOf(cand(1, source = 2)), complete, false
        )
        assertTrue(plan.toDelete.isEmpty())
        assertEquals(listOf(1L), plan.toImport.map { it.id })
    }

    @Test
    fun ignoreShort_prunesStoredShortFiles_butKeepsUnknownDurations() {
        val s = listOf(
            stored(1, duration = 10_000L),
            stored(2, duration = 0L),
            stored(3, duration = 30_000L)
        )
        val plan = SourceReconciler.plan(s, seenOf(cand(1), cand(2), cand(3)), complete, true)
        assertEquals(listOf(1L), plan.toDelete)
        val off = SourceReconciler.plan(s, seenOf(cand(1), cand(2), cand(3)), complete, false)
        assertTrue(off.toDelete.isEmpty())
    }

    @Test
    fun durationFilterBoundaries() {
        assertTrue(SourceReconciler.passesDurationFilter(5_000L, ignoreShort = false))
        assertFalse(SourceReconciler.passesDurationFilter(29_999L, ignoreShort = true))
        assertTrue(SourceReconciler.passesDurationFilter(30_000L, ignoreShort = true))
        assertTrue(SourceReconciler.passesDurationFilter(0L, ignoreShort = true))
    }
}
