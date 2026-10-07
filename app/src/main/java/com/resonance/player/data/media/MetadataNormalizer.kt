package com.resonance.player.data.media

import com.resonance.player.core.media.AudioCandidate
import com.resonance.player.core.media.SongMetadata
import com.resonance.player.core.media.UnknownMetadata

/**
 * Display-ready normalized track fields. Normalization is deterministic and
 * pure: trim/collapse whitespace, drop control characters, never touch case
 * or transliteration, never mutate the source file. Missing values fall back
 * to filename-derived or Unknown-* display constants.
 */
data class NormalizedTrack(
    val title: String,
    val artistName: String,
    val albumName: String,
    val albumArtist: String?,
    val genreName: String?,
    val trackNumber: Int?,
    val totalTracks: Int?,
    val discNumber: Int?,
    val totalDiscs: Int?,
    val year: Int?,
    val durationMs: Long,
    val mimeType: String?,
    val bitrate: Int?,
    val sampleRate: Int?,
    val displayPath: String
)

object MetadataNormalizer {

    /**
     * Priority: embedded tags (MediaMetadataRetriever) > filename/display
     * fallbacks. A missing tag is normal; only the final display value must
     * never be blank, "null" or "undefined".
     */
    fun normalize(candidate: AudioCandidate, extracted: SongMetadata?): NormalizedTrack {
        val (track, totalTracks) = parseTrackNumber(extracted?.trackRaw)
        val (disc, totalDiscs) = parseTrackNumber(extracted?.discRaw)
        return NormalizedTrack(
            title = clean(extracted?.title)
                ?: FilenameFallback.titleFromFileName(candidate.displayName),
            artistName = clean(extracted?.artistName) ?: UnknownMetadata.ARTIST,
            albumName = clean(extracted?.albumName) ?: UnknownMetadata.ALBUM,
            albumArtist = clean(extracted?.albumArtist),
            genreName = clean(extracted?.genreName),
            trackNumber = track,
            totalTracks = totalTracks,
            discNumber = disc,
            totalDiscs = totalDiscs,
            year = parseYear(extracted?.yearRaw),
            durationMs = extracted?.durationMs?.takeIf { it > 0L } ?: 0L,
            mimeType = extracted?.mimeType?.takeIf { it.isNotBlank() }
                ?: candidate.mimeType,
            bitrate = extracted?.bitrate,
            sampleRate = extracted?.sampleRate,
            displayPath = displayPathFor(candidate.relativePath, candidate.displayName)
        )
    }

    /**
     * Trims, collapses inner whitespace and strips control characters while
     * preserving meaningful Unicode (no case folding, no transliteration).
     * Returns null for blank results so callers can apply fallbacks.
     */
    fun clean(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val collapsed = raw.trim().replace("\\s+".toRegex(), " ")
        val stripped = collapsed.filterNot { it.isISOControl() }
        return stripped.trim().takeIf { it.isNotEmpty() }
    }

    /**
     * Parses "1", "01", "1/12", "01/12" (and noisy variants) into
     * (number, total). Non-positive numbers are treated as absent so bogus
     * "0" tags never corrupt track sorting.
     */
    fun parseTrackNumber(raw: String?): Pair<Int?, Int?> {
        if (raw.isNullOrBlank()) return null to null
        val parts = raw.trim().split("/")
        val number = parts.getOrNull(0)?.trim()?.toIntOrNull()?.takeIf { it > 0 }
        val total = parts.getOrNull(1)?.trim()?.toIntOrNull()?.takeIf { it > 0 }
        if (number == null && total == null) return null to null
        return number to total
    }

    /**
     * Extracts a sane year from "2021", "2021-05-01" or similar. Values
     * outside 1000..2999 are rejected (durations-in-disguise or garbage).
     */
    fun parseYear(raw: String?): Int? {
        if (raw.isNullOrBlank()) return null
        val match = "([12][0-9]{3})".toRegex().find(raw.trim())
        return match?.value?.toIntOrNull()?.takeIf { it in 1000..2999 }
    }

    fun parseBitrate(raw: String?): Int? =
        raw?.trim()?.toIntOrNull()?.takeIf { it > 0 }

    fun parseSampleRate(raw: String?): Int? =
        raw?.trim()?.toIntOrNull()?.takeIf { it > 0 }

    fun displayPathFor(relativePath: String?, displayName: String): String {
        val folder = relativePath?.trim()?.trimEnd('/')?.takeIf { it.isNotEmpty() }
        return if (folder == null) displayName else folder + "/" + displayName
    }
}

/**
 * Derives a display title from a file name when tags are absent. Conservative:
 * strips the extension and an obvious leading track prefix ("01 - Name"),
 * never rewrites anything else. The 4-digit guard keeps years ("1984 - ...")
 * intact.
 */
object FilenameFallback {
    private val trackPrefix = "^\\d{1,3}\\s*[-._\\s]+".toRegex()

    fun titleFromFileName(displayName: String): String {
        val withoutExtension = displayName.substringBeforeLast(".", missingDelimiterValue = displayName)
        val withoutPrefix = withoutExtension.replace(trackPrefix, "")
        val cleaned = withoutPrefix.trim().replace("\\s+".toRegex(), " ")
        return cleaned.takeIf { it.isNotEmpty() } ?: UnknownMetadata.TITLE
    }
}
