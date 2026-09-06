package com.resonance.player.core.media

/**
 * What MediaStore knows about one audio item BEFORE deeper metadata
 * extraction. Lightweight discovery row: the reconciler diffs these against
 * stored fingerprints to decide NEW / MODIFIED / UNCHANGED / DELETED without
 * touching tags or artwork for unchanged files.
 *
 * Times are epoch SECONDS (MediaStore DATE_MODIFIED unit); sizes are bytes.
 */
data class MediaItemCandidate(
    val mediaStoreId: Long,
    val volumeName: String,
    val contentUri: String,
    val displayName: String,
    val mimeType: String?,
    val sizeBytes: Long,
    val dateModifiedSec: Long,
    val dateAddedSec: Long,
    val durationMs: Long,
    val relativePath: String?,
    val title: String?,
    val artist: String?,
    val album: String?,
    val genre: String?,
    val year: Int?,
    val track: Int?,
    val albumId: Long?,
    val artistId: Long?
)

/**
 * One MediaStore cursor row as untyped column values. The Android glue reads
 * the Cursor once per row into this map; the pure [mapCandidate] function
 * below turns it into a [MediaItemCandidate] and is fully JVM-testable
 * without Robolectric (no android.database classes leak into logic).
 */
data class RawRow(val values: Map<String, Any?>) {
    fun string(key: String): String? = (values[key] as? String)?.takeIf { it.isNotEmpty() }
    fun long(key: String): Long? = when (val v = values[key]) {
        is Long -> v
        is Int -> v.toLong()
        is String -> v.toLongOrNull()
        else -> null
    }
    fun int(key: String): Int? = when (val v = values[key]) {
        is Int -> v
        is Long -> v.toInt()
        is String -> v.toIntOrNull()
        else -> null
    }
}

/**
 * Builds the item content URI for a volume without assuming a raw path.
 * [volumeName] is the MediaStore volume ("external" pre-Q, "external_primary"
 * or a storage UUID on Q+); callers pass whatever the row reported.
 */
fun contentUriFor(volumeName: String, mediaStoreId: Long): String =
    "content://media/" + volumeName + "/audio/media/" + mediaStoreId

/** Pure row mapping: RawRow -> candidate. Never throws for bad values. */
fun mapCandidate(row: RawRow, volumeFallback: String = "external"): MediaItemCandidate {
    val mediaStoreId = row.long("_id") ?: -1L
    val volume = row.string("volume_name")?.takeIf { it.isNotBlank() } ?: volumeFallback
    val explicitUri = row.string("_uri")
    return MediaItemCandidate(
        mediaStoreId = mediaStoreId,
        volumeName = volume,
        contentUri = explicitUri ?: contentUriFor(volume, mediaStoreId),
        displayName = row.string("_display_name") ?: "unknown",
        mimeType = row.string("mime_type"),
        sizeBytes = row.long("_size") ?: 0L,
        dateModifiedSec = row.long("date_modified") ?: 0L,
        dateAddedSec = row.long("date_added") ?: 0L,
        durationMs = row.long("duration") ?: 0L,
        relativePath = row.string("relative_path"),
        title = row.string("title"),
        artist = row.string("artist"),
        album = row.string("album"),
        genre = row.string("genre"),
        year = row.int("year"),
        track = row.int("track"),
        albumId = row.long("album_id"),
        artistId = row.long("artist_id")
    )
}
