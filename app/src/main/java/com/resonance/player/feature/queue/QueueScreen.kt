package com.resonance.player.feature.queue

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.resonance.player.R
import com.resonance.player.core.common.formatDurationMs
import com.resonance.player.core.model.ShuffleMode
import com.resonance.player.core.ui.components.ArtworkImage
import com.resonance.player.core.ui.components.ReorderableLazyColumn
import com.resonance.player.core.ui.components.ResonanceCompactSongRow
import com.resonance.player.core.ui.components.ResonanceDialog
import com.resonance.player.core.ui.components.ResonanceEmptyState
import com.resonance.player.core.ui.components.ResonanceTopBar
import com.resonance.player.core.ui.theme.ResonanceTheme

/**
 * Stitch Queue: Up Next list over the authoritative snapshot queue.
 * Long-press-drag reorders through moveQueueItem (single commit on
 * release); remove/clear/tap-play map 1:1 to engine operations. No
 * Smart Mix entry — generating queues is a future phase, and a dead
 * button would be fake functionality.
 */
@Composable
fun QueueScreen(viewModel: QueueViewModel, onBack: () -> Unit) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val spacing = ResonanceTheme.spacing
    val snapshot by viewModel.snapshot.collectAsState()
    var confirmClear by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        ResonanceTopBar(
            title = stringResource(R.string.nav_queue),
            onBack = onBack
        )
        if (snapshot.queue.isEmpty()) {
            ResonanceEmptyState(
                title = stringResource(R.string.nav_queue),
                body = stringResource(R.string.queue_empty)
            )
            return@Column
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.lg, vertical = spacing.sm)
        ) {
            Text(
                stringResource(R.string.queue_up_next_count, snapshot.queue.size),
                style = typography.titleMd,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            if (snapshot.shuffle == ShuffleMode.ON) {
                Icon(
                    Icons.Filled.Shuffle,
                    contentDescription = stringResource(R.string.cd_shuffle),
                    tint = colors.accent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(spacing.sm))
            }
            TextButton(onClick = { confirmClear = true }) {
                Text(
                    stringResource(R.string.queue_clear),
                    style = typography.labelMd,
                    color = colors.textSecondary
                )
            }
        }
        ReorderableLazyColumn(
            items = snapshot.queue,
            key = { it.queueId },
            modifier = Modifier.fillMaxSize()
        ) { item, index ->
            val isCurrent = index == snapshot.queueIndex
            ResonanceCompactSongRow(
                title = item.song.title,
                subtitle = item.song.artistName + " - " + formatDurationMs(item.song.durationMs),
                artwork = {
                    ArtworkImage(
                        artworkUri = item.song.artworkUri,
                        contentDescription = item.song.albumName
                    )
                },
                onClick = { viewModel.playAt(index) },
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
                        onClick = { viewModel.remove(index) },
                        modifier = Modifier.size(spacing.touchMin)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.queue_remove),
                            tint = colors.textSecondary
                        )
                    }
                },
                highlighted = isCurrent
            )
        }
    }
    if (confirmClear) {
        ResonanceDialog(
            title = stringResource(R.string.queue_clear_title),
            text = stringResource(R.string.queue_clear_body),
            confirmLabel = stringResource(R.string.queue_clear),
            onConfirm = {
                viewModel.clear()
                confirmClear = false
            },
            onDismiss = { confirmClear = false },
            dismissLabel = stringResource(R.string.action_dismiss)
        )
    }
}

