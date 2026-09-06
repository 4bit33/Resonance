package com.resonance.player.core

import com.resonance.player.core.model.PlaybackSnapshot
import com.resonance.player.core.ui.components.shouldShowMiniPlayer
import com.resonance.player.core.ui.components.songRowState
import com.resonance.player.core.ui.components.SongRowState
import com.resonance.player.fakes.testSong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignSystemRulesTest {

    @Test
    fun miniPlayer_showsOnlyWithLoadedTrack() {
        assertFalse(shouldShowMiniPlayer(PlaybackSnapshot.Idle))
        assertTrue(
            shouldShowMiniPlayer(PlaybackSnapshot.Idle.copy(song = testSong(1L)))
        )
    }

    @Test
    fun songRowState_priorityOrder() {
        assertEquals(
            SongRowState.Missing,
            songRowState(isCurrent = true, isSelected = true, isMissing = true, isLoading = true)
        )
        assertEquals(
            SongRowState.Loading,
            songRowState(isCurrent = false, isSelected = false, isMissing = false, isLoading = true)
        )
        assertEquals(
            SongRowState.Selected,
            songRowState(isCurrent = true, isSelected = true, isMissing = false, isLoading = false)
        )
        assertEquals(
            SongRowState.Playing,
            songRowState(isCurrent = true, isSelected = false, isMissing = false, isLoading = false)
        )
        assertEquals(
            SongRowState.Disabled,
            songRowState(
                isCurrent = false, isSelected = false, isMissing = false,
                isLoading = false, isEnabled = false
            )
        )
        assertEquals(
            SongRowState.Normal,
            songRowState(isCurrent = false, isSelected = false, isMissing = false, isLoading = false)
        )
    }
}
