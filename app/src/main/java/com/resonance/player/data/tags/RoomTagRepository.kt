package com.resonance.player.data.tags

import com.resonance.player.core.database.entity.GenreOverrideEntity
import com.resonance.player.domain.tags.TagSnapshot
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.resonance.player.core.common.AppDispatchers
import com.resonance.player.core.common.AppError
import com.resonance.player.core.common.Result
import com.resonance.player.core.database.ResonanceDatabase
import com.resonance.player.core.database.entity.TagOverrideEntity
import com.resonance.player.data.media.ArtworkStore
import com.resonance.player.domain.tags.SongTags
import com.resonance.player.domain.tags.TagRepository
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Saves fixed tags as overrides (files are not rewritten). A cover is taken
 * from a URL (online lookup) or a picked picture, checked to be a real image
 * and kept in app-private storage, so cache clearing never loses it.
 */
class RoomTagRepository(
    private val context: Context,
    private val database: ResonanceDatabase,
    private val coverStore: ArtworkStore,
    private val dispatchers: AppDispatchers,
    private val userAgent: String
) : TagRepository {

    override suspend fun save(songId: Long, tags: SongTags, coverSource: String?): Result<Unit> = withContext(dispatchers.io) {
        try {
            val cover = coverSource?.let { source ->
                val bytes = readImage(source) ?: return@withContext Result.Failure(AppError.Unknown("The cover could not be loaded"))
                coverStore.store(bytes) ?: return@withContext Result.Failure(AppError.Unknown("The cover could not be saved"))
            }
            val previous = database.tagOverrideDao().get(songId)
            database.tagOverrideDao().save(
                TagOverrideEntity(
                    songId = songId,
                    title = tags.title.trim().ifBlank { null },
                    artist = tags.artist.trim().ifBlank { null },
                    album = tags.album.trim().ifBlank { null },
                    albumArtist = tags.albumArtist.trim().ifBlank { null },
                    year = tags.year.trim().toIntOrNull(),
                    trackNumber = tags.trackNumber.trim().toIntOrNull(),
                    artworkKey = cover?.key ?: previous?.artworkKey,
                    artworkUri = cover?.uri ?: previous?.artworkUri
                )
            )
            database.genreOverrideDao().override(listOf(songId), tags.genre.trim().ifBlank { null })
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.DatabaseError(e.message))
        }
    }

    override suspend fun reset(songId: Long): Result<Unit> = withContext(dispatchers.io) {
        try {
            database.tagOverrideDao().reset(songId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.DatabaseError(e.message))
        }
    }

    override suspend fun snapshot(songId: Long): TagSnapshot = withContext(dispatchers.io) {
        val o = database.tagOverrideDao().get(songId)
        val g = database.genreOverrideDao().get(songId)
        TagSnapshot(
            hadOverride = o != null,
            title = o?.title,
            artist = o?.artist,
            album = o?.album,
            albumArtist = o?.albumArtist,
            year = o?.year,
            trackNumber = o?.trackNumber,
            artworkKey = o?.artworkKey,
            artworkUri = o?.artworkUri,
            hadGenreOverride = g != null,
            genre = g?.genre
        )
    }

    override suspend fun restore(songId: Long, snapshot: TagSnapshot): Result<Unit> = withContext(dispatchers.io) {
        try {
            val tags = database.tagOverrideDao()
            if (snapshot.hadOverride) {
                tags.upsert(
                    TagOverrideEntity(
                        songId, snapshot.title, snapshot.artist, snapshot.album, snapshot.albumArtist,
                        snapshot.year, snapshot.trackNumber, snapshot.artworkKey, snapshot.artworkUri
                    )
                )
            } else {
                tags.delete(songId)
            }
            if (snapshot.hadGenreOverride) {
                database.genreOverrideDao().upsert(listOf(GenreOverrideEntity(songId, snapshot.genre)))
            } else {
                database.genreOverrideDao().delete(songId)
            }
            // The next scan re-reads the file, then lays the restored fixes over it.
            tags.markForReread(songId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.DatabaseError(e.message))
        }
    }

    private fun readImage(source: String): ByteArray? {
        val bytes = if (source.startsWith("http")) {
            val connection = (URL(source).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 20_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", userAgent)
            }
            try {
                if (connection.responseCode !in 200..299) return null
                connection.inputStream.use { it.readBytes() }
            } finally {
                connection.disconnect()
            }
        } else {
            context.contentResolver.openInputStream(Uri.parse(source))?.use { it.readBytes() }
        } ?: return null
        if (bytes.isEmpty() || bytes.size > 10 * 1024 * 1024) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        return bytes.takeIf { bounds.outWidth > 0 && bounds.outHeight > 0 }
    }
}
