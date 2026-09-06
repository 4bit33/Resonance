package com.resonance.player.fakes

import com.resonance.player.core.common.AppError
import com.resonance.player.core.common.Result
import com.resonance.player.core.media.ScanReport
import com.resonance.player.core.model.PlaybackSnapshot
import com.resonance.player.core.model.RepeatMode
import com.resonance.player.core.model.ShuffleMode
import com.resonance.player.core.model.Song
import com.resonance.player.core.playback.PlaybackController
import com.resonance.player.domain.library.MusicRepository
import com.resonance.player.domain.library.SongSort
import com.resonance.player.domain.playlists.PlaylistRepository
import com.resonance.player.core.model.Playlist
import com.resonance.player.core.model.PlaylistItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf

fun testSong(id: Long, title: String = "Song $id") = Song(
    id = id, mediaStoreId = id, title = title, artistName = "Artist",
    albumName = "Album", albumId = null, artistId = null, genreName = null,
    trackNumber = null, discNumber = null, year = null, durationMs = 180_000L,
    path = "/music/$id.mp3", contentUri = "content://media/$id",
    mimeType = "audio/mpeg", bitrate = null, sampleRate = null,
    dateAddedEpochSec = id, dateModifiedEpochSec = id
)

class FakeMusicRepository(songs: List<Song> = listOf(testSong(1L), testSong(2L))) : MusicRepository {
    private val backing = songs
    var recordedPlays = mutableListOf<Long>()
        private set

    override fun observeSongs(sort: SongSort): Flow<List<Song>> = flowOf(backing)

    override suspend fun getSong(id: Long): Result<Song> =
        backing.firstOrNull { it.id == id }?.let { Result.Success(it) }
            ?: Result.Failure(AppError.MissingFile("song id=$id"))

    override fun searchSongs(query: String): Flow<List<Song>> =
        flowOf(backing.filter { it.title.contains(query, ignoreCase = true) })

    override suspend fun recordPlay(songId: Long, completed: Boolean): Result<Unit> {
        recordedPlays.add(songId)
        return Result.Success(Unit)
    }

    override suspend fun scanAndImport(): Result<ScanReport> =
        Result.Failure(AppError.FeatureUnavailable("Media scanner (Phase 2)"))
}

class FakePlaybackController : PlaybackController {
    private val mutable = MutableStateFlow(PlaybackSnapshot.Idle)
    override val snapshot = mutable.asStateFlow()
    val calls = mutableListOf<String>()

    override suspend fun play(queue: List<Song>, startIndex: Int): Result<Unit> {
        calls.add("play")
        return Result.Success(Unit)
    }

    override suspend fun resume(): Result<Unit> {
        calls.add("resume")
        mutable.value = mutable.value.copy(isPlaying = true)
        return Result.Success(Unit)
    }

    override suspend fun pause(): Result<Unit> {
        calls.add("pause")
        mutable.value = mutable.value.copy(isPlaying = false)
        return Result.Success(Unit)
    }

    override suspend fun stop(): Result<Unit> {
        calls.add("stop")
        return Result.Success(Unit)
    }

    override suspend fun seekTo(positionMs: Long): Result<Unit> = Result.Success(Unit)
    override suspend fun skipToNext(): Result<Unit> = Result.Success(Unit)
    override suspend fun skipToPrevious(): Result<Unit> = Result.Success(Unit)
    override suspend fun setShuffle(mode: ShuffleMode): Result<Unit> = Result.Success(Unit)
    override suspend fun setRepeat(mode: RepeatMode): Result<Unit> = Result.Success(Unit)
    override suspend fun moveQueueItem(fromIndex: Int, toIndex: Int): Result<Unit> =
        Result.Success(Unit)
}

class FakePlaylistRepository : PlaylistRepository {
    private val playlists = mutableListOf<Playlist>()
    override fun observePlaylists(): Flow<List<Playlist>> = flowOf(playlists)
    override fun observeItems(playlistId: Long): Flow<List<PlaylistItem>> = flowOf(emptyList())
    override suspend fun create(name: String): Result<Playlist> {
        val playlist = Playlist(1L, name, 0L, 0L)
        playlists.add(playlist)
        return Result.Success(playlist)
    }

    override suspend fun delete(playlistId: Long): Result<Unit> = Result.Success(Unit)
    override suspend fun addSong(playlistId: Long, songId: Long): Result<Unit> = Result.Success(Unit)
    override suspend fun removeSong(playlistId: Long, songId: Long): Result<Unit> =
        Result.Success(Unit)
}
