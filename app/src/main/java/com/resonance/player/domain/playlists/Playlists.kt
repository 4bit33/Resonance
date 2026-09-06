package com.resonance.player.domain.playlists

import com.resonance.player.core.common.AppError
import com.resonance.player.core.common.Result
import com.resonance.player.core.model.Playlist
import com.resonance.player.core.model.PlaylistItem
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun observePlaylists(): Flow<List<Playlist>>
    fun observeItems(playlistId: Long): Flow<List<PlaylistItem>>
    suspend fun create(name: String): Result<Playlist>
    suspend fun delete(playlistId: Long): Result<Unit>
    suspend fun addSong(playlistId: Long, songId: Long): Result<Unit>
    suspend fun removeSong(playlistId: Long, songId: Long): Result<Unit>
}

/** Name validation lives here (pure, tested); persistence in Phase 3. */
class CreatePlaylistUseCase(private val repository: PlaylistRepository) {
    suspend operator fun invoke(rawName: String): Result<Playlist> {
        val name = rawName.trim()
        if (name.isEmpty()) return Result.Failure(AppError.InvalidMetadata("Playlist name is empty"))
        if (name.length > 120) return Result.Failure(AppError.InvalidMetadata("Playlist name is too long"))
        return repository.create(name)
    }
}
