package com.resonance.player.feature.favorites

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
    Column(Modifier.fillMaxSize()) {
        ResonanceTopBar(
            title = stringResource(R.string.favorites_title),
            onBack = onBack
        )
        if (songs.isEmpty()) {
            ResonanceEmptyState(
                title = stringResource(R.string.favorites_empty_title),
                body = stringResource(R.string.favorites_empty_body)
            )
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(songs, key = { it.id }) { song ->
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
                            isCurrent = song.id == currentSongId,
                            isSelected = false,
                            isMissing = false,
                            isLoading = false
                        ),
                        isPlayingAnimation = song.id == currentSongId,
                        badge = { SongFormatBadge(song) },
                        onOverflowClick = { overflowSong = song },
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
