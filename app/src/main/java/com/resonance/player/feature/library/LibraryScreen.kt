package com.resonance.player.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.resonance.player.R
import com.resonance.player.core.common.formatDurationMs
import com.resonance.player.core.ui.components.EmptyLibraryView
import com.resonance.player.core.ui.components.ErrorView
import com.resonance.player.core.ui.components.LoadingView

@Composable
fun LibraryScreen(viewModel: LibraryViewModel, onSongClick: (Long) -> Unit) {
    val state by viewModel.uiState.collectAsState()
    Column(Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.nav_library),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )
        when (val s = state) {
            LibraryUiState.Loading -> LoadingView()
            LibraryUiState.Empty -> EmptyLibraryView()
            is LibraryUiState.Error -> ErrorView(message = s.message)
            is LibraryUiState.Content -> LazyColumn(Modifier.fillMaxSize()) {
                items(s.songs, key = { it.id }) { song ->
                    ListItem(
                        headlineContent = { Text(song.title) },
                        supportingContent = { Text(song.artistName + " - " + song.albumName) },
                        trailingContent = { Text(formatDurationMs(song.durationMs)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSongClick(song.id) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
