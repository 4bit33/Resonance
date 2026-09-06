package com.resonance.player.core.media

import com.resonance.player.core.common.Result
import com.resonance.player.core.model.Song

/**
 * Report for one incremental library scan. [failed] counts items that could
 * not be imported (corrupt/unreadable) without aborting the scan; [total] is
 * the number of MediaStore candidates seen.
 */
data class ScanReport(
    val added: Int,
    val updated: Int,
    val removed: Int,
    val failed: Int = 0,
    val total: Int = 0
)

/** Authoritative scanner state observed by Library/Settings UI. */
sealed interface ScanState {
    data object Idle : ScanState
    data object CheckingPermission : ScanState
    data class Scanning(
        val processed: Int,
        val total: Int,
        val added: Int,
        val updated: Int
    ) : ScanState
    data class Completed(val report: ScanReport) : ScanState
    data object PermissionRequired : ScanState
    data class Failed(val error: com.resonance.player.core.common.AppError) : ScanState
    data object Cancelled : ScanState
}

/**
 * Boundary for the MediaStore scanner implementation. Must:
 * - run off the main thread (Dispatchers.IO),
 * - stream the MediaStore query (never hold full result sets in memory),
 * - extract metadata only for new/changed items (incremental),
 * - upsert into Room in batches and prune rows whose files vanished,
 * - be single-flight (one authoritative scan at a time) and cancellable.
 */
interface AudioScanner {
    val state: kotlinx.coroutines.flow.StateFlow<ScanState>
    suspend fun scanLibrary(): Result<ScanReport>
    fun cancel()
}

/** Raw tags for a single audio file, before normalization. Textual fields stay RAW strings: "01/12" or "2021-05-01" are parsed by the pure MetadataNormalizer, not by Android glue. */
data class SongMetadata(
    val title: String?,
    val artistName: String?,
    val albumName: String?,
    val albumArtist: String?,
    val genreName: String?,
    val trackRaw: String?,
    val discRaw: String?,
    val yearRaw: String?,
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

/**
 * MIME-based audio classification. MIME type (not the extension) is the
 * primary discovery signal; playlist/manifest types are explicitly excluded
 * so .m3u/.pls sidecars never become library songs.
 */
object SupportedMimeTypes {
    private val playlistMimes = setOf(
        "audio/x-mpegurl",
        "audio/mpegurl",
        "application/vnd.apple.mpegurl",
        "audio/x-scpls"
    )

    fun isAudioMime(mimeType: String?): Boolean {
        if (mimeType.isNullOrBlank()) return false
        val normalized = mimeType.trim().lowercase().substringBefore(";").trim()
        if (normalized in playlistMimes) return false
        if (normalized == "application/ogg") return true
        return normalized.startsWith("audio/")
    }
}
