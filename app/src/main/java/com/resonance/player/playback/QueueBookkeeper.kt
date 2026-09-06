package com.resonance.player.playback

import com.resonance.player.core.common.AppError
import com.resonance.player.core.common.Result
import com.resonance.player.core.model.Song

/**
 * Pure in-memory playback-queue bookkeeping (no ExoPlayer, no Context).
 *
 * The bookkeeper owns queue ORDER (always the original, unshuffled order —
 * ExoPlayer applies shuffle to its own timeline) and the current index.
 * The current item is tracked by stable media id so track transitions stay
 * correct even while ExoPlayer shuffles its timeline.
 *
 * All mutations return [Result]; invalid indices are reported, never crash.
 */
class QueueBookkeeper {

    private var songs: List<Song> = emptyList()
    private var index: Int = -1

    fun songs(): List<Song> = songs

    fun index(): Int = index

    fun currentSong(): Song? = songs.getOrNull(index)

    fun setQueue(newSongs: List<Song>, startIndex: Int): Result<Unit> {
        if (newSongs.isEmpty()) return Result.Failure(AppError.EmptyLibrary)
        if (startIndex !in newSongs.indices) {
            return Result.Failure(AppError.Unknown("Invalid queue position"))
        }
        songs = newSongs.toList()
        index = startIndex
        return Result.Success(Unit)
    }

    fun append(newSongs: List<Song>): Result<Unit> {
        if (newSongs.isEmpty()) return Result.Success(Unit)
        songs = songs + newSongs
        if (index == -1) index = 0
        return Result.Success(Unit)
    }

    fun insert(atIndex: Int, newSongs: List<Song>): Result<Unit> {
        if (newSongs.isEmpty()) return Result.Success(Unit)
        val point = atIndex.coerceIn(0, songs.size)
        val currentId = currentSong()?.id
        songs = songs.subList(0, point) + newSongs + songs.subList(point, songs.size)
        index = if (currentId == null) 0 else songs.indexOfFirst { it.id == currentId }
        return Result.Success(Unit)
    }

    fun remove(atIndex: Int): Result<Unit> {
        if (atIndex !in songs.indices) {
            return Result.Failure(AppError.Unknown("Invalid queue position"))
        }
        val removedId = songs[atIndex].id
        val currentId = currentSong()?.id
        songs = songs.filterIndexed { i, _ -> i != atIndex }
        index = when {
            songs.isEmpty() -> -1
            currentId == null -> 0
            currentId == removedId -> index.coerceIn(0, songs.size - 1)
            else -> songs.indexOfFirst { it.id == currentId }.takeIf { it >= 0 } ?: 0
        }
        return Result.Success(Unit)
    }

    fun clear() {
        songs = emptyList()
        index = -1
    }

    fun move(fromIndex: Int, toIndex: Int): Result<Unit> {
        if (fromIndex !in songs.indices || toIndex !in songs.indices) {
            return Result.Failure(AppError.Unknown("Invalid queue position"))
        }
        if (fromIndex == toIndex) return Result.Success(Unit)
        val currentId = currentSong()?.id
        val mutable = songs.toMutableList()
        val moved = mutable.removeAt(fromIndex)
        mutable.add(toIndex, moved)
        songs = mutable
        index = if (currentId == null) -1 else songs.indexOfFirst { it.id == currentId }
        return Result.Success(Unit)
    }

    fun skipTo(atIndex: Int): Result<Unit> {
        if (atIndex !in songs.indices) {
            return Result.Failure(AppError.Unknown("Invalid queue position"))
        }
        index = atIndex
        return Result.Success(Unit)
    }

    /** Called on player track transitions; keeps index truthful by media id. */
    fun setIndexByMediaId(mediaId: String?) {
        val id = mediaIdFor(mediaId) ?: return
        val found = songs.indexOfFirst { it.id == id }
        if (found >= 0) index = found
    }

    fun setIndex(atIndex: Int) {
        if (songs.isEmpty()) {
            index = -1
        } else {
            index = atIndex.coerceIn(0, songs.size - 1)
        }
    }
}
