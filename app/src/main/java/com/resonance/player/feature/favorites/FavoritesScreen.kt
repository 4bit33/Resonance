package com.resonance.player.feature.favorites

import com.resonance.player.core.ui.theme.ResonanceTheme
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.Icons
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.resonance.player.R
import com.resonance.player.core.common.formatDurationMs
import com.resonance.player.core.model.Song
import com.resonance.player.core.ui.components.ArtworkImage
import com.resonance.player.core.ui.components.PlaylistPickerSheet
import com.resonance.player.core.ui.components.ResonanceEmptyState
import com.resonance.player.core.ui.components.ResonanceSongRow
import com.resonance.player.core.ui.components.ResonanceTopBar
import com.resonance.player.core.ui.components.SongFormatBadge
import com.resonance.player.core.ui.components.SongOverflowSheet
import com.resonance.player.core.ui.components.songRowState

@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel,
    currentSongId: Long?,
    onBack: () -> Unit,
    onSongClick: (Long) -> Unit
) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    var overflowSong by remember { mutableStateOf<Song?>(null) }
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    Column(Modifier.fillMaxSize()) {
        ResonanceTopBar(title = "", onBack = onBack)
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), modifier = Modifier.fillMaxSize()) {
            item(key = "header") {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Brush.linearGradient(listOf(colors.accent, colors.accentDim)))
                    ) {
                        Icon(Icons.Rounded.Favorite, contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(52.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(stringResource(R.string.favorites_title), style = typography.headlineLg, color = colors.textPrimary)
                        Text(
                            pluralStringResource(R.plurals.music_song_count, songs.size, songs.size) + " · " +
                                formatDurationMs(songs.sumOf { it.durationMs }),
                            style = typography.bodySm,
                            color = colors.textSecondary
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { viewModel.playAll(shuffled = false) },
                                enabled = songs.isNotEmpty(),
                                contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                                Text(stringResource(R.string.cd_play))
                            }
                            FilledTonalIconButton(onClick = { viewModel.playAll(shuffled = true) }, enabled = songs.isNotEmpty()) {
                                Icon(Icons.Rounded.Shuffle, contentDescription = stringResource(R.string.cd_shuffle))
                            }
                        }
                    }
                }
            }
            if (songs.isEmpty()) {
                item(key = "empty") {
                    ResonanceEmptyState(
                        title = stringResource(R.string.favorites_empty_title),
                        body = stringResource(R.string.favorites_empty_body),
                        icon = Icons.Rounded.FavoriteBorder
                    )
                }
            }
            items(songs, key = { it.id }) { song ->
                ResonanceSongRow(
                    title = song.title,
                    artistLine = song.artistName + " - " + song.albumName,
                    duration = formatDurationMs(song.durationMs),
                    artwork = { ArtworkImage(artworkUri = song.artworkUri, contentDescription = song.albumName) },
                    onClick = {
                        val index = songs.indexOfFirst { it.id == song.id }
                        if (index >= 0) viewModel.playFrom(songs, index)
                        onSongClick(song.id)
                    },
                    state = songRowState(isCurrent = song.id == currentSongId, isSelected = false, isMissing = false, isLoading = false),
                    isPlayingAnimation = song.id == currentSongId,
                    onLongClick = { overflowSong = song },
                    modifier = Modifier.animateItem()
                )
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
