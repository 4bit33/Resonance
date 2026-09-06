package com.resonance.player.core.ui.components

import androidx.compose.runtime.Composable
import com.resonance.player.core.model.Song

/**
 * Shared Stitch format badge: container derived from the real MIME type
 * ("FLAC", "MP3", ...), kilobits from the real bitrate when known.
 * Bit depth is unavailable on-device, so no fake "24b" suffix is shown.
 */
@Composable
fun SongFormatBadge(song: Song) {
    val badge = formatBadgeText(song.mimeType, song.bitrate) ?: return
    ResonanceFormatBadge(
        text = badge,
        highlighted = isLosslessMime(song.mimeType)
    )
}

internal fun formatBadgeText(mimeType: String?, bitrate: Int?): String? {
    val container = when {
        mimeType == null -> return null
        "flac" in mimeType -> "FLAC"
        "mp4" in mimeType || "m4a" in mimeType || "aac" in mimeType -> "AAC"
        "ogg" in mimeType || "opus" in mimeType -> "OGG"
        "wav" in mimeType || "x-wav" in mimeType -> "WAV"
        "mpeg" in mimeType -> "MP3"
        else -> return null
    }
    return if (bitrate != null && bitrate > 0) {
        "$container ${bitrate / 1000}k"
    } else {
        container
    }
}

internal fun isLosslessMime(mimeType: String?): Boolean =
    mimeType != null && ("flac" in mimeType || "wav" in mimeType)
