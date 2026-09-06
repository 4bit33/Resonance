package com.resonance.player.domain

import com.resonance.player.core.common.Result
import com.resonance.player.core.media.ScanReport
import com.resonance.player.core.media.ScanState
import com.resonance.player.domain.library.GetAlbumSongsUseCase
import com.resonance.player.domain.library.GetLibraryStatsUseCase
import com.resonance.player.domain.library.ObserveAlbumsUseCase
import com.resonance.player.domain.library.ObserveArtistsUseCase
import com.resonance.player.domain.library.ObserveFoldersUseCase
import com.resonance.player.domain.library.ObserveGenresUseCase
import com.resonance.player.domain.library.ObserveLastScanUseCase
import com.resonance.player.domain.library.ObserveScanStateUseCase
import com.resonance.player.domain.library.RescanLibraryUseCase
import com.resonance.player.fakes.FakeMusicRepository
import com.resonance.player.fakes.FakePlaylistRepository
import com.resonance.player.fakes.testSong
import com.resonance.player.feature.settings.formatScanTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import com.resonance.player.domain.library.recentAlbumsFromSongs
import com.resonance.player.domain.search.SearchAllUseCase
import com.resonance.player.domain.search.SearchResults
import com.resonance.player.feature.library.alphabetIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryBrowseTest {

    @Test
    fun browseFlows_emit() = runTest {
        val repo = FakeMusicRepository()
        assertTrue(ObserveAlbumsUseCase(repo)().first().isEmpty())
        assertTrue(ObserveArtistsUseCase(repo)().first().isEmpty())
        assertTrue(ObserveGenresUseCase(repo)().first().isEmpty())
        assertTrue(ObserveFoldersUseCase(repo)().first().isEmpty())
        assertEquals(ScanState.Idle, ObserveScanStateUseCase(repo)().first())
    }

    @Test
    fun scanState_emitsChanges() = runTest {
        val repo = FakeMusicRepository()
        val report = ScanReport(1, 0, 0, 0, 1)
        repo.emitScanState(ScanState.Completed(report))
        assertEquals(
            ScanState.Completed(report),
            ObserveScanStateUseCase(repo)().first()
        )
    }

    @Test
    fun albumSongs_returnLibrarySongs() = runTest {
        val result = GetAlbumSongsUseCase(FakeMusicRepository())("Album", null)
        assertTrue(result is Result.Success && result.value.size == 2)
    }

    @Test
    fun rescan_delegatesToRepository() = runTest {
        val repo = FakeMusicRepository()
        RescanLibraryUseCase(repo)()
        assertEquals(1, repo.scans)
    }

    @Test
    fun stats_reflectFakeLibrary() = runTest {
        val stats = GetLibraryStatsUseCase(FakeMusicRepository())()
        assertEquals(2, stats.songCount)
    }

    @Test
    fun lastScan_emitsNullByDefault() = runTest {
        assertEquals(null, ObserveLastScanUseCase(FakeMusicRepository())().first())
    }

    @Test
    fun formatScanTime_handlesUnknown() {
        assertEquals("n/a", formatScanTime(null, "n/a"))
        assertEquals("n/a", formatScanTime(0L, "n/a"))
        assertEquals("n/a", formatScanTime(-5L, "n/a"))
        assertTrue(formatScanTime(1_700_000_000L, "n/a").isNotBlank())
    }

    @Test
    fun alphabetIndex_groupsFirstLetters() {
        val index = alphabetIndex(listOf("apple", "Apricot", "banana", "123 GO", "", "  cherry"))
        assertEquals(
            listOf("A" to 0, "B" to 2, "#" to 3, "C" to 5),
            index
        )
    }

    @Test
    fun alphabetIndex_emptyInput() {
        assertEquals(emptyList<Pair<String, Int>>(), alphabetIndex(emptyList()))
    }

    @Test
    fun searchAll_blankShortCircuits() = runTest {
        val results = SearchAllUseCase(FakeMusicRepository(), FakePlaylistRepository())("   ").first()
        assertTrue(results.isEmpty())
    }

    @Test
    fun searchAll_combinesCategories() = runTest {
        val results = SearchAllUseCase(FakeMusicRepository(), FakePlaylistRepository())("Song").first()
        assertEquals(2, results.songs.size)
        assertTrue(results.albums.isEmpty())
        assertTrue(results.playlists.isEmpty())
        assertTrue(!SearchResults(songs = listOf(testSong(1L))).isEmpty())
    }
}
