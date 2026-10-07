package com.resonance.player.core.ui.components

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.material3.ripple
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.resonance.player.R
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
 * Song row: 52dp cover with soft corners, title over "artist · album", a
 * quiet duration. The playing song gets an accent title and moving bars
 * over its cover. A long press opens the song menu ([onLongClick]); the row
 * squeezes slightly under the finger. [badge] is accepted for old call sites
 * but no longer drawn (format details live in the song's tech sheet).
 * [trailingInset] keeps the duration clear of the Library fast scroller.
 */
@OptIn(ExperimentalFoundationApi::class)
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
    onLongClick: (() -> Unit)? = null,
    onToggleSelect: (() -> Unit)? = null,
    missingMessage: String? = null,
    trailingInset: Dp = 0.dp
) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val motion = ResonanceTheme.motion
    val haptics = LocalHapticFeedback.current
    val menu: (() -> Unit)? = if (onLongClick != null && state != SongRowState.Missing) {
        {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onLongClick()
        }
    } else {
        null
    }
    val playing = state == SongRowState.Playing
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, motion.expressive(), label = "row-press")
    val titleColor by animateColorAsState(
        when (state) {
            SongRowState.Playing -> colors.accent
            SongRowState.Missing, SongRowState.Disabled -> colors.textMuted
            else -> colors.textPrimary
        },
        motion.effects(),
        label = "row-title"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(if (state == SongRowState.Selected) colors.surfaceHigh else androidx.compose.ui.graphics.Color.Transparent)
            .combinedClickable(
                interactionSource = interaction,
                indication = ripple(),
                enabled = state != SongRowState.Disabled && state != SongRowState.Loading,
                onLongClickLabel = if (menu != null) stringResource(R.string.song_actions) else null,
                onLongClick = menu,
                onClick = onClick
            )
            .padding(start = 20.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
    ) {
        if (state == SongRowState.Selected) {
            Checkbox(
                checked = true,
                onCheckedChange = { onToggleSelect?.invoke() },
                colors = CheckboxDefaults.colors(checkedColor = colors.accent, checkmarkColor = colors.onAccent)
            )
            Spacer(Modifier.width(8.dp))
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            artwork()
            if (playing) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .matchParentSize()
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f))
                ) {
                    EqualizerBars(isPlaying = isPlayingAnimation, modifier = Modifier.size(20.dp))
                }
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = typography.titleMd,
                color = titleColor,
                textDecoration = if (state == SongRowState.Missing) TextDecoration.LineThrough else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (state == SongRowState.Missing && missingMessage != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = colors.statusError, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(missingMessage, style = typography.bodySm, color = colors.statusError, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            } else {
                Text(
                    artistLine.replace(" - ", " · "),
                    style = typography.bodySm,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            duration,
            style = typography.bodySm.copy(fontFeatureSettings = "tnum"),
            color = colors.textMuted
        )
        Spacer(Modifier.width(trailingInset))
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
