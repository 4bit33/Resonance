package com.resonance.player.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.resonance.player.R
import com.resonance.player.core.common.formatDurationMs
import com.resonance.player.core.common.userMessage
import com.resonance.player.core.media.ScanState
import com.resonance.player.core.permissions.AudioPermissionManager
import com.resonance.player.core.ui.components.ArtworkImage
import com.resonance.player.core.ui.components.AudioPermissionGate
import com.resonance.player.core.ui.components.EmptyLibraryView
import com.resonance.player.core.ui.components.ErrorView
import com.resonance.player.core.ui.components.LoadingView

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    permissionManager: AudioPermissionManager,
    onSongClick: (Long) -> Unit,
    onOpenQueue: () -> Unit
) {
    AudioPermissionGate(
        manager = permissionManager,
        onPermissionGranted = viewModel::rescan
    ) {
        LibraryTabs(viewModel, onSongClick, onOpenQueue)
    }
}

@Composable
private fun LibraryTabs(
    viewModel: LibraryViewModel,
    onSongClick: (Long) -> Unit,
    onOpenQueue: () -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    val scanState by viewModel.scanState.collectAsState()
    Column(Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.nav_library),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )
        ScanBanner(scanState)
        ScrollableTabRow(selectedTabIndex = tab, edgePadding = 16.dp) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.tab_songs)) })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.tab_albums)) })
            Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text(stringResource(R.string.tab_artists)) })
            Tab(selected = tab == 3, onClick = { tab = 3 }, text = { Text(stringResource(R.string.tab_genres)) })
            Tab(selected = tab == 4, onClick = { tab = 4 }, text = { Text(stringResource(R.string.tab_folders)) })
        }
        when (tab) {
            0 -> SongsTab(viewModel, onSongClick)
            1 -> AlbumsTab(viewModel, onOpenQueue)
            2 -> ArtistsTab(viewModel)
            3 -> GenresTab(viewModel)
            4 -> FoldersTab(viewModel)
        }
    }
}

@Composable
private fun ScanBanner(state: ScanState) {
    when (state) {
        is ScanState.Scanning -> {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(stringResource(R.string.scan_scanning), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                val progress = if (state.total > 0) {
                    state.processed.toFloat() / state.total.toFloat()
                } else {
                    null
                }
                if (progress != null) {
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
        is ScanState.Completed -> {
            if (state.report.failed > 0) {
                Text(
                    text = stringResource(R.string.scan_failed_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
        is ScanState.Failed -> {
            Text(
                text = state.error.userMessage(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        else -> Unit
    }
}

@Composable
private fun SongsTab(viewModel: LibraryViewModel, onSongClick: (Long) -> Unit) {
    val state by viewModel.uiState.collectAsState()
    when (val s = state) {
        LibraryUiState.Loading -> LoadingView()
        LibraryUiState.Empty -> EmptyLibraryView()
        is LibraryUiState.Error -> ErrorView(message = s.message)
        is LibraryUiState.Content -> LazyColumn(Modifier.fillMaxSize()) {
            items(s.songs, key = { it.id }) { song ->
                ListItem(
                    headlineContent = { Text(song.title) },
                    supportingContent = { Text(song.artistName + " - " + song.albumName) },
                    trailingContent = { Text(formatDurationMs(song.durationMs)) },
                    leadingContent = {
                        ArtworkImage(
                            artworkUri = song.artworkUri,
                            contentDescription = song.albumName,
                            modifier = Modifier.size(48.dp),
                            fallbackSize = 20.dp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val index = s.songs.indexOfFirst { it.id == song.id }
                            if (index >= 0) viewModel.playFrom(s.songs, index)
                            onSongClick(song.id)
                        }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun AlbumsTab(viewModel: LibraryViewModel, onOpenQueue: () -> Unit) {
    val albums by viewModel.albums.collectAsState()
    if (albums.isEmpty()) {
        EmptyLibraryView()
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(albums, key = { it.id }) { album ->
            ListItem(
                headlineContent = { Text(album.name) },
                supportingContent = {
                    Text(album.artistName + " - " + album.songCount + " - " + formatDurationMs(album.totalDurationMs))
                },
                leadingContent = {
                    ArtworkImage(
                        artworkUri = album.artUri,
                        contentDescription = album.name,
                        modifier = Modifier.size(56.dp),
                        fallbackSize = 24.dp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.playAlbum(album.name, album.albumArtist)
                        onOpenQueue()
                    }
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun ArtistsTab(viewModel: LibraryViewModel) {
    val artists by viewModel.artists.collectAsState()
    if (artists.isEmpty()) {
        EmptyLibraryView()
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(artists, key = { it.id }) { artist ->
            ListItem(
                headlineContent = { Text(artist.name) },
                supportingContent = { Text(artist.songCount.toString() + " - " + artist.albumCount) },
                modifier = Modifier.fillMaxWidth()
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun GenresTab(viewModel: LibraryViewModel) {
    val genres by viewModel.genres.collectAsState()
    if (genres.isEmpty()) {
        EmptyLibraryView()
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(genres, key = { it.name }) { genre ->
            ListItem(
                headlineContent = { Text(genre.name) },
                supportingContent = { Text(genre.songCount.toString()) },
                modifier = Modifier.fillMaxWidth()
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun FoldersTab(viewModel: LibraryViewModel) {
    val folders by viewModel.folders.collectAsState()
    if (folders.isEmpty()) {
        EmptyLibraryView()
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(folders, key = { it.path }) { folder ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(folder.name, style = MaterialTheme.typography.bodyLarge)
                    if (folder.path.isNotEmpty()) {
                        Text(
                            folder.path,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(folder.songCount.toString(), style = MaterialTheme.typography.bodyMedium)
            }
            HorizontalDivider()
        }
    }
}
