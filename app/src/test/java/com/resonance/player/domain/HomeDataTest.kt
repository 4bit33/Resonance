package com.resonance.player.domain

import com.resonance.player.core.media.UnknownMetadata
import com.resonance.player.domain.favorites.ToggleFavoriteUseCase
import com.resonance.player.domain.library.ObserveMostPlayedUseCase
import com.resonance.player.domain.library.ObserveRecentlyAddedUseCase
import com.resonance.player.domain.library.ObserveRecentlyPlayedUseCase
import com.resonance.player.domain.library.ObserveStorageOverviewUseCase
import com.resonance.player.domain.library.recentAlbumsFromSongs
import com.resonance.player.fakes.FakeFavoritesRepository
import com.resonance.player.fakes.FakeMusicRepository
import com.resonance.player.fakes.testSong
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeDataTest {

    @Test
    fun recentAlbums_groupsAndCaps() {
        val songs = listOf(
            testSong(1L).copy(albumName = "A", albumArtist = "X", artworkUri = "u1"),
            testSong(2L).copy(albumName = "A", albumArtist = "X", artworkUri = "u2"),
            testSong(3L).copy(albumName = "B", albumArtist = null, artistName = "Solo")
        )
        val albums = recentAlbumsFromSongs(songs, limit = 10)
        assertEquals(2, albums.size)
        assertEquals("A", albums[0].name)
        assertEquals("X", albums[0].albumArtist)
        assertEquals(2, albums[0].songCount)
        assertEquals("u1", albums[0].artUri)
        assertEquals("Solo", albums[1].artistName)
    }

    @Test
    fun recentAlbums_compilationLabel() {
        val songs = listOf(
            testSong(1L).copy(albumName = "Mix", albumArtist = null, artistName = "A"),
            testSong(2L).copy(albumName = "Mix", albumArtist = null, artistName = "B")
        )
        val albums = recentAlbumsFromSongs(songs)
        assertEquals(UnknownMetadata.VARIOUS_ARTISTS, albums[0].artistName)
    }

    @Test
    fun limitedQueries_delegate() = runTest {
        val repo = FakeMusicRepository()
        assertEquals(2, ObserveRecentlyPlayedUseCase(repo)(10).first().size)
        assertEquals(2, ObserveMostPlayedUseCase(repo)(10).first().size)
        assertEquals(2, ObserveRecentlyAddedUseCase(repo)(10).first().size)
        val overview = ObserveStorageOverviewUseCase(repo)().first()
        assertEquals(2, overview.trackCount)
        assertTrue(overview.libraryBytes > 0L)
    }

    @Test
    fun toggleFavorite_flips() = runTest {
        val repo = FakeFavoritesRepository()
        val toggle = ToggleFavoriteUseCase(repo)
        assertEquals(true, (toggle(1L) as com.resonance.player.core.common.Result.Success).value)
        assertEquals(false, (toggle(1L) as com.resonance.player.core.common.Result.Success).value)
        assertEquals(listOf<Long>(), repo.observeFavoriteIds().first())
    }
}
