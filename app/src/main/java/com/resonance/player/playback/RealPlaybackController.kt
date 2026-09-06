package com.resonance.player.playback

import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.resonance.player.core.common.AppDispatchers
import com.resonance.player.core.common.AppError
import com.resonance.player.core.common.Result
import com.resonance.player.core.model.PlaybackSnapshot
import com.resonance.player.core.model.RepeatMode
import com.resonance.player.core.model.ShuffleMode
import com.resonance.player.core.model.Song
import com.resonance.player.core.playback.PlaybackController
import com.resonance.player.domain.library.MusicRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withContext

/**
 * Production Media3-backed [PlaybackController].
 *
 * Architecture: this class NEVER owns ExoPlayer — [PlaybackService] does.
 * It binds a same-process [MediaController] to the session and translates
 * between domain models and Media3. Player callbacks + a 500 ms position
 * ticker (reading the REAL player position, only while playing/buffering)
 * rebuild the single authoritative [snapshot]; StateFlow equality dedupes
 * emissions so idle UI costs nothing.
 *
 * Threading: MediaController calls run on Dispatchers.Main (its looper);
 * repository/restore work runs on Dispatchers.IO. Transport methods are
 * main-safe suspend functions returning [Result].
 *
 * Shuffle: ExoPlayer owns shuffled timeline order; the bookkeeper keeps the
 * ORIGINAL queue order (never lost) and the current item is tracked by
 * stable media id. Domain indices are translated to timeline indices for
 * move/remove/insert/skip operations.
 */
