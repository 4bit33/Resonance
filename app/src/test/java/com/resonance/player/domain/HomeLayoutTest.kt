package com.resonance.player.domain

import com.resonance.player.domain.settings.HomeLayout
import com.resonance.player.domain.settings.HomeSection
import com.resonance.player.domain.settings.decodeHomeLayout
import com.resonance.player.domain.settings.encodeHomeLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLayoutTest {

    @Test
    fun default_isTheChosenOrder_compact() {
        assertEquals(
            listOf(
                HomeSection.GREETING, HomeSection.SEARCH, HomeSection.CONTINUE,
                HomeSection.RECENTLY_PLAYED, HomeSection.RECENTLY_ADDED, HomeSection.GENRES,
                HomeSection.PLAYLISTS, HomeSection.STATS, HomeSection.IMPORT
            ),
            HomeLayout.Default.visible
        )
        assertTrue(HomeLayout.Default.compact)
        assertEquals(HomeSection.entries.size, HomeLayout.Default.sections.size)
    }

    @Test
    fun encodeDecode_roundTrips() {
        val layout = HomeLayout.Default
            .moved(HomeSection.STATS, -3)
            .toggled(HomeSection.GREETING)
            .copy(compact = false)
        assertEquals(layout, decodeHomeLayout(encodeHomeLayout(layout)))
    }

    @Test
    fun decode_toleratesGarbageAndAppendsNewSectionsOff() {
        val decoded = decodeHomeLayout("STATS:1,NOPE:1,STATS:0,SEARCH:0;compact=0")
        assertEquals(HomeSection.STATS, decoded.sections[0].section)
        assertTrue(decoded.sections[0].enabled)
        assertEquals(HomeSection.SEARCH, decoded.sections[1].section)
        assertFalse(decoded.sections[1].enabled)
        assertEquals(HomeSection.entries.size, decoded.sections.size)
        assertEquals(listOf(HomeSection.STATS), decoded.visible)
        assertFalse(decoded.compact)
    }

    @Test
    fun decode_emptyOrBlank_isDefault() {
        assertEquals(HomeLayout.Default, decodeHomeLayout(null))
        assertEquals(HomeLayout.Default, decodeHomeLayout("   "))
        assertEquals(HomeLayout.Default, decodeHomeLayout("NOTHING:1"))
    }

    @Test
    fun moved_outOfRange_doesNothing() {
        val first = HomeLayout.Default.sections.first().section
        assertEquals(HomeLayout.Default, HomeLayout.Default.moved(first, -1))
    }
}
