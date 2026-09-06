package com.resonance.player.core

import com.resonance.player.core.database.entity.SongEntity
import com.resonance.player.core.database.toDomain
import com.resonance.player.core.database.toEntity
import com.resonance.player.core.permissions.MusicPermissions
import com.resonance.player.core.ui.adaptive.WindowWidthSize
import com.resonance.player.core.ui.adaptive.windowWidthSizeFor
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SongMapperTest {

    private fun entity() = SongEntity(
        id = 7L, mediaStoreId = 7L, title = "T", artistName = "A", albumName = "Al",
        albumId = null, artistId = null, genreName = null, trackNumber = 1, discNumber = null,
        year = 2020, durationMs = 180_000L, path = "/m/t.mp3", contentUri = "content://m/7",
        mimeType = "audio/mpeg", bitrate = 320_000, sampleRate = 44_100,
        dateAddedEpochSec = 1L, dateModifiedEpochSec = 2L
    )

    @Test
    fun roundTrip_preservesFields() {
        val domain = entity().toDomain(isFavorite = true)
        assertTrue(domain.isFavorite)
        assertEquals(entity(), domain.toEntity())
    }

    @Test
    fun default_notFavorite() {
        assertFalse(entity().toDomain().isFavorite)
    }
}

class MusicPermissionsTest {

    @Test
    fun selectsPermissionBySdk() {
        assertEquals(
            MusicPermissions.READ_MEDIA_AUDIO,
            MusicPermissions.audioPermissionForSdk(33)
        )
        assertEquals(
            MusicPermissions.READ_EXTERNAL_STORAGE,
            MusicPermissions.audioPermissionForSdk(32)
        )
        assertEquals(
            MusicPermissions.READ_EXTERNAL_STORAGE,
            MusicPermissions.audioPermissionForSdk(26)
        )
    }

    @Test
    fun branches_matchMinSdkContract() {
        assertTrue(MusicPermissions.needsNotificationPermission(33))
        assertFalse(MusicPermissions.needsNotificationPermission(32))
        assertTrue(MusicPermissions.supportsMediaPlaybackServiceType(29))
        assertFalse(MusicPermissions.supportsMediaPlaybackServiceType(26))
    }
}

class AdaptiveTest {

    @Test
    fun buckets_followBreakpoints() {
        assertEquals(WindowWidthSize.COMPACT, windowWidthSizeFor(360.dp))
        assertEquals(WindowWidthSize.MEDIUM, windowWidthSizeFor(600.dp))
        assertEquals(WindowWidthSize.EXPANDED, windowWidthSizeFor(840.dp))
    }
}
