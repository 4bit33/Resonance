package com.resonance.player.app

import android.content.Context
import androidx.room.Room
import com.resonance.player.core.common.DefaultAppDispatchers
import com.resonance.player.core.database.ResonanceDatabase
import com.resonance.player.core.playback.PlaybackController
import com.resonance.player.core.playback.UnimplementedPlaybackController
import com.resonance.player.data.local.DataStoreSettingsRepository
import com.resonance.player.data.local.RoomMusicRepository
import com.resonance.player.domain.library.GetSongUseCase
import com.resonance.player.domain.library.ObserveSongsUseCase
import com.resonance.player.domain.library.RecordPlayUseCase
import com.resonance.player.domain.playback.PlaySongsUseCase
import com.resonance.player.domain.playback.TogglePlayPauseUseCase
import com.resonance.player.domain.search.SearchLibraryUseCase
import com.resonance.player.domain.settings.UserPreferencesRepository

/**
 * Manual service locator (ADR-006). Single :app module + hand-written graph
 * keeps the project understandable for a solo developer; Hilt/Koin would add
 * build cost and coupling the foundation does not need.
 *
 * Dependency direction: UI -> ViewModel -> UseCase -> Repository -> (Room).
 * Nothing outside core.playback knows how playback is implemented.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    val dispatchers = DefaultAppDispatchers()

    val database: ResonanceDatabase by lazy {
        Room.databaseBuilder(appContext, ResonanceDatabase::class.java, "resonance.db")
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            .build()
    }

    val musicRepository: RoomMusicRepository by lazy {
        RoomMusicRepository(database, dispatchers)
    }

    val settingsRepository: UserPreferencesRepository by lazy {
        DataStoreSettingsRepository(appContext)
    }

    /**
     * Single playback entry point. Phase 1: honest stand-in (idle state +
     * FeatureUnavailable on transport). Phase 2 swaps in the Media3-backed
     * implementation behind this same type — no caller changes.
     */
    val playbackController: PlaybackController by lazy {
        UnimplementedPlaybackController()
    }

    // Use cases (thin, Android-free, unit-tested).
    val observeSongs = ObserveSongsUseCase(musicRepository)
    val getSong = GetSongUseCase(musicRepository)
    val recordPlay = RecordPlayUseCase(musicRepository)
    val searchLibrary = SearchLibraryUseCase(musicRepository)
    val playSongs = PlaySongsUseCase(playbackController)
    val togglePlayPause = TogglePlayPauseUseCase(playbackController)
}

