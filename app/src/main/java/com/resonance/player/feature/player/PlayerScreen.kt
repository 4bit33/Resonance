package com.resonance.player.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Slider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonance.player.R
import com.resonance.player.core.common.formatBytes
import com.resonance.player.core.common.formatDurationMs
import com.resonance.player.core.common.userMessage
import com.resonance.player.core.model.RepeatMode
import com.resonance.player.core.model.ShuffleMode
import com.resonance.player.core.ui.components.ArtworkImage
import com.resonance.player.core.ui.components.ResonanceBottomSheet
import com.resonance.player.core.ui.components.ResonanceIconButton
import com.resonance.player.core.ui.components.ResonanceMetric
import com.resonance.player.core.ui.components.ResonancePlaybackButton
import com.resonance.player.core.ui.components.ResonanceQueuePeek
import com.resonance.player.core.ui.components.ResonanceTopBar
import com.resonance.player.core.ui.components.SongFormatBadge
import com.resonance.player.core.ui.theme.ResonanceTheme

/**
 * Stitch Now Playing bound to the REAL engine: meta badges, large artwork
 * with queue counter, custom scrubber (real position + buffered layer),
 * 64dp master switch, favorite toggle, queue peek, technical sheet with
 * real file data. EQ/sleep-timer controls are intentionally absent (no
 * such functionality exists — never fake it).
 */
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onOpenQueue: () -> Unit,
    onBack: () -> Unit
) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val spacing = ResonanceTheme.spacing
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val details by viewModel.details.collectAsStateWithLifecycle()
    val commandError by viewModel.commandError.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()

    val song = snapshot.song ?: details
    val duration = snapshot.durationMs
    val hasQueue = snapshot.queue.isNotEmpty()

    var dragging by remember { mutableStateOf(false) }
    var dragMs by remember { mutableFloatStateOf(0f) }
    val shownPosition = if (dragging) dragMs.toLong() else snapshot.positionMs
    var techSheetOpen by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        ResonanceTopBar(
            title = stringResource(R.string.nav_player),
            onBack = onBack
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.lg)
        ) {
            if (song != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SongFormatBadge(song)
                    Spacer(Modifier.weight(1f))
                    ResonanceIconButton(
                        onClick = viewModel::onToggleFavorite,
                        icon = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = stringResource(R.string.cd_favorite),
                        active = isFavorite
                    )
                }
                Spacer(Modifier.height(spacing.sm))
                ArtworkImage(
                    artworkUri = song.artworkUri,
                    contentDescription = song.albumName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(ResonanceTheme.radii.card)
                )
                Spacer(Modifier.height(spacing.lg))
                Text(
                    song.title,
                    style = typography.headlineLg,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    song.artistName,
                    style = typography.titleMd,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    albumLine(song.albumName, song.year),
                    style = typography.bodyMd,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = stringResource(R.string.empty_library_title),
                    style = typography.headlineMd,
                    color = colors.textPrimary
                )
            }
            Spacer(Modifier.height(spacing.lg))
            StitchScrubber(
                positionMs = shownPosition.coerceAtLeast(0L),
                durationMs = duration,
                bufferedMs = snapshot.bufferedPositionMs,
                enabled = hasQueue && duration > 0L,
                onSeekStart = { dragging = true },
                onSeek = { dragMs = it.toFloat() },
                onSeekEnd = {
                    dragging = false
                    viewModel.onSeek(dragMs.toLong())
                }
            )
            Spacer(Modifier.height(spacing.md))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(Modifier.weight(1f))
                ResonanceIconButton(
                    onClick = { viewModel.onToggleShuffle(snapshot.shuffle) },
                    icon = Icons.Filled.Shuffle,
                    contentDescription = stringResource(R.string.cd_shuffle),
                    active = snapshot.shuffle == ShuffleMode.ON
                )
                ResonanceIconButton(
                    onClick = viewModel::onPrevious,
                    icon = Icons.Filled.SkipPrevious,
                    contentDescription = stringResource(R.string.cd_previous),
                    enabled = hasQueue
                )
                ResonancePlaybackButton(
                    playing = snapshot.isPlaying,
                    onClick = viewModel::onTogglePlayPause,
                    playIcon = Icons.Filled.PlayArrow,
                    pauseIcon = Icons.Filled.Pause,
                    playDescription = stringResource(R.string.cd_play),
                    pauseDescription = stringResource(R.string.cd_pause),
                    enabled = hasQueue
                )
                ResonanceIconButton(
                    onClick = viewModel::onNext,
                    icon = Icons.Filled.SkipNext,
                    contentDescription = stringResource(R.string.cd_next),
                    enabled = hasQueue
                )
                ResonanceIconButton(
                    onClick = { viewModel.onCycleRepeat(snapshot.repeat) },
                    icon = if (snapshot.repeat == RepeatMode.ONE) {
                        Icons.Filled.RepeatOne
                    } else {
                        Icons.Filled.Repeat
                    },
                    contentDescription = stringResource(R.string.cd_repeat),
                    active = snapshot.repeat != RepeatMode.OFF
                )
                Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(spacing.md))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ResonanceIconButton(
                    onClick = { techSheetOpen = true },
                    icon = Icons.Filled.Info,
                    contentDescription = stringResource(R.string.tech_specs),
                    enabled = song != null
                )
                Spacer(Modifier.weight(1f))
                if (hasQueue) {
                    Text(
                        stringResource(R.string.queue_counter, snapshot.queueIndex + 1, snapshot.queue.size),
                        style = typography.monoMetric,
                        color = colors.textSecondary
                    )
                }
            }
            Spacer(Modifier.height(spacing.md))
            val nextItem = snapshot.queue.getOrNull(snapshot.queueIndex + 1)
            if (nextItem != null) {
                ResonanceQueuePeek(
                    nextTitle = nextItem.song.title,
                    nextArtist = nextItem.song.artistName,
                    nextDuration = formatDurationMs(nextItem.song.durationMs),
                    nextArtwork = {
                        ArtworkImage(
                            artworkUri = nextItem.song.artworkUri,
                            contentDescription = nextItem.song.albumName
                        )
                    },
                    dragDescription = null,
                    onClick = onOpenQueue
                )
                Spacer(Modifier.height(spacing.md))
            }
            val visibleError = commandError ?: snapshot.error
            if (visibleError != null) {
                Text(
                    text = visibleError.userMessage(),
                    style = typography.bodyMd,
                    color = colors.error
                )
                TextButton(onClick = viewModel::clearCommandError) {
                    Text(stringResource(R.string.action_dismiss))
                }
                Spacer(Modifier.height(spacing.sm))
            }
            Spacer(Modifier.height(spacing.xxl))
        }
    }
    if (techSheetOpen && song != null) {
        ResonanceBottomSheet(onDismiss = { techSheetOpen = false }) {
            TechSheetContent(
                title = song.title,
                rows = techRows(song)
            )
        }
    }
}

