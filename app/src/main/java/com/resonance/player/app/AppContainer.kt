package com.resonance.player.app

import android.content.Context
import androidx.room.Room
import com.resonance.player.core.common.DefaultAppDispatchers
import com.resonance.player.core.database.MIGRATION_1_2
import com.resonance.player.core.database.MIGRATION_2_3
import com.resonance.player.core.database.ResonanceDatabase
import com.resonance.player.core.playback.PlaybackController
import com.resonance.player.data.local.DataStoreSettingsRepository
import com.resonance.player.data.local.LibraryPreferences
import com.resonance.player.data.local.RoomFavoritesRepository
import com.resonance.player.data.local.RoomMusicRepository
import com.resonance.player.data.local.RoomPlaylistRepository
import com.resonance.player.data.local.RoomSourceRepository
import com.resonance.player.data.local.StorageStatsProvider
import com.resonance.player.data.media.AndroidMetadataExtractor
import com.resonance.player.data.media.ArtworkStore
import com.resonance.player.data.media.RetrieverArtworkExtractor
import com.resonance.player.data.media.SafAudioDataSource
import com.resonance.player.data.media.SafLibraryScanner
import com.resonance.player.domain.library.GetAlbumSongsUseCase
import com.resonance.player.domain.library.GetFolderSongsUseCase
import com.resonance.player.domain.library.GetArtistSongsUseCase
import com.resonance.player.domain.library.GetGenreSongsUseCase
import com.resonance.player.domain.library.AddSourcesUseCase
import com.resonance.player.domain.library.GetLibraryStatsUseCase
import com.resonance.player.domain.library.GetSongUseCase
import com.resonance.player.domain.library.ObserveAlbumsUseCase
import com.resonance.player.domain.library.ObserveArtistsUseCase
import com.resonance.player.domain.library.ObserveFoldersUseCase
import com.resonance.player.domain.library.ObserveGenresUseCase
import com.resonance.player.domain.favorites.ObserveFavoriteIdsUseCase
import com.resonance.player.domain.favorites.ObserveFavoriteSongsUseCase
import com.resonance.player.domain.favorites.ToggleFavoriteUseCase
import com.resonance.player.domain.library.ObserveLastScanUseCase
import com.resonance.player.domain.library.ObserveMostPlayedUseCase
import com.resonance.player.domain.library.ObserveRecentlyAddedUseCase
import com.resonance.player.domain.library.ObserveRecentlyPlayedUseCase
import com.resonance.player.domain.library.ObserveStorageOverviewUseCase
import com.resonance.player.domain.library.ObserveScanStateUseCase
import com.resonance.player.domain.library.ObserveSongsUseCase
import com.resonance.player.domain.library.ObserveSourcesUseCase
import com.resonance.player.domain.library.RecordPlayUseCase
import com.resonance.player.domain.library.RemoveSourcesUseCase
import com.resonance.player.domain.library.RescanLibraryUseCase
import com.resonance.player.domain.playback.AppendToQueueUseCase
import com.resonance.player.domain.playback.ClearQueueUseCase
import com.resonance.player.domain.playback.InsertIntoQueueUseCase
import com.resonance.player.domain.playback.MoveQueueItemUseCase
import com.resonance.player.domain.playback.PlayNextUseCase
import com.resonance.player.domain.playback.PlaySongsUseCase
import com.resonance.player.domain.playlists.AddSongToPlaylistUseCase
import com.resonance.player.domain.playlists.CreatePlaylistUseCase
import com.resonance.player.domain.playlists.DeletePlaylistUseCase
import com.resonance.player.domain.playlists.MovePlaylistItemUseCase
import com.resonance.player.domain.playlists.ObservePlaylistSongsUseCase
import com.resonance.player.domain.playlists.ObservePlaylistsUseCase
import com.resonance.player.domain.playlists.RemoveSongFromPlaylistUseCase
import com.resonance.player.domain.playlists.RenamePlaylistUseCase
import com.resonance.player.domain.playback.RemoveQueueItemUseCase
import com.resonance.player.domain.playback.SkipToQueueItemUseCase
import com.resonance.player.domain.playback.SeekToUseCase
import com.resonance.player.domain.playback.SetRepeatModeUseCase
import com.resonance.player.domain.playback.SetShuffleModeUseCase
import com.resonance.player.domain.playback.SkipToNextUseCase
import com.resonance.player.domain.playback.SkipToPreviousUseCase
import com.resonance.player.domain.playback.TogglePlayPauseUseCase
import com.resonance.player.domain.search.SearchAllUseCase
import com.resonance.player.domain.search.SearchLibraryUseCase
import com.resonance.player.domain.settings.UserPreferencesRepository
import com.resonance.player.playback.PlaybackStateStore
import com.resonance.player.playback.RealPlaybackController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
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
 * Storage Access Framework / Room. Composables never touch ContentResolver or
 * cursors.
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
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            .build()
    }

    val libraryPreferences: LibraryPreferences by lazy {
        LibraryPreferences(appContext)
    }

    private val safDataSource: SafAudioDataSource by lazy {
        SafAudioDataSource(appContext.contentResolver)
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

    val libraryScanner: SafLibraryScanner by lazy {
        SafLibraryScanner(
            dispatchers,
            applicationScope,
            database,
            safDataSource,
            metadataExtractor,
            artworkExtractor,
            artworkStore,
            libraryPreferences
        )
    }

    val sourceRepository: RoomSourceRepository by lazy {
        RoomSourceRepository(
            appContext.contentResolver,
            database,
            libraryScanner,
            safDataSource,
            dispatchers
        )
    }

    val favoritesRepository: RoomFavoritesRepository by lazy {
        RoomFavoritesRepository(database, dispatchers)
    }

    val playlistRepository: RoomPlaylistRepository by lazy {
        RoomPlaylistRepository(database, dispatchers)
    }

    val musicRepository: RoomMusicRepository by lazy {
        RoomMusicRepository(
            database,
            libraryScanner,
            libraryPreferences,
            StorageStatsProvider(),
            dispatchers
        )
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
    val searchAll = SearchAllUseCase(musicRepository, playlistRepository)
    val getArtistSongs = GetArtistSongsUseCase(musicRepository)
    val getGenreSongs = GetGenreSongsUseCase(musicRepository)
    val observeAlbums = ObserveAlbumsUseCase(musicRepository)
    val observeArtists = ObserveArtistsUseCase(musicRepository)
    val observeGenres = ObserveGenresUseCase(musicRepository)
    val observeFolders = ObserveFoldersUseCase(musicRepository)
    val getAlbumSongs = GetAlbumSongsUseCase(musicRepository)
    val getFolderSongs = GetFolderSongsUseCase(musicRepository)
    val observeScanState = ObserveScanStateUseCase(musicRepository)
    val rescanLibrary = RescanLibraryUseCase(musicRepository)
    val observeSources = ObserveSourcesUseCase(sourceRepository)
    val addSources = AddSourcesUseCase(sourceRepository, musicRepository)
    val removeSources = RemoveSourcesUseCase(sourceRepository, musicRepository)
    val getLibraryStats = GetLibraryStatsUseCase(musicRepository)
    val observeLastScan = ObserveLastScanUseCase(musicRepository)
    val observeRecentlyPlayed = ObserveRecentlyPlayedUseCase(musicRepository)
    val observeMostPlayed = ObserveMostPlayedUseCase(musicRepository)
    val observeRecentlyAdded = ObserveRecentlyAddedUseCase(musicRepository)
    val observeStorageOverview = ObserveStorageOverviewUseCase(musicRepository)
    val toggleFavorite = ToggleFavoriteUseCase(favoritesRepository)
    val observeFavoriteSongs = ObserveFavoriteSongsUseCase(favoritesRepository)
    val observeFavoriteIds = ObserveFavoriteIdsUseCase(favoritesRepository)
    val observePlaylists = ObservePlaylistsUseCase(playlistRepository)
    val observePlaylistSongs = ObservePlaylistSongsUseCase(playlistRepository)
    val createPlaylist = CreatePlaylistUseCase(playlistRepository)
    val renamePlaylist = RenamePlaylistUseCase(playlistRepository)
    val deletePlaylist = DeletePlaylistUseCase(playlistRepository)
    val addSongToPlaylist = AddSongToPlaylistUseCase(playlistRepository)
    val removeSongFromPlaylist = RemoveSongFromPlaylistUseCase(playlistRepository)
    val movePlaylistItem = MovePlaylistItemUseCase(playlistRepository)
    val playSongs = PlaySongsUseCase(playbackController)
    val playNext = PlayNextUseCase(playbackController)
    val togglePlayPause = TogglePlayPauseUseCase(playbackController)
    val seekTo = SeekToUseCase(playbackController)
    val skipToNext = SkipToNextUseCase(playbackController)
    val skipToPrevious = SkipToPreviousUseCase(playbackController)
    val setShuffleMode = SetShuffleModeUseCase(playbackController)
    val setRepeatMode = SetRepeatModeUseCase(playbackController)
    val moveQueueItem = MoveQueueItemUseCase(playbackController)
    val removeQueueItem = RemoveQueueItemUseCase(playbackController)
    val clearQueue = ClearQueueUseCase(playbackController)
    val skipToQueueItem = SkipToQueueItemUseCase(playbackController)
    val appendToQueue = AppendToQueueUseCase(playbackController)
    val insertIntoQueue = InsertIntoQueueUseCase(playbackController)
}
