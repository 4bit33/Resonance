package com.resonance.player.core.database

import com.resonance.player.core.database.entity.SongEntity
import com.resonance.player.core.model.Song

/**
 * Entity <-> domain mapping. The ONLY place Room types convert to UI-safe
 * domain models (ADR-004). Favorites are joined at the repository layer.
 */
fun SongEntity.toDomain(isFavorite: Boolean = false): Song = Song(
    id = id,
    mediaStoreId = mediaStoreId,
    title = title,
    artistName = artistName,
    albumName = albumName,
    albumId = albumId,
    artistId = artistId,
    genreName = genreName,
    trackNumber = trackNumber,
    discNumber = discNumber,
    year = year,
    durationMs = durationMs,
    path = path,
    contentUri = contentUri,
    mimeType = mimeType,
    bitrate = bitrate,
    sampleRate = sampleRate,
    dateAddedEpochSec = dateAddedEpochSec,
    dateModifiedEpochSec = dateModifiedEpochSec,
    playCount = playCount,
    lastPlayedEpochSec = lastPlayedEpochSec,
    isFavorite = isFavorite,
    bpm = bpm,
    musicalKey = musicalKey
)

fun Song.toEntity(): SongEntity = SongEntity(
    id = id,
    mediaStoreId = mediaStoreId,
    title = title,
    artistName = artistName,
    albumName = albumName,
    albumId = albumId,
    artistId = artistId,
    genreName = genreName,
    trackNumber = trackNumber,
    discNumber = discNumber,
    year = year,
    durationMs = durationMs,
    path = path,
    contentUri = contentUri,
    mimeType = mimeType,
    bitrate = bitrate,
    sampleRate = sampleRate,
    dateAddedEpochSec = dateAddedEpochSec,
    dateModifiedEpochSec = dateModifiedEpochSec,
    playCount = playCount,
    lastPlayedEpochSec = lastPlayedEpochSec,
    bpm = bpm,
    musicalKey = musicalKey
)
