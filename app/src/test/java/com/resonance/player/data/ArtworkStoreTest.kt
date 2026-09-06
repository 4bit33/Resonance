package com.resonance.player.data

import com.resonance.player.data.media.ArtworkStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ArtworkStoreTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun store() = ArtworkStore(temp.newFolder("artwork")) { file -> "file://" + file.absolutePath }

    @Test
    fun store_returnsStableKeyAndUri() = runTest {
        val s = store()
        val bytes = "fake-image-bytes".toByteArray()
        val first = s.store(bytes)
        val second = s.store(bytes)
        assertNotNull(first)
        assertEquals(first?.key, second?.key)
        assertEquals(first?.uri, second?.uri)
        assertEquals(64, first?.key?.length)
        // Deduplication: one file for identical bytes.
        assertEquals(1, temp.root.resolve("artwork").listFiles()?.size)
    }

    @Test
    fun distinctBytes_getDistinctFiles() = runTest {
        val s = store()
        val a = s.store("image-a".toByteArray())
        val b = s.store("image-b".toByteArray())
        assertNotNull(a)
        assertNotNull(b)
        assert(a?.key != b?.key)
    }

    @Test
    fun emptyBytes_areRejected() = runTest {
        assertNull(store().store(ByteArray(0)))
    }

    @Test
    fun uriFor_resolvesOnlyValidFiles() = runTest {
        val s = store()
        assertNull(s.uriFor(null))
        assertNull(s.uriFor(""))
        assertNull(s.uriFor("../evil"))
        assertNull(s.uriFor("a\\b"))
        assertNull(s.uriFor("missing-key"))
        val ref = s.store("img".toByteArray())
        assertNotNull(ref)
        assertEquals(ref?.uri, s.uriFor(ref?.key))
    }

    @Test
    fun prune_removesOrphansAndKeepsReferenced() = runTest {
        val s = store()
        val keep = s.store("keep-me".toByteArray())
        s.store("drop-me".toByteArray())
        assertNotNull(keep)
        val deleted = s.prune(setOf(keep!!.key))
        assertEquals(1, deleted)
        assertNotNull(s.uriFor(keep.key))
        assertEquals(0, s.prune(setOf(keep.key)))
        assertEquals(1, s.prune(emptySet()))
    }
}
