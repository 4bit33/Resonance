package com.resonance.player.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.resonance.player.BuildConfig
import com.resonance.player.R
import com.resonance.player.core.common.userMessage
import com.resonance.player.core.media.ScanState
import com.resonance.player.core.permissions.AudioPermissionStatus
import com.resonance.player.core.ui.components.openAppSettings
import com.resonance.player.core.ui.components.rememberPermissionGrant
import com.resonance.player.domain.settings.ThemeMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val theme by viewModel.themeMode.collectAsState()
    val scanState by viewModel.scanState.collectAsState()
    val lastScan by viewModel.lastScan.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val permission by viewModel.permissionStatus.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val requestGrant = rememberPermissionGrant(viewModel.permissionManager) {
        viewModel.rescan()
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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
        Text(stringResource(R.string.settings_library), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        val statsText = stats?.let {
            it.songCount.toString() + " " + stringResource(R.string.settings_songs) + " - " +
                it.albumCount.toString() + " " + stringResource(R.string.settings_albums) + " - " +
                it.artistCount.toString() + " " + stringResource(R.string.settings_artists) + " - " +
                it.genreCount.toString() + " " + stringResource(R.string.settings_genres)
        } ?: stringResource(R.string.settings_scanning)
        Text(statsText, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.settings_last_scan) + ": " +
                formatScanTime(lastScan, stringResource(R.string.settings_never)),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(8.dp))
        when (val s = scanState) {
            is ScanState.Scanning -> {
                val progress = if (s.total > 0) s.processed.toFloat() / s.total.toFloat() else null
                if (progress != null) {
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(8.dp))
            }
            is ScanState.Failed -> {
                Text(
                    text = s.error.userMessage(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(8.dp))
            }
            else -> Unit
        }
        Button(
            onClick = viewModel::rescan,
            enabled = scanState !is ScanState.Scanning,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.settings_rescan))
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.settings_permission), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = permissionLabel(permission),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            if (permission != AudioPermissionStatus.Granted) {
                TextButton(
                    onClick = {
                        if (permission == AudioPermissionStatus.PermanentlyDenied) {
                            openAppSettings(activity)
                        } else {
                            requestGrant()
                        }
                    }
                ) {
                    Text(
                        if (permission == AudioPermissionStatus.PermanentlyDenied) {
                            stringResource(R.string.perm_open_settings)
                        } else {
                            stringResource(R.string.perm_grant)
                        }
                    )
                }
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

@Composable
private fun permissionLabel(status: AudioPermissionStatus): String = when (status) {
    AudioPermissionStatus.Granted -> stringResource(R.string.perm_granted)
    AudioPermissionStatus.NotAsked -> stringResource(R.string.perm_rationale)
    AudioPermissionStatus.Denied -> stringResource(R.string.perm_rationale)
    AudioPermissionStatus.PermanentlyDenied -> stringResource(R.string.perm_permanently_denied)
}

private val scanTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)

fun formatScanTime(epochSec: Long?, neverLabel: String = "Never"): String {
    if (epochSec == null || epochSec <= 0L) return neverLabel
    return try {
        scanTimeFormatter.format(Instant.ofEpochSecond(epochSec).atZone(ZoneId.systemDefault()))
    } catch (e: Exception) {
        neverLabel
    }
}
