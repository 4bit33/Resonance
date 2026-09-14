package com.resonance.player.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.resonance.player.R
import com.resonance.player.core.model.Song
import com.resonance.player.core.ui.theme.ResonanceTheme

/**
 * Shared multi-select song browser (add-songs-to-playlist flow). Filters the
 * full library by title/artist locally; selection is local until confirmed.
 */
@Composable
fun SongPickerSheet(
    title: String,
    songs: List<Song>,
    onConfirm: (List<Long>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val spacing = ResonanceTheme.spacing
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(setOf<Long>()) }
    val filtered = remember(songs, query) {
        if (query.isBlank()) {
            songs
        } else {
            songs.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.artistName.contains(query, ignoreCase = true)
            }
        }
    }
    ResonanceBottomSheet(onDismiss = onDismiss, modifier = modifier) {
        Text(
            title,
            style = typography.titleMd,
            color = colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(spacing.sm))
        ResonanceSearchField(
            value = query,
            onValueChange = { query = it },
            label = stringResource(R.string.search_hint)
        )
        Spacer(Modifier.height(spacing.sm))
        LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
            items(filtered, key = { it.id }) { song ->
                val isSelected = song.id in selected
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selected = if (isSelected) selected - song.id else selected + song.id
                        }
                        .padding(vertical = spacing.sm)
                ) {
                    ArtworkImage(
                        artworkUri = song.artworkUri,
                        contentDescription = song.albumName,
                        modifier = Modifier.size(ResonanceTheme.dimensions.songArtwork)
                    )
                    Spacer(Modifier.width(spacing.md))
                    Column(Modifier.weight(1f)) {
                        Text(
                            song.title,
                            style = typography.bodyLg,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            song.artistName,
                            style = typography.bodySm,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                        contentDescription = null,
                        tint = if (isSelected) colors.accent else colors.textMuted
                    )
                }
            }
        }
        Spacer(Modifier.height(spacing.sm))
        val label = if (selected.isEmpty()) {
            stringResource(R.string.action_add)
        } else {
            stringResource(R.string.action_add) + " (${selected.size})"
        }
        ResonancePrimaryButton(
            label = label,
            onClick = { onConfirm(selected.toList()) },
            enabled = selected.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
