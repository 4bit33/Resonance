package com.resonance.player.feature.playlists

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.resonance.player.R
import com.resonance.player.core.common.formatDurationMs
import com.resonance.player.core.model.Playlist
import com.resonance.player.core.ui.components.ArtworkImage
import com.resonance.player.core.ui.components.ReorderableLazyColumn
import com.resonance.player.core.ui.components.ResonanceCompactSongRow
import com.resonance.player.core.ui.components.ResonanceDialog
import com.resonance.player.core.ui.components.ResonanceEmptyState
import com.resonance.player.core.ui.components.ResonanceIconButton
import com.resonance.player.core.ui.components.ResonanceTopBar
import com.resonance.player.core.ui.components.SongPickerSheet
import com.resonance.player.core.ui.theme.ResonanceTheme

/**
 * Playlist detail: persisted songs in stored order, play-all/shuffle,
 * remove, drag reorder, rename, delete. Everything hits Room through
 * use cases; the queue only receives explicit play commands.
 */
@Composable
fun PlaylistDetailScreen(
    viewModel: PlaylistDetailViewModel,
    playlist: Playlist?,
    onBack: () -> Unit,
    onSongClick: (Long) -> Unit,
    onDeleted: () -> Unit
) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val spacing = ResonanceTheme.spacing
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showAddSongs by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        ResonanceTopBar(
            title = playlist?.name ?: stringResource(R.string.nav_playlists),
            onBack = onBack
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.lg, vertical = spacing.sm)
        ) {
            Text(
                songs.size.toString() + " " + stringResource(R.string.settings_songs) + " - " +
                    formatDurationMs(songs.sumOf { it.durationMs }),
                style = typography.bodySm,
                color = colors.textSecondary,
                modifier = Modifier.weight(1f)
            )
            ResonanceIconButton(
                onClick = { viewModel.playAll(shuffled = false) },
                icon = Icons.Filled.PlayArrow,
                contentDescription = stringResource(R.string.cd_play),
                enabled = songs.isNotEmpty()
            )
            ResonanceIconButton(
                onClick = { viewModel.playAll(shuffled = true) },
                icon = Icons.Filled.Shuffle,
                contentDescription = stringResource(R.string.cd_shuffle),
                enabled = songs.isNotEmpty()
            )
            ResonanceIconButton(
                onClick = { showAddSongs = true },
                icon = Icons.Filled.Add,
                contentDescription = stringResource(R.string.playlist_add_songs),
                enabled = playlist != null
            )
            ResonanceIconButton(
                onClick = { showRename = true },
                icon = Icons.Filled.Edit,
                contentDescription = stringResource(R.string.playlist_rename),
                enabled = playlist != null
            )
            ResonanceIconButton(
                onClick = { showDelete = true },
                icon = Icons.Filled.Delete,
                contentDescription = stringResource(R.string.playlist_delete),
                enabled = playlist != null
            )
        }
        if (songs.isEmpty()) {
            ResonanceEmptyState(
                title = stringResource(R.string.playlist_detail_empty_title),
                body = stringResource(R.string.playlist_detail_empty_body)
            )
        } else {
            ReorderableLazyColumn(
                items = songs,
                key = { it.id },
                modifier = Modifier.fillMaxSize()
            ) { song, index ->
                ResonanceCompactSongRow(
                    title = song.title,
                    subtitle = song.artistName + " - " + formatDurationMs(song.durationMs),
                    artwork = {
                        ArtworkImage(
                            artworkUri = song.artworkUri,
                            contentDescription = song.albumName
                        )
                    },
                    onClick = {
                        viewModel.playFrom(index)
                        onSongClick(song.id)
                    },
                    modifier = Modifier.dragged(index),
                    leading = {
                        Icon(
                            Icons.Filled.DragHandle,
                            contentDescription = stringResource(R.string.queue_drag),
                            tint = colors.textMuted,
                            modifier = Modifier
                                .size(spacing.touchMin)
                                .dragHandle(index) { from, to -> viewModel.move(from, to) }
                        )
                    },
                    trailing = {
                        IconButton(
                            onClick = { viewModel.remove(song.id) },
                            modifier = Modifier.size(spacing.touchMin)
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.queue_remove),
                                tint = colors.textSecondary
                            )
                        }
                    },
                    highlighted = false
                )
            }
        }
        if (error != null) {
            Text(
                text = error ?: "",
                style = typography.bodyMd,
                color = colors.error,
                modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm)
            )
        }
    }
    if (showRename && playlist != null) {
        PlaylistNameDialog(
            title = stringResource(R.string.playlist_rename),
            confirmLabel = stringResource(R.string.playlist_rename),
            initial = playlist.name,
            onConfirm = {
                viewModel.rename(it)
                showRename = false
            },
            onDismiss = { showRename = false }
        )
    }
    if (showDelete) {
        ResonanceDialog(
            title = stringResource(R.string.playlist_delete_title),
            text = stringResource(R.string.playlist_delete_body),
            confirmLabel = stringResource(R.string.playlist_delete),
            onConfirm = {
                viewModel.delete(onDeleted)
            },
            onDismiss = { showDelete = false },
            dismissLabel = stringResource(R.string.action_dismiss)
        )
    }
    if (showAddSongs && playlist != null) {
        val allSongs by viewModel.allSongs.collectAsStateWithLifecycle()
        SongPickerSheet(
            title = stringResource(R.string.playlist_add_songs_title, playlist.name),
            songs = allSongs,
            onConfirm = { ids ->
                viewModel.addSongs(ids) { showAddSongs = false }
            },
            onDismiss = { showAddSongs = false }
        )
    }
}
