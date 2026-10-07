package com.resonance.player.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonance.player.R
import com.resonance.player.core.media.ScanState
import com.resonance.player.core.model.Album
import com.resonance.player.core.model.Genre
import com.resonance.player.core.model.PlaybackSnapshot
import com.resonance.player.core.model.Playlist
import com.resonance.player.core.model.Song
import com.resonance.player.core.ui.components.ArtworkImage
import com.resonance.player.core.ui.components.EmptyLibraryView
import com.resonance.player.core.ui.components.GenreTile
import com.resonance.player.core.ui.components.LocalMusicActions
import com.resonance.player.core.ui.components.ScanProgressBanner
import com.resonance.player.core.ui.components.pressClickable
import com.resonance.player.core.ui.theme.ResonanceTheme
import com.resonance.player.core.ui.theme.rememberArtworkPalette
import com.resonance.player.domain.library.CollectionRef
import com.resonance.player.domain.library.ListeningStats
import com.resonance.player.domain.playback.PlaybackSource
import com.resonance.player.domain.settings.HomeSection
import java.util.Calendar

/** Card sizes for the user's "compact" / "large" choice. */
private class HomeSizes(compact: Boolean) {
    val album: Dp = if (compact) 108.dp else 144.dp
    val added: Dp = if (compact) 88.dp else 116.dp
    val continueArt: Dp = if (compact) 60.dp else 88.dp
    val tile: Dp = if (compact) 72.dp else 92.dp
}

private val Gutter = 20.dp

