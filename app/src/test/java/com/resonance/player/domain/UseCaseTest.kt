package com.resonance.player.domain

import com.resonance.player.core.common.AppError
import com.resonance.player.core.common.Result
import com.resonance.player.core.model.MixPreset
import com.resonance.player.core.playback.UnimplementedPlaybackController
import com.resonance.player.domain.library.ObserveSongsUseCase
import com.resonance.player.domain.library.RecordPlayUseCase
import com.resonance.player.domain.library.GetAlbumSongsUseCase
import com.resonance.player.domain.playback.PlaySongsUseCase
import com.resonance.player.domain.playback.TogglePlayPauseUseCase
import com.resonance.player.domain.playlists.AddSongToPlaylistUseCase
import com.resonance.player.domain.playlists.CreatePlaylistUseCase
import com.resonance.player.domain.playlists.DeletePlaylistUseCase
import com.resonance.player.domain.playlists.MovePlaylistItemUseCase
import com.resonance.player.domain.playlists.ObservePlaylistSongsUseCase
import com.resonance.player.domain.playlists.RemoveSongFromPlaylistUseCase
import com.resonance.player.domain.playlists.RenamePlaylistUseCase
import com.resonance.player.fakes.FakeMusicRepository
import com.resonance.player.fakes.FakePlaybackController
import com.resonance.player.fakes.FakePlaylistRepository
import com.resonance.player.fakes.testSong
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UseCaseTest {

    @Test
    fun observeSongs_emitsRepositoryContent() = runTest {
        val useCase = ObserveSongsUseCase(FakeMusicRepository())
        val songs = mutableListOf<com.resonance.player.core.model.Song>()
        useCase().collect { songs.addAll(it) }
        assertEquals(2, songs.size)
    }

    @Test
    fun recordPlay_delegates() = runTest {
        val repo = FakeMusicRepository()
        RecordPlayUseCase(repo)(1L, completed = true)
        assertEquals(listOf(1L), repo.recordedPlays)
    }

    @Test
    fun playSongs_rejectsEmptyQueue() = runTest {
        val result = PlaySongsUseCase(FakePlaybackController())(emptyList())
        assertTrue(result is Result.Failure && result.error is AppError.EmptyLibrary)
    }

    @Test
    fun playSongs_rejectsBadIndex() = runTest {
        val result = PlaySongsUseCase(FakePlaybackController())(listOf(testSong(1L)), startIndex = 5)
        assertTrue(result is Result.Failure)
    }

    @Test
    fun toggle_resumesWhenPaused() = runTest {
        val controller = FakePlaybackController()
        TogglePlayPauseUseCase(controller)()
        assertEquals(listOf("resume"), controller.calls)
    }

    @Test
    fun createPlaylist_trimsAndValidates() = runTest {
        val useCase = CreatePlaylistUseCase(FakePlaylistRepository())
        assertTrue(useCase("   ") is Result.Failure)
        val ok = useCase("  Road Trip  ")
        assertTrue(ok is Result.Success && (ok as Result.Success).value.name == "Road Trip")
    }

    @Test
    fun playlistCrud_roundTrip() = runTest {
        val repo = FakePlaylistRepository()
        val created = CreatePlaylistUseCase(repo)("Mix") as Result.Success
        val id = created.value.id
        assertTrue(AddSongToPlaylistUseCase(repo)(id, 1L) is Result.Success)
        assertTrue(AddSongToPlaylistUseCase(repo)(id, 1L) is Result.Success)
        assertTrue(AddSongToPlaylistUseCase(repo)(id, 2L) is Result.Success)
        val songs = ObservePlaylistSongsUseCase(repo)(id)
        assertTrue(songs is Result.Success && (songs as Result.Success).value.map { it.id } == listOf(1L, 2L))
        assertTrue(MovePlaylistItemUseCase(repo)(id, 0, 1) is Result.Success)
        val moved = ObservePlaylistSongsUseCase(repo)(id) as Result.Success
        assertEquals(listOf(2L, 1L), moved.value.map { it.id })
        assertTrue(RenamePlaylistUseCase(repo)(id, "  New  ") is Result.Success)
        assertTrue(RenamePlaylistUseCase(repo)(id, "  ") is Result.Failure)
        assertTrue(RenamePlaylistUseCase(repo)(-99L, "X") is Result.Failure)
        assertTrue(RemoveSongFromPlaylistUseCase(repo)(id, 2L) is Result.Success)
        val afterRemove = ObservePlaylistSongsUseCase(repo)(id) as Result.Success
        assertEquals(listOf(1L), afterRemove.value.map { it.id })
        assertTrue(RemoveSongFromPlaylistUseCase(repo)(id, 1L) is Result.Success)
        // Existing-but-empty playlist is a valid empty list, not an error.
        assertEquals(emptyList<Long>(), (ObservePlaylistSongsUseCase(repo)(id) as Result.Success).value.map { it.id })
        assertTrue(DeletePlaylistUseCase(repo)(id) is Result.Success)
        assertTrue(ObservePlaylistSongsUseCase(repo)(id) is Result.Failure)
    }

    @Test
    fun unimplementedController_reportsUnavailable() = runTest {
        val controller = UnimplementedPlaybackController()
        val result = controller.play(listOf(testSong(1L)))
        assertTrue(result is Result.Failure && result.error is AppError.FeatureUnavailable)
        assertEquals(false, controller.snapshot.value.isPlaying)
    }

    @Test
    fun mixPreset_validatesBounds() {
        try {
            MixPreset(id = "", name = "x")
            assertTrue("blank id must fail", false)
        } catch (e: IllegalArgumentException) {
            assertTrue(true)
        }
        try {
            MixPreset(id = "a", name = "b", targetSize = 0)
            assertTrue("targetSize 0 must fail", false)
        } catch (e: IllegalArgumentException) {
            assertTrue(true)
        }
        assertEquals(25, MixPreset(id = "a", name = "b").targetSize)
    }
}
