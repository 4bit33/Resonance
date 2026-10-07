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
    id = id, title = title,
    artistName = "Artist", albumName = "Album", albumArtist = null,
    genreName = null,
    trackNumber = null, totalTracks = null, discNumber = null, totalDiscs = null,
    year = null, durationMs = 180_000L,
    path = "Music/$id.mp3", contentUri = "content://media/external/audio/media/$id",
    relativePath = "Music/", mimeType = "audio/mpeg", bitrate = null, sampleRate = null,
    fileSizeBytes = 1_000_000L + id,
    dateAddedEpochSec = id, dateModifiedEpochSec = id, lastScannedAtSec = id,
    artworkKey = null, artworkUri = null
)

class FakeMusicRepository(songs: List<Song> = listOf(testSong(1L), testSong(2L))) : MusicRepository {
    private val backing = songs
    var scans = 0
        private set
    var recordedPlays = mutableListOf<Long>()
        private set

    override fun observeSongs(sort: SongSort): Flow<List<Song>> = flowOf(backing)

    override suspend fun getSong(id: Long): Result<Song> =
        backing.firstOrNull { it.id == id }?.let { Result.Success(it) }
            ?: Result.Failure(AppError.MissingFile("song id=$id"))

    override fun searchSongs(query: String): Flow<List<Song>> =
        flowOf(backing.filter { it.title.contains(query, ignoreCase = true) })

    override fun searchAlbums(query: String): Flow<List<com.resonance.player.core.model.Album>> =
        flowOf(emptyList())

    override fun searchArtists(query: String): Flow<List<com.resonance.player.core.model.Artist>> =
        flowOf(emptyList())

    override fun searchGenres(query: String): Flow<List<com.resonance.player.core.model.Genre>> =
        flowOf(emptyList())

    override suspend fun getArtistSongs(artistName: String): Result<List<Song>> =
        Result.Success(backing.filter { it.artistName == artistName })

    override suspend fun getGenreSongs(genreName: String): Result<List<Song>> =
        Result.Success(backing.filter { it.genreName == genreName })

    override suspend fun recordPlay(songId: Long, completed: Boolean): Result<Unit> {
        recordedPlays.add(songId)
        return Result.Success(Unit)
    }

    override suspend fun scanAndImport(): Result<ScanReport> {
        scans++
        return Result.Failure(AppError.FeatureUnavailable("Media scanner (Phase 2)"))
    }

    private val scanStateFlow =
        MutableStateFlow<com.resonance.player.core.media.ScanState>(
            com.resonance.player.core.media.ScanState.Idle
        )

    override fun observeScanState(): kotlinx.coroutines.flow.Flow<com.resonance.player.core.media.ScanState> =
        scanStateFlow

    override fun observeAlbums(): kotlinx.coroutines.flow.Flow<List<com.resonance.player.core.model.Album>> =
        kotlinx.coroutines.flow.flowOf(emptyList())

    override fun observeArtists(): kotlinx.coroutines.flow.Flow<List<com.resonance.player.core.model.Artist>> =
        kotlinx.coroutines.flow.flowOf(emptyList())

    override fun observeGenres(): kotlinx.coroutines.flow.Flow<List<com.resonance.player.core.model.Genre>> =
        kotlinx.coroutines.flow.flowOf(emptyList())

    override fun observeFolders(): kotlinx.coroutines.flow.Flow<List<com.resonance.player.core.model.MusicFolder>> =
        kotlinx.coroutines.flow.flowOf(emptyList())

    override suspend fun getAlbumSongs(
        albumName: String,
        albumArtist: String?
    ): Result<List<Song>> = Result.Success(backing)

    override suspend fun getFolderSongs(relativePath: String?): Result<List<Song>> =
        Result.Success(backing)

    override suspend fun getLibraryStats(): com.resonance.player.core.model.LibraryStats =
        com.resonance.player.core.model.LibraryStats(
            songCount = backing.size, albumCount = 1, artistCount = 1,
            genreCount = 0, lastScanEpochSec = null
        )

    override fun observeLastScanEpochSec(): kotlinx.coroutines.flow.Flow<Long?> =
        kotlinx.coroutines.flow.flowOf(null)

    override fun observeRecentlyPlayed(limit: Int): Flow<List<Song>> =
        flowOf(backing.take(limit))

    override fun observeMostPlayed(limit: Int): Flow<List<Song>> =
        flowOf(backing.take(limit))

    override fun observeRecentlyAdded(limit: Int): Flow<List<Song>> =
        flowOf(backing.take(limit))

