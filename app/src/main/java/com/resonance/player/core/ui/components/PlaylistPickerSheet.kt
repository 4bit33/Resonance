package com.resonance.player.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.resonance.player.R
import com.resonance.player.core.model.Playlist
import com.resonance.player.core.ui.components.ResonanceBottomSheet
import com.resonance.player.core.ui.components.ResonancePrimaryButton
import com.resonance.player.core.ui.theme.ResonanceTheme

/**
 * Shared add-to-playlist picker (song overflow sheets everywhere).
 * Lists real playlists, creates on the spot, reports errors honestly.
 */
@Composable
fun PlaylistPickerSheet(
    songTitle: String,
    playlists: List<Playlist>,
    error: String?,
    onPick: (Long) -> Unit,
    onNewPlaylist: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val spacing = ResonanceTheme.spacing
    var creating by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    ResonanceBottomSheet(onDismiss = onDismiss, modifier = modifier) {
        Text(
            stringResource(R.string.playlist_picker_title, songTitle),
            style = typography.titleMd,
            color = colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(spacing.md))
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(playlists, key = { it.id }) { playlist ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(playlist.id) }
                        .padding(vertical = spacing.md)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            playlist.name,
                            style = typography.bodyLg,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            playlist.itemCount.toString() + " " +
                                stringResource(R.string.settings_songs),
                            style = typography.bodySm,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
        if (creating) {
            Spacer(Modifier.height(spacing.sm))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.playlist_name_hint)) },
                singleLine = true,
                shape = ResonanceTheme.radii.control,
                textStyle = typography.bodyLg.copy(color = colors.textPrimary),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(spacing.sm))
            ResonancePrimaryButton(
                label = stringResource(R.string.playlist_create),
                onClick = { onNewPlaylist(name) },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Spacer(Modifier.height(spacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                    tint = colors.accent
                )
                Spacer(Modifier.width(spacing.sm))
                androidx.compose.material3.                TextButton(onClick = { creating = true }) {
                    Text(
                        stringResource(R.string.playlist_new),
                        style = typography.labelLg,
                        color = colors.accent
                    )
                }
            }
        }
        if (error != null) {
            Spacer(Modifier.height(spacing.sm))
            Text(error, style = typography.bodyMd, color = colors.error)
        }
    }
}
