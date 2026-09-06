package com.resonance.player.data

import com.resonance.player.core.media.MediaItemCandidate
import com.resonance.player.data.media.Reconciler
import com.resonance.player.data.media.StoredFingerprint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun stored(
    id: Long,
    modified: Long = 100L,
    size: Long = 1000L,
    volume: String = "external"
) = StoredFingerprint(id, id, volume, modified, size, playCount = 5L, lastPlayedSec = 50L)

private fun seen(
    id: Long,
    modified: Long = 100L,
    size: Long = 1000L,
    volume: String = "external"
) = MediaItemCandidate(
    mediaStoreId = id, volumeName = volume,
    contentUri = "content://media/$volume/audio/media/$id",
    displayName = "$id.mp3", mimeType = "audio/mpeg", sizeBytes = size,
    dateModifiedSec = modified, dateAddedSec = 90L, durationMs = 200_000L,
    relativePath = null, title = null, artist = null, album = null,
    genre = null, year = null, track = null, albumId = null, artistId = null
)

private fun keyOf(id: Long, volume: String = "external") = id to volume

class ReconcilerTest {

    @Test
    fun emptyDb_importsEverything() {
        val seenMap = mapOf(keyOf(1L) to seen(1L), keyOf(2L) to seen(2L))
        val plan = Reconciler.plan(emptyList(), seenMap)
        assertEquals(2, plan.toImport.size)
        assertTrue(plan.toDelete.isEmpty())
    }

    @Test
    fun unchangedItems_areSkipped() {
        val plan = Reconciler.plan(
            listOf(stored(1L), stored(2L)),
            mapOf(keyOf(1L) to seen(1L), keyOf(2L) to seen(2L))
        )
        assertTrue(plan.toImport.isEmpty())
        assertTrue(plan.toDelete.isEmpty())
    }

    @Test
    fun modifiedSizeOrMtime_triggersReimport() {
        val plan = Reconciler.plan(
            listOf(stored(1L), stored(2L)),
            mapOf(keyOf(1L) to seen(1L, modified = 200L), keyOf(2L) to seen(2L, size = 2000L))
        )
        assertEquals(setOf(1L, 2L), plan.toImport.map { it.mediaStoreId }.toSet())
        assertTrue(plan.toDelete.isEmpty())
    }

    @Test
    fun vanishedRows_areDeleted() {
        val plan = Reconciler.plan(
            listOf(stored(1L), stored(2L), stored(3L)),
            mapOf(keyOf(1L) to seen(1L))
        )
        assertTrue(plan.toImport.isEmpty())
        assertEquals(listOf(2L, 3L), plan.toDelete.sorted())
    }

    @Test
    fun mixedScenario() {
        val plan = Reconciler.plan(
            listOf(stored(1L), stored(2L), stored(4L)),
            mapOf(
                keyOf(1L) to seen(1L),
                keyOf(2L) to seen(2L, modified = 999L),
                keyOf(3L) to seen(3L)
            )
        )
        assertEquals(listOf(2L, 3L), plan.toImport.map { it.mediaStoreId }.sorted())
        assertEquals(listOf(4L), plan.toDelete)
    }

    @Test
    fun sameIdOnDifferentVolumes_areDistinct() {
        val plan = Reconciler.plan(
            listOf(stored(1L, volume = "external")),
            mapOf(keyOf(1L, "ABCD-1234") to seen(1L, volume = "ABCD-1234"))
        )
        assertEquals(1, plan.toImport.size)
        assertEquals(listOf(1L), plan.toDelete)
    }
}
