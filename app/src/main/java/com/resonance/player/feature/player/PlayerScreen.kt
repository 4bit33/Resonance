package com.resonance.player.feature.player

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.resonance.player.R
import com.resonance.player.core.common.formatDurationMs
import com.resonance.player.core.common.userMessage
import com.resonance.player.core.ui.components.ArtworkImage
import com.resonance.player.core.model.RepeatMode
import com.resonance.player.core.model.ShuffleMode

/**
 * Now Playing bound to the REAL engine. Position comes from the player via
 * the snapshot ticker; the slider only writes on release (no command spam).
 * No fake progress anywhere: an empty queue shows details + idle controls.
 */
@Composable
fun PlayerScreen(viewModel: PlayerViewModel, onOpenQueue: () -> Unit) {
    val snapshot by viewModel.snapshot.collectAsState()
    val details by viewModel.details.collectAsState()
    val commandError by viewModel.commandError.collectAsState()

    val song = snapshot.song ?: details
    val duration = snapshot.durationMs
    val hasQueue = snapshot.queue.isNotEmpty()

    var dragging by remember { mutableStateOf(false) }
    var dragMs by remember { mutableFloatStateOf(0f) }
    val shownPosition = if (dragging) dragMs.toLong() else snapshot.positionMs

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.nav_player),
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(16.dp))
        if (song != null) {
            ArtworkImage(
                artworkUri = song.artworkUri,
                contentDescription = song.albumName,
                modifier = Modifier.size(192.dp),
                fallbackSize = 64.dp
            )
            Spacer(Modifier.height(16.dp))
            Text(song.title, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(song.artistName, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(song.albumName, style = MaterialTheme.typography.bodyMedium)
        } else {
            Text(
                text = stringResource(R.string.empty_library_title),
                style = MaterialTheme.typography.headlineSmall
            )
        }
        Spacer(Modifier.height(16.dp))
        Slider(
            value = shownPosition.toFloat(),
            onValueChange = {
                dragging = true
                dragMs = it
            },
            onValueChangeFinished = {
                dragging = false
                viewModel.onSeek(dragMs.toLong())
            },
            valueRange = 0f..(duration.coerceAtLeast(1L).toFloat()),
            enabled = hasQueue && duration > 0L,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(formatDurationMs(shownPosition.coerceAtLeast(0L)))
            Spacer(Modifier.weight(1f))
            Text(formatDurationMs(duration))
        }
        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(Modifier.weight(1f))
            IconButton(onClick = viewModel::onPrevious, enabled = hasQueue) {
                Icon(Icons.Filled.SkipPrevious, stringResource(R.string.cd_previous))
            }
            IconButton(onClick = viewModel::onTogglePlayPause, enabled = hasQueue) {
                if (snapshot.isPlaying) {
                    Icon(Icons.Filled.Pause, stringResource(R.string.cd_pause))
                } else {
                    Icon(Icons.Filled.PlayArrow, stringResource(R.string.cd_play))
                }
            }
            IconButton(onClick = viewModel::onNext, enabled = hasQueue) {
                Icon(Icons.Filled.SkipNext, stringResource(R.string.cd_next))
            }
            Spacer(Modifier.weight(1f))
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { viewModel.onToggleShuffle(snapshot.shuffle) }) {
                Icon(
                    Icons.Filled.Shuffle,
                    stringResource(R.string.cd_shuffle),
                    tint = if (snapshot.shuffle == ShuffleMode.ON) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            IconButton(onClick = { viewModel.onCycleRepeat(snapshot.repeat) }) {
                when (snapshot.repeat) {
                    RepeatMode.ONE -> Icon(
                        Icons.Filled.RepeatOne,
                        stringResource(R.string.cd_repeat),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    else -> Icon(
                        Icons.Filled.Repeat,
                        stringResource(R.string.cd_repeat),
                        tint = if (snapshot.repeat == RepeatMode.ALL) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onOpenQueue, enabled = hasQueue) {
                Text(stringResource(R.string.queue_up_next))
            }
        }
        val visibleError = commandError ?: snapshot.error
        if (visibleError != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = visibleError.userMessage(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            TextButton(onClick = viewModel::clearCommandError) {
                Text(stringResource(R.string.action_dismiss))
            }
        }
    }
}