class RealPlaybackController(
    private val appContext: Context,
    private val dispatchers: AppDispatchers,
    private val musicRepository: MusicRepository,
    private val stateStore: PlaybackStateStore,
    private val scope: CoroutineScope
) : PlaybackController {

    private val bookkeeper = QueueBookkeeper()
    private val mutable = MutableStateFlow(PlaybackSnapshot.Idle)
    override val snapshot: StateFlow<PlaybackSnapshot> = mutable.asStateFlow()
    private var lastError: AppError? = null
    private var errorMediaId: String? = null

    private val connectionMutex = Mutex()
    private var controller: MediaController? = null

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val previousId = bookkeeper.currentSong()?.id
            bookkeeper.setIndexByMediaId(mediaItem?.mediaId)
            if (mediaItem?.mediaId != errorMediaId) {
                lastError = null
                errorMediaId = null
            }
            // History bookkeeping: the track we are leaving counts as heard
            // through only on natural auto-transition (see transitionCompleted).
            // Skipped when the "transition" resolves to the same song.
            val leavingId = previousId
            if (leavingId != null && bookkeeper.currentSong()?.id != leavingId) {
                recordPlayAsync(leavingId, transitionCompleted(reason))
            }
            persistNow()
            refreshSnapshot()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            // Queue end (repeat OFF): the last track completed.
            if (playbackState == Player.STATE_ENDED) {
                bookkeeper.currentSong()?.let { recordPlayAsync(it.id, completed = true) }
            }
            refreshSnapshot()
        }
        override fun onIsPlayingChanged(isPlaying: Boolean) = refreshSnapshot()
        override fun onRepeatModeChanged(repeatMode: Int) = refreshSnapshot()
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) = refreshSnapshot()
        override fun onPositionDiscontinuity(reason: Int) = refreshSnapshot()
        override fun onTimelineChanged(timeline: Timeline, reason: Int) = refreshSnapshot()

        override fun onPlayerError(error: PlaybackException) {
            val current = bookkeeper.currentSong()
            errorMediaId = current?.let { mediaIdForSong(it.id) }
            lastError = PlayerErrorMapper.map(
                error.errorCode,
                errorMediaId,
                current?.mimeType
            )
            Log.w(TAG, "Playback error: code=" + error.errorCodeName, error)
            persistNow()
            refreshSnapshot()
        }
    }

    private val controllerListener = object : MediaController.Listener {
        override fun onDisconnected(controller: MediaController) {
            if (this@RealPlaybackController.controller === controller) {
                this@RealPlaybackController.controller = null
            }
            lastError = AppError.PlaybackFailed("Playback service disconnected")
            refreshSnapshot()
        }
    }

    init {
        scope.launch { restorePreviousSession() }
        scope.launch {
            var lastPersistMs = 0L
            while (true) {
                delay(POSITION_TICK_MS)
                val state = mutable.value
                if (state.isPlaying || state.isBuffering) {
                    refreshSnapshot()
                    val now = SystemClock.elapsedRealtime()
                    if (now - lastPersistMs >= PERSIST_DEBOUNCE_MS) {
                        lastPersistMs = now
                        persist(mutable.value)
                    }
                }
            }
        }
    }

    // Transport (PlaybackController interface).

    override suspend fun play(queue: List<Song>, startIndex: Int): Result<Unit> {
        if (queue.isEmpty()) return Result.Failure(AppError.EmptyLibrary)
        if (startIndex !in queue.indices) {
            return Result.Failure(AppError.Unknown("Invalid queue position"))
        }
        bookkeeper.setQueue(queue, startIndex)
        return withPlayer { c ->
            c.setMediaItems(queue.map { it.toMediaItem() }, startIndex, 0L)
            c.prepare()
            c.playWhenReady = true
            c.play()
            onCommandSucceeded()
        }.also { persistNow() }
    }

    override suspend fun resume(): Result<Unit> {
        if (bookkeeper.songs().isEmpty()) return Result.Failure(AppError.EmptyLibrary)
        return withPlayer { c ->
            reattachIfEmpty(c)
            if (c.playbackState == Player.STATE_ENDED) c.seekToDefaultPosition()
            c.playWhenReady = true
            c.play()
            onCommandSucceeded()
        }.also { persistNow() }
    }

    override suspend fun pause(): Result<Unit> = withPlayer { c ->
        c.pause()
        onCommandSucceeded()
    }.also { persistNow() }

    override suspend fun stop(): Result<Unit> = withPlayer { c ->
        c.stop()
        c.playWhenReady = false
        bookkeeper.setIndex(0)
        onCommandSucceeded()
    }.also { persistNow() }

    override suspend fun seekTo(positionMs: Long): Result<Unit> {
        val current = bookkeeper.currentSong()
            ?: return Result.Failure(AppError.EmptyLibrary)
        return withPlayer { c ->
            reattachIfEmpty(c)
            val duration = c.duration.takeIf { it > 0L } ?: current.durationMs
            c.seekTo(positionMs.coerceIn(0L, duration))
            refreshSnapshot()
        }
    }

    override suspend fun skipToNext(): Result<Unit> = withPlayer { c ->
        reattachIfEmpty(c)
        if (c.hasNextMediaItem()) c.seekToNextMediaItem()
        refreshSnapshot()
    }

    override suspend fun skipToPrevious(): Result<Unit> = withPlayer { c ->
        reattachIfEmpty(c)
        if (c.hasPreviousMediaItem()) c.seekToPreviousMediaItem()
        refreshSnapshot()
    }

    override suspend fun setShuffle(mode: ShuffleMode): Result<Unit> = withPlayer { c ->
        c.shuffleModeEnabled = mode == ShuffleMode.ON
        onCommandSucceeded()
    }.also { persistNow() }

    override suspend fun setRepeat(mode: RepeatMode): Result<Unit> = withPlayer { c ->
        c.repeatMode = repeatToPlayer(mode)
        onCommandSucceeded()
    }.also { persistNow() }

    override suspend fun moveQueueItem(fromIndex: Int, toIndex: Int): Result<Unit> {
        val songs = bookkeeper.songs()
        if (fromIndex !in songs.indices || toIndex !in songs.indices) {
            return Result.Failure(AppError.Unknown("Invalid queue position"))
        }
        if (fromIndex == toIndex) return Result.Success(Unit)
        val movedId = mediaIdForSong(songs[fromIndex].id)
        bookkeeper.move(fromIndex, toIndex)
        val ordered = bookkeeper.songs()
        return withPlayer { c ->
            moveOnTimeline(c, movedId, ordered, toIndex)
            refreshSnapshot()
        }.also { persistNow() }
    }

    override suspend fun appendToQueue(songs: List<Song>): Result<Unit> {
        if (songs.isEmpty()) return Result.Success(Unit)
        bookkeeper.append(songs)
        return withPlayer { c ->
            if (c.mediaItemCount == 0) {
                c.setMediaItems(songs.map { it.toMediaItem() })
            } else {
                c.addMediaItems(songs.map { it.toMediaItem() })
            }
            if (c.playbackState == Player.STATE_IDLE && c.mediaItemCount > 0) c.prepare()
            refreshSnapshot()
        }.also { persistNow() }
    }

    override suspend fun insertIntoQueue(index: Int, songs: List<Song>): Result<Unit> {
        if (songs.isEmpty()) return Result.Success(Unit)
        val sizeBefore = bookkeeper.songs().size
        val point = index.coerceIn(0, sizeBefore)
        val anchorId = bookkeeper.songs().getOrNull(point)?.let { mediaIdForSong(it.id) }
        bookkeeper.insert(point, songs)
        return withPlayer { c ->
            if (c.mediaItemCount == 0) {
                c.setMediaItems(songs.map { it.toMediaItem() })
            } else {
                val at = anchorId?.let { c.timelineIndexFor(it) } ?: c.mediaItemCount
                c.addMediaItems(at, songs.map { it.toMediaItem() })
            }
            if (c.playbackState == Player.STATE_IDLE && c.mediaItemCount > 0) c.prepare()
            refreshSnapshot()
        }.also { persistNow() }
    }

    override suspend fun removeQueueItem(index: Int): Result<Unit> {
        val songs = bookkeeper.songs()
        if (index !in songs.indices) {
            return Result.Failure(AppError.Unknown("Invalid queue position"))
        }
        val removedId = mediaIdForSong(songs[index].id)
        bookkeeper.remove(index)
        return withPlayer { c ->
            c.timelineIndexFor(removedId)?.let { c.removeMediaItem(it) }
            refreshSnapshot()
        }.also { persistNow() }
    }

    override suspend fun clearQueue(): Result<Unit> {
        bookkeeper.clear()
        lastError = null
        errorMediaId = null
        return withPlayer { c ->
            c.stop()
            c.playWhenReady = false
            c.clearMediaItems()
            refreshSnapshot()
        }.also {
            scope.launch {
                try {
                    stateStore.clear()
                } catch (e: Exception) {
                    Log.w(TAG, "Could not clear persisted playback state", e)
                }
            }
        }
    }

    override suspend fun skipToQueueItem(index: Int): Result<Unit> {
        val songs = bookkeeper.songs()
        if (index !in songs.indices) {
            return Result.Failure(AppError.Unknown("Invalid queue position"))
        }
        return withPlayer { c ->
            if (c.mediaItemCount == 0) {
                c.setMediaItems(songs.map { it.toMediaItem() })
            }
            val target = c.timelineIndexFor(mediaIdForSong(songs[index].id)) ?: index
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            c.seekTo(target.coerceIn(0, (c.mediaItemCount - 1).coerceAtLeast(0)), 0L)
            c.playWhenReady = true
            c.play()
            onCommandSucceeded()
        }.also { persistNow() }
    }

    // Internals.

    private fun onCommandSucceeded() {
        lastError = null
        refreshSnapshot()
    }

    /**
     * Maps a domain-order move onto the player timeline (which may be
     * shuffled). Anchors the moved item next to its post-move bookkeeper
     * neighbors so the two orders stay intentionally aligned. With shuffle
     * off, domain and timeline indices coincide and the move is exact.
     */
    private fun moveOnTimeline(
        c: MediaController,
        movedId: String,
        ordered: List<Song>,
        toDomainIndex: Int
    ) {
        if (c.mediaItemCount == 0) return
        val from = c.timelineIndexFor(movedId) ?: return
        val to = timelineInsertIndex(c, ordered, toDomainIndex, movedId, from) ?: return
        if (from != to) c.moveMediaItem(from, to)
    }

    private fun timelineInsertIndex(
        c: MediaController,
        ordered: List<Song>,
        toDomainIndex: Int,
        movedId: String,
        fromTimelineIndex: Int
    ): Int? {
        val after = ordered.subList(toDomainIndex + 1, ordered.size)
            .firstNotNullOfOrNull { c.timelineIndexFor(mediaIdForSong(it.id)) }
        if (after != null) {
            return if (fromTimelineIndex < after) after - 1 else after
        }
        val before = ordered.subList(0, toDomainIndex)
            .mapNotNull { c.timelineIndexFor(mediaIdForSong(it.id)) }
            .maxOrNull()
        if (before != null) {
            return if (fromTimelineIndex <= before) before else before + 1
        }
        return null
    }

    /**
     * Re-pushes the bookkeeper queue when the player playlist is empty
     * (e.g. the service process restarted while the app survived). Keeps
     * transport commands truthful instead of silently no-opping.
     */
    private fun reattachIfEmpty(c: MediaController): Boolean {
        if (c.mediaItemCount > 0) return true
        val songs = bookkeeper.songs()
        if (songs.isEmpty()) return false
        c.setMediaItems(
            songs.map { it.toMediaItem() },
            bookkeeper.index().coerceIn(songs.indices),
            0L
        )
        c.prepare()
        return true
    }

    private fun Player.timelineIndexFor(mediaId: String): Int? {
        val timeline = currentTimeline
        if (timeline.isEmpty) return null
        val window = Timeline.Window()
        for (i in 0 until timeline.windowCount) {
            timeline.getWindow(i, window)
            if (window.mediaItem?.mediaId == mediaId) return i
        }
        return null
    }

    private fun refreshSnapshot() {
        val c = controller
        val songs = bookkeeper.songs()
        if (c == null || !c.isConnected) {
            val previous = mutable.value
            mutable.value = if (songs.isEmpty()) {
                PlaybackSnapshot.Idle.copy(error = lastError)
            } else {
                previous.copy(isPlaying = false, isBuffering = false, error = lastError)
            }
            return
        }
        mutable.value = SnapshotMapper.build(
            PlayerSnapshotInput(
                isPlaying = c.isPlaying,
                isBuffering = c.playbackState == Player.STATE_BUFFERING,
                positionMs = c.currentPosition,
                durationMs = c.duration,
                bufferedPositionMs = c.bufferedPosition,
                shuffle = if (c.shuffleModeEnabled) ShuffleMode.ON else ShuffleMode.OFF,
                repeat = repeatFromPlayer(c.repeatMode)
            ),
            songs,
            bookkeeper.index(),
            lastError
        )
    }

    private suspend fun <T> withPlayer(block: (MediaController) -> T): Result<T> {
        val c = try {
            ensureController()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return Result.Failure(AppError.PlaybackFailed(e.message))
        } ?: return Result.Failure(AppError.PlaybackFailed("Playback service unavailable"))
        return try {
            Result.Success(block(c))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Failure(AppError.PlaybackFailed(e.message))
        }
    }

    private suspend fun ensureController(): MediaController? =
        withContext(dispatchers.main) {
            connectionMutex.withLock {
                val existing = controller
                if (existing != null && existing.isConnected) return@withContext existing
                try {
                    existing?.release()
                } catch (e: Exception) {
                    Log.w(TAG, "Could not release stale controller", e)
                }
                controller = null
                try {
                    val token = SessionToken(
                        appContext,
                        ComponentName(appContext, PlaybackService::class.java)
                    )
                    val connection = CompletableDeferred<MediaController>()
                    val future = MediaController.Builder(appContext, token)
                        .setListener(controllerListener)
                        .buildAsync()
                    future.addListener(
                        {
                            try {
                                connection.complete(future.get())
                            } catch (e: Exception) {
                                connection.completeExceptionally(e)
                            }
                        },
                        ContextCompat.getMainExecutor(appContext)
                    )
                    val built = try {
                        connection.await()
                    } catch (e: CancellationException) {
                        future.cancel(true)
                        throw e
                    }
                    built.addListener(playerListener)
                    controller = built
                    refreshSnapshot()
                    built
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    lastError = AppError.PlaybackFailed(e.message)
                    refreshSnapshot()
                    null
                }
            }
        }

    private suspend fun restorePreviousSession() {
        val restored = try {
            withContext(dispatchers.io) { stateStore.load() }
        } catch (e: Exception) {
            Log.w(TAG, "Could not load persisted playback state", e)
            null
        } ?: return
        val songs = withContext(dispatchers.io) {
            restored.songIds.mapNotNull { id ->
                when (val result = musicRepository.getSong(id)) {
                    is Result.Success -> result.value
                    else -> null
                }
            }
        }
        if (songs.isEmpty()) return
        val c = try {
            ensureController()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Could not connect for restore", e)
            null
        } ?: return
        bookkeeper.setQueue(songs, restored.index.coerceIn(songs.indices))
        withContext(dispatchers.main) {
            c.setMediaItems(
                songs.map { it.toMediaItem() },
                bookkeeper.index(),
                restored.positionMs
            )
            c.repeatMode = repeatToPlayer(restored.repeat)
            c.shuffleModeEnabled = restored.shuffle == ShuffleMode.ON
            c.prepare()
            c.playWhenReady = false
            lastError = null
            refreshSnapshot()
        }
    }

    private fun persistNow() {
        scope.launch { persist(mutable.value) }
    }

    /**
     * Best-effort history write on the app scope (repository confines to
     * IO). Failures only log — playback must never break over bookkeeping.
     */
    private fun recordPlayAsync(songId: Long, completed: Boolean) {
        scope.launch {
            try {
                musicRepository.recordPlay(songId, completed)
            } catch (t: Exception) {
                if (t is CancellationException) throw t
                Log.w(TAG, "recordPlay failed for song $songId", t)
            }
        }
    }

    private suspend fun persist(snapshot: PlaybackSnapshot) {
        try {
            if (snapshot.queue.isEmpty()) {
                stateStore.clear()
            } else {
                stateStore.save(
                    RestoredPlayback(
                        songIds = snapshot.queue.map { it.song.id },
                        index = snapshot.queueIndex,
                        positionMs = snapshot.positionMs,
                        shuffle = snapshot.shuffle,
                        repeat = snapshot.repeat
                    )
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not persist playback state", e)
        }
    }

    companion object {
        private const val TAG = "RealPlaybackController"

        /** Position refresh cadence while playing/buffering. */
        const val POSITION_TICK_MS = 500L

        /** Best-effort persisted-state freshness while playing. */
        const val PERSIST_DEBOUNCE_MS = 5000L
    }
}

private fun repeatToPlayer(mode: RepeatMode): Int = when (mode) {
    RepeatMode.OFF -> Player.REPEAT_MODE_OFF
    RepeatMode.ALL -> Player.REPEAT_MODE_ALL
    RepeatMode.ONE -> Player.REPEAT_MODE_ONE
}

private fun repeatFromPlayer(mode: Int): RepeatMode = when (mode) {
    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
    else -> RepeatMode.OFF
}
