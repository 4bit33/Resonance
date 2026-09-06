package com.resonance.player.feature.player

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.resonance.player.R
import com.resonance.player.core.common.formatDurationMs
import com.resonance.player.core.ui.components.ErrorView
import com.resonance.player.core.ui.components.LoadingView

/**
 * Now Playing placeholder. Shows genuine file metadata; there is deliberately
 * NO progress bar / fake transport until the Phase 2 engine exists.
 */
@Composable
fun PlayerScreen(viewModel: PlayerViewModel) {
    val state by viewModel.uiState.collectAsState()
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
        when (val s = state) {
            PlayerUiState.Loading -> LoadingView()
            is PlayerUiState.Error -> ErrorView(message = s.message)
            is PlayerUiState.Details -> {
                val song = s.song
                Text(song.title, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
                Text(song.artistName, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(song.albumName, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text("Duration: " + formatDurationMs(song.durationMs))
                Text("Format: " + (song.mimeType ?: "unknown"))
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Playback engine arrives in Phase 2. " +
                        "This screen already reads the real library entry.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
