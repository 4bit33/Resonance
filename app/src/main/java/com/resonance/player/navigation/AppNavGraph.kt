package com.resonance.player.navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.resonance.player.R
import com.resonance.player.app.AppContainer
import com.resonance.player.core.ui.adaptive.WindowWidthSize
import com.resonance.player.core.ui.adaptive.rememberWindowWidthSize
import com.resonance.player.feature.home.HomeScreen
import com.resonance.player.feature.library.LibraryScreen
import com.resonance.player.feature.library.LibraryViewModel
import com.resonance.player.feature.player.PlayerScreen
import com.resonance.player.feature.player.PlayerViewModel
import com.resonance.player.feature.playlists.PlaylistsScreen
import com.resonance.player.feature.queue.QueueScreen
import com.resonance.player.feature.queue.QueueViewModel
import com.resonance.player.feature.search.SearchScreen
import com.resonance.player.feature.search.SearchViewModel
import com.resonance.player.feature.settings.SettingsScreen
import com.resonance.player.feature.settings.SettingsViewModel

private data class Tab(
    val destination: AppDestination,
    val labelRes: Int,
    val icon: @Composable () -> Unit
)

/**
 * Foundation navigation shell. Bottom bar on phones, navigation rail on
 * expanded widths (foldables / tablets) — same graph, adaptive chrome.
 */
@Composable
fun ResonanceAppShell(container: AppContainer) {
    val navController = rememberNavController()
    val widthSize = rememberWindowWidthSize()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val tabs = listOf(
        Tab(AppDestination.Home, R.string.nav_home) { Icon(Icons.Filled.Home, null) },
        Tab(AppDestination.Library, R.string.nav_library) { Icon(Icons.AutoMirrored.Filled.List, null) },
        Tab(AppDestination.Search, R.string.nav_search) { Icon(Icons.Filled.Search, null) },
        Tab(AppDestination.Settings, R.string.nav_settings) { Icon(Icons.Filled.Settings, null) }
    )

    fun navigate(route: String) {
        navController.navigate(route) {
            popUpTo(AppDestination.Home.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Row(Modifier.fillMaxSize()) {
        if (widthSize == WindowWidthSize.EXPANDED) {
            NavigationRail {
                tabs.forEach { tab ->
                    NavigationRailItem(
                        selected = currentRoute == tab.destination.route,
                        onClick = { navigate(tab.destination.route) },
                        icon = tab.icon,
                        label = { Text(stringResource(tab.labelRes)) }
                    )
                }
            }
        }
        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
            NavHost(
                navController = navController,
                startDestination = AppDestination.Home.route,
                modifier = Modifier.weight(1f)
            ) {
                composable(AppDestination.Home.route) {
                    HomeScreen(
                        onOpenLibrary = { navigate(AppDestination.Library.route) },
                        onOpenSearch = { navigate(AppDestination.Search.route) },
                        onOpenSettings = { navigate(AppDestination.Settings.route) }
                    )
                }
                composable(AppDestination.Library.route) {
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
                                container.rescanLibrary
                            )
                        }
                    )
                    LibraryScreen(
                        vm,
                        container.permissionManager,
                        onSongClick = { navigate(AppDestination.Player.routeFor(it)) },
                        onOpenQueue = { navigate(AppDestination.Queue.route) }
                    )
                }
                composable(AppDestination.Search.route) {
                    val vm: SearchViewModel = viewModel(
                        factory = factory { SearchViewModel(container.searchLibrary) }
                    )
                    SearchScreen(vm)
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
                                container.observeLastScan
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
                                container.setRepeatMode
                            )
                        }
                    )
                    PlayerScreen(vm, onOpenQueue = { navigate(AppDestination.Queue.route) })
                }
                composable(AppDestination.Queue.route) {
                    val vm: QueueViewModel = viewModel(
                        factory = factory { QueueViewModel(container.playbackController) }
                    )
                    QueueScreen(vm)
                }
                composable(AppDestination.Playlists.route) { PlaylistsScreen() }
            }
            if (widthSize != WindowWidthSize.EXPANDED) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.destination.route,
                            onClick = { navigate(tab.destination.route) },
                            icon = tab.icon,
                            label = { Text(stringResource(tab.labelRes)) }
                        )
                    }
                }
            }
        }
    }
}

private inline fun <reified T : ViewModel> factory(crossinline create: () -> T): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <M : ViewModel> create(modelClass: Class<M>): M = create() as M
    }

