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

enum class SongSort { TITLE, ARTIST, ALBUM, DATE_ADDED, LAST_PLAYED, PLAY_COUNT }

/**
 * Library repository contract. Implemented by data/local with Room;
 * faked in tests. Flows must stay cold — collection happens on IO.
 */
interface MusicRepository {
    fun observeSongs(sort: SongSort = SongSort.TITLE): Flow<List<Song>>
    suspend fun getSong(id: Long): Result<Song>
    fun searchSongs(query: String): Flow<List<Song>>
    suspend fun recordPlay(songId: Long, completed: Boolean): Result<Unit>
    suspend fun scanAndImport(): Result<ScanReport>
    fun observeScanState(): Flow<ScanState>
    fun observeAlbums(): Flow<List<Album>>
    fun observeArtists(): Flow<List<Artist>>
    fun observeGenres(): Flow<List<Genre>>
    fun observeFolders(): Flow<List<MusicFolder>>
    suspend fun getAlbumSongs(albumName: String, albumArtist: String?): Result<List<Song>>
    suspend fun getLibraryStats(): LibraryStats
    fun observeLastScanEpochSec(): Flow<Long?>
}

/** Reactive song list for Library/Home screens. */
class ObserveSongsUseCase(private val repository: MusicRepository) {
    operator fun invoke(sort: SongSort = SongSort.TITLE): Flow<List<Song>> =
        repository.observeSongs(sort)
}

/** Single-song lookup for the details / Now Playing screen. */
class GetSongUseCase(private val repository: MusicRepository) {
    suspend operator fun invoke(id: Long): Result<Song> = repository.getSong(id)
}

/** Play-count + history bookkeeping; called by the Phase 2 player only. */
class RecordPlayUseCase(private val repository: MusicRepository) {
    suspend operator fun invoke(songId: Long, completed: Boolean): Result<Unit> =
        repository.recordPlay(songId, completed)
}
