package com.resonance.player.playback

import androidx.media3.common.PlaybackException
import com.resonance.player.core.common.AppError
import com.resonance.player.core.common.Result
import com.resonance.player.core.model.RepeatMode
import com.resonance.player.core.model.ShuffleMode
import com.resonance.player.fakes.testSong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QueueBookkeeperTest {

    private fun songs() = listOf(testSong(1L), testSong(2L), testSong(3L))

    @Test
    fun setQueue_validates() {
        val q = QueueBookkeeper()
        assertTrue(q.setQueue(emptyList(), 0) is Result.Failure)
        assertTrue(q.setQueue(songs(), 9) is Result.Failure)
        assertTrue(q.setQueue(songs(), 1) is Result.Success)
        assertEquals(1, q.index())
        assertEquals(2L, q.currentSong()?.id)
    }

    @Test
    fun append_startsAtZeroWhenEmpty() {
        val q = QueueBookkeeper()
        q.append(listOf(testSong(5L)))
        assertEquals(0, q.index())
        q.append(listOf(testSong(6L)))
        assertEquals(0, q.index())
        assertEquals(2, q.songs().size)
    }

    @Test
    fun insert_clampsAndFollowsCurrent() {
        val q = QueueBookkeeper()
        q.setQueue(songs(), 1)
        q.insert(99, listOf(testSong(9L)))
        assertEquals(listOf(1L, 2L, 3L, 9L), q.songs().map { it.id })
        assertEquals(2L, q.currentSong()?.id)
        q.insert(0, listOf(testSong(8L)))
        assertEquals(listOf(8L, 1L, 2L, 3L, 9L), q.songs().map { it.id })
        assertEquals(2L, q.currentSong()?.id)
        assertEquals(2, q.index())
    }

    @Test
    fun remove_keepsIndexCoherent() {
        val q = QueueBookkeeper()
        q.setQueue(songs(), 1)
        q.remove(0)
        assertEquals(listOf(2L, 3L), q.songs().map { it.id })
        assertEquals(2L, q.currentSong()?.id)
        q.remove(0)
        assertEquals(listOf(3L), q.songs().map { it.id })
        assertEquals(3L, q.currentSong()?.id)
        q.remove(0)
        assertEquals(-1, q.index())
        assertNull(q.currentSong())
        assertTrue(q.remove(0) is Result.Failure)
    }

    @Test
    fun move_followsCurrentByIdentity() {
        val q = QueueBookkeeper()
        q.setQueue(songs(), 0)
        q.move(0, 2)
        assertEquals(listOf(2L, 3L, 1L), q.songs().map { it.id })
        assertEquals(1L, q.currentSong()?.id)
        assertEquals(2, q.index())
        assertTrue(q.move(0, 9) is Result.Failure)
        assertTrue(q.move(1, 1) is Result.Success)
    }

    @Test
    fun skipTo_validates() {
        val q = QueueBookkeeper()
        q.setQueue(songs(), 0)
        assertTrue(q.skipTo(2) is Result.Success)
        assertEquals(2, q.index())
        assertTrue(q.skipTo(5) is Result.Failure)
    }

    @Test
    fun setIndexByMediaId_tracksTransitions() {
        val q = QueueBookkeeper()
        q.setQueue(songs(), 0)
        q.setIndexByMediaId(mediaIdForSong(3L))
        assertEquals(2, q.index())
        q.setIndexByMediaId("foreign:42")
        assertEquals(2, q.index())
        q.setIndexByMediaId(null)
        assertEquals(2, q.index())
        q.clear()
        assertEquals(-1, q.index())
    }
}

class MediaRequestMapperTest {

    @Test
    fun request_carriesStableIdAndMetadata() {
        val request = testSong(7L, "Title").toMediaRequest()
        assertEquals("song:7", request.mediaId)
        assertEquals("content://media/external/audio/media/7", request.uri)
        assertEquals("Title", request.title)
        assertEquals("Artist", request.artist)
        assertEquals("Album", request.album)
    }

    @Test
    fun mediaId_roundTrips() {
        assertEquals(42L, mediaIdFor("song:42"))
        assertNull(mediaIdFor("other:42"))
        assertNull(mediaIdFor(null))
        assertNull(mediaIdFor("song:abc"))
    }
}

class PlayerErrorMapperTest {

    @Test
    fun mapsIoFailures() {
        assertTrue(
            PlayerErrorMapper.map(
                PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND, "song:3", "audio/mpeg"
            ) is AppError.MissingFile
        )
        assertTrue(
            PlayerErrorMapper.map(
                PlaybackException.ERROR_CODE_IO_NO_PERMISSION, "song:3", null
            ) is AppError.PermissionDenied
        )
        assertTrue(
            PlayerErrorMapper.map(
                PlaybackException.ERROR_CODE_IO_UNSPECIFIED, "song:3", null
            ) is AppError.PlaybackFailed
        )
    }

