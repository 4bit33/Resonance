package com.resonance.player.core.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.resonance.player.core.model.PlaybackSnapshot
import com.resonance.player.core.ui.theme.ResonanceTheme

/**
 * Pure rule: the mini player exists only while a track is loaded.
 * Tested — the shell must not invent visibility state.
 */
fun shouldShowMiniPlayer(snapshot: PlaybackSnapshot): Boolean = snapshot.song != null

/**
 * Stitch mini player: 64dp floating dock (16dp radius, high container,
 * strong top border), 40dp art, title/artist column, 48dp play + next
 * targets, 2dp copper progress along the bottom. Tap opens Now Playing.
 * Receives snapshot values as params — never touches playback state.
 */
@Composable
fun ResonanceMiniPlayer(
    title: String,
    artist: String,
    artwork: @Composable () -> Unit,
    isPlaying: Boolean,
    progress: Float,
    playDescription: String,
    pauseDescription: String,
    nextDescription: String?,
    onToggle: () -> Unit,
    onOpenPlayer: () -> Unit,
    onNext: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val spacing = ResonanceTheme.spacing
    val dimensions = ResonanceTheme.dimensions
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensions.miniPlayerHeight)
            .clip(ResonanceTheme.radii.card)
            .background(colors.surfaceHigh)
            .clickable(onClick = onOpenPlayer)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = spacing.md)
        ) {
            Box(
                modifier = Modifier
                    .size(dimensions.miniPlayerArtwork)
                    .clip(ResonanceTheme.radii.control)
            ) {
                artwork()
            }
            Spacer(Modifier.width(spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = typography.titleMd,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    artist,
                    style = typography.bodySm,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onToggle,
                modifier = Modifier.size(spacing.touchMin)
            ) {
                Crossfade(targetState = isPlaying, label = "mini-playback-icon") { playing ->
                    Icon(
                        if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playing) pauseDescription else playDescription,
                        tint = colors.textPrimary
                    )
                }
            }
            if (onNext != null) {
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(spacing.touchMin)
                ) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = nextDescription,
                        tint = colors.textSecondary
                    )
                }
            }
        }
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensions.miniProgress),
            color = colors.accent,
            trackColor = colors.outlineSubtle,
            strokeCap = StrokeCap.Butt,
            gapSize = 0.dp,
            drawStopIndicator = {}
        )
    }
}

/**
 * Up-Next queue peek card: accent eyebrow, next title/artist, duration,
 * drag affordance. Tap navigates to the full queue.
 */
@Composable
fun ResonanceQueuePeek(
    nextTitle: String,
    nextArtist: String,
    nextDuration: String,
    nextArtwork: @Composable () -> Unit,
    dragDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    eyebrow: String = "UP NEXT IN QUEUE",
    dragIcon: (@Composable () -> Unit)? = null
) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val spacing = ResonanceTheme.spacing
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(ResonanceTheme.radii.card)
            .background(colors.surfaceHigh)
            .clickable(onClick = onClick)
            .padding(spacing.md)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(ResonanceTheme.radii.control)
        ) {
            nextArtwork()
        }
        Spacer(Modifier.width(spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                eyebrow,
                style = typography.labelSm,
                color = colors.accent
            )
            Text(
                nextTitle,
                style = typography.titleMd,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                nextArtist,
                style = typography.bodySm,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(nextDuration, style = typography.monoMetric, color = colors.textSecondary)
        Spacer(Modifier.width(spacing.sm))
        if (dragIcon != null) {
            dragIcon()
        } else {
            Spacer(Modifier.size(20.dp))
        }
    }
}
