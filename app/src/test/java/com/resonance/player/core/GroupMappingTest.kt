package com.resonance.player.core

import com.resonance.player.core.database.dao.AlbumRow
import com.resonance.player.core.database.dao.ArtistRow
import com.resonance.player.core.database.dao.FolderRow
import com.resonance.player.core.database.dao.GenreRow
import com.resonance.player.core.database.toDomain
import org.junit.Assert.assertEquals
import org.junit.Test

class GroupMappingTest {

    @Test
    fun album_prefersAlbumArtist() {
        val album = AlbumRow(
            albumName = "Album", albumArtist = "Band", songCount = 10,
            totalDurationMs = 1_000L, year = 2020, distinctArtists = 1,
            sampleArtist = "Band", sampleArtworkUri = "file:///a.bin"
        ).toDomain()
        assertEquals("Band", album.artistName)
        assertEquals("Band", album.albumArtist)
        assertEquals("file:///a.bin", album.artUri)
    }

    @Test
    fun compilation_withoutAlbumArtist_showsVariousArtists() {
        val album = AlbumRow(
            albumName = "Hits", albumArtist = null, songCount = 12,
            totalDurationMs = 1_000L, year = null, distinctArtists = 5,
            sampleArtist = "Someone", sampleArtworkUri = null
        ).toDomain()
        assertEquals("Various Artists", album.artistName)
        assertEquals(null, album.albumArtist)
    }

    @Test
    fun singleArtist_withoutAlbumArtist_showsThatArtist() {
        val album = AlbumRow(
            albumName = "Solo", albumArtist = "", songCount = 8,
            totalDurationMs = 1_000L, year = 2021, distinctArtists = 1,
            sampleArtist = "Singer", sampleArtworkUri = null
        ).toDomain()
        assertEquals("Singer", album.artistName)
    }

    @Test
    fun artist_genre_folder_mappings() {
        val artist = ArtistRow("Name", 4, 2).toDomain()
        assertEquals("Name", artist.name)
        assertEquals(4, artist.songCount)
        assertEquals(2, artist.albumCount)

        val genre = GenreRow("Rock", 9).toDomain()
        assertEquals("Rock", genre.name)
        assertEquals(9, genre.songCount)

        val folder = FolderRow("Music/Rock/", 3).toDomain()
        assertEquals("Rock", folder.name)
        assertEquals("Music/Rock/", folder.path)

        assertEquals("Download", FolderRow("Download/", 29).toDomain().name)

        val unknown = FolderRow(null, 1).toDomain()
        assertEquals("Device storage", unknown.name)
    }
}
