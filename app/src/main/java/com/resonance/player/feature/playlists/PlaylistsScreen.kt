package com.resonance.player.feature.playlists

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

/** Playlist management UI lands with the repository implementation (Phase 3). */
@Composable
fun PlaylistsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(stringResource(R.string.nav_playlists), style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "Playlists, favorites and history use the tables already in the database.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
