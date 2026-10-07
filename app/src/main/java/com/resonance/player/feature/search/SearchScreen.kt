package com.resonance.player.feature.search

import com.resonance.player.core.ui.components.pressClickable
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import com.resonance.player.domain.library.CollectionRef
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.resonance.player.R
import com.resonance.player.core.common.formatDurationMs
import com.resonance.player.core.model.Song
import com.resonance.player.core.ui.components.ArtworkImage
import com.resonance.player.core.ui.components.PlaylistPickerSheet
import com.resonance.player.core.ui.components.ResonanceEmptyState
import com.resonance.player.core.ui.components.ResonanceSearchField
import com.resonance.player.core.ui.components.ResonanceSectionHeader
import com.resonance.player.core.ui.components.ResonanceSongRow
import com.resonance.player.core.ui.components.ResonanceTopBar
import com.resonance.player.core.ui.components.SongFormatBadge
import com.resonance.player.core.ui.components.SongOverflowSheet
import com.resonance.player.core.ui.components.songRowState
import com.resonance.player.core.ui.theme.ResonanceTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search

/**
 * Global search: debounced categorized results over the local index.
 * Songs play from the result list; albums/artists/genres/playlists open
 * their page. Blank queries hit no database.
 */
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    currentSongId: Long?,
    onBack: () -> Unit,
    onSongClick: (Long) -> Unit,
    onOpenCollection: (CollectionRef) -> Unit,
    onOpenPlaylist: (Long) -> Unit
) {
    val query by viewModel.currentQuery.collectAsStateWithLifecycle()
    val results by viewModel.grouped.collectAsStateWithLifecycle()
    val spacing = ResonanceTheme.spacing
    var overflowSong by remember { mutableStateOf<Song?>(null) }
    Column(Modifier.fillMaxSize()) {
        ResonanceSearchField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            label = stringResource(R.string.search_hint),
            leading = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                }
            },
            autoFocus = true,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
        )
        if (query.isBlank()) {
            ResonanceEmptyState(
                title = stringResource(R.string.search_empty_title),
                body = stringResource(R.string.search_empty_body),
                icon = Icons.Rounded.Search
            )
            return@Column
        }
        if (results.isEmpty()) {
            // Debounced flow may lag one frame behind typing; an empty
            // result set with a non-blank query is the no-results state.
            ResonanceEmptyState(
                title = stringResource(R.string.search_no_results_title),
                body = stringResource(R.string.search_no_results_body),
                icon = Icons.Rounded.SearchOff
            )
            return@Column
        }
        LazyColumn(Modifier.fillMaxSize()) {
            if (results.songs.isNotEmpty()) {
                item(key = "h-songs") {
                    ResonanceSectionHeader(title = stringResource(R.string.tab_songs))
                }
                items(results.songs, key = { "song-${it.id}" }) { song ->
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
                            val at = results.songs.indexOfFirst { it.id == song.id }
                            if (at >= 0) viewModel.playFrom(results.songs, at)
                            onSongClick(song.id)
                        },
                        state = songRowState(
                            isCurrent = isCurrent,
                            isSelected = false,
                            isMissing = false,
                            isLoading = false
                        ),
                        isPlayingAnimation = isCurrent,
                        onLongClick = { overflowSong = song },
                        modifier = Modifier.animateItem()
                    )
                }
            }
            if (results.albums.isNotEmpty()) {
                item(key = "h-albums") {
                    ResonanceSectionHeader(title = stringResource(R.string.tab_albums))
                }
                items(results.albums, key = { "album-${it.id}" }) { album ->
                    CategoryRow(
                        title = album.name,
                        subtitle = stringResource(R.string.source_album) + " · " + album.artistName,
                        artUri = album.artUri,
                        onClick = { onOpenCollection(CollectionRef.Album(album.name, album.albumArtist)) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
            if (results.artists.isNotEmpty()) {
                item(key = "h-artists") {
                    ResonanceSectionHeader(title = stringResource(R.string.tab_artists))
                }
                items(results.artists, key = { "artist-${it.id}" }) { artist ->
                    CategoryRow(
                        title = artist.name,
                        subtitle = stringResource(R.string.source_artist) + " · " +
                            pluralStringResource(R.plurals.music_song_count, artist.songCount, artist.songCount),
                        round = true,
                        artUri = artist.artUri,
                        onClick = { onOpenCollection(CollectionRef.Artist(artist.name)) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
            if (results.playlists.isNotEmpty()) {
                item(key = "h-playlists") {
                    ResonanceSectionHeader(title = stringResource(R.string.nav_playlists))
                }
                items(results.playlists, key = { "playlist-${it.id}" }) { playlist ->
                    CategoryRow(
                        title = playlist.name,
                        subtitle = stringResource(R.string.source_playlist) + " · " +
                            pluralStringResource(R.plurals.music_song_count, playlist.itemCount, playlist.itemCount),
                        artUri = null,
                        onClick = { onOpenPlaylist(playlist.id) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
            if (results.genres.isNotEmpty()) {
                item(key = "h-genres") {
                    ResonanceSectionHeader(title = stringResource(R.string.tab_genres))
                }
                items(results.genres, key = { "genre-${it.name}" }) { genre ->
                    CategoryRow(
                        title = genre.name,
                        subtitle = stringResource(R.string.collection_genre) + " · " +
                            pluralStringResource(R.plurals.music_song_count, genre.songCount, genre.songCount),
                        artUri = genre.artUri,
                        onClick = { onOpenCollection(CollectionRef.Genre(genre.name)) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
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

/** An album / artist / playlist / genre result, styled like a song row (artists get a round picture). */
@Composable
private fun CategoryRow(
    title: String,
    subtitle: String,
    artUri: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    round: Boolean = false
) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .pressClickable(pressedScale = 0.98f, onClickLabel = title, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Box(Modifier.size(52.dp).clip(if (round) CircleShape else RoundedCornerShape(12.dp))) {
            ArtworkImage(artworkUri = artUri, contentDescription = null, modifier = Modifier.fillMaxSize())
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = typography.titleMd, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = typography.bodySm, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