/**
 * Home: the blocks the user picked, in their order (Settings > Home screen,
 * or "Customize home" at the bottom). Blocks with nothing to show stay out
 * of the way instead of showing empty frames.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    snapshot: PlaybackSnapshot,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPlayer: (Long) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onOpenPlaylists: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenLibrary: (Int) -> Unit,
    onOpenCollection: (CollectionRef) -> Unit,
    onOpenImport: () -> Unit,
    onCustomize: () -> Unit
) {
    val layout by viewModel.layout.collectAsStateWithLifecycle()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val storage by viewModel.storage.collectAsStateWithLifecycle()
    // storage is null until Room's first emission: not "empty" yet, so no CTA flash on cold start.
    val isEmpty = storage != null && storage?.trackCount == 0 && scanState !is ScanState.Scanning
    val sections = layout.visible
    val sizes = remember(layout.compact) { HomeSizes(layout.compact) }

    Column(Modifier.fillMaxSize()) {
        ScanProgressBanner(scanState)
        if (isEmpty) {
            HomeHeader(showGreeting = HomeSection.GREETING in sections, onOpenSettings = onOpenSettings)
            EmptyLibraryView()
            return@Column
        }
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(key = "header") {
                HomeHeader(showGreeting = HomeSection.GREETING in sections, onOpenSettings = onOpenSettings)
            }
            sections.filter { it != HomeSection.GREETING }.forEach { section ->
                item(key = section.name) {
                    when (section) {
                        HomeSection.SEARCH -> SearchEntry(onOpenSearch)
                        HomeSection.CONTINUE -> ContinueCard(viewModel, snapshot, sizes, onOpenPlayer)
                        HomeSection.RECENTLY_PLAYED -> {
                            val albums by viewModel.recentAlbums.collectAsStateWithLifecycle()
                            AlbumRow(
                                title = stringResource(R.string.home_recently_played),
                                albums = albums,
                                size = sizes.album,
                                onAll = { onOpenLibrary(1) },
                                onPlay = { onOpenCollection(CollectionRef.Album(it.name, it.albumArtist)) }
                            )
                        }
                        HomeSection.RECENTLY_ADDED -> {
                            val albums by viewModel.recentlyAddedAlbums.collectAsStateWithLifecycle()
                            AlbumRow(
                                title = stringResource(R.string.home_recently_added),
                                albums = albums,
                                size = sizes.added,
                                onAll = null,
                                onPlay = { onOpenCollection(CollectionRef.Album(it.name, it.albumArtist)) }
                            )
                        }
                        HomeSection.GENRES -> {
                            val genres by viewModel.genres.collectAsStateWithLifecycle()
                            GenreGrid(genres, sizes.tile, onAll = { onOpenLibrary(3) }, onPlay = { onOpenCollection(CollectionRef.Genre(it)) })
                        }
                        HomeSection.PLAYLISTS -> {
                            val playlists by viewModel.playlists.collectAsStateWithLifecycle()
                            PlaylistGrid(playlists, onOpen = onOpenPlaylist, onAll = onOpenPlaylists)
                        }
                        HomeSection.STATS -> {
                            val stats by viewModel.stats.collectAsStateWithLifecycle()
                            StatsCard(stats)
                        }
                        HomeSection.IMPORT -> ImportCard(onOpenImport)
                        HomeSection.QUICK_ACTIONS -> QuickActions(onShuffle = viewModel::shuffleAll, onFavorites = onOpenFavorites)
                        HomeSection.MOST_PLAYED -> {
                            val songs by viewModel.mostPlayed.collectAsStateWithLifecycle()
                            MostPlayed(songs) { index ->
                                viewModel.playFrom(songs, index, PlaybackSource(PlaybackSource.Kind.MOST_PLAYED))
                            }
                        }
                        HomeSection.GREETING -> Unit
                    }
                }
            }
            item(key = "customize") {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TextButton(onClick = onCustomize) {
                        Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.home_customize))
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(showGreeting: Boolean, onOpenSettings: () -> Unit) {
    val colors = ResonanceTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Gutter, end = 8.dp, top = 12.dp)
    ) {
        if (showGreeting) {
            Text(
                greeting(),
                style = ResonanceTheme.typography.displayLgMobile,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.nav_settings), tint = colors.textSecondary)
        }
    }
}

@Composable
private fun greeting(): String {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    return stringResource(
        when (hour) {
            in 5..11 -> R.string.home_greeting_morning
            in 12..16 -> R.string.home_greeting_afternoon
            in 17..22 -> R.string.home_greeting_evening
            else -> R.string.home_greeting_night
        }
    )
}

@Composable
private fun SearchEntry(onOpenSearch: () -> Unit) {
    val colors = ResonanceTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = Gutter)
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceContainer)
            .pressClickable(pressedScale = 0.98f, onClickLabel = stringResource(R.string.nav_search), onClick = onOpenSearch)
            .padding(horizontal = 16.dp)
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(stringResource(R.string.home_search_hint), style = ResonanceTheme.typography.bodyLg, color = colors.textSecondary)
    }
}

/**
 * The song in the player, or the last one heard when nothing is loaded.
 * Tinted with its artwork; play resumes right here without opening anything.
 */
