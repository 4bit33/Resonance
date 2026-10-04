package com.resonance.player.domain

import com.resonance.player.data.tags.LookupParsers
import com.resonance.player.domain.tags.TagCandidate
import com.resonance.player.domain.tags.cleanTitle
import com.resonance.player.domain.tags.rankCandidates
import com.resonance.player.domain.tags.searchTerms
import com.resonance.player.domain.tags.yearOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TagsTest {

    @Test
    fun cleanTitle_dropsVideoNoise() {
        assertEquals("Blade", cleanTitle("Blade (Official Video) [HD]"))
        assertEquals("Naughty", cleanTitle("Naughty (Official Music Video)"))
        assertEquals("Song", cleanTitle("Song [Lyrics]"))
        assertEquals("Song (Remix)", cleanTitle("Song (Remix)"))
    }

    @Test
    fun searchTerms_splitsArtistDashTitleWhenArtistUnknown() {
        assertEquals("D'Angello & Francis" to "Blade", searchTerms("D'Angello & Francis - Blade (Official Video)", "Unknown"))
        assertEquals("Marie Vaunt" to "Naughty", searchTerms("Naughty", "Marie Vaunt"))
        assertEquals(null to "1239575272", searchTerms("1239575272", null))
        // A real artist tag wins over a dash inside the title.
        assertEquals("X" to "A - B", searchTerms("A - B", "X"))
    }

    @Test
    fun yearOf_readsDates() {
        assertEquals(2019, yearOf("2019-05-01"))
        assertEquals(2005, yearOf("2005"))
        assertNull(yearOf(""))
        assertNull(yearOf(null))
    }

    @Test
    fun rank_prefersCloseDurationThenMusicBrainz() {
        fun c(source: TagCandidate.Source, ms: Long?) = TagCandidate(source, ms.toString(), "t", "a", null, null, null, null, null, ms, null, null)
        val ranked = rankCandidates(
            listOf(
                c(TagCandidate.Source.DEEZER, 200_000),
                c(TagCandidate.Source.MUSICBRAINZ, 300_000),
                c(TagCandidate.Source.MUSICBRAINZ, 201_000),
                c(TagCandidate.Source.DEEZER, null)
            ),
            songDurationMs = 200_500
        )
        assertEquals(listOf("201000", "200000", "300000", "null"), ranked.map { it.id })
    }

    @Test
    fun parsesMusicBrainz_earliestOfficialRelease() {
        val json = """
            {"recordings":[{"id":"r1","title":"Blade","length":168000,
              "artist-credit":[{"name":"D'Angello","joinphrase":" & "},{"name":"Francis"}],
              "tags":[{"name":"techno","count":3},{"name":"house","count":1}],
              "releases":[
                {"id":"bootleg","title":"Mix","status":"Bootleg","date":"2019"},
                {"id":"late","title":"Blade (Deluxe)","status":"Official","date":"2023-02-01","media":[{"track":[{"number":"7"}]}]},
                {"id":"rel","title":"Blade","status":"Official","date":"2022-10-14","media":[{"track":[{"number":"1"}]}]}
              ]}]}
        """.trimIndent()
        val c = LookupParsers.musicBrainz(json).single()
        assertEquals("Blade", c.title)
        assertEquals("D'Angello & Francis", c.artist)
        assertEquals("Blade", c.album)
        assertEquals(2022, c.year)
        assertEquals(1, c.trackNumber)
        assertEquals("Techno", c.genre)
        assertEquals("https://coverartarchive.org/release/rel/front-500", c.coverUrl)
    }

    @Test
    fun parsesDeezer_searchThenTrackAndAlbum() {
        val search = """{"data":[{"id":42,"title":"Naughty","duration":174,"artist":{"name":"Marie Vaunt"},
            "album":{"id":7,"title":"Naughty","cover_xl":"https://x/1000.jpg","cover_medium":"https://x/250.jpg"}}]}"""
        val base = LookupParsers.deezerSearch(search).single()
        assertEquals(174_000L, base.durationMs)
        assertEquals("https://x/1000.jpg", base.coverUrl)
        val (detailed, albumId) = LookupParsers.deezerTrack(
            """{"track_position":2,"release_date":"2026-03-06","album":{"id":7},"contributors":[{"name":"Marie Vaunt"}]}""",
            base
        )
        assertEquals(2, detailed.trackNumber)
        assertEquals(2026, detailed.year)
        assertNull(detailed.albumArtist)
        assertEquals(7L, albumId)
        assertEquals("Techno", LookupParsers.deezerAlbumGenre("""{"genres":{"data":[{"name":"Techno"}]}}"""))
    }
}
