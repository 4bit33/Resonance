package com.resonance.player.data.media

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.resonance.player.core.media.MediaItemCandidate
import com.resonance.player.core.media.RawRow
import com.resonance.player.core.media.mapCandidate
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** One streamed MediaStore row: a usable candidate or a skipped bad row. */
sealed interface ScanRow {
    data class Row(val candidate: MediaItemCandidate) : ScanRow
    data class Skipped(val reason: String) : ScanRow
}

/**
 * MediaStore is the ONLY discovery mechanism (no raw filesystem walks, no
 * hardcoded /Music paths). Queries run on the collector dispatcher (IO);
 * the cursor streams (CursorWindow pages internally) and is always closed.
 *
 * Query-level failures (SecurityException / IllegalArgumentException /
 * provider errors) propagate to the scanner, which maps them explicitly —
 * per-row failures become [ScanRow.Skipped] and never abort the scan.
 */
class MediaStoreAudioDataSource(private val appContext: Context) {

    fun collectionUri(): Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

    fun collectionUriForVolume(volumeName: String): Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(volumeName)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

    fun projection(): Array<String> {
        val base = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.IS_MUSIC
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            base.add(MediaStore.Audio.AudioColumns.GENRE)
            base.add(MediaStore.MediaColumns.VOLUME_NAME)
            base.add(MediaStore.MediaColumns.RELATIVE_PATH)
        }
        return base.toTypedArray()
    }

    fun selection(): String = MediaStore.Audio.Media.IS_MUSIC + " != 0"

    suspend fun count(): Int {
        val cursor = appContext.contentResolver.query(
            collectionUri(),
            arrayOf(MediaStore.Audio.Media._ID),
            selection(),
            null,
            null
        ) ?: return 0
        cursor.use { return it.count }
    }

    fun streamCandidates(): Flow<ScanRow> = flow {
        val projection = projection()
        val cursor = appContext.contentResolver.query(
            collectionUri(),
            projection,
            selection(),
            null,
            MediaStore.Audio.Media._ID + " ASC"
        ) ?: throw IllegalStateException("MediaStore query returned null cursor")
        cursor.use {
            while (it.moveToNext()) {
                currentCoroutineContext().ensureActive()
                emit(readRow(it))
            }
        }
    }

    private fun readRow(cursor: android.database.Cursor): ScanRow {
        return try {
            val values = HashMap<String, Any?>(32)
            for (i in 0 until cursor.columnCount) {
                values[cursor.getColumnName(i)] = when (cursor.getType(i)) {
                    android.database.Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(i)
                    android.database.Cursor.FIELD_TYPE_STRING -> cursor.getString(i)
                    android.database.Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(i)
                    else -> null
                }
            }
            val mediaId = (values["_id"] as? Long) ?: return ScanRow.Skipped("missing _id")
            val volume = (values["volume_name"] as? String)
                ?.takeIf { s -> s.isNotBlank() } ?: "external"
            values["_uri"] = android.content.ContentUris.withAppendedId(
                collectionUriForVolume(volume), mediaId
            ).toString()
            ScanRow.Row(mapCandidate(RawRow(values)))
        } catch (e: Exception) {
            ScanRow.Skipped(e.message ?: "bad row")
        }
    }
}