private fun albumLine(album: String, year: Int?): String =
    if (year != null && year > 0) "$album ($year)" else album

/**
 * Stitch scrubber: 4dp track with a buffered layer, copper progress and a
 * 14dp thumb; mono-metric timestamps flank the bar. Position comes from the
 * snapshot ticker; writes happen only on release (no command spam).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StitchScrubber(
    positionMs: Long,
    durationMs: Long,
    bufferedMs: Long,
    enabled: Boolean,
    onSeekStart: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekEnd: () -> Unit
) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val spacing = ResonanceTheme.spacing
    val dimensions = ResonanceTheme.dimensions
    val range = 0f..durationMs.coerceAtLeast(1L).toFloat()
    Column {
        Slider(
            value = positionMs.toFloat().coerceIn(range),
            onValueChange = {
                onSeekStart()
                onSeek(it.toLong())
            },
            onValueChangeFinished = onSeekEnd,
            valueRange = range,
            enabled = enabled,
            thumb = {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.size(dimensions.scrubThumb)
                ) {
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier.matchParentSize()
                    ) {
                        drawCircle(color = colors.textPrimary, radius = size.minDimension / 2f)
                    }
                }
            },
            track = { state ->
                val progress = if (range.endInclusive > 0f) {
                    (state.value / range.endInclusive).coerceIn(0f, 1f)
                } else {
                    0f
                }
                val buffered = if (durationMs > 0L) {
                    (bufferedMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensions.scrubTrack)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(colors.outlineSubtle)
                ) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxWidth(buffered)
                            .height(dimensions.scrubTrack)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(colors.textMuted)
                    )
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(dimensions.scrubTrack)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(colors.accent)
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            ResonanceMetric(text = formatDurationMs(positionMs))
            Spacer(Modifier.weight(1f))
            ResonanceMetric(text = formatDurationMs(durationMs))
        }
        Spacer(Modifier.height(spacing.xs))
    }
}

private data class TechRow(val label: String, val value: String)

/**
 * Technical sheet rows from REAL file data only. Channels and bit depth
 * are unavailable on-device (no MediaStore column, no retriever key), so
 * they are omitted rather than fabricated.
 */
