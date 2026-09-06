package com.resonance.player.feature.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.resonance.player.R
import com.resonance.player.core.common.formatDurationMs
import com.resonance.player.core.ui.components.ArtworkImage

@Composable
fun SearchScreen(viewModel: SearchViewModel) {
    val query by viewModel.currentQuery.collectAsState()
    val results by viewModel.results.collectAsState()
    Column(Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.nav_search),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            label = { Text(stringResource(R.string.nav_search)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        LazyColumn(Modifier.fillMaxSize()) {
            items(results, key = { it.id }) { song ->
                ListItem(
                    headlineContent = { Text(song.title) },
                    supportingContent = { Text(song.artistName + " - " + song.albumName) },
                    trailingContent = { Text(formatDurationMs(song.durationMs)) },
                    leadingContent = {
                        ArtworkImage(
                            artworkUri = song.artworkUri,
                            contentDescription = song.albumName,
                            modifier = Modifier.size(48.dp),
                            fallbackSize = 20.dp
                        )
                    }
                )
            }
        }
    }
}
