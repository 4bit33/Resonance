package com.resonance.player.feature.favorites

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.resonance.player.R
import com.resonance.player.core.common.formatDurationMs
import com.resonance.player.core.ui.components.ArtworkImage
import com.resonance.player.core.ui.components.ResonanceEmptyState
import com.resonance.player.core.ui.components.ResonanceSongRow
import com.resonance.player.core.ui.components.ResonanceTopBar
import com.resonance.player.core.ui.components.SongFormatBadge
import com.resonance.player.core.ui.components.songRowState

@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel,
    currentSongId: Long?,
    onBack: () -> Unit,
    onSongClick: (Long) -> Unit
) {
    val songs by viewModel.songs.collectAsState()
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
                        badge = { SongFormatBadge(song) }
                    )
                }
            }
        }
    }
}
