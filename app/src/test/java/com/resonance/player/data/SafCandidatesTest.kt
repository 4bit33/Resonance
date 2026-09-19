package com.resonance.player.data

import com.resonance.player.core.media.DOCUMENT_DIR_MIME
import com.resonance.player.core.media.DocRow
import com.resonance.player.core.media.StableIds
import com.resonance.player.core.media.audioCandidateOf
import com.resonance.player.core.media.isAudioDoc
import com.resonance.player.core.media.joinRelPath
import com.resonance.player.core.media.uniqueName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SafCandidatesTest {

    private val storage = "com.android.externalstorage.documents"

    /** Golden values computed with an independent MD5 implementation: this hash is a persisted format. */
    @Test
    fun stableId_goldenValues() {
        assertEquals(5731844828744923300L, StableIds.songId(storage, "primary:Music/Rock/a.mp3"))
        assertEquals(6270970878750308638L, StableIds.songId(storage, "primary:Music/Rock/b.mp3"))
        assertEquals(
            4394339703935811002L,
            StableIds.songId("com.android.providers.media.documents", "audio:1234")
        )
    }

    @Test
    fun stableId_isPositive_deterministic_andDependsOnAuthority() {
        val a = StableIds.songId(storage, "primary:Music/x.mp3")
        assertTrue(a > 0)
        assertEquals(a, StableIds.songId(storage, "primary:Music/x.mp3"))
        assertNotEquals(a, StableIds.songId("other.provider", "primary:Music/x.mp3"))
    }

    @Test
    fun isAudioDoc_usesMimeThenExtensionForGenericTypes() {
        assertTrue(isAudioDoc("a.mp3", "audio/mpeg"))
        assertTrue(isAudioDoc("a", "application/ogg"))
        assertTrue(isAudioDoc(null, "audio/flac"))
        assertTrue(isAudioDoc("Song.FLAC", "application/octet-stream"))
        assertTrue(isAudioDoc("song.m4a", null))
        assertFalse(isAudioDoc("list.m3u", "audio/x-mpegurl"))
        assertFalse(isAudioDoc("notes.txt", "text/plain"))
        assertFalse(isAudioDoc("notes.txt", null))
        assertFalse(isAudioDoc("readme", "application/octet-stream"))
    }

    @Test
    fun isAudioDoc_skipsDotFiles() {
        assertFalse(isAudioDoc("._a.mp3", "audio/mpeg"))
        assertFalse(isAudioDoc(".trashed-1234-a.mp3", "audio/mpeg"))
    }

    @Test
    fun joinRelPath_buildsRootRelativeFolders() {
        assertEquals("Music/", joinRelPath(null, "Music"))
        assertEquals("Music/Rock/", joinRelPath("Music/", "Rock"))
        assertEquals("Music/Rock/Live/", joinRelPath("Music/Rock/", "/Live/"))
    }

    @Test
    fun uniqueName_numbersDuplicates() {
        assertEquals("Music", uniqueName("Music", emptySet()))
        assertEquals("Music (2)", uniqueName("Music", setOf("Music")))
        assertEquals("Music (3)", uniqueName("Music", setOf("Music", "Music (2)")))
        assertEquals("Rock", uniqueName("Rock", setOf("Music", "Music (2)")))
    }

    @Test
    fun audioCandidateOf_mapsRow() {
        val c = audioCandidateOf(
            row = DocRow("primary:Music/Rock/a.mp3", "a.mp3", "audio/mpeg", 1_700_000_123_456L, 4096L),
            authority = storage,
            sourceId = 7L,
            contentUri = "content://x/tree/t/document/d",
            relativePath = "Music/Rock/"
        )
        assertEquals(5731844828744923300L, c.id)
        assertEquals(7L, c.sourceId)
        assertEquals("a.mp3", c.displayName)
        assertEquals("audio/mpeg", c.mimeType)
        assertEquals(4096L, c.sizeBytes)
        assertEquals(1_700_000_123L, c.dateModifiedSec)
        assertEquals("Music/Rock/", c.relativePath)
    }

    @Test
    fun audioCandidateOf_toleratesMissingFields() {
        val c = audioCandidateOf(
            row = DocRow("primary:Music/no-name", null, "application/octet-stream", null, null),
            authority = storage,
            sourceId = 1L,
            contentUri = "content://x/d",
            relativePath = null
        )
        assertEquals("no-name", c.displayName)
        assertNull(c.mimeType)
        assertEquals(0L, c.sizeBytes)
        assertEquals(0L, c.dateModifiedSec)
        assertNull(c.relativePath)
    }

    @Test
    fun directoryMime_isTheSafConstant() {
        assertEquals("vnd.android.document/directory", DOCUMENT_DIR_MIME)
    }
}
