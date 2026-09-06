package com.resonance.player.feature.queue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.resonance.player.R

/** Queue renders the authoritative PlaybackSnapshot queue in Phase 2. */
@Composable
fun QueueScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(stringResource(R.string.nav_queue), style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "The playback queue lives here once the Phase 2 engine owns it.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