    override fun observeStorageOverview(): kotlinx.coroutines.flow.Flow<com.resonance.player.core.model.StorageOverview> =
        kotlinx.coroutines.flow.flowOf(
            com.resonance.player.core.model.StorageOverview(
                trackCount = backing.size,
                libraryBytes = backing.sumOf { it.fileSizeBytes },
                deviceUsedBytes = 0L,
                deviceTotalBytes = 0L
            )
        )

    fun emitScanState(state: com.resonance.player.core.media.ScanState) {
        scanStateFlow.value = state
    }
}

class FakePlaybackController : PlaybackController {
    private val mutable = MutableStateFlow(PlaybackSnapshot.Idle)
    override val snapshot = mutable.asStateFlow()
    val calls = mutableListOf<String>()

    /** Seeds a non-empty queue snapshot for queue-behavior tests. */
    fun seedQueue(songs: List<Song>, index: Int = 0) {
        mutable.value = PlaybackSnapshot(
            song = songs.getOrNull(index),
            queue = songs.mapIndexed { i, song ->
                com.resonance.player.core.model.QueueItem(
                    queueId = i.toLong(),
                    song = song,
                    position = i
                )
            },
            queueIndex = index
        )
    }


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
    override suspend fun moveQueueItem(fromIndex: Int, toIndex: Int): Result<Unit> {
        calls.add("move")
        return Result.Success(Unit)
    }

    override suspend fun appendToQueue(songs: List<Song>): Result<Unit> {
        calls.add("append")
        return Result.Success(Unit)
    }

    override suspend fun insertIntoQueue(index: Int, songs: List<Song>): Result<Unit> {
        calls.add("insert")
        return Result.Success(Unit)
    }

    override suspend fun removeQueueItem(index: Int): Result<Unit> {
        calls.add("remove")
        return Result.Success(Unit)
    }

    override suspend fun clearQueue(): Result<Unit> {
        calls.add("clear")
        return Result.Success(Unit)
    }

    override suspend fun skipToQueueItem(index: Int): Result<Unit> {
        calls.add("skipTo")
        return Result.Success(Unit)
    }
}

class FakePlaylistRepository(
    private val songs: List<Song> = listOf(testSong(1L), testSong(2L))
) : PlaylistRepository {
    private val playlists = mutableListOf<Playlist>()
    private val items = mutableMapOf<Long, MutableList<Long>>()
    var nextId = 2L

    override fun observePlaylists(): Flow<List<Playlist>> = flowOf(playlists)
    override fun observeItems(playlistId: Long): Flow<List<PlaylistItem>> = flowOf(emptyList())

    override fun searchPlaylists(query: String): Flow<List<Playlist>> =
        flowOf(playlists.filter { it.name.contains(query, ignoreCase = true) })
    override suspend fun create(name: String): Result<Playlist> {
        val playlist = Playlist(nextId++, name, 0L, 0L)
        playlists.add(playlist)
        return Result.Success(playlist)
    }

    override suspend fun delete(playlistId: Long): Result<Unit> {
        playlists.removeAll { it.id == playlistId }
        items.remove(playlistId)
        return Result.Success(Unit)
    }

    override suspend fun addSong(playlistId: Long, songId: Long): Result<Unit> {
        val list = items.getOrPut(playlistId) { mutableListOf() }
        if (!list.contains(songId)) list.add(songId)
        return Result.Success(Unit)
    }

    override suspend fun removeSong(playlistId: Long, songId: Long): Result<Unit> {
        items[playlistId]?.remove(songId)
        return Result.Success(Unit)
    }

    override suspend fun rename(playlistId: Long, name: String): Result<Unit> {
        val index = playlists.indexOfFirst { it.id == playlistId }
        if (index < 0) return Result.Failure(AppError.Unknown("Playlist not found"))
        playlists[index] = playlists[index].copy(name = name)
        return Result.Success(Unit)
    }

    override suspend fun moveItem(playlistId: Long, fromPosition: Int, toPosition: Int): Result<Unit> {
        val list = items[playlistId] ?: return Result.Failure(AppError.Unknown("Playlist not found"))
        if (fromPosition !in list.indices || toPosition !in list.indices) {
            return Result.Failure(AppError.Unknown("Invalid playlist position"))
        }
        val moved = list.removeAt(fromPosition)
        list.add(toPosition, moved)
        return Result.Success(Unit)
    }

    override suspend fun getPlaylistSongs(playlistId: Long): Result<List<Song>> {
        val ids = items[playlistId] ?: return Result.Failure(AppError.Unknown("Playlist not found"))
        return Result.Success(ids.mapNotNull { id -> songs.firstOrNull { it.id == id } })
    }
}
