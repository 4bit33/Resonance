package com.resonance.player.domain.library

import com.resonance.player.core.media.UnknownMetadata
import com.resonance.player.core.model.Album
import com.resonance.player.core.model.Song
import kotlinx.coroutines.flow.Flow

/** Recently played songs, most recent first (null/never-played excluded). */
class ObserveRecentlyPlayedUseCase(private val repository: MusicRepository) {
    operator fun invoke(limit: Int = 25): Flow<List<Song>> =
        repository.observeRecentlyPlayed(limit.coerceIn(1, 200))
}

/** Most played songs by play count. */
class ObserveMostPlayedUseCase(private val repository: MusicRepository) {
    operator fun invoke(limit: Int = 25): Flow<List<Song>> =
        repository.observeMostPlayed(limit.coerceIn(1, 200))
}

/** Recently added songs by index date. */
class ObserveRecentlyAddedUseCase(private val repository: MusicRepository) {
    operator fun invoke(limit: Int = 25): Flow<List<Song>> =
        repository.observeRecentlyAdded(limit.coerceIn(1, 200))
}

class ObserveStorageOverviewUseCase(private val repository: MusicRepository) {
    operator fun invoke(): Flow<com.resonance.player.core.model.StorageOverview> =
        repository.observeStorageOverview()
}

/**
 * Derives album cards from a song list, preserving first-seen order and
 * capping the result (Home carousel). Pure and unit-tested.
 */
fun recentAlbumsFromSongs(songs: List<Song>, limit: Int = 10): List<Album> {
    val seen = LinkedHashMap<Pair<String, String?>, MutableList<Song>>()
    for (song in songs) {
        seen.getOrPut(song.albumName to song.albumArtist) { mutableListOf() }.add(song)
    }
    return seen.entries.take(limit.coerceAtLeast(1)).map { (key, group) ->
        val first = group.first()
        val artists = group.map { it.artistName }.distinct()
        Album(
            id = (key.first + "|" + (key.second ?: "")).hashCode().toLong(),
            name = key.first,
            artistName = key.second
                ?: if (artists.size == 1) artists.first() else UnknownMetadata.VARIOUS_ARTISTS,
            albumArtist = key.second,
            songCount = group.size,
            totalDurationMs = group.sumOf { it.durationMs },
            year = group.mapNotNull { it.year }.maxOrNull(),
            artUri = group.firstNotNullOfOrNull { it.artworkUri }
        )
    }
}