@Composable
private fun ContinueCard(
    viewModel: HomeViewModel,
    snapshot: PlaybackSnapshot,
    sizes: HomeSizes,
    onOpenPlayer: (Long) -> Unit
) {
    val lastPlayed by viewModel.lastPlayed.collectAsStateWithLifecycle()
    val loaded = snapshot.song
    val song = loaded ?: lastPlayed ?: return
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val palette = rememberArtworkPalette(song.artworkUri, ResonanceTheme.look.artworkColors)
    val playing = loaded != null && snapshot.isPlaying
    fun start() {
        if (loaded != null) viewModel.togglePlayPause() else viewModel.playFrom(listOf(song), 0)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = Gutter)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(palette.surface)
            .drawBehind {
                drawCircle(
                    Brush.radialGradient(
                        listOf(palette.glow.copy(alpha = 0.45f), Color.Transparent),
                        center = Offset(size.height * 0.4f, size.height * 0.3f),
                        radius = size.height * 1.6f
                    ),
                    radius = size.height * 1.6f,
                    center = Offset(size.height * 0.4f, size.height * 0.3f)
                )
            }
            .pressClickable(pressedScale = 0.98f) {
                if (loaded == null) viewModel.playFrom(listOf(song), 0)
                onOpenPlayer(song.id)
            }
            .padding(12.dp)
    ) {
        Box(Modifier.size(sizes.continueArt).clip(RoundedCornerShape(14.dp))) {
            ArtworkImage(artworkUri = song.artworkUri, contentDescription = song.albumName, modifier = Modifier.fillMaxSize())
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.home_continue).uppercase(),
                style = typography.labelSm,
                color = palette.accent
            )
            Text(song.title, style = typography.titleMd, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artistName, style = typography.bodySm, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (loaded != null && snapshot.durationMs > 0L) {
                val fraction = (snapshot.positionMs.toFloat() / snapshot.durationMs).coerceIn(0f, 1f)
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(colors.textPrimary.copy(alpha = 0.14f))
                ) {
                    Box(Modifier.fillMaxWidth(fraction).height(3.dp).background(palette.accent))
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(palette.accent)
                .pressClickable(
                    pressedScale = 0.88f,
                    onClickLabel = stringResource(if (playing) R.string.cd_pause else R.string.cd_play),
                    onClick = ::start
                )
        ) {
            Icon(
                if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = stringResource(if (playing) R.string.cd_pause else R.string.cd_play),
                tint = colors.onAccent
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String, onAll: (() -> Unit)? = null) {
    val colors = ResonanceTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Gutter, end = 8.dp, bottom = 10.dp)
            .heightIn(min = 36.dp)
    ) {
        Text(title, style = ResonanceTheme.typography.headlineMd, color = colors.textPrimary, modifier = Modifier.weight(1f))
        if (onAll != null) {
            TextButton(onClick = onAll) {
                Text(stringResource(R.string.home_view_all), color = colors.accent)
            }
        }
    }
}

