package com.resonance.player.app

import android.content.Context
import androidx.room.Room
import com.resonance.player.core.common.DefaultAppDispatchers
import com.resonance.player.core.database.MIGRATION_1_2
import com.resonance.player.core.database.ResonanceDatabase
import com.resonance.player.core.playback.PlaybackController
import com.resonance.player.data.local.AppAudioPermissionManager
import com.resonance.player.data.local.DataStoreSettingsRepository
import com.resonance.player.data.local.LibraryPreferences
import com.resonance.player.data.local.RoomMusicRepository
import com.resonance.player.data.media.AndroidMetadataExtractor
import com.resonance.player.data.media.ArtworkStore
import com.resonance.player.data.media.MediaStoreAudioDataSource
import com.resonance.player.data.media.MediaStoreLibraryScanner
import com.resonance.player.data.media.RetrieverArtworkExtractor
import com.resonance.player.domain.library.GetAlbumSongsUseCase
import com.resonance.player.domain.library.GetLibraryStatsUseCase
import com.resonance.player.domain.library.GetSongUseCase
import com.resonance.player.domain.library.ObserveAlbumsUseCase
import com.resonance.player.domain.library.ObserveArtistsUseCase
import com.resonance.player.domain.library.ObserveFoldersUseCase
import com.resonance.player.domain.library.ObserveGenresUseCase
import com.resonance.player.domain.library.ObserveLastScanUseCase
import com.resonance.player.domain.library.ObserveScanStateUseCase
import com.resonance.player.domain.library.ObserveSongsUseCase
import com.resonance.player.domain.library.RecordPlayUseCase
import com.resonance.player.domain.library.RescanLibraryUseCase
import com.resonance.player.domain.playback.PlaySongsUseCase
import com.resonance.player.domain.playback.SeekToUseCase
import com.resonance.player.domain.playback.SetRepeatModeUseCase
import com.resonance.player.domain.playback.SetShuffleModeUseCase
import com.resonance.player.domain.playback.SkipToNextUseCase
import com.resonance.player.domain.playback.SkipToPreviousUseCase
import com.resonance.player.domain.playback.TogglePlayPauseUseCase
import com.resonance.player.domain.search.SearchLibraryUseCase
import com.resonance.player.domain.settings.UserPreferencesRepository
import com.resonance.player.playback.PlaybackStateStore
import com.resonance.player.playback.RealPlaybackController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

/**
 * Manual service locator (ADR-006). Single :app module + hand-written graph
 * keeps the project understandable for a solo developer; Hilt/Koin would add
 * build cost and coupling the foundation does not need.
 *
 * Dependency direction: UI -> ViewModel -> UseCase -> Repository -> (Room).
 * Playback: UI -> ViewModel -> UseCase -> PlaybackController -> Media3
 * service. Nothing outside the playback package knows about ExoPlayer.
 * Scanning: UI -> ViewModel -> UseCase -> Repository -> LibraryScanner ->
 * MediaStore / Room. Composables never touch ContentResolver or cursors.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    val dispatchers = DefaultAppDispatchers()

    /**
     * Application-lifetime scope for the playback controller (position
     * ticker, restore, persistence) and the library scanner. It lives as
     * long as the process — neither playback nor scanning depends on any
     * Activity lifecycle.
     */
    private val applicationScope = CoroutineScope(SupervisorJob() + dispatchers.main)

    val database: ResonanceDatabase by lazy {
        Room.databaseBuilder(appContext, ResonanceDatabase::class.java, "resonance.db")
            .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            .build()
    }

    val libraryPreferences: LibraryPreferences by lazy {
        LibraryPreferences(appContext)
    }

    val permissionManager: AppAudioPermissionManager by lazy {
        AppAudioPermissionManager(appContext, libraryPreferences)
    }

    private val mediaStoreDataSource: MediaStoreAudioDataSource by lazy {
        MediaStoreAudioDataSource(appContext)
    }

    private val metadataExtractor: AndroidMetadataExtractor by lazy {
        AndroidMetadataExtractor(appContext)
    }

    private val artworkStore: ArtworkStore by lazy {
        ArtworkStore(File(appContext.cacheDir, "artwork"))
    }

    private val artworkExtractor: RetrieverArtworkExtractor by lazy {
        RetrieverArtworkExtractor(appContext)
    }

    val libraryScanner: MediaStoreLibraryScanner by lazy {
        MediaStoreLibraryScanner(
            appContext,
            dispatchers,
            applicationScope,
            database,
            mediaStoreDataSource,
            metadataExtractor,
            artworkExtractor,
            artworkStore,
            libraryPreferences
        )
    }

    val musicRepository: RoomMusicRepository by lazy {
        RoomMusicRepository(database, libraryScanner, libraryPreferences, dispatchers)
    }

    val settingsRepository: UserPreferencesRepository by lazy {
        DataStoreSettingsRepository(appContext)
    }

    val playbackStateStore: PlaybackStateStore by lazy {
        PlaybackStateStore(appContext)
    }

    /**
     * Single playback entry point. Real Media3 implementation owned by the
     * foreground PlaybackService; created lazily so a cold start never binds
     * the service before the user (or a restore) needs playback.
     */
    val playbackController: PlaybackController by lazy {
        RealPlaybackController(
            appContext,
            dispatchers,
            musicRepository,
            playbackStateStore,
            applicationScope
        )
    }

    // Use cases (thin, Android-free, unit-tested).
    val observeSongs = ObserveSongsUseCase(musicRepository)
    val getSong = GetSongUseCase(musicRepository)
    val recordPlay = RecordPlayUseCase(musicRepository)
    val searchLibrary = SearchLibraryUseCase(musicRepository)
    val observeAlbums = ObserveAlbumsUseCase(musicRepository)
    val observeArtists = ObserveArtistsUseCase(musicRepository)
    val observeGenres = ObserveGenresUseCase(musicRepository)
    val observeFolders = ObserveFoldersUseCase(musicRepository)
    val getAlbumSongs = GetAlbumSongsUseCase(musicRepository)
    val observeScanState = ObserveScanStateUseCase(musicRepository)
    val rescanLibrary = RescanLibraryUseCase(musicRepository)
    val getLibraryStats = GetLibraryStatsUseCase(musicRepository)
    val observeLastScan = ObserveLastScanUseCase(musicRepository)
    val playSongs = PlaySongsUseCase(playbackController)
    val togglePlayPause = TogglePlayPauseUseCase(playbackController)
    val seekTo = SeekToUseCase(playbackController)
    val skipToNext = SkipToNextUseCase(playbackController)
    val skipToPrevious = SkipToPreviousUseCase(playbackController)
    val setShuffleMode = SetShuffleModeUseCase(playbackController)
    val setRepeatMode = SetRepeatModeUseCase(playbackController)

    /**
     * Cold-start sync: the cached Room library renders immediately; the
     * incremental scan reconciles in the background (no-op without
     * permission — the scanner reports PermissionRequired, never a loop).
     */
    fun onAppStarted() {
        applicationScope.launch { musicRepository.scanAndImport() }
    }

    /**
     * Foreground re-check (permission may have changed while away). The
     * scanner is single-flight: concurrent calls observe the running scan.
     */
    fun onForegrounded() {
        applicationScope.launch { musicRepository.scanAndImport() }
    }
}
