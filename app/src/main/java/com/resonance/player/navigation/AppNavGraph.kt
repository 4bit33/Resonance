package com.resonance.player.navigation

import com.resonance.player.feature.tags.TagEditorViewModel
import com.resonance.player.feature.tags.TagEditorScreen
import com.resonance.player.core.ui.components.PlayerAnchors
import com.resonance.player.feature.player.NowPlayingSheet
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Rect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.animation.core.Animatable
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.scaleIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import com.resonance.player.R
import com.resonance.player.core.ui.theme.ResonanceTheme
import com.resonance.player.core.ui.theme.rememberArtworkPalette
import com.resonance.player.app.AppContainer
import com.resonance.player.core.ui.adaptive.WindowWidthSize
import com.resonance.player.core.model.SourceKind
import com.resonance.player.core.ui.adaptive.rememberWindowWidthSize
import com.resonance.player.core.ui.components.ArtworkImage
import com.resonance.player.core.ui.components.GenreDialog
import com.resonance.player.core.model.Song
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.resonance.player.core.ui.components.LocalMusicActions
import com.resonance.player.core.ui.components.MusicActions
import com.resonance.player.core.ui.components.NavDockDestination
import com.resonance.player.core.ui.components.ResonanceMiniPlayer
import com.resonance.player.core.ui.components.ResonanceNavDock
import com.resonance.player.core.ui.components.ResonanceSnackbar
import com.resonance.player.core.ui.components.ResonanceSnackbarVisuals
import com.resonance.player.core.ui.components.shouldShowMiniPlayer
import com.resonance.player.feature.favorites.FavoritesScreen
import com.resonance.player.feature.favorites.FavoritesViewModel
import com.resonance.player.domain.library.CollectionRef
import com.resonance.player.feature.home.HomeDependencies
import com.resonance.player.feature.importer.ImportScreen
import com.resonance.player.feature.library.CollectionScreen
import com.resonance.player.feature.library.CollectionViewModel
import com.resonance.player.feature.home.HomeEditorScreen
import com.resonance.player.feature.home.HomeEditorViewModel
import com.resonance.player.feature.home.HomeScreen
import com.resonance.player.feature.home.HomeViewModel
import com.resonance.player.feature.library.LibraryScreen
import com.resonance.player.feature.library.LibraryViewModel
import com.resonance.player.feature.player.PlayerScreen
import com.resonance.player.feature.player.PlayerViewModel
import com.resonance.player.feature.playlists.PlaylistDetailScreen
import com.resonance.player.feature.playlists.PlaylistDetailViewModel
import com.resonance.player.feature.playlists.PlaylistsScreen
import com.resonance.player.feature.playlists.PlaylistsViewModel
import com.resonance.player.feature.queue.QueueScreen
import com.resonance.player.feature.queue.QueueViewModel
import com.resonance.player.feature.search.SearchScreen
import com.resonance.player.feature.search.SearchViewModel
import com.resonance.player.feature.settings.SettingsScreen
import com.resonance.player.feature.settings.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
private fun dockDestinations(): List<NavDockDestination> = listOf(
    NavDockDestination(
        AppDestination.Home.route,
        stringResource(R.string.nav_home),
        Icons.Rounded.Home
    ),
    NavDockDestination(
        AppDestination.Library.route,
        stringResource(R.string.nav_library),
        Icons.Rounded.LibraryMusic
    ),
    NavDockDestination(
        AppDestination.Playlists.route,
        stringResource(R.string.nav_playlists),
        Icons.AutoMirrored.Rounded.QueueMusic
    )
)

/**
 * Stitch app shell: 68dp custom dock (Home/Library/Playlists/Settings),
 * global Search/Player/Queue routes, persistent mini player above the dock
 * while a track is loaded. Expanded widths keep a rail with the same four
 * destinations; the mini player docks at the content bottom.
 */
