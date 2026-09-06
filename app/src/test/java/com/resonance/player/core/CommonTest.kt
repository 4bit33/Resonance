package com.resonance.player.core

import com.resonance.player.core.common.AppError
import com.resonance.player.core.common.formatDurationMs
import com.resonance.player.core.common.userMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppErrorTest {

    @Test
    fun everyCase_hasNonBlankMessage() {
        val cases: List<AppError> = listOf(
            AppError.MissingFile("/music/a.mp3"),
            AppError.PermissionDenied,
            AppError.UnsupportedFormat("audio/x-weird"),
            AppError.UnsupportedFormat(null),
            AppError.PlaybackFailed("timeout"),
            AppError.DatabaseError("io"),
            AppError.InvalidMetadata("bad tag"),
            AppError.CorruptedArtwork,
            AppError.EmptyLibrary,
            AppError.FeatureUnavailable("Playback engine (Phase 2)"),
            AppError.Unknown("boom"),
            AppError.Unknown()
        )
        cases.forEach { assertTrue(it.userMessage().isNotBlank()) }
    }

    @Test
    fun featureUnavailable_namesFeature() {
        assertTrue(AppError.FeatureUnavailable("X").userMessage().contains("X"))
    }
}

class DurationFormatTest {

    @Test
    fun formatsMinutesAndSeconds() {
        assertEquals("0:00", formatDurationMs(0L))
        assertEquals("3:07", formatDurationMs(187_000L))
        assertEquals("10:00", formatDurationMs(600_000L))
    }

    @Test
    fun clampsNegative() {
        assertEquals("0:00", formatDurationMs(-5_000L))
    }
}
