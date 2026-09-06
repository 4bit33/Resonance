package com.resonance.player.domain.library

import com.resonance.player.core.common.Result
import com.resonance.player.core.media.ScanReport
import com.resonance.player.core.media.ScanState
import com.resonance.player.core.model.Album
import com.resonance.player.core.model.Artist
import com.resonance.player.core.model.Genre
import com.resonance.player.core.model.LibraryStats
import com.resonance.player.core.model.MusicFolder
import com.resonance.player.core.model.Song
import kotlinx.coroutines.flow.Flow

/** Browse aggregates for the Library tabs (GROUP BY queries, never full loads). */
class ObserveAlbumsUseCase(private val repository: MusicRepository) {
    operator fun invoke(): Flow<List<Album>> = repository.observeAlbums()
}

class ObserveArtistsUseCase(private val repository: MusicRepository) {
    operator fun invoke(): Flow<List<Artist>> = repository.observeArtists()
}

class ObserveGenresUseCase(private val repository: MusicRepository) {
    operator fun invoke(): Flow<List<Genre>> = repository.observeGenres()
}

class ObserveFoldersUseCase(private val repository: MusicRepository) {
    operator fun invoke(): Flow<List<MusicFolder>> = repository.observeFolders()
}

/** Songs of one album in disc/track order, for tap-to-play-album. */
class GetAlbumSongsUseCase(private val repository: MusicRepository) {
    suspend operator fun invoke(albumName: String, albumArtist: String?): Result<List<Song>> =
        repository.getAlbumSongs(albumName, albumArtist)
}

/** Scanner state for progress banners and Settings. */
class ObserveScanStateUseCase(private val repository: MusicRepository) {
    operator fun invoke(): Flow<ScanState> = repository.observeScanState()
}

/** Explicit user-triggered rescan (Settings action). */
class RescanLibraryUseCase(private val repository: MusicRepository) {
    suspend operator fun invoke(): Result<ScanReport> = repository.scanAndImport()
}

/** Cheap COUNT-based stats for Settings headers. */
class GetLibraryStatsUseCase(private val repository: MusicRepository) {
    suspend operator fun invoke(): LibraryStats = repository.getLibraryStats()
}

class ObserveLastScanUseCase(private val repository: MusicRepository) {
    operator fun invoke(): Flow<Long?> = repository.observeLastScanEpochSec()
}
