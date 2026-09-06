package com.resonance.player.feature.queue

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.resonance.player.R
import com.resonance.player.core.common.formatDurationMs

/** Live view of the authoritative playback queue (read-only in Phase 2). */
@Composable
fun QueueScreen(viewModel: QueueViewModel) {
    val snapshot by viewModel.snapshot.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(stringResource(R.string.nav_queue), style = MaterialTheme.typography.headlineMedium)
        if (snapshot.queue.isEmpty()) {
            Text(
                text = stringResource(R.string.queue_empty),
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(snapshot.queue, key = { it.queueId }) { item ->
                    val isCurrent = item.position == snapshot.queueIndex
                    ListItem(
                        headlineContent = {
                            Text(
                                item.song.title,
                                fontWeight = if (isCurrent) FontWeight.Bold else null,
                                color = if (isCurrent) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        },
                        supportingContent = {
                            Text(item.song.artistName + " - " + item.song.albumName)
                        },
                        trailingContent = { Text(formatDurationMs(item.song.durationMs)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