@Composable
private fun AlbumRow(
    title: String,
    albums: List<Album>,
    size: Dp,
    onAll: (() -> Unit)?,
    onPlay: (Album) -> Unit
) {
    if (albums.isEmpty()) return
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    Column {
        SectionTitle(title, onAll)
        LazyRow(
            contentPadding = PaddingValues(horizontal = Gutter),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(albums, key = { it.id }) { album ->
                Column(
                    Modifier
                        .width(size)
                        .pressClickable(onClickLabel = album.name) { onPlay(album) }
                ) {
                    Box(Modifier.size(size).clip(RoundedCornerShape(16.dp))) {
                        ArtworkImage(artworkUri = album.artUri, contentDescription = album.name, modifier = Modifier.fillMaxSize())
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(album.name, style = typography.labelLg, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(album.artistName, style = typography.bodySm, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** Genre tiles; a tap opens the genre page. */
@Composable
private fun GenreGrid(genres: List<Genre>, tileHeight: Dp, onAll: () -> Unit, onPlay: (String) -> Unit) {
    if (genres.isEmpty()) return
    Column {
        SectionTitle(stringResource(R.string.home_genres), onAll)
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = Gutter)
        ) {
            genres.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { genre -> GenreTile(genre, tileHeight, Modifier.weight(1f)) { onPlay(genre.name) } }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PlaylistGrid(playlists: List<Playlist>, onOpen: (Long) -> Unit, onAll: () -> Unit) {
    if (playlists.isEmpty()) return
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    Column {
        SectionTitle(stringResource(R.string.nav_playlists), onAll)
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = Gutter)
        ) {
            playlists.take(4).chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { playlist ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.surfaceContainer)
                                .pressClickable(onClickLabel = playlist.name) { onOpen(playlist.id) }
                        ) {
                            PlaylistCover(playlist.coverUri, Modifier.size(56.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                playlist.name,
                                style = typography.labelLg,
                                color = colors.textPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** A playlist's own cover, or a quiet accent tile with the playlist icon. */
@Composable
fun PlaylistCover(coverUri: String?, modifier: Modifier = Modifier) {
    val colors = ResonanceTheme.colors
    if (coverUri != null) {
        ArtworkImage(artworkUri = coverUri, contentDescription = null, modifier = modifier)
    } else {
        Box(contentAlignment = Alignment.Center, modifier = modifier.background(colors.accentDim)) {
            Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null, tint = colors.accent)
        }
    }
}

@Composable
private fun StatsCard(stats: ListeningStats?) {
    stats ?: return
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    Column {
        SectionTitle(stringResource(R.string.home_stats))
        Box(
            Modifier
                .padding(horizontal = Gutter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.surfaceContainer)
                .padding(16.dp)
        ) {
            if (stats.plays == 0) {
                Text(stringResource(R.string.home_stats_empty), style = typography.bodyMd, color = colors.textSecondary)
            } else {
                val minutes = (stats.listenedMs / 60_000L).toInt()
                val listened = if (minutes >= 60) {
                    stringResource(R.string.home_stats_hours, minutes / 60, minutes % 60)
                } else {
                    stringResource(R.string.home_stats_minutes, minutes)
                }
                val top = stats.topGenre ?: stats.topArtist
                val topLabel = if (stats.topGenre != null) R.string.home_stats_top_genre else R.string.home_stats_top_artist
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCell(listened, stringResource(R.string.home_stats_listened), colors.textPrimary, Modifier.weight(1f))
                    StatCell(stats.plays.toString(), stringResource(R.string.home_stats_plays), colors.textPrimary, Modifier.weight(1f))
                    if (top != null) StatCell(top, stringResource(topLabel), colors.accent, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatCell(value: String, label: String, valueColor: Color, modifier: Modifier) {
    Column(modifier) {
        Text(value, style = ResonanceTheme.typography.titleMd, color = valueColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, style = ResonanceTheme.typography.bodySm, color = ResonanceTheme.colors.textSecondary)
    }
}

/** Adding music: download from YouTube (Music) and other sites, or add a folder / files from the phone. */
@Composable
private fun ImportCard(onOpenImport: () -> Unit) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val actions = LocalMusicActions.current
    Column(
        Modifier
            .padding(horizontal = Gutter)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surfaceContainer)
            .padding(16.dp)
    ) {
        Text(stringResource(R.string.home_import_title), style = typography.titleMd, color = colors.textPrimary)
        Text(stringResource(R.string.home_import_body), style = typography.bodySm, color = colors.textSecondary)
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Pill(Icons.Rounded.Download, stringResource(R.string.home_import_youtube), onOpenImport) }
            item { Pill(Icons.Rounded.CreateNewFolder, stringResource(R.string.music_add_folder)) { actions.addFolder(null) } }
            item { Pill(Icons.Rounded.LibraryAdd, stringResource(R.string.music_add_songs)) { actions.addSongs() } }
        }
    }
}

@Composable
private fun QuickActions(onShuffle: () -> Unit, onFavorites: () -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Gutter),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Pill(Icons.Rounded.Shuffle, stringResource(R.string.home_shuffle_all), onShuffle) }
        item { Pill(Icons.Rounded.Favorite, stringResource(R.string.home_favorites), onFavorites) }
    }
}

@Composable
private fun Pill(icon: ImageVector, label: String, onClick: () -> Unit) {
    val colors = ResonanceTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(40.dp)
            .clip(CircleShape)
            .background(colors.textPrimary.copy(alpha = 0.07f))
            .pressClickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = ResonanceTheme.typography.labelLg, color = colors.textPrimary)
    }
}

@Composable
private fun MostPlayed(songs: List<Song>, onPlay: (Int) -> Unit) {
    if (songs.isEmpty()) return
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    Column {
        SectionTitle(stringResource(R.string.home_most_played))
        songs.forEachIndexed { index, song ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .pressClickable(pressedScale = 0.98f, onClickLabel = song.title) { onPlay(index) }
                    .padding(horizontal = Gutter, vertical = 6.dp)
            ) {
                Text(
                    (index + 1).toString(),
                    style = typography.titleMd,
                    color = colors.accent,
                    modifier = Modifier.width(24.dp)
                )
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))) {
                    ArtworkImage(artworkUri = song.artworkUri, contentDescription = null, modifier = Modifier.fillMaxSize())
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(song.title, style = typography.labelLg, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(song.artistName, style = typography.bodySm, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
