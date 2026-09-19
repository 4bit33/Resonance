package com.resonance.player.core.media

import java.util.UUID

/**
 * One audio file found in a user-added source (a folder tree or a single
 * file). Discovery-only row: tags and artwork are extracted later, and only
 * for new or changed items. Times are epoch SECONDS, sizes are bytes.
 */
data class AudioCandidate(
    val id: Long,
    val sourceId: Long,
    val contentUri: String,
    val displayName: String,
    val mimeType: String?,
    val sizeBytes: Long,
    val dateModifiedSec: Long,
    val relativePath: String?
)

/** One Storage Access Framework directory-listing row, before any Android type touches it. */
data class DocRow(
    val documentId: String,
    val name: String?,
    val mimeType: String?,
    val lastModifiedMs: Long?,
    val sizeBytes: Long?
)

/** Same value as DocumentsContract.Document.MIME_TYPE_DIR (kept literal so this file stays JVM-testable). */
const val DOCUMENT_DIR_MIME = "vnd.android.document/directory"

object StableIds {
    /**
     * Stable song id: 63-bit positive hash of provider authority + documentId
     * (NOT the tree URI), so a file reached through a folder grant and through
     * a single-file grant is one song, and removing then re-adding a source
     * gives the same ids back. This is a PERSISTED FORMAT (favorites, playlists
     * and history key on it): changing it orphans user data. The golden-value
     * test in SafCandidatesTest pins it.
     */
    fun songId(authority: String, documentId: String): Long =
        UUID.nameUUIDFromBytes("$authority/$documentId".toByteArray(Charsets.UTF_8))
            .mostSignificantBits and Long.MAX_VALUE
}

/**
 * Audio filter for SAF listings. MIME first; providers that report no type or
 * application/octet-stream fall back to the extension allow-list. Dot-files
 * are skipped: AppleDouble `._x.mp3` on exFAT cards and `.trashed-*` files are
 * listed by SAF (MediaStore used to hide them) and would fail extraction on
 * every scan.
 */
fun isAudioDoc(name: String?, mimeType: String?): Boolean {
    if (name?.startsWith(".") == true) return false
    if (SupportedMimeTypes.isAudioMime(mimeType)) return true
    val generic = mimeType.isNullOrBlank() || mimeType == "application/octet-stream"
    return generic && name != null && SupportedFormats.isSupported(name)
}

/** Root-relative folder path in the same shape MediaStore used: "Music/Rock/". */
fun joinRelPath(parent: String?, dirName: String): String =
    (parent ?: "") + dirName.trim('/') + "/"

fun audioCandidateOf(
    row: DocRow,
    authority: String,
    sourceId: Long,
    contentUri: String,
    relativePath: String?
): AudioCandidate = AudioCandidate(
    id = StableIds.songId(authority, row.documentId),
    sourceId = sourceId,
    contentUri = contentUri,
    displayName = row.name?.takeIf { it.isNotBlank() }
        ?: row.documentId.substringAfterLast('/').substringAfterLast(':'),
    mimeType = row.mimeType?.takeIf { it.isNotBlank() && it != "application/octet-stream" },
    sizeBytes = (row.sizeBytes ?: 0L).coerceAtLeast(0L),
    dateModifiedSec = (row.lastModifiedMs ?: 0L).coerceAtLeast(0L) / 1000L,
    relativePath = relativePath
)
