package com.resonance.player.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonance.player.R
import com.resonance.player.core.common.formatDurationMs
import com.resonance.player.core.model.Song
import com.resonance.player.domain.library.SongSort
import com.resonance.player.core.permissions.AudioPermissionManager
import com.resonance.player.core.ui.components.ArtworkImage
import com.resonance.player.core.ui.components.AudioPermissionGate
import com.resonance.player.core.ui.components.EmptyLibraryView
import com.resonance.player.core.ui.components.ErrorView
import com.resonance.player.core.ui.components.LoadingView
import com.resonance.player.core.ui.components.ResonanceChip
import com.resonance.player.core.ui.components.ResonanceSongRow
import com.resonance.player.core.ui.components.ResonanceTopBar
import com.resonance.player.core.ui.components.ScanProgressBanner
import com.resonance.player.core.ui.components.SongFormatBadge
import com.resonance.player.core.ui.components.SongOverflowSheet
import com.resonance.player.core.ui.components.PlaylistPickerSheet
import com.resonance.player.core.ui.components.songRowState
import com.resonance.player.core.ui.theme.ResonanceTheme
import kotlinx.coroutines.launch

/**
 * Stitch Library: top bar with global search, category chips with counts,
 * sort toolbar + shuffle-all, Stitch song rows with playing state/badges/
 * overflow, alphabet scrubber with HUD. Missing-file rows are intentionally
 * absent (the scanner auto-prunes vanished files; playback-time absence
 * surfaces via the player error state).
 */
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    permissionManager: AudioPermissionManager,
    initialTab: Int = 0,
    currentSongId: Long? = null,
    onSongClick: (Long) -> Unit,
    onOpenQueue: () -> Unit,
    onOpenSearch: () -> Unit
) {
    AudioPermissionGate(
        manager = permissionManager,
        onPermissionGranted = viewModel::rescan
    ) {
        LibraryTabs(viewModel, initialTab, currentSongId, onSongClick, onOpenQueue, onOpenSearch)
    }
}

@Composable
private fun LibraryTabs(
    viewModel: LibraryViewModel,
    initialTab: Int,
    currentSongId: Long?,
    onSongClick: (Long) -> Unit,
    onOpenQueue: () -> Unit,
    onOpenSearch: () -> Unit
) {
    var tab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 4)) }
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        ResonanceTopBar(
            title = stringResource(R.string.nav_library),
            searchIcon = Icons.Filled.Search,
            onSearch = onOpenSearch,
            searchDescription = stringResource(R.string.nav_search)
        )
        ScanProgressBanner(scanState)
        CategoryChips(viewModel, tab, onSelect = { tab = it })
        when (tab) {
            0 -> SongsTab(viewModel, currentSongId, onSongClick)
            1 -> AlbumsTab(viewModel, onOpenQueue)
            2 -> ArtistsTab(viewModel, onOpenQueue)
            3 -> GenresTab(viewModel, onOpenQueue)
            4 -> FoldersTab(viewModel, onOpenQueue)
        }
    }
}

@Composable
private fun CategoryChips(
    viewModel: LibraryViewModel,
    selectedTab: Int,
    onSelect: (Int) -> Unit
) {
    val spacing = ResonanceTheme.spacing
    val songs by viewModel.uiState.collectAsStateWithLifecycle()
    val songCount = (songs as? LibraryUiState.Content)?.songs?.size ?: 0
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val genres by viewModel.genres.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        item { Spacer(Modifier.width(spacing.lg - spacing.sm)) }
        item {
            ResonanceChip(
                label = stringResource(R.string.tab_songs),
                count = songCount.toString(),
                selected = selectedTab == 0,
                onClick = { onSelect(0) }
            )
        }
        item {
            ResonanceChip(
                label = stringResource(R.string.tab_albums),
                count = albums.size.toString(),
                selected = selectedTab == 1,
                onClick = { onSelect(1) }
            )
        }
        item {
            ResonanceChip(
                label = stringResource(R.string.tab_artists),
                count = artists.size.toString(),
                selected = selectedTab == 2,
                onClick = { onSelect(2) }
            )
        }
        item {
            ResonanceChip(
                label = stringResource(R.string.tab_genres),
                count = genres.size.toString(),
                selected = selectedTab == 3,
                onClick = { onSelect(3) }
            )
        }
        item {
            ResonanceChip(
                label = stringResource(R.string.tab_folders),
                count = folders.size.toString(),
                selected = selectedTab == 4,
                onClick = { onSelect(4) }
            )
        }
        item { Spacer(Modifier.width(spacing.lg - spacing.sm)) }
    }
}

@Composable
private fun sortLabel(sort: SongSort): String = when (sort) {
    SongSort.TITLE -> stringResource(R.string.sort_title)
    SongSort.ARTIST -> stringResource(R.string.sort_artist)
    SongSort.ALBUM -> stringResource(R.string.sort_album)
    SongSort.DATE_ADDED -> stringResource(R.string.sort_recently_added)
    SongSort.LAST_PLAYED -> stringResource(R.string.sort_recently_played)
    SongSort.PLAY_COUNT -> stringResource(R.string.sort_most_played)
}

