package com.resonance.player.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.resonance.player.core.model.Song

/** Stable Media3 media id for a domain song id. */
fun mediaIdForSong(songId: Long): String = "song:$songId"

/** Parses a media id back to a song id, or null for foreign ids. */
fun mediaIdFor(mediaId: String?): Long? =
    mediaId?.removePrefix("song:")?.toLongOrNull()

/**
 * Android-free request describing one queue entry. Unit tests assert on this
 * type; [toMediaItem] is thin Media3 glue (needs the framework at runtime).
 */
data class MediaRequest(
    val mediaId: String,
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String?,
    val trackNumber: Int?,
    val durationMs: Long,
    val mimeType: String?,
    val artworkUri: String?
)

fun Song.toMediaRequest(): MediaRequest = MediaRequest(
    mediaId = mediaIdForSong(id),
    uri = contentUri,
    title = title,
    artist = artistName,
    album = albumName,
    genre = genreName,
    trackNumber = trackNumber,
    durationMs = durationMs,
    mimeType = mimeType,
    artworkUri = artworkUri
)

/**
 * Domain Song -> Media3 MediaItem. Carries the stable media id, the
 * URI (content:// preferred, never assuming a raw filesystem path) and
 * human metadata for session / notification / lock screen. Artwork is
 * intentionally absent in Phase 2 (artwork pipeline is a later phase;
 * a missing image must never fail playback).
 */
fun MediaRequest.toMediaItem(): MediaItem {
    val metadataBuilder = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(album)
        .setGenre(genre)
        .setTrackNumber(trackNumber)
        .setDurationMs(durationMs.takeIf { it > 0L })
    if (!artworkUri.isNullOrBlank()) {
        metadataBuilder.setArtworkUri(android.net.Uri.parse(artworkUri))
    }
    return MediaItem.Builder()
        .setMediaId(mediaId)
        .setUri(uri)
        .setMimeType(mimeType)
        .setMediaMetadata(metadataBuilder.build())
        .build()
}

fun Song.toMediaItem(): MediaItem = toMediaRequest().toMediaItem()