@Composable
fun ResonanceAppShell(container: AppContainer) {
    val navController = rememberNavController()
    val widthSize = rememberWindowWidthSize()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val selectedTab = AppDestination.tabForRoute(currentRoute)?.route
    val scope = rememberCoroutineScope()
    val snapshot by container.playbackController.snapshot.collectAsStateWithLifecycle()
    // Now Playing is a layer over the whole app (dock and mini player included), not a
    // page inside the content area: it slides up whole instead of being clipped above the bars.
    // 0 = mini player, 1 = full Now Playing; fingers drive it directly (NowPlayingSheet).
    val sheet = remember { Animatable(0f) }
    val sheetVisible by remember { derivedStateOf { sheet.value > 0f } }
    val sheetExpanded by remember { derivedStateOf { sheet.value >= 0.999f } }
    var rootHeight by remember { mutableFloatStateOf(2000f) }
    var rootWidth by remember { mutableFloatStateOf(1000f) }
    var miniBounds by remember { mutableStateOf<Rect?>(null) }
    val anchors = remember { PlayerAnchors() }
    val sheetMotion = ResonanceTheme.motion
    fun settleSheet(target: Float, velocity: Float = 0f) {
        scope.launch {
            sheet.animateTo(target, sheetMotion.settle(), initialVelocity = -velocity / rootHeight)
        }
    }
    fun dragSheet(delta: Float) {
        scope.launch { sheet.snapTo((sheet.value - delta / rootHeight).coerceIn(0f, 1f)) }
    }
    LaunchedEffect(snapshot.song == null) { if (snapshot.song == null) sheet.snapTo(0f) }
    val showMiniPlayer = shouldShowMiniPlayer(snapshot) && currentRoute != AppDestination.Queue.route
    val dockDestinations = dockDestinations()
    val snackbarHostState = remember { SnackbarHostState() }

    /** Shared feedback channel for actions that otherwise silently no-op (e.g. playing an empty playlist). */
    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(ResonanceSnackbarVisuals(message)) }
    }

    // The system pickers are created once here; screens trigger them through LocalMusicActions.
    // The grant is taken and the scan started right after a pick (nothing else scans by itself).
    val addFolderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) scope.launch { container.addSources(SourceKind.TREE, listOf(uri.toString())) }
    }
    val addSongsPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) scope.launch { container.addSources(SourceKind.FILE, uris.map { it.toString() }) }
    }
    var genreTarget by remember { mutableStateOf<GenreTarget?>(null) }
    var tagsRequest by remember { mutableStateOf<Long?>(null) }
    val musicActions = remember {
        MusicActions(
            addFolder = { initialUri -> addFolderPicker.launch(initialUri?.let(Uri::parse)) },
            addSongs = { addSongsPicker.launch(arrayOf("audio/*")) },
            removeSong = { song -> scope.launch { container.removeSong(song.id) } },
            editGenre = { songs, onSaved -> genreTarget = GenreTarget(songs, onSaved) },
            editTags = { song -> tagsRequest = song.id }
        )
    }

    /** Bottom-nav/rail tab switches only: single-top with saved/restored tab state. */
    fun navigateToTab(route: String) {
        // A tab always opens on its own first page: if it is already in the back
        // stack, drop everything above it (a playlist, settings...), else push it
        // right above Home. No saved/restored state: that is what left people
        // stuck inside an old page when they tapped a tab.
        if (navController.popBackStack(route, inclusive = false)) return
        val target = if (route == AppDestination.Library.route) AppDestination.Library.routeFor(0) else route
        navController.navigate(target) {
            popUpTo(AppDestination.Home.route)
            launchSingleTop = true
        }
    }

    /**
     * Drill-down pushes (Player/Queue/Search/PlaylistDetail/Favorites). Plain
     * push so back returns to the screen the user actually came from, not to
     * Home — popUpTo(Home) is only correct for the 4 tab destinations above.
     */
    fun navigate(route: String) {
        navController.navigate(route) { launchSingleTop = true }
    }

    // "Share > Crate" from another app: open the import with the shared link filled in.
    val pendingShare by container.importManager.pendingShare.collectAsStateWithLifecycle()
    LaunchedEffect(pendingShare) {
        if (pendingShare != null && currentRoute != AppDestination.Import.route) navigate(AppDestination.Import.route)
    }

    fun openTags(songId: Long) {
        scope.launch { sheet.snapTo(0f) }
        navigate(AppDestination.Tags.routeFor(songId))
    }
    LaunchedEffect(tagsRequest) {
        tagsRequest?.let {
            tagsRequest = null
            openTags(it)
        }
    }

    fun openCollection(ref: CollectionRef) {
        navigate(AppDestination.Collection.routeFor(ref, Uri::encode))
    }

    fun openPlayerForCurrentTrack() {
        if (snapshot.song != null) settleSheet(1f)
    }

    @Composable
    fun MiniPlayerSlot(modifier: Modifier = Modifier) {
        // Visibility/route gating lives in the AnimatedVisibility wrapper below
        // (so it can still render the last-known song while animating out);
        // this guard only protects against a genuinely absent song.
        val song = snapshot.song ?: return
        val palette = rememberArtworkPalette(song.artworkUri, ResonanceTheme.look.artworkColors)
        val progress = if (snapshot.durationMs > 0L) {
            snapshot.positionMs.toFloat() / snapshot.durationMs.toFloat()
        } else {
            0f
        }
        ResonanceMiniPlayer(
            title = song.title,
            artist = song.artistName,
            artwork = {
                ArtworkImage(
                    artworkUri = song.artworkUri,
                    contentDescription = song.albumName
                )
            },
            isPlaying = snapshot.isPlaying,
            progress = progress,
            playDescription = stringResource(R.string.cd_play),
            pauseDescription = stringResource(R.string.cd_pause),
            nextDescription = stringResource(R.string.cd_next),
            onToggle = { scope.launch { container.togglePlayPause() } },
            onOpenPlayer = ::openPlayerForCurrentTrack,
            onNext = { scope.launch { container.skipToNext() } },
            onPrevious = { scope.launch { container.skipToPrevious() } },
            modifier = modifier
                .onGloballyPositioned { miniBounds = it.boundsInRoot() }
                .graphicsLayer { alpha = 1f - (sheet.value * 5f).coerceIn(0f, 1f) },
            containerColor = palette.surface,
            accent = palette.accent,
            onExpandDrag = ::dragSheet,
            onExpandDragStopped = { velocity ->
                settleSheet(if (velocity < -800f || sheet.value > 0.3f) 1f else 0f, velocity)
            },
            onAnchor = { anchor, bounds -> if (anchors.mini[anchor] != bounds) anchors.mini[anchor] = bounds },
            anchorAlpha = { if (sheet.value > 0f) 0f else 1f }
        )
    }

    @Composable
    fun AppGraph(modifier: Modifier = Modifier) {
        val motion = ResonanceTheme.motion
        NavHost(
            navController = navController,
            startDestination = AppDestination.Home.route,
            modifier = modifier,
            // Quick fade-through: the old screen is gone in 70 ms, the new one settles in.
            enterTransition = { fadeIn(motion.duration(150)) + scaleIn(motion.spatialFast(), initialScale = 0.985f) },
            exitTransition = { fadeOut(motion.duration(70)) },
            popEnterTransition = { fadeIn(motion.duration(150)) + scaleIn(motion.spatialFast(), initialScale = 0.985f) },
            popExitTransition = { fadeOut(motion.duration(70)) }
        ) {
            screen(AppDestination.Home.route) {
                val vm: HomeViewModel = viewModel(
                    factory = factory {
                        HomeViewModel(
                            HomeDependencies(
                                observeRecentlyPlayed = container.observeRecentlyPlayed,
                                observeMostPlayed = container.observeMostPlayed,
                                observeRecentlyAdded = container.observeRecentlyAdded,
                                observeStorageOverview = container.observeStorageOverview,
                                observeScanState = container.observeScanState,
                                observeGenres = container.observeGenres,
                                observeListeningStats = container.observeListeningStats,
                                observeSongs = container.observeSongs,
                                observePlaylists = container.observePlaylists,
                                settings = container.settingsRepository,
                                playSongs = container.playSongs,
                                setShuffleMode = container.setShuffleMode,
                                togglePlayPause = container.togglePlayPause,
                                playNext = container.playNext,
                                appendToQueue = container.appendToQueue,
                                addSongToPlaylist = container.addSongToPlaylist,
                                createPlaylist = container.createPlaylist
                            )
                        )
                    }
                )
                HomeScreen(
                    vm,
                    snapshot,
                    onOpenSearch = { navigate(AppDestination.Search.route) },
                    onOpenSettings = { navigate(AppDestination.Settings.route) },
                    onOpenPlayer = { settleSheet(1f) },
                    onOpenPlaylist = { navigate(AppDestination.PlaylistDetail.routeFor(it)) },
                    onOpenPlaylists = { navigateToTab(AppDestination.Playlists.route) },
                    onOpenFavorites = { navigate(AppDestination.Favorites.route) },
                    onOpenLibrary = { navigate(AppDestination.Library.routeFor(it)) },
                    onOpenCollection = ::openCollection,
                    onOpenImport = { navigate(AppDestination.Import.route) },
                    onCustomize = { navigate(AppDestination.HomeEditor.route) }
                )
            }
            // Draws its artwork-tinted background under the status bar itself.
            composable(
                route = AppDestination.Collection.route,
                arguments = listOf(
                    navArgument(AppDestination.Collection.ARG_KIND) { type = NavType.StringType },
                    navArgument(AppDestination.Collection.ARG_KEY) { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument(AppDestination.Collection.ARG_EXTRA) { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) { entry ->
                val args = entry.arguments
                val ref = AppDestination.Collection.refFor(
                    args?.getString(AppDestination.Collection.ARG_KIND),
                    args?.getString(AppDestination.Collection.ARG_KEY),
                    args?.getString(AppDestination.Collection.ARG_EXTRA)
                ) ?: return@composable
                val vm: CollectionViewModel = viewModel(
                    key = "collection-${entry.id}",
                    factory = factory {
                        CollectionViewModel(
                            ref,
                            container.getAlbumSongs,
                            container.getArtistSongs,
                            container.getGenreSongs,
                            container.getFolderSongs,
                            container.playSongs,
                            container.setShuffleMode,
                            container.libraryEdits
                        )
                    }
                )
                CollectionScreen(
                    vm,
                    snapshot.song?.id,
                    onBack = { navController.popBackStack() },
                    onSongClick = { /* a tap just plays; the mini player opens Now Playing */ }
                )
            }
            screen(
                route = AppDestination.Tags.route,
                arguments = listOf(navArgument(AppDestination.Tags.ARG_SONG_ID) { type = NavType.LongType })
            ) { entry ->
                val songId = entry.arguments?.getLong(AppDestination.Tags.ARG_SONG_ID) ?: -1L
                val vm: TagEditorViewModel = viewModel(
                    key = "tags-$songId",
                    factory = factory {
                        TagEditorViewModel(
                            songId,
                            container.getSong,
                            container.metadataLookup,
                            container.tagRepository,
                            container.rescanLibrary,
                            container.libraryEdits,
                            container.audioFingerprinter
                        )
                    }
                )
                TagEditorScreen(vm, onBack = { navController.popBackStack() })
            }
            screen(AppDestination.Import.route) {
                ImportScreen(container, onBack = { navController.popBackStack() })
            }
            screen(AppDestination.HomeEditor.route) {
                val vm: HomeEditorViewModel = viewModel(factory = factory { HomeEditorViewModel(container.settingsRepository) })
                HomeEditorScreen(vm, onBack = { navController.popBackStack() })
            }
            screen(
                route = AppDestination.Library.route,
                arguments = listOf(navArgument(AppDestination.Library.ARG_TAB) {
                    type = NavType.IntType
                    defaultValue = 0
                })
            ) { entry ->
                val initialTab = entry.arguments?.getInt(AppDestination.Library.ARG_TAB) ?: 0
                val vm: LibraryViewModel = viewModel(
                    factory = factory {
                        LibraryViewModel(
                            container.observeSongs,
                            container.playSongs,
                            container.observeScanState,
                            container.observeAlbums,
                            container.observeArtists,
                            container.observeGenres,
                            container.observeFolders,
                            container.setShuffleMode,
                            container.playNext,
                            container.appendToQueue,
                            container.observePlaylists,
                            container.addSongToPlaylist,
                            container.createPlaylist,
                            container.rescanLibrary
                        )
                    }
                )
                LibraryScreen(
                    vm,
                    initialTab,
                    snapshot.song?.id,
                    onSongClick = { /* a tap just plays; the mini player opens Now Playing */ },
                    onOpenSearch = { navigate(AppDestination.Search.route) },
                    onOpenCollection = ::openCollection,
                    onOpenFavorites = { navigate(AppDestination.Favorites.route) }
                )
            }
            screen(AppDestination.Search.route) {
                val vm: SearchViewModel = viewModel(
                    factory = factory {
                        SearchViewModel(
                            container.searchLibrary,
                            container.searchAll,
                            container.playSongs,
                            container.getAlbumSongs,
                            container.getArtistSongs,
                            container.getGenreSongs,
                            container.playNext,
                            container.appendToQueue,
                            container.observePlaylists,
                            container.addSongToPlaylist,
                            container.createPlaylist
                        )
                    }
                )
                SearchScreen(
                    vm,
                    snapshot.song?.id,
                    onBack = { navController.popBackStack() },
                    onSongClick = { /* a tap just plays; the mini player opens Now Playing */ },
                    onOpenCollection = ::openCollection,
                    onOpenPlaylist = { navigate(AppDestination.PlaylistDetail.routeFor(it)) }
                )
            }
            screen(AppDestination.Settings.route) {
                val vm: SettingsViewModel = viewModel(
                    factory = factory {
                        SettingsViewModel(
                            container.settingsRepository,
                            container.observeScanState,
                            container.rescanLibrary,
                            container.observeSources,
                            container.removeSources,
                            container.getLibraryStats,
                            container.observeLastScan,
                            container.libraryPreferences
                        )
                    }
                )
                SettingsScreen(
                    vm,
                    onBack = { navController.popBackStack() },
                    onOpenHomeEditor = { navigate(AppDestination.HomeEditor.route) }
                )
            }
            screen(AppDestination.Queue.route) {
                val vm: QueueViewModel = viewModel(
                    factory = factory {
                        QueueViewModel(
                            container.playbackController,
                            container.moveQueueItem,
                            container.removeQueueItem,
                            container.clearQueue,
                            container.skipToQueueItem
                        )
                    }
                )
                QueueScreen(vm, onBack = { navController.popBackStack() })
            }
            screen(AppDestination.Playlists.route) {
                val vm: PlaylistsViewModel = viewModel(
                    factory = factory {
                        PlaylistsViewModel(
                            container.observePlaylists,
                            container.createPlaylist,
                            container.deletePlaylist,
                            container.renamePlaylist,
                            container.observePlaylistSongs,
                            container.playSongs
                        )
                    }
                )
                PlaylistsScreen(
                    vm,
                    onOpenDetail = { navigate(AppDestination.PlaylistDetail.routeFor(it)) },
                    onOpenQueue = { navigate(AppDestination.Queue.route) },
                    onShowMessage = ::showMessage
                )
            }
            screen(
                route = AppDestination.PlaylistDetail.route,
                arguments = listOf(navArgument(AppDestination.PlaylistDetail.ARG_PLAYLIST_ID) {
                    type = NavType.LongType
                })
            ) { entry ->
                val playlistId =
                    entry.arguments?.getLong(AppDestination.PlaylistDetail.ARG_PLAYLIST_ID) ?: -1L
                val vm: PlaylistDetailViewModel = viewModel(
                    key = "playlist-$playlistId",
                    factory = factory {
                        PlaylistDetailViewModel(
                            playlistId,
                            container.observePlaylistSongs,
                            container.playSongs,
                            container.setShuffleMode,
                            container.renamePlaylist,
                            container.deletePlaylist,
                            container.removeSongFromPlaylist,
                            container.movePlaylistItem,
                            container.observeSongs,
                            container.addSongToPlaylist,
                            container.setPlaylistCover
                        )
                    }
                )
                val playlistsFlow = remember { container.observePlaylists() }
                val playlists by playlistsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
                PlaylistDetailScreen(
                    vm,
                    playlists.firstOrNull { it.id == playlistId },
                    onBack = { navController.popBackStack() },
                    onSongClick = { /* a tap just plays; the mini player opens Now Playing */ },
                    onDeleted = { navController.popBackStack() }
                )
            }
            screen(AppDestination.Favorites.route) {
                val vm: FavoritesViewModel = viewModel(
                    factory = factory {
                        FavoritesViewModel(
                            container.observeFavoriteSongs,
                            container.playSongs,
                            container.playNext,
                            container.appendToQueue,
                            container.observePlaylists,
                            container.addSongToPlaylist,
                            container.createPlaylist,
                            container.setShuffleMode
                        )
                    }
                )
                FavoritesScreen(
                    vm,
                    snapshot.song?.id,
                    onBack = { navController.popBackStack() },
                    onSongClick = { /* a tap just plays; the mini player opens Now Playing */ }
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ResonanceTheme.colors.background)
            .onSizeChanged {
                rootHeight = it.height.toFloat().coerceAtLeast(1f)
                rootWidth = it.width.toFloat().coerceAtLeast(1f)
            }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (widthSize == WindowWidthSize.EXPANDED) {
                ResonanceRail(
                    destinations = dockDestinations,
                    selectedRoute = selectedTab,
                    onSelect = ::navigateToTab
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                CompositionLocalProvider(LocalMusicActions provides musicActions) {
                    AppGraph(modifier = Modifier.weight(1f))
                }
                AnimatedVisibility(
                    visible = showMiniPlayer,
                    enter = fadeIn(ResonanceTheme.motion.duration(200)) + slideInVertically(ResonanceTheme.motion.spatial()) { it },
                    exit = fadeOut(ResonanceTheme.motion.duration(150)) + slideOutVertically(ResonanceTheme.motion.spatial()) { it }
                ) {
                    Box(modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 8.dp)) {
                        MiniPlayerSlot()
                    }
                }
                AnimatedVisibility(
                    visible = widthSize != WindowWidthSize.EXPANDED,
                    enter = slideInVertically(ResonanceTheme.motion.spatial()) { it },
                    exit = slideOutVertically(ResonanceTheme.motion.spatial()) { it }
                ) {
                    ResonanceNavDock(
                        destinations = dockDestinations,
                        selectedRoute = selectedTab,
                        onSelect = ::navigateToTab,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    )
                }
            }
        }
        val snackbarBottomInset = if (widthSize != WindowWidthSize.EXPANDED) {
            ResonanceTheme.dimensions.navigationDockHeight + if (showMiniPlayer) {
                ResonanceTheme.dimensions.miniPlayerHeight
            } else {
                0.dp
            }
        } else {
            0.dp
        }
        // Composed as soon as something is loaded (hidden off-screen while collapsed), so
        // opening never pays for building Now Playing on its first frame.
        if (snapshot.song != null || sheetVisible) {
            val song = snapshot.song
            val sheetPalette = rememberArtworkPalette(song?.artworkUri, ResonanceTheme.look.artworkColors)
            NowPlayingSheet(
                progress = { sheet.value },
                miniBounds = miniBounds,
                anchors = anchors,
                fallbackArt = estimatedCoverBounds(rootWidth, rootHeight),
                artworkUri = song?.artworkUri,
                title = song?.title.orEmpty(),
                artist = song?.artistName.orEmpty(),
                playing = snapshot.isPlaying,
                palette = sheetPalette,
                coverScale = if (snapshot.isPlaying) 1f else 0.86f
            ) {
                val vm: PlayerViewModel = viewModel(
                    key = "now-playing",
                    factory = factory {
                        PlayerViewModel(
                            -1L,
                            container.getSong,
                            container.playbackController,
                            container.togglePlayPause,
                            container.seekTo,
                            container.skipToNext,
                            container.skipToPrevious,
                            container.setShuffleMode,
                            container.setRepeatMode,
                            container.toggleFavorite,
                            container.observeFavoriteIds,
                            container.playbackSources
                        )
                    }
                )
                PlayerScreen(
                    vm,
                    onOpenQueue = {
                        settleSheet(0f)
                        navigate(AppDestination.Queue.route)
                    },
                    onBack = { settleSheet(0f) },
                    onCollapseDrag = ::dragSheet,
                    onCollapseDragStopped = { velocity ->
                        settleSheet(if (velocity > 800f || sheet.value < 0.85f) 0f else 1f, velocity)
                    },
                    // Taken only at rest (open, or hidden while collapsed), so it never chases the moving content.
                    onAnchor = { anchor, bounds ->
                        val p = sheet.value
                        val resting = when {
                            p >= 0.999f -> bounds
                            // Hidden while collapsed: undo the off-screen offset and the 8% rise.
                            p <= 0f -> bounds.translate(0f, -(1_000_000f + rootHeight * 0.08f))
                            else -> null
                        }
                        if (resting != null && anchors.big[anchor] != resting) anchors.big[anchor] = resting
                    },
                    anchorAlpha = { if (sheet.value >= 0.999f) 1f else 0f },
                    expanded = sheetExpanded
                )
            }
        }
        BackHandler(enabled = sheetVisible) { settleSheet(0f) }
        genreTarget?.let { target ->
            val genresFlow = remember { container.observeGenres() }
            val genres by genresFlow.collectAsStateWithLifecycle(initialValue = emptyList())
            GenreDialog(
                songCount = target.songs.size,
                current = target.songs.map { it.genreName }.distinct().singleOrNull(),
                genres = genres,
                onSave = { genre ->
                    val clean = genre?.trim()?.takeIf { it.isNotEmpty() }
                    genreTarget = null
                    scope.launch {
                        container.setGenre(target.songs.map { it.id }, clean)
                        container.libraryEdits.tryEmit(Unit)
                        target.onSaved(clean)
                    }
                },
                onDismiss = { genreTarget = null }
            )
        }
        SnackbarHost(
            hostState = snackbarHostState,
            snackbar = { data -> ResonanceSnackbar(data) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = snackbarBottomInset)
        )
    }
}

@Composable
private fun ResonanceRail(
    destinations: List<NavDockDestination>,
    selectedRoute: String?,
    onSelect: (String) -> Unit
) {
    val colors = ResonanceTheme.colors
    NavigationRail(
        containerColor = colors.surfaceContainer,
        contentColor = colors.textSecondary,
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        destinations.forEach { destination ->
            val selected = destination.route == selectedRoute
            NavigationRailItem(
                selected = selected,
                onClick = { onSelect(destination.route) },
                icon = {
                    Icon(destination.icon, contentDescription = destination.label)
                },
                label = { Text(destination.label) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = colors.accent,
                    selectedTextColor = colors.accent,
                    unselectedIconColor = colors.textSecondary,
                    unselectedTextColor = colors.textSecondary,
                    indicatorColor = colors.accentDim
                )
            )
        }
    }
}

/**
 * A regular destination: content starts below the status bar. Now Playing is
 * the one route that does not use this, so its colors reach under the status bar.
 */
private fun NavGraphBuilder.screen(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) {
    composable(route = route, arguments = arguments) { entry ->
        val scope = this
        Box(Modifier.fillMaxSize().statusBarsPadding()) { scope.content(entry) }
    }
}

/** Songs waiting in the genre dialog, and what to do once a genre is saved. */
/** Until the big cover has been measured once: a square across the width, a bit below the top. */
private fun estimatedCoverBounds(width: Float, rootHeight: Float): Rect {
    val side = width * 0.88f
    val left = (width - side) / 2f
    return Rect(left, rootHeight * 0.14f, left + side, rootHeight * 0.14f + side)
}

private class GenreTarget(val songs: List<Song>, val onSaved: (String?) -> Unit)

private inline fun <reified T : ViewModel> factory(crossinline create: () -> T): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <M : ViewModel> create(modelClass: Class<M>): M = create() as M
    }
