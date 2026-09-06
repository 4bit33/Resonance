package com.resonance.player.core.common

/** Pure duration formatting ("3:07"). Unit-tested, locale-independent. */
fun formatDurationMs(durationMs: Long): String {
    val totalSec = (durationMs.coerceAtLeast(0L) / 1000L)
    val minutes = totalSec / 60L
    val seconds = totalSec % 60L
    return "$minutes:" + seconds.toString().padStart(2, Char(48))
}
