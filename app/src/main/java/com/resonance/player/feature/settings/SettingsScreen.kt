package com.resonance.player.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.resonance.player.BuildConfig
import com.resonance.player.R
import com.resonance.player.core.common.userMessage
import com.resonance.player.core.media.ScanState
import com.resonance.player.core.model.MusicSource
import com.resonance.player.core.model.SourceKind
import com.resonance.player.core.ui.components.LocalMusicActions
import com.resonance.player.core.ui.components.ResonanceDialog
import com.resonance.player.core.ui.components.ResonancePrimaryButton
import com.resonance.player.core.ui.components.ResonanceSectionHeader
import com.resonance.player.core.ui.components.ResonanceSegmentedControl
import com.resonance.player.core.ui.components.ResonanceSettingsIcon
import com.resonance.player.core.ui.components.ResonanceSettingsRow
import com.resonance.player.core.ui.components.ResonanceSwitch
import com.resonance.player.core.ui.components.ResonanceTopBar
import com.resonance.player.core.ui.theme.ACCENT_PRESETS
import com.resonance.player.core.ui.theme.ResonanceTheme
import com.resonance.player.core.ui.theme.accentPreviewColor
import com.resonance.player.domain.settings.ThemeMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val theme by viewModel.themeMode.collectAsStateWithLifecycle()
    val accentHue by viewModel.accentHue.collectAsStateWithLifecycle()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val lastScan by viewModel.lastScan.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val sources by viewModel.sources.collectAsStateWithLifecycle()
    val ignoreShort by viewModel.ignoreShortFiles.collectAsStateWithLifecycle()
    var removeTarget by remember { mutableStateOf<List<MusicSource>?>(null) }
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
            SettingsGroupLabel(stringResource(R.string.settings_group_sources))
            MusicSourcesCard(sources, onRemove = { removeTarget = it })
            Text(
                text = stringResource(R.string.music_sources_hint),
                style = typography.bodySm,
                color = colors.textMuted,
                modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm)
            )
            Spacer(Modifier.height(spacing.sectionSpacing))
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
                Spacer(Modifier.height(spacing.md))
                AccentPicker(hue = accentHue, onSelect = viewModel::setAccentHue)
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
    removeTarget?.let { targets ->
        val folder = targets.singleOrNull()?.takeIf { it.kind == SourceKind.TREE }
        ResonanceDialog(
            title = stringResource(
                if (folder != null) R.string.music_remove_folder_title else R.string.music_remove_songs_title
            ),
            text = if (folder != null) {
                stringResource(R.string.music_remove_folder_body, folder.displayName)
            } else {
                stringResource(R.string.music_remove_songs_body)
            },
            confirmLabel = stringResource(R.string.music_remove),
            onConfirm = {
                viewModel.removeSources(targets.map { it.id })
                removeTarget = null
            },
            onDismiss = { removeTarget = null },
            dismissLabel = stringResource(R.string.action_dismiss)
        )
    }
}

/**
 * The folders and songs the library is built from. Folders get a row each;
 * individually added songs share ONE row (a row per song would bury the
 * folders). A folder whose access was lost stays listed (its songs are kept):
 * tapping it reopens the picker there, which repairs it.
 */
@Composable
private fun MusicSourcesCard(sources: List<MusicSource>, onRemove: (List<MusicSource>) -> Unit) {
    val actions = LocalMusicActions.current
    val colors = ResonanceTheme.colors
    Surface(
        shape = ResonanceTheme.radii.card,
        color = colors.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ResonanceTheme.spacing.lg)
    ) {
        Column {
            sources.filter { it.kind == SourceKind.TREE }.forEach { source ->
                ResonanceSettingsRow(
                    title = source.displayName,
                    subtitle = if (source.accessOk) {
                        songCountLabel(source.songCount)
                    } else {
                        stringResource(R.string.music_access_lost)
                    },
                    leading = { ResonanceSettingsIcon(icon = Icons.Filled.Folder, contentDescription = null) },
                    trailing = { RemoveButton(source.displayName) { onRemove(listOf(source)) } },
                    onClick = if (source.accessOk) null else ({ actions.addFolder(source.uri) })
                )
            }
            val files = sources.filter { it.kind == SourceKind.FILE }
            if (files.isNotEmpty()) {
                val count = songCountLabel(files.sumOf { it.songCount })
                val title = stringResource(R.string.music_added_songs)
                ResonanceSettingsRow(
                    title = title,
                    subtitle = if (files.all { it.accessOk }) {
                        count
                    } else {
                        count + " - " + stringResource(R.string.music_files_lost)
                    },
                    leading = { ResonanceSettingsIcon(icon = Icons.Filled.MusicNote, contentDescription = null) },
                    trailing = { RemoveButton(title) { onRemove(files) } }
                )
            }
            ResonanceSettingsRow(
                title = stringResource(R.string.music_add_folder),
                subtitle = null,
                leading = { ResonanceSettingsIcon(icon = Icons.Filled.CreateNewFolder, contentDescription = null) },
                trailing = { },
                onClick = { actions.addFolder(null) }
            )
            ResonanceSettingsRow(
                title = stringResource(R.string.music_add_songs),
                subtitle = null,
                leading = { ResonanceSettingsIcon(icon = Icons.Filled.MusicNote, contentDescription = null) },
                trailing = { },
                onClick = actions.addSongs
            )
        }
    }
}

@Composable
private fun RemoveButton(name: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            Icons.Filled.Close,
            contentDescription = stringResource(R.string.music_remove_named, name),
            tint = ResonanceTheme.colors.textSecondary
        )
    }
}

@Composable
private fun songCountLabel(count: Int): String =
    pluralStringResource(R.plurals.music_song_count, count, count)

/** Accent hue picker: curated swatches (tap) + a full hue slider (drag). */
@Composable
private fun AccentPicker(hue: Float, onSelect: (Float) -> Unit) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val spacing = ResonanceTheme.spacing
    Text(
        stringResource(R.string.settings_accent),
        style = typography.labelLg,
        color = colors.textSecondary
    )
    Spacer(Modifier.height(spacing.sm))
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
        ACCENT_PRESETS.forEach { (_, presetHue) ->
            val selected = kotlin.math.abs(presetHue - hue) < 1f
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accentPreviewColor(presetHue))
                    .border(
                        width = if (selected) 2.dp else 0.dp,
                        color = colors.textPrimary,
                        shape = CircleShape
                    )
                    .clickable { onSelect(presetHue) }
            )
        }
    }
    Spacer(Modifier.height(spacing.sm))
    Slider(
        value = hue,
        onValueChange = onSelect,
        valueRange = 0f..360f,
        colors = SliderDefaults.colors(
            thumbColor = colors.accent,
            activeTrackColor = colors.accent,
            inactiveTrackColor = colors.surfaceHighest
        )
    )
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
