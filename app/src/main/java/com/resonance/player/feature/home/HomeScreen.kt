package com.resonance.player.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.resonance.player.R

/** Foundation home: navigation shortcuts only. Smart sections come later. */
@Composable
fun HomeScreen(
    onOpenLibrary: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.tagline), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onOpenLibrary, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.nav_library))
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onOpenSearch, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.nav_search))
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.nav_settings))
        }
    }
}
