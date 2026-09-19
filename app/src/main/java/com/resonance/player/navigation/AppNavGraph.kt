package com.resonance.player.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
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
import com.resonance.player.app.AppContainer
import com.resonance.player.core.ui.adaptive.WindowWidthSize
import com.resonance.player.core.ui.adaptive.rememberWindowWidthSize
import com.resonance.player.core.ui.components.ArtworkImage
import com.resonance.player.core.ui.components.NavDockDestination
import com.resonance.player.core.ui.components.ResonanceMiniPlayer
import com.resonance.player.core.ui.components.ResonanceNavDock
import com.resonance.player.core.ui.components.ResonanceSnackbar
import com.resonance.player.core.ui.components.ResonanceSnackbarVisuals
import com.resonance.player.core.ui.components.shouldShowMiniPlayer
import com.resonance.player.feature.favorites.FavoritesScreen
import com.resonance.player.feature.favorites.FavoritesViewModel
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
        Icons.Filled.Home
    ),
    NavDockDestination(
        AppDestination.Library.route,
        stringResource(R.string.nav_library),
        Icons.AutoMirrored.Filled.List
    ),
    NavDockDestination(
        AppDestination.Playlists.route,
        stringResource(R.string.nav_playlists),
        Icons.AutoMirrored.Filled.QueueMusic
    ),
    NavDockDestination(
        AppDestination.Settings.route,
        stringResource(R.string.nav_settings),
        Icons.Filled.Settings
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
    val onPlayerOrQueue = currentRoute == AppDestination.Player.route ||
        currentRoute == AppDestination.Queue.route
    val showMiniPlayer = shouldShowMiniPlayer(snapshot) && !onPlayerOrQueue
    val dockDestinations = dockDestinations()
    val snackbarHostState = remember { SnackbarHostState() }

    /** Shared feedback channel for actions that otherwise silently no-op (e.g. playing an empty playlist). */
    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(ResonanceSnackbarVisuals(message)) }
    }

    /** Bottom-nav/rail tab switches only: single-top with saved/restored tab state. */
    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(AppDestination.Home.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
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

    fun openPlayerForCurrentTrack() {
        val song = snapshot.song ?: return
        navigate(AppDestination.Player.routeFor(song.id))
    }

    @Composable
    fun MiniPlayerSlot(modifier: Modifier = Modifier) {
        // Visibility/route gating lives in the AnimatedVisibility wrapper below
        // (so it can still render the last-known song while animating out);
        // this guard only protects against a genuinely absent song.
        val song = snapshot.song ?: return
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
            modifier = modifier
        )
    }

    @Composable
    fun AppGraph(modifier: Modifier = Modifier) {
        NavHost(
            navController = navController,
            startDestination = AppDestination.Home.route,
            modifier = modifier,
            enterTransition = { fadeIn(tween(180)) },
            exitTransition = { fadeOut(tween(120)) },
            popEnterTransition = { fadeIn(tween(180)) },
            popExitTransition = { fadeOut(tween(120)) }
        ) {
            composable(AppDestination.Home.route) {
                val vm: HomeViewModel = viewModel(
                    factory = factory {
                        HomeViewModel(
                            container.observeRecentlyPlayed,
                            container.observeMostPlayed,
                            container.observeRecentlyAdded,
                            container.observeStorageOverview,
                            container.observeScanState,
                            container.observeFavoriteIds,
                            container.playSongs,
                            container.getAlbumSongs,
                            container.setShuffleMode,
                            container.rescanLibrary,
                            container.playNext,
                            container.appendToQueue,
                            container.observePlaylists,
                            container.addSongToPlaylist,
                            container.createPlaylist
                        )
                    }
                )
                HomeScreen(
                    vm,
                    container.permissionManager,
                    snapshot.song?.id,
                    onOpenLibrary = { navigate(AppDestination.Library.routeFor(it)) },
                    onOpenSearch = { navigate(AppDestination.Search.route) },
                    onOpenFavorites = { navigate(AppDestination.Favorites.route) },
                    onSongClick = { navigate(AppDestination.Player.routeFor(it)) },
                    onOpenQueue = { navigate(AppDestination.Queue.route) }
                )
            }
            composable(
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
                            container.getAlbumSongs,
                            container.getArtistSongs,
                            container.getGenreSongs,
                            container.getFolderSongs,
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
                    container.permissionManager,
                    initialTab,
                    snapshot.song?.id,
                    onSongClick = { navigate(AppDestination.Player.routeFor(it)) },
                    onOpenQueue = { navigate(AppDestination.Queue.route) },
                    onOpenSearch = { navigate(AppDestination.Search.route) }
                )
            }
            composable(AppDestination.Search.route) {
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
                    onSongClick = { navigate(AppDestination.Player.routeFor(it)) },
                    onOpenQueue = { navigate(AppDestination.Queue.route) },
                    onOpenPlaylist = { navigate(AppDestination.PlaylistDetail.routeFor(it)) }
                )
            }
            composable(AppDestination.Settings.route) {
                val vm: SettingsViewModel = viewModel(
                    factory = factory {
                        SettingsViewModel(
                            container.settingsRepository,
                            container.permissionManager,
                            container.observeScanState,
                            container.rescanLibrary,
                            container.getLibraryStats,
                            container.observeLastScan,
                            container.libraryPreferences
                        )
                    }
                )
                SettingsScreen(vm)
            }
            composable(
                route = AppDestination.Player.route,
                arguments = listOf(navArgument(AppDestination.Player.ARG_SONG_ID) {
                    type = NavType.LongType
                })
            ) { entry ->
                val songId = entry.arguments?.getLong(AppDestination.Player.ARG_SONG_ID) ?: -1L
                val vm: PlayerViewModel = viewModel(
                    key = "player-$songId",
                    factory = factory {
                        PlayerViewModel(
                            songId,
                            container.getSong,
                            container.playbackController,
                            container.togglePlayPause,
                            container.seekTo,
                            container.skipToNext,
                            container.skipToPrevious,
                            container.setShuffleMode,
                            container.setRepeatMode,
                            container.toggleFavorite,
                            container.observeFavoriteIds
                        )
                    }
                )
                PlayerScreen(
                    vm,
                    onOpenQueue = { navigate(AppDestination.Queue.route) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(AppDestination.Queue.route) {
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
            composable(AppDestination.Playlists.route) {
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
            composable(
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
                            container.addSongToPlaylist
                        )
                    }
                )
                val playlistsFlow = remember { container.observePlaylists() }
                val playlists by playlistsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
                PlaylistDetailScreen(
                    vm,
                    playlists.firstOrNull { it.id == playlistId },
                    onBack = { navController.popBackStack() },
                    onSongClick = { navigate(AppDestination.Player.routeFor(it)) },
                    onDeleted = { navController.popBackStack() }
                )
            }
            composable(AppDestination.Favorites.route) {
                val vm: FavoritesViewModel = viewModel(
                    factory = factory {
                        FavoritesViewModel(
                            container.observeFavoriteSongs,
                            container.playSongs,
                            container.playNext,
                            container.appendToQueue,
                            container.observePlaylists,
                            container.addSongToPlaylist,
                            container.createPlaylist
                        )
                    }
                )
                FavoritesScreen(
                    vm,
                    snapshot.song?.id,
                    onBack = { navController.popBackStack() },
                    onSongClick = { navigate(AppDestination.Player.routeFor(it)) }
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            if (widthSize == WindowWidthSize.EXPANDED) {
                ResonanceRail(
                    destinations = dockDestinations,
                    selectedRoute = selectedTab,
                    onSelect = ::navigateToTab
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                AppGraph(modifier = Modifier.weight(1f))
                AnimatedVisibility(
                    visible = showMiniPlayer,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it }
                ) {
                    Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                        MiniPlayerSlot()
                    }
                }
                if (widthSize != WindowWidthSize.EXPANDED) {
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
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
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

private inline fun <reified T : ViewModel> factory(crossinline create: () -> T): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <M : ViewModel> create(modelClass: Class<M>): M = create() as M
    }
