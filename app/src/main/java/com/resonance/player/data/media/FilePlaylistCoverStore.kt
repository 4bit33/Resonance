package com.resonance.player.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import com.resonance.player.core.common.AppDispatchers
import com.resonance.player.domain.playlists.PlaylistCoverStore
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Playlist covers live in the app's private files as JPEGs of at most
 * [MAX_SIDE] px, named "<playlistId>-<time>.jpg". A new name per change, so
 * image caches never show the old picture. The picked original is not kept
 * and never touched.
 */
class FilePlaylistCoverStore(
    private val context: Context,
    private val dispatchers: AppDispatchers
) : PlaylistCoverStore {

    private val directory: File
        get() = File(context.filesDir, "playlist_covers").apply { mkdirs() }

    override suspend fun save(playlistId: Long, sourceUri: String): String? = withContext(dispatchers.io) {
        val bitmap = runCatching { decode(Uri.parse(sourceUri)) }.getOrNull() ?: return@withContext null
        val target = File(directory, "$playlistId-${System.currentTimeMillis()}.jpg")
        val written = runCatching {
            target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        }.getOrDefault(false)
        bitmap.recycle()
        if (!written) {
            target.delete()
            return@withContext null
        }
        filesOf(playlistId).filter { it != target }.forEach { it.delete() }
        Uri.fromFile(target).toString()
    }

    override suspend fun delete(playlistId: Long) {
        withContext(dispatchers.io) { filesOf(playlistId).forEach { it.delete() } }
    }

    private fun filesOf(playlistId: Long): List<File> =
        directory.listFiles { file -> file.name.startsWith("$playlistId-") }?.toList().orEmpty()

    /** ImageDecoder (API 28+) also applies the photo's rotation; older versions decode as stored. */
    private fun decode(uri: Uri): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val longest = maxOf(info.size.width, info.size.height)
                if (longest > MAX_SIDE) {
                    val scale = MAX_SIDE.toFloat() / longest
                    decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    private companion object {
        const val MAX_SIDE = 1024
    }
}
