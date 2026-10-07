package com.resonance.player.data

import com.resonance.player.core.media.AudioCandidate
import com.resonance.player.core.media.SongMetadata
import com.resonance.player.core.media.UnknownMetadata
import com.resonance.player.data.media.FilenameFallback
import com.resonance.player.data.media.MetadataNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private fun candidate(
    displayName: String = "track.mp3",
    relativePath: String? = "Music/"
) = AudioCandidate(
    id = 1L, sourceId = 1L,
    contentUri = "content://com.android.externalstorage.documents/tree/x/document/y",
    displayName = displayName, mimeType = "audio/mpeg", sizeBytes = 5_000_000L,
    dateModifiedSec = 100L, relativePath = relativePath
)

private fun tags(
    title: String? = null,
    artist: String? = null,
    album: String? = null,
    albumArtist: String? = null,
    genre: String? = null,
    trackRaw: String? = null,
    discRaw: String? = null,
    yearRaw: String? = null
) = SongMetadata(
    title = title, artistName = artist, albumName = album, albumArtist = albumArtist,
    genreName = genre, trackRaw = trackRaw, discRaw = discRaw, yearRaw = yearRaw,
    durationMs = null, mimeType = null, bitrate = 320_000, sampleRate = 44_100,
    bpm = null, musicalKey = null
)

class MetadataNormalizerTest {

    @Test
    fun embeddedTags_areUsed() {
        val track = MetadataNormalizer.normalize(
            candidate(),
            tags(title = "Tag Title", artist = "Tag Artist", album = "Tag Album", genre = "Rock", yearRaw = "2020-05-01", trackRaw = "3/12")
        )
        assertEquals("Tag Title", track.title)
        assertEquals("Tag Artist", track.artistName)
        assertEquals("Tag Album", track.albumName)
        assertEquals("Rock", track.genreName)
        assertEquals(2020, track.year)
        assertEquals(3, track.trackNumber)
        assertEquals(12, track.totalTracks)
        assertEquals("Music/track.mp3", track.displayPath)
    }

    @Test
    fun fallbacks_coverMissingMetadata() {
        val track = MetadataNormalizer.normalize(candidate(displayName = "song.mp3"), null)
        assertEquals("song", track.title)
        assertEquals(UnknownMetadata.ARTIST, track.artistName)
        assertEquals(UnknownMetadata.ALBUM, track.albumName)
        assertNull(track.genreName)
        assertNull(track.albumArtist)
    }

    @Test
    fun clean_trimsAndPreservesUnicode() {
        assertEquals("a b c", MetadataNormalizer.clean("  a   b\tc  "))
        assertEquals("Борис Гребенщиков", MetadataNormalizer.clean("  Борис   Гребенщиков "))
        assertEquals("日本語タイトル", MetadataNormalizer.clean("日本語タイトル"))
        assertNull(MetadataNormalizer.clean("   "))
        assertNull(MetadataNormalizer.clean(null))
        assertEquals("ab", MetadataNormalizer.clean("a\u0000b\u0007"))
    }

    @Test
    fun duration_comesFromTags_elseZero() {
        assertEquals(
            180_000L,
            MetadataNormalizer.normalize(candidate(), tags().copy(durationMs = 180_000L)).durationMs
        )
        assertEquals(0L, MetadataNormalizer.normalize(candidate(), null).durationMs)
        assertEquals(
            0L,
            MetadataNormalizer.normalize(candidate(), tags().copy(durationMs = -5L)).durationMs
        )
    }

    @Test
    fun mimeType_fallsBackToTheCandidate() {
        assertEquals("audio/mpeg", MetadataNormalizer.normalize(candidate(), null).mimeType)
        assertEquals(
            "audio/flac",
            MetadataNormalizer.normalize(candidate(), tags().copy(mimeType = "audio/flac")).mimeType
        )
    }
}

class TrackNumberParserTest {

    @Test
    fun parses_plainAndTotalForms() {
        assertEquals(1 to 12, MetadataNormalizer.parseTrackNumber("1/12"))
        assertEquals(1 to 12, MetadataNormalizer.parseTrackNumber("01/12"))
        assertEquals(3 to null, MetadataNormalizer.parseTrackNumber("03"))
        assertEquals(7 to null, MetadataNormalizer.parseTrackNumber(" 7 "))
        assertEquals(null to null, MetadataNormalizer.parseTrackNumber("0"))
        assertEquals(null to null, MetadataNormalizer.parseTrackNumber("abc"))
        assertEquals(null to null, MetadataNormalizer.parseTrackNumber(null))
        assertEquals(null to null, MetadataNormalizer.parseTrackNumber(""))
    }

    @Test
    fun disc_parsing_reusesTrackParser() {
        assertEquals(2 to 3, MetadataNormalizer.parseTrackNumber("2/3"))
    }
}

class YearParserTest {

    @Test
    fun extractsSaneYears() {
        assertEquals(2021, MetadataNormalizer.parseYear("2021"))
        assertEquals(2021, MetadataNormalizer.parseYear("2021-05-01"))
        assertEquals(1999, MetadataNormalizer.parseYear("Recorded 1999 live"))
        assertNull(MetadataNormalizer.parseYear("99"))
        assertNull(MetadataNormalizer.parseYear("3050"))
        assertNull(MetadataNormalizer.parseYear("not a year"))
        assertNull(MetadataNormalizer.parseYear(null))
    }
}

class FilenameFallbackTest {

    @Test
    fun stripsExtensionAndTrackPrefix() {
        assertEquals("Song Name", FilenameFallback.titleFromFileName("01 - Song Name.mp3"))
        assertEquals("Song Name", FilenameFallback.titleFromFileName("02.Song Name.flac"))
        assertEquals("Song Name", FilenameFallback.titleFromFileName("03_Song Name.ogg"))
        assertEquals("plain", FilenameFallback.titleFromFileName("plain.wav"))
        assertEquals("1984 - Anthem", FilenameFallback.titleFromFileName("1984 - Anthem.mp3"))
    }

    @Test
    fun degenerateNames_fallBackToUnknown() {
        assertEquals(UnknownMetadata.TITLE, FilenameFallback.titleFromFileName(".mp3"))
        assertEquals(UnknownMetadata.TITLE, FilenameFallback.titleFromFileName("01 -.mp3"))
    }
}

class DisplayPathTest {

    @Test
    fun joinsFolderAndName() {
        assertEquals(
            "Music/Artist/song.mp3",
            MetadataNormalizer.displayPathFor("Music/Artist/", "song.mp3")
        )
        assertEquals("song.mp3", MetadataNormalizer.displayPathFor(null, "song.mp3"))
        assertEquals("song.mp3", MetadataNormalizer.displayPathFor("  ", "song.mp3"))
    }
}
