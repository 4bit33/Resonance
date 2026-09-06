package com.resonance.player.domain

import com.resonance.player.core.common.AppError
import com.resonance.player.core.common.Result
import com.resonance.player.core.model.RepeatMode
import com.resonance.player.core.model.ShuffleMode
import com.resonance.player.core.playback.UnimplementedPlaybackController
import com.resonance.player.domain.playback.SeekToUseCase
import com.resonance.player.domain.playback.SetRepeatModeUseCase
import com.resonance.player.domain.playback.SetShuffleModeUseCase
import com.resonance.player.domain.playback.SkipToNextUseCase
import com.resonance.player.domain.playback.SkipToPreviousUseCase
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
