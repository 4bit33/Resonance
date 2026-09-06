package com.resonance.player.core.media

import com.resonance.player.core.common.Result
import com.resonance.player.core.model.Song

/** Report for one incremental library scan. */
data class ScanReport(val added: Int, val updated: Int, val removed: Int)

/**
 * Boundary for the Phase 2 MediaStore scanner. Implementation must:
 * - run off the main thread (Dispatchers.IO),
 * - page the MediaStore query (never hold the full cursor set in memory),
 * - upsert into Room and prune rows whose files vanished.
 */
interface AudioScanner {
    suspend fun scanLibrary(): Result<ScanReport>
}

/** Raw tags for a single audio file, before it becomes a [Song]. */
data class SongMetadata(
    val title: String?,
    val artistName: String?,
    val albumName: String?,
    val genreName: String?,
    val trackNumber: Int?,
    val discNumber: Int?,
    val year: Int?,
    val durationMs: Long?,
    val mimeType: String?,
    val bitrate: Int?,
    val sampleRate: Int?,
    val bpm: Float?,
    val musicalKey: String?
)

/**
 * Boundary for tag extraction (MediaMetadataRetriever / MediaStore columns).
 * Must tolerate missing or corrupt tags and return [SongMetadata] with nulls
 * rather than throwing; callers map failures to AppError.InvalidMetadata.
 */
interface MediaMetadataExtractor {
    suspend fun extract(contentUri: String): Result<SongMetadata>
}

/** Local-format allow-list. Pure logic, unit-testable, no Android dependency. */
object SupportedFormats {
    val audioExtensions: Set<String> = setOf(
        "mp3", "flac", "ogg", "oga", "opus", "m4a", "aac",
        "wav", "aiff", "aif", "wma", "amr", "mid", "midi", "xmf"
    )

    fun isSupported(path: String): Boolean {
        val ext = path.substringAfterLast(".", missingDelimiterValue = "").lowercase()
        return ext.isNotEmpty() && audioExtensions.contains(ext)
    }
}
