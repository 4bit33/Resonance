package com.resonance.player.core.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.resonance.player.core.ui.theme.ResonanceTheme

/**
 * Primary circular playback button: 64dp copper fill, dark glyph
 * (DESIGN.md Now Playing master switch). Guaranteed 48dp+ touch target.
 */
@Composable
fun ResonancePlaybackButton(
    playing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    playIcon: ImageVector,
    pauseIcon: ImageVector,
    playDescription: String,
    pauseDescription: String,
    enabled: Boolean = true,
    size: androidx.compose.ui.unit.Dp = ResonanceTheme.dimensions.primaryPlayButton
) {
    val colors = ResonanceTheme.colors
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = ResonanceTheme.radii.full,
        color = colors.accent,
        contentColor = colors.onAccent,
        modifier = modifier
            .size(size)
            .defaultMinSize(
                minWidth = ResonanceTheme.spacing.touchMin,
                minHeight = ResonanceTheme.spacing.touchMin
            )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Crossfade(targetState = playing, label = "playback-icon") { isPlaying ->
                Icon(
                    if (isPlaying) pauseIcon else playIcon,
                    contentDescription = if (isPlaying) pauseDescription else playDescription,
                    modifier = Modifier.size(size * 0.45f)
                )
            }
        }
    }
}

/** Ghost circular transport button (prev/next/shuffle/repeat/favorite/more). */
@Composable
fun ResonanceIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector,
    contentDescription: String?,
    enabled: Boolean = true,
    active: Boolean = false
) {
    val colors = ResonanceTheme.colors
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(ResonanceTheme.spacing.touchMin)
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = when {
                !enabled -> colors.textMuted
                active -> colors.accent
                else -> colors.textPrimary
            }
        )
    }
}

/**
 * Filter chip: 32dp, 8dp radius. Selected = 15% accent fill + accent
 * border + accent text; unselected = highest container + secondary text
 * with an optional muted mono-metric count.
 */
@Composable
fun ResonanceChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    count: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    Surface(
        onClick = onClick,
        shape = ResonanceTheme.radii.control,
        color = if (selected) colors.accent.copy(alpha = 0.15f) else colors.surfaceHighest,
        border = BorderStroke(
            width = androidx.compose.ui.unit.Dp.Hairline,
            color = if (selected) colors.accent else colors.outlineSubtle
        ),
        modifier = modifier.height(32.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ResonanceTheme.spacing.xs),
            modifier = Modifier.padding(horizontal = ResonanceTheme.spacing.md)
        ) {
            leadingIcon?.invoke()
            Text(
                label,
                style = typography.labelMd,
                color = if (selected) colors.accent else colors.textSecondary
            )
            if (count != null) {
                Text(
                    count,
                    style = typography.monoMetric,
                    color = if (selected) colors.accent else colors.textMuted
                )
            }
        }
    }
}

/** Segmented single-select control (tempo arcs, theme picker, transitions). */
@Composable
fun ResonanceSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    Surface(
        shape = ResonanceTheme.radii.control,
        color = colors.surfaceHighest,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(ResonanceTheme.spacing.xs)) {
            options.forEachIndexed { index, option ->
                val selected = index == selectedIndex
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(ResonanceTheme.radii.control)
                        .clickable(role = Role.RadioButton) { onSelect(index) }
                        .background(
                            if (selected) colors.accentDim else androidx.compose.ui.graphics.Color.Transparent
                        )
                        .padding(vertical = ResonanceTheme.spacing.sm)
                ) {
                    Text(
                        option,
                        style = typography.labelMd,
                        color = if (selected) colors.accent else colors.textSecondary
                    )
                }
            }
        }
    }
}

/** Stitch toggle: copper track when on, highest container when off. */
@Composable
fun ResonanceSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = ResonanceTheme.colors
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = colors.onAccent,
            checkedTrackColor = colors.accent,
            checkedBorderColor = colors.accent,
            uncheckedThumbColor = colors.textPrimary,
            uncheckedTrackColor = colors.surfaceHighest,
            uncheckedBorderColor = androidx.compose.ui.graphics.Color.Transparent
        ),
        modifier = modifier
    )
}

/** Settings-cluster row slot used by Stitch grouped rows (icon + texts + trailing). */
@Composable
fun ResonanceSettingsRow(
    title: String,
    subtitle: String?,
    leading: @Composable () -> Unit,
    trailing: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val spacing = ResonanceTheme.spacing
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(spacing.lg)
    ) {
        leading()
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = spacing.md)
        ) {
            Text(title, style = typography.titleMd, color = colors.textPrimary)
            if (subtitle != null) {
                Text(subtitle, style = typography.bodySm, color = colors.textSecondary)
            }
        }
        trailing()
    }
}

/** 40dp settings-cluster leading icon box (high container, secondary glyph). */
@Composable
fun ResonanceSettingsIcon(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val colors = ResonanceTheme.colors
    Surface(
        shape = ResonanceTheme.radii.control,
        color = colors.surfaceHigh,
        modifier = modifier.size(40.dp),
        contentColor = colors.textSecondary
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(20.dp))
        }
    }
}

/** Primary CTA: 48dp, 8dp radius, copper fill, dark bold label. */
@Composable
fun ResonancePrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    androidx.compose.material3.Button(
        onClick = onClick,
        enabled = enabled,
        shape = ResonanceTheme.radii.control,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.accent,
            contentColor = colors.onAccent,
            disabledContainerColor = colors.surfaceHighest,
            disabledContentColor = colors.textMuted
        ),
        contentPadding = PaddingValues(horizontal = ResonanceTheme.spacing.md),
        modifier = modifier.height(ResonanceTheme.spacing.touchMin)
    ) {
        Text(label, style = typography.labelLg)
    }
}
