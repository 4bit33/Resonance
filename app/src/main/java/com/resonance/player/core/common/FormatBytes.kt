package com.resonance.player.core.common

import kotlin.math.ln
import kotlin.math.pow

/**
 * Human byte counts ("38.4 GB", "512 MB", "900 KB", "0 B").
 * Pure and unit-tested; locale-independent formatting.
 */
fun formatBytes(bytes: Long): String {
    val safe = bytes.coerceAtLeast(0L)
    if (safe < 1024L) return "$safe B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    val digitGroups = (ln(safe.toDouble()) / ln(1024.0)).toInt().coerceIn(1, units.size)
    val value = safe.toDouble() / 1024.0.pow(digitGroups.toDouble())
    if (value >= 100.0) return "${value.toLong()} ${units[digitGroups - 1]}"
    val oneDecimal = (value * 10.0).toLong() / 10.0
    val rounded = if (oneDecimal % 1.0 == 0.0) {
        oneDecimal.toLong().toString()
    } else {
        oneDecimal.toString()
    }
    return "$rounded ${units[digitGroups - 1]}"
}
