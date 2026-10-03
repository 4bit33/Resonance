package com.resonance.player.feature.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import com.resonance.player.core.ui.components.GenreTile
import com.resonance.player.core.ui.components.pressClickable
import com.resonance.player.core.ui.theme.ResonanceMotion
import com.resonance.player.domain.library.CollectionRef
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
import com.resonance.player.core.ui.components.ArtworkImage
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
 * Library: everything that is not on Home. Quick buttons (shuffle all,
 * favorites, most played), then songs / albums / artists / genres / folders.
 * Albums, artists, genres and folders open their own page.
 */
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    initialTab: Int = 0,
    currentSongId: Long? = null,
    onSongClick: (Long) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenCollection: (CollectionRef) -> Unit,
    onOpenFavorites: () -> Unit
) {
    var tab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 4)) }
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val colors = ResonanceTheme.colors
    Column(Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 12.dp)
        ) {
            Text(
                stringResource(R.string.nav_library),
                style = ResonanceTheme.typography.displayLgMobile,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onOpenSearch) {
                Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.nav_search), tint = colors.textSecondary)
            }
        }
        ScanProgressBanner(scanState)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 12.dp)
        ) {
            item { QuickPill(Icons.Rounded.Shuffle, stringResource(R.string.home_shuffle_all), viewModel::shuffleAll) }
            item { QuickPill(Icons.Rounded.Favorite, stringResource(R.string.home_favorites), onOpenFavorites) }
            item {
                QuickPill(Icons.AutoMirrored.Rounded.TrendingUp, stringResource(R.string.home_most_played)) {
                    tab = 0
                    viewModel.setSort(SongSort.PLAY_COUNT)
                }
            }
        }
        CategoryChips(viewModel, tab, onSelect = { tab = it })
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                val motion = ResonanceMotion()
                fadeIn(motion.duration(180)) togetherWith fadeOut(motion.duration(90))
            },
            label = "library-tab",
            modifier = Modifier.fillMaxSize()
        ) { shown ->
            Column(Modifier.fillMaxSize()) {
                when (shown) {
                    0 -> SongsTab(viewModel, currentSongId, onSongClick)
                    1 -> AlbumsTab(viewModel, onOpenCollection)
                    2 -> ArtistsTab(viewModel, onOpenCollection)
                    3 -> GenresTab(viewModel, onOpenCollection)
                    else -> FoldersTab(viewModel, onOpenCollection)
                }
            }
        }
    }
}

@Composable
private fun QuickPill(icon: ImageVector, label: String, onClick: () -> Unit) {
    val colors = ResonanceTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(40.dp)
            .clip(CircleShape)
            .background(colors.surfaceContainer)
            .pressClickable(onClick = onClick)
            .padding(horizontal = 14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = ResonanceTheme.typography.labelLg, color = colors.textPrimary)
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
                            onLongClick = { overflowSong = song },
                            trailingInset = 20.dp
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
private fun AlbumsTab(viewModel: LibraryViewModel, onOpen: (CollectionRef) -> Unit) {
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    if (albums.isEmpty()) {
        EmptyLibraryView()
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(albums, key = { it.id }) { album ->
            Column(
                Modifier
                    .animateItem()
                    .pressClickable(onClickLabel = album.name) { onOpen(CollectionRef.Album(album.name, album.albumArtist)) }
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(18.dp))
                ) {
                    ArtworkImage(artworkUri = album.artUri, contentDescription = album.name, modifier = Modifier.fillMaxSize())
                }
                Spacer(Modifier.height(8.dp))
                Text(album.name, style = typography.labelLg, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(album.artistName, album.year?.toString()).joinToString(" · "),
                    style = typography.bodySm,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ArtistsTab(viewModel: LibraryViewModel, onOpen: (CollectionRef) -> Unit) {
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    if (artists.isEmpty()) {
        EmptyLibraryView()
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), modifier = Modifier.fillMaxSize()) {
        items(artists, key = { it.id }) { artist ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .animateItem()
                    .fillMaxWidth()
                    .pressClickable(pressedScale = 0.98f, onClickLabel = artist.name) { onOpen(CollectionRef.Artist(artist.name)) }
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Box(Modifier.size(56.dp).clip(CircleShape)) {
                    ArtworkImage(artworkUri = artist.artUri, contentDescription = null, modifier = Modifier.fillMaxSize())
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(artist.name, style = typography.titleMd, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        pluralStringResource(R.plurals.album_count, artist.albumCount, artist.albumCount) + " · " +
                            pluralStringResource(R.plurals.music_song_count, artist.songCount, artist.songCount),
                        style = typography.bodySm,
                        color = colors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun GenresTab(viewModel: LibraryViewModel, onOpen: (CollectionRef) -> Unit) {
    val genres by viewModel.genres.collectAsStateWithLifecycle()
    if (genres.isEmpty()) {
        EmptyLibraryView()
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(genres, key = { it.name }) { genre ->
            GenreTile(genre, 88.dp, Modifier.animateItem()) { onOpen(CollectionRef.Genre(genre.name)) }
        }
    }
}

@Composable
private fun FoldersTab(viewModel: LibraryViewModel, onOpen: (CollectionRef) -> Unit) {
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    if (folders.isEmpty()) {
        EmptyLibraryView()
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), modifier = Modifier.fillMaxSize()) {
        items(folders, key = { it.path }) { folder ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .animateItem()
                    .fillMaxWidth()
                    .pressClickable(pressedScale = 0.98f, onClickLabel = folder.name) {
                        onOpen(CollectionRef.Folder(folder.path, folder.name))
                    }
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceContainer)
                ) {
                    Icon(Icons.Rounded.Folder, contentDescription = null, tint = colors.accent)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(folder.name, style = typography.titleMd, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (folder.path.isNotEmpty()) {
                        Text(folder.path, style = typography.bodySm, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(folder.songCount.toString(), style = typography.monoMetric, color = colors.textSecondary)
            }
        }
    }
}
