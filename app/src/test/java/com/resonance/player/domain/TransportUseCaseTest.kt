package com.resonance.player.domain

import com.resonance.player.core.common.AppError
import com.resonance.player.core.common.Result
import com.resonance.player.core.model.RepeatMode
import com.resonance.player.core.model.ShuffleMode
import com.resonance.player.core.model.Song
import com.resonance.player.core.playback.PlaybackController
import com.resonance.player.core.playback.UnimplementedPlaybackController
import com.resonance.player.domain.playback.SeekToUseCase
import com.resonance.player.domain.playback.SetRepeatModeUseCase
import com.resonance.player.domain.playback.SetShuffleModeUseCase
import com.resonance.player.domain.playback.SkipToNextUseCase
import com.resonance.player.domain.playback.SkipToPreviousUseCase
import com.resonance.player.domain.playback.MoveQueueItemUseCase
import com.resonance.player.domain.playback.RemoveQueueItemUseCase
import com.resonance.player.domain.playback.ClearQueueUseCase
import com.resonance.player.domain.playback.SkipToQueueItemUseCase
import com.resonance.player.domain.playback.AppendToQueueUseCase
import com.resonance.player.domain.playback.InsertIntoQueueUseCase
import com.resonance.player.domain.playback.PlayNextUseCase
import com.resonance.player.fakes.testSong
import com.resonance.player.fakes.FakePlaybackController
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransportUseCaseTest {

    @Test
    fun seek_rejectsNegative() = runTest {
        val result = SeekToUseCase(FakePlaybackController())(-100L)
        assertTrue(result is Result.Failure && result.error is AppError.Unknown)
    }

    @Test
    fun seek_delegates() = runTest {
        assertTrue(SeekToUseCase(FakePlaybackController())(5_000L) is Result.Success)
    }

    @Test
    fun skipAndModes_delegate() = runTest {
        val controller = FakePlaybackController()
        assertTrue(SkipToNextUseCase(controller)() is Result.Success)
        assertTrue(SkipToPreviousUseCase(controller)() is Result.Success)
        assertTrue(SetShuffleModeUseCase(controller)(ShuffleMode.ON) is Result.Success)
        assertTrue(SetRepeatModeUseCase(controller)(RepeatMode.ONE) is Result.Success)
    }

    @Test
    fun queueOps_delegate() = runTest {
        val controller = FakePlaybackController()
        assertTrue(controller.appendToQueue(emptyList()) is Result.Success)
        assertTrue(controller.insertIntoQueue(0, emptyList()) is Result.Success)
        assertTrue(controller.removeQueueItem(0) is Result.Success)
        assertTrue(controller.clearQueue() is Result.Success)
        assertTrue(controller.skipToQueueItem(0) is Result.Success)
        assertEquals(
            listOf("append", "insert", "remove", "clear", "skipTo"),
            controller.calls
        )
    }

    @Test
    fun queueUseCases_delegateToController() = runTest {
        val controller = FakePlaybackController()
        val songs = listOf(testSong(1L))
        assertTrue(MoveQueueItemUseCase(controller)(0, 1) is Result.Success)
        assertTrue(RemoveQueueItemUseCase(controller)(0) is Result.Success)
        assertTrue(ClearQueueUseCase(controller)() is Result.Success)
        assertTrue(SkipToQueueItemUseCase(controller)(0) is Result.Success)
        assertTrue(AppendToQueueUseCase(controller)(songs) is Result.Success)
        assertTrue(InsertIntoQueueUseCase(controller)(0, songs) is Result.Success)
        assertEquals(
            listOf("move", "remove", "clear", "skipTo", "append", "insert"),
            controller.calls
        )
    }

    @Test
    fun playNext_emptyQueueStartsPlayback() = runTest {
        val controller = FakePlaybackController()
        val song = testSong(9L)
        assertTrue(PlayNextUseCase(controller)(song) is Result.Success)
        assertEquals(listOf("play"), controller.calls)
    }

    @Test
    fun playNext_insertsAfterCurrent() = runTest {
        val controller = FakePlaybackController()
        controller.seedQueue(listOf(testSong(1L), testSong(2L)), index = 0)
        controller.calls.clear()
        // Fake controller records inserts but ignores indices; use a spy instead.
        var insertedAt = -1
        val spy = object : PlaybackController by controller {
            override suspend fun insertIntoQueue(index: Int, songs: List<Song>): Result<Unit> {
                insertedAt = index
                return Result.Success(Unit)
            }
        }
        assertTrue(PlayNextUseCase(spy)(testSong(3L)) is Result.Success)
        assertEquals(1, insertedAt)
    }

    @Test
    fun unimplemented_reportsUnavailableForQueueOps() = runTest {
        val controller = UnimplementedPlaybackController()
        val results = listOf(
            controller.appendToQueue(emptyList()),
            controller.insertIntoQueue(0, emptyList()),
            controller.removeQueueItem(0),
            controller.clearQueue(),
            controller.skipToQueueItem(0)
        )
        assertTrue(results.all { it is Result.Failure && it.error is AppError.FeatureUnavailable })
    }
}
