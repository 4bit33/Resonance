package com.resonance.player.feature.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonance.player.R
import com.resonance.player.core.model.Playlist
import com.resonance.player.core.ui.components.ResonanceBottomSheet
import com.resonance.player.core.ui.components.ResonanceDialog
import com.resonance.player.core.ui.components.ResonanceEmptyState
import com.resonance.player.core.ui.components.ResonanceTopBar
import com.resonance.player.core.ui.theme.ResonanceTheme

/**
 * Stitch Playlists tab: real persisted playlists with counts, create via
 * dialog, overflow actions (play/rename/delete). No cloud, no sharing.
 */
@Composable
fun PlaylistsScreen(
    viewModel: PlaylistsViewModel,
    onOpenDetail: (Long) -> Unit,
    onOpenQueue: () -> Unit,
    onShowMessage: (String) -> Unit = {}
) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val spacing = ResonanceTheme.spacing
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    var overflowPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var renameTarget by remember { mutableStateOf<Playlist?>(null) }
    var deleteTarget by remember { mutableStateOf<Playlist?>(null) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ResonanceTopBar(title = stringResource(R.string.nav_playlists))
            if (playlists.isEmpty()) {
                ResonanceEmptyState(
                    title = stringResource(R.string.playlist_empty_title),
                    body = stringResource(R.string.playlist_empty_body),
                    actionLabel = stringResource(R.string.playlist_new),
                    onAction = { showCreate = true }
                )
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(playlists, key = { it.id }) { playlist ->
                        PlaylistRow(
                            playlist = playlist,
                            onClick = { onOpenDetail(playlist.id) },
                            onOverflow = { overflowPlaylist = playlist }
                        )
                    }
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
        FloatingActionButton(
            onClick = { showCreate = true },
            containerColor = colors.accent,
            contentColor = colors.onAccent,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(spacing.lg)
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.playlist_new))
        }
    }
    if (showCreate) {
        PlaylistNameDialog(
            title = stringResource(R.string.playlist_new),
            confirmLabel = stringResource(R.string.playlist_create),
            onConfirm = {
                viewModel.create(it)
                showCreate = false
            },
            onDismiss = { showCreate = false }
        )
    }
    val emptyPlaylistMessage = stringResource(R.string.playlist_empty_action)
    overflowPlaylist?.let { playlist ->
        PlaylistOverflowSheet(
            playlist = playlist,
            onDismiss = { overflowPlaylist = null },
            onPlay = {
                viewModel.play(
                    playlist.id,
                    onPlaying = { onOpenQueue() },
                    onEmpty = { onShowMessage(emptyPlaylistMessage) }
                )
                overflowPlaylist = null
            },
            onRename = {
                renameTarget = playlist
                overflowPlaylist = null
            },
            onDelete = {
                deleteTarget = playlist
                overflowPlaylist = null
            }
        )
    }
    renameTarget?.let { playlist ->
        PlaylistNameDialog(
            title = stringResource(R.string.playlist_rename),
            confirmLabel = stringResource(R.string.playlist_rename),
            initial = playlist.name,
            onConfirm = {
                viewModel.rename(playlist.id, it)
                renameTarget = null
            },
            onDismiss = { renameTarget = null }
        )
    }
    deleteTarget?.let { playlist ->
        ResonanceDialog(
            title = stringResource(R.string.playlist_delete_title),
            text = stringResource(R.string.playlist_delete_body),
            confirmLabel = stringResource(R.string.playlist_delete),
            onConfirm = {
                viewModel.delete(playlist.id)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
            dismissLabel = stringResource(R.string.action_dismiss)
        )
    }
}

@Composable
private fun PlaylistRow(
    playlist: Playlist,
    onClick: () -> Unit,
    onOverflow: () -> Unit
) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val spacing = ResonanceTheme.spacing
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(ResonanceTheme.dimensions.songRowHeight)
            .padding(horizontal = spacing.lg)
            .clickable(onClick = onClick)
    ) {
        androidx.compose.foundation.layout.Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(ResonanceTheme.dimensions.songArtwork)
                .clip(ResonanceTheme.radii.control)
                .background(colors.surfaceContainer)
        ) {
            Icon(
                Icons.Filled.MusicNote,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(spacing.md))
        Column(Modifier.weight(1f)) {
            Text(
                playlist.name,
                style = typography.titleMd,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                playlist.itemCount.toString() + " " + stringResource(R.string.settings_songs),
                style = typography.bodySm,
                color = colors.textSecondary
            )
        }
        IconButton(onClick = onOverflow, modifier = Modifier.size(spacing.touchMin)) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = null,
                tint = colors.textSecondary
            )
        }
    }
}

@Composable
private fun PlaylistOverflowSheet(
    playlist: Playlist,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val spacing = ResonanceTheme.spacing
    ResonanceBottomSheet(onDismiss = onDismiss) {
        Text(playlist.name, style = typography.titleMd, color = colors.textPrimary)
        Spacer(Modifier.height(spacing.md))
        SheetAction(
            icon = Icons.Filled.PlayArrow,
            label = stringResource(R.string.cd_play),
            onClick = onPlay
        )
        SheetAction(
            icon = Icons.Filled.Edit,
            label = stringResource(R.string.playlist_rename),
            onClick = onRename
        )
        SheetAction(
            icon = Icons.Filled.Delete,
            label = stringResource(R.string.playlist_delete),
            destructive = true,
            onClick = onDelete
        )
    }
}

@Composable
private fun SheetAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    destructive: Boolean = false
) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val spacing = ResonanceTheme.spacing
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = spacing.md)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (destructive) colors.statusError else colors.textSecondary
        )
        Spacer(Modifier.width(spacing.lg))
        Text(
            label,
            style = typography.bodyLg,
            color = if (destructive) colors.statusError else colors.textPrimary
        )
    }
}

/** Single-line name dialog (create + rename share validation + styling). */
@Composable
fun PlaylistNameDialog(
    title: String,
    confirmLabel: String,
    initial: String = "",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    var name by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = typography.titleMd, color = colors.textPrimary) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.playlist_name_hint)) },
                singleLine = true,
                shape = ResonanceTheme.radii.control,
                textStyle = typography.bodyLg.copy(color = colors.textPrimary),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) {
                Text(confirmLabel, style = typography.labelLg, color = colors.accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.action_dismiss),
                    style = typography.labelLg,
                    color = colors.textSecondary
                )
            }
        },
        shape = ResonanceTheme.radii.card,
        containerColor = colors.surfaceHigh
    )
}
