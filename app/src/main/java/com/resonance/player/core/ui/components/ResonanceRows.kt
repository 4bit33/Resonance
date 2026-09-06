package com.resonance.player.core.ui.components

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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.resonance.player.core.ui.theme.ResonanceTheme

/**
 * Stitch song-row visual states. Priority: missing > loading > selected >
 * playing > disabled > normal (a missing file is never shown as playing).
 */
enum class SongRowState { Normal, Playing, Selected, Disabled, Missing, Loading }

fun songRowState(
    isCurrent: Boolean,
    isSelected: Boolean,
    isMissing: Boolean,
    isLoading: Boolean,
    isEnabled: Boolean = true
): SongRowState = when {
    isMissing -> SongRowState.Missing
    isLoading -> SongRowState.Loading
    isSelected -> SongRowState.Selected
    isCurrent -> SongRowState.Playing
    !isEnabled -> SongRowState.Disabled
    else -> SongRowState.Normal
}

/**
 * Standard 64dp Stitch song row: 48dp art (8dp radius), two-tier
 * title/artist-album column, mono-metric badge + duration column, 48dp
 * overflow. Playing rows get the left indicator bar + EQ overlay + accent
 * title; missing rows get the error-container treatment.
 */
@Composable
fun ResonanceSongRow(
    title: String,
    artistLine: String,
    duration: String,
    artwork: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    state: SongRowState = SongRowState.Normal,
    badge: (@Composable () -> Unit)? = null,
    isPlayingAnimation: Boolean = false,
    onOverflowClick: (() -> Unit)? = null,
    onToggleSelect: (() -> Unit)? = null,
    missingMessage: String? = null
) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val spacing = ResonanceTheme.spacing
    val background = when (state) {
        SongRowState.Playing -> colors.accent.copy(alpha = 0.10f)
        SongRowState.Selected -> colors.surfaceHighest
        SongRowState.Missing -> colors.statusErrorContainer.copy(alpha = 0.20f)
        else -> androidx.compose.ui.graphics.Color.Transparent
    }
    Box(modifier = modifier.fillMaxWidth().background(background)) {
        if (state == SongRowState.Playing) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(width = 4.dp, height = ResonanceTheme.dimensions.songRowHeight)
                    .background(colors.accent)
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(ResonanceTheme.dimensions.songRowHeight)
                .padding(horizontal = spacing.lg)
                .clickable(
                    enabled = state != SongRowState.Disabled && state != SongRowState.Loading,
                    onClick = onClick
                )
        ) {
            if (state == SongRowState.Selected) {
                Checkbox(
                    checked = true,
                    onCheckedChange = { onToggleSelect?.invoke() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = colors.accent,
                        checkmarkColor = colors.onAccent
                    )
                )
                Spacer(Modifier.width(spacing.sm))
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(ResonanceTheme.dimensions.songArtwork)
            ) {
                artwork()
                if (state == SongRowState.Playing) {
                    EqualizerBars(
                        isPlaying = isPlayingAnimation,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.width(spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = typography.titleMd,
                    color = when (state) {
                        SongRowState.Playing -> colors.accent
                        SongRowState.Missing -> colors.textSecondary
                        else -> colors.textPrimary
                    },
                    textDecoration = if (state == SongRowState.Missing) {
                        TextDecoration.LineThrough
                    } else {
                        null
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (state == SongRowState.Missing && missingMessage != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = null,
                            tint = colors.statusError,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(spacing.xs))
                        Text(
                            missingMessage,
                            style = typography.bodySm,
                            color = colors.statusError,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Text(
                        artistLine,
                        style = typography.bodySm,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(spacing.sm))
            Column(horizontalAlignment = Alignment.End) {
                badge?.invoke()
                Text(duration, style = typography.monoMetric, color = colors.textSecondary)
            }
            if (onOverflowClick != null && state != SongRowState.Missing) {
                IconButton(
                    onClick = onOverflowClick,
                    modifier = Modifier.size(spacing.touchMin)
                ) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = null,
                        tint = colors.textSecondary
                    )
                }
            }
        }
    }
}

/**
 * Compact 48dp row for queue inspect mode: smaller art, single-line text,
 * optional drag handle + remove slots (slots keep reorder controls in the
 * screen, not the design system).
 */
@Composable
fun ResonanceCompactSongRow(
    title: String,
    subtitle: String,
    artwork: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    highlighted: Boolean = false
) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val spacing = ResonanceTheme.spacing
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(ResonanceTheme.dimensions.compactRowHeight)
            .padding(horizontal = spacing.lg)
            .clickable(onClick = onClick)
    ) {
        leading?.invoke()
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(40.dp).clip(ResonanceTheme.radii.control)
        ) {
            artwork()
        }
        Spacer(Modifier.width(spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = typography.titleMd,
                color = if (highlighted) colors.accent else colors.textPrimary,
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
        trailing?.invoke()
    }
}
