package com.resonance.player.core.ui.components

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.runtime.getValue
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

/** Filter chip (Material 3): a check appears when selected; [count] trails the label. */
@Composable
fun ResonanceChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    count: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(if (count != null) "$label  $count" else label) },
        leadingIcon = leadingIcon ?: if (selected) {
            { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
        } else {
            null
        },
        shape = CircleShape,
        modifier = modifier
    )
}

/** Single-choice segmented buttons (Material 3); the selected segment shows a check. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResonanceSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
            ) {
                Text(option, maxLines = 1)
            }
        }
    }
}

/** Material 3 switch; a check rides on the thumb when on. */
@Composable
fun ResonanceSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        thumbContent = if (checked) {
            { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
        } else {
            null
        },
        modifier = modifier
    )
}

/** A settings line (Material 3 list item): leading icon, title, optional supporting text, trailing slot. */
@Composable
fun ResonanceSettingsRow(
    title: String,
    subtitle: String?,
    leading: @Composable () -> Unit,
    trailing: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = leading,
        trailingContent = { Row(verticalAlignment = Alignment.CenterVertically, content = trailing) },
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    )
}

/** Leading icon for a settings line: tonal circle. */
@Composable
fun ResonanceSettingsIcon(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(20.dp))
        }
    }
}

/** Tonal button (Material 3). */
@Composable
fun ResonancePrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    FilledTonalButton(onClick = onClick, enabled = enabled, modifier = modifier) {
        Text(label)
    }
}
