package com.resonance.player.domain.playlists

import com.resonance.player.core.common.AppError
import com.resonance.player.core.common.Result
import com.resonance.player.core.model.Playlist
import com.resonance.player.core.model.PlaylistItem
import com.resonance.player.core.model.Song
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun observePlaylists(): Flow<List<Playlist>>
    fun observeItems(playlistId: Long): Flow<List<PlaylistItem>>
    fun searchPlaylists(query: String): Flow<List<Playlist>>
    suspend fun create(name: String): Result<Playlist>
    suspend fun delete(playlistId: Long): Result<Unit>
    suspend fun addSong(playlistId: Long, songId: Long): Result<Unit>
    suspend fun removeSong(playlistId: Long, songId: Long): Result<Unit>
    suspend fun rename(playlistId: Long, name: String): Result<Unit>
    suspend fun moveItem(playlistId: Long, fromPosition: Int, toPosition: Int): Result<Unit>
    suspend fun getPlaylistSongs(playlistId: Long): Result<List<Song>>
    suspend fun setCover(playlistId: Long, coverUri: String?): Result<Unit>
}

/** Shared playlist-name rules (create + rename stay consistent). */
fun validatePlaylistName(rawName: String): Result<String> {
    val name = rawName.trim()
    if (name.isEmpty()) return Result.Failure(AppError.InvalidMetadata("Playlist name is empty"))
    if (name.length > 120) return Result.Failure(AppError.InvalidMetadata("Playlist name is too long"))
    return Result.Success(name)
}

/** Name validation lives here (pure, tested); persistence in Phase 3. */
class CreatePlaylistUseCase(private val repository: PlaylistRepository) {
    suspend operator fun invoke(rawName: String): Result<Playlist> {
        return when (val validated = validatePlaylistName(rawName)) {
            is Result.Success -> repository.create(validated.value)
            is Result.Failure -> validated
            Result.Loading -> Result.Failure(AppError.Unknown("Invalid playlist name"))
        }
    }
}

class RenamePlaylistUseCase(private val repository: PlaylistRepository) {
    suspend operator fun invoke(playlistId: Long, rawName: String): Result<Unit> {
        return when (val validated = validatePlaylistName(rawName)) {
            is Result.Success -> repository.rename(playlistId, validated.value)
            is Result.Failure -> validated
            Result.Loading -> Result.Failure(AppError.Unknown("Invalid playlist name"))
        }
    }
}

class DeletePlaylistUseCase(
    private val repository: PlaylistRepository,
    private val covers: PlaylistCoverStore? = null
) {
    suspend operator fun invoke(playlistId: Long): Result<Unit> {
        val result = repository.delete(playlistId)
        if (result is Result.Success) covers?.delete(playlistId)
        return result
    }
}

/** Keeps user-picked playlist covers as small app-private image files. */
interface PlaylistCoverStore {
    /** Copies (and downsizes) the picked image; returns the stored file's URI, or null if it could not be read. */
    suspend fun save(playlistId: Long, sourceUri: String): String?
    suspend fun delete(playlistId: Long)
}

/** Sets a picked image as the playlist cover, or clears it ([sourceUri] null). */
class SetPlaylistCoverUseCase(
    private val repository: PlaylistRepository,
    private val covers: PlaylistCoverStore
) {
    suspend operator fun invoke(playlistId: Long, sourceUri: String?): Result<Unit> {
        if (sourceUri == null) {
            val result = repository.setCover(playlistId, null)
            if (result is Result.Success) covers.delete(playlistId)
            return result
        }
        val stored = covers.save(playlistId, sourceUri)
            ?: return Result.Failure(AppError.Unknown("The picture could not be read"))
        return repository.setCover(playlistId, stored)
    }
}

class AddSongToPlaylistUseCase(private val repository: PlaylistRepository) {
    suspend operator fun invoke(playlistId: Long, songId: Long): Result<Unit> =
        repository.addSong(playlistId, songId)
}

class RemoveSongFromPlaylistUseCase(private val repository: PlaylistRepository) {
    suspend operator fun invoke(playlistId: Long, songId: Long): Result<Unit> =
        repository.removeSong(playlistId, songId)
}

class MovePlaylistItemUseCase(private val repository: PlaylistRepository) {
    suspend operator fun invoke(playlistId: Long, fromPosition: Int, toPosition: Int): Result<Unit> =
        repository.moveItem(playlistId, fromPosition, toPosition)
}

class ObservePlaylistsUseCase(private val repository: PlaylistRepository) {
    operator fun invoke(): Flow<List<Playlist>> = repository.observePlaylists()
}

class ObservePlaylistSongsUseCase(private val repository: PlaylistRepository) {
    suspend operator fun invoke(playlistId: Long): Result<List<Song>> =
        repository.getPlaylistSongs(playlistId)
}
