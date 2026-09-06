package com.resonance.player.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.AddToQueue
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.resonance.player.R
import com.resonance.player.core.model.Song
import com.resonance.player.core.ui.theme.ResonanceTheme

/**
 * Shared song overflow sheet (Library / Search / Queue / Player rows).
 * Only exposes actions the caller wires — no dead buttons. The
 * add-to-playlist row appears only when [onAddToPlaylist] is provided
 * (Stage G playlist picker).
 */
@Composable
fun SongOverflowSheet(
    song: Song,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val spacing = ResonanceTheme.spacing
    ResonanceBottomSheet(onDismiss = onDismiss, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ArtworkImage(
                artworkUri = song.artworkUri,
                contentDescription = song.albumName,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.width(spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    song.title,
                    style = typography.titleMd,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    song.artistName + " - " + song.albumName,
                    style = typography.bodySm,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.height(spacing.lg))
        OverflowAction(
            icon = Icons.Filled.SkipNext,
            label = stringResource(R.string.action_play_next),
            onClick = onPlayNext
        )
        OverflowAction(
            icon = Icons.Filled.AddToQueue,
            label = stringResource(R.string.action_add_to_queue),
            onClick = onAddToQueue
        )
        if (onAddToPlaylist != null) {
            OverflowAction(
                icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                label = stringResource(R.string.action_add_to_playlist),
                onClick = onAddToPlaylist
            )
        }
    }
}

@Composable
private fun OverflowAction(icon: ImageVector, label: String, onClick: () -> Unit) {
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
        Icon(icon, contentDescription = null, tint = colors.textSecondary)
        Spacer(Modifier.width(spacing.lg))
        Text(label, style = typography.bodyLg, color = colors.textPrimary)
    }
}
