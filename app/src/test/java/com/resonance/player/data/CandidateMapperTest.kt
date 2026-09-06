package com.resonance.player.data

import com.resonance.player.core.media.RawRow
import com.resonance.player.core.media.SupportedMimeTypes
import com.resonance.player.core.media.contentUriFor
import com.resonance.player.core.media.mapCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CandidateMapperTest {

    private fun row(vararg pairs: Pair<String, Any?>) = RawRow(mapOf(*pairs))

    @Test
    fun mapsFullRow() {
        val candidate = mapCandidate(
            row(
                "_id" to 42L,
                "volume_name" to "external_primary",
                "_display_name" to "song.mp3",
                "mime_type" to "audio/mpeg",
                "_size" to 5_000_000L,
                "date_modified" to 1_700_000_000L,
                "date_added" to 1_699_000_000L,
                "duration" to 200_000L,
                "relative_path" to "Music/Rock/",
                "title" to "Song",
                "artist" to "Artist",
                "album" to "Album",
                "genre" to "Rock",
                "year" to 2021,
                "track" to 3,
                "album_id" to 7L,
                "artist_id" to 8L
            )
        )
        assertEquals(42L, candidate.mediaStoreId)
        assertEquals("external_primary", candidate.volumeName)
        assertEquals("content://media/external_primary/audio/media/42", candidate.contentUri)
        assertEquals("song.mp3", candidate.displayName)
        assertEquals("audio/mpeg", candidate.mimeType)
        assertEquals(5_000_000L, candidate.sizeBytes)
        assertEquals(1_700_000_000L, candidate.dateModifiedSec)
        assertEquals(200_000L, candidate.durationMs)
        assertEquals("Music/Rock/", candidate.relativePath)
        assertEquals("Song", candidate.title)
        assertEquals(2021, candidate.year)
        assertEquals(3, candidate.track)
        assertEquals(7L, candidate.albumId)
    }

    @Test
    fun toleratesMissingAndGarbage() {
        val candidate = mapCandidate(
            row("_id" to 1L, "duration" to "not-a-number", "year" to "abc"),
            volumeFallback = "external"
        )
        assertEquals("external", candidate.volumeName)
        assertEquals("content://media/external/audio/media/1", candidate.contentUri)
        assertEquals("unknown", candidate.displayName)
        assertEquals(0L, candidate.durationMs)
        assertEquals(0L, candidate.sizeBytes)
        assertEquals(null, candidate.year)
        assertEquals(null, candidate.title)
    }

    @Test
    fun contentUriFor_buildsVolumeUris() {
        assertEquals(
            "content://media/ABCD-1234/audio/media/9",
            contentUriFor("ABCD-1234", 9L)
        )
    }
}

class MimeClassificationTest {

    @Test
    fun acceptsAudioMimes() {
        assertTrue(SupportedMimeTypes.isAudioMime("audio/mpeg"))
        assertTrue(SupportedMimeTypes.isAudioMime("audio/flac"))
        assertTrue(SupportedMimeTypes.isAudioMime("audio/x-flac"))
        assertTrue(SupportedMimeTypes.isAudioMime("audio/mp4"))
        assertTrue(SupportedMimeTypes.isAudioMime("audio/ogg"))
        assertTrue(SupportedMimeTypes.isAudioMime("audio/opus"))
        assertTrue(SupportedMimeTypes.isAudioMime("audio/wav"))
        assertTrue(SupportedMimeTypes.isAudioMime("application/ogg"))
        assertTrue(SupportedMimeTypes.isAudioMime("audio/mpeg; charset=binary"))
    }

    @Test
    fun rejectsPlaylistsAndNonAudio() {
        assertFalse(SupportedMimeTypes.isAudioMime("audio/x-mpegurl"))
        assertFalse(SupportedMimeTypes.isAudioMime("audio/mpegurl"))
        assertFalse(SupportedMimeTypes.isAudioMime("application/vnd.apple.mpegurl"))
        assertFalse(SupportedMimeTypes.isAudioMime("audio/x-scpls"))
        assertFalse(SupportedMimeTypes.isAudioMime("video/mp4"))
        assertFalse(SupportedMimeTypes.isAudioMime("image/jpeg"))
        assertFalse(SupportedMimeTypes.isAudioMime(null))
        assertFalse(SupportedMimeTypes.isAudioMime(""))
        assertFalse(SupportedMimeTypes.isAudioMime("  "))
    }
}
