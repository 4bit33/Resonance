package com.resonance.player.core

import com.resonance.player.core.common.formatBytes
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatBytesTest {

    @Test
    fun formatsUnits() {
        assertEquals("0 B", formatBytes(0L))
        assertEquals("512 B", formatBytes(512L))
        assertEquals("1 KB", formatBytes(1024L))
        assertEquals("900 KB", formatBytes(900L * 1024L))
        assertEquals("10.5 GB", formatBytes((10.5 * 1024 * 1024 * 1024).toLong()))
        assertEquals("2 GB", formatBytes(2L * 1024 * 1024 * 1024))
        assertEquals("512 MB", formatBytes(512L * 1024 * 1024))
    }

    @Test
    fun clampsNegative() {
        assertEquals("0 B", formatBytes(-100L))
    }
}
