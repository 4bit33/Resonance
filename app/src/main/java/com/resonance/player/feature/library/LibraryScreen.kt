package com.resonance.player.feature.library

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.drawBehind
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.HorizontalPager
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
 * Library: everything that is not on Home. Three tiles (shuffle all,
 * favorites, most played), then swipeable categories with a sliding
 * indicator: songs, albums, artists, genres, folders. Albums, artists,
 * genres and folders open their own page.
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
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val colors = ResonanceTheme.colors
    val pager = rememberPagerState(initialPage = initialTab.coerceIn(0, 4)) { 5 }
    val scope = rememberCoroutineScope()
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp)
        ) {
            QuickTile(Icons.Rounded.Shuffle, stringResource(R.string.tile_shuffle), Modifier.weight(1f), viewModel::shuffleAll)
            QuickTile(Icons.Rounded.Favorite, stringResource(R.string.home_favorites), Modifier.weight(1f), onOpenFavorites)
            QuickTile(Icons.AutoMirrored.Rounded.TrendingUp, stringResource(R.string.tile_most_played), Modifier.weight(1f)) {
                viewModel.setSort(SongSort.PLAY_COUNT)
                scope.launch { pager.animateScrollToPage(0) }
            }
        }
        CategoryTabs(pager) { page -> scope.launch { pager.animateScrollToPage(page) } }
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
            Column(Modifier.fillMaxSize()) {
                when (page) {
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
private fun QuickTile(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    val colors = ResonanceTheme.colors
    Column(
        modifier = modifier
            .height(76.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surfaceContainer)
            .pressClickable(onClickLabel = label, onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(colors.accent.copy(alpha = 0.16f))
        ) {
            Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
        }
        Text(label, style = ResonanceTheme.typography.labelMd, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Material 3 scrollable tabs, kept in step with the swipeable pager. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryTabs(pager: PagerState, onSelect: (Int) -> Unit) {
    val labels = listOf(
        stringResource(R.string.tab_songs),
        stringResource(R.string.tab_albums),
        stringResource(R.string.tab_artists),
        stringResource(R.string.tab_genres),
        stringResource(R.string.tab_folders)
    )
    PrimaryScrollableTabRow(
        selectedTabIndex = pager.currentPage,
        edgePadding = 12.dp,
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        divider = {},
        modifier = Modifier.padding(top = 8.dp)
    ) {
        labels.forEachIndexed { index, label ->
            Tab(
                selected = pager.currentPage == index,
                onClick = { onSelect(index) },
                text = { Text(label, maxLines = 1) },
                unselectedContentColor = ResonanceTheme.colors.textSecondary
            )
        }
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

/** "N songs" on the left; sort (a small menu) and shuffle on the right. */
@Composable
private fun SongsHeader(count: Int, viewModel: LibraryViewModel) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 4.dp, bottom = 4.dp)
    ) {
        Text(
            pluralStringResource(R.plurals.music_song_count, count, count),
            style = typography.bodyMd,
            color = colors.textSecondary,
            modifier = Modifier.weight(1f)
        )
        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(36.dp)
                    .clip(CircleShape)
                    .pressClickable { expanded = true }
                    .padding(horizontal = 12.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(sortLabel(sort), style = typography.labelLg, color = colors.textPrimary)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                SongSort.entries.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                sortLabel(option),
                                style = typography.bodyMd,
                                color = if (option == sort) colors.accent else colors.textPrimary
                            )
                        },
                        onClick = {
                            viewModel.setSort(option)
                            expanded = false
                        }
                    )
                }
            }
        }
        IconButton(onClick = viewModel::shuffleAll) {
            Icon(Icons.Rounded.Shuffle, contentDescription = stringResource(R.string.cd_shuffle), tint = colors.textPrimary)
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
            var overflowSong by remember { mutableStateOf<Song?>(null) }
            val listState = rememberLazyListState()
            Box(Modifier.fillMaxSize()) {
                LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 24.dp), modifier = Modifier.fillMaxSize()) {
                    item(key = "header") { SongsHeader(s.songs.size, viewModel) }
                    items(s.songs, key = { it.id }) { song ->
                        val isCurrent = song.id == currentSongId
                        ResonanceSongRow(
                            title = song.title,
                            artistLine = song.artistName + " - " + song.albumName,
                            duration = formatDurationMs(song.durationMs),
                            artwork = {
                                ArtworkImage(artworkUri = song.artworkUri, contentDescription = song.albumName)
                            },
                            onClick = {
                                val at = s.songs.indexOfFirst { it.id == song.id }
                                if (at >= 0) viewModel.playFrom(s.songs, at)
                                onSongClick(song.id)
                            },
                            modifier = Modifier.animateItem(),
                            state = songRowState(isCurrent = isCurrent, isSelected = false, isMissing = false, isLoading = false),
                            isPlayingAnimation = isCurrent,
                            onLongClick = { overflowSong = song },
                            trailingInset = 12.dp
                        )
                    }
                }
                FastScroller(
                    listState = listState,
                    labels = remember(s.songs) { s.songs.map { it.title } },
                    headerItems = 1,
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

/**
 * Fast scroll for long lists: a slim thumb at the right edge that shows up
 * while the list moves; dragging it jumps through the list and shows the
 * first letter of where you are in a bubble.
 */
@Composable
private fun FastScroller(
    listState: androidx.compose.foundation.lazy.LazyListState,
    labels: List<String>,
    headerItems: Int,
    modifier: Modifier = Modifier
) {
    if (labels.size < 30) return
    val colors = ResonanceTheme.colors
    val motion = ResonanceTheme.motion
    val scope = rememberCoroutineScope()
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var trackHeight by remember { mutableFloatStateOf(1f) }
    val visible = dragging || listState.isScrollInProgress
    val alpha by animateFloatAsState(if (visible) 1f else 0f, motion.duration(if (visible) 150 else 600), label = "scroller-alpha")
    val fraction = if (dragging) {
        dragFraction
    } else {
        val total = (listState.layoutInfo.totalItemsCount - headerItems).coerceAtLeast(1)
        ((listState.firstVisibleItemIndex - headerItems).coerceAtLeast(0).toFloat() / total).coerceIn(0f, 1f)
    }
    val letter = labels.getOrNull((fraction * (labels.size - 1)).toInt())
        ?.trim()?.firstOrNull()?.uppercaseChar()?.let { if (it.isLetter()) it.toString() else "#" }
    Box(
        modifier
            .fillMaxHeight()
            .width(40.dp)
            .padding(vertical = 12.dp)
            .onSizeChanged { trackHeight = it.height.toFloat().coerceAtLeast(1f) }
            .graphicsLayer { this.alpha = alpha }
            .pointerInput(labels) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        dragging = true
                        dragFraction = (offset.y / trackHeight).coerceIn(0f, 1f)
                    },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false },
                    onVerticalDrag = { change, _ ->
                        change.consume()
                        dragFraction = (change.position.y / trackHeight).coerceIn(0f, 1f)
                        val target = headerItems + (dragFraction * (labels.size - 1)).toInt()
                        scope.launch { listState.scrollToItem(target) }
                    }
                )
            }
    ) {
        val thumbHeight = 44.dp
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(end = 6.dp)
                .offset { IntOffset(0, ((trackHeight - thumbHeight.toPx()) * fraction).toInt()) }
                .size(width = if (dragging) 6.dp else 4.dp, height = thumbHeight)
                .clip(CircleShape)
                .background(if (dragging) colors.accent else colors.textSecondary)
        )
        if (dragging && letter != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset { IntOffset(-56.dp.roundToPx(), ((trackHeight - 64.dp.toPx()) * fraction).toInt()) }
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(colors.accent)
            ) {
                Text(letter, style = ResonanceTheme.typography.headlineLg, color = colors.onAccent)
            }
        }
    }
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
