package com.resonance.player.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import android.app.Activity
import com.resonance.player.BuildConfig
import com.resonance.player.R
import com.resonance.player.core.common.userMessage
import com.resonance.player.core.media.ScanState
import com.resonance.player.core.permissions.AudioPermissionStatus
import com.resonance.player.core.ui.components.ResonancePrimaryButton
import com.resonance.player.core.ui.components.ResonanceSectionHeader
import com.resonance.player.core.ui.components.ResonanceSegmentedControl
import com.resonance.player.core.ui.components.ResonanceSettingsIcon
import com.resonance.player.core.ui.components.ResonanceSettingsRow
import com.resonance.player.core.ui.components.ResonanceSwitch
import com.resonance.player.core.ui.components.ResonanceTopBar
import com.resonance.player.core.ui.components.openAppSettings
import com.resonance.player.core.ui.components.rememberPermissionGrant
import com.resonance.player.core.ui.theme.ResonanceTheme
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
    val ignoreShort by viewModel.ignoreShortFiles.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val requestGrant = rememberPermissionGrant(viewModel.permissionManager) {
        viewModel.rescan()
    }
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val spacing = ResonanceTheme.spacing
    Column(Modifier.fillMaxSize()) {
        ResonanceTopBar(title = stringResource(R.string.nav_settings))
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            SettingsGroupLabel(stringResource(R.string.settings_group_library))
            Surface(
                shape = ResonanceTheme.radii.card,
                color = colors.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.lg)
            ) {
                Column {
                    val statsText = stats?.let {
                        it.songCount.toString() + " " + stringResource(R.string.settings_songs) + " - " +
                            it.albumCount.toString() + " " + stringResource(R.string.settings_albums) + " - " +
                            it.artistCount.toString() + " " + stringResource(R.string.settings_artists) + " - " +
                            it.genreCount.toString() + " " + stringResource(R.string.settings_genres)
                    } ?: stringResource(R.string.settings_scanning)
                    ResonanceSettingsRow(
                        title = stringResource(R.string.settings_library),
                        subtitle = statsText,
                        leading = {
                            ResonanceSettingsIcon(
                                icon = Icons.Filled.FolderSpecial,
                                contentDescription = null
                            )
                        },
                        trailing = { }
                    )
                    if (scanState is ScanState.Scanning) {
                        val s = scanState as ScanState.Scanning
                        val progress = if (s.total > 0) {
                            s.processed.toFloat() / s.total.toFloat()
                        } else {
                            null
                        }
                        if (progress != null) {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = spacing.lg)
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = spacing.lg)
                            )
                        }
                    }
                    if (scanState is ScanState.Failed) {
                        Text(
                            text = (scanState as ScanState.Failed).error.userMessage(),
                            style = typography.bodyMd,
                            color = colors.error,
                            modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm)
                        )
                    }
                    ResonanceSettingsRow(
                        title = stringResource(R.string.settings_rescan),
                        subtitle = stringResource(R.string.settings_last_scan) + ": " +
                            formatScanTime(lastScan, stringResource(R.string.settings_never)),
                        leading = {
                            ResonanceSettingsIcon(
                                icon = Icons.Filled.Refresh,
                                contentDescription = null
                            )
                        },
                        trailing = {
                            ResonancePrimaryButton(
                                label = stringResource(R.string.settings_rescan),
                                onClick = viewModel::rescan,
                                enabled = scanState !is ScanState.Scanning
                            )
                        }
                    )
                    ResonanceSettingsRow(
                        title = stringResource(R.string.settings_ignore_short),
                        subtitle = stringResource(R.string.settings_ignore_short_body),
                        leading = {
                            ResonanceSettingsIcon(
                                icon = Icons.Filled.TimerOff,
                                contentDescription = null
                            )
                        },
                        trailing = {
                            ResonanceSwitch(
                                checked = ignoreShort,
                                onCheckedChange = viewModel::setIgnoreShortFiles
                            )
                        }
                    )
                    ResonanceSettingsRow(
                        title = stringResource(R.string.settings_permission),
                        subtitle = permissionLabel(permission),
                        leading = {
                            ResonanceSettingsIcon(
                                icon = Icons.Filled.Shield,
                                contentDescription = null
                            )
                        },
                        trailing = {
                            if (permission != AudioPermissionStatus.Granted) {
                                androidx.compose.material3.TextButton(
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
                                        },
                                        style = typography.labelLg,
                                        color = colors.accent
                                    )
                                }
                            }
                        }
                    )
                }
            }
            Spacer(Modifier.height(spacing.sectionSpacing))
            SettingsGroupLabel(stringResource(R.string.settings_group_appearance))
            Column(modifier = Modifier.padding(horizontal = spacing.lg)) {
                ResonanceSegmentedControl(
                    options = ThemeMode.entries.map { themeName(it) },
                    selectedIndex = ThemeMode.entries.indexOf(theme),
                    onSelect = { viewModel.setThemeMode(ThemeMode.entries[it]) }
                )
            }
            Spacer(Modifier.height(spacing.sectionSpacing))
            SettingsGroupLabel(stringResource(R.string.settings_group_about))
            Surface(
                shape = ResonanceTheme.radii.card,
                color = colors.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.lg)
            ) {
                Column {
                    ResonanceSettingsRow(
                        title = stringResource(R.string.settings_privacy_title),
                        subtitle = stringResource(R.string.settings_privacy_body),
                        leading = {
                            ResonanceSettingsIcon(
                                icon = Icons.Filled.Shield,
                                contentDescription = null
                            )
                        },
                        trailing = { }
                    )
                    ResonanceSettingsRow(
                        title = stringResource(R.string.settings_version) + ": " +
                            BuildConfig.VERSION_NAME,
                        subtitle = stringResource(R.string.settings_about_body),
                        leading = {
                            ResonanceSettingsIcon(
                                icon = Icons.Filled.Info,
                                contentDescription = null
                            )
                        },
                        trailing = { }
                    )
                }
            }
            Spacer(Modifier.height(spacing.xxl))
        }
    }
}

@Composable
private fun SettingsGroupLabel(text: String) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val spacing = ResonanceTheme.spacing
    Text(
        text = text.uppercase(),
        style = typography.labelLg,
        color = colors.accent,
        modifier = Modifier.padding(
            horizontal = spacing.lg,
            vertical = spacing.sm
        )
    )
}

@Composable
private fun themeName(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
    ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
    ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
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
