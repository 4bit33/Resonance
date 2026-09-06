package com.resonance.player.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.resonance.player.BuildConfig
import com.resonance.player.R
import com.resonance.player.domain.settings.ThemeMode

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val theme by viewModel.themeMode.collectAsState()
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(stringResource(R.string.nav_settings), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleMedium)
        ThemeMode.entries.forEach { mode ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                RadioButton(selected = theme == mode, onClick = { viewModel.setThemeMode(mode) })
                Text(mode.name)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Local only. No account, no ads, no tracking.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.settings_version) + ": " + BuildConfig.VERSION_NAME,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
