package com.resonance.player.data.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.resonance.player.core.common.AppError
import com.resonance.player.core.common.Result
import com.resonance.player.core.media.MediaMetadataExtractor
import com.resonance.player.core.media.SongMetadata
import com.resonance.player.data.media.MetadataNormalizer.parseBitrate
import com.resonance.player.data.media.MetadataNormalizer.parseSampleRate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Tag extraction via platform MediaMetadataRetriever (no third-party tag
 * library: maintained-by-Google, zero size cost, offline, sufficient codec
 * coverage for the required fields). Best-effort per file: missing tags are
 * null, corrupt files map to InvalidMetadata, vanished files to MissingFile.
 * Callers must invoke off the main thread; this class additionally confines
 * itself to Dispatchers.IO so composition stays main-safe.
 */
class AndroidMetadataExtractor(private val appContext: Context) : MediaMetadataExtractor {

    override suspend fun extract(contentUri: String): Result<SongMetadata> =
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                try {
                    retriever.setDataSource(appContext, Uri.parse(contentUri))
                } catch (e: SecurityException) {
                    return@withContext Result.Failure(AppError.PermissionDenied)
                } catch (e: IllegalArgumentException) {
                    return@withContext Result.Failure(AppError.MissingFile(contentUri))
                } catch (e: RuntimeException) {
                    return@withContext Result.Failure(AppError.InvalidMetadata("unreadable file"))
                }
                Result.Success(
                    SongMetadata(
                        title = safeMeta(retriever, MediaMetadataRetriever.METADATA_KEY_TITLE),
                        artistName = safeMeta(retriever, MediaMetadataRetriever.METADATA_KEY_ARTIST),
                        albumName = safeMeta(retriever, MediaMetadataRetriever.METADATA_KEY_ALBUM),
                        albumArtist = safeMeta(
                            retriever, MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST
                        ),
                        genreName = safeMeta(retriever, MediaMetadataRetriever.METADATA_KEY_GENRE),
                        trackRaw = safeMeta(
                            retriever, MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER
                        ),
                        discRaw = safeMeta(
                            retriever, MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER
                        ),
                        yearRaw = safeMeta(retriever, MediaMetadataRetriever.METADATA_KEY_YEAR)
                            ?: safeMeta(retriever, MediaMetadataRetriever.METADATA_KEY_DATE),
                        durationMs = safeMeta(
                            retriever, MediaMetadataRetriever.METADATA_KEY_DURATION
                        )?.toLongOrNull(),
                        mimeType = safeMeta(
                            retriever, MediaMetadataRetriever.METADATA_KEY_MIMETYPE
                        ),
                        bitrate = parseBitrate(
                            safeMeta(retriever, MediaMetadataRetriever.METADATA_KEY_BITRATE)
                        ),
                        sampleRate = parseSampleRate(
                            safeMeta(retriever, MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
                        ),
                        bpm = null,
                        musicalKey = null
                    )
                )
            } finally {
                runCatching { retriever.release() }
            }
        }

    private fun safeMeta(retriever: MediaMetadataRetriever, key: Int): String? =
        try {
            retriever.extractMetadata(key)
        } catch (e: RuntimeException) {
            null
        }
}
