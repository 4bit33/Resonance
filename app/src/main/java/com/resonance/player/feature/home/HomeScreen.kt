package com.resonance.player.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.resonance.player.R
import com.resonance.player.core.common.formatBytes
import com.resonance.player.core.common.formatDurationMs
import com.resonance.player.core.media.ScanState
import com.resonance.player.core.model.Song
import com.resonance.player.core.permissions.AudioPermissionManager
import com.resonance.player.core.ui.components.ArtworkImage
import com.resonance.player.core.ui.components.AudioPermissionGate
import com.resonance.player.core.ui.components.EmptyLibraryView
import com.resonance.player.core.ui.components.ResonanceAlbumCard
import com.resonance.player.core.ui.components.ResonanceCardPlayButton
import com.resonance.player.core.ui.components.ResonanceMetric
import com.resonance.player.core.ui.components.ResonanceSectionHeader
import com.resonance.player.core.ui.components.SongFormatBadge
import com.resonance.player.core.ui.components.ResonanceSongRow
import com.resonance.player.core.ui.components.ResonanceTopBar
import com.resonance.player.core.ui.components.ScanProgressBanner
import com.resonance.player.core.ui.components.songRowState
import com.resonance.player.core.ui.theme.ResonanceTheme

/**
 * Stitch Home: storage health card, Recently Played album carousel,
 * Jump-Back-In tiles, Recently Added rows. Everything is real repository
 * data; empty library shows the permission/scan flow, never demo content.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    permissionManager: AudioPermissionManager,
    currentSongId: Long?,
    onOpenLibrary: (Int) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenFavorites: () -> Unit,
    onSongClick: (Long) -> Unit,
    onOpenQueue: () -> Unit
) {
    AudioPermissionGate(
        manager = permissionManager,
        onPermissionGranted = viewModel::rescan
    ) {
        HomeContent(viewModel, currentSongId, onOpenLibrary, onOpenSearch, onOpenFavorites, onSongClick, onOpenQueue)
    }
}

@Composable
private fun HomeContent(
    viewModel: HomeViewModel,
    currentSongId: Long?,
    onOpenLibrary: (Int) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenFavorites: () -> Unit,
    onSongClick: (Long) -> Unit,
    onOpenQueue: () -> Unit
) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val spacing = ResonanceTheme.spacing
    val scanState by viewModel.scanState.collectAsState()
    val storage by viewModel.storage.collectAsState()
    val trackCount = storage?.trackCount ?: 0
    val isEmpty = trackCount == 0 && scanState !is ScanState.Scanning

    Column(Modifier.fillMaxSize()) {
        ResonanceTopBar(
            title = stringResource(R.string.nav_home),
            searchIcon = Icons.Filled.Search,
            onSearch = onOpenSearch,
            searchDescription = stringResource(R.string.nav_search)
        )
        ScanProgressBanner(scanState)
        if (isEmpty) {
            EmptyLibraryView()
            return@Column
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            StorageCard(
                trackCount = trackCount,
                viewModel = viewModel,
                onRescan = viewModel::rescan
            )
            Spacer(Modifier.height(spacing.sectionSpacing))
            RecentlyPlayedSection(viewModel, onOpenLibrary, onOpenQueue)
            Spacer(Modifier.height(spacing.sectionSpacing))
            JumpBackInSection(viewModel, onOpenFavorites, onOpenLibrary, onOpenQueue)
            Spacer(Modifier.height(spacing.sectionSpacing))
            RecentlyAddedSection(viewModel, currentSongId, onSongClick)
            Spacer(Modifier.height(spacing.xxl))
        }
    }
}

@Composable
private fun StorageCard(
    trackCount: Int,
    viewModel: HomeViewModel,
    onRescan: () -> Unit
) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val spacing = ResonanceTheme.spacing
    val overview by viewModel.storage.collectAsState()
    Surface(
        shape = ResonanceTheme.radii.card,
        color = colors.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg)
    ) {
        Column(Modifier.padding(spacing.cardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = ResonanceTheme.radii.control,
                    color = colors.surfaceHigh,
                    modifier = Modifier.size(32.dp)
                ) {
                    androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.FolderSpecial,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.width(spacing.md))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.home_storage),
                        style = typography.titleMd,
                        color = colors.textPrimary
                    )
                    ResonanceMetric(
                        text = "$trackCount " + stringResource(R.string.settings_songs),
                        color = colors.textSecondary
                    )
                }
                IconButton(onClick = onRescan, modifier = Modifier.size(spacing.touchMin)) {
                    Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.settings_rescan), tint = colors.textSecondary)
                }
            }
            Spacer(Modifier.height(spacing.md))
            val fraction = overview?.usedFraction() ?: 0f
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth(),
                color = colors.accent,
                trackColor = colors.surfaceHighest
            )
            Spacer(Modifier.height(spacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$trackCount " + stringResource(R.string.settings_songs) + " - " +
                        formatBytes(overview?.libraryBytes ?: 0L),
                    style = typography.monoMetric,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Filled.OfflinePin,
                    contentDescription = null,
                    tint = colors.accentSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(spacing.xs))
                Text(
                    stringResource(R.string.home_offline_ready),
                    style = typography.labelSm,
                    color = colors.accentSecondary
                )
            }
        }
    }
}

@Composable
private fun RecentlyPlayedSection(
    viewModel: HomeViewModel,
    onOpenLibrary: (Int) -> Unit,
    onOpenQueue: () -> Unit
) {
    val albums by viewModel.recentAlbums.collectAsState()
    if (albums.isEmpty()) return
    val spacing = ResonanceTheme.spacing
    ResonanceSectionHeader(
        title = stringResource(R.string.home_recently_played),
        actionLabel = stringResource(R.string.home_view_all),
        onAction = { onOpenLibrary(1) }
    )
    Spacer(Modifier.height(spacing.md))
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        modifier = Modifier.fillMaxWidth()
    ) {
        item { Spacer(Modifier.width(spacing.lg - spacing.md)) }
        items(albums, key = { it.id }) { album ->
            ResonanceAlbumCard(
                title = album.name,
                artist = album.artistName,
                artwork = {
                    ArtworkImage(
                        artworkUri = album.artUri,
                        contentDescription = album.name
                    )
                },
                onClick = {
                    viewModel.playAlbum(album.name, album.albumArtist)
                    onOpenQueue()
                },
                playButton = {
                    ResonanceCardPlayButton(
                        onClick = {
                            viewModel.playAlbum(album.name, album.albumArtist)
                            onOpenQueue()
                        },
                        playIcon = Icons.Filled.PlayArrow,
                        contentDescription = stringResource(R.string.cd_play)
                    )
                }
            )
        }
        item { Spacer(Modifier.width(spacing.lg - spacing.md)) }
    }
}

@Composable
private fun JumpBackInSection(
    viewModel: HomeViewModel,
    onOpenFavorites: () -> Unit,
    onOpenLibrary: (Int) -> Unit,
    onOpenQueue: () -> Unit
) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val spacing = ResonanceTheme.spacing
    val favCount by viewModel.favoriteCount.collectAsState()
    val mostPlayed by viewModel.mostPlayed.collectAsState()
    val recentSongs by viewModel.recentSongs.collectAsState()
    ResonanceSectionHeader(title = stringResource(R.string.home_jump_back_in))
    Spacer(Modifier.height(spacing.md))
    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.md),
        modifier = Modifier.padding(horizontal = spacing.lg)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
            JumpTile(
                icon = Icons.Filled.Favorite,
                title = stringResource(R.string.home_favorites),
                subtitle = "$favCount " + stringResource(R.string.settings_songs),
                onClick = onOpenFavorites,
                modifier = Modifier.weight(1f)
            )
            JumpTile(
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                title = stringResource(R.string.home_most_played),
                subtitle = mostPlayed.firstOrNull()?.title
                    ?: stringResource(R.string.settings_never),
                onClick = {
                    if (mostPlayed.isNotEmpty()) {
                        viewModel.playFrom(mostPlayed, 0)
                        onOpenQueue()
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
            JumpTile(
                icon = Icons.Filled.History,
                title = stringResource(R.string.home_recently_added),
                subtitle = recentSongs.firstOrNull()?.title
                    ?: stringResource(R.string.settings_never),
                onClick = { onOpenLibrary(0) },
                modifier = Modifier.weight(1f)
            )
            JumpTile(
                icon = Icons.Filled.Shuffle,
                title = stringResource(R.string.home_shuffle_play),
                subtitle = stringResource(R.string.tab_songs),
                onClick = {
                    if (recentSongs.isNotEmpty()) {
                        viewModel.shufflePlay(recentSongs)
                        onOpenQueue()
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun JumpTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val spacing = ResonanceTheme.spacing
    Surface(
        onClick = onClick,
        shape = ResonanceTheme.radii.card,
        color = colors.surfaceContainer,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(spacing.md)
        ) {
            Surface(
                shape = ResonanceTheme.radii.control,
                color = colors.surfaceHighest,
                modifier = Modifier.size(48.dp)
            ) {
                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = colors.accent)
                }
            }
            Spacer(Modifier.width(spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = typography.labelLg,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    subtitle,
                    style = typography.monoMetric,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun RecentlyAddedSection(
    viewModel: HomeViewModel,
    currentSongId: Long?,
    onSongClick: (Long) -> Unit
) {
    val songs by viewModel.recentSongs.collectAsState()
    if (songs.isEmpty()) return
    val spacing = ResonanceTheme.spacing
    ResonanceSectionHeader(title = stringResource(R.string.home_recently_added))
    Spacer(Modifier.height(spacing.sm))
    Column {
        songs.forEach { song ->
            SongHomeRow(song, song.id == currentSongId, viewModel, songs, onSongClick)
        }
    }
}

@Composable
private fun SongHomeRow(
    song: Song,
    isCurrent: Boolean,
    viewModel: HomeViewModel,
    songs: List<Song>,
    onSongClick: (Long) -> Unit
) {
    ResonanceSongRow(
        title = song.title,
        artistLine = song.artistName + " - " + song.albumName,
        duration = formatDurationMs(song.durationMs),
        artwork = {
            ArtworkImage(
                artworkUri = song.artworkUri,
                contentDescription = song.albumName
            )
        },
        onClick = {
            val index = songs.indexOfFirst { it.id == song.id }
            if (index >= 0) viewModel.playFrom(songs, index)
            onSongClick(song.id)
        },
        state = songRowState(
            isCurrent = isCurrent,
            isSelected = false,
            isMissing = false,
            isLoading = false
        ),
        isPlayingAnimation = isCurrent,
        badge = {
            SongFormatBadge(song)
        }
    )
}


