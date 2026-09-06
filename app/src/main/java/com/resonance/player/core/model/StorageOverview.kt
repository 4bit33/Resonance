package com.resonance.player.core.model

/**
 * Storage overview for the Home library card: real DB aggregates plus real
 * device counters. No estimates, no fake paths.
 */
data class StorageOverview(
    val trackCount: Int,
    val libraryBytes: Long,
    val deviceUsedBytes: Long,
    val deviceTotalBytes: Long
) {
    /** Fraction of device storage used (for the health bar). */
    fun usedFraction(): Float =
        if (deviceTotalBytes > 0L) {
            (deviceUsedBytes.toFloat() / deviceTotalBytes.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
}
