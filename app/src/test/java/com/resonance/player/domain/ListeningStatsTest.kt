package com.resonance.player.domain

import com.resonance.player.domain.library.HistoryPlay
import com.resonance.player.domain.library.listeningStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ListeningStatsTest {

    @Test
    fun empty_hasNoTops() {
        val stats = listeningStats(emptyList())
        assertEquals(0, stats.plays)
        assertEquals(0L, stats.listenedMs)
        assertNull(stats.topGenre)
        assertNull(stats.topArtist)
    }

    @Test
    fun sumsDurations_andPicksMostPlayedNames_ignoringBlankAndUnknown() {
        val stats = listeningStats(
            listOf(
                HistoryPlay(180_000L, "Lo-fi", "A"),
                HistoryPlay(200_000L, "Lo-fi", "B"),
                HistoryPlay(220_000L, "Rock", "B"),
                HistoryPlay(100_000L, " ", "Unknown"),
                HistoryPlay(100_000L, null, "unknown")
            )
        )
        assertEquals(5, stats.plays)
        assertEquals(800_000L, stats.listenedMs)
        assertEquals("Lo-fi", stats.topGenre)
        assertEquals("B", stats.topArtist)
    }
}
