package com.resonance.player.core.database

import com.resonance.player.core.database.entity.SongEntity
import com.resonance.player.core.model.Album
import com.resonance.player.core.model.Artist
import com.resonance.player.core.model.Genre
import com.resonance.player.core.model.MusicFolder
import com.resonance.player.core.model.Song
import com.resonance.player.core.media.UnknownMetadata
import com.resonance.player.core.database.dao.AlbumRow
import com.resonance.player.core.database.dao.ArtistRow
import com.resonance.player.core.database.dao.FolderRow
import com.resonance.player.core.database.dao.GenreRow

/**
 * Entity <-> domain mapping. The ONLY place Room types convert to UI-safe
 * domain models (ADR-004). Favorites are joined at the repository layer.
 */
fun SongEntity.toDomain(isFavorite: Boolean = false): Song = Song(
    id = id,
    mediaStoreId = mediaStoreId,
    volumeName = volumeName,
    title = title,
    artistName = artistName,
    albumName = albumName,
    albumArtist = albumArtist,
    albumId = albumId,
    artistId = artistId,
    genreName = genreName,
    trackNumber = trackNumber,
    totalTracks = totalTracks,
    discNumber = discNumber,
    totalDiscs = totalDiscs,
    year = year,
    durationMs = durationMs,
    path = path,
    contentUri = contentUri,
    relativePath = relativePath,
    mimeType = mimeType,
    bitrate = bitrate,
    sampleRate = sampleRate,
    fileSizeBytes = fileSizeBytes,
    dateAddedEpochSec = dateAddedEpochSec,
    dateModifiedEpochSec = dateModifiedEpochSec,
    lastScannedAtSec = lastScannedAtSec,
    artworkKey = artworkKey,
    artworkUri = artworkUri,
    playCount = playCount,
    lastPlayedEpochSec = lastPlayedEpochSec,
    isFavorite = isFavorite,
    bpm = bpm,
    musicalKey = musicalKey
)

fun Song.toEntity(): SongEntity = SongEntity(
    id = id,
    mediaStoreId = mediaStoreId,
    volumeName = volumeName,
    title = title,
    artistName = artistName,
    albumName = albumName,
    albumArtist = albumArtist,
    albumId = albumId,
    artistId = artistId,
    genreName = genreName,
    trackNumber = trackNumber,
    totalTracks = totalTracks,
    discNumber = discNumber,
    totalDiscs = totalDiscs,
    year = year,
    durationMs = durationMs,
    path = path,
    contentUri = contentUri,
    relativePath = relativePath,
    mimeType = mimeType,
    bitrate = bitrate,
    sampleRate = sampleRate,
    fileSizeBytes = fileSizeBytes,
    dateAddedEpochSec = dateAddedEpochSec,
    dateModifiedEpochSec = dateModifiedEpochSec,
    lastScannedAtSec = lastScannedAtSec,
    artworkKey = artworkKey,
    artworkUri = artworkUri,
    playCount = playCount,
    lastPlayedEpochSec = lastPlayedEpochSec,
    bpm = bpm,
    musicalKey = musicalKey
)

/**
 * Merges a freshly scanned entity over the stored one, PRESERVING playback
 * statistics. The scanner owns library fields; playCount/lastPlayed belong
 * to the playback subsystem and must survive rescans.
 */
fun SongEntity.withPreservedStats(existing: SongEntity?): SongEntity {
    if (existing == null) return this
    return copy(playCount = existing.playCount, lastPlayedEpochSec = existing.lastPlayedEpochSec)
}

/** Stable display id for an album group (non-persistent, UI keys only). */
fun albumGroupId(albumName: String, albumArtist: String?): Long {
    val key = albumName.lowercase() + "|" + (albumArtist?.lowercase() ?: "")
    return key.hashCode().toLong()
}

fun AlbumRow.toDomain(): Album {
    val displayArtist = albumArtist?.takeIf { it.isNotBlank() }
        ?: if (distinctArtists == 1) (sampleArtist ?: UnknownMetadata.ARTIST)
        else UnknownMetadata.VARIOUS_ARTISTS
    return Album(
        id = albumGroupId(albumName, albumArtist),
        name = albumName,
        artistName = displayArtist,
        albumArtist = albumArtist?.takeIf { it.isNotBlank() },
        songCount = songCount,
        totalDurationMs = totalDurationMs,
        year = year,
        artUri = sampleArtworkUri
    )
}

fun ArtistRow.toDomain(): Artist = Artist(
    id = artistName.lowercase().hashCode().toLong(),
    name = artistName,
    songCount = songCount,
    albumCount = albumCount
)

fun GenreRow.toDomain(): Genre = Genre(name = genreName, songCount = songCount)

fun FolderRow.toDomain(): MusicFolder {
    val path = relativePath ?: ""
    val name = path.trimEnd('/').substringAfterLast('/', missingDelimiterValue = "")
        .takeIf { it.isNotEmpty() } ?: "Device storage"
    return MusicFolder(path = path, name = name, songCount = songCount)
}
