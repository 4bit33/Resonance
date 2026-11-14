package com.resonance.player.core.ui

import com.resonance.player.core.ui.theme.pickSeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArtworkSeedTest {
    private val orange = 0xE0661A
    private val red = 0xD01010
    private val grey = 0x808080
    private val nearBlack = 0x0A0505
    private val mutedBlue = 0x5A6478

    @Test
    fun theColorThatFillsTheCoverWins() {
        assertEquals(orange, pickSeed(listOf(orange to 600, red to 80, nearBlack to 900)))
    }

    @Test
    fun aVividSpeckDoesNotBeatALargeArea() {
        assertEquals(grey, pickSeed(listOf(grey to 900, red to 10)))
        assertEquals(mutedBlue, pickSeed(listOf(mutedBlue to 800, red to 40)))
    }

    @Test
    fun noSwatchesNoSeed() {
        assertNull(pickSeed(emptyList()))
    }
}