private fun techRows(song: com.resonance.player.core.model.Song): List<TechRow> {
    val rows = ArrayList<TechRow>()
    rows.add(TechRow("Title", song.title))
    rows.add(TechRow("Artist", song.artistName))
    rows.add(TechRow("Album", albumLine(song.albumName, song.year)))
    song.albumArtist?.let { rows.add(TechRow("Album artist", it)) }
    song.genreName?.let { rows.add(TechRow("Genre", it)) }
    containerName(song.mimeType)?.let { rows.add(TechRow("Container", it)) }
    song.mimeType?.let { rows.add(TechRow("MIME type", it)) }
    if (song.bitrate != null && song.bitrate > 0) {
        rows.add(TechRow("Bitrate", "${song.bitrate / 1000} kbps"))
    }
    if (song.sampleRate != null && song.sampleRate > 0) {
        val khz = song.sampleRate / 1000.0
        val formatted = if (khz % 1.0 == 0.0) {
            khz.toLong().toString()
        } else {
            khz.toString()
        }
        rows.add(TechRow("Sample rate", "$formatted kHz"))
    }
    song.trackNumber?.let { track ->
        val total = song.totalTracks?.let { " / $it" } ?: ""
        rows.add(TechRow("Track", "$track$total"))
    }
    song.discNumber?.let { disc ->
        val total = song.totalDiscs?.let { " / $it" } ?: ""
        rows.add(TechRow("Disc", "$disc$total"))
    }
    rows.add(TechRow("Duration", formatDurationMs(song.durationMs)))
    rows.add(TechRow("Size", formatBytes(song.fileSizeBytes)))
    rows.add(TechRow("Location", song.path))
    return rows
}

private fun containerName(mimeType: String?): String? {
    if (mimeType == null) return null
    return when {
        "flac" in mimeType -> "FLAC"
        "mp4" in mimeType || "m4a" in mimeType || "aac" in mimeType -> "MPEG-4 Audio"
        "ogg" in mimeType || "opus" in mimeType -> "Ogg"
        "wav" in mimeType || "x-wav" in mimeType -> "WAV"
        "mpeg" in mimeType -> "MP3"
        else -> null
    }
}

@Composable
private fun TechSheetContent(title: String, rows: List<TechRow>) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val spacing = ResonanceTheme.spacing
    Column {
        Text(title, style = typography.titleMd, color = colors.textPrimary)
        Spacer(Modifier.height(spacing.md))
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.xs)
            ) {
                ResonanceMetric(text = row.label, color = colors.textSecondary)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(spacing.lg))
                Text(
                    row.value,
                    style = typography.bodyMd,
                    color = colors.textPrimary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
