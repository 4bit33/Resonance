package com.resonance.player.data.media

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/** Reference to one deduplicated cached artwork file. */
data class ArtworkRef(val key: String, val uri: String)

/** Boundary for embedded-artwork extraction (fakes in tests). */
interface ArtworkExtractor {
    suspend fun extractArtwork(contentUri: String): ByteArray?
}

/**
 * Embedded-picture extraction via MediaMetadataRetriever. Returns raw bytes
 * only when they look like a real image (bounds-decodable) and fit the size
 * cap; corrupt/oversized art is skipped (song still imports) — a missing
 * image is never a playback or scan error.
 */
class RetrieverArtworkExtractor(private val appContext: Context) : ArtworkExtractor {

    override suspend fun extractArtwork(contentUri: String): ByteArray? =
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                try {
                    retriever.setDataSource(appContext, Uri.parse(contentUri))
                } catch (e: Exception) {
                    return@withContext null
                }
                val bytes = try {
                    retriever.embeddedPicture
                } catch (e: RuntimeException) {
                    null
                } ?: return@withContext null
                if (bytes.isEmpty() || bytes.size > MAX_ARTWORK_BYTES) return@withContext null
                if (!isDecodableImage(bytes)) return@withContext null
                bytes
            } finally {
                runCatching { retriever.release() }
            }
        }

    private fun isDecodableImage(bytes: ByteArray): Boolean = try {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        options.outWidth > 0 && options.outHeight > 0
    } catch (e: Exception) {
        false
    }

    companion object {
        const val MAX_ARTWORK_BYTES = 5 * 1024 * 1024
    }
}

/**
 * Deterministic file cache for artwork bytes. Key = SHA-256 hex of the exact
 * bytes, so identical covers across an album (or library) share ONE file
 * (deduplication); changed art hashes differently and replaces naturally.
 *
 * Layout: <dir>/<sha256>.bin (app-owned cache dir; never user folders).
 * Filenames are app-generated hashes — metadata strings can never become
 * paths. Missing/corrupt files resolve to null (UI falls back).
 */
/** [fileUri] builds the stored URI for a cache file (android.net.Uri in production, plain strings in tests). */
class ArtworkStore(
    private val directory: File,
    private val fileUri: (File) -> String = { android.net.Uri.fromFile(it).toString() }
) {

    suspend fun store(bytes: ByteArray): ArtworkRef? = withContext(Dispatchers.IO) {
        if (bytes.isEmpty()) return@withContext null
        val key = sha256Hex(bytes)
        val file = File(directory, "$key.bin")
        if (!file.exists()) {
            directory.mkdirs()
            val tmp = File(directory, "$key.tmp")
            try {
                tmp.writeBytes(bytes)
                if (!tmp.renameTo(file) && !file.exists()) return@withContext null
            } finally {
                tmp.delete()
            }
        }
        ArtworkRef(key, fileUri(file))
    }

    /** File URI for a stored key, or null when the file is gone/invalid. */
    fun uriFor(key: String?): String? {
        if (key.isNullOrBlank() || key.contains('/') || key.contains('\\')) return null
        val file = File(directory, "$key.bin")
        return if (file.isFile && file.length() > 0L) {
            fileUri(file)
        } else {
            null
        }
    }

    /**
     * Deletes cached files nobody references anymore (plus zero-length
     * leftovers). Returns the deleted count for scan diagnostics.
     */
    suspend fun prune(keepKeys: Set<String>): Int = withContext(Dispatchers.IO) {
        val files = directory.listFiles() ?: return@withContext 0
        var deleted = 0
        for (file in files) {
            val name = file.name
            val key = name.removeSuffix(".bin").takeIf { name.endsWith(".bin") }
            try {
                if (file.isFile && (key == null || key !in keepKeys || file.length() == 0L)) {
                    if (file.delete()) deleted++
                }
            } catch (e: SecurityException) {
                // Best-effort cleanup; a single stubborn file never fails a scan.
            }
        }
        deleted
    }

    companion object {
        fun sha256Hex(bytes: ByteArray): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
            return digest.joinToString("") { it.toInt().and(0xFF).toString(16).padStart(2, '0') }
        }
    }
}
