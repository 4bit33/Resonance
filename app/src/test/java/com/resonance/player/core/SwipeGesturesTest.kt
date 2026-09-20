package com.resonance.player.core

import com.resonance.player.core.ui.components.SwipeDirection
import com.resonance.player.core.ui.components.shouldDismiss
import com.resonance.player.core.ui.components.swipeDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeGesturesTest {

    private val t = 100f

    @Test
    fun shortDragsAreNotSwipes() {
        assertNull(swipeDirection(60f, 5f, t))
        assertNull(swipeDirection(-99f, 0f, t))
        assertNull(swipeDirection(0f, 0f, t))
    }

    @Test
    fun dominantAxisWins() {
        assertEquals(SwipeDirection.Left, swipeDirection(-150f, 40f, t))
        assertEquals(SwipeDirection.Right, swipeDirection(150f, -40f, t))
        assertEquals(SwipeDirection.Up, swipeDirection(30f, -160f, t))
        assertEquals(SwipeDirection.Down, swipeDirection(-30f, 160f, t))
    }

    @Test
    fun aDiagonalIsNotASwipe() {
        assertNull(swipeDirection(120f, 120f, t))
        // long on the minor axis but the major axis is under the threshold
        assertNull(swipeDirection(90f, 80f, t))
    }

    @Test
    fun pullDown_needsDistance_orAShortPullWithAFlick() {
        val pull = 200f
        val flick = 60f
        val velocity = 1000f
        assertTrue(shouldDismiss(pulledPx = 200f, velocityY = 0f, pullPx = pull, flickPx = flick, flickVelocity = velocity))
        assertTrue(shouldDismiss(80f, 1500f, pull, flick, velocity))
        assertFalse(shouldDismiss(80f, 500f, pull, flick, velocity))
        assertFalse(shouldDismiss(150f, 0f, pull, flick, velocity))
    }

    @Test
    fun pullDown_aFastScrollBackToTheTopNeverCloses() {
        // velocity alone, with (almost) nothing pulled past the top, must not dismiss
        assertFalse(shouldDismiss(0f, 5000f, 200f, 60f, 1000f))
        assertFalse(shouldDismiss(30f, 5000f, 200f, 60f, 1000f))
    }
}
