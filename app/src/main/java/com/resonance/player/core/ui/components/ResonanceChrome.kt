package com.resonance.player.core.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.resonance.player.core.ui.theme.ResonanceTheme

/** 56dp Stitch top bar: title + global search + overflow slots. */
@Composable
fun ResonanceTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    searchIcon: ImageVector? = null,
    onSearch: (() -> Unit)? = null,
    searchDescription: String? = null,
    overflowIcon: ImageVector? = null,
    onOverflow: (() -> Unit)? = null,
    overflowDescription: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val spacing = ResonanceTheme.spacing
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(ResonanceTheme.dimensions.topBarHeight)
            .padding(horizontal = spacing.lg)
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.size(spacing.touchMin)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = colors.textPrimary)
            }
        }
        Text(
            title,
            style = typography.headlineMd,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        if (searchIcon != null && onSearch != null) {
            IconButton(onClick = onSearch, modifier = Modifier.size(spacing.touchMin)) {
                Icon(searchIcon, contentDescription = searchDescription, tint = colors.textSecondary)
            }
        }
        if (overflowIcon != null && onOverflow != null) {
            IconButton(onClick = onOverflow, modifier = Modifier.size(spacing.touchMin)) {
                Icon(overflowIcon, contentDescription = overflowDescription, tint = colors.textSecondary)
            }
        }
        trailing?.invoke()
    }
}

/** Section header: title-md + optional copper action (View all / Sort). */
@Composable
fun ResonanceSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ResonanceTheme.spacing.lg)
    ) {
        Text(
            title,
            style = typography.titleMd,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        if (actionLabel != null && onAction != null) {
            Text(
                actionLabel,
                style = typography.labelMd,
                color = colors.accent,
                modifier = Modifier.clickable(onClick = onAction)
            )
        }
        trailing?.invoke()
    }
}

/** 48dp filled search field (container-high, 8dp radius, clear action). */
@Composable
fun ResonanceSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    searchIcon: ImageVector? = null,
    clearIcon: ImageVector? = null,
    clearDescription: String? = null,
    onSearch: (() -> Unit)? = null
) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = typography.bodyMd) },
        singleLine = true,
        shape = ResonanceTheme.radii.control,
        textStyle = typography.bodyLg.copy(color = colors.textPrimary),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = colors.surfaceHigh,
            focusedContainerColor = colors.surfaceHigh,
            unfocusedBorderColor = colors.outlineSubtle,
            focusedBorderColor = colors.accent,
            unfocusedLabelColor = colors.textMuted,
            focusedLabelColor = colors.textSecondary,
            cursorColor = colors.accent
        ),
        leadingIcon = if (searchIcon != null) {
            { Icon(searchIcon, contentDescription = null, tint = colors.textSecondary) }
        } else {
            null
        },
        trailingIcon = if (value.isNotEmpty() && clearIcon != null) {
            {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier.size(ResonanceTheme.spacing.touchMin)
                ) {
                    Icon(clearIcon, contentDescription = clearDescription, tint = colors.textSecondary)
                }
            }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    )
}

/** Custom 68dp Stitch navigation dock with dot indicator. */
@Composable
fun ResonanceNavDock(
    destinations: List<NavDockDestination>,
    selectedRoute: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val spacing = ResonanceTheme.spacing
    androidx.compose.material3.Surface(
        color = colors.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .height(ResonanceTheme.dimensions.navigationDockHeight)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.fillMaxWidth()
        ) {
            destinations.forEach { destination ->
                val selected = destination.route == selectedRoute
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .size(spacing.touchMin)
                        .clickable(onClick = { onSelect(destination.route) })
                ) {
                    val iconTint by animateColorAsState(
                        if (selected) colors.accent else colors.textSecondary,
                        label = "nav-icon-tint"
                    )
                    Icon(
                        destination.icon,
                        contentDescription = destination.label,
                        tint = iconTint,
                        modifier = Modifier.size(ResonanceTheme.dimensions.navIcon)
                    )
                    Spacer(Modifier.height(2.dp))
                    Crossfade(targetState = selected, label = "nav-indicator") { isSelected ->
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .background(colors.accent, ResonanceTheme.radii.full)
                            )
                        } else {
                            Text(
                                destination.label,
                                style = typography.labelSm,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

data class NavDockDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)