@Composable
private fun SortToolbar(viewModel: LibraryViewModel) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val spacing = ResonanceTheme.spacing
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg, vertical = spacing.xs)
    ) {
        androidx.compose.material3.TextButton(onClick = { expanded = true }) {
            Icon(
                Icons.Filled.SwapVert,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(spacing.xs))
            Text(
                stringResource(R.string.sort_label) + ": " + sortLabel(sort),
                style = typography.labelMd,
                color = colors.textPrimary
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SongSort.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(sortLabel(option), style = typography.bodyMd, color = colors.textPrimary)
                    },
                    onClick = {
                        viewModel.setSort(option)
                        expanded = false
                    }
                )
            }
        }
        Spacer(Modifier.weight(1f))
        IconButton(
            onClick = viewModel::shuffleAll,
            modifier = Modifier.size(spacing.touchMin)
        ) {
            Icon(
                Icons.Filled.Shuffle,
                contentDescription = stringResource(R.string.cd_shuffle),
                tint = colors.textSecondary
            )
        }
    }
}

/** First-letter index (label text plus first position). Pure and unit-tested. */
internal fun alphabetIndex(titles: List<String>): List<Pair<String, Int>> {
    val result = ArrayList<Pair<String, Int>>()
    var lastLabel: String? = null
    titles.forEachIndexed { index, title ->
        val first = title.trim().firstOrNull()?.uppercaseChar()
        val label = if (first != null && first.isLetter()) first.toString() else "#"
        if (result.isEmpty() || label != lastLabel) {
            result.add(label to index)
            lastLabel = label
        }
    }
    return result
}

@Composable
private fun SongsTab(
    viewModel: LibraryViewModel,
    currentSongId: Long?,
    onSongClick: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (val s = state) {
        LibraryUiState.Loading -> LoadingView()
        LibraryUiState.Empty -> EmptyLibraryView()
        is LibraryUiState.Error -> ErrorView(message = s.message)
        is LibraryUiState.Content -> {
            SortToolbar(viewModel)
            var overflowSong by remember { mutableStateOf<Song?>(null) }
            val listState = rememberLazyListState()
            val scope = rememberCoroutineScope()
            val index = remember(s.songs) { alphabetIndex(s.songs.map { it.title }) }
            var hudLetter by remember { mutableStateOf<String?>(null) }
            Box(Modifier.fillMaxSize()) {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(s.songs, key = { it.id }) { song ->
                        val isCurrent = song.id == currentSongId
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
                                val at = s.songs.indexOfFirst { it.id == song.id }
                                if (at >= 0) viewModel.playFrom(s.songs, at)
                                onSongClick(song.id)
                            },
                            modifier = Modifier.animateItem(),
                            state = songRowState(
                                isCurrent = isCurrent,
                                isSelected = false,
                                isMissing = false,
                                isLoading = false
                            ),
                            isPlayingAnimation = isCurrent,
                            badge = { SongFormatBadge(song) },
                            onOverflowClick = { overflowSong = song }
                        )
                    }
                }
                AlphabetScrubber(
                    index = index,
                    hudLetter = hudLetter,
                    onScrub = { letter, position ->
                        val target = index.firstOrNull { it.first == letter }?.second
                        if (target != null) {
                            hudLetter = letter
                            scope.launch { listState.scrollToItem(target) }
                        }
                    },
                    onScrubEnd = { hudLetter = null },
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
            overflowSong?.let { song ->
                var showPicker by remember { mutableStateOf(false) }
                val playlists by viewModel.playlists.collectAsStateWithLifecycle()
                val playlistError by viewModel.playlistError.collectAsStateWithLifecycle()
                if (showPicker) {
                    PlaylistPickerSheet(
                        songTitle = song.title,
                        playlists = playlists,
                        error = playlistError,
                        onPick = { playlistId ->
                            viewModel.addToPlaylist(playlistId, song.id) {
                                showPicker = false
                                overflowSong = null
                            }
                        },
                        onNewPlaylist = { name ->
                            viewModel.createPlaylistAndAdd(name, song.id) {
                                showPicker = false
                                overflowSong = null
                            }
                        },
                        onDismiss = {
                            showPicker = false
                            viewModel.clearPlaylistError()
                        }
                    )
                } else {
                    SongOverflowSheet(
                        song = song,
                        onDismiss = { overflowSong = null },
                        onPlayNext = {
                            viewModel.playNext(song)
                            overflowSong = null
                        },
                        onAddToQueue = {
                            viewModel.addToQueue(song)
                            overflowSong = null
                        },
                        onAddToPlaylist = { showPicker = true }
                    )
                }
            }
        }
    }
}

