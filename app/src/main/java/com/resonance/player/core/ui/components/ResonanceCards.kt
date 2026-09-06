package com.resonance.player.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.resonance.player.core.ui.theme.ResonanceTheme

/**
 * Audio format badge: mono-metric pill. Lossless/highlighted badges use
 * audio cyan on its dim container; lossy ones use muted text
 * (e.g. "FLAC 24b" vs "320k MP3" in Stitch rows).
 */
@Composable
fun ResonanceFormatBadge(
    text: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = true
) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    Text(
        text,
        style = typography.monoMetric,
        color = if (highlighted) colors.accentSecondary else colors.textMuted,
        maxLines = 1,
        modifier = modifier
    )
}

/** Tabular metric text (durations, counts, storage, tech specs). */
@Composable
fun ResonanceMetric(
    text: String,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = ResonanceTheme.colors.textSecondary
) {
    Text(text, style = ResonanceTheme.typography.monoMetric, color = color, modifier = modifier)
}

/**
 * 160dp album card: 16dp art tile with gradient-safe overlay slot, title-md
 * + body-sm caption, optional accent play-button overlay.
 */
@Composable
fun ResonanceAlbumCard(
    title: String,
    artist: String,
    artwork: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onPlayClick: (() -> Unit)? = null,
    playButton: (@Composable () -> Unit)? = null
) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val spacing = ResonanceTheme.spacing
    Column(modifier = modifier.width(ResonanceTheme.dimensions.albumCardWidth)) {
        Box(
            modifier = Modifier
                .size(ResonanceTheme.dimensions.albumCardWidth)
                .clip(ResonanceTheme.radii.card)
                .background(colors.surfaceHigh)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.BottomEnd
        ) {
            artwork()
            if (onPlayClick != null || playButton != null) {
                Box(modifier = Modifier.padding(spacing.sm)) {
                    playButton?.invoke()
                }
            }
        }
        Spacer(Modifier.height(spacing.sm))
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
}

/** Playlist/artist tile card: art + two-line caption (shared geometry). */
@Composable
fun ResonancePlaylistCard(
    title: String,
    subtitle: String,
    artwork: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val spacing = ResonanceTheme.spacing
    Column(modifier = modifier.width(ResonanceTheme.dimensions.albumCardWidth)) {
        Box(
            modifier = Modifier
                .size(ResonanceTheme.dimensions.albumCardWidth)
                .clip(ResonanceTheme.radii.card)
                .background(colors.surfaceContainer)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            artwork()
        }
        Spacer(Modifier.height(spacing.sm))
        Text(
            title,
            style = typography.titleMd,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            subtitle,
            style = typography.bodySm,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Small accent play overlay used on album cards (40dp copper circle). */
@Composable
fun ResonanceCardPlayButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    playIcon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?
) {
    val colors = ResonanceTheme.colors
    Surface(
        onClick = onClick,
        shape = ResonanceTheme.radii.full,
        color = colors.accent,
        contentColor = colors.onAccent,
        modifier = modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
            androidx.compose.material3.Icon(
                playIcon,
                contentDescription = contentDescription,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