    @Test
    fun mapsDecodeFailuresToUnsupportedFormat() {
        val parsing = PlayerErrorMapper.map(
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED, "song:3", "audio/x-weird"
        )
        assertTrue(parsing is AppError.UnsupportedFormat)
        assertEquals("audio/x-weird", (parsing as AppError.UnsupportedFormat).mimeType)
        assertTrue(
            PlayerErrorMapper.map(
                PlaybackException.ERROR_CODE_DECODER_INIT_FAILED, "song:3", null
            ) is AppError.UnsupportedFormat
        )
    }

    @Test
    fun mapsOutputDrmTimeoutAndUnknown() {
        assertTrue(
            PlayerErrorMapper.map(
                PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED, null, null
            ) is AppError.PlaybackFailed
        )
        assertTrue(
            PlayerErrorMapper.map(
                PlaybackException.ERROR_CODE_DRM_UNSPECIFIED, null, null
            ) is AppError.PlaybackFailed
        )
        assertTrue(
            PlayerErrorMapper.map(
                PlaybackException.ERROR_CODE_TIMEOUT, null, null
            ) is AppError.PlaybackFailed
        )
        assertTrue(
            PlayerErrorMapper.map(
                PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW, null, null
            ) is AppError.Unknown
        )
    }
}

class SnapshotMapperTest {

    @Test
    fun emptyQueue_yieldsIdleWithError() {
        val snapshot = SnapshotMapper.build(
            PlayerSnapshotInput(false, false, 0L, 0L, 0L, ShuffleMode.OFF, RepeatMode.OFF),
            emptyList(),
            -1,
            AppError.PlaybackFailed("x")
        )
        assertNull(snapshot.song)
        assertTrue(snapshot.queue.isEmpty())
        assertTrue(snapshot.error is AppError.PlaybackFailed)
    }

    @Test
    fun buildsQueueAndClampsValues() {
        val songs = listOf(testSong(1L), testSong(2L))
        val snapshot = SnapshotMapper.build(
            PlayerSnapshotInput(true, false, 999_999L, 100_000L, 500_000L, ShuffleMode.ON, RepeatMode.ALL),
            songs,
            5,
            null
        )
        assertEquals(2L, snapshot.song?.id)
        assertEquals(1, snapshot.queueIndex)
        assertEquals(listOf(0L, 1L), snapshot.queue.map { it.queueId })
        assertEquals(100_000L, snapshot.positionMs)
        assertEquals(100_000L, snapshot.durationMs)
        assertEquals(100_000L, snapshot.bufferedPositionMs)
        assertEquals(ShuffleMode.ON, snapshot.shuffle)
        assertEquals(RepeatMode.ALL, snapshot.repeat)
        assertNull(snapshot.error)
    }

    @Test
    fun duration_fallsBackToLibraryValue() {
        val snapshot = SnapshotMapper.build(
            PlayerSnapshotInput(false, false, 1_000L, -1L, 0L, ShuffleMode.OFF, RepeatMode.OFF),
            listOf(testSong(1L)),
            0,
            null
        )
        assertEquals(180_000L, snapshot.durationMs)
        assertEquals(1_000L, snapshot.positionMs)
    }
}

class RestoredPlaybackCodecTest {

    @Test
    fun roundTrips() {
        val original = RestoredPlayback(
            listOf(1L, 2L, 3L), 2, 45_000L, ShuffleMode.ON, RepeatMode.ONE
        )
        val fields = RestoredPlaybackCodec.encode(original)
        val decoded = RestoredPlaybackCodec.decode(
            fields["queue"], 2, 45_000L, fields["shuffle"], fields["repeat"]
        )
        assertEquals(original, decoded)
    }

    @Test
    fun rejectsEmptyAndCoerces() {
        assertNull(RestoredPlaybackCodec.decode(null, 0, 0L, null, null))
        assertNull(RestoredPlaybackCodec.decode("", 0, 0L, null, null))
        assertNull(RestoredPlaybackCodec.decode("a,b", 0, 0L, null, null))
        val decoded = RestoredPlaybackCodec.decode("x,7,y", 9, -5L, "NOPE", "NOPE")
        assertEquals(listOf(7L), decoded?.songIds)
        assertEquals(0, decoded?.index)
        assertEquals(0L, decoded?.positionMs)
        assertEquals(ShuffleMode.OFF, decoded?.shuffle)
        assertEquals(RepeatMode.OFF, decoded?.repeat)
    }
}