@Composable
private fun AlphabetScrubber(
    index: List<Pair<String, Int>>,
    hudLetter: String?,
    onScrub: (String, Int) -> Unit,
    onScrubEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    if (index.isEmpty()) return
    Box(modifier = modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(32.dp)
                .pointerInput(index) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            pickLetter(index, offset.y, size.height)?.let { (letter, position) ->
                                onScrub(letter, position)
                            }
                        },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            pickLetter(index, change.position.y, size.height)?.let { (letter, position) ->
                                onScrub(letter, position)
                            }
                        },
                        onDragEnd = onScrubEnd,
                        onDragCancel = onScrubEnd
                    )
                }
                .padding(vertical = 32.dp)
        ) {
            val letters = index.map { it.first }
            val step = maxOf(1, letters.size / 18)
            letters.filterIndexed { i, _ -> i % step == 0 }.forEach { letter ->
                Text(
                    letter,
                    style = typography.labelSm,
                    color = if (letter == hudLetter) colors.accent else colors.textMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (hudLetter != null) {
            Surface(
                shape = ResonanceTheme.radii.card,
                color = colors.surfaceHighest,
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text(
                    hudLetter,
                    style = typography.headlineMd,
                    color = colors.accent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(
                        horizontal = ResonanceTheme.spacing.xl,
                        vertical = ResonanceTheme.spacing.lg
                    )
                )
            }
        }
    }
}

private fun pickLetter(
    index: List<Pair<String, Int>>,
    y: Float,
    height: Int
): Pair<String, Int>? {
    if (index.isEmpty() || height <= 0) return null
    val position = ((y / height.toFloat()) * index.size).toInt().coerceIn(0, index.size - 1)
    return index[position]
}

@Composable
private fun AlbumsTab(viewModel: LibraryViewModel, onOpenQueue: () -> Unit) {
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val spacing = ResonanceTheme.spacing
    if (albums.isEmpty()) {
        EmptyLibraryView()
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(albums, key = { it.id }) { album ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .animateItem()
                    .fillMaxWidth()
                    .height(ResonanceTheme.dimensions.songRowHeight)
                    .padding(horizontal = spacing.lg)
                    .clickable {
                        viewModel.playAlbum(album.name, album.albumArtist)
                        onOpenQueue()
                    }
            ) {
                ArtworkImage(
                    artworkUri = album.artUri,
                    contentDescription = album.name,
                    modifier = Modifier.size(ResonanceTheme.dimensions.songArtwork)
                )
                Spacer(Modifier.width(spacing.md))
                Column(Modifier.weight(1f)) {
                    Text(
                        album.name,
                        style = typography.titleMd,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        album.artistName + " - " + album.songCount + " " +
                            stringResource(R.string.settings_songs),
                        style = typography.bodySm,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
            HorizontalDivider(color = colors.outlineSubtle)
        }
    }
}

@Composable
private fun ArtistsTab(viewModel: LibraryViewModel, onOpenQueue: () -> Unit) {
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val spacing = ResonanceTheme.spacing
    if (artists.isEmpty()) {
        EmptyLibraryView()
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(artists, key = { it.id }) { artist ->
            Column(
                modifier = Modifier
                    .animateItem()
                    .fillMaxWidth()
                    .clickable {
                        viewModel.playArtist(artist.name)
                        onOpenQueue()
                    }
                    .padding(horizontal = spacing.lg, vertical = spacing.md)
            ) {
                Text(
                    artist.name,
                    style = typography.titleMd,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    artist.songCount.toString() + " " + stringResource(R.string.settings_songs) +
                        " - " + artist.albumCount.toString() + " " +
                        stringResource(R.string.settings_albums),
                    style = typography.bodySm,
                    color = colors.textSecondary
                )
            }
            HorizontalDivider(color = colors.outlineSubtle)
        }
    }
}

@Composable
private fun GenresTab(viewModel: LibraryViewModel, onOpenQueue: () -> Unit) {
    val genres by viewModel.genres.collectAsStateWithLifecycle()
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val spacing = ResonanceTheme.spacing
    if (genres.isEmpty()) {
        EmptyLibraryView()
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(genres, key = { it.name }) { genre ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .animateItem()
                    .fillMaxWidth()
                    .clickable {
                        viewModel.playGenre(genre.name)
                        onOpenQueue()
                    }
                    .padding(horizontal = spacing.lg, vertical = spacing.md)
            ) {
                Text(
                    genre.name,
                    style = typography.titleMd,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    genre.songCount.toString(),
                    style = typography.monoMetric,
                    color = colors.textSecondary
                )
            }
            HorizontalDivider(color = colors.outlineSubtle)
        }
    }
}

@Composable
private fun FoldersTab(viewModel: LibraryViewModel, onOpenQueue: () -> Unit) {
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val spacing = ResonanceTheme.spacing
    if (folders.isEmpty()) {
        EmptyLibraryView()
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(folders, key = { it.path }) { folder ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .animateItem()
                    .fillMaxWidth()
                    .clickable {
                        viewModel.playFolder(folder.path)
                        onOpenQueue()
                    }
                    .padding(horizontal = spacing.lg, vertical = spacing.md)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        folder.name,
                        style = typography.titleMd,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    if (folder.path.isNotEmpty()) {
                        Text(
                            folder.path,
                            style = typography.bodySm,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.width(spacing.sm))
                Text(
                    folder.songCount.toString(),
                    style = typography.monoMetric,
                    color = colors.textSecondary
                )
            }
            HorizontalDivider(color = colors.outlineSubtle)
        }
    }
}









